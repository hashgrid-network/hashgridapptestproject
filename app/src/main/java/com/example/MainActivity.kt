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
import com.example.service.SessionManager
import com.example.service.UpdateStatus
import android.widget.Toast
import com.example.ui.components.BottomNavBar
import com.example.ui.components.TopBar
import com.example.ui.modals.AccountSettingsModal
import com.example.ui.modals.AdminControlPanelModal
import com.example.ui.modals.RecentBroadcastsSheet
import com.example.util.TickerEngine
import com.example.ui.modals.AiSupportChatModal
import com.example.ui.modals.AppUpdateModal
import com.example.ui.modals.AuditDossierModal
import com.example.ui.modals.BountySubmissionModal
import com.example.ui.modals.CreatorMilestoneModal
import com.example.ui.modals.DepositModal
import com.example.ui.modals.KycSubmissionModal
import com.example.ui.modals.LanguageSelectorModal
import com.example.ui.modals.LuckyWheelModal
import com.example.ui.modals.RewardDialog
import com.example.ui.modals.NotificationSheet
import com.example.ui.modals.SyndicateTermSheetModal
import com.example.ui.modals.WithdrawModal
import com.example.ui.modals.StarterGridDeployModal
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.TotpSetupScreen
import com.example.ui.screens.TwoFactorAuthScreen
import com.example.ui.screens.GrowthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlansScreen
import com.example.ui.screens.WalletScreen
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.service.FirebaseSyncService
import com.example.service.TotpHelper
import com.example.ui.theme.CardWhite
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy
import com.example.ui.theme.CanvasBackground
import com.google.firebase.FirebaseApp
import com.example.ui.theme.HashGridTheme
import com.example.viewmodel.HashGridViewModel

sealed class AuthWorkflowState {
    object AuthScreenView : AuthWorkflowState()
    data class SplashSecurityCheck(
        val uid: String,
        val email: String,
        val displayName: String
    ) : AuthWorkflowState()
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
    data class SecurityCheckError(
        val uid: String,
        val email: String,
        val displayName: String,
        val errorMessage: String
    ) : AuthWorkflowState()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            HashGridApplication.ensureFirebaseInitialized(applicationContext)
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
    val sessionManager = remember { SessionManager.getInstance(context) }
    val initialWorkflowState = remember {
        val fbAuth = AuthService.firebaseAuth
        val fbUser = try { fbAuth?.currentUser } catch (_: Exception) { null }
        val savedUser = AuthService.currentUser.value
        val uid = fbUser?.uid ?: savedUser?.id
        val isVerified = sessionManager.isDeviceVerified(uid)

        if (fbUser != null || !uid.isNullOrBlank()) {
            if (isVerified && uid != null) {
                // CASE 1: (currentUser != null && isVerified == true)
                // Skip all Login, SignUp, and 2FA screens completely.
                // Set start destination directly to "dashboard" (Instant entry for daily mining).
                AuthService.isSession2FAVerified = true
                val accountId = savedUser?.id ?: ("HG-" + uid.takeLast(6).uppercase())
                val refCode = savedUser?.referralCode ?: ("HG-" + uid.takeLast(4).uppercase())
                val verifiedUser = savedUser ?: User(
                    id = accountId,
                    email = fbUser?.email ?: savedUser?.email ?: "",
                    role = "user",
                    referralCode = refCode,
                    displayName = fbUser?.displayName ?: savedUser?.displayName ?: "Miner"
                )
                viewModel.onDirectAuthSuccess(verifiedUser)
                AuthWorkflowState.AuthScreenView
            } else if (uid != null) {
                // CASE 2: (currentUser != null && isVerified == false)
                // Check Firestore if 'totp_enabled == true'
                AuthWorkflowState.SplashSecurityCheck(
                    uid = uid,
                    email = fbUser?.email ?: savedUser?.email ?: "",
                    displayName = fbUser?.displayName ?: savedUser?.displayName ?: "Miner"
                )
            } else {
                AuthWorkflowState.AuthScreenView
            }
        } else {
            // CASE 3: (currentUser == null)
            AuthWorkflowState.AuthScreenView
        }
    }
    var authWorkflowState by remember { mutableStateOf<AuthWorkflowState>(initialWorkflowState) }

    LaunchedEffect(currentUser?.id) {
        if (currentUser != null && currentUser?.id?.isNotBlank() == true) {
            viewModel.initPersistence(context)
        }
    }

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
    val referralCount by viewModel.referralCount.collectAsStateWithLifecycle()
    val bonusHashrate by viewModel.bonusHashrate.collectAsStateWithLifecycle()

    // Gamification state
    val canSpinToday by viewModel.canSpinToday.collectAsStateWithLifecycle()
    val wheelCooldownEnd by viewModel.wheelCooldownEnd.collectAsStateWithLifecycle()
    val isSpinning by viewModel.isSpinning.collectAsStateWithLifecycle()
    val lastSpinResult by viewModel.spinResultText.collectAsStateWithLifecycle()
    val wonRewardSlice by viewModel.wonRewardSlice.collectAsStateWithLifecycle()
    val showRewardDialog by viewModel.showRewardDialog.collectAsStateWithLifecycle()

    // Live Tickers
    val liveTickers by viewModel.liveTickers.collectAsStateWithLifecycle()
    val isWsConnected by viewModel.isWsConnected.collectAsStateWithLifecycle()
    val wsStatusText by viewModel.wsStatusText.collectAsStateWithLifecycle()

    // Update Status
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()

    // Modals visibility
    val showLuckyWheel by viewModel.showLuckyWheelModal.collectAsStateWithLifecycle()
    val showDeposit by viewModel.showDepositModal.collectAsStateWithLifecycle()
    val showStarterDeploy by viewModel.showStarterDeployModal.collectAsStateWithLifecycle()
    val showWithdraw by viewModel.showWithdrawModal.collectAsStateWithLifecycle()
    val showAuditDossier by viewModel.showAuditDossierModal.collectAsStateWithLifecycle()
    val showSyndicate by viewModel.showSyndicateModal.collectAsStateWithLifecycle()
    val showAiSupport by viewModel.showAiSupportModal.collectAsStateWithLifecycle()
    val showLanguage by viewModel.showLanguageModal.collectAsStateWithLifecycle()
    val showNotifications by viewModel.showNotificationSheet.collectAsStateWithLifecycle()
    val showKyc by viewModel.showKycModal.collectAsStateWithLifecycle()
    val kycStatus by viewModel.kycStatus.collectAsStateWithLifecycle()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val miningSessionEndTimestamp by viewModel.miningSessionEndTimestamp.collectAsStateWithLifecycle()
    val gridCoinBalance by viewModel.gridCoinBalance.collectAsStateWithLifecycle()
    val isGridMiningActive by viewModel.isGridMiningActive.collectAsStateWithLifecycle()
    val effectiveGridRate by viewModel.effectiveGridRate.collectAsStateWithLifecycle()
    val showAdminPanelModal by viewModel.showAdminPanelModal.collectAsStateWithLifecycle()

    // Active Bounty Task selected for review modal
    var selectedBountyTask by remember { mutableStateOf<BountyTask?>(null) }
    var showCreatorMilestoneModal by remember { mutableStateOf(false) }
    var showAccountSettingsModal by remember { mutableStateOf(false) }
    var showRecentBroadcastsModal by remember { mutableStateOf(false) }
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
                is AuthWorkflowState.SplashSecurityCheck -> {
                    SplashSecurityCheckScreen(
                        uid = state.uid,
                        email = state.email,
                        displayName = state.displayName,
                        onNavigateToSetup = { secret ->
                            authWorkflowState = AuthWorkflowState.TotpSetupView(
                                uid = state.uid,
                                email = state.email,
                                displayName = state.displayName,
                                secret = secret
                            )
                        },
                        onNavigateToChallenge = { secret ->
                            authWorkflowState = AuthWorkflowState.TotpChallengeView(
                                uid = state.uid,
                                email = state.email,
                                displayName = state.displayName,
                                secret = secret
                            )
                        },
                        onDirectDashboard = {
                            val accountId = "HG-" + state.uid.takeLast(6).uppercase()
                            val refCode = "HG-" + state.uid.takeLast(4).uppercase()
                            val verifiedUser = User(
                                id = accountId,
                                email = state.email,
                                role = "user",
                                referralCode = refCode,
                                displayName = state.displayName
                            )
                            viewModel.onDirectAuthSuccess(verifiedUser)
                            authWorkflowState = AuthWorkflowState.AuthScreenView
                        },
                        onError = { errMsg ->
                            authWorkflowState = AuthWorkflowState.SecurityCheckError(
                                uid = state.uid,
                                email = state.email,
                                displayName = state.displayName,
                                errorMessage = errMsg
                            )
                        }
                    )
                }
                is AuthWorkflowState.SecurityCheckError -> {
                    SecurityCheckErrorScreen(
                        uid = state.uid,
                        email = state.email,
                        displayName = state.displayName,
                        errorMessage = state.errorMessage,
                        onRetry = {
                            authWorkflowState = AuthWorkflowState.SplashSecurityCheck(
                                uid = state.uid,
                                email = state.email,
                                displayName = state.displayName
                            )
                        },
                        onSignOut = {
                            viewModel.logout()
                            authWorkflowState = AuthWorkflowState.AuthScreenView
                        }
                    )
                }
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
                        onSignOut = {
                            viewModel.logout()
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
                        onSignOut = {
                            viewModel.logout()
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
                        onProfileClick = { showAccountSettingsModal = true },
                        onNotificationClick = { showRecentBroadcastsModal = true },
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
                                activeContracts = activeContracts,
                                gridCoinBalance = gridCoinBalance,
                                isGridMiningActive = isGridMiningActive,
                                effectiveGridRate = effectiveGridRate,
                                onClaimDailySpin = { viewModel.showLuckyWheelModal.value = true },
                                onExtendMining = { viewModel.extendMiningSession() },
                                onOpenAuditDossier = {
                                    auditDossierTab = 0
                                    viewModel.showAuditDossierModal.value = true
                                },
                                onNavigateToPlans = { subTab ->
                                    viewModel.setPlansSubTab(subTab)
                                    viewModel.setTab(1)
                                },
                                onDeployStarterPlan = {
                                    viewModel.showStarterDeployModal.value = true
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
                                activeContracts = activeContracts,
                                onDepositClick = { viewModel.showDepositModal.value = true },
                                onWithdrawClick = {
                                    viewModel.showWithdrawModal.value = true
                                }
                            )
                            3 -> GrowthScreen(
                                referralCode = viewModel.referralCode,
                                referralCount = referralCount,
                                bonusHashrate = bonusHashrate,
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
                                onCheckForUpdates = { viewModel.checkForUpdates(context) },
                                onLogout = {
                                    sessionManager.clearDeviceSession()
                                    viewModel.logout()
                                    authWorkflowState = AuthWorkflowState.AuthScreenView
                                }
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
            cooldownEndTimestamp = wheelCooldownEnd,
            lastResult = lastSpinResult,
            onDismiss = { viewModel.showLuckyWheelModal.value = false },
            onSpinStart = { viewModel.selectNextWheelSlice() },
            onSpinComplete = { slice ->
                viewModel.onWheelSpinCompleted(slice)
            }
        )
    }

    if (showRewardDialog && wonRewardSlice != null) {
        RewardDialog(
            wonSlice = wonRewardSlice!!,
            onDismiss = { viewModel.showRewardDialog.value = false },
            onCollect = { viewModel.showRewardDialog.value = false }
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

    if (showStarterDeploy) {
        StarterGridDeployModal(
            walletBalanceUsdt = walletBalance,
            onDismiss = { viewModel.showStarterDeployModal.value = false },
            onDeployWithBalance = {
                val success = viewModel.deployStarterRig()
                if (success) {
                    Toast.makeText(context, "🚀 Starter Rig Deployed! Mining at +10 TH/s", Toast.LENGTH_LONG).show()
                    viewModel.showStarterDeployModal.value = false
                } else {
                    Toast.makeText(context, "Insufficient Funds - Please Deposit $10 USDT", Toast.LENGTH_SHORT).show()
                    viewModel.showStarterDeployModal.value = false
                    viewModel.showDepositModal.value = true
                }
            },
            onTriggerDeposit = {
                viewModel.showStarterDeployModal.value = false
                viewModel.showDepositModal.value = true
            }
        )
    }

    if (showWithdraw) {
        WithdrawModal(
            availableBalanceUsdt = walletBalance,
            lockedAuditBalanceUsdt = viewModel.lockedAuditBalanceUsdt.collectAsStateWithLifecycle().value,
            activeContracts = activeContracts,
            withdrawableUnlockedBalanceUsdt = viewModel.withdrawableUnlockedBalance.collectAsStateWithLifecycle().value,
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

    if (showAccountSettingsModal) {
        AccountSettingsModal(
            userEmail = viewModel.userEmail,
            onLogout = {
                viewModel.logout()
            },
            onOpenAdminPanel = {
                viewModel.showAdminPanelModal.value = true
            },
            onDismiss = { showAccountSettingsModal = false }
        )
    }

    if (showAdminPanelModal) {
        AdminControlPanelModal(
            adminEmail = viewModel.userEmail,
            currentUsdtBalance = walletBalance,
            currentGridBalance = gridCoinBalance,
            marketplacePlans = viewModel.marketplacePlans,
            pendingPayouts = payoutsList,
            onInjectUsdt = { amt -> viewModel.injectUsdt(amt) },
            onInjectGrid = { amt -> viewModel.injectGrid(amt) },
            onFreeDeployRig = { plan -> viewModel.freeDeployRig(plan) },
            onApproveWithdrawal = { payoutId -> viewModel.approveWithdrawal(payoutId) },
            onRejectWithdrawal = { payoutId -> viewModel.rejectWithdrawal(payoutId) },
            onDismiss = { viewModel.showAdminPanelModal.value = false }
        )
    }

    if (showRecentBroadcastsModal) {
        RecentBroadcastsSheet(
            broadcasts = TickerEngine.getRecentBroadcasts(),
            onDismiss = { showRecentBroadcastsModal = false }
        )
    }
}

@Composable
fun SplashSecurityCheckScreen(
    uid: String,
    email: String,
    displayName: String,
    onNavigateToSetup: (secret: String) -> Unit,
    onNavigateToChallenge: (secret: String) -> Unit,
    onDirectDashboard: () -> Unit,
    onError: (String) -> Unit
) {
    // Hard back-press lock during initial security verification
    BackHandler(enabled = true) {
        // Intentionally no-op to lock user on splash
    }

    val context = LocalContext.current

    LaunchedEffect(uid) {
        try {
            // Fetch security & TOTP status from Firestore / RTDB
            val (enabled, secret) = FirebaseSyncService.fetchTotpDetails(uid)
            if (!enabled || secret.isNullOrBlank()) {
                // CASE A: Document does NOT exist or 'totp_enabled != true'
                val newSecret = TotpHelper.generateSecret(16)
                onNavigateToSetup(newSecret)
            } else {
                // CASE B: 'totp_enabled == true'
                if (AuthService.isSession2FAVerified || SessionManager.getInstance(context).isDeviceVerified(uid)) {
                    AuthService.isSession2FAVerified = true
                    onDirectDashboard()
                } else {
                    onNavigateToChallenge(secret)
                }
            }
        } catch (e: Exception) {
            // CASE C: If network error or failure fetching state
            onError(e.localizedMessage ?: "Multi-sig security verification failed.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ObsidianNavy)
                    .border(2.dp, GoldGradientMid, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Vault",
                    tint = GoldGradientMid,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "HASHGRID MULTI-SIG VAULT",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = ObsidianNavy
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Verifying Institutional 2FA Credentials...",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SlateGray
            )

            Spacer(modifier = Modifier.height(28.dp))

            CircularProgressIndicator(
                color = GoldGradientEnd,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun SecurityCheckErrorScreen(
    uid: String,
    email: String,
    displayName: String,
    errorMessage: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    BackHandler(enabled = true) {
        // Locked
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF0F0))
                        .border(1.5.dp, Color(0xFFD32F2F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Warning",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Security Verification Failed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = ObsidianNavy,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorMessage,
                    fontSize = 12.sp,
                    color = SlateGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("btn_retry_security_check"),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                ) {
                    Text(
                        text = "RETRY SECURITY HANDSHAKE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.6.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("btn_error_sign_out"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateGray),
                    border = BorderStroke(1.dp, GoldBorderSubtle)
                ) {
                    Text(
                        text = "SIGN OUT / SWITCH ACCOUNT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                }
            }
        }
    }
}
