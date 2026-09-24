package com.example.service

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiSupportService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT = """
You are HashGrid's Institutional AI Concierge. HashGrid is an institutional cloud mining platform powered by 45 MW Geothermal PPA with Landsvirkjun in Iceland at $0.034/kWh locked rate. The facility uses Tier-III colocation with sub-zero liquid hydro cooling (PUE 1.05) and over 8,200 Antminer S21 Hydro units.
Contracts are 30-day fixed term, settled daily in USDT with 0 maintenance fee.
Minimum withdrawal is $130 USDT with a 24-hour security review window for cold-storage segregation (maintaining 50%+ institutional cold reserve).
Give professional, concise, authoritative answers formatted cleanly with bullet points if helpful.
"""

    suspend fun askGemini(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", "$SYSTEM_PROMPT\n\nUser Question: $prompt"))
                            })
                        })
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody(mediaType))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: ""
                    val root = JSONObject(respStr)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val reply = parts.getJSONObject(0).optString("text")
                            if (reply.isNotBlank()) return@withContext reply
                        }
                    }
                }
            } catch (e: Exception) {
                // Fall back to institutional expert knowledge base
            }
        }

        // Institutional Offline Knowledge Base response
        return@withContext getLocalInstitutionalAnswer(prompt)
    }

    private fun getLocalInstitutionalAnswer(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("withdrawal") || q.contains("withdraw") || q.contains("130") || q.contains("limit") -> {
                "**HashGrid Audited Withdrawal Policy**:\n\n" +
                "• **Minimum Withdrawal**: $130.00 USDT.\n" +
                "• **Review Window**: 24-Hour Audited Window to verify cold-storage multi-sig signatures.\n" +
                "• **Networks Supported**: TRC20 (Tron) and BEP20 (BNB Smart Chain).\n" +
                "• **Network Fee**: Flat $1.50 USDT subsidy applied automatically."
            }
            q.contains("arctic") || q.contains("geothermal") || q.contains("iceland") || q.contains("power") || q.contains("energy") -> {
                "**Arctic Geothermal Infrastructure**:\n\n" +
                "• **PPA Contract**: 45 MW locked with Landsvirkjun (Iceland National Power Company) at $0.034/kWh.\n" +
                "• **Location**: Krafla Geothermal Field, North-East Iceland.\n" +
                "• **Efficiency**: PUE 1.05 Sub-Zero Closed-Loop Liquid Cooling system.\n" +
                "• **Fleet**: 8,200+ Antminer S21 Hydro rigs delivering clean zero-emission hash power."
            }
            q.contains("plan") || q.contains("yield") || q.contains("contract") || q.contains("term") || q.contains("30") -> {
                "**Mining Contract Architecture**:\n\n" +
                "• **Contract Duration**: 30-Day Fixed Term.\n" +
                "• **Settlement**: Daily automatic USDT settlement credited directly to your wallet.\n" +
                "• **Maintenance Fee**: 0% hardware maintenance or hosting charge.\n" +
                "• **Maturity Options**: Transfer principal to wallet or Re-Stake for +2.0% compounding bonus."
            }
            q.contains("spin") || q.contains("wheel") || q.contains("lucky") || q.contains("bonus") -> {
                "**Daily Lucky Wheel Rewards**:\n\n" +
                "• Registered users receive **1 Free Spin every 24 hours**.\n" +
                "• Prizes include 0.5 USDT, 1.0 USDT Cash Vouchers, 100 Gh/s boosters, and 250 Gh/s (24h) power boosts.\n" +
                "• All winnings credit instantly to your institutional account."
            }
            q.contains("syndicate") || q.contains("referral") || q.contains("affiliate") || q.contains("partner") -> {
                "**Growth & Syndicate Program**:\n\n" +
                "• **Direct Referral**: Instant 7.0% USDT commission on all direct 30-day activations.\n" +
                "• **Founding Syndicate**: Limited to 10 partner seats globally, earning 1.0% of gross monthly platform turnover.\n" +
                "• **Daily Bounties**: Complete Telegram and WhatsApp community actions for $1 Hash Vouchers daily."
            }
            else -> {
                "**HashGrid Institutional Support**:\n\n" +
                "HashGrid combines institutional-grade Bitcoin and Altcoin cloud mining with 100% renewable Arctic geothermal power. Our operations are fully audited with real-time hash allocation and daily automated USDT settlements.\n\n" +
                "How may our concierge assist your portfolio today? You can ask about our 45 MW Geothermal PPA, 30-Day Plans, $130 Audited Withdrawals, or the Founding Syndicate."
            }
        }
    }
}
