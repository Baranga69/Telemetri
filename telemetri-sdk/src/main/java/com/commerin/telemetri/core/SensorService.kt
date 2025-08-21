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

    fun start() {
        Log.d(TAG, "Starting comprehensive sensor data collection...")
        initializeAllSensors()
    }

    fun stop() {
        Log.d(TAG, "Stopping all sensor data collection...")
        stopAllSensors()
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
                } else {
                    Log.w(TAG, "Failed to register ${sensorType.name} sensor")
                }
            } else {
                Log.w(TAG, "${sensorType.name} sensor not available on this device")
            }
        }
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

                        // Log high-frequency sensors less frequently to avoid spam
                        if (shouldLogSensorData(sensorType)) {
                            Log.v(TAG, "Sensor data: ${sensorType.name} = ${sensorEvent.values.contentToString()}")
                        }
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
    fun enableSensorType(sensorType: SensorType) {
        enabledSensorTypes.add(sensorType)
    }

    fun disableSensorType(sensorType: SensorType) {
        enabledSensorTypes.remove(sensorType)
        // Stop the specific sensor if currently active
        sensorListeners[sensorType]?.let { listener ->
            sensorManager.unregisterListener(listener)
            sensorListeners.remove(sensorType)
            activeSensors.remove(sensorType)
        }
    }

    fun getAvailableSensors(): List<SensorType> {
        return enabledSensorTypes.filter { sensorType ->
            val androidType = mapToAndroidSensorType(sensorType)
            sensorManager.getDefaultSensor(androidType) != null
        }
    }

    fun isHardwareSupported(sensorType: SensorType): Boolean {
        val androidType = mapToAndroidSensorType(sensorType)
        return sensorManager.getDefaultSensor(androidType) != null
    }
}