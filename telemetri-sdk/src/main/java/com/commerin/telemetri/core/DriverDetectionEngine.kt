package com.commerin.telemetri.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.DriverState
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Advanced driver detection engine for insurance telematics
 * Uses multi-modal sensor fusion to distinguish driver from passenger
 */
class DriverDetectionEngine(private val context: Context) : SensorEventListener {
    companion object {
        private const val TAG = "DriverDetectionEngine"
        private const val ANALYSIS_WINDOW_MS = 2000L // 2 seconds for pattern analysis
        private const val DRIVER_CONFIDENCE_THRESHOLD = 0.75f
        private const val PHONE_POSITION_BUFFER_SIZE = 50
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val _driverState = MutableLiveData<DriverState>()
    val driverState: LiveData<DriverState> = _driverState

    private var isAnalyzing = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Enhanced sensor buffers for driver detection
    private val accelerometerBuffer = mutableListOf<FloatArray>()
    private val gyroscopeBuffer = mutableListOf<FloatArray>()
    private val magnetometerBuffer = mutableListOf<FloatArray>()
    private val proximityReadings = mutableListOf<Float>()
    private val lightReadings = mutableListOf<Float>()

    // Driver behavior indicators
    private var phoneMovementPatterns = mutableListOf<PhoneMovementPattern>()
    private var vehicleTurnCorrelations = mutableListOf<TurnCorrelation>()
    private var phonePositionHistory = mutableListOf<PhonePosition>()

    data class PhoneMovementPattern(
        val timestamp: Long,
        val movementType: MovementType,
        val intensity: Float,
        val correlationWithVehicle: Float
    )

    data class TurnCorrelation(
        val vehicleTurnMagnitude: Float,
        val phoneResponseDelay: Long,
        val correlationStrength: Float
    )

    data class PhonePosition(
        val timestamp: Long,
        val position: Position,
        val confidence: Float
    )

    enum class MovementType {
        STEERING_MOTION,      // Circular/arc movements typical of steering
        PASSIVE_MOVEMENT,     // Linear movements typical of passengers
        GEAR_SHIFTING,        // Sharp, deliberate movements
        PHONE_HANDLING,       // Irregular, non-driving movements
        STABLE_MOUNT,         // Mounted phone with minimal movement
        UNKNOWN,
        DISTRACTED,
        PASSENGER_LIKE,
        DRIVING_FOCUSED

    }

    enum class Position {
        DRIVER_SIDE_DASHBOARD,    // Mounted on driver side
        DRIVER_HAND,              // In driver's hand
        PASSENGER_SIDE,           // Passenger area
        CENTER_CONSOLE,           // Central position
        CUP_HOLDER,              // In cup holder
        UNKNOWN,
        CENTER,
        DRIVER_SIDE
    }

    fun startDriverDetection() {
        if (isAnalyzing) return

        isAnalyzing = true
        registerSensors()

        scope.launch {
            while (isAnalyzing) {
                delay(ANALYSIS_WINDOW_MS)
                analyzeDriverBehavior()
            }
        }
    }

    fun stopDriverDetection() {
        isAnalyzing = false
        unregisterSensors()
        clearBuffers()
    }

    private fun registerSensors() {
        val sensors = listOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_LIGHT
        )

        sensors.forEach { sensorType ->
            sensorManager.getDefaultSensor(sensorType)?.let { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
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
                    accelerometerBuffer.add(event.values.clone())
                    keepBufferSize(accelerometerBuffer)
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroscopeBuffer.add(event.values.clone())
                    keepBufferSize(gyroscopeBuffer)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    magnetometerBuffer.add(event.values.clone())
                    keepBufferSize(magnetometerBuffer)
                }
                Sensor.TYPE_PROXIMITY -> {
                    proximityReadings.add(event.values[0])
                    keepBufferSize(proximityReadings, 20)
                }
                Sensor.TYPE_LIGHT -> {
                    lightReadings.add(event.values[0])
                    keepBufferSize(lightReadings, 20)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun analyzeDriverBehavior() {
        if (accelerometerBuffer.size < 10) return

        val phonePosition = detectPhonePosition()
        val movementPattern = analyzeMovementPatterns()
        val turnCorrelation = analyzeTurnCorrelations()
        val handlingPattern = detectPhoneHandling()

        val driverProbability = calculateDriverProbability(
            phonePosition,
            movementPattern,
            turnCorrelation,
            handlingPattern
        )

        val driverState = DriverState(
            isDriver = driverProbability > DRIVER_CONFIDENCE_THRESHOLD,
            confidence = driverProbability,
            phonePosition = phonePosition.position,
            movementPattern = movementPattern,
            evidenceFactors = buildEvidenceMap(phonePosition, movementPattern, turnCorrelation),
            timestamp = System.currentTimeMillis()
        )

        _driverState.postValue(driverState)
    }

    private fun detectPhonePosition(): PhonePosition {
        if (accelerometerBuffer.isEmpty() || magnetometerBuffer.isEmpty()) {
            return PhonePosition(System.currentTimeMillis(), Position.UNKNOWN, 0f)
        }

        // Analyze orientation relative to vehicle
        val avgAccel = averageVector(accelerometerBuffer.takeLast(10))
        val avgMag = averageVector(magnetometerBuffer.takeLast(10))

        // Phone orientation analysis
        val tiltAngle = calculateTiltAngle(avgAccel)
        val magneticHeading = calculateMagneticHeading(avgMag)

        // Movement stability analysis
        val movementStability = calculateMovementStability()

        val position = when {
            movementStability > 0.8f && tiltAngle < 15f -> Position.DRIVER_SIDE_DASHBOARD
            movementStability < 0.3f -> Position.DRIVER_HAND
            tiltAngle > 45f -> Position.CUP_HOLDER
            magneticHeading in 45f..135f -> Position.PASSENGER_SIDE
            else -> Position.CENTER_CONSOLE
        }

        val confidence = calculatePositionConfidence(position, movementStability, tiltAngle)

        return PhonePosition(System.currentTimeMillis(), position, confidence)
    }

    private fun analyzeMovementPatterns(): MovementType {
        if (gyroscopeBuffer.size < 10) return MovementType.UNKNOWN

        val gyroData = gyroscopeBuffer.takeLast(20)
        val accelData = accelerometerBuffer.takeLast(20)

        // Detect circular motions (steering)
        val circularMotion = detectCircularMotion(gyroData)

        // Detect sharp, deliberate movements (gear shifting)
        val sharpMovements = detectSharpMovements(accelData)

        // Detect irregular handling patterns
        val irregularPatterns = detectIrregularPatterns(accelData, gyroData)

        return when {
            circularMotion > 0.7f -> MovementType.STEERING_MOTION
            sharpMovements > 0.6f -> MovementType.GEAR_SHIFTING
            irregularPatterns > 0.5f -> MovementType.PHONE_HANDLING
            calculateMovementStability() > 0.8f -> MovementType.STABLE_MOUNT
            else -> MovementType.PASSIVE_MOVEMENT
        }
    }

    private fun analyzeTurnCorrelations(): Float {
        // Analyze correlation between vehicle turns and phone movements
        // Driver's phone typically moves in correlation with steering
        // Passenger's phone moves more independently

        if (gyroscopeBuffer.size < 20) return 0f

        val vehicleTurns = detectVehicleTurns()
        val phoneMovements = detectPhoneMovements()

        return calculateCorrelation(vehicleTurns, phoneMovements)
    }

    private fun detectPhoneHandling(): Float {
        // Detect if phone is being actively used/handled
        // Active handling typically indicates passenger use

        if (proximityReadings.isEmpty()) return 0f

        val proximityChanges = detectProximityChanges()
        val lightChanges = detectLightChanges()
        val accelerationPatterns = detectHandlingAcceleration()

        return (proximityChanges + lightChanges + accelerationPatterns) / 3f
    }

    private fun calculateDriverProbability(
        phonePosition: PhonePosition,
        movementPattern: MovementType,
        turnCorrelation: Float,
        handlingPattern: Float
    ): Float {
        var probability = 0.5f // Start neutral

        // Phone position indicators
        probability += when (phonePosition.position) {
            Position.DRIVER_SIDE_DASHBOARD -> 0.3f * phonePosition.confidence
            Position.DRIVER_HAND -> 0.1f * phonePosition.confidence
            Position.PASSENGER_SIDE -> -0.4f * phonePosition.confidence
            Position.CENTER_CONSOLE -> 0.0f
            Position.CUP_HOLDER -> 0.2f * phonePosition.confidence
            Position.UNKNOWN -> 0.0f
            Position.CENTER -> 0.0f
            Position.DRIVER_SIDE -> 0.0f
        }

        // Movement pattern indicators
        probability += when (movementPattern) {
            MovementType.STEERING_MOTION -> 0.25f
            MovementType.GEAR_SHIFTING -> 0.2f
            MovementType.STABLE_MOUNT -> 0.1f
            MovementType.PHONE_HANDLING -> -0.3f
            MovementType.PASSIVE_MOVEMENT -> -0.1f
            MovementType.UNKNOWN -> 0f
            MovementType.DISTRACTED -> 0f
            MovementType.PASSENGER_LIKE -> 0f
            MovementType.DRIVING_FOCUSED -> 0f
        }

        // Turn correlation (driver's phone correlates with vehicle turns)
        probability += turnCorrelation * 0.2f

        // Phone handling (active use typically means passenger)
        probability -= handlingPattern * 0.15f

        return probability.coerceIn(0f, 1f)
    }

    // Helper methods for calculations
    private fun averageVector(vectors: List<FloatArray>): FloatArray {
        if (vectors.isEmpty()) return floatArrayOf(0f, 0f, 0f)

        val sum = floatArrayOf(0f, 0f, 0f)
        vectors.forEach { vector ->
            sum[0] += vector[0]
            sum[1] += vector[1]
            sum[2] += vector[2]
        }

        return floatArrayOf(sum[0] / vectors.size, sum[1] / vectors.size, sum[2] / vectors.size)
    }

    private fun calculateTiltAngle(accel: FloatArray): Float {
        return atan2(sqrt(accel[0] * accel[0] + accel[1] * accel[1]), accel[2]) * 180f / PI.toFloat()
    }

    private fun calculateMagneticHeading(mag: FloatArray): Float {
        return atan2(mag[1], mag[0]) * 180f / PI.toFloat()
    }

    private fun calculateMovementStability(): Float {
        if (accelerometerBuffer.size < 5) return 0f

        val recent = accelerometerBuffer.takeLast(10)
        val variance = calculateVariance(recent.map { sqrt(it[0]*it[0] + it[1]*it[1] + it[2]*it[2]) })

        return (1f / (1f + variance)).coerceIn(0f, 1f)
    }

    private fun calculatePositionConfidence(position: Position, stability: Float, tiltAngle: Float): Float {
        return when (position) {
            Position.DRIVER_SIDE_DASHBOARD -> stability * 0.8f + (1f - tiltAngle / 90f) * 0.2f
            Position.DRIVER_HAND -> (1f - stability) * 0.6f + 0.4f
            Position.PASSENGER_SIDE -> 0.7f
            else -> 0.5f
        }.coerceIn(0f, 1f)
    }

    private fun detectCircularMotion(gyroData: List<FloatArray>): Float {
        // Implement circular motion detection algorithm
        // This would analyze the rotation patterns for steering-like movements
        return 0f // Placeholder
    }

    private fun detectSharpMovements(accelData: List<FloatArray>): Float {
        // Detect sudden acceleration changes typical of gear shifting
        return 0f // Placeholder
    }

    private fun detectIrregularPatterns(accelData: List<FloatArray>, gyroData: List<FloatArray>): Float {
        // Detect irregular movement patterns typical of phone handling
        return 0f // Placeholder
    }

    private fun detectVehicleTurns(): List<Float> {
        // Extract vehicle turn events from sensor data
        return emptyList() // Placeholder
    }

    private fun detectPhoneMovements(): List<Float> {
        // Extract phone movement events
        return emptyList() // Placeholder
    }

    private fun calculateCorrelation(turns: List<Float>, movements: List<Float>): Float {
        // Calculate correlation between vehicle turns and phone movements
        return 0f // Placeholder
    }

    private fun detectProximityChanges(): Float {
        if (proximityReadings.size < 5) return 0f
        return calculateVariance(proximityReadings.takeLast(10)) / 10f
    }

    private fun detectLightChanges(): Float {
        if (lightReadings.size < 5) return 0f
        return calculateVariance(lightReadings.takeLast(10)) / 1000f
    }

    private fun detectHandlingAcceleration(): Float {
        // Detect acceleration patterns typical of phone handling
        return 0f // Placeholder
    }

    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average().toFloat()
        return values.map { (it - mean) * (it - mean) }.average().toFloat()
    }

    private fun buildEvidenceMap(
        phonePosition: PhonePosition,
        movementPattern: MovementType,
        turnCorrelation: Float
    ): Map<String, Float> {
        return mapOf(
            "phonePosition" to phonePosition.confidence,
            "movementPattern" to if (movementPattern == MovementType.STEERING_MOTION) 1f else 0f,
            "turnCorrelation" to turnCorrelation,
            "stability" to calculateMovementStability()
        )
    }

    private fun <T> keepBufferSize(buffer: MutableList<T>, maxSize: Int = PHONE_POSITION_BUFFER_SIZE) {
        while (buffer.size > maxSize) {
            buffer.removeAt(0)
        }
    }

    private fun clearBuffers() {
        accelerometerBuffer.clear()
        gyroscopeBuffer.clear()
        magnetometerBuffer.clear()
        proximityReadings.clear()
        lightReadings.clear()
        phoneMovementPatterns.clear()
        vehicleTurnCorrelations.clear()
        phonePositionHistory.clear()
    }
}
