package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveContract
import com.example.model.MiningPlan
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
import java.util.Locale

@Composable
fun PlansScreen(
    subTabIndex: Int,
    onSubTabChanged: (Int) -> Unit,
    activeContracts: List<ActiveContract>,
    marketplacePlans: List<MiningPlan>,
    walletBalanceUsdt: Double,
    onToggleRestake: (String) -> Unit,
    onActivatePlan: (MiningPlan) -> Boolean,
    onTriggerDeposit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subTabs = listOf("Marketplace", "Active Contracts")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Dual Sub-Tabs
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
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(16.dp))
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
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (subTabIndex == 0) {
            // ==========================================
            // MARKETPLACE TAB (30-Day USDT-Hedged Plans)
            // ==========================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Highlights Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GoldLight)
                            .border(0.8.dp, GoldBorder, RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔒 30-Day Fixed Term", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ObsidianNavy)
                            Text("•", fontSize = 11.sp, color = SlateGray)
                            Text("⚡ Daily USDT Settlement", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ObsidianNavy)
                            Text("•", fontSize = 11.sp, color = SlateGray)
                            Text("🌱 0% Hosting Fee", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MintDark)
                        }
                    }
                }

                items(marketplacePlans) { plan ->
                    MarketplacePlanCard(
                        plan = plan,
                        walletBalanceUsdt = walletBalanceUsdt,
                        onActivateClick = {
                            val success = onActivatePlan(plan)
                            if (success) {
                                Toast.makeText(context, "${plan.name} activated successfully!", Toast.LENGTH_SHORT).show()
                                onSubTabChanged(1) // Switch to active
                            } else {
                                Toast.makeText(context, "Insufficient balance. Please deposit funds first.", Toast.LENGTH_SHORT).show()
                                onTriggerDeposit()
                            }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        } else {
            // ==========================================
            // ACTIVE CONTRACTS TAB
            // ==========================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(activeContracts) { contract ->
                    ActiveContractCard(
                        contract = contract,
                        onToggleRestake = { onToggleRestake(contract.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun MarketplacePlanCard(
    plan: MiningPlan,
    walletBalanceUsdt: Double,
    onActivateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, GoldBorder, RoundedCornerShape(22.dp))
            .testTag("plan_card_${plan.id}"),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = plan.iconCrypto, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = plan.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy
                        )
                        Text(
                            text = plan.subtitle,
                            fontSize = 10.sp,
                            color = SlateGray
                        )
                    }
                }

                if (plan.tag != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MintGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = plan.tag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Spec Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF9F7F3))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Deposit Min.", fontSize = 10.sp, color = SlateGray)
                    Text("$${plan.minDepositUsdt.toInt()} USDT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ObsidianNavy, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text("Hash Power", fontSize = 10.sp, color = SlateGray)
                    Text("${plan.hashPowerGh} Gh/s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ObsidianNavy, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text("Monthly Est.", fontSize = 10.sp, color = SlateGray)
                    Text("~${plan.monthlyYieldPercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MintDark, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Hardware: ${plan.hardwareType} • Fixed 30-Day Lockup",
                fontSize = 10.sp,
                color = SlateGray
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            Button(
                onClick = onActivateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("activate_plan_${plan.id}"),
                colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = GoldGradientMid,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ACTIVATE 30-DAY PLAN ($${plan.minDepositUsdt.toInt()} USDT)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveContractCard(
    contract: ActiveContract,
    onToggleRestake: () -> Unit
) {
    val now = System.currentTimeMillis()
    val totalDurationMs = (contract.totalDays * 24L * 3600 * 1000).coerceAtLeast(1L)
    val elapsedMs = Math.max(0L, now - contract.startTimestampMs)
    val remainingMs = Math.max(0L, contract.endTimestampMs - now)
    val progress = (elapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    val remainingDays = (remainingMs / (24L * 3600 * 1000)).toInt()
    val elapsedDays = (contract.totalDays - remainingDays).coerceIn(0, contract.totalDays)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, GoldBorder, RoundedCornerShape(22.dp))
            .testTag("active_contract_${contract.id}"),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
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
                            text = contract.planName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val isTaskCompleted = contract.work_status == "COMPLETED"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isTaskCompleted) MintGreen.copy(alpha = 0.2f) else Color(0xFFFEF3C7))
                                .border(0.6.dp, if (isTaskCompleted) MintDark else Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isTaskCompleted) "🟢 TASK READY" else "🟡 IN PROGRESS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isTaskCompleted) MintDark else Color(0xFFB45309)
                            )
                        }
                    }
                    Text(
                        text = "${contract.hashPowerGh} Gh/s • ${contract.cryptoSymbol} Node",
                        fontSize = 10.sp,
                        color = SlateGray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Accrued Profit", fontSize = 10.sp, color = SlateGray)
                    Text(
                        text = "+$" + String.format(Locale.US, "%.2f", contract.accruedProfitUsdt) + " USDT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MintDark,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 30% Task Work Target Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "30% Task Work Target",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianNavy
                )
                Text(
                    text = "$${String.format(Locale.US, "%.2f", contract.current_yield_mined)} / $${String.format(Locale.US, "%.2f", contract.target_yield_30_percent)} USDT (${contract.task_progress_pct.toInt()}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (contract.work_status == "COMPLETED") MintDark else GoldGradientEnd,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (contract.task_progress_pct / 100.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (contract.work_status == "COMPLETED") MintDark else GoldGradientEnd,
                trackColor = Color(0xFFF1ECE4)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Duration Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Contract Term: Day ${contract.elapsedDays} / ${contract.totalDays}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianNavy
                )
                Text(
                    text = "${remainingDays}d remaining",
                    fontSize = 11.sp,
                    color = SlateGray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = GoldGradientEnd,
                trackColor = GoldLight
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Start: ${contract.startDateStr}", fontSize = 10.sp, color = SlateGray)
                Text("Maturity: ${contract.maturityDateStr}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = ObsidianNavy)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Re-Stake vs Transfer to Wallet Toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF9F7F3))
                    .border(0.8.dp, GoldBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Re-Stake at Maturity",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MintGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("+2% BONUS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MintDark)
                            }
                        }
                        Text(
                            text = if (contract.isRestakeEnabled) "Auto-compound into fresh 30-day node" else "Disburse full capital to wallet balance",
                            fontSize = 9.sp,
                            color = SlateGray
                        )
                    }

                    Switch(
                        checked = contract.isRestakeEnabled,
                        onCheckedChange = { onToggleRestake() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GoldGradientEnd,
                            uncheckedTrackColor = Color(0xFFD4CDC3)
                        )
                    )
                }
            }
        }
    }
}
