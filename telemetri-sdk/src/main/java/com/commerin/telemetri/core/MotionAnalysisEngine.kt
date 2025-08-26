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

// Enum for sensor sampling rates

class MotionAnalysisEngine(private val context: Context) : SensorEventListener {
    companion object {
        private const val TAG = "MotionAnalysisEngine"
        private const val ANALYSIS_WINDOW_MS = 500L
        private const val STEP_UPDATE_INTERVAL_MS = 100L
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

    // Additional tracking variables
    private var lastVelocity = floatArrayOf(0f, 0f, 0f)
    private var lastTimestamp = 0L
    private val velocityHistory = mutableListOf<Float>()
    private var currentSamplingRate = SensorSamplingRate.MEDIUM
    private var isParkingMode = false

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

    fun updateSamplingRate(newRate: SensorSamplingRate) {
        if (currentSamplingRate == newRate) return

        Log.d(TAG, "Updating motion analysis sampling rate from $currentSamplingRate to $newRate")
        currentSamplingRate = newRate

        if (isAnalyzing) {
            // Re-register sensors with new sampling rate
            unregisterSensors()
            registerSensorsWithRate(newRate)
        }
    }

    fun enableParkingMode() {
        if (isParkingMode) return

        Log.d(TAG, "Enabling parking mode - minimal motion detection")
        isParkingMode = true

        if (isAnalyzing) {
            // Switch to parking mode analysis
            analysisJob?.cancel()
            analysisJob = scope.launch {
                while (isAnalyzing && isParkingMode) {
                    delay(5000L) // Check every 5 seconds in parking mode
                    analyzeParkingModeMotion()
                }
            }
        }
    }

    fun disableParkingMode() {
        if (!isParkingMode) return

        Log.d(TAG, "Disabling parking mode - restoring full motion analysis")
        isParkingMode = false

        if (isAnalyzing) {
            // Restart normal analysis
            analysisJob?.cancel()
            analysisJob = scope.launch {
                while (isAnalyzing && !isParkingMode) {
                    delay(ANALYSIS_WINDOW_MS)
                    analyzeMotionData()
                }
            }
        }
    }

    private fun registerSensorsWithRate(samplingRate: SensorSamplingRate) {
        val sensorDelay = when (samplingRate) {
            SensorSamplingRate.LOW -> SensorManager.SENSOR_DELAY_NORMAL
            SensorSamplingRate.MEDIUM -> SensorManager.SENSOR_DELAY_UI
            SensorSamplingRate.HIGH -> SensorManager.SENSOR_DELAY_GAME
            SensorSamplingRate.ULTRA_HIGH -> SensorManager.SENSOR_DELAY_FASTEST
            SensorSamplingRate.ADAPTIVE -> SensorManager.SENSOR_DELAY_GAME
        }

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        accelerometer?.let {
            sensorManager.registerListener(this, it, sensorDelay)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, sensorDelay)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, sensorDelay)
        }
        gravity?.let {
            sensorManager.registerListener(this, it, sensorDelay)
        }
        linearAccel?.let {
            sensorManager.registerListener(this, it, sensorDelay)
        }
        stepDetector?.let {
            sensorManager.registerListener(this, it, sensorDelay)
        }
    }

    private fun analyzeParkingModeMotion() {
        if (accelerometerData.isEmpty()) return

        // Get latest acceleration data
        val latestAccel = accelerometerData.lastOrNull() ?: return
        val accelMagnitude = sqrt(latestAccel[0] * latestAccel[0] + latestAccel[1] * latestAccel[1] + latestAccel[2] * latestAccel[2])

        // If significant movement detected, switch back to normal mode
        if (accelMagnitude > 2f) {
            Log.d(TAG, "Movement detected in parking mode - vehicle may be starting")

            val motionData = MotionData(
                accelerationMagnitude = accelMagnitude,
                gyroscopeMagnitude = 0f,
                magneticFieldMagnitude = 0f,
                linearAcceleration = Triple(0f, 0f, 0f),
                gravity = Triple(0f, 0f, 0f),
                rotationVector = floatArrayOf(),
                activityType = ActivityType.UNKNOWN,
                confidence = 0.5f,
                stepCount = stepCount,
                stepFrequency = 0f,
                vehicleSpeed = 0f,
                accelerationX = latestAccel[0],
                accelerationY = latestAccel[1],
                accelerationZ = latestAccel[2],
                gyroscopeX = 0f,
                gyroscopeY = 0f,
                gyroscopeZ = 0f,
                timestamp = System.currentTimeMillis()
            )

            _motionData.postValue(motionData)
        }
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

    // Missing function implementations
    private fun updateStepData() {
        // Update step-related data if needed
        // This function was called but not implemented
    }

    private fun clearBuffers() {
        accelerometerData.clear()
        gyroscopeData.clear()
        magnetometerData.clear()
        gravityData.clear()
        linearAccelData.clear()
        velocityHistory.clear()
    }

    private fun getLatestAcceleration(): FloatArray {
        return accelerometerData.lastOrNull() ?: floatArrayOf(0f, 0f, 0f)
    }

    private fun getLatestGyroscope(): FloatArray {
        return gyroscopeData.lastOrNull() ?: floatArrayOf(0f, 0f, 0f)
    }

    private fun getMagneticFieldMagnitude(): Float {
        val latest = magnetometerData.lastOrNull() ?: return 0f
        return sqrt(latest[0] * latest[0] + latest[1] * latest[1] + latest[2] * latest[2])
    }

    private fun getLatestLinearAcceleration(): Triple<Float, Float, Float> {
        val latest = linearAccelData.lastOrNull() ?: return Triple(0f, 0f, 0f)
        return Triple(latest[0], latest[1], latest[2])
    }

    private fun getLatestGravity(): Triple<Float, Float, Float> {
        val latest = gravityData.lastOrNull() ?: return Triple(0f, 0f, 9.8f)
        return Triple(latest[0], latest[1], latest[2])
    }

    private fun calculateRotationVector(): FloatArray {
        // Simple implementation - would need more complex calculation for actual rotation vector
        return floatArrayOf(0f, 0f, 0f, 1f)
    }

    private fun calculateStepFrequency(): Float {
        if (stepCount == 0 || lastStepTime == 0L) return 0f

        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastStepTime

        if (timeDiff == 0L) return 0f

        // Calculate steps per minute
        return (stepCount * 60000f) / timeDiff
    }

    private fun analyzeMotionData() {
        if (accelerometerData.isEmpty()) return

        val latestAccel = getLatestAcceleration()
        val latestGyro = getLatestGyroscope()

        val accelMagnitude = sqrt(latestAccel[0] * latestAccel[0] + latestAccel[1] * latestAccel[1] + latestAccel[2] * latestAccel[2])
        val gyroMagnitude = sqrt(latestGyro[0] * latestGyro[0] + latestGyro[1] * latestGyro[1] + latestGyro[2] * latestGyro[2])

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
            stepFrequency = calculateStepFrequency(),
            vehicleSpeed = vehicleSpeed,
            accelerationX = latestAccel[0],
            accelerationY = latestAccel[1],
            accelerationZ = latestAccel[2],
            gyroscopeX = latestGyro[0],
            gyroscopeY = latestGyro[1],
            gyroscopeZ = latestGyro[2],
            timestamp = System.currentTimeMillis()
        )

        _motionData.postValue(motionData)
    }

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
        return accelMagnitude > 2f && (gyroMagnitude > 0.5f || accelMagnitude > 8f)
    }

    private fun detectActivity(
        accelMagnitude: Float,
        gyroMagnitude: Float,
        vehicleSpeed: Float
    ): ActivityType {
        // Enhanced vehicle detection with multiple criteria and confidence thresholds
        val vehicleIndicators = mutableListOf<Float>()

        // Speed-based detection (most reliable)
        if (vehicleSpeed > 5f) vehicleIndicators.add(0.9f)
        else if (vehicleSpeed > 2f) vehicleIndicators.add(0.6f)

        // Acceleration pattern analysis for vehicle motion
        val accelPattern = analyzeAccelerationPattern(accelMagnitude)
        if (accelPattern > 0.7f) vehicleIndicators.add(0.8f)

        // Sustained motion analysis
        val sustainedMotion = analyzeSustainedMotion()
        if (sustainedMotion > 0.6f) vehicleIndicators.add(0.7f)

        // Calculate vehicle confidence
        val vehicleConfidence = if (vehicleIndicators.isNotEmpty()) {
            vehicleIndicators.average().toFloat()
        } else 0f

        return when {
            // Enhanced vehicle detection with confidence scoring
            vehicleConfidence > 0.7f -> ActivityType.IN_VEHICLE

            // Refined thresholds for other activities
            accelMagnitude < 0.3f && gyroMagnitude < 0.05f -> ActivityType.STILL
            accelMagnitude > 15f && gyroMagnitude > 4f -> ActivityType.IN_VEHICLE // High-intensity driving

            // Walking/running with step frequency validation
            accelMagnitude in 2f..8f && calculateStepFrequency() > 1.5f -> {
                if (calculateStepFrequency() > 3f) ActivityType.RUNNING else ActivityType.WALKING
            }

            // Enhanced motion detection
            gyroMagnitude > 2f && accelMagnitude > 1f -> ActivityType.TILTING

            // Default case with better classification
            accelMagnitude > 1f -> ActivityType.ON_FOOT
            else -> ActivityType.UNKNOWN
        }
    }

    private fun calculateConfidence(
        activityType: ActivityType,
        accelMag: Float,
        gyroMag: Float
    ): Float {
        // Enhanced confidence calculation with multiple factors
        val baseConfidence = when (activityType) {
            ActivityType.STILL -> {
                val stillnessScore = 1f - (accelMag + gyroMag) / 2f
                stillnessScore.coerceIn(0f, 1f)
            }

            ActivityType.WALKING -> {
                val walkingScore = when {
                    accelMag in 3f..7f && calculateStepFrequency() in 1.5f..3f -> 0.85f
                    accelMag in 2f..9f -> 0.6f
                    else -> 0.3f
                }
                walkingScore
            }

            ActivityType.RUNNING -> {
                val runningScore = when {
                    accelMag > 8f && calculateStepFrequency() > 3f -> 0.8f
                    accelMag > 6f -> 0.6f
                    else -> 0.3f
                }
                runningScore
            }

            ActivityType.IN_VEHICLE -> {
                // Multi-factor vehicle confidence
                val speedConfidence = if (lastVelocity.any { it > 3f }) 0.9f else 0.5f
                val motionConfidence = if (accelMag > 8f && gyroMag > 2f) 0.8f else 0.4f
                val patternConfidence = analyzeVehiclePatterns()

                (speedConfidence + motionConfidence + patternConfidence) / 3f
            }

            ActivityType.CYCLING -> {
                val cyclingScore = when {
                    accelMag in 4f..10f && gyroMag in 1f..3f -> 0.7f
                    else -> 0.4f
                }
                cyclingScore
            }

            else -> 0.3f
        }

        // Apply temporal consistency boost
        val consistencyBoost = calculateTemporalConsistency(activityType)
        val finalConfidence = baseConfidence + (consistencyBoost * 0.2f)

        return finalConfidence.coerceIn(0f, 1f)
    }

    // Helper methods for enhanced detection
    private fun analyzeAccelerationPattern(accelMagnitude: Float): Float {
        if (accelerometerData.size < 10) return 0f

        val recentAccel = accelerometerData.takeLast(10)
        val accelerationVariance = calculateAccelerationVariance(recentAccel)

        // Vehicle motion typically has consistent but varying acceleration patterns
        return when {
            accelerationVariance in 2f..10f -> 0.8f  // Typical vehicle variance
            accelerationVariance > 10f -> 0.6f       // High variance (city driving)
            accelerationVariance < 1f -> 0.2f        // Too consistent (not vehicle)
            else -> 0.4f
        }
    }

    private fun analyzeSustainedMotion(): Float {
        if (accelerometerData.size < 20) return 0f

        val motionDuration = calculateContinuousMotionDuration()
        return when {
            motionDuration > 30000L -> 0.9f  // 30+ seconds of motion = likely vehicle
            motionDuration > 15000L -> 0.7f  // 15+ seconds = possible vehicle
            motionDuration > 5000L -> 0.4f   // 5+ seconds = might be vehicle
            else -> 0.1f
        }
    }

    private fun analyzeVehiclePatterns(): Float {
        // Analyze patterns specific to vehicle motion
        val turnPatterns = detectTurnPatterns()
        val accelerationPatterns = detectAccelerationPatterns()
        val brakingPatterns = detectBrakingPatterns()

        return (turnPatterns + accelerationPatterns + brakingPatterns) / 3f
    }

    private fun calculateTemporalConsistency(currentActivity: ActivityType): Float {
        // Check consistency with previous activity detections
        return 0.5f // Placeholder - would implement actual consistency checking
    }

    private fun calculateAccelerationVariance(accelerations: List<FloatArray>): Float {
        if (accelerations.size < 2) return 0f

        val magnitudes = accelerations.map {
            sqrt(it[0] * it[0] + it[1] * it[1] + it[2] * it[2])
        }

        val mean = magnitudes.average().toFloat()
        val variance = magnitudes.map { (it - mean) * (it - mean) }.average().toFloat()

        return sqrt(variance)
    }

    private fun calculateContinuousMotionDuration(): Long {
        val currentTime = System.currentTimeMillis()
        val motionThreshold = 1.5f

        var continuousMotionStart = currentTime

        // Look backwards for continuous motion above threshold
        for (i in accelerometerData.indices.reversed()) {
            val accelData = accelerometerData[i]
            val magnitude =
                sqrt(accelData[0] * accelData[0] + accelData[1] * accelData[1] + accelData[2] * accelData[2])

            if (magnitude > motionThreshold) {
                continuousMotionStart =
                    currentTime - (accelerometerData.size - i) * 100L // Approximate timing
            } else {
                break
            }
        }

        return currentTime - continuousMotionStart
    }

    private fun detectTurnPatterns(): Float {
        if (gyroscopeData.size < 10) return 0f

        val recentGyro = gyroscopeData.takeLast(10)
        val turnEvents = recentGyro.count { gyroData ->
            val magnitude = sqrt(gyroData[0] * gyroData[0] + gyroData[1] * gyroData[1] + gyroData[2] * gyroData[2])
            magnitude > 1.0f // Turn threshold
        }

        return turnEvents / 10f // Return as ratio
    }

    private fun detectAccelerationPatterns(): Float {
        if (accelerometerData.size < 10) return 0f

        val recentAccel = accelerometerData.takeLast(10)
        val highAccelEvents = recentAccel.count { accelData ->
            val magnitude = sqrt(accelData[0] * accelData[0] + accelData[1] * accelData[1] + accelData[2] * accelData[2])
            magnitude > 5.0f // High acceleration threshold
        }

        return highAccelEvents / 10f // Return as ratio
    }

    private fun detectBrakingPatterns(): Float {
        if (accelerometerData.size < 5) return 0f

        val recentAccel = accelerometerData.takeLast(5)
        var brakingEvents = 0

        for (i in 1 until recentAccel.size) {
            val prevMagnitude = sqrt(recentAccel[i-1][0] * recentAccel[i-1][0] + recentAccel[i-1][1] * recentAccel[i-1][1] + recentAccel[i-1][2] * recentAccel[i-1][2])
            val currMagnitude = sqrt(recentAccel[i][0] * recentAccel[i][0] + recentAccel[i][1] * recentAccel[i][1] + recentAccel[i][2] * recentAccel[i][2])

            // Detect sudden deceleration
            if (prevMagnitude > 3f && currMagnitude < prevMagnitude - 2f) {
                brakingEvents++
            }
        }

        return brakingEvents / 4f // Return as ratio
    }
}
