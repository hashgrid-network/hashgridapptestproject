package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DeepLinkManager {
    private const val PREFS_NAME = "hashgrid_deeplink_prefs"
    private const val KEY_PENDING_REF = "pending_referral_code"

    private val _pendingReferralCode = MutableStateFlow<String?>(null)
    val pendingReferralCode: StateFlow<String?> = _pendingReferralCode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_PENDING_REF, null)
        if (!saved.isNullOrBlank()) {
            _pendingReferralCode.value = saved.trim().uppercase()
        }
    }

    fun handleIntent(context: Context, intent: Intent?) {
        val data: Uri = intent?.data ?: return
        var refParam = data.getQueryParameter("ref")
        if (refParam.isNullOrBlank()) {
            refParam = data.getQueryParameter("referral")
        }
        if (refParam.isNullOrBlank() && data.scheme == "hashgrid") {
            refParam = data.host ?: data.lastPathSegment
        }

        if (!refParam.isNullOrBlank()) {
            val cleanRef = refParam.trim().uppercase().replace("REF=", "")
            _pendingReferralCode.value = cleanRef
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PENDING_REF, cleanRef).apply()
        }
    }

    fun setPendingReferralCode(context: Context, code: String) {
        val cleanRef = code.trim().uppercase()
        _pendingReferralCode.value = cleanRef.ifBlank { null }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (cleanRef.isNotBlank()) {
            prefs.edit().putString(KEY_PENDING_REF, cleanRef).apply()
        } else {
            prefs.edit().remove(KEY_PENDING_REF).apply()
        }
    }

    fun clearPendingReferralCode(context: Context) {
        _pendingReferralCode.value = null
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PENDING_REF).apply()
    }
}
