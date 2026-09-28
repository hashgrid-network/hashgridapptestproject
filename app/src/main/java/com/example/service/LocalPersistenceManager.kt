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
    val referralBalanceUsdt: Double = 0.0,
    val taskBalanceUsdt: Double = 0.0,
    val totalReferrals: Long = 0L,
    val isGridMiningActive: Boolean = false,
    val miningSessionEndTimestamp: Long = 0L,
    val sessionStartTimeMillis: Long = System.currentTimeMillis(),
    val activeContracts: List<ActiveContract> = emptyList(),
    val activityList: List<ActivityItem> = emptyList(),
    val payoutsList: List<PayoutItem> = emptyList(),
    val canSpinToday: Boolean = true,
    val wheelCooldownEnd: Long = 0L,
    val lastDailyClaimAt: Long = 0L,
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
            editor.putString("${userId}_referral_balance", data.referralBalanceUsdt.toCleanDouble().toString())
            editor.putString("${userId}_task_balance", data.taskBalanceUsdt.toCleanDouble().toString())
            editor.putLong("${userId}_total_referrals", data.totalReferrals)
            editor.putBoolean("${userId}_is_grid_active", data.isGridMiningActive)
            editor.putLong("${userId}_grid_end_time", data.miningSessionEndTimestamp)
            editor.putLong("${userId}_session_start", data.sessionStartTimeMillis)
            editor.putBoolean("${userId}_can_spin", data.canSpinToday)
            editor.putLong("${userId}_wheel_cooldown", data.wheelCooldownEnd)
            editor.putLong("${userId}_last_daily_claim", data.lastDailyClaimAt)
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

            val rawRefBal = prefs.getString("${userId}_referral_balance", null)
            val referralBalance = (rawRefBal?.toDoubleOrNull() ?: 0.0).toCleanDouble()

            val rawTaskBal = prefs.getString("${userId}_task_balance", null)
            val taskBalance = (rawTaskBal?.toDoubleOrNull() ?: 0.0).toCleanDouble()

            val totalReferrals = try { prefs.getLong("${userId}_total_referrals", 0L) } catch (_: Exception) { 0L }
            val isGridActive = try { prefs.getBoolean("${userId}_is_grid_active", false) } catch (_: Exception) { false }
            val gridEndTime = try { prefs.getLong("${userId}_grid_end_time", 0L) } catch (_: Exception) { 0L }
            val sessionStart = try { prefs.getLong("${userId}_session_start", System.currentTimeMillis()) } catch (_: Exception) { System.currentTimeMillis() }
            val canSpin = try { prefs.getBoolean("${userId}_can_spin", true) } catch (_: Exception) { true }
            val wheelCooldown = try { prefs.getLong("${userId}_wheel_cooldown", 0L) } catch (_: Exception) { 0L }
            val lastDailyClaim = try { prefs.getLong("${userId}_last_daily_claim", 0L) } catch (_: Exception) { 0L }
            val lastSaved = try { prefs.getLong("${userId}_last_saved", System.currentTimeMillis()) } catch (_: Exception) { System.currentTimeMillis() }

            val contracts = parseContracts(prefs.getString("${userId}_active_contracts", null))
            val activities = parseActivities(prefs.getString("${userId}_activity_list", null))
            val payouts = parsePayouts(prefs.getString("${userId}_payouts_list", null))

            if (usdtBalance > 0.0 || gridBalance > 0.0 || referralBalance > 0.0 || taskBalance > 0.0 || totalReferrals > 0L || contracts.isNotEmpty() || activities.isNotEmpty() || payouts.isNotEmpty()) {
                return PersistentUserData(
                    walletBalanceUsdt = usdtBalance,
                    gridCoinBalance = gridBalance,
                    referralBalanceUsdt = referralBalance,
                    taskBalanceUsdt = taskBalance,
                    totalReferrals = totalReferrals,
                    isGridMiningActive = isGridActive,
                    miningSessionEndTimestamp = gridEndTime,
                    sessionStartTimeMillis = sessionStart,
                    activeContracts = contracts,
                    activityList = activities,
                    payoutsList = payouts,
                    canSpinToday = canSpin,
                    wheelCooldownEnd = wheelCooldown,
                    lastDailyClaimAt = lastDailyClaim,
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
                        referralBalanceUsdt = 0.0,
                        taskBalanceUsdt = 0.0,
                        totalReferrals = 0L,
                        isGridMiningActive = roomEntity.isGridMiningActive,
                        miningSessionEndTimestamp = roomEntity.miningSessionEndTimestamp,
                        sessionStartTimeMillis = roomEntity.sessionStartTimeMillis,
                        activeContracts = roomContracts,
                        activityList = roomActivities,
                        payoutsList = roomPayouts,
                        canSpinToday = roomEntity.canSpinToday,
                        wheelCooldownEnd = roomEntity.wheelCooldownEnd,
                        lastDailyClaimAt = 0L,
                        lastSavedTimestamp = roomEntity.lastSavedTimestamp
                    )
                    return restored
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return PersistentUserData()
    }

    private fun parseContracts(jsonStr: String?): List<ActiveContract> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<ActiveContract>()
        try {
            val array = JSONArray(jsonStr)
            val now = System.currentTimeMillis()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val deposit = obj.optDouble("depositUsdt", 10.0).toCleanDouble()
                val target30 = deposit * 0.30
                val accrued = obj.optDouble("accruedProfitUsdt", 0.0).toCleanDouble()
                val progress = safeComputeTaskProgress(accrued, deposit)
                val status = if (accrued >= target30 && target30 > 0) "COMPLETED" else "IN_PROGRESS"

                list.add(
                    ActiveContract(
                        id = obj.optString("id", "c_$i"),
                        planName = obj.optString("planName", "Mining Rig"),
                        cryptoSymbol = obj.optString("cryptoSymbol", "BTC"),
                        depositUsdt = deposit,
                        hashPowerGh = obj.optDouble("hashPowerGh", 10000.0).toCleanDouble(),
                        elapsedDays = obj.optInt("elapsedDays", 0),
                        totalDays = obj.optInt("totalDays", 30),
                        accruedProfitUsdt = accrued,
                        dailyYieldUsdt = obj.optDouble("dailyYieldUsdt", 0.5).toCleanDouble(),
                        isRestakeEnabled = obj.optBoolean("isRestakeEnabled", false),
                        startDateStr = obj.optString("startDateStr", "Active"),
                        maturityDateStr = obj.optString("maturityDateStr", "30 Days"),
                        startTimestampMs = obj.optLong("startTimestampMs", now),
                        endTimestampMs = obj.optLong("endTimestampMs", now + (30L * 24 * 3600 * 1000)),
                        plan_cost = deposit,
                        target_yield_30_percent = target30,
                        current_yield_mined = accrued,
                        task_progress_pct = progress,
                        work_status = status,
                        unlocked_for_withdrawal = (status == "COMPLETED")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseActivities(jsonStr: String?): List<ActivityItem> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<ActivityItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ActivityItem(
                        id = obj.optString("id", "act_$i"),
                        title = obj.optString("title", "Transaction"),
                        subtitle = obj.optString("subtitle", ""),
                        btcAmountStr = obj.optString("btcAmountStr", ""),
                        usdtAmount = obj.optDouble("usdtAmount", 0.0).toCleanDouble(),
                        timestampStr = obj.optString("timestampStr", "Recently"),
                        isCredit = obj.optBoolean("isCredit", true)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parsePayouts(jsonStr: String?): List<PayoutItem> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<PayoutItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val statusStr = obj.optString("status", "PENDING_24H_AUDIT")
                val status = try { PayoutStatus.valueOf(statusStr) } catch (_: Exception) { PayoutStatus.PENDING_24H_AUDIT }
                list.add(
                    PayoutItem(
                        id = obj.optString("id", "payout_$i"),
                        dateStr = obj.optString("dateStr", "Recently"),
                        amountUsdt = obj.optDouble("amountUsdt", 0.0).toCleanDouble(),
                        targetAddress = obj.optString("targetAddress", "USDT Wallet"),
                        network = obj.optString("network", "TRC20"),
                        status = status
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
