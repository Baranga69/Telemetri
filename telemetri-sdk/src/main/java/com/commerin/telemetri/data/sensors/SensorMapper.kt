package com.commerin.telemetri.data.sensors

import com.commerin.telemetri.domain.model.SensorData
import com.commerin.telemetri.domain.model.SensorType
import android.hardware.Sensor

object SensorMapper {
    fun map(raw: SensorRaw): SensorData {
        return SensorData(
            sensorType = mapSensorType(raw.type),
            values = raw.values,
            accuracy = 0, // Default accuracy since SensorRaw doesn't have it
            x = raw.values.getOrElse(0) { 0f },
            y = raw.values.getOrElse(1) { 0f },
            z = raw.values.getOrElse(2) { 0f },
            timestamp = raw.timestamp
        )
    }

    private fun mapSensorType(type: Int): SensorType {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> SensorType.ACCELEROMETER
            Sensor.TYPE_GYROSCOPE -> SensorType.GYROSCOPE
            Sensor.TYPE_MAGNETIC_FIELD -> SensorType.MAGNETOMETER
            Sensor.TYPE_GRAVITY -> SensorType.GRAVITY
            Sensor.TYPE_LINEAR_ACCELERATION -> SensorType.LINEAR_ACCELERATION
            Sensor.TYPE_ROTATION_VECTOR -> SensorType.ROTATION_VECTOR
            Sensor.TYPE_GAME_ROTATION_VECTOR -> SensorType.GAME_ROTATION_VECTOR
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> SensorType.GEOMAGNETIC_ROTATION_VECTOR
            Sensor.TYPE_AMBIENT_TEMPERATURE -> SensorType.AMBIENT_TEMPERATURE
            Sensor.TYPE_LIGHT -> SensorType.LIGHT
            Sensor.TYPE_PRESSURE -> SensorType.PRESSURE
            Sensor.TYPE_RELATIVE_HUMIDITY -> SensorType.RELATIVE_HUMIDITY
            Sensor.TYPE_SIGNIFICANT_MOTION -> SensorType.SIGNIFICANT_MOTION
            Sensor.TYPE_STEP_COUNTER -> SensorType.STEP_COUNTER
            Sensor.TYPE_STEP_DETECTOR -> SensorType.STEP_DETECTOR
            Sensor.TYPE_HEART_RATE -> SensorType.HEART_RATE
            Sensor.TYPE_PROXIMITY -> SensorType.PROXIMITY
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> SensorType.ACCELEROMETER_UNCALIBRATED
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> SensorType.GYROSCOPE_UNCALIBRATED
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> SensorType.MAGNETOMETER_UNCALIBRATED
            else -> SensorType.ACCELEROMETER // Default fallback
        }
    }
}