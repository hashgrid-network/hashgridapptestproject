package com.example.ui.modals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.WheelRewardType
import com.example.model.WheelSlice
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBrush
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import kotlin.random.Random

data class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float
)

@Composable
fun RewardDialog(
    wonSlice: WheelSlice,
    onDismiss: () -> Unit,
    onCollect: () -> Unit
) {
    val isJackpot = wonSlice.isJackpot || wonSlice.gridAmount >= 200.0

    // Pulse animation for jackpot badge
    val infiniteTransition = rememberInfiniteTransition(label = "RewardGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    // Confetti particles for jackpot
    val particles = remember {
        List(45) {
            ConfettiParticle(
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat() * -0.3f,
                velocityX = (Random.nextFloat() - 0.5f) * 0.25f,
                velocityY = 0.4f + Random.nextFloat() * 0.5f,
                size = 10f + Random.nextFloat() * 16f,
                color = listOf(
                    Color(0xFFFFD700),
                    Color(0xFFFFE082),
                    Color(0xFFFFA000),
                    Color(0xFFFF6F00),
                    Color(0xFF00E676),
                    Color(0xFFFFFFFF)
                ).random(),
                rotationSpeed = Random.nextFloat() * 360f
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (isJackpot) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3500, easing = LinearEasing)
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Gold Confetti Canvas for 200 GRID Jackpot
            if (isJackpot) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val p = progress.value
                    particles.forEach { particle ->
                        val currentX = (particle.initialX + particle.velocityX * p) * size.width
                        val currentY = (particle.initialY + particle.velocityY * p) * size.height
                        if (currentY in 0f..size.height) {
                            drawRect(
                                color = particle.color.copy(alpha = 1f - (p * 0.3f)),
                                topLeft = Offset(currentX, currentY),
                                size = Size(particle.size, particle.size * 0.6f)
                            )
                        }
                    }
                }
            }

            // Main Obsidian-Gold Modal Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = if (isJackpot) 2.dp else 1.2.dp,
                        brush = if (isJackpot) Brush.sweepGradient(
                            listOf(Color(0xFFFFD700), Color(0xFFFF9800), Color(0xFFFFD700))
                        ) else GoldBrush,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .testTag("reward_celebration_dialog"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1420)),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Icon Hero Badge
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .then(if (isJackpot) Modifier.scale(glowScale) else Modifier)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    if (isJackpot) listOf(Color(0xFFFFD700), Color(0xFFB8860B))
                                    else listOf(GoldLight, GoldGradientEnd)
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isJackpot -> Icons.Default.Stars
                                wonSlice.rewardType == WheelRewardType.GRID_COINS -> Icons.Default.ElectricBolt
                                else -> Icons.Default.Speed
                            },
                            contentDescription = "Reward Icon",
                            tint = if (isJackpot) Color(0xFF0B0E14) else ObsidianNavy,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (isJackpot) {
                        // Jackpot Header
                        Text(
                            text = "🎉 JACKPOT UNLOCKED!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color(0xFFFFD700),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You won 200 GRID Coins!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Coins have been credited directly to your pre-launch GRID reserve.",
                            fontSize = 12.5.sp,
                            color = SlateGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    } else if (wonSlice.rewardType == WheelRewardType.GRID_COINS) {
                        // 10, 50, or 5 GRID reward
                        Text(
                            text = "⚡ CONGRATULATIONS!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = GoldGradientEnd,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You won ${wonSlice.gridAmount.toInt()} GRID Coins!",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Native Genesis tokens credited instantly to your wallet.",
                            fontSize = 12.5.sp,
                            color = SlateGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    } else {
                        // Hashrate boost perk
                        Text(
                            text = "🚀 HASHRATE BOOST UNLOCKED!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MintGreen,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = wonSlice.label,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Applied to your active cloud mining speed for the next 24 hours.",
                            fontSize = 12.5.sp,
                            color = SlateGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Action Button
                    Button(
                        onClick = {
                            onCollect()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("collect_reward_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(
                                    if (isJackpot) Brush.horizontalGradient(
                                        listOf(Color(0xFFFFD700), Color(0xFFFF9800))
                                    ) else GoldBrush
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "COLLECT & CONTINUE MINING",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = ObsidianNavy
                            )
                        }
                    }
                }
            }
        }
    }
}
