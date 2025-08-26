package com.commerin.telemetri.core

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.*
import kotlinx.coroutines.*

class TelemetriManager private constructor(val context: Context) {
    companion object {
        private const val TAG = "TelemetriManager"

        @Volatile
        private var INSTANCE: TelemetriManager? = null

        fun getInstance(context: Context): TelemetriManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TelemetriManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Core telemetry services
    private val sensorService = SensorService(context)
    private val locationService = OptimizedLocationService(context)
    private val motionAnalysisEngine = MotionAnalysisEngine(context)
    private val audioTelemetryService = AudioTelemetryService(context)
    private val networkTelemetryService = NetworkTelemetryService(context)
    private val networkSpeedTestService = NetworkSpeedTestService()
    private val performanceTelemetryService = PerformanceTelemetryService(context)
    private val deviceStateService = DeviceStateService(context)

    // Power optimization and adaptive sampling
    private val adaptivePowerManager = AdaptivePowerManager(context)

    // Telemetry configuration
    private var telemetryConfig = TelemetryConfig()

    // Current adaptive strategy
    private var currentSamplingStrategy: AdaptivePowerManager.SamplingStrategy? = null

    // Comprehensive telemetry data streams
    private val _comprehensiveTelemetry = MediatorLiveData<ComprehensiveTelemetryEvent>()
    val comprehensiveTelemetry: LiveData<ComprehensiveTelemetryEvent> = _comprehensiveTelemetry

    // Individual data streams for specific use cases
    private val _sensorData = MutableLiveData<List<SensorData>>()
    val sensorData: LiveData<List<SensorData>> = _sensorData

    private val _locationData = MutableLiveData<LocationData>()
    val locationData: LiveData<LocationData> = _locationData

    private val _audioTelemetry = MutableLiveData<AudioTelemetryData>()
    val audioTelemetry: LiveData<AudioTelemetryData> = _audioTelemetry

    private val _networkTelemetry = MutableLiveData<NetworkTelemetryData>()
    val networkTelemetry: LiveData<NetworkTelemetryData> = _networkTelemetry

    private val _speedTestResult = MutableLiveData<SpeedTestResult>()
    val speedTestResult: LiveData<SpeedTestResult> = _speedTestResult

    private val _performanceTelemetry = MutableLiveData<PerformanceTelemetryData>()
    val performanceTelemetry: LiveData<PerformanceTelemetryData> = _performanceTelemetry

    private val _motionAnalysis = MutableLiveData<MotionData>()
    val motionAnalysis: LiveData<MotionData> = _motionAnalysis

    // Session management
    private var currentSessionId = generateSessionId()
    private var isCollecting = false

    // Data aggregation
    private val sensorDataBuffer = mutableListOf<SensorData>()
    private var lastLocationData: LocationData? = null
    private var lastAudioData: AudioTelemetryData? = null
    private var lastNetworkData: NetworkTelemetryData? = null
    private var lastPerformanceData: PerformanceTelemetryData? = null
    private var lastMotionData: MotionData? = null
    private var lastDeviceStateData: DeviceStateData? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        setupDataObservers()
    }

    /**
     * Start comprehensive telemetry data collection with enhanced capabilities
     */
    fun startTelemetryCollection(config: TelemetryConfig = TelemetryConfig()) {
        if (isCollecting) {
            Log.d(TAG, "Telemetry collection already running")
            return
        }

        this.telemetryConfig = config
        isCollecting = true
        currentSessionId = generateSessionId()

        Log.d(TAG, "Starting comprehensive telemetry collection with session: $currentSessionId")

        // Start adaptive power management first
        if (config.batteryOptimizationEnabled) {
            startAdaptivePowerManagement()
        }

        // Start core services based on configuration
        if (config.enableSensorCollection) {
            startSensorCollection()
        }

        if (config.enableLocationTracking) {
            startLocationTracking()
        }

        if (config.enableAudioTelemetry) {
            startAudioTelemetry()
        }

        if (config.enableNetworkTelemetry) {
            startNetworkTelemetry()
        }

        if (config.enablePerformanceMonitoring) {
            startPerformanceMonitoring()
        }

        if (config.enableMotionAnalysis) {
            startMotionAnalysis()
        }

        if (config.enableDeviceStateMonitoring) {
            startDeviceStateMonitoring()
        }

        Log.d(TAG, "Comprehensive telemetry collection started successfully")
    }

    /**
     * Stop all telemetry data collection
     */
    fun stopTelemetryCollection() {
        if (!isCollecting) {
            Log.d(TAG, "Telemetry collection not running")
            return
        }

        isCollecting = false

        Log.d(TAG, "Stopping comprehensive telemetry collection")

        // Stop adaptive power management
        adaptivePowerManager.stopAdaptivePowerManagement()

        // Stop all services regardless of config to ensure clean state
        sensorService.stop()
        locationService.stopLocationUpdates()
        audioTelemetryService.stopAudioTelemetry()
        networkTelemetryService.stopNetworkMonitoring()
        networkSpeedTestService.stopSpeedTest()
        performanceTelemetryService.stopPerformanceMonitoring()
        motionAnalysisEngine.stopAnalysis()
        deviceStateService.stopMonitoring()

        // Clear data buffers
        sensorDataBuffer.clear()

        Log.d(TAG, "Comprehensive telemetry collection stopped")
    }

    /**
     * Configure telemetry collection settings for specific use cases
     */
    fun configureTelemetry(config: TelemetryConfig) {
        this.telemetryConfig = config

        if (isCollecting) {
            // Restart with new configuration
            stopTelemetryCollection()
            startTelemetryCollection(config)
        }
    }

    /**
     * Get current telemetry configuration
     */
    fun getCurrentConfig(): TelemetryConfig = telemetryConfig

    /**
     * Create use case specific configurations optimized for minimal permissions
     */
    object ConfigPresets {
        /**
         * Automotive use case - optimized for vehicle speed and navigation
         * Only uses: GPS, Motion sensors (accelerometer/gyroscope), Basic device state
         * Excludes: Microphone, Network monitoring, Performance monitoring
         */
        fun automotiveUseCase(): TelemetryConfig {
            return TelemetryConfig(
                enableSensorCollection = true,        // Need accelerometer/gyroscope for motion
                enableLocationTracking = true,        // Essential for GPS speed and navigation
                enableAudioTelemetry = false,         // NOT needed for speed - removes microphone permission
                enableNetworkTelemetry = false,       // NOT needed for basic speed tracking
                enablePerformanceMonitoring = false,  // NOT needed for speed
                enableMotionAnalysis = true,          // Essential for sensor-based speed calculation
                enableDeviceStateMonitoring = true,   // Basic device state for context
                sensorSamplingRate = SensorSamplingRate.HIGH, // High precision for accurate speed
                locationUpdateInterval = 1000L,       // 1 second for responsive speed updates
                audioAnalysisEnabled = false,         // No audio analysis needed
                networkQualityMonitoring = false,     // No network monitoring needed
                batteryOptimizationEnabled = true     // Optimize battery for longer trips
            )
        }

        /**
         * Fitness tracking use case - optimized for activity detection and health metrics
         * Only uses: Motion sensors, GPS, Device state
         * Excludes: Microphone, Network monitoring
         */
        fun fitnessTrackingUseCase(): TelemetryConfig {
            return TelemetryConfig(
                enableSensorCollection = true,        // Essential for step counting and activity detection
                enableLocationTracking = true,        // Essential for distance and route tracking
                enableAudioTelemetry = false,         // NOT needed for fitness - removes microphone permission
                enableNetworkTelemetry = false,       // NOT needed for basic fitness tracking
                enablePerformanceMonitoring = true,   // Useful for battery life during workouts
                enableMotionAnalysis = true,          // Essential for activity type detection
                enableDeviceStateMonitoring = true,   // Important for battery monitoring during workouts
                sensorSamplingRate = SensorSamplingRate.HIGH, // High precision for accurate step counting
                locationUpdateInterval = 5000L,       // 5 seconds - balance accuracy vs battery
                audioAnalysisEnabled = false,         // No audio analysis needed
                networkQualityMonitoring = false,     // No network monitoring needed
                batteryOptimizationEnabled = true     // Important for long workouts
            )
        }

        /**
         * Environmental monitoring use case - optimized for ambient data collection
         * Uses: Environmental sensors, Audio for noise monitoring, GPS for location context
         * Excludes: Motion analysis (not needed for stationary monitoring)
         */
        fun environmentalMonitoringUseCase(): TelemetryConfig {
            return TelemetryConfig(
                enableSensorCollection = true,        // Essential for environmental sensors (temp, light, pressure)
                enableLocationTracking = true,        // Important for location context of environmental data
                enableAudioTelemetry = true,          // Essential for noise level monitoring
                enableNetworkTelemetry = true,        // Useful for data upload and connectivity context
                enablePerformanceMonitoring = false,  // NOT needed for environmental monitoring
                enableMotionAnalysis = false,         // NOT needed - usually stationary monitoring
                enableDeviceStateMonitoring = false,  // NOT critical for environmental data
                sensorSamplingRate = SensorSamplingRate.MEDIUM, // Medium precision sufficient
                locationUpdateInterval = 30000L,      // 30 seconds - slow updates for stationary monitoring
                audioAnalysisEnabled = true,          // Essential for noise classification
                networkQualityMonitoring = true,      // Useful for data transmission quality
                batteryOptimizationEnabled = true     // Important for long-term monitoring
            )
        }

        /**
         * Security monitoring use case - comprehensive monitoring for security purposes
         * Uses: All available sensors for maximum security coverage
         * Note: This is the only use case that requires all permissions
         */
        fun securityMonitoringUseCase(): TelemetryConfig {
            return TelemetryConfig(
                enableSensorCollection = true,        // Essential for detecting device tampering
                enableLocationTracking = true,        // Essential for theft detection and tracking
                enableAudioTelemetry = true,          // Important for detecting unauthorized access/sounds
                enableNetworkTelemetry = true,        // Important for detecting network intrusions
                enablePerformanceMonitoring = true,   // Important for detecting malicious software
                enableMotionAnalysis = true,          // Essential for detecting theft/unauthorized movement
                enableDeviceStateMonitoring = true,   // Essential for comprehensive security monitoring
                sensorSamplingRate = SensorSamplingRate.ULTRA_HIGH, // Maximum precision for security
                locationUpdateInterval = 500L,        // 0.5 seconds - rapid updates for security
                audioAnalysisEnabled = true,          // Important for security event detection
                networkQualityMonitoring = true,      // Important for detecting network attacks
                batteryOptimizationEnabled = false    // Security takes priority over battery
            )
        }

        /**
         * Battery saver use case - minimal monitoring for maximum battery life
         * Only uses: Basic sensors for essential monitoring
         * Excludes: GPS, Microphone, Network monitoring, Performance monitoring
         */
        fun batterySaverUseCase(): TelemetryConfig {
            return TelemetryConfig(
                enableSensorCollection = true,        // Minimal sensors only
                enableLocationTracking = false,       // GPS disabled to save battery
                enableAudioTelemetry = false,         // Microphone disabled to save battery
                enableNetworkTelemetry = false,       // Network monitoring disabled
                enablePerformanceMonitoring = false,  // Performance monitoring disabled
                enableMotionAnalysis = false,         // Motion analysis disabled
                enableDeviceStateMonitoring = true,   // Only basic device state for battery monitoring
                sensorSamplingRate = SensorSamplingRate.LOW, // Lowest sampling rate
                locationUpdateInterval = 300000L,     // 5 minutes - very infrequent updates
                audioAnalysisEnabled = false,         // No audio analysis
                networkQualityMonitoring = false,     // No network monitoring
                batteryOptimizationEnabled = true     // Maximum battery optimization
            )
        }

        /**
         * Network diagnostics use case - optimized for network performance testing
         * Only uses: Network monitoring, basic device state, minimal location for context
         * Excludes: Motion sensors, Microphone, Performance monitoring
         */
        fun networkDiagnosticsUseCase(): TelemetryConfig {
            return TelemetryConfig(
                enableSensorCollection = false,       // NOT needed for network diagnostics
                enableLocationTracking = true,        // Minimal location for network context
                enableAudioTelemetry = false,         // NOT needed for network testing
                enableNetworkTelemetry = true,        // Essential for network diagnostics
                enablePerformanceMonitoring = false,  // NOT needed for network testing
                enableMotionAnalysis = false,         // NOT needed for network testing
                enableDeviceStateMonitoring = true,   // Basic device state for context
                sensorSamplingRate = SensorSamplingRate.LOW, // Minimal sensor usage
                locationUpdateInterval = 60000L,      // 1 minute - just for general location context
                audioAnalysisEnabled = false,         // No audio analysis needed
                networkQualityMonitoring = true,      // Essential for network diagnostics
                batteryOptimizationEnabled = true     // Optimize battery during network tests
            )
        }
    }

    private fun setupDataObservers() {
        // Observe sensor data
        sensorService.sensorData.observeForever { sensorData ->
            sensorDataBuffer.add(sensorData)

            // Keep buffer size manageable - use removeAt(0) for API 21+ compatibility
            if (sensorDataBuffer.size > 100) {
                sensorDataBuffer.removeAt(0)
            }

            _sensorData.postValue(sensorDataBuffer.toList())
            aggregateComprehensiveTelemetry()
        }

        // Observe location data - fix type inference
        locationService.locationData.observeForever { locationData: LocationData ->
            lastLocationData = locationData
            _locationData.postValue(locationData)
            aggregateComprehensiveTelemetry()
        }

        // Observe audio telemetry
        audioTelemetryService.audioData.observeForever { audioData ->
            lastAudioData = audioData
            _audioTelemetry.postValue(audioData)
            aggregateComprehensiveTelemetry()
        }

        // Observe network telemetry
        networkTelemetryService.networkData.observeForever { networkData ->
            lastNetworkData = networkData
            _networkTelemetry.postValue(networkData)
            aggregateComprehensiveTelemetry()
        }

        // Observe speed test results
        networkSpeedTestService.speedTestResult.observeForever { speedTestResult ->
            _speedTestResult.postValue(speedTestResult)
        }

        // Observe performance telemetry
        performanceTelemetryService.performanceData.observeForever { performanceData ->
            lastPerformanceData = performanceData
            _performanceTelemetry.postValue(performanceData)
            aggregateComprehensiveTelemetry()
        }

        // Observe motion analysis - fix type inference
        motionAnalysisEngine.motionData.observeForever { motionData: MotionData ->
            lastMotionData = motionData
            _motionAnalysis.postValue(motionData)
            aggregateComprehensiveTelemetry()
        }

        // Observe device state - fix property name and type inference
        deviceStateService.deviceState.observeForever { deviceStateData: DeviceStateData ->
            lastDeviceStateData = deviceStateData
            aggregateComprehensiveTelemetry()
        }
    }

    private fun aggregateComprehensiveTelemetry() {
        scope.launch {
            try {
                val environmentalData = createEnvironmentalData()
                val userContextData = createUserContextData()

                val comprehensiveTelemetry = ComprehensiveTelemetryEvent(
                    eventId = generateEventId(),
                    location = lastLocationData,
                    motion = lastMotionData,
                    sensors = sensorDataBuffer.takeLast(10), // Last 10 sensor readings
                    deviceState = lastDeviceStateData,
                    environmental = environmentalData,
                    userContext = userContextData,
                    timestamp = System.currentTimeMillis(),
                    sessionId = currentSessionId,
                    eventType = determineEventType(),
                    metadata = createMetadata()
                )

                _comprehensiveTelemetry.postValue(comprehensiveTelemetry)
            } catch (e: Exception) {
                Log.e(TAG, "Error aggregating comprehensive telemetry", e)
            }
        }
    }

    private fun createEnvironmentalData(): EnvironmentalData? {
        val audioData = lastAudioData
        val sensorData = sensorDataBuffer.takeLast(5)

        // Extract environmental sensors from recent sensor data
        val tempSensor = sensorData.find { it.sensorType == SensorType.AMBIENT_TEMPERATURE }
        val lightSensor = sensorData.find { it.sensorType == SensorType.LIGHT }
        val pressureSensor = sensorData.find { it.sensorType == SensorType.PRESSURE }
        val humiditySensor = sensorData.find { it.sensorType == SensorType.RELATIVE_HUMIDITY }
        val proximitySensor = sensorData.find { it.sensorType == SensorType.PROXIMITY }

        return if (audioData != null || tempSensor != null || lightSensor != null) {
            EnvironmentalData(
                ambientTemperature = tempSensor?.values?.get(0) ?: 25f,
                lightLevel = lightSensor?.values?.get(0) ?: 0f,
                pressure = pressureSensor?.values?.get(0) ?: 1013.25f,
                humidity = humiditySensor?.values?.get(0) ?: 50f,
                proximityDistance = proximitySensor?.values?.get(0) ?: 0f,
                noiseLevel = audioData?.decibels ?: 0f,
                uvIndex = 0f, // Would need specific UV sensor
                airQuality = 0f, // Would need specific air quality sensor
                timestamp = System.currentTimeMillis()
            )
        } else null
    }

    private fun createUserContextData(): UserContextData? {
        val motionData = lastMotionData
        return motionData?.let {
            UserContextData(
                activityType = it.activityType,
                confidence = it.confidence,
                locationContext = determineLocationContext(),
                deviceUsagePattern = determineDeviceUsagePattern(),
                timeOfDay = getCurrentTimeOfDay(),
                dayOfWeek = getCurrentDayOfWeek(),
                timestamp = System.currentTimeMillis()
            )
        }
    }

    private fun determineLocationContext(): LocationContext {
        // This would be enhanced with actual location analysis
        return LocationContext.UNKNOWN
    }

    private fun determineDeviceUsagePattern(): String {
        val performanceData = lastPerformanceData
        return when {
            performanceData?.cpuUsage ?: 0f > 80f -> "Heavy Usage"
            performanceData?.cpuUsage ?: 0f > 50f -> "Moderate Usage"
            else -> "Light Usage"
        }
    }

    private fun getCurrentTimeOfDay(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..11 -> "Morning"
            in 12..17 -> "Afternoon"
            in 18..21 -> "Evening"
            else -> "Night"
        }
    }

    private fun getCurrentDayOfWeek(): String {
        val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            java.util.Calendar.SUNDAY -> "Sunday"
            java.util.Calendar.MONDAY -> "Monday"
            java.util.Calendar.TUESDAY -> "Tuesday"
            java.util.Calendar.WEDNESDAY -> "Wednesday"
            java.util.Calendar.THURSDAY -> "Thursday"
            java.util.Calendar.FRIDAY -> "Friday"
            java.util.Calendar.SATURDAY -> "Saturday"
            else -> "Unknown"
        }
    }

    private fun determineEventType(): TelemetryEventType {
        val motionData = lastMotionData
        val audioData = lastAudioData
        val networkData = lastNetworkData

        return when {
            motionData?.activityType == ActivityType.IN_VEHICLE -> TelemetryEventType.VEHICLE_TELEMETRY
            motionData?.activityType in listOf(ActivityType.WALKING, ActivityType.RUNNING) -> TelemetryEventType.FITNESS_TRACKING
            audioData?.decibels ?: 0f > 70f -> TelemetryEventType.ENVIRONMENTAL_MONITORING
            networkData?.networkType == NetworkType.DISCONNECTED -> TelemetryEventType.CONNECTIVITY_CHANGE
            else -> TelemetryEventType.GENERAL_TELEMETRY
        }
    }

    private fun createMetadata(): Map<String, Any> {
        return mapOf(
            "sdkVersion" to "1.0.0",
            "configHash" to telemetryConfig.hashCode(),
            "bufferSize" to sensorDataBuffer.size,
            "servicesActive" to getActiveServicesCount(),
            "dataQuality" to assessDataQuality()
        )
    }

    private fun getActiveServicesCount(): Int {
        var count = 0
        if (telemetryConfig.enableSensorCollection) count++
        if (telemetryConfig.enableLocationTracking) count++
        if (telemetryConfig.enableAudioTelemetry) count++
        if (telemetryConfig.enableNetworkTelemetry) count++
        if (telemetryConfig.enablePerformanceMonitoring) count++
        if (telemetryConfig.enableMotionAnalysis) count++
        if (telemetryConfig.enableDeviceStateMonitoring) count++
        return count
    }

    private fun assessDataQuality(): String {
        val hasLocation = lastLocationData != null
        val hasSensors = sensorDataBuffer.isNotEmpty()
        val hasNetwork = lastNetworkData != null

        return when {
            hasLocation && hasSensors && hasNetwork -> "High"
            (hasLocation && hasSensors) || (hasSensors && hasNetwork) -> "Medium"
            hasSensors -> "Low"
            else -> "Poor"
        }
    }

    // Service startup methods
    private fun startSensorCollection() {
        sensorService.start()
    }

    private fun startLocationTracking() {
        locationService.startLocationUpdates()
    }

    private fun startAudioTelemetry() {
        audioTelemetryService.startAudioTelemetry()
    }

    private fun startNetworkTelemetry() {
        networkTelemetryService.startNetworkMonitoring()
    }

    private fun startPerformanceMonitoring() {
        performanceTelemetryService.startPerformanceMonitoring()
    }

    private fun startMotionAnalysis() {
        motionAnalysisEngine.startAnalysis()
    }

    private fun startDeviceStateMonitoring() {
        deviceStateService.startMonitoring()
    }

    private fun startAdaptivePowerManagement() {
        adaptivePowerManager.startAdaptivePowerManagement()

        // Observe power state changes
        adaptivePowerManager.powerState.observeForever { powerState ->
            Log.d(TAG, "Power state changed: ${powerState.powerMode}, Battery: ${powerState.batteryLevel}%")
            handlePowerStateChange(powerState)
        }

        // Observe sampling strategy changes
        adaptivePowerManager.samplingStrategy.observeForever { strategy ->
            Log.d(TAG, "Sampling strategy updated: ${strategy.adaptiveReason}")
            currentSamplingStrategy = strategy
            applySamplingStrategy(strategy)
        }

        // Observe driving context changes
        adaptivePowerManager.drivingContext.observeForever { context ->
            Log.d(TAG, "Driving context changed: $context")
            handleDrivingContextChange(context)
        }

        // Feed location data to power manager for context analysis
        locationService.locationData.observeForever { locationData ->
            adaptivePowerManager.updateLocationData(locationData)
        }
    }

    private fun handlePowerStateChange(powerState: AdaptivePowerManager.PowerState) {
        when (powerState.powerMode) {
            AdaptivePowerManager.PowerMode.CRITICAL_BATTERY -> {
                Log.w(TAG, "Critical battery level detected - switching to emergency mode")
                // Disable non-essential services
                if (telemetryConfig.enableAudioTelemetry) {
                    audioTelemetryService.stopAudioTelemetry()
                }
                if (telemetryConfig.enableNetworkTelemetry) {
                    networkTelemetryService.stopNetworkMonitoring()
                }
                if (telemetryConfig.enablePerformanceMonitoring) {
                    performanceTelemetryService.stopPerformanceMonitoring()
                }
            }

            AdaptivePowerManager.PowerMode.DEEP_SLEEP -> {
                Log.i(TAG, "Vehicle parked - entering deep sleep mode")
                // Reduce all services to minimal operation
                reduceServicesForParkingMode()
            }

            AdaptivePowerManager.PowerMode.PERFORMANCE -> {
                Log.i(TAG, "Optimal conditions detected - enabling performance mode")
                // Re-enable services if they were disabled
                restoreFullServices()
            }

            else -> {
                // Handle BALANCED and BATTERY_SAVER modes
                Log.d(TAG, "Adjusting services for power mode: ${powerState.powerMode}")
            }
        }
    }

    private fun handleDrivingContextChange(context: AdaptivePowerManager.DrivingContext) {
        when (context) {
            AdaptivePowerManager.DrivingContext.PARKED -> {
                Log.i(TAG, "Vehicle detected as parked")
                // Could trigger additional parking-specific logic
            }

            AdaptivePowerManager.DrivingContext.HIGHWAY_DRIVING -> {
                Log.i(TAG, "Highway driving detected - optimizing for sustained high speed")
                // Could reduce motion analysis frequency since highway driving is more predictable
            }

            AdaptivePowerManager.DrivingContext.CITY_DRIVING,
            AdaptivePowerManager.DrivingContext.STOP_AND_GO -> {
                Log.i(TAG, "City/stop-and-go driving detected - maintaining high precision")
                // Maintain full precision for unpredictable city driving
            }

            else -> {
                Log.d(TAG, "Driving context: $context")
            }
        }
    }

    private fun applySamplingStrategy(strategy: AdaptivePowerManager.SamplingStrategy) {
        // Apply location interval changes
        if (strategy.locationInterval != telemetryConfig.locationUpdateInterval) {
            locationService.updateLocationInterval(strategy.locationInterval)
        }

        // Apply sensor rate changes
        if (strategy.sensorRate != telemetryConfig.sensorSamplingRate) {
            sensorService.updateSamplingRate(strategy.sensorRate)
            motionAnalysisEngine.updateSamplingRate(strategy.sensorRate)
        }

        // Apply sensor filtering
        sensorService.updateEnabledSensors(strategy.enabledSensors)

        // Apply background processing changes
        if (!strategy.backgroundProcessing && telemetryConfig.backgroundProcessingEnabled) {
            // Reduce background processing
            scope.launch {
                delay(1000) // Small delay between operations
            }
        }
    }

    private fun reduceServicesForParkingMode() {
        // Temporarily stop non-essential services while parked
        if (telemetryConfig.enableAudioTelemetry) {
            audioTelemetryService.pauseForPowerSaving()
        }
        if (telemetryConfig.enableNetworkTelemetry) {
            networkTelemetryService.pauseForPowerSaving()
        }
        if (telemetryConfig.enablePerformanceMonitoring) {
            performanceTelemetryService.pauseForPowerSaving()
        }

        // Keep minimal motion detection for movement detection
        motionAnalysisEngine.enableParkingMode()
    }

    private fun restoreFullServices() {
        // Restore services when optimal conditions return
        if (telemetryConfig.enableAudioTelemetry) {
            audioTelemetryService.resumeFromPowerSaving()
        }
        if (telemetryConfig.enableNetworkTelemetry) {
            networkTelemetryService.resumeFromPowerSaving()
        }
        if (telemetryConfig.enablePerformanceMonitoring) {
            performanceTelemetryService.resumeFromPowerSaving()
        }

        motionAnalysisEngine.disableParkingMode()
    }

    // Network speed test methods
    fun startNetworkSpeedTest() {
        networkSpeedTestService.startSpeedTest()
    }

    fun stopNetworkSpeedTest() {
        networkSpeedTestService.stopSpeedTest()
    }

    // Utility methods
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000, 9999)}"
    }

    private fun generateEventId(): String {
        return "event_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(100, 999)}"
    }

    fun cleanup() {
        stopTelemetryCollection()
        adaptivePowerManager.stopAdaptivePowerManagement()
        audioTelemetryService.cleanup()
        networkTelemetryService.cleanup()
        networkSpeedTestService.cleanup()
        performanceTelemetryService.cleanup()
        scope.cancel()
    }

    // =====================================
    // Power Optimization Public APIs
    // =====================================

    /**
     * Get current power state including battery level, charging status, and power mode
     */
    fun getCurrentPowerState(): LiveData<AdaptivePowerManager.PowerState> = adaptivePowerManager.powerState

    /**
     * Get current sampling strategy being applied
     */
    fun getCurrentSamplingStrategy(): AdaptivePowerManager.SamplingStrategy? = currentSamplingStrategy

    /**
     * Get current driving context (parked, city, highway, etc.)
     */
    fun getCurrentDrivingContext(): LiveData<AdaptivePowerManager.DrivingContext> = adaptivePowerManager.drivingContext

    /**
     * Get power optimization recommendations for the user
     */
    fun getPowerOptimizationRecommendations(): List<String> {
        return adaptivePowerManager.getPowerOptimizationRecommendations()
    }

    /**
     * Force a specific power mode (overrides automatic detection)
     * Use with caution - automatic mode is usually better
     */
    fun forcePowerMode(mode: AdaptivePowerManager.PowerMode) {
        val currentState = adaptivePowerManager.powerState.value ?: return
        val forcedState = currentState.copy(powerMode = mode)
        handlePowerStateChange(forcedState)
    }

    /**
     * Get estimated battery life based on current usage patterns
     */
    fun getEstimatedBatteryLife(): Long {
        return adaptivePowerManager.powerState.value?.estimatedBatteryLife ?: 0L
    }

    /**
     * Check if the SDK is currently in parking mode
     */
    fun isInParkingMode(): Boolean {
        return adaptivePowerManager.drivingContext.value == AdaptivePowerManager.DrivingContext.PARKED
    }

    /**
     * Get battery optimization statistics
     */
    fun getBatteryOptimizationStats(): BatteryOptimizationStats {
        val powerState = adaptivePowerManager.powerState.value
        val strategy = currentSamplingStrategy

        return BatteryOptimizationStats(
            currentBatteryLevel = powerState?.batteryLevel ?: 0f,
            estimatedLifeMinutes = powerState?.estimatedBatteryLife ?: 0L,
            powerMode = powerState?.powerMode?.name ?: "UNKNOWN",
            activeSensors = strategy?.enabledSensors?.size ?: 0,
            locationInterval = strategy?.locationInterval ?: 0L,
            sensorRate = strategy?.sensorRate?.name ?: "UNKNOWN",
            batterySavingsEnabled = telemetryConfig.batteryOptimizationEnabled
        )
    }

    data class BatteryOptimizationStats(
        val currentBatteryLevel: Float,
        val estimatedLifeMinutes: Long,
        val powerMode: String,
        val activeSensors: Int,
        val locationInterval: Long,
        val sensorRate: String,
        val batterySavingsEnabled: Boolean
    )
}