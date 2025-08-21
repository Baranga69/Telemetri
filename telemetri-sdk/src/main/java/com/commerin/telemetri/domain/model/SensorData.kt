package com.commerin.telemetri.domain.model

// Enhanced sensor data model for comprehensive telemetry collection
data class SensorData(
    val sensorType: SensorType,
    val values: FloatArray,
    val accuracy: Int,
    val timestamp: Long,
    val eventTimestamp: Long = System.nanoTime(), // High precision timestamp
    val x: Float = if (values.isNotEmpty()) values[0] else 0f,
    val y: Float = if (values.size > 1) values[1] else 0f,
    val z: Float = if (values.size > 2) values[2] else 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SensorData
        return sensorType == other.sensorType &&
               values.contentEquals(other.values) &&
               accuracy == other.accuracy &&
               timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = sensorType.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + accuracy
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

// Comprehensive sensor types for various telemetry use cases
enum class SensorType {
    // Motion sensors
    ACCELEROMETER,           // Device acceleration (m/s²)
    GYROSCOPE,              // Device rotation rate (rad/s)
    MAGNETOMETER,           // Magnetic field strength (µT)
    GRAVITY,                // Gravity vector (m/s²)
    LINEAR_ACCELERATION,    // Acceleration without gravity (m/s²)
    ROTATION_VECTOR,        // Device orientation as rotation vector

    // Position sensors
    GAME_ROTATION_VECTOR,   // Rotation vector without magnetometer
    GEOMAGNETIC_ROTATION_VECTOR, // Rotation vector using magnetometer
    ORIENTATION,            // Device orientation (deprecated but still useful)

    // Environmental sensors
    AMBIENT_TEMPERATURE,    // Ambient air temperature (°C)
    LIGHT,                  // Ambient light level (lx)
    PRESSURE,               // Atmospheric pressure (hPa)
    RELATIVE_HUMIDITY,      // Relative ambient air humidity (%)

    // Motion detection
    SIGNIFICANT_MOTION,     // Significant motion trigger
    STEP_COUNTER,          // Steps taken since last reboot
    STEP_DETECTOR,         // Step detection trigger

    // Advanced sensors
    HEART_RATE,            // Heart rate (bpm)
    PROXIMITY,             // Proximity to object (cm)

    // Device state sensors
    DEVICE_ORIENTATION,    // Device orientation changes
    STATIONARY_DETECT,     // Device stationary detection
    MOTION_DETECT,         // Device motion detection

    // Vehicle-specific sensors (for automotive use cases)
    AUTOMOTIVE_GEAR,       // Current gear
    AUTOMOTIVE_SPEED,      // Vehicle speed
    AUTOMOTIVE_STEERING_ANGLE, // Steering wheel angle

    // Modern Android sensors (API 24+)
    POSE_6DOF,             // 6DOF pose sensor
    HEART_BEAT,            // Heart beat detection
    LOW_LATENCY_OFFBODY_DETECT, // Off-body detection
    MOTION_DETECT_WAKEUP,  // Wake-up motion detection
    STATIONARY_DETECT_WAKEUP, // Wake-up stationary detection

    // Environmental quality sensors
    ACCELEROMETER_UNCALIBRATED, // Uncalibrated accelerometer
    GYROSCOPE_UNCALIBRATED,     // Uncalibrated gyroscope
    MAGNETOMETER_UNCALIBRATED,  // Uncalibrated magnetometer

    // Audio-based sensors
    AUDIO_AMPLITUDE,       // Microphone amplitude level
    AUDIO_FREQUENCY,       // Dominant frequency analysis
    NOISE_LEVEL,           // Environmental noise level
    SOUND_CLASSIFICATION,  // Sound type classification (traffic, nature, etc.)

    // Network and connectivity
    WIFI_SCAN_RESULTS,     // WiFi networks in range
    BLUETOOTH_SCAN_RESULTS, // Bluetooth devices in range
    CELLULAR_INFO,         // Cellular tower information
    NETWORK_QUALITY,       // Network performance metrics

    // Performance monitoring
    CPU_USAGE,             // Real-time CPU usage
    MEMORY_USAGE,          // Memory consumption
    BATTERY_ANALYTICS,     // Detailed battery metrics
    THERMAL_STATE,         // Device thermal conditions

    // Location enhancement
    FUSED_LOCATION,        // Google Play Services fused location
    GNSS_MEASUREMENTS,     // Raw GNSS measurements
    INDOOR_POSITIONING,    // Indoor positioning data

    // Biometric sensors
    FINGERPRINT_SENSOR,    // Fingerprint sensor events
    FACE_UNLOCK_SENSOR,    // Face unlock events

    // Camera-based analysis
    VISUAL_CONTEXT,        // Visual scene analysis
    LIGHT_ESTIMATION,      // Camera-based light estimation
    MOTION_ESTIMATION,     // Visual motion estimation

    // Custom/Unknown sensors
    UNKNOWN
}
