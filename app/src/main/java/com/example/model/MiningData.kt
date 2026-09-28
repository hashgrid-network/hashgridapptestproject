package com.example.model

enum class PriceDirection {
    UP,
    DOWN,
    NEUTRAL
}

enum class PayoutStatus(val label: String) {
    COMPLETED("Completed"),
    PENDING_24H_AUDIT("24h Audit Pending"),
    AUDITED_DISBURSED("Audited & Disbursed"),
    PROCESSING("Processing"),
    REJECTED("Rejected")
}

enum class BountyStatus {
    AVAILABLE,
    PENDING_ADMIN_REVIEW,
    APPROVED,
    APPROVED_CREDITED,
    REJECTED
}

enum class BountyType {
    WHATSAPP,
    TELEGRAM,
    YOUTUBE,
    TWITTER,
    CUSTOM
}

enum class MilestoneStatus {
    PENDING_EXECUTIVE_AUDIT,
    APPROVED,
    REJECTED
}

data class LiveTickerItem(
    val id: String = "",
    val displaySymbol: String = "",
    val baseName: String = "",
    val iconCrypto: String = "",
    val symbol: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val priceChangePercent: Double = 0.0,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val volume24h: Double = 0.0,
    val direction: PriceDirection = PriceDirection.NEUTRAL,
    val lastTickTime: Long = System.currentTimeMillis()
)

data class TeamMember(
    val uid: String = "",
    val displayName: String = "Active Miner",
    val email: String = "",
    val joinedAtStr: String = "Recently",
    val joinedAtMs: Long = 0L,
    val status: String = "ACTIVE",
    val hashrateBonus: Double = 1.5,
    val hashrateContributed: Double = 1.5,
    val isMining: Boolean = true,
    val avatarUrl: String? = null
)

data class User(
    val id: String = "",
    val email: String = "",
    val role: String = "user",
    val referralCode: String = "",
    val referredBy: String? = null,
    val referrerUid: String? = null,
    val appliedReferralCode: String? = null,
    val referralCount: Long = 0,
    val teamCount: Long = 0,
    val directReferrals: Long = 0,
    val bonusHashrate: Double = 0.0,
    val extraHashrate: Double = 0.0,
    val totalReferralRewardsUsdt: Double = 0.0,
    val syndicateTier: String = "NOVICE",
    val displayName: String = "Institutional Miner",
    val photoUrl: String? = null,
    val isFlaggedDuplicate: Boolean = false,
    val kycStatus: String = "UNVERIFIED",
    val twoFactorEnabled: Boolean = false,
    val usdtBalance: Double = 0.0,
    val btcBalance: Double = 0.0
)

data class MiningPlan(
    val id: String = "",
    val name: String = "Cloud Rig",
    val subtitle: String = "",
    val cryptoSymbol: String = "BTC",
    val iconCrypto: String = "⚡",
    val minDepositUsdt: Double = 10.0,
    val hashPowerGh: Double = 10000.0,
    val monthlyYieldPercent: Double = 11.5,
    val termDays: Int = 30,
    val dailyYieldUsdtEst: Double = 0.038,
    val hardwareType: String = "ASIC Liquid Rig",
    val tag: String? = null
)

fun safeComputeTaskProgress(accrued: Double, deposit: Double): Double {
    return try {
        if (deposit <= 0.0 || accrued.isNaN() || deposit.isNaN() || accrued.isInfinite() || deposit.isInfinite()) {
            100.0
        } else {
            val target = deposit * 0.30
            if (target <= 0.0) {
                100.0
            } else {
                val pct = (accrued / target) * 100.0
                if (pct.isNaN() || pct.isInfinite()) 0.0 else pct.coerceIn(0.0, 100.0)
            }
        }
    } catch (_: Exception) {
        0.0
    }
}

data class ActiveContract(
    val id: String = "",
    val planName: String = "Mining Rig",
    val cryptoSymbol: String = "BTC",
    val depositUsdt: Double = 0.0,
    val hashPowerGh: Double = 0.0,
    val elapsedDays: Int = 0,
    val totalDays: Int = 30,
    val accruedProfitUsdt: Double = 0.0,
    val dailyYieldUsdt: Double = 0.0,
    val isRestakeEnabled: Boolean = false,
    val startDateStr: String = "Today",
    val maturityDateStr: String = "30 Days Term",
    val startTimestampMs: Long = System.currentTimeMillis(),
    val endTimestampMs: Long = System.currentTimeMillis() + (30L * 24 * 3600 * 1000),
    val costUsdt: Double = depositUsdt,
    val hashrateThs: Double = if (hashPowerGh >= 1000) hashPowerGh / 1000.0 else hashPowerGh,
    val isActive: Boolean = System.currentTimeMillis() < endTimestampMs,
    // Work Tracker Data Model (30% contract completion rule)
    val plan_cost: Double = depositUsdt,
    val target_yield_30_percent: Double = if (depositUsdt > 0) depositUsdt * 0.30 else 3.0,
    val current_yield_mined: Double = accruedProfitUsdt,
    val task_progress_pct: Double = safeComputeTaskProgress(accruedProfitUsdt, depositUsdt),
    val work_status: String = if (accruedProfitUsdt >= (depositUsdt * 0.30) && depositUsdt > 0) "COMPLETED" else if (depositUsdt <= 0) "COMPLETED" else "IN_PROGRESS",
    val unlocked_for_withdrawal: Boolean = (accruedProfitUsdt >= (depositUsdt * 0.30) || depositUsdt <= 0)
) {
    val planCost: Double get() = plan_cost
    val targetYield30Percent: Double get() = target_yield_30_percent
    val currentYieldMined: Double get() = current_yield_mined
    val taskProgressPct: Double get() = task_progress_pct
    val workStatus: String get() = work_status
    val unlockedForWithdrawal: Boolean get() = unlocked_for_withdrawal

    fun getMaturityCountdownStr(currentTimeMs: Long = System.currentTimeMillis()): String {
        return try {
            val remainingMs = maxOf(0L, endTimestampMs - currentTimeMs)
            if (remainingMs <= 0) return "Contract Matured"
            val days = remainingMs / (24 * 3600 * 1000L)
            val hours = (remainingMs % (24 * 3600 * 1000L)) / (3600 * 1000L)
            val mins = (remainingMs % (3600 * 1000L)) / (60 * 1000L)
            if (days > 0) "${days}d ${hours}h left" else if (hours > 0) "${hours}h ${mins}m left" else "${mins}m left"
        } catch (_: Exception) {
            "30 Days Term"
        }
    }
}

data class ActivityItem(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val btcAmountStr: String = "",
    val usdtAmount: Double = 0.0,
    val timestampStr: String = "",
    val isCredit: Boolean = true
)

data class PayoutItem(
    val id: String = "",
    val dateStr: String = "",
    val amountUsdt: Double = 0.0,
    val targetAddress: String = "",
    val network: String = "TRC20",
    val status: PayoutStatus = PayoutStatus.PENDING_24H_AUDIT
)

data class BountyTask(
    val id: String = "",
    val type: BountyType = BountyType.CUSTOM,
    val title: String = "",
    val description: String = "",
    val rewardUsdt: Double = 0.0,
    val status: BountyStatus = BountyStatus.AVAILABLE,
    val submissionProof: String? = null,
    val extraDetail: String? = null,
    val rejectionReason: String? = null
)

data class CreatorMilestoneSubmission(
    val id: String = "",
    val userId: String = "",
    val channelUrl: String = "",
    val videoUrl: String = "",
    val contactTelegram: String = "",
    val submittedAt: String = "",
    val status: MilestoneStatus = MilestoneStatus.PENDING_EXECUTIVE_AUDIT
)

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
