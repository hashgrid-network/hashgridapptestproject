package com.example.service

import android.content.Context
import com.example.db.HashGridRoomDatabase
import com.example.db.UserPersistenceEntity
import com.example.model.ActiveContract
import com.example.model.ActivityItem
import com.example.model.PayoutItem
import com.example.model.PayoutStatus
import com.example.model.safeComputeTaskProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun Double.toCleanDouble(default: Double = 0.0): Double {
        return if (this.isNaN() || this.isInfinite()) default else this
    }

    fun saveUserData(context: Context, userId: String, data: PersistentUserData) {
        if (userId.isBlank()) return
        try {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            editor.putString("${userId}_usdt_balance", data.walletBalanceUsdt.toCleanDouble().toString())
            editor.putString("${userId}_grid_balance", data.gridCoinBalance.toCleanDouble().toString())
            editor.putBoolean("${userId}_is_grid_active", data.isGridMiningActive)
            editor.putLong("${userId}_grid_end_time", data.miningSessionEndTimestamp)
            editor.putLong("${userId}_session_start", data.sessionStartTimeMillis)
            editor.putBoolean("${userId}_can_spin", data.canSpinToday)
            editor.putLong("${userId}_wheel_cooldown", data.wheelCooldownEnd)
            editor.putLong("${userId}_last_saved", data.lastSavedTimestamp)

            // Serialize active contracts
            val contractsArray = JSONArray()
            for (c in data.activeContracts) {
                try {
                    val obj = JSONObject()
                    obj.put("id", c.id)
                    obj.put("planName", c.planName)
                    obj.put("cryptoSymbol", c.cryptoSymbol)
                    obj.put("depositUsdt", c.depositUsdt.toCleanDouble())
                    obj.put("hashPowerGh", c.hashPowerGh.toCleanDouble())
                    obj.put("elapsedDays", c.elapsedDays)
                    obj.put("totalDays", c.totalDays)
                    obj.put("accruedProfitUsdt", c.accruedProfitUsdt.toCleanDouble())
                    obj.put("dailyYieldUsdt", c.dailyYieldUsdt.toCleanDouble())
                    obj.put("isRestakeEnabled", c.isRestakeEnabled)
                    obj.put("startDateStr", c.startDateStr)
                    obj.put("maturityDateStr", c.maturityDateStr)
                    obj.put("startTimestampMs", c.startTimestampMs)
                    obj.put("endTimestampMs", c.endTimestampMs)
                    contractsArray.put(obj)
                } catch (_: Exception) {}
            }
            editor.putString("${userId}_active_contracts", contractsArray.toString())

            // Serialize activity list
            val activityArray = JSONArray()
            for (a in data.activityList) {
                try {
                    val obj = JSONObject()
                    obj.put("id", a.id)
                    obj.put("title", a.title)
                    obj.put("subtitle", a.subtitle)
                    obj.put("btcAmountStr", a.btcAmountStr)
                    obj.put("usdtAmount", a.usdtAmount.toCleanDouble())
                    obj.put("timestampStr", a.timestampStr)
                    obj.put("isCredit", a.isCredit)
                    activityArray.put(obj)
                } catch (_: Exception) {}
            }
            editor.putString("${userId}_activity_list", activityArray.toString())

            // Serialize payouts list
            val payoutArray = JSONArray()
            for (p in data.payoutsList) {
                try {
                    val obj = JSONObject()
                    obj.put("id", p.id)
                    obj.put("dateStr", p.dateStr)
                    obj.put("amountUsdt", p.amountUsdt.toCleanDouble())
                    obj.put("targetAddress", p.targetAddress)
                    obj.put("network", p.network)
                    obj.put("status", p.status.name)
                    payoutArray.put(obj)
                } catch (_: Exception) {}
            }
            editor.putString("${userId}_payouts_list", payoutArray.toString())

            editor.apply()

            // Asynchronously save to Room Database
            scope.launch {
                try {
                    val db = HashGridRoomDatabase.getDatabase(appContext)
                    val entity = UserPersistenceEntity(
                        userId = userId,
                        walletBalanceUsdt = data.walletBalanceUsdt.toCleanDouble(),
                        gridCoinBalance = data.gridCoinBalance.toCleanDouble(),
                        isGridMiningActive = data.isGridMiningActive,
                        miningSessionEndTimestamp = data.miningSessionEndTimestamp,
                        sessionStartTimeMillis = data.sessionStartTimeMillis,
                        activeContractsJson = contractsArray.toString(),
                        activityListJson = activityArray.toString(),
                        payoutsListJson = payoutArray.toString(),
                        canSpinToday = data.canSpinToday,
                        wheelCooldownEnd = data.wheelCooldownEnd,
                        lastSavedTimestamp = data.lastSavedTimestamp
                    )
                    db.userPersistenceDao().saveUserData(entity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadUserData(context: Context, userId: String): PersistentUserData {
        if (userId.isBlank()) return PersistentUserData()
        try {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            val rawUsdt = prefs.getString("${userId}_usdt_balance", null)
            val usdtBalance = (rawUsdt?.toDoubleOrNull()
                ?: try { prefs.getFloat("${userId}_usdt_balance", 0.0f).toDouble() } catch (_: Exception) { 0.0 }).toCleanDouble()

            val rawGrid = prefs.getString("${userId}_grid_balance", null)
            val gridBalance = (rawGrid?.toDoubleOrNull()
                ?: try { prefs.getFloat("${userId}_grid_balance", 0.0f).toDouble() } catch (_: Exception) { 0.0 }).toCleanDouble()

            val isGridActive = try { prefs.getBoolean("${userId}_is_grid_active", false) } catch (_: Exception) { false }
            val gridEndTime = try { prefs.getLong("${userId}_grid_end_time", 0L) } catch (_: Exception) { 0L }
            val sessionStart = try { prefs.getLong("${userId}_session_start", System.currentTimeMillis()) } catch (_: Exception) { System.currentTimeMillis() }
            val canSpin = try { prefs.getBoolean("${userId}_can_spin", true) } catch (_: Exception) { true }
            val wheelCooldown = try { prefs.getLong("${userId}_wheel_cooldown", 0L) } catch (_: Exception) { 0L }
            val lastSaved = try { prefs.getLong("${userId}_last_saved", System.currentTimeMillis()) } catch (_: Exception) { System.currentTimeMillis() }

            val contracts = parseContracts(prefs.getString("${userId}_active_contracts", null))
            val activities = parseActivities(prefs.getString("${userId}_activity_list", null))
            val payouts = parsePayouts(prefs.getString("${userId}_payouts_list", null))

            if (usdtBalance > 0.0 || gridBalance > 0.0 || contracts.isNotEmpty() || activities.isNotEmpty() || payouts.isNotEmpty()) {
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

            // Room Database fallback
            try {
                val db = HashGridRoomDatabase.getDatabase(appContext)
                val roomEntity = db.userPersistenceDao().getUserDataSync(userId)
                if (roomEntity != null) {
                    val roomContracts = parseContracts(roomEntity.activeContractsJson)
                    val roomActivities = parseActivities(roomEntity.activityListJson)
                    val roomPayouts = parsePayouts(roomEntity.payoutsListJson)

                    val restored = PersistentUserData(
                        walletBalanceUsdt = roomEntity.walletBalanceUsdt.toCleanDouble(),
                        gridCoinBalance = roomEntity.gridCoinBalance.toCleanDouble(),
                        isGridMiningActive = roomEntity.isGridMiningActive,
                        miningSessionEndTimestamp = roomEntity.miningSessionEndTimestamp,
                        sessionStartTimeMillis = roomEntity.sessionStartTimeMillis,
                        activeContracts = roomContracts,
                        activityList = roomActivities,
                        payoutsList = roomPayouts,
                        canSpinToday = roomEntity.canSpinToday,
                        wheelCooldownEnd = roomEntity.wheelCooldownEnd,
                        lastSavedTimestamp = roomEntity.lastSavedTimestamp
                    )
                    saveUserData(appContext, userId, restored)
                    return restored
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
        } catch (e: Exception) {
            e.printStackTrace()
            return PersistentUserData()
        }
    }

    private fun parseContracts(jsonStr: String?): List<ActiveContract> {
        val list = mutableListOf<ActiveContract>()
        if (jsonStr.isNullOrBlank()) return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                try {
                    val obj = array.getJSONObject(i)
                    val depositUsdt = obj.optDouble("depositUsdt", 0.0).toCleanDouble()
                    val hashPowerGh = obj.optDouble("hashPowerGh", 0.0).toCleanDouble()
                    val accruedProfitUsdt = obj.optDouble("accruedProfitUsdt", 0.0).toCleanDouble()
                    val dailyYieldUsdt = obj.optDouble("dailyYieldUsdt", 0.0).toCleanDouble()
                    val targetYield30 = if (depositUsdt > 0) depositUsdt * 0.30 else 3.0
                    val taskProgress = safeComputeTaskProgress(accruedProfitUsdt, depositUsdt)

                    list.add(
                        ActiveContract(
                            id = obj.optString("id", "contract_$i"),
                            planName = obj.optString("planName", "Mining Rig"),
                            cryptoSymbol = obj.optString("cryptoSymbol", "USDT"),
                            depositUsdt = depositUsdt,
                            hashPowerGh = hashPowerGh,
                            elapsedDays = obj.optInt("elapsedDays", 0),
                            totalDays = obj.optInt("totalDays", 30),
                            accruedProfitUsdt = accruedProfitUsdt,
                            dailyYieldUsdt = dailyYieldUsdt,
                            isRestakeEnabled = obj.optBoolean("isRestakeEnabled", false),
                            startDateStr = obj.optString("startDateStr", "Today"),
                            maturityDateStr = obj.optString("maturityDateStr", "30 Days"),
                            startTimestampMs = obj.optLong("startTimestampMs", System.currentTimeMillis()),
                            endTimestampMs = obj.optLong("endTimestampMs", System.currentTimeMillis() + 30L * 24 * 3600 * 1000),
                            plan_cost = depositUsdt,
                            target_yield_30_percent = targetYield30,
                            current_yield_mined = accruedProfitUsdt,
                            task_progress_pct = taskProgress,
                            work_status = if (accruedProfitUsdt >= targetYield30 && depositUsdt > 0) "COMPLETED" else if (depositUsdt <= 0) "COMPLETED" else "IN_PROGRESS",
                            unlocked_for_withdrawal = (accruedProfitUsdt >= targetYield30 || depositUsdt <= 0)
                        )
                    )
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseActivities(jsonStr: String?): List<ActivityItem> {
        val list = mutableListOf<ActivityItem>()
        if (jsonStr.isNullOrBlank()) return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                try {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ActivityItem(
                            id = obj.optString("id", "act_$i"),
                            title = obj.optString("title", "Activity"),
                            subtitle = obj.optString("subtitle", ""),
                            btcAmountStr = obj.optString("btcAmountStr", ""),
                            usdtAmount = obj.optDouble("usdtAmount", 0.0).toCleanDouble(),
                            timestampStr = obj.optString("timestampStr", "Today"),
                            isCredit = obj.optBoolean("isCredit", true)
                        )
                    )
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parsePayouts(jsonStr: String?): List<PayoutItem> {
        val list = mutableListOf<PayoutItem>()
        if (jsonStr.isNullOrBlank()) return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                try {
                    val obj = array.getJSONObject(i)
                    val statusStr = obj.optString("status", "PENDING_24H_AUDIT")
                    val statusEnum = try { PayoutStatus.valueOf(statusStr) } catch (_: Exception) { PayoutStatus.PENDING_24H_AUDIT }
                    list.add(
                        PayoutItem(
                            id = obj.optString("id", "pay_$i"),
                            dateStr = obj.optString("dateStr", "Today"),
                            amountUsdt = obj.optDouble("amountUsdt", 0.0).toCleanDouble(),
                            targetAddress = obj.optString("targetAddress", ""),
                            network = obj.optString("network", "TRC20"),
                            status = statusEnum
                        )
                    )
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
