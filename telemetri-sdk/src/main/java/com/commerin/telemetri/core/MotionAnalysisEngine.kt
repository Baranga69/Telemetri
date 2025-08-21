package com.commerin.telemetri.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.ActivityType
import com.commerin.telemetri.domain.model.MotionData
import kotlinx.coroutines.*
import kotlin.math.*

class MotionAnalysisEngine(private val context: Context) : SensorEventListener {
    companion object {
        private const val TAG = "MotionAnalysisEngine"
        private const val ANALYSIS_WINDOW_MS = 2000L // 2 seconds
        private const val ACTIVITY_CONFIDENCE_THRESHOLD = 0.7f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val _motionData = MutableLiveData<MotionData>()
    val motionData: LiveData<MotionData> = _motionData

    private var isAnalyzing = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Sensor data buffers
    private val accelerometerData = mutableListOf<FloatArray>()
    private val gyroscopeData = mutableListOf<FloatArray>()
    private val magnetometerData = mutableListOf<FloatArray>()
    private val gravityData = mutableListOf<FloatArray>()
    private val linearAccelData = mutableListOf<FloatArray>()

    private var stepCount = 0
    private var lastStepTime = 0L
    private var analysisJob: Job? = null

    fun startAnalysis() {
        if (isAnalyzing) {
            Log.d(TAG, "Motion analysis already running")
            return
        }

        isAnalyzing = true
        registerSensors()

        analysisJob = scope.launch {
            while (isAnalyzing) {
                delay(ANALYSIS_WINDOW_MS)
                analyzeMotionData()
            }
        }

        Log.d(TAG, "Motion analysis started")
    }

    fun stopAnalysis() {
        if (!isAnalyzing) {
            Log.d(TAG, "Motion analysis not running")
            return
        }

        isAnalyzing = false
        unregisterSensors()
        analysisJob?.cancel()
        clearBuffers()

        Log.d(TAG, "Motion analysis stopped")
    }

    private fun registerSensors() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gravity?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        linearAccel?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        stepDetector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isAnalyzing) return

        synchronized(this) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accelerometerData.add(event.values.clone())
                    keepBufferSize(accelerometerData)
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroscopeData.add(event.values.clone())
                    keepBufferSize(gyroscopeData)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    magnetometerData.add(event.values.clone())
                    keepBufferSize(magnetometerData)
                }
                Sensor.TYPE_GRAVITY -> {
                    gravityData.add(event.values.clone())
                    keepBufferSize(gravityData)
                }
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    linearAccelData.add(event.values.clone())
                    keepBufferSize(linearAccelData)
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    stepCount++
                    lastStepTime = System.currentTimeMillis()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    private fun keepBufferSize(buffer: MutableList<FloatArray>, maxSize: Int = 100) {
        while (buffer.size > maxSize) {
            buffer.removeAt(0)
        }
    }

    private fun analyzeMotionData() {
        try {
            synchronized(this) {
                if (accelerometerData.isEmpty()) return

                val accelerationMagnitude = calculateMagnitude(accelerometerData)
                val gyroscopeMagnitude = calculateMagnitude(gyroscopeData)
                val magneticFieldMagnitude = calculateMagnitude(magnetometerData)

                val linearAcceleration = getLatestTriple(linearAccelData)
                val gravity = getLatestTriple(gravityData)
                val rotationVector = calculateRotationVector()

                val activityType = detectActivity(accelerationMagnitude, gyroscopeMagnitude)
                val confidence = calculateConfidence(activityType, accelerationMagnitude, gyroscopeMagnitude)
                val stepFrequency = calculateStepFrequency()

                val motionData = MotionData(
                    accelerationMagnitude = accelerationMagnitude,
                    gyroscopeMagnitude = gyroscopeMagnitude,
                    magneticFieldMagnitude = magneticFieldMagnitude,
                    linearAcceleration = linearAcceleration,
                    gravity = gravity,
                    rotationVector = rotationVector,
                    activityType = activityType,
                    confidence = confidence,
                    stepCount = stepCount,
                    stepFrequency = stepFrequency,
                    timestamp = System.currentTimeMillis()
                )

                _motionData.postValue(motionData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing motion data", e)
        }
    }

    private fun calculateMagnitude(data: List<FloatArray>): Float {
        if (data.isEmpty()) return 0f

        var totalMagnitude = 0f
        for (values in data.takeLast(10)) { // Use last 10 readings
            val magnitude = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
            totalMagnitude += magnitude
        }
        return totalMagnitude / minOf(data.size, 10)
    }

    private fun getLatestTriple(data: List<FloatArray>): Triple<Float, Float, Float> {
        return if (data.isNotEmpty()) {
            val latest = data.last()
            Triple(latest[0], latest[1], latest[2])
        } else {
            Triple(0f, 0f, 0f)
        }
    }

    private fun calculateRotationVector(): FloatArray {
        // Simplified rotation vector calculation
        // In a real implementation, you'd use SensorManager.getRotationMatrix()
        return if (accelerometerData.isNotEmpty() && magnetometerData.isNotEmpty()) {
            val accel = accelerometerData.last()
            val mag = magnetometerData.last()
            floatArrayOf(accel[0], accel[1], accel[2], mag[0])
        } else {
            floatArrayOf(0f, 0f, 0f, 0f)
        }
    }

    private fun detectActivity(accelMagnitude: Float, gyroMagnitude: Float): ActivityType {
        return when {
            accelMagnitude < 0.5f && gyroMagnitude < 0.1f -> ActivityType.STILL
            accelMagnitude > 15f && gyroMagnitude > 5f -> ActivityType.IN_VEHICLE
            accelMagnitude in 2f..8f && stepFrequency() > 1f -> ActivityType.WALKING
            accelMagnitude > 8f && stepFrequency() > 2f -> ActivityType.RUNNING
            gyroMagnitude > 2f -> ActivityType.TILTING
            else -> ActivityType.UNKNOWN
        }
    }

    private fun calculateConfidence(activityType: ActivityType, accelMag: Float, gyroMag: Float): Float {
        return when (activityType) {
            ActivityType.STILL -> if (accelMag < 1f && gyroMag < 0.2f) 0.9f else 0.6f
            ActivityType.WALKING -> if (accelMag in 3f..7f) 0.8f else 0.5f
            ActivityType.RUNNING -> if (accelMag > 8f) 0.8f else 0.5f
            ActivityType.IN_VEHICLE -> if (accelMag > 10f && gyroMag > 3f) 0.7f else 0.4f
            else -> 0.3f
        }
    }

    private fun stepFrequency(): Float = calculateStepFrequency()

    private fun calculateStepFrequency(): Float {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastStepTime
        return if (timeDiff > 0 && stepCount > 0) {
            (stepCount * 60000f) / timeDiff // Steps per minute
        } else {
            0f
        }
    }

    private fun clearBuffers() {
        synchronized(this) {
            accelerometerData.clear()
            gyroscopeData.clear()
            magnetometerData.clear()
            gravityData.clear()
            linearAccelData.clear()
            stepCount = 0
            lastStepTime = 0L
        }
    }

    fun cleanup() {
        stopAnalysis()
        scope.cancel()
    }
}
