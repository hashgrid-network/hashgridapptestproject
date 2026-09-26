package com.example.service

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent trusted-device authentication sessions.
 * Remembers verified devices across app restarts so that authenticated miners
 * seamlessly enter the dashboard without repeatedly passing 2FA on every cold start.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "hashgrid_trusted_device_session"
        const val KEY_IS_DEVICE_AUTHENTICATED = "is_device_authenticated"
        const val KEY_AUTHENTICATED_USER_UID = "authenticated_user_uid"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Marks the current device as verified for the given user ID.
     * Saves 'is_device_authenticated' = true and 'authenticated_user_uid' = uid.
     */
    fun markDeviceAsVerified(uid: String) {
        prefs.edit()
            .putBoolean(KEY_IS_DEVICE_AUTHENTICATED, true)
            .putString(KEY_AUTHENTICATED_USER_UID, uid)
            .apply()
    }

    /**
     * Checks if the device is currently verified for the active user ID.
     * Returns true if stored uid matches currentUid AND 'is_device_authenticated' == true.
     */
    fun isDeviceVerified(currentUid: String?): Boolean {
        if (currentUid.isNullOrBlank()) return false
        val storedUid = prefs.getString(KEY_AUTHENTICATED_USER_UID, "") ?: ""
        val isAuthenticated = prefs.getBoolean(KEY_IS_DEVICE_AUTHENTICATED, false)
        return isAuthenticated && storedUid == currentUid
    }

    /**
     * Clears stored session flags and resets keys to default.
     */
    fun clearDeviceSession() {
        prefs.edit()
            .putBoolean(KEY_IS_DEVICE_AUTHENTICATED, false)
            .putString(KEY_AUTHENTICATED_USER_UID, "")
            .apply()
    }
}
