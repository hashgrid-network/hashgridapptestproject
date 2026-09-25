package com.example.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

object FirebaseSyncService {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Configurable Firebase Realtime Database Base URL
    var firebaseDatabaseUrl: String = "https://hashgrid-institutional-default-rtdb.firebaseio.com"

    private val _isFirebaseSynced = MutableStateFlow(true)
    val isFirebaseSynced: StateFlow<Boolean> = _isFirebaseSynced.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Sync user profile to /users/{uid}/profile (User writable)
     */
    fun syncUserProfile(
        uid: String,
        email: String,
        displayName: String,
        referralCode: String,
        isFlaggedDuplicate: Boolean
    ) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("uid", uid)
                    put("email", email)
                    put("displayName", displayName)
                    put("referralCode", referralCode)
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

    /**
     * Sync user wallet & balance to /users/{uid}/wallet (Admin controlled)
     */
    fun syncUser(user: FirebaseUser) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("uid", user.uid)
                    put("email", user.email)
                    put("walletBalance", user.walletBalance)
                    put("miningRate", user.miningRate)
                    put("createdAt", user.createdAt)
                    put("lastActive", getCurrentTimestamp())
                }

                val url = "$firebaseDatabaseUrl/users/${sanitizeKey(user.uid)}.json"
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
     * Listen to wallet balance updates from Firebase
     */
    fun startRealtimeBalanceListener(
        userId: String,
        onRemoteBalanceReceived: (Double) -> Unit
    ) {
        scope.launch {
            while (isActive) {
                try {
                    val url = "$firebaseDatabaseUrl/users/${sanitizeKey(userId)}/walletBalance.json"
                    val request = Request.Builder().url(url).get().build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()?.trim()
                            if (!body.isNullOrBlank() && body != "null") {
                                val balance = body.toDoubleOrNull()
                                if (balance != null) {
                                    onRemoteBalanceReceived(balance)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}

                delay(15000L) // Poll remote every 15s
            }
        }
    }

    private fun sanitizeKey(key: String): String {
        return key.replace("#", "").replace(".", "_").replace("$", "").replace("[", "").replace("]", "")
    }

    fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return sdf.format(Date())
    }
}
