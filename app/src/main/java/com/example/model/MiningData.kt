package com.example.model

data class User(
    val id: String = "#HG-142597",
    val email: String = "goldbrownp@gmail.com",
    val role: String = "user",
    val referralCode: String = "HG-7798",
    val displayName: String = "Institutional Miner"
)

data class MiningPlan(
    val id: String,
    val name: String,
    val subtitle: String,
    val cryptoSymbol: String,
    val iconCrypto: String,
    val minDepositUsdt: Double,
    val hashPowerGh: Double,
    val monthlyYieldPercent: Double,
    val termDays: Int = 30,
    val dailyYieldUsdtEst: Double,
    val hardwareType: String,
    val tag: String? = null
)

data class ActiveContract(
    val id: String,
    val planName: String,
    val cryptoSymbol: String,
    val depositUsdt: Double,
    val hashPowerGh: Double,
    val elapsedDays: Int,
    val totalDays: Int = 30,
    val accruedProfitUsdt: Double,
    val dailyYieldUsdt: Double,
    val isRestakeEnabled: Boolean = false,
    val startDateStr: String,
    val maturityDateStr: String,
    val startTimestampMs: Long = System.currentTimeMillis() - (12L * 24 * 3600 * 1000),
    val endTimestampMs: Long = System.currentTimeMillis() + (18L * 24 * 3600 * 1000)
)

data class ActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val btcAmountStr: String,
    val usdtAmount: Double,
    val timestampStr: String,
    val isCredit: Boolean = true
)

data class PayoutItem(
    val id: String,
    val dateStr: String,
    val amountUsdt: Double,
    val targetAddress: String,
    val network: String,
    val status: PayoutStatus
)

enum class PayoutStatus(val label: String) {
    COMPLETED("Completed"),
    AUDITED_DISBURSED("Audited & Disbursed"),
    PENDING_24H_AUDIT("Pending 24h Audit")
}

enum class BountyType(val label: String, val defaultReward: Double) {
    WHATSAPP("WhatsApp Status", 0.20),
    TELEGRAM("Telegram Community", 0.20)
}

enum class MilestoneStatus(val label: String) {
    PENDING_EXECUTIVE_AUDIT("Pending Executive Audit"),
    DISBURSED("Milestone Disbursed"),
    REJECTED("Audit Failed")
}

data class CreatorMilestoneSubmission(
    val id: String,
    val userId: String,
    val channelUrl: String,
    val videoUrl: String,
    val contactTelegram: String,
    val claimedViews: Long = 50000L,
    val submittedAt: String,
    val userReferralLink: String = "https://hashgrid.io/join?ref=HG-7798",
    var status: MilestoneStatus = MilestoneStatus.PENDING_EXECUTIVE_AUDIT,
    var awardedGift: String? = null,
    var auditNotes: String? = null
)

enum class BountyStatus(val label: String) {
    AVAILABLE("Available"),
    PENDING_ADMIN_REVIEW("Pending Review"),
    APPROVED_CREDITED("Approved & Credited"),
    REJECTED("Rejected")
}

data class BountyTask(
    val id: String,
    val type: BountyType,
    val title: String,
    val description: String,
    val rewardUsdt: Double,
    val status: BountyStatus = BountyStatus.AVAILABLE,
    val submissionProof: String? = null,
    val extraDetail: String? = null,
    val rejectionReason: String? = null,
    val lastSubmittedTimestamp: Long? = null
)

data class AdminBountySubmission(
    val id: String,
    val userId: String,
    val taskId: String,
    val type: BountyType,
    val title: String,
    val rewardUsdt: Double,
    val submissionProof: String,
    val channelOrExtra: String? = null,
    val submittedAt: String,
    val submittedTimestamp: Long = System.currentTimeMillis(),
    val userReferralLink: String = "https://hashgrid.io/join?ref=HG-7798",
    var status: BountyStatus = BountyStatus.PENDING_ADMIN_REVIEW,
    var rejectionReason: String? = null
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class AdminWithdrawalRequest(
    val id: String,
    val userId: String,
    val userEmail: String,
    val amountUsdt: Double,
    val targetAddress: String,
    val network: String,
    val requestedAt: String,
    val lockedAuditAmount: Double = amountUsdt,
    var status: String = "PENDING_24H_AUDIT"
)

enum class PriceDirection {
    UP,
    DOWN,
    NEUTRAL
}

data class LiveTickerItem(
    val id: String, // e.g. "BTCUSDT"
    val displaySymbol: String, // e.g. "BTC/USDT"
    val baseName: String, // e.g. "Bitcoin"
    val iconCrypto: String, // e.g. "₿"
    val price: Double,
    val priceChangePercent: Double,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val volume24h: Double = 0.0,
    val direction: PriceDirection = PriceDirection.NEUTRAL,
    val lastTickTime: Long = System.currentTimeMillis()
)
