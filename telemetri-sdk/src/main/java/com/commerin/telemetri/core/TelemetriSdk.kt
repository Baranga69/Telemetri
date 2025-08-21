package com.commerin.telemetri.core

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import com.commerin.telemetri.domain.model.TelemetryEvent

class TelematriSdk(private val context: Context) {
    private val locationService = LocationService(context)
    private val sensorService = SensorService(context)
    private val telemetryManager = TelemetriManager(locationService, sensorService)

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startTracking() {
        telemetryManager.start()
    }

    fun stopTracking() {
        telemetryManager.stop()
    }

    fun observeTelemetry(): LiveData<TelemetryEvent> {
        return telemetryManager.getTelemetryData()
    }
}