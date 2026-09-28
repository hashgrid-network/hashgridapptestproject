package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.User
import com.example.service.AuthService
import com.example.service.AuthStepResult
import com.example.ui.theme.CardWhite
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldBrush
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy
import kotlinx.coroutines.launch

enum class PinMode {
    SET_PIN,
    CONFIRM_PIN,
    RESTORE_MINER,
    UNLOCK_PIN
}

val Web3Cyan = Color(0xFF00F2FE)

@Composable
fun AuthScreen(
    onGoogleSignIn: (String, String?, (Result<User>) -> Unit) -> Unit = { _, _, _ -> },
    onLoginSubmit: (String, String, (AuthStepResult) -> Unit) -> Unit = { _, _, _ -> },
    onSignUpSubmit: (String, String, String, String, String, (AuthStepResult) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onCheckEmailVerification: (String?, (Result<User>) -> Unit) -> Unit = { _, _ -> },
    onResendVerificationEmail: ((Result<String>) -> Unit) -> Unit = { _ -> },
    onForgotPassword: (String, (Result<String>) -> Unit) -> Unit = { _, _ -> },
    onAuthSuccess: (User) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val walletAddress = remember { AuthService.getOrCreateWalletAddress(context) }
    val hasSavedPin = remember { AuthService.hasSavedPin(context) }

    val pendingRefCode by com.example.service.DeepLinkManager.pendingReferralCode.collectAsStateWithLifecycle()
    val initialRefCode = pendingRefCode ?: ""
    var userReferralInput by remember { mutableStateOf(initialRefCode) }

    androidx.compose.runtime.LaunchedEffect(pendingRefCode) {
        val code = pendingRefCode
        if (!code.isNullOrBlank() && userReferralInput.isBlank()) {
            userReferralInput = code
        }
    }

    // Tab Index: 0 = Create New Miner (SET_PIN/CONFIRM_PIN), 1 = Restore Existing Miner
    var selectedAuthTab by remember { mutableIntStateOf(if (hasSavedPin) 0 else 0) }
    var pinMode by remember { mutableStateOf(if (hasSavedPin) PinMode.UNLOCK_PIN else PinMode.SET_PIN) }

    var initialPin by remember { mutableStateOf("") }
    var pinBuffer by remember { mutableStateOf("") }
    var restoreMinerKeyInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Backup Key Modal State for Newly Created Miners
    var showBackupModal by remember { mutableStateOf(false) }
    var createdUserForBackup by remember { mutableStateOf<User?>(null) }

    fun copyText(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun handleDigitInput(digit: String) {
        if (isLoading || pinBuffer.length >= 4) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        errorMessage = null

        val newBuffer = pinBuffer + digit
        pinBuffer = newBuffer

        if (newBuffer.length == 4) {
            when (pinMode) {
                PinMode.SET_PIN -> {
                    initialPin = newBuffer
                    pinBuffer = ""
                    pinMode = PinMode.CONFIRM_PIN
                }
                PinMode.CONFIRM_PIN -> {
                    if (newBuffer == initialPin) {
                        isLoading = true
                        coroutineScope.launch {
                            val res = AuthService.createWalletWithPin(context, newBuffer, userReferralInput.trim().ifBlank { null })
                            isLoading = false
                            res.onSuccess { user ->
                                createdUserForBackup = user
                                showBackupModal = true
                            }.onFailure { err ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                errorMessage = err.localizedMessage ?: "Failed to initialize Cloud Miner"
                                pinBuffer = ""
                                initialPin = ""
                                pinMode = PinMode.SET_PIN
                            }
                        }
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        errorMessage = "PINs do not match. Please start over."
                        pinBuffer = ""
                        initialPin = ""
                        pinMode = PinMode.SET_PIN
                    }
                }
                PinMode.RESTORE_MINER -> {
                    val cleanKey = restoreMinerKeyInput.trim().uppercase()
                    if (cleanKey.isBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        errorMessage = "Please enter your Secret Miner Key (e.g. HG-D867C42B1F9A)."
                        pinBuffer = ""
                        return
                    }

                    isLoading = true
                    coroutineScope.launch {
                        val res = AuthService.restoreMinerNodeWithKeyAndPin(context, cleanKey, newBuffer)
                        isLoading = false
                        res.onSuccess { user ->
                            onAuthSuccess(user)
                        }.onFailure { err ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            errorMessage = err.localizedMessage ?: "Failed to restore miner node."
                            pinBuffer = ""
                        }
                    }
                }
                PinMode.UNLOCK_PIN -> {
                    isLoading = true
                    coroutineScope.launch {
                        val res = AuthService.authenticateWithPin(context, newBuffer)
                        isLoading = false
                        res.onSuccess { user ->
                            onAuthSuccess(user)
                        }.onFailure { err ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            errorMessage = err.localizedMessage ?: "Incorrect 4-Digit PIN. Please try again."
                            pinBuffer = ""
                        }
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (isLoading || pinBuffer.isEmpty()) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        pinBuffer = pinBuffer.dropLast(1)
        errorMessage = null
    }

    fun handleReset() {
        if (isLoading) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        pinBuffer = ""
        errorMessage = null
        if (pinMode == PinMode.CONFIRM_PIN) {
            initialPin = ""
            pinMode = PinMode.SET_PIN
        }
    }

    // ==========================================
    // MANDATORY SECRET MINER KEY BACKUP DIALOG
    // ==========================================
    if (showBackupModal && createdUserForBackup != null) {
        Dialog(onDismissRequest = { /* Require explicit confirmation */ }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.5.dp, GoldBorder, RoundedCornerShape(26.dp))
                    .testTag("secret_miner_key_backup_dialog"),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(GoldLight)
                            .border(1.5.dp, GoldBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = GoldGradientEnd,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SECRET MINER KEY BACKUP",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = ObsidianNavy,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Save your Secret Miner Key! You will need this key + your 4-digit PIN to restore your cloud miner node if you reinstall the app or switch devices.",
                        fontSize = 11.sp,
                        color = SlateGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secret Miner Key Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SlateNavy)
                            .border(1.dp, GoldBorderSubtle, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = createdUserForBackup?.id ?: walletAddress,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1-Tap Copy Button
                    Button(
                        onClick = {
                            copyText(createdUserForBackup?.id ?: walletAddress, "Secret Miner Key")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("copy_secret_miner_key_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldLight)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "COPY SECRET MINER KEY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldGradientEnd
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Continue Button
                    Button(
                        onClick = {
                            showBackupModal = false
                            createdUserForBackup?.let { onAuthSuccess(it) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("continue_to_dashboard_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MintGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CONTINUE TO DASHBOARD",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Branding Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GoldBrush)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(SlateNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Web3 Security Shield",
                            tint = Web3Cyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "HASHGRID",
                    color = GoldLight,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "DECENTRALIZED CLOUD MINER",
                    color = Web3Cyan,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Auth Tab Switcher (Create New vs Restore Existing) if not in UNLOCK_PIN mode
                if (pinMode != PinMode.UNLOCK_PIN) {
                    TabRow(
                        selectedTabIndex = selectedAuthTab,
                        containerColor = Color(0xFF0F172A),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedAuthTab]),
                                color = Web3Cyan
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedAuthTab == 0,
                            onClick = {
                                selectedAuthTab = 0
                                pinMode = PinMode.SET_PIN
                                pinBuffer = ""
                                initialPin = ""
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "Create Cloud Miner",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedAuthTab == 0) Web3Cyan else Color.Gray
                                )
                            }
                        )

                        Tab(
                            selected = selectedAuthTab == 1,
                            onClick = {
                                selectedAuthTab = 1
                                pinMode = PinMode.RESTORE_MINER
                                pinBuffer = ""
                                initialPin = ""
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "Restore Miner",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedAuthTab == 1) Web3Cyan else Color.Gray
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // If Restore Mode: Input Secret Miner Key field
                if (pinMode == PinMode.RESTORE_MINER) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = restoreMinerKeyInput,
                                onValueChange = {
                                    restoreMinerKeyInput = it
                                    errorMessage = null
                                },
                                label = { Text("Secret Miner Key (HG-XXXXXXXXXXXX)", fontSize = 11.sp) },
                                placeholder = { Text("e.g. HG-D867C42B1F9A", fontSize = 10.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_miner_key_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Web3Cyan,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedLabelColor = Web3Cyan,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val clipText = clipboardManager.getText()?.text?.toString() ?: ""
                                    if (clipText.isNotBlank()) {
                                        restoreMinerKeyInput = clipText.trim().uppercase()
                                        Toast.makeText(context, "Pasted Secret Miner Key!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        tint = Web3Cyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PASTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else if (pinMode != PinMode.RESTORE_MINER) {
                    // Miner Address Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                            .clickable { copyText(walletAddress, "Miner Node Address") }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Web3Cyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = walletAddress,
                            color = CardWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Miner Node Address",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (pinMode == PinMode.SET_PIN || pinMode == PinMode.CONFIRM_PIN) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = userReferralInput,
                                onValueChange = {
                                    userReferralInput = it.uppercase()
                                    com.example.service.DeepLinkManager.setPendingReferralCode(context, it)
                                },
                                label = { Text("Referral Code / Parent Miner ID (Optional)", fontSize = 11.sp) },
                                placeholder = { Text("e.g. HG-808080", fontSize = 10.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("referral_code_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MintGreen,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedLabelColor = MintGreen,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                trailingIcon = {
                                    if (userReferralInput.isNotBlank()) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Verified Referral Code",
                                            tint = MintGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )

                            if (userReferralInput.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = "⚡ Referred By: ${userReferralInput.trim().uppercase()} (Verified)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MintGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Text(
                    text = when (pinMode) {
                        PinMode.SET_PIN -> "Create 4-Digit Security PIN"
                        PinMode.CONFIRM_PIN -> "Confirm Your 4-Digit PIN"
                        PinMode.RESTORE_MINER -> "Enter 4-Digit Security PIN"
                        PinMode.UNLOCK_PIN -> "Enter Your 4-Digit PIN to Unlock"
                    },
                    color = CardWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (pinMode) {
                        PinMode.SET_PIN -> "Enter a 4-digit PIN to secure your cloud mining terminal"
                        PinMode.CONFIRM_PIN -> "Re-enter your 4-digit PIN to verify accuracy"
                        PinMode.RESTORE_MINER -> "Enter your 4-digit PIN to restore your existing cloud miner"
                        PinMode.UNLOCK_PIN -> "Unlock your non-custodial quantum mining node"
                    },
                    color = Color.LightGray,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 4 Dynamic Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < pinBuffer.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) Web3Cyan else Color(0xFF1E293B))
                                .border(
                                    width = 1.5.dp,
                                    color = if (isFilled) Web3Cyan else Color(0xFF334155),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error / Loading Message Area
                Box(
                    modifier = Modifier.height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Web3Cyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Verifying Cloud Firestore Miner Node...",
                                color = Web3Cyan,
                                fontSize = 12.sp
                            )
                        }
                    } else if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = CrimsonRed,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 0-9 Numeric Keypad Grid
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("RESET", "0", "DEL")
                )

                keypadRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { item ->
                            when (item) {
                                "RESET" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = !isLoading) { handleReset() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reset PIN",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                "DEL" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = !isLoading) { handleBackspace() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete Digit",
                                            tint = Web3Cyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(SlateNavy)
                                            .border(1.dp, GoldBorderSubtle, CircleShape)
                                            .clickable(enabled = !isLoading) { handleDigitInput(item) }
                                            .testTag("keypad_btn_$item"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item,
                                            color = CardWhite,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
