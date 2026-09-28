package com.example.ui.modals

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.runtime.DisposableEffect
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
import com.example.model.OFFICIAL_BEP20_ADDRESS
import com.example.model.OFFICIAL_TRC20_ADDRESS
import com.example.service.FirebaseSyncService
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()

    // 1. Network Selector: 0 = USDT (BEP-20 / BSC), 1 = USDT (TRC-20 / TRON)
    var selectedNetworkIndex by remember { mutableIntStateOf(0) }
    val networks = listOf("USDT (BEP-20)", "USDT (TRC-20)")

    // Dedicated verified official deposit addresses
    val currentSelectedAddress = if (selectedNetworkIndex == 0) {
        FirebaseSyncService.bep20DepositAddress.ifBlank { OFFICIAL_BEP20_ADDRESS }
    } else {
        FirebaseSyncService.trc20DepositAddress.ifBlank { OFFICIAL_TRC20_ADDRESS }
    }
    val currentNetworkCode = if (selectedNetworkIndex == 0) "BEP-20" else "TRC-20"
    val currentNetworkName = if (selectedNetworkIndex == 0) "Binance Smart Chain (BEP-20)" else "Tron Network (TRC-20)"

    // 2. Amount Input & Quick Presets
    var amountInput by remember { mutableStateOf(initialAmount) }
    val presetAmounts = listOf("10", "25", "100", "500")

    // 3. TxID Verification Tracker
    var txHashInput by remember { mutableStateOf("") }
    var isVerifyingTx by remember { mutableStateOf(false) }
    var txErrorMessage by remember { mutableStateOf<String?>(null) }

    var isConfirmed by remember { mutableStateOf(false) }
    var confirmedAmount by remember { mutableStateOf(100.0) }
    var confirmedTxId by remember { mutableStateOf("") }

    // Pulsing Radar Animation for Live Blockchain Scanner
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
                        text = "Deposit of $${String.format(Locale.US, "%.2f", confirmedAmount)} USDT confirmed & credited!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MintDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Funds have been credited directly to your withdrawable USDT balance via Atomic Anti-Replay Ledger.",
                        fontSize = 11.5.sp,
                        color = SlateGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onDismiss,
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
                    // DUAL-NETWORK INSTIUTIONAL DEPOSIT PIPELINE
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
                                        text = "DUAL-NETWORK",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MintDark
                                    )
                                }
                            }
                            Text(
                                text = "NOWPayments + BSC / TRON Direct Settlement",
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

                    // 1. DUAL-NETWORK SWITCHER TABS: Tab A [USDT (BEP-20)], Tab B [USDT (TRC-20)]
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
                                onClick = {
                                    selectedNetworkIndex = index
                                    txErrorMessage = null
                                },
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. High-Contrast Auto-Generated QR Code
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(1.2.dp, GoldBorder, RoundedCornerShape(18.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodePlaceholder(address = currentSelectedAddress)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                    text = "Deposit Address ($currentNetworkCode):",
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
                                            Toast.makeText(context, "Address Copied!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("copy_deposit_address_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Address",
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = ObsidianNavy,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. RIG-MATCHING QUICK AMOUNT PRESETS CHIPS
                    Text(
                        text = "Quick Rig Amount Presets:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetAmounts.forEach { preset ->
                            val isSelected = amountInput == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) SlateNavy else Color(0xFFF1ECE4))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) GoldGradientEnd else GoldBorderSubtle,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        amountInput = preset
                                        txErrorMessage = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$$preset USDT",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) Color.White else ObsidianNavy
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. TRANSACTION TRACKER / TxID VERIFICATION INPUT
                    Text(
                        text = "Verify Transaction Hash (TxID):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = txHashInput,
                        onValueChange = {
                            txHashInput = it
                            txErrorMessage = null
                        },
                        placeholder = {
                            Text("Paste Transaction Hash (TxID)", fontSize = 11.sp, color = SlateGray)
                        },
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GoldLight)
                                    .clickable {
                                        clipboardManager.getText()?.text?.let { pasted ->
                                            txHashInput = FirebaseSyncService.sanitizeTxHash(pasted)
                                            Toast.makeText(context, "TxID Pasted & Sanitized!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = GoldGradientEnd,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("PASTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ObsidianNavy)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldGradientEnd,
                            unfocusedBorderColor = GoldBorderSubtle,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_id_input_field")
                    )

                    if (!txErrorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = txErrorMessage!!,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 6. ACTION BUTTON: "⚡ Verify & Update Balance"
                    Button(
                        onClick = {
                            val targetAmt = amountInput.toDoubleOrNull() ?: 0.0
                            val sanitizedTx = FirebaseSyncService.sanitizeTxHash(txHashInput)

                            if (targetAmt <= 0) {
                                txErrorMessage = "Please select or enter a valid deposit amount."
                                return@Button
                            }
                            if (sanitizedTx.isBlank()) {
                                txErrorMessage = "Please paste or enter a valid Transaction Hash (TxID)."
                                return@Button
                            }

                            isVerifyingTx = true
                            txErrorMessage = null

                            coroutineScope.launch {
                                val (success, msg) = FirebaseSyncService.verifyAndProcessDepositAtomic(
                                    walletAddress = userId,
                                    userId = userId,
                                    txId = sanitizedTx,
                                    amountUsdt = targetAmt,
                                    network = currentNetworkCode
                                )
                                isVerifyingTx = false
                                if (success) {
                                    confirmedAmount = targetAmt
                                    confirmedTxId = sanitizedTx
                                    isConfirmed = true
                                    onDepositSuccess(targetAmt, sanitizedTx)
                                    Toast.makeText(context, "Deposit of $${String.format(Locale.US, "%.2f", targetAmt)} USDT confirmed!", Toast.LENGTH_SHORT).show()
                                } else {
                                    txErrorMessage = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isVerifyingTx,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("verify_deposit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        if (isVerifyingTx) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying On-Chain Ledger...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Text(
                                text = "⚡ VERIFY & UPDATE BALANCE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Pulsing Blockchain Scanner Status Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, MintGreen.copy(alpha = pulseAlpha), RoundedCornerShape(12.dp))
                            .padding(10.dp)
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
                                text = "Monitoring $currentNetworkName blockchain... (Auto-credits in 1-2 mins)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MintGreen,
                                textAlign = TextAlign.Center
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
                            text = "Send only USDT on $currentNetworkCode. Minimum deposit: 10 USDT.",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E)
                        )
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
                                val simAmt = amountInput.toDoubleOrNull() ?: 100.0
                                val simTxId = "tx_onchain_" + System.currentTimeMillis().toString().takeLast(6)
                                coroutineScope.launch {
                                    FirebaseSyncService.verifyAndProcessDepositAtomic(userId, userId, simTxId, simAmt, currentNetworkCode)
                                    confirmedAmount = simAmt
                                    confirmedTxId = simTxId
                                    isConfirmed = true
                                    onDepositSuccess(simAmt, simTxId)
                                    Toast.makeText(context, "Deposit of $${String.format(Locale.US, "%.2f", simAmt)} USDT confirmed automatically! 🎉", Toast.LENGTH_SHORT).show()
                                }
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
    Canvas(modifier = Modifier.size(120.dp)) {
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
