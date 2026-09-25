package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ActiveContract
import com.example.model.ActivityItem
import com.example.model.BountyStatus
import com.example.model.BountyTask
import com.example.model.BountyType
import com.example.model.ChatMessage
import com.example.model.CreatorMilestoneSubmission
import com.example.model.LiveTickerItem
import com.example.model.MilestoneStatus
import com.example.model.MiningPlan
import com.example.model.PayoutItem
import com.example.model.PayoutStatus
import com.example.model.PriceDirection
import com.example.model.User
import com.example.service.AppUpdateInfo
import com.example.service.AppUpdateManager
import com.example.service.AuthService
import com.example.service.BinanceWebSocketService
import com.example.service.FirebaseDeposit
import com.example.service.FirebaseSyncService
import com.example.service.FirebaseTaskClaim
import com.example.service.FirebaseUser
import com.example.service.FirebaseWithdrawal
import com.example.service.GeminiSupportService
import com.example.service.UpdateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class HashGridViewModel : ViewModel() {

    // --- Navigation & Sub-Tabs ---
    private val _currentTab = MutableStateFlow(0) // 0: Home, 1: Plans, 2: Wallet, 3: Growth, 4: Account
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _plansSubTab = MutableStateFlow(0) // 0: Active Mining, 1: Hardware Marketplace
    val plansSubTab: StateFlow<Int> = _plansSubTab.asStateFlow()

    private val _walletSubTab = MutableStateFlow(0) // 0: Activity, 1: Payouts
    val walletSubTab: StateFlow<Int> = _walletSubTab.asStateFlow()

    // --- Live Real-Time Ticker & Hashpower ---
    private val _btcPrice = MutableStateFlow(89450.00)
    val btcPrice: StateFlow<Double> = _btcPrice.asStateFlow()

    private val _kasPrice = MutableStateFlow(0.1428)
    val kasPrice: StateFlow<Double> = _kasPrice.asStateFlow()

    private val _hashPower = MutableStateFlow(0.0)
    val hashPower: StateFlow<Double> = _hashPower.asStateFlow()

    // --- User Profile & Auth State ---
    val currentUser: StateFlow<User?> = AuthService.currentUser
    val isLoggedIn: StateFlow<Boolean> = AuthService.isLoggedIn

    val userId: String get() = currentUser.value?.id?.ifBlank { "HG-ACCOUNT" } ?: "HG-ACCOUNT"
    val userEmail: String get() = currentUser.value?.email ?: ""
    val userDisplayName: String get() = currentUser.value?.displayName ?: "User"
    val referralCode: String get() = currentUser.value?.referralCode ?: "HG-7798"

    // Real dynamic balances (Starts at 0.00 for new user, updated via Firebase Realtime listener)
    private val _walletBalanceUsdt = MutableStateFlow(0.00)
    val walletBalanceUsdt: StateFlow<Double> = _walletBalanceUsdt.asStateFlow()

    // Atomic Escrow Balance (Locked in 24h Audit)
    private val _lockedAuditBalanceUsdt = MutableStateFlow(0.0)
    val lockedAuditBalanceUsdt: StateFlow<Double> = _lockedAuditBalanceUsdt.asStateFlow()

    // Device Fingerprinting & IP Limit (Max 1 Free Ad Plan session per 24 hours)
    val deviceFingerprint = "DEV-ARM64-IS8921"
    private val _lastFreeAdSessionTimestamp = MutableStateFlow<Long?>(null)
    val lastFreeAdSessionTimestamp: StateFlow<Long?> = _lastFreeAdSessionTimestamp.asStateFlow()

    private val _twoFactorEnabled = MutableStateFlow(true)
    val twoFactorEnabled: StateFlow<Boolean> = _twoFactorEnabled.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    // --- Gamification: Lucky Wheel ---
    private val _canSpinToday = MutableStateFlow(true)
    val canSpinToday: StateFlow<Boolean> = _canSpinToday.asStateFlow()

    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _spinResultText = MutableStateFlow<String?>(null)
    val spinResultText: StateFlow<String?> = _spinResultText.asStateFlow()

    // --- UI Modals State ---
    val showLuckyWheelModal = MutableStateFlow(false)
    val showDepositModal = MutableStateFlow(false)
    val showWithdrawModal = MutableStateFlow(false)
    val showAuditDossierModal = MutableStateFlow(false)
    val showSyndicateModal = MutableStateFlow(false)
    val showAiSupportModal = MutableStateFlow(false)
    val showLanguageModal = MutableStateFlow(false)
    val showNotificationSheet = MutableStateFlow(false)
    val showCreatorMilestoneModal = MutableStateFlow(false)

    // --- In-App Auto Update State ---
    val updateStatus: StateFlow<UpdateStatus> = AppUpdateManager.updateStatus

    // --- Marketplace Plans ---
    val marketplacePlans = listOf(
        MiningPlan(
            id = "plan_kas",
            name = "Starter Kaspa Node",
            subtitle = "KHeavyHash ASIC Array",
            cryptoSymbol = "KAS",
            iconCrypto = "⚡",
            minDepositUsdt = 100.0,
            hashPowerGh = 25.0,
            monthlyYieldPercent = 14.5,
            termDays = 30,
            dailyYieldUsdtEst = 0.48,
            hardwareType = "IceRiver KS0 Ultra Liquid",
            tag = "Low Entry"
        ),
        MiningPlan(
            id = "plan_btc",
            name = "Prime BTC Hydro",
            subtitle = "SHA-256 Hydro Immersion",
            cryptoSymbol = "BTC",
            iconCrypto = "₿",
            minDepositUsdt = 300.0,
            hashPowerGh = 3150.0,
            monthlyYieldPercent = 16.0,
            termDays = 30,
            dailyYieldUsdtEst = 1.60,
            hardwareType = "Antminer S21 Hydro (Sub-Zero)",
            tag = "Most Popular"
        ),
        MiningPlan(
            id = "plan_free_ad",
            name = "Free 4-Hour Mining Ad Node",
            subtitle = "Sponsored Micro Hashrate Booster",
            cryptoSymbol = "BTC",
            iconCrypto = "⚡",
            minDepositUsdt = 0.0,
            hashPowerGh = 50.0,
            monthlyYieldPercent = 0.0,
            termDays = 1,
            dailyYieldUsdtEst = 0.15,
            hardwareType = "Micro Hydro Shared Pool",
            tag = "Free Ad Booster"
        ),
        MiningPlan(
            id = "plan_institutional",
            name = "Institutional Geothermal Cluster",
            subtitle = "Direct Volcano Sub-Zero Connection",
            cryptoSymbol = "BTC",
            iconCrypto = "🌋",
            minDepositUsdt = 1000.0,
            hashPowerGh = 12500.0,
            monthlyYieldPercent = 19.5,
            termDays = 60,
            dailyYieldUsdtEst = 6.50,
            hardwareType = "Dedicated Whatsminer M63S Container",
            tag = "Institutional"
        )
    )

    // Dynamic Contracts list
    private val _activeContracts = MutableStateFlow<List<ActiveContract>>(emptyList())
    val activeContracts: StateFlow<List<ActiveContract>> = _activeContracts.asStateFlow()

    // Dynamic Activity List
    private val _activityList = MutableStateFlow<List<ActivityItem>>(emptyList())
    val activityList: StateFlow<List<ActivityItem>> = _activityList.asStateFlow()

    // Dynamic Payouts History
    private val _payoutsList = MutableStateFlow<List<PayoutItem>>(emptyList())
    val payoutsList: StateFlow<List<PayoutItem>> = _payoutsList.asStateFlow()

    // --- Bounty Tasks List ---
    private val _bountyTasks = MutableStateFlow(
        listOf(
            BountyTask(
                id = "bt_01",
                type = BountyType.WHATSAPP,
                title = "WhatsApp Status Verification",
                description = "Post the official HashGrid promotional poster on your WhatsApp status for 24 hours.",
                rewardUsdt = 5.00,
                status = BountyStatus.AVAILABLE
            ),
            BountyTask(
                id = "bt_02",
                type = BountyType.TELEGRAM,
                title = "Telegram Global Syndicate Community",
                description = "Join the official HashGrid announcements and institutional miners group.",
                rewardUsdt = 3.00,
                status = BountyStatus.AVAILABLE
            )
        )
    )
    val bountyTasks: StateFlow<List<BountyTask>> = _bountyTasks.asStateFlow()

    private val _creatorSubmissions = MutableStateFlow<List<CreatorMilestoneSubmission>>(emptyList())
    val creatorSubmissions: StateFlow<List<CreatorMilestoneSubmission>> = _creatorSubmissions.asStateFlow()

    // Live Tickers
    val liveTickers: StateFlow<List<LiveTickerItem>> = BinanceWebSocketService.tickers
    val isWsConnected: StateFlow<Boolean> = BinanceWebSocketService.isConnected
    val wsStatusText: StateFlow<String> = BinanceWebSocketService.connectionStatusText

    // In-app Notifications
    val notifications = listOf(
        "⚡ Sub-Zero Geothermal Cluster online: 99.98% uptime.",
        "🔒 256-Bit Multi-Sig Escrow is actively monitoring withdrawal batches.",
        "🎁 Daily Lucky Wheel has reset. Claim your bonus."
    )
    private val _unreadNotificationsCount = MutableStateFlow(3)
    val unreadNotificationsCount: StateFlow<Int> = _unreadNotificationsCount.asStateFlow()

    // AI Chat Support
    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "ai_welcome",
                text = "Welcome to HashGrid Institutional Support. I can help answer questions regarding our sub-zero geothermal mining clusters, active contracts, payouts, and cold storage security.",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping: StateFlow<Boolean> = _isAiTyping.asStateFlow()

    // Mining Session countdown
    private val _miningSessionEndTimestamp = MutableStateFlow(System.currentTimeMillis() + (14L * 3600 * 1000 + 22L * 60 * 1000))
    val miningSessionEndTimestamp: StateFlow<Long> = _miningSessionEndTimestamp.asStateFlow()

    init {
        BinanceWebSocketService.start()

        viewModelScope.launch {
            BinanceWebSocketService.tickers.collect { tickers ->
                tickers.find { it.id.equals("BTCUSDT", ignoreCase = true) || it.id.equals("btc", ignoreCase = true) }?.let { btcTicker ->
                    _btcPrice.value = btcTicker.price
                }
                tickers.find { it.id.equals("KASUSDT", ignoreCase = true) || it.id.equals("kas", ignoreCase = true) }?.let { kasTicker ->
                    _kasPrice.value = kasTicker.price
                }
            }
        }

        // Attach listener for currently logged in user
        viewModelScope.launch {
            AuthService.currentUser.collect { user ->
                if (user != null && user.id.isNotBlank()) {
                    FirebaseSyncService.startRealtimeBalanceListener(user.id) { remoteBal ->
                        _walletBalanceUsdt.value = remoteBal
                    }
                } else {
                    _walletBalanceUsdt.value = 0.00
                    _hashPower.value = 0.0
                    _activeContracts.value = emptyList()
                    _activityList.value = emptyList()
                    _payoutsList.value = emptyList()
                }
            }
        }
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun setPlansSubTab(index: Int) {
        _plansSubTab.value = index
    }

    fun setWalletSubTab(index: Int) {
        _walletSubTab.value = index
    }

    fun clearNotifications() {
        _unreadNotificationsCount.value = 0
    }

    fun toggle2FA() {
        _twoFactorEnabled.value = !_twoFactorEnabled.value
    }

    fun selectLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    fun extendMiningSession() {
        _miningSessionEndTimestamp.value = System.currentTimeMillis() + (24L * 3600 * 1000)
    }

    fun getFreeAdCooldownHoursRemaining(): Int {
        val lastTs = _lastFreeAdSessionTimestamp.value ?: return 0
        val diffMs = System.currentTimeMillis() - lastTs
        val twentyFourHoursMs = 24L * 3600 * 1000
        val remainingMs = twentyFourHoursMs - diffMs
        return if (remainingMs > 0) ((remainingMs / (3600 * 1000)).toInt() + 1) else 0
    }

    fun claimFreeAdSession(): Pair<Boolean, String> {
        val remaining = getFreeAdCooldownHoursRemaining()
        if (remaining > 0) {
            return Pair(false, "Device cooldown active: $remaining hours remaining before next free session.")
        }
        _lastFreeAdSessionTimestamp.value = System.currentTimeMillis()
        _hashPower.value += 50.0

        val newContract = ActiveContract(
            id = "c_${UUID.randomUUID().toString().take(6)}",
            planName = "Free 4-Hour Mining Ad Node",
            cryptoSymbol = "BTC",
            depositUsdt = 0.0,
            hashPowerGh = 50.0,
            elapsedDays = 0,
            totalDays = 1,
            accruedProfitUsdt = 0.0,
            dailyYieldUsdt = 0.15,
            isRestakeEnabled = false,
            startDateStr = "Today",
            maturityDateStr = "Tomorrow"
        )
        _activeContracts.value = listOf(newContract) + _activeContracts.value

        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "+0.15 USDT",
            subtitle = "Free 4h Ad Session Micro Yield",
            btcAmountStr = "+0.00000170 BTC",
            usdtAmount = 0.15,
            timestampStr = "Just now",
            isCredit = true
        )
        _activityList.value = listOf(newAct) + _activityList.value
        return Pair(true, "Free 4-Hour Ad Node activated! +50 GH/s hashrate credited.")
    }

    fun activatePlan(plan: MiningPlan): Boolean {
        if (_walletBalanceUsdt.value < plan.minDepositUsdt) {
            return false
        }
        _walletBalanceUsdt.value -= plan.minDepositUsdt
        _hashPower.value += plan.hashPowerGh

        val newContract = ActiveContract(
            id = "c_${UUID.randomUUID().toString().take(6)}",
            planName = plan.name,
            cryptoSymbol = plan.cryptoSymbol,
            depositUsdt = plan.minDepositUsdt,
            hashPowerGh = plan.hashPowerGh,
            elapsedDays = 0,
            totalDays = plan.termDays,
            accruedProfitUsdt = 0.0,
            dailyYieldUsdt = plan.dailyYieldUsdtEst,
            isRestakeEnabled = false,
            startDateStr = "Today",
            maturityDateStr = "In ${plan.termDays} Days"
        )
        _activeContracts.value = listOf(newContract) + _activeContracts.value

        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "-${plan.minDepositUsdt} USDT",
            subtitle = "Activated ${plan.name}",
            btcAmountStr = "-${"%.6f".format(plan.minDepositUsdt / _btcPrice.value)} BTC",
            usdtAmount = plan.minDepositUsdt,
            timestampStr = "Just now",
            isCredit = false
        )
        _activityList.value = listOf(newAct) + _activityList.value
        return true
    }

    fun toggleRestake(contractId: String) {
        _activeContracts.value = _activeContracts.value.map {
            if (it.id == contractId) {
                it.copy(isRestakeEnabled = !it.isRestakeEnabled)
            } else it
        }
    }

    fun onDepositSuccess(amountUsdt: Double, txId: String) {
        _walletBalanceUsdt.value += amountUsdt
        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "+$amountUsdt USDT",
            subtitle = "NOWPayments Confirmed (${txId.take(8)}...)",
            btcAmountStr = "+${"%.6f".format(amountUsdt / _btcPrice.value)} BTC",
            usdtAmount = amountUsdt,
            timestampStr = "Just now",
            isCredit = true
        )
        _activityList.value = listOf(newAct) + _activityList.value

        FirebaseSyncService.pushDeposit(
            FirebaseDeposit(
                paymentId = txId,
                userId = userId,
                amount = amountUsdt,
                currency = "USDT",
                status = "confirmed",
                timestamp = FirebaseSyncService.getCurrentTimestamp()
            )
        )
    }

    fun requestWithdrawal(amountUsdt: Double, address: String, network: String): String? {
        if (amountUsdt > _walletBalanceUsdt.value) {
            return "Insufficient available balance ($${"%.2f".format(_walletBalanceUsdt.value)} USDT available)."
        }
        if (amountUsdt < 10.0) {
            return "Minimum withdrawal amount is 10.00 USDT."
        }
        if (address.isBlank() || address.length < 10) {
            return "Please provide a valid $network wallet address."
        }

        _walletBalanceUsdt.value -= amountUsdt
        _lockedAuditBalanceUsdt.value += amountUsdt

        val reqId = "wd_${System.currentTimeMillis().toString().takeLast(6)}"
        val payoutItem = PayoutItem(
            id = reqId,
            dateStr = "Today",
            amountUsdt = amountUsdt,
            targetAddress = address.take(6) + "..." + address.takeLast(4),
            network = network,
            status = PayoutStatus.PENDING_24H_AUDIT
        )
        _payoutsList.value = listOf(payoutItem) + _payoutsList.value

        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "-$amountUsdt USDT",
            subtitle = "Audited Multi-Sig Escrow ($network)",
            btcAmountStr = "-${"%.6f".format(amountUsdt / _btcPrice.value)} BTC",
            usdtAmount = amountUsdt,
            timestampStr = "Just now",
            isCredit = false
        )
        _activityList.value = listOf(newAct) + _activityList.value

        FirebaseSyncService.pushWithdrawal(
            FirebaseWithdrawal(
                requestId = reqId,
                userId = userId,
                amount = amountUsdt,
                cryptoAddress = address,
                network = network,
                status = "pending",
                timestamp = FirebaseSyncService.getCurrentTimestamp()
            )
        )
        return null
    }

    fun executeSpin(onResult: (prizeAmount: Double, prizeType: String, message: String) -> Unit) {
        if (!_canSpinToday.value || _isSpinning.value) return

        _isSpinning.value = true
        _canSpinToday.value = false

        viewModelScope.launch {
            kotlinx.coroutines.delay(2000L)
            _isSpinning.value = false

            val prizeAmount = 1.50
            val prizeType = "USDT"
            _walletBalanceUsdt.value += prizeAmount
            _spinResultText.value = "+$prizeAmount USDT Prize Credited!"

            val newAct = ActivityItem(
                id = "act_${System.currentTimeMillis()}",
                title = "+$prizeAmount USDT",
                subtitle = "Daily Lucky Wheel Reward",
                btcAmountStr = "+${"%.6f".format(prizeAmount / _btcPrice.value)} BTC",
                usdtAmount = prizeAmount,
                timestampStr = "Just now",
                isCredit = true
            )
            _activityList.value = listOf(newAct) + _activityList.value
            onResult(prizeAmount, prizeType, "+$prizeAmount USDT Prize Credited!")
        }
    }

    fun submitCreatorMilestone(channelUrl: String, videoUrl: String, contactTelegram: String): String? {
        if (channelUrl.isBlank() || videoUrl.isBlank()) {
            return "Please provide both channel and video review URLs."
        }
        val sub = CreatorMilestoneSubmission(
            id = "ms_${System.currentTimeMillis().toString().takeLast(6)}",
            userId = userId,
            channelUrl = channelUrl.trim(),
            videoUrl = videoUrl.trim(),
            contactTelegram = contactTelegram.trim(),
            submittedAt = "Just now",
            status = MilestoneStatus.PENDING_EXECUTIVE_AUDIT
        )
        _creatorSubmissions.value = listOf(sub) + _creatorSubmissions.value
        return null
    }

    fun submitWhatsAppBounty(viewsCount: String, timePosted: String): String? {
        val trimmedViews = viewsCount.trim()
        val num = trimmedViews.toIntOrNull()
        if (num == null || num < 10) {
            return "Minimum 10 status views required for verification."
        }

        val claimId = "claim_wa_${System.currentTimeMillis()}"
        _bountyTasks.value = _bountyTasks.value.map {
            if (it.type == BountyType.WHATSAPP) {
                it.copy(
                    status = BountyStatus.PENDING_ADMIN_REVIEW,
                    submissionProof = "Status Views: $trimmedViews (Posted: $timePosted)",
                    rejectionReason = null
                )
            } else it
        }

        FirebaseSyncService.submitTaskClaim(
            FirebaseTaskClaim(
                claimId = claimId,
                userId = userId,
                userEmail = userEmail,
                deviceId = "dev_${userId.takeLast(6)}",
                taskId = "task_whatsapp_status",
                taskTitle = "WhatsApp Status Bounty",
                proofLink = "Status Views: $trimmedViews (Posted: $timePosted)",
                requestedAmountUsdt = 5.00,
                status = "PENDING",
                timestamp = FirebaseSyncService.getCurrentTimestamp()
            )
        )

        return null
    }

    fun submitTelegramBounty(telegramUsername: String): String? {
        val trimmed = telegramUsername.trim()
        if (trimmed.length < 3) {
            return "Please enter a valid Telegram username or handle."
        }
        val handle = if (trimmed.startsWith("@")) trimmed else "@$trimmed"

        val claimId = "claim_tg_${System.currentTimeMillis()}"
        _bountyTasks.value = _bountyTasks.value.map {
            if (it.type == BountyType.TELEGRAM) {
                it.copy(
                    status = BountyStatus.PENDING_ADMIN_REVIEW,
                    submissionProof = handle,
                    rejectionReason = null
                )
            } else it
        }

        FirebaseSyncService.submitTaskClaim(
            FirebaseTaskClaim(
                claimId = claimId,
                userId = userId,
                userEmail = userEmail,
                deviceId = "dev_${userId.takeLast(6)}",
                taskId = "task_telegram_join",
                taskTitle = "Telegram Global Syndicate Community",
                proofLink = handle,
                requestedAmountUsdt = 3.00,
                status = "PENDING",
                timestamp = FirebaseSyncService.getCurrentTimestamp()
            )
        )

        return null
    }

    // --- AI Support Chat ---
    fun sendChatMessage(query: String) {
        if (query.isBlank()) return
        val userMsg = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            text = query,
            isUser = true
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiTyping.value = true

        viewModelScope.launch {
            val responseText = GeminiSupportService.askGemini(query)
            _isAiTyping.value = false
            val aiMsg = ChatMessage(
                id = "ai_${System.currentTimeMillis()}",
                text = responseText,
                isUser = false
            )
            _chatMessages.value = _chatMessages.value + aiMsg
        }
    }

    // --- Authentication Actions ---
    fun login(context: Context, email: String, pass: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = AuthService.loginWithEmail(context, email, pass)
            if (res.isSuccess) {
                val user = res.getOrThrow()
                FirebaseSyncService.startRealtimeBalanceListener(user.id) { remoteBal ->
                    _walletBalanceUsdt.value = remoteBal
                }
                onComplete(Result.success(Unit))
            } else {
                onComplete(Result.failure(res.exceptionOrNull() ?: Exception("Login failed")))
            }
        }
    }

    fun signUp(context: Context, name: String, email: String, pass: String, confirmPass: String, refCode: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = AuthService.signUpWithEmail(context, name, email, pass, confirmPass, refCode)
            if (res.isSuccess) {
                val user = res.getOrThrow()
                FirebaseSyncService.startRealtimeBalanceListener(user.id) { remoteBal ->
                    _walletBalanceUsdt.value = remoteBal
                }
                onComplete(Result.success(Unit))
            } else {
                onComplete(Result.failure(res.exceptionOrNull() ?: Exception("Registration failed")))
            }
        }
    }

    fun signInWithGoogle(context: Context, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = AuthService.signInWithGoogleCredential(context)
            if (res.isSuccess) {
                val user = res.getOrThrow()
                FirebaseSyncService.startRealtimeBalanceListener(user.id) { remoteBal ->
                    _walletBalanceUsdt.value = remoteBal
                }
                onComplete(Result.success(Unit))
            } else {
                onComplete(Result.failure(res.exceptionOrNull() ?: Exception("Google sign-in failed")))
            }
        }
    }

    fun logout() {
        AuthService.logout()
        _walletBalanceUsdt.value = 0.00
        _hashPower.value = 0.0
        _activeContracts.value = emptyList()
        _activityList.value = emptyList()
        _payoutsList.value = emptyList()
    }

    // --- App Update Actions ---
    fun checkForUpdates(context: Context? = null) {
        AppUpdateManager.checkForUpdates(context, isManualCheck = true)
    }

    fun startAppUpdate(context: Context, info: AppUpdateInfo) {
        AppUpdateManager.startDownload(context, info)
    }

    fun installAppUpdate(context: Context, apkFile: File) {
        AppUpdateManager.triggerInstall(context, apkFile)
    }

    fun dismissAppUpdate() {
        AppUpdateManager.dismissUpdate()
    }

    override fun onCleared() {
        super.onCleared()
        BinanceWebSocketService.stop()
    }
}
