package com.commerin.telemetri.core

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.*
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Intelligent power management and adaptive sampling engine
 * Optimizes battery usage while maintaining data quality for insurance telematics
 */
class AdaptivePowerManager(private val context: Context) {
    companion object {
        private const val TAG = "AdaptivePowerManager"

        // Battery thresholds
        private const val CRITICAL_BATTERY_LEVEL = 10f
        private const val LOW_BATTERY_LEVEL = 20f
        private const val NORMAL_BATTERY_LEVEL = 50f

        // Speed thresholds for context detection
        private const val PARKING_SPEED_THRESHOLD = 0.5f // m/s
        private const val CITY_SPEED_THRESHOLD = 13.9f // 50 km/h
        private const val HIGHWAY_SPEED_THRESHOLD = 22.2f // 80 km/h

        // Sampling intervals (milliseconds)
        private const val PARKING_INTERVAL = 30000L // 30 seconds
        private const val CITY_INTERVAL = 1000L // 1 second
        private const val HIGHWAY_INTERVAL = 2000L // 2 seconds
        private const val LOW_BATTERY_INTERVAL = 5000L // 5 seconds
        private const val CRITICAL_BATTERY_INTERVAL = 15000L // 15 seconds

        // Context analysis window
        private const val CONTEXT_ANALYSIS_WINDOW = 60000L // 1 minute
        private const val PARKING_DETECTION_DURATION = 120000L // 2 minutes stationary
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private val _powerState = MutableLiveData<PowerState>()
    val powerState: LiveData<PowerState> = _powerState

    private val _samplingStrategy = MutableLiveData<SamplingStrategy>()
    val samplingStrategy: LiveData<SamplingStrategy> = _samplingStrategy

    private val _drivingContext = MutableLiveData<DrivingContext>()
    val drivingContext: LiveData<DrivingContext> = _drivingContext

    private var isMonitoring = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Context tracking
    private val speedHistory = mutableListOf<TimestampedSpeed>()
    private val locationHistory = mutableListOf<TimestampedLocation>()
    private var lastMovementTime = 0L
    private var currentContext = DrivingContext.UNKNOWN
    private var isInParkingMode = false

    data class TimestampedSpeed(val timestamp: Long, val speed: Float)
    data class TimestampedLocation(val timestamp: Long, val lat: Double, val lon: Double)

    data class PowerState(
        val batteryLevel: Float,
        val isCharging: Boolean,
        val isPowerSaveMode: Boolean,
        val thermalState: ThermalState,
        val powerMode: PowerMode,
        val estimatedBatteryLife: Long // minutes
    )

    data class SamplingStrategy(
        val locationInterval: Long,
        val sensorRate: SensorSamplingRate,
        val enabledSensors: Set<SensorType>,
        val backgroundProcessing: Boolean,
        val dataCompressionLevel: CompressionLevel,
        val adaptiveReason: String
    )

    enum class DrivingContext {
        PARKED,           // Stationary for extended period
        CITY_DRIVING,     // Urban driving with frequent stops
        HIGHWAY_DRIVING,  // High-speed sustained driving
        STOP_AND_GO,      // Traffic congestion
        UNKNOWN
    }

    enum class PowerMode {
        PERFORMANCE,      // Full data collection
        BALANCED,         // Optimized for battery/quality balance
        BATTERY_SAVER,    // Minimal data collection
        CRITICAL_BATTERY, // Emergency mode
        DEEP_SLEEP        // Parking mode with minimal activity
    }

    enum class ThermalState {
        NORMAL, WARM, HOT, CRITICAL
    }

    enum class CompressionLevel {
        NONE, LOW, MEDIUM, HIGH
    }

    enum class SensorType {
        ACCELEROMETER, GYROSCOPE, MAGNETOMETER,
        GRAVITY, LINEAR_ACCELERATION, PROXIMITY, LIGHT
    }

    fun startAdaptivePowerManagement() {
        if (isMonitoring) return

        isMonitoring = true
        Log.d(TAG, "Starting adaptive power management")

        scope.launch {
            while (isMonitoring) {
                analyzePowerState()
                analyzeDrivingContext()
                updateSamplingStrategy()
                delay(5000) // Check every 5 seconds
            }
        }

        // Monitor battery changes
        scope.launch {
            monitorBatteryChanges()
        }
    }

    fun stopAdaptivePowerManagement() {
        isMonitoring = false
        Log.d(TAG, "Stopping adaptive power management")
    }

    fun updateLocationData(locationData: LocationData) {
        if (!isMonitoring) return

        synchronized(this) {
            // Update speed history
            locationData.speed?.let { speed ->
                speedHistory.add(TimestampedSpeed(locationData.timestamp, speed))

                // Track movement for parking detection
                if (speed > PARKING_SPEED_THRESHOLD) {
                    lastMovementTime = locationData.timestamp
                }
            }

            // Update location history
            locationHistory.add(
                TimestampedLocation(locationData.timestamp, locationData.latitude, locationData.longitude)
            )

            // Maintain buffer sizes
            keepBufferSize(speedHistory, 100)
            keepBufferSize(locationHistory, 50)
        }
    }

    private fun analyzePowerState() {
        val batteryLevel = getBatteryLevel()
        val isCharging = isDeviceCharging()
        val isPowerSaveMode = powerManager.isPowerSaveMode
        val thermalState = getThermalState()

        val powerMode = determinePowerMode(batteryLevel, isCharging, isPowerSaveMode, thermalState)
        val estimatedBatteryLife = estimateBatteryLife(batteryLevel, isCharging)

        val powerState = PowerState(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            thermalState = thermalState,
            powerMode = powerMode,
            estimatedBatteryLife = estimatedBatteryLife
        )

        _powerState.postValue(powerState)
    }

    private fun analyzeDrivingContext() {
        val currentTime = System.currentTimeMillis()
        val recentSpeeds = speedHistory.filter {
            currentTime - it.timestamp < CONTEXT_ANALYSIS_WINDOW
        }

        if (recentSpeeds.isEmpty()) {
            currentContext = DrivingContext.UNKNOWN
            _drivingContext.postValue(currentContext)
            return
        }

        // Check for parking (stationary for extended period)
        val timeSinceMovement = currentTime - lastMovementTime
        val isStationary = recentSpeeds.all { it.speed < PARKING_SPEED_THRESHOLD }

        currentContext = when {
            timeSinceMovement > PARKING_DETECTION_DURATION && isStationary -> {
                isInParkingMode = true
                DrivingContext.PARKED
            }

            else -> {
                isInParkingMode = false
                analyzeMovingContext(recentSpeeds)
            }
        }

        _drivingContext.postValue(currentContext)
    }

    private fun analyzeMovingContext(speeds: List<TimestampedSpeed>): DrivingContext {
        if (speeds.isEmpty()) return DrivingContext.UNKNOWN

        val avgSpeed = speeds.map { it.speed }.average().toFloat()
        val maxSpeed = speeds.maxOfOrNull { it.speed } ?: 0f
        val speedVariance = calculateSpeedVariance(speeds.map { it.speed })

        return when {
            // Highway driving: sustained high speeds with low variance
            avgSpeed > HIGHWAY_SPEED_THRESHOLD && speedVariance < 5f ->
                DrivingContext.HIGHWAY_DRIVING

            // Stop and go: high variance at moderate speeds
            avgSpeed > 5f && speedVariance > 10f ->
                DrivingContext.STOP_AND_GO

            // City driving: moderate speeds with moderate variance
            avgSpeed > 2f && avgSpeed <= CITY_SPEED_THRESHOLD ->
                DrivingContext.CITY_DRIVING

            else -> DrivingContext.UNKNOWN
        }
    }

    private fun updateSamplingStrategy() {
        val powerState = _powerState.value ?: return
        val drivingContext = _drivingContext.value ?: DrivingContext.UNKNOWN

        val strategy = calculateOptimalSamplingStrategy(powerState, drivingContext)
        _samplingStrategy.postValue(strategy)
    }

    private fun calculateOptimalSamplingStrategy(
        powerState: PowerState,
        drivingContext: DrivingContext
    ): SamplingStrategy {

        // Base strategy on power mode
        val baseStrategy = when (powerState.powerMode) {
            PowerMode.PERFORMANCE -> createPerformanceStrategy()
            PowerMode.BALANCED -> createBalancedStrategy()
            PowerMode.BATTERY_SAVER -> createBatterySaverStrategy()
            PowerMode.CRITICAL_BATTERY -> createCriticalBatteryStrategy()
            PowerMode.DEEP_SLEEP -> createDeepSleepStrategy()
        }

        // Adjust for driving context
        return adjustStrategyForContext(baseStrategy, drivingContext, powerState)
    }

    private fun createPerformanceStrategy(): SamplingStrategy {
        return SamplingStrategy(
            locationInterval = CITY_INTERVAL,
            sensorRate = SensorSamplingRate.HIGH,
            enabledSensors = setOf(
                SensorType.ACCELEROMETER, SensorType.GYROSCOPE,
                SensorType.MAGNETOMETER, SensorType.LINEAR_ACCELERATION,
                SensorType.GRAVITY, SensorType.PROXIMITY, SensorType.LIGHT
            ),
            backgroundProcessing = true,
            dataCompressionLevel = CompressionLevel.LOW,
            adaptiveReason = "Performance mode - full data collection"
        )
    }

    private fun createBalancedStrategy(): SamplingStrategy {
        return SamplingStrategy(
            locationInterval = CITY_INTERVAL,
            sensorRate = SensorSamplingRate.MEDIUM,
            enabledSensors = setOf(
                SensorType.ACCELEROMETER, SensorType.GYROSCOPE,
                SensorType.LINEAR_ACCELERATION, SensorType.PROXIMITY
            ),
            backgroundProcessing = true,
            dataCompressionLevel = CompressionLevel.MEDIUM,
            adaptiveReason = "Balanced mode - optimized for quality and battery"
        )
    }

    private fun createBatterySaverStrategy(): SamplingStrategy {
        return SamplingStrategy(
            locationInterval = LOW_BATTERY_INTERVAL,
            sensorRate = SensorSamplingRate.LOW,
            enabledSensors = setOf(
                SensorType.ACCELEROMETER, SensorType.GYROSCOPE
            ),
            backgroundProcessing = false,
            dataCompressionLevel = CompressionLevel.HIGH,
            adaptiveReason = "Battery saver - reduced data collection"
        )
    }

    private fun createCriticalBatteryStrategy(): SamplingStrategy {
        return SamplingStrategy(
            locationInterval = CRITICAL_BATTERY_INTERVAL,
            sensorRate = SensorSamplingRate.LOW,
            enabledSensors = setOf(SensorType.ACCELEROMETER),
            backgroundProcessing = false,
            dataCompressionLevel = CompressionLevel.HIGH,
            adaptiveReason = "Critical battery - minimal data collection"
        )
    }

    private fun createDeepSleepStrategy(): SamplingStrategy {
        return SamplingStrategy(
            locationInterval = PARKING_INTERVAL,
            sensorRate = SensorSamplingRate.LOW,
            enabledSensors = setOf(SensorType.ACCELEROMETER), // Only for movement detection
            backgroundProcessing = false,
            dataCompressionLevel = CompressionLevel.HIGH,
            adaptiveReason = "Deep sleep - parking mode"
        )
    }

    private fun adjustStrategyForContext(
        baseStrategy: SamplingStrategy,
        context: DrivingContext,
        powerState: PowerState
    ): SamplingStrategy {

        return when (context) {
            DrivingContext.PARKED -> {
                // Minimal sampling when parked
                baseStrategy.copy(
                    locationInterval = PARKING_INTERVAL,
                    sensorRate = SensorSamplingRate.LOW,
                    enabledSensors = setOf(SensorType.ACCELEROMETER),
                    adaptiveReason = "${baseStrategy.adaptiveReason} + parking optimization"
                )
            }

            DrivingContext.HIGHWAY_DRIVING -> {
                // Less frequent sampling on highway (more predictable motion)
                if (powerState.batteryLevel > NORMAL_BATTERY_LEVEL) {
                    baseStrategy.copy(
                        locationInterval = HIGHWAY_INTERVAL,
                        adaptiveReason = "${baseStrategy.adaptiveReason} + highway optimization"
                    )
                } else baseStrategy
            }

            DrivingContext.CITY_DRIVING, DrivingContext.STOP_AND_GO -> {
                // More frequent sampling in city (unpredictable motion)
                if (powerState.batteryLevel > LOW_BATTERY_LEVEL) {
                    baseStrategy.copy(
                        locationInterval = CITY_INTERVAL,
                        adaptiveReason = "${baseStrategy.adaptiveReason} + city optimization"
                    )
                } else baseStrategy
            }

            else -> baseStrategy
        }
    }

    private fun determinePowerMode(
        batteryLevel: Float,
        isCharging: Boolean,
        isPowerSaveMode: Boolean,
        thermalState: ThermalState
    ): PowerMode {

        return when {
            // Critical conditions override everything
            batteryLevel <= CRITICAL_BATTERY_LEVEL && !isCharging -> PowerMode.CRITICAL_BATTERY
            thermalState == ThermalState.CRITICAL -> PowerMode.CRITICAL_BATTERY

            // Parking mode when stationary
            isInParkingMode -> PowerMode.DEEP_SLEEP

            // Battery saver conditions
            batteryLevel <= LOW_BATTERY_LEVEL && !isCharging -> PowerMode.BATTERY_SAVER
            isPowerSaveMode -> PowerMode.BATTERY_SAVER
            thermalState == ThermalState.HOT -> PowerMode.BATTERY_SAVER

            // Performance mode when conditions are good
            isCharging && batteryLevel > NORMAL_BATTERY_LEVEL -> PowerMode.PERFORMANCE
            batteryLevel > 80f && thermalState == ThermalState.NORMAL -> PowerMode.PERFORMANCE

            // Default to balanced
            else -> PowerMode.BALANCED
        }
    }

    private fun getBatteryLevel(): Float {
        return try {
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            level.toFloat()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery level", e)
            50f // Default
        }
    }

    private fun isDeviceCharging(): Boolean {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            Log.e(TAG, "Error checking charging status", e)
            false
        }
    }

    private fun getThermalState(): ThermalState {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                when (powerManager.currentThermalStatus) {
                    PowerManager.THERMAL_STATUS_NONE -> ThermalState.NORMAL
                    PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.WARM
                    PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.HOT
                    PowerManager.THERMAL_STATUS_SEVERE,
                    PowerManager.THERMAL_STATUS_CRITICAL,
                    PowerManager.THERMAL_STATUS_EMERGENCY,
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.CRITICAL
                    else -> ThermalState.NORMAL
                }
            } else {
                ThermalState.NORMAL // Assume normal for older devices
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting thermal state", e)
            ThermalState.NORMAL
        }
    }

    private fun estimateBatteryLife(batteryLevel: Float, isCharging: Boolean): Long {
        if (isCharging) return Long.MAX_VALUE // Unlimited when charging

        // Simplified battery life estimation based on current usage patterns
        val currentContext = _drivingContext.value ?: DrivingContext.UNKNOWN

        val hourlyDrainRate = when (currentContext) {
            DrivingContext.PARKED -> 1f // 1% per hour when parked
            DrivingContext.HIGHWAY_DRIVING -> 8f // 8% per hour on highway
            DrivingContext.CITY_DRIVING, DrivingContext.STOP_AND_GO -> 12f // 12% per hour in city
            else -> 10f // Default drain rate
        }

        return if (hourlyDrainRate > 0) {
            ((batteryLevel / hourlyDrainRate) * 60).toLong() // Convert to minutes
        } else {
            Long.MAX_VALUE
        }
    }

    private suspend fun monitorBatteryChanges() {
        var lastBatteryLevel = getBatteryLevel()

        while (isMonitoring) {
            delay(30000) // Check every 30 seconds

            val currentBatteryLevel = getBatteryLevel()
            val batteryDrop = lastBatteryLevel - currentBatteryLevel

            // Trigger immediate strategy update for significant battery changes
            if (batteryDrop > 5f ||
                (currentBatteryLevel <= LOW_BATTERY_LEVEL && lastBatteryLevel > LOW_BATTERY_LEVEL) ||
                (currentBatteryLevel <= CRITICAL_BATTERY_LEVEL && lastBatteryLevel > CRITICAL_BATTERY_LEVEL)) {

                Log.d(TAG, "Significant battery change detected, updating strategy")
                analyzePowerState()
                updateSamplingStrategy()
            }

            lastBatteryLevel = currentBatteryLevel
        }
    }

    private fun calculateSpeedVariance(speeds: List<Float>): Float {
        if (speeds.size < 2) return 0f

        val mean = speeds.average().toFloat()
        val variance = speeds.map { (it - mean) * (it - mean) }.average().toFloat()
        return sqrt(variance)
    }

    private fun <T> keepBufferSize(buffer: MutableList<T>, maxSize: Int) {
        while (buffer.size > maxSize) {
            buffer.removeAt(0)
        }
    }

    /**
     * Get current power optimization recommendations
     */
    fun getPowerOptimizationRecommendations(): List<String> {
        val powerState = _powerState.value ?: return emptyList()
        val recommendations = mutableListOf<String>()

        when (powerState.powerMode) {
            PowerMode.CRITICAL_BATTERY -> {
                recommendations.add("Critical battery level - consider charging your device")
                recommendations.add("Data collection reduced to essential sensors only")
            }
            PowerMode.BATTERY_SAVER -> {
                recommendations.add("Low battery detected - sampling frequency reduced")
                recommendations.add("Some advanced features temporarily disabled")
            }
            PowerMode.DEEP_SLEEP -> {
                recommendations.add("Vehicle appears to be parked - entering power save mode")
                recommendations.add("Will resume full monitoring when movement detected")
            }
            else -> {
                if (powerState.thermalState == ThermalState.HOT) {
                    recommendations.add("Device running warm - reducing processing load")
                }
            }
        }

        return recommendations
    }
}
