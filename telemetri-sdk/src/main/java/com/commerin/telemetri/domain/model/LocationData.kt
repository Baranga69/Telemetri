package com.commerin.telemetri.domain.model

// Location data from FusedLocationProviderClient
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val speed: Float?,          // m/s
    val accuracy: Float?,       // meters
    val bearing: Float?,        // degrees
    val timestamp: Long         // epoch millis
)