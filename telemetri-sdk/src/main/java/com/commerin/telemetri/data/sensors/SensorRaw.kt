package com.commerin.telemetri.data.sensors

data class SensorRaw(
    val type: Int,
    val values: FloatArray,
    val timestamp: Long
)
