package com.example.ui.modals

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.collectAsState
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
import com.example.service.NowPaymentSession
import com.example.service.NowPaymentStatus
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
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun DepositModal(
    onDismiss: () -> Unit,
    onDepositSuccess: (Double, String) -> Unit
) {
    var selectedNetworkIndex by remember { mutableIntStateOf(0) }
    val networks = listOf("USDT (TRC20)", "USDT (BEP20)")
    val addresses = listOf(
        "TJj7G3U8qVSzqcJaxAhQG34ADHihnR6WuD",
        "0xb8292096322bd6c4988e017c305984e6bd01e2dd"
    )
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var remainingSeconds by remember { mutableIntStateOf(899) } // 14:59
    var isConfirmedSuccess by remember { mutableStateOf(false) }
    var confirmedAmount by remember { mutableStateOf(100.0) }
    var confirmedTxId by remember { mutableStateOf("") }

    // NOWPayments session & automated 15-second polling
    val currentSession by NowPaymentsService.currentSession.collectAsState()
    var pollCountdown by remember { mutableIntStateOf(15) }

    LaunchedEffect(selectedNetworkIndex) {
        val session = NowPaymentsService.createDepositSession(
            amountUsdt = 100.0,
            network = networks[selectedNetworkIndex],
            targetAddress = addresses[selectedNetworkIndex]
        )
        NowPaymentsService.startAutomatedPolling(
            paymentId = session.paymentId,
            onStatusChanged = {},
            onPaymentSuccess = { amount, txId ->
                confirmedAmount = amount
                confirmedTxId = txId
                isConfirmedSuccess = true
                onDepositSuccess(amount, txId)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            NowPaymentsService.stopPolling()
        }
    }

    // 15-second polling indicator countdown ticker
    LaunchedEffect(Unit) {
        while (!isConfirmedSuccess) {
            delay(1000)
            if (pollCountdown > 1) {
                pollCountdown--
            } else {
                pollCountdown = 15
            }
            if (remainingSeconds > 0) remainingSeconds--
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timerStr = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.2.dp, GoldBorderSubtle, RoundedCornerShape(28.dp))
                .testTag("deposit_dialog"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isConfirmedSuccess) {
                    // ==========================================
                    // SUCCESS CONFIRMATION STATE
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MintGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MintDark,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "DEPOSIT CONFIRMED!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = ObsidianNavy
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "+$${String.format(Locale.US, "%.2f", confirmedAmount)} USDT successfully credited to your wallet balance via NOWPayments gateway.",
                        fontSize = 12.sp,
                        color = SlateGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldLight)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Payment ID:", fontSize = 11.sp, color = SlateGray)
                                Text(
                                    text = confirmedTxId.ifBlank { "NP-8829471" },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = ObsidianNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Status:", fontSize = 11.sp, color = SlateGray)
                                Text(
                                    text = "FINISHED / CONFIRMED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        Text(
                            text = "DONE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                } else {
                    // ==========================================
                    // ACTIVE PAYMENT & POLLING CHECKOUT
                    // ==========================================
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NOWPAYMENTS GATEWAY",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = ObsidianNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MintGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIVE POLLING",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MintDark
                                    )
                                }
                            }
                            Text(
                                text = "Automated 15s Blockchain Verification",
                                fontSize = 11.sp,
                                color = SlateGray
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = SlateGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Automated 15s Polling Status Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoldLight)
                            .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = GoldGradientEnd,
                                strokeWidth = 1.8.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Auto-polling in ${pollCountdown}s...",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ObsidianNavy
                            )
                        }
                        Text(
                            text = "STATUS: ${currentSession?.status?.name ?: "WAITING"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentSession?.status == NowPaymentStatus.FINISHED) MintDark else GoldGradientEnd
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Network Selector Tabs
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
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        networks.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedNetworkIndex == index,
                                onClick = { selectedNetworkIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedNetworkIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedNetworkIndex == index) ObsidianNavy else SlateGray,
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dynamic Canvas QR Code
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF8F9FA))
                            .border(1.dp, GoldBorder, RoundedCornerShape(16.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(120.dp)) {
                            val gridSize = 9
                            val cellSize = size.width / gridSize
                            for (r in 0 until gridSize) {
                                for (c in 0 until gridSize) {
                                    val isCornerFinder = (r < 3 && c < 3) || (r < 3 && c >= gridSize - 3) || (r >= gridSize - 3 && c < 3)
                                    val isMatrixFilled = isCornerFinder || ((r * 7 + c * 13 + selectedNetworkIndex * 5) % 3 == 0)
                                    if (isMatrixFilled) {
                                        drawRect(
                                            color = if (isCornerFinder) Color(0xFF0B0E14) else Color(0xFFD4AF37),
                                            topLeft = Offset(c * cellSize, r * cellSize),
                                            size = Size(cellSize - 1.2f, cellSize - 1.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Countdown Timer Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF9F7F3))
                            .border(0.6.dp, GoldBorderSubtle, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = GoldGradientEnd,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Rate Locked: $timerStr",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGradientEnd,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Address Box with Copy Button
                    val currentAddress = addresses[selectedNetworkIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1ECE4))
                            .border(1.dp, GoldBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Deposit Address (${networks[selectedNetworkIndex]}):",
                                    fontSize = 9.sp,
                                    color = SlateGray
                                )
                                Text(
                                    text = currentAddress,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianNavy,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentAddress))
                                    Toast.makeText(context, "Address copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = GoldGradientEnd,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulation & Testing actions
                    Text(
                        text = "Instant Simulation / Testing:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100.0, 300.0, 500.0).forEach { amt ->
                            Button(
                                onClick = {
                                    confirmedAmount = amt
                                    confirmedTxId = "NP-" + System.currentTimeMillis().toString().takeLast(8)
                                    isConfirmedSuccess = true
                                    onDepositSuccess(amt, confirmedTxId)
                                    Toast.makeText(context, "+$$amt USDT Deposited successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                            ) {
                                Text(
                                    text = "+$${amt.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
