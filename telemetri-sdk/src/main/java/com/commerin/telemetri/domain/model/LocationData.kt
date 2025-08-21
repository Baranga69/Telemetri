package com.commerin.telemetri.domain.model

// Location data from FusedLocationProviderClient
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,      // meters above sea level
    val speed: Float?,          // m/s
    val accuracy: Float?,       // meters
    val bearing: Float?,        // degrees
    val provider: String = "fused", // location provider
    val timestamp: Long         // epoch millis
)