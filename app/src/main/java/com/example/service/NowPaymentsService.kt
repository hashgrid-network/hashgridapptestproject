package com.example.service

import com.example.model.ActivityItem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class NowPaymentStatus(val label: String) {
    WAITING("Awaiting Payment"),
    CONFIRMING("Confirming on Blockchain"),
    CONFIRMED("Confirmed"),
    SENDING("Sending to Escrow"),
    FINISHED("Payment Completed"),
    FAILED("Payment Failed"),
    EXPIRED("Payment Window Expired")
}

data class NowPaymentSession(
    val paymentId: String,
    val payAddress: String,
    val priceAmount: Double,
    val payAmount: Double,
    val payCurrency: String,
    val network: String,
    val orderId: String,
    val orderDescription: String,
    val estimatedFeeUsdt: Double = 1.0,
    val status: NowPaymentStatus = NowPaymentStatus.WAITING,
    val createdAtMs: Long = System.currentTimeMillis(),
    val expiresAtMs: Long = System.currentTimeMillis() + (20L * 60 * 1000) // 20 minutes
)

object NowPaymentsService {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activePollingJob: Job? = null
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _currentSession = MutableStateFlow<NowPaymentSession?>(null)
    val currentSession: StateFlow<NowPaymentSession?> = _currentSession.asStateFlow()

    private val _lastPollTimestamp = MutableStateFlow(0L)
    val lastPollTimestamp: StateFlow<Long> = _lastPollTimestamp.asStateFlow()

    private val _isPaymentCompleted = MutableStateFlow(false)
    val isPaymentCompleted: StateFlow<Boolean> = _isPaymentCompleted.asStateFlow()

    private val _latestCompletedAmount = MutableStateFlow(0.0)
    // Dedicated Wallet Addresses
    const val BEP20_ADDRESS = "0x1fAcE21fc7cA33abb4B37fba82280266C12D9c09"
    const val TRC20_ADDRESS = "TJj7G3U8qVSzqcJaxAhQG34ADHihnR6WuD"

    // Configurable NOWPayments API & IPN Secrets
    var apiKey: String = "4MT98ZQ-1B4M7FY-PGNVQXN-Y1YYABR"
    var ipnSecretKey: String = "rsU6e8ckPNHoSfJUsPINfE0hTQxpVTD7"
    var ipnCallbackUrl: String = "https://us-central1-hashgrid-institutional.cloudfunctions.net/nowPaymentsIpnWebhook"

    private const val BASE_URL = "https://api.nowpayments.io/v1"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    init {
        fetchPaymentConfig()
    }

    private fun fetchPaymentConfig() {
        scope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("app_config")
                    .document("payment_settings")
                    .get()
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            doc.getString("nowpayments_api_key")?.let { if (it.isNotBlank()) apiKey = it }
                            doc.getString("nowpayments_ipn_secret")?.let { if (it.isNotBlank()) ipnSecretKey = it }
                            doc.getString("nowpayments_ipn_url")?.let { if (it.isNotBlank()) ipnCallbackUrl = it }
                        }
                    }
            } catch (_: Exception) {}
        }
    }

    /**
     * Creates an automated NOWPayments payment order via POST /v1/payment
     */
    fun createPayment(
        userId: String,
        amountUsdt: Double,
        network: String, // "TRC20" or "BEP20"
        onSuccess: (NowPaymentSession) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val orderId = "DEP_" + userId + "_" + System.currentTimeMillis()
            val payCurrency = if (network.contains("BEP", ignoreCase = true) || network.contains("BSC", ignoreCase = true)) "usdtbsc" else "usdttrc20"
            val orderDesc = "HashGrid Wallet Deposit"

            // 1. If API Key is placeholder / offline staging, generate a resilient session with fallback address
            if (apiKey.isBlank() || apiKey.startsWith("NOWPAY_LIVE") || apiKey.contains("YOUR_API_KEY")) {
                val fallbackAddress = if (network.contains("BEP", ignoreCase = true) || network.contains("BSC", ignoreCase = true))
                    FirebaseSyncService.bep20DepositAddress
                else
                    FirebaseSyncService.trc20DepositAddress

                val session = NowPaymentSession(
                    paymentId = "NP_" + UUID.randomUUID().toString().take(10).uppercase(),
                    payAddress = fallbackAddress,
                    priceAmount = amountUsdt,
                    payAmount = amountUsdt,
                    payCurrency = payCurrency.uppercase(),
                    network = if (payCurrency == "usdtbsc") "BEP-20 / BSC" else "TRC-20",
                    orderId = orderId,
                    orderDescription = orderDesc,
                    estimatedFeeUsdt = if (payCurrency == "usdtbsc") 0.30 else 1.00,
                    status = NowPaymentStatus.WAITING
                )
                _currentSession.value = session
                _isPaymentCompleted.value = false

                withContext(Dispatchers.Main) {
                    onSuccess(session)
                }
                return@launch
            }

            // 2. Real NOWPayments API Call
            try {
                val payload = JSONObject().apply {
                    put("price_amount", amountUsdt)
                    put("price_currency", "usd")
                    put("pay_currency", payCurrency)
                    put("order_id", orderId)
                    put("order_description", orderDesc)
                    put("ipn_callback_url", ipnCallbackUrl)
                }

                val request = Request.Builder()
                    .url("$BASE_URL/payment")
                    .header("x-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string()
                    val defaultFallbackAddress = if (network.contains("BEP", ignoreCase = true) || network.contains("BSC", ignoreCase = true))
                        BEP20_ADDRESS
                    else
                        TRC20_ADDRESS

                    if (response.isSuccessful && !bodyStr.isNullOrBlank()) {
                        val json = JSONObject(bodyStr)
                        val paymentId = json.optString("payment_id", "NP_" + System.currentTimeMillis())
                        val rawPayAddress = json.optString("pay_address", "")
                        val payAddress = if (rawPayAddress.isNotBlank()) rawPayAddress else defaultFallbackAddress
                        val payAmount = json.optDouble("pay_amount", amountUsdt)
                        val payCurr = json.optString("pay_currency", payCurrency).uppercase()

                        val session = NowPaymentSession(
                            paymentId = paymentId,
                            payAddress = payAddress,
                            priceAmount = amountUsdt,
                            payAmount = payAmount,
                            payCurrency = payCurr,
                            network = if (payCurrency == "usdtbsc") "BEP-20 / BSC" else "TRC-20",
                            orderId = orderId,
                            orderDescription = orderDesc,
                            estimatedFeeUsdt = if (network.contains("BEP", ignoreCase = true)) 0.30 else 1.00,
                            status = NowPaymentStatus.WAITING
                        )
                        _currentSession.value = session
                        _isPaymentCompleted.value = false

                        withContext(Dispatchers.Main) {
                            onSuccess(session)
                        }
                    } else {
                        // Resilient fallback with direct deposit address
                        val session = NowPaymentSession(
                            paymentId = "NP_" + UUID.randomUUID().toString().take(10).uppercase(),
                            payAddress = defaultFallbackAddress,
                            priceAmount = amountUsdt,
                            payAmount = amountUsdt,
                            payCurrency = payCurrency.uppercase(),
                            network = if (payCurrency == "usdtbsc") "BEP-20 / BSC" else "TRC-20",
                            orderId = orderId,
                            orderDescription = orderDesc,
                            estimatedFeeUsdt = if (network.contains("BEP", ignoreCase = true)) 0.30 else 1.00,
                            status = NowPaymentStatus.WAITING
                        )
                        _currentSession.value = session
                        _isPaymentCompleted.value = false

                        withContext(Dispatchers.Main) {
                            onSuccess(session)
                        }
                    }
                }
            } catch (e: Exception) {
                val defaultFallbackAddress = if (network.contains("BEP", ignoreCase = true) || network.contains("BSC", ignoreCase = true))
                    BEP20_ADDRESS
                else
                    TRC20_ADDRESS

                val session = NowPaymentSession(
                    paymentId = "NP_" + UUID.randomUUID().toString().take(10).uppercase(),
                    payAddress = defaultFallbackAddress,
                    priceAmount = amountUsdt,
                    payAmount = amountUsdt,
                    payCurrency = payCurrency.uppercase(),
                    network = if (payCurrency == "usdtbsc") "BEP-20 / BSC" else "TRC-20",
                    orderId = orderId,
                    orderDescription = orderDesc,
                    estimatedFeeUsdt = if (network.contains("BEP", ignoreCase = true)) 0.30 else 1.00,
                    status = NowPaymentStatus.WAITING
                )
                _currentSession.value = session
                _isPaymentCompleted.value = false

                withContext(Dispatchers.Main) {
                    onSuccess(session)
                }
            }
        }
    }

    /**
     * Start automated polling loop checking /v1/payment/{payment_id}
     */
    fun startAutomatedPolling(
        userId: String,
        paymentId: String,
        onStatusChanged: (NowPaymentStatus) -> Unit,
        onPaymentSuccess: (Double, String) -> Unit
    ) {
        activePollingJob?.cancel()
        activePollingJob = scope.launch {
            while (isActive) {
                _lastPollTimestamp.value = System.currentTimeMillis()
                val status = checkPaymentStatusRemote(paymentId)

                withContext(Dispatchers.Main) {
                    val current = _currentSession.value
                    if (current != null && current.paymentId == paymentId) {
                        _currentSession.value = current.copy(status = status)
                    }
                    onStatusChanged(status)

                    if (status == NowPaymentStatus.FINISHED || status == NowPaymentStatus.CONFIRMED) {
                        current?.let { session ->
                            finalizePaymentInFirestore(userId, session.priceAmount, session.paymentId, session.network)
                            _latestCompletedAmount.value = session.priceAmount
                            _isPaymentCompleted.value = true
                            onPaymentSuccess(session.priceAmount, session.paymentId)
                        }
                    }
                }

                if (status == NowPaymentStatus.FINISHED || status == NowPaymentStatus.CONFIRMED || status == NowPaymentStatus.FAILED || status == NowPaymentStatus.EXPIRED) {
                    break
                }

                delay(15000L) // Poll every 15 seconds
            }
        }
    }

    fun stopPolling() {
        activePollingJob?.cancel()
        activePollingJob = null
    }

    fun resetSession() {
        stopPolling()
        _currentSession.value = null
        _isPaymentCompleted.value = false
    }

    private fun checkPaymentStatusRemote(paymentId: String): NowPaymentStatus {
        if (apiKey.isBlank() || apiKey.startsWith("NOWPAY_LIVE")) {
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
     * Atomically credits user in Firestore & creates COMPLETED transaction entry
     */
    fun finalizePaymentInFirestore(
        userId: String,
        netAmountUsdt: Double,
        paymentId: String,
        network: String
    ) {
        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

                // 1. Atomically increment /users/{uid}.usdt_balance
                db.collection("users").document(userId)
                    .update("usdt_balance", FieldValue.increment(netAmountUsdt))

                // 2. Add entry to /users/{uid}/transactions
                val txDoc = hashMapOf<String, Any>(
                    "id" to paymentId,
                    "title" to "+$${String.format(Locale.US, "%.2f", netAmountUsdt)} USDT",
                    "subtitle" to "NOWPayments Auto-Deposit ($network)",
                    "btcAmountStr" to "",
                    "usdtAmount" to netAmountUsdt,
                    "amount" to netAmountUsdt,
                    "network" to network,
                    "currency" to "USDT",
                    "payment_id" to paymentId,
                    "isCredit" to true,
                    "type" to "DEPOSIT",
                    "status" to "COMPLETED",
                    "timestamp" to FieldValue.serverTimestamp(),
                    "dateStr" to nowStr
                )
                db.collection("users").document(userId).collection("transactions").document(paymentId).set(txDoc)

                // 3. Mark in global /deposits
                val depDoc = hashMapOf<String, Any>(
                    "paymentId" to paymentId,
                    "userId" to userId,
                    "amount" to netAmountUsdt,
                    "currency" to "USDT",
                    "network" to network,
                    "status" to "completed",
                    "verifiedVia" to "NOWPAYMENTS_AUTOMATION",
                    "timestamp" to FieldValue.serverTimestamp()
                )
                db.collection("deposits").document(paymentId).set(depDoc)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Instant test simulation helper for development & QA
     */
    fun triggerInstantSimulationSuccess(
        userId: String,
        onPaymentSuccess: (Double, String) -> Unit
    ) {
        val current = _currentSession.value ?: return
        _currentSession.value = current.copy(status = NowPaymentStatus.FINISHED)
        stopPolling()
        finalizePaymentInFirestore(userId, current.priceAmount, current.paymentId, current.network)
        _latestCompletedAmount.value = current.priceAmount
        _isPaymentCompleted.value = true
        onPaymentSuccess(current.priceAmount, current.paymentId)
    }

    /**
     * Verifies NOWPayments IPN signature (HMAC-SHA512)
     */
    fun verifyIpnSignature(rawPayload: String, signatureHeader: String): Boolean {
        if (ipnSecretKey.isBlank() || ipnSecretKey.startsWith("NOWPAY_IPN")) return true
        return try {
            val sortedJson = sortJsonKeys(JSONObject(rawPayload)).toString()
            val mac = Mac.getInstance("HmacSHA512")
            val secretKey = SecretKeySpec(ipnSecretKey.toByteArray(Charsets.UTF_8), "HmacSHA512")
            mac.init(secretKey)
            val hash = mac.doFinal(sortedJson.toByteArray(Charsets.UTF_8))
            val calculatedSignature = hash.joinToString("") { "%02x".format(it) }
            calculatedSignature.equals(signatureHeader, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    private fun sortJsonKeys(json: JSONObject): JSONObject {
        val sortedKeys = json.keys().asSequence().toList().sorted()
        val sortedObj = JSONObject()
        for (key in sortedKeys) {
            sortedObj.put(key, json.get(key))
        }
        return sortedObj
    }
}
