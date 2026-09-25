package com.example.service

import com.example.model.ActiveContract
import com.example.model.ActivityItem
import com.example.model.MiningPlan
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
    val kycStatus: String = "UNVERIFIED",
    val twoFactorEnabled: Boolean = false,
    val activityLogs: List<ActivityItem> = emptyList()
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

    // Payment settings default
    var trc20DepositAddress: String = "TJj7G3U8qVSzqcJaxAhQG34ADHihnR6WuD"
    var bep20DepositAddress: String = "0x1fAcE21fc7cA33abb4B37fba82280266C12D9c09"

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
                        UserRemoteData(
                            usdtBalance = usdt,
                            btcBalance = btc,
                            hashRate = hr,
                            totalWithdrawn = withdrawn,
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
     * Initializes a NEW user document in Firestore and Firebase RTDB /users/{uid}
     */
    fun initializeNewUser(
        uid: String,
        email: String,
        displayName: String,
        photoUrl: String?,
        accountId: String
    ) {
        scope.launch {
            val safeKey = sanitizeKey(uid)
            try {
                // 1. Initialize in Firestore /users/{uid}
                val firestoreMap = hashMapOf<String, Any>(
                    "uid" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "photoUrl" to (photoUrl ?: ""),
                    "accountId" to accountId,
                    "usdt_balance" to 0.00,
                    "btc_balance" to 0.000000,
                    "total_withdrawn" to 0.00,
                    "hash_rate" to 0.0,
                    "kyc_status" to "UNVERIFIED",
                    "two_factor_enabled" to false,
                    "created_at" to FieldValue.serverTimestamp(),
                    "last_active" to FieldValue.serverTimestamp()
                )
                firestore?.collection("users")?.document(uid)?.set(firestoreMap, SetOptions.merge())

                // 2. Initialize in RTDB /users/{uid}.json
                val json = JSONObject().apply {
                    put("uid", uid)
                    put("email", email)
                    put("displayName", displayName)
                    put("photoUrl", photoUrl ?: "")
                    put("accountId", accountId)
                    put("usdt_balance", 0.00)
                    put("btc_balance", 0.000000)
                    put("total_withdrawn", 0.00)
                    put("hash_rate", 0.0)
                    put("kyc_status", "UNVERIFIED")
                    put("two_factor_enabled", false)
                    put("walletBalance", 0.00)
                    put("miningRate", "0.00 GH/s")
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
            val minerId = "miner_" + System.currentTimeMillis().toString().takeLast(8)
            val nowStr = getCurrentTimestamp()

            try {
                // 1. Add miner record under /users/{uid}/miners
                val minerDoc = hashMapOf<String, Any>(
                    "id" to minerId,
                    "planId" to plan.id,
                    "planName" to plan.name,
                    "cryptoSymbol" to plan.cryptoSymbol,
                    "depositUsdt" to plan.minDepositUsdt,
                    "hashPowerGh" to plan.hashPowerGh,
                    "elapsedDays" to 0,
                    "totalDays" to plan.termDays,
                    "dailyYieldUsdt" to plan.dailyYieldUsdtEst,
                    "isRestakeEnabled" to false,
                    "startDateStr" to nowStr,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore?.collection("users")?.document(userId)?.collection("miners")?.document(minerId)?.set(minerDoc)

                // 2. Add transaction record under /users/{uid}/transactions
                if (plan.minDepositUsdt > 0) {
                    val txDoc = hashMapOf<String, Any>(
                        "id" to "act_${System.currentTimeMillis()}",
                        "title" to "-$${String.format(Locale.US, "%.2f", plan.minDepositUsdt)} USDT",
                        "subtitle" to "Activated: ${plan.name}",
                        "btcAmountStr" to "",
                        "usdtAmount" to plan.minDepositUsdt,
                        "isCredit" to false,
                        "type" to "PLAN_PURCHASE",
                        "status" to "COMPLETED",
                        "timestamp" to FieldValue.serverTimestamp(),
                        "dateStr" to nowStr
                    )
                    firestore?.collection("users")?.document(userId)?.collection("transactions")?.document("act_${System.currentTimeMillis()}")?.set(txDoc)

                    // 3. Deduct USDT balance in Firestore
                    firestore?.collection("users")?.document(userId)?.update("usdt_balance", FieldValue.increment(-plan.minDepositUsdt))
                }

                // 4. Update hashpower in Firestore
                firestore?.collection("users")?.document(userId)?.update("hash_rate", FieldValue.increment(plan.hashPowerGh))

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

    /**
     * Listen to Firestore user document & subcollections
     */
    fun listenFirestoreUser(
        userId: String,
        onProfileUpdated: (Double, String, Boolean) -> Unit,
        onTransactionsUpdated: (List<ActivityItem>, List<PayoutItem>) -> Unit,
        onMinersUpdated: (List<ActiveContract>) -> Unit,
        onNotificationsCountUpdated: (Int) -> Unit
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
                        onProfileUpdated(bal, kyc, twoFa)
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

            // 3. Miners subcollection listener (/users/{uid}/miners)
            db.collection("users").document(userId).collection("miners")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val contracts = mutableListOf<ActiveContract>()
                        for (doc in snapshot.documents) {
                            val id = doc.getString("id") ?: doc.id
                            val planName = doc.getString("planName") ?: "Mining Node"
                            val cryptoSymbol = doc.getString("cryptoSymbol") ?: "BTC"
                            val depositUsdt = doc.getDouble("depositUsdt") ?: 0.0
                            val hashPowerGh = doc.getDouble("hashPowerGh") ?: 0.0
                            val elapsedDays = doc.getLong("elapsedDays")?.toInt() ?: 0
                            val totalDays = doc.getLong("totalDays")?.toInt() ?: 30
                            val dailyYieldUsdt = doc.getDouble("dailyYieldUsdt") ?: 0.0
                            val isRestake = doc.getBoolean("isRestakeEnabled") ?: false
                            val startDateStr = doc.getString("startDateStr") ?: "Active"

                            contracts.add(
                                ActiveContract(
                                    id = id,
                                    planName = planName,
                                    cryptoSymbol = cryptoSymbol,
                                    depositUsdt = depositUsdt,
                                    hashPowerGh = hashPowerGh,
                                    elapsedDays = elapsedDays,
                                    totalDays = totalDays,
                                    accruedProfitUsdt = dailyYieldUsdt * elapsedDays,
                                    dailyYieldUsdt = dailyYieldUsdt,
                                    isRestakeEnabled = isRestake,
                                    startDateStr = startDateStr,
                                    maturityDateStr = "30 Days Term"
                                )
                            )
                        }
                        onMinersUpdated(contracts)
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
}
