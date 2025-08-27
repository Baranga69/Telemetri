package com.commerin.telemetri.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.commerin.telemetri.core.EnhancedTelematicsManager
import com.commerin.telemetri.core.TelemetryConfig
import com.commerin.telemetri.domain.model.TelemetrySession
import com.commerin.telemetri.domain.model.SessionState
import kotlinx.coroutines.launch

/**
 * ViewModel for managing telematics sessions with background persistence
 */
class TelematicsSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val telematicsManager = EnhancedTelematicsManager.getInstance(application)

    // Expose session data
    val activeSession: LiveData<TelemetrySession?> = telematicsManager.activeSession
    val sessionState: LiveData<SessionState> = telematicsManager.sessionState

    // Expose telemetry data
    val locationData = telematicsManager.locationData
    val sensorData = telematicsManager.sensorData
    val comprehensiveTelemetry = telematicsManager.comprehensiveTelemetry

    // UI state
    private val _isStartingSession = MutableLiveData<Boolean>()
    val isStartingSession: LiveData<Boolean> = _isStartingSession

    private val _sessionDuration = MutableLiveData<Long>()
    val sessionDuration: LiveData<Long> = _sessionDuration

    private val _batteryOptimizationStatus = MutableLiveData<String>()
    val batteryOptimizationStatus: LiveData<String> = _batteryOptimizationStatus

    init {
        updateBatteryOptimizationStatus()
        startDurationUpdater()
    }

    /**
     * Start a new automotive telematics session
     */
    fun startAutomotiveSession() {
        viewModelScope.launch {
            _isStartingSession.value = true
            try {
                telematicsManager.startAutomotiveSession()
                _isStartingSession.value = false
            } catch (e: Exception) {
                _isStartingSession.value = false
                // Handle error - could emit error state
            }
        }
    }

    /**
     * Start session with custom configuration
     */
    fun startSession(config: TelemetryConfig) {
        viewModelScope.launch {
            _isStartingSession.value = true
            try {
                telematicsManager.startBackgroundSession(config)
                _isStartingSession.value = false
            } catch (e: Exception) {
                _isStartingSession.value = false
                // Handle error - could emit error state
            }
        }
    }

    /**
     * Stop the current session
     */
    fun stopSession() {
        telematicsManager.stopBackgroundSession()
    }

    /**
     * Pause the current session
     */
    fun pauseSession() {
        telematicsManager.pauseSession()
    }

    /**
     * Resume the current session
     */
    fun resumeSession() {
        telematicsManager.resumeSession()
    }

    /**
     * Check if session is currently active
     */
    fun hasActiveSession(): Boolean = telematicsManager.hasActiveSession()

    /**
     * Get current session
     */
    fun getCurrentSession(): TelemetrySession? = telematicsManager.getCurrentSession()

    /**
     * Update battery optimization status
     */
    fun updateBatteryOptimizationStatus() {
        _batteryOptimizationStatus.value = telematicsManager.getBatteryOptimizationStatus()
    }

    /**
     * Check if battery optimization needs to be addressed
     */
    fun needsBatteryOptimization(): Boolean = telematicsManager.isBatteryOptimized()

    private fun startDurationUpdater() {
        viewModelScope.launch {
            while (true) {
                _sessionDuration.value = telematicsManager.getSessionDuration()
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }
}
