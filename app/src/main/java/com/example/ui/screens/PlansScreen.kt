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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    val subTabs = listOf("Hardware Catalog", "Active Rigs")
    var newlyDeployedPlan by remember { mutableStateOf<MiningPlan?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Sub-Tabs Header
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
            // ========================================================
            // MARKETPLACE TAB (Hardware Rig Store Catalog & Deployed Pool)
            // ========================================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Highlights Pill Banner
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
                            Text("🔒 30-Day Lockup", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ObsidianNavy)
                            Text("•", fontSize = 11.sp, color = SlateGray)
                            Text("⚡ Real-Time Telemetry", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ObsidianNavy)
                            Text("•", fontSize = 11.sp, color = SlateGray)
                            Text("🌱 0% Hosting Fee", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MintDark)
                        }
                    }
                }

                // Section Header: Hardware Rig Store
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "HARDWARE RIG STORE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = ObsidianNavy
                            )
                            Text(
                                text = "Deploy High-Efficiency Mining Hardware Nodes",
                                fontSize = 10.5.sp,
                                color = SlateGray
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF101522))
                                .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "USDT: $${String.format(Locale.US, "%.2f", walletBalanceUsdt)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                }

                // STORE CATALOG CARDS (WEB3 DARK AESTHETIC)
                items(marketplacePlans) { plan ->
                    HardwareStoreCard(
                        plan = plan,
                        walletBalanceUsdt = walletBalanceUsdt,
                        onBuyClick = {
                            val success = onActivatePlan(plan)
                            if (success) {
                                newlyDeployedPlan = plan
                            } else {
                                Toast.makeText(context, "Insufficient USDT balance to buy ${plan.name}. Deposit funds to proceed.", Toast.LENGTH_SHORT).show()
                                onTriggerDeposit()
                            }
                        },
                        onDepositClick = onTriggerDeposit
                    )
                }

                // SECTION 3: "MY DEPLOYED HARDWARE" (OWNED RIGS POOL DISPLAY)
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "⚡ MY DEPLOYED HARDWARE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = ObsidianNavy
                                )
                                Text(
                                    text = "User-Owned Active & Expired Mining Rigs Pool",
                                    fontSize = 10.5.sp,
                                    color = SlateGray
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MintGreen.copy(alpha = 0.15f))
                                    .border(0.6.dp, MintDark, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${activeContracts.size} DEPLOYED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MintDark
                                )
                            }
                        }
                    }
                }

                if (activeContracts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(18.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardWhite)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⚡", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No Hardware Deployed Yet",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianNavy
                                )
                                Text(
                                    text = "Select a hardware node from the catalog above to deploy and start yield telemetry.",
                                    fontSize = 11.sp,
                                    color = SlateGray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(activeContracts) { contract ->
                        DeployedRigCard(
                            contract = contract,
                            onToggleRestake = { onToggleRestake(contract.id) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        } else {
            // ========================================================
            // ACTIVE CONTRACTS TAB (Detailed Telemetry View)
            // ========================================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(activeContracts) { contract ->
                    DeployedRigCard(
                        contract = contract,
                        onToggleRestake = { onToggleRestake(contract.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    // Success Modal Dialog on Purchase
    newlyDeployedPlan?.let { plan ->
        Dialog(onDismissRequest = { newlyDeployedPlan = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, Color(0xFF00E676), RoundedCornerShape(24.dp))
                    .testTag("purchase_success_modal"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(alpha = 0.2f))
                            .border(1.5.dp, Color(0xFF00E676), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = plan.iconCrypto, fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Rig Deployed & Mining Activated!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Your ${plan.name} node (${plan.hashPowerGh} GH/s Power) is officially online and mining live yield into your wallet balance.",
                        fontSize = 12.sp,
                        color = SlateGray,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Spec Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161E2E))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Capacity", fontSize = 9.5.sp, color = SlateGray)
                            Text("${plan.hashPowerGh} GH/s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Est. Monthly", fontSize = 9.5.sp, color = SlateGray)
                            Text("${plan.monthlyYieldPercent.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676), fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tier", fontSize = 9.5.sp, color = SlateGray)
                            Text(plan.badge, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldGradientEnd)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            newlyDeployedPlan = null
                            onSubTabChanged(0) // Stay on or move to deployed pool
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(GoldBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "VIEW MY DEPLOYED HARDWARE",
                                fontSize = 11.5.sp,
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

/**
 * SLEEK WEB3 DARK AESTHETIC HARDWARE CATALOG STORE CARD
 */
@Composable
private fun HardwareStoreCard(
    plan: MiningPlan,
    walletBalanceUsdt: Double,
    onBuyClick: () -> Unit,
    onDepositClick: () -> Unit
) {
    val canAfford = walletBalanceUsdt >= plan.minDepositUsdt

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.2.dp,
                brush = if (canAfford) Brush.linearGradient(
                    listOf(GoldBorderSubtle, Color(0xFF00E5FF).copy(alpha = 0.5f), GoldBorder)
                ) else Brush.linearGradient(
                    listOf(Color(0xFF2C394F), Color(0xFF1E2838))
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("plan_card_${plan.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // CARD HEADER: Tier Badge + 3D Hardware Icon + Model Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF1E2838), Color(0xFF0B0E14))
                                )
                            )
                            .border(1.dp, GoldBorderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = plan.iconCrypto, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = plan.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = plan.subtitle,
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    }
                }

                // Tier Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoldLight.copy(alpha = 0.15f))
                        .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = plan.badge,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldGradientEnd,
                        letterSpacing = 0.4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // HARDWARE METRICS CONTAINER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF161E2E))
                    .border(0.8.dp, Color(0xFF263248), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Capacity Column (Cyan)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ Capacity:", fontSize = 10.sp, color = SlateGray)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${plan.hashPowerGh} GH/s Power",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Est Monthly Column (Bright Green)
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📈 Est. Monthly:", fontSize = 10.sp, color = SlateGray)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${plan.monthlyYieldPercent.toInt()}% (${plan.estMonthlyAmountStr})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E676),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PRICE & DIRECT ACTION BUTTON
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("HARDWARE PRICE", fontSize = 9.5.sp, color = SlateGray, letterSpacing = 0.5.sp)
                    Text(
                        text = "$${plan.minDepositUsdt.toInt()} USDT",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = if (canAfford) onBuyClick else onDepositClick,
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .testTag("activate_plan_${plan.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford) Color.Transparent else Color(0xFF1E2838)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    if (canAfford) {
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFFD700), Color(0xFFFF9800), Color(0xFF00E676))
                                    )
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "⚡ BUY RIG — $${plan.minDepositUsdt.toInt()} USDT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = ObsidianNavy
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(21.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Deposit USDT to Buy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldGradientEnd
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * "MY DEPLOYED HARDWARE" (OWNED RIGS POOL ITEM)
 */
@Composable
private fun DeployedRigCard(
    contract: ActiveContract,
    onToggleRestake: () -> Unit
) {
    val now = System.currentTimeMillis()
    val maxCap = contract.maxPayoutCap
    val earned = contract.earned_amount
    val capPct = if (maxCap > 0) ((earned / maxCap) * 100.0).coerceIn(0.0, 100.0) else 100.0
    val isExpired = contract.isExpired

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
            .testTag("active_contract_${contract.id}"),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldLight)
                                .border(0.6.dp, GoldBorderSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = contract.badge,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldGradientEnd
                            )
                        }
                    }
                    Text(
                        text = "Rig ID: ${contract.rig_id.ifBlank { contract.id }} • Capacity: ${contract.hashPowerGh} GH/s",
                        fontSize = 10.sp,
                        color = SlateGray,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Live Earned Amount & Status Badge
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isExpired) Color(0xFFFEE2E2) else MintGreen.copy(alpha = 0.2f))
                            .border(0.6.dp, if (isExpired) Color(0xFFEF4444) else MintDark, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isExpired) "🔴 EXPIRED" else "⚡ MINING ACTIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isExpired) Color(0xFF991B1B) else MintDark
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "+$" + String.format(Locale.US, "%.4f", earned) + " USDT",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpired) Color(0xFFB91C1C) else MintDark,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar towards max earnings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mining Earnings Progress:",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianNavy
                )
                Text(
                    text = "$${String.format(Locale.US, "%.2f", earned)} / $${String.format(Locale.US, "%.2f", maxCap)} USDT (${String.format(Locale.US, "%.1f", capPct)}%)",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpired) Color(0xFFB91C1C) else MintDark,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (capPct / 100.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isExpired) Color(0xFFEF4444) else MintDark,
                trackColor = Color(0xFFF1ECE4)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Re-Stake vs Disburse Toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF9F7F3))
                    .border(0.8.dp, GoldBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Re-Stake at Maturity (+2% Bonus)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy
                        )
                        Text(
                            text = if (contract.isRestakeEnabled) "Auto-compound node upon cycle end" else "Disburse full capital to wallet balance",
                            fontSize = 8.5.sp,
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
