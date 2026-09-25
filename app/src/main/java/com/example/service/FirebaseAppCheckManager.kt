package com.example.service

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Manages Firebase App Check enforcement using Google Play Integrity in production
 * and DebugAppCheckProviderFactory during development/testing.
 * Protects Firestore and Authentication from unauthorized abuse or counterfeit builds.
 */
object FirebaseAppCheckManager {
    private const val TAG = "HashGridAppCheck"

    fun initialize(context: Context) {
        try {
            val app = FirebaseApp.getInstance()
            val firebaseAppCheck = FirebaseAppCheck.getInstance(app)

            if (BuildConfig.DEBUG) {
                // In Debug mode (development / emulator builds), install Debug Provider
                Log.d(TAG, "Initializing Firebase AppCheck with DebugAppCheckProviderFactory")
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                // In Production mode, enforce Google Play Integrity API
                Log.d(TAG, "Enforcing Firebase AppCheck with PlayIntegrityAppCheckProviderFactory")
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }

            // Enable automatic token refresh for seamless requests
            firebaseAppCheck.setTokenAutoRefreshEnabled(true)
            Log.d(TAG, "Firebase AppCheck successfully configured and token auto-refresh enabled.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase AppCheck: ${e.localizedMessage}", e)
        }
    }
}
