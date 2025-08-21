package com.commerin.telemetri.domain.model

// Derived / processed motion metrics (from SensorData fusion)
data class MotionData(
    val acceleration: Float?,  // m/s² magnitude
    val rotationRate: Float?,  // rad/s magnitude
    val isHardBrake: Boolean,
    val isRapidAcceleration: Boolean,
    val isSharpTurn: Boolean
)
