package com.commerin.telemetri.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.commerin.telemetri.domain.model.TelemetrySession
import com.commerin.telemetri.domain.model.SessionState
import kotlinx.coroutines.*
import java.util.*

/**
 * Enhanced foreground service for continuous telematics data collection.
 * This service ensures telematics sessions persist through app lifecycle changes,
 * screen locks, and background transitions.
 */
class TelematicsBackgroundService : LifecycleService() {

    companion object {
        private const val TAG = "TelematicsBackground"

        // Notification constants
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "telematics_service_channel"
        private const val CHANNEL_NAME = "Telematics Data Collection"

        // Service actions
        const val ACTION_START_SESSION = "ACTION_START_SESSION"
        const val ACTION_STOP_SESSION = "ACTION_STOP_SESSION"
        const val ACTION_PAUSE_SESSION = "ACTION_PAUSE_SESSION"
        const val ACTION_RESUME_SESSION = "ACTION_RESUME_SESSION"

        // Intent extras
        const val EXTRA_SESSION_CONFIG = "EXTRA_SESSION_CONFIG"
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"

        // Service state tracking
        private var isServiceRunning = false

        fun startTelematicsSession(context: Context, config: TelemetryConfig): String {
            val sessionId = UUID.randomUUID().toString()
            val intent = Intent(context, TelematicsBackgroundService::class.java).apply {
                action = ACTION_START_SESSION
                putExtra(EXTRA_SESSION_CONFIG, config as java.io.Serializable)
                putExtra(EXTRA_SESSION_ID, sessionId)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            return sessionId
        }

        fun stopTelematicsSession(context: Context, sessionId: String? = null) {
            val intent = Intent(context, TelematicsBackgroundService::class.java).apply {
                action = ACTION_STOP_SESSION
                sessionId?.let { putExtra(EXTRA_SESSION_ID, it) }
            }
            context.startService(intent)
        }

        fun pauseTelematicsSession(context: Context, sessionId: String) {
            val intent = Intent(context, TelematicsBackgroundService::class.java).apply {
                action = ACTION_PAUSE_SESSION
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            context.startService(intent)
        }

        fun resumeTelematicsSession(context: Context, sessionId: String) {
            val intent = Intent(context, TelematicsBackgroundService::class.java).apply {
                action = ACTION_RESUME_SESSION
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            context.startService(intent)
        }

        fun isRunning(): Boolean = isServiceRunning
    }

    // Service binder for local connections
    inner class TelematicsBinder : Binder() {
        fun getService(): TelematicsBackgroundService = this@TelematicsBackgroundService
    }

    private val binder = TelematicsBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Core components
    private lateinit var telemetriManager: TelemetriManager
    private lateinit var notificationManager: NotificationManager
    private var wakeLock: PowerManager.WakeLock? = null

    // Session management
    private var currentSession: TelemetrySession? = null
    private var sessionState = SessionState.STOPPED
    private var sessionStartTime: Long = 0

    // Configuration
    private var currentConfig: TelemetryConfig? = null

    // Observers for data collection status
    private val telemetryObserver = Observer<Any> { data ->
        updateNotificationWithData()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TelematicsBackgroundService created")

        // Initialize core components
        telemetriManager = TelemetriManager.getInstance(this)
        notificationManager = getSystemService(NotificationManager::class.java)

        // Create notification channel
        createNotificationChannel()

        // Acquire partial wake lock for critical location tracking
        acquireWakeLock()

        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.let { handleIntent(it) }

        // Return START_STICKY to ensure service restarts if killed by system
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TelematicsBackgroundService destroyed")

        // Stop current session if running
        stopCurrentSession()

        // Release wake lock
        releaseWakeLock()

        // Cancel all coroutines
        serviceScope.cancel()

        isServiceRunning = false
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_START_SESSION -> {
                val config = intent.getSerializableExtra(EXTRA_SESSION_CONFIG) as? TelemetryConfig
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)

                if (config != null && sessionId != null) {
                    startTelematicsSession(sessionId, config)
                }
            }

            ACTION_STOP_SESSION -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                stopTelematicsSession(sessionId)
            }

            ACTION_PAUSE_SESSION -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                pauseTelematicsSession(sessionId)
            }

            ACTION_RESUME_SESSION -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                resumeTelematicsSession(sessionId)
            }
        }
    }

    private fun startTelematicsSession(sessionId: String, config: TelemetryConfig) {
        if (sessionState == SessionState.RUNNING && currentSession?.sessionId == sessionId) {
            Log.d(TAG, "Session $sessionId already running")
            return
        }

        // Stop any existing session
        stopCurrentSession()

        Log.d(TAG, "Starting telematics session: $sessionId")

        // Create new session
        currentSession = TelemetrySession(
            sessionId = sessionId,
            startTime = System.currentTimeMillis(),
            config = config,
            state = SessionState.RUNNING
        )

        currentConfig = config
        sessionState = SessionState.RUNNING
        sessionStartTime = System.currentTimeMillis()

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createSessionNotification())

        // Start telemetry collection
        serviceScope.launch {
            try {
                telemetriManager.startTelemetryCollection(config)

                // Observe telemetry data for notification updates
                observeTelemetryData()

                Log.d(TAG, "Telematics session started successfully: $sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting telematics session: $sessionId", e)
                sessionState = SessionState.ERROR
                updateNotification()
            }
        }
    }

    private fun stopTelematicsSession(sessionId: String?) {
        if (sessionId != null && currentSession?.sessionId != sessionId) {
            Log.w(TAG, "Attempted to stop session $sessionId but current session is ${currentSession?.sessionId}")
            return
        }

        stopCurrentSession()
        stopSelf()
    }

    private fun pauseTelematicsSession(sessionId: String?) {
        if (sessionId != null && currentSession?.sessionId != sessionId) {
            Log.w(TAG, "Attempted to pause session $sessionId but current session is ${currentSession?.sessionId}")
            return
        }

        if (sessionState != SessionState.RUNNING) {
            Log.d(TAG, "Cannot pause session - not currently running")
            return
        }

        Log.d(TAG, "Pausing telematics session: $sessionId")

        sessionState = SessionState.PAUSED

        serviceScope.launch {
            try {
                telemetriManager.stopTelemetryCollection()
                updateNotification()
                Log.d(TAG, "Telematics session paused: $sessionId")
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing telematics session: $sessionId", e)
            }
        }
    }

    private fun resumeTelematicsSession(sessionId: String?) {
        if (sessionId != null && currentSession?.sessionId != sessionId) {
            Log.w(TAG, "Attempted to resume session $sessionId but current session is ${currentSession?.sessionId}")
            return
        }

        if (sessionState != SessionState.PAUSED) {
            Log.d(TAG, "Cannot resume session - not currently paused")
            return
        }

        Log.d(TAG, "Resuming telematics session: $sessionId")

        sessionState = SessionState.RUNNING

        serviceScope.launch {
            try {
                currentConfig?.let { config ->
                    telemetriManager.startTelemetryCollection(config)
                    updateNotification()
                    Log.d(TAG, "Telematics session resumed: $sessionId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming telematics session: $sessionId", e)
                sessionState = SessionState.ERROR
                updateNotification()
            }
        }
    }

    private fun stopCurrentSession() {
        if (sessionState == SessionState.STOPPED) return

        Log.d(TAG, "Stopping current telematics session: ${currentSession?.sessionId}")

        sessionState = SessionState.STOPPED

        serviceScope.launch {
            try {
                telemetriManager.stopTelemetryCollection()
                unobserveTelemetryData()

                currentSession = null
                currentConfig = null

                Log.d(TAG, "Telematics session stopped successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping telematics session", e)
            }
        }
    }

    private fun observeTelemetryData() {
        // Observe various telemetry data streams for status updates
        telemetriManager.locationData.observe(this, telemetryObserver)
        telemetriManager.sensorData.observe(this, telemetryObserver)
    }

    private fun unobserveTelemetryData() {
        telemetriManager.locationData.removeObserver(telemetryObserver)
        telemetriManager.sensorData.removeObserver(telemetryObserver)
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$TAG::TelematicsWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes timeout
            }
            Log.d(TAG, "Wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous telematics data collection for vehicle monitoring"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createSessionNotification(): Notification {
        val sessionDuration = if (sessionStartTime > 0) {
            (System.currentTimeMillis() - sessionStartTime) / 1000 / 60 // minutes
        } else 0

        val contentText = when (sessionState) {
            SessionState.RUNNING -> "Recording driving session • ${sessionDuration}m"
            SessionState.PAUSED -> "Session paused • ${sessionDuration}m total"
            SessionState.STOPPED -> "Session stopped"
            SessionState.ERROR -> "Session error - please restart"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Telematics Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(createStopAction())
            .build()
    }

    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent = Intent(this, TelematicsBackgroundService::class.java).apply {
            action = ACTION_STOP_SESSION
            currentSession?.sessionId?.let { putExtra(EXTRA_SESSION_ID, it) }
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_pause,
            "Stop",
            stopPendingIntent
        ).build()
    }

    private fun updateNotification() {
        if (::notificationManager.isInitialized) {
            notificationManager.notify(NOTIFICATION_ID, createSessionNotification())
        }
    }

    private fun updateNotificationWithData() {
        // Update notification periodically, not on every data point to avoid performance issues
        serviceScope.launch {
            delay(5000) // Update every 5 seconds max
            updateNotification()
        }
    }

    // Public methods for external access
    fun getCurrentSession(): TelemetrySession? = currentSession
    fun getSessionState(): SessionState = sessionState
    fun getSessionDuration(): Long = if (sessionStartTime > 0) System.currentTimeMillis() - sessionStartTime else 0
}
