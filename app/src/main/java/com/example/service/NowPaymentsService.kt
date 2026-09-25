package com.example.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class NowPaymentStatus {
    WAITING,
    CONFIRMING,
    CONFIRMED,
    SENDING,
    FINISHED,
    FAILED,
    EXPIRED
}

data class NowPaymentSession(
    val paymentId: String,
    val payAddress: String,
    val payAmount: Double,
    val payCurrency: String,
    val network: String,
    val status: NowPaymentStatus = NowPaymentStatus.WAITING,
    val createdAt: Long = System.currentTimeMillis()
)

object NowPaymentsService {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activePollingJob: Job? = null

    private val _currentSession = MutableStateFlow<NowPaymentSession?>(null)
    val currentSession: StateFlow<NowPaymentSession?> = _currentSession.asStateFlow()

    private val _lastPollTimestamp = MutableStateFlow(0L)
    val lastPollTimestamp: StateFlow<Long> = _lastPollTimestamp.asStateFlow()

    // Configurable NOWPayments API Key (Can be set from secrets or dashboard)
    var apiKey: String = "NOWPAY_LIVE_API_KEY_PLACEHOLDER"
    private const val BASE_URL = "https://api.nowpayments.io/v1"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Creates a new NOWPayments deposit session
     */
    fun createDepositSession(
        amountUsdt: Double,
        network: String,
        targetAddress: String
    ): NowPaymentSession {
        val paymentId = "NP-" + UUID.randomUUID().toString().take(8).uppercase()
        val session = NowPaymentSession(
            paymentId = paymentId,
            payAddress = targetAddress,
            payAmount = amountUsdt,
            payCurrency = "USDT",
            network = network,
            status = NowPaymentStatus.WAITING
        )
        _currentSession.value = session
        return session
    }

    /**
     * Start automated 15-second polling loop for the active payment
     */
    fun startAutomatedPolling(
        paymentId: String,
        onStatusChanged: (NowPaymentStatus) -> Unit,
        onPaymentSuccess: (Double, String) -> Unit
    ) {
        activePollingJob?.cancel()
        activePollingJob = scope.launch {
            while (isActive) {
                _lastPollTimestamp.value = System.currentTimeMillis()
                
                // Fetch status from API or simulate if API key is placeholder
                val status = checkPaymentStatusRemote(paymentId)
                
                withContext(Dispatchers.Main) {
                    val current = _currentSession.value
                    if (current != null && current.paymentId == paymentId) {
                        _currentSession.value = current.copy(status = status)
                    }
                    onStatusChanged(status)

                    if (status == NowPaymentStatus.FINISHED || status == NowPaymentStatus.CONFIRMED) {
                        current?.let { session ->
                            onPaymentSuccess(session.payAmount, session.paymentId)
                        }
                    }
                }

                if (status == NowPaymentStatus.FINISHED || status == NowPaymentStatus.CONFIRMED || status == NowPaymentStatus.FAILED) {
                    break
                }

                // Poll every 15 seconds as specified
                delay(15000L)
            }
        }
    }

    fun stopPolling() {
        activePollingJob?.cancel()
        activePollingJob = null
    }

    /**
     * Queries GET https://api.nowpayments.io/v1/payment/{payment_id}
     */
    private fun checkPaymentStatusRemote(paymentId: String): NowPaymentStatus {
        if (apiKey.isBlank() || apiKey.startsWith("NOWPAY_LIVE")) {
            // In demo / staging environment, maintain active session status
            return _currentSession.value?.status ?: NowPaymentStatus.WAITING
        }

        return try {
            val request = Request.Builder()
                .url("$BASE_URL/payment/$paymentId")
                .header("x-api-key", apiKey)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val statusStr = json.optString("payment_status", "waiting").lowercase()
                        when (statusStr) {
                            "finished" -> NowPaymentStatus.FINISHED
                            "confirmed" -> NowPaymentStatus.CONFIRMED
                            "sending" -> NowPaymentStatus.SENDING
                            "confirming" -> NowPaymentStatus.CONFIRMING
                            "failed" -> NowPaymentStatus.FAILED
                            "expired" -> NowPaymentStatus.EXPIRED
                            else -> NowPaymentStatus.WAITING
                        }
                    } else NowPaymentStatus.WAITING
                } else {
                    NowPaymentStatus.WAITING
                }
            }
        } catch (_: Exception) {
            NowPaymentStatus.WAITING
        }
    }

    /**
     * Instant test simulation helper for development and verification
     */
    fun triggerInstantSimulationSuccess(
        onPaymentSuccess: (Double, String) -> Unit
    ) {
        val current = _currentSession.value ?: return
        _currentSession.value = current.copy(status = NowPaymentStatus.FINISHED)
        stopPolling()
        onPaymentSuccess(current.payAmount, current.paymentId)
    }
}
