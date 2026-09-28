package com.example.viewmodel

import android.content.Context
import android.content.Intent
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
import com.example.model.TeamMember
import com.example.model.TokenConfig
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
import com.example.service.LocalPersistenceManager
import com.example.service.PersistentUserData
import com.example.service.UpdateStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
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
    val referralCode: String
        get() {
            val user = currentUser.value
            val isMaster = AuthService.isMasterAccount(user?.email, user?.id)
            val code = user?.referralCode
            return when {
                isMaster -> "HG-8080"
                !code.isNullOrBlank() && code != "HG-8080" -> code
                else -> AuthService.generateReferralCode(user?.id ?: "")
            }
        }

    // Real dynamic balances (Starts at 0.00 for new user, updated via Firebase Realtime listener)
    private val _walletBalanceUsdt = MutableStateFlow(0.00)
    val walletBalanceUsdt: StateFlow<Double> = _walletBalanceUsdt.asStateFlow()

    // Bulletproof Referral & Team Syndicate Stats
    private val _teamCount = MutableStateFlow(0L)
    val teamCount: StateFlow<Long> = _teamCount.asStateFlow()
    val referralCount: StateFlow<Long> = _teamCount.asStateFlow()

    private val _extraHashrate = MutableStateFlow(0.0)
    val extraHashrate: StateFlow<Double> = _extraHashrate.asStateFlow()
    val bonusHashrate: StateFlow<Double> = _extraHashrate.asStateFlow()

    private val _teamMembers = MutableStateFlow<List<TeamMember>>(emptyList())
    val teamMembers: StateFlow<List<TeamMember>> = _teamMembers.asStateFlow()

    private val _syndicateTier = MutableStateFlow("NOVICE")
    val syndicateTier: StateFlow<String> = _syndicateTier.asStateFlow()

    private val _totalReferralRewardsUsdt = MutableStateFlow(0.0)
    val totalReferralRewardsUsdt: StateFlow<Double> = _totalReferralRewardsUsdt.asStateFlow()

    private val _referralRewards = MutableStateFlow<List<com.example.model.ReferralReward>>(emptyList())
    val referralRewards: StateFlow<List<com.example.model.ReferralReward>> = _referralRewards.asStateFlow()

    private val _taskSubmissions = MutableStateFlow<List<com.example.model.TaskSubmissionItem>>(emptyList())
    val taskSubmissions: StateFlow<List<com.example.model.TaskSubmissionItem>> = _taskSubmissions.asStateFlow()


    // Pi-style Free Mining Boost Telemetry (+0.10 GH/s per active referred miner)
    val activeReferredMinersCount: StateFlow<Int> = _teamMembers.map { members ->
        members.count { it.isMining || it.status.equals("ACTIVE", ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val piTeamBoostGhs: StateFlow<Double> = activeReferredMinersCount.map { activeCount ->
        activeCount * 0.10
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val piTeamBoostDisplayStr: StateFlow<String> = activeReferredMinersCount.map { activeCount ->
        val boost = activeCount * 0.10
        val total = _teamMembers.value.size
        "⚡ Team Boost: +${String.format(Locale.US, "%.2f", boost)} GH/s ($activeCount / $total Active Miners)"
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "⚡ Team Boost: +0.00 GH/s (0 / 0 Active Miners)")

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

    private val _wheelCooldownEnd = MutableStateFlow(0L)
    val wheelCooldownEnd: StateFlow<Long> = _wheelCooldownEnd.asStateFlow()

    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _spinResultText = MutableStateFlow<String?>(null)
    val spinResultText: StateFlow<String?> = _spinResultText.asStateFlow()

    val wonRewardSlice = MutableStateFlow<com.example.model.WheelSlice?>(null)
    val showRewardDialog = MutableStateFlow(false)

    // --- UI Modals State ---
    val showLuckyWheelModal = MutableStateFlow(false)
    val showDepositModal = MutableStateFlow(false)
    val showStarterDeployModal = MutableStateFlow(false)
    val showWithdrawModal = MutableStateFlow(false)
    val showAuditDossierModal = MutableStateFlow(false)
    val showSyndicateModal = MutableStateFlow(false)
    val showAiSupportModal = MutableStateFlow(false)
    val showLanguageModal = MutableStateFlow(false)
    val showNotificationSheet = MutableStateFlow(false)
    val showCreatorMilestoneModal = MutableStateFlow(false)
    val showKycModal = MutableStateFlow(false)
    val showAdminPanelModal = MutableStateFlow(false)
    val auditDossierInitialTab = MutableStateFlow(0)

    // Anti-fraud Registry for YouTube submissions
    private val _submittedYoutubeUrls = mutableSetOf<String>()

    // --- In-App Auto Update State ---
    val updateStatus: StateFlow<UpdateStatus> = AppUpdateManager.updateStatus

    // --- Marketplace Plans (Hardware Rig Tiers with Capacity & Monthly Performance) ---
    val marketplacePlans = listOf(
        MiningPlan(
            id = "plan_starter_node",
            name = "Starter Node",
            subtitle = "Dedicated Entry Hardware",
            cryptoSymbol = "USDT",
            iconCrypto = "⚡",
            minDepositUsdt = 10.0,
            hashPowerGh = 2.0,
            monthlyYieldPercent = 15.0,
            termDays = 30,
            dailyYieldUsdtEst = 0.05,
            ratePerSecond = 0.0000005787,
            hardwareType = "Antminer Micro Hydro Node",
            tag = "Bronze Node",
            badge = "Bronze Node",
            estMonthlyAmountStr = "~$1.50 / Month",
            activeMiningHours = 9600,
            activeMiningHoursStr = "9,600h"
        ),
        MiningPlan(
            id = "plan_pro_miner_node",
            name = "Pro Miner Node",
            subtitle = "High Efficiency Micro Array",
            cryptoSymbol = "USDT",
            iconCrypto = "⚙️",
            minDepositUsdt = 25.0,
            hashPowerGh = 6.0,
            monthlyYieldPercent = 16.0,
            termDays = 30,
            dailyYieldUsdtEst = 0.133333,
            ratePerSecond = 0.0000015432,
            hardwareType = "IceRiver KS0 Ultra Liquid",
            tag = "Silver Node",
            badge = "Silver Node",
            estMonthlyAmountStr = "~$4.00 / Month",
            activeMiningHours = 9000,
            activeMiningHoursStr = "9,000h"
        ),
        MiningPlan(
            id = "plan_quantum_rig_node",
            name = "Quantum Rig Node",
            subtitle = "Sub-Zero Liquid-Cooled Cluster",
            cryptoSymbol = "USDT",
            iconCrypto = "💎",
            minDepositUsdt = 100.0,
            hashPowerGh = 30.0,
            monthlyYieldPercent = 18.0,
            termDays = 30,
            dailyYieldUsdtEst = 0.60,
            ratePerSecond = 0.0000069444,
            hardwareType = "Antminer S21 Hydro (Sub-Zero)",
            tag = "Gold Cyber Node",
            badge = "Gold Cyber Node",
            estMonthlyAmountStr = "~$18.00 / Month",
            activeMiningHours = 8000,
            activeMiningHoursStr = "8,000h"
        ),
        MiningPlan(
            id = "plan_titan_enterprise_node",
            name = "Titan Enterprise Node",
            subtitle = "Direct Industrial Volcano Connection",
            cryptoSymbol = "USDT",
            iconCrypto = "🚀",
            minDepositUsdt = 500.0,
            hashPowerGh = 180.0,
            monthlyYieldPercent = 20.0,
            termDays = 30,
            dailyYieldUsdtEst = 3.333333,
            ratePerSecond = 0.0000385802,
            hardwareType = "Dedicated Whatsminer M63S Immersion Array",
            tag = "Diamond Node",
            badge = "Diamond Node",
            estMonthlyAmountStr = "~$100.00 / Month",
            activeMiningHours = 7200,
            activeMiningHoursStr = "7,200h"
        )
    )

    // Dynamic Contracts list
    private val _activeContracts = MutableStateFlow<List<ActiveContract>>(emptyList())
    val activeContracts: StateFlow<List<ActiveContract>> = _activeContracts.asStateFlow()

    // Withdrawable Unlocked Balance: sum of earnings from grids where work_status == "COMPLETED"
    val withdrawableUnlockedBalance: StateFlow<Double> = _activeContracts.map { contracts ->
        contracts.filter { it.work_status == "COMPLETED" }
            .sumOf { it.current_yield_mined.coerceAtLeast(it.target_yield_30_percent) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

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
    private val _miningSessionEndTimestamp = MutableStateFlow(System.currentTimeMillis() + (24L * 3600 * 1000))
    val miningSessionEndTimestamp: StateFlow<Long> = _miningSessionEndTimestamp.asStateFlow()

    // GRID Native Tokenomics State
    private val _gridCoinBalance = MutableStateFlow(24.8500)
    val gridCoinBalance: StateFlow<Double> = _gridCoinBalance.asStateFlow()

    private val _isGridMiningActive = MutableStateFlow(true)
    val isGridMiningActive: StateFlow<Boolean> = _isGridMiningActive.asStateFlow()

    private val _globalMinersCount = MutableStateFlow(842L)
    val globalMinersCount: StateFlow<Long> = _globalMinersCount.asStateFlow()

    private val _activeReferralsMiningNow = MutableStateFlow(3)
    val activeReferralsMiningNow: StateFlow<Int> = _activeReferralsMiningNow.asStateFlow()

    private val _baseGridRate = MutableStateFlow(TokenConfig.BASE_RATE_PER_HOUR)
    val baseGridRate: StateFlow<Double> = _baseGridRate.asStateFlow()

    private val _effectiveGridRate = MutableStateFlow(
        TokenConfig.calculateEffectiveRate(TokenConfig.BASE_RATE_PER_HOUR, 3)
    )
    val effectiveGridRate: StateFlow<Double> = _effectiveGridRate.asStateFlow()

    val showMiningSheetModal = MutableStateFlow(false)
    private var appContext: Context? = null

    private fun applyLoadedData(data: PersistentUserData) {
        if (data.walletBalanceUsdt > 0.0) {
            _walletBalanceUsdt.value = maxOf(_walletBalanceUsdt.value, data.walletBalanceUsdt)
        }
        val now = System.currentTimeMillis()
        if (data.miningSessionEndTimestamp > 0) {
            _miningSessionEndTimestamp.value = data.miningSessionEndTimestamp
        }
        val isStillActive = if (data.miningSessionEndTimestamp > 0) now < data.miningSessionEndTimestamp else data.isGridMiningActive
        _isGridMiningActive.value = isStillActive

        // Compute elapsed GRID token accrual on App Open / Resume (Offline mining catch-up)
        var currentGrid = if (data.gridCoinBalance > 0.0) maxOf(_gridCoinBalance.value, data.gridCoinBalance) else _gridCoinBalance.value
        if (isStillActive && data.lastSavedTimestamp > 0 && data.lastSavedTimestamp < now) {
            val activeEnd = _miningSessionEndTimestamp.value
            val effectiveEnd = now.coerceAtMost(activeEnd)
            if (effectiveEnd > data.lastSavedTimestamp) {
                val elapsedSec = (effectiveEnd - data.lastSavedTimestamp) / 1000.0
                val rate = if (_effectiveGridRate.value > 0.0) _effectiveGridRate.value else 1.30
                val secAccrual = rate / 3600.0
                currentGrid += (elapsedSec * secAccrual)
            }
            if (now >= activeEnd) {
                _isGridMiningActive.value = false
            }
        }
        _gridCoinBalance.value = currentGrid

        if (data.activeContracts.isNotEmpty()) {
            val reevaluatedContracts = data.activeContracts.map { contract ->
                val matchingPlan = marketplacePlans.find { it.minDepositUsdt == contract.depositUsdt }
                    ?: marketplacePlans.firstOrNull()
                val newDailyYield = matchingPlan?.dailyYieldUsdtEst ?: contract.dailyYieldUsdt
                val newHashPower = matchingPlan?.hashPowerGh ?: contract.hashPowerGh
                val startMs = if (contract.startTimestampMs > 0 && contract.startTimestampMs <= now) contract.startTimestampMs else now
                val elapsedSec = ((now - startMs) / 1000.0).coerceAtLeast(0.0)
                val calculatedAccrued = elapsedSec * (newDailyYield / 86400.0)
                val target30Pct = if (contract.depositUsdt > 0) contract.depositUsdt * 0.30 else 3.0
                val isCompleted = calculatedAccrued >= target30Pct
                val finalAccrued = if (isCompleted) target30Pct else calculatedAccrued
                val progressPct = if (target30Pct > 0) ((finalAccrued / target30Pct) * 100.0).coerceIn(0.0, 100.0) else 100.0
                val status = if (isCompleted) "COMPLETED" else "IN_PROGRESS"

                contract.copy(
                    dailyYieldUsdt = newDailyYield,
                    hashPowerGh = newHashPower,
                    startTimestampMs = startMs,
                    accruedProfitUsdt = finalAccrued,
                    current_yield_mined = finalAccrued,
                    task_progress_pct = progressPct,
                    work_status = status,
                    unlocked_for_withdrawal = isCompleted,
                    totalDays = matchingPlan?.termDays ?: 30
                )
            }
            _activeContracts.value = reevaluatedContracts
            _hashPower.value = reevaluatedContracts.sumOf { it.hashPowerGh } + _extraHashrate.value
        }
        if (data.activityList.isNotEmpty()) {
            _activityList.value = data.activityList
        }
        if (data.payoutsList.isNotEmpty()) {
            _payoutsList.value = data.payoutsList
        }
        _canSpinToday.value = data.canSpinToday
        _wheelCooldownEnd.value = data.wheelCooldownEnd
    }

    fun initPersistence(context: Context) {
        appContext = context.applicationContext
        val ctx = context.applicationContext
        val uid = userId
        val data = if (uid.isNotBlank() && uid != "HG-ACCOUNT") {
            LocalPersistenceManager.loadUserData(ctx, uid)
        } else {
            val fallback = LocalPersistenceManager.loadUserData(ctx, "LAST_ACTIVE_USER")
            if (fallback.gridCoinBalance > 0.0 || fallback.walletBalanceUsdt > 0.0) {
                fallback
            } else {
                LocalPersistenceManager.loadUserData(ctx, "HG-ACCOUNT")
            }
        }
        applyLoadedData(data)
    }

    fun saveLocalState() {
        val ctx = appContext ?: return
        val uid = userId
        if (uid.isNotBlank()) {
            val now = System.currentTimeMillis()
            val data = PersistentUserData(
                walletBalanceUsdt = _walletBalanceUsdt.value,
                gridCoinBalance = _gridCoinBalance.value,
                isGridMiningActive = _isGridMiningActive.value,
                miningSessionEndTimestamp = _miningSessionEndTimestamp.value,
                sessionStartTimeMillis = now,
                activeContracts = _activeContracts.value,
                activityList = _activityList.value,
                payoutsList = _payoutsList.value,
                canSpinToday = _canSpinToday.value,
                wheelCooldownEnd = _wheelCooldownEnd.value,
                lastSavedTimestamp = now
            )
            LocalPersistenceManager.saveUserData(ctx, uid, data)
            if (uid != "HG-ACCOUNT") {
                LocalPersistenceManager.saveUserData(ctx, "LAST_ACTIVE_USER", data)
            }
        }
    }

    init {
        try {
            BinanceWebSocketService.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Live second-by-second fractional GRID token accumulator with continuous auto-save
        viewModelScope.launch {
            var tickCount = 0L
            while (isActive) {
                delay(1000L)
                tickCount++
                if (_isGridMiningActive.value) {
                    val now = System.currentTimeMillis()
                    if (now < _miningSessionEndTimestamp.value) {
                        val rate = _effectiveGridRate.value
                        val secAccrual = rate / 3600.0
                        _gridCoinBalance.value += secAccrual

                        // Continuous local storage save every 2 seconds
                        if (tickCount % 2L == 0L) {
                            saveLocalState()
                        }

                        // Sync to Firebase Cloud every 10 seconds so balance persists across devices
                        if (tickCount % 10L == 0L && userId.isNotBlank() && userId != "HG-ACCOUNT") {
                            FirebaseSyncService.updateGridCoinBalance(
                                userId,
                                _gridCoinBalance.value,
                                true
                            )
                        }
                    } else {
                        _isGridMiningActive.value = false
                        saveLocalState()
                        if (userId.isNotBlank() && userId != "HG-ACCOUNT") {
                            FirebaseSyncService.updateGridCoinBalance(
                                userId,
                                _gridCoinBalance.value,
                                false
                            )
                        }
                    }
                }
            }
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
                            appContext?.let { ctx ->
                                val localUserData = LocalPersistenceManager.loadUserData(ctx, user.id)
                                if (localUserData.gridCoinBalance > 0.0 || localUserData.walletBalanceUsdt > 0.0) {
                                    applyLoadedData(localUserData)
                                } else {
                                    saveLocalState()
                                }
                            }

                            reconcileReferrals()
                            FirebaseSyncService.listenAllWithdrawals { masterPayouts ->
                                if (masterPayouts.isNotEmpty()) {
                                    _payoutsList.value = masterPayouts
                                }
                            }
                            FirebaseSyncService.listenReferralRewards(user.id) { rewards ->
                                _referralRewards.value = rewards
                            }
                            FirebaseSyncService.listenAllTaskSubmissions { submissions ->
                                _taskSubmissions.value = submissions
                            }

                            FirebaseSyncService.listenFirestoreUser(
                                userId = user.id,
                                onProfileUpdated = { remoteBal, kyc, twoFa, docTeamCount, extraHr, refCode ->
                                    if (remoteBal > 0.0) {
                                        _walletBalanceUsdt.value = maxOf(_walletBalanceUsdt.value, remoteBal)
                                    } else if (_walletBalanceUsdt.value > 0.0) {
                                        FirebaseSyncService.updateWalletBalance(user.id, _walletBalanceUsdt.value)
                                    }
                                    _kycStatus.value = kyc
                                    _twoFactorEnabled.value = twoFa
                                    _teamCount.value = maxOf(docTeamCount, _teamMembers.value.size.toLong())
                                    _extraHashrate.value = if (extraHr > 0.0) extraHr else (_teamCount.value * 1.5)
                                    if (refCode.isNotBlank() && refCode != AuthService.currentUser.value?.referralCode) {
                                        AuthService.updateReferralCode(refCode)
                                    }
                                    saveLocalState()
                                },
                                onTransactionsUpdated = { acts, payouts ->
                                    if (acts.isNotEmpty()) _activityList.value = acts
                                    if (payouts.isNotEmpty()) _payoutsList.value = payouts
                                    saveLocalState()
                                },
                                onMinersUpdated = { miners ->
                                    if (miners.isNotEmpty()) {
                                        _activeContracts.value = miners
                                        _hashPower.value = miners.sumOf { it.hashPowerGh } + _extraHashrate.value
                                        saveLocalState()
                                    }
                                },
                                onNotificationsCountUpdated = { count ->
                                    _unreadNotificationsCount.value = count
                                },
                                onGridMiningUpdated = { remoteGridBal, remoteActive, _, remoteEnd, remoteBase, _ ->
                                    if (remoteGridBal > 0.0) {
                                        _gridCoinBalance.value = maxOf(_gridCoinBalance.value, remoteGridBal)
                                    }
                                    val now = System.currentTimeMillis()
                                    if (remoteEnd > now) {
                                        _miningSessionEndTimestamp.value = remoteEnd
                                        _isGridMiningActive.value = true
                                    } else if (remoteEnd > 0L) {
                                        _miningSessionEndTimestamp.value = remoteEnd
                                        _isGridMiningActive.value = remoteActive
                                    }
                                    if (remoteBase > 0.0) {
                                        _baseGridRate.value = remoteBase
                                    }
                                    saveLocalState()
                                },
                                onWheelCooldownUpdated = { lastSpinTime ->
                                    if (lastSpinTime > 0L) {
                                        val cdEnd = lastSpinTime + 86_400_000L
                                        val now = System.currentTimeMillis()
                                        if (now < cdEnd) {
                                            _canSpinToday.value = false
                                            _wheelCooldownEnd.value = cdEnd
                                        } else {
                                            _canSpinToday.value = true
                                            _wheelCooldownEnd.value = 0L
                                        }
                                    } else {
                                        _canSpinToday.value = true
                                        _wheelCooldownEnd.value = 0L
                                    }
                                },
                                onTeamUpdated = { activeCount, members ->
                                    _teamMembers.value = members
                                    if (members.isNotEmpty()) {
                                        _teamCount.value = maxOf(_teamCount.value, members.size.toLong())
                                    }
                                },
                                onSyndicateUpdated = { tier, rewards ->
                                    _syndicateTier.value = tier
                                    _totalReferralRewardsUsdt.value = rewards
                                }
                            )
                        } else {
                            _walletBalanceUsdt.value = 0.00
                            _hashPower.value = 0.0
                            _teamCount.value = 0L
                            _extraHashrate.value = 0.0
                            _syndicateTier.value = "NOVICE"
                            _totalReferralRewardsUsdt.value = 0.0
                            _teamMembers.value = emptyList()
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
        startMiningTicks()
    }

    fun reconcileReferrals() {
        val uid = currentUser.value?.id ?: userId
        val code = referralCode
        if (uid.isNotBlank() && uid != "HG-ACCOUNT") {
            viewModelScope.launch {
                try {
                    val reconciledCount = FirebaseSyncService.reconcileUserReferrals(uid, code)
                    if (reconciledCount > _teamCount.value) {
                        _teamCount.value = reconciledCount
                        _extraHashrate.value = _teamCount.value * 1.5
                        _totalReferralRewardsUsdt.value = _teamCount.value * 5.0
                        _syndicateTier.value = when {
                            _teamCount.value >= 20 -> "ELITE"
                            _teamCount.value >= 5 -> "PRO"
                            else -> "NOVICE"
                        }
                        _hashPower.value = _activeContracts.value.sumOf { it.hashPowerGh } + _extraHashrate.value
                        saveLocalState()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun startMiningTicks() {
        viewModelScope.launch {
            var syncTick = 0L
            while (isActive) {
                delay(1000L)
                syncTick++
                try {
                    val currentList = _activeContracts.value
                    if (currentList.isNotEmpty() && currentList.any { !it.isExpired && it.depositUsdt > 0 }) {
                        val now = System.currentTimeMillis()
                        var hasChanges = false
                        var batchYieldAccrued = 0.0

                        val updatedList = currentList.map { contract ->
                            if (!contract.isExpired && contract.depositUsdt > 0) {
                                hasChanges = true
                                val lastSync = if (contract.last_synced_at > 0) contract.last_synced_at else now - 1000L
                                val deltaSec = ((now - lastSync) / 1000.0).coerceAtLeast(0.0)

                                val rate = if (contract.rate_per_second > 0) contract.rate_per_second
                                           else if (contract.dailyYieldUsdt > 0) contract.dailyYieldUsdt / 86400.0
                                           else (contract.depositUsdt * 0.005) / 86400.0

                                val potentialYield = deltaSec * rate
                                val currentEarned = contract.earned_amount
                                val maxCap = contract.maxPayoutCap
                                val remainingCap = (maxCap - currentEarned).coerceAtLeast(0.0)

                                val actualYield: Double
                                val newEarned: Double
                                val newStatus: String
                                val newWorkStatus: String

                                if (potentialYield >= remainingCap && remainingCap > 0) {
                                    actualYield = remainingCap
                                    newEarned = maxCap
                                    newStatus = "EXPIRED"
                                    newWorkStatus = "EXPIRED"
                                } else if (remainingCap <= 0) {
                                    actualYield = 0.0
                                    newEarned = maxCap
                                    newStatus = "EXPIRED"
                                    newWorkStatus = "EXPIRED"
                                } else {
                                    actualYield = potentialYield
                                    newEarned = currentEarned + actualYield
                                    newStatus = "ACTIVE"
                                    newWorkStatus = if (newEarned >= contract.target_yield_30_percent) "COMPLETED" else "IN_PROGRESS"
                                }

                                batchYieldAccrued += actualYield

                                contract.copy(
                                    earned_amount = newEarned,
                                    accruedProfitUsdt = newEarned,
                                    current_yield_mined = newEarned,
                                    task_progress_pct = if (maxCap > 0) ((newEarned / maxCap) * 100.0).coerceIn(0.0, 100.0) else 100.0,
                                    status = newStatus,
                                    work_status = newWorkStatus,
                                    unlocked_for_withdrawal = newEarned >= contract.target_yield_30_percent || newStatus == "EXPIRED",
                                    last_synced_at = now
                                )
                            } else {
                                contract
                            }
                        }

                        if (hasChanges) {
                            _activeContracts.value = updatedList
                            if (batchYieldAccrued > 0.0) {
                                _walletBalanceUsdt.value += batchYieldAccrued
                            }
                            if (syncTick % 3L == 0L) {
                                saveLocalState()
                            }
                            if (syncTick % 10L == 0L) {
                                val walletAddress = appContext?.let { AuthService.getOrCreateWalletAddress(it) } ?: ""
                                val activeRigs = updatedList.map { it.toMiningRig() }
                                FirebaseSyncService.syncBatchRigs(walletAddress, userId, activeRigs)
                                FirebaseSyncService.updateWalletBalance(userId, _walletBalanceUsdt.value)
                            }
                        }
                    }
                } catch (_: Exception) {}
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
        activateGridMiningSession()
    }

    fun activateGridMiningSession() {
        val base = TokenConfig.getBaseRateForMiners(_globalMinersCount.value)
        val teamBonus = _activeReferralsMiningNow.value * 0.10
        val effective = TokenConfig.calculateEffectiveRate(base, _activeReferralsMiningNow.value)

        _baseGridRate.value = base
        _effectiveGridRate.value = effective
        _isGridMiningActive.value = true

        val currentServerTime = FirebaseSyncService.getAuthoritativeServerTime()
        val sessionEnd = currentServerTime + TokenConfig.SESSION_DURATION_MS
        _miningSessionEndTimestamp.value = sessionEnd
        saveLocalState()

        FirebaseSyncService.startGridMiningSession(
            userId = userId,
            baseRate = base,
            teamBonusRate = teamBonus
        ) { success, _, end ->
            if (success && end > 0L) {
                _miningSessionEndTimestamp.value = end
                saveLocalState()
            }
        }
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
        saveLocalState()
        return Pair(true, "Free 4-Hour Ad Node activated! +50 GH/s hashrate credited.")
    }

    fun activatePlan(plan: MiningPlan): Boolean {
        if (_walletBalanceUsdt.value < plan.minDepositUsdt) {
            return false
        }
        _walletBalanceUsdt.value -= plan.minDepositUsdt
        _hashPower.value += plan.hashPowerGh

        val rigId = "RIG-${plan.minDepositUsdt.toInt()}-${UUID.randomUUID().toString().take(6).uppercase()}"
        val targetYield30 = plan.minDepositUsdt * 0.30
        val maxCap = plan.minDepositUsdt * 2.0
        val dailyYield = plan.dailyYieldUsdtEst
        val ratePerSec = dailyYield / 86400.0
        val now = System.currentTimeMillis()

        val newContract = ActiveContract(
            id = rigId,
            planName = plan.name,
            cryptoSymbol = plan.cryptoSymbol,
            depositUsdt = plan.minDepositUsdt,
            hashPowerGh = plan.hashPowerGh,
            elapsedDays = 0,
            totalDays = plan.termDays,
            accruedProfitUsdt = 0.0,
            dailyYieldUsdt = dailyYield,
            isRestakeEnabled = false,
            startDateStr = "Today",
            maturityDateStr = "2X Cap: $${String.format(Locale.US, "%.2f", maxCap)}",
            startTimestampMs = now,
            endTimestampMs = now + (plan.termDays * 24L * 3600 * 1000),
            costUsdt = plan.minDepositUsdt,
            hashrateThs = if (plan.hashPowerGh >= 1000) plan.hashPowerGh / 1000.0 else plan.hashPowerGh,
            isActive = true,
            plan_cost = plan.minDepositUsdt,
            target_yield_30_percent = targetYield30,
            current_yield_mined = 0.0,
            task_progress_pct = 0.0,
            work_status = if (plan.minDepositUsdt > 0) "IN_PROGRESS" else "COMPLETED",
            unlocked_for_withdrawal = (plan.minDepositUsdt <= 0),
            rig_id = rigId,
            max_payout_cap = maxCap,
            earned_amount = 0.0,
            rate_per_second = ratePerSec,
            status = "ACTIVE",
            purchased_at = now,
            last_synced_at = now
        )
        _activeContracts.value = listOf(newContract) + _activeContracts.value

        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "-${String.format(Locale.US, "%.2f", plan.minDepositUsdt)} USDT",
            subtitle = "Deployed Rig ${plan.name} ($rigId)",
            btcAmountStr = "2X Cap: $${String.format(Locale.US, "%.2f", maxCap)}",
            usdtAmount = plan.minDepositUsdt,
            timestampStr = "Just now",
            isCredit = false
        )
        _activityList.value = listOf(newAct) + _activityList.value
        saveLocalState()

        val walletAddress = appContext?.let { AuthService.getOrCreateWalletAddress(it) } ?: ""
        FirebaseSyncService.purchaseMiningPlan(userId, plan) {}
        FirebaseSyncService.purchaseRigForWallet(walletAddress, userId, newContract.toMiningRig())
        return true
    }

    fun deployStarterRig(): Boolean {
        val starterPlan = marketplacePlans.find { it.id == "plan_starter_node" || it.minDepositUsdt == 10.0 } ?: return false
        return activatePlan(starterPlan)
    }

    fun toggleRestake(contractId: String) {
        _activeContracts.value = _activeContracts.value.map {
            if (it.id == contractId) it.copy(isRestakeEnabled = !it.isRestakeEnabled) else it
        }
        saveLocalState()
    }

    fun selectNextWheelSlice(): com.example.model.WheelSlice {
        return com.example.model.WheelConfig.selectWeightedWinningSlice()
    }

    fun onWheelSpinCompleted(slice: com.example.model.WheelSlice) {
        _isSpinning.value = false
        _canSpinToday.value = false
        val now = System.currentTimeMillis()
        _wheelCooldownEnd.value = now + 86_400_000L
        _spinResultText.value = slice.label

        if (slice.rewardType == com.example.model.WheelRewardType.GRID_COINS) {
            _gridCoinBalance.value += slice.gridAmount
        } else if (slice.rewardType == com.example.model.WheelRewardType.HASHRATE_BOOST) {
            _extraHashrate.value += slice.hashrateGhs
            _hashPower.value += slice.hashrateGhs
        } else if (slice.rewardType == com.example.model.WheelRewardType.USDT_BONUS) {
            _walletBalanceUsdt.value += slice.usdtAmount
        }

        wonRewardSlice.value = slice
        showRewardDialog.value = true

        val newAct = ActivityItem(
            id = "spin_${System.currentTimeMillis()}",
            title = slice.label,
            subtitle = "Daily Lucky Spin Prize",
            btcAmountStr = "",
            usdtAmount = 0.0,
            timestampStr = "Just now",
            isCredit = true
        )
        _activityList.value = listOf(newAct) + _activityList.value
        saveLocalState()

        FirebaseSyncService.claimLuckyWheelReward(userId, slice)
    }

    fun spinLuckyWheel(onResult: (Double, String) -> Unit) {
        if (!_canSpinToday.value || _isSpinning.value) return
        val slice = selectNextWheelSlice()
        onWheelSpinCompleted(slice)
        onResult(slice.gridAmount, slice.label)
    }

    fun verifyTxIdDeposit(
        txHash: String,
        amountUsdt: Double,
        network: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val walletAddress = appContext?.let { AuthService.getOrCreateWalletAddress(it) } ?: userId
            val (success, message) = FirebaseSyncService.verifyAndProcessDepositAtomic(
                walletAddress = walletAddress,
                userId = userId,
                txId = txHash,
                amountUsdt = amountUsdt,
                network = network
            )
            if (success) {
                _walletBalanceUsdt.value += amountUsdt
                val newAct = ActivityItem(
                    id = FirebaseSyncService.sanitizeTxHash(txHash),
                    title = "+$${String.format(Locale.US, "%.2f", amountUsdt)} USDT",
                    subtitle = "Verified Deposit ($network)",
                    btcAmountStr = "",
                    usdtAmount = amountUsdt,
                    timestampStr = "Just now",
                    isCredit = true
                )
                _activityList.value = listOf(newAct) + _activityList.value
                saveLocalState()
            }
            onResult(success, message)
        }
    }

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
                saveLocalState()
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
        saveLocalState()
        return true
    }

    fun requestWithdrawal(amountUsdt: Double, address: String, network: String): String? {
        val hasPending = _payoutsList.value.any { it.status == PayoutStatus.PENDING_24H_AUDIT }
        if (hasPending) {
            return "A withdrawal request is already pending approval. Please wait for admin dispatch (typically within 1-12 hours)."
        }
        val inProgressContract = _activeContracts.value.firstOrNull { it.work_status == "IN_PROGRESS" && it.depositUsdt > 0 }
        if (inProgressContract != null && inProgressContract.current_yield_mined < inProgressContract.target_yield_30_percent) {
            val cur = String.format(Locale.US, "%.2f", inProgressContract.current_yield_mined)
            val tar = String.format(Locale.US, "%.2f", inProgressContract.target_yield_30_percent)
            val pct = inProgressContract.task_progress_pct.toInt()
            return "Hardware Stability Notice: Minimum payout unlocks after completing the 30% work milestone ($$tar for this rig). Current progress: $$cur / $$tar ($pct%)."
        }
        val minWithdrawalThreshold = 10.00
        if (amountUsdt < minWithdrawalThreshold) {
            return "Minimum withdrawal amount is 10.00 USDT."
        }
        if (_walletBalanceUsdt.value < minWithdrawalThreshold) {
            return "Minimum withdrawable threshold is 10 USDT. Keep mining to reach threshold."
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
        saveLocalState()

        FirebaseSyncService.submitWithdrawal(
            userId = userId,
            userEmail = userEmail,
            amount = amountUsdt,
            cryptoAddress = address,
            network = network
        ) {}

        return null
    }

    fun injectUsdt(amount: Double) {
        if (amount <= 0) return
        val newBal = _walletBalanceUsdt.value + amount
        _walletBalanceUsdt.value = newBal
        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "+$${String.format(Locale.US, "%.2f", amount)} USDT",
            subtitle = "God Mode Admin Instant Injection",
            btcAmountStr = "Injected",
            usdtAmount = amount,
            timestampStr = "Just now",
            isCredit = true
        )
        _activityList.value = listOf(newAct) + _activityList.value
        saveLocalState()
        FirebaseSyncService.updateWalletBalance(userId, newBal)
    }

    fun injectGrid(amount: Double) {
        if (amount <= 0) return
        val newBal = _gridCoinBalance.value + amount
        _gridCoinBalance.value = newBal
        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "+$${String.format(Locale.US, "%.2f", amount)} GRID",
            subtitle = "God Mode Admin GRID Injection",
            btcAmountStr = "Injected",
            usdtAmount = 0.0,
            timestampStr = "Just now",
            isCredit = true
        )
        _activityList.value = listOf(newAct) + _activityList.value
        saveLocalState()
        FirebaseSyncService.updateGridCoinBalance(userId, newBal, _isGridMiningActive.value)
    }

    fun freeDeployRig(plan: MiningPlan) {
        _hashPower.value += plan.hashPowerGh

        val minerId = "admin_miner_${UUID.randomUUID().toString().take(6)}"
        val targetYield30 = plan.minDepositUsdt * 0.30
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
            maturityDateStr = "In ${plan.termDays} Days",
            plan_cost = plan.minDepositUsdt,
            target_yield_30_percent = targetYield30,
            current_yield_mined = 0.0,
            task_progress_pct = 0.0,
            work_status = "IN_PROGRESS",
            unlocked_for_withdrawal = false
        )
        _activeContracts.value = listOf(newContract) + _activeContracts.value

        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "Admin Free Rig Deployed",
            subtitle = "Activated ${plan.name} (${plan.hashPowerGh.toInt()} GH/s)",
            btcAmountStr = "0 USDT Deducted",
            usdtAmount = 0.0,
            timestampStr = "Just now",
            isCredit = true
        )
        _activityList.value = listOf(newAct) + _activityList.value
        saveLocalState()
    }

    fun approveWithdrawal(payoutId: String) {
        val payout = _payoutsList.value.find { it.id == payoutId }
        _payoutsList.value = _payoutsList.value.map {
            if (it.id == payoutId) it.copy(status = PayoutStatus.COMPLETED) else it
        }
        if (payout != null) {
            _lockedAuditBalanceUsdt.value = (_lockedAuditBalanceUsdt.value - payout.amountUsdt).coerceAtLeast(0.0)
        }
        val txid = "0x" + UUID.randomUUID().toString().replace("-", "").take(16)
        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "-$${String.format(Locale.US, "%.2f", payout?.amountUsdt ?: 0.0)} USDT",
            subtitle = "Withdrawal Dispatched (TX: $txid)",
            btcAmountStr = "Approved by Admin",
            usdtAmount = payout?.amountUsdt ?: 0.0,
            timestampStr = "Just now",
            isCredit = false
        )
        _activityList.value = listOf(newAct) + _activityList.value
        saveLocalState()

        FirebaseSyncService.approveWithdrawalAtomic(payoutId, txid) { _, _ -> }
    }

    fun rejectWithdrawal(payoutId: String) {
        val payout = _payoutsList.value.find { it.id == payoutId }
        _payoutsList.value = _payoutsList.value.map {
            if (it.id == payoutId) it.copy(status = PayoutStatus.REJECTED) else it
        }
        if (payout != null) {
            _lockedAuditBalanceUsdt.value = (_lockedAuditBalanceUsdt.value - payout.amountUsdt).coerceAtLeast(0.0)
            _walletBalanceUsdt.value += payout.amountUsdt
        }
        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "+$${String.format(Locale.US, "%.2f", payout?.amountUsdt ?: 0.0)} USDT",
            subtitle = "Withdrawal Rejected & Refunded to Wallet",
            btcAmountStr = "Refunded",
            usdtAmount = payout?.amountUsdt ?: 0.0,
            timestampStr = "Just now",
            isCredit = true
        )
        _activityList.value = listOf(newAct) + _activityList.value
        saveLocalState()

        FirebaseSyncService.rejectAndRefundWithdrawalAtomic(payoutId, "Rejected by Admin - Funds Refunded") { _, _ -> }
    }

    fun approveTaskSubmission(submissionId: String, adminNote: String = "Approved by Admin") {
        FirebaseSyncService.approveTaskSubmissionAtomic(submissionId, adminNote) { success, msg -> }
    }

    fun rejectTaskSubmission(submissionId: String, adminNote: String = "Rejected by Admin") {
        FirebaseSyncService.rejectTaskSubmissionAtomic(submissionId, adminNote) { success, msg -> }
    }

    fun submitYouTubeBounty(videoUrl: String, channelName: String): String? {
        val cleanUrl = videoUrl.trim()
        val cleanChannel = channelName.trim()

        if (cleanChannel.isBlank()) {
            return "Please enter your YouTube channel name."
        }
        if (cleanUrl.isBlank()) {
            return "Please enter your YouTube video URL."
        }

        val isYoutubeFormat = cleanUrl.contains("youtube.com/watch?v=", ignoreCase = true) ||
                cleanUrl.contains("youtu.be/", ignoreCase = true) ||
                cleanUrl.contains("youtube.com/shorts/", ignoreCase = true)

        if (!isYoutubeFormat) {
            return "Invalid YouTube URL format. Must be youtube.com/watch?v= or youtu.be/"
        }

        if (_submittedYoutubeUrls.contains(cleanUrl.lowercase())) {
            return "This exact YouTube video has already been submitted across the database."
        }
        _submittedYoutubeUrls.add(cleanUrl.lowercase())

        val subId = "TSK-YT-" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase()

        _bountyTasks.value = _bountyTasks.value.map {
            if (it.type == BountyType.YOUTUBE) {
                it.copy(
                    status = BountyStatus.PENDING_ADMIN_REVIEW,
                    submissionProof = cleanUrl,
                    rejectionReason = null
                )
            } else it
        }

        FirebaseSyncService.submitTaskSubmissionToFirestore(
            com.example.model.TaskSubmissionItem(
                submissionId = subId,
                walletId = "HG-" + userId.replace("-", "").take(8).uppercase(),
                userId = userId,
                taskType = "YOUTUBE_COLLAB",
                title = "YouTube Video Showcase Bounty",
                proofLink = "Channel: $cleanChannel | URL: $cleanUrl",
                rewardAmountUsdt = 5.00,
                status = "PENDING"
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

        val subId = "TSK-WA-" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
        _bountyTasks.value = _bountyTasks.value.map {
            if (it.type == BountyType.WHATSAPP) {
                it.copy(
                    status = BountyStatus.PENDING_ADMIN_REVIEW,
                    submissionProof = "Status Views: $trimmedViews (Posted: $timePosted)",
                    rejectionReason = null
                )
            } else it
        }

        FirebaseSyncService.submitTaskSubmissionToFirestore(
            com.example.model.TaskSubmissionItem(
                submissionId = subId,
                walletId = "HG-" + userId.replace("-", "").take(8).uppercase(),
                userId = userId,
                taskType = "DAILY_STATUS",
                title = "Daily Status 10-Hour Views",
                proofMorningUrl = "https://hashgrid.io/proof/morning_${userId.take(6)}.png",
                proofEveningUrl = "https://hashgrid.io/proof/evening_${userId.take(6)}.png",
                proofLink = "Status Views: $trimmedViews (Posted: $timePosted)",
                rewardAmountUsdt = 0.20,
                status = "PENDING"
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

        val subId = "TSK-TG-" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
        _bountyTasks.value = _bountyTasks.value.map {
            if (it.type == BountyType.TELEGRAM) {
                it.copy(
                    status = BountyStatus.PENDING_ADMIN_REVIEW,
                    submissionProof = handle,
                    rejectionReason = null
                )
            } else it
        }

        FirebaseSyncService.submitTaskSubmissionToFirestore(
            com.example.model.TaskSubmissionItem(
                submissionId = subId,
                walletId = "HG-" + userId.replace("-", "").take(8).uppercase(),
                userId = userId,
                taskType = "TELEGRAM_PROMO",
                title = "Telegram Syndicate Community",
                proofLink = handle,
                rewardAmountUsdt = 0.20,
                status = "PENDING"
            )
        )

        return null
    }

    fun submitCreatorMilestone(channelUrl: String, videoUrl: String, contactTelegram: String): String? {
        if (channelUrl.isBlank() || videoUrl.isBlank()) {
            return "Please provide both channel and video review URLs."
        }
        val subId = "TSK-50K-" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
        val sub = CreatorMilestoneSubmission(
            id = subId,
            userId = userId,
            channelUrl = channelUrl.trim(),
            videoUrl = videoUrl.trim(),
            contactTelegram = contactTelegram.trim(),
            submittedAt = "Just now",
            status = MilestoneStatus.PENDING_EXECUTIVE_AUDIT
        )
        _creatorSubmissions.value = listOf(sub) + _creatorSubmissions.value

        FirebaseSyncService.submitTaskSubmissionToFirestore(
            com.example.model.TaskSubmissionItem(
                submissionId = subId,
                walletId = "HG-" + userId.replace("-", "").take(8).uppercase(),
                userId = userId,
                taskType = "VIEWS_50K",
                title = "Creator Milestone 50,000 Views",
                proofLink = "Channel: ${channelUrl.trim()} | Video: ${videoUrl.trim()} | Telegram: ${contactTelegram.trim()}",
                rewardAmountUsdt = 50.00,
                status = "PENDING"
            )
        )

        return null
    }


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

    fun signInWithGoogle(
        context: Context,
        idToken: String,
        referralCode: String? = null,
        onResult: (Result<User>) -> Unit
    ) {
        viewModelScope.launch {
            val result = AuthService.signInWithGoogleCredential(context, idToken, referralCode)
            result.onSuccess { user ->
                onDirectAuthSuccess(user)
                FirebaseSyncService.reconcileUserReferrals(user.id, user.referralCode)
            }
            onResult(result)
        }
    }

    fun login(context: Context, email: String, pass: String, onResult: (com.example.service.AuthStepResult) -> Unit) {
        viewModelScope.launch {
            val stepResult = AuthService.loginWithEmail(context, email, pass)
            if (stepResult is com.example.service.AuthStepResult.Authenticated) {
                FirebaseSyncService.startRealtimeBalanceListener(stepResult.user.id) { remoteBal ->
                    _walletBalanceUsdt.value = remoteBal
                }
            }
            onResult(stepResult)
        }
    }

    fun signUp(
        context: Context,
        name: String,
        email: String,
        pass: String,
        confirmPass: String,
        refCode: String,
        onResult: (com.example.service.AuthStepResult) -> Unit
    ) {
        viewModelScope.launch {
            val stepResult = AuthService.signUpWithEmail(context, name, email, pass, confirmPass, refCode)
            if (stepResult is com.example.service.AuthStepResult.Authenticated) {
                FirebaseSyncService.startRealtimeBalanceListener(stepResult.user.id) { remoteBal ->
                    _walletBalanceUsdt.value = remoteBal
                }
            }
            onResult(stepResult)
        }
    }

    fun checkEmailVerificationAndActivate(context: Context, appliedCode: String?, onResult: (Result<User>) -> Unit) {
        viewModelScope.launch {
            val result = AuthService.checkEmailVerifiedAndActivate(context, appliedCode)
            result.onSuccess { user ->
                onDirectAuthSuccess(user)
                FirebaseSyncService.reconcileUserReferrals(user.id, user.referralCode)
            }
            onResult(result)
        }
    }

    fun resendVerificationEmail(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = AuthService.resendCurrentEmailVerification()
            onResult(result)
        }
    }

    fun sendPasswordReset(email: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = AuthService.sendPasswordResetDirect(email)
            onResult(result)
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

    fun checkForUpdates(context: Context? = null) {
        AppUpdateManager.checkForUpdates(context, isManualCheck = true)
    }

    fun startAppUpdate(context: Context, info: AppUpdateInfo) {
        AppUpdateManager.openReleasesPage(context)
        AppUpdateManager.startDownload(context, info)
    }

    fun installAppUpdate(context: Context, apkFile: File) {
        AppUpdateManager.openReleasesPage(context)
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
