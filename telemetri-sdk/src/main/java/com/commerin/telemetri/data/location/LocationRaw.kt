package com.commerin.telemetri.data.location

// Raw location data from the location provider - matches LocationData structure
data class LocationRaw(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,      // meters above sea level
    val speed: Float?,          // m/s
    val accuracy: Float?,       // meters
    val bearing: Float?,        // degrees
    val provider: String,       // location provider (gps, network, fused, etc.)
    val timestamp: Long         // epoch millis
)
