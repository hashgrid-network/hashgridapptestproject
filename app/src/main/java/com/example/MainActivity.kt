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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.BountyTask
import com.example.service.AppUpdateManager
import com.example.service.AuthService
import com.example.service.UpdateStatus
import android.widget.Toast
import com.example.ui.components.BottomNavBar
import com.example.ui.components.TopBar
import com.example.ui.modals.AdminVerificationModal
import com.example.ui.modals.AiSupportChatModal
import com.example.ui.modals.AppUpdateModal
import com.example.ui.modals.AuditDossierModal
import com.example.ui.modals.BountySubmissionModal
import com.example.ui.modals.CreatorMilestoneModal
import com.example.ui.modals.DepositModal
import com.example.ui.modals.KycSubmissionModal
import com.example.ui.modals.LanguageSelectorModal
import com.example.ui.modals.LuckyWheelModal
import com.example.ui.modals.NotificationSheet
import com.example.ui.modals.SyndicateTermSheetModal
import com.example.ui.modals.WithdrawModal
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.TotpSetupScreen
import com.example.ui.screens.TwoFactorAuthScreen
import com.example.ui.screens.GrowthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlansScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.CanvasBackground
import com.google.firebase.FirebaseApp
import com.example.ui.theme.HashGridTheme
import com.example.viewmodel.HashGridViewModel

sealed class AuthWorkflowState {
    object AuthScreenView : AuthWorkflowState()
    data class TotpSetupView(
        val uid: String,
        val email: String,
        val displayName: String,
        val secret: String
    ) : AuthWorkflowState()
    data class TotpChallengeView(
        val uid: String,
        val email: String,
        val displayName: String,
        val secret: String
    ) : AuthWorkflowState()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            FirebaseApp.initializeApp(applicationContext)
            com.example.service.FirebaseAppCheckManager.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            AuthService.init(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            AppUpdateManager.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
    val context = LocalContext.current

    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var authWorkflowState by remember { mutableStateOf<AuthWorkflowState>(AuthWorkflowState.AuthScreenView) }

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
    val adminBountyClaims by viewModel.adminBountyClaims.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiTyping by viewModel.isAiTyping.collectAsStateWithLifecycle()
    val twoFactorEnabled by viewModel.twoFactorEnabled.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    // Gamification state
    val canSpinToday by viewModel.canSpinToday.collectAsStateWithLifecycle()
    val isSpinning by viewModel.isSpinning.collectAsStateWithLifecycle()
    val lastSpinResult by viewModel.spinResultText.collectAsStateWithLifecycle()

    // Live Tickers
    val liveTickers by viewModel.liveTickers.collectAsStateWithLifecycle()
    val isWsConnected by viewModel.isWsConnected.collectAsStateWithLifecycle()
    val wsStatusText by viewModel.wsStatusText.collectAsStateWithLifecycle()

    // Update Status
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()

    // Modals visibility
    val showLuckyWheel by viewModel.showLuckyWheelModal.collectAsStateWithLifecycle()
    val showDeposit by viewModel.showDepositModal.collectAsStateWithLifecycle()
    val showWithdraw by viewModel.showWithdrawModal.collectAsStateWithLifecycle()
    val showAuditDossier by viewModel.showAuditDossierModal.collectAsStateWithLifecycle()
    val showSyndicate by viewModel.showSyndicateModal.collectAsStateWithLifecycle()
    val showAiSupport by viewModel.showAiSupportModal.collectAsStateWithLifecycle()
    val showLanguage by viewModel.showLanguageModal.collectAsStateWithLifecycle()
    val showNotifications by viewModel.showNotificationSheet.collectAsStateWithLifecycle()
    val showAdminVerification by viewModel.showAdminVerificationModal.collectAsStateWithLifecycle()
    val showKyc by viewModel.showKycModal.collectAsStateWithLifecycle()
    val kycStatus by viewModel.kycStatus.collectAsStateWithLifecycle()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val miningSessionEndTimestamp by viewModel.miningSessionEndTimestamp.collectAsStateWithLifecycle()

    // Active Bounty Task selected for review modal
    var selectedBountyTask by remember { mutableStateOf<BountyTask?>(null) }
    var showCreatorMilestoneModal by remember { mutableStateOf(false) }
    var auditDossierTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    AnimatedContent(
        targetState = isLoggedIn,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AuthSessionTransition"
    ) { authenticated ->
        if (!authenticated) {
            // ==========================================
            // AUTH WORKFLOW (LOGIN / SIGN UP / 2FA TOTP)
            // ==========================================
            when (val state = authWorkflowState) {
                is AuthWorkflowState.TotpSetupView -> {
                    TotpSetupScreen(
                        uid = state.uid,
                        email = state.email,
                        displayName = state.displayName,
                        totpSecret = state.secret,
                        onSetupSuccess = { user ->
                            viewModel.onDirectAuthSuccess(user)
                            authWorkflowState = AuthWorkflowState.AuthScreenView
                        },
                        onBackToLogin = {
                            authWorkflowState = AuthWorkflowState.AuthScreenView
                        }
                    )
                }
                is AuthWorkflowState.TotpChallengeView -> {
                    TwoFactorAuthScreen(
                        uid = state.uid,
                        email = state.email,
                        displayName = state.displayName,
                        totpSecret = state.secret,
                        onAuthSuccess = { user ->
                            viewModel.onDirectAuthSuccess(user)
                            authWorkflowState = AuthWorkflowState.AuthScreenView
                        },
                        onBackToLogin = {
                            authWorkflowState = AuthWorkflowState.AuthScreenView
                        }
                    )
                }
                else -> {
                    AuthScreen(
                        onLoginSubmit = { email, pass, onResult ->
                            viewModel.login(context, email, pass) { stepResult ->
                                when (stepResult) {
                                    is com.example.service.AuthStepResult.RequireTotpSetup -> {
                                        authWorkflowState = AuthWorkflowState.TotpSetupView(
                                            uid = stepResult.uid,
                                            email = stepResult.email,
                                            displayName = stepResult.displayName,
                                            secret = stepResult.totpSecret
                                        )
                                    }
                                    is com.example.service.AuthStepResult.RequireTotpChallenge -> {
                                        authWorkflowState = AuthWorkflowState.TotpChallengeView(
                                            uid = stepResult.uid,
                                            email = stepResult.email,
                                            displayName = stepResult.displayName,
                                            secret = stepResult.totpSecret
                                        )
                                    }
                                    is com.example.service.AuthStepResult.Authenticated -> {
                                        viewModel.onDirectAuthSuccess(stepResult.user)
                                        authWorkflowState = AuthWorkflowState.AuthScreenView
                                    }
                                    is com.example.service.AuthStepResult.Failure -> {}
                                }
                                onResult(stepResult)
                            }
                        },
                        onSignUpSubmit = { name, email, pass, confirm, ref, onResult ->
                            viewModel.signUp(context, name, email, pass, confirm, ref) { stepResult ->
                                when (stepResult) {
                                    is com.example.service.AuthStepResult.RequireTotpSetup -> {
                                        authWorkflowState = AuthWorkflowState.TotpSetupView(
                                            uid = stepResult.uid,
                                            email = stepResult.email,
                                            displayName = stepResult.displayName,
                                            secret = stepResult.totpSecret
                                        )
                                    }
                                    is com.example.service.AuthStepResult.RequireTotpChallenge -> {
                                        authWorkflowState = AuthWorkflowState.TotpChallengeView(
                                            uid = stepResult.uid,
                                            email = stepResult.email,
                                            displayName = stepResult.displayName,
                                            secret = stepResult.totpSecret
                                        )
                                    }
                                    is com.example.service.AuthStepResult.Authenticated -> {
                                        viewModel.onDirectAuthSuccess(stepResult.user)
                                        authWorkflowState = AuthWorkflowState.AuthScreenView
                                    }
                                    is com.example.service.AuthStepResult.Failure -> {}
                                }
                                onResult(stepResult)
                            }
                        },
                        onGoogleSignInClick = { onResult ->
                            viewModel.signInWithGoogle(context, onResult)
                        },
                        onAuthSuccess = { user ->
                            viewModel.onDirectAuthSuccess(user)
                            authWorkflowState = AuthWorkflowState.AuthScreenView
                        }
                    )
                }
            }
        } else {
            // ==========================================
            // 2. MAIN APP SCAFFOLD & DASHBOARD
            // ==========================================
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CanvasBackground),
                topBar = {
                    TopBar(
                        unreadNotificationCount = unreadNotificationsCount,
                        onNotificationClick = { viewModel.showNotificationSheet.value = true },
                        modifier = Modifier.statusBarsPadding()
                    )
                },
                bottomBar = {
                    BottomNavBar(
                        selectedTab = currentTab,
                        onTabSelected = { tabIdx -> viewModel.setTab(tabIdx) }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition"
                    ) { targetIndex ->
                        when (targetIndex) {
                            0 -> HomeScreen(
                                hashPower = hashPower,
                                btcPrice = btcPrice,
                                kasPrice = kasPrice,
                                walletBalanceUsdt = walletBalance,
                                miningSessionEndTimestampMs = miningSessionEndTimestamp,
                                liveTickers = liveTickers,
                                onClaimDailySpin = { viewModel.showLuckyWheelModal.value = true },
                                onExtendMining = { viewModel.extendMiningSession() },
                                onOpenAuditDossier = {
                                    auditDossierTab = 0
                                    viewModel.showAuditDossierModal.value = true
                                },
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
                                onActivatePlan = { plan ->
                                    val success = viewModel.activatePlan(plan)
                                    if (!success) {
                                        Toast.makeText(context, "Insufficient Funds - Please Deposit", Toast.LENGTH_SHORT).show()
                                        viewModel.showDepositModal.value = true
                                    }
                                    success
                                },
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
                                onWithdrawClick = {
                                    if (walletBalance < 130.0) {
                                        Toast.makeText(context, "Minimum withdrawal threshold is $130 USDT. Current balance: $${String.format(java.util.Locale.US, "%.2f", walletBalance)}", Toast.LENGTH_LONG).show()
                                    }
                                    viewModel.showWithdrawModal.value = true
                                }
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
                                displayName = viewModel.userDisplayName,
                                kycStatus = kycStatus,
                                twoFactorEnabled = twoFactorEnabled,
                                selectedLanguage = selectedLanguage,
                                onToggle2FA = { viewModel.toggle2FA() },
                                onOpenKycModal = { viewModel.showKycModal.value = true },
                                onOpenLanguageModal = { viewModel.showLanguageModal.value = true },
                                onOpenAuditDossier = {
                                    auditDossierTab = 0
                                    viewModel.showAuditDossierModal.value = true
                                },
                                onOpenAuditDossierWithTab = { tab ->
                                    auditDossierTab = tab
                                    viewModel.showAuditDossierModal.value = true
                                },
                                onOpenAiSupport = { viewModel.showAiSupportModal.value = true },
                                onOpenAdminDashboard = { viewModel.showAdminVerificationModal.value = true },
                                onCheckForUpdates = { viewModel.checkForUpdates(context) },
                                onLogout = { viewModel.logout() }
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // IN-APP AUTO UPDATE MODAL DIALOG
    // ==========================================
    if (updateStatus is UpdateStatus.UpdateAvailable ||
        updateStatus is UpdateStatus.Downloading ||
        updateStatus is UpdateStatus.ReadyToInstall
    ) {
        AppUpdateModal(
            status = updateStatus,
            onDismiss = { viewModel.dismissAppUpdate() },
            onUpdateNow = { info -> viewModel.startAppUpdate(context, info) },
            onInstallNow = { file -> viewModel.installAppUpdate(context, file) }
        )
    }

    // ==========================================
    // INTERACTIVE MODALS
    // ==========================================
    if (showLuckyWheel) {
        LuckyWheelModal(
            canSpin = canSpinToday,
            isSpinning = isSpinning,
            lastResult = lastSpinResult,
            onDismiss = { viewModel.showLuckyWheelModal.value = false },
            onSpinTrigger = {
                viewModel.spinLuckyWheel { _, _ -> }
            }
        )
    }

    if (showDeposit) {
        DepositModal(
            userId = viewModel.userId,
            onDismiss = { viewModel.showDepositModal.value = false },
            onDepositSuccess = { amount, paymentId ->
                viewModel.processDeposit(amount, paymentId, "NOWPayments-USDT")
                viewModel.showDepositModal.value = false
            }
        )
    }

    if (showWithdraw) {
        WithdrawModal(
            availableBalanceUsdt = walletBalance,
            lockedAuditBalanceUsdt = viewModel.lockedAuditBalanceUsdt.collectAsStateWithLifecycle().value,
            onDismiss = { viewModel.showWithdrawModal.value = false },
            onSubmitWithdrawal = { amount, address, network ->
                viewModel.requestWithdrawal(amount, address, network)
            }
        )
    }

    if (showAuditDossier) {
        AuditDossierModal(
            initialTabIndex = auditDossierTab,
            onDismiss = { viewModel.showAuditDossierModal.value = false }
        )
    }

    if (showKyc) {
        KycSubmissionModal(
            currentStatus = kycStatus,
            onDismiss = { viewModel.showKycModal.value = false },
            onSubmitKyc = { fullName, idType, idNumber ->
                viewModel.submitKyc(fullName, idType, idNumber)
                Toast.makeText(context, "KYC submitted. Status updated to PENDING REVIEW.", Toast.LENGTH_LONG).show()
                viewModel.showKycModal.value = false
            }
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
            onSubmitTelegram = { username -> viewModel.submitTelegramBounty(username) },
            onSubmitYouTube = { url, channel -> viewModel.submitYouTubeBounty(url, channel) }
        )
    }

    if (showAdminVerification) {
        AdminVerificationModal(
            claims = adminBountyClaims,
            onDismiss = { viewModel.showAdminVerificationModal.value = false },
            onApproveClaim = { claim -> viewModel.approveBountyClaim(claim) },
            onRejectClaim = { claim, reason -> viewModel.rejectBountyClaim(claim, reason) }
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
