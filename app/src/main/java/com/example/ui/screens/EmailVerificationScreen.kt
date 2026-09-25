package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.service.AuthService
import com.example.service.FirebaseSyncService
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CardWhite
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationScreen(
    email: String,
    displayName: String = "Miner",
    onVerificationSuccess: (User) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isCheckingStatus by remember { mutableStateOf(false) }
    var isResendingEmail by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableIntStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Intercept hardware back press: sign out and return to login safely
    BackHandler {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        AuthService.logout()
        onSignOut()
    }

    // Cooldown timer for resend button (60 seconds)
    LaunchedEffect(canResend) {
        if (!canResend) {
            cooldownSeconds = 60
            while (cooldownSeconds > 0) {
                delay(1000L)
                cooldownSeconds -= 1
            }
            canResend = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // BRAND & SECURITY BADGE
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(ObsidianNavy)
                    .border(2.dp, GoldGradientMid, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Security Shield",
                    tint = GoldGradientMid,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "SECURITY ACTIVATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = GoldGradientEnd
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Verify Your Email Address",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = ObsidianNavy,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "We have sent a verification link to:",
                fontSize = 12.sp,
                color = SlateGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = email,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = ObsidianNavy,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // MAIN VERIFICATION ACTION CARD
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.2.dp, GoldBorderSubtle, RoundedCornerShape(24.dp))
                    .testTag("email_verification_card"),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MintGreen.copy(alpha = 0.15f))
                            .border(1.2.dp, MintDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            tint = MintDark,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ACTIVATION GUIDE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = SlateGray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF9F7F3))
                            .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Top) {
                                Text("1.", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldGradientEnd)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open your Gmail app or inbox using the button below.", fontSize = 12.sp, color = ObsidianNavy)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Text("2.", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldGradientEnd)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tap the confirmation link in the verification email.", fontSize = 12.sp, color = ObsidianNavy)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Text("3.", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldGradientEnd)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Return here and tap 'I Have Verified' to unlock your dashboard and activate mining.", fontSize = 12.sp, color = ObsidianNavy)
                            }
                        }
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = statusMessage ?: "",
                            fontSize = 11.sp,
                            color = GoldGradientEnd,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // BUTTON 1: "OPEN GMAIL APP" (Primary Accent Button)
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_EMAIL)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val mailIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"))
                                    mailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(mailIntent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "No email app found on device.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("btn_open_gmail_app"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldGradientEnd)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = ObsidianNavy,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OPEN GMAIL APP",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp,
                                color = ObsidianNavy
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // BUTTON 2: "I HAVE VERIFIED (UNLOCK DASHBOARD)"
                    Button(
                        onClick = {
                            isCheckingStatus = true
                            statusMessage = null
                            val auth = FirebaseAuth.getInstance()
                            val user = auth.currentUser

                            if (user != null) {
                                user.reload().addOnCompleteListener { reloadTask ->
                                    isCheckingStatus = false
                                    if (reloadTask.isSuccessful) {
                                        if (user.isEmailVerified) {
                                            Toast.makeText(context, "Email verified successfully! Welcome to HashGrid.", Toast.LENGTH_LONG).show()

                                            val uid = user.uid
                                            val accountId = "HG-" + uid.takeLast(6).uppercase()
                                            val refCode = "HG-" + uid.takeLast(4).uppercase()

                                            // Atomically update Firestore /users/{uid}.is_verified = true
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    FirebaseFirestore.getInstance()
                                                        .collection("users")
                                                        .document(uid)
                                                        .update("is_verified", true)
                                                } catch (_: Exception) {}

                                                FirebaseSyncService.initializeNewUser(uid, user.email ?: email, displayName, null, accountId)
                                            }

                                            val verifiedUser = User(
                                                id = accountId,
                                                email = user.email ?: email,
                                                role = "user",
                                                referralCode = refCode,
                                                displayName = user.displayName ?: displayName,
                                                photoUrl = null,
                                                isFlaggedDuplicate = false
                                            )
                                            onVerificationSuccess(verifiedUser)
                                        } else {
                                            val msg = "Email not verified yet. Please tap the confirmation link in your Gmail inbox."
                                            statusMessage = msg
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        val err = reloadTask.exception?.localizedMessage ?: "Could not reach auth server."
                                        statusMessage = "Check failed: $err"
                                        Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                isCheckingStatus = false
                                Toast.makeText(context, "Session expired. Please sign in again.", Toast.LENGTH_SHORT).show()
                                onSignOut()
                            }
                        },
                        enabled = !isCheckingStatus && !isResendingEmail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("btn_unlock_dashboard_verification"),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        if (isCheckingStatus) {
                            CircularProgressIndicator(color = MintGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MintGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I HAVE VERIFIED (UNLOCK DASHBOARD)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // BUTTON 3: "RESEND EMAIL" (With 60s cooldown)
                    OutlinedButton(
                        onClick = {
                            if (!canResend) return@OutlinedButton
                            isResendingEmail = true
                            canResend = false
                            val auth = FirebaseAuth.getInstance()
                            val user = auth.currentUser

                            if (user != null) {
                                user.sendEmailVerification().addOnCompleteListener { resendTask ->
                                    isResendingEmail = false
                                    if (resendTask.isSuccessful) {
                                        Toast.makeText(context, "Verification email resent! Check your inbox.", Toast.LENGTH_LONG).show()
                                        statusMessage = "New verification link sent to $email."
                                    } else {
                                        val err = resendTask.exception?.localizedMessage ?: "Failed to resend."
                                        Toast.makeText(context, "Resend failed: $err", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                isResendingEmail = false
                                Toast.makeText(context, "User session not found. Please log in.", Toast.LENGTH_SHORT).show()
                                onSignOut()
                            }
                        },
                        enabled = canResend && !isCheckingStatus && !isResendingEmail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("btn_resend_verification_email"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ObsidianNavy),
                        border = BorderStroke(1.dp, if (canResend) GoldGradientEnd else Color.LightGray)
                    ) {
                        if (isResendingEmail) {
                            CircularProgressIndicator(color = GoldGradientEnd, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = if (canResend) GoldGradientEnd else Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (canResend) "RESEND EMAIL" else "RESEND IN ${cooldownSeconds}s",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (canResend) ObsidianNavy else SlateGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // BUTTON 4: "SIGN OUT / USE DIFFERENT ACCOUNT"
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                try {
                                    FirebaseAuth.getInstance().signOut()
                                } catch (_: Exception) {}
                                AuthService.logout()
                                onSignOut()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = SlateGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SIGN OUT / USE DIFFERENT ACCOUNT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // FOOTER SECURITY LABEL
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = GoldGradientEnd,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Mandatory anti-bot and sybil resistance security check",
                    fontSize = 10.sp,
                    color = SlateGray
                )
            }
        }
    }
}
