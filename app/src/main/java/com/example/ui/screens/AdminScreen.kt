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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdminBountySubmission
import com.example.model.AdminWithdrawalRequest
import com.example.model.BountyType
import com.example.model.CreatorMilestoneSubmission
import com.example.model.MilestoneStatus
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CardWhite
import com.example.ui.theme.CrimsonRed
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
import java.util.Locale

@Composable
fun AdminScreen(
    coldReserveUsdt: Double,
    hotReserveUsdt: Double,
    withdrawalQueue: List<AdminWithdrawalRequest>,
    bountyQueue: List<AdminBountySubmission>,
    creatorMilestones: List<CreatorMilestoneSubmission>,
    onApproveWithdrawal: (String) -> Unit,
    onRejectWithdrawal: (String) -> Unit,
    onApproveBounty: (String) -> Unit,
    onRejectBounty: (String, String) -> Unit,
    onDisburseMilestone: (id: String, giftName: String, notes: String) -> Unit,
    onRejectMilestone: (id: String, reason: String) -> Unit,
    onExitAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalReserves = coldReserveUsdt + hotReserveUsdt
    val coldRatio = if (totalReserves > 0) (coldReserveUsdt / totalReserves).toFloat() else 0.85f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onExitAdmin,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardWhite)
                        .border(1.dp, GoldBorder, CircleShape)
                        .testTag("exit_admin_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit Admin",
                        tint = ObsidianNavy,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "HASHGRID BACKOFFICE",
                            fontSize = 13.sp,
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
                                text = "PIN VERIFIED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldGradientEnd
                            )
                        }
                    }
                    Text(
                        text = "Executive Reserve & Audit Engine",
                        fontSize = 10.sp,
                        color = SlateGray
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlateNavy)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AUDITOR ROLE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldGradientMid
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 1. COLD RESERVE VS HOT WALLET MONITOR
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RESERVE SOLVENCY MONITOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = ObsidianNavy
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MintGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "50%+ RULE COMPLIANT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Institutional Cold Reserve", fontSize = 10.sp, color = SlateGray)
                        Text(
                            text = "$" + String.format(Locale.US, "%,.2f", coldReserveUsdt),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Active Hot Buffer", fontSize = 10.sp, color = SlateGray)
                        Text(
                            text = "$" + String.format(Locale.US, "%,.2f", hotReserveUsdt),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGradientEnd,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { coldRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ObsidianNavy,
                    trackColor = GoldGradientMid
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${(coldRatio * 100).toInt()}% Cold Segregation • ${(100 - (coldRatio * 100).toInt())}% Hot Buffer",
                    fontSize = 10.sp,
                    color = SlateGray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 2. ATOMIC WITHDRAWAL REVIEW QUEUE ($130 ENFORCED)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ATOMIC WITHDRAWAL AUDIT QUEUE ($130 MIN)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = SlateGray
                )
                Text(
                    text = "Locked Escrow • Atomic Balance Transfer",
                    fontSize = 10.sp,
                    color = SlateGray
                )
            }
            Text(
                text = "${withdrawalQueue.size} Pending",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GoldGradientEnd
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (withdrawalQueue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .border(1.dp, GoldBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No pending withdrawal requests. Queue clear.",
                    fontSize = 12.sp,
                    color = SlateGray
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                withdrawalQueue.forEach { req ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, GoldBorder, RoundedCornerShape(18.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardWhite)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "$${req.amountUsdt.toInt()} USDT (${req.network})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ObsidianNavy,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "User: ${req.userId} • ${req.userEmail}",
                                        fontSize = 10.sp,
                                        color = SlateGray
                                    )
                                    Text(
                                        text = "To: ${req.targetAddress}",
                                        fontSize = 10.sp,
                                        color = SlateGray,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = req.status,
                                            fontSize = 8.sp,
                                            color = Color(0xFFB45309),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Locked Escrow: $${req.lockedAuditAmount.toInt()} USDT",
                                        fontSize = 9.sp,
                                        color = GoldGradientEnd,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onApproveWithdrawal(req.id)
                                        Toast.makeText(context, "Withdrawal $${req.amountUsdt.toInt()} Approved & Disbursed from Locked Escrow!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = MintDark)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AUDIT & DISBURSE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        onRejectWithdrawal(req.id)
                                        Toast.makeText(context, "Withdrawal rejected. Escrow refunded to available balance.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("REJECT & REFUND", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CrimsonRed)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // 3. CREATOR MILESTONES REVIEW (50,000+ VIEWS) SECTION
        // =========================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CREATOR MILESTONES REVIEW (50,000+ VIEWS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = SlateGray
                )
                Text(
                    text = "Executive Surprise Rewards & Travel Grants",
                    fontSize = 10.sp,
                    color = SlateGray
                )
            }
            Text(
                text = "${creatorMilestones.size} Dossiers",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GoldGradientEnd
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (creatorMilestones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .border(1.dp, GoldBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No pending creator milestones. Queue clear.",
                    fontSize = 12.sp,
                    color = SlateGray
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                creatorMilestones.forEach { milestone ->
                    AdminCreatorMilestoneCard(
                        milestone = milestone,
                        onDisburse = { gift, notes -> onDisburseMilestone(milestone.id, gift, notes) },
                        onReject = { reason -> onRejectMilestone(milestone.id, reason) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 4. COMMUNITY TASK AUDITING
        // ==========================================
        if (bountyQueue.isNotEmpty()) {
            Text(
                text = "COMMUNITY MICRO-TASK AUDITING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = SlateGray
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                bountyQueue.forEach { sub ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, GoldBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardWhite)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sub.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianNavy
                                )
                                Text(
                                    text = "+$${String.format(Locale.US, "%.2f", sub.rewardUsdt)} USDT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldGradientEnd
                                )
                            }
                            Text(
                                text = "Submitted by ${sub.userId} • ${sub.submissionProof}",
                                fontSize = 10.sp,
                                color = SlateGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApproveBounty(sub.id) },
                                    modifier = Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(8.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = MintDark)
                                ) {
                                    Text("APPROVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Button(
                                    onClick = { onRejectBounty(sub.id, "Proof rejected") },
                                    modifier = Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(8.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                                ) {
                                    Text("REJECT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CrimsonRed)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // 5. 10-PARTNER SYNDICATE ROYALTY LEDGER
        // ==========================================
        Text(
            text = "10-PARTNER SYNDICATE ROYALTY LEDGER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = SlateGray
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Gross Platform Turnover (Month)", fontSize = 10.sp, color = SlateGray)
                        Text("$482,500.00 USDT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ObsidianNavy, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("1% Royalty Pool", fontSize = 10.sp, color = SlateGray)
                        Text("$4,825.00 USDT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldGradientEnd, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoldLight)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Per-Seat Payout (7 active / 10 cap):", fontSize = 10.sp, color = SlateGray)
                    Text("$482.50 USDT / partner", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ObsidianNavy, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 6. TELEMETRY & FLEET ANALYTICS
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = SlateNavy)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ARCTIC GRID TELEMETRY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp)
                    Text("Landsvirkjun Krafla", fontSize = 10.sp, color = MintGreen)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Aggregate Hash", fontSize = 10.sp, color = SlateGray)
                        Text("128.4 Ph/s", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text("Power Draw", fontSize = 10.sp, color = SlateGray)
                        Text("45.2 MW", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text("Active Rigs", fontSize = 10.sp, color = SlateGray)
                        Text("8,240 Units", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MintGreen)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AdminCreatorMilestoneCard(
    milestone: CreatorMilestoneSubmission,
    onDisburse: (giftName: String, notes: String) -> Unit,
    onReject: (reason: String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var checkViews by remember { mutableStateOf(true) }
    var checkUserId by remember { mutableStateOf(true) }
    var checkDuration by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.2.dp, GoldBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GoldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "50,000+ VIEWS MILESTONE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = ObsidianNavy
                        )
                        Text(
                            text = "Claimant: ${milestone.userId} • ${milestone.submittedAt}",
                            fontSize = 10.sp,
                            color = SlateGray
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (milestone.status) {
                                MilestoneStatus.DISBURSED -> MintGreen.copy(alpha = 0.2f)
                                MilestoneStatus.REJECTED -> Color(0xFFFEE2E2)
                                else -> Color(0xFFFEF3C7)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = milestone.status.label.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (milestone.status) {
                            MilestoneStatus.DISBURSED -> MintDark
                            MilestoneStatus.REJECTED -> Color(0xFFDC2626)
                            else -> Color(0xFFB45309)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Video URL (Clickable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF9F7F3))
                    .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(10.dp))
                    .clickable {
                        try {
                            uriHandler.openUri(milestone.videoUrl)
                        } catch (_: Exception) {
                            Toast.makeText(context, "URL: ${milestone.videoUrl}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "▶ 50,000+ Video Link (Tap to open & inspect):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianNavy
                    )
                    Text(
                        text = milestone.videoUrl,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Channel: ${milestone.channelUrl}",
                        fontSize = 9.sp,
                        color = SlateGray
                    )
                    Text(
                        text = "Telegram Contact: ${milestone.contactTelegram}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldGradientEnd
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Verification Checklist
            Text(
                text = "EXECUTIVE AUDITOR CHECKLIST:",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = ObsidianNavy,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checkViews,
                        onCheckedChange = { checkViews = it },
                        colors = CheckboxDefaults.colors(checkedColor = MintDark)
                    )
                    Text("Confirmed 50,000+ organic views (no artificial botting)", fontSize = 10.sp, color = ObsidianNavy)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checkUserId,
                        onCheckedChange = { checkUserId = it },
                        colors = CheckboxDefaults.colors(checkedColor = MintDark)
                    )
                    Text("User ID (${milestone.userId}) & Referral link in description verified", fontSize = 10.sp, color = ObsidianNavy)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checkDuration,
                        onCheckedChange = { checkDuration = it },
                        colors = CheckboxDefaults.colors(checkedColor = MintDark)
                    )
                    Text("Authentic video duration (2+ minutes) verified", fontSize = 10.sp, color = ObsidianNavy)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (milestone.status == MilestoneStatus.PENDING_EXECUTIVE_AUDIT) {
                Text(
                    text = "DISBURSE SURPRISE REWARD TRIGGER:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateGray
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Disburse Triggers (Iceland Tour, iPhone 18 Pro, $2,500 Cash Grant)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            onDisburse("Iceland / Dubai All-Expenses-Paid Tour", "Verified 50k+ views. Travel coordination opened via ${milestone.contactTelegram}.")
                            Toast.makeText(context, "Iceland Tour Disbursed to ${milestone.userId}!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        Icon(Icons.Default.Flight, contentDescription = null, tint = GoldGradientMid, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DISBURSE ICELAND / DUBAI TOUR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            onDisburse("Brand New iPhone 18 Pro Flagship", "Verified 50k+ views. Courier tracking assigned for ${milestone.userId}.")
                            Toast.makeText(context, "iPhone 18 Pro Disbursed to ${milestone.userId}!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = GoldGradientMid, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DISBURSE IPHONE 18 PRO FLAGSHIP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            onDisburse("$2,500.00 USDT Direct Cash Grant", "Direct cash grant credited to segregated institutional balance.")
                            Toast.makeText(context, "$2,500 USDT Grant Disbursed to ${milestone.userId}!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = MintDark)
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DISBURSE $2,500 USDT CASH GRANT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Reject Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Less than 50K views", "Missing User ID in description", "Inorganic / bot views").forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEE2E2))
                                .clickable {
                                    onReject(tag)
                                    Toast.makeText(context, "Rejected: $tag", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(tag, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                }
            } else if (milestone.awardedGift != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MintGreen.copy(alpha = 0.15f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "✓ Awarded Gift: ${milestone.awardedGift}\nNotes: ${milestone.auditNotes}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MintDark
                    )
                }
            }
        }
    }
}
