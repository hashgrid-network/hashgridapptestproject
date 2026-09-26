package com.example.ui.modals

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
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
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldBrush
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy
import java.util.Locale

@Composable
fun StarterGridDeployModal(
    walletBalanceUsdt: Double,
    onDismiss: () -> Unit,
    onDeployWithBalance: () -> Unit,
    onTriggerDeposit: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val hasEnoughBalance = walletBalanceUsdt >= 10.0

    var selectedNetworkIndex by remember { mutableIntStateOf(0) }
    val networks = listOf("USDT (TRC-20)", "USDT (BEP-20)")
    val addresses = listOf(TRC20_ADDRESS, BEP20_ADDRESS)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, GoldBorder, RoundedCornerShape(24.dp))
                .testTag("starter_grid_deploy_sheet"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(GoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DEPLOY STARTER RIG",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = ObsidianNavy,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "10 TH/s ASIC Micro-Node Allocation",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MintDark
                            )
                        }
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

                Spacer(modifier = Modifier.height(14.dp))

                // Hardware & Yield Specs Summary Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F7F3)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Contract Cost",
                                fontSize = 12.sp,
                                color = SlateGray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$10.00 USDT",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = ObsidianNavy,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dedicated Hashrate",
                                fontSize = 12.sp,
                                color = SlateGray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "+10.00 TH/s",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldGradientEnd
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "30% Task Quota Target",
                                fontSize = 12.sp,
                                color = SlateGray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$3.00 USDT Unlocked",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintDark
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Liquidity Pool",
                                fontSize = 12.sp,
                                color = SlateGray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Segregated Hot-Wallet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ObsidianNavy
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // User Settled Balance Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (hasEnoughBalance) Color(0xFFE8F8F0) else Color(0xFFFFF7ED))
                        .border(
                            0.8.dp,
                            if (hasEnoughBalance) MintGreen.copy(alpha = 0.5f) else Color(0xFFFFB74D),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settled Balance:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateNavy
                    )
                    Text(
                        text = String.format(Locale.US, "$%.2f USDT", walletBalanceUsdt),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasEnoughBalance) MintDark else Color(0xFFD97706)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Area: If user has >= $10, prominent Instant Deploy button
                if (hasEnoughBalance) {
                    Button(
                        onClick = onDeployWithBalance,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .testTag("instant_deploy_balance_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(GoldBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = ObsidianNavy,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DEPLOY NOW ($10.00 USDT)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = ObsidianNavy
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Or deposit additional USDT to your account below:",
                        fontSize = 11.sp,
                        color = SlateGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Instant Deposit Section for $10 Starter
                Text(
                    text = "INSTANT DEPOSIT $10 USDT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateGray,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Network Selector Tabs
                TabRow(
                    selectedTabIndex = selectedNetworkIndex,
                    containerColor = Color(0xFFF1F3F5),
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
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedNetworkIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedNetworkIndex == index) ObsidianNavy else SlateGray
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Address Box
                val currentAddress = addresses[selectedNetworkIndex]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Deposit Address (Send 10.00 USDT):",
                                fontSize = 10.5.sp,
                                color = SlateGray,
                                fontWeight = FontWeight.Medium
                            )

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentAddress))
                                    Toast.makeText(context, "Deposit address copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = GoldGradientEnd,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = currentAddress,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ObsidianNavy,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Deposit Modal Opener / Confirmation button
                Button(
                    onClick = onTriggerDeposit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .testTag("open_deposit_flow_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasEnoughBalance) Color(0xFFF1F3F5) else Color.Transparent
                    ),
                    border = if (hasEnoughBalance) null else androidx.compose.foundation.BorderStroke(1.5.dp, GoldBorder)
                ) {
                    if (hasEnoughBalance) {
                        Text(
                            text = "Open Full Deposit Options",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(GoldBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = ObsidianNavy,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DEPOSIT $10 USDT VIA NOWPAYMENTS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = ObsidianNavy
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
