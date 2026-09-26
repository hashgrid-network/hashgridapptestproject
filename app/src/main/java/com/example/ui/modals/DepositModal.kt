package com.example.ui.modals

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.service.NowPaymentSession
import com.example.service.NowPaymentStatus
import com.example.service.NowPaymentsService
import com.example.ui.theme.CardWhite
import com.example.ui.theme.CrimsonRed
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

const val BEP20_ADDRESS = "0x1fAcE21fc7cA33abb4B37fba82280266C12D9c09"
const val TRC20_ADDRESS = "TJj7G3U8qVSzqcJaxAhQG34ADHihnR6WuD"

@Composable
fun DepositModal(
    userId: String,
    initialAmount: String = "100",
    onDismiss: () -> Unit,
    onDepositSuccess: (Double, String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedNetworkIndex by remember { mutableIntStateOf(0) }
    val networks = listOf("USDT (TRC-20)", "USDT (BEP-20 / BSC)")
    val networkCodes = listOf("usdttrc20", "usdtbsc")

    var depositAmountInput by remember { mutableStateOf(initialAmount) }
    val quickAmounts = listOf(10, 20, 50, 100, 300, 500, 1000)

    var isCreatingPayment by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf<NowPaymentSession?>(null) }
    var paymentStatus by remember { mutableStateOf(NowPaymentStatus.WAITING) }

    // Countdown Timer (20 minutes from creation)
    var remainingSeconds by remember { mutableLongStateOf(20L * 60) }

    // Clean up polling on modal dismiss
    DisposableEffect(Unit) {
        onDispose {
            NowPaymentsService.stopPolling()
        }
    }

    // Countdown clock effect when session is active
    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            remainingSeconds = 20L * 60
            while (remainingSeconds > 0 && paymentStatus != NowPaymentStatus.FINISHED && paymentStatus != NowPaymentStatus.CONFIRMED) {
                delay(1000L)
                remainingSeconds -= 1
            }
            if (remainingSeconds <= 0 && paymentStatus == NowPaymentStatus.WAITING) {
                paymentStatus = NowPaymentStatus.EXPIRED
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.2.dp, GoldBorderSubtle, RoundedCornerShape(28.dp))
                .testTag("nowpayments_deposit_dialog"),
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
                if (paymentStatus == NowPaymentStatus.FINISHED || paymentStatus == NowPaymentStatus.CONFIRMED) {
                    // ==========================================
                    // SUCCESS CELEBRATION SCREEN
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(MintGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MintDark,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "DEPOSIT SUCCESSFUL!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = ObsidianNavy
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val creditedAmount = activeSession?.priceAmount ?: depositAmountInput.toDoubleOrNull() ?: 100.0
                    Text(
                        text = "+$${String.format(Locale.US, "%.2f", creditedAmount)} USDT has been automatically credited to your segregated HashGrid wallet balance.",
                        fontSize = 12.sp,
                        color = SlateGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onDepositSuccess(creditedAmount, activeSession?.paymentId ?: "NP_DONE")
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
                } else if (activeSession == null) {
                    // ==========================================
                    // STEP 1: ENTER AMOUNT & SELECT NETWORK
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AUTOMATED DEPOSIT",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = ObsidianNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GoldLight)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "NOWPAYMENTS",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldGradientEnd
                                    )
                                }
                            }
                            Text(
                                text = "Instant zero-manual TxID blockchain settlement",
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

                    // Network Selector Tabs
                    Text(
                        text = "Select USDT Network:",
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
                                        text = net,
                                        fontWeight = if (selectedNetworkIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedNetworkIndex == index) ObsidianNavy else SlateGray,
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Selected Network Address Quick Preview
                    val currentSelectedAddress = if (selectedNetworkIndex == 1) BEP20_ADDRESS else TRC20_ADDRESS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1ECE4))
                            .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(10.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(currentSelectedAddress))
                                Toast.makeText(context, "Address copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Selected ${networks[selectedNetworkIndex]} Address:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray
                            )
                            Text(
                                text = currentSelectedAddress,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = ObsidianNavy,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Address",
                            tint = GoldGradientEnd,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Amount Chips
                    Text(
                        text = "Select Deposit Amount (USDT):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickAmounts.take(3).forEach { amt ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (depositAmountInput == amt.toString()) GoldLight else Color(0xFFF9F7F3))
                                    .border(
                                        1.dp,
                                        if (depositAmountInput == amt.toString()) GoldGradientEnd else GoldBorderSubtle,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { depositAmountInput = amt.toString(); errorMessage = null }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$$amt",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (depositAmountInput == amt.toString()) ObsidianNavy else SlateGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickAmounts.drop(3).forEach { amt ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (depositAmountInput == amt.toString()) GoldLight else Color(0xFFF9F7F3))
                                    .border(
                                        1.dp,
                                        if (depositAmountInput == amt.toString()) GoldGradientEnd else GoldBorderSubtle,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { depositAmountInput = amt.toString(); errorMessage = null }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$$amt",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (depositAmountInput == amt.toString()) ObsidianNavy else SlateGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = depositAmountInput,
                        onValueChange = { depositAmountInput = it; errorMessage = null },
                        label = { Text("Custom Amount (USDT - Min 10.00)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nowpayments_amount_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldGradientEnd,
                            unfocusedBorderColor = GoldBorder
                        )
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 11.sp,
                            color = CrimsonRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Proceed to Deposit Button
                    Button(
                        onClick = {
                            val amt = depositAmountInput.toDoubleOrNull()
                            if (amt == null || amt < 10.0) {
                                errorMessage = "Minimum deposit is 10.00 USDT."
                                return@Button
                            }

                            isCreatingPayment = true
                            errorMessage = null

                            NowPaymentsService.createPayment(
                                userId = userId,
                                amountUsdt = amt,
                                network = networkCodes[selectedNetworkIndex],
                                onSuccess = { session ->
                                    isCreatingPayment = false
                                    activeSession = session
                                    paymentStatus = NowPaymentStatus.WAITING

                                    // Launch automated polling
                                    NowPaymentsService.startAutomatedPolling(
                                        userId = userId,
                                        paymentId = session.paymentId,
                                        onStatusChanged = { newStatus ->
                                            paymentStatus = newStatus
                                        },
                                        onPaymentSuccess = { credited, paymentId ->
                                            onDepositSuccess(credited, paymentId)
                                            Toast.makeText(context, "Deposit Confirmed! +$${String.format(Locale.US, "%.2f", credited)} USDT credited.", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                onError = { err ->
                                    isCreatingPayment = false
                                    errorMessage = err
                                }
                            )
                        },
                        enabled = !isCreatingPayment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("generate_deposit_address_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        if (isCreatingPayment) {
                            CircularProgressIndicator(color = MintGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GENERATE DEPOSIT ADDRESS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = GoldGradientMid,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // STEP 2: ACTIVE NOWPAYMENTS PAYMENT SCREEN
                    // ==========================================
                    val session = activeSession!!
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    val timeStr = String.format(Locale.US, "%02d:%02d", minutes, seconds)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AWAITING PAYMENT",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = ObsidianNavy
                            )
                            Text(
                                text = "Order: ${session.paymentId}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SlateGray
                            )
                        }

                        IconButton(
                            onClick = {
                                NowPaymentsService.stopPolling()
                                activeSession = null
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SlateGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status pill & Countdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldLight)
                            .border(1.dp, GoldBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = GoldGradientEnd
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = paymentStatus.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = ObsidianNavy, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timeStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (remainingSeconds < 180) CrimsonRed else ObsidianNavy
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // QR Code Visualizer
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.2.dp, GoldBorder, RoundedCornerShape(16.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodePlaceholder(address = session.payAddress)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Exact Amount To Pay Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F7F3))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Amount to Send:", fontSize = 11.sp, color = SlateGray)
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", session.payAmount)} ${session.payCurrency}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ObsidianNavy,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Network:", fontSize = 11.sp, color = SlateGray)
                                Text(
                                    text = "USDT (${session.network})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldGradientEnd
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Deposit Address with Copy Button
                    Text(
                        text = "Deposit Address:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1ECE4))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(session.payAddress))
                                Toast.makeText(context, "Address copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = session.payAddress,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = ObsidianNavy,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = GoldGradientEnd,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Automated status notice
                    Text(
                        text = "⚡ Waiting for blockchain transfer... (Balance auto-credits once confirmed)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldGradientEnd,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dev/QA Simulation Helper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Testing: ",
                            fontSize = 10.sp,
                            color = SlateGray
                        )
                        Text(
                            text = "Simulate Instant Blockchain Confirmation",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGradientEnd,
                            modifier = Modifier.clickable {
                                NowPaymentsService.triggerInstantSimulationSuccess(userId) { credited, payId ->
                                    onDepositSuccess(credited, payId)
                                    Toast.makeText(context, "Simulated deposit confirmed! +$${String.format(Locale.US, "%.2f", credited)} USDT", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
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
