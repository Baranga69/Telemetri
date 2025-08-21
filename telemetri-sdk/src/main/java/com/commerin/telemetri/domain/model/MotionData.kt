package com.commerin.telemetri.domain.model

// Activity recognition for context-aware telemetry
enum class ActivityType {
    STILL,                  // Device is stationary
    WALKING,                // User is walking
    RUNNING,                // User is running
    CYCLING,                // User is cycling
    IN_VEHICLE,             // User is in a vehicle
    ON_FOOT,                // User is on foot (walking or running)
    TILTING,                // Device is being tilted
    UNKNOWN                 // Activity cannot be determined
}

// Enhanced motion data model for advanced analysis
data class MotionData(
    val accelerationMagnitude: Float,    // Overall acceleration magnitude
    val gyroscopeMagnitude: Float,       // Overall rotation magnitude
    val magneticFieldMagnitude: Float,   // Magnetic field strength
    val linearAcceleration: Triple<Float, Float, Float>, // Linear acceleration (x,y,z)
    val gravity: Triple<Float, Float, Float>,            // Gravity vector (x,y,z)
    val rotationVector: FloatArray,      // Device orientation as quaternion
    val activityType: ActivityType,      // Detected activity
    val confidence: Float,               // Confidence in activity detection
    val stepCount: Int,                  // Current step count
    val stepFrequency: Float,            // Steps per minute
    val timestamp: Long,

    // Legacy properties for backward compatibility
    val acceleration: Float? = accelerationMagnitude,  // m/s² magnitude
    val rotationRate: Float? = gyroscopeMagnitude,     // rad/s magnitude
    val isHardBrake: Boolean = false,
    val isRapidAcceleration: Boolean = accelerationMagnitude > 4.0f,
    val isSharpTurn: Boolean = gyroscopeMagnitude > 2.0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MotionData
        return accelerationMagnitude == other.accelerationMagnitude &&
               gyroscopeMagnitude == other.gyroscopeMagnitude &&
               magneticFieldMagnitude == other.magneticFieldMagnitude &&
               timestamp == other.timestamp &&
               rotationVector.contentEquals(other.rotationVector)
    }

    override fun hashCode(): Int {
        var result = accelerationMagnitude.hashCode()
        result = 31 * result + gyroscopeMagnitude.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + rotationVector.contentHashCode()
        return result
    }
}
