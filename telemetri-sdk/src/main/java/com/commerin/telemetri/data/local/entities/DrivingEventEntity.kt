package com.commerin.telemetri.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.commerin.telemetri.data.local.converters.DatabaseConverters
import java.util.UUID

/**
 * Enhanced database entity for storing driving events with full context and metadata
 * Designed to capture all information from the refined driving event detection system
 */
@Entity(tableName = "driving_events")
@TypeConverters(DatabaseConverters::class)
data class DrivingEventEntity(
    @PrimaryKey
    val eventId: String = UUID.randomUUID().toString(),

    // Core event information
    val eventType: String, // SPEEDING, PHONE_USAGE, HARD_BRAKING, etc.
    val severity: String, // LOW, MEDIUM, HIGH, CRITICAL
    val timestamp: Long,
    val tripId: String?, // Link to trip session

    // Event magnitude and confidence
    val magnitude: Float, // Event intensity (speed over limit, deceleration rate, etc.)
    val confidence: Float, // Detection confidence (0.0 - 1.0)
    val duration: Long = 0L, // Event duration in milliseconds

    // Location and speed context
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val accuracy: Float?,
    val speed: Float?, // Speed at time of event (m/s)
    val bearing: Float?,

    // Road and environmental context
    val roadType: String?, // HIGHWAY, ARTERIAL, RESIDENTIAL, etc.
    val speedLimit: Float?, // Speed limit at location (km/h)
    val weatherConditions: String?, // CLEAR, RAIN, FOG, etc.
    val trafficDensity: String?, // LIGHT, MODERATE, HEAVY, CONGESTED
    val timeOfDay: String?, // MORNING_RUSH, EVENING, NIGHT, etc.
    val isRushHour: Boolean = false,
    val isSchoolZone: Boolean = false,
    val isConstructionZone: Boolean = false,

    // Phone usage specific data
    val phoneUsageDetails: String? = null, // JSON with detection factors
    val handMovementScore: Float? = null,
    val drivingDisruptionScore: Float? = null,
    val orientationChangeScore: Float? = null,
    val audioPatternScore: Float? = null,
    val speedCorrelationScore: Float? = null,

    // Speeding specific data
    val speedingThresholdType: String? = null, // URBAN, RURAL, HIGHWAY
    val speedOverLimit: Float? = null, // km/h over the speed limit
    val speedingDuration: Long? = null, // Duration of speeding in ms

    // Motion event specific data
    val accelerationX: Float? = null,
    val accelerationY: Float? = null,
    val accelerationZ: Float? = null,
    val gyroscopeX: Float? = null,
    val gyroscopeY: Float? = null,
    val gyroscopeZ: Float? = null,

    // Trip context
    val tripDistance: Float? = null, // Total trip distance at time of event
    val tripDuration: Long? = null, // Trip duration at time of event
    val previousEventTimestamp: Long? = null, // Time since last similar event

    // Sync and processing status
    val synced: Boolean = false,
    val processed: Boolean = false,
    val uploadAttempts: Int = 0,
    val lastUploadAttempt: Long? = null,

    // Additional metadata
    val deviceId: String? = null,
    val appVersion: String? = null,
    val sdkVersion: String? = null,
    val notes: String? = null,

    // Created and updated timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
