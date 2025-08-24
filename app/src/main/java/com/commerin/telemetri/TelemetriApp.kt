package com.commerin.telemetri

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TelemetriApp : Application() {

    companion object {
        private const val TAG = "TelemetriApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Telemetri App initialized")
    }
}
