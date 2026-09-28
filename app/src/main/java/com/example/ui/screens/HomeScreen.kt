package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.window.Dialog
import com.example.model.ActiveContract
import com.example.model.LiveTickerItem
import com.example.ui.components.LiveCommunityTicker
import com.example.ui.components.UnifiedCryptoPriceBar
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
    activeContracts: List<ActiveContract> = emptyList(),
    onClaimDailySpin: () -> Unit,
    onExtendMining: () -> Unit = {},
    onOpenAuditDossier: () -> Unit,
    onNavigateToPlans: (Int) -> Unit, // 0: marketplace, 1: active
    onDeployStarterPlan: () -> Unit = {},
    gridCoinBalance: Double = 24.85,
    isGridMiningActive: Boolean = true,
    effectiveGridRate: Double = 1.30,
    canSpinWheel: Boolean = true,
    wheelCooldownEndTimestamp: Long = 0L,
    onOpenMiningSheet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
        String.format(Locale.US, "%02d:%02d:%02d", remainingMins, remainingSecs)
    }

    // Lucky Wheel Cooldown Calculation
    val isWheelOnCooldown = !canSpinWheel && wheelCooldownEndTimestamp > currentTimeMs
    val wheelRemainingMs = if (isWheelOnCooldown) maxOf(0L, wheelCooldownEndTimestamp - currentTimeMs) else 0L
    val wheelHours = TimeUnit.MILLISECONDS.toHours(wheelRemainingMs)
    val wheelMins = TimeUnit.MILLISECONDS.toMinutes(wheelRemainingMs) % 60
    val wheelSecs = TimeUnit.MILLISECONDS.toSeconds(wheelRemainingMs) % 60
    val wheelCountdownText = String.format(Locale.US, "%02d:%02d:%02d", wheelHours, wheelMins, wheelSecs)

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
        // 1. UNIFIED CLEAN TOP HEADER: COMPACT LIVE PRICE BAR
        // ========================================================
        UnifiedCryptoPriceBar(
            liveTickers = liveTickers
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ========================================================
        // 1B. DUAL WALLET DISPLAY CARDS (USDT & GRID BALANCES)
        // ========================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: [USDT Balance] -> Withdrawable mining profits from purchased grids
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.2.dp, GoldBorderSubtle, RoundedCornerShape(18.dp))
                    .testTag("card_usdt_wallet_balance"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101522)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MINER BALANCE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateGray,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00F5A0).copy(alpha = 0.15f))
                                .border(0.8.dp, Color(0xFF00F5A0).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "WITHDRAWABLE",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00F5A0),
                                letterSpacing = 0.2.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = String.format(Locale.US, "$%.2f", walletBalanceUsdt),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Withdrawable mining profits",
                        fontSize = 8.5.sp,
                        color = SlateGray,
                        lineHeight = 11.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Card 2: [GRID Balance] -> Pre-launch native token reserve (100% claimable at DEX listing)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.2.dp, GoldBorder, RoundedCornerShape(18.dp))
                    .clickable { onOpenMiningSheet() }
                    .testTag("card_grid_token_balance"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101522)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GRID BALANCE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldGradientEnd,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldLight.copy(alpha = 0.15f))
                                .border(0.6.dp, GoldBorderSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "GENESIS",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldGradientEnd,
                                letterSpacing = 0.2.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = String.format(Locale.US, "%.5f", gridCoinBalance),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = GoldGradientEnd,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Pre-launch native token reserve",
                        fontSize = 8.5.sp,
                        color = SlateGray,
                        lineHeight = 11.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ========================================================
        // 1C. HIGH-VISIBILITY GLOWING 🎡 LUCKY WHEEL / DAILY SPIN CARD
        // ========================================================
        LuckyWheelDashboardCard(
            canSpin = canSpinWheel,
            cooldownText = wheelCountdownText,
            onSpinClick = onClaimDailySpin
        )

        Spacer(modifier = Modifier.height(12.dp))
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

                    // ========================================================
                    // 1. ULTRA-PREMIUM 3D QUANTUM MINING REACTOR (210dp CENTERPIECE)
                    // ========================================================
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        QuantumMiningReactor(
                            isMiningActive = isGridMiningActive,
                            hashPowerGhs = hashPower,
                            effectiveGridRate = effectiveGridRate,
                            countdownText = countdownText,
                            onActivate = {
                                onExtendMining()
                                onOpenMiningSheet()
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Tap reactor core to cycle 24h quantum node • Next tap in: $countdownText",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateGray,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Live Speed Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFE8F5E9))
                                .border(1.dp, MintGreen.copy(alpha = 0.6f * pulseAlpha), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                .testTag("live_grid_speed_badge")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MintDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "⚡ Live Telemetry: +${String.format(Locale.US, "%.5f", effectiveGridRate / 3600.0)} GRID/sec (${String.format(Locale.US, "%.2f", effectiveGridRate)} GRID/h)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Aggregate Hashpower & Settled Balance Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AGGREGATE HASHPOWER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format(Locale.US, "%.2f", hashPower),
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ObsidianNavy,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gh/s",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldGradientEnd,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        val btcEquiv = if (btcPrice > 0) walletBalanceUsdt / btcPrice else 0.000963
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "SETTLED BALANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateGray,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "$%.2f USDT", walletBalanceUsdt),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = MintDark,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format(Locale.US, "≈ %.6f BTC", btcEquiv),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = SlateGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ========================================================
                    // 2. SPEED COMPARISON CHIP
                    // ========================================================
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                            .testTag("speed_comparison_chip"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00C853))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Current Status: Free Node (0.50 GRID/hr)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ObsidianNavy
                                    )
                                }
                                Text(
                                    text = "FREE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateGray,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GoldLight)
                                        .border(0.6.dp, GoldBorderSubtle, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = GoldGradientEnd,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Boost Potential: +10 TH/s USDT with $10 Plan",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ObsidianNavy
                                    )
                                }

                                Text(
                                    text = "⚡ 20x YIELD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MintDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Action: Golden Gradient Pill "Claim Daily Spin / Extend Mining (+2h)"
                    Button(
                        onClick = {
                            onExtendMining()
                            onClaimDailySpin()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .testTag("claim_spin_hero_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
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
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Claim Daily Spin / Extend Mining (+2h)",
                                    fontSize = 11.5.sp,
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

        Spacer(modifier = Modifier.height(14.dp))

        // ========================================================
        // 2A. SLEEK GOLD-BORDERED STARTER UPGRADE TEASER CARD
        // ========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, GoldBorder, RoundedCornerShape(20.dp))
                .testTag("starter_upgrade_teaser_card"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White, Color(0xFFFFFDF8), Color(0xFFFFF9EE))
                        )
                    )
                    .padding(18.dp)
            ) {
                Column {
                    // Header: "⚡ Want Instant USDT Yields?"
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
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = GoldGradientEnd,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "⚡ Want Instant USDT Yields?",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ObsidianNavy
                                )
                                Text(
                                    text = "INSTANT ACTIVATION • DEDICATED HARDWARE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldGradientEnd,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE8F8F0))
                                .border(0.6.dp, MintGreen, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$10 ENTRY",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = MintDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sub-text
                    Text(
                        text = "Upgrade from free node to Starter Grid for just $10 USDT. Mine real-time withdrawable balance.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateNavy,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Value Highlights Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF4F6F8))
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚡ 10 TH/s Rig",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF4F6F8))
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎯 $3.00 Task Yield",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF4F6F8))
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔓 Auto Unlocked",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Button: "DEPLOY $10 STARTER RIG (10 TH/s)" -> Directly opens instant deposit/purchase sheet
                    Button(
                        onClick = onDeployStarterPlan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .testTag("deploy_starter_rig_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(GoldBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = ObsidianNavy,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DEPLOY $10 STARTER RIG (10 TH/s)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = ObsidianNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = ObsidianNavy,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ========================================================
        // 2B. DASHBOARD ACTIVE GRIDS RACK (30% WORK-COMPLETION PROGRESS)
        // ========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, GoldBorderSubtle, RoundedCornerShape(22.dp))
                .testTag("dashboard_active_grids_rack"),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ACTIVE MINING RIGS RACK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = ObsidianNavy
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldLight)
                            .border(0.6.dp, GoldBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${activeContracts.size} Units",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeContracts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF9F7F3))
                            .border(0.8.dp, GoldBorderSubtle, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Active Hardware Contracts Deployed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Deploy a dedicated cloud mining grid to begin 30-day yield task completion.",
                                fontSize = 11.sp,
                                color = SlateGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onNavigateToPlans(0) },
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .testTag("deploy_first_rig_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .background(GoldBrush)
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Deploy Cloud Mining Rig",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ObsidianNavy
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        activeContracts.forEach { contract ->
                            val isCompleted = contract.work_status == "COMPLETED"
                            val progressFloat = (contract.task_progress_pct / 100.0).coerceIn(0.0, 1.0).toFloat()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isCompleted) Color(0xFFF0FDF4) else Color(0xFFFCFBF9))
                                    .border(
                                        width = 1.dp,
                                        color = if (isCompleted) MintGreen.copy(alpha = 0.6f) else GoldBorderSubtle,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column {
                                    // Row 1: Plan Title, Tier Badge, & Status Pill
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f, fill = false),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = contract.planName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                color = ObsidianNavy
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(GoldLight)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${contract.hashrateThs.toInt()} TH/s Dedicated",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldGradientEnd
                                                )
                                            }
                                        }

                                        // Status Pill: "🟡 MINING TASK IN PROGRESS" or "🟢 TASK COMPLETED - WITHDRAW READY"
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isCompleted) MintGreen.copy(alpha = 0.2f) else Color(0xFFFEF3C7)
                                                )
                                                .border(
                                                    0.6.dp,
                                                    if (isCompleted) MintDark else Color(0xFFF59E0B),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (isCompleted) "🟢 WITHDRAW READY" else "⚡ MINING IN PROGRESS",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCompleted) MintDark else Color(0xFFB45309),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Contract Maturity: ${contract.getMaturityCountdownStr(currentTimeMs)}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SlateNavy,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Accrued Profit (Locked): $${String.format(Locale.US, "%.4f", contract.current_yield_mined)} USDT",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCompleted) MintDark else GoldGradientEnd,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Capital Unlock Milestone:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SlateNavy
                                        )
                                        Text(
                                            text = "$${String.format(Locale.US, "%.4f", contract.current_yield_mined)} / $${String.format(Locale.US, "%.2f", contract.target_yield_30_percent)} USDT (${String.format(Locale.US, "%.1f", contract.task_progress_pct)}%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCompleted) MintDark else GoldGradientEnd,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(5.dp))

                                    LinearProgressIndicator(
                                        progress = { progressFloat },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (isCompleted) MintDark else GoldGradientEnd,
                                        trackColor = Color(0xFFF1ECE4)
                                    )
                                }
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

        Spacer(modifier = Modifier.height(14.dp))

        // ========================================================
        // 7. LIVE MINING COMMUNITY ACTIVITY TICKER
        // ========================================================
        LiveCommunityTicker(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_community_activity_ticker")
        )

        Spacer(modifier = Modifier.height(20.dp))
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

@Composable
fun GeothermalPowerSwitchButton(
    isGridMiningActive: Boolean,
    effectiveGridRate: Double,
    countdownText: String,
    remainingMs: Long,
    pulseAlpha: Float,
    onExtendMining: () -> Unit,
    onOpenMiningSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var isPressed by remember { mutableStateOf(false) }
    var shockwaveProgress by remember { mutableStateOf(0f) }
    var isShockwaveActive by remember { mutableStateOf(false) }
    var showHudBanner by remember { mutableStateOf(false) }

    // Spring dampened bounce animation
    val scaleState by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "switchScale"
    )

    // Continuous 360-degree radar rotation
    val infiniteTransition = rememberInfiniteTransition(label = "power_switch_radar")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    // Handle Shockwave Animation
    LaunchedEffect(isShockwaveActive) {
        if (isShockwaveActive) {
            shockwaveProgress = 0f
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 600) {
                val elapsed = System.currentTimeMillis() - startTime
                shockwaveProgress = (elapsed / 600f).coerceIn(0f, 1f)
                delay(16L)
            }
            isShockwaveActive = false
        }
    }

    // Handle HUD Banner timer
    LaunchedEffect(showHudBanner) {
        if (showHudBanner) {
            delay(3500L)
            showHudBanner = false
        }
    }

    val activeGlowColor = if (isGridMiningActive) Color(0xFF00E676) else GoldGradientEnd
    val activeRateStr = String.format(Locale.US, "%.2f", effectiveGridRate)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // SLEEK HUD BANNER POPUP
        AnimatedVisibility(
            visible = showHudBanner,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianNavy)
                    .border(1.2.dp, Color(0xFF00E676), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚡ Node Link Established | Connected to Geothermal Core #08 | 24-Hour Cycle Active",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scaleState),
            contentAlignment = Alignment.Center
        ) {
            // CANVAS: RADIAL AMBIENT GLOW, DASHED RADAR RING, PROGRESS ARC & SHOCKWAVE
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = center
                val radius = size.minDimension / 2f

                // 1. Outer Radial Ambient Aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeGlowColor.copy(alpha = 0.35f * pulseAlpha),
                            activeGlowColor.copy(alpha = 0.12f * pulseAlpha),
                            Color.Transparent
                        ),
                        center = centerOffset,
                        radius = radius
                    ),
                    radius = radius
                )

                // 2. Continuous Dashed Rotating Radar Ring
                val ringRadius = radius * 0.82f
                val strokeWidth = 3.dp.toPx()
                rotate(degrees = rotationAngle, pivot = centerOffset) {
                    drawCircle(
                        color = activeGlowColor.copy(alpha = 0.65f),
                        radius = ringRadius,
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), 0f)
                        )
                    )
                }

                // 3. Active 24H Circular Progress Arc
                if (isGridMiningActive) {
                    val totalMs = 24L * 3600 * 1000
                    val elapsedMs = (totalMs - remainingMs).coerceIn(0L, totalMs)
                    val progressFraction = (elapsedMs.toFloat() / totalMs).coerceIn(0f, 1f)
                    val sweepAngle = progressFraction * 360f

                    drawArc(
                        color = Color(0xFF00E676),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                // 4. Expanding Energy Shockwave Ripple Effect
                if (isShockwaveActive) {
                    val shockwaveRadius = ringRadius * (0.8f + shockwaveProgress * 0.7f)
                    val shockwaveAlpha = (1f - shockwaveProgress).coerceIn(0f, 1f)
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = shockwaveAlpha),
                        radius = shockwaveRadius,
                        style = Stroke(width = (5 - shockwaveProgress * 3).dp.toPx())
                    )
                }
            }

            // CENTRAL 3D POWER SWITCH BUTTON
            Button(
                onClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                    isPressed = true
                    isShockwaveActive = true
                    showHudBanner = true

                    onExtendMining()
                    onOpenMiningSheet()

                    Toast.makeText(
                        context,
                        "⚡ Node Link Established | Connected to Geothermal Core #08 | 24-Hour Cycle Active",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .border(
                        3.dp,
                        Brush.verticalGradient(
                            listOf(
                                activeGlowColor,
                                activeGlowColor.copy(alpha = 0.4f),
                                ObsidianNavy
                            )
                        ),
                        CircleShape
                    )
                    .testTag("mine_grid_tap_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                if (isGridMiningActive) {
                                    listOf(Color(0xFF065F46), Color(0xFF022C22), ObsidianNavy)
                                } else {
                                    listOf(Color(0xFF263238), ObsidianNavy, Color(0xFF0F172A))
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Power Switch",
                            tint = if (isGridMiningActive) Color(0xFF00E676) else GoldGradientEnd,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "MINE GRID",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isGridMiningActive) "ACTIVE • 24H" else "24h Session",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGridMiningActive) Color(0xFF00E676) else MintGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // LIVE COUNTDOWN & STATUS TAG DIRECTLY BELOW BUTTON
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGridMiningActive) Color(0xFF00E676).copy(alpha = pulseAlpha)
                        else SlateGray
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isGridMiningActive) "GRID LIVE • 15.0 MH/s" else "GRID IDLE • TAP TO LINK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
                color = if (isGridMiningActive) Color(0xFF00E676) else SlateGray
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Cycle Countdown: $countdownText • +${activeRateStr} GRID/hr",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = SlateNavy,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LuckyWheelDashboardCard(
    canSpin: Boolean,
    cooldownText: String,
    onSpinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wheel_glow")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = if (canSpin) 1.5.dp else 1.dp,
                brush = if (canSpin) Brush.sweepGradient(
                    listOf(
                        Color(0xFFFFD700).copy(alpha = borderAlpha),
                        Color(0xFFFF9800).copy(alpha = borderAlpha),
                        Color(0xFF00E676).copy(alpha = borderAlpha),
                        Color(0xFFFFD700).copy(alpha = borderAlpha)
                    )
                ) else Brush.linearGradient(listOf(Color(0xFF2C394F), Color(0xFF1E2838))),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { onSpinClick() }
            .testTag("lucky_wheel_dashboard_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1524)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF0D121F),
                            Color(0xFF151C2C),
                            Color(0xFF1A1528)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Icon with animated golden halo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSpin) Brush.sweepGradient(
                                    listOf(Color(0xFFFFD700), Color(0xFFFF9800), Color(0xFFFFE082), Color(0xFFFFD700))
                                ) else Brush.linearGradient(listOf(Color(0xFF1E2838), Color(0xFF2C394F)))
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF0B0E14)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎡",
                                fontSize = 24.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LUCKY WHEEL",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (canSpin) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF00E676).copy(alpha = 0.2f))
                                        .border(0.8.dp, Color(0xFF00E676), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "FREE SPIN READY",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF00E676)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = if (canSpin) "Win up to +200 GRID, +2.0 GH/s, or USDT" else "Daily spin on cooldown • Returns in $cooldownText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (canSpin) GoldGradientEnd else SlateGray,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Action Pill Button
                Button(
                    onClick = onSpinClick,
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .testTag("lucky_wheel_spin_action_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canSpin) Color.Transparent else Color(0xFF1E2838)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    if (canSpin) {
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .background(GoldBrush)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SPIN",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = ObsidianNavy
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = ObsidianNavy,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = GoldGradientEnd,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = cooldownText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
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

@Composable
fun QuantumMiningReactor(
    isMiningActive: Boolean,
    hashPowerGhs: Double,
    effectiveGridRate: Double,
    countdownText: String,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    var triggerBurst by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "PressSpring"
    )

    val burstAnim by animateFloatAsState(
        targetValue = if (triggerBurst) 1.0f else 0.0f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        finishedListener = { triggerBurst = false },
        label = "BurstAnim"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "QuantumReactorLoop")

    // Clockwise outer cyber telemetry rotation (slow & majestic)
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OuterRingRotate"
    )

    // Counter-clockwise inner energy sweep ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "InnerRingRotate"
    )

    // Pulsing radar expanding shockwave
    val shockwaveScale by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShockwaveScale"
    )

    val shockwaveAlpha by infiniteTransition.animateFloat(
        initialValue = if (isMiningActive) 0.65f else 0.35f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShockwaveAlpha"
    )

    // Breathing glow aura
    val breathingGlow by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingGlow"
    )

    // High-voltage electric flicker
    val electricFlicker by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ElectricFlicker"
    )

    Box(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .scale(pressScale),
        contentAlignment = Alignment.Center
    ) {
        // 1. BACKDROP EXPANDING SHOCKWAVE RADAR
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.92f

            if (isMiningActive || shockwaveAlpha > 0.05f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isMiningActive) Color(0xFF00E676) else Color(0xFFFFD700)).copy(alpha = shockwaveAlpha * 0.45f),
                            (if (isMiningActive) Color(0xFF00E5FF) else GoldGradientEnd).copy(alpha = shockwaveAlpha * 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * shockwaveScale
                    ),
                    radius = baseRadius * shockwaveScale,
                    center = center
                )
            }

            // Burst celebration particles
            if (burstAnim > 0f) {
                val burstRadius = baseRadius * (0.8f + burstAnim * 0.7f)
                val particleAlpha = (1f - burstAnim).coerceIn(0f, 1f)
                for (i in 0 until 16) {
                    val angle = (i * (360f / 16f)) * (Math.PI / 180f)
                    val px = center.x + (burstRadius * cos(angle)).toFloat()
                    val py = center.y + (burstRadius * sin(angle)).toFloat()
                    drawCircle(
                        color = (if (i % 2 == 0) Color(0xFFFFD700) else Color(0xFF00E676)).copy(alpha = particleAlpha),
                        radius = 4.dp.toPx() * (1f - burstAnim * 0.5f),
                        center = Offset(px, py)
                    )
                }
            }
        }

        // 2. OUTER CYBERNETIC TELEMETRY RING (Clockwise Rotation)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 8.dp.toPx()

            rotate(outerRotation, pivot = center) {
                // Segmented Outer Border
                drawCircle(
                    color = (if (isMiningActive) Color(0xFF00E676) else GoldGradientEnd).copy(alpha = 0.35f),
                    radius = radius,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )

                // 24 Segmented Telemetry Tick Marks
                for (i in 0 until 24) {
                    val angle = (i * 15f) * (Math.PI / 180f)
                    val isMajor = i % 6 == 0
                    val tickLen = if (isMajor) 7.dp.toPx() else 4.dp.toPx()
                    val startR = radius - tickLen
                    val endR = radius

                    val x1 = center.x + (startR * cos(angle)).toFloat()
                    val y1 = center.y + (startR * sin(angle)).toFloat()
                    val x2 = center.x + (endR * cos(angle)).toFloat()
                    val y2 = center.y + (endR * sin(angle)).toFloat()

                    drawLine(
                        color = if (isMajor) (if (isMiningActive) Color(0xFF00E676) else Color(0xFFFFD700)) else Color(0xFF64748B).copy(alpha = 0.5f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Orbiting Glowing Telemetry Nodes
                for (i in 0 until 4) {
                    val nodeAngle = (i * 90f) * (Math.PI / 180f)
                    val nx = center.x + (radius * cos(nodeAngle)).toFloat()
                    val ny = center.y + (radius * sin(nodeAngle)).toFloat()
                    drawCircle(
                        color = if (isMiningActive) Color(0xFF00E676) else Color(0xFFFFD700),
                        radius = 3.dp.toPx(),
                        center = Offset(nx, ny)
                    )
                }
            }
        }

        // 3. INNER ACCENT ENERGY SWEEP RING (Counter-Clockwise Rotation)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 4.dp.toPx()

            rotate(innerRotation, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            (if (isMiningActive) Color(0xFF00E676) else Color(0xFFFFD700)).copy(alpha = 0.85f * breathingGlow),
                            (if (isMiningActive) Color(0xFF00E5FF) else GoldGradientMid).copy(alpha = 0.4f),
                            Color.Transparent,
                            (if (isMiningActive) Color(0xFF00E676) else Color(0xFFFFD700)).copy(alpha = 0.85f * breathingGlow)
                        ),
                        center = center
                    ),
                    radius = radius,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // 4. MAIN INTERACTIVE 3D REACTOR CORE BUTTON (Diameter ~160dp)
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            (if (isMiningActive) Color(0xFF00E676) else Color(0xFFFFD700)).copy(alpha = 0.25f * breathingGlow),
                            Color.Transparent
                        )
                    )
                )
                .padding(4.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        isPressed = true
                        triggerBurst = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onActivate()
                        Toast.makeText(
                            context,
                            "⚡ Quantum Mining Core Linked • 24h Session Active (+${String.format(Locale.US, "%.2f", effectiveGridRate)} GRID/h)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                .testTag("quantum_mining_reactor_button"),
            contentAlignment = Alignment.Center
        ) {
            // Layered Brushed Titanium / Gold Bezel
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFFFFD700),
                                Color(0xFFB8860B),
                                Color(0xFFFFF4B8),
                                if (isMiningActive) Color(0xFF00E676) else Color(0xFFFFD700),
                                Color(0xFFFFD700)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF0F172A).copy(alpha = 0.8f),
                        shape = CircleShape
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Interior Quantum Core Content Layout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    // Top Telemetry Badge: "CORE ONLINE" / "STANDBY"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isMiningActive) Color(0xFF00E676).copy(alpha = 0.18f)
                                else Color(0xFFFFD700).copy(alpha = 0.15f)
                            )
                            .border(
                                width = 0.6.dp,
                                color = if (isMiningActive) Color(0xFF00E676).copy(alpha = 0.6f) else Color(0xFFFFD700).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isMiningActive) Color(0xFF00E676).copy(alpha = breathingGlow)
                                    else Color(0xFFFFD700)
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isMiningActive) "CORE ONLINE" else "STANDBY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = if (isMiningActive) Color(0xFF00E676) else Color(0xFFFFD700)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 3D Glowing Electric Bolt Icon with subtle flicker
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        (if (isMiningActive) Color(0xFF00E676) else Color(0xFFFFD700)).copy(alpha = 0.35f * electricFlicker),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Quantum Mining Reactor Core",
                            tint = if (isMiningActive) Color(0xFF00E676) else Color(0xFFFFD700),
                            modifier = Modifier
                                .size(26.dp)
                                .scale(electricFlicker)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Center Headline: "HASHGRID" or "ACTIVATE RIG"
                    Text(
                        text = if (isMiningActive) "HASHGRID" else "ACTIVATE RIG",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Bottom Dynamic Telemetry Live Hash Rate / Cycle
                    Text(
                        text = if (isMiningActive) "${String.format(Locale.US, "%.2f", hashPowerGhs)} GH/s ACTIVE" else "START 24H CYCLE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isMiningActive) Color(0xFF38BDF8) else GoldGradientMid
                    )
                }
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(120)
            isPressed = false
        }
    }
}
