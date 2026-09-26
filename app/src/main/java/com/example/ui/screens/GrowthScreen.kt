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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BountyStatus
import com.example.model.BountyTask
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

import android.content.Intent
import androidx.compose.material.icons.filled.ContentCopy

@Composable
fun GrowthScreen(
    referralCode: String,
    referralCount: Long = 0L,
    bonusHashrate: Double = 0.0,
    bountyTasks: List<BountyTask>,
    freeAdCooldownHours: Int,
    onClaimFreeAdSession: () -> Pair<Boolean, String>,
    onOpenBountyModal: (BountyTask) -> Unit,
    onOpenCreatorMilestoneModal: () -> Unit,
    onOpenSyndicateTerms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var adSessionMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // ==========================================
        // 1. TOP EARNINGS & LIVE STATS HEADER
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(26.dp))
                .testTag("referral_earnings_card"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL REFERRAL REWARDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$10,000.00",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = ObsidianNavy,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "USDT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldGradientEnd,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Up to $10,000 USDT in community tier rewards & team commissions.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MintDark
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(GoldLight)
                            .border(1.5.dp, GoldBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🏛️",
                            fontSize = 26.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFAF7F2))
                            .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("Total Friends Joined", fontSize = 10.sp, color = SlateGray)
                            Text("$referralCount Joined", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ObsidianNavy)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFAF7F2))
                            .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("Extra Hashrate Earned", fontSize = 10.sp, color = SlateGray)
                            Text("+${String.format(Locale.US, "%.1f", bonusHashrate)} GH/s", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldGradientEnd)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 2. BULLETPROOF OBSIDIAN & GOLD REFERRAL CARD
        // ==========================================
        val finalReferralUrl = com.example.model.getReferralUrl(referralCode)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(1.5.dp, GoldBorder, RoundedCornerShape(22.dp))
                .testTag("exclusive_referral_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianNavy),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
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
                                .background(GoldGradientEnd.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = GoldGradientMid,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "YOUR EXCLUSIVE REFERRAL CODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldGradientMid,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldGradientEnd.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+1.5 GH/s BONUS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGradientMid
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Bold Code Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SlateNavy)
                        .border(1.dp, GoldBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = referralCode,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Friends who register with your link receive an instant +1.5 GH/s hashrate welcome gift. You earn +1.5 GH/s hashrate + 7.0% mining commissions.",
                    fontSize = 10.sp,
                    color = SlateGray,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3 Action Buttons: [COPY CODE], [COPY LINK], [SHARE INVITE]
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Button 1: Copy Code
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(referralCode))
                                Toast.makeText(context, "Referral code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("copy_referral_code_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SlateNavy,
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorderSubtle)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = GoldGradientMid,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "COPY CODE",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Button 2: Copy Link
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(finalReferralUrl))
                                Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("copy_referral_link_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SlateNavy,
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorderSubtle)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = GoldGradientMid,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "COPY LINK",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Button 3: Share Invite via Native Android Share Sheet
                    Button(
                        onClick = {
                            val shareMessage = "⚡ Join me on HashGrid Cloud Mining!\n" +
                                    "Mine native GRID tokens & earn 10% - 15% monthly USDT yields on 30-day institutional contracts.\n\n" +
                                    "🎁 Referral Code: $referralCode\n" +
                                    "📲 Download App & Start Mining: $finalReferralUrl"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share HashGrid Referral Invite")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("share_invite_social_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(GoldBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = ObsidianNavy,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SHARE INVITE LINK & CODE",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ObsidianNavy,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // 3. EXCLUSIVE 50,000+ VIEWS CREATOR MEGA REWARD SHOWCASE CARD
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(1.5.dp, GoldBorder, RoundedCornerShape(22.dp))
                .testTag("creator_milestone_showcase_card"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFFFFF), Color(0xFFFBF8F2))
                        )
                    )
                    .padding(18.dp)
            ) {
                // Top Milestone Badge
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
                                .background(ObsidianNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = GoldGradientMid,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "50,000+ VIEWS CREATOR MILESTONE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = ObsidianNavy
                            )
                            Text(
                                text = "Global Creator Showcase Program",
                                fontSize = 10.sp,
                                color = SlateGray
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldLight)
                            .border(1.dp, GoldBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EXCLUSIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGradientEnd
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Surprise Rewards Showcase (Luxury Gold Cards)
                Text(
                    text = "SURPRISE REWARDS SHOWCASE:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = ObsidianNavy
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Reward 1: Iceland / Dubai International Tour
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateNavy)
                            .border(1.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianNavy)
                                    .border(1.dp, GoldGradientMid, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Flight, contentDescription = null, tint = GoldGradientMid, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "All-Expenses-Paid International Tour",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "VIP Access to Iceland Geothermal Facility (Krafla) or Dubai Summit",
                                    fontSize = 9.sp,
                                    color = SlateGray
                                )
                            }
                        }
                    }

                    // Reward 2: iPhone 18 Pro Flagship
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateNavy)
                            .border(1.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianNavy)
                                    .border(1.dp, GoldGradientMid, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = GoldGradientMid, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Brand New iPhone 18 Pro Flagship Device",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Delivered directly to your address with priority international courier",
                                    fontSize = 9.sp,
                                    color = SlateGray
                                )
                            }
                        }
                    }

                    // Reward 3: High-Tier Direct USDT Cash Grant
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateNavy)
                            .border(1.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianNavy)
                                    .border(1.dp, MintGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = MintGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "High-Tier Direct USDT Cash Grant",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Up to $2,500.00 USDT disbursed directly to your Segregated Wallet",
                                    fontSize = 9.sp,
                                    color = MintGreen
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Milestone Rules Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF9F7F3))
                        .border(1.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "MILESTONE RULES:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val rules = listOf(
                            "1. Create an authentic video review/showcase of HashGrid (Minimum 2+ minutes duration).",
                            "2. Your HashGrid User ID and Referral Link must be placed in the YouTube description.",
                            "3. Video must achieve 50,000+ verified organic views."
                        )
                        rules.forEach { r ->
                            Text(r, fontSize = 9.sp, color = SlateGray, modifier = Modifier.padding(vertical = 1.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Discretion Terms Notice
                Text(
                    text = "⚖️ Discretion Terms: All creator milestone gifts are audited for organic engagement and disbursed at the sole discretion of HashGrid Executive Administration.",
                    fontSize = 9.sp,
                    color = Color(0xFF92400E),
                    lineHeight = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Button: Claim Creator Milestone
                Button(
                    onClick = { onOpenCreatorMilestoneModal() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("open_creator_milestone_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(GoldBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = ObsidianNavy, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CLAIM CREATOR MILESTONE (50K+ VIEWS)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = ObsidianNavy
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // 4. AD PLAN ANTI-ABUSE & REFERRAL LOCK
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(20.dp))
                .testTag("ad_plan_anti_abuse_card"),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AD PLAN ANTI-ABUSE PROTOCOL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = ObsidianNavy
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "IP / DEVICE LOCKED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Maximum 1 Free Ad Plan session per IP/device every 24 hours.\n• Strictly 0% referral commissions and $0 team volume credited for Free/Ad plans. Commissions (7%) and VIP volume ONLY trigger on verified paid contracts ($100+ USDT).",
                    fontSize = 10.sp,
                    color = SlateGray,
                    lineHeight = 14.sp
                )

                if (adSessionMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = adSessionMessage ?: "",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (freeAdCooldownHours > 0) CrimsonRed else MintDark
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val (success, msg) = onClaimFreeAdSession()
                        adSessionMessage = msg
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (freeAdCooldownHours > 0) Color(0xFFE2E8F0) else SlateNavy
                    ),
                    enabled = freeAdCooldownHours <= 0
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (freeAdCooldownHours > 0) Icons.Default.Timer else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (freeAdCooldownHours > 0) SlateGray else GoldGradientMid,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (freeAdCooldownHours > 0) "DEVICE COOLDOWN (${freeAdCooldownHours}H REMAINING)" else "CLAIM DAILY FREE AD BOOST (+5 Gh/s)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (freeAdCooldownHours > 0) SlateGray else Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 5. COMMUNITY MICRO-TASKS
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(20.dp))
                .testTag("bounty_tasks_card"),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "COMMUNITY MICRO-TASKS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = ObsidianNavy
                        )
                        Text(
                            text = "Daily Social Broadcasting ($0.20 USDT)",
                            fontSize = 10.sp,
                            color = SlateGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                bountyTasks.forEach { task ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF9F7F3))
                            .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = task.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
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
                                            text = "+$${String.format(Locale.US, "%.2f", task.rewardUsdt)}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldGradientEnd
                                        )
                                    }
                                }
                                Text(
                                    text = task.description,
                                    fontSize = 10.sp,
                                    color = SlateGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (task.status) {
                                BountyStatus.AVAILABLE -> {
                                    Button(
                                        onClick = { onOpenBountyModal(task) },
                                        modifier = Modifier
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                                    ) {
                                        Text(
                                            text = "SUBMIT PROOF",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                BountyStatus.PENDING_ADMIN_REVIEW -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(GoldLight)
                                            .border(1.dp, GoldBorder, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "⏳ PENDING REVIEW",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldGradientEnd
                                        )
                                    }
                                }
                                BountyStatus.APPROVED, BountyStatus.APPROVED_CREDITED -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MintGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "✓ APPROVED (+$${String.format(Locale.US, "%.2f", task.rewardUsdt)})",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MintDark
                                        )
                                    }
                                }
                                BountyStatus.REJECTED -> {
                                    Button(
                                        onClick = { onOpenBountyModal(task) },
                                        modifier = Modifier
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                                    ) {
                                        Text(
                                            text = "RESUBMIT PROOF",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 6. SYNDICATE PARTNER PORTAL LINK
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
                .clickable { onOpenSyndicateTerms() }
                .testTag("syndicate_teaser_banner"),
            colors = CardDefaults.cardColors(containerColor = SlateNavy)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ObsidianNavy)
                            .border(1.dp, GoldBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = GoldGradientMid,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Founding Syndicate",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GoldGradientEnd.copy(alpha = 0.3f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "10 SEATS CAP",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldGradientMid
                                )
                            }
                        }
                        Text(
                            text = "1.0% Global Gross Turnover Royalty • View Term Sheet",
                            fontSize = 10.sp,
                            color = SlateGray
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = GoldGradientMid,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
