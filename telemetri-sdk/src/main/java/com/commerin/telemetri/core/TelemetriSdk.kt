package com.commerin.telemetri.core

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import com.commerin.telemetri.domain.model.ComprehensiveTelemetryEvent
import com.commerin.telemetri.core.TelemetryConfig

class TelemetriSdk(private val context: Context) {
    private val telemetriManager = TelemetriManager.getInstance(context)

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startTracking(config: TelemetryConfig = TelemetryConfig()) {
        telemetriManager.startTelemetryCollection(config)
    }

    fun stopTracking() {
        telemetriManager.stopTelemetryCollection()
    }

    fun observeTelemetry(): LiveData<ComprehensiveTelemetryEvent> {
        return telemetriManager.comprehensiveTelemetry
    }

    fun configureTelemetry(config: TelemetryConfig) {
        telemetriManager.configureTelemetry(config)
    }

    fun cleanup() {
        telemetriManager.cleanup()
    }
}