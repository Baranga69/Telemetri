package com.commerin.telemetri.domain.model

// Aggregated telemetry events, combining location + motion + context
data class TelemetryEvent(
    val eventId: String,
    val location: LocationData?,
    val motion: MotionData?,            // derived metrics, not raw
    val rawSensors: List<SensorData>,   // optional (for advanced usage)
    val timestamp: Long
)
