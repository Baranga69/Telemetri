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

data class ActivityStats(
    val stepCount: Int,
    val distance: Double, // in kilometers
    val calories: Int,
    val duration: Int // in minutes
)

@HiltViewModel
class FitnessViewModel @Inject constructor(
    private val telemetriManager: TelemetriManager
) : ViewModel() {

    private val _isCollecting = MutableLiveData<Boolean>(false)
    val isCollecting: LiveData<Boolean> = _isCollecting

    private val _motionData = MutableLiveData<MotionData>()
    val motionData: LiveData<MotionData> = _motionData

    private val _locationData = MutableLiveData<LocationData>()
    val locationData: LiveData<LocationData> = _locationData

    private val _performanceData = MutableLiveData<PerformanceTelemetryData>()
    val performanceData: LiveData<PerformanceTelemetryData> = _performanceData

    private val _activityStats = MutableLiveData<ActivityStats>()
    val activityStats: LiveData<ActivityStats> = _activityStats

    private var startTime: Long = 0
    private var totalDistance: Double = 0.0
    private var previousLocation: LocationData? = null

    init {
        observeTelemetryData()
    }

    fun startFitnessCollection() {
        viewModelScope.launch {
            val fitnessConfig = TelemetriManager.ConfigPresets.fitnessTrackingUseCase()
            telemetriManager.startTelemetryCollection(fitnessConfig)
            _isCollecting.value = true
            startTime = System.currentTimeMillis()
            totalDistance = 0.0
            updateActivityStats()
        }
    }

    fun stopCollection() {
        viewModelScope.launch {
            telemetriManager.stopTelemetryCollection()
            _isCollecting.value = false
        }
    }

    private fun observeTelemetryData() {
        telemetriManager.motionAnalysis.observeForever { motion ->
            _motionData.value = motion
            updateActivityStats()
        }

        telemetriManager.locationData.observeForever { location ->
            _locationData.value = location
            calculateDistance(location)
            updateActivityStats()
        }

        telemetriManager.performanceTelemetry.observeForever { performance ->
            _performanceData.value = performance
        }
    }

    private fun calculateDistance(currentLocation: LocationData) {
        previousLocation?.let { previous ->
            val distance = calculateDistanceBetween(
                previous.latitude, previous.longitude,
                currentLocation.latitude, currentLocation.longitude
            )
            totalDistance += distance
        }
        previousLocation = currentLocation
    }

    private fun calculateDistanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }

    private fun updateActivityStats() {
        if (!_isCollecting.value!!) return

        val motion = _motionData.value
        val duration = if (startTime > 0) ((System.currentTimeMillis() - startTime) / 60000).toInt() else 0
        val stepCount = motion?.stepCount ?: 0
        val calories = calculateCalories(stepCount, totalDistance, duration)

        _activityStats.value = ActivityStats(
            stepCount = stepCount,
            distance = totalDistance,
            calories = calories,
            duration = duration
        )
    }

    private fun calculateCalories(steps: Int, distance: Double, durationMinutes: Int): Int {
        // Simple calorie calculation based on steps and distance
        // This is a basic estimation and would be more sophisticated in a real app
        val caloriesPerStep = 0.04 // Average calories per step
        val caloriesPerKm = 50 // Average calories per kilometer
        return (steps * caloriesPerStep + distance * caloriesPerKm).toInt()
    }

    override fun onCleared() {
        super.onCleared()
        telemetriManager.cleanup()
    }
}
