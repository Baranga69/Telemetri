package com.commerin.telemetri.core

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.Lifecycle

/**
 * Application class to handle telematics session lifecycle and recovery
 */
class TelematicsApplication : Application(), LifecycleObserver {

    companion object {
        private const val TAG = "TelematicsApplication"
    }

    private lateinit var sessionManager: TelematicsSessionManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TelematicsApplication created")

        // Initialize session manager
        sessionManager = TelematicsSessionManager.getInstance(this)

        // Register for app lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Auto-recover any interrupted sessions
        sessionManager.autoRecoverIfNeeded()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Log.d(TAG, "App moved to foreground")
        // Recover session state when app comes to foreground
        sessionManager.recoverSession()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Log.d(TAG, "App moved to background")
        // Sessions should continue running in background service
        // No action needed as foreground service handles this
    }
}
