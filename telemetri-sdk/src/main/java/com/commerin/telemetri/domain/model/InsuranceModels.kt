package com.commerin.telemetri.domain.model

/**
 * Enhanced data models for insurance telematics
 */

data class DriverState(
    val isDriver: Boolean,
    val confidence: Float,
    val phonePosition: com.commerin.telemetri.core.DriverDetectionEngine.Position,
    val movementPattern: com.commerin.telemetri.core.DriverDetectionEngine.MovementType,
    val evidenceFactors: Map<String, Float>,
    val timestamp: Long
)

data class DrivingEvent(
    val eventType: DrivingEventType,
    val severity: EventSeverity,
    val timestamp: Long,
    val location: LocationData?,
    val speed: Float,
    val acceleration: Float,
    val duration: Long,
    val confidence: Float,
    val context: EventContext
)

enum class DrivingEventType {
    HARD_BRAKING,
    RAPID_ACCELERATION,
    HARSH_CORNERING,
    SPEEDING,
    PHONE_USAGE,
    DISTRACTED_DRIVING,
    FATIGUE_DETECTED,
    AGGRESSIVE_DRIVING,
    SMOOTH_DRIVING,
    ECO_DRIVING
}

enum class EventSeverity(i: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    val value: Int
        get() = ordinal + 1
}

data class EventContext(
    val weatherConditions: WeatherConditions?,
    val trafficDensity: TrafficDensity,
    val roadType: RoadType,
    val timeOfDay: TimeOfDay,
    val isRushHour: Boolean,
    val schoolZone: Boolean,
    val constructionZone: Boolean
)

enum class WeatherConditions {
    CLEAR, RAIN, SNOW, FOG, ICE, STORM
}

enum class TrafficDensity {
    LIGHT, MODERATE, HEAVY, CONGESTED
}

enum class RoadType {
    HIGHWAY, ARTERIAL, RESIDENTIAL, PARKING_LOT, UNKNOWN
}

enum class TimeOfDay {
    EARLY_MORNING(0, 6),
    MORNING_RUSH(6, 10),
    MIDDAY(10, 15),
    EVENING_RUSH(15, 19),
    EVENING(19, 22),
    NIGHT(22, 24);

    val startHour: Int
    val endHour: Int

    constructor(start: Int, end: Int) {
        this.startHour = start
        this.endHour = end
    }
}

data class TripScore(
    val overallScore: Float, // 0-100
    val safetyScore: Float,
    val efficiencyScore: Float,
    val smoothnessScore: Float,
    val legalComplianceScore: Float,
    val events: List<DrivingEvent>,
    val tripStatistics: TripStatistics,
    val riskFactors: List<RiskFactor>
)

data class TripStatistics(
    val totalDistance: Float, // km
    val totalDuration: Long, // milliseconds
    val averageSpeed: Float, // km/h
    val maxSpeed: Float, // km/h
    val speedingDuration: Long, // milliseconds over limit
    val idleTime: Long, // milliseconds
    val fuelEfficiencyScore: Float,
    val nightDrivingPercentage: Float,
    val highRiskRoadPercentage: Float
)

data class RiskFactor(
    val type: RiskFactorType,
    val impact: Float, // -100 to +100
    val frequency: Int,
    val description: String
)

enum class RiskFactorType {
    SPEEDING,
    AGGRESSIVE_ACCELERATION,
    HARD_BRAKING,
    DISTRACTED_DRIVING,
    NIGHT_DRIVING,
    WEATHER_CONDITIONS,
    HIGH_TRAFFIC_AREAS,
    FATIGUE_INDICATORS,
    PHONE_USAGE,
    ROUTE_FAMILIARITY
}

data class DriverProfile(
    val driverId: String,
    val totalMileage: Float,
    val safetyRating: Float, // 0-100
    val riskCategory: RiskCategory,
    val drivingHistory: DrivingHistory,
    val behaviorTrends: BehaviorTrends,
    val preferredRoutes: List<RoutePattern>,
    val drivingHabits: DrivingHabits
)

enum class RiskCategory {
    VERY_LOW, LOW, MODERATE, HIGH, VERY_HIGH
}

data class DrivingHistory(
    val totalTrips: Int,
    val totalDistance: Float,
    val totalDrivingTime: Long,
    val accidentHistory: List<AccidentRecord>,
    val violationHistory: List<ViolationRecord>,
    val averageTripScore: Float,
    val improvementTrend: Float // Positive = improving
)

data class AccidentRecord(
    val timestamp: Long,
    val severity: AccidentSeverity,
    val faultDetermination: FaultDetermination,
    val damageAmount: Float?,
    val location: LocationData,
    val contributingFactors: List<String>
)

enum class AccidentSeverity {
    MINOR, MODERATE, MAJOR, SEVERE
}

enum class FaultDetermination {
    AT_FAULT, NOT_AT_FAULT, PARTIAL_FAULT, DISPUTED, UNKNOWN
}

data class ViolationRecord(
    val timestamp: Long,
    val violationType: ViolationType,
    val fineAmount: Float?,
    val location: LocationData?,
    val detectedByTelematics: Boolean
)

enum class ViolationType {
    SPEEDING, RECKLESS_DRIVING, PHONE_USAGE, RUNNING_RED_LIGHT,
    IMPROPER_LANE_CHANGE, TAILGATING, PARKING_VIOLATION
}

data class BehaviorTrends(
    val speedingTrend: Trend,
    val aggressivenessTrend: Trend,
    val smoothnessTrend: Trend,
    val attentivenessTrend: Trend,
    val overallSafetyTrend: Trend
)

data class Trend(
    val currentValue: Float,
    val thirtyDayChange: Float,
    val sixMonthChange: Float,
    val direction: TrendDirection
)

enum class TrendDirection {
    IMPROVING, STABLE, DECLINING
}

data class RoutePattern(
    val routeId: String,
    val startLocation: LocationData,
    val endLocation: LocationData,
    val frequency: Int,
    val averageTripTime: Long,
    val riskLevel: Float,
    val familiarityScore: Float
)

data class DrivingHabits(
    val preferredDrivingTimes: List<TimeOfDay>,
    val averageTripDistance: Float,
    val weekdayVsWeekendRatio: Float,
    val phoneUsageFrequency: Float,
    val speedingTendency: Float,
    val aggressivenessFactor: Float
)
