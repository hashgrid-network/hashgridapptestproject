package com.example.model

/**
 * Tokenomics and Halving configuration for the native GRID Genesis token.
 */
object TokenConfig {
    /**
     * Hard-capped Genesis total supply.
     */
    const val MAX_TOTAL_SUPPLY: Double = 100_000_000.0

    /**
     * Base mining rate in GRID per hour (24.0 GRID per 24h session).
     */
    const val BASE_RATE_PER_HOUR: Double = 1.0

    /**
     * Strict 24-hour mining session duration in milliseconds.
     */
    const val SESSION_DURATION_MS: Long = 86_400_000L

    /**
     * Calculates the base mining rate for the current global miner epoch:
     * - Epoch 1 (< 1,000 miners): Base 1.0 GRID/hr
     * - Epoch 2 (1,000 to 10,000 miners): Base 0.5 GRID/hr
     * - Epoch 3 (10,000 to 100,000 miners): Base 0.25 GRID/hr
     * - Epoch 4 (Supply depleted / Mainnet): 0.0 GRID/hr (Mining concludes)
     */
    fun getBaseRateForMiners(globalMinersCount: Long): Double {
        return when {
            globalMinersCount < 1_000L -> 1.0
            globalMinersCount < 10_000L -> 0.5
            globalMinersCount < 100_000L -> 0.25
            else -> 0.0
        }
    }

    /**
     * Human-readable epoch badge label.
     */
    fun getEpochLabel(globalMinersCount: Long): String {
        return when {
            globalMinersCount < 1_000L -> "Early Adopter Epoch 1"
            globalMinersCount < 10_000L -> "Growth Epoch 2"
            globalMinersCount < 100_000L -> "Expansion Epoch 3"
            else -> "Genesis Concluded (Mainnet)"
        }
    }

    /**
     * Calculates the halving progress gauge string.
     */
    fun getHalvingGaugeText(globalMinersCount: Long): String {
        return when {
            globalMinersCount < 1_000L -> "Current: 1.0 GRID/hr (Next Halving at 1,000 Registered Miners)"
            globalMinersCount < 10_000L -> "Current: 0.5 GRID/hr (Next Halving at 10,000 Registered Miners)"
            globalMinersCount < 100_000L -> "Current: 0.25 GRID/hr (Next Halving at 100,000 Registered Miners)"
            else -> "Final Epoch • Mainnet DEX Listing Imminent"
        }
    }

    /**
     * Active Referral Team Multiplier:
     * Effective Rate = BaseRate * (1.0 + (activeReferralsMiningNow * 0.10))
     * E.g. 3 active referrals gives 1.0 * (1 + 0.30) = 1.30 GRID/hr.
     */
    fun calculateEffectiveRate(baseRate: Double, activeReferralsMiningNow: Int): Double {
        val multiplier = 1.0 + (activeReferralsMiningNow * 0.10)
        return baseRate * multiplier
    }
}
