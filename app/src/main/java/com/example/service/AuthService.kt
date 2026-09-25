package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.example.model.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
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
    private const val KEY_PHOTO_URL = "photo_url"
    private const val KEY_IS_FLAGGED_DUPLICATE = "is_flagged_duplicate"
    private const val KEY_SAVED_USERS_JSON = "saved_users_json"

    private var prefs: SharedPreferences? = null

    val firebaseAuth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun init(context: Context) {
        try {
            try {
                FirebaseApp.initializeApp(context.applicationContext)
            } catch (_: Exception) {}

            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentPrefs = prefs

            val loggedIn = currentPrefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
            val savedUid = currentPrefs?.getString(KEY_USER_ID, null)
            val savedEmail = currentPrefs?.getString(KEY_USER_EMAIL, null)

            val fbAuth = firebaseAuth
            val firebaseUser = try {
                fbAuth?.currentUser
            } catch (e: Exception) {
                null
            }

            if (loggedIn && firebaseUser != null && !firebaseUser.uid.isNullOrBlank()) {
                val uid = firebaseUser.uid
                val email = firebaseUser.email ?: currentPrefs?.getString(KEY_USER_EMAIL, "") ?: ""
                val name = firebaseUser.displayName ?: currentPrefs?.getString(KEY_USER_NAME, "Miner") ?: "Miner"
                val photoUrl = firebaseUser.photoUrl?.toString() ?: currentPrefs?.getString(KEY_PHOTO_URL, null)
                val refCode = currentPrefs?.getString(KEY_REFERRAL_CODE, "HG-" + uid.takeLast(4).uppercase()) ?: "HG-7798"
                val accountId = "HG-" + uid.takeLast(6).uppercase()

                val user = User(
                    id = accountId,
                    email = email,
                    role = "user",
                    referralCode = refCode,
                    displayName = name,
                    photoUrl = photoUrl,
                    isFlaggedDuplicate = currentPrefs?.getBoolean(KEY_IS_FLAGGED_DUPLICATE, false) ?: false
                )

                _currentUser.value = user
                _isLoggedIn.value = true
            } else if (loggedIn && !savedUid.isNullOrBlank() && !savedEmail.isNullOrBlank()) {
                val user = User(
                    id = savedUid,
                    email = savedEmail,
                    role = currentPrefs.getString(KEY_USER_ROLE, "user") ?: "user",
                    referralCode = currentPrefs.getString(KEY_REFERRAL_CODE, "HG-7798") ?: "HG-7798",
                    displayName = currentPrefs.getString(KEY_USER_NAME, "Institutional Miner") ?: "Institutional Miner",
                    photoUrl = currentPrefs.getString(KEY_PHOTO_URL, null),
                    isFlaggedDuplicate = currentPrefs.getBoolean(KEY_IS_FLAGGED_DUPLICATE, false)
                )
                _currentUser.value = user
                _isLoggedIn.value = true
            } else {
                _currentUser.value = null
                _isLoggedIn.value = false
            }
        } catch (e: Exception) {
            _currentUser.value = null
            _isLoggedIn.value = false
        }
    }

    /**
     * Obtains a SHA-256 hashed Android Device ID to prevent multi-account referral fraud
     */
    fun getHashedDeviceId(context: Context): String {
        return try {
            val rawId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawId.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }.take(16)
        } catch (_: Exception) {
            "dev_${UUID.randomUUID().toString().take(12)}"
        }
    }

    /**
     * Step 1 Login with Email + Password.
     * Evaluates 2FA state:
     * - If user has TOTP enabled with secret -> returns RequireTotpChallenge
     * - If user does not have TOTP setup -> returns RequireTotpSetup
     */
    suspend fun loginWithEmail(context: Context, email: String, pass: String): AuthStepResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank()) {
            return@withContext AuthStepResult.Failure("Please enter your email address.")
        }
        if (cleanPass.length < 6) {
            return@withContext AuthStepResult.Failure("Password must be at least 6 characters.")
        }

        val deviceIdHash = getHashedDeviceId(context)

        try {
            val auth = firebaseAuth
            if (auth != null) {
                val authResult = auth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
                val fbUser = authResult.user

                if (fbUser != null) {
                    val uid = fbUser.uid
                    val displayName = fbUser.displayName ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

                    // Check user 2FA status in Firebase
                    val (isTotpEnabled, totpSecret) = FirebaseSyncService.fetchTotpDetails(uid)
                    return@withContext if (isTotpEnabled && !totpSecret.isNullOrBlank()) {
                        AuthStepResult.RequireTotpChallenge(
                            uid = uid,
                            email = cleanEmail,
                            displayName = displayName,
                            totpSecret = totpSecret
                        )
                    } else {
                        val generatedSecret = TotpHelper.generateSecret(16)
                        AuthStepResult.RequireTotpSetup(
                            uid = uid,
                            email = cleanEmail,
                            displayName = displayName,
                            totpSecret = generatedSecret
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Check local credentials registry below
        }

        // Fallback for local credentials registry
        try {
            val usersJson = prefs?.getString(KEY_SAVED_USERS_JSON, "{}") ?: "{}"
            val usersObj = JSONObject(usersJson)

            if (usersObj.has(cleanEmail)) {
                val userRecord = usersObj.getJSONObject(cleanEmail)
                val storedPassword = userRecord.optString("password", "")
                if (storedPassword == cleanPass) {
                    val uid = userRecord.optString("uid", UUID.randomUUID().toString())
                    val displayName = userRecord.optString("name", cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() })
                    val storedTotpSecret = userRecord.optString("totpSecret", "")
                    val isTotpEnabled = userRecord.optBoolean("totpEnabled", false)

                    return@withContext if (isTotpEnabled && storedTotpSecret.isNotBlank()) {
                        AuthStepResult.RequireTotpChallenge(
                            uid = uid,
                            email = cleanEmail,
                            displayName = displayName,
                            totpSecret = storedTotpSecret
                        )
                    } else {
                        val generatedSecret = TotpHelper.generateSecret(16)
                        AuthStepResult.RequireTotpSetup(
                            uid = uid,
                            email = cleanEmail,
                            displayName = displayName,
                            totpSecret = generatedSecret
                        )
                    }
                } else {
                    return@withContext AuthStepResult.Failure("Incorrect password. Please try again.")
                }
            }
        } catch (_: Exception) {}

        AuthStepResult.Failure("Login failed. Please verify your email and password.")
    }

    /**
     * Step 1 Sign-Up with Email + Password.
     * Creates account and returns RequireTotpSetup with generated Base32 secret.
     */
    suspend fun signUpWithEmail(
        context: Context,
        name: String,
        email: String,
        password: String,
        confirmPass: String,
        referralCode: String
    ): AuthStepResult = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPass = password.trim()
        val cleanConfirm = confirmPass.trim()
        val cleanRef = referralCode.trim()

        if (cleanName.isBlank()) {
            return@withContext AuthStepResult.Failure("Please enter your name or username.")
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return@withContext AuthStepResult.Failure("Please enter a valid email address.")
        }
        if (cleanPass.length < 6) {
            return@withContext AuthStepResult.Failure("Password must be at least 6 characters.")
        }
        if (cleanPass != cleanConfirm) {
            return@withContext AuthStepResult.Failure("Passwords do not match.")
        }

        val deviceIdHash = getHashedDeviceId(context)
        val generatedSecret = TotpHelper.generateSecret(16)

        try {
            val auth = firebaseAuth
            if (auth != null) {
                val authResult = auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
                val fbUser = authResult.user

                if (fbUser != null) {
                    val uid = fbUser.uid
                    val accountId = "HG-" + uid.takeLast(6).uppercase()
                    val assignedRefCode = if (cleanRef.isNotBlank()) cleanRef else "HG-" + uid.takeLast(4).uppercase()

                    FirebaseSyncService.initializeNewUser(uid, cleanEmail, cleanName, null, accountId)
                    saveUserToRegistry(cleanEmail, cleanPass, accountId, cleanName, assignedRefCode, deviceIdHash, false, uid, generatedSecret)

                    return@withContext AuthStepResult.RequireTotpSetup(
                        uid = uid,
                        email = cleanEmail,
                        displayName = cleanName,
                        totpSecret = generatedSecret
                    )
                }
            }
        } catch (e: Exception) {
            // If Firebase fails or user exists, check local fallback
        }

        try {
            val usersJson = prefs?.getString(KEY_SAVED_USERS_JSON, "{}") ?: "{}"
            val usersObj = JSONObject(usersJson)
            if (usersObj.has(cleanEmail)) {
                return@withContext AuthStepResult.Failure("An account with this email already exists.")
            }

            val uid = UUID.randomUUID().toString()
            val accountId = "HG-" + uid.takeLast(6).uppercase()
            val assignedRefCode = if (cleanRef.isNotBlank()) cleanRef else "HG-" + uid.takeLast(4).uppercase()

            FirebaseSyncService.initializeNewUser(uid, cleanEmail, cleanName, null, accountId)
            saveUserToRegistry(cleanEmail, cleanPass, accountId, cleanName, assignedRefCode, deviceIdHash, false, uid, generatedSecret)

            AuthStepResult.RequireTotpSetup(
                uid = uid,
                email = cleanEmail,
                displayName = cleanName,
                totpSecret = generatedSecret
            )
        } catch (e: Exception) {
            AuthStepResult.Failure(e.localizedMessage ?: "Sign-up failed.")
        }
    }

    /**
     * Native Google Sign-In (Auto-authenticated with Google 2FA)
     */
    suspend fun signInWithGoogleCredential(
        context: Context,
        serverClientId: String = "67298041154-mock-client-id.apps.googleusercontent.com"
    ): Result<User> = withContext(Dispatchers.IO) {
        val deviceIdHash = getHashedDeviceId(context)

        try {
            val credentialManager = try {
                CredentialManager.create(context)
            } catch (e: Exception) {
                return@withContext Result.failure(IllegalArgumentException("Credential Manager is unavailable on this device."))
            }

            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = try {
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .setNonce(hashedNonce)
                    .build()
            } catch (e: Exception) {
                return@withContext Result.failure(IllegalArgumentException("Failed to configure Google Sign-In: ${e.localizedMessage}"))
            }

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            val googleIdTokenCredential = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (e: Exception) {
                return@withContext Result.failure(IllegalArgumentException("Could not extract Google credentials: ${e.localizedMessage}"))
            }

            val idToken = googleIdTokenCredential.idToken
            val googleEmail = googleIdTokenCredential.id
            val googleName = googleIdTokenCredential.displayName ?: googleEmail.substringBefore("@")
            val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

            var uid = UUID.randomUUID().toString()
            try {
                val auth = firebaseAuth
                if (auth != null && !idToken.isNullOrBlank()) {
                    val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                    val authRes = auth.signInWithCredential(firebaseCred).await()
                    if (authRes.user != null) {
                        uid = authRes.user!!.uid
                    }
                }
            } catch (_: Exception) {}

            val accountId = "HG-" + uid.takeLast(6).uppercase()
            val refCode = "HG-" + uid.takeLast(4).uppercase()

            val existingRemote = FirebaseSyncService.fetchUserData(uid)
            if (existingRemote == null) {
                FirebaseSyncService.initializeNewUser(uid, googleEmail, googleName, photoUrl, accountId)
            }

            val matchedUser = User(
                id = accountId,
                email = googleEmail,
                role = "user",
                referralCode = refCode,
                displayName = googleName,
                photoUrl = photoUrl,
                isFlaggedDuplicate = false
            )

            saveUserToRegistry(googleEmail, "oauth_google", accountId, googleName, refCode, deviceIdHash, false, uid, null)
            setSessionDirect(matchedUser, uid)
            Result.success(matchedUser)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(IllegalArgumentException("Google sign-in was cancelled."))
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException(e.localizedMessage ?: "Google sign-in encountered an error. Please try again."))
        }
    }

    private fun saveUserToRegistry(
        email: String,
        pass: String,
        accountId: String,
        name: String,
        refCode: String,
        deviceId: String,
        isDuplicate: Boolean,
        uid: String,
        totpSecret: String?
    ) {
        try {
            val usersJson = prefs?.getString(KEY_SAVED_USERS_JSON, "{}") ?: "{}"
            val usersObj = JSONObject(usersJson)
            val userObj = JSONObject().apply {
                put("uid", uid)
                put("id", accountId)
                put("password", pass)
                put("name", name)
                put("role", "user")
                put("referralCode", refCode)
                put("deviceId", deviceId)
                put("isFlaggedDuplicate", isDuplicate)
                if (!totpSecret.isNullOrBlank()) {
                    put("totpSecret", totpSecret)
                    put("totpEnabled", true)
                }
                put("createdAt", FirebaseSyncService.getCurrentTimestamp())
            }
            usersObj.put(email, userObj)
            prefs?.edit()?.putString(KEY_SAVED_USERS_JSON, usersObj.toString())?.apply()

            FirebaseSyncService.registerDevice(deviceId, accountId, email, isDuplicate)
        } catch (_: Exception) {}
    }

    fun setSessionDirect(user: User, uid: String) {
        try {
            prefs?.edit()
                ?.putBoolean(KEY_IS_LOGGED_IN, true)
                ?.putString(KEY_USER_ID, user.id)
                ?.putString(KEY_USER_NAME, user.displayName)
                ?.putString(KEY_USER_EMAIL, user.email)
                ?.putString(KEY_USER_ROLE, user.role)
                ?.putString(KEY_REFERRAL_CODE, user.referralCode)
                ?.putString(KEY_PHOTO_URL, user.photoUrl)
                ?.putBoolean(KEY_IS_FLAGGED_DUPLICATE, user.isFlaggedDuplicate)
                ?.apply()
        } catch (_: Exception) {}

        _currentUser.value = user
        _isLoggedIn.value = true

        FirebaseSyncService.syncUserProfile(
            uid = uid,
            email = user.email,
            displayName = user.displayName,
            referralCode = user.referralCode,
            isFlaggedDuplicate = user.isFlaggedDuplicate,
            photoUrl = user.photoUrl
        )
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}

        try {
            prefs?.edit()
                ?.putBoolean(KEY_IS_LOGGED_IN, false)
                ?.remove(KEY_USER_ID)
                ?.remove(KEY_USER_NAME)
                ?.remove(KEY_USER_EMAIL)
                ?.remove(KEY_USER_ROLE)
                ?.remove(KEY_REFERRAL_CODE)
                ?.remove(KEY_PHOTO_URL)
                ?.remove(KEY_IS_FLAGGED_DUPLICATE)
                ?.apply()
        } catch (_: Exception) {}

        _currentUser.value = null
        _isLoggedIn.value = false
    }
}
