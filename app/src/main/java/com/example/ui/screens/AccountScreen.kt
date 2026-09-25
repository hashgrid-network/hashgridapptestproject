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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ElectricBolt
import com.example.BuildConfig
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun AccountScreen(
    userId: String,
    userEmail: String,
    displayName: String = "Institutional Miner",
    kycStatus: String = "UNVERIFIED",
    twoFactorEnabled: Boolean,
    selectedLanguage: String,
    onToggle2FA: () -> Unit,
    onOpenKycModal: () -> Unit = {},
    onOpenLanguageModal: () -> Unit,
    onOpenAuditDossier: () -> Unit,
    onOpenAuditDossierWithTab: (Int) -> Unit = {},
    onOpenAiSupport: () -> Unit,
    onOpenAdminDashboard: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val initials = if (displayName.isNotBlank()) {
        displayName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    } else "HG"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // ==========================================
        // 1. PROFILE CARD
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(26.dp))
                .testTag("account_profile_card"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with Gold Border
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ObsidianNavy)
                            .border(2.dp, GoldGradientMid, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.ifBlank { "AV" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldGradientMid
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayName.ifBlank { "Alexander Vance" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = ObsidianNavy
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = if (kycStatus.equals("VERIFIED", ignoreCase = true)) MintDark else GoldGradientEnd,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = userEmail,
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GoldLight)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = userId,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianNavy,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Dynamic KYC Badge
                            val (badgeText, badgeBg, badgeTextColor) = when (kycStatus.uppercase()) {
                                "VERIFIED" -> Triple("KYC: TIER-2 VERIFIED", MintGreen.copy(alpha = 0.2f), MintDark)
                                "PENDING REVIEW" -> Triple("KYC: PENDING REVIEW", Color(0xFFFEF3C7), Color(0xFFB45309))
                                else -> Triple("KYC: UNVERIFIED", Color(0xFFF3F4F6), SlateGray)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeBg)
                                    .clickable { onOpenKycModal() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 2. MULTI-LANGUAGE SELECTOR ROW
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(18.dp))
                .clickable { onOpenLanguageModal() },
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = GoldGradientEnd,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Platform Language",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy
                        )
                        Text(
                            text = "Active: $selectedLanguage",
                            fontSize = 11.sp,
                            color = SlateGray
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = SlateGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 3. OFFICIAL INFRASTRUCTURE VAULT (AUDIT DOSSIER)
        // ==========================================
        Text(
            text = "OFFICIAL INFRASTRUCTURE VAULT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = SlateGray
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                VaultDocRow(
                    title = "45 MW Geothermal PPA Contract",
                    subtitle = "Landsvirkjun Grid Interconnect • $0.034/kWh Locked",
                    icon = Icons.Default.ElectricBolt,
                    onClick = { onOpenAuditDossierWithTab(0) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                VaultDocRow(
                    title = "Tier-III Colocation SLA Manifest",
                    subtitle = "PUE 1.05 Sub-Zero Hydro Immersion Cooling",
                    icon = Icons.Default.Security,
                    onClick = { onOpenAuditDossierWithTab(1) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                VaultDocRow(
                    title = "Fleet Deployment Ledger",
                    subtitle = "8,200+ Antminer S21 Hydro Units Verified",
                    icon = Icons.Default.Memory,
                    onClick = { onOpenAuditDossierWithTab(2) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 4. SECURITY & 24/7 AI SUPPORT
        // ==========================================
        Text(
            text = "SECURITY & SUPPORT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = SlateGray
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 2FA Switch Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = GoldGradientEnd,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "2FA Authentication",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy
                        )
                        Text(
                            text = if (twoFactorEnabled) "Hardware token & Google Auth Active" else "Disabled (Not recommended)",
                            fontSize = 10.sp,
                            color = if (twoFactorEnabled) MintDark else SlateGray
                        )
                    }
                }

                Switch(
                    checked = twoFactorEnabled,
                    onCheckedChange = {
                        onToggle2FA()
                        Toast.makeText(context, if (!twoFactorEnabled) "2FA Enabled" else "2FA Disabled", Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GoldGradientEnd
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // AI Support Bot Card (Gemini)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(18.dp))
                .clickable { onOpenAiSupport() }
                .testTag("open_ai_support_card"),
            colors = CardDefaults.cardColors(containerColor = SlateNavy)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ObsidianNavy)
                            .border(1.dp, MintGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MintGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "24/7 AI Concierge Bot",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MintGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "GEMINI AI",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintGreen
                                )
                            }
                        }
                        Text(
                            text = "Live telemetry, contracts & withdrawal assistant",
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

        // ==========================================
        // 5. ADMIN VERIFICATION DASHBOARD (/admin)
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(18.dp))
                .clickable { onOpenAdminDashboard() }
                .testTag("admin_verification_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFBF8F2))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = GoldGradientMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Admin Verification Dashboard",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
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
                                    text = "/admin",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldGradientEnd
                                )
                            }
                        }
                        Text(
                            text = "Audit YouTube ($5) & WhatsApp ($0.20) submissions",
                            fontSize = 10.sp,
                            color = SlateGray
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = GoldGradientEnd,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 6. APP VERSION & CLOUD SYNC TELEMETRY
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF1ECE4))
                .clickable { onCheckForUpdates() }
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = GoldGradientEnd,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Cloud Node & RTDB: Connected",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy
                        )
                        Text(
                            text = "HashGrid App v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            fontSize = 10.sp,
                            color = SlateGray
                        )
                    }
                }

                Text(
                    text = "Check Updates",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldGradientEnd
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 6. LOG OUT ACTION BUTTON
        // ==========================================
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFFFFCCCC), RoundedCornerShape(14.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFF0F0),
                contentColor = Color(0xFFD32F2F)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Log Out",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOG OUT OF ACCOUNT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFFD32F2F)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun VaultDocRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9F7F3))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GoldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = GoldGradientEnd, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ObsidianNavy)
                Text(text = subtitle, fontSize = 10.sp, color = SlateGray)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = GoldGradientEnd,
            modifier = Modifier.size(14.dp)
        )
    }
}
