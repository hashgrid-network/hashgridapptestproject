package com.example.service

import android.content.Context
import com.example.model.ActiveContract
import com.example.model.ActivityItem
import com.example.model.PayoutItem
import com.example.model.PayoutStatus
import org.json.JSONArray
import org.json.JSONObject

data class PersistentUserData(
    val walletBalanceUsdt: Double = 0.0,
    val gridCoinBalance: Double = 0.0,
    val isGridMiningActive: Boolean = false,
    val miningSessionEndTimestamp: Long = 0L,
    val sessionStartTimeMillis: Long = System.currentTimeMillis(),
    val activeContracts: List<ActiveContract> = emptyList(),
    val activityList: List<ActivityItem> = emptyList(),
    val payoutsList: List<PayoutItem> = emptyList(),
    val canSpinToday: Boolean = true,
    val wheelCooldownEnd: Long = 0L,
    val lastSavedTimestamp: Long = System.currentTimeMillis()
)

object LocalPersistenceManager {
    private const val PREF_NAME = "hashgrid_persistent_user_data"

    fun saveUserData(context: Context, userId: String, data: PersistentUserData) {
        if (userId.isBlank()) return
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putFloat("${userId}_usdt_balance", data.walletBalanceUsdt.toFloat())
        editor.putFloat("${userId}_grid_balance", data.gridCoinBalance.toFloat())
        editor.putBoolean("${userId}_is_grid_active", data.isGridMiningActive)
        editor.putLong("${userId}_grid_end_time", data.miningSessionEndTimestamp)
        editor.putLong("${userId}_session_start", data.sessionStartTimeMillis)
        editor.putBoolean("${userId}_can_spin", data.canSpinToday)
        editor.putLong("${userId}_wheel_cooldown", data.wheelCooldownEnd)
        editor.putLong("${userId}_last_saved", data.lastSavedTimestamp)

        // Serialize active contracts
        val contractsArray = JSONArray()
        for (c in data.activeContracts) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("planName", c.planName)
            obj.put("cryptoSymbol", c.cryptoSymbol)
            obj.put("depositUsdt", c.depositUsdt)
            obj.put("hashPowerGh", c.hashPowerGh)
            obj.put("elapsedDays", c.elapsedDays)
            obj.put("totalDays", c.totalDays)
            obj.put("accruedProfitUsdt", c.accruedProfitUsdt)
            obj.put("dailyYieldUsdt", c.dailyYieldUsdt)
            obj.put("isRestakeEnabled", c.isRestakeEnabled)
            obj.put("startDateStr", c.startDateStr)
            obj.put("maturityDateStr", c.maturityDateStr)
            obj.put("startTimestampMs", c.startTimestampMs)
            obj.put("endTimestampMs", c.endTimestampMs)
            contractsArray.put(obj)
        }
        editor.putString("${userId}_active_contracts", contractsArray.toString())

        // Serialize activity list
        val activityArray = JSONArray()
        for (a in data.activityList) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("title", a.title)
            obj.put("subtitle", a.subtitle)
            obj.put("btcAmountStr", a.btcAmountStr)
            obj.put("usdtAmount", a.usdtAmount)
            obj.put("timestampStr", a.timestampStr)
            obj.put("isCredit", a.isCredit)
            activityArray.put(obj)
        }
        editor.putString("${userId}_activity_list", activityArray.toString())

        // Serialize payouts list
        val payoutArray = JSONArray()
        for (p in data.payoutsList) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("dateStr", p.dateStr)
            obj.put("amountUsdt", p.amountUsdt)
            obj.put("targetAddress", p.targetAddress)
            obj.put("network", p.network)
            obj.put("status", p.status.name)
            payoutArray.put(obj)
        }
        editor.putString("${userId}_payouts_list", payoutArray.toString())

        editor.apply()
    }

    fun loadUserData(context: Context, userId: String): PersistentUserData {
        if (userId.isBlank()) return PersistentUserData()
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val usdtBalance = prefs.getFloat("${userId}_usdt_balance", 0.0f).toDouble()
        val gridBalance = prefs.getFloat("${userId}_grid_balance", 0.0f).toDouble()
        val isGridActive = prefs.getBoolean("${userId}_is_grid_active", false)
        val gridEndTime = prefs.getLong("${userId}_grid_end_time", 0L)
        val sessionStart = prefs.getLong("${userId}_session_start", System.currentTimeMillis())
        val canSpin = prefs.getBoolean("${userId}_can_spin", true)
        val wheelCooldown = prefs.getLong("${userId}_wheel_cooldown", 0L)
        val lastSaved = prefs.getLong("${userId}_last_saved", System.currentTimeMillis())

        val contracts = mutableListOf<ActiveContract>()
        val contractsStr = prefs.getString("${userId}_active_contracts", null)
        if (!contractsStr.isNullOrBlank()) {
            try {
                val array = JSONArray(contractsStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    contracts.add(
                        ActiveContract(
                            id = obj.optString("id", "contract_$i"),
                            planName = obj.optString("planName", "Mining Rig"),
                            cryptoSymbol = obj.optString("cryptoSymbol", "USDT"),
                            depositUsdt = obj.optDouble("depositUsdt", 0.0),
                            hashPowerGh = obj.optDouble("hashPowerGh", 0.0),
                            elapsedDays = obj.optInt("elapsedDays", 0),
                            totalDays = obj.optInt("totalDays", 30),
                            accruedProfitUsdt = obj.optDouble("accruedProfitUsdt", 0.0),
                            dailyYieldUsdt = obj.optDouble("dailyYieldUsdt", 0.0),
                            isRestakeEnabled = obj.optBoolean("isRestakeEnabled", false),
                            startDateStr = obj.optString("startDateStr", "Today"),
                            maturityDateStr = obj.optString("maturityDateStr", "30 Days"),
                            startTimestampMs = obj.optLong("startTimestampMs", System.currentTimeMillis()),
                            endTimestampMs = obj.optLong("endTimestampMs", System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val activities = mutableListOf<ActivityItem>()
        val activityStr = prefs.getString("${userId}_activity_list", null)
        if (!activityStr.isNullOrBlank()) {
            try {
                val array = JSONArray(activityStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    activities.add(
                        ActivityItem(
                            id = obj.optString("id", "act_$i"),
                            title = obj.optString("title", "Activity"),
                            subtitle = obj.optString("subtitle", ""),
                            btcAmountStr = obj.optString("btcAmountStr", ""),
                            usdtAmount = obj.optDouble("usdtAmount", 0.0),
                            timestampStr = obj.optString("timestampStr", "Today"),
                            isCredit = obj.optBoolean("isCredit", true)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val payouts = mutableListOf<PayoutItem>()
        val payoutsStr = prefs.getString("${userId}_payouts_list", null)
        if (!payoutsStr.isNullOrBlank()) {
            try {
                val array = JSONArray(payoutsStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val statusStr = obj.optString("status", "PENDING_24H_AUDIT")
                    val statusEnum = try { PayoutStatus.valueOf(statusStr) } catch (_: Exception) { PayoutStatus.PENDING_24H_AUDIT }
                    payouts.add(
                        PayoutItem(
                            id = obj.optString("id", "pay_$i"),
                            dateStr = obj.optString("dateStr", "Today"),
                            amountUsdt = obj.optDouble("amountUsdt", 0.0),
                            targetAddress = obj.optString("targetAddress", ""),
                            network = obj.optString("network", "TRC20"),
                            status = statusEnum
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return PersistentUserData(
            walletBalanceUsdt = usdtBalance,
            gridCoinBalance = gridBalance,
            isGridMiningActive = isGridActive,
            miningSessionEndTimestamp = gridEndTime,
            sessionStartTimeMillis = sessionStart,
            activeContracts = contracts,
            activityList = activities,
            payoutsList = payouts,
            canSpinToday = canSpin,
            wheelCooldownEnd = wheelCooldown,
            lastSavedTimestamp = lastSaved
        )
    }
}
