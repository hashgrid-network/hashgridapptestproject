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
    TWITTER,
    CUSTOM
}

enum class MilestoneStatus {
    PENDING_EXECUTIVE_AUDIT,
    APPROVED,
    REJECTED
}

data class LiveTickerItem(
    val id: String,
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

data class User(
    val id: String = "#HG-142597",
    val email: String = "goldbrownp@gmail.com",
    val role: String = "user",
    val referralCode: String = "HG-7798",
    val displayName: String = "Institutional Miner",
    val isFlaggedDuplicate: Boolean = false
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
    val subtitle: String = "",
    val btcAmountStr: String = "",
    val usdtAmount: Double = 0.0,
    val timestampStr: String = "",
    val isCredit: Boolean = true
)

data class PayoutItem(
    val id: String,
    val dateStr: String,
    val amountUsdt: Double,
    val targetAddress: String,
    val network: String = "TRC20",
    val status: PayoutStatus = PayoutStatus.PENDING_24H_AUDIT
)

data class BountyTask(
    val id: String,
    val type: BountyType,
    val title: String,
    val description: String,
    val rewardUsdt: Double,
    val status: BountyStatus = BountyStatus.AVAILABLE,
    val submissionProof: String? = null,
    val extraDetail: String? = null,
    val rejectionReason: String? = null
)

data class CreatorMilestoneSubmission(
    val id: String,
    val userId: String,
    val channelUrl: String,
    val videoUrl: String,
    val contactTelegram: String,
    val submittedAt: String,
    val status: MilestoneStatus = MilestoneStatus.PENDING_EXECUTIVE_AUDIT
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
