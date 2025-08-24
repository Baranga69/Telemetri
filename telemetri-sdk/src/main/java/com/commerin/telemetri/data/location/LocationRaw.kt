package com.commerin.telemetri.data.location

data class LocationRaw(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val speed: Float?,
    val accuracy: Float?,
    val bearing: Float?,
    val provider: String,
    val timestamp: Long
)
