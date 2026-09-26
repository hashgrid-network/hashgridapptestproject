package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class WheelRewardType {
    GRID_COINS,
    HASHRATE_BOOST
}

data class WheelSlice(
    val index: Int,
    val label: String,
    val shortLabel: String,
    val rewardType: WheelRewardType,
    val gridAmount: Double = 0.0,
    val hashrateGhs: Double = 0.0,
    val color: Color,
    val textColor: Color,
    val weight: Double,
    val isJackpot: Boolean = false
)

object WheelConfig {
    /**
     * Slices for the 24-hour Lucky Spin Wheel:
     * - Slice 1 (0): 10 GRID Coins (Gold Accent)
     * - Slice 2 (1): +0.5 GH/s Hashrate (24h)
     * - Slice 3 (2): 50 GRID Coins (Neon Amber)
     * - Slice 4 (3): +1.0 GH/s Hashrate (24h)
     * - Slice 5 (4): 200 GRID JACKPOT (Glowing Pure Gold)
     * - Slice 6 (5): 5 GRID Starter Bonus
     * - Slice 7 (6): +2.0 GH/s Power Boost
     * - Slice 8 (7): 10 GRID Coins
     */
    val SLICES = listOf(
        WheelSlice(
            index = 0,
            label = "10 GRID Coins",
            shortLabel = "10 GRID",
            rewardType = WheelRewardType.GRID_COINS,
            gridAmount = 10.0,
            color = Color(0xFFD4AF37), // Gold Accent
            textColor = Color(0xFF0F1420),
            weight = 22.5
        ),
        WheelSlice(
            index = 1,
            label = "+0.5 GH/s Hashrate (24h)",
            shortLabel = "+0.5 GH/s",
            rewardType = WheelRewardType.HASHRATE_BOOST,
            hashrateGhs = 0.5,
            color = Color(0xFF161F30), // Obsidian Slate
            textColor = Color.White,
            weight = 15.0
        ),
        WheelSlice(
            index = 2,
            label = "50 GRID Coins",
            shortLabel = "50 GRID",
            rewardType = WheelRewardType.GRID_COINS,
            gridAmount = 50.0,
            color = Color(0xFFFF9800), // Neon Amber
            textColor = Color(0xFF0F1420),
            weight = 12.0
        ),
        WheelSlice(
            index = 3,
            label = "+1.0 GH/s Hashrate (24h)",
            shortLabel = "+1.0 GH/s",
            rewardType = WheelRewardType.HASHRATE_BOOST,
            hashrateGhs = 1.0,
            color = Color(0xFF0F1524), // Dark Obsidian
            textColor = Color.White,
            weight = 10.0
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
            label = "5 GRID Starter Bonus",
            shortLabel = "5 GRID",
            rewardType = WheelRewardType.GRID_COINS,
            gridAmount = 5.0,
            color = Color(0xFF1E2838), // Slate Gray
            textColor = Color.White,
            weight = 10.0
        ),
        WheelSlice(
            index = 6,
            label = "+2.0 GH/s Power Boost",
            shortLabel = "+2.0 GH/s",
            rewardType = WheelRewardType.HASHRATE_BOOST,
            hashrateGhs = 2.0,
            color = Color(0xFF121B2B), // Deep Slate
            textColor = Color.White,
            weight = 5.0
        ),
        WheelSlice(
            index = 7,
            label = "10 GRID Coins",
            shortLabel = "10 GRID",
            rewardType = WheelRewardType.GRID_COINS,
            gridAmount = 10.0,
            color = Color(0xFFE5A93C), // Gold Accent
            textColor = Color(0xFF0F1420),
            weight = 22.5
        )
    )

    /**
     * Server-safe weighted random selection engine.
     * Guaranteed distribution:
     * - 10 GRID Coins: ~45% (Slice 0: 22.5% + Slice 7: 22.5%)
     * - 50 GRID Coins: ~12%
     * - 200 GRID Jackpot: ~3% (Ultra Rare)
     * - Hashrate/Other Perks: ~40%
     */
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

    /**
     * Calculates the physics target rotation in degrees so that the needle at TopCenter (270°)
     * aligns precisely with the center of the target slice index.
     */
    fun calculateTargetRotation(currentRotation: Float, targetIndex: Int): Float {
        val sweepAngle = 360f / SLICES.size // 45.0 degrees
        val sliceCenterAngle = targetIndex * sweepAngle + (sweepAngle / 2f)
        var desiredOffset = (270f - sliceCenterAngle) % 360f
        if (desiredOffset < 0f) desiredOffset += 360f

        val currentNorm = ((currentRotation % 360f) + 360f) % 360f
        val deltaToTarget = (desiredOffset - currentNorm + 360f) % 360f

        // 6 full spins (2160 deg) for suspenseful deceleration
        val totalSpins = 6f * 360f
        return currentRotation + totalSpins + deltaToTarget
    }
}
