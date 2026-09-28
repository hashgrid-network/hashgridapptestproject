package com.example.service

import com.example.model.ActiveContract
import com.example.model.ActivityItem
import com.example.model.MiningPlan
import com.example.model.OFFICIAL_BEP20_ADDRESS
import com.example.model.OFFICIAL_TRC20_ADDRESS
import com.example.model.PayoutItem
import com.example.model.PayoutStatus
import com.example.model.TeamMember
import com.example.model.safeComputeTaskProgress
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
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
    val uid: String = "",
    val email: String = "",
    val walletBalance: Double = 0.0,
    val miningRate: String = "0.0",
    val createdAt: String = ""
)

data class FirebaseWithdrawal(
    val requestId: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val cryptoAddress: String = "",
    val network: String = "TRC20",
    val status: String = "pending",
    val timestamp: String = ""
)

data class FirebaseDeposit(
    val paymentId: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val currency: String = "USDT",
    val network: String = "TRC20",
    val txHash: String = "",
    val status: String = "pending",
    val timestamp: String = ""
)

data class FirebaseTaskClaim(
    val claimId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val deviceId: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val proofLink: String = "",
    val requestedAmountUsdt: Double = 0.0,
    val status: String = "PENDING",
    val timestamp: String = ""
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

// Ultra-Defensive DocumentSnapshot Extension Functions
fun DocumentSnapshot.getSafeDouble(field: String, default: Double = 0.0): Double {
    return try {
        when (val v = this.get(field)) {
            is Number -> if (v.toDouble().isNaN() || v.toDouble().isInfinite()) default else v.toDouble()
            is String -> v.toDoubleOrNull()?.let { if (it.isNaN() || it.isInfinite()) default else it } ?: default
            is Boolean -> if (v) 1.0 else 0.0
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

fun DocumentSnapshot.getSafeLong(field: String, default: Long = 0L): Long {
    return try {
        when (val v = this.get(field)) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: v.toDoubleOrNull()?.toLong() ?: default
            is Boolean -> if (v) 1L else 0L
            is Timestamp -> v.toDate().time
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

fun DocumentSnapshot.getSafeBoolean(field: String, default: Boolean = false): Boolean {
    return try {
        when (val v = this.get(field)) {
            is Boolean -> v
            is String -> v.lowercase().toBooleanStrictOrNull() ?: default
            is Number -> v.toInt() != 0
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

fun DocumentSnapshot.getSafeString(field: String, default: String = ""): String {
    return try {
        when (val v = this.get(field)) {
            is String -> v
            null -> default
            else -> v.toString()
        }
    } catch (_: Exception) {
        default
    }
}

fun DocumentSnapshot.getSafeTimestampMs(field: String, default: Long = System.currentTimeMillis()): Long {
    return try {
        when (val v = this.get(field)) {
            is Timestamp -> v.toDate().time
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: default
            is Date -> v.time
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

object FirebaseSyncService {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

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
                            val trc = doc.getSafeString("trc20_address")
                            if (trc.isNotBlank()) trc20DepositAddress = trc
                            val bep = doc.getSafeString("bep20_address")
                            if (bep.isNotBlank()) bep20DepositAddress = bep
                        }
                    }
            } catch (_: Exception) {}
        }
    }

    suspend fun validateAndApplyReferral(
        cleanCode: String,
        newUid: String,
        newDisplayName: String = "New Miner",
        newEmail: String = ""
    ): Pair<Boolean, String?> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (cleanCode.isBlank() || cleanCode == "HG-8080") {
            return@withContext Pair(cleanCode == "HG-8080", if (cleanCode == "HG-8080") "master_8080_uid" else null)
        }
        val db = firestore ?: return@withContext Pair(false, null)

        try {
            var querySnap = db.collection("users")
                .whereEqualTo("referralCode", cleanCode)
                .limit(1)
                .get()
                .await()

            if (querySnap.isEmpty) {
                querySnap = db.collection("users")
                    .whereEqualTo("referral_code", cleanCode)
                    .limit(1)
                    .get()
                    .await()
            }

            if (!querySnap.isEmpty) {
                val referrerDoc = querySnap.documents[0]
                val referrerUid = referrerDoc.getSafeString("uid").ifBlank { referrerDoc.id }

                if (referrerUid.isNotBlank() && referrerUid != newUid && referrerDoc.id != newUid) {
                    try {
                        val currentCount = referrerDoc.getSafeLong("teamCount")
                        val newCount = currentCount + 1
                        val newTier = when {
                            newCount >= 20 -> "ELITE"
                            newCount >= 5 -> "PRO"
                            else -> "NOVICE"
                        }

                        db.collection("users").document(referrerUid).update(
                            "teamCount", FieldValue.increment(1),
                            "directReferrals", FieldValue.increment(1),
                            "referral_count", FieldValue.increment(1),
                            "referralCount", FieldValue.increment(1),
                            "extraHashrate", FieldValue.increment(1.5),
                            "bonus_hashrate", FieldValue.increment(1.5),
                            "hash_rate", FieldValue.increment(1.5),
                            "syndicateTier", newTier,
                            "lastReconciledAt", FieldValue.serverTimestamp(),
                            "last_active", FieldValue.serverTimestamp()
                        ).await()

                        val maskedName = if (newDisplayName.isNotBlank() && !newDisplayName.contains("@")) {
                            newDisplayName
                        } else {
                            "User 0x" + newUid.replace("-", "").take(6).lowercase()
                        }
                        val teamMemberDoc = hashMapOf<String, Any>(
                            "uid" to newUid,
                            "displayName" to maskedName,
                            "joinedAt" to FieldValue.serverTimestamp(),
                            "joinedAtMs" to System.currentTimeMillis(),
                            "status" to "ACTIVE",
                            "hashrateBonus" to 1.5,
                            "hashrateContributed" to 1.5,
                            "isMining" to true,
                            "email" to newEmail
                        )
                        db.collection("users").document(referrerUid)
                            .collection("team").document(newUid)
                            .set(teamMemberDoc, SetOptions.merge()).await()

                    } catch (_: Exception) {}
                    return@withContext Pair(true, referrerUid)
                }
            }
        } catch (_: Exception) {}
        Pair(false, null)
    }

    suspend fun reconcileUserReferrals(currentUid: String, referralCode: String): Long = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (currentUid.isBlank()) return@withContext 0L
        val db = firestore ?: return@withContext 0L
        try {
            val userDoc = db.collection("users").document(currentUid).get().await()
            val userEmail = userDoc.getSafeString("email")
            val isMasterAccount = (currentUid == "master_8080_uid" || userEmail.equals("parkashom8080@gmail.com", ignoreCase = true))

            val existingDocCode = userDoc.getSafeString("referralCode").ifBlank { userDoc.getSafeString("referral_code") }
            val userPersonalCode = when {
                isMasterAccount -> "HG-8080"
                existingDocCode.isNotBlank() && existingDocCode != "HG-8080" -> existingDocCode
                referralCode.isNotBlank() && referralCode != "HG-8080" -> referralCode
                else -> AuthService.generateReferralCode(currentUid)
            }

            val uppercaseCode = userPersonalCode.trim().uppercase()
            val lowercaseCode = userPersonalCode.trim().lowercase()

            val matchedUsers = mutableMapOf<String, DocumentSnapshot>()

            val fieldsToQuery = listOf("referredBy", "referred_by", "referrerUid", "referrer_uid")
            for (field in fieldsToQuery) {
                try {
                    val q = db.collection("users").whereEqualTo(field, currentUid).get().await()
                    for (doc in q.documents) {
                        if (doc.id != currentUid) matchedUsers[doc.id] = doc
                    }
                } catch (_: Exception) {}
            }

            val canQueryCode = uppercaseCode.isNotBlank() && (uppercaseCode != "HG-8080" || isMasterAccount)
            if (canQueryCode) {
                val codeQueries = listOf(uppercaseCode, lowercaseCode)
                for (code in codeQueries) {
                    for (field in listOf("appliedReferralCode", "referredBy", "referred_by")) {
                        try {
                            val q = db.collection("users").whereEqualTo(field, code).get().await()
                            for (doc in q.documents) {
                                if (doc.id != currentUid) matchedUsers[doc.id] = doc
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            val existingSubcollectionIds = mutableSetOf<String>()
            try {
                val teamSnap = db.collection("users").document(currentUid).collection("team").get().await()
                for (tdoc in teamSnap.documents) {
                    existingSubcollectionIds.add(tdoc.id)
                }
            } catch (_: Exception) {}

            val actualCount = maxOf(matchedUsers.size.toLong(), existingSubcollectionIds.size.toLong())
            val currentTeamCount = userDoc.getSafeLong("teamCount")
            val needsCodeFix = !isMasterAccount && (existingDocCode == "HG-8080" || existingDocCode.isBlank())

            if (actualCount > currentTeamCount || needsCodeFix || !userDoc.contains("extraHashrate") || !userDoc.contains("totalReferralRewardsUsdt")) {
                val finalExtraHashrate = actualCount * 1.5
                val tier = when {
                    actualCount >= 20 -> "ELITE"
                    actualCount >= 5 -> "PRO"
                    else -> "NOVICE"
                }
                val existingRewards = userDoc.getSafeDouble("totalReferralRewardsUsdt")
                val updates = hashMapOf<String, Any>(
                    "teamCount" to actualCount,
                    "directReferrals" to actualCount,
                    "referralCount" to actualCount,
                    "referral_count" to actualCount,
                    "extraHashrate" to finalExtraHashrate,
                    "bonus_hashrate" to finalExtraHashrate,
                    "totalReferralRewardsUsdt" to existingRewards,
                    "syndicateTier" to tier,
                    "lastReconciledAt" to FieldValue.serverTimestamp(),
                    "referralCode" to userPersonalCode,
                    "referral_code" to userPersonalCode
                )
                db.collection("users").document(currentUid).set(updates, SetOptions.merge()).await()
            }

            for ((memberUid, memberDoc) in matchedUsers) {
                if (!existingSubcollectionIds.contains(memberUid)) {
                    val rawName = memberDoc.getSafeString("displayName").ifBlank { memberDoc.getSafeString("name") }
                    val displayName = if (rawName.isNotBlank() && !rawName.contains("@")) {
                        rawName
                    } else {
                        "User 0x" + memberUid.replace("-", "").take(6).lowercase()
                    }
                    val email = memberDoc.getSafeString("email")
                    val joinedAtMs = memberDoc.getSafeTimestampMs("created_at", memberDoc.getSafeTimestampMs("joinedAt"))
                    val teamDoc = hashMapOf<String, Any>(
                        "uid" to memberUid,
                        "displayName" to displayName,
                        "email" to email,
                        "joinedAt" to FieldValue.serverTimestamp(),
                        "joinedAtMs" to joinedAtMs,
                        "status" to "ACTIVE",
                        "hashrateBonus" to 1.5,
                        "hashrateContributed" to 1.5,
                        "isMining" to true
                    )
                    db.collection("users").document(currentUid).collection("team").document(memberUid)
                        .set(teamDoc, SetOptions.merge())
                }
            }

            return@withContext actualCount
        } catch (_: Exception) {
            return@withContext 0L
        }
    }

    fun initializeNewUser(
        uid: String,
        email: String,
        displayName: String,
        photoUrl: String?,
        accountId: String,
        referralCode: String = "",
        referredBy: String? = null,
        referrerUid: String? = null,
        appliedReferralCode: String = "",
        welcomeBonusHashrate: Double = 0.0
    ) {
        scope.launch {
            try {
                val safeKey = sanitizeKey(uid)
                val isGodMode = email.equals("parkashom8080@gmail.com", ignoreCase = true)
                val assignedRefCode = if (isGodMode) {
                    "HG-8080"
                } else if (referralCode.isNotBlank() && referralCode != "HG-8080") {
                    referralCode
                } else {
                    AuthService.generateReferralCode(uid)
                }
                val initialUsdt = if (isGodMode) 1000.00 else 0.00
                val initialGrid = if (isGodMode) 50.00 else 0.00
                val initialHashrate = if (isGodMode) 500000.0 else welcomeBonusHashrate
                val initialExtraHashrate = if (isGodMode) 50.0 else welcomeBonusHashrate

                val firestoreMap = hashMapOf<String, Any>(
                    "uid" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "photoUrl" to (photoUrl ?: ""),
                    "accountId" to accountId,
                    "referralCode" to assignedRefCode,
                    "referral_code" to assignedRefCode,
                    "referredBy" to (referredBy ?: (referrerUid ?: "")),
                    "referred_by" to (referredBy ?: (referrerUid ?: "")),
                    "referrer_uid" to (referrerUid ?: ""),
                    "referrerUid" to (referrerUid ?: ""),
                    "appliedReferralCode" to appliedReferralCode,
                    "applied_referral_code" to appliedReferralCode,
                    "teamCount" to (if (isGodMode) 12L else 0L),
                    "directReferrals" to (if (isGodMode) 12L else 0L),
                    "referral_count" to (if (isGodMode) 12L else 0L),
                    "referralCount" to (if (isGodMode) 12L else 0L),
                    "extraHashrate" to initialExtraHashrate,
                    "bonus_hashrate" to initialExtraHashrate,
                    "totalReferralRewardsUsdt" to (if (isGodMode) 250.0 else 0.0),
                    "syndicateTier" to (if (isGodMode) "ELITE" else "NOVICE"),
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
                    "last_active" to FieldValue.serverTimestamp(),
                    "lastReconciledAt" to FieldValue.serverTimestamp()
                )
                firestore?.collection("users")?.document(uid)?.set(firestoreMap, SetOptions.merge())?.await()

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

                firestore?.collection("users")?.document(userId)?.update("usdt_balance", FieldValue.increment(-amount))

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

                    firestore?.collection("users")?.document(userId)?.update(
                        "usdt_balance", FieldValue.increment(-plan.minDepositUsdt),
                        "active_investment_sum", FieldValue.increment(plan.minDepositUsdt)
                    )
                }

                firestore?.collection("users")?.document(userId)?.update(
                    "hash_rate", FieldValue.increment(plan.hashPowerGh),
                    "total_active_grid_power", FieldValue.increment(hashrateThs)
                )

                if (plan.minDepositUsdt > 0) {
                    try {
                        val buyerDoc = firestore?.collection("users")?.document(userId)?.get()?.await()
                        val referrerUid = buyerDoc?.getSafeString("referredBy")
                            ?.takeIf { it.isNotBlank() }
                            ?: buyerDoc?.getSafeString("referrerUid")?.takeIf { it.isNotBlank() }
                            ?: buyerDoc?.getSafeString("referred_by")?.takeIf { it.isNotBlank() }

                        val buyerName = buyerDoc?.getSafeString("displayName")
                            ?.takeIf { it.isNotBlank() && !it.contains("@") }
                            ?: ("User 0x" + userId.replace("-", "").take(6).lowercase())

                        if (!referrerUid.isNullOrBlank() && referrerUid != userId) {
                            val commission = plan.minDepositUsdt * 0.07
                            firestore?.collection("users")?.document(referrerUid)?.update(
                                "totalReferralRewardsUsdt", FieldValue.increment(commission),
                                "usdtBalance", FieldValue.increment(commission),
                                "usdt_balance", FieldValue.increment(commission),
                                "availableBalance", FieldValue.increment(commission),
                                "last_active", FieldValue.serverTimestamp()
                            )

                            val commTxId = "comm_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
                            val commTxDoc = hashMapOf<String, Any>(
                                "id" to commTxId,
                                "title" to "+$${String.format(Locale.US, "%.2f", commission)} USDT",
                                "subtitle" to "7% Rig Purchase Commission from $buyerName",
                                "btcAmountStr" to "+$${String.format(Locale.US, "%.2f", commission)}",
                                "usdtAmount" to commission,
                                "isCredit" to true,
                                "type" to "COMMISSION",
                                "status" to "COMPLETED",
                                "timestamp" to FieldValue.serverTimestamp(),
                                "dateStr" to nowStr
                            )
                            firestore?.collection("users")?.document(referrerUid)?.collection("transactions")?.document(commTxId)?.set(commTxDoc)
                        }
                    } catch (_: Exception) {}
                }

                onComplete(true)
            } catch (_: Exception) {
                onComplete(false)
            }
        }
    }

    fun purchaseRigForWallet(
        walletAddress: String,
        userId: String,
        rig: com.example.model.MiningRig
    ) {
        scope.launch {
            try {
                val db = firestore ?: return@launch
                val maxCap = rig.maxPayoutCap
                val ratePerSec = rig.ratePerSecond
                val rigDocMap = hashMapOf<String, Any>(
                    "rig_id" to rig.rig_id,
                    "id" to rig.rig_id,
                    "model_name" to rig.model_name,
                    "cost_usdt" to rig.cost_usdt,
                    "max_payout_cap" to maxCap,
                    "earned_amount" to rig.earned_amount,
                    "rate_per_second" to ratePerSec,
                    "status" to rig.status,
                    "purchased_at" to FieldValue.serverTimestamp(),
                    "last_synced_at" to System.currentTimeMillis(),
                    "hashrate_ths" to rig.hashrate_ths,
                    "crypto_symbol" to rig.crypto_symbol
                )

                if (walletAddress.isNotBlank()) {
                    db.collection("wallets").document(walletAddress).collection("rigs").document(rig.rig_id).set(rigDocMap, SetOptions.merge())
                }
                if (userId.isNotBlank()) {
                    db.collection("users").document(userId).collection("rigs").document(rig.rig_id).set(rigDocMap, SetOptions.merge())
                    db.collection("users").document(userId).collection("miners").document(rig.rig_id).set(rigDocMap, SetOptions.merge())
                }
            } catch (_: Exception) {}
        }
    }

    fun syncBatchRigs(
        walletAddress: String,
        userId: String,
        updatedRigs: List<com.example.model.MiningRig>
    ) {
        scope.launch {
            try {
                val db = firestore ?: return@launch
                val batch = db.batch()
                for (rig in updatedRigs) {
                    val rigMap = hashMapOf<String, Any>(
                        "earned_amount" to rig.earned_amount,
                        "status" to rig.status,
                        "last_synced_at" to rig.last_synced_at
                    )
                    if (walletAddress.isNotBlank()) {
                        val ref = db.collection("wallets").document(walletAddress).collection("rigs").document(rig.rig_id)
                        batch.set(ref, rigMap, SetOptions.merge())
                    }
                    if (userId.isNotBlank()) {
                        val userRef = db.collection("users").document(userId).collection("miners").document(rig.rig_id)
                        batch.set(userRef, rigMap, SetOptions.merge())
                    }
                }
                batch.commit()
            } catch (_: Exception) {}
        }
    }

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

    fun updateGridCoinBalance(
        userId: String,
        newBalance: Double,
        active: Boolean,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val cleanBal = if (newBalance.isNaN() || newBalance.isInfinite()) 0.0 else newBalance
                val updateMap = hashMapOf<String, Any>(
                    "gridBalance" to cleanBal,
                    "grid_coin_balance" to cleanBal,
                    "grid_mining_active" to active,
                    "last_claim_server" to FieldValue.serverTimestamp()
                )
                firestore?.collection("users")?.document(userId)?.set(updateMap, SetOptions.merge())?.await()

                val safeKey = sanitizeKey(userId)
                val patchJson = JSONObject().apply {
                    put("gridBalance", cleanBal)
                    put("grid_coin_balance", cleanBal)
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

    fun updateWalletBalance(
        userId: String,
        newBalance: Double,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val cleanBal = if (newBalance.isNaN() || newBalance.isInfinite()) 0.0 else newBalance
                val updateMap = hashMapOf<String, Any>(
                    "usdtBalance" to cleanBal,
                    "usdt_balance" to cleanBal,
                    "availableBalance" to cleanBal,
                    "last_updated_server" to FieldValue.serverTimestamp()
                )
                firestore?.collection("users")?.document(userId)?.set(updateMap, SetOptions.merge())?.await()

                val safeKey = sanitizeKey(userId)
                val patchJson = JSONObject().apply {
                    put("usdtBalance", cleanBal)
                    put("usdt_balance", cleanBal)
                    put("availableBalance", cleanBal)
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

                val spinRecord = hashMapOf<String, Any>(
                    "timestamp" to FieldValue.serverTimestamp(),
                    "reward_label" to slice.label,
                    "reward_type" to slice.rewardType.name,
                    "grid_amount" to slice.gridAmount,
                    "hashrate_ghs" to slice.hashrateGhs,
                    "usdt_amount" to slice.usdtAmount
                )
                db?.collection("users")?.document(userId)?.collection("spin_history")?.add(spinRecord)

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

    fun listenFirestoreUser(
        userId: String,
        onProfileUpdated: (Double, String, Boolean, Long, Double, String) -> Unit,
        onTransactionsUpdated: (List<ActivityItem>, List<PayoutItem>) -> Unit,
        onMinersUpdated: (List<ActiveContract>) -> Unit,
        onNotificationsCountUpdated: (Int) -> Unit,
        onGridMiningUpdated: ((Double, Boolean, Long, Long, Double, Double) -> Unit)? = null,
        onWheelCooldownUpdated: ((Long) -> Unit)? = null,
        onTeamUpdated: ((Long, List<TeamMember>) -> Unit)? = null,
        onSyndicateUpdated: ((String, Double) -> Unit)? = null
    ) {
        if (userId.isBlank()) return

        try {
            val db = firestore ?: return

            // 1. User doc listener (/users/{uid})
            db.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    try {
                        if (snapshot != null && snapshot.exists()) {
                            val bal = snapshot.getSafeDouble("usdt_balance", snapshot.getSafeDouble("usdtBalance"))
                            val kyc = snapshot.getSafeString("kyc_status", "UNVERIFIED")
                            val twoFa = snapshot.getSafeBoolean("two_factor_enabled")
                            val teamCount = snapshot.getSafeLong("teamCount",
                                snapshot.getSafeLong("directReferrals",
                                    snapshot.getSafeLong("referral_count",
                                        snapshot.getSafeLong("referralCount", 0L))))
                            val extraHashrate = snapshot.getSafeDouble("extraHashrate", snapshot.getSafeDouble("bonus_hashrate"))
                            val isMaster = (userId == "master_8080_uid" || snapshot.getSafeString("email").equals("parkashom8080@gmail.com", ignoreCase = true))
                            val storedRefCode = snapshot.getSafeString("referralCode").ifBlank { snapshot.getSafeString("referral_code") }
                            val refCode = when {
                                isMaster -> "HG-8080"
                                storedRefCode.isNotBlank() && storedRefCode != "HG-8080" -> storedRefCode
                                else -> AuthService.generateReferralCode(userId)
                            }
                            onProfileUpdated(bal, kyc, twoFa, teamCount, extraHashrate, refCode)

                            val tier = snapshot.getSafeString("syndicateTier",
                                if (teamCount >= 20) "ELITE" else if (teamCount >= 5) "PRO" else "NOVICE")
                            val totalRewards = snapshot.getSafeDouble("totalReferralRewardsUsdt")
                            onSyndicateUpdated?.invoke(tier, totalRewards)

                            val needsCodeFix = !isMaster && (storedRefCode == "HG-8080" || storedRefCode.isBlank())
                            if (!snapshot.contains("teamCount") || !snapshot.contains("extraHashrate") || !snapshot.contains("referralCode") || !snapshot.contains("syndicateTier") || !snapshot.contains("totalReferralRewardsUsdt") || needsCodeFix) {
                                val fixes = hashMapOf<String, Any>()
                                if (!snapshot.contains("teamCount")) fixes["teamCount"] = 0L
                                if (!snapshot.contains("directReferrals")) fixes["directReferrals"] = 0L
                                if (!snapshot.contains("extraHashrate")) fixes["extraHashrate"] = 0.0
                                if (!snapshot.contains("bonus_hashrate")) fixes["bonus_hashrate"] = 0.0
                                if (!snapshot.contains("syndicateTier")) fixes["syndicateTier"] = tier
                                if (!snapshot.contains("totalReferralRewardsUsdt")) fixes["totalReferralRewardsUsdt"] = totalRewards
                                if (!snapshot.contains("lastReconciledAt")) fixes["lastReconciledAt"] = FieldValue.serverTimestamp()
                                fixes["referralCode"] = refCode
                                fixes["referral_code"] = refCode
                                db.collection("users").document(userId).set(fixes, SetOptions.merge())
                            }

                            val gridBal = snapshot.getSafeDouble("grid_coin_balance", snapshot.getSafeDouble("gridBalance"))
                            val gridActive = snapshot.getSafeBoolean("grid_mining_active")
                            val gridStart = snapshot.getSafeLong("grid_session_start")
                            val gridEnd = snapshot.getSafeLong("grid_session_end")
                            val appliedBase = snapshot.getSafeDouble("applied_base_rate", 1.0)
                            val teamBonus = snapshot.getSafeDouble("active_team_bonus", 0.0)
                            onGridMiningUpdated?.invoke(gridBal, gridActive, gridStart, gridEnd, appliedBase, teamBonus)

                            val lastSpinTimestamp = snapshot.getSafeTimestampMs("last_wheel_spin_time", 0L)
                            onWheelCooldownUpdated?.invoke(lastSpinTimestamp)
                        } else if (snapshot != null && !snapshot.exists()) {
                            val isMaster = (userId == "master_8080_uid")
                            val assignedCode = if (isMaster) "HG-8080" else AuthService.generateReferralCode(userId)
                            val initDoc = hashMapOf<String, Any>(
                                "uid" to userId,
                                "teamCount" to 0L,
                                "directReferrals" to 0L,
                                "extraHashrate" to 0.0,
                                "bonus_hashrate" to 0.0,
                                "totalReferralRewardsUsdt" to 0.0,
                                "syndicateTier" to "NOVICE",
                                "lastReconciledAt" to FieldValue.serverTimestamp(),
                                "referralCode" to assignedCode,
                                "referral_code" to assignedCode
                            )
                            db.collection("users").document(userId).set(initDoc, SetOptions.merge())
                            onProfileUpdated(0.00, "UNVERIFIED", false, 0L, 0.0, assignedCode)
                            onSyndicateUpdated?.invoke("NOVICE", 0.0)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            // 2. Transactions subcollection listener (/users/{uid}/transactions)
            db.collection("users").document(userId).collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    try {
                        if (snapshot != null) {
                            val activities = mutableListOf<ActivityItem>()
                            val payouts = mutableListOf<PayoutItem>()

                            for (doc in snapshot.documents) {
                                val id = doc.getSafeString("id", doc.id)
                                val title = doc.getSafeString("title")
                                val subtitle = doc.getSafeString("subtitle")
                                val btcStr = doc.getSafeString("btcAmountStr")
                                val usdtAmt = doc.getSafeDouble("usdtAmount")
                                val isCredit = doc.getSafeBoolean("isCredit", true)
                                val dateStr = doc.getSafeString("dateStr", "Recently")
                                val type = doc.getSafeString("type", "ACTIVITY")
                                val statusStr = doc.getSafeString("status", "COMPLETED")

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
                                            targetAddress = doc.getSafeString("cryptoAddress", "USDT Wallet"),
                                            network = doc.getSafeString("network", "TRC20"),
                                            status = payoutStatus
                                        )
                                    )
                                }
                            }

                            onTransactionsUpdated(activities, payouts)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            // 3. Multi-Grid contracts subcollection listener (/users/{uid}/grid_contracts)
            db.collection("users").document(userId).collection("grid_contracts")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    try {
                        if (snapshot != null && !snapshot.isEmpty) {
                            val contracts = mutableListOf<ActiveContract>()
                            val now = System.currentTimeMillis()
                            for (doc in snapshot.documents) {
                                val id = doc.getSafeString("contract_id", doc.getSafeString("id", doc.id))
                                val planName = doc.getSafeString("plan_name", doc.getSafeString("planName", "Starter Grid"))
                                val cryptoSymbol = doc.getSafeString("cryptoSymbol", "BTC")
                                val depositUsdt = doc.getSafeDouble("cost_usdt", doc.getSafeDouble("depositUsdt", 10.0))
                                val planCost = doc.getSafeDouble("plan_cost", depositUsdt)
                                val targetYield30 = doc.getSafeDouble("target_yield_30_percent", if (planCost > 0) planCost * 0.30 else 3.0)
                                val hashPowerGh = doc.getSafeDouble("hashPowerGh", doc.getSafeDouble("hashrate_ths", 10.0) * 1000.0)
                                val hashrateThs = doc.getSafeDouble("hashrate_ths", hashPowerGh / 1000.0)
                                val totalDays = doc.getSafeLong("totalDays", 30L).toInt()
                                val dailyYieldUsdt = doc.getSafeDouble("dailyYieldUsdt", depositUsdt * 0.005)
                                val isRestake = doc.getSafeBoolean("isRestakeEnabled")
                                val startDateStr = doc.getSafeString("startDateStr", "Active")
                                val maturityDateStr = doc.getSafeString("maturityDateStr", "30 Days Term")
                                val startMs = doc.getSafeTimestampMs("purchased_at_ms", now)
                                val endMs = doc.getSafeTimestampMs("expires_at_ms", startMs + (totalDays * 24L * 3600 * 1000))
                                val isActive = doc.getSafeBoolean("is_active", now < endMs)
                                val elapsedDays = ((now - startMs) / (24L * 3600 * 1000)).toInt().coerceIn(0, totalDays)

                                val currentYield = doc.getSafeDouble("current_yield_mined", doc.getSafeDouble("accruedProfitUsdt", dailyYieldUsdt * elapsedDays))
                                val taskProgress = safeComputeTaskProgress(currentYield, depositUsdt)
                                val workStatus = doc.getSafeString("work_status", if (currentYield >= targetYield30 && targetYield30 > 0) "COMPLETED" else if (planCost <= 0) "COMPLETED" else "IN_PROGRESS")
                                val unlocked = doc.getSafeBoolean("unlocked_for_withdrawal", workStatus == "COMPLETED")

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
                        } else if (snapshot != null) {
                            db.collection("users").document(userId).collection("miners")
                                .get().addOnSuccessListener { minerSnap ->
                                    if (minerSnap != null && !minerSnap.isEmpty) {
                                        try {
                                            val fallbackContracts = mutableListOf<ActiveContract>()
                                            val now = System.currentTimeMillis()
                                            for (doc in minerSnap.documents) {
                                                val id = doc.getSafeString("contract_id", doc.getSafeString("id", doc.id))
                                                val planName = doc.getSafeString("plan_name", doc.getSafeString("planName", "Mining Rig"))
                                                val depositUsdt = doc.getSafeDouble("cost_usdt", doc.getSafeDouble("depositUsdt", 10.0))
                                                val planCost = doc.getSafeDouble("plan_cost", depositUsdt)
                                                val targetYield30 = doc.getSafeDouble("target_yield_30_percent", if (planCost > 0) planCost * 0.30 else 3.0)
                                                val hashPowerGh = doc.getSafeDouble("hashPowerGh", 10000.0)
                                                val hashrateThs = doc.getSafeDouble("hashrate_ths", hashPowerGh / 1000.0)
                                                val startMs = doc.getSafeTimestampMs("purchased_at_ms", now)
                                                val endMs = doc.getSafeTimestampMs("expires_at_ms", startMs + (30L * 24 * 3600 * 1000))
                                                val dailyYieldUsdt = doc.getSafeDouble("dailyYieldUsdt", 0.5)
                                                val elapsedDays = ((now - startMs) / (24L * 3600 * 1000)).toInt().coerceIn(0, 30)

                                                val currentYield = doc.getSafeDouble("current_yield_mined", doc.getSafeDouble("accruedProfitUsdt", dailyYieldUsdt * elapsedDays))
                                                val taskProgress = safeComputeTaskProgress(currentYield, depositUsdt)
                                                val workStatus = doc.getSafeString("work_status", if (currentYield >= targetYield30 && targetYield30 > 0) "COMPLETED" else if (planCost <= 0) "COMPLETED" else "IN_PROGRESS")
                                                val unlocked = doc.getSafeBoolean("unlocked_for_withdrawal", workStatus == "COMPLETED")

                                                fallbackContracts.add(
                                                    ActiveContract(
                                                        id = id,
                                                        planName = planName,
                                                        cryptoSymbol = doc.getSafeString("cryptoSymbol", "BTC"),
                                                        depositUsdt = depositUsdt,
                                                        hashPowerGh = hashPowerGh,
                                                        elapsedDays = elapsedDays,
                                                        totalDays = 30,
                                                        accruedProfitUsdt = currentYield,
                                                        dailyYieldUsdt = dailyYieldUsdt,
                                                        isRestakeEnabled = doc.getSafeBoolean("isRestakeEnabled"),
                                                        startDateStr = doc.getSafeString("startDateStr", "Active"),
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
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            // 4. Notifications subcollection (/users/{uid}/notifications)
            db.collection("users").document(userId).collection("notifications")
                .whereEqualTo("isRead", false)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    try {
                        if (snapshot != null) {
                            onNotificationsCountUpdated(snapshot.size())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            // 5. Team & Syndicate subcollection listener (/users/{uid}/team)
            db.collection("users").document(userId).collection("team")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    try {
                        if (snapshot != null) {
                            val teamList = mutableListOf<TeamMember>()
                            val simpleDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                            for (doc in snapshot.documents) {
                                val memberUid = doc.getSafeString("uid", doc.id)
                                val rawName = doc.getSafeString("displayName").ifBlank { doc.getSafeString("name") }
                                val displayName = if (rawName.isNotBlank() && !rawName.contains("@")) {
                                    rawName
                                } else {
                                    "User 0x" + memberUid.replace("-", "").take(6).lowercase()
                                }
                                val email = doc.getSafeString("email")
                                val status = doc.getSafeString("status", "ACTIVE")
                                val bonus = doc.getSafeDouble("hashrateContributed", doc.getSafeDouble("hashrateBonus", doc.getSafeDouble("bonus", 1.5)))
                                val isMining = doc.getSafeBoolean("isMining", true)
                                val avatar = doc.getSafeString("avatarUrl").ifBlank { doc.getSafeString("photoUrl") }.takeIf { it.isNotBlank() }
                                val timestampMs = doc.getSafeTimestampMs("joinedAt", System.currentTimeMillis())
                                val dateStr = try { simpleDateFormat.format(Date(timestampMs)) } catch (_: Exception) { "Recently" }

                                teamList.add(
                                    TeamMember(
                                        uid = memberUid,
                                        displayName = displayName,
                                        email = email,
                                        joinedAtStr = dateStr,
                                        joinedAtMs = timestampMs,
                                        status = status,
                                        hashrateBonus = bonus,
                                        hashrateContributed = bonus,
                                        isMining = isMining,
                                        avatarUrl = avatar
                                    )
                                )
                            }
                            teamList.sortByDescending { it.joinedAtMs }
                            val activeCount = teamList.count { it.status.equals("ACTIVE", ignoreCase = true) }.toLong()
                            onTeamUpdated?.invoke(activeCount, teamList)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
                firestore?.collection("users")?.document(user.uid)?.update("usdt_balance", user.walletBalance)

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
                                val cleanBal = if (balance.isNaN() || balance.isInfinite()) 0.0 else balance
                                onRemoteBalanceReceived(cleanBal)
                            }
                        }
                    }
                } catch (_: Exception) {}

                delay(10000L)
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

    fun saveTotpSecret(uid: String, secret: String, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            try {
                val updateMap = hashMapOf<String, Any>(
                    "totp_enabled" to true,
                    "totp_secret" to secret,
                    "two_factor_enabled" to true,
                    "totp_activated_at" to FieldValue.serverTimestamp()
                )
                firestore?.collection("users")?.document(uid)?.set(updateMap, SetOptions.merge())

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

    suspend fun fetchTotpDetails(uid: String): Pair<Boolean, String?> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val snap = firestore?.collection("users")?.document(uid)?.get()?.await()
            if (snap != null && snap.exists()) {
                val enabled = snap.getSafeBoolean("totp_enabled", snap.getSafeBoolean("two_factor_enabled"))
                val secret = snap.getSafeString("totp_secret")
                if (secret.isNotBlank()) {
                    return@withContext Pair(enabled, secret)
                }
            }

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
                val cleanYield = if (currentYieldMined.isNaN() || currentYieldMined.isInfinite()) 0.0 else currentYieldMined
                val cleanProgress = if (taskProgressPct.isNaN() || taskProgressPct.isInfinite()) 0.0 else taskProgressPct.coerceIn(0.0, 100.0)

                val updates = hashMapOf<String, Any>(
                    "current_yield_mined" to cleanYield,
                    "task_progress_pct" to cleanProgress,
                    "work_status" to workStatus,
                    "unlocked_for_withdrawal" to unlockedForWithdrawal,
                    "accruedProfitUsdt" to cleanYield
                )
                firestore?.collection("users")?.document(userId)?.collection("grid_contracts")?.document(contractId)?.update(updates)
                firestore?.collection("users")?.document(userId)?.collection("miners")?.document(contractId)?.update(updates)
            } catch (_: Exception) {}
        }
    }

    /**
     * PILLAR 4: Firestore Atomic Transaction & Anti-Replay Lock for USDT Dual-Network Deposits
     */
    suspend fun verifyAndProcessDepositAtomic(
        walletAddress: String,
        userId: String,
        txId: String,
        amountUsdt: Double,
        network: String
    ): Pair<Boolean, String> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val cleanTxId = sanitizeTxHash(txId)
        if (cleanTxId.isBlank()) {
            return@withContext Pair(false, "Invalid or empty Transaction Hash / Payment ID.")
        }
        if (amountUsdt <= 0.0) {
            return@withContext Pair(false, "Invalid deposit amount specified.")
        }
        val db = firestore ?: return@withContext Pair(false, "Firestore database instance not available.")

        val targetWallet = if (walletAddress.isNotBlank()) walletAddress else userId
        if (targetWallet.isBlank()) {
            return@withContext Pair(false, "Target wallet address or User ID missing.")
        }

        try {
            val ledgerRef = db.collection("processed_deposits").document(cleanTxId)
            val walletRef = db.collection("wallets").document(targetWallet)
            val walletDepRef = walletRef.collection("deposits").document(cleanTxId)
            val userRef = if (userId.isNotBlank()) db.collection("users").document(userId) else null
            val userTxRef = if (userId.isNotBlank()) db.collection("users").document(userId).collection("transactions").document(cleanTxId) else null

            val resultMessage = db.runTransaction { transaction ->
                // 1. Anti-Replay Check
                val ledgerSnap = transaction.get(ledgerRef)
                if (ledgerSnap.exists() && ledgerSnap.getSafeString("status") == "COMPLETED") {
                    throw IllegalStateException("Transaction already processed and credited.")
                }

                val nowMs = System.currentTimeMillis()
                val nowStr = getCurrentTimestamp()

                // 2. Global Anti-Replay Ledger Entry
                val ledgerData = hashMapOf<String, Any>(
                    "tx_id" to cleanTxId,
                    "wallet_address" to targetWallet,
                    "amount_usdt" to amountUsdt,
                    "network" to network,
                    "processed_at" to FieldValue.serverTimestamp(),
                    "status" to "COMPLETED"
                )
                transaction.set(ledgerRef, ledgerData, SetOptions.merge())

                // 3. Atomically update wallets/{wallet_address}
                val walletUpdates = hashMapOf<String, Any>(
                    "wallet_address" to targetWallet,
                    "usdt_balance" to FieldValue.increment(amountUsdt),
                    "usdtBalance" to FieldValue.increment(amountUsdt),
                    "availableBalance" to FieldValue.increment(amountUsdt),
                    "last_synced_at" to nowMs
                )
                transaction.set(walletRef, walletUpdates, SetOptions.merge())

                // 4. Record deposit entry in wallets/{wallet_address}/deposits/{payment_or_tx_id}
                val depositEntry = hashMapOf<String, Any>(
                    "deposit_id" to cleanTxId,
                    "amount_usdt" to amountUsdt,
                    "network" to network,
                    "status" to "COMPLETED",
                    "timestamp" to FieldValue.serverTimestamp()
                )
                transaction.set(walletDepRef, depositEntry, SetOptions.merge())

                // 5. Dual-sync user document & user transaction sub-collection if userId provided
                if (userRef != null) {
                    val userUpdates = hashMapOf<String, Any>(
                        "usdt_balance" to FieldValue.increment(amountUsdt),
                        "usdtBalance" to FieldValue.increment(amountUsdt),
                        "availableBalance" to FieldValue.increment(amountUsdt),
                        "last_active" to FieldValue.serverTimestamp()
                    )
                    transaction.set(userRef, userUpdates, SetOptions.merge())
                }

                if (userTxRef != null) {
                    val userTxDoc = hashMapOf<String, Any>(
                        "id" to cleanTxId,
                        "title" to "+$${String.format(Locale.US, "%.2f", amountUsdt)} USDT",
                        "subtitle" to "Verified Deposit ($network)",
                        "btcAmountStr" to "",
                        "usdtAmount" to amountUsdt,
                        "amount" to amountUsdt,
                        "network" to network,
                        "currency" to "USDT",
                        "isCredit" to true,
                        "type" to "DEPOSIT",
                        "status" to "COMPLETED",
                        "timestamp" to FieldValue.serverTimestamp(),
                        "dateStr" to nowStr
                    )
                    transaction.set(userTxRef, userTxDoc, SetOptions.merge())
                }

                "SUCCESS"
            }.await()

            if (resultMessage == "SUCCESS") {
                pushDeposit(FirebaseDeposit(
                    paymentId = cleanTxId,
                    userId = targetWallet,
                    amount = amountUsdt,
                    currency = "USDT",
                    network = network,
                    txHash = cleanTxId,
                    status = "COMPLETED",
                    timestamp = getCurrentTimestamp()
                ))
                Pair(true, "Deposit of $${String.format(Locale.US, "%.2f", amountUsdt)} USDT confirmed & credited!")
            } else {
                Pair(false, "Deposit transaction failed.")
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Transaction error"
            if (msg.contains("already processed", ignoreCase = true)) {
                Pair(false, "Transaction already processed and credited.")
            } else {
                Pair(false, "Verification error: $msg")
            }
        }
    }

    fun sanitizeTxHash(input: String): String {
        return input.trim().replace("\r", "").replace("\n", "").replace(" ", "")
    }
}

