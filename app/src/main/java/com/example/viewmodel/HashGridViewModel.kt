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
import com.example.service.UpdateStatus
import com.example.service.LocalPersistenceManager
import com.example.service.PersistentUserData
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
    val referralCode: String get() = currentUser.value?.referralCode ?: "HG-8080"

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

    // --- Marketplace Plans (Sustainable 10% - 15% Monthly Yield) ---
    val marketplacePlans = listOf(
        MiningPlan(
            id = "plan_starter_rig",
            name = "Starter Grid Rig (10 TH/s)",
            subtitle = "Dedicated ASIC Instant Yield Node",
            cryptoSymbol = "USDT",
            iconCrypto = "⚡",
            minDepositUsdt = 10.0,
            hashPowerGh = 10000.0, // 10 TH/s
            monthlyYieldPercent = 11.5,
            termDays = 30,
            dailyYieldUsdtEst = 0.038,
            hardwareType = "Antminer Micro 10 TH/s Liquid Rig",
            tag = "Starter $10"
        ),
        MiningPlan(
            id = "plan_kas_25",
            name = "Kaspa Micro Array (25 TH/s)",
            subtitle = "KHeavyHash ASIC Liquid Array",
            cryptoSymbol = "KAS",
            iconCrypto = "⚡",
            minDepositUsdt = 25.0,
            hashPowerGh = 25000.0,
            monthlyYieldPercent = 12.5,
            termDays = 30,
            dailyYieldUsdtEst = 0.104,
            hardwareType = "IceRiver KS0 Ultra Liquid",
            tag = "Entry $25"
        ),
        MiningPlan(
            id = "plan_asic_50",
            name = "Antminer Dual Node (50 TH/s)",
            subtitle = "SHA-256 Dual Sub-Zero Rig",
            cryptoSymbol = "BTC",
            iconCrypto = "⛏️",
            minDepositUsdt = 50.0,
            hashPowerGh = 50000.0,
            monthlyYieldPercent = 14.0,
            termDays = 30,
            dailyYieldUsdtEst = 0.233,
            hardwareType = "Antminer Dual S19 Pro Hydro",
            tag = "Standard $50"
        ),
        MiningPlan(
            id = "plan_btc_100",
            name = "Prime BTC Hydro (100 TH/s)",
            subtitle = "Sub-Zero Hydro Immersion Node",
            cryptoSymbol = "BTC",
            iconCrypto = "₿",
            minDepositUsdt = 100.0,
            hashPowerGh = 100000.0,
            monthlyYieldPercent = 15.0,
            termDays = 30,
            dailyYieldUsdtEst = 0.500,
            hardwareType = "Antminer S21 Hydro (Sub-Zero)",
            tag = "Popular $100"
        ),
        MiningPlan(
            id = "plan_institutional_500",
            name = "Institutional Cluster (500 TH/s)",
            subtitle = "Direct Volcano Sub-Zero Connection",
            cryptoSymbol = "BTC",
            iconCrypto = "🌋",
            minDepositUsdt = 500.0,
            hashPowerGh = 500000.0,
            monthlyYieldPercent = 15.0,
            termDays = 30,
            dailyYieldUsdtEst = 2.500,
            hardwareType = "Dedicated Whatsminer M63S Immersion Array",
            tag = "Enterprise $500"
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
    private val _miningSessionEndTimestamp = MutableStateFlow(System.currentTimeMillis() + (14L * 3600 * 1000 + 22L * 60 * 1000))
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
    fun initPersistence(context: Context) {
        appContext = context.applicationContext
        val uid = userId
        if (uid.isNotBlank()) {
            val data = LocalPersistenceManager.loadUserData(context.applicationContext, uid)
            if (data.walletBalanceUsdt > 0.0) {
                _walletBalanceUsdt.value = data.walletBalanceUsdt
            }
            val now = System.currentTimeMillis()
            _isGridMiningActive.value = data.isGridMiningActive
            if (data.miningSessionEndTimestamp > 0) {
                _miningSessionEndTimestamp.value = data.miningSessionEndTimestamp
            }

            // Compute elapsed GRID token accrual on App Open / Resume
            var currentGrid = if (data.gridCoinBalance > 0.0) data.gridCoinBalance else 24.8500
            if (data.isGridMiningActive && data.lastSavedTimestamp > 0 && data.lastSavedTimestamp < now) {
                val activeEnd = _miningSessionEndTimestamp.value
                val effectiveEnd = now.coerceAtMost(activeEnd)
                if (effectiveEnd > data.lastSavedTimestamp) {
                    val elapsedSec = (effectiveEnd - data.lastSavedTimestamp) / 1000.0
                    val rate = _effectiveGridRate.value
                    val secAccrual = rate / 3600.0
                    currentGrid += (elapsedSec * secAccrual)
                }
                if (now >= activeEnd) {
                    _isGridMiningActive.value = false
                }
            }
            _gridCoinBalance.value = currentGrid
            if (data.activeContracts.isNotEmpty()) {
                val now = System.currentTimeMillis()
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
    }



    fun saveLocalState() {
        val ctx = appContext ?: return
        val uid = userId
        if (uid.isNotBlank()) {
            val data = PersistentUserData(
                walletBalanceUsdt = _walletBalanceUsdt.value,
                gridCoinBalance = _gridCoinBalance.value,
                isGridMiningActive = _isGridMiningActive.value,
                miningSessionEndTimestamp = _miningSessionEndTimestamp.value,
                sessionStartTimeMillis = System.currentTimeMillis(),
                activeContracts = _activeContracts.value,
                activityList = _activityList.value,
                payoutsList = _payoutsList.value,
                canSpinToday = _canSpinToday.value,
                wheelCooldownEnd = _wheelCooldownEnd.value,
                lastSavedTimestamp = System.currentTimeMillis()
            )
            LocalPersistenceManager.saveUserData(ctx, uid, data)
        }
    }

    init {
        try {
            BinanceWebSocketService.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Live second-by-second fractional GRID token accumulator with anti-cheat guard
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                if (_isGridMiningActive.value) {
                    val now = System.currentTimeMillis()
                    if (now < _miningSessionEndTimestamp.value) {
                        val rate = _effectiveGridRate.value
                        val secAccrual = rate / 3600.0
                        _gridCoinBalance.value += secAccrual
                        saveLocalState()
                    } else {
                        _isGridMiningActive.value = false
                        saveLocalState()
                        FirebaseSyncService.updateGridCoinBalance(
                            userId,
                            _gridCoinBalance.value,
                            false
                        )
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
                            reconcileReferrals()
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
                                    _isGridMiningActive.value = remoteActive || _isGridMiningActive.value
                                    if (remoteEnd > 0L) {
                                        _miningSessionEndTimestamp.value = remoteEnd
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

    /**
     * Trigger retroactive referral recovery and reconciliation task:
     * Discovers all users in database who entered current user's referral code or UID,
     * updates teamCount & extraHashrate, and backfills subcollection "users/{uid}/team".
     */
    fun reconcileReferrals() {
        val uid = currentUser.value?.id ?: userId
        val code = referralCode
        if (uid.isNotBlank() && uid != "HG-ACCOUNT") {
            viewModelScope.launch {
                try {
                    val reconciledCount = FirebaseSyncService.reconcileUserReferrals(uid, code)
                    if (reconciledCount > _teamCount.value || _teamCount.value == 0L) {
                        _teamCount.value = maxOf(_teamCount.value, reconciledCount)
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
        // Live Cloud Mining Ticks for 30% Contract Work Completion based on exact elapsed seconds
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                try {
                    val currentList = _activeContracts.value
                    if (currentList.isNotEmpty() && currentList.any { it.work_status == "IN_PROGRESS" && it.depositUsdt > 0 }) {
                        val now = System.currentTimeMillis()
                        var hasChanges = false
                        val updatedList = currentList.map { contract ->
                            if (contract.work_status == "IN_PROGRESS" && contract.depositUsdt > 0) {
                                hasChanges = true
                                val matchingPlan = marketplacePlans.find { it.minDepositUsdt == contract.depositUsdt }
                                val dailyYield = matchingPlan?.dailyYieldUsdtEst ?: contract.dailyYieldUsdt.takeIf { it > 0 } ?: 2.50
                                val startMs = if (contract.startTimestampMs > 0 && contract.startTimestampMs <= now) contract.startTimestampMs else now
                                val elapsedSec = ((now - startMs) / 1000.0).coerceAtLeast(0.0)
                                val calculatedYield = elapsedSec * (dailyYield / 86400.0)
                                val target30Pct = contract.target_yield_30_percent
                                val isCompleted = calculatedYield >= target30Pct
                                val finalYield = if (isCompleted) target30Pct else calculatedYield
                                val progress = if (target30Pct > 0) ((finalYield / target30Pct) * 100.0).coerceIn(0.0, 100.0) else 100.0
                                val status = if (isCompleted) "COMPLETED" else "IN_PROGRESS"

                                if (isCompleted && contract.work_status != "COMPLETED") {
                                    _walletBalanceUsdt.value += target30Pct
                                    val newAct = ActivityItem(
                                        id = "act_${System.currentTimeMillis()}",
                                        title = "+$${String.format(Locale.US, "%.2f", target30Pct)} USDT",
                                        subtitle = "${contract.planName} 30% Mining Task Completed! Unlocked for Withdrawal",
                                        btcAmountStr = "Task Finished",
                                        usdtAmount = target30Pct,
                                        timestampStr = "Just now",
                                        isCredit = true
                                    )
                                    _activityList.value = listOf(newAct) + _activityList.value
                                }

                                FirebaseSyncService.updateGridContractWorkStatus(
                                    userId = userId,
                                    contractId = contract.id,
                                    currentYieldMined = finalYield,
                                    taskProgressPct = progress,
                                    workStatus = status,
                                    unlockedForWithdrawal = isCompleted
                                )

                                contract.copy(
                                    dailyYieldUsdt = dailyYield,
                                    current_yield_mined = finalYield,
                                    accruedProfitUsdt = finalYield,
                                    task_progress_pct = progress,
                                    work_status = status,
                                    unlocked_for_withdrawal = isCompleted
                                )
                            } else {
                                contract
                            }
                        }
                        if (hasChanges) {
                            _activeContracts.value = updatedList
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

        FirebaseSyncService.startGridMiningSession(
            userId = userId,
            baseRate = base,
            teamBonusRate = teamBonus
        ) { success, _, end ->
            if (success) {
                _miningSessionEndTimestamp.value = end
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
        return Pair(true, "Free 4-Hour Ad Node activated! +50 GH/s hashrate credited.")
    }

    fun activatePlan(plan: MiningPlan): Boolean {
        if (_walletBalanceUsdt.value < plan.minDepositUsdt) {
            return false
        }
        _walletBalanceUsdt.value -= plan.minDepositUsdt
        _hashPower.value += plan.hashPowerGh

        val minerId = "miner_${UUID.randomUUID().toString().take(6)}"
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
            work_status = if (plan.minDepositUsdt > 0) "IN_PROGRESS" else "COMPLETED",
            unlocked_for_withdrawal = (plan.minDepositUsdt <= 0)
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

    fun deployStarterRig(): Boolean {
        val starterPlan = marketplacePlans.find { it.id == "plan_starter_rig" } ?: return false
        return activatePlan(starterPlan)
    }

    fun toggleRestake(contractId: String) {
        _activeContracts.value = _activeContracts.value.map {
            if (it.id == contractId) it.copy(isRestakeEnabled = !it.isRestakeEnabled) else it
        }
    }

    // --- Lucky Wheel Spin ---
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

        // Sync Firestore transaction atomically
        FirebaseSyncService.claimLuckyWheelReward(userId, slice)
    }

    fun spinLuckyWheel(onResult: (Double, String) -> Unit) {
        if (!_canSpinToday.value || _isSpinning.value) return
        val slice = selectNextWheelSlice()
        onWheelSpinCompleted(slice)
        onResult(slice.gridAmount, slice.label)
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

    // --- Admin God Mode Functions ---
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
        val payout = _payoutsList.value.find { it.id == payoutId } ?: return
        _payoutsList.value = _payoutsList.value.map {
            if (it.id == payoutId) it.copy(status = PayoutStatus.COMPLETED) else it
        }
        _lockedAuditBalanceUsdt.value = (_lockedAuditBalanceUsdt.value - payout.amountUsdt).coerceAtLeast(0.0)

        val txid = "0x" + UUID.randomUUID().toString().replace("-", "").take(16)
        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "-$${String.format(Locale.US, "%.2f", payout.amountUsdt)} USDT",
            subtitle = "Withdrawal Dispatched (TX: $txid)",
            btcAmountStr = "Approved by Admin",
            usdtAmount = payout.amountUsdt,
            timestampStr = "Just now",
            isCredit = false
        )
        _activityList.value = listOf(newAct) + _activityList.value
        saveLocalState()
    }

    fun rejectWithdrawal(payoutId: String) {
        val payout = _payoutsList.value.find { it.id == payoutId } ?: return
        _payoutsList.value = _payoutsList.value.map {
            if (it.id == payoutId) it.copy(status = PayoutStatus.REJECTED) else it
        }
        _lockedAuditBalanceUsdt.value = (_lockedAuditBalanceUsdt.value - payout.amountUsdt).coerceAtLeast(0.0)
        _walletBalanceUsdt.value += payout.amountUsdt

        val newAct = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "+$${String.format(Locale.US, "%.2f", payout.amountUsdt)} USDT",
            subtitle = "Withdrawal Rejected & Refunded to Wallet",
            btcAmountStr = "Refunded",
            usdtAmount = payout.amountUsdt,
            timestampStr = "Just now",
            isCredit = true
        )
        _activityList.value = listOf(newAct) + _activityList.value
        saveLocalState()
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
