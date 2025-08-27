package com.commerin.telemetri.domain.model

import com.commerin.telemetri.core.TelemetryConfig
import java.io.Serializable

/**
 * Represents a telematics data collection session with lifecycle management
 */
data class TelemetrySession(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val config: TelemetryConfig,
    val state: SessionState,
    val pausedDuration: Long = 0,
    val totalDistance: Double = 0.0,
    val averageSpeed: Double = 0.0,
    val maxSpeed: Double = 0.0,
    val eventCount: Int = 0
) : Serializable

/**
 * Session state enumeration for lifecycle management
 */
enum class SessionState {
    RUNNING,    // Active data collection
    PAUSED,     // Temporarily stopped but can be resumed
    STOPPED,    // Ended normally
    ERROR       // Stopped due to error
}

/**
 * Session statistics for monitoring and reporting
 */
data class SessionStatistics(
    val sessionId: String,
    val duration: Long,
    val activeDuration: Long, // Total duration minus paused time
    val totalDistance: Double,
    val averageSpeed: Double,
    val maxSpeed: Double,
    val eventCount: Int,
    val dataPointsCollected: Int,
    val batteryUsage: Double? = null
)
