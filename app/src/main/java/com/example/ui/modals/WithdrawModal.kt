package com.example.ui.modals

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CardWhite
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBrush
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldLight
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy
import java.util.Locale

@Composable
fun WithdrawModal(
    availableBalanceUsdt: Double,
    lockedAuditBalanceUsdt: Double = 0.0,
    onDismiss: () -> Unit,
    onSubmitWithdrawal: (Double, String, String) -> String?
) {
    val context = LocalContext.current
    var selectedNetworkIndex by remember { mutableIntStateOf(0) }
    val networks = listOf("TRC20", "BEP20")

    var addressInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(28.dp))
                .testTag("withdraw_dialog"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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

                    if (lockedAuditBalanceUsdt > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
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
                    label = { Text("Amount (USDT)", fontSize = 11.sp) },
                    placeholder = { Text("Min. 130.00", fontSize = 11.sp) },
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
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                )

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
                            text = "Minimum: $130 USDT. Subject to 24-Hour Audited Window for cold-storage multi-sig safety.",
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
                        val amt = amountInput.toDoubleOrNull()
                        if (amt == null) {
                            errorMessage = "Please enter a valid numeric amount."
                            return@Button
                        }
                        val result = onSubmitWithdrawal(amt, addressInput.trim(), networks[selectedNetworkIndex])
                        if (result != null) {
                            errorMessage = result
                        } else {
                            Toast.makeText(context, "Withdrawal queued for 24h review.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("submit_withdraw_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                ) {
                    Text(
                        text = "REQUEST AUDITED WITHDRAWAL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
