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
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    }

    /**
     * Sync user data to /users/{userId}.json
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
     * Push withdrawal request to /withdrawals/{requestId}.json with status 'pending'
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
     * Start background poll to listen to balance updates from Firebase
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

                delay(20000L) // Poll remote every 20s
            }
        }
    }

    private fun sanitizeKey(key: String): String {
        return key.replace("#", "").replace(".", "_").replace("$", "").replace("[", "").replace("]", "")
    }
}
