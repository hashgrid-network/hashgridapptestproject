package com.example.service

import com.example.model.ActiveContract
import com.example.model.ActivityItem
import com.example.model.MiningPlan
import com.example.model.OFFICIAL_BEP20_ADDRESS
import com.example.model.OFFICIAL_TRC20_ADDRESS
import com.example.model.PayoutItem
import com.example.model.PayoutStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class FirebaseUser(
    val uid: String,
    val email: String,
    val walletBalance: Double,
    val miningRate: String,
    val createdAt: String
)

data class FirebaseWithdrawal(
    val requestId: String,
    val userId: String,
    val amount: Double,
    val cryptoAddress: String,
    val network: String,
    val status: String, // "pending", "approved", "rejected"
    val timestamp: String
)

data class FirebaseDeposit(
    val paymentId: String,
    val userId: String,
    val amount: Double,
    val currency: String,
    val network: String = "TRC20",
    val txHash: String = "",
    val status: String, // "pending", "confirmed"
    val timestamp: String
)

data class FirebaseTaskClaim(
    val claimId: String,
    val userId: String,
    val userEmail: String,
    val deviceId: String,
    val taskId: String,
    val taskTitle: String,
    val proofLink: String,
    val requestedAmountUsdt: Double,
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val timestamp: String
)

data class UserRemoteData(
    val usdtBalance: Double = 0.0,
    val btcBalance: Double = 0.0,
    val hashRate: Double = 0.0,
    val totalWithdrawn: Double = 0.0,
    val referralCount: Long = 0,
    val bonusHashrate: Double = 0.0,
    val referralCode: String = "",
    val referredBy: String? = null,
    val kycStatus: String = "UNVERIFIED",
    val twoFactorEnabled: Boolean = false,
    val activityLogs: List<ActivityItem> = emptyList(),
    val gridCoinBalance: Double = 0.0,
    val gridMiningActive: Boolean = false,
    val gridSessionStart: Long = 0L,
    val gridSessionEnd: Long = 0L,
    val appliedBaseRate: Double = 1.0,
    val activeTeamBonus: Double = 0.0
)

object FirebaseSyncService {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Configurable Firebase Realtime Database Base URL
    var firebaseDatabaseUrl: String = "https://hashgrid-institutional-default-rtdb.firebaseio.com"

    val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }

    private val _isFirebaseSynced = MutableStateFlow(true)
    val isFirebaseSynced: StateFlow<Boolean> = _isFirebaseSynced.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    // Permanently locked official deposit addresses
    var trc20DepositAddress: String = OFFICIAL_TRC20_ADDRESS
    var bep20DepositAddress: String = OFFICIAL_BEP20_ADDRESS

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    init {
        fetchPaymentSettings()
    }

    fun fetchPaymentSettings() {
        scope.launch {
            try {
                firestore?.collection("app_config")?.document("payment_settings")?.get()
                    ?.addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            doc.getString("trc20_address")?.let { if (it.isNotBlank()) trc20DepositAddress = it }
                            doc.getString("bep20_address")?.let { if (it.isNotBlank()) bep20DepositAddress = it }
                        }
                    }
            } catch (_: Exception) {}
        }
    }

    /**
     * Fetch user record from Firestore (/users/{uid}) and RTDB
     */
    fun fetchUserData(uid: String): UserRemoteData? {
        val safeKey = sanitizeKey(uid)
        return try {
            val url = "$firebaseDatabaseUrl/users/$safeKey.json"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim()
                    if (!body.isNullOrBlank() && body != "null") {
                        val obj = JSONObject(body)
                        val usdt = obj.optDouble("usdt_balance", obj.optDouble("walletBalance", 0.0))
                        val btc = obj.optDouble("btc_balance", 0.0)
                        val withdrawn = obj.optDouble("total_withdrawn", 0.0)
                        val hr = obj.optDouble("hash_rate", 0.0)
                        val kyc = obj.optString("kyc_status", "UNVERIFIED")
                        val twoFa = obj.optBoolean("two_factor_enabled", false)
                        val refCount = obj.optLong("referral_count", 0L)
                        val bonusHr = obj.optDouble("bonus_hashrate", 0.0)
                        val refCode = obj.optString("referral_code", "")
                        val refBy = obj.optString("referred_by", null)
                        UserRemoteData(
                            usdtBalance = usdt,
                            btcBalance = btc,
                            hashRate = hr,
                            totalWithdrawn = withdrawn,
                            referralCount = refCount,
                            bonusHashrate = bonusHr,
                            referralCode = refCode,
                            referredBy = refBy,
                            kycStatus = kyc,
                            twoFactorEnabled = twoFa
                        )
                    } else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Validates referral code in Firestore transactional/query lookup
     * Returns Pair(isValid, referrerUid) and updates referrer atomically (+1 referral_count, +1.5 GH/s bonus)
     */
    suspend fun validateAndApplyReferral(cleanCode: String, newUid: String): Pair<Boolean, String?> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val uppercaseCode = cleanCode.trim().uppercase()
        if (uppercaseCode.isBlank()) return@withContext Pair(false, null)

        try {
            val db = firestore
            if (db != null) {
                val querySnap = db.collection("users")
                    .whereEqualTo("referral_code", uppercaseCode)
                    .limit(1)
                    .get()
                    .await()

                if (!querySnap.isEmpty) {
                    val referrerDoc = querySnap.documents[0]
                    val referrerUid = referrerDoc.getString("uid") ?: referrerDoc.id

                    // Bulletproof Self-referral prevention lock
                    if (referrerUid.isNotBlank() && referrerUid != newUid && referrerDoc.id != newUid) {
                        try {
                            db.collection("users").document(referrerUid).update(
                                "referral_count", FieldValue.increment(1),
                                "bonus_hashrate", FieldValue.increment(1.5),
                                "hash_rate", FieldValue.increment(1.5)
                            )
                        } catch (_: Exception) {}
                        return@withContext Pair(true, referrerUid)
                    } else {
                        // Matching self-referral is explicitly rejected
                        return@withContext Pair(false, null)
                    }
                }
            }
        } catch (_: Exception) {}
        Pair(false, null)
    }

    /**
     * Initializes a NEW user document in Firestore and Firebase RTDB /users/{uid}
     */
    fun initializeNewUser(
        uid: String,
        email: String,
        displayName: String,
        photoUrl: String?,
        accountId: String,
        referralCode: String = "",
        referredBy: String? = null,
        referrerUid: String? = null,
        welcomeBonusHashrate: Double = 0.0
    ) {
        scope.launch {
            val safeKey = sanitizeKey(uid)
            val assignedRefCode = if (referralCode.isNotBlank()) referralCode else "HG-" + uid.replace("-", "").takeLast(4).uppercase()
            val isGodMode = email.equals("parkashom8080@gmail.com", ignoreCase = true)
            val initialUsdt = if (isGodMode) 1000.00 else 0.00
            val initialGrid = if (isGodMode) 50.00 else 0.00
            val initialHashrate = if (isGodMode) 500000.0 else welcomeBonusHashrate

            try {
                // 1. Initialize in Firestore /users/{uid}
                val firestoreMap = hashMapOf<String, Any>(
                    "uid" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "photoUrl" to (photoUrl ?: ""),
                    "accountId" to accountId,
                    "referralCode" to assignedRefCode,
                    "referral_code" to assignedRefCode,
                    "referredBy" to (referredBy ?: ""),
                    "referred_by" to (referredBy ?: ""),
                    "referrer_uid" to (referrerUid ?: ""),
                    "referral_count" to (if (isGodMode) 12L else 0L),
                    "bonus_hashrate" to (if (isGodMode) 50.0 else welcomeBonusHashrate),
                    "usdtBalance" to initialUsdt,
                    "usdt_balance" to initialUsdt,
                    "gridBalance" to initialGrid,
                    "grid_coin_balance" to initialGrid,
                    "totalMined" to 0.0,
                    "isGodMode" to isGodMode,
                    "btc_balance" to 0.000000,
                    "total_withdrawn" to 0.00,
                    "hash_rate" to initialHashrate,
                    "kyc_status" to "UNVERIFIED",
                    "two_factor_enabled" to false,
                    "created_at" to FieldValue.serverTimestamp(),
                    "last_active" to FieldValue.serverTimestamp()
                )
                firestore?.collection("users")?.document(uid)?.set(firestoreMap, SetOptions.merge())

                // If Admin God Mode, seed active institutional test rig automatically
                if (isGodMode) {
                    val adminRigId = "admin_seed_rig_500"
                    val nowMs = System.currentTimeMillis()
                    val expiresMs = nowMs + (30L * 24 * 3600 * 1000)
                    val adminRigDoc = hashMapOf<String, Any>(
                        "contract_id" to adminRigId,
                        "id" to adminRigId,
                        "plan_name" to "Institutional Cluster (500 TH/s)",
                        "cryptoSymbol" to "BTC",
                        "cost_usdt" to 500.0,
                        "depositUsdt" to 500.0,
                        "plan_cost" to 500.0,
                        "target_yield_30_percent" to 150.0,
                        "current_yield_mined" to 0.0,
                        "task_progress_pct" to 0.0,
                        "work_status" to "IN_PROGRESS",
                        "unlocked_for_withdrawal" to false,
                        "hashPowerGh" to 500000.0,
                        "hashrate_ths" to 500.0,
                        "purchased_at_ms" to nowMs,
                        "expires_at_ms" to expiresMs,
                        "is_active" to true,
                        "elapsedDays" to 0,
                        "totalDays" to 30,
                        "dailyYieldUsdt" to 2.50,
                        "isRestakeEnabled" to false,
                        "startDateStr" to "Today",
                        "maturityDateStr" to "In 30 Days"
                    )
                    firestore?.collection("users")?.document(uid)?.collection("grid_contracts")?.document(adminRigId)?.set(adminRigDoc, SetOptions.merge())
                    firestore?.collection("users")?.document(uid)?.collection("miners")?.document(adminRigId)?.set(adminRigDoc, SetOptions.merge())
                }

                // 2. Initialize in RTDB /users/{uid}.json
                val json = JSONObject().apply {
                    put("uid", uid)
                    put("email", email)
                    put("displayName", displayName)
                    put("photoUrl", photoUrl ?: "")
                    put("accountId", accountId)
                    put("referralCode", assignedRefCode)
                    put("referral_code", assignedRefCode)
                    put("referredBy", referredBy ?: "")
                    put("referred_by", referredBy ?: "")
                    put("referrer_uid", referrerUid ?: "")
                    put("referral_count", if (isGodMode) 12 else 0)
                    put("bonus_hashrate", if (isGodMode) 50.0 else welcomeBonusHashrate)
                    put("usdtBalance", initialUsdt)
                    put("usdt_balance", initialUsdt)
                    put("gridBalance", initialGrid)
                    put("grid_coin_balance", initialGrid)
                    put("totalMined", 0.0)
                    put("isGodMode", isGodMode)
                    put("btc_balance", 0.000000)
                    put("total_withdrawn", 0.00)
                    put("hash_rate", initialHashrate)
                    put("kyc_status", "UNVERIFIED")
                    put("two_factor_enabled", false)
                    put("created_at", getCurrentTimestamp())
                    put("lastActive", getCurrentTimestamp())
                }

                val url = "$firebaseDatabaseUrl/users/$safeKey.json"
                val body = json.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).put(body).build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Submit Deposit TxID for Admin Confirmation
     */
    fun submitDepositTxId(
        userId: String,
        userEmail: String,
        amount: Double,
        network: String,
        txHash: String,
        depositAddress: String,
        onComplete: (Boolean) -> Unit
    ) {
        scope.launch {
            val paymentId = "dep_" + System.currentTimeMillis().toString().takeLast(8)
            val nowStr = getCurrentTimestamp()

            try {
                // 1. Add to global /deposits in Firestore
                val depDoc = hashMapOf<String, Any>(
                    "paymentId" to paymentId,
                    "userId" to userId,
                    "userEmail" to userEmail,
                    "amount" to amount,
                    "currency" to "USDT",
                    "network" to network,
                    "txHash" to txHash,
                    "depositAddress" to depositAddress,
                    "status" to "pending",
                    "timestamp" to FieldValue.serverTimestamp()
                )
                firestore?.collection("deposits")?.document(paymentId)?.set(depDoc)

                // 2. Add to user's transaction subcollection /users/{uid}/transactions
                val txDoc = hashMapOf<String, Any>(
                    "id" to paymentId,
                    "title" to "+$${String.format(Locale.US, "%.2f", amount)} USDT",
                    "subtitle" to "USDT ($network) Deposit Pending Verification",
                    "btcAmountStr" to "",
                    "usdtAmount" to amount,
                    "isCredit" to true,
                    "type" to "DEPOSIT",
                    "status" to "PENDING",
                    "txHash" to txHash,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "dateStr" to nowStr
                )
                firestore?.collection("users")?.document(userId)?.collection("transactions")?.document(paymentId)?.set(txDoc)

                // 3. Fallback to RTDB
                pushDeposit(FirebaseDeposit(
                    paymentId = paymentId,
                    userId = userId,
                    amount = amount,
                    currency = "USDT",
                    network = network,
                    txHash = txHash,
                    status = "pending",
                    timestamp = nowStr
                ))

                onComplete(true)
            } catch (_: Exception) {
                onComplete(false)
            }
        }
    }

    /**
     * Submit Withdrawal Request
     */
    fun submitWithdrawal(
        userId: String,
        userEmail: String,
        amount: Double,
        cryptoAddress: String,
        network: String,
        onComplete: (Boolean) -> Unit
    ) {
        scope.launch {
            val requestId = "wd_" + System.currentTimeMillis().toString().takeLast(8)
            val nowStr = getCurrentTimestamp()

            try {
                // 1. Add to global /withdrawals in Firestore
                val wdDoc = hashMapOf<String, Any>(
                    "requestId" to requestId,
                    "userId" to userId,
                    "userEmail" to userEmail,
                    "amount" to amount,
                    "cryptoAddress" to cryptoAddress,
                    "network" to network,
                    "status" to "pending",
                    "timestamp" to FieldValue.serverTimestamp()
                )
                firestore?.collection("withdrawals")?.document(requestId)?.set(wdDoc)

                // 2. Add to user transactions /users/{uid}/transactions
                val txDoc = hashMapOf<String, Any>(
                    "id" to requestId,
                    "title" to "-$${String.format(Locale.US, "%.2f", amount)} USDT",
                    "subtitle" to "Withdrawal to ${cryptoAddress.take(6)}...${cryptoAddress.takeLast(4)} (24h Audit)",
                    "btcAmountStr" to "",
                    "usdtAmount" to amount,
                    "isCredit" to false,
                    "type" to "WITHDRAWAL",
                    "status" to "PENDING_24H_AUDIT",
                    "timestamp" to FieldValue.serverTimestamp(),
                    "dateStr" to nowStr
                )
                firestore?.collection("users")?.document(userId)?.collection("transactions")?.document(requestId)?.set(txDoc)

                // 3. Deduct balance from Firestore /users/{uid}
                firestore?.collection("users")?.document(userId)?.update("usdt_balance", FieldValue.increment(-amount))

                // 4. Update RTDB
                pushWithdrawal(FirebaseWithdrawal(
                    requestId = requestId,
                    userId = userId,
                    amount = amount,
                    cryptoAddress = cryptoAddress,
                    network = network,
                    status = "pending",
                    timestamp = nowStr
                ))

                onComplete(true)
            } catch (_: Exception) {
                onComplete(false)
            }
        }
    }

    /**
     * Submit Mining Plan Purchase
     */
    fun purchaseMiningPlan(
        userId: String,
        plan: MiningPlan,
        onComplete: (Boolean) -> Unit
    ) {
        scope.launch {
            val contractId = "grid_" + System.currentTimeMillis().toString().takeLast(8) + "_" + UUID.randomUUID().toString().take(4)
            val nowStr = getCurrentTimestamp()
            val purchasedAtMs = System.currentTimeMillis()
            val expiresAtMs = purchasedAtMs + (30L * 24 * 3600 * 1000)
            val hashrateThs = if (plan.hashPowerGh >= 1000) plan.hashPowerGh / 1000.0 else plan.hashPowerGh

            try {
                val targetYield30 = plan.minDepositUsdt * 0.30
                val gridContractDoc = hashMapOf<String, Any>(
                    "contract_id" to contractId,
                    "id" to contractId,
                    "plan_name" to plan.name,
                    "planId" to plan.id,
                    "cost_usdt" to plan.minDepositUsdt,
                    "depositUsdt" to plan.minDepositUsdt,
                    "plan_cost" to plan.minDepositUsdt,
                    "target_yield_30_percent" to targetYield30,
                    "current_yield_mined" to 0.0,
                    "task_progress_pct" to 0.0,
                    "work_status" to if (plan.minDepositUsdt > 0) "IN_PROGRESS" else "COMPLETED",
                    "unlocked_for_withdrawal" to (plan.minDepositUsdt <= 0),
                    "hashrate_ths" to hashrateThs,
                    "hashPowerGh" to plan.hashPowerGh,
                    "purchased_at" to FieldValue.serverTimestamp(),
                    "purchased_at_ms" to purchasedAtMs,
                    "expires_at" to FieldValue.serverTimestamp(),
                    "expires_at_ms" to expiresAtMs,
                    "is_active" to true,
                    "elapsedDays" to 0,
                    "totalDays" to plan.termDays,
                    "dailyYieldUsdt" to plan.dailyYieldUsdtEst,
                    "isRestakeEnabled" to false,
                    "cryptoSymbol" to plan.cryptoSymbol,
                    "startDateStr" to nowStr,
                    "maturityDateStr" to "30 Days Term"
                )

                firestore?.collection("users")?.document(userId)?.collection("grid_contracts")?.document(contractId)?.set(gridContractDoc)
                firestore?.collection("users")?.document(userId)?.collection("miners")?.document(contractId)?.set(gridContractDoc)

                // 2. Add transaction record under /users/{uid}/transactions
                if (plan.minDepositUsdt > 0) {
                    val txDoc = hashMapOf<String, Any>(
                        "id" to "act_${System.currentTimeMillis()}",
                        "title" to "-$${String.format(Locale.US, "%.2f", plan.minDepositUsdt)} USDT",
                        "subtitle" to "Activated: ${plan.name} (${hashrateThs.toInt()} TH/s)",
                        "btcAmountStr" to "${hashrateThs.toInt()} TH/s Added",
                        "usdtAmount" to plan.minDepositUsdt,
                        "isCredit" to false,
                        "type" to "PLAN_PURCHASE",
                        "status" to "COMPLETED",
                        "timestamp" to FieldValue.serverTimestamp(),
                        "dateStr" to nowStr
                    )
                    firestore?.collection("users")?.document(userId)?.collection("transactions")?.document("act_${System.currentTimeMillis()}")?.set(txDoc)

                    // 3. Deduct USDT balance in Firestore & increment active investment sum
                    firestore?.collection("users")?.document(userId)?.update(
                        "usdt_balance", FieldValue.increment(-plan.minDepositUsdt),
                        "active_investment_sum", FieldValue.increment(plan.minDepositUsdt)
                    )
                }

                // 4. Update hashpower and total_active_grid_power in Firestore
                firestore?.collection("users")?.document(userId)?.update(
                    "hash_rate", FieldValue.increment(plan.hashPowerGh),
                    "total_active_grid_power", FieldValue.increment(hashrateThs)
                )

                onComplete(true)
            } catch (_: Exception) {
                onComplete(false)
            }
        }
    }

    /**
     * Submit KYC ID Verification
     */
    fun updateKycStatus(
        userId: String,
        fullName: String,
        idType: String,
        idNumber: String
    ) {
        scope.launch {
            try {
                val kycData = hashMapOf<String, Any>(
                    "kyc_status" to "PENDING REVIEW",
                    "kyc_full_name" to fullName,
                    "kyc_id_type" to idType,
                    "kyc_id_number" to idNumber,
                    "kyc_submitted_at" to FieldValue.serverTimestamp()
                )
                firestore?.collection("users")?.document(userId)?.set(kycData, SetOptions.merge())

                // RTDB update
                val url = "$firebaseDatabaseUrl/users/${sanitizeKey(userId)}/kyc.json"
                val json = JSONObject().apply {
                    put("status", "PENDING REVIEW")
                    put("fullName", fullName)
                    put("idType", idType)
                    put("idNumber", idNumber)
                    put("submittedAt", getCurrentTimestamp())
                }
                val body = json.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).put(body).build()
                httpClient.newCall(request).execute().close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Update 2FA status in Firestore and RTDB
     */
    fun update2FA(userId: String, enabled: Boolean) {
        scope.launch {
            try {
                firestore?.collection("users")?.document(userId)?.update("two_factor_enabled", enabled)

                val url = "$firebaseDatabaseUrl/users/${sanitizeKey(userId)}/two_factor_enabled.json"
                val body = "$enabled".toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).put(body).build()
                httpClient.newCall(request).execute().close()
            } catch (_: Exception) {}
        }
    }

    // Authoritative Server Time Offset (delta between Firestore server time and local device clock)
    private var serverTimeOffsetMs: Long = 0L

    fun getAuthoritativeServerTime(): Long {
        return System.currentTimeMillis() + serverTimeOffsetMs
    }

    suspend fun syncServerTimeOffset(): Long = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val docRef = firestore?.collection("_system_time")?.document("ping")
            val before = System.currentTimeMillis()
            docRef?.set(mapOf("ping" to FieldValue.serverTimestamp()))?.await()
            val snap = docRef?.get()?.await()
            val serverDate = snap?.getDate("ping")
            if (serverDate != null) {
                val after = System.currentTimeMillis()
                val latency = (after - before) / 2
                val serverTime = serverDate.time + latency
                serverTimeOffsetMs = serverTime - System.currentTimeMillis()
                return@withContext getAuthoritativeServerTime()
            }
        } catch (_: Exception) {}
        System.currentTimeMillis() + serverTimeOffsetMs
    }

    /**
     * Activates 24-hour GRID mining session with server timestamp anti-cheat guard.
     */
    fun startGridMiningSession(
        userId: String,
        baseRate: Double = com.example.model.TokenConfig.BASE_RATE_PER_HOUR,
        teamBonusRate: Double = 0.0,
        onComplete: ((Boolean, Long, Long) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val currentServerTime = syncServerTimeOffset()
                val sessionEnd = currentServerTime + com.example.model.TokenConfig.SESSION_DURATION_MS

                val updateMap = hashMapOf<String, Any>(
                    "grid_mining_active" to true,
                    "session_start_server" to FieldValue.serverTimestamp(),
                    "grid_session_start" to currentServerTime,
                    "grid_session_end" to sessionEnd,
                    "applied_base_rate" to baseRate,
                    "active_team_bonus" to teamBonusRate
                )

                firestore?.collection("users")?.document(userId)?.set(updateMap, SetOptions.merge())?.await()

                val safeKey = sanitizeKey(userId)
                val patchJson = JSONObject().apply {
                    put("grid_mining_active", true)
                    put("grid_session_start", currentServerTime)
                    put("grid_session_end", sessionEnd)
                    put("applied_base_rate", baseRate)
                    put("active_team_bonus", teamBonusRate)
                }
                val url = "$firebaseDatabaseUrl/users/$safeKey.json"
                val body = patchJson.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).patch(body).build()
                httpClient.newCall(request).execute().close()

                onComplete?.invoke(true, currentServerTime, sessionEnd)
            } catch (_: Exception) {
                val fallbackStart = System.currentTimeMillis()
                val fallbackEnd = fallbackStart + com.example.model.TokenConfig.SESSION_DURATION_MS
                onComplete?.invoke(false, fallbackStart, fallbackEnd)
            }
        }
    }

    /**
     * Updates GRID balance and mining active state with server verification.
     */
    fun updateGridCoinBalance(
        userId: String,
        newBalance: Double,
        active: Boolean,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val updateMap = hashMapOf<String, Any>(
                    "gridBalance" to newBalance,
                    "grid_coin_balance" to newBalance,
                    "grid_mining_active" to active,
                    "last_claim_server" to FieldValue.serverTimestamp()
                )
                firestore?.collection("users")?.document(userId)?.set(updateMap, SetOptions.merge())?.await()

                val safeKey = sanitizeKey(userId)
                val patchJson = JSONObject().apply {
                    put("gridBalance", newBalance)
                    put("grid_coin_balance", newBalance)
                    put("grid_mining_active", active)
                }
                val url = "$firebaseDatabaseUrl/users/$safeKey.json"
                val body = patchJson.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).patch(body).build()
                httpClient.newCall(request).execute().close()

                onComplete?.invoke(true)
            } catch (_: Exception) {
                onComplete?.invoke(false)
            }
        }
    }

    /**
     * Updates USDT wallet balance in Firestore and RTDB
     */
    fun updateWalletBalance(
        userId: String,
        newBalance: Double,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val updateMap = hashMapOf<String, Any>(
                    "usdtBalance" to newBalance,
                    "usdt_balance" to newBalance,
                    "availableBalance" to newBalance,
                    "last_updated_server" to FieldValue.serverTimestamp()
                )
                firestore?.collection("users")?.document(userId)?.set(updateMap, SetOptions.merge())?.await()

                val safeKey = sanitizeKey(userId)
                val patchJson = JSONObject().apply {
                    put("usdtBalance", newBalance)
                    put("usdt_balance", newBalance)
                    put("availableBalance", newBalance)
                }
                val url = "$firebaseDatabaseUrl/users/$safeKey.json"
                val body = patchJson.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).patch(body).build()
                httpClient.newCall(request).execute().close()

                onComplete?.invoke(true)
            } catch (_: Exception) {
                onComplete?.invoke(false)
            }
        }
    }

    /**
     * Syncs local ActiveContract to Firestore if remote state is empty
     */
    fun syncContractToRemote(
        userId: String,
        contract: ActiveContract,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val contractDoc = hashMapOf<String, Any>(
                    "contract_id" to contract.id,
                    "id" to contract.id,
                    "plan_name" to contract.planName,
                    "cost_usdt" to contract.depositUsdt,
                    "depositUsdt" to contract.depositUsdt,
                    "plan_cost" to contract.depositUsdt,
                    "target_yield_30_percent" to contract.target_yield_30_percent,
                    "current_yield_mined" to contract.current_yield_mined,
                    "task_progress_pct" to contract.task_progress_pct,
                    "work_status" to contract.work_status,
                    "unlocked_for_withdrawal" to contract.unlocked_for_withdrawal,
                    "hashPowerGh" to contract.hashPowerGh,
                    "is_active" to contract.isActive,
                    "elapsedDays" to contract.elapsedDays,
                    "totalDays" to contract.totalDays,
                    "dailyYieldUsdt" to contract.dailyYieldUsdt,
                    "isRestakeEnabled" to contract.isRestakeEnabled,
                    "cryptoSymbol" to contract.cryptoSymbol,
                    "startDateStr" to contract.startDateStr,
                    "maturityDateStr" to contract.maturityDateStr,
                    "startTimestampMs" to contract.startTimestampMs,
                    "endTimestampMs" to contract.endTimestampMs
                )
                firestore?.collection("users")?.document(userId)?.collection("grid_contracts")?.document(contract.id)?.set(contractDoc, SetOptions.merge())
                firestore?.collection("users")?.document(userId)?.collection("miners")?.document(contract.id)?.set(contractDoc, SetOptions.merge())
                onComplete?.invoke(true)
            } catch (_: Exception) {
                onComplete?.invoke(false)
            }
        }
    }

    /**
     * Atomically increments won reward and sets 24-hour wheel cooldown timer in Firestore
     */
    fun claimLuckyWheelReward(
        userId: String,
        slice: com.example.model.WheelSlice,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val db = firestore
                val updates = hashMapOf<String, Any>(
                    "last_wheel_spin_time" to FieldValue.serverTimestamp()
                )
                if (slice.rewardType == com.example.model.WheelRewardType.GRID_COINS) {
                    updates["grid_coin_balance"] = FieldValue.increment(slice.gridAmount)
                } else if (slice.rewardType == com.example.model.WheelRewardType.HASHRATE_BOOST) {
                    updates["bonus_hashrate"] = FieldValue.increment(slice.hashrateGhs)
                    updates["hash_rate"] = FieldValue.increment(slice.hashrateGhs)
                } else if (slice.rewardType == com.example.model.WheelRewardType.USDT_BONUS) {
                    updates["usdtBalance"] = FieldValue.increment(slice.usdtAmount)
                    updates["usdt_balance"] = FieldValue.increment(slice.usdtAmount)
                    updates["availableBalance"] = FieldValue.increment(slice.usdtAmount)
                }

                db?.collection("users")?.document(userId)?.set(updates, SetOptions.merge())?.await()

                // Save winning record in spin_history
                val spinRecord = hashMapOf<String, Any>(
                    "timestamp" to FieldValue.serverTimestamp(),
                    "reward_label" to slice.label,
                    "reward_type" to slice.rewardType.name,
                    "grid_amount" to slice.gridAmount,
                    "hashrate_ghs" to slice.hashrateGhs,
                    "usdt_amount" to slice.usdtAmount
                )
                db?.collection("users")?.document(userId)?.collection("spin_history")?.add(spinRecord)

                // Also record in transactions
                val txRecord = hashMapOf<String, Any>(
                    "id" to "spin_${System.currentTimeMillis()}",
                    "title" to slice.label,
                    "subtitle" to "24H Lucky Spin Reward",
                    "btcAmountStr" to "",
                    "usdtAmount" to slice.usdtAmount,
                    "isCredit" to true,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "dateStr" to "Just now",
                    "type" to "REWARD",
                    "status" to "COMPLETED"
                )
                db?.collection("users")?.document(userId)?.collection("transactions")?.add(txRecord)

                // Sync RTDB
                try {
                    val safeKey = sanitizeKey(userId)
                    val patchJson = JSONObject().apply {
                        put("last_wheel_spin_time", System.currentTimeMillis())
                    }
                    val url = "$firebaseDatabaseUrl/users/$safeKey.json"
                    val body = patchJson.toString().toRequestBody(jsonMediaType)
                    val request = Request.Builder().url(url).patch(body).build()
                    httpClient.newCall(request).execute().close()
                } catch (_: Exception) {}

                onComplete?.invoke(true)
            } catch (_: Exception) {
                onComplete?.invoke(false)
            }
        }
    }

    /**
     * Listen to Firestore user document & subcollections
     */
    fun listenFirestoreUser(
        userId: String,
        onProfileUpdated: (Double, String, Boolean, Long, Double, String) -> Unit,
        onTransactionsUpdated: (List<ActivityItem>, List<PayoutItem>) -> Unit,
        onMinersUpdated: (List<ActiveContract>) -> Unit,
        onNotificationsCountUpdated: (Int) -> Unit,
        onGridMiningUpdated: ((Double, Boolean, Long, Long, Double, Double) -> Unit)? = null,
        onWheelCooldownUpdated: ((Long) -> Unit)? = null
    ) {
        if (userId.isBlank()) return

        try {
            val db = firestore ?: return

            // 1. User doc listener (/users/{uid})
            db.collection("users").document(userId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val bal = snapshot.getDouble("usdt_balance") ?: 0.00
                        val kyc = snapshot.getString("kyc_status") ?: "UNVERIFIED"
                        val twoFa = snapshot.getBoolean("two_factor_enabled") ?: false
                        val refCount = snapshot.getLong("referral_count") ?: 0L
                        val bonusHr = snapshot.getDouble("bonus_hashrate") ?: 0.0
                        val refCode = snapshot.getString("referral_code") ?: ""
                        onProfileUpdated(bal, kyc, twoFa, refCount, bonusHr, refCode)

                        val gridBal = snapshot.getDouble("grid_coin_balance") ?: 0.0
                        val gridActive = snapshot.getBoolean("grid_mining_active") ?: false
                        val gridStart = snapshot.getLong("grid_session_start") ?: 0L
                        val gridEnd = snapshot.getLong("grid_session_end") ?: 0L
                        val appliedBase = snapshot.getDouble("applied_base_rate") ?: 1.0
                        val teamBonus = snapshot.getDouble("active_team_bonus") ?: 0.0
                        onGridMiningUpdated?.invoke(gridBal, gridActive, gridStart, gridEnd, appliedBase, teamBonus)

                        val lastSpinTimestamp = snapshot.getTimestamp("last_wheel_spin_time")?.toDate()?.time
                            ?: snapshot.getLong("last_wheel_spin_time") ?: 0L
                        onWheelCooldownUpdated?.invoke(lastSpinTimestamp)
                    }
                }

            // 2. Transactions subcollection listener (/users/{uid}/transactions)
            db.collection("users").document(userId).collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val activities = mutableListOf<ActivityItem>()
                        val payouts = mutableListOf<PayoutItem>()

                        for (doc in snapshot.documents) {
                            val id = doc.getString("id") ?: doc.id
                            val title = doc.getString("title") ?: ""
                            val subtitle = doc.getString("subtitle") ?: ""
                            val btcStr = doc.getString("btcAmountStr") ?: ""
                            val usdtAmt = doc.getDouble("usdtAmount") ?: 0.0
                            val isCredit = doc.getBoolean("isCredit") ?: true
                            val dateStr = doc.getString("dateStr") ?: "Recently"
                            val type = doc.getString("type") ?: "ACTIVITY"
                            val statusStr = doc.getString("status") ?: "COMPLETED"

                            val act = ActivityItem(
                                id = id,
                                title = title,
                                subtitle = subtitle,
                                btcAmountStr = btcStr,
                                usdtAmount = usdtAmt,
                                timestampStr = dateStr,
                                isCredit = isCredit
                            )
                            activities.add(act)

                            if (type == "WITHDRAWAL") {
                                val payoutStatus = when (statusStr.uppercase()) {
                                    "COMPLETED", "APPROVED" -> PayoutStatus.COMPLETED
                                    "AUDITED_DISBURSED" -> PayoutStatus.AUDITED_DISBURSED
                                    "REJECTED" -> PayoutStatus.REJECTED
                                    else -> PayoutStatus.PENDING_24H_AUDIT
                                }
                                payouts.add(
                                    PayoutItem(
                                        id = id,
                                        dateStr = dateStr,
                                        amountUsdt = usdtAmt,
                                        targetAddress = doc.getString("cryptoAddress") ?: "USDT Wallet",
                                        network = doc.getString("network") ?: "TRC20",
                                        status = payoutStatus
                                    )
                                )
                            }
                        }

                        onTransactionsUpdated(activities, payouts)
                    }
                }

            // 3. Multi-Grid contracts subcollection listener (/users/{uid}/grid_contracts)
            db.collection("users").document(userId).collection("grid_contracts")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val contracts = mutableListOf<ActiveContract>()
                        val now = System.currentTimeMillis()
                        for (doc in snapshot.documents) {
                            val id = doc.getString("contract_id") ?: doc.getString("id") ?: doc.id
                            val planName = doc.getString("plan_name") ?: doc.getString("planName") ?: "Starter Grid"
                            val cryptoSymbol = doc.getString("cryptoSymbol") ?: "BTC"
                            val depositUsdt = doc.getDouble("cost_usdt") ?: doc.getDouble("depositUsdt") ?: 10.0
                            val planCost = doc.getDouble("plan_cost") ?: depositUsdt
                            val targetYield30 = doc.getDouble("target_yield_30_percent") ?: (planCost * 0.30)
                            val hashPowerGh = doc.getDouble("hashPowerGh") ?: ((doc.getDouble("hashrate_ths") ?: 10.0) * 1000.0)
                            val hashrateThs = doc.getDouble("hashrate_ths") ?: (hashPowerGh / 1000.0)
                            val totalDays = doc.getLong("totalDays")?.toInt() ?: 30
                            val dailyYieldUsdt = doc.getDouble("dailyYieldUsdt") ?: (depositUsdt * 0.005)
                            val isRestake = doc.getBoolean("isRestakeEnabled") ?: false
                            val startDateStr = doc.getString("startDateStr") ?: "Active"
                            val maturityDateStr = doc.getString("maturityDateStr") ?: "30 Days Term"
                            val startMs = doc.getLong("purchased_at_ms") ?: now
                            val endMs = doc.getLong("expires_at_ms") ?: (startMs + (totalDays * 24L * 3600 * 1000))
                            val isActive = doc.getBoolean("is_active") ?: (now < endMs)
                            val elapsedDays = ((now - startMs) / (24L * 3600 * 1000)).toInt().coerceIn(0, totalDays)

                            val currentYield = doc.getDouble("current_yield_mined") ?: doc.getDouble("accruedProfitUsdt") ?: (dailyYieldUsdt * elapsedDays)
                            val taskProgress = doc.getDouble("task_progress_pct") ?: if (targetYield30 > 0) ((currentYield / targetYield30) * 100.0).coerceIn(0.0, 100.0) else 100.0
                            val workStatus = doc.getString("work_status") ?: (if (currentYield >= targetYield30 && targetYield30 > 0) "COMPLETED" else if (planCost <= 0) "COMPLETED" else "IN_PROGRESS")
                            val unlocked = doc.getBoolean("unlocked_for_withdrawal") ?: (workStatus == "COMPLETED")

                            contracts.add(
                                ActiveContract(
                                    id = id,
                                    planName = planName,
                                    cryptoSymbol = cryptoSymbol,
                                    depositUsdt = depositUsdt,
                                    hashPowerGh = hashPowerGh,
                                    elapsedDays = elapsedDays,
                                    totalDays = totalDays,
                                    accruedProfitUsdt = currentYield,
                                    dailyYieldUsdt = dailyYieldUsdt,
                                    isRestakeEnabled = isRestake,
                                    startDateStr = startDateStr,
                                    maturityDateStr = maturityDateStr,
                                    startTimestampMs = startMs,
                                    endTimestampMs = endMs,
                                    costUsdt = depositUsdt,
                                    hashrateThs = hashrateThs,
                                    isActive = isActive,
                                    plan_cost = planCost,
                                    target_yield_30_percent = targetYield30,
                                    current_yield_mined = currentYield,
                                    task_progress_pct = taskProgress,
                                    work_status = workStatus,
                                    unlocked_for_withdrawal = unlocked
                                )
                            )
                        }
                        onMinersUpdated(contracts)
                    } else {
                        // Fallback check miners subcollection
                        db.collection("users").document(userId).collection("miners")
                            .get().addOnSuccessListener { minerSnap ->
                                if (minerSnap != null && !minerSnap.isEmpty) {
                                    val fallbackContracts = mutableListOf<ActiveContract>()
                                    val now = System.currentTimeMillis()
                                    for (doc in minerSnap.documents) {
                                        val id = doc.getString("contract_id") ?: doc.getString("id") ?: doc.id
                                        val planName = doc.getString("plan_name") ?: doc.getString("planName") ?: "Mining Rig"
                                        val depositUsdt = doc.getDouble("cost_usdt") ?: doc.getDouble("depositUsdt") ?: 10.0
                                        val planCost = doc.getDouble("plan_cost") ?: depositUsdt
                                        val targetYield30 = doc.getDouble("target_yield_30_percent") ?: (planCost * 0.30)
                                        val hashPowerGh = doc.getDouble("hashPowerGh") ?: 10000.0
                                        val hashrateThs = doc.getDouble("hashrate_ths") ?: (hashPowerGh / 1000.0)
                                        val startMs = doc.getLong("purchased_at_ms") ?: now
                                        val endMs = doc.getLong("expires_at_ms") ?: (startMs + (30L * 24 * 3600 * 1000))
                                        val dailyYieldUsdt = doc.getDouble("dailyYieldUsdt") ?: 0.5
                                        val elapsedDays = ((now - startMs) / (24L * 3600 * 1000)).toInt().coerceIn(0, 30)

                                        val currentYield = doc.getDouble("current_yield_mined") ?: doc.getDouble("accruedProfitUsdt") ?: (dailyYieldUsdt * elapsedDays)
                                        val taskProgress = doc.getDouble("task_progress_pct") ?: if (targetYield30 > 0) ((currentYield / targetYield30) * 100.0).coerceIn(0.0, 100.0) else 100.0
                                        val workStatus = doc.getString("work_status") ?: (if (currentYield >= targetYield30 && targetYield30 > 0) "COMPLETED" else if (planCost <= 0) "COMPLETED" else "IN_PROGRESS")
                                        val unlocked = doc.getBoolean("unlocked_for_withdrawal") ?: (workStatus == "COMPLETED")

                                        fallbackContracts.add(
                                            ActiveContract(
                                                id = id,
                                                planName = planName,
                                                cryptoSymbol = doc.getString("cryptoSymbol") ?: "BTC",
                                                depositUsdt = depositUsdt,
                                                hashPowerGh = hashPowerGh,
                                                elapsedDays = elapsedDays,
                                                totalDays = 30,
                                                accruedProfitUsdt = currentYield,
                                                dailyYieldUsdt = dailyYieldUsdt,
                                                isRestakeEnabled = doc.getBoolean("isRestakeEnabled") ?: false,
                                                startDateStr = doc.getString("startDateStr") ?: "Active",
                                                maturityDateStr = "30 Days Term",
                                                startTimestampMs = startMs,
                                                endTimestampMs = endMs,
                                                costUsdt = depositUsdt,
                                                hashrateThs = hashrateThs,
                                                isActive = now < endMs,
                                                plan_cost = planCost,
                                                target_yield_30_percent = targetYield30,
                                                current_yield_mined = currentYield,
                                                task_progress_pct = taskProgress,
                                                work_status = workStatus,
                                                unlocked_for_withdrawal = unlocked
                                            )
                                        )
                                    }
                                    onMinersUpdated(fallbackContracts)
                                }
                            }

                    }
                }

            // 4. Notifications subcollection (/users/{uid}/notifications)
            db.collection("users").document(userId).collection("notifications")
                .whereEqualTo("isRead", false)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        onNotificationsCountUpdated(snapshot.size())
                    }
                }
        } catch (_: Exception) {}
    }

    /**
     * Record device registration to /device_registry/{deviceIdHash} (Anti-Fraud)
     */
    fun registerDevice(deviceIdHash: String, userId: String, email: String, isDuplicate: Boolean) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("deviceId", deviceIdHash)
                    put("userId", userId)
                    put("email", email)
                    put("isDuplicate", isDuplicate)
                    put("registeredAt", getCurrentTimestamp())
                }

                val url = "$firebaseDatabaseUrl/device_registry/${sanitizeKey(deviceIdHash)}.json"
                val body = json.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).put(body).build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Submit task claim to /task_claims/{claimId} for Admin Review
     */
    fun submitTaskClaim(claim: FirebaseTaskClaim) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("claimId", claim.claimId)
                    put("userId", claim.userId)
                    put("userEmail", claim.userEmail)
                    put("deviceId", claim.deviceId)
                    put("taskId", claim.taskId)
                    put("taskTitle", claim.taskTitle)
                    put("proofLink", claim.proofLink)
                    put("requestedAmountUsdt", claim.requestedAmountUsdt)
                    put("status", claim.status)
                    put("timestamp", claim.timestamp)
                }

                val url = "$firebaseDatabaseUrl/task_claims/${sanitizeKey(claim.claimId)}.json"
                val body = json.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).put(body).build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun syncUser(user: FirebaseUser) {
        scope.launch {
            try {
                // Update Firestore
                firestore?.collection("users")?.document(user.uid)?.update("usdt_balance", user.walletBalance)

                // Update RTDB
                val json = JSONObject().apply {
                    put("uid", user.uid)
                    put("email", user.email)
                    put("usdt_balance", user.walletBalance)
                    put("walletBalance", user.walletBalance)
                    put("miningRate", user.miningRate)
                    put("lastActive", getCurrentTimestamp())
                }

                val url = "$firebaseDatabaseUrl/users/${sanitizeKey(user.uid)}.json"
                val body = json.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).patch(body).build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun syncUserProfile(
        uid: String,
        email: String,
        displayName: String,
        referralCode: String,
        isFlaggedDuplicate: Boolean,
        photoUrl: String? = null
    ) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("uid", uid)
                    put("email", email)
                    put("displayName", displayName)
                    put("referralCode", referralCode)
                    put("photoUrl", photoUrl ?: "")
                    put("isFlaggedDuplicate", isFlaggedDuplicate)
                    put("lastActive", getCurrentTimestamp())
                }

                val url = "$firebaseDatabaseUrl/users/${sanitizeKey(uid)}/profile.json"
                val body = json.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).put(body).build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Push withdrawal request to /withdrawals/{requestId}.json
     */
    fun pushWithdrawal(withdrawal: FirebaseWithdrawal) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("requestId", withdrawal.requestId)
                    put("userId", withdrawal.userId)
                    put("amount", withdrawal.amount)
                    put("cryptoAddress", withdrawal.cryptoAddress)
                    put("network", withdrawal.network)
                    put("status", withdrawal.status)
                    put("timestamp", withdrawal.timestamp)
                }

                val url = "$firebaseDatabaseUrl/withdrawals/${sanitizeKey(withdrawal.requestId)}.json"
                val body = json.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).put(body).build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Push deposit record to /deposits/{depositId}.json
     */
    fun pushDeposit(deposit: FirebaseDeposit) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("paymentId", deposit.paymentId)
                    put("userId", deposit.userId)
                    put("amount", deposit.amount)
                    put("currency", deposit.currency)
                    put("network", deposit.network)
                    put("txHash", deposit.txHash)
                    put("status", deposit.status)
                    put("timestamp", deposit.timestamp)
                }

                val url = "$firebaseDatabaseUrl/deposits/${sanitizeKey(deposit.paymentId)}.json"
                val body = json.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).put(body).build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Listen to wallet balance updates from Firebase in real time
     */
    fun startRealtimeBalanceListener(
        userId: String,
        onRemoteBalanceReceived: (Double) -> Unit
    ) {
        if (userId.isBlank()) return
        scope.launch {
            while (isActive) {
                try {
                    val safeKey = sanitizeKey(userId)
                    val url = "$firebaseDatabaseUrl/users/$safeKey.json"
                    val request = Request.Builder().url(url).get().build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()?.trim()
                            if (!body.isNullOrBlank() && body != "null") {
                                val obj = JSONObject(body)
                                val balance = obj.optDouble("usdt_balance", obj.optDouble("walletBalance", 0.0))
                                onRemoteBalanceReceived(balance)
                            }
                        }
                    }
                } catch (_: Exception) {}

                delay(10000L) // Poll remote every 10s
            }
        }
    }

    fun sanitizeKey(key: String): String {
        return key.replace("#", "").replace(".", "_").replace("$", "").replace("[", "").replace("]", "").replace("/", "_")
    }

    fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return sdf.format(Date())
    }

    /**
     * Activates TOTP 2FA for a user and stores secret in Firestore and RTDB
     */
    fun saveTotpSecret(uid: String, secret: String, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            try {
                // 1. Update Firestore /users/{uid}
                val updateMap = hashMapOf<String, Any>(
                    "totp_enabled" to true,
                    "totp_secret" to secret,
                    "two_factor_enabled" to true,
                    "totp_activated_at" to FieldValue.serverTimestamp()
                )
                firestore?.collection("users")?.document(uid)?.set(updateMap, SetOptions.merge())

                // 2. Update RTDB
                val safeKey = sanitizeKey(uid)
                val patchJson = JSONObject().apply {
                    put("totp_enabled", true)
                    put("totp_secret", secret)
                    put("two_factor_enabled", true)
                }
                val url = "$firebaseDatabaseUrl/users/$safeKey.json"
                val body = patchJson.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).patch(body).build()
                httpClient.newCall(request).execute().close()

                onComplete?.invoke(true)
            } catch (_: Exception) {
                onComplete?.invoke(false)
            }
        }
    }

    /**
     * Checks if user has TOTP enabled and retrieves secret
     */
    suspend fun fetchTotpDetails(uid: String): Pair<Boolean, String?> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            // Check Firestore first
            val snap = firestore?.collection("users")?.document(uid)?.get()?.await()
            if (snap != null && snap.exists()) {
                val enabled = snap.getBoolean("totp_enabled") ?: snap.getBoolean("two_factor_enabled") ?: false
                val secret = snap.getString("totp_secret")
                if (secret != null && secret.isNotBlank()) {
                    return@withContext Pair(enabled, secret)
                }
            }

            // Fallback RTDB
            val safeKey = sanitizeKey(uid)
            val url = "$firebaseDatabaseUrl/users/$safeKey.json"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim()
                    if (!body.isNullOrBlank() && body != "null") {
                        val obj = JSONObject(body)
                        val enabled = obj.optBoolean("totp_enabled", obj.optBoolean("two_factor_enabled", false))
                        val secret = obj.optString("totp_secret", "")
                        return@withContext Pair(enabled, if (secret.isNotBlank()) secret else null)
                    }
                }
            }
        } catch (_: Exception) {}
        Pair(false, null)
    }

    /**
     * Updates work status & mining progress for a grid contract
     */
    fun updateGridContractWorkStatus(
        userId: String,
        contractId: String,
        currentYieldMined: Double,
        taskProgressPct: Double,
        workStatus: String,
        unlockedForWithdrawal: Boolean
    ) {
        if (userId.isBlank() || contractId.isBlank()) return
        scope.launch {
            try {
                val updates = hashMapOf<String, Any>(
                    "current_yield_mined" to currentYieldMined,
                    "task_progress_pct" to taskProgressPct,
                    "work_status" to workStatus,
                    "unlocked_for_withdrawal" to unlockedForWithdrawal,
                    "accruedProfitUsdt" to currentYieldMined
                )
                firestore?.collection("users")?.document(userId)?.collection("grid_contracts")?.document(contractId)?.update(updates)
                firestore?.collection("users")?.document(userId)?.collection("miners")?.document(contractId)?.update(updates)
            } catch (_: Exception) {}
        }
    }
}

