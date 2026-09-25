package com.example.ui.modals

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CardWhite
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy

@Composable
fun AuditDossierModal(
    initialTabIndex: Int = 0,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTabIndex.coerceIn(0, 2)) }
    val tabs = listOf("PPA Contract", "Colocation SLA", "Hardware Ledger")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(28.dp))
                .testTag("audit_dossier_dialog"),
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
                                .background(GoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "INFRASTRUCTURE DOSSIER",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = ObsidianNavy
                            )
                            Text(
                                text = "Audited Arctic Telemetry",
                                fontSize = 11.sp,
                                color = SlateGray
                            )
                        }
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

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GoldLight,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GoldGradientEnd
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) ObsidianNavy else SlateGray,
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> {
                            // PPA Contract Summary
                            DossierItem(
                                title = "Power Purchase Agreement (PPA)",
                                badge = "VERIFIED CONTRACT",
                                lines = listOf(
                                    "Counterparty" to "Landsvirkjun (National Power Co. of Iceland)",
                                    "Capacity" to "45.0 MW Continuous Baseload",
                                    "Power Source" to "100% Krafla Geothermal Volcanic Steam",
                                    "Tariff Locked" to "$0.034 / kWh (Fixed until 2034)",
                                    "Carbon Footprint" to "0.00 g CO2e / kWh (True Net-Zero)",
                                    "Interconnect" to "Nordic Grid 132kV Substation Hub"
                                )
                            )
                        }
                        1 -> {
                            // Colocation SLA
                            DossierItem(
                                title = "Tier-III Colocation Specification",
                                badge = "PUE 1.05 CERTIFIED",
                                lines = listOf(
                                    "Facility" to "Arctic Hydro Data Bunker (Reykjanes)",
                                    "Cooling Technology" to "Sub-Zero Liquid Hydro Immersion Loop",
                                    "PUE Efficiency" to "1.05 Average Annual Rating",
                                    "Uptime SLA" to "99.98% Redundant Dual-Bus Grid",
                                    "Security Standard" to "Biometric Multi-Chamber Access + 24/7 Guards",
                                    "Fire Suppression" to "Novec 1230 Clean Agent Gas"
                                )
                            )
                        }
                        2 -> {
                            // Hardware Deployment Ledger
                            DossierItem(
                                title = "ASIC Fleet Deployment Ledger",
                                badge = "8,200+ UNITS ACTIVE",
                                lines = listOf(
                                    "Primary Fleet" to "Antminer S21 Hydro (335 TH/s each)",
                                    "Kaspa Fleet" to "IceRiver KS0 Ultra Custom Liquid Arrays",
                                    "Altcoin Scrypt" to "Antminer L9 16.2 GH/s Immersion Rigs",
                                    "Fleet Count" to "8,240 Hydro Enclosures Operational",
                                    "Aggregate Hash" to "128.4 Ph/s Active Aggregate",
                                    "Warranty Custody" to "Bitmain Direct SLA Gold Tier Partner"
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DossierItem(
    title: String,
    badge: String,
    lines: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF9F7F3))
            .border(1.dp, GoldBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ObsidianNavy
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MintGreen.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintDark
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        lines.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = SlateGray
                )
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianNavy,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MintDark,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Cryptographically signed via Landsvirkjun Public PGP Key",
                fontSize = 9.sp,
                color = MintDark
            )
        }
    }
}
