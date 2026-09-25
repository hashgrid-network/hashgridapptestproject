package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.service.AuthService
import com.example.service.FirebaseSyncService
import com.example.service.TotpHelper
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CardWhite
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy
import kotlinx.coroutines.launch

@Composable
fun TotpSetupScreen(
    uid: String,
    email: String,
    displayName: String = "Miner",
    totpSecret: String,
    onSetupSuccess: (User) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var otpInput by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler {
        onBackToLogin()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // SECURITY SHIELD BADGE
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(ObsidianNavy)
                    .border(2.dp, GoldGradientMid, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "2FA Shield",
                    tint = GoldGradientMid,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Setup Google Authenticator",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = ObsidianNavy,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Protect your miner account and wallet balance with mandatory 2-Factor Authentication (2FA).",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SlateGray,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SETUP CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.2.dp, GoldBorderSubtle, RoundedCornerShape(24.dp))
                    .testTag("totp_setup_card"),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // STEP 1: SECRET KEY SECTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(GoldGradientMid),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Your Secret Key",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secret Key Display Box with 1-Tap Copy
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF6F3EB))
                            .border(1.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "BASE-32 SECRET KEY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateGray,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = totpSecret.chunked(4).joinToString(" "),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = ObsidianNavy,
                                    letterSpacing = 1.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(totpSecret))
                                    Toast.makeText(context, "Secret key copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianNavy)
                                    .testTag("btn_copy_totp_secret")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Secret",
                                    tint = GoldGradientMid,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // "OPEN GOOGLE AUTHENTICATOR" BUTTON (DISPATCHES OTPAUTH INTENT)
                    OutlinedButton(
                        onClick = {
                            val uriString = TotpHelper.getOtpAuthUri(email = email, secret = totpSecret)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    "Google Authenticator not found. Key copied to clipboard! Paste it manually in your 2FA app.",
                                    Toast.LENGTH_LONG
                                ).show()
                                clipboardManager.setText(AnnotatedString(totpSecret))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("btn_open_authenticator"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ObsidianNavy),
                        border = BorderStroke(1.dp, GoldGradientEnd)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OPEN GOOGLE AUTHENTICATOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // STEP 2: ENTER CODE SECTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(GoldGradientMid),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "2",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enter 6-Digit Code from App",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                otpInput = it
                                errorMessage = null
                            }
                        },
                        placeholder = { Text("000000", fontSize = 16.sp, color = Color.Gray, fontFamily = FontFamily.Monospace) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldGradientEnd,
                            unfocusedBorderColor = GoldBorderSubtle
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_totp_setup_code")
                    )

                    // Error Message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            fontSize = 11.sp,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // VERIFY & ACTIVATE 2FA BUTTON
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (otpInput.length != 6) {
                                errorMessage = "Please enter the 6-digit code from Authenticator."
                                return@Button
                            }

                            isVerifying = true
                            errorMessage = null

                            val isValid = TotpHelper.verifyTotp(otpInput, totpSecret)
                            if (isValid) {
                                scope.launch {
                                    FirebaseSyncService.saveTotpSecret(uid, totpSecret) { success ->
                                        isVerifying = false
                                        val accountId = "HG-" + uid.takeLast(6).uppercase()
                                        val refCode = "HG-" + uid.takeLast(4).uppercase()

                                        val verifiedUser = User(
                                            id = accountId,
                                            email = email,
                                            role = "user",
                                            referralCode = refCode,
                                            displayName = displayName,
                                            photoUrl = null,
                                            isFlaggedDuplicate = false
                                        )

                                        AuthService.setSessionDirect(verifiedUser, uid)
                                        Toast.makeText(context, "Google Authenticator 2FA Activated Successfully!", Toast.LENGTH_LONG).show()
                                        onSetupSuccess(verifiedUser)
                                    }
                                }
                            } else {
                                isVerifying = false
                                errorMessage = "Invalid 6-digit code. Please check your Authenticator app."
                                Toast.makeText(context, "Invalid 6-digit code. Please check your Authenticator app.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isVerifying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("btn_activate_2fa"),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(color = MintGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = "VERIFY & ACTIVATE 2FA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // BACK TO SIGN IN
                    Text(
                        text = "Cancel & Return to Login",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        modifier = Modifier
                            .clickable { onBackToLogin() }
                            .padding(8.dp)
                            .testTag("btn_totp_cancel")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECURITY NOTICE FOOTER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = GoldGradientEnd,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HashGrid Institutional Security Architecture",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateGray
                )
            }
        }
    }
}
