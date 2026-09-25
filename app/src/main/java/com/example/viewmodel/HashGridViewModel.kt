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
import com.example.model.User
import com.example.service.AppUpdateInfo
import com.example.service.AppUpdateManager
import com.example.service.BinanceWebSocketService
import com.example.service.FirebaseDeposit
import com.example.service.FirebaseSyncService
import com.example.service.FirebaseUser
import com.example.service.FirebaseWithdrawal
import com.example.service.GeminiSupportService
import com.example.service.UpdateStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class HashGridViewModel : ViewModel() {

    // --- Navigation & Sub-Tabs ---
    private val _currentTab = MutableStateFlow(0) // 0:Home, 1:Plans, 2:Wallet, 3:Growth, 4:Account
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _plansSubTab = MutableStateFlow(0) // 0: Marketplace, 1: Active
    val plansSubTab: StateFlow<Int> = _plansSubTab.asStateFlow()

    private val _walletSubTab = MutableStateFlow(0) // 0: Activity, 1: Payouts
    val walletSubTab: StateFlow<Int> = _walletSubTab.asStateFlow()

    // --- Live Real-Time Ticker & Hashpower ---
    private val _btcPrice = MutableStateFlow(89450.00)
    val btcPrice: StateFlow<Double> = _btcPrice.asStateFlow()

    private val _kasPrice = MutableStateFlow(0.1428)
    val kasPrice: StateFlow<Double> = _kasPrice.asStateFlow()

    private val _hashPower = MutableStateFlow(520.87)
    val hashPower: StateFlow<Double> = _hashPower.asStateFlow()

    // --- User Profile State ---
    private val _currentUser = MutableStateFlow<User?>(
        User(
            id = "#HG-142597",
            email = "goldbrownp@gmail.com",
            role = "user",
            referralCode = "HG-7798"
        )
    )
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val userId: String get() = _currentUser.value?.id ?: "#HG-142597"
    val userEmail: String get() = _currentUser.value?.email ?: "goldbrownp@gmail.com"
    val referralCode: String get() = _currentUser.value?.referralCode ?: "HG-7798"

    private val _walletBalanceUsdt = MutableStateFlow(84.20)
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
            hardwareType = "Antminer S21 Hydro (335 TH/s)",
            tag = "Most Popular"
        ),
        MiningPlan(
            id = "plan_btc_pro",
            name = "Enterprise ASIC Cluster",
            subtitle = "Geothermal Sub-Zero Direct",
            cryptoSymbol = "BTC",
            iconCrypto = "₿",
            minDepositUsdt = 1000.0,
            hashPowerGh = 12500.0,
            monthlyYieldPercent = 19.5,
            termDays = 30,
            dailyYieldUsdtEst = 6.50,
            hardwareType = "MicroBT Whatsminer M63S+",
            tag = "Institutional"
        ),
        MiningPlan(
            id = "plan_doge_ltc",
            name = "Scrypt Fusion Rig",
            subtitle = "Dual Merged DOGE + LTC",
            cryptoSymbol = "LTC",
            iconCrypto = "Ł",
            minDepositUsdt = 250.0,
            hashPowerGh = 1800.0,
            monthlyYieldPercent = 15.0,
            termDays = 30,
            dailyYieldUsdtEst = 1.25,
            hardwareType = "Antminer L9 16GH/s Hydro",
            tag = "Dual Mining"
        )
    )

    // --- Active Contracts ---
    private val _activeContracts = MutableStateFlow(
        listOf(
            ActiveContract(
                id = "ct_01",
                planName = "Prime BTC Hydro Tier-II",
                cryptoSymbol = "BTC",
                depositUsdt = 300.0,
                hashPowerGh = 3150.0,
                elapsedDays = 14,
                totalDays = 30,
                accruedProfitUsdt = 22.40,
                dailyYieldUsdt = 1.60,
                isRestakeEnabled = true,
                startDateStr = "Sep 10, 2026",
                maturityDateStr = "Oct 10, 2026"
            ),
            ActiveContract(
                id = "ct_02",
                planName = "Starter Kaspa Array",
                cryptoSymbol = "KAS",
                depositUsdt = 100.0,
                hashPowerGh = 25.0,
                elapsedDays = 8,
                totalDays = 30,
                accruedProfitUsdt = 3.84,
                dailyYieldUsdt = 0.48,
                isRestakeEnabled = false,
                startDateStr = "Sep 16, 2026",
                maturityDateStr = "Oct 16, 2026"
            )
        )
    )
    val activeContracts: StateFlow<List<ActiveContract>> = _activeContracts.asStateFlow()

    // --- Activity List ---
    private val _activityList = MutableStateFlow(
        listOf(
            ActivityItem(
                id = "act_01",
                title = "+5.85 USDT",
                subtitle = "Sub-Zero Daily Hydro Mining Payout",
                btcAmountStr = "+0.00006540 BTC",
                usdtAmount = 5.85,
                timestampStr = "Today, 00:01 UTC",
                isCredit = true
            ),
            ActivityItem(
                id = "act_02",
                title = "-145.00 USDT",
                subtitle = "Audited Multi-Sig Cold Withdrawal",
                btcAmountStr = "-0.00162000 BTC",
                usdtAmount = 145.00,
                timestampStr = "Sep 15, 2026",
                isCredit = false
            ),
            ActivityItem(
                id = "act_03",
                title = "+3.03 USDT",
                subtitle = "Daily Lucky Wheel Prize Credit",
                btcAmountStr = "+0.00003820 BTC",
                usdtAmount = 3.03,
                timestampStr = "Yesterday, 14:22 UTC",
                isCredit = true
            ),
            ActivityItem(
                id = "act_04",
                title = "+14.00 USDT",
                subtitle = "Syndicate Direct Referral Commission",
                btcAmountStr = "+0.00017650 BTC",
                usdtAmount = 14.00,
                timestampStr = "Sep 22, 2026",
                isCredit = true
            )
        )
    )
    val activityList: StateFlow<List<ActivityItem>> = _activityList.asStateFlow()

    // --- Payouts History ---
    private val _payoutsList = MutableStateFlow(
        listOf(
            PayoutItem(
                id = "po_01",
                dateStr = "Sep 15, 2026",
                amountUsdt = 145.00,
                targetAddress = "TL7N8...x9Wq2",
                network = "TRC20",
                status = PayoutStatus.AUDITED_DISBURSED
            ),
            PayoutItem(
                id = "po_02",
                dateStr = "Aug 28, 2026",
                amountUsdt = 160.00,
                targetAddress = "0x892...f54A1",
                network = "BEP20",
                status = PayoutStatus.COMPLETED
            )
        )
    )
    val payoutsList: StateFlow<List<PayoutItem>> = _payoutsList.asStateFlow()

    // --- Bounty Tasks (Community Micro-Tasks) ---
    private val _bountyTasks = MutableStateFlow(
        listOf(
            BountyTask(
                id = "task_tg",
                type = BountyType.TELEGRAM,
                title = "Join Official Institutional Telegram",
                description = "Join @HashGrid_Official & verify handle",
                rewardUsdt = 0.20,
                status = BountyStatus.AVAILABLE
            ),
            BountyTask(
                id = "task_wa",
                type = BountyType.WHATSAPP,
                title = "WhatsApp Status Daily Mining Share",
                description = "Post referral link with 10+ views for 24h",
                rewardUsdt = 0.20,
                status = BountyStatus.AVAILABLE
            )
        )
    )
    val bountyTasks: StateFlow<List<BountyTask>> = _bountyTasks.asStateFlow()

    // --- Creator Milestones ---
    private val _creatorMilestones = MutableStateFlow<List<CreatorMilestoneSubmission>>(emptyList())
    val creatorMilestones: StateFlow<List<CreatorMilestoneSubmission>> = _creatorMilestones.asStateFlow()

    // --- Live WebSocket Tickers ---
    val liveTickers: StateFlow<List<LiveTickerItem>> = BinanceWebSocketService.tickers
    val isWsConnected: StateFlow<Boolean> = BinanceWebSocketService.isConnected
    val wsStatusText: StateFlow<String> = BinanceWebSocketService.connectionStatusText

    // --- AI Support Chat ---
    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "welcome_1",
                text = "Hello! I am your HashGrid AI Assistant. How can I assist you with your sub-zero mining contracts, NOWPayments deposits, or withdrawal status?",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping: StateFlow<Boolean> = _isAiTyping.asStateFlow()

    // --- Notifications ---
    private val _unreadNotificationsCount = MutableStateFlow(4)
    val unreadNotificationsCount: StateFlow<Int> = _unreadNotificationsCount.asStateFlow()

    fun clearNotifications() {
        _unreadNotificationsCount.value = 0
    }

    // --- Server-Timestamp Mining Progress ---
    private val _miningSessionEndTimestamp = MutableStateFlow(
        System.currentTimeMillis() + (7L * 60 * 1000 + 45 * 1000)
    )
    val miningSessionEndTimestamp: StateFlow<Long> = _miningSessionEndTimestamp.asStateFlow()

    fun extendMiningSession() {
        _miningSessionEndTimestamp.value = Math.max(
            System.currentTimeMillis(),
            _miningSessionEndTimestamp.value
        ) + (2L * 3600 * 1000)
    }

    val notifications = listOf(
        "⚡ Daily Payout Settled: +$5.85 USDT successfully routed from Arctic Sub-Zero Node.",
        "🔒 Security Audit: 24h Cold-Storage Reconciliation passed with 99.8% multi-sig integrity.",
        "🎁 Lucky Spin Available: Your 24h daily wheel spin has reset. Claim your bonus hash!",
        "🌱 PPA Telemetry: Landsvirkjun Geothermal Grid output stable at 45.2 MW ($0.034/kWh)."
    )

    init {
        // Start live Binance WebSocket stream and Kaspa oscillator
        BinanceWebSocketService.start()

        // Sync main BTC and KAS ticker prices
        viewModelScope.launch {
            BinanceWebSocketService.tickers.collect { tickerList ->
                tickerList.find { it.id == "BTCUSDT" }?.let { btc ->
                    _btcPrice.value = btc.price
                }
                tickerList.find { it.id == "KASUSDT" }?.let { kas ->
                    _kasPrice.value = kas.price
                }
            }
        }

        // Start background oscillating hashpower
        startOscillatingHashpower()

        // Auto-check for updates on launch
        AppUpdateManager.checkForUpdates()

        // Sync initial user state with Firebase
        FirebaseSyncService.syncUser(
            FirebaseUser(
                uid = userId,
                email = userEmail,
                walletBalance = _walletBalanceUsdt.value,
                miningRate = "520.87 TH/s",
                createdAt = "2026-09-01"
            )
        )

        // Start real-time Firebase balance sync listener
        FirebaseSyncService.startRealtimeBalanceListener(userId) { remoteBal ->
            _walletBalanceUsdt.value = remoteBal
        }
    }

    private fun startOscillatingHashpower() {
        viewModelScope.launch {
            while (true) {
                delay(2200)
                val hashDelta = (Random.nextDouble() - 0.48) * 0.85
                _hashPower.value = ((_hashPower.value + hashDelta) * 100).toInt() / 100.0
            }
        }
    }

    fun setTab(index: Int) {
        _currentTab.value = index.coerceIn(0, 4)
    }

    fun setPlansSubTab(index: Int) {
        _plansSubTab.value = index
    }

    fun setWalletSubTab(index: Int) {
        _walletSubTab.value = index
    }

    fun toggleRestake(contractId: String) {
        _activeContracts.value = _activeContracts.value.map {
            if (it.id == contractId) it.copy(isRestakeEnabled = !it.isRestakeEnabled) else it
        }
    }

    fun toggle2FA() {
        _twoFactorEnabled.value = !_twoFactorEnabled.value
    }

    fun selectLanguage(lang: String) {
        _selectedLanguage.value = lang
        showLanguageModal.value = false
    }

    // --- Lucky Wheel Execution ---
    fun executeSpin(onResult: (String, Double, Double) -> Unit) {
        if (!_canSpinToday.value || _isSpinning.value) return
        _isSpinning.value = true

        viewModelScope.launch {
            delay(3500)
            val outcomes = listOf(
                Triple("0.5 USDT Instant Cash", 0.5, 0.0),
                Triple("100 Gh/s Power Booster", 0.0, 100.0),
                Triple("1.0 USDT Cash Voucher", 1.0, 0.0),
                Triple("250 Gh/s (24h) Superboost", 0.0, 250.0),
                Triple("0.5 USDT Instant Cash", 0.5, 0.0)
            )
            val win = outcomes[Random.nextInt(outcomes.size)]
            _spinResultText.value = win.first

            if (win.second > 0) {
                _walletBalanceUsdt.value += win.second
                val df = SimpleDateFormat("HH:mm 'UTC'", Locale.US)
                _activityList.value = listOf(
                    ActivityItem(
                        id = "spin_${System.currentTimeMillis()}",
                        title = "+$${win.second} USDT",
                        subtitle = "Daily Lucky Wheel Prize",
                        btcAmountStr = "+0.00000630 BTC",
                        usdtAmount = win.second,
                        timestampStr = "Today, " + df.format(Date()),
                        isCredit = true
                    )
                ) + _activityList.value

                // Sync balance to Firebase
                FirebaseSyncService.syncUser(
                    FirebaseUser(
                        uid = userId,
                        email = userEmail,
                        walletBalance = _walletBalanceUsdt.value,
                        miningRate = "${_hashPower.value} TH/s",
                        createdAt = "2026-09-01"
                    )
                )
            }
            if (win.third > 0) {
                _hashPower.value += win.third
            }

            onResult(win.first, win.second, win.third)
        }
    }

    // --- Deposit Settlement via NOWPayments & Firebase ---
    fun onDepositSuccess(amountUsdt: Double, txId: String) {
        _walletBalanceUsdt.value += amountUsdt
        val df = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
        _activityList.value = listOf(
            ActivityItem(
                id = "dep_${System.currentTimeMillis()}",
                title = "+$${amountUsdt} USDT",
                subtitle = "NOWPayments Gateway Confirmed ($txId)",
                btcAmountStr = "+${String.format(Locale.US, "%.6f", amountUsdt / _btcPrice.value)} BTC",
                usdtAmount = amountUsdt,
                timestampStr = df.format(Date()),
                isCredit = true
            )
        ) + _activityList.value

        // Record in Firebase /deposits and sync /users/{userId}
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

        FirebaseSyncService.syncUser(
            FirebaseUser(
                uid = userId,
                email = userEmail,
                walletBalance = _walletBalanceUsdt.value,
                miningRate = "${_hashPower.value} TH/s",
                createdAt = "2026-09-01"
            )
        )
    }

    // --- Plan Activation ---
    fun activatePlan(plan: MiningPlan): Boolean {
        if (_walletBalanceUsdt.value >= plan.minDepositUsdt) {
            _walletBalanceUsdt.value -= plan.minDepositUsdt
            _hashPower.value += plan.hashPowerGh

            val df = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val startDate = df.format(Date())
            val maturityDate = df.format(Date(System.currentTimeMillis() + 30L * 24 * 3600 * 1000))

            val newContract = ActiveContract(
                id = "ct_${System.currentTimeMillis()}",
                planName = plan.name,
                cryptoSymbol = plan.cryptoSymbol,
                depositUsdt = plan.minDepositUsdt,
                hashPowerGh = plan.hashPowerGh,
                elapsedDays = 0,
                totalDays = 30,
                accruedProfitUsdt = 0.0,
                dailyYieldUsdt = plan.dailyYieldUsdtEst,
                isRestakeEnabled = true,
                startDateStr = startDate,
                maturityDateStr = maturityDate
            )
            _activeContracts.value = listOf(newContract) + _activeContracts.value

            _activityList.value = listOf(
                ActivityItem(
                    id = "act_buy_${System.currentTimeMillis()}",
                    title = "-$${plan.minDepositUsdt} USDT",
                    subtitle = "Plan Allocation: ${plan.name}",
                    btcAmountStr = "-${String.format(Locale.US, "%.6f", plan.minDepositUsdt / _btcPrice.value)} BTC",
                    usdtAmount = plan.minDepositUsdt,
                    timestampStr = "Today, Just now",
                    isCredit = false
                )
            ) + _activityList.value

            // Sync updated balance to Firebase
            FirebaseSyncService.syncUser(
                FirebaseUser(
                    uid = userId,
                    email = userEmail,
                    walletBalance = _walletBalanceUsdt.value,
                    miningRate = "${_hashPower.value} TH/s",
                    createdAt = "2026-09-01"
                )
            )

            return true
        }
        return false
    }

    // --- Ad Plan Anti-Abuse & Device Limit ---
    fun getFreeAdCooldownHoursRemaining(): Int {
        val last = _lastFreeAdSessionTimestamp.value ?: return 0
        val diff = (last + 24L * 3600 * 1000) - System.currentTimeMillis()
        return if (diff > 0) {
            val hours = (diff / (3600 * 1000)).toInt() + 1
            hours.coerceAtLeast(1)
        } else {
            0
        }
    }

    fun claimFreeAdSession(): Pair<Boolean, String> {
        val cooldownHours = getFreeAdCooldownHoursRemaining()
        if (cooldownHours > 0) {
            return Pair(
                false,
                "Device Limit Exceeded: Only 1 Free Ad Plan session allowed per device/IP every 24 hours. Next session opens in $cooldownHours hours."
            )
        }

        _lastFreeAdSessionTimestamp.value = System.currentTimeMillis()
        _hashPower.value += 5.0

        return Pair(
            true,
            "Free Ad Plan Activated! +5.00 Gh/s allocated. (Anti-Abuse Rule: 0% referral commission & $0 team volume credited for free ad plans)."
        )
    }

    // --- Server-Side Database Withdrawal Enforcement ($130) ---
    fun requestWithdrawal(amountUsdt: Double, address: String, network: String): String? {
        if (amountUsdt < 130.00) {
            return "Minimum withdrawal limit is $130.00 USDT."
        }
        if (amountUsdt > _walletBalanceUsdt.value) {
            return "Insufficient available balance. Available: $${String.format(Locale.US, "%.2f", _walletBalanceUsdt.value)} USDT."
        }
        if (address.trim().length < 15) {
            return "Please enter a valid $network wallet destination address."
        }

        // Atomic deduction
        _walletBalanceUsdt.value -= amountUsdt
        _lockedAuditBalanceUsdt.value += amountUsdt

        val df = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val todayStr = df.format(Date())

        val requestId = "TX-" + System.currentTimeMillis().toString().takeLast(6)

        val newPayout = PayoutItem(
            id = "po_${System.currentTimeMillis()}",
            dateStr = todayStr,
            amountUsdt = amountUsdt,
            targetAddress = address.trim(),
            network = network,
            status = PayoutStatus.PENDING_24H_AUDIT
        )
        _payoutsList.value = listOf(newPayout) + _payoutsList.value

        // Push directly to Firebase /withdrawals/{requestId} with status 'pending'
        FirebaseSyncService.pushWithdrawal(
            FirebaseWithdrawal(
                requestId = requestId,
                userId = userId,
                amount = amountUsdt,
                cryptoAddress = address.trim(),
                network = network,
                status = "pending",
                timestamp = FirebaseSyncService.getCurrentTimestamp()
            )
        )

        // Sync new balance to Firebase
        FirebaseSyncService.syncUser(
            FirebaseUser(
                uid = userId,
                email = userEmail,
                walletBalance = _walletBalanceUsdt.value,
                miningRate = "${_hashPower.value} TH/s",
                createdAt = "2026-09-01"
            )
        )

        showWithdrawModal.value = false
        return null
    }

    // --- Creator Milestone ---
    fun submitCreatorMilestone(channelUrl: String, videoUrl: String, contactTelegram: String): String? {
        val cUrl = channelUrl.trim()
        val vUrl = videoUrl.trim()
        val tg = contactTelegram.trim()

        if (cUrl.isBlank() || !cUrl.contains("youtube.com", ignoreCase = true)) {
            return "Please enter a valid YouTube channel profile URL."
        }
        if (vUrl.isBlank() || (!vUrl.contains("youtube.com", ignoreCase = true) && !vUrl.contains("youtu.be", ignoreCase = true))) {
            return "Please enter a valid YouTube video link with 50,000+ views."
        }
        if (tg.isBlank()) {
            return "Please enter your executive contact Telegram handle."
        }

        val submission = CreatorMilestoneSubmission(
            id = "ms_${System.currentTimeMillis()}",
            userId = userId,
            channelUrl = cUrl,
            videoUrl = vUrl,
            contactTelegram = tg,
            submittedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()),
            status = MilestoneStatus.PENDING_EXECUTIVE_AUDIT
        )
        _creatorMilestones.value = listOf(submission) + _creatorMilestones.value

        showCreatorMilestoneModal.value = false
        return null
    }

    // --- Bounty Submissions ---
    fun submitWhatsAppBounty(views: String, timePosted: String): String? {
        val trimmedViews = views.trim()
        val viewCount = trimmedViews.toIntOrNull() ?: 0
        if (viewCount < 10) {
            return "Minimum 10 status views required for verification."
        }

        _bountyTasks.value = _bountyTasks.value.map {
            if (it.type == BountyType.WHATSAPP) {
                it.copy(
                    status = BountyStatus.PENDING_ADMIN_REVIEW,
                    submissionProof = "Screenshot Proof Uploaded ($trimmedViews views)",
                    extraDetail = timePosted,
                    rejectionReason = null
                )
            } else it
        }

        return null
    }

    fun submitTelegramBounty(username: String): String? {
        val trimmed = username.trim()
        if (trimmed.isBlank()) {
            return "Please enter your Telegram @username."
        }
        val handle = if (trimmed.startsWith("@")) trimmed else "@$trimmed"

        _bountyTasks.value = _bountyTasks.value.map {
            if (it.type == BountyType.TELEGRAM) {
                it.copy(
                    status = BountyStatus.PENDING_ADMIN_REVIEW,
                    submissionProof = handle,
                    rejectionReason = null
                )
            } else it
        }

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
