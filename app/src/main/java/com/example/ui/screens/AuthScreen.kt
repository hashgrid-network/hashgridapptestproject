package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.User
import com.example.service.AuthStepResult
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CardWhite
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldBrush
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

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
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Login, 1: Sign Up

    // Login fields
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isLoginPasswordVisible by remember { mutableStateOf(false) }

    // Sign Up fields
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var isSignupPasswordVisible by remember { mutableStateOf(false) }
    var isSignupConfirmPasswordVisible by remember { mutableStateOf(false) }
    var signupReferralCode by remember { mutableStateOf("") }
    var isReferralDetectedFromClipboard by remember { mutableStateOf(false) }

    // Verification Dialog State
    var showVerificationDialog by remember { mutableStateOf(false) }
    var pendingVerificationEmail by remember { mutableStateOf("") }
    var pendingAppliedReferralCode by remember { mutableStateOf("") }
    var verificationDialogError by remember { mutableStateOf<String?>(null) }
    var isCheckingVerification by remember { mutableStateOf(false) }
    var isResendingEmail by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Detect referral from clipboard on load or when switching to signup
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1 && signupReferralCode.isBlank()) {
            val clipText = clipboardManager.getText()?.text?.trim() ?: ""
            val detected = if (clipText.startsWith("HG-", ignoreCase = true)) {
                clipText.substringBefore(" ").substringBefore("&").substringBefore("?").uppercase()
            } else {
                val regex = Regex("(?i)HG-[A-Z0-9]{2,10}")
                regex.find(clipText)?.value?.uppercase()
            }
            if (!detected.isNullOrBlank()) {
                signupReferralCode = detected
                isReferralDetectedFromClipboard = true
                Toast.makeText(context, "Syndicate invite applied from clipboard: $detected ✅", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Google Sign-In setup
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                isGoogleLoading = true
                errorMessage = null
                val cleanRef = signupReferralCode.trim().ifBlank { null }
                onGoogleSignIn(idToken, cleanRef) { authRes ->
                    isGoogleLoading = false
                    authRes.onSuccess { user ->
                        Toast.makeText(context, "Welcome, ${user.displayName}! Verified with Google ✅", Toast.LENGTH_SHORT).show()
                        onAuthSuccess(user)
                    }.onFailure {
                        Toast.makeText(context, "Google Sign-in unavailable. Please use Email/Password.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                isGoogleLoading = false
                Toast.makeText(context, "Google Sign-in unavailable. Please use Email/Password.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            isGoogleLoading = false
            if (e.statusCode != 12501) {
                Toast.makeText(context, "Google Sign-in unavailable. Please use Email/Password.", Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {
            isGoogleLoading = false
            Toast.makeText(context, "Google Sign-in unavailable. Please use Email/Password.", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
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

            Spacer(modifier = Modifier.height(24.dp))

            // MAIN DUAL-TAB AUTH CARD
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

                    // Error Message Banner (User-Friendly, No Raw Code)
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
                                fontSize = 11.5.sp,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (selectedTabIndex == 0) {
                        // ==========================================
                        // 1. LOG IN TAB (Email & Password)
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
                                onValueChange = {
                                    loginEmail = it
                                    errorMessage = null
                                },
                                placeholder = { Text("miner@institution.is", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(Icons.Default.AlternateEmail, null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
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
                                onValueChange = {
                                    loginPassword = it
                                    errorMessage = null
                                },
                                placeholder = { Text("••••••••", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
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

                            // FORGOT PASSWORD BUTTON
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
                                            if (loginEmail.isBlank()) {
                                                errorMessage = "Please enter your email above to receive a password reset link."
                                            } else {
                                                isLoading = true
                                                onForgotPassword(loginEmail) { res ->
                                                    isLoading = false
                                                    res.onSuccess { msg ->
                                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    }.onFailure { err ->
                                                        errorMessage = err.localizedMessage ?: "Failed to send reset link."
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("forgot_password_btn")
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // LOG IN PRIMARY BUTTON
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (loginEmail.isBlank()) {
                                        errorMessage = "Please enter your email address."
                                        return@Button
                                    }
                                    if (loginPassword.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@Button
                                    }

                                    isLoading = true
                                    errorMessage = null

                                    onLoginSubmit(loginEmail, loginPassword) { res ->
                                        isLoading = false
                                        when (res) {
                                            is AuthStepResult.Authenticated -> onAuthSuccess(res.user)
                                            is AuthStepResult.RequireEmailVerification -> {
                                                pendingVerificationEmail = res.email
                                                pendingAppliedReferralCode = res.appliedCode
                                                verificationDialogError = null
                                                showVerificationDialog = true
                                            }
                                            is AuthStepResult.Failure -> errorMessage = res.message
                                            else -> {}
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
                        // 2. SIGN UP TAB (Email, Password, Confirm & Referral)
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
                                value = signupEmail,
                                onValueChange = {
                                    signupEmail = it
                                    errorMessage = null
                                },
                                placeholder = { Text("miner@institution.is", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(Icons.Default.AlternateEmail, null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
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
                                onValueChange = {
                                    signupPassword = it
                                    errorMessage = null
                                },
                                placeholder = { Text("••••••••", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
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
                                onValueChange = {
                                    signupConfirmPassword = it
                                    errorMessage = null
                                },
                                placeholder = { Text("••••••••", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
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

                            // REFERRAL CODE INPUT
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
                                            text = "Applied from invite ✅",
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
                                placeholder = { Text("e.g. HG-8080", fontSize = 13.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(Icons.Default.Key, null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(GoldGradientEnd.copy(alpha = 0.15f))
                                            .clickable {
                                                val clipText = clipboardManager.getText()?.text?.trim() ?: ""
                                                val detected = if (clipText.startsWith("HG-", ignoreCase = true)) {
                                                    clipText.substringBefore(" ").substringBefore("&").substringBefore("?").uppercase()
                                                } else {
                                                    val regex = Regex("(?i)HG-[A-Z0-9]{2,10}")
                                                    regex.find(clipText)?.value?.uppercase()
                                                } ?: clipText.uppercase()

                                                if (detected.isNotBlank()) {
                                                    signupReferralCode = detected
                                                    isReferralDetectedFromClipboard = true
                                                    Toast.makeText(context, "Referral code applied: $detected ✅", Toast.LENGTH_SHORT).show()
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

                            Spacer(modifier = Modifier.height(18.dp))

                            // CREATE ACCOUNT PRIMARY BUTTON
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    val cleanEmail = signupEmail.trim()
                                    val cleanPass = signupPassword.trim()
                                    val cleanConfirm = signupConfirmPassword.trim()

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

                                    val name = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                                    onSignUpSubmit(name, cleanEmail, cleanPass, cleanConfirm, signupReferralCode) { res ->
                                        isLoading = false
                                        when (res) {
                                            is AuthStepResult.Authenticated -> onAuthSuccess(res.user)
                                            is AuthStepResult.RequireEmailVerification -> {
                                                pendingVerificationEmail = res.email
                                                pendingAppliedReferralCode = res.appliedCode
                                                verificationDialogError = null
                                                showVerificationDialog = true
                                            }
                                            is AuthStepResult.Failure -> errorMessage = res.message
                                            else -> {}
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
                                        text = "CREATE ACCOUNT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.6.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── OR ── DIVIDER
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(GoldBorderSubtle)
                        )
                        Text(
                            text = "OR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(GoldBorderSubtle)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // OPTIONAL SECONDARY "CONTINUE WITH GOOGLE" BUTTON
                    Button(
                        onClick = {
                            if (isGoogleLoading) return@Button
                            try {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    googleLauncher.launch(googleSignInClient.signInIntent)
                                }
                            } catch (_: Exception) {
                                googleLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        enabled = !isGoogleLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.2.dp, GoldBrush, RoundedCornerShape(14.dp))
                            .testTag("btn_google_signin"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianNavy,
                            disabledContainerColor = ObsidianNavy.copy(alpha = 0.7f)
                        )
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                color = GoldGradientMid,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_google_logo),
                                        contentDescription = "Google Logo",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "Continue with Google",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
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

        // ==========================================
        // CHAMPAGNE GOLD EMAIL VERIFICATION DIALOG
        // ==========================================
        if (showVerificationDialog) {
            Dialog(onDismissRequest = { showVerificationDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, GoldBorder, RoundedCornerShape(24.dp))
                        .testTag("verification_dialog"),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { showVerificationDialog = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateGray, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Icon badge
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(ObsidianNavy)
                                .border(1.5.dp, GoldGradientMid, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = null,
                                tint = GoldGradientMid,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Verification Link Sent!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = ObsidianNavy,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "We sent a link to your Gmail inbox:\n$pendingVerificationEmail\nPlease open Gmail, click the link, and tap 'I Have Verified' below.",
                            fontSize = 12.sp,
                            color = SlateGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        if (verificationDialogError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFF0F0))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = verificationDialogError!!,
                                    fontSize = 11.sp,
                                    color = Color(0xFFD32F2F),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // BUTTON 1: "I HAVE VERIFIED"
                        Button(
                            onClick = {
                                isCheckingVerification = true
                                verificationDialogError = null
                                onCheckEmailVerification(pendingAppliedReferralCode) { res ->
                                    isCheckingVerification = false
                                    res.onSuccess { user ->
                                        showVerificationDialog = false
                                        Toast.makeText(context, "Email verified successfully! Welcome to HashGrid.", Toast.LENGTH_SHORT).show()
                                        onAuthSuccess(user)
                                    }.onFailure { err ->
                                        verificationDialogError = err.localizedMessage ?: "Email not verified yet. Please click the link in your email and tap 'I Have Verified'."
                                    }
                                }
                            },
                            enabled = !isCheckingVerification,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("btn_i_have_verified"),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                        ) {
                            if (isCheckingVerification) {
                                CircularProgressIndicator(color = MintGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("I HAVE VERIFIED", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // BUTTON 2: "RESEND EMAIL"
                        TextButton(
                            onClick = {
                                isResendingEmail = true
                                onResendVerificationEmail { res ->
                                    isResendingEmail = false
                                    res.onSuccess { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, err.localizedMessage ?: "Failed to resend email.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isResendingEmail,
                            modifier = Modifier.testTag("btn_resend_verification")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isResendingEmail) "Sending..." else "Resend Email",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldGradientEnd
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
