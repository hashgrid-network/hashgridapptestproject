package com.example.ui.modals

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.CardWhite
import com.example.ui.theme.GoldBorder
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
    onSimulateDeposit: (Double, String) -> Unit
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

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
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
                .border(1.dp, GoldBorder, RoundedCornerShape(28.dp))
                .testTag("deposit_dialog"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
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
                                    text = "AUDITED",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintDark
                                )
                            }
                        }
                        Text(
                            text = "Institutional Cold-Storage Escrow",
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SlateGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Network Selector Tabs
                TabRow(
                    selectedTabIndex = selectedNetworkIndex,
                    containerColor = GoldLight,
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
                    networks.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedNetworkIndex == index,
                            onClick = { selectedNetworkIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedNetworkIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedNetworkIndex == index) ObsidianNavy else SlateGray,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Canvas QR Code
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8F9FA))
                        .border(1.dp, GoldBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(136.dp)) {
                        val gridSize = 9
                        val cellSize = size.width / gridSize
                        // Draw clean geometric deterministic QR matrix pattern
                        for (r in 0 until gridSize) {
                            for (c in 0 until gridSize) {
                                val isCornerFinder = (r < 3 && c < 3) || (r < 3 && c >= gridSize - 3) || (r >= gridSize - 3 && c < 3)
                                val isMatrixFilled = isCornerFinder || ((r * 7 + c * 13 + selectedNetworkIndex * 5) % 3 == 0)
                                if (isMatrixFilled) {
                                    drawRect(
                                        color = if (isCornerFinder) Color(0xFF0B0E14) else Color(0xFFD4AF37),
                                        topLeft = Offset(c * cellSize, r * cellSize),
                                        size = Size(cellSize - 1.5f, cellSize - 1.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Countdown Timer Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GoldLight)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = GoldGradientEnd,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rate Locked: $timerStr",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldGradientEnd,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Address Box with Copy Button
                val currentAddress = addresses[selectedNetworkIndex]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1ECE4))
                        .border(1.dp, GoldBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Deposit Address (${networks[selectedNetworkIndex]}):",
                                fontSize = 10.sp,
                                color = SlateGray
                            )
                            Text(
                                text = currentAddress,
                                fontSize = 11.sp,
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
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Simulation Testing Block
                Text(
                    text = "Instant Simulation / Testing:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100.0, 300.0, 500.0).forEach { amt ->
                        Button(
                            onClick = {
                                onSimulateDeposit(amt, networks[selectedNetworkIndex])
                                Toast.makeText(context, "+$$amt USDT Deposited successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                        ) {
                            Text(
                                text = "+$${amt.toInt()}",
                                fontSize = 12.sp,
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
