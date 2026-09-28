package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.service.AuthService
import com.example.service.AuthStepResult
import com.example.ui.theme.CardWhite
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldBrush
import com.example.ui.theme.GoldLight
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateNavy
import kotlinx.coroutines.launch

enum class PinMode {
    SET_PIN,
    CONFIRM_PIN,
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
    val coroutineScope = rememberCoroutineScope()

    val walletAddress = remember { AuthService.getOrCreateWalletAddress(context) }
    val hasSavedPin = remember { AuthService.hasSavedPin(context) }
    var pinMode by remember { mutableStateOf(if (hasSavedPin) PinMode.UNLOCK_PIN else PinMode.SET_PIN) }

    var initialPin by remember { mutableStateOf("") }
    var pinBuffer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

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
                            val res = AuthService.createWalletWithPin(context, newBuffer)
                            isLoading = false
                            res.onSuccess { user ->
                                onAuthSuccess(user)
                            }.onFailure { err ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                errorMessage = err.localizedMessage ?: "Failed to initialize Web3 Wallet"
                                pinBuffer = ""
                                initialPin = ""
                                pinMode = PinMode.SET_PIN
                            }
                        }
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        errorMessage = "PINs do not match. Try again."
                        pinBuffer = ""
                        initialPin = ""
                        pinMode = PinMode.SET_PIN
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
                            errorMessage = err.localizedMessage ?: "Incorrect PIN, try again"
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

    fun copyWalletAddress() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Wallet Address", walletAddress)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Wallet address copied to clipboard!", Toast.LENGTH_SHORT).show()
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
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
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "HASHGRID",
                    color = GoldLight,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "NON-CUSTODIAL WEB3 WALLET",
                    color = Web3Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Wallet Address Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                        .clickable { copyWalletAddress() }
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
                        contentDescription = "Copy Wallet Address",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = when (pinMode) {
                        PinMode.SET_PIN -> "Create 4-Digit PIN to Secure Your Wallet"
                        PinMode.CONFIRM_PIN -> "Confirm Your 4-Digit PIN"
                        PinMode.UNLOCK_PIN -> "Enter Your 4-Digit PIN to Unlock"
                    },
                    color = CardWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (pinMode) {
                        PinMode.SET_PIN -> "Create a 4-digit security PIN to access your decentralized wallet"
                        PinMode.CONFIRM_PIN -> "Re-enter your 4-digit PIN to confirm key creation"
                        PinMode.UNLOCK_PIN -> "Unlock your non-custodial quantum mining node"
                    },
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4 Indicator Dots
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

                Spacer(modifier = Modifier.height(14.dp))

                // Error / Loading Message
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
                                text = "Syncing Non-Custodial Wallet Node...",
                                color = Web3Cyan,
                                fontSize = 12.sp
                            )
                        }
                    } else if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = CrimsonRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 0-9 Numeric Keypad Grid
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("RESET", "0", "DEL")
                )

                keypadRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { item ->
                            when (item) {
                                "RESET" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = !isLoading) { handleReset() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reset PIN",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                "DEL" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = !isLoading) { handleBackspace() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete Digit",
                                            tint = Web3Cyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
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
                                            fontSize = 24.sp,
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
