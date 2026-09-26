package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.service.AuthStepResult
import com.example.ui.modals.ForgotPasswordModal
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
fun AuthScreen(
    onLoginSubmit: (String, String, (AuthStepResult) -> Unit) -> Unit,
    onSignUpSubmit: (String, String, String, String, String, (AuthStepResult) -> Unit) -> Unit,
    onAuthSuccess: (User) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Login, 1: Sign Up

    // Login Form States
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isLoginPasswordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordModal by remember { mutableStateOf(false) }

    // Sign Up Form States
    var signupName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var signupReferralCode by remember { mutableStateOf("") }
    var isSignupPasswordVisible by remember { mutableStateOf(false) }
    var isSignupConfirmPasswordVisible by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    var isReferralDetectedFromClipboard by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1 && signupReferralCode.isBlank()) {
            val clipText = clipboardManager.getText()?.text?.trim() ?: ""
            val regex = Regex("(?i)\\b(HG-[A-Z0-9]{4,6})\\b")
            val match = regex.find(clipText)
            if (match != null) {
                val detected = match.value.uppercase()
                signupReferralCode = detected
                isReferralDetectedFromClipboard = true
                Toast.makeText(context, "Referral code applied from clipboard: $detected ✅", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            Spacer(modifier = Modifier.height(16.dp))

            // BRAND LOGO & HERO HEADER
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianNavy)
                    .border(1.5.dp, GoldGradientMid, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = "HashGrid Icon",
                    tint = GoldGradientMid,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "HASHGRID",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = ObsidianNavy
            )

            Text(
                text = "Sub-Zero Geothermal Mining Network",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SlateGray,
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // MAIN AUTH CARD (LOG IN / SIGN UP)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, GoldBorderSubtle, RoundedCornerShape(24.dp))
                    .testTag("auth_card"),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // TAB SELECTOR (Log In / Sign Up)
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color(0xFFF4F0E8),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = GoldGradientEnd
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = {
                                selectedTabIndex = 0
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "LOG IN",
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabIndex == 0) ObsidianNavy else SlateGray,
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_login")
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = {
                                selectedTabIndex = 1
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "SIGN UP",
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabIndex == 1) ObsidianNavy else SlateGray,
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_signup")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Message Banner
                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFF0F0))
                                .border(1.dp, Color(0xFFFFCCCC), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                fontSize = 11.sp,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (selectedTabIndex == 0) {
                        // ==========================================
                        // 1. LOG IN FORM
                        // ==========================================
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Email Address",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = loginEmail,
                                onValueChange = { loginEmail = it },
                                placeholder = { Text("miner@institution.is", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.AlternateEmail, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldGradientEnd,
                                    unfocusedBorderColor = GoldBorderSubtle
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_login_email")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Password",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                placeholder = { Text("••••••••", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isLoginPasswordVisible = !isLoginPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isLoginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password",
                                            tint = SlateGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (isLoginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldGradientEnd,
                                    unfocusedBorderColor = GoldBorderSubtle
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_login_password")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldGradientEnd,
                                    modifier = Modifier
                                        .clickable {
                                            showForgotPasswordModal = true
                                        }
                                        .testTag("forgot_password_btn")
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    isLoading = true
                                    errorMessage = null
                                    onLoginSubmit(loginEmail, loginPassword) { res ->
                                        isLoading = false
                                        when (res) {
                                            is AuthStepResult.Authenticated -> {
                                                onAuthSuccess(res.user)
                                            }
                                            is AuthStepResult.Failure -> {
                                                errorMessage = res.message
                                            }
                                            else -> {
                                                // Handled upstream by navigation state
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("btn_login_submit"),
                                colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = MintGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = "LOG IN TO DASHBOARD",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.6.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    } else {
                        // ==========================================
                        // 2. SIGN UP FORM
                        // ==========================================
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Full Name / Username",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = signupName,
                                onValueChange = { signupName = it },
                                placeholder = { Text("e.g. Alex Thor", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldGradientEnd,
                                    unfocusedBorderColor = GoldBorderSubtle
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_signup_name")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Email Address (Gmail / Corporate)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = signupEmail,
                                onValueChange = { signupEmail = it },
                                placeholder = { Text("miner@institution.is", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.AlternateEmail, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldGradientEnd,
                                    unfocusedBorderColor = GoldBorderSubtle
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_signup_email")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Password (Min 6 characters)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = signupPassword,
                                onValueChange = { signupPassword = it },
                                placeholder = { Text("••••••••", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isSignupPasswordVisible = !isSignupPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isSignupPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password",
                                            tint = SlateGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (isSignupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldGradientEnd,
                                    unfocusedBorderColor = GoldBorderSubtle
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_signup_password")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Confirm Password",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = signupConfirmPassword,
                                onValueChange = { signupConfirmPassword = it },
                                placeholder = { Text("••••••••", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isSignupConfirmPasswordVisible = !isSignupConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isSignupConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password",
                                            tint = SlateGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (isSignupConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldGradientEnd,
                                    unfocusedBorderColor = GoldBorderSubtle
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_signup_confirm_password")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Referral Code (Optional)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateGray
                                )
                                if (isReferralDetectedFromClipboard) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFE8F5E9))
                                            .border(0.8.dp, MintGreen, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Referral code applied from clipboard ✅",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MintGreen
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = signupReferralCode,
                                onValueChange = {
                                    signupReferralCode = it.uppercase()
                                    isReferralDetectedFromClipboard = false
                                },
                                placeholder = { Text("e.g. HG-78A2", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(GoldGradientEnd.copy(alpha = 0.15f))
                                            .clickable {
                                                val clipText = clipboardManager.getText()?.text?.trim() ?: ""
                                                val regex = Regex("(?i)\\b(HG-[A-Z0-9]{4,6})\\b")
                                                val match = regex.find(clipText)
                                                val pasteCode = if (match != null) match.value.uppercase() else clipText.uppercase()
                                                if (pasteCode.isNotBlank()) {
                                                    signupReferralCode = pasteCode
                                                    isReferralDetectedFromClipboard = true
                                                    Toast.makeText(context, "Referral code applied from clipboard: $pasteCode ✅", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("paste_referral_btn")
                                    ) {
                                        Text(
                                            text = "PASTE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = ObsidianNavy
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldGradientEnd,
                                    unfocusedBorderColor = GoldBorderSubtle
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_signup_referral")
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // REGISTER BUTTON
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    val cleanEmail = signupEmail.trim()
                                    val cleanPass = signupPassword.trim()
                                    val cleanConfirm = signupConfirmPassword.trim()
                                    val cleanName = signupName.trim()

                                    if (cleanName.isBlank()) {
                                        errorMessage = "Please enter your full name."
                                        return@Button
                                    }
                                    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                                        errorMessage = "Please enter a valid email address."
                                        return@Button
                                    }
                                    if (cleanPass.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }
                                    if (cleanPass != cleanConfirm) {
                                        errorMessage = "Passwords do not match."
                                        return@Button
                                    }

                                    isLoading = true
                                    errorMessage = null

                                    onSignUpSubmit(cleanName, cleanEmail, cleanPass, cleanConfirm, signupReferralCode) { res ->
                                        isLoading = false
                                        when (res) {
                                            is AuthStepResult.Authenticated -> {
                                                onAuthSuccess(res.user)
                                            }
                                            is AuthStepResult.Failure -> {
                                                errorMessage = res.message
                                            }
                                            else -> {
                                                // Handled upstream by navigation state
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("btn_signup_submit"),
                                colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = MintGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = "CREATE MINER ACCOUNT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.6.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
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
                    text = "End-to-End Multi-Sig Escrow & 256-Bit Hardware Security",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showForgotPasswordModal) {
            ForgotPasswordModal(
                initialEmail = loginEmail,
                onDismiss = { showForgotPasswordModal = false }
            )
        }
    }
}
