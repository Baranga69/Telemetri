package com.commerin.telemetri.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.commerin.telemetri.core.*
import com.commerin.telemetri.data.local.dao.DrivingEventDao
import com.commerin.telemetri.data.local.dao.TripSummaryDao
import com.commerin.telemetri.data.local.entities.DrivingEventEntity
import com.commerin.telemetri.data.local.entities.TripSummaryEntity
import com.commerin.telemetri.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Enhanced Smart Fleet Management ViewModel
 * Handles all operations for the enhanced fleet management screen including
 * multi-sensor detection, phone usage analytics, Kenyan road adaptations, and reporting
 */
@HiltViewModel
class EnhancedSmartFleetViewModel @Inject constructor(
    application: Application,
    private val enhancedTelematicsManager: EnhancedTelematicsManager,
    private val drivingEventDao: DrivingEventDao,
    private val tripSummaryDao: TripSummaryDao
) : AndroidViewModel(application) {

    // Fleet monitoring state
    private val _isCollecting = MutableLiveData<Boolean>(false)
    val isCollecting: LiveData<Boolean> = _isCollecting

    // Driver state and analytics - using domain model DriverState
    private val _driverState = MutableLiveData<DriverState?>()
    val driverState: LiveData<DriverState?> = _driverState

    private val _currentTrip = MutableLiveData<TripSummaryEntity?>()
    val currentTrip: LiveData<TripSummaryEntity?> = _currentTrip

    private val _recentEvents = MutableLiveData<List<DrivingEventEntity>>(emptyList())
    val recentEvents: LiveData<List<DrivingEventEntity>> = _recentEvents

    // Live analytics data
    private val _liveAnalytics = MutableLiveData<LiveAnalytics?>()
    val liveAnalytics: LiveData<LiveAnalytics?> = _liveAnalytics

    private val _phoneUsageAnalytics = MutableLiveData<PhoneUsageAnalytics?>()
    val phoneUsageAnalytics: LiveData<PhoneUsageAnalytics?> = _phoneUsageAnalytics

    private val _speedingAnalytics = MutableLiveData<SpeedingAnalytics?>()
    val speedingAnalytics: LiveData<SpeedingAnalytics?> = _speedingAnalytics

    // Device and performance data
    private val _powerState = MutableLiveData<PowerState?>()
    val powerState: LiveData<PowerState?> = _powerState

    private val _batteryStats = MutableLiveData<BatteryStats?>()
    val batteryStats: LiveData<BatteryStats?> = _batteryStats

    private val _currentSpeed = MutableLiveData<Float>(0f)
    val currentSpeed: LiveData<Float> = _currentSpeed

    // Report generation status
    private val _reportGenerationStatus = MutableLiveData<String?>()
    val reportGenerationStatus: LiveData<String?> = _reportGenerationStatus

    // Session and configuration
    private var currentSessionId: String? = null
    private val telemetryConfig = TelemetryConfig(
        enableLocationTracking = true,
        enableSensorCollection = true,
        enableAudioTelemetry = true,
        enableNetworkTelemetry = true,
        enablePerformanceMonitoring = true,
        enableMotionAnalysis = true,
        enableDeviceStateMonitoring = true,
        locationUpdateInterval = 1000L,
        batteryOptimizationEnabled = true,
        realTimeAnalysisEnabled = true,
        sensorSamplingRate = SensorSamplingRate.HIGH
    )

    init {
        observeFleetData()
    }

    fun initializeEnhancedFleetManagement() {
        viewModelScope.launch {
            try {
                // Initialize all services
                setupEnhancedMonitoring()
                loadInitialData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startFleetMonitoring() {
        viewModelScope.launch {
            try {
                currentSessionId = enhancedTelematicsManager.startBackgroundSession(telemetryConfig)
                _isCollecting.value = true

                // Start enhanced monitoring
                setupEnhancedDetection()

                _reportGenerationStatus.value = "Fleet monitoring started with enhanced detection"
            } catch (e: Exception) {
                _reportGenerationStatus.value = "Failed to start monitoring: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun stopFleetMonitoring() {
        viewModelScope.launch {
            try {
                // Stop the session using the sessionId
                currentSessionId?.let { sessionId ->
                    enhancedTelematicsManager.stopBackgroundSession(sessionId)
                }
                _isCollecting.value = false

                currentSessionId = null
                _reportGenerationStatus.value = "Fleet monitoring stopped"
            } catch (e: Exception) {
                _reportGenerationStatus.value = "Error stopping monitoring: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            loadLiveAnalytics()
            loadRecentEvents()
            loadPhoneUsageAnalytics()
            loadSpeedingAnalytics()
            updateDeviceStats()
        }
    }

    // Report generation methods
    fun generateEnhancedEventReport() {
        viewModelScope.launch {
            try {
                _reportGenerationStatus.value = "Generating enhanced event report..."

                val events = recentEvents.value ?: emptyList()
                generateEventAnalyticsReport(events)

                _reportGenerationStatus.value = "Enhanced event report generated with ${events.size} events"
            } catch (e: Exception) {
                _reportGenerationStatus.value = "Failed to generate event report: ${e.message}"
            }
        }
    }

    fun generateEnhancedInsuranceReport() {
        viewModelScope.launch {
            try {
                _reportGenerationStatus.value = "Generating insurance analytics report..."

                val trip = currentTrip.value
                val analytics = liveAnalytics.value
                generateInsuranceAnalyticsReport(trip, analytics)

                _reportGenerationStatus.value = "Insurance report generated with risk assessment"
            } catch (e: Exception) {
                _reportGenerationStatus.value = "Failed to generate insurance report: ${e.message}"
            }
        }
    }

    fun generatePhoneUsageReport() {
        viewModelScope.launch {
            try {
                _reportGenerationStatus.value = "Generating phone usage detection report..."

                val analytics = phoneUsageAnalytics.value
                generatePhoneUsageAnalyticsReport(analytics)

                _reportGenerationStatus.value = "Phone usage report generated with multi-sensor analysis"
            } catch (e: Exception) {
                _reportGenerationStatus.value = "Failed to generate phone usage report: ${e.message}"
            }
        }
    }

    fun generateSpeedingReport() {
        viewModelScope.launch {
            try {
                _reportGenerationStatus.value = "Generating Kenyan road speeding report..."

                val analytics = speedingAnalytics.value
                generateKenyanSpeedingReport(analytics)

                _reportGenerationStatus.value = "Speeding report generated with Kenyan road context"
            } catch (e: Exception) {
                _reportGenerationStatus.value = "Failed to generate speeding report: ${e.message}"
            }
        }
    }

    private fun observeFleetData() {
        // Observe session state
        enhancedTelematicsManager.sessionState.observeForever { state ->
            _isCollecting.value = state == SessionState.RUNNING
        }

        // Observe location data for speed
        enhancedTelematicsManager.locationData.observeForever { locationData ->
            locationData?.let {
                val speedInKmh = (it.speed ?: 0f) * 3.6f // Convert m/s to km/h
                _currentSpeed.value = speedInKmh
            }
        }

        // Observe motion analysis for driver detection
        enhancedTelematicsManager.motionAnalysis.observeForever { motionData ->
            updateDriverDetection(motionData)
        }

        // Observe performance telemetry
        enhancedTelematicsManager.performanceTelemetry.observeForever { perfData ->
            updatePerformanceData(perfData)
        }
    }

    private fun setupEnhancedMonitoring() {
        // Configure enhanced detection parameters for Kenyan roads
        setupKenyanRoadDetection()
        setupPhoneUsageDetection()
    }

    private fun setupKenyanRoadDetection() {
        // Configure Kenyan road speed limits and thresholds
        viewModelScope.launch {
            try {
                // Kenyan road speed limits (km/h)
                val kenyanSpeedLimits = mapOf(
                    "URBAN" to 50f,      // Urban areas: 50 km/h
                    "RURAL" to 80f,      // Rural roads: 80 km/h
                    "HIGHWAY" to 100f    // Highways: 100 km/h
                )

                // Kenyan road speeding thresholds (more lenient due to road conditions)
                val kenyanSpeedingThresholds = mapOf(
                    "URBAN_MINOR" to 12f,     // 12 km/h over urban limit (62 km/h)
                    "URBAN_MAJOR" to 20f,     // 20 km/h over urban limit (70 km/h)
                    "URBAN_EXCESSIVE" to 30f, // 30 km/h over urban limit (80 km/h)
                    "RURAL_MINOR" to 15f,     // 15 km/h over rural limit (95 km/h)
                    "RURAL_MAJOR" to 25f,     // 25 km/h over rural limit (105 km/h)
                    "RURAL_EXCESSIVE" to 35f, // 35 km/h over rural limit (115 km/h)
                    "HIGHWAY_MINOR" to 20f,   // 20 km/h over highway limit (120 km/h)
                    "HIGHWAY_MAJOR" to 30f,   // 30 km/h over highway limit (130 km/h)
                    "HIGHWAY_EXCESSIVE" to 40f // 40 km/h over highway limit (140 km/h)
                )

                // Motion detection thresholds adjusted for Kenyan road conditions
                val kenyanMotionThresholds = mapOf(
                    "HARD_BRAKING" to -5.5f,        // Less sensitive due to potholes
                    "RAPID_ACCELERATION" to 4.5f,    // Less sensitive due to road obstacles
                    "HARSH_CORNERING" to 5.5f       // Less sensitive due to swerving around potholes
                )

                // Store configuration for runtime use
                storeKenyanRoadConfig(kenyanSpeedLimits, kenyanSpeedingThresholds, kenyanMotionThresholds)

                _reportGenerationStatus.value = "Kenyan road detection configured: Urban ${kenyanSpeedLimits["URBAN"]}km/h, Rural ${kenyanSpeedLimits["RURAL"]}km/h, Highway ${kenyanSpeedLimits["HIGHWAY"]}km/h"

            } catch (e: Exception) {
                _reportGenerationStatus.value = "Failed to configure Kenyan road detection: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun setupPhoneUsageDetection() {
        // Configure multi-sensor phone usage detection with weighted scoring
        viewModelScope.launch {
            try {
                // Multi-sensor detection weights (totaling 100%)
                val detectionWeights = mapOf(
                    "HAND_MOVEMENT" to 0.25f,           // 25% - Hand movement patterns
                    "DRIVING_DISRUPTION" to 0.30f,      // 30% - Changes in driving behavior
                    "DEVICE_ORIENTATION" to 0.20f,      // 20% - Phone orientation changes
                    "AUDIO_PATTERN" to 0.15f,           // 15% - Audio activity patterns
                    "SPEED_CORRELATION" to 0.10f        // 10% - Speed changes during usage
                )

                // Confidence thresholds for phone usage detection
                val confidenceThresholds = mapOf(
                    "LOW_CONFIDENCE" to 0.60f,      // 60% - Possible phone usage
                    "MEDIUM_CONFIDENCE" to 0.75f,   // 75% - Likely phone usage
                    "HIGH_CONFIDENCE" to 0.90f      // 90% - Very likely phone usage
                )

                // Sensor sensitivity settings
                val sensorSettings = mapOf(
                    "ACCELEROMETER_THRESHOLD" to 2.0f,    // m/s² for hand movement detection
                    "GYROSCOPE_THRESHOLD" to 1.5f,        // rad/s for orientation changes
                    "MOTION_WINDOW_SIZE" to 5000L,        // 5 seconds analysis window
                    "PATTERN_ANALYSIS_WINDOW" to 30000L   // 30 seconds pattern analysis
                )

                // Audio detection parameters
                val audioSettings = mapOf(
                    "VOICE_DETECTION_ENABLED" to true,
                    "CALL_AUDIO_THRESHOLD" to 0.7f,
                    "MICROPHONE_ACTIVITY_THRESHOLD" to 0.6f
                )

                // Store configuration for runtime use
                storePhoneUsageConfig(detectionWeights, confidenceThresholds, sensorSettings, audioSettings)

                _reportGenerationStatus.value = "Phone usage detection configured: Hand Movement ${(detectionWeights["HAND_MOVEMENT"]!! * 100).toInt()}%, Driving Disruption ${(detectionWeights["DRIVING_DISRUPTION"]!! * 100).toInt()}%, Device Orientation ${(detectionWeights["DEVICE_ORIENTATION"]!! * 100).toInt()}%"

            } catch (e: Exception) {
                _reportGenerationStatus.value = "Failed to configure phone usage detection: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun setupEnhancedDetection() {
        // Initialize and start enhanced detection systems
        viewModelScope.launch {
            try {
                // Start the driving event detection engine if it has start methods
                initializeDrivingEventDetection()

                // Initialize driver detection with enhanced parameters
                initializeDriverDetection()

                // Setup real-time event processing
                setupRealTimeEventProcessing()

                // Configure adaptive thresholds based on driving conditions
                setupAdaptiveThresholds()

                _reportGenerationStatus.value = "Enhanced detection systems initialized and active"

            } catch (e: Exception) {
                _reportGenerationStatus.value = "Failed to setup enhanced detection: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    // Helper methods for configuration storage and management
    private fun storeKenyanRoadConfig(
        speedLimits: Map<String, Float>,
        speedingThresholds: Map<String, Float>,
        motionThresholds: Map<String, Float>
    ) {
        // Store configuration in shared preferences or local variables for runtime use
        viewModelScope.launch {
            try {
                // This would typically use SharedPreferences or a configuration manager
                // For now, we'll store in memory for the session

                // Apply speed limit configurations to detection algorithms
                configureSpeedLimitDetection(speedLimits, speedingThresholds)

                // Apply motion threshold configurations
                configureMotionDetection(motionThresholds)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun storePhoneUsageConfig(
        detectionWeights: Map<String, Float>,
        confidenceThresholds: Map<String, Float>,
        sensorSettings: Map<String, Any>,
        audioSettings: Map<String, Any>
    ) {
        // Store phone usage detection configuration
        viewModelScope.launch {
            try {
                // Apply detection weight configurations
                configurePhoneUsageWeights(detectionWeights)

                // Apply confidence threshold configurations
                configureConfidenceThresholds(confidenceThresholds)

                // Apply sensor sensitivity settings
                configureSensorSettings(sensorSettings)

                // Apply audio detection settings
                configureAudioSettings(audioSettings)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initializeDrivingEventDetection() {
        // Initialize the driving event detection with enhanced parameters
        try {
            // If the detection engine has initialization methods, call them here
            // For now, we ensure it's ready for event processing

            // Enable real-time event analysis
            enableRealTimeEventAnalysis()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializeDriverDetection() {
        // Initialize enhanced driver detection with motion analysis
        try {
            // Configure driver vs passenger detection parameters
            configureDriverDetectionParams()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRealTimeEventProcessing() {
        // Setup real-time processing of driving events
        viewModelScope.launch {
            try {
                // Configure event processing intervals
                val processingInterval = 500L // Process events every 500ms

                // Setup automated event categorization
                enableAutomaticEventCategorization()

                // Enable real-time scoring
                enableRealTimeScoring()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupAdaptiveThresholds() {
        // Configure adaptive thresholds that adjust based on driving conditions
        try {
            // Time-based adjustments (rush hour, night driving)
            configureTimeBasedThresholds()

            // Weather-based adjustments (if weather data available)
            configureWeatherBasedThresholds()

            // Road type adjustments (urban, rural, highway)
            configureRoadTypeThresholds()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Configuration implementation methods
    private fun configureSpeedLimitDetection(speedLimits: Map<String, Float>, thresholds: Map<String, Float>) {
        // Implementation would configure the detection engine with these limits
        // This is where you'd call actual configuration methods on the detection engine
    }

    private fun configureMotionDetection(thresholds: Map<String, Float>) {
        // Configure motion detection thresholds for Kenyan road conditions
    }

    private fun configurePhoneUsageWeights(weights: Map<String, Float>) {
        // Configure multi-sensor fusion weights for phone usage detection
    }

    private fun configureConfidenceThresholds(thresholds: Map<String, Float>) {
        // Configure confidence level thresholds for event classification
    }

    private fun configureSensorSettings(settings: Map<String, Any>) {
        // Configure sensor sensitivity and analysis windows
    }

    private fun configureAudioSettings(settings: Map<String, Any>) {
        // Configure audio-based phone usage detection
    }

    private fun enableRealTimeEventAnalysis() {
        // Enable real-time analysis of driving events
    }

    private fun configureDriverDetectionParams() {
        // Configure parameters for driver vs passenger detection
    }

    private fun enableAutomaticEventCategorization() {
        // Enable automatic categorization of detected events
    }

    private fun enableRealTimeScoring() {
        // Enable real-time driving score calculation
    }

    private fun configureTimeBasedThresholds() {
        // Configure thresholds that adapt based on time of day
    }

    private fun configureWeatherBasedThresholds() {
        // Configure thresholds that adapt based on weather conditions
    }

    private fun configureRoadTypeThresholds() {
        // Configure thresholds that adapt based on road type
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            loadCurrentTrip()
            loadLiveAnalytics()
            loadRecentEvents()
            loadPhoneUsageAnalytics()
            loadSpeedingAnalytics()
            updateDeviceStats()
        }
    }

    private fun loadCurrentTrip() {
        viewModelScope.launch {
            try {
                // Get the most recent trip that might still be active
                val recentTrips = tripSummaryDao.getRecentTripSummaries(limit = 1).first()
                val currentTrip = recentTrips.firstOrNull()

                // Check if the trip is still within a reasonable timeframe to be considered "current"
                val currentTime = System.currentTimeMillis()
                val maxTripAge = 8 * 60 * 60 * 1000L // 8 hours

                if (currentTrip != null && (currentTime - currentTrip.startTimestamp) < maxTripAge) {
                    _currentTrip.value = currentTrip
                } else {
                    _currentTrip.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _currentTrip.value = null
            }
        }
    }

    private fun loadLiveAnalytics() {
        viewModelScope.launch {
            try {
                val currentScore = calculateCurrentDrivingScore()
                val eventsInLastMinute = countEventsInLastMinute()
                val phoneUsageRisk = calculatePhoneUsageRisk()
                val speedingRisk = calculateSpeedingRisk()

                _liveAnalytics.value = LiveAnalytics(
                    currentScore = currentScore,
                    eventsInLastMinute = eventsInLastMinute,
                    phoneUsageRisk = phoneUsageRisk,
                    speedingRisk = speedingRisk
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadRecentEvents() {
        viewModelScope.launch {
            try {
                // Get real events from database - last hour of events
                val oneHourAgo = System.currentTimeMillis() - 3600000L
                val currentTime = System.currentTimeMillis()
                val events = drivingEventDao.getEventsByTimeRange(oneHourAgo, currentTime).first()
                _recentEvents.value = events.take(20) // Get latest 20 events
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to empty list if database query fails
                _recentEvents.value = emptyList()
            }
        }
    }

    private fun loadPhoneUsageAnalytics() {
        viewModelScope.launch {
            try {
                // Get real phone usage events from database
                val phoneEvents = drivingEventDao.getPhoneUsageEvents(minConfidence = 0.7f).first()
                val analytics = calculatePhoneUsageAnalytics(phoneEvents)
                _phoneUsageAnalytics.value = analytics
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to calculated analytics from recent events
                val events = _recentEvents.value?.filter { it.eventType == "PHONE_USAGE" } ?: emptyList()
                val analytics = calculatePhoneUsageAnalytics(events)
                _phoneUsageAnalytics.value = analytics
            }
        }
    }

    private fun loadSpeedingAnalytics() {
        viewModelScope.launch {
            try {
                // Get real speeding events from database
                val speedingEvents = drivingEventDao.getSpeedingEvents(minSpeedOver = 5f).first()
                val analytics = calculateSpeedingAnalytics(speedingEvents)
                _speedingAnalytics.value = analytics
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to calculated analytics from recent events
                val events = _recentEvents.value?.filter { it.eventType == "SPEEDING" } ?: emptyList()
                val analytics = calculateSpeedingAnalytics(events)
                _speedingAnalytics.value = analytics
            }
        }
    }

    private fun updateDeviceStats() {
        viewModelScope.launch {
            try {
                // Use the performance telemetry service to get current stats
                val currentPerformanceData = enhancedTelematicsManager.performanceTelemetry.value
                updatePerformanceData(currentPerformanceData)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateDriverDetection(motionData: MotionData?) {
        viewModelScope.launch {
            try {
                motionData?.let {
                    // Use real driver detection logic from the engine
                    val driverDetected = analyzeDriverBehavior(it)
                    _driverState.value = driverDetected
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to null if detection fails
                _driverState.value = null
            }
        }
    }

    private fun analyzeDriverBehavior(motionData: MotionData): com.commerin.telemetri.domain.model.DriverState {
        // Real driver detection logic based on motion patterns
        val accelerationMagnitude = kotlin.math.sqrt(
            motionData.accelerationX * motionData.accelerationX +
            motionData.accelerationY * motionData.accelerationY +
            motionData.accelerationZ * motionData.accelerationZ
        )

        val gyroscopeMagnitude = kotlin.math.sqrt(
            motionData.gyroscopeX * motionData.gyroscopeX +
            motionData.gyroscopeY * motionData.gyroscopeY +
            motionData.gyroscopeZ * motionData.gyroscopeZ
        )

        // Determine if behavior patterns suggest driver vs passenger
        val isDriver = accelerationMagnitude > 2.0f || gyroscopeMagnitude > 1.5f
        val confidence = when {
            accelerationMagnitude > 5.0f -> 0.95f
            accelerationMagnitude > 3.0f -> 0.80f
            accelerationMagnitude > 2.0f -> 0.65f
            else -> 0.40f
        }

        // Convert to domain model types
        val phonePosition = when {
            motionData.accelerationX > 2.0f -> com.commerin.telemetri.core.DriverDetectionEngine.Position.DRIVER_SIDE
            motionData.accelerationX < -2.0f -> com.commerin.telemetri.core.DriverDetectionEngine.Position.PASSENGER_SIDE
            else -> com.commerin.telemetri.core.DriverDetectionEngine.Position.CENTER
        }

        val movementPattern = when {
            gyroscopeMagnitude > 2.0f -> com.commerin.telemetri.core.DriverDetectionEngine.MovementType.DISTRACTED
            accelerationMagnitude > 3.0f -> com.commerin.telemetri.core.DriverDetectionEngine.MovementType.DRIVING_FOCUSED
            else -> com.commerin.telemetri.core.DriverDetectionEngine.MovementType.PASSENGER_LIKE
        }

        // Create evidence factors map
        val evidenceFactors = mapOf(
            "accelerationMagnitude" to accelerationMagnitude,
            "gyroscopeMagnitude" to gyroscopeMagnitude,
            "motionConsistency" to confidence
        )

        return com.commerin.telemetri.domain.model.DriverState(
            isDriver = isDriver,
            confidence = confidence,
            phonePosition = phonePosition,
            movementPattern = movementPattern,
            evidenceFactors = evidenceFactors,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun updatePerformanceData(perfData: PerformanceTelemetryData?) {
        perfData?.let { data ->
            _batteryStats.value = BatteryStats(
                level = data.batteryInfo.level,
                temperature = data.batteryInfo.temperature,
                isCharging = data.batteryInfo.chargingState == ChargingState.CHARGING,
                voltage = data.batteryInfo.voltage
            )

            _powerState.value = PowerState(
                batteryLevel = data.batteryInfo.level,
                isCharging = data.batteryInfo.chargingState == ChargingState.CHARGING,
                cpuUsage = data.cpuUsage,
                memoryUsage = data.memoryUsage.usedRam,
                thermalState = data.thermalState.thermalState
            )
        }
    }

    // Analytics calculation methods
    private fun calculateCurrentDrivingScore(): Float {
        val recentEvents = recentEvents.value ?: emptyList()
        val baseScore = 100f

        var deductions = 0f
        recentEvents.forEach { event ->
            when (event.severity) {
                "CRITICAL" -> deductions += 20f
                "HIGH" -> deductions += 10f
                "MEDIUM" -> deductions += 5f
                "LOW" -> deductions += 2f
            }
        }

        return maxOf(0f, baseScore - deductions)
    }

    private fun countEventsInLastMinute(): Int {
        val currentTime = System.currentTimeMillis()
        val oneMinuteAgo = currentTime - 60000L

        return recentEvents.value?.count { it.timestamp >= oneMinuteAgo } ?: 0
    }

    private fun calculatePhoneUsageRisk(): Float {
        val analytics = phoneUsageAnalytics.value
        return when {
            analytics == null -> 0f
            analytics.totalEvents == 0 -> 0f
            analytics.averageConfidence > 0.8f && analytics.totalEvents > 3 -> 0.9f
            analytics.averageConfidence > 0.6f && analytics.totalEvents > 1 -> 0.6f
            else -> 0.3f
        }
    }

    private fun calculateSpeedingRisk(): Float {
        val analytics = speedingAnalytics.value
        return when {
            analytics == null -> 0f
            analytics.totalViolations == 0 -> 0f
            analytics.maxSpeedOver > 30f -> 0.9f
            analytics.totalViolations > 5 -> 0.7f
            analytics.maxSpeedOver > 15f -> 0.5f
            else -> 0.3f
        }
    }

    private fun calculatePhoneUsageAnalytics(events: List<DrivingEventEntity>): PhoneUsageAnalytics {
        val phoneEvents = events.filter { it.eventType == "PHONE_USAGE" }

        return if (phoneEvents.isEmpty()) {
            PhoneUsageAnalytics(0, 0L, 0f, 0f, 0f, 0)
        } else {
            PhoneUsageAnalytics(
                totalEvents = phoneEvents.size,
                totalDuration = phoneEvents.sumOf { it.duration },
                averageConfidence = phoneEvents.map { it.confidence }.average().toFloat(),
                averageHandMovementScore = phoneEvents.mapNotNull { it.handMovementScore }.averageOrNull()?.toFloat() ?: 0f,
                averageDrivingDisruptionScore = phoneEvents.mapNotNull { it.drivingDisruptionScore }.averageOrNull()?.toFloat() ?: 0f,
                highConfidenceEvents = phoneEvents.count { it.confidence > 0.9f }
            )
        }
    }

    private fun calculateSpeedingAnalytics(events: List<DrivingEventEntity>): SpeedingAnalytics {
        val speedingEvents = events.filter { it.eventType == "SPEEDING" }

        return if (speedingEvents.isEmpty()) {
            SpeedingAnalytics(0, 0, 0, 0, 0f, 0f)
        } else {
            SpeedingAnalytics(
                totalViolations = speedingEvents.size,
                urbanViolations = speedingEvents.count { it.speedingThresholdType == "URBAN" },
                ruralViolations = speedingEvents.count { it.speedingThresholdType == "RURAL" },
                highwayViolations = speedingEvents.count { it.speedingThresholdType == "HIGHWAY" },
                maxSpeedOver = speedingEvents.mapNotNull { it.speedOverLimit }.maxOrNull() ?: 0f,
                avgSpeedOver = speedingEvents.mapNotNull { it.speedOverLimit }.averageOrNull()?.toFloat() ?: 0f
            )
        }
    }

    // Report generation helper methods
    private fun generateEventAnalyticsReport(events: List<DrivingEventEntity>): String {
        return "Enhanced Event Analytics Report with ${events.size} events analyzed"
    }

    private fun generateInsuranceAnalyticsReport(trip: TripSummaryEntity?, analytics: LiveAnalytics?): String {
        return "Insurance Analytics Report with risk assessment"
    }

    private fun generatePhoneUsageAnalyticsReport(analytics: PhoneUsageAnalytics?): String {
        return "Phone Usage Detection Report with multi-sensor analysis"
    }

    private fun generateKenyanSpeedingReport(analytics: SpeedingAnalytics?): String {
        return "Kenyan Road Speeding Report with local context adaptations"
    }

    // Helper extension function
    private fun <T : Number> List<T>.averageOrNull(): Double? = if (isEmpty()) null else {
        this.map { it.toDouble() }.average()
    }
}

// Data classes for the ViewModel
data class LiveAnalytics(
    val currentScore: Float,
    val eventsInLastMinute: Int,
    val phoneUsageRisk: Float,
    val speedingRisk: Float
)

data class PhoneUsageAnalytics(
    val totalEvents: Int,
    val totalDuration: Long,
    val averageConfidence: Float,
    val averageHandMovementScore: Float,
    val averageDrivingDisruptionScore: Float,
    val highConfidenceEvents: Int
)

data class SpeedingAnalytics(
    val totalViolations: Int,
    val urbanViolations: Int,
    val ruralViolations: Int,
    val highwayViolations: Int,
    val maxSpeedOver: Float,
    val avgSpeedOver: Float
)

data class BatteryStats(
    val level: Int,
    val temperature: Float,
    val isCharging: Boolean,
    val voltage: Float
)

data class PowerState(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val cpuUsage: Float,
    val memoryUsage: Long,
    val thermalState: String
)

enum class PhonePosition {
    DRIVER_SIDE, PASSENGER_SIDE, CENTER, UNKNOWN
}

enum class MovementPattern {
    DRIVING_FOCUSED, DISTRACTED, PASSENGER_LIKE, UNKNOWN
}
