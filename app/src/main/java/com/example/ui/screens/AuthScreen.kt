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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.modals.ForgotPasswordModal
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CardWhite
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthScreen(
    onGoogleSignIn: (String, String?, (Result<User>) -> Unit) -> Unit = { _, _, _ -> },
    onLoginSubmit: (String, String, (AuthStepResult) -> Unit) -> Unit = { _, _, _ -> },
    onSignUpSubmit: (String, String, String, String, String, (AuthStepResult) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onAuthSuccess: (User) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    // Referral code state
    var referralCode by remember { mutableStateOf("") }
    var isReferralDetectedFromClipboard by remember { mutableStateOf(false) }

    // Dialog & error states
    var showReferralPromptDialog by remember { mutableStateOf(false) }
    var dialogReferralInput by remember { mutableStateOf("") }
    var showAdminConsole by remember { mutableStateOf(false) }
    var showForgotPasswordModal by remember { mutableStateOf(false) }

    // Admin fallback login inputs (Strictly for emergency God Mode / parkashom8080@gmail.com)
    var adminEmail by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var isAdminPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Clipboard invite detection
    LaunchedEffect(Unit) {
        val clipText = clipboardManager.getText()?.text?.trim() ?: ""
        val detected = if (clipText.startsWith("HG-", ignoreCase = true)) {
            clipText.substringBefore(" ").substringBefore("&").substringBefore("?").uppercase()
        } else {
            val regex = Regex("(?i)HG-[A-Z0-9]{2,10}")
            regex.find(clipText)?.value?.uppercase()
        }
        if (!detected.isNullOrBlank()) {
            referralCode = detected
            isReferralDetectedFromClipboard = true
            Toast.makeText(context, "Syndicate invite applied from clipboard: $detected ✅", Toast.LENGTH_SHORT).show()
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
                isLoading = true
                errorMessage = null
                val cleanRef = referralCode.trim().ifBlank { null }
                onGoogleSignIn(idToken, cleanRef) { authRes ->
                    isLoading = false
                    authRes.onSuccess { user ->
                        Toast.makeText(context, "Welcome, ${user.displayName}! Verified with Google ✅", Toast.LENGTH_SHORT).show()
                        onAuthSuccess(user)
                    }.onFailure { err ->
                        errorMessage = err.localizedMessage ?: "Google sign-in failed. Please try again."
                    }
                }
            } else {
                isLoading = false
                errorMessage = "Google authentication returned an empty ID token. Please verify Google Play Services."
            }
        } catch (e: ApiException) {
            isLoading = false
            if (e.statusCode != 12501) { // 12501 is user cancelled
                errorMessage = "Google Sign-In failed (${e.statusCode}): ${e.localizedMessage ?: e.message}"
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = e.localizedMessage ?: "Google Sign-In error"
        }
    }

    val launchGoogleSignIn: (String?) -> Unit = { refInput ->
        if (!refInput.isNullOrBlank()) {
            referralCode = refInput.trim().uppercase()
        }
        try {
            googleSignInClient.signOut().addOnCompleteListener {
                googleLauncher.launch(googleSignInClient.signInIntent)
            }
        } catch (_: Exception) {
            googleLauncher.launch(googleSignInClient.signInIntent)
        }
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

            // MAIN AUTH CARD
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
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // VERIFIED BADGE
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldLight.copy(alpha = 0.5f))
                            .border(1.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = GoldGradientEnd,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "INSTITUTIONAL VERIFIED ACCESS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = ObsidianNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "1-Tap Institutional Access",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ObsidianNavy
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Connect with your verified Google account. Unverified email registration has been disabled to protect network security & mining difficulty.",
                        fontSize = 11.5.sp,
                        color = SlateGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

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
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ==========================================
                    // 1. PROMINENT "CONTINUE WITH GOOGLE" BUTTON
                    // ==========================================
                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            if (referralCode.isBlank()) {
                                showReferralPromptDialog = true
                            } else {
                                launchGoogleSignIn(referralCode)
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, GoldBrush, RoundedCornerShape(16.dp))
                            .testTag("btn_google_signin"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianNavy,
                            disabledContainerColor = ObsidianNavy.copy(alpha = 0.7f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = GoldGradientMid,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Official Google 'G' icon in clean white circle
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_google_logo),
                                        contentDescription = "Google Logo",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "Continue with Google",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Verified 1-Tap Login & Cloud Sync",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GoldGradientMid
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // DIVIDER / SYNDICATE INVITE SECTION
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
                            text = "SYNDICATE SPONSORSHIP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(GoldBorderSubtle)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // OPTIONAL REFERRAL CODE INPUT FIELD
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
                        value = referralCode,
                        onValueChange = {
                            referralCode = it.uppercase()
                            isReferralDetectedFromClipboard = false
                        },
                        placeholder = { Text("e.g. HG-8080", fontSize = 13.sp, color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(18.dp)
                            )
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
                                            referralCode = detected
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

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "New miners entering a valid sponsor code receive an instant +1.5 GH/s hashrate speed boost.",
                        fontSize = 10.sp,
                        color = SlateGray,
                        lineHeight = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // ZERO-TOLERANCE ANTI-SYBIL COMPLIANCE NOTE
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF9F7F2))
                            .border(1.dp, GoldBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = GoldGradientEnd,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Existing miners are seamlessly restored upon Google sign-in. New miners are auto-provisioned with segregated cloud wallets.",
                            fontSize = 10.sp,
                            color = SlateNavy,
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // OPTIONAL EXPANDABLE INSTITUTIONAL MASTER KEY CONSOLE
                    TextButton(
                        onClick = { showAdminConsole = !showAdminConsole },
                        modifier = Modifier.testTag("toggle_admin_console_btn")
                    ) {
                        Text(
                            text = if (showAdminConsole) "Hide Master Console" else "Institutional Master Console Access",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateGray
                        )
                    }

                    if (showAdminConsole) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF4F0E8))
                                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Institutional Master / Administrator Login",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = adminEmail,
                                onValueChange = { adminEmail = it },
                                placeholder = { Text("parkashom8080@gmail.com", fontSize = 12.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(Icons.Default.AlternateEmail, null, tint = GoldGradientEnd, modifier = Modifier.size(16.dp))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_admin_email")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = adminPassword,
                                onValueChange = { adminPassword = it },
                                placeholder = { Text("Master Password", fontSize = 12.sp, color = Color.Gray) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, null, tint = GoldGradientEnd, modifier = Modifier.size(16.dp))
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isAdminPasswordVisible = !isAdminPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isAdminPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = SlateGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (isAdminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_admin_password")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldGradientEnd,
                                    modifier = Modifier.clickable { showForgotPasswordModal = true }
                                )

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        isLoading = true
                                        errorMessage = null
                                        onLoginSubmit(adminEmail, adminPassword) { res ->
                                            isLoading = false
                                            when (res) {
                                                is AuthStepResult.Authenticated -> onAuthSuccess(res.user)
                                                is AuthStepResult.Failure -> errorMessage = res.message
                                                else -> {}
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                                    modifier = Modifier.testTag("btn_admin_login")
                                ) {
                                    Text("AUTHENTICATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

        // SWIFT 1-TIME OPTIONAL REFERRAL DIALOG
        if (showReferralPromptDialog) {
            Dialog(onDismissRequest = { showReferralPromptDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, GoldBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Syndicate Referral Code",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                            IconButton(
                                onClick = { showReferralPromptDialog = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = SlateGray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Enter a sponsor's invite code to claim an instant +1.5 GH/s hashrate speed boost, or skip to start with standard institutional rate.",
                            fontSize = 11.5.sp,
                            color = SlateGray,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = dialogReferralInput,
                            onValueChange = { dialogReferralInput = it.uppercase() },
                            placeholder = { Text("e.g. HG-8080 (Optional)", fontSize = 12.sp, color = Color.Gray) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GoldGradientEnd.copy(alpha = 0.15f))
                                        .clickable {
                                            val clip = clipboardManager.getText()?.text?.trim() ?: ""
                                            if (clip.isNotBlank()) dialogReferralInput = clip.uppercase()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("PASTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ObsidianNavy)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldGradientEnd,
                                unfocusedBorderColor = GoldBorderSubtle
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    showReferralPromptDialog = false
                                    launchGoogleSignIn(null)
                                }
                            ) {
                                Text("SKIP & PROCEED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateGray)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    showReferralPromptDialog = false
                                    launchGoogleSignIn(dialogReferralInput.ifBlank { null })
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                            ) {
                                Text("APPLY & SIGN IN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (showForgotPasswordModal) {
            ForgotPasswordModal(
                initialEmail = adminEmail,
                onDismiss = { showForgotPasswordModal = false },
                onPasswordResetSuccess = { resetEmail ->
                    adminEmail = resetEmail
                }
            )
        }
    }
}
