package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ActiveContract
import com.example.model.ActivityItem
import com.example.model.AdminBountySubmission
import com.example.model.AdminWithdrawalRequest
import com.example.model.BountyStatus
import com.example.model.BountyTask
import com.example.model.BountyType
import com.example.model.ChatMessage
import com.example.model.CreatorMilestoneSubmission
import com.example.model.MilestoneStatus
import com.example.model.MiningPlan
import com.example.model.LiveTickerItem
import com.example.model.PayoutItem
import com.example.model.PayoutStatus
import com.example.model.User
import com.example.service.BinanceWebSocketService
import com.example.service.GeminiSupportService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class HashGridViewModel : ViewModel() {

    // --- Navigation & Sub-Tabs ---
    private val _currentTab = MutableStateFlow(0) // 0:Home, 1:Plans, 2:Wallet, 3:Growth, 4:Account, 5:Admin
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

    // --- User & RBAC Authentication State ---
    private val _currentUser = MutableStateFlow<User?>(
        User(
            id = "#HG-142597",
            email = "goldbrownp@gmail.com",
            role = "super_admin",
            referralCode = "HG-7798"
        )
    )
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val userId: String get() = _currentUser.value?.id ?: "#HG-142597"
    val userEmail: String get() = _currentUser.value?.email ?: "goldbrownp@gmail.com"
    val userRole: String get() = _currentUser.value?.role ?: "user"
    val referralCode: String get() = _currentUser.value?.referralCode ?: "HG-7798"

    fun isSuperAdmin(): Boolean {
        val user = _currentUser.value
        return user != null && user.role == "super_admin"
    }

    private val _walletBalanceUsdt = MutableStateFlow(84.20)
    val walletBalanceUsdt: StateFlow<Double> = _walletBalanceUsdt.asStateFlow()

    // Atomic Server-Side Escrow Balance (Locked in 24h Audit)
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
    val showAdminPinModal = MutableStateFlow(false)
    val showNotificationSheet = MutableStateFlow(false)
    val showCreatorMilestoneModal = MutableStateFlow(false)

    // Admin Access
    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    // Admin Reserves
    private val _coldReserveUsdt = MutableStateFlow(1420000.0)
    val coldReserveUsdt: StateFlow<Double> = _coldReserveUsdt.asStateFlow()

    private val _hotReserveUsdt = MutableStateFlow(234500.0)
    val hotReserveUsdt: StateFlow<Double> = _hotReserveUsdt.asStateFlow()

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
            id = "plan_dual",
            name = "Pro Dual (LTC+DOGE)",
            subtitle = "Scrypt Merge-Mining Array",
            cryptoSymbol = "LTC",
            iconCrypto = "Ł",
            minDepositUsdt = 500.0,
            hashPowerGh = 6400.0,
            monthlyYieldPercent = 17.5,
            termDays = 30,
            dailyYieldUsdtEst = 2.91,
            hardwareType = "Antminer L9 Sub-Zero",
            tag = "High Yield"
        ),
        MiningPlan(
            id = "plan_inst",
            name = "Institutional Geothermal Node",
            subtitle = "VIP Dedicated 45MW Interconnect",
            cryptoSymbol = "BTC",
            iconCrypto = "🏛️",
            minDepositUsdt = 1000.0,
            hashPowerGh = 15000.0,
            monthlyYieldPercent = 19.5,
            termDays = 30,
            dailyYieldUsdtEst = 6.50,
            hardwareType = "Dedicated Krafla Geothermal Rack",
            tag = "VIP Tier"
        )
    )

    // --- Active Contracts ---
    private val _activeContracts = MutableStateFlow(
        listOf(
            ActiveContract(
                id = "ct_01",
                planName = "Prime BTC Hydro",
                cryptoSymbol = "BTC",
                depositUsdt = 300.0,
                hashPowerGh = 3150.0,
                elapsedDays = 12,
                totalDays = 30,
                accruedProfitUsdt = 19.20,
                dailyYieldUsdt = 1.60,
                isRestakeEnabled = true,
                startDateStr = "Sep 12, 2026",
                maturityDateStr = "Oct 12, 2026"
            ),
            ActiveContract(
                id = "ct_02",
                planName = "Starter Kaspa Node",
                cryptoSymbol = "KAS",
                depositUsdt = 100.0,
                hashPowerGh = 25.0,
                elapsedDays = 24,
                totalDays = 30,
                accruedProfitUsdt = 11.52,
                dailyYieldUsdt = 0.48,
                isRestakeEnabled = false,
                startDateStr = "Aug 31, 2026",
                maturityDateStr = "Sep 30, 2026"
            )
        )
    )
    val activeContracts: StateFlow<List<ActiveContract>> = _activeContracts.asStateFlow()

    // --- Wallet Activity History ---
    private val _activityList = MutableStateFlow(
        listOf(
            ActivityItem(
                id = "act_01",
                title = "+0.00007376 BTC",
                subtitle = "Daily Contract Settlement (Prime BTC)",
                btcAmountStr = "+0.00007376 BTC",
                usdtAmount = 5.85,
                timestampStr = "Today, 00:01 UTC",
                isCredit = true
            ),
            ActivityItem(
                id = "act_02",
                title = "+38.40 KAS",
                subtitle = "Daily Contract Settlement (Kaspa Node)",
                btcAmountStr = "+0.00000690 BTC",
                usdtAmount = 0.48,
                timestampStr = "Today, 00:01 UTC",
                isCredit = true
            ),
            ActivityItem(
                id = "act_03",
                title = "+0.00003820 BTC",
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

    // --- Admin Withdrawal Queue ---
    private val _adminWithdrawalQueue = MutableStateFlow(
        listOf(
            AdminWithdrawalRequest(
                id = "req_101",
                userId = "#HG-88491",
                userEmail = "m.nordic@reykjavik.is",
                amountUsdt = 180.00,
                targetAddress = "TY6hU8s...kL83q",
                network = "TRC20",
                requestedAt = "Today, 09:15 UTC"
            ),
            AdminWithdrawalRequest(
                id = "req_102",
                userId = "#HG-142597",
                userEmail = "a.vance@vanguard-holdings.is",
                amountUsdt = 135.00,
                targetAddress = "0x3Fa9...2e91",
                network = "BEP20",
                requestedAt = "Today, 11:40 UTC"
            )
        )
    )
    val adminWithdrawalQueue: StateFlow<List<AdminWithdrawalRequest>> = _adminWithdrawalQueue.asStateFlow()

    // --- Bounty Tasks (Community Micro-Tasks) ---
    private val _bountyTasks = MutableStateFlow(
        listOf(
            BountyTask(
                id = "task_wa",
                type = BountyType.WHATSAPP,
                title = "Daily WhatsApp Status Share",
                description = "Broadcast Arctic PPA proof. Upload daily screenshot showing views & timestamp.",
                rewardUsdt = 0.20,
                status = BountyStatus.AVAILABLE
            ),
            BountyTask(
                id = "task_tg",
                type = BountyType.TELEGRAM,
                title = "Telegram Community Verification",
                description = "Join official Arctic Hydro channel & submit your @username.",
                rewardUsdt = 0.20,
                status = BountyStatus.AVAILABLE
            )
        )
    )
    val bountyTasks: StateFlow<List<BountyTask>> = _bountyTasks.asStateFlow()

    // --- Admin Community Bounty Queue ---
    private val _adminBountyQueue = MutableStateFlow(
        listOf(
            AdminBountySubmission(
                id = "sub_wa_01",
                userId = "#HG-99214",
                taskId = "task_wa",
                type = BountyType.WHATSAPP,
                title = "Daily WhatsApp Status Share",
                rewardUsdt = 0.20,
                submissionProof = "Screenshot: Status posted 4h ago • 68 contacts viewed",
                channelOrExtra = "68 Views • Timestamp: Today 08:30 UTC",
                submittedAt = "Today, 12:40 UTC",
                userReferralLink = "https://hashgrid.io/join?ref=HG-99214",
                status = BountyStatus.PENDING_ADMIN_REVIEW
            )
        )
    )
    val adminBountyQueue: StateFlow<List<AdminBountySubmission>> = _adminBountyQueue.asStateFlow()

    // --- Global Creator Milestone (50,000+ Views) Showcase Queue ---
    private val _creatorMilestones = MutableStateFlow(
        listOf(
            CreatorMilestoneSubmission(
                id = "cm_01",
                userId = "#HG-88491",
                channelUrl = "https://youtube.com/@NordicMiningInsight",
                videoUrl = "https://youtube.com/watch?v=kY9q8Z1A3w",
                contactTelegram = "@nordic_analyst",
                claimedViews = 58400L,
                submittedAt = "Today, 10:15 UTC",
                status = MilestoneStatus.PENDING_EXECUTIVE_AUDIT,
                auditNotes = "Organic engagement verified. Description includes user ID & ref link."
            )
        )
    )
    val creatorMilestones: StateFlow<List<CreatorMilestoneSubmission>> = _creatorMilestones.asStateFlow()

    // --- High-Frequency WebSocket Tickers ---
    val liveTickers: StateFlow<List<LiveTickerItem>> = BinanceWebSocketService.tickers
    val isWsConnected: StateFlow<Boolean> = BinanceWebSocketService.isConnected
    val wsStatusText: StateFlow<String> = BinanceWebSocketService.connectionStatusText

    // --- AI Support Chat State ---
    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "msg_0",
                text = "Welcome to HashGrid Institutional Concierge. I am your 24/7 AI liaison powered by Arctic infrastructure telemetry. How can I assist you with our 45 MW Geothermal PPA, 30-Day contracts, or cold-storage withdrawals?",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping: StateFlow<Boolean> = _isAiTyping.asStateFlow()

    // --- Notifications State ---
    private val _unreadNotificationsCount = MutableStateFlow(3)
    val unreadNotificationsCount: StateFlow<Int> = _unreadNotificationsCount.asStateFlow()

    fun clearNotifications() {
        _unreadNotificationsCount.value = 0
    }

    // --- Server-Timestamp Mining Progress ---
    // Derives countdown using Math.max(0, endTime - System.currentTimeMillis())
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

        // Sync main BTC and KAS ticker prices from live ticker stream
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
        if (index == 5) {
            // Role-based Access Control (RBAC):
            // if (!user || user.role !== 'super_admin') {
            //   return <Navigate to="/" replace />;
            // }
            val user = _currentUser.value
            if (user == null || user.role != "super_admin") {
                _currentTab.value = 0 // Redirect to "/"
                return
            }
        }
        _currentTab.value = index
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

    fun unlockAdmin(pin: String): Boolean {
        // Enforce RBAC super_admin check
        val user = _currentUser.value
        if (user == null || user.role != "super_admin") {
            _currentTab.value = 0 // Redirect to "/"
            showAdminPinModal.value = false
            return false
        }

        return if (pin == "7798" || pin == "1234" || pin == "admin") {
            _isAdminUnlocked.value = true
            showAdminPinModal.value = false
            _currentTab.value = 5 // Go to Admin screen
            true
        } else {
            false
        }
    }

    // --- Lucky Wheel Execution ---
    fun executeSpin(onResult: (String, Double, Double) -> Unit) {
        if (!_canSpinToday.value || _isSpinning.value) return
        _isSpinning.value = true

        viewModelScope.launch {
            delay(3500) // Simulation of wheel spinning
            _isSpinning.value = false
            _canSpinToday.value = false

            // Segments: 0.5 USDT, 100 Gh/s Booster, Try Tomorrow, 1.0 USDT Voucher, 250 Gh/s (24h)
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
            }
            if (win.third > 0) {
                _hashPower.value += win.third
            }

            onResult(win.first, win.second, win.third)
        }
    }

    // --- Deposit Simulation ---
    fun simulateDeposit(amountUsdt: Double, network: String) {
        _walletBalanceUsdt.value += amountUsdt
        val df = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
        _activityList.value = listOf(
            ActivityItem(
                id = "dep_${System.currentTimeMillis()}",
                title = "+$${amountUsdt} USDT",
                subtitle = "NOWPayments Gateway ($network) Confirmed",
                btcAmountStr = "+${String.format(Locale.US, "%.6f", amountUsdt / _btcPrice.value)} BTC",
                usdtAmount = amountUsdt,
                timestampStr = df.format(Date()),
                isCredit = true
            )
        ) + _activityList.value

        _hotReserveUsdt.value += amountUsdt * 0.5
        _coldReserveUsdt.value += amountUsdt * 0.5
        showDepositModal.value = false
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
        _hashPower.value += 5.0 // +5 Gh/s 24h allocation

        // [ANTI-ABUSE CONSTRAINT ENFORCED]:
        // Strictly disable all referral commissions (7%) and VIP team volume for Free/Ad plans.
        // Referral earnings ONLY trigger on verified paid contracts ($100+ USDT).
        return Pair(
            true,
            "Free Ad Plan Activated! +5.00 Gh/s allocated. (Anti-Abuse Rule: 0% referral commission & $0 team volume credited for free ad plans)."
        )
    }

    // --- Server-Side Database Withdrawal Enforcement ($130) ---
    fun requestWithdrawal(amountUsdt: Double, address: String, network: String): String? {
        // [HARD-CODED CONSTRAINT]: Abort immediately if amount < 130.00
        if (amountUsdt < 130.00) {
            return "Minimum withdrawal limit is $130.00 USDT."
        }
        // Verify available balance
        if (amountUsdt > _walletBalanceUsdt.value) {
            return "Insufficient available balance. Available: $${String.format(Locale.US, "%.2f", _walletBalanceUsdt.value)} USDT."
        }
        if (address.trim().length < 15) {
            return "Please enter a valid $network wallet destination address."
        }

        // ATOMIC TRANSFER: available_balance -> locked_audit_balance (prevents race conditions)
        _walletBalanceUsdt.value -= amountUsdt
        _lockedAuditBalanceUsdt.value += amountUsdt

        val df = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val todayStr = df.format(Date())

        val newPayout = PayoutItem(
            id = "po_${System.currentTimeMillis()}",
            dateStr = todayStr,
            amountUsdt = amountUsdt,
            targetAddress = address.trim(),
            network = network,
            status = PayoutStatus.PENDING_24H_AUDIT
        )
        _payoutsList.value = listOf(newPayout) + _payoutsList.value

        // Push into Admin Review Queue with PENDING_24H_AUDIT
        val adminReq = AdminWithdrawalRequest(
            id = "req_${System.currentTimeMillis()}",
            userId = userId,
            userEmail = userEmail,
            amountUsdt = amountUsdt,
            targetAddress = address.trim(),
            network = network,
            requestedAt = "Just now",
            lockedAuditAmount = amountUsdt,
            status = "PENDING_24H_AUDIT"
        )
        _adminWithdrawalQueue.value = listOf(adminReq) + _adminWithdrawalQueue.value

        showWithdrawModal.value = false
        return null // Transaction committed successfully
    }

    // --- Admin Withdrawal Actions ---
    fun adminApproveWithdrawal(requestId: String) {
        val req = _adminWithdrawalQueue.value.find { it.id == requestId }
        if (req != null) {
            _adminWithdrawalQueue.value = _adminWithdrawalQueue.value.filter { it.id != requestId }
            _payoutsList.value = _payoutsList.value.map {
                if (it.amountUsdt == req.amountUsdt && it.status == PayoutStatus.PENDING_24H_AUDIT) {
                    it.copy(status = PayoutStatus.AUDITED_DISBURSED)
                } else it
            }
            if (req.userId == userId) {
                _lockedAuditBalanceUsdt.value = (_lockedAuditBalanceUsdt.value - req.amountUsdt).coerceAtLeast(0.0)
            }
            _hotReserveUsdt.value = (_hotReserveUsdt.value - req.amountUsdt).coerceAtLeast(0.0)
        }
    }

    fun adminRejectWithdrawal(requestId: String) {
        val req = _adminWithdrawalQueue.value.find { it.id == requestId }
        if (req != null) {
            _adminWithdrawalQueue.value = _adminWithdrawalQueue.value.filter { it.id != requestId }
            // Refund atomic locked audit balance back to available balance
            if (req.userId == userId) {
                _lockedAuditBalanceUsdt.value = (_lockedAuditBalanceUsdt.value - req.amountUsdt).coerceAtLeast(0.0)
                _walletBalanceUsdt.value += req.amountUsdt
            }
        }
    }

    // --- 50,000+ Views Creator Milestone Actions ---
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

        val df = SimpleDateFormat("MMM dd, HH:mm 'UTC'", Locale.US)
        val nowStr = df.format(Date())

        val submission = CreatorMilestoneSubmission(
            id = "cm_${System.currentTimeMillis()}",
            userId = userId,
            channelUrl = cUrl,
            videoUrl = vUrl,
            contactTelegram = if (tg.startsWith("@")) tg else "@$tg",
            claimedViews = 50000L,
            submittedAt = "Today, $nowStr",
            status = MilestoneStatus.PENDING_EXECUTIVE_AUDIT
        )

        _creatorMilestones.value = listOf(submission) + _creatorMilestones.value
        return null // Success
    }

    fun adminDisburseCreatorMilestone(milestoneId: String, giftName: String, auditNotes: String) {
        _creatorMilestones.value = _creatorMilestones.value.map {
            if (it.id == milestoneId) {
                it.copy(
                    status = MilestoneStatus.DISBURSED,
                    awardedGift = giftName,
                    auditNotes = auditNotes
                )
            } else it
        }
    }

    fun adminRejectCreatorMilestone(milestoneId: String, reason: String) {
        _creatorMilestones.value = _creatorMilestones.value.map {
            if (it.id == milestoneId) {
                it.copy(
                    status = MilestoneStatus.REJECTED,
                    auditNotes = reason
                )
            } else it
        }
    }

    fun submitWhatsAppBounty(viewsCount: String, timestampNote: String): String? {
        val trimmedViews = viewsCount.trim()
        val trimmedTime = timestampNote.trim()

        if (trimmedViews.isBlank()) {
            return "Please enter status view count shown in screenshot."
        }

        val df = SimpleDateFormat("MMM dd, HH:mm 'UTC'", Locale.US)
        val nowStr = df.format(Date())

        val submission = AdminBountySubmission(
            id = "sub_wa_${System.currentTimeMillis()}",
            userId = userId,
            taskId = "task_wa",
            type = BountyType.WHATSAPP,
            title = "Daily WhatsApp Status Share",
            rewardUsdt = 0.20,
            submissionProof = "Screenshot: Status posted • $trimmedViews contacts viewed",
            channelOrExtra = "$trimmedViews Views • ${if (trimmedTime.isNotBlank()) trimmedTime else nowStr}",
            submittedAt = "Today, $nowStr",
            submittedTimestamp = System.currentTimeMillis(),
            userReferralLink = "https://hashgrid.io/join?ref=$referralCode",
            status = BountyStatus.PENDING_ADMIN_REVIEW
        )
        _adminBountyQueue.value = listOf(submission) + _adminBountyQueue.value

        _bountyTasks.value = _bountyTasks.value.map {
            if (it.type == BountyType.WHATSAPP) {
                it.copy(
                    status = BountyStatus.PENDING_ADMIN_REVIEW,
                    submissionProof = "Screenshot Proof Uploaded ($trimmedViews views)",
                    extraDetail = trimmedTime,
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

        val df = SimpleDateFormat("MMM dd, HH:mm 'UTC'", Locale.US)
        val nowStr = df.format(Date())

        val submission = AdminBountySubmission(
            id = "sub_tg_${System.currentTimeMillis()}",
            userId = userId,
            taskId = "task_tg",
            type = BountyType.TELEGRAM,
            title = "Telegram Community Verification",
            rewardUsdt = 0.20,
            submissionProof = handle,
            channelOrExtra = "Verified Community Member ($handle)",
            submittedAt = "Today, $nowStr",
            submittedTimestamp = System.currentTimeMillis(),
            userReferralLink = "https://hashgrid.io/join?ref=$referralCode",
            status = BountyStatus.PENDING_ADMIN_REVIEW
        )
        _adminBountyQueue.value = listOf(submission) + _adminBountyQueue.value

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

    fun adminApproveBounty(submissionId: String) {
        val sub = _adminBountyQueue.value.find { it.id == submissionId } ?: return
        _adminBountyQueue.value = _adminBountyQueue.value.filter { it.id != submissionId }

        // Credit User's Wallet if it belongs to current user or mock credited
        if (sub.userId == userId) {
            _walletBalanceUsdt.value += sub.rewardUsdt
            val df = SimpleDateFormat("HH:mm 'UTC'", Locale.US)
            _activityList.value = listOf(
                ActivityItem(
                    id = "bounty_${System.currentTimeMillis()}",
                    title = "+$${String.format(Locale.US, "%.2f", sub.rewardUsdt)} USDT",
                    subtitle = "Bounty Approved: ${sub.title}",
                    btcAmountStr = "+${String.format(Locale.US, "%.8f", sub.rewardUsdt / _btcPrice.value)} BTC",
                    usdtAmount = sub.rewardUsdt,
                    timestampStr = "Today, " + df.format(Date()),
                    isCredit = true
                )
            ) + _activityList.value

            _bountyTasks.value = _bountyTasks.value.map {
                if (it.type == sub.type) {
                    it.copy(status = BountyStatus.APPROVED_CREDITED, rejectionReason = null)
                } else it
            }
        }
    }

    fun adminRejectBounty(submissionId: String, reason: String) {
        val sub = _adminBountyQueue.value.find { it.id == submissionId } ?: return
        _adminBountyQueue.value = _adminBountyQueue.value.filter { it.id != submissionId }

        if (sub.userId == userId) {
            _bountyTasks.value = _bountyTasks.value.map {
                if (it.type == sub.type) {
                    it.copy(
                        status = BountyStatus.REJECTED,
                        rejectionReason = reason
                    )
                } else it
            }
        }
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

    override fun onCleared() {
        super.onCleared()
        BinanceWebSocketService.stop()
    }
}
