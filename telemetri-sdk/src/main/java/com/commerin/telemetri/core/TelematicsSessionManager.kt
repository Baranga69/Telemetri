package com.commerin.telemetri.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

/**
 * Manages telematics session lifecycle, persistence, and recovery
 */
class TelematicsSessionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "TelematicsSessionManager"
        private const val PREFS_NAME = "telematics_sessions"
        private const val KEY_ACTIVE_SESSION = "active_session"
        private const val KEY_SESSION_HISTORY = "session_history"

        @Volatile
        private var INSTANCE: TelematicsSessionManager? = null

        fun getInstance(context: Context): TelematicsSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TelematicsSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Live data for session state
    private val _activeSession = MutableLiveData<TelemetrySession?>()
    val activeSession: LiveData<TelemetrySession?> = _activeSession

    private val _sessionState = MutableLiveData<SessionState>()
    val sessionState: LiveData<SessionState> = _sessionState

    private val _sessionStatistics = MutableLiveData<SessionStatistics>()
    val sessionStatistics: LiveData<SessionStatistics> = _sessionStatistics

    init {
        // Restore active session if exists
        restoreActiveSession()
    }

    /**
     * Start a new telematics session
     */
    fun startSession(config: TelemetryConfig): String {
        val sessionId = TelematicsBackgroundService.startTelematicsSession(context, config)

        val session = TelemetrySession(
            sessionId = sessionId,
            startTime = System.currentTimeMillis(),
            config = config,
            state = SessionState.RUNNING
        )

        saveActiveSession(session)
        _activeSession.postValue(session)
        _sessionState.postValue(SessionState.RUNNING)

        Log.d(TAG, "Started new telematics session: $sessionId")
        return sessionId
    }

    /**
     * Stop the current active session
     */
    fun stopSession(sessionId: String? = null) {
        val currentSession = _activeSession.value

        if (sessionId != null && currentSession?.sessionId != sessionId) {
            Log.w(TAG, "Attempted to stop session $sessionId but active session is ${currentSession?.sessionId}")
            return
        }

        currentSession?.let { session ->
            TelematicsBackgroundService.stopTelematicsSession(context, session.sessionId)

            val endedSession = session.copy(
                endTime = System.currentTimeMillis(),
                state = SessionState.STOPPED
            )

            // Save to history and clear active session
            saveSessionToHistory(endedSession)
            clearActiveSession()

            _activeSession.postValue(null)
            _sessionState.postValue(SessionState.STOPPED)

            Log.d(TAG, "Stopped telematics session: ${session.sessionId}")
        }
    }

    /**
     * Pause the current active session
     */
    fun pauseSession(sessionId: String? = null) {
        val currentSession = _activeSession.value

        if (sessionId != null && currentSession?.sessionId != sessionId) {
            Log.w(TAG, "Attempted to pause session $sessionId but active session is ${currentSession?.sessionId}")
            return
        }

        currentSession?.let { session ->
            if (session.state == SessionState.RUNNING) {
                TelematicsBackgroundService.pauseTelematicsSession(context, session.sessionId)

                val pausedSession = session.copy(state = SessionState.PAUSED)
                saveActiveSession(pausedSession)
                _activeSession.postValue(pausedSession)
                _sessionState.postValue(SessionState.PAUSED)

                Log.d(TAG, "Paused telematics session: ${session.sessionId}")
            }
        }
    }

    /**
     * Resume the current paused session
     */
    fun resumeSession(sessionId: String? = null) {
        val currentSession = _activeSession.value

        if (sessionId != null && currentSession?.sessionId != sessionId) {
            Log.w(TAG, "Attempted to resume session $sessionId but active session is ${currentSession?.sessionId}")
            return
        }

        currentSession?.let { session ->
            if (session.state == SessionState.PAUSED) {
                TelematicsBackgroundService.resumeTelematicsSession(context, session.sessionId)

                val resumedSession = session.copy(state = SessionState.RUNNING)
                saveActiveSession(resumedSession)
                _activeSession.postValue(resumedSession)
                _sessionState.postValue(SessionState.RUNNING)

                Log.d(TAG, "Resumed telematics session: ${session.sessionId}")
            }
        }
    }

    /**
     * Get session duration in milliseconds
     */
    fun getSessionDuration(sessionId: String? = null): Long {
        val session = if (sessionId != null) {
            getSessionFromHistory(sessionId) ?: _activeSession.value
        } else {
            _activeSession.value
        }

        return session?.let {
            val endTime = it.endTime ?: System.currentTimeMillis()
            endTime - it.startTime
        } ?: 0L
    }

    /**
     * Check if a session is currently active
     */
    fun hasActiveSession(): Boolean {
        return _activeSession.value?.state == SessionState.RUNNING ||
               _activeSession.value?.state == SessionState.PAUSED
    }

    /**
     * Get current active session
     */
    fun getCurrentSession(): TelemetrySession? = _activeSession.value

    /**
     * Recovery mechanism for app restart scenarios
     */
    fun recoverSession() {
        val activeSession = _activeSession.value

        if (activeSession != null && TelematicsBackgroundService.isRunning()) {
            Log.d(TAG, "Recovering active session: ${activeSession.sessionId}")

            // Session is already running in background service, just update UI state
            _sessionState.postValue(activeSession.state)
        } else if (activeSession != null) {
            Log.d(TAG, "Restarting session after app recovery: ${activeSession.sessionId}")

            // Restart the background service with the saved session
            TelematicsBackgroundService.startTelematicsSession(context, activeSession.config)
            _sessionState.postValue(SessionState.RUNNING)
        }
    }

    /**
     * Auto-recovery for system-killed scenarios
     */
    fun autoRecoverIfNeeded() {
        managerScope.launch {
            delay(2000) // Wait for app to fully initialize

            val activeSession = _activeSession.value
            if (activeSession != null && !TelematicsBackgroundService.isRunning()) {
                Log.d(TAG, "Auto-recovering session after system kill: ${activeSession.sessionId}")

                // Restart session with existing configuration
                TelematicsBackgroundService.startTelematicsSession(context, activeSession.config)
            }
        }
    }

    private fun saveActiveSession(session: TelemetrySession) {
        try {
            val sessionJson = gson.toJson(session)
            prefs.edit().putString(KEY_ACTIVE_SESSION, sessionJson).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving active session", e)
        }
    }

    private fun restoreActiveSession() {
        try {
            val sessionJson = prefs.getString(KEY_ACTIVE_SESSION, null)
            if (sessionJson != null) {
                val session = gson.fromJson(sessionJson, TelemetrySession::class.java)
                _activeSession.postValue(session)
                _sessionState.postValue(session.state)

                Log.d(TAG, "Restored active session: ${session.sessionId}")

                // Auto-recover if needed
                autoRecoverIfNeeded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring active session", e)
            clearActiveSession()
        }
    }

    private fun clearActiveSession() {
        prefs.edit().remove(KEY_ACTIVE_SESSION).apply()
    }

    private fun saveSessionToHistory(session: TelemetrySession) {
        try {
            val historyJson = prefs.getString(KEY_SESSION_HISTORY, "[]")
            val historyType = object : TypeToken<MutableList<TelemetrySession>>() {}.type
            val history: MutableList<TelemetrySession> = gson.fromJson(historyJson, historyType)

            history.add(session)

            // Keep only last 50 sessions to avoid excessive storage
            if (history.size > 50) {
                history.removeAt(0)
            }

            val updatedHistoryJson = gson.toJson(history)
            prefs.edit().putString(KEY_SESSION_HISTORY, updatedHistoryJson).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving session to history", e)
        }
    }

    private fun getSessionFromHistory(sessionId: String): TelemetrySession? {
        try {
            val historyJson = prefs.getString(KEY_SESSION_HISTORY, "[]")
            val historyType = object : TypeToken<List<TelemetrySession>>() {}.type
            val history: List<TelemetrySession> = gson.fromJson(historyJson, historyType)

            return history.find { it.sessionId == sessionId }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving session from history", e)
            return null
        }
    }

    /**
     * Get session history
     */
    fun getSessionHistory(): List<TelemetrySession> {
        return try {
            val historyJson = prefs.getString(KEY_SESSION_HISTORY, "[]")
            val historyType = object : TypeToken<List<TelemetrySession>>() {}.type
            gson.fromJson(historyJson, historyType) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving session history", e)
            emptyList()
        }
    }

    /**
     * Clear all session history
     */
    fun clearSessionHistory() {
        prefs.edit().remove(KEY_SESSION_HISTORY).apply()
    }
}
