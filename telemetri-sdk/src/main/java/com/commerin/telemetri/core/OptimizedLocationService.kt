package com.commerin.telemetri.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.data.location.LocationDataSource
import com.commerin.telemetri.domain.model.LocationData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

class OptimizedLocationService(private val context: Context) {
    companion object {
        private const val TAG = "OptimizedLocService"
        private const val DEFAULT_UPDATE_INTERVAL = 5000L // 5 seconds
    }

    private val locationDataSource = LocationDataSource(context)
    private val _locationData = MutableLiveData<LocationData>()
    val locationData: LiveData<LocationData> = _locationData

    private var isCollecting = false
    private var locationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(intervalMs: Long = DEFAULT_UPDATE_INTERVAL) {
        if (isCollecting) {
            Log.d(TAG, "Location updates already running")
            return
        }

        isCollecting = true
        locationJob = scope.launch {
            try {
                locationDataSource.getLocationUpdates(intervalMs)
                    .catch { e ->
                        Log.e(TAG, "Error in location updates", e)
                    }
                    .collect { locationRaw ->
                        val locationData = LocationData(
                            latitude = locationRaw.latitude,
                            longitude = locationRaw.longitude,
                            altitude = locationRaw.altitude,
                            speed = locationRaw.speed,
                            accuracy = locationRaw.accuracy,
                            bearing = locationRaw.bearing,
                            provider = locationRaw.provider,
                            timestamp = locationRaw.timestamp
                        )
                        _locationData.postValue(locationData)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start location updates", e)
            }
        }

        Log.d(TAG, "Location updates started with interval: ${intervalMs}ms")
    }

    fun stopLocationUpdates() {
        if (!isCollecting) {
            Log.d(TAG, "Location updates not running")
            return
        }

        isCollecting = false
        locationJob?.cancel()
        locationJob = null
        Log.d(TAG, "Location updates stopped")
    }

    fun cleanup() {
        stopLocationUpdates()
        scope.cancel()
    }
}
