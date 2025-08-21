package com.commerin.telemetri.domain.model

// Raw accelerometer / gyroscope / magnetometer readings
data class SensorData(
    val sensorType: SensorType,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
)

enum class SensorType {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER
}
