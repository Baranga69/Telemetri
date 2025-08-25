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
        private const val ANALYSIS_WINDOW_MS = 500L // Reduced to 0.5 seconds for more responsive fitness tracking
        private const val ACTIVITY_CONFIDENCE_THRESHOLD = 0.7f
        private const val STEP_UPDATE_INTERVAL_MS = 100L // Update step count every 100ms for real-time feedback
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
    private var stepUpdateJob: Job? = null
    private var lastStepUpdateTime = 0L

    fun startAnalysis() {
        if (isAnalyzing) {
            Log.d(TAG, "Motion analysis already running")
            return
        }

        isAnalyzing = true
        registerSensors()

        // Main analysis job for motion data
        analysisJob = scope.launch {
            while (isAnalyzing) {
                delay(ANALYSIS_WINDOW_MS)
                analyzeMotionData()
            }
        }

        // Separate job for real-time step updates
        stepUpdateJob = scope.launch {
            while (isAnalyzing) {
                delay(STEP_UPDATE_INTERVAL_MS)
                updateStepData()
            }
        }

        Log.d(TAG, "Motion analysis started with enhanced step tracking")
    }

    fun stopAnalysis() {
        if (!isAnalyzing) {
            Log.d(TAG, "Motion analysis not running")
            return
        }

        isAnalyzing = false
        unregisterSensors()
        analysisJob?.cancel()
        stepUpdateJob?.cancel()
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
        if (accelerometerData.isEmpty()) return

        val (accelX, accelY, accelZ) = getLatestAcceleration()
        val (gyroX, gyroY, gyroZ) = getLatestGyroscope()

        val accelMagnitude = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)
        val gyroMagnitude = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)

        // Enhanced vehicle speed calculation using sensor fusion
        val vehicleSpeed = calculateVehicleSpeed(accelMagnitude, gyroMagnitude)

        val activityType = detectActivity(accelMagnitude, gyroMagnitude, vehicleSpeed)
        val confidence = calculateConfidence(activityType, accelMagnitude, gyroMagnitude)

        val motionData = MotionData(
            accelerationMagnitude = accelMagnitude,
            gyroscopeMagnitude = gyroMagnitude,
            magneticFieldMagnitude = getMagneticFieldMagnitude(),
            linearAcceleration = getLatestLinearAcceleration(),
            gravity = getLatestGravity(),
            rotationVector = calculateRotationVector(),
            activityType = activityType,
            confidence = confidence,
            stepCount = stepCount,
            stepFrequency = stepFrequency(),
            vehicleSpeed = vehicleSpeed,
            accelerationX = accelX,
            accelerationY = accelY,
            accelerationZ = accelZ,
            gyroscopeX = gyroX,
            gyroscopeY = gyroY,
            gyroscopeZ = gyroZ,
            timestamp = System.currentTimeMillis()
        )

        _motionData.postValue(motionData)
    }

    private var lastVelocity = floatArrayOf(0f, 0f, 0f)
    private var lastTimestamp = 0L
    private val velocityHistory = mutableListOf<Float>()

    private fun calculateVehicleSpeed(accelMagnitude: Float, gyroMagnitude: Float): Float {
        if (linearAccelData.isEmpty()) return 0f

        val currentTime = System.currentTimeMillis()
        if (lastTimestamp == 0L) {
            lastTimestamp = currentTime
            return 0f
        }

        val deltaTime = (currentTime - lastTimestamp) / 1000f // Convert to seconds
        lastTimestamp = currentTime

        // Get linear acceleration (gravity removed)
        val linearAccel = linearAccelData.lastOrNull() ?: return 0f

        // Integrate acceleration to get velocity change
        val deltaVx = linearAccel[0] * deltaTime
        val deltaVy = linearAccel[1] * deltaTime
        val deltaVz = linearAccel[2] * deltaTime

        // Update velocity
        lastVelocity[0] += deltaVx
        lastVelocity[1] += deltaVy
        lastVelocity[2] += deltaVz

        // Apply damping to prevent drift (sensor noise compensation)
        val dampingFactor = if (accelMagnitude < 0.5f) 0.95f else 0.98f
        lastVelocity[0] *= dampingFactor
        lastVelocity[1] *= dampingFactor
        lastVelocity[2] *= dampingFactor

        // Calculate speed magnitude
        val speed = sqrt(
            lastVelocity[0] * lastVelocity[0] +
            lastVelocity[1] * lastVelocity[1] +
            lastVelocity[2] * lastVelocity[2]
        )

        // Apply vehicle motion filtering
        val filteredSpeed = if (isVehicleMotion(accelMagnitude, gyroMagnitude)) {
            // Store in history for smoothing
            velocityHistory.add(speed)
            if (velocityHistory.size > 10) velocityHistory.removeAt(0)

            // Return smoothed speed
            velocityHistory.average().toFloat()
        } else {
            // Reset velocity if not in vehicle
            lastVelocity = floatArrayOf(0f, 0f, 0f)
            0f
        }

        return maxOf(0f, filteredSpeed) // Ensure non-negative speed
    }

    private fun isVehicleMotion(accelMagnitude: Float, gyroMagnitude: Float): Boolean {
        // Vehicle motion characteristics:
        // - Sustained acceleration patterns
        // - Moderate to high acceleration magnitude
        // - Rotational movement (turns, steering)
        return accelMagnitude > 2f && (gyroMagnitude > 0.5f || accelMagnitude > 8f)
    }

    private fun detectActivity(accelMagnitude: Float, gyroMagnitude: Float, vehicleSpeed: Float): ActivityType {
        return when {
            vehicleSpeed > 5f -> ActivityType.IN_VEHICLE // Speed > 5 m/s indicates vehicle
            accelMagnitude < 0.5f && gyroMagnitude < 0.1f -> ActivityType.STILL
            accelMagnitude > 12f && gyroMagnitude > 3f -> ActivityType.IN_VEHICLE
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

    private fun getLatestAcceleration(): Triple<Float, Float, Float> {
        return if (accelerometerData.isNotEmpty()) {
            val latest = accelerometerData.last()
            Triple(latest[0], latest[1], latest[2])
        } else {
            Triple(0f, 0f, 0f)
        }
    }

    private fun getLatestGyroscope(): Triple<Float, Float, Float> {
        return if (gyroscopeData.isNotEmpty()) {
            val latest = gyroscopeData.last()
            Triple(latest[0], latest[1], latest[2])
        } else {
            Triple(0f, 0f, 0f)
        }
    }

    private fun getLatestLinearAcceleration(): Triple<Float, Float, Float> {
        return if (linearAccelData.isNotEmpty()) {
            val latest = linearAccelData.last()
            Triple(latest[0], latest[1], latest[2])
        } else {
            Triple(0f, 0f, 0f)
        }
    }

    private fun getLatestGravity(): Triple<Float, Float, Float> {
        return if (gravityData.isNotEmpty()) {
            val latest = gravityData.last()
            Triple(latest[0], latest[1], latest[2])
        } else {
            Triple(0f, 9.8f, 0f) // Default gravity
        }
    }

    private fun getMagneticFieldMagnitude(): Float {
        return if (magnetometerData.isNotEmpty()) {
            val latest = magnetometerData.last()
            sqrt(latest[0] * latest[0] + latest[1] * latest[1] + latest[2] * latest[2])
        } else {
            0f
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

    /**
     * Real-time step data update for immediate fitness tracking feedback
     * This runs every 100ms to provide responsive step count updates
     */
    private fun updateStepData() {
        val currentTime = System.currentTimeMillis()

        // Only update if we have recent step data and sufficient time has passed
        if (currentTime - lastStepUpdateTime > STEP_UPDATE_INTERVAL_MS && stepCount > 0) {
            // Create quick motion data update for step count changes
            val currentMotionData = _motionData.value
            if (currentMotionData != null) {
                val quickMotionData = currentMotionData.copy(
                    stepCount = stepCount,
                    stepFrequency = calculateStepFrequency(),
                    timestamp = currentTime
                )
                _motionData.postValue(quickMotionData)
            }

            lastStepUpdateTime = currentTime
        }
    }
}
