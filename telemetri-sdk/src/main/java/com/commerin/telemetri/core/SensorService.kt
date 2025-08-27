package com.commerin.telemetri.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.SensorData
import com.commerin.telemetri.domain.model.SensorType
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class SensorService(private val context: Context) {
    companion object {
        private const val TAG = "SensorService"
        private const val DEFAULT_SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val _sensorData = MutableLiveData<SensorData>()
    val sensorData: LiveData<SensorData> = _sensorData

    // Track all active sensors and their listeners
    private val activeSensors = ConcurrentHashMap<SensorType, Sensor>()
    private val sensorListeners = ConcurrentHashMap<SensorType, SensorEventListener>()

    // Service state management
    private var isServiceRunning = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Configurable sensor types to collect (can be customized based on use case)
    private val enabledSensorTypes = mutableSetOf<SensorType>().apply {
        // Motion sensors (essential for most telemetry use cases)
        add(SensorType.ACCELEROMETER)
        add(SensorType.GYROSCOPE)
        add(SensorType.MAGNETOMETER)
        add(SensorType.GRAVITY)
        add(SensorType.LINEAR_ACCELERATION)
        add(SensorType.ROTATION_VECTOR)

        // Environmental sensors (useful for context awareness)
        add(SensorType.LIGHT)
        add(SensorType.PRESSURE)
        add(SensorType.AMBIENT_TEMPERATURE)
        add(SensorType.RELATIVE_HUMIDITY)

        // Motion detection (for activity recognition)
        add(SensorType.STEP_COUNTER)
        add(SensorType.STEP_DETECTOR)
        add(SensorType.SIGNIFICANT_MOTION)

        // Additional sensors
        add(SensorType.PROXIMITY)
        add(SensorType.GAME_ROTATION_VECTOR)
        add(SensorType.GEOMAGNETIC_ROTATION_VECTOR)
    }

    // Callback interface for SDK users
    interface SensorConfigurationListener {
        fun onSensorEnabled(sensorType: SensorType, success: Boolean)
        fun onSensorDisabled(sensorType: SensorType)
        fun onSensorAvailabilityChanged(availableSensors: List<SensorType>)
    }

    private var configurationListener: SensorConfigurationListener? = null

    fun start() {
        if (isServiceRunning) {
            Log.w(TAG, "SensorService is already running")
            return
        }

        Log.d(TAG, "Starting comprehensive sensor data collection...")
        isServiceRunning = true
        initializeAllSensors()
    }

    fun stop() {
        if (!isServiceRunning) {
            Log.w(TAG, "SensorService is not running")
            return
        }

        Log.d(TAG, "Stopping all sensor data collection...")
        isServiceRunning = false
        stopAllSensors()
    }

    // Enhanced configuration listener management
    fun setConfigurationListener(listener: SensorConfigurationListener?) {
        this.configurationListener = listener
    }

    private fun initializeAllSensors() {
        enabledSensorTypes.forEach { sensorType ->
            val androidSensorType = mapToAndroidSensorType(sensorType)
            val sensor = sensorManager.getDefaultSensor(androidSensorType)

            if (sensor != null) {
                val listener = createSensorListener(sensorType)
                sensorListeners[sensorType] = listener
                activeSensors[sensorType] = sensor

                val success = sensorManager.registerListener(
                    listener,
                    sensor,
                    getSensorDelay(sensorType)
                )

                if (success) {
                    Log.d(TAG, "Successfully registered ${sensorType.name} sensor")
                    configurationListener?.onSensorEnabled(sensorType, true)
                } else {
                    Log.w(TAG, "Failed to register ${sensorType.name} sensor")
                    configurationListener?.onSensorEnabled(sensorType, false)
                }
            } else {
                Log.w(TAG, "${sensorType.name} sensor not available on this device")
            }
        }

        // Notify available sensors after initialization
        notifyAvailableSensors()
    }

    private fun stopAllSensors() {
        sensorListeners.values.forEach { listener ->
            sensorManager.unregisterListener(listener)
        }
        sensorListeners.clear()
        activeSensors.clear()
        Log.d(TAG, "All sensors stopped")
    }

    private fun createSensorListener(sensorType: SensorType): SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let { sensorEvent ->
                    try {
                        val sensorData = SensorData(
                            sensorType = sensorType,
                            values = sensorEvent.values.clone(),
                            accuracy = sensorEvent.accuracy,
                            timestamp = System.currentTimeMillis(),
                            eventTimestamp = sensorEvent.timestamp
                        )

                        // Post sensor data to LiveData
                        _sensorData.postValue(sensorData)

//                        // Log high-frequency sensors less frequently to avoid spam
//                        if (shouldLogSensorData(sensorType)) {
//                            Log.v(TAG, "Sensor data: ${sensorType.name} = ${sensorEvent.values.contentToString()}")
//                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing sensor data for ${sensorType.name}", e)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                Log.d(TAG, "Sensor accuracy changed: ${sensor?.name} = $accuracy")
            }
        }
    }

    private fun mapToAndroidSensorType(sensorType: SensorType): Int {
        return when (sensorType) {
            SensorType.ACCELEROMETER -> Sensor.TYPE_ACCELEROMETER
            SensorType.GYROSCOPE -> Sensor.TYPE_GYROSCOPE
            SensorType.MAGNETOMETER -> Sensor.TYPE_MAGNETIC_FIELD
            SensorType.GRAVITY -> Sensor.TYPE_GRAVITY
            SensorType.LINEAR_ACCELERATION -> Sensor.TYPE_LINEAR_ACCELERATION
            SensorType.ROTATION_VECTOR -> Sensor.TYPE_ROTATION_VECTOR
            SensorType.GAME_ROTATION_VECTOR -> Sensor.TYPE_GAME_ROTATION_VECTOR
            SensorType.GEOMAGNETIC_ROTATION_VECTOR -> Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
            SensorType.ORIENTATION -> Sensor.TYPE_ORIENTATION
            SensorType.AMBIENT_TEMPERATURE -> Sensor.TYPE_AMBIENT_TEMPERATURE
            SensorType.LIGHT -> Sensor.TYPE_LIGHT
            SensorType.PRESSURE -> Sensor.TYPE_PRESSURE
            SensorType.RELATIVE_HUMIDITY -> Sensor.TYPE_RELATIVE_HUMIDITY
            SensorType.SIGNIFICANT_MOTION -> Sensor.TYPE_SIGNIFICANT_MOTION
            SensorType.STEP_COUNTER -> Sensor.TYPE_STEP_COUNTER
            SensorType.STEP_DETECTOR -> Sensor.TYPE_STEP_DETECTOR
            SensorType.HEART_RATE -> Sensor.TYPE_HEART_RATE
            SensorType.PROXIMITY -> Sensor.TYPE_PROXIMITY
            else -> -1 // Unknown sensor type
        }
    }

    private fun getSensorDelay(sensorType: SensorType): Int {
        return when (sensorType) {
            // High-frequency sensors for motion analysis
            SensorType.ACCELEROMETER,
            SensorType.GYROSCOPE,
            SensorType.LINEAR_ACCELERATION -> SensorManager.SENSOR_DELAY_GAME

            // Medium-frequency sensors
            SensorType.MAGNETOMETER,
            SensorType.GRAVITY,
            SensorType.ROTATION_VECTOR -> SensorManager.SENSOR_DELAY_UI

            // Low-frequency environmental sensors
            SensorType.LIGHT,
            SensorType.PRESSURE,
            SensorType.AMBIENT_TEMPERATURE,
            SensorType.RELATIVE_HUMIDITY,
            SensorType.PROXIMITY -> SensorManager.SENSOR_DELAY_NORMAL

            // Event-based sensors
            SensorType.STEP_DETECTOR,
            SensorType.SIGNIFICANT_MOTION -> SensorManager.SENSOR_DELAY_NORMAL

            // Default
            else -> DEFAULT_SENSOR_DELAY
        }
    }

    private fun shouldLogSensorData(sensorType: SensorType): Boolean {
        // Only log data for low-frequency sensors to avoid log spam
        return when (sensorType) {
            SensorType.STEP_DETECTOR,
            SensorType.STEP_COUNTER,
            SensorType.SIGNIFICANT_MOTION,
            SensorType.LIGHT,
            SensorType.PRESSURE,
            SensorType.AMBIENT_TEMPERATURE,
            SensorType.RELATIVE_HUMIDITY,
            SensorType.PROXIMITY -> true
            else -> false
        }
    }

    // Public methods for configuration
    /**
     * Enable a specific sensor type with immediate activation if service is running
     */
    fun enableSensorType(sensorType: SensorType): Boolean {
        val wasEnabled = enabledSensorTypes.contains(sensorType)
        enabledSensorTypes.add(sensorType)

        // If service is running and sensor wasn't previously enabled, start it immediately
        if (isServiceRunning && !wasEnabled) {
            return startIndividualSensor(sensorType)
        }

        return true
    }

    /**
     * Enable multiple sensor types at once for batch configuration
     */
    fun enableSensorTypes(sensorTypes: Collection<SensorType>): Map<SensorType, Boolean> {
        val results = mutableMapOf<SensorType, Boolean>()

        sensorTypes.forEach { sensorType ->
            results[sensorType] = enableSensorType(sensorType)
        }

        notifyAvailableSensors()
        return results
    }

    /**
     * Disable a specific sensor type with immediate deactivation
     */
    fun disableSensorType(sensorType: SensorType): Boolean {
        val wasEnabled = enabledSensorTypes.remove(sensorType)

        // Stop the specific sensor if currently active
        if (wasEnabled) {
            stopIndividualSensor(sensorType)
            return true
        }

        return false
    }

    /**
     * Disable multiple sensor types at once
     */
    fun disableSensorTypes(sensorTypes: Collection<SensorType>): Map<SensorType, Boolean> {
        val results = mutableMapOf<SensorType, Boolean>()

        sensorTypes.forEach { sensorType ->
            results[sensorType] = disableSensorType(sensorType)
        }

        notifyAvailableSensors()
        return results
    }

    /**
     * Get all sensors available on the current device (hardware supported)
     */
    fun getAllHardwareSupportedSensors(): List<SensorType> {
        return SensorType.values().filter { sensorType ->
            isHardwareSupported(sensorType)
        }
    }

    /**
     * Get currently enabled sensor types
     */
    fun getEnabledSensorTypes(): Set<SensorType> {
        return enabledSensorTypes.toSet()
    }

    /**
     * Get currently active (running) sensors
     */
    fun getActiveSensors(): Set<SensorType> {
        return activeSensors.keys.toSet()
    }

    /**
     * Check if a specific sensor type is currently enabled
     */
    fun isSensorEnabled(sensorType: SensorType): Boolean {
        return enabledSensorTypes.contains(sensorType)
    }

    /**
     * Check if a specific sensor is currently active (collecting data)
     */
    fun isSensorActive(sensorType: SensorType): Boolean {
        return activeSensors.containsKey(sensorType)
    }

    /**
     * Get available sensors (enabled and hardware supported)
     */
    fun getAvailableSensors(): List<SensorType> {
        return enabledSensorTypes.filter { sensorType ->
            isHardwareSupported(sensorType)
        }
    }

    /**
     * Check if specific sensor hardware is supported on device
     */
    fun isHardwareSupported(sensorType: SensorType): Boolean {
        val androidType = mapToAndroidSensorType(sensorType)
        return androidType != -1 && sensorManager.getDefaultSensor(androidType) != null
    }

    /**
     * Get sensor information including support and status
     */
    fun getSensorInfo(sensorType: SensorType): SensorInfo {
        val androidType = mapToAndroidSensorType(sensorType)
        val sensor = sensorManager.getDefaultSensor(androidType)

        return SensorInfo(
            sensorType = sensorType,
            isHardwareSupported = sensor != null,
            isEnabled = enabledSensorTypes.contains(sensorType),
            isActive = activeSensors.containsKey(sensorType),
            sensorName = sensor?.name,
            vendor = sensor?.vendor,
            maxRange = sensor?.maximumRange,
            resolution = sensor?.resolution,
            power = sensor?.power,
            minDelay = sensor?.minDelay
        )
    }

    /**
     * Configure sensor sampling rates dynamically
     */
    fun setSensorDelay(sensorType: SensorType): Boolean {
        if (!activeSensors.containsKey(sensorType)) {
            Log.w(TAG, "Cannot set delay for inactive sensor: ${sensorType.name}")
            return false
        }

        // Restart the sensor with new delay
        stopIndividualSensor(sensorType)
        return startIndividualSensor(sensorType)
    }

    /**
     * Reset to default sensor configuration
     */
    fun resetToDefaultConfiguration() {
        // Stop all current sensors
        stopAllSensors()

        // Reset to default enabled sensors
        enabledSensorTypes.clear()
        enabledSensorTypes.addAll(getDefaultSensorTypes())

        // Restart if service was running
        if (isServiceRunning) {
            initializeAllSensors()
        }

        Log.d(TAG, "Reset to default sensor configuration")
    }

    // =====================================
    // Power Management Methods
    // =====================================

    private var currentSamplingRate = SensorSamplingRate.MEDIUM
    private var originalEnabledSensors = mutableSetOf<SensorType>()
    private var isPowerSavingMode = false

    /**
     * Update sampling rate for power optimization
     */
    fun updateSamplingRate(newRate: SensorSamplingRate) {
        if (currentSamplingRate == newRate) return

        Log.d(TAG, "Updating sensor sampling rate from $currentSamplingRate to $newRate")
        currentSamplingRate = newRate

        if (isServiceRunning) {
            // Re-register sensors with new sampling rate
            reregisterSensorsWithNewRate()
        }
    }

    /**
     * Update enabled sensors for power optimization
     */
    fun updateEnabledSensors(newEnabledSensors: Set<AdaptivePowerManager.SensorType>) {
        // Convert AdaptivePowerManager.SensorType to our SensorType
        val convertedSensors = newEnabledSensors.mapNotNull { powerSensorType ->
            when (powerSensorType) {
                AdaptivePowerManager.SensorType.ACCELEROMETER -> SensorType.ACCELEROMETER
                AdaptivePowerManager.SensorType.GYROSCOPE -> SensorType.GYROSCOPE
                AdaptivePowerManager.SensorType.MAGNETOMETER -> SensorType.MAGNETOMETER
                AdaptivePowerManager.SensorType.GRAVITY -> SensorType.GRAVITY
                AdaptivePowerManager.SensorType.LINEAR_ACCELERATION -> SensorType.LINEAR_ACCELERATION
                AdaptivePowerManager.SensorType.PROXIMITY -> SensorType.PROXIMITY
                AdaptivePowerManager.SensorType.LIGHT -> SensorType.LIGHT
            }
        }.toSet()

        Log.d(TAG, "Updating enabled sensors. Previous: ${enabledSensorTypes.size}, New: ${convertedSensors.size}")

        if (isServiceRunning) {
            // Store original sensors if this is the first power optimization
            if (!isPowerSavingMode) {
                originalEnabledSensors = enabledSensorTypes.toMutableSet()
            }

            // Stop sensors not in the new set
            val sensorsToStop = enabledSensorTypes - convertedSensors
            sensorsToStop.forEach { sensorType ->
                stopIndividualSensor(sensorType)
            }

            // Start new sensors
            val sensorsToStart = convertedSensors - enabledSensorTypes
            sensorsToStart.forEach { sensorType ->
                startIndividualSensor(sensorType)
            }
        }

        enabledSensorTypes.clear()
        enabledSensorTypes.addAll(convertedSensors)
        isPowerSavingMode = true
    }

    /**
     * Restore original sensor configuration
     */
    fun restoreOriginalSensors() {
        if (!isPowerSavingMode) return

        Log.d(TAG, "Restoring original sensor configuration")
        isPowerSavingMode = false

        if (isServiceRunning) {
            // Stop all current sensors
            stopAllSensors()

            // Restore original configuration
            enabledSensorTypes.clear()
            enabledSensorTypes.addAll(originalEnabledSensors)

            // Restart with original configuration
            initializeAllSensors()
        } else {
            enabledSensorTypes.clear()
            enabledSensorTypes.addAll(originalEnabledSensors)
        }
    }

    private fun reregisterSensorsWithNewRate() {
        Log.d(TAG, "Re-registering sensors with new sampling rate")

        // Stop all sensors
        sensorListeners.values.forEach { listener ->
            sensorManager.unregisterListener(listener)
        }

        // Re-register with new delay
        sensorListeners.forEach { (sensorType, listener) ->
            activeSensors[sensorType]?.let { sensor ->
                val newDelay = getSensorDelayForRate(currentSamplingRate, sensorType)
                val success = sensorManager.registerListener(listener, sensor, newDelay)

                if (!success) {
                    Log.w(TAG, "Failed to re-register ${sensorType.name} sensor with new rate")
                }
            }
        }
    }

    private fun getSensorDelayForRate(rate: SensorSamplingRate, sensorType: SensorType): Int {
        val baseDelay = when (rate) {
            SensorSamplingRate.LOW -> SensorManager.SENSOR_DELAY_NORMAL
            SensorSamplingRate.MEDIUM -> SensorManager.SENSOR_DELAY_UI
            SensorSamplingRate.HIGH -> SensorManager.SENSOR_DELAY_GAME
            SensorSamplingRate.ULTRA_HIGH -> SensorManager.SENSOR_DELAY_FASTEST
            SensorSamplingRate.ADAPTIVE -> getSensorDelay(sensorType) // Use default logic
        }

        return baseDelay
    }

    private fun startIndividualSensor(sensorType: SensorType): Boolean {
        val androidSensorType = mapToAndroidSensorType(sensorType)
        val sensor = sensorManager.getDefaultSensor(androidSensorType)

        return if (sensor != null) {
            val listener = createSensorListener(sensorType)
            sensorListeners[sensorType] = listener
            activeSensors[sensorType] = sensor

            val delay = getSensorDelayForRate(currentSamplingRate, sensorType)
            val success = sensorManager.registerListener(listener, sensor, delay)

            if (success) {
                Log.d(TAG, "Successfully started individual sensor: ${sensorType.name}")
                configurationListener?.onSensorEnabled(sensorType, true)
            } else {
                Log.w(TAG, "Failed to start individual sensor: ${sensorType.name}")
                configurationListener?.onSensorEnabled(sensorType, false)
            }

            success
        } else {
            Log.w(TAG, "Sensor not available: ${sensorType.name}")
            false
        }
    }

    private fun stopIndividualSensor(sensorType: SensorType) {
        sensorListeners[sensorType]?.let { listener ->
            sensorManager.unregisterListener(listener)
            sensorListeners.remove(sensorType)
            activeSensors.remove(sensorType)
            configurationListener?.onSensorDisabled(sensorType)
            Log.d(TAG, "Stopped individual sensor: ${sensorType.name}")
        }
    }

    private fun getDefaultSensorTypes(): Set<SensorType> {
        return setOf(
            // Motion sensors (essential for most telemetry use cases)
            SensorType.ACCELEROMETER,
            SensorType.GYROSCOPE,
            SensorType.MAGNETOMETER,
            SensorType.GRAVITY,
            SensorType.LINEAR_ACCELERATION,
            SensorType.ROTATION_VECTOR,

            // Environmental sensors (useful for context awareness)
            SensorType.LIGHT,
            SensorType.PRESSURE,
            SensorType.AMBIENT_TEMPERATURE,
            SensorType.RELATIVE_HUMIDITY,

            // Motion detection (for activity recognition)
            SensorType.STEP_COUNTER,
            SensorType.STEP_DETECTOR,
            SensorType.SIGNIFICANT_MOTION,

            // Additional sensors
            SensorType.PROXIMITY,
            SensorType.GAME_ROTATION_VECTOR,
            SensorType.GEOMAGNETIC_ROTATION_VECTOR
        )
    }

    private fun notifyAvailableSensors() {
        serviceScope.launch {
            val availableSensors = getAvailableSensors()
            configurationListener?.onSensorAvailabilityChanged(availableSensors)
        }
    }

    /**
     * Cleanup resources when service is destroyed
     */
    fun cleanup() {
        stop()
        serviceScope.cancel()
        configurationListener = null
        Log.d(TAG, "SensorService cleanup completed")
    }

    // Data class for detailed sensor information
    data class SensorInfo(
        val sensorType: SensorType,
        val isHardwareSupported: Boolean,
        val isEnabled: Boolean,
        val isActive: Boolean,
        val sensorName: String?,
        val vendor: String?,
        val maxRange: Float?,
        val resolution: Float?,
        val power: Float?,
        val minDelay: Int?
    )
}