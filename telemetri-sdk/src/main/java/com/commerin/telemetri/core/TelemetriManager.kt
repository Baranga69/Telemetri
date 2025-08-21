package com.commerin.telemetri.core

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.*
import kotlinx.coroutines.*

class TelemetriManager private constructor(private val context: Context) {
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
    private val performanceTelemetryService = PerformanceTelemetryService(context)
    private val deviceStateService = DeviceStateService(context)


    // Telemetry configuration
    private var telemetryConfig = TelemetryConfig()

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

        // Stop all services
        sensorService.stop()
        locationService.stopLocationUpdates()
        audioTelemetryService.stopAudioTelemetry()
        networkTelemetryService.stopNetworkMonitoring()
        performanceTelemetryService.stopPerformanceMonitoring()
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
     * Create use case specific configurations
     */
    object ConfigPresets {
        fun automotiveUseCase(): TelemetryConfig {
            return TelemetryConfig(
                enableSensorCollection = true,
                enableLocationTracking = true,
                enableAudioTelemetry = true,
                enableNetworkTelemetry = true,
                enablePerformanceMonitoring = true,
                enableMotionAnalysis = true,
                enableDeviceStateMonitoring = true,
                sensorSamplingRate = SensorSamplingRate.HIGH,
                locationUpdateInterval = 1000L, // 1 second for driving
                audioAnalysisEnabled = true,
                networkQualityMonitoring = true,
                batteryOptimizationEnabled = false // Full data for automotive
            )
        }

        fun fitnessTrackingUseCase(): TelemetryConfig {
            return TelemetryConfig(
                enableSensorCollection = true,
                enableLocationTracking = true,
                enableAudioTelemetry = false,
                enableNetworkTelemetry = false,
                enablePerformanceMonitoring = true,
                enableMotionAnalysis = true,
                enableDeviceStateMonitoring = true,
                sensorSamplingRate = SensorSamplingRate.HIGH,
                locationUpdateInterval = 5000L, // 5 seconds
                audioAnalysisEnabled = false,
                networkQualityMonitoring = false,
                batteryOptimizationEnabled = true
            )
        }

        fun environmentalMonitoringUseCase(): TelemetryConfig {
            return TelemetryConfig(
                enableSensorCollection = true,
                enableLocationTracking = true,
                enableAudioTelemetry = true,
                enableNetworkTelemetry = true,
                enablePerformanceMonitoring = false,
                enableMotionAnalysis = false,
                enableDeviceStateMonitoring = false,
                sensorSamplingRate = SensorSamplingRate.MEDIUM,
                locationUpdateInterval = 30000L, // 30 seconds
                audioAnalysisEnabled = true,
                networkQualityMonitoring = true,
                batteryOptimizationEnabled = true
            )
        }

        fun securityMonitoringUseCase(): TelemetryConfig {
            return TelemetryConfig(
                enableSensorCollection = true,
                enableLocationTracking = true,
                enableAudioTelemetry = true,
                enableNetworkTelemetry = true,
                enablePerformanceMonitoring = true,
                enableMotionAnalysis = true,
                enableDeviceStateMonitoring = true,
                sensorSamplingRate = SensorSamplingRate.ULTRA_HIGH,
                locationUpdateInterval = 500L, // 0.5 seconds
                audioAnalysisEnabled = true,
                networkQualityMonitoring = true,
                batteryOptimizationEnabled = false // Maximum data collection
            )
        }

        fun batterySaverUseCase(): TelemetryConfig {
            return TelemetryConfig(
                enableSensorCollection = true,
                enableLocationTracking = false,
                enableAudioTelemetry = false,
                enableNetworkTelemetry = false,
                enablePerformanceMonitoring = false,
                enableMotionAnalysis = false,
                enableDeviceStateMonitoring = true,
                sensorSamplingRate = SensorSamplingRate.LOW,
                locationUpdateInterval = 300000L, // 5 minutes
                audioAnalysisEnabled = false,
                networkQualityMonitoring = false,
                batteryOptimizationEnabled = true
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

    // Utility methods
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000, 9999)}"
    }

    private fun generateEventId(): String {
        return "event_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(100, 999)}"
    }

    fun cleanup() {
        stopTelemetryCollection()
        audioTelemetryService.cleanup()
        networkTelemetryService.cleanup()
        performanceTelemetryService.cleanup()
        scope.cancel()
    }
}