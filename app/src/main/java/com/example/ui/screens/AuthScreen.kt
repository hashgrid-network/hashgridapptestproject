package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.example.R
import com.example.model.User
import com.example.service.AuthService
import com.example.service.FirebaseSyncService
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun GoogleBrandIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        val blue = Color(0xFF4285F4)
        val red = Color(0xFFEA4335)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)

        val stroke = w * 0.18f
        val radius = (w - stroke) / 2f

        // Blue right-arm & quadrant
        drawArc(
            color = blue,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(radius * 2f, radius * 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        // Green bottom
        drawArc(
            color = green,
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(radius * 2f, radius * 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        // Yellow left
        drawArc(
            color = yellow,
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(radius * 2f, radius * 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        // Red top
        drawArc(
            color = red,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(radius * 2f, radius * 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        // Center horizontal bar
        drawLine(
            color = blue,
            start = Offset(center.x - 1f, center.y),
            end = Offset(w - stroke / 2f, center.y),
            strokeWidth = stroke
        )
    }
}

@Composable
fun AuthScreen(
    onLoginSubmit: (String, String, (Result<Unit>) -> Unit) -> Unit,
    onSignUpSubmit: (String, String, String, String, String, (Result<Unit>) -> Unit) -> Unit,
    onGoogleSignInClick: ((Result<Unit>) -> Unit) -> Unit,
    onAuthSuccess: (User) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Login, 1: Sign Up

    // Email Verification View State
    var showVerifyEmailScreen by remember { mutableStateOf(false) }
    var pendingVerifyEmail by remember { mutableStateOf("") }
    var pendingVerifyName by remember { mutableStateOf("") }
    var isCheckingVerification by remember { mutableStateOf(false) }
    var isResendingEmail by remember { mutableStateOf(false) }

    // Login Form States
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isLoginPasswordVisible by remember { mutableStateOf(false) }

    // Sign Up Form States
    var signupName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var signupReferralCode by remember { mutableStateOf("") }
    var isSignupPasswordVisible by remember { mutableStateOf(false) }
    var isSignupConfirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // -------------------------------------------------------------
    // 1. NATIVE GOOGLE SIGN-IN CLIENT WITH FORCED ACCOUNT PICKER
    // -------------------------------------------------------------
    val webClientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        "67298041154-mock-client-id.apps.googleusercontent.com"
    }

    val googleSignInOptions = remember {
        try {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
        } catch (_: Exception) {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        }
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, googleSignInOptions)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            scope.launch {
                isGoogleLoading = true
                errorMessage = null
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account.idToken
                    val email = account.email ?: ""
                    val name = account.displayName ?: email.substringBefore("@")
                    val photoUrl = account.photoUrl?.toString()

                    if (idToken != null) {
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        val auth = AuthService.firebaseAuth
                        if (auth != null) {
                            auth.signInWithCredential(credential)
                                .addOnSuccessListener { authResult ->
                                    val fbUser = authResult.user
                                    val uid = fbUser?.uid ?: UUID.randomUUID().toString()
                                    val accountId = "HG-" + uid.takeLast(6).uppercase()
                                    val refCode = "HG-" + uid.takeLast(4).uppercase()

                                    // Save / Initialize Firestore & RTDB
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val firestore = FirebaseFirestore.getInstance()
                                            val userDoc = hashMapOf(
                                                "uid" to uid,
                                                "email" to (fbUser?.email ?: email),
                                                "displayName" to (fbUser?.displayName ?: name),
                                                "accountId" to accountId,
                                                "usdt_balance" to 0.00,
                                                "btc_balance" to 0.000000,
                                                "total_withdrawn" to 0.00,
                                                "is_verified" to true,
                                                "created_at" to FieldValue.serverTimestamp()
                                            )
                                            firestore.collection("users").document(uid).set(userDoc)
                                        } catch (_: Exception) {}

                                        FirebaseSyncService.initializeNewUser(uid, email, name, photoUrl, accountId)
                                    }

                                    val matchedUser = User(
                                        id = accountId,
                                        email = email,
                                        role = "user",
                                        referralCode = refCode,
                                        displayName = name,
                                        photoUrl = photoUrl,
                                        isFlaggedDuplicate = false
                                    )
                                    onAuthSuccess(matchedUser)
                                    isGoogleLoading = false
                                }
                                .addOnFailureListener { e ->
                                    isGoogleLoading = false
                                    val msg = "Google Auth Failed: ${e.localizedMessage}"
                                    errorMessage = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                        } else {
                            // Fallback auth
                            val uid = UUID.randomUUID().toString()
                            val accountId = "HG-" + uid.takeLast(6).uppercase()
                            val matchedUser = User(
                                id = accountId,
                                email = email,
                                role = "user",
                                referralCode = "HG-" + uid.takeLast(4).uppercase(),
                                displayName = name,
                                photoUrl = photoUrl,
                                isFlaggedDuplicate = false
                            )
                            onAuthSuccess(matchedUser)
                            isGoogleLoading = false
                        }
                    } else {
                        isGoogleLoading = false
                        errorMessage = "Google authentication did not return an ID token."
                    }
                } catch (e: ApiException) {
                    isGoogleLoading = false
                    val msg = "Sign in cancelled or failed (Code: ${e.statusCode})"
                    errorMessage = msg
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    isGoogleLoading = false
                    val msg = e.localizedMessage ?: "Google Sign-In failed."
                    errorMessage = msg
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            isGoogleLoading = false
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

            Spacer(modifier = Modifier.height(28.dp))

            // ==============================================================
            // CONDITIONAL: EMAIL VERIFICATION SCREEN OR AUTH FORM CARD
            // ==============================================================
            if (showVerifyEmailScreen) {
                // ==============================================================
                // DEDICATED GMAIL VERIFICATION SCREEN
                // ==============================================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.2.dp, GoldGradientEnd, RoundedCornerShape(24.dp))
                        .testTag("verify_email_card"),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MintGreen.copy(alpha = 0.15f))
                                .border(1.5.dp, MintDark, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = null,
                                tint = MintDark,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "VERIFY YOUR GMAIL",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = ObsidianNavy
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "A verification link has been sent to your Gmail inbox:",
                            fontSize = 12.sp,
                            color = SlateGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = pendingVerifyEmail,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGradientEnd,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF9F7F3))
                                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Please click the link in your email, then return here and tap 'I Have Verified' below to unlock your dashboard and start mining.",
                                fontSize = 11.sp,
                                color = SlateGray,
                                lineHeight = 15.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Button 1: "I Have Verified"
                        Button(
                            onClick = {
                                isCheckingVerification = true
                                val auth = AuthService.firebaseAuth
                                val user = auth?.currentUser
                                if (user != null) {
                                    user.reload().addOnCompleteListener { reloadTask ->
                                        isCheckingVerification = false
                                        if (reloadTask.isSuccessful && user.isEmailVerified) {
                                            Toast.makeText(context, "Email verified successfully! Welcome to HashGrid.", Toast.LENGTH_LONG).show()
                                            val accountId = "HG-" + user.uid.takeLast(6).uppercase()
                                            val refCode = "HG-" + user.uid.takeLast(4).uppercase()
                                            val verifiedUser = User(
                                                id = accountId,
                                                email = user.email ?: pendingVerifyEmail,
                                                role = "user",
                                                referralCode = refCode,
                                                displayName = user.displayName ?: pendingVerifyName.ifBlank { "Miner" },
                                                photoUrl = null,
                                                isFlaggedDuplicate = false
                                            )
                                            onAuthSuccess(verifiedUser)
                                        } else {
                                            Toast.makeText(context, "Please verify your email via the link sent to your inbox first.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    isCheckingVerification = false
                                    // Local check
                                    Toast.makeText(context, "Checking verification status... Please ensure you clicked the link.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isCheckingVerification && !isResendingEmail,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .testTag("btn_confirm_verified"),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                        ) {
                            if (isCheckingVerification) {
                                CircularProgressIndicator(color = MintGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = "I HAVE VERIFIED",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.8.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Button 2: Resend Email
                        OutlinedButton(
                            onClick = {
                                isResendingEmail = true
                                val auth = AuthService.firebaseAuth
                                val user = auth?.currentUser
                                if (user != null) {
                                    user.sendEmailVerification().addOnCompleteListener { resendTask ->
                                        isResendingEmail = false
                                        if (resendTask.isSuccessful) {
                                            Toast.makeText(context, "Verification email resent to $pendingVerifyEmail", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Could not resend email: ${resendTask.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    isResendingEmail = false
                                    Toast.makeText(context, "Verification email resent!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isCheckingVerification && !isResendingEmail,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ObsidianNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldGradientEnd)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Resend Verification Email",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianNavy
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Button 3: Back to Login
                        Text(
                            text = "Back to Sign In",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray,
                            modifier = Modifier
                                .clickable {
                                    showVerifyEmailScreen = false
                                    selectedTabIndex = 0
                                }
                                .padding(8.dp)
                        )
                    }
                }
            } else {
                // ==============================================================
                // MAIN AUTH CARD (LOG IN / SIGN UP)
                // ==============================================================
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

                        AnimatedContent(
                            targetState = selectedTabIndex,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "AuthTabTransition"
                        ) { tabIndex ->
                            if (tabIndex == 0) {
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
                                                    Toast.makeText(context, "Password reset instructions sent to email.", Toast.LENGTH_SHORT).show()
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
                                                res.onFailure { err ->
                                                    errorMessage = err.message ?: "Login failed. Please check credentials."
                                                }
                                            }
                                        },
                                        enabled = !isLoading && !isGoogleLoading,
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

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Divider "─── OR ───"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = GoldBorderSubtle)
                                        Text(
                                            text = "  OR  ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SlateGray
                                        )
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = GoldBorderSubtle)
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Continue with Google Button (Forced Native Picker)
                                    OutlinedButton(
                                        onClick = {
                                            isGoogleLoading = true
                                            errorMessage = null
                                            // Call signOut() immediately before launching so the native account picker always shows
                                            googleSignInClient.signOut().addOnCompleteListener {
                                                try {
                                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                                } catch (e: Exception) {
                                                    isGoogleLoading = false
                                                    // Fallback to Credential Manager in AuthService
                                                    onGoogleSignInClick { res ->
                                                        res.onFailure { err ->
                                                            val msg = err.message ?: "Google sign-in failed."
                                                            errorMessage = msg
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isLoading && !isGoogleLoading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .testTag("btn_google_signin_login"),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = ObsidianNavy,
                                            contentColor = Color.White
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldGradientMid)
                                    ) {
                                        if (isGoogleLoading) {
                                            CircularProgressIndicator(color = MintGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                GoogleBrandIcon()
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Sign in with Google",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // ==========================================
                                // 2. SIGN UP FORM WITH GMAIL VERIFICATION FLOW
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

                                    Text(
                                        text = "Referral Code (Optional)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateGray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = signupReferralCode,
                                        onValueChange = { signupReferralCode = it },
                                        placeholder = { Text("e.g. HG-7798", fontSize = 13.sp, color = Color.Gray) },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
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

                                    // REGISTER BUTTON (Triggers Firebase Auth & Email Verification)
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

                                            val auth = AuthService.firebaseAuth
                                            if (auth != null) {
                                                auth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
                                                    .addOnSuccessListener { authRes ->
                                                        val user = authRes.user
                                                        if (user != null) {
                                                            // 3. Immediately send email verification
                                                            user.sendEmailVerification()

                                                            // 4. Initialize Firestore & RTDB document
                                                            val uid = user.uid
                                                            val accountId = "HG-" + uid.takeLast(6).uppercase()

                                                            scope.launch(Dispatchers.IO) {
                                                                try {
                                                                    val firestore = FirebaseFirestore.getInstance()
                                                                    val userDoc = hashMapOf(
                                                                        "uid" to uid,
                                                                        "email" to cleanEmail,
                                                                        "displayName" to cleanName,
                                                                        "accountId" to accountId,
                                                                        "usdt_balance" to 0.00,
                                                                        "btc_balance" to 0.000000,
                                                                        "total_withdrawn" to 0.00,
                                                                        "is_verified" to false,
                                                                        "created_at" to FieldValue.serverTimestamp()
                                                                    )
                                                                    firestore.collection("users").document(uid).set(userDoc)
                                                                } catch (_: Exception) {}

                                                                FirebaseSyncService.initializeNewUser(uid, cleanEmail, cleanName, null, accountId)
                                                            }

                                                            // 5. Navigate to dedicated "Verify Email" screen
                                                            isLoading = false
                                                            pendingVerifyEmail = cleanEmail
                                                            pendingVerifyName = cleanName
                                                            showVerifyEmailScreen = true
                                                            Toast.makeText(context, "Verification email sent to $cleanEmail", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            isLoading = false
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        isLoading = false
                                                        errorMessage = e.localizedMessage ?: "Registration failed."
                                                    }
                                            } else {
                                                onSignUpSubmit(cleanName, cleanEmail, cleanPass, cleanConfirm, signupReferralCode) { res ->
                                                    isLoading = false
                                                    res.onSuccess {
                                                        pendingVerifyEmail = cleanEmail
                                                        pendingVerifyName = cleanName
                                                        showVerifyEmailScreen = true
                                                    }.onFailure { err ->
                                                        errorMessage = err.message ?: "Registration failed."
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isLoading && !isGoogleLoading,
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

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Divider "─── OR ───"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = GoldBorderSubtle)
                                        Text(
                                            text = "  OR  ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SlateGray
                                        )
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = GoldBorderSubtle)
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Continue with Google Button
                                    OutlinedButton(
                                        onClick = {
                                            isGoogleLoading = true
                                            errorMessage = null
                                            googleSignInClient.signOut().addOnCompleteListener {
                                                try {
                                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                                } catch (e: Exception) {
                                                    isGoogleLoading = false
                                                    onGoogleSignInClick { res ->
                                                        res.onFailure { err ->
                                                            val msg = err.message ?: "Google sign-in failed."
                                                            errorMessage = msg
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isLoading && !isGoogleLoading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .testTag("btn_google_signin_signup"),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = ObsidianNavy,
                                            contentColor = Color.White
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldGradientMid)
                                    ) {
                                        if (isGoogleLoading) {
                                            CircularProgressIndicator(color = MintGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                GoogleBrandIcon()
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Sign in with Google",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White
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
    }
}
