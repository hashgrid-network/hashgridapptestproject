package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class WheelRewardType {
    GRID_COINS,
    HASHRATE_BOOST,
    USDT_BONUS
}

data class WheelSlice(
    val index: Int,
    val label: String,
    val shortLabel: String,
    val rewardType: WheelRewardType,
    val gridAmount: Double = 0.0,
    val hashrateGhs: Double = 0.0,
    val usdtAmount: Double = 0.0,
    val color: Color,
    val textColor: Color,
    val weight: Double,
    val isJackpot: Boolean = false
)

object WheelConfig {
    /**
     * Slices for the 24-hour Lucky Spin Wheel:
     * - Slice 1 (0): 10 GRID Tokens
     * - Slice 2 (1): +10% Speed (+0.5 GH/s)
     * - Slice 3 (2): +0.25 USDT Bonus
     * - Slice 4 (3): 25 GRID Tokens
     * - Slice 5 (4): 200 GRID MEGA JACKPOT
     * - Slice 6 (5): +25% Speed (+1.0 GH/s)
     * - Slice 7 (6): +0.50 USDT Bonus
     * - Slice 8 (7): 5 GRID Tokens
     */
    val SLICES = listOf(
        WheelSlice(
            index = 0,
            label = "10 GRID Tokens",
            shortLabel = "10 GRID",
            rewardType = WheelRewardType.GRID_COINS,
            gridAmount = 10.0,
            color = Color(0xFFD4AF37), // Gold Accent
            textColor = Color(0xFF0F1420),
            weight = 20.0
        ),
        WheelSlice(
            index = 1,
            label = "+10% Speed (+0.5 GH/s)",
            shortLabel = "+10% Spd",
            rewardType = WheelRewardType.HASHRATE_BOOST,
            hashrateGhs = 0.5,
            color = Color(0xFF161F30), // Obsidian Slate
            textColor = Color.White,
            weight = 15.0
        ),
        WheelSlice(
            index = 2,
            label = "+0.25 USDT Bonus",
            shortLabel = "+$0.25",
            rewardType = WheelRewardType.USDT_BONUS,
            usdtAmount = 0.25,
            color = Color(0xFF00E676), // Emerald Green
            textColor = Color(0xFF0F1420),
            weight = 12.0
        ),
        WheelSlice(
            index = 3,
            label = "25 GRID Tokens",
            shortLabel = "25 GRID",
            rewardType = WheelRewardType.GRID_COINS,
            gridAmount = 25.0,
            color = Color(0xFFFF9800), // Neon Amber
            textColor = Color(0xFF0F1420),
            weight = 15.0
        ),
        WheelSlice(
            index = 4,
            label = "200 GRID JACKPOT",
            shortLabel = "200 JACKPOT",
            rewardType = WheelRewardType.GRID_COINS,
            gridAmount = 200.0,
            color = Color(0xFFFFD700), // Glowing Pure Gold
            textColor = Color(0xFF0F1420),
            weight = 3.0, // Ultra Rare (~3%)
            isJackpot = true
        ),
        WheelSlice(
            index = 5,
            label = "+25% Speed (+1.0 GH/s)",
            shortLabel = "+25% Spd",
            rewardType = WheelRewardType.HASHRATE_BOOST,
            hashrateGhs = 1.0,
            color = Color(0xFF1E2838), // Slate Gray
            textColor = Color.White,
            weight = 10.0
        ),
        WheelSlice(
            index = 6,
            label = "+0.50 USDT Bonus",
            shortLabel = "+$0.50",
            rewardType = WheelRewardType.USDT_BONUS,
            usdtAmount = 0.50,
            color = Color(0xFF00C853), // Deep Mint Green
            textColor = Color.White,
            weight = 10.0
        ),
        WheelSlice(
            index = 7,
            label = "5 GRID Tokens",
            shortLabel = "5 GRID",
            rewardType = WheelRewardType.GRID_COINS,
            gridAmount = 5.0,
            color = Color(0xFFE5A93C), // Gold Accent
            textColor = Color(0xFF0F1420),
            weight = 15.0
        )
    )

    fun selectWeightedWinningSlice(): WheelSlice {
        val totalWeight = SLICES.sumOf { it.weight }
        val randomPoint = Random.nextDouble(0.0, totalWeight)
        var cumulative = 0.0
        for (slice in SLICES) {
            cumulative += slice.weight
            if (randomPoint <= cumulative) {
                return slice
            }
        }
        return SLICES.first()
    }

    fun calculateTargetRotation(currentRotation: Float, targetIndex: Int): Float {
        val sweepAngle = 360f / SLICES.size // 45.0 degrees
        val sliceCenterAngle = targetIndex * sweepAngle + (sweepAngle / 2f)
        var desiredOffset = (270f - sliceCenterAngle) % 360f
        if (desiredOffset < 0f) desiredOffset += 360f

        val currentNorm = ((currentRotation % 360f) + 360f) % 360f
        val deltaToTarget = (desiredOffset - currentNorm + 360f) % 360f

        val totalSpins = 6f * 360f
        return currentRotation + totalSpins + deltaToTarget
    }
}
