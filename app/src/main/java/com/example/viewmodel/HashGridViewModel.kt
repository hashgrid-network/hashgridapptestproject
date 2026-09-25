package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ActiveContract
import com.example.model.ActivityItem
import com.example.model.AdminBountyClaim
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
import java.util.Locale
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

    private val _kycStatus = MutableStateFlow("UNVERIFIED")
    val kycStatus: StateFlow<String> = _kycStatus.asStateFlow()

    private val _twoFactorEnabled = MutableStateFlow(false)
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
    val showAdminVerificationModal = MutableStateFlow(false)
    val showKycModal = MutableStateFlow(false)
    val auditDossierInitialTab = MutableStateFlow(0)

    // Anti-fraud Registry for YouTube submissions
    private val _submittedYoutubeUrls = mutableSetOf<String>()

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

    // --- REVISED BOUNTY TASKS LIST ---
    private val _bountyTasks = MutableStateFlow(
        listOf(
            BountyTask(
                id = "bt_whatsapp",
                type = BountyType.WHATSAPP,
                title = "WhatsApp Status Broadcasting",
                description = "Broadcast official HashGrid Arctic Geothermal proof with your referral link on your WhatsApp status for 24 hours. Attach daily screenshot proof.",
                rewardUsdt = 0.20,
                status = BountyStatus.AVAILABLE
            ),
            BountyTask(
                id = "bt_telegram",
                type = BountyType.TELEGRAM,
                title = "Telegram Syndicate Community",
                description = "Join the official HashGrid announcements and institutional miners syndicate group. Submit your Telegram handle.",
                rewardUsdt = 0.20,
                status = BountyStatus.AVAILABLE
            ),
            BountyTask(
                id = "bt_youtube",
                type = BountyType.YOUTUBE,
                title = "YouTube Video Showcase Bounty",
                description = "Create an authentic public video review/showcase of HashGrid (min 2 minutes). Mandatory: Your HashGrid User ID and referral link MUST be in description.",
                rewardUsdt = 5.00,
                status = BountyStatus.AVAILABLE
            )
        )
    )
    val bountyTasks: StateFlow<List<BountyTask>> = _bountyTasks.asStateFlow()

    // --- ADMIN VERIFICATION BOUNTY QUEUE ---
    private val _adminBountyClaims = MutableStateFlow<List<AdminBountyClaim>>(
        listOf(
            AdminBountyClaim(
                id = "claim_demo_01",
                userId = "HG-779842",
                userEmail = "miner.scandinavia@grid.is",
                taskType = BountyType.YOUTUBE,
                taskTitle = "YouTube Video Showcase Bounty",
                rewardUsdt = 5.00,
                submissionProof = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                youtubeVideoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                youtubeChannelName = "Nordic Crypto Review",
                status = BountyStatus.PENDING_ADMIN_REVIEW,
                timestamp = "Today, 10:15 AM"
            ),
            AdminBountyClaim(
                id = "claim_demo_02",
                userId = "HG-918231",
                userEmail = "alex.crypto@nordic.no",
                taskType = BountyType.WHATSAPP,
                taskTitle = "WhatsApp Status Broadcasting",
                rewardUsdt = 0.20,
                submissionProof = "Status Views: 64 views (Posted: Today 08:30)",
                whatsappViews = "64 views",
                status = BountyStatus.PENDING_ADMIN_REVIEW,
                timestamp = "Today, 08:45 AM"
            )
        )
    )
    val adminBountyClaims: StateFlow<List<AdminBountyClaim>> = _adminBountyClaims.asStateFlow()

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
        try {
            BinanceWebSocketService.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        viewModelScope.launch {
            try {
                BinanceWebSocketService.tickers.collect { tickers ->
                    try {
                        tickers.find { it.id.equals("BTCUSDT", ignoreCase = true) || it.id.equals("btc", ignoreCase = true) }?.let { btcTicker ->
                            _btcPrice.value = btcTicker.price
                        }
                        tickers.find { it.id.equals("KASUSDT", ignoreCase = true) || it.id.equals("kas", ignoreCase = true) }?.let { kasTicker ->
                            _kasPrice.value = kasTicker.price
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        // Attach listener for currently logged in user
        viewModelScope.launch {
            try {
                AuthService.currentUser.collect { user ->
                    try {
                        if (user != null && user.id.isNotBlank()) {
                            FirebaseSyncService.listenFirestoreUser(
                                userId = user.id,
                                onProfileUpdated = { remoteBal, kyc, twoFa ->
                                    _walletBalanceUsdt.value = remoteBal
                                    _kycStatus.value = kyc
                                    _twoFactorEnabled.value = twoFa
                                },
                                onTransactionsUpdated = { acts, payouts ->
                                    if (acts.isNotEmpty()) _activityList.value = acts
                                    if (payouts.isNotEmpty()) _payoutsList.value = payouts
                                },
                                onMinersUpdated = { miners ->
                                    if (miners.isNotEmpty()) {
                                        _activeContracts.value = miners
                                        _hashPower.value = miners.sumOf { it.hashPowerGh }
                                    }
                                },
                                onNotificationsCountUpdated = { count ->
                                    _unreadNotificationsCount.value = count
                                }
                            )
                        } else {
                            _walletBalanceUsdt.value = 0.00
                            _hashPower.value = 0.0
                            _activeContracts.value = emptyList()
                            _activityList.value = emptyList()
                            _payoutsList.value = emptyList()
                            _kycStatus.value = "UNVERIFIED"
                            _twoFactorEnabled.value = false
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
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
        val newVal = !_twoFactorEnabled.value
        _twoFactorEnabled.value = newVal
        FirebaseSyncService.update2FA(userId, newVal)
    }

    fun submitKyc(fullName: String, idType: String, idNumber: String) {
        _kycStatus.value = "PENDING REVIEW"
        FirebaseSyncService.updateKycStatus(userId, fullName, idType, idNumber)
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

        val minerId = "miner_${UUID.randomUUID().toString().take(6)}"
        val newContract = ActiveContract(
            id = minerId,
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
            title = "-${String.format(Locale.US, "%.2f", plan.minDepositUsdt)} USDT",
            subtitle = "Activated ${plan.name} (${plan.hashPowerGh.toInt()} GH/s)",
            btcAmountStr = "Contract Deployed",
            usdtAmount = plan.minDepositUsdt,
            timestampStr = "Just now",
            isCredit = false
        )
        _activityList.value = listOf(newAct) + _activityList.value

        // Sync miner & transaction document to Firestore
        FirebaseSyncService.purchaseMiningPlan(userId, plan) {}
        return true
    }

    fun toggleRestake(contractId: String) {
        _activeContracts.value = _activeContracts.value.map {
            if (it.id == contractId) it.copy(isRestakeEnabled = !it.isRestakeEnabled) else it
        }
    }

    // --- Lucky Wheel Spin ---
    fun spinLuckyWheel(onResult: (Double, String) -> Unit) {
        if (!_canSpinToday.value || _isSpinning.value) return
        _isSpinning.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(2200L)
            val rewards = listOf(0.10, 0.25, 0.50, 1.00, 2.00, 5.00)
            val chosen = rewards.random()
            _walletBalanceUsdt.value += chosen
            _canSpinToday.value = false
            _isSpinning.value = false
            val text = "+$chosen USDT Credited to Segregated Wallet"
            _spinResultText.value = text

            val newAct = ActivityItem(
                id = "act_${System.currentTimeMillis()}",
                title = "+$chosen USDT",
                subtitle = "Daily Lucky Wheel Prize",
                btcAmountStr = "+0.00000${(chosen * 12).toInt()} BTC",
                usdtAmount = chosen,
                timestampStr = "Just now",
                isCredit = true
            )
            _activityList.value = listOf(newAct) + _activityList.value
            onResult(chosen, text)
        }
    }

    // --- Deposit & Withdraw ---
    fun submitDeposit(amountUsdt: Double, network: String, txHash: String, depositAddress: String) {
        FirebaseSyncService.submitDepositTxId(
            userId = userId,
            userEmail = userEmail,
            amount = amountUsdt,
            network = network,
            txHash = txHash,
            depositAddress = depositAddress
        ) { success ->
            if (success) {
                val newAct = ActivityItem(
                    id = "dep_${System.currentTimeMillis().toString().takeLast(6)}",
                    title = "+$${String.format(Locale.US, "%.2f", amountUsdt)} USDT",
                    subtitle = "USDT ($network) Deposit Pending Verification",
                    btcAmountStr = "",
                    usdtAmount = amountUsdt,
                    timestampStr = "Just now",
                    isCredit = true
                )
                _activityList.value = listOf(newAct) + _activityList.value
            }
        }
    }

    fun processDeposit(amountUsdt: Double, txHash: String, currency: String): Boolean {
        if (amountUsdt <= 0) return false
        val newDep = FirebaseDeposit(
            paymentId = "dep_${System.currentTimeMillis().toString().takeLast(6)}",
            userId = userId,
            amount = amountUsdt,
            currency = currency,
            status = "confirmed",
            timestamp = FirebaseSyncService.getCurrentTimestamp()
        )
        FirebaseSyncService.pushDeposit(newDep)
        _walletBalanceUsdt.value += amountUsdt

        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "+${String.format(Locale.US, "%.2f", amountUsdt)} USDT",
            subtitle = "Deposit via $currency (Confirmed)",
            btcAmountStr = "+${String.format(Locale.US, "%.6f", amountUsdt / _btcPrice.value)} BTC",
            usdtAmount = amountUsdt,
            timestampStr = "Just now",
            isCredit = true
        )
        _activityList.value = listOf(newAct) + _activityList.value
        return true
    }

    fun requestWithdrawal(amountUsdt: Double, address: String, network: String): String? {
        if (amountUsdt < 130.0) {
            return "Minimum withdrawal threshold is 130.00 USDT."
        }
        if (amountUsdt > _walletBalanceUsdt.value) {
            return "Insufficient balance."
        }
        if (address.isBlank() || address.length < 15) {
            return "Invalid destination wallet address."
        }

        _walletBalanceUsdt.value -= amountUsdt
        _lockedAuditBalanceUsdt.value += amountUsdt

        val reqId = "w_${UUID.randomUUID().toString().take(6)}"
        val newPayout = PayoutItem(
            id = reqId,
            dateStr = "Today",
            amountUsdt = amountUsdt,
            targetAddress = address.take(6) + "..." + address.takeLast(4),
            network = network,
            status = PayoutStatus.PENDING_24H_AUDIT
        )
        _payoutsList.value = listOf(newPayout) + _payoutsList.value

        FirebaseSyncService.submitWithdrawal(
            userId = userId,
            userEmail = userEmail,
            amount = amountUsdt,
            cryptoAddress = address,
            network = network
        ) {}

        return null
    }

    // --- BOUNTY SUBMISSIONS & ANTI-FRAUD LOGIC ---

    fun submitYouTubeBounty(videoUrl: String, channelName: String): String? {
        val cleanUrl = videoUrl.trim()
        val cleanChannel = channelName.trim()

        if (cleanChannel.isBlank()) {
            return "Please enter your YouTube channel name."
        }
        if (cleanUrl.isBlank()) {
            return "Please enter your YouTube video URL."
        }

        // Auto-validation: Validate YouTube URL format
        val isYoutubeFormat = cleanUrl.contains("youtube.com/watch?v=", ignoreCase = true) ||
                cleanUrl.contains("youtu.be/", ignoreCase = true) ||
                cleanUrl.contains("youtube.com/shorts/", ignoreCase = true)

        if (!isYoutubeFormat) {
            return "Invalid YouTube URL format. Must be youtube.com/watch?v= or youtu.be/"
        }

        // Anti-Fraud Database Constraint: Prevent duplicate submissions
        if (_submittedYoutubeUrls.contains(cleanUrl.lowercase())) {
            return "This exact YouTube video has already been submitted across the database."
        }
        _submittedYoutubeUrls.add(cleanUrl.lowercase())

        val claimId = "claim_yt_${System.currentTimeMillis()}"

        // Update local task state
        _bountyTasks.value = _bountyTasks.value.map {
            if (it.type == BountyType.YOUTUBE) {
                it.copy(
                    status = BountyStatus.PENDING_ADMIN_REVIEW,
                    submissionProof = cleanUrl,
                    rejectionReason = null
                )
            } else it
        }

        // Add to Admin Review Queue
        val newAdminClaim = AdminBountyClaim(
            id = claimId,
            userId = userId,
            userEmail = userEmail,
            taskType = BountyType.YOUTUBE,
            taskTitle = "YouTube Video Showcase Bounty",
            rewardUsdt = 5.00,
            submissionProof = cleanUrl,
            youtubeVideoUrl = cleanUrl,
            youtubeChannelName = cleanChannel,
            status = BountyStatus.PENDING_ADMIN_REVIEW,
            timestamp = "Just now"
        )
        _adminBountyClaims.value = listOf(newAdminClaim) + _adminBountyClaims.value

        // Sync to Firebase
        FirebaseSyncService.submitTaskClaim(
            FirebaseTaskClaim(
                claimId = claimId,
                userId = userId,
                userEmail = userEmail,
                deviceId = "dev_${userId.takeLast(6)}",
                taskId = "task_youtube_showcase",
                taskTitle = "YouTube Video Showcase Bounty",
                proofLink = "Channel: $cleanChannel | URL: $cleanUrl",
                requestedAmountUsdt = 5.00,
                status = "PENDING",
                timestamp = FirebaseSyncService.getCurrentTimestamp()
            )
        )

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

        val newAdminClaim = AdminBountyClaim(
            id = claimId,
            userId = userId,
            userEmail = userEmail,
            taskType = BountyType.WHATSAPP,
            taskTitle = "WhatsApp Status Broadcasting",
            rewardUsdt = 0.20,
            submissionProof = "Status Views: $trimmedViews (Posted: $timePosted)",
            whatsappViews = "$trimmedViews views",
            status = BountyStatus.PENDING_ADMIN_REVIEW,
            timestamp = "Just now"
        )
        _adminBountyClaims.value = listOf(newAdminClaim) + _adminBountyClaims.value

        FirebaseSyncService.submitTaskClaim(
            FirebaseTaskClaim(
                claimId = claimId,
                userId = userId,
                userEmail = userEmail,
                deviceId = "dev_${userId.takeLast(6)}",
                taskId = "task_whatsapp_status",
                taskTitle = "WhatsApp Status Broadcasting",
                proofLink = "Status Views: $trimmedViews (Posted: $timePosted)",
                requestedAmountUsdt = 0.20,
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

        val newAdminClaim = AdminBountyClaim(
            id = claimId,
            userId = userId,
            userEmail = userEmail,
            taskType = BountyType.TELEGRAM,
            taskTitle = "Telegram Syndicate Community",
            rewardUsdt = 0.20,
            submissionProof = handle,
            telegramHandle = handle,
            status = BountyStatus.PENDING_ADMIN_REVIEW,
            timestamp = "Just now"
        )
        _adminBountyClaims.value = listOf(newAdminClaim) + _adminBountyClaims.value

        FirebaseSyncService.submitTaskClaim(
            FirebaseTaskClaim(
                claimId = claimId,
                userId = userId,
                userEmail = userEmail,
                deviceId = "dev_${userId.takeLast(6)}",
                taskId = "task_telegram_join",
                taskTitle = "Telegram Syndicate Community",
                proofLink = handle,
                requestedAmountUsdt = 0.20,
                status = "PENDING",
                timestamp = FirebaseSyncService.getCurrentTimestamp()
            )
        )

        return null
    }

    // --- ADMIN ACTIONS: APPROVE / REJECT BOUNTY ---

    fun approveBountyClaim(claim: AdminBountyClaim) {
        _adminBountyClaims.value = _adminBountyClaims.value.map {
            if (it.id == claim.id) it.copy(status = BountyStatus.APPROVED_CREDITED) else it
        }

        _bountyTasks.value = _bountyTasks.value.map {
            if (it.type == claim.taskType) it.copy(status = BountyStatus.APPROVED_CREDITED) else it
        }

        // Credit User Balance
        _walletBalanceUsdt.value += claim.rewardUsdt

        // Activity log
        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "+${String.format(Locale.US, "%.2f", claim.rewardUsdt)} USDT",
            subtitle = "${claim.taskTitle} Bonus Voucher Credited",
            btcAmountStr = "+0.00000${(claim.rewardUsdt * 12).toInt()} BTC",
            usdtAmount = claim.rewardUsdt,
            timestampStr = "Just now",
            isCredit = true
        )
        _activityList.value = listOf(newAct) + _activityList.value

        // Sync with Firebase User
        FirebaseSyncService.syncUser(
            FirebaseUser(
                uid = userId,
                email = userEmail,
                walletBalance = _walletBalanceUsdt.value,
                miningRate = "${_hashPower.value.toInt()} GH/s",
                createdAt = FirebaseSyncService.getCurrentTimestamp()
            )
        )
    }

    fun rejectBountyClaim(claim: AdminBountyClaim, reason: String) {
        _adminBountyClaims.value = _adminBountyClaims.value.map {
            if (it.id == claim.id) it.copy(status = BountyStatus.REJECTED, rejectionReason = reason) else it
        }

        _bountyTasks.value = _bountyTasks.value.map {
            if (it.type == claim.taskType) it.copy(status = BountyStatus.REJECTED, rejectionReason = reason) else it
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

    fun onDirectAuthSuccess(user: User) {
        FirebaseSyncService.startRealtimeBalanceListener(user.id) { remoteBal ->
            _walletBalanceUsdt.value = remoteBal
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
