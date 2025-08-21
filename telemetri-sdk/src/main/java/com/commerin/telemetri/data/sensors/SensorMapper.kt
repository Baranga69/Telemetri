package com.commerin.telemetri.data.sensors

import com.commerin.telemetri.domain.model.SensorData
import com.commerin.telemetri.domain.model.SensorType
import android.hardware.Sensor

object SensorMapper {
    fun map(raw: SensorRaw): SensorData {
        return SensorData(
            sensorType = mapSensorType(raw.type),
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
            else -> SensorType.ACCELEROMETER // Default fallback
        }
    }
}