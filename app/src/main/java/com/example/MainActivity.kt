package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.BountyTask
import com.example.ui.components.BottomNavBar
import com.example.ui.components.TopBar
import com.example.ui.modals.AdminPinModal
import com.example.ui.modals.AiSupportChatModal
import com.example.ui.modals.AuditDossierModal
import com.example.ui.modals.BountySubmissionModal
import com.example.ui.modals.CreatorMilestoneModal
import com.example.ui.modals.DepositModal
import com.example.ui.modals.LanguageSelectorModal
import com.example.ui.modals.LuckyWheelModal
import com.example.ui.modals.NotificationSheet
import com.example.ui.modals.SyndicateTermSheetModal
import com.example.ui.modals.WithdrawModal
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.GrowthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlansScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.HashGridTheme
import com.example.viewmodel.HashGridViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HashGridTheme {
                HashGridApp()
            }
        }
    }
}

@Composable
fun HashGridApp(
    viewModel: HashGridViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val plansSubTab by viewModel.plansSubTab.collectAsStateWithLifecycle()
    val walletSubTab by viewModel.walletSubTab.collectAsStateWithLifecycle()

    val btcPrice by viewModel.btcPrice.collectAsStateWithLifecycle()
    val kasPrice by viewModel.kasPrice.collectAsStateWithLifecycle()
    val hashPower by viewModel.hashPower.collectAsStateWithLifecycle()
    val walletBalance by viewModel.walletBalanceUsdt.collectAsStateWithLifecycle()
    val activeContracts by viewModel.activeContracts.collectAsStateWithLifecycle()
    val activityList by viewModel.activityList.collectAsStateWithLifecycle()
    val payoutsList by viewModel.payoutsList.collectAsStateWithLifecycle()
    val bountyTasks by viewModel.bountyTasks.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiTyping by viewModel.isAiTyping.collectAsStateWithLifecycle()
    val twoFactorEnabled by viewModel.twoFactorEnabled.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    // Gamification state
    val canSpinToday by viewModel.canSpinToday.collectAsStateWithLifecycle()
    val isSpinning by viewModel.isSpinning.collectAsStateWithLifecycle()
    val lastSpinResult by viewModel.spinResultText.collectAsStateWithLifecycle()

    // High-Frequency WebSocket Tickers
    val liveTickers by viewModel.liveTickers.collectAsStateWithLifecycle()
    val isWsConnected by viewModel.isWsConnected.collectAsStateWithLifecycle()
    val wsStatusText by viewModel.wsStatusText.collectAsStateWithLifecycle()

    // Admin reserves & queue
    val coldReserve by viewModel.coldReserveUsdt.collectAsStateWithLifecycle()
    val hotReserve by viewModel.hotReserveUsdt.collectAsStateWithLifecycle()
    val adminQueue by viewModel.adminWithdrawalQueue.collectAsStateWithLifecycle()
    val adminBountyQueue by viewModel.adminBountyQueue.collectAsStateWithLifecycle()
    val creatorMilestones by viewModel.creatorMilestones.collectAsStateWithLifecycle()

    // Modals visibility
    val showLuckyWheel by viewModel.showLuckyWheelModal.collectAsStateWithLifecycle()
    val showDeposit by viewModel.showDepositModal.collectAsStateWithLifecycle()
    val showWithdraw by viewModel.showWithdrawModal.collectAsStateWithLifecycle()
    val showAuditDossier by viewModel.showAuditDossierModal.collectAsStateWithLifecycle()
    val showSyndicate by viewModel.showSyndicateModal.collectAsStateWithLifecycle()
    val showAiSupport by viewModel.showAiSupportModal.collectAsStateWithLifecycle()
    val showLanguage by viewModel.showLanguageModal.collectAsStateWithLifecycle()
    val showAdminPin by viewModel.showAdminPinModal.collectAsStateWithLifecycle()
    val showNotifications by viewModel.showNotificationSheet.collectAsStateWithLifecycle()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val miningSessionEndTimestamp by viewModel.miningSessionEndTimestamp.collectAsStateWithLifecycle()

    // Active Bounty Task selected for review modal
    var selectedBountyTask by remember { mutableStateOf<BountyTask?>(null) }
    var showCreatorMilestoneModal by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBackground),
        topBar = {
            if (currentTab != 5) { // Hide default top bar in admin backoffice
                TopBar(
                    unreadNotificationCount = unreadNotificationsCount,
                    onNotificationClick = { viewModel.showNotificationSheet.value = true },
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        bottomBar = {
            if (currentTab != 5) { // Hide bottom nav in admin backoffice
                BottomNavBar(
                    selectedTab = currentTab,
                    onTabSelected = { tabIdx -> viewModel.setTab(tabIdx) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CanvasBackground)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screenTransition"
            ) { tab ->
                when (tab) {
                    0 -> HomeScreen(
                        hashPower = hashPower,
                        btcPrice = btcPrice,
                        kasPrice = kasPrice,
                        walletBalanceUsdt = walletBalance,
                        miningSessionEndTimestampMs = miningSessionEndTimestamp,
                        liveTickers = liveTickers,
                        onClaimDailySpin = { viewModel.showLuckyWheelModal.value = true },
                        onExtendMining = { viewModel.extendMiningSession() },
                        onOpenAuditDossier = { viewModel.showAuditDossierModal.value = true },
                        onNavigateToPlans = { subTab ->
                            viewModel.setPlansSubTab(subTab)
                            viewModel.setTab(1)
                        }
                    )
                    1 -> PlansScreen(
                        subTabIndex = plansSubTab,
                        onSubTabChanged = { subTab -> viewModel.setPlansSubTab(subTab) },
                        activeContracts = activeContracts,
                        marketplacePlans = viewModel.marketplacePlans,
                        walletBalanceUsdt = walletBalance,
                        onToggleRestake = { contractId -> viewModel.toggleRestake(contractId) },
                        onActivatePlan = { plan -> viewModel.activatePlan(plan) },
                        onTriggerDeposit = { viewModel.showDepositModal.value = true }
                    )
                    2 -> WalletScreen(
                        userId = viewModel.userId,
                        walletBalanceUsdt = walletBalance,
                        btcPrice = btcPrice,
                        subTabIndex = walletSubTab,
                        onSubTabChanged = { subTab -> viewModel.setWalletSubTab(subTab) },
                        activityList = activityList,
                        payoutsList = payoutsList,
                        onDepositClick = { viewModel.showDepositModal.value = true },
                        onWithdrawClick = { viewModel.showWithdrawModal.value = true }
                    )
                    3 -> GrowthScreen(
                        referralCode = viewModel.referralCode,
                        bountyTasks = bountyTasks,
                        freeAdCooldownHours = viewModel.getFreeAdCooldownHoursRemaining(),
                        onClaimFreeAdSession = { viewModel.claimFreeAdSession() },
                        onOpenBountyModal = { task -> selectedBountyTask = task },
                        onOpenCreatorMilestoneModal = { showCreatorMilestoneModal = true },
                        onOpenSyndicateTerms = { viewModel.showSyndicateModal.value = true }
                    )
                    4 -> AccountScreen(
                        userId = viewModel.userId,
                        userEmail = viewModel.userEmail,
                        userRole = viewModel.userRole,
                        twoFactorEnabled = twoFactorEnabled,
                        selectedLanguage = selectedLanguage,
                        onToggle2FA = { viewModel.toggle2FA() },
                        onOpenLanguageModal = { viewModel.showLanguageModal.value = true },
                        onOpenAuditDossier = { viewModel.showAuditDossierModal.value = true },
                        onOpenAiSupport = { viewModel.showAiSupportModal.value = true },
                        onOpenAdminPin = { viewModel.showAdminPinModal.value = true }
                    )
                    5 -> {
                        val user by viewModel.currentUser.collectAsStateWithLifecycle()
                        if (user == null || user?.role != "super_admin") {
                            // if (!user || user.role !== 'super_admin') {
                            //   return <Navigate to="/" replace />;
                            // }
                            androidx.compose.runtime.LaunchedEffect(Unit) {
                                viewModel.setTab(0)
                            }
                        } else {
                            AdminScreen(
                                coldReserveUsdt = coldReserve,
                                hotReserveUsdt = hotReserve,
                                withdrawalQueue = adminQueue,
                                bountyQueue = adminBountyQueue,
                                creatorMilestones = creatorMilestones,
                                onApproveWithdrawal = { reqId -> viewModel.adminApproveWithdrawal(reqId) },
                                onRejectWithdrawal = { reqId -> viewModel.adminRejectWithdrawal(reqId) },
                                onApproveBounty = { subId -> viewModel.adminApproveBounty(subId) },
                                onRejectBounty = { subId, reason -> viewModel.adminRejectBounty(subId, reason) },
                                onDisburseMilestone = { id, giftName, notes -> viewModel.adminDisburseCreatorMilestone(id, giftName, notes) },
                                onRejectMilestone = { id, reason -> viewModel.adminRejectCreatorMilestone(id, reason) },
                                onExitAdmin = { viewModel.setTab(4) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // INTERACTIVE MODALS & GAMIFICATION
    // ==========================================
    if (showLuckyWheel) {
        LuckyWheelModal(
            canSpin = canSpinToday,
            isSpinning = isSpinning,
            lastResult = lastSpinResult,
            onDismiss = { viewModel.showLuckyWheelModal.value = false },
            onSpinTrigger = {
                viewModel.executeSpin { _, _, _ -> }
            }
        )
    }

    if (showDeposit) {
        DepositModal(
            onDismiss = { viewModel.showDepositModal.value = false },
            onSimulateDeposit = { amount, network ->
                viewModel.simulateDeposit(amount, network)
            }
        )
    }

    if (showWithdraw) {
        WithdrawModal(
            availableBalanceUsdt = walletBalance,
            onDismiss = { viewModel.showWithdrawModal.value = false },
            onSubmitWithdrawal = { amount, address, network ->
                viewModel.requestWithdrawal(amount, address, network)
            }
        )
    }

    if (showAuditDossier) {
        AuditDossierModal(
            onDismiss = { viewModel.showAuditDossierModal.value = false }
        )
    }

    if (showSyndicate) {
        SyndicateTermSheetModal(
            onDismiss = { viewModel.showSyndicateModal.value = false }
        )
    }

    if (showAiSupport) {
        AiSupportChatModal(
            messages = chatMessages,
            isTyping = isAiTyping,
            onSendMessage = { query -> viewModel.sendChatMessage(query) },
            onDismiss = { viewModel.showAiSupportModal.value = false }
        )
    }

    if (showAdminPin) {
        AdminPinModal(
            onDismiss = { viewModel.showAdminPinModal.value = false },
            onUnlock = { pin -> viewModel.unlockAdmin(pin) }
        )
    }

    if (showLanguage) {
        LanguageSelectorModal(
            currentLanguage = selectedLanguage,
            onSelectLanguage = { lang -> viewModel.selectLanguage(lang) },
            onDismiss = { viewModel.showLanguageModal.value = false }
        )
    }

    if (showNotifications) {
        NotificationSheet(
            notifications = viewModel.notifications,
            onMarkAllAsRead = { viewModel.clearNotifications() },
            onDismiss = { viewModel.showNotificationSheet.value = false }
        )
    }

    if (selectedBountyTask != null) {
        BountySubmissionModal(
            task = selectedBountyTask!!,
            userId = viewModel.userId,
            referralCode = viewModel.referralCode,
            onDismiss = { selectedBountyTask = null },
            onSubmitWhatsApp = { views, time -> viewModel.submitWhatsAppBounty(views, time) },
            onSubmitTelegram = { username -> viewModel.submitTelegramBounty(username) }
        )
    }

    if (showCreatorMilestoneModal) {
        CreatorMilestoneModal(
            userId = viewModel.userId,
            referralCode = viewModel.referralCode,
            onDismiss = { showCreatorMilestoneModal = false },
            onSubmit = { channelUrl, videoUrl, contactTelegram ->
                viewModel.submitCreatorMilestone(channelUrl, videoUrl, contactTelegram)
            }
        )
    }
}
