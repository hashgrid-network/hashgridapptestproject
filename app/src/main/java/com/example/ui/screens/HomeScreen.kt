package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.model.LiveTickerItem
import com.example.ui.components.UnifiedCryptoPriceBar
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CardWhite
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldBrush
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    hashPower: Double,
    btcPrice: Double,
    kasPrice: Double,
    walletBalanceUsdt: Double = 84.20,
    miningSessionEndTimestampMs: Long = System.currentTimeMillis() + (7L * 60 * 1000 + 45 * 1000),
    liveTickers: List<LiveTickerItem> = emptyList(),
    onClaimDailySpin: () -> Unit,
    onExtendMining: () -> Unit = {},
    onOpenAuditDossier: () -> Unit,
    onNavigateToPlans: (Int) -> Unit, // 0: marketplace, 1: active
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // PWA Install Banner Dismiss State
    var isInstallBannerDismissed by remember { mutableStateOf(false) }
    var showInstallModal by remember { mutableStateOf(false) }

    // Server-Timestamp Mining Progress countdown (derived live from System.currentTimeMillis())
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    val remainingMs = maxOf(0L, miningSessionEndTimestampMs - currentTimeMs)
    val remainingHours = TimeUnit.MILLISECONDS.toHours(remainingMs)
    val remainingMins = TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
    val remainingSecs = TimeUnit.MILLISECONDS.toSeconds(remainingMs) % 60
    val countdownText = if (remainingHours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", remainingHours, remainingMins, remainingSecs)
    } else {
        String.format(Locale.US, "%02d:%02d", remainingMins, remainingSecs)
    }

    var activeStepIndex by remember { mutableIntStateOf(0) }

    val steps = listOf(
        Triple("1. Start", "Deploy institutional capital into 30-day fixed contracts starting from $100.", Icons.Default.Memory),
        Triple("2. Mine", "Iceland Geothermal energy powers dedicated liquid-cooled S21 Hydro rigs.", Icons.Default.ElectricBolt),
        Triple("3. Boost", "Continuous algorithmic hedging to lock mined coins into pure stable USDT.", Icons.Default.Shield),
        Triple("4. Withdraw", "Daily automated payouts disbursed directly into your segregated balance.", Icons.Default.FlashOn)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp) // px-4 mobile side margins
    ) {
        // ========================================================
        // 0. PWA / MOBILE "INSTALL APP" DISMISSIBLE BANNER
        // ========================================================
        if (!isInstallBannerDismissed) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GoldBorderSubtle, RoundedCornerShape(16.dp))
                    .testTag("pwa_install_banner"),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(GoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.InstallMobile,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Install HashGrid App for instant mining alerts & biometric login.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ObsidianNavy,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Golden pill "Install App" button
                        Button(
                            onClick = { showInstallModal = true },
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .testTag("install_app_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .background(GoldBrush)
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Install App",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianNavy
                                )
                            }
                        }

                        IconButton(
                            onClick = { isInstallBannerDismissed = true },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = SlateGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // ========================================================
        // 1. UNIFIED CLEAN TOP HEADER: COMPACT LIVE PRICE BAR
        // ========================================================
        UnifiedCryptoPriceBar(
            liveTickers = liveTickers
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ========================================================
        // 2. PRIMARY MINING POWER HERO CARD (INITIAL VIEWPORT)
        // ========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(22.dp))
                .testTag("mining_power_hero_card"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White, Color(0xFFFCFBF9))
                        )
                    )
                    .padding(18.dp)
            ) {
                Column {
                    // Status Badge with Pulsing Green Dot: "• Mining Active (Arctic Node #04)"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFE6FBF2))
                                .border(0.8.dp, MintGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00C853).copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Mining Active (Arctic Node #04)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintDark
                            )
                        }

                        // Real-Time Server Timestamp Countdown Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GoldLight)
                                .border(0.6.dp, GoldBorderSubtle, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = countdownText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = GoldGradientEnd
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Aggregate Hashpower Display: "520.87 Gh/s"
                    Text(
                        text = "AGGREGATE HASHPOWER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateGray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.2f", hashPower),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = ObsidianNavy,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gh/s",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGradientEnd,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Dual-notation balance display: e.g., "0.000963 BTC (~ $84.20 USDT)"
                    val btcEquiv = if (btcPrice > 0) walletBalanceUsdt / btcPrice else 0.000963
                    Text(
                        text = String.format(
                            Locale.US,
                            "Settled Balance: %.6f BTC (~ $%.2f USDT)",
                            btcEquiv,
                            walletBalanceUsdt
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateGray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Button: Golden Gradient Pill "Claim Daily Spin / Extend Mining (+2h)"
                    Button(
                        onClick = {
                            onExtendMining()
                            onClaimDailySpin()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .testTag("claim_spin_hero_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(GoldBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = ObsidianNavy,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Claim Daily Spin / Extend Mining (+2h)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.3.sp,
                                    color = ObsidianNavy
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========================================================
        // 3. 4-STEP VISUAL STEPPER (HORIZONTAL FLOW)
        // [1. Start] -> [2. Mine] -> [3. Boost] -> [4. Withdraw]
        // ========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(20.dp))
                .testTag("how_it_works_stepper_card"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        text = "HOW IT WORKS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = ObsidianNavy
                    )
                    Text(
                        text = "Institutional Flow",
                        fontSize = 10.sp,
                        color = SlateGray
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal Flow: [1. Start] -> [2. Mine] -> [3. Boost] -> [4. Withdraw]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, (label, _, _) ->
                        val isSelected = activeStepIndex == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) GoldLight else Color(0xFFF8F9FA))
                                .border(
                                    width = if (isSelected) 1.dp else 0.6.dp,
                                    color = if (isSelected) GoldBorder else GoldBorderSubtle,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { activeStepIndex = index }
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                color = if (isSelected) GoldGradientEnd else SlateGray
                            )
                        }

                        if (index < steps.size - 1) {
                            Text(
                                text = "→",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldGradientEnd.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Step Interactive Subtitle Box
                val activeStep = steps[activeStepIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoldLight.copy(alpha = 0.6f))
                        .border(0.6.dp, GoldBorderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(CardWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = activeStep.third,
                            contentDescription = null,
                            tint = GoldGradientEnd,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeStep.second,
                        fontSize = 11.sp,
                        color = ObsidianNavy,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========================================================
        // 4. DUAL ACTION GRID TILES
        // "My Mining Plans" & "Boost Mining Power"
        // ========================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action Tile 1: My Mining Plans
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, GoldBorderSubtle, RoundedCornerShape(18.dp))
                    .clickable { onNavigateToPlans(1) } // Active plans tab
                    .testTag("quick_active_plans_tile"),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(GoldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = GoldGradientEnd,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "My Mining Plans",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianNavy
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "View active contracts",
                        fontSize = 11.sp,
                        color = SlateGray
                    )
                }
            }

            // Action Tile 2: Boost Mining Power
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, GoldBorderSubtle, RoundedCornerShape(18.dp))
                    .clickable { onNavigateToPlans(0) } // Marketplace tab
                    .testTag("quick_boost_hash_tile"),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE6FBF2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = MintDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Boost Mining Power",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianNavy
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Upgrade plan",
                        fontSize = 11.sp,
                        color = SlateGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // PWA Install Instructions Modal
    if (showInstallModal) {
        Dialog(onDismissRequest = { showInstallModal = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, GoldBorderSubtle, RoundedCornerShape(24.dp))
                    .testTag("pwa_install_instructions_dialog"),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
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
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(GoldLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InstallMobile,
                                    contentDescription = null,
                                    tint = GoldGradientEnd,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Install HashGrid App",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = ObsidianNavy
                            )
                        }

                        IconButton(
                            onClick = { showInstallModal = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SlateGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Experience seamless institutional mining telemetry directly from your home screen:",
                        fontSize = 12.sp,
                        color = SlateGray,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8F9FA))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "1. Tap your browser's Share or Menu icon (⋮ or Share).",
                            fontSize = 12.sp,
                            color = ObsidianNavy,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "2. Select 'Add to Home Screen' or 'Install App'.",
                            fontSize = 12.sp,
                            color = ObsidianNavy,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "3. Launch HashGrid with biometric login & real-time alerts.",
                            fontSize = 12.sp,
                            color = MintDark,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showInstallModal = false },
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
                                text = "Got it",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                        }
                    }
                }
            }
        }
    }
}
