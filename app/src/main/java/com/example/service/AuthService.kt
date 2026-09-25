package com.example.service

import android.content.Context
import android.content.SharedPreferences
import com.example.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID

object AuthService {

    private const val PREFS_NAME = "hashgrid_auth_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_REFERRAL_CODE = "referral_code"
    private const val KEY_SAVED_USERS_JSON = "saved_users_json"

    private lateinit var prefs: SharedPreferences

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (loggedIn) {
            val user = User(
                id = prefs.getString(KEY_USER_ID, "#HG-142597") ?: "#HG-142597",
                email = prefs.getString(KEY_USER_EMAIL, "goldbrownp@gmail.com") ?: "goldbrownp@gmail.com",
                role = prefs.getString(KEY_USER_ROLE, "user") ?: "user",
                referralCode = prefs.getString(KEY_REFERRAL_CODE, "HG-7798") ?: "HG-7798",
                displayName = prefs.getString(KEY_USER_NAME, "Institutional Miner") ?: "Institutional Miner"
            )
            _currentUser.value = user
            _isLoggedIn.value = true
        } else {
            _currentUser.value = null
            _isLoggedIn.value = false
        }
    }

    fun login(email: String, password: String): Result<User> {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = password.trim()

        if (cleanEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your email address."))
        }
        if (cleanPass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        // Lookup in local accounts registry
        val usersJson = prefs.getString(KEY_SAVED_USERS_JSON, "{}") ?: "{}"
        val usersObj = JSONObject(usersJson)

        var matchedUser: User? = null

        if (usersObj.has(cleanEmail)) {
            val userRecord = usersObj.getJSONObject(cleanEmail)
            val storedPassword = userRecord.optString("password", "")
            if (storedPassword == cleanPass) {
                matchedUser = User(
                    id = userRecord.optString("id", "#HG-${(100000..999999).random()}"),
                    email = cleanEmail,
                    role = userRecord.optString("role", "user"),
                    referralCode = userRecord.optString("referralCode", "HG-${(1000..9999).random()}"),
                    displayName = userRecord.optString("name", cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() })
                )
            } else {
                return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
            }
        } else {
            // Default demo account or auto-create demo session
            val randomId = "#HG-${(100000..999999).random()}"
            val refCode = "HG-${(1000..9999).random()}"
            val defaultName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            matchedUser = User(
                id = randomId,
                email = cleanEmail,
                role = "user",
                referralCode = refCode,
                displayName = defaultName
            )
            // Persist for future logins
            saveUserToRegistry(cleanEmail, cleanPass, randomId, defaultName, refCode)
        }

        persistSession(matchedUser)
        return Result.success(matchedUser)
    }

    fun signUp(
        name: String,
        email: String,
        password: String,
        confirmPass: String,
        referralCode: String
    ): Result<User> {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPass = password.trim()
        val cleanConfirm = confirmPass.trim()
        val cleanRef = referralCode.trim()

        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your name or username."))
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (cleanPass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (cleanPass != cleanConfirm) {
            return Result.failure(IllegalArgumentException("Passwords do not match."))
        }

        val usersJson = prefs.getString(KEY_SAVED_USERS_JSON, "{}") ?: "{}"
        val usersObj = JSONObject(usersJson)
        if (usersObj.has(cleanEmail)) {
            return Result.failure(IllegalArgumentException("An account with this email already exists. Please log in."))
        }

        val randomId = "#HG-${(100000..999999).random()}"
        val assignedRefCode = if (cleanRef.isNotBlank()) cleanRef else "HG-${(1000..9999).random()}"

        val newUser = User(
            id = randomId,
            email = cleanEmail,
            role = "user",
            referralCode = assignedRefCode,
            displayName = cleanName
        )

        saveUserToRegistry(cleanEmail, cleanPass, randomId, cleanName, assignedRefCode)
        persistSession(newUser)
        return Result.success(newUser)
    }

    private fun saveUserToRegistry(email: String, pass: String, id: String, name: String, refCode: String) {
        val usersJson = prefs.getString(KEY_SAVED_USERS_JSON, "{}") ?: "{}"
        val usersObj = JSONObject(usersJson)
        val userObj = JSONObject().apply {
            put("id", id)
            put("password", pass)
            put("name", name)
            put("role", "user")
            put("referralCode", refCode)
            put("createdAt", FirebaseSyncService.getCurrentTimestamp())
        }
        usersObj.put(email, userObj)
        prefs.edit().putString(KEY_SAVED_USERS_JSON, usersObj.toString()).apply()
    }

    private fun persistSession(user: User) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.displayName)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_ROLE, user.role)
            .putString(KEY_REFERRAL_CODE, user.referralCode)
            .apply()

        _currentUser.value = user
        _isLoggedIn.value = true

        // Sync with remote Firebase database
        FirebaseSyncService.syncUser(
            FirebaseUser(
                uid = user.id,
                email = user.email,
                walletBalance = 84.20,
                miningRate = "520.87 TH/s",
                createdAt = FirebaseSyncService.getCurrentTimestamp()
            )
        )
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_ROLE)
            .remove(KEY_REFERRAL_CODE)
            .apply()

        _currentUser.value = null
        _isLoggedIn.value = false
    }
}
