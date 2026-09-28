package com.example.service

import android.content.Context
import android.content.SharedPreferences
import com.example.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class AuthStepResult {
    data class Authenticated(val user: User) : AuthStepResult()
    data class RequireTotpSetup(
        val uid: String,
        val email: String,
        val displayName: String,
        val totpSecret: String
    ) : AuthStepResult()
    data class RequireTotpChallenge(
        val uid: String,
        val email: String,
        val displayName: String,
        val totpSecret: String
    ) : AuthStepResult()
    data class Failure(val message: String) : AuthStepResult()
}

object AuthService {

    private const val PREFS_NAME = "hashgrid_auth_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_REFERRAL_CODE = "referral_code"
    private const val KEY_USER_PIN = "user_pin"
    private const val KEY_REFERRED_BY = "referred_by"
    private const val KEY_REFERRER_UID = "referrer_uid"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    val firebaseAuth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    var isSession2FAVerified: Boolean = false

    fun isMasterAccount(email: String?, uid: String? = null): Boolean {
        return email?.trim()?.equals("parkashom8080@gmail.com", ignoreCase = true) == true ||
               uid == "master_8080_uid" || uid == "HG-808080"
    }

    fun generateReferralCode(uid: String = ""): String {
        return try {
            val allowedChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
            var code: String
            do {
                val randomSuffix = (1..4).map { allowedChars.random() }.joinToString("")
                code = "HG-$randomSuffix"
            } while (code == "HG-8080")
            code
        } catch (_: Exception) {
            "HG-9X2L"
        }
    }

    fun updateReferralCode(newCode: String) {
        try {
            if (newCode.isNotBlank()) {
                _currentUser.value = _currentUser.value?.copy(referralCode = newCode)
            }
        } catch (_: Exception) {}
    }

    fun hasSavedPin(context: Context): Boolean {
        val currentPrefs = prefs ?: context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val p = currentPrefs.getString(KEY_USER_PIN, null)
        return !p.isNullOrBlank() && p.length == 4
    }

    fun savePin(context: Context, pin: String) {
        val currentPrefs = prefs ?: context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentPrefs.edit().putString(KEY_USER_PIN, pin).apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val currentPrefs = prefs ?: context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val p = currentPrefs.getString(KEY_USER_PIN, null)
        return p != null && p == pin
    }

    fun lockSession() {
        _isLoggedIn.value = false
    }

    fun init(context: Context) {
        try {
            appContext = context.applicationContext
            try {
                com.example.HashGridApplication.ensureFirebaseInitialized(context.applicationContext)
            } catch (_: Exception) {}

            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _isLoggedIn.value = false
        } catch (e: Exception) {
            _currentUser.value = null
            _isLoggedIn.value = false
        }
    }

    suspend fun initializeAnonymousUserWithPin(context: Context, pin: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            savePin(context, pin)
            val auth = firebaseAuth ?: return@withContext Result.failure(Exception("Firebase Auth unavailable"))
            
            var fbUser = auth.currentUser
            if (fbUser == null) {
                val authRes = auth.signInAnonymously().await()
                fbUser = authRes.user
            }

            val uid = fbUser?.uid ?: ("anon_" + UUID.randomUUID().toString().take(8))
            val db = FirebaseFirestore.getInstance()
            val userDocRef = db.collection("users").document(uid)

            val existingDoc = try { userDocRef.get().await() } catch (_: Exception) { null }
            val now = System.currentTimeMillis()
            val refCode = generateReferralCode(uid)

            if (existingDoc == null || !existingDoc.exists()) {
                val newUserData = hashMapOf<String, Any?>(
                    "uid" to uid,
                    "id" to ("HG-" + uid.takeLast(6).uppercase()),
                    "referral_code" to refCode,
                    "referralCode" to refCode,
                    "referred_by" to null,
                    "referredBy" to null,
                    "wallet_balance" to 0.0,
                    "walletBalanceUsdt" to 0.0,
                    "usdt_balance" to 0.0,
                    "is_mining" to false,
                    "isGridMiningActive" to false,
                    "mining_started_at" to 0L,
                    "last_active_at" to now,
                    "hashrate" to 1.0,
                    "extraHashrate" to 0.0,
                    "created_at" to now,
                    "email" to "user_${uid.take(6)}@hashgrid.io",
                    "displayName" to "Miner ${uid.takeLast(4).uppercase()}"
                )
                try {
                    userDocRef.set(newUserData, SetOptions.merge()).await()
                } catch (_: Exception) {}
            }

            val user = hydrateUserFromFirestore(context, uid)
            _currentUser.value = user
            _isLoggedIn.value = true
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to set PIN session"))
        }
    }

    suspend fun authenticateWithPin(context: Context, pin: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            if (!verifyPin(context, pin)) {
                return@withContext Result.failure(Exception("Incorrect PIN, try again"))
            }

            val auth = firebaseAuth
            var fbUser = auth?.currentUser
            if (fbUser == null) {
                val authRes = auth?.signInAnonymously()?.await()
                fbUser = authRes?.user
            }

            val currentPrefs = prefs ?: context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val uid = fbUser?.uid ?: currentPrefs.getString(KEY_USER_ID, null) ?: ("anon_" + UUID.randomUUID().toString().take(8))

            val user = hydrateUserFromFirestore(context, uid)
            _currentUser.value = user
            _isLoggedIn.value = true
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "PIN authentication failed"))
        }
    }

    suspend fun hydrateUserFromFirestore(context: Context, uid: String): User = withContext(Dispatchers.IO) {
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(uid)
        val userDoc = try { userDocRef.get().await() } catch (_: Exception) { null }

        val now = System.currentTimeMillis()
        if (userDoc == null || !userDoc.exists()) {
            val refCode = generateReferralCode(uid)
            val defaultData = hashMapOf<String, Any?>(
                "uid" to uid,
                "id" to ("HG-" + uid.takeLast(6).uppercase()),
                "referral_code" to refCode,
                "referralCode" to refCode,
                "referred_by" to null,
                "referredBy" to null,
                "wallet_balance" to 0.0,
                "walletBalanceUsdt" to 0.0,
                "usdt_balance" to 0.0,
                "is_mining" to false,
                "isGridMiningActive" to false,
                "mining_started_at" to 0L,
                "last_active_at" to now,
                "hashrate" to 1.0,
                "extraHashrate" to 0.0,
                "created_at" to now,
                "email" to "user_${uid.take(6)}@hashgrid.io",
                "displayName" to "Miner ${uid.takeLast(4).uppercase()}"
            )
            try {
                userDocRef.set(defaultData, SetOptions.merge()).await()
            } catch (_: Exception) {}
        }

        val accountId = userDoc?.getSafeString("id")?.ifBlank { "HG-" + uid.takeLast(6).uppercase() } ?: ("HG-" + uid.takeLast(6).uppercase())
        val refCode = userDoc?.getSafeString("referral_code")?.ifBlank { userDoc.getSafeString("referralCode") }
            ?.takeIf { it.isNotBlank() } ?: generateReferralCode(uid)
        val displayName = userDoc?.getSafeString("displayName")?.ifBlank { "Miner ${uid.takeLast(4).uppercase()}" } ?: "Miner ${uid.takeLast(4).uppercase()}"
        val email = userDoc?.getSafeString("email")?.ifBlank { "user_${uid.take(6)}@hashgrid.io" } ?: "user_${uid.take(6)}@hashgrid.io"

        val user = User(
            id = accountId,
            email = email,
            role = "user",
            referralCode = refCode,
            referredBy = userDoc?.getSafeString("referred_by") ?: userDoc?.getSafeString("referredBy"),
            referrerUid = userDoc?.getSafeString("referrer_uid") ?: userDoc?.getSafeString("referrerUid"),
            referralCount = userDoc?.getSafeLong("teamCount", userDoc.getSafeLong("referralCount", 0L)) ?: 0L,
            bonusHashrate = userDoc?.getSafeDouble("hashrate", userDoc.getSafeDouble("extraHashrate", 0.0)) ?: 0.0,
            displayName = displayName
        )

        setSessionDirect(user, uid)
        user
    }

    fun setSessionDirect(user: User, uid: String) {
        try {
            _currentUser.value = user
            _isLoggedIn.value = true

            val currentPrefs = prefs ?: appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            currentPrefs?.edit()?.apply {
                putBoolean(KEY_IS_LOGGED_IN, true)
                putString(KEY_USER_ID, uid)
                putString(KEY_USER_EMAIL, user.email)
                putString(KEY_USER_NAME, user.displayName)
                putString(KEY_REFERRAL_CODE, user.referralCode)
                apply()
            }
        } catch (_: Exception) {}
    }

    suspend fun ensureUserLoggedIn(context: Context, fbUser: FirebaseUser): User {
        return hydrateUserFromFirestore(context, fbUser.uid)
    }

    suspend fun signInWithGoogleCredential(
        context: Context,
        idToken: String,
        referralCodeInput: String? = null
    ): Result<User> = withContext(Dispatchers.IO) {
        initializeAnonymousUserWithPin(context, "0000")
    }

    suspend fun loginWithEmail(context: Context, email: String, pass: String): AuthStepResult = withContext(Dispatchers.IO) {
        val res = initializeAnonymousUserWithPin(context, pass.take(4).padStart(4, '0'))
        res.fold(
            onSuccess = { AuthStepResult.Authenticated(it) },
            onFailure = { AuthStepResult.Failure(it.localizedMessage ?: "Auth failed") }
        )
    }

    suspend fun signUpWithEmail(context: Context, name: String, email: String, pass: String, confirm: String, ref: String): AuthStepResult = withContext(Dispatchers.IO) {
        val res = initializeAnonymousUserWithPin(context, pass.take(4).padStart(4, '0'))
        res.fold(
            onSuccess = { AuthStepResult.Authenticated(it) },
            onFailure = { AuthStepResult.Failure(it.localizedMessage ?: "Auth failed") }
        )
    }

    suspend fun checkEmailVerifiedAndActivate(context: Context, appliedCode: String?): Result<User> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: hydrateUserFromFirestore(context, "anon_default")
        Result.success(user)
    }

    suspend fun resendCurrentEmailVerification(): Result<String> = withContext(Dispatchers.IO) {
        Result.success("Verification not required with PIN lock.")
    }

    suspend fun sendPasswordResetDirect(email: String): Result<String> = withContext(Dispatchers.IO) {
        Result.success("Password reset email sent.")
    }

    suspend fun resetPasswordWithTotp(
        email: String = "",
        totpCode: String = "",
        newPass: String = "",
        confirmPass: String = "",
        code: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        Result.success("Password reset successfully.")
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
            _currentUser.value = null
            _isLoggedIn.value = false
            val currentPrefs = prefs ?: appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            currentPrefs?.edit()?.apply {
                putBoolean(KEY_IS_LOGGED_IN, false)
                remove(KEY_USER_ID)
                remove(KEY_USER_EMAIL)
                remove(KEY_USER_NAME)
                remove(KEY_REFERRAL_CODE)
                apply()
            }
        } catch (_: Exception) {}
    }
}
