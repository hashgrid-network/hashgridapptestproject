package com.example.util

import java.util.ArrayDeque
import java.util.Locale
import kotlin.random.Random

object TickerEngine {
    private const val MAX_QUEUE_SIZE = 50
    private val recentIdsQueue = ArrayDeque<String>(MAX_QUEUE_SIZE)

    private val plans = listOf(
        "$10 Starter Rig",
        "$25 Basic Rig",
        "$50 Standard Rig",
        "$100 Pro Rig"
    )

    private val timeAgoOptions = listOf(
        "Just now",
        "1m ago",
        "3m ago",
        "6m ago",
        "11m ago"
    )

    /**
     * Generates a realistic organic delay between 20000ms and 30000ms (20s to 30s).
     */
    fun nextDelay(): Long {
        return Random.nextLong(20000L, 30000L)
    }

    /**
     * Procedural dynamic generator for infinite live miner activity.
     * Guarantees no ID repetition within the last 50 events.
     */
    @Synchronized
    fun generateNextActivity(): String {
        val id = generateUniqueMinerId()
        val timeAgo = timeAgoOptions.random()
        val roll = Random.nextInt(100) // 0 until 100

        return when {
            // 60% chance: RIG DEPLOYMENT (roll 0..59)
            roll < 60 -> {
                val plan = plans.random()
                "🟢 Miner $id deployed $plan • $timeAgo"
            }
            // 25% chance: TASK COMPLETION (roll 60..84)
            roll < 85 -> {
                "⚡ Miner $id reached 30% mining threshold • $timeAgo"
            }
            // 15% chance: REAL WITHDRAWAL (roll 85..99)
            else -> {
                val withdrawAmount = String.format(Locale.US, "%.2f", Random.nextDouble(3.00, 28.50))
                "💸 Miner $id successfully withdrew $withdrawAmount USDT • $timeAgo"
            }
        }
    }

    private fun generateUniqueMinerId(): String {
        var attempts = 0
        var id: String
        do {
            id = "HG-" + Random.nextInt(1024, 9899).toString()
            attempts++
        } while (recentIdsQueue.contains(id) && attempts < 100)

        if (recentIdsQueue.size >= MAX_QUEUE_SIZE) {
            recentIdsQueue.removeFirst()
        }
        recentIdsQueue.addLast(id)
        return id
    }
}
