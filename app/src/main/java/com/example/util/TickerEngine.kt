package com.example.util

import java.util.ArrayDeque
import kotlin.random.Random

data class BroadcastItem(
    val walletId: String,
    val icon: String,
    val message: String,
    val fullBroadcast: String,
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun getTimeAgoStr(): String {
        val diffSec = ((System.currentTimeMillis() - timestampMs) / 1000).coerceAtLeast(0)
        return when {
            diffSec < 15 -> "Just now"
            diffSec < 60 -> "${diffSec}s ago"
            diffSec < 3600 -> "${diffSec / 60}m ago"
            else -> "${diffSec / 3600}h ago"
        }
    }
}

object TickerEngine {
    private const val HISTORY_SIZE = 10
    private val historyBuffer = ArrayDeque<BroadcastItem>(HISTORY_SIZE)
    private val recentWalletIds = ArrayDeque<String>(HISTORY_SIZE)

    private val rigActivities = listOf(
        Pair("⚡", "deployed $10 Starter Rig (10 TH/s)"),
        Pair("🚀", "deployed $25 Basic Rig (28 TH/s)"),
        Pair("💎", "deployed $50 Standard Rig (65 TH/s)"),
        Pair("🔥", "deployed $100 Pro Node (150 TH/s)")
    )

    private val withdrawalActivities = listOf(
        Pair("💰", "withdrew $3.00 USDT (30% Milestone reached)"),
        Pair("💰", "withdrew $7.50 USDT (Milestone unlocked)"),
        Pair("💰", "withdrew $15.00 USDT (BEP-20 Payout)")
    )

    private val faucetActivities = listOf(
        Pair("⛏️", "started 24h tap session (+24 GRID)"),
        Pair("🎉", "won 50 GRID on 24h Lucky Wheel"),
        Pair("🎁", "unlocked +2h Mining Hashrate Boost"),
        Pair("✨", "claimed 10 GRID daily faucet reward")
    )

    init {
        // Pre-seed 10 initial broadcasts with realistic staggered timestamps
        val now = System.currentTimeMillis()
        val templates = listOf(
            Pair("⚡", "deployed $10 Starter Rig (10 TH/s)"),
            Pair("💰", "withdrew $3.00 USDT (30% Milestone reached)"),
            Pair("⛏️", "started 24h tap session (+24 GRID)"),
            Pair("🚀", "deployed $25 Basic Rig (28 TH/s)"),
            Pair("🎉", "won 50 GRID on 24h Lucky Wheel"),
            Pair("💰", "withdrew $7.50 USDT (Milestone unlocked)"),
            Pair("🎁", "unlocked +2h Mining Hashrate Boost"),
            Pair("💎", "deployed $50 Standard Rig (65 TH/s)"),
            Pair("✨", "claimed 10 GRID daily faucet reward"),
            Pair("🔥", "deployed $100 Pro Node (150 TH/s)")
        )

        val offsets = listOf(15, 45, 90, 150, 240, 320, 410, 500, 620, 780) // seconds ago
        for (i in templates.indices) {
            val walletHex = generateUniqueWalletId()
            val (icon, msg) = templates[i]
            val fullText = "$icon User 0x$walletHex... $msg"
            val item = BroadcastItem(
                walletId = "0x$walletHex",
                icon = icon,
                message = "User 0x$walletHex... $msg",
                fullBroadcast = fullText,
                timestampMs = now - (offsets[i] * 1000L)
            )
            historyBuffer.addLast(item)
        }
    }

    fun nextDelay(): Long {
        return Random.nextLong(20000L, 25000L)
    }

    private fun generateUniqueWalletId(): String {
        var id: String
        val chars = "0123456789abcdef"
        do {
            id = (1..4).map { chars.random() }.joinToString("")
        } while (recentWalletIds.contains(id))

        if (recentWalletIds.size >= HISTORY_SIZE) {
            recentWalletIds.removeFirst()
        }
        recentWalletIds.addLast(id)
        return id
    }

    @Synchronized
    fun generateNextBroadcast(): BroadcastItem {
        val walletHex = generateUniqueWalletId()
        val walletId = "0x$walletHex"

        val category = Random.nextInt(3)
        val (icon, msg) = when (category) {
            0 -> rigActivities.random()
            1 -> withdrawalActivities.random()
            else -> faucetActivities.random()
        }

        val fullText = "$icon User $walletId... $msg"
        val newItem = BroadcastItem(
            walletId = walletId,
            icon = icon,
            message = "User $walletId... $msg",
            fullBroadcast = fullText,
            timestampMs = System.currentTimeMillis()
        )

        if (historyBuffer.size >= HISTORY_SIZE) {
            historyBuffer.removeLast()
        }
        historyBuffer.addFirst(newItem)

        return newItem
    }

    @Synchronized
    fun getRecentBroadcasts(): List<BroadcastItem> {
        return historyBuffer.toList()
    }

    @Synchronized
    fun generateNextActivity(): String {
        return generateNextBroadcast().fullBroadcast
    }
}
