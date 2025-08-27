package com.commerin.telemetri.core

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.commerin.telemetri.domain.model.*

/**
 * Enhanced Telematics Manager with background session management
 * This wraps the existing TelemetriManager to provide session persistence
 */
class EnhancedTelematicsManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: EnhancedTelematicsManager? = null

        fun getInstance(context: Context): EnhancedTelematicsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EnhancedTelematicsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val sessionManager = TelematicsSessionManager.getInstance(context)
    private val telemetriManager = TelemetriManager.getInstance(context)

    // Expose session management
    val activeSession: LiveData<TelemetrySession?> = sessionManager.activeSession
    val sessionState: LiveData<SessionState> = sessionManager.sessionState
    val sessionStatistics: LiveData<SessionStatistics> = sessionManager.sessionStatistics

    // Expose telemetry data (delegates to existing TelemetriManager)
    val comprehensiveTelemetry = telemetriManager.comprehensiveTelemetry
    val sensorData = telemetriManager.sensorData
    val locationData = telemetriManager.locationData
    val audioTelemetry = telemetriManager.audioTelemetry
    val networkTelemetry = telemetriManager.networkTelemetry
    val speedTestResult = telemetriManager.speedTestResult
    val performanceTelemetry = telemetriManager.performanceTelemetry
    val motionAnalysis = telemetriManager.motionAnalysis

    /**
     * Start a telematics session with background persistence
     */
    fun startBackgroundSession(config: TelemetryConfig): String {
        return sessionManager.startSession(config)
    }

    /**
     * Stop the current session
     */
    fun stopBackgroundSession(sessionId: String? = null) {
        sessionManager.stopSession(sessionId)
    }

    /**
     * Pause the current session (useful for breaks during long trips)
     */
    fun pauseSession(sessionId: String? = null) {
        sessionManager.pauseSession(sessionId)
    }

    /**
     * Resume a paused session
     */
    fun resumeSession(sessionId: String? = null) {
        sessionManager.resumeSession(sessionId)
    }

    /**
     * Check if a session is currently active
     */
    fun hasActiveSession(): Boolean = sessionManager.hasActiveSession()

    /**
     * Get current session information
     */
    fun getCurrentSession(): TelemetrySession? = sessionManager.getCurrentSession()

    /**
     * Get session duration
     */
    fun getSessionDuration(sessionId: String? = null): Long = sessionManager.getSessionDuration(sessionId)

    /**
     * Get session history
     */
    fun getSessionHistory(): List<TelemetrySession> = sessionManager.getSessionHistory()

    /**
     * Convenient method to start automotive session with optimized config
     */
    fun startAutomotiveSession(): String {
        val config = TelemetriManager.ConfigPresets.automotiveUseCase()
        return startBackgroundSession(config)
    }

    /**
     * Check battery optimization status
     */
    fun isBatteryOptimized(): Boolean = !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)

    /**
     * Get battery optimization status message
     */
    fun getBatteryOptimizationStatus(): String = BatteryOptimizationHelper.getBatteryOptimizationStatus(context)
}
