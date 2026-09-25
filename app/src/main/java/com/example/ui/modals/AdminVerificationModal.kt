package com.example.ui.modals

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AdminBountyClaim
import com.example.model.BountyStatus
import com.example.model.BountyType
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
fun AdminVerificationModal(
    claims: List<AdminBountyClaim>,
    onDismiss: () -> Unit,
    onApproveClaim: (AdminBountyClaim) -> Unit,
    onRejectClaim: (AdminBountyClaim, String) -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .border(1.2.dp, GoldBorder, RoundedCornerShape(26.dp))
                .testTag("admin_verification_modal"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ObsidianNavy)
                                .border(1.dp, GoldGradientMid, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = GoldGradientMid,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ADMIN VERIFICATION DASHBOARD",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = ObsidianNavy
                            )
                            Text(
                                text = "Bounty Review Queue • ${claims.count { it.status == BountyStatus.PENDING_ADMIN_REVIEW }} Pending",
                                fontSize = 10.sp,
                                color = SlateGray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SlateGray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (claims.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF9F7F3))
                            .border(1.dp, GoldBorderSubtle, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🎉", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No pending submissions in queue",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                            Text(
                                text = "All community & creator bounties are audited",
                                fontSize = 10.sp,
                                color = SlateGray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(claims, key = { it.id }) { claim ->
                            AdminClaimCard(
                                claim = claim,
                                onApprove = { onApproveClaim(claim) },
                                onReject = { reason -> onRejectClaim(claim, reason) },
                                onOpenUrl = { url ->
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                ) {
                    Text(
                        text = "CLOSE ADMIN CONSOLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminClaimCard(
    claim: AdminBountyClaim,
    onApprove: () -> Unit,
    onReject: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GoldBorderSubtle, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFCFA))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = claim.userId,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = claim.taskTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianNavy
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MintGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+$${String.format(Locale.US, "%.2f", claim.rewardUsdt)} USDT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MintDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submission Details Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3EFE8))
                    .padding(10.dp)
            ) {
                Column {
                    when (claim.taskType) {
                        BountyType.YOUTUBE -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleOutline,
                                    contentDescription = null,
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Channel: ${claim.youtubeChannelName ?: "Creator"}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (!claim.youtubeVideoUrl.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onOpenUrl(claim.youtubeVideoUrl) }
                                ) {
                                    Text(
                                        text = claim.youtubeVideoUrl,
                                        fontSize = 10.sp,
                                        color = Color(0xFF1D4ED8),
                                        textDecoration = TextDecoration.Underline,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Open Link",
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // YouTube Checklist warning
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFFBEB))
                                    .border(0.8.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                                    .padding(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Check: User ID (${claim.userId}) & referral link MUST be in video description. Min 2 min length.",
                                        fontSize = 9.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        BountyType.WHATSAPP -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MintDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Screenshot Attached • Views: ${claim.whatsappViews ?: "50+ views"}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianNavy
                                )
                            }
                            Text(
                                text = "Proof: ${claim.submissionProof}",
                                fontSize = 9.sp,
                                color = SlateGray
                            )
                        }
                        BountyType.TELEGRAM -> {
                            Text(
                                text = "Telegram Handle: ${claim.telegramHandle ?: claim.submissionProof}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                        }
                        else -> {
                            Text(
                                text = "Proof: ${claim.submissionProof}",
                                fontSize = 10.sp,
                                color = ObsidianNavy
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            if (claim.status == BountyStatus.PENDING_ADMIN_REVIEW) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onReject("Video description missing User ID or invalid duration.") },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCCCC))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "REJECT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = MintDark)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "APPROVE & CREDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (claim.status == BountyStatus.APPROVED || claim.status == BountyStatus.APPROVED_CREDITED) MintGreen.copy(alpha = 0.2f) else Color(0xFFFFEEEE))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (claim.status == BountyStatus.APPROVED || claim.status == BountyStatus.APPROVED_CREDITED) "✓ APPROVED & BONUS CREDITED" else "✗ REJECTED: ${claim.rejectionReason ?: "Invalid proof"}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (claim.status == BountyStatus.APPROVED || claim.status == BountyStatus.APPROVED_CREDITED) MintDark else CrimsonRed
                    )
                }
            }
        }
    }
}
