package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveContract
import com.example.model.ActivityItem
import com.example.model.PayoutItem
import com.example.model.PayoutStatus
import com.example.ui.modals.HardwareStabilityNoticeDialog
import com.example.ui.theme.CanvasBackground
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
import java.util.Locale

@Composable
fun WalletScreen(
    userId: String,
    walletBalanceUsdt: Double,
    btcPrice: Double,
    subTabIndex: Int,
    onSubTabChanged: (Int) -> Unit,
    activityList: List<ActivityItem>,
    payoutsList: List<PayoutItem>,
    activeContracts: List<ActiveContract> = emptyList(),
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val btcEquivalent = if (btcPrice > 0) walletBalanceUsdt / btcPrice else 0.0

    val inProgressContract = activeContracts.firstOrNull { it.work_status == "IN_PROGRESS" && it.depositUsdt > 0 }
    val primaryContract = inProgressContract ?: activeContracts.firstOrNull { it.depositUsdt > 0 }
    val rigPrice = primaryContract?.depositUsdt ?: 10.0
    val minWithdrawalTarget = primaryContract?.target_yield_30_percent ?: (rigPrice * 0.30)
    val currentEarnings = inProgressContract?.current_yield_mined ?: (walletBalanceUsdt.coerceAtMost(minWithdrawalTarget))
    val progressFraction = (currentEarnings / minWithdrawalTarget).coerceIn(0.0, 1.0).toFloat()
    val progressPercent = (progressFraction * 100).toInt()
    val isWithdrawalLocked = inProgressContract != null && inProgressContract.current_yield_mined < minWithdrawalTarget

    var showStabilityNotice by remember { mutableStateOf(false) }

    if (showStabilityNotice) {
        HardwareStabilityNoticeDialog(
            rigName = primaryContract?.planName ?: "Cloud Rig",
            rigPrice = rigPrice,
            currentEarnings = currentEarnings,
            onDismiss = { showStabilityNotice = false }
        )
    }

    val subTabs = listOf("Activity Log", "Payouts History")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // ==========================================
        // 1. WALLET HEADER CARD
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(26.dp))
                .testTag("wallet_header_card"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // User & Sync Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "INSTITUTIONAL MINING NODE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = SlateGray
                        )

                        Text(
                            text = userId,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = ObsidianNavy,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(GoldLight)
                            .border(0.8.dp, GoldBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MintDark)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Cold-Storage Synced (99.8%)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Available Balance
                Text(
                    text = "AVAILABLE BALANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateGray,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$" + String.format(Locale.US, "%.2f", walletBalanceUsdt),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = ObsidianNavy,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "USDT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldGradientEnd,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Text(
                    text = String.format(Locale.US, "%.6f BTC (~ $%.2f USDT)", btcEquivalent, walletBalanceUsdt),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateGray,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Golden Action Buttons: Deposit & Withdraw
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Deposit Button
                    Button(
                        onClick = onDepositClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("wallet_deposit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(GoldBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = ObsidianNavy,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DEPOSIT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = ObsidianNavy
                                )
                            }
                        }
                    }

                    // Withdraw Button
                    Button(
                        onClick = {
                            if (isWithdrawalLocked) {
                                showStabilityNotice = true
                            } else {
                                onWithdrawClick()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("wallet_withdraw_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isWithdrawalLocked) Color(0xFFE2E8F0) else SlateNavy,
                            contentColor = if (isWithdrawalLocked) Color(0xFF64748B) else Color.White
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isWithdrawalLocked) Icons.Default.Lock else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isWithdrawalLocked) Color(0xFF64748B) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isWithdrawalLocked) "LOCKED" else "WITHDRAW",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (isWithdrawalLocked) Color(0xFF64748B) else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Security Disclaimer with Dynamic 30% Milestone
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF9F7F3))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = GoldGradientEnd,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Min Withdrawal: $${String.format(Locale.US, "%.2f", minWithdrawalTarget)} USDT (30% Milestone for $${String.format(Locale.US, "%.2f", rigPrice)} Rig) | Audited 24-Hour Review Window",
                        fontSize = 9.sp,
                        color = SlateGray,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 2. PROGRESS BAR: ROAD TO 30% WORK MILESTONE
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(18.dp))
                .testTag("road_to_130_card"),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
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
                        text = "Road to $${String.format(Locale.US, "%.2f", minWithdrawalTarget)} Work Milestone (30%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianNavy
                    )
                    Text(
                        text = "$progressPercent% ($${String.format(Locale.US, "%.2f", currentEarnings)} / $${String.format(Locale.US, "%.2f", minWithdrawalTarget)})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldGradientEnd,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GoldGradientEnd,
                    trackColor = Color(0xFFF1ECE4)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // BILINGUAL TASK COMPLETION POLICY CARD
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(18.dp))
                .testTag("wallet_task_completion_policy_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianNavy),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = GoldGradientEnd,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grid Task Completion Policy | ग्रिड कार्य पूर्णता नियम",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldGradientEnd
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Each purchased grid runs on a dedicated cloud hardware allocation until it completes its 30% yield target. Payouts unlock only after the grid finishes its assigned task to prevent premature disruption.",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "प्रत्येक ग्रिड अपने 30% माइनिंग कार्य को पूरा करने तक निरंतर कार्य करता है। ग्रिड का काम पूरा होते ही विथड्रॉल अनलॉक हो जाता है ताकि आपको पूरा लाभ मिले और सिस्टम की स्थिरता बनी रहे।",
                    fontSize = 10.sp,
                    color = GoldLight,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 3. SUB-TABS: ACTIVITY VS PAYOUTS
        // ==========================================
        TabRow(
            selectedTabIndex = subTabIndex,
            containerColor = CardWhite,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[subTabIndex]),
                    color = GoldGradientEnd
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(14.dp))
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = subTabIndex == index,
                    onClick = { onSubTabChanged(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (subTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (subTabIndex == index) ObsidianNavy else SlateGray,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (subTabIndex == 0) {
            // Activity Log
            if (activityList.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = GoldGradientEnd,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Activity Recorded",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ObsidianNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "New account balance begins at $0.00 USDT. Start mining or activate a node to see realtime activity logs.",
                            fontSize = 11.sp,
                            color = SlateGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activityList) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardWhite)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isCredit) MintDark else CrimsonRed,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 10.sp,
                                        color = ObsidianNavy
                                    )
                                    Text(
                                        text = item.timestampStr,
                                        fontSize = 9.sp,
                                        color = SlateGray
                                    )
                                }

                                Text(
                                    text = (if (item.isCredit) "+$" else "-$") + String.format(Locale.US, "%.2f", item.usdtAmount) + " USDT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isCredit) MintDark else CrimsonRed,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        } else {
            // Payouts Log
            if (payoutsList.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = GoldGradientEnd,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Payout Requests Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ObsidianNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Withdrawals requested will appear here and undergo the automated 24-hour audit before dispatch.",
                            fontSize = 11.sp,
                            color = SlateGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(payoutsList) { payout ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardWhite)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$${payout.amountUsdt.toInt()} USDT (${payout.network})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ObsidianNavy,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = "To: ${payout.targetAddress} • ${payout.dateStr}",
                                        fontSize = 10.sp,
                                        color = SlateGray
                                    )
                                }

                                val (statusBg, statusFg) = when (payout.status) {
                                    PayoutStatus.COMPLETED -> MintGreen.copy(alpha = 0.2f) to MintDark
                                    PayoutStatus.AUDITED_DISBURSED -> GoldLight to GoldGradientEnd
                                    PayoutStatus.PENDING_24H_AUDIT -> Color(0xFFF1ECE4) to SlateNavy
                                    PayoutStatus.PROCESSING -> Color(0xFFE8F0FE) to Color(0xFF1967D2)
                                    PayoutStatus.REJECTED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(statusBg)
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = payout.status.label,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusFg
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}
