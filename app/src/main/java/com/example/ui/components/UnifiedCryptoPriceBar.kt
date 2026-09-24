package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.example.model.LiveTickerItem
import com.example.model.PriceDirection
import com.example.ui.theme.CardWhite
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBorderSubtle
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import kotlinx.coroutines.delay
import java.util.Locale

private val ElectricMintGreen = Color(0xFF00FFA3)
private val SoftCrimson = Color(0xFFFF4D4D)

@Composable
fun UnifiedCryptoPriceBar(
    liveTickers: List<LiveTickerItem>,
    modifier: Modifier = Modifier
) {
    val btcTicker = liveTickers.find { it.id == "BTCUSDT" } ?: LiveTickerItem(
        id = "BTCUSDT",
        displaySymbol = "BTC/USDT",
        baseName = "Bitcoin",
        iconCrypto = "₿",
        price = 87420.50,
        priceChangePercent = 2.45
    )

    val ethTicker = liveTickers.find { it.id == "ETHUSDT" } ?: LiveTickerItem(
        id = "ETHUSDT",
        displaySymbol = "ETH/USDT",
        baseName = "Ethereum",
        iconCrypto = "Ξ",
        price = 2740.15,
        priceChangePercent = 1.82
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_live_badge")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, GoldBorderSubtle, RoundedCornerShape(22.dp))
            .testTag("unified_crypto_price_bar"),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFFCFBF9))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                // Header: Subtle Glowing Green "● LIVE" Badge & Institutional Stream Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Subtle glowing green badge: "● LIVE"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE6FBF2))
                            .border(0.8.dp, MintGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853).copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "● LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MintDark,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = "Institutional Spot Feed",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateGray,
                        letterSpacing = 0.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Ticker: BTC/USDT Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: BTC Icon & Label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₿",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldGradientEnd
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BTC / USDT",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ObsidianNavy,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GoldBorderSubtle.copy(alpha = 0.5f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "SPOT",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldGradientEnd
                                    )
                                }
                            }
                            Text(
                                text = "Bitcoin Reference Rate",
                                fontSize = 10.sp,
                                color = SlateGray
                            )
                        }
                    }

                    // Right: Animated Live BTC Price & 24h Change
                    TickAnimatedPriceView(
                        price = btcTicker.price,
                        changePercent = btcTicker.priceChangePercent,
                        isLarge = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Divider Line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GoldBorderSubtle.copy(alpha = 0.45f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Ticker: ETH/USDT Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: ETH Icon & Label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDE9FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ξ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ETH / USDT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianNavy,
                                letterSpacing = 0.3.sp
                            )
                            Text(
                                text = "Ethereum Liquid Index",
                                fontSize = 10.sp,
                                color = SlateGray
                            )
                        }
                    }

                    // Right: Animated Live ETH Price & 24h Change
                    TickAnimatedPriceView(
                        price = ethTicker.price,
                        changePercent = ethTicker.priceChangePercent,
                        isLarge = false
                    )
                }
            }
        }
    }
}

/**
 * Visual Tick Animation:
 * - If price goes up: Flash text in Electric Mint Green (#00FFA3) with small ▲ arrow
 * - If price goes down: Flash text in Soft Crimson (#FF4D4D) with small ▼ arrow
 * - Reset to primary color smoothly after 300ms
 */
@Composable
private fun TickAnimatedPriceView(
    price: Double,
    changePercent: Double,
    isLarge: Boolean
) {
    var prevPrice by remember { mutableDoubleStateOf(price) }
    var tickDirection by remember { mutableStateOf(PriceDirection.NEUTRAL) }

    LaunchedEffect(price) {
        if (price > prevPrice) {
            tickDirection = PriceDirection.UP
            delay(300)
            tickDirection = PriceDirection.NEUTRAL
        } else if (price < prevPrice) {
            tickDirection = PriceDirection.DOWN
            delay(300)
            tickDirection = PriceDirection.NEUTRAL
        }
        prevPrice = price
    }

    val animatedColor by animateColorAsState(
        targetValue = when (tickDirection) {
            PriceDirection.UP -> ElectricMintGreen
            PriceDirection.DOWN -> SoftCrimson
            PriceDirection.NEUTRAL -> ObsidianNavy
        },
        animationSpec = tween(durationMillis = 280),
        label = "price_tick_color"
    )

    val isPositive = changePercent >= 0

    Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Direction Arrow
            when (tickDirection) {
                PriceDirection.UP -> {
                    Text(
                        text = "▲",
                        fontSize = if (isLarge) 13.sp else 11.sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricMintGreen
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                PriceDirection.DOWN -> {
                    Text(
                        text = "▼",
                        fontSize = if (isLarge) 13.sp else 11.sp,
                        fontWeight = FontWeight.Black,
                        color = SoftCrimson
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                PriceDirection.NEUTRAL -> {
                    // Neutral subtle arrow
                    Text(
                        text = if (isPositive) "▲" else "▼",
                        fontSize = if (isLarge) 11.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) MintDark else SoftCrimson
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
            }

            // Clean formatted price with 2 decimal places e.g. $87,420.50
            Text(
                text = String.format(Locale.US, "$%,.2f", price),
                fontSize = if (isLarge) 19.sp else 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = animatedColor
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 24h percentage badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isPositive) MintGreen.copy(alpha = 0.18f) else SoftCrimson.copy(alpha = 0.12f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = String.format(Locale.US, "%s%.2f%%", if (isPositive) "+" else "", changePercent),
                fontSize = if (isLarge) 10.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) MintDark else SoftCrimson
            )
        }
    }
}
