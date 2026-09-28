package com.example.model

import java.util.Locale

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
    val name: String = "Starter Node",
    val subtitle: String = "Dedicated Entry Hardware",
    val cryptoSymbol: String = "USDT",
    val iconCrypto: String = "⚡",
    val minDepositUsdt: Double = 10.0,
    val hashPowerGh: Double = 2.0,
    val monthlyYieldPercent: Double = 15.0,
    val termDays: Int = 30,
    val dailyYieldUsdtEst: Double = 0.05,
    val ratePerSecond: Double = 0.0000005787,
    val hardwareType: String = "ASIC Liquid Rig",
    val tag: String? = null,
    val badge: String = "Bronze Node",
    val estMonthlyAmountStr: String = "~$1.50 / Month"
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

data class MiningRig(
    val rig_id: String = "",
    val model_name: String = "Starter Node",
    val cost_usdt: Double = 10.0,
    val max_payout_cap: Double = cost_usdt * 2.0,
    val earned_amount: Double = 0.0,
    val rate_per_second: Double = if (cost_usdt > 0) 0.0000005787 else 0.0001,
    val status: String = "ACTIVE", // "ACTIVE" or "EXPIRED"
    val purchased_at: Long = System.currentTimeMillis(),
    val last_synced_at: Long = System.currentTimeMillis(),
    val hashrate_ths: Double = 2.0,
    val crypto_symbol: String = "USDT",
    val badge: String = "Bronze Node"
) {
    val id: String get() = rig_id
    val costUsdt: Double get() = cost_usdt
    val maxPayoutCap: Double get() = if (max_payout_cap > 0.0) max_payout_cap else cost_usdt * 2.0
    val earnedAmount: Double get() = earned_amount
    val ratePerSecond: Double get() = rate_per_second
    val isExpired: Boolean get() = status == "EXPIRED" || earnedAmount >= maxPayoutCap
    val isActive: Boolean get() = status == "ACTIVE" && earnedAmount < maxPayoutCap
    val progressPct: Double get() = if (maxPayoutCap > 0.0) ((earnedAmount / maxPayoutCap) * 100.0).coerceIn(0.0, 100.0) else 100.0
    val dailyYieldUsdt: Double get() = rate_per_second * 86400.0
    val remainingCap: Double get() = (maxPayoutCap - earnedAmount).coerceAtLeast(0.0)

    fun toActiveContract(): ActiveContract {
        val target30 = if (cost_usdt > 0) cost_usdt * 0.30 else 3.0
        val isCompletedOrExpired = isExpired || earned_amount >= target30
        return ActiveContract(
            id = rig_id,
            planName = model_name,
            cryptoSymbol = crypto_symbol,
            depositUsdt = cost_usdt,
            hashPowerGh = hashrate_ths,
            elapsedDays = (((System.currentTimeMillis() - purchased_at) / (24L * 3600 * 1000)).toInt()).coerceAtLeast(0),
            totalDays = 30,
            accruedProfitUsdt = earned_amount,
            dailyYieldUsdt = dailyYieldUsdt,
            isRestakeEnabled = false,
            startDateStr = "Active",
            maturityDateStr = if (isExpired) "Expired" else "Mining Active",
            startTimestampMs = purchased_at,
            endTimestampMs = purchased_at + (30L * 24 * 3600 * 1000),
            costUsdt = cost_usdt,
            hashrateThs = hashrate_ths,
            isActive = isActive,
            plan_cost = cost_usdt,
            target_yield_30_percent = target30,
            current_yield_mined = earned_amount,
            task_progress_pct = progressPct,
            work_status = if (isExpired) "EXPIRED" else if (earned_amount >= target30) "COMPLETED" else "IN_PROGRESS",
            unlocked_for_withdrawal = isCompletedOrExpired || cost_usdt <= 0,
            rig_id = rig_id,
            max_payout_cap = maxPayoutCap,
            earned_amount = earned_amount,
            rate_per_second = rate_per_second,
            status = if (isExpired) "EXPIRED" else "ACTIVE",
            purchased_at = purchased_at,
            last_synced_at = last_synced_at,
            badge = badge
        )
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
    val unlocked_for_withdrawal: Boolean = (accruedProfitUsdt >= (depositUsdt * 0.30) || depositUsdt <= 0),
    // Multi-Rig Independent 2X Cap Lifecycle Fields
    val rig_id: String = id,
    val max_payout_cap: Double = if (depositUsdt > 0) depositUsdt * 2.0 else 2.0,
    val earned_amount: Double = accruedProfitUsdt,
    val rate_per_second: Double = if (dailyYieldUsdt > 0) dailyYieldUsdt / 86400.0 else (depositUsdt * 0.005) / 86400.0,
    val status: String = if (accruedProfitUsdt >= (if (depositUsdt > 0) depositUsdt * 2.0 else 2.0) && depositUsdt > 0) "EXPIRED" else "ACTIVE",
    val purchased_at: Long = startTimestampMs,
    val last_synced_at: Long = System.currentTimeMillis(),
    val badge: String = "Bronze Node"
) {
    val planCost: Double get() = plan_cost
    val targetYield30Percent: Double get() = target_yield_30_percent
    val currentYieldMined: Double get() = current_yield_mined
    val taskProgressPct: Double get() = task_progress_pct
    val workStatus: String get() = work_status
    val unlockedForWithdrawal: Boolean get() = unlocked_for_withdrawal

    val maxPayoutCap: Double get() = if (max_payout_cap > 0.0) max_payout_cap else if (depositUsdt > 0) depositUsdt * 2.0 else 2.0
    val isExpired: Boolean get() = status == "EXPIRED" || earned_amount >= maxPayoutCap || work_status == "EXPIRED"
    val remainingCap: Double get() = (maxPayoutCap - earned_amount).coerceAtLeast(0.0)

    fun toMiningRig(): MiningRig {
        val cost = if (costUsdt > 0) costUsdt else if (depositUsdt > 0) depositUsdt else 10.0
        val cap = if (max_payout_cap > 0) max_payout_cap else cost * 2.0
        val rate = if (rate_per_second > 0) rate_per_second else if (dailyYieldUsdt > 0) dailyYieldUsdt / 86400.0 else (cost * 0.005) / 86400.0
        val hr = if (hashrateThs > 0) hashrateThs else if (hashPowerGh > 0) hashPowerGh else 2.0
        val st = if (status.isNotBlank()) status else if (accruedProfitUsdt >= cap && cap > 0) "EXPIRED" else "ACTIVE"
        return MiningRig(
            rig_id = id.ifBlank { rig_id.ifBlank { "RIG_${System.currentTimeMillis()}" } },
            model_name = planName,
            cost_usdt = cost,
            max_payout_cap = cap,
            earned_amount = accruedProfitUsdt,
            rate_per_second = rate,
            status = st,
            purchased_at = startTimestampMs,
            last_synced_at = last_synced_at,
            hashrate_ths = hr,
            crypto_symbol = cryptoSymbol,
            badge = badge
        )
    }

    fun getMaturityCountdownStr(currentTimeMs: Long = System.currentTimeMillis()): String {
        return try {
            if (isExpired) return "EXPIRED (2X Cap Reached)"
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

data class ReferralReward(
    val rewardId: String = "",
    val fromMinerId: String = "",
    val rigName: String = "Starter Node",
    val rigCost: Double = 10.0,
    val commissionRate: Double = 0.07,
    val commissionUsdt: Double = 0.70,
    val createdAtStr: String = "Just now",
    val createdAtMs: Long = System.currentTimeMillis(),
    val type: String = "RIG_PURCHASE_COMMISSION"
)

data class TaskSubmissionItem(
    val submissionId: String = "",
    val walletId: String = "",
    val userId: String = "",
    val taskType: String = "DAILY_STATUS", // "DAILY_STATUS" | "TELEGRAM_PROMO" | "YOUTUBE_COLLAB" | "VIEWS_50K"
    val title: String = "Daily Status 10-Hour Views",
    val proofMorningUrl: String? = null,
    val proofEveningUrl: String? = null,
    val proofLink: String? = null,
    val status: String = "PENDING", // "PENDING" | "APPROVED" | "REJECTED"
    val rewardAmountUsdt: Double = 0.20,
    val submittedAtStr: String = "Just now",
    val submittedAtMs: Long = System.currentTimeMillis(),
    val reviewedAtStr: String? = null,
    val adminNote: String? = null
)

