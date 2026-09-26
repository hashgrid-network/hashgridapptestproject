package com.example.ui.modals

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.OFFICIAL_BEP20_ADDRESS
import com.example.model.OFFICIAL_TRC20_ADDRESS
import com.example.service.FirebaseSyncService
import com.example.service.NowPaymentsService
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun DepositModal(
    userId: String,
    initialAmount: String = "100",
    onDismiss: () -> Unit,
    onDepositSuccess: (Double, String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Network Selector: 0 = USDT (TRC-20 / TRON), 1 = USDT (BEP-20 / BSC)
    var selectedNetworkIndex by remember { mutableIntStateOf(0) }
    val networks = listOf("USDT (TRC-20 / TRON)", "USDT (BEP-20 / BSC)")

    // Dedicated addresses mapped to user
    val currentSelectedAddress = if (selectedNetworkIndex == 0) {
        FirebaseSyncService.trc20DepositAddress.ifBlank { OFFICIAL_TRC20_ADDRESS }
    } else {
        FirebaseSyncService.bep20DepositAddress.ifBlank { OFFICIAL_BEP20_ADDRESS }
    }
    val currentNetworkName = networks[selectedNetworkIndex]

    var isConfirmed by remember { mutableStateOf(false) }
    var confirmedAmount by remember { mutableStateOf(100.0) }
    var confirmedTxId by remember { mutableStateOf("") }

    // Pulsing Radar Animation for Live Scanner
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // Real-Time Blockchain Scanner / Firestore Listener
    DisposableEffect(userId) {
        var listener: ListenerRegistration? = null
        try {
            val firestore = FirebaseFirestore.getInstance()
            listener = firestore.collection("users").document(userId)
                .collection("transactions")
                .whereEqualTo("type", "DEPOSIT")
                .whereEqualTo("status", "COMPLETED")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val latestDoc = snapshot.documents.firstOrNull()
                        val amt = latestDoc?.getDouble("usdtAmount") ?: 0.0
                        val id = latestDoc?.getString("id") ?: "dep_auto"
                        if (amt > 0 && !isConfirmed) {
                            confirmedAmount = amt
                            confirmedTxId = id
                            isConfirmed = true
                            onDepositSuccess(amt, id)
                        }
                    }
                }
        } catch (_: Exception) {}

        onDispose {
            listener?.remove()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.2.dp, GoldBorderSubtle, RoundedCornerShape(28.dp))
                .testTag("zero_input_deposit_dialog"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isConfirmed) {
                    // ==========================================
                    // REAL-TIME SUCCESS CELEBRATION
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MintGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MintDark,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "DEPOSIT CONFIRMED!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = ObsidianNavy
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Deposit of $${String.format(Locale.US, "%.2f", confirmedAmount)} USDT confirmed automatically!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MintDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Funds have been credited directly to your withdrawable USDT balance and added to your Activity Log.",
                        fontSize = 11.5.sp,
                        color = SlateGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        Text(
                            text = "RETURN TO WALLET",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // ==========================================
                    // ZERO-INPUT AUTOMATED DEPOSIT MODAL
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "USDT DEPOSIT",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = ObsidianNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE8F5E9))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AUTO-CREDIT",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MintDark
                                    )
                                }
                            }
                            Text(
                                text = "Zero-Input Automated Blockchain Settlement",
                                fontSize = 11.sp,
                                color = SlateGray
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SlateGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Network Selector (USDT-TRC20 / USDT-BEP20)
                    Text(
                        text = "Select Transfer Network:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TabRow(
                        selectedTabIndex = selectedNetworkIndex,
                        containerColor = Color(0xFFF1ECE4),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedNetworkIndex]),
                                color = GoldGradientEnd
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        networks.forEachIndexed { index, net ->
                            Tab(
                                selected = selectedNetworkIndex == index,
                                onClick = { selectedNetworkIndex = index },
                                text = {
                                    Text(
                                        text = if (index == 0) "USDT (TRC-20)" else "USDT (BEP-20)",
                                        fontWeight = if (selectedNetworkIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedNetworkIndex == index) ObsidianNavy else SlateGray,
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Auto-generated Dynamic QR Code
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(1.2.dp, GoldBorder, RoundedCornerShape(18.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodePlaceholder(address = currentSelectedAddress)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. User Dedicated Address Display Card with 1-Tap Copy
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, GoldBorderSubtle, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F4EE))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your Dedicated $currentNetworkName Address:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateGray
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GoldLight)
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(currentSelectedAddress))
                                            Toast.makeText(context, "Address copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("copy_deposit_address_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = GoldGradientEnd,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "COPY",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ObsidianNavy
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = currentSelectedAddress,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = ObsidianNavy,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Live Pulsing Blockchain Scanner Status Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, MintGreen.copy(alpha = pulseAlpha), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MintGreen.copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monitoring blockchain for incoming transfer... (Auto-credits in 1-2 minutes)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MintGreen,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Network Safety Note
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFFBEB))
                            .border(0.8.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Send only USDT on $currentNetworkName. Minimum deposit: 10 USDT.",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1-Tap Copy Full Address Primary Action
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentSelectedAddress))
                            Toast.makeText(context, "Address copied to clipboard! Send USDT to auto-credit.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("copy_address_main_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = GoldGradientMid,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "COPY $currentNetworkName ADDRESS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dev/QA Simulation Trigger
                    Text(
                        text = "Simulate On-Chain Confirmation ($100 USDT)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldGradientEnd,
                        modifier = Modifier
                            .clickable {
                                val simAmt = 100.0
                                val simTxId = "tx_onchain_" + System.currentTimeMillis().toString().takeLast(6)
                                FirebaseSyncService.updateWalletBalance(userId, simAmt)
                                confirmedAmount = simAmt
                                confirmedTxId = simTxId
                                isConfirmed = true
                                onDepositSuccess(simAmt, simTxId)
                                Toast.makeText(context, "Deposit of $100.00 USDT confirmed automatically! 🎉", Toast.LENGTH_SHORT).show()
                            }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QrCodePlaceholder(address: String) {
    Canvas(modifier = Modifier.size(130.dp)) {
        val squareSize = size.width / 13
        val hash = address.hashCode()

        // Background
        drawRect(color = Color.White)

        // Draw outer positioning targets (top-left, top-right, bottom-left)
        fun drawFinder(x: Float, y: Float) {
            drawRect(color = ObsidianNavy, topLeft = Offset(x, y), size = Size(squareSize * 3.5f, squareSize * 3.5f))
            drawRect(color = Color.White, topLeft = Offset(x + squareSize * 0.7f, y + squareSize * 0.7f), size = Size(squareSize * 2.1f, squareSize * 2.1f))
            drawRect(color = ObsidianNavy, topLeft = Offset(x + squareSize * 1.2f, y + squareSize * 1.2f), size = Size(squareSize * 1.1f, squareSize * 1.1f))
        }

        drawFinder(0f, 0f)
        drawFinder(size.width - squareSize * 3.5f, 0f)
        drawFinder(0f, size.height - squareSize * 3.5f)

        // Draw deterministic matrix modules based on address
        for (i in 0 until 13) {
            for (j in 0 until 13) {
                val isFinderArea = (i < 4 && j < 4) || (i > 8 && j < 4) || (i < 4 && j > 8)
                if (!isFinderArea) {
                    val bit = ((hash xor (i * 31 + j * 17)) and 1) == 1
                    if (bit) {
                        drawRect(
                            color = ObsidianNavy,
                            topLeft = Offset(i * squareSize, j * squareSize),
                            size = Size(squareSize * 0.9f, squareSize * 0.9f)
                        )
                    }
                }
            }
        }
    }
}
