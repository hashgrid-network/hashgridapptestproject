package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.service.AuthService
import com.example.service.TotpHelper
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CardWhite
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy

@Composable
fun TwoFactorAuthScreen(
    uid: String,
    email: String,
    displayName: String = "Miner",
    totpSecret: String,
    onAuthSuccess: (User) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

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
            Spacer(modifier = Modifier.height(20.dp))

            // SECURITY SHIELD ICON
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Two-Factor Authentication",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = ObsidianNavy,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter the 6-digit verification code generated by your Google Authenticator app for account:",
                fontSize = 12.sp,
                color = SlateGray,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = email,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GoldGradientEnd,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CHALLENGE CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.2.dp, GoldBorderSubtle, RoundedCornerShape(24.dp))
                    .testTag("totp_challenge_card"),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Authenticator Code",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                otpInput = it
                                errorMessage = null
                            }
                        },
                        placeholder = { Text("000000", fontSize = 18.sp, color = Color.Gray, fontFamily = FontFamily.Monospace) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(20.dp)
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
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_totp_challenge_code")
                    )

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

                    // VERIFY BUTTON
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (otpInput.length != 6) {
                                errorMessage = "Please enter your 6-digit Authenticator code."
                                return@Button
                            }

                            isVerifying = true
                            errorMessage = null

                            val isValid = TotpHelper.verifyTotp(otpInput, totpSecret)
                            if (isValid) {
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
                                Toast.makeText(context, "Verification Successful! Welcome back.", Toast.LENGTH_SHORT).show()
                                onAuthSuccess(verifiedUser)
                            } else {
                                isVerifying = false
                                errorMessage = "Incorrect code. Try again."
                                Toast.makeText(context, "Incorrect code. Try again.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isVerifying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("btn_verify_challenge_code"),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(color = MintGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = "VERIFY & ENTER DASHBOARD",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Back to Sign In",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        modifier = Modifier
                            .clickable { onBackToLogin() }
                            .padding(8.dp)
                            .testTag("btn_challenge_back")
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
                    text = "End-to-End Multi-Sig Escrow & Hardware Security",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateGray
                )
            }
        }
    }
}
