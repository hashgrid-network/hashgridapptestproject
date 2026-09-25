package com.example

import android.app.Application
import com.example.service.AuthService
import com.example.service.FirebaseAppCheckManager
import com.google.firebase.FirebaseApp

class HashGridApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
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
