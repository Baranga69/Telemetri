package com.commerin.telemetri.core

import com.commerin.telemetri.domain.model.SensorType
import java.io.Serializable

/**
 * Enhanced telemetry configuration for comprehensive sensor data collection
 * Supports multiple use cases with optimized settings
 */
data class TelemetryConfig(
    // Core telemetry services
    val enableSensorCollection: Boolean = true,
    val enableLocationTracking: Boolean = true,
    val enableAudioTelemetry: Boolean = false,
    val enableNetworkTelemetry: Boolean = false,
    val enablePerformanceMonitoring: Boolean = false,
    val enableMotionAnalysis: Boolean = true,
    val enableDeviceStateMonitoring: Boolean = true,

    // Sensor configuration
    val sensorSamplingRate: SensorSamplingRate = SensorSamplingRate.MEDIUM,
    val enabledSensorTypes: Set<SensorType> = emptySet(),
    val sensorBufferSize: Int = 100,
    val adaptiveSampling: Boolean = true,

    // Location configuration
    val locationUpdateInterval: Long = 10000L, // 10 seconds
    val locationAccuracy: LocationAccuracy = LocationAccuracy.HIGH,
    val enableBackgroundLocation: Boolean = false,
    val geofencingEnabled: Boolean = false,

    // Audio telemetry configuration
    val audioAnalysisEnabled: Boolean = false,
    val audioSamplingRate: Int = 44100,
    val audioAnalysisWindowMs: Int = 1000,
    val noiseThresholdDb: Float = 30.0f,
    val voiceDetectionEnabled: Boolean = false,

    // Network telemetry configuration
    val networkQualityMonitoring: Boolean = false,
    val wifiScanningEnabled: Boolean = false,
    val bluetoothScanningEnabled: Boolean = false,
    val networkLatencyTestEnabled: Boolean = false,
    val bandwidthTestEnabled: Boolean = false,

    // Performance monitoring configuration
    val cpuMonitoringEnabled: Boolean = false,
    val memoryMonitoringEnabled: Boolean = false,
    val batteryAnalyticsEnabled: Boolean = true,
    val thermalMonitoringEnabled: Boolean = false,
    val storageMonitoringEnabled: Boolean = false,

    // Motion analysis configuration
    val activityRecognitionEnabled: Boolean = true,
    val stepCountingEnabled: Boolean = true,
    val fallDetectionEnabled: Boolean = false,
    val gestureRecognitionEnabled: Boolean = false,

    // Data management
    val dataCompressionEnabled: Boolean = true,
    val dataEncryptionEnabled: Boolean = true,
    val localStorageEnabled: Boolean = true,
    val cloudSyncEnabled: Boolean = false,
    val dataRetentionDays: Int = 7,

    // Battery optimization
    val batteryOptimizationEnabled: Boolean = true,
    val adaptiveFrequencyEnabled: Boolean = true,
    val lowPowerModeThreshold: Int = 20, // Battery percentage
    val backgroundProcessingEnabled: Boolean = true,

    // Privacy and security
    val anonymizeData: Boolean = true,
    val gpsAccuracyReduction: Boolean = false,
    val sensitiveDataFiltering: Boolean = true,

    // Advanced features
    val machineLearningEnabled: Boolean = false,
    val edgeComputingEnabled: Boolean = false,
    val realTimeAnalysisEnabled: Boolean = false,
    val predictiveAnalyticsEnabled: Boolean = false
) : Serializable {
    /**
     * Insurance telematics use case - comprehensive behavioral analysis
     * Optimized for accurate driver behavior assessment and risk scoring
     */
    fun insuranceTelematicsUseCase(): TelemetryConfig {
        return TelemetryConfig(
            enableSensorCollection = true,
            enableLocationTracking = true,
            enableAudioTelemetry = false,         // Optional - for distraction detection
            enableNetworkTelemetry = false,
            enablePerformanceMonitoring = true,   // For device usage patterns
            enableMotionAnalysis = true,
            enableDeviceStateMonitoring = true,

            // High precision settings for insurance accuracy
            sensorSamplingRate = SensorSamplingRate.HIGH,
            locationUpdateInterval = 1000L,       // 1 second for precise tracking
            locationAccuracy = LocationAccuracy.VERY_HIGH,

            // Enhanced motion analysis for behavioral detection
            activityRecognitionEnabled = true,
            stepCountingEnabled = false,          // Not needed for driving
            fallDetectionEnabled = false,
            gestureRecognitionEnabled = true,     // For phone usage detection

            // Insurance-specific features
            adaptiveSampling = true,              // Optimize based on driving context
            dataCompressionEnabled = true,
            dataEncryptionEnabled = true,         // Required for privacy compliance
            localStorageEnabled = true,
            cloudSyncEnabled = true,              // For insurance data processing
            dataRetentionDays = 30,               // Insurance typically needs 30-day history

            // Battery optimization for long trips
            batteryOptimizationEnabled = true,
            adaptiveFrequencyEnabled = true,
            lowPowerModeThreshold = 15,

            // Privacy and compliance
            anonymizeData = false,                // Insurance needs identifiable data
            gpsAccuracyReduction = false,         // Need full accuracy for claims
            sensitiveDataFiltering = true,

            // Advanced analytics
            machineLearningEnabled = true,        // For pattern recognition
            edgeComputingEnabled = true,          // Real-time risk assessment
            realTimeAnalysisEnabled = true,       // Immediate feedback
            predictiveAnalyticsEnabled = true     // Risk prediction
        )
    }
}

/**
 * Location accuracy levels for GPS tracking
 */
enum class LocationAccuracy : Serializable {
    LOW,        // ~500m accuracy, low power
    MEDIUM,     // ~100m accuracy, balanced
    HIGH,       // ~5m accuracy, high power
    VERY_HIGH   // <5m accuracy, maximum power
}

/**
 * Sensor sampling rate configurations
 */
enum class SensorSamplingRate : Serializable {
    LOW,         // Lowest frequency, maximum battery saving
    MEDIUM,      // Balanced frequency and power consumption
    HIGH,        // High frequency for detailed analysis
    ULTRA_HIGH,  // Maximum frequency for critical applications
    ADAPTIVE     // Dynamically adjusts based on context
}
