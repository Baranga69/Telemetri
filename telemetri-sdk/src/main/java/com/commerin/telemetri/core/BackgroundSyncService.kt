package com.commerin.telemetri.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * Foreground service for background telemetry collection and data synchronization.
 * This service ensures telemetry collection continues even when the app is in the background.
 */
class BackgroundSyncService : Service() {
    companion object {
        private const val TAG = "BackgroundSyncService"
        private const val WORK_NAME = "TelemetrySyncWorker"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "telemetry_sync_channel"
        private const val CHANNEL_NAME = "Telemetry Background Sync"

        // Service actions
        const val ACTION_START_SYNC = "ACTION_START_SYNC"
        const val ACTION_STOP_SYNC = "ACTION_STOP_SYNC"

        fun startService(context: Context) {
            val intent = Intent(context, BackgroundSyncService::class.java).apply {
                action = ACTION_START_SYNC
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BackgroundSyncService::class.java).apply {
                action = ACTION_STOP_SYNC
            }
            context.startService(intent)
        }
    }

    private var telemetriManager: TelemetriManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BackgroundSyncService created")
        createNotificationChannel()
        telemetriManager = TelemetriManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SYNC -> {
                startForegroundService()
                startTelemetryCollection()
                schedulePeriodicSync()
            }
            ACTION_STOP_SYNC -> {
                stopTelemetryCollection()
                stopSelf()
            }
            else -> {
                startForegroundService()
                startTelemetryCollection()
            }
        }

        // Return START_STICKY to restart service if killed by system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This service doesn't support binding
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "BackgroundSyncService destroyed")
        stopTelemetryCollection()
        serviceScope.cancel()
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Started as foreground service")
    }

    private fun startTelemetryCollection() {
        if (isRunning) {
            Log.d(TAG, "Telemetry collection already running")
            return
        }

        isRunning = true
        serviceScope.launch {
            try {
                // Start telemetry collection with battery-optimized configuration
                val config = TelemetriManager.ConfigPresets.batterySaverUseCase()
                telemetriManager?.startTelemetryCollection(config)
                Log.d(TAG, "Background telemetry collection started")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting telemetry collection", e)
            }
        }
    }

    private fun stopTelemetryCollection() {
        if (!isRunning) return

        isRunning = false
        serviceScope.launch {
            try {
                telemetriManager?.stopTelemetryCollection()
                Log.d(TAG, "Background telemetry collection stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping telemetry collection", e)
            }
        }
    }

    private fun schedulePeriodicSync() {
        val syncRequest = PeriodicWorkRequestBuilder<TelemetrySyncWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        Log.d(TAG, "Periodic sync scheduled")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background telemetry data collection and synchronization"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Telemetry Service")
            .setContentText("Collecting telemetry data in background")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use your app icon here
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
