package com.commerin.telemetri.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commerin.telemetri.core.TelemetriManager
import com.commerin.telemetri.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionStats(
    val totalDataPoints: Int,
    val activeSensors: Int,
    val uptimeMinutes: Int,
    val securityEvents: Int
)

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val telemetriManager: TelemetriManager
) : ViewModel() {

    private val _isCollecting = MutableLiveData<Boolean>(false)
    val isCollecting: LiveData<Boolean> = _isCollecting

    private val _telemetryData = MutableLiveData<ComprehensiveTelemetryEvent>()
    val telemetryData: LiveData<ComprehensiveTelemetryEvent> = _telemetryData

    private val _alertLevel = MutableLiveData<String>("Normal")
    val alertLevel: LiveData<String> = _alertLevel

    private val _dataQuality = MutableLiveData<String>("Unknown")
    val dataQuality: LiveData<String> = _dataQuality

    private val _collectionStats = MutableLiveData<CollectionStats>()
    val collectionStats: LiveData<CollectionStats> = _collectionStats

    private var startTime: Long = 0
    private var totalDataPoints = 0
    private var securityEvents = 0

    init {
        observeTelemetryData()
    }

    fun startSecurityCollection() {
        viewModelScope.launch {
            val securityConfig = TelemetriManager.ConfigPresets.securityMonitoringUseCase()
            telemetriManager.startTelemetryCollection(securityConfig)
            _isCollecting.value = true
            startTime = System.currentTimeMillis()
            totalDataPoints = 0
            securityEvents = 0
            updateCollectionStats()
        }
    }

    fun stopCollection() {
        viewModelScope.launch {
            telemetriManager.stopTelemetryCollection()
            _isCollecting.value = false
        }
    }

    private fun observeTelemetryData() {
        telemetriManager.comprehensiveTelemetry.observeForever { telemetry ->
            _telemetryData.value = telemetry
            totalDataPoints++

            // Analyze data quality
            analyzeDataQuality(telemetry)

            // Check for security events
            analyzeSecurityEvents(telemetry)

            // Update statistics
            updateCollectionStats()
        }
    }

    private fun analyzeDataQuality(telemetry: ComprehensiveTelemetryEvent) {
        val hasLocation = telemetry.location != null
        val hasSensors = telemetry.sensors.isNotEmpty()
        val hasEnvironmental = telemetry.environmental != null
        val hasMotion = telemetry.motion != null
        val hasDeviceState = telemetry.deviceState != null

        val qualityScore = listOf(hasLocation, hasSensors, hasEnvironmental, hasMotion, hasDeviceState).count { it }

        _dataQuality.value = when (qualityScore) {
            5 -> "Excellent"
            4 -> "High"
            3 -> "Medium"
            2 -> "Low"
            1 -> "Poor"
            else -> "Critical"
        }
    }

    private fun analyzeSecurityEvents(telemetry: ComprehensiveTelemetryEvent) {
        var currentAlertLevel = "Normal"

        // Check various security indicators
        telemetry.environmental?.let { env ->
            if (env.noiseLevel > 80) {
                currentAlertLevel = "High"
                securityEvents++
            }
        }

        telemetry.motion?.let { motion ->
            if (motion.accelerationMagnitude > 15.0f) {
                currentAlertLevel = "Critical"
                securityEvents++
            }
        }

        telemetry.deviceState?.let { device ->
            if (device.batteryTemperature > 40) {
                currentAlertLevel = "Medium"
            }
        }

        telemetry.location?.let { location ->
            location.speed?.let {
                if (it > 30) { // High speed detected
                    currentAlertLevel = "High"
                }
            }
        }

        _alertLevel.value = currentAlertLevel
    }

    private fun updateCollectionStats() {
        if (!_isCollecting.value!!) return

        val uptime = if (startTime > 0) ((System.currentTimeMillis() - startTime) / 60000).toInt() else 0
        val activeSensors = _telemetryData.value?.sensors?.size ?: 0

        _collectionStats.value = CollectionStats(
            totalDataPoints = totalDataPoints,
            activeSensors = activeSensors,
            uptimeMinutes = uptime,
            securityEvents = securityEvents
        )
    }

    override fun onCleared() {
        super.onCleared()
        telemetriManager.cleanup()
    }
}
