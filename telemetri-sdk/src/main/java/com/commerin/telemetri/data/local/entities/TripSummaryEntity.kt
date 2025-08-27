package com.commerin.telemetri.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.commerin.telemetri.data.local.converters.DatabaseConverters
import java.util.UUID

/**
 * Entity for storing trip-level aggregated data and scoring information
 */
@Entity(tableName = "trip_summaries")
@TypeConverters(DatabaseConverters::class)
data class TripSummaryEntity(
    @PrimaryKey
    val tripId: String = UUID.randomUUID().toString(),

    // Trip basic information
    val startTimestamp: Long,
    val endTimestamp: Long,
    val totalDuration: Long, // milliseconds
    val totalDistance: Float, // kilometers
    val averageSpeed: Float, // km/h
    val maxSpeed: Float, // km/h

    // Trip scores (0-100)
    val overallScore: Float,
    val safetyScore: Float,
    val efficiencyScore: Float,
    val smoothnessScore: Float,
    val legalComplianceScore: Float,

    // Event counts by type
    val speedingEventCount: Int = 0,
    val phoneUsageEventCount: Int = 0,
    val hardBrakingEventCount: Int = 0,
    val rapidAccelerationEventCount: Int = 0,
    val harshCorneringEventCount: Int = 0,
    val aggressiveDrivingEventCount: Int = 0,
    val smoothDrivingEventCount: Int = 0,
    val ecoDrivingEventCount: Int = 0,

    // Event counts by severity
    val criticalEventCount: Int = 0,
    val highSeverityEventCount: Int = 0,
    val mediumSeverityEventCount: Int = 0,
    val lowSeverityEventCount: Int = 0,

    // Trip characteristics
    val speedingDuration: Long = 0L, // Total time spent speeding (ms)
    val idleTime: Long = 0L, // Time spent stationary (ms)
    val nightDrivingPercentage: Float = 0f,
    val highRiskRoadPercentage: Float = 0f,
    val fuelEfficiencyScore: Float = 0f,

    // Route information
    val startLatitude: Double?,
    val startLongitude: Double?,
    val endLatitude: Double?,
    val endLongitude: Double?,
    val maxAltitude: Float? = null,
    val minAltitude: Float? = null,

    // Road type breakdown (percentages)
    val highwayPercentage: Float = 0f,
    val arterialPercentage: Float = 0f,
    val residentialPercentage: Float = 0f,
    val unknownRoadPercentage: Float = 0f,

    // Weather and traffic context
    val primaryWeatherCondition: String? = null,
    val primaryTrafficDensity: String? = null,
    val rushHourPercentage: Float = 0f,

    // Risk factors summary
    val totalRiskFactors: Int = 0,
    val riskFactorDetails: String? = null, // JSON array of risk factors

    // Phone usage analytics
    val phoneUsageTotalDuration: Long = 0L,
    val phoneUsageConfidenceAvg: Float = 0f,
    val phoneUsageHighestConfidence: Float = 0f,

    // Speeding analytics
    val maxSpeedOverLimit: Float = 0f,
    val avgSpeedOverLimit: Float = 0f,
    val urbanSpeedingCount: Int = 0,
    val ruralSpeedingCount: Int = 0,
    val highwaySpeedingCount: Int = 0,

    // Motion analytics
    val maxDeceleration: Float = 0f,
    val maxAcceleration: Float = 0f,
    val maxLateralAcceleration: Float = 0f,
    val avgAccelerationVariance: Float = 0f,

    // Sync and processing
    val synced: Boolean = false,
    val processed: Boolean = false,
    val reportGenerated: Boolean = false,
    val uploadAttempts: Int = 0,
    val lastUploadAttempt: Long? = null,

    // Metadata
    val deviceId: String? = null,
    val driverProfileId: String? = null,
    val vehicleId: String? = null,
    val appVersion: String? = null,
    val sdkVersion: String? = null,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
