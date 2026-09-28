package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.service.AuthService
import com.example.service.FirebaseAppCheckManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class HashGridApplication : Application() {

    companion object {
        var lastFirebaseInitError: String? = null

        fun ensureFirebaseInitialized(context: Context): Boolean {
            return try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:885427334784:android:3b18fbfb14a82cb36e0065")
                        .setProjectId("hashgrid-c7fe4")
                        .setApiKey("AIzaSyCMDAfHJ6awiJYRDoJ1PR-UMC7yF8_kauc")
                        .setGcmSenderId("885427334784")
                        .setStorageBucket("hashgrid-c7fe4.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    try {
                        val firestore = FirebaseFirestore.getInstance()
                        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                            .setPersistenceEnabled(true)
                            .build()
                        firestore.firestoreSettings = settings
                    } catch (_: Exception) {}
                }
                lastFirebaseInitError = null
                true
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: e.message ?: "Failed to initialize Firebase"
                lastFirebaseInitError = errorMsg
                Log.e("HashGridApp", "Firebase programmatic init error: $errorMsg", e)
                false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureFirebaseInitialized(this)
        try {
            FirebaseAppCheckManager.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            AuthService.init(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

