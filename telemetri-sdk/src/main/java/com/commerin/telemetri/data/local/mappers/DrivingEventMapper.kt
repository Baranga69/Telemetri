package com.commerin.telemetri.data.local.mappers

import com.commerin.telemetri.data.local.entities.DrivingEventEntity
import com.commerin.telemetri.data.local.entities.TripSummaryEntity
import com.commerin.telemetri.domain.model.*
import com.google.gson.Gson

/**
 * Mappers to convert between domain models and database entities
 * Handles the enhanced driving event data structure
 */
object DrivingEventMapper {
    private val gson = Gson()

    /**
     * Converts DrivingEvent domain model to DrivingEventEntity for database storage
     */
    fun toDatabaseEntity(
        event: DrivingEvent,
        tripId: String? = null,
        deviceInfo: DeviceInfo? = null
    ): DrivingEventEntity {
        return DrivingEventEntity(
            eventId = generateEventId(event),
            eventType = event.eventType.name,
            severity = event.severity.name,
            timestamp = event.timestamp,
            tripId = tripId,

            // Event magnitude and confidence
            magnitude = event.acceleration,
            confidence = event.confidence,
            duration = event.duration,

            // Location context
            latitude = event.location?.latitude,
            longitude = event.location?.longitude,
            altitude = event.location?.altitude,
            accuracy = event.location?.accuracy,
            speed = event.speed,
            bearing = event.location?.bearing,

            // Road and environmental context
            roadType = event.context.roadType.name,
            speedLimit = getSpeedLimitFromContext(event.context),
            weatherConditions = event.context.weatherConditions?.name,
            trafficDensity = event.context.trafficDensity.name,
            timeOfDay = event.context.timeOfDay.name,
            isRushHour = event.context.isRushHour,
            isSchoolZone = event.context.schoolZone,
            isConstructionZone = event.context.constructionZone,

            // Phone usage specific data (will be populated by enhanced detection)
            phoneUsageDetails = if (event.eventType == DrivingEventType.PHONE_USAGE) {
                createPhoneUsageDetails(event)
            } else null,

            // Speeding specific data
            speedingThresholdType = if (event.eventType == DrivingEventType.SPEEDING) {
                determineSpeedingThresholdType(event.context.roadType)
            } else null,
            speedOverLimit = if (event.eventType == DrivingEventType.SPEEDING) {
                event.acceleration // The magnitude for speeding events is speed over limit
            } else null,

            // Device and app info
            deviceId = deviceInfo?.deviceId,
            appVersion = deviceInfo?.appVersion,
            sdkVersion = deviceInfo?.sdkVersion
        )
    }

    /**
     * Converts TripScore domain model to TripSummaryEntity for database storage
     */
    fun toTripSummaryEntity(
        tripScore: TripScore,
        tripId: String,
        startTimestamp: Long,
        endTimestamp: Long,
        deviceInfo: DeviceInfo? = null
    ): TripSummaryEntity {
        val stats = tripScore.tripStatistics

        return TripSummaryEntity(
            tripId = tripId,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            totalDuration = stats.totalDuration,
            totalDistance = stats.totalDistance,
            averageSpeed = stats.averageSpeed,
            maxSpeed = stats.maxSpeed,

            // Scores
            overallScore = tripScore.overallScore,
            safetyScore = tripScore.safetyScore,
            efficiencyScore = tripScore.efficiencyScore,
            smoothnessScore = tripScore.smoothnessScore,
            legalComplianceScore = tripScore.legalComplianceScore,

            // Event counts by type
            speedingEventCount = tripScore.events.count { it.eventType == DrivingEventType.SPEEDING },
            phoneUsageEventCount = tripScore.events.count { it.eventType == DrivingEventType.PHONE_USAGE },
            hardBrakingEventCount = tripScore.events.count { it.eventType == DrivingEventType.HARD_BRAKING },
            rapidAccelerationEventCount = tripScore.events.count { it.eventType == DrivingEventType.RAPID_ACCELERATION },
            harshCorneringEventCount = tripScore.events.count { it.eventType == DrivingEventType.HARSH_CORNERING },
            aggressiveDrivingEventCount = tripScore.events.count { it.eventType == DrivingEventType.AGGRESSIVE_DRIVING },
            smoothDrivingEventCount = tripScore.events.count { it.eventType == DrivingEventType.SMOOTH_DRIVING },
            ecoDrivingEventCount = tripScore.events.count { it.eventType == DrivingEventType.ECO_DRIVING },

            // Event counts by severity
            criticalEventCount = tripScore.events.count { it.severity == EventSeverity.CRITICAL },
            highSeverityEventCount = tripScore.events.count { it.severity == EventSeverity.HIGH },
            mediumSeverityEventCount = tripScore.events.count { it.severity == EventSeverity.MEDIUM },
            lowSeverityEventCount = tripScore.events.count { it.severity == EventSeverity.LOW },

            // Trip characteristics
            speedingDuration = stats.speedingDuration,
            idleTime = stats.idleTime,
            nightDrivingPercentage = stats.nightDrivingPercentage,
            highRiskRoadPercentage = stats.highRiskRoadPercentage,
            fuelEfficiencyScore = stats.fuelEfficiencyScore,

            // Route information
            startLatitude = getStartLatitude(tripScore.events),
            startLongitude = getStartLongitude(tripScore.events),
            endLatitude = getEndLatitude(tripScore.events),
            endLongitude = getEndLongitude(tripScore.events),

            // Risk factors
            totalRiskFactors = tripScore.riskFactors.size,
            riskFactorDetails = gson.toJson(tripScore.riskFactors),

            // Phone usage analytics
            phoneUsageTotalDuration = calculatePhoneUsageTotalDuration(tripScore.events),
            phoneUsageConfidenceAvg = calculatePhoneUsageAverageConfidence(tripScore.events),
            phoneUsageHighestConfidence = calculatePhoneUsageHighestConfidence(tripScore.events),

            // Speeding analytics
            maxSpeedOverLimit = calculateMaxSpeedOverLimit(tripScore.events),
            avgSpeedOverLimit = calculateAvgSpeedOverLimit(tripScore.events),
            urbanSpeedingCount = countSpeedingByRoadType(tripScore.events, RoadType.RESIDENTIAL),
            ruralSpeedingCount = countSpeedingByRoadType(tripScore.events, RoadType.ARTERIAL),
            highwaySpeedingCount = countSpeedingByRoadType(tripScore.events, RoadType.HIGHWAY),

            // Motion analytics
            maxDeceleration = calculateMaxDeceleration(tripScore.events),
            maxAcceleration = calculateMaxAcceleration(tripScore.events),
            maxLateralAcceleration = calculateMaxLateralAcceleration(tripScore.events),

            // Device info
            deviceId = deviceInfo?.deviceId,
            appVersion = deviceInfo?.appVersion,
            sdkVersion = deviceInfo?.sdkVersion
        )
    }

    /**
     * Converts database entity back to domain model
     */
    fun toDomainModel(entity: DrivingEventEntity): DrivingEvent {
        return DrivingEvent(
            eventType = DrivingEventType.valueOf(entity.eventType),
            severity = EventSeverity.valueOf(entity.severity),
            timestamp = entity.timestamp,
            location = if (entity.latitude != null && entity.longitude != null) {
                LocationData(
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    altitude = entity.altitude,
                    accuracy = entity.accuracy,
                    speed = entity.speed,
                    bearing = entity.bearing,
                    timestamp = entity.timestamp
                )
            } else null,
            speed = entity.speed ?: 0f,
            acceleration = entity.magnitude,
            duration = entity.duration,
            confidence = entity.confidence,
            context = EventContext(
                weatherConditions = entity.weatherConditions?.let { WeatherConditions.valueOf(it) },
                trafficDensity = TrafficDensity.valueOf(entity.trafficDensity ?: "MODERATE"),
                roadType = RoadType.valueOf(entity.roadType ?: "UNKNOWN"),
                timeOfDay = TimeOfDay.valueOf(entity.timeOfDay ?: "MIDDAY"),
                isRushHour = entity.isRushHour,
                schoolZone = entity.isSchoolZone,
                constructionZone = entity.isConstructionZone
            )
        )
    }

    // Helper methods for mapping calculations

    private fun generateEventId(event: DrivingEvent): String {
        return "${event.eventType.name}_${event.timestamp}_${event.hashCode()}"
    }

    private fun getSpeedLimitFromContext(context: EventContext): Float? {
        return when (context.roadType) {
            RoadType.RESIDENTIAL -> 50f
            RoadType.ARTERIAL -> 80f
            RoadType.HIGHWAY -> 100f
            else -> null
        }
    }

    private fun createPhoneUsageDetails(event: DrivingEvent): String {
        // This would be populated with actual phone usage detection scores
        // For now, return basic info
        return gson.toJson(mapOf(
            "confidence" to event.confidence,
            "duration" to event.duration,
            "timestamp" to event.timestamp
        ))
    }

    private fun determineSpeedingThresholdType(roadType: RoadType): String {
        return when (roadType) {
            RoadType.RESIDENTIAL -> "URBAN"
            RoadType.ARTERIAL -> "RURAL"
            RoadType.HIGHWAY -> "HIGHWAY"
            else -> "URBAN"
        }
    }

    private fun calculatePhoneUsageTotalDuration(events: List<DrivingEvent>): Long {
        return events.filter { it.eventType == DrivingEventType.PHONE_USAGE }
            .sumOf { it.duration }
    }

    private fun calculatePhoneUsageAverageConfidence(events: List<DrivingEvent>): Float {
        val phoneEvents = events.filter { it.eventType == DrivingEventType.PHONE_USAGE }
        return if (phoneEvents.isNotEmpty()) {
            phoneEvents.map { it.confidence }.average().toFloat()
        } else 0f
    }

    private fun calculatePhoneUsageHighestConfidence(events: List<DrivingEvent>): Float {
        return events.filter { it.eventType == DrivingEventType.PHONE_USAGE }
            .maxOfOrNull { it.confidence } ?: 0f
    }

    private fun calculateMaxSpeedOverLimit(events: List<DrivingEvent>): Float {
        return events.filter { it.eventType == DrivingEventType.SPEEDING }
            .maxOfOrNull { it.acceleration } ?: 0f
    }

    private fun calculateAvgSpeedOverLimit(events: List<DrivingEvent>): Float {
        val speedingEvents = events.filter { it.eventType == DrivingEventType.SPEEDING }
        return if (speedingEvents.isNotEmpty()) {
            speedingEvents.map { it.acceleration }.average().toFloat()
        } else 0f
    }

    private fun countSpeedingByRoadType(events: List<DrivingEvent>, roadType: RoadType): Int {
        return events.count {
            it.eventType == DrivingEventType.SPEEDING && it.context.roadType == roadType
        }
    }

    private fun calculateMaxDeceleration(events: List<DrivingEvent>): Float {
        return events.filter { it.eventType == DrivingEventType.HARD_BRAKING }
            .maxOfOrNull { it.acceleration } ?: 0f
    }

    private fun calculateMaxAcceleration(events: List<DrivingEvent>): Float {
        return events.filter { it.eventType == DrivingEventType.RAPID_ACCELERATION }
            .maxOfOrNull { it.acceleration } ?: 0f
    }

    private fun calculateMaxLateralAcceleration(events: List<DrivingEvent>): Float {
        return events.filter { it.eventType == DrivingEventType.HARSH_CORNERING }
            .maxOfOrNull { it.acceleration } ?: 0f
    }

    private fun getStartLatitude(events: List<DrivingEvent>): Double? {
        return events.minByOrNull { it.timestamp }?.location?.latitude
    }

    private fun getStartLongitude(events: List<DrivingEvent>): Double? {
        return events.minByOrNull { it.timestamp }?.location?.longitude
    }

    private fun getEndLatitude(events: List<DrivingEvent>): Double? {
        return events.maxByOrNull { it.timestamp }?.location?.latitude
    }

    private fun getEndLongitude(events: List<DrivingEvent>): Double? {
        return events.maxByOrNull { it.timestamp }?.location?.longitude
    }
}

// Data class for device information
data class DeviceInfo(
    val deviceId: String,
    val appVersion: String,
    val sdkVersion: String
)
