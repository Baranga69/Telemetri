package com.commerin.telemetri.core

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.TelemetryEvent
import com.commerin.telemetri.domain.model.LocationData

class TelemetriManager(
    private val locationService: LocationService,
    private val sensorService: SensorService
) {
    private val telemetryLiveData = MutableLiveData<TelemetryEvent>()

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun start() {
        locationService.start()
        sensorService.start()
        // Observe and combine data
        locationService.locationData.observeForever { location ->
            sensorService.sensorData.value?.let { sensor ->
                // Convert Location to LocationData
                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    speed = location.speed,
                    bearing = location.bearing,
                    timestamp = location.time
                )

                val telemetryEvent = TelemetryEvent(
                    eventId = System.currentTimeMillis().toString(),
                    location = locationData,
                    motion = null, // Add motion data if available
                    rawSensors = listOf(sensor),
                    timestamp = System.currentTimeMillis()
                )
                telemetryLiveData.value = telemetryEvent
            }
        }
    }

    fun stop() {
        locationService.stop()
        sensorService.stop()
    }

    fun getTelemetryData(): LiveData<TelemetryEvent> = telemetryLiveData
}