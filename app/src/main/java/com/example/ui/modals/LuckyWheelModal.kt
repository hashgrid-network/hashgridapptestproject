package com.example.ui.modals

import android.graphics.Paint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CardWhite
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBrush
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientMid
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SlateNavy
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LuckyWheelModal(
    canSpin: Boolean,
    isSpinning: Boolean,
    lastResult: String?,
    onDismiss: () -> Unit,
    onSpinTrigger: () -> Unit
) {
    var targetRotation by remember { mutableFloatStateOf(0f) }
    var winAwardText by remember { mutableStateOf<String?>(lastResult) }

    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
        label = "wheelRotation"
    )

    val segments = listOf(
        "0.5 USDT",
        "100 Gh/s",
        "1.0 USDT",
        "250 Gh/s",
        "0.5 USDT"
    )

    val segmentColors = listOf(
        Color(0xFFD4AF37), // Gold
        Color(0xFF0B0E14), // Obsidian
        Color(0xFFE6CA65), // Light Gold
        Color(0xFF1E293B), // Dark Slate
        Color(0xFFB8860B)  // Deep Gold
    )

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(28.dp))
                .testTag("lucky_wheel_dialog"),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = GoldGradientEnd,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DAILY LUCKY WHEEL",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = ObsidianNavy
                            )
                            Text(
                                text = "1 Free Spin Every 24h",
                                fontSize = 11.sp,
                                color = SlateGray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSpinning,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SlateGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Wheel Container with Pointer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(260.dp)
                ) {
                    // Spinning Wheel Canvas
                    Canvas(
                        modifier = Modifier
                            .size(240.dp)
                            .rotate(animatedRotation)
                    ) {
                        val canvasSize = size.minDimension
                        val radius = canvasSize / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val sweepAngle = 360f / segments.size

                        for (i in segments.indices) {
                            val startAngle = i * sweepAngle
                            drawArc(
                                color = segmentColors[i % segmentColors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2)
                            )

                            // Draw dividing lines
                            val rad = Math.toRadians(startAngle.toDouble())
                            val endX = center.x + (radius * cos(rad)).toFloat()
                            val endY = center.y + (radius * sin(rad)).toFloat()
                            drawLine(
                                color = Color.White.copy(alpha = 0.4f),
                                start = center,
                                end = Offset(endX, endY),
                                strokeWidth = 2f
                            )
                        }

                        // Outer rim
                        drawCircle(
                            color = Color(0xFFD4AF37),
                            radius = radius,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                        )

                        // Draw Text labels on each segment
                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 32f
                                textAlign = Paint.Align.CENTER
                                isFakeBoldText = true
                            }

                            for (i in segments.indices) {
                                val textAngle = (i * sweepAngle) + (sweepAngle / 2f)
                                val textRad = Math.toRadians(textAngle.toDouble())
                                val textDist = radius * 0.65f
                                val tx = center.x + (textDist * cos(textRad)).toFloat()
                                val ty = center.y + (textDist * sin(textRad)).toFloat()

                                canvas.nativeCanvas.save()
                                canvas.nativeCanvas.rotate(textAngle + 90f, tx, ty)
                                canvas.nativeCanvas.drawText(segments[i], tx, ty + 10f, paint)
                                canvas.nativeCanvas.restore()
                            }
                        }
                    }

                    // Top Pointer Needle
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(24.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Canvas(modifier = Modifier.size(20.dp, 24.dp)) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width / 2f, size.height)
                                lineTo(0f, 0f)
                                lineTo(size.width, 0f)
                                close()
                            }
                            drawPath(path, color = MintGreen)
                        }
                    }

                    // Center Golden Hub
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(ObsidianNavy)
                            .border(3.dp, GoldGradientMid, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HG",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldGradientMid
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Win Result Notice
                if (winAwardText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MintGreen.copy(alpha = 0.12f))
                            .border(1.dp, MintGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎉 Awarded: $winAwardText!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintDark,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Spin Action Button
                Button(
                    onClick = {
                        if (canSpin && !isSpinning) {
                            val extraRounds = 360f * 5f
                            val randomSlice = (0 until segments.size).random() * (360f / segments.size)
                            targetRotation += extraRounds + randomSlice
                            onSpinTrigger()
                        }
                    },
                    enabled = canSpin && !isSpinning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("spin_wheel_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(
                                if (canSpin && !isSpinning) GoldBrush else Brush.linearGradient(listOf(SlateGray, SlateGray))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSpinning) "SPINNING ARCTIC NODE..." else if (!canSpin) "NEXT SPIN IN 24H" else "SPIN FREE WHEEL",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (canSpin && !isSpinning) ObsidianNavy else Color.White
                        )
                    }
                }
            }
        }
    }
}
