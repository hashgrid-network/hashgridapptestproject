package com.example.ui.modals

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ActiveContract
import com.example.ui.theme.CardWhite
import com.example.ui.theme.CrimsonRed
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
fun WithdrawModal(
    availableBalanceUsdt: Double,
    lockedAuditBalanceUsdt: Double = 0.0,
    activeContracts: List<ActiveContract> = emptyList(),
    withdrawableUnlockedBalanceUsdt: Double = 0.0,
    onDismiss: () -> Unit,
    onSubmitWithdrawal: (Double, String, String) -> String?
) {
    val context = LocalContext.current
    var selectedNetworkIndex by remember { mutableIntStateOf(0) }
    val networks = listOf("BEP20", "TRC20")

    var addressInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showStabilityNoticeDialog by remember { mutableStateOf(false) }

    val inProgressContract = activeContracts.firstOrNull { it.work_status == "IN_PROGRESS" && it.depositUsdt > 0 }
    val primaryContract = inProgressContract ?: activeContracts.firstOrNull { it.depositUsdt > 0 }
    val activeRigPrice = primaryContract?.depositUsdt ?: 10.0
    val milestoneTarget = primaryContract?.target_yield_30_percent ?: (activeRigPrice * 0.30)
    val minWithdrawalTarget = 10.00
    val currentEarnings = inProgressContract?.current_yield_mined ?: (availableBalanceUsdt.coerceAtMost(milestoneTarget))
    val progressPercentage = ((currentEarnings / milestoneTarget) * 100.0).coerceIn(0.0, 100.0).toInt()

    val isWithdrawalLocked = inProgressContract != null && inProgressContract.current_yield_mined < milestoneTarget

    if (showStabilityNoticeDialog) {
        HardwareStabilityNoticeDialog(
            rigName = inProgressContract?.planName ?: "Cloud Rig",
            rigPrice = activeRigPrice,
            currentEarnings = currentEarnings,
            onDismiss = { showStabilityNoticeDialog = false }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.2.dp, GoldBorder, RoundedCornerShape(28.dp))
                .testTag("withdraw_dialog"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AUDITED WITHDRAWAL",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = ObsidianNavy
                        )
                        Text(
                            text = "Institutional Multi-Sig Settlement",
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

                // Available Balance & Locked in Audit Pills
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GoldLight)
                        .border(1.dp, GoldBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available Balance:",
                            fontSize = 12.sp,
                            color = SlateGray
                        )
                        Text(
                            text = "$" + String.format(Locale.US, "%.2f", availableBalanceUsdt) + " USDT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (withdrawableUnlockedBalanceUsdt > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Unlocked Task Yield:",
                                fontSize = 11.sp,
                                color = MintDark,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$" + String.format(Locale.US, "%.2f", withdrawableUnlockedBalanceUsdt) + " USDT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintDark,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (lockedAuditBalanceUsdt > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Locked in 24h Audit:",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$" + String.format(Locale.US, "%.2f", lockedAuditBalanceUsdt) + " USDT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Grid Task In Progress Status Pill & Notice (30% Work Milestone)
                if (isWithdrawalLocked && inProgressContract != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF7ED))
                            .border(1.dp, Color(0xFFFDBA74), RoundedCornerShape(12.dp))
                            .clickable { showStabilityNoticeDialog = true }
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEA580C))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "30% Work Milestone In Progress",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9A3412)
                                    )
                                }
                                Text(
                                    text = "$progressPercentage%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEA580C),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Progress: $${String.format(Locale.US, "%.2f", currentEarnings)} / $${String.format(Locale.US, "%.2f", minWithdrawalTarget)} USDT. Tap to view Hardware Stability Notice.",
                                fontSize = 10.sp,
                                color = SlateNavy,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Network Selector Tabs
                Text(
                    text = "Select Payout Network:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateGray
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                    networks.forEachIndexed { index, net ->
                        Tab(
                            selected = selectedNetworkIndex == index,
                            onClick = { selectedNetworkIndex = index },
                            text = {
                                Text(
                                    text = "USDT ($net)",
                                    fontWeight = if (selectedNetworkIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedNetworkIndex == index) ObsidianNavy else SlateGray,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Destination Address Input
                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it; errorMessage = null },
                    label = { Text("Destination ${networks[selectedNetworkIndex]} Address", fontSize = 11.sp) },
                    placeholder = { Text("Paste wallet address...", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_address_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldGradientEnd,
                        unfocusedBorderColor = GoldBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it; errorMessage = null },
                    label = { Text("Amount (USDT - Min. $${String.format(Locale.US, "%.2f", minWithdrawalTarget)})", fontSize = 11.sp) },
                    placeholder = { Text("Min. $${String.format(Locale.US, "%.2f", minWithdrawalTarget)}", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldGradientEnd,
                        unfocusedBorderColor = GoldBorder
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    trailingIcon = {
                        Text(
                            text = "MAX",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGradientEnd,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(GoldLight)
                                .clickable {
                                    amountInput = String.format(Locale.US, "%.2f", availableBalanceUsdt)
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                )

                if (availableBalanceUsdt < 10.00) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(0.8.dp, CrimsonRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("minimum_threshold_warning_note")
                    ) {
                        Text(
                            text = "Minimum withdrawable threshold is 10 USDT. Keep mining to reach threshold.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Universal 30% Work Milestone Notice Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, GoldBorder, RoundedCornerShape(12.dp))
                        .testTag("task_completion_policy_card"),
                    colors = CardDefaults.cardColors(containerColor = ObsidianNavy),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Universal 30% Work Milestone Policy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldGradientEnd
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Minimum withdrawal unlock = 30% of active rig price ($${String.format(Locale.US, "%.2f", minWithdrawalTarget)} USDT for this rig). Payouts unlock automatically upon completing the 30% mining cycle.",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Security & Policy Disclaimer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF9F7F3))
                        .border(0.8.dp, GoldBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = GoldGradientEnd,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Minimum payout: $${String.format(Locale.US, "%.2f", minWithdrawalTarget)} USDT. Subject to 24-Hour Audited Window for cold-storage multi-sig safety.",
                            fontSize = 10.sp,
                            color = SlateGray,
                            lineHeight = 14.sp
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 11.sp,
                        color = CrimsonRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (isWithdrawalLocked) {
                            showStabilityNoticeDialog = true
                            errorMessage = "Hardware Stability Notice: Minimum payout unlocks after completing the 30% work milestone ($${String.format(Locale.US, "%.2f", minWithdrawalTarget)} for this rig). Current progress: $${String.format(Locale.US, "%.2f", currentEarnings)} / $${String.format(Locale.US, "%.2f", minWithdrawalTarget)} ($progressPercentage%)."
                            return@Button
                        }
                        val amt = amountInput.toDoubleOrNull()
                        if (amt == null) {
                            errorMessage = "Please enter a valid numeric amount."
                            return@Button
                        }
                        if (amt < minWithdrawalTarget) {
                            errorMessage = "Minimum withdrawal is $${String.format(Locale.US, "%.2f", minWithdrawalTarget)} USDT."
                            return@Button
                        }
                        val result = onSubmitWithdrawal(amt, addressInput.trim(), networks[selectedNetworkIndex])
                        if (result != null) {
                            errorMessage = result
                            if (result.contains("Hardware Stability Notice", ignoreCase = true)) {
                                showStabilityNoticeDialog = true
                            }
                        } else {
                            Toast.makeText(context, "Withdrawal queued for 24h review.", Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("submit_withdraw_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWithdrawalLocked) Color(0xFFE2E8F0) else SlateNavy,
                        disabledContainerColor = Color(0xFFD1D5DB),
                        disabledContentColor = Color(0xFF6B7280)
                    )
                ) {
                    Text(
                        text = if (isWithdrawalLocked) "WITHDRAWAL LOCKED (30% MILESTONE IN PROGRESS)" else "REQUEST AUDITED WITHDRAWAL",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = if (isWithdrawalLocked) Color(0xFF64748B) else Color.White
                    )
                }
            }
        }
    }
}
