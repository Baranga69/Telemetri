package com.commerin.telemetri.core

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.*
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Advanced driving event detection engine for insurance telematics
 * Detects and scores critical driving behaviors using multi-sensor fusion
 */
class DrivingEventDetectionEngine(private val context: Context) {
    companion object {
        private const val TAG = "DrivingEventEngine"

        // Event detection thresholds - adjusted for Kenyan road conditions
        private const val HARD_BRAKING_THRESHOLD = -5.5f // m/s² (less sensitive for potholes)
        private const val RAPID_ACCELERATION_THRESHOLD = 4.5f // m/s² (less sensitive for road obstacles)
        private const val HARSH_CORNERING_THRESHOLD = 5.5f // m/s² (less sensitive for swerving around potholes)
        private const val PHONE_USAGE_CONFIDENCE_THRESHOLD = 0.8f

        // Speed thresholds - adjusted for Kenyan speed limits and driving patterns
        private const val MINOR_SPEEDING_THRESHOLD_URBAN = 12f // km/h over limit (50 km/h zones)
        private const val MINOR_SPEEDING_THRESHOLD_RURAL = 15f // km/h over limit (80 km/h zones)
        private const val MINOR_SPEEDING_THRESHOLD_HIGHWAY = 20f // km/h over limit (100 km/h zones)

        private const val MAJOR_SPEEDING_THRESHOLD_URBAN = 20f // km/h over limit
        private const val MAJOR_SPEEDING_THRESHOLD_RURAL = 25f // km/h over limit
        private const val MAJOR_SPEEDING_THRESHOLD_HIGHWAY = 30f // km/h over limit

        private const val EXCESSIVE_SPEEDING_THRESHOLD_URBAN = 30f // km/h over limit
        private const val EXCESSIVE_SPEEDING_THRESHOLD_RURAL = 35f // km/h over limit
        private const val EXCESSIVE_SPEEDING_THRESHOLD_HIGHWAY = 40f // km/h over limit

        // Kenyan speed limits
        private const val URBAN_SPEED_LIMIT = 50f // km/h
        private const val RURAL_SPEED_LIMIT = 80f // km/h
        private const val HIGHWAY_SPEED_LIMIT = 100f // km/h

        // Time windows for event analysis
        private const val EVENT_ANALYSIS_WINDOW = 3000L // 3 seconds
        private const val PATTERN_ANALYSIS_WINDOW = 30000L // 30 seconds
    }

    private val _drivingEvents = MutableLiveData<DrivingEvent>()
    val drivingEvents: LiveData<DrivingEvent> = _drivingEvents

    private val _tripScore = MutableLiveData<TripScore>()
    val tripScore: LiveData<TripScore> = _tripScore

    private var isAnalyzing = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Event detection buffers
    private val accelerationHistory = mutableListOf<TimestampedValue>()
    private val speedHistory = mutableListOf<TimestampedValue>()
    private val locationHistory = mutableListOf<TimestampedLocation>()
    private val gyroscopeHistory = mutableListOf<TimestampedVector>()

    // Trip tracking
    private var currentTrip = mutableListOf<DrivingEvent>()
    private var tripStartTime = 0L
    private var tripDistance = 0f
    private var previousLocation: LocationData? = null

    data class TimestampedValue(val timestamp: Long, val value: Float)
    data class TimestampedVector(val timestamp: Long, val x: Float, val y: Float, val z: Float)
    data class TimestampedLocation(val timestamp: Long, val location: LocationData)

    fun startEventDetection() {
        if (isAnalyzing) return

        isAnalyzing = true
        tripStartTime = System.currentTimeMillis()
        currentTrip.clear()

        scope.launch {
            while (isAnalyzing) {
                delay(500) // Analyze every 500ms
                analyzeEvents()
            }
        }
    }

    fun stopEventDetection() {
        isAnalyzing = false
        if (currentTrip.isNotEmpty()) {
            generateTripScore()
        }
    }

    fun updateMotionData(motionData: MotionData) {
        if (!isAnalyzing) return

        synchronized(this) {
            // Store acceleration data
            accelerationHistory.add(
                TimestampedValue(motionData.timestamp, motionData.accelerationMagnitude)
            )

            // Store gyroscope data for cornering detection
            gyroscopeHistory.add(
                TimestampedVector(
                    motionData.timestamp,
                    motionData.gyroscopeX,
                    motionData.gyroscopeY,
                    motionData.gyroscopeZ
                )
            )

            keepBufferSize(accelerationHistory, 100)
            keepBufferSize(gyroscopeHistory, 100)
        }
    }

    fun updateLocationData(locationData: LocationData) {
        if (!isAnalyzing) return

        synchronized(this) {
            locationHistory.add(TimestampedLocation(locationData.timestamp, locationData))

            locationData.speed?.let { speed ->
                speedHistory.add(TimestampedValue(locationData.timestamp, speed))
            }

            // Calculate trip distance
            previousLocation?.let { prev ->
                val distance = calculateDistance(prev, locationData)
                tripDistance += distance
            }
            previousLocation = locationData

            keepBufferSize(locationHistory, 50)
            keepBufferSize(speedHistory, 100)
        }
    }

    private fun analyzeEvents() {
        val currentTime = System.currentTimeMillis()

        // Analyze different types of events
        detectHardBraking(currentTime)
        detectRapidAcceleration(currentTime)
        detectHarshCornering(currentTime)
        detectSpeeding(currentTime)
        detectPhoneUsage(currentTime)
        detectAggressiveDriving(currentTime)

        // Analyze patterns every 30 seconds
        if (currentTime % PATTERN_ANALYSIS_WINDOW < 1000) {
            analyzeDriverBehaviorPatterns()
        }
    }

    private fun detectHardBraking(currentTime: Long) {
        val recentAccelerations = accelerationHistory
            .filter { currentTime - it.timestamp < EVENT_ANALYSIS_WINDOW }
            .sortedBy { it.timestamp }

        if (recentAccelerations.size < 5) return

        // Calculate deceleration rate
        val decelerationRate = calculateDecelerationRate(recentAccelerations)

        if (decelerationRate < HARD_BRAKING_THRESHOLD) {
            val severity = when {
                decelerationRate < -8.0f -> EventSeverity.CRITICAL
                decelerationRate < -6.0f -> EventSeverity.HIGH
                decelerationRate < -5.0f -> EventSeverity.MEDIUM
                else -> EventSeverity.LOW
            }

            val event = createDrivingEvent(
                DrivingEventType.HARD_BRAKING,
                severity,
                currentTime,
                abs(decelerationRate),
                calculateEventConfidence(decelerationRate, HARD_BRAKING_THRESHOLD)
            )

            recordEvent(event)
        }
    }

    private fun detectRapidAcceleration(currentTime: Long) {
        // Create a thread-safe copy of the acceleration data
        val accelerationSnapshot: List<TimestampedValue>

        synchronized(this) {
            accelerationSnapshot = accelerationHistory.toList()
        }

        val recentAccelerations = accelerationSnapshot
            .filter { currentTime - it.timestamp < EVENT_ANALYSIS_WINDOW }
            .sortedBy { it.timestamp }

        if (recentAccelerations.size < 5) return

        val accelerationRate = calculateAccelerationRate(recentAccelerations)

        if (accelerationRate > RAPID_ACCELERATION_THRESHOLD) {
            val severity = when {
                accelerationRate > 6.0f -> EventSeverity.CRITICAL
                accelerationRate > 5.0f -> EventSeverity.HIGH
                accelerationRate > 4.0f -> EventSeverity.MEDIUM
                else -> EventSeverity.LOW
            }

            val event = createDrivingEvent(
                DrivingEventType.RAPID_ACCELERATION,
                severity,
                currentTime,
                accelerationRate,
                calculateEventConfidence(accelerationRate, RAPID_ACCELERATION_THRESHOLD)
            )

            recordEvent(event)
        }
    }

    private fun detectHarshCornering(currentTime: Long) {
        val recentGyro = gyroscopeHistory
            .filter { currentTime - it.timestamp < EVENT_ANALYSIS_WINDOW }

        if (recentGyro.size < 5) return

        val maxAngularVelocity = recentGyro.maxOfOrNull {
            sqrt(it.x * it.x + it.y * it.y + it.z * it.z)
        } ?: 0f

        // Convert to lateral acceleration approximation
        val currentSpeed = speedHistory.lastOrNull()?.value ?: 0f
        val lateralAcceleration = maxAngularVelocity * currentSpeed

        if (lateralAcceleration > HARSH_CORNERING_THRESHOLD) {
            val severity = when {
                lateralAcceleration > 8.0f -> EventSeverity.CRITICAL
                lateralAcceleration > 6.0f -> EventSeverity.HIGH
                lateralAcceleration > 5.0f -> EventSeverity.MEDIUM
                else -> EventSeverity.LOW
            }

            val event = createDrivingEvent(
                DrivingEventType.HARSH_CORNERING,
                severity,
                currentTime,
                lateralAcceleration,
                calculateEventConfidence(lateralAcceleration, HARSH_CORNERING_THRESHOLD)
            )

            recordEvent(event)
        }
    }

    private fun detectSpeeding(currentTime: Long) {
        val currentSpeed = speedHistory.lastOrNull()?.value ?: return
        val currentLocation = locationHistory.lastOrNull()?.location ?: return

        // Get speed limit and road type for current location
        val speedLimit = getSpeedLimit(currentLocation) ?: return
        val roadType = determineRoadType(speedLimit)

        val speedOver = (currentSpeed * 3.6f) - speedLimit // Convert m/s to km/h

        // Use context-aware thresholds based on road type
        val (minorThreshold, majorThreshold, excessiveThreshold) = getSpeedingThresholds(roadType)

        if (speedOver > minorThreshold) {
            val severity = when {
                speedOver > excessiveThreshold -> EventSeverity.CRITICAL
                speedOver > majorThreshold -> EventSeverity.HIGH
                speedOver > minorThreshold -> EventSeverity.MEDIUM
                else -> EventSeverity.LOW
            }

            // Check duration of speeding with context-aware threshold
            val speedingDuration = calculateSpeedingDuration(speedLimit, minorThreshold)

            val event = createDrivingEvent(
                DrivingEventType.SPEEDING,
                severity,
                currentTime,
                speedOver,
                0.9f // High confidence for GPS-based speeding
            )

            recordEvent(event)
        }
    }

    private fun determineRoadType(speedLimit: Float): RoadType {
        return when (speedLimit) {
            URBAN_SPEED_LIMIT -> RoadType.RESIDENTIAL // Urban areas map to residential
            RURAL_SPEED_LIMIT -> RoadType.ARTERIAL // Rural roads map to arterial
            HIGHWAY_SPEED_LIMIT -> RoadType.HIGHWAY
            else -> RoadType.UNKNOWN
        }
    }

    private fun getSpeedingThresholds(roadType: RoadType): Triple<Float, Float, Float> {
        return when (roadType) {
            RoadType.RESIDENTIAL -> Triple(
                MINOR_SPEEDING_THRESHOLD_URBAN,
                MAJOR_SPEEDING_THRESHOLD_URBAN,
                EXCESSIVE_SPEEDING_THRESHOLD_URBAN
            )
            RoadType.ARTERIAL -> Triple(
                MINOR_SPEEDING_THRESHOLD_RURAL,
                MAJOR_SPEEDING_THRESHOLD_RURAL,
                EXCESSIVE_SPEEDING_THRESHOLD_RURAL
            )
            RoadType.HIGHWAY -> Triple(
                MINOR_SPEEDING_THRESHOLD_HIGHWAY,
                MAJOR_SPEEDING_THRESHOLD_HIGHWAY,
                EXCESSIVE_SPEEDING_THRESHOLD_HIGHWAY
            )
            else -> Triple(
                MINOR_SPEEDING_THRESHOLD_URBAN, // Default to most restrictive
                MAJOR_SPEEDING_THRESHOLD_URBAN,
                EXCESSIVE_SPEEDING_THRESHOLD_URBAN
            )
        }
    }

    private fun detectPhoneUsage(currentTime: Long) {
        // Multi-sensor fusion approach for phone usage detection
        val phoneUsageProbability = calculatePhoneUsageProbability(currentTime)

        if (phoneUsageProbability > PHONE_USAGE_CONFIDENCE_THRESHOLD) {
            val severity = when {
                phoneUsageProbability > 0.95f -> EventSeverity.CRITICAL
                phoneUsageProbability > 0.90f -> EventSeverity.HIGH
                phoneUsageProbability > 0.85f -> EventSeverity.MEDIUM
                else -> EventSeverity.LOW
            }

            val event = createDrivingEvent(
                DrivingEventType.PHONE_USAGE,
                severity,
                currentTime,
                phoneUsageProbability,
                phoneUsageProbability
            )

            recordEvent(event)
        }
    }

    private fun calculatePhoneUsageProbability(currentTime: Long): Float {
        var confidenceScore = 0f
        var factorCount = 0

        // 1. Motion Pattern Analysis - detect hand-to-ear movements
        val handMovementScore = analyzeHandMovementPatterns(currentTime)
        if (handMovementScore > 0) {
            confidenceScore += handMovementScore * 0.25f
            factorCount++
        }

        // 2. Driving Pattern Disruption - detect distracted driving patterns
        val drivingDisruptionScore = analyzeDrivingDisruption(currentTime)
        if (drivingDisruptionScore > 0) {
            confidenceScore += drivingDisruptionScore * 0.30f
            factorCount++
        }

        // 3. Device Orientation Changes - detect phone pickup/positioning
        val orientationScore = analyzeDeviceOrientationChanges(currentTime)
        if (orientationScore > 0) {
            confidenceScore += orientationScore * 0.20f
            factorCount++
        }

        // 4. Audio Pattern Analysis - detect call audio patterns
        val audioScore = analyzeAudioPatterns(currentTime)
        if (audioScore > 0) {
            confidenceScore += audioScore * 0.15f
            factorCount++
        }

        // 5. Speed and Acceleration Correlation - detect typical phone usage driving
        val speedCorrelationScore = analyzeSpeedCorrelationPatterns(currentTime)
        if (speedCorrelationScore > 0) {
            confidenceScore += speedCorrelationScore * 0.10f
            factorCount++
        }

        // Return normalized confidence score
        return if (factorCount > 0) confidenceScore else 0f
    }

    private fun analyzeHandMovementPatterns(currentTime: Long): Float {
        val recentMotion = accelerationHistory
            .filter { currentTime - it.timestamp < EVENT_ANALYSIS_WINDOW }
            .takeLast(20)

        if (recentMotion.size < 10) return 0f

        // Detect characteristic hand-to-ear movement patterns
        var phonePickupIndicators = 0
        var totalSamples = 0

        for (i in 1 until recentMotion.size) {
            val current = recentMotion[i].value
            val previous = recentMotion[i - 1].value
            val acceleration = abs(current - previous)

            totalSamples++

            // Look for sudden upward movements followed by stabilization
            if (acceleration > 2.0f && i < recentMotion.size - 3) {
                val nextFew = recentMotion.subList(i + 1, minOf(i + 4, recentMotion.size))
                val avgNext = nextFew.map { it.value }.average().toFloat()

                // If movement stabilizes after sudden change, indicates phone positioning
                if (abs(avgNext - current) < 1.0f) {
                    phonePickupIndicators++
                }
            }
        }

        return if (totalSamples > 0) {
            (phonePickupIndicators.toFloat() / totalSamples * 4).coerceAtMost(1f)
        } else 0f
    }

    private fun analyzeDrivingDisruption(currentTime: Long): Float {
        val recentEvents = currentTrip.filter {
            currentTime - it.timestamp < PATTERN_ANALYSIS_WINDOW
        }

        // Look for patterns indicating distracted driving
        var disruptionScore = 0f

        // Check for increased lane corrections (harsh cornering)
        val harshCorneringCount = recentEvents.count {
            it.eventType == DrivingEventType.HARSH_CORNERING
        }
        if (harshCorneringCount > 2) {
            disruptionScore += 0.4f
        }

        // Check for inconsistent speed patterns
        val speedVariations = analyzeSpeedVariations(currentTime)
        if (speedVariations > 0.3f) {
            disruptionScore += 0.3f
        }

        // Check for delayed reactions (sudden braking)
        val delayedReactions = recentEvents.count {
            it.eventType == DrivingEventType.HARD_BRAKING && it.severity.value >= 2
        }
        if (delayedReactions > 1) {
            disruptionScore += 0.3f
        }

        return disruptionScore.coerceAtMost(1f)
    }

    private fun analyzeDeviceOrientationChanges(currentTime: Long): Float {
        val recentGyro = gyroscopeHistory
            .filter { currentTime - it.timestamp < EVENT_ANALYSIS_WINDOW }
            .takeLast(15)

        if (recentGyro.size < 5) return 0f

        // Detect rapid orientation changes indicating phone handling
        var orientationChanges = 0
        val threshold = 1.5f // rad/s threshold for significant rotation

        for (i in 1 until recentGyro.size) {
            val current = recentGyro[i]
            val previous = recentGyro[i - 1]

            val rotationMagnitude = sqrt(
                (current.x - previous.x).pow(2) +
                        (current.y - previous.y).pow(2) +
                        (current.z - previous.z).pow(2)
            )

            if (rotationMagnitude > threshold) {
                orientationChanges++
            }
        }

        // Multiple orientation changes suggest phone handling
        return when {
            orientationChanges >= 3 -> 1f
            orientationChanges == 2 -> 0.7f
            orientationChanges == 1 -> 0.3f
            else -> 0f
        }
    }

    private fun analyzeAudioPatterns(currentTime: Long): Float {
        // This would integrate with AudioTelemetryData when available
        // For now, we'll analyze indirect audio indicators through motion patterns

        // Look for periodic motion patterns that might indicate talking
        val recentMotion = accelerationHistory
            .filter { currentTime - it.timestamp < 10000L } // 10 second window
            .takeLast(50)

        if (recentMotion.size < 20) return 0f

        // Detect rhythmic patterns that might indicate speech
        val periodicPatterns = detectPeriodicMotion(recentMotion)

        return if (periodicPatterns > 0.6f) 0.8f else 0f
    }

    private fun analyzeSpeedCorrelationPatterns(currentTime: Long): Float {
        val recentSpeeds = speedHistory
            .filter { currentTime - it.timestamp < PATTERN_ANALYSIS_WINDOW }
            .map { it.value * 3.6f } // Convert to km/h

        if (recentSpeeds.size < 10) return 0f

        // Phone usage often correlates with specific speed patterns:
        // 1. Maintaining steady highway speeds (cruise control-like)
        // 2. Or erratic city driving with sudden speed changes

        val speedVariance = calculateVariance(recentSpeeds)
        val avgSpeed = recentSpeeds.average().toFloat()

        return when {
            // Highway scenario: very steady speeds (low variance) at high speed
            avgSpeed > 60f && speedVariance < 25f -> 0.6f
            // City scenario: high variance indicating distracted driving
            avgSpeed < 60f && speedVariance > 100f -> 0.7f
            else -> 0f
        }
    }

    private fun analyzeSpeedVariations(currentTime: Long): Float {
        val recentSpeeds = speedHistory
            .filter { currentTime - it.timestamp < 15000L } // 15 second window
            .map { it.value * 3.6f }

        if (recentSpeeds.size < 5) return 0f

        val variance = calculateVariance(recentSpeeds)
        val avgSpeed = recentSpeeds.average().toFloat()

        // Normalize variance relative to average speed
        val normalizedVariance = if (avgSpeed > 0) variance / avgSpeed else 0f

        return (normalizedVariance / 50f).coerceAtMost(1f) // Scale appropriately
    }

    private fun detectPeriodicMotion(motionData: List<TimestampedValue>): Float {
        if (motionData.size < 10) return 0f

        // Simple periodic pattern detection using autocorrelation approach
        val values = motionData.map { it.value }
        var maxCorrelation = 0f

        // Check for periods between 0.5-3 seconds (typical speech patterns)
        for (lag in 3..15) { // Assuming ~200ms sampling rate
            if (lag >= values.size) break

            var correlation = 0f
            val validPairs = values.size - lag

            for (i in 0 until validPairs) {
                correlation += values[i] * values[i + lag]
            }

            correlation /= validPairs
            maxCorrelation = maxOf(maxCorrelation, abs(correlation))
        }

        return (maxCorrelation / 10f).coerceAtMost(1f) // Normalize
    }

    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f

        val mean = values.average().toFloat()
        val sumSquaredDiffs = values.sumOf { (it - mean).pow(2).toDouble() }

        return (sumSquaredDiffs / values.size).toFloat()
    }

    private fun detectAggressiveDriving(currentTime: Long) {
        // Analyze patterns of multiple aggressive behaviors
        val recentEvents = currentTrip.filter {
            currentTime - it.timestamp < PATTERN_ANALYSIS_WINDOW
        }

        val aggressiveEventCount = recentEvents.count { event ->
            event.eventType in listOf(
                DrivingEventType.HARD_BRAKING,
                DrivingEventType.RAPID_ACCELERATION,
                DrivingEventType.HARSH_CORNERING,
                DrivingEventType.SPEEDING
            ) && event.severity.value >= 2
        }

        if (aggressiveEventCount >= 3) {
            val event = createDrivingEvent(
                DrivingEventType.AGGRESSIVE_DRIVING,
                EventSeverity.HIGH,
                currentTime,
                aggressiveEventCount.toFloat(),
                0.8f
            )

            recordEvent(event)
        }
    }

    private fun analyzeDriverBehaviorPatterns() {
        // Analyze overall driving patterns and award positive behaviors
        val recentEvents = currentTrip.filter {
            System.currentTimeMillis() - it.timestamp < PATTERN_ANALYSIS_WINDOW
        }

        // Detect smooth driving (absence of harsh events)
        if (recentEvents.none { it.severity.value >= 2 }) {
            val avgSpeed = speedHistory.takeLast(20).map { it.value }.average().toFloat()
            if (avgSpeed > 5f) { // Only if actually driving
                val event = createDrivingEvent(
                    DrivingEventType.SMOOTH_DRIVING,
                    EventSeverity.LOW,
                    System.currentTimeMillis(),
                    1f,
                    0.9f
                )
                recordEvent(event)
            }
        }

        // Detect eco-driving (efficient acceleration/deceleration patterns)
        detectEcoDriving()
    }

    private fun detectEcoDriving() {
        // Analyze for fuel-efficient driving patterns
        val recentAccelerations = accelerationHistory.takeLast(50)
        if (recentAccelerations.size < 10) return

        val gentleAccelerations = recentAccelerations.count {
            it.value in 0.5f..2.0f
        }
        val totalAccelerations = recentAccelerations.size

        val ecoScore = gentleAccelerations.toFloat() / totalAccelerations

        if (ecoScore > 0.8f) {
            val event = createDrivingEvent(
                DrivingEventType.ECO_DRIVING,
                EventSeverity.LOW,
                System.currentTimeMillis(),
                ecoScore,
                0.7f
            )
            recordEvent(event)
        }
    }

    private fun createDrivingEvent(
        eventType: DrivingEventType,
        severity: EventSeverity,
        timestamp: Long,
        magnitude: Float,
        confidence: Float,
        duration: Long = 0L
    ): DrivingEvent {
        val currentLocation = locationHistory.lastOrNull()?.location
        val currentSpeed = speedHistory.lastOrNull()?.value ?: 0f

        return DrivingEvent(
            eventType = eventType,
            severity = severity,
            timestamp = timestamp,
            location = currentLocation,
            speed = currentSpeed,
            acceleration = magnitude,
            duration = duration,
            confidence = confidence,
            context = createEventContext()
        )
    }

    private fun createEventContext(): EventContext {
        // This would integrate with weather APIs, traffic data, etc.
        return EventContext(
            weatherConditions = null, // Would get from weather API
            trafficDensity = TrafficDensity.MODERATE, // Would get from traffic API
            roadType = RoadType.UNKNOWN, // Would get from mapping API
            timeOfDay = getCurrentTimeOfDay(),
            isRushHour = isRushHour(),
            schoolZone = false, // Would check against school zone database
            constructionZone = false // Would check against construction database
        )
    }

    private fun recordEvent(event: DrivingEvent) {
        currentTrip.add(event)
        _drivingEvents.postValue(event)
    }

    private fun generateTripScore() {
        if (currentTrip.isEmpty()) return

        val tripDuration = System.currentTimeMillis() - tripStartTime
        val avgSpeed = speedHistory.map { it.value }.average().toFloat() * 3.6f // Convert to km/h
        val maxSpeed = speedHistory.maxOfOrNull { it.value }?.times(3.6f) ?: 0f

        // Calculate individual scores
        val safetyScore = calculateSafetyScore()
        val efficiencyScore = calculateEfficiencyScore()
        val smoothnessScore = calculateSmoothnessScore()
        val legalComplianceScore = calculateLegalComplianceScore()

        val overallScore = (safetyScore + efficiencyScore + smoothnessScore + legalComplianceScore) / 4f

        val tripStatistics = TripStatistics(
            totalDistance = tripDistance,
            totalDuration = tripDuration,
            averageSpeed = avgSpeed,
            maxSpeed = maxSpeed,
            speedingDuration = calculateTotalSpeedingDuration(),
            idleTime = calculateIdleTime(),
            fuelEfficiencyScore = efficiencyScore,
            nightDrivingPercentage = calculateNightDrivingPercentage(),
            highRiskRoadPercentage = 0f // Would calculate based on road data
        )

        val riskFactors = identifyRiskFactors()

        val tripScore = TripScore(
            overallScore = overallScore,
            safetyScore = safetyScore,
            efficiencyScore = efficiencyScore,
            smoothnessScore = smoothnessScore,
            legalComplianceScore = legalComplianceScore,
            events = currentTrip.toList(),
            tripStatistics = tripStatistics,
            riskFactors = riskFactors
        )

        _tripScore.postValue(tripScore)
    }

    // Helper calculation methods
    private fun calculateDecelerationRate(accelerations: List<TimestampedValue>): Float {
        if (accelerations.size < 2) return 0f

        val startAccel = accelerations.first().value
        val endAccel = accelerations.last().value
        val timeDiff = (accelerations.last().timestamp - accelerations.first().timestamp) / 1000f

        return if (timeDiff > 0) (endAccel - startAccel) / timeDiff else 0f
    }

    private fun calculateAccelerationRate(accelerations: List<TimestampedValue>): Float {
        return abs(calculateDecelerationRate(accelerations))
    }

    private fun calculateEventConfidence(actualValue: Float, threshold: Float): Float {
        val ratio = abs(actualValue) / abs(threshold)
        return (ratio.coerceAtMost(3f) / 3f).coerceIn(0f, 1f)
    }

    private fun getSpeedLimit(location: LocationData): Float? {
        // Would integrate with mapping service to get actual speed limits
        // For now, return placeholder based on typical road types
        return 50f // km/h default
    }

    private fun calculateSpeedingDuration(speedLimit: Float, minorThreshold: Float): Long {
        val currentTime = System.currentTimeMillis()
        return speedHistory
            .filter { currentTime - it.timestamp < 30000 } // Last 30 seconds
            .count { (it.value * 3.6f) > speedLimit + minorThreshold }
            .times(1000L) // Approximate duration in ms
    }

    private fun calculateSafetyScore(): Float {
        val safetyEvents = currentTrip.filter {
            it.eventType in listOf(
                DrivingEventType.HARD_BRAKING,
                DrivingEventType.RAPID_ACCELERATION,
                DrivingEventType.HARSH_CORNERING,
                DrivingEventType.PHONE_USAGE
            )
        }

        val penaltyPoints = safetyEvents.sumOf { it.severity.value }
        val maxPossiblePenalty = currentTrip.size * 4 // Assuming max severity of 4

        return if (maxPossiblePenalty > 0) {
            ((maxPossiblePenalty - penaltyPoints).toFloat() / maxPossiblePenalty * 100f).coerceAtLeast(0f)
        } else 100f
    }

    private fun calculateEfficiencyScore(): Float {
        val ecoEvents = currentTrip.count { it.eventType == DrivingEventType.ECO_DRIVING }
        val aggressiveEvents = currentTrip.count {
            it.eventType in listOf(DrivingEventType.RAPID_ACCELERATION, DrivingEventType.HARD_BRAKING)
        }

        val totalEvents = maxOf(1, ecoEvents + aggressiveEvents)
        return (ecoEvents.toFloat() / totalEvents * 100f).coerceAtMost(100f)
    }

    private fun calculateSmoothnessScore(): Float {
        val smoothEvents = currentTrip.count { it.eventType == DrivingEventType.SMOOTH_DRIVING }
        val harshEvents = currentTrip.count {
            it.eventType in listOf(
                DrivingEventType.HARD_BRAKING,
                DrivingEventType.RAPID_ACCELERATION,
                DrivingEventType.HARSH_CORNERING
            )
        }

        val totalEvents = maxOf(1, smoothEvents + harshEvents)
        return (smoothEvents.toFloat() / totalEvents * 100f).coerceAtMost(100f)
    }

    private fun calculateLegalComplianceScore(): Float {
        val speedingEvents = currentTrip.filter { it.eventType == DrivingEventType.SPEEDING }
        val totalSpeedingDuration = speedingEvents.sumOf { it.duration }
        val tripDuration = System.currentTimeMillis() - tripStartTime

        return if (tripDuration > 0) {
            ((tripDuration - totalSpeedingDuration).toFloat() / tripDuration * 100f).coerceAtLeast(0f)
        } else 100f
    }

    private fun calculateTotalSpeedingDuration(): Long {
        return currentTrip
            .filter { it.eventType == DrivingEventType.SPEEDING }
            .sumOf { it.duration }
    }

    private fun calculateIdleTime(): Long {
        return speedHistory.count { it.value < 0.5f }.times(1000L) // Approximate
    }

    private fun calculateNightDrivingPercentage(): Float {
        val nightHours = 22..6
        val tripHours = tripStartTime.let { start ->
            val startHour = java.util.Calendar.getInstance().apply { timeInMillis = start }.get(java.util.Calendar.HOUR_OF_DAY)
            val endHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            startHour..endHour
        }

        return if (tripHours.any { it in nightHours }) 50f else 0f // Simplified calculation
    }

    private fun identifyRiskFactors(): List<RiskFactor> {
        val riskFactors = mutableListOf<RiskFactor>()

        val speedingCount = currentTrip.count { it.eventType == DrivingEventType.SPEEDING }
        if (speedingCount > 0) {
            riskFactors.add(
                RiskFactor(
                    RiskFactorType.SPEEDING,
                    -speedingCount * 6f, // Reduced from 10f to be less harsh
                    speedingCount,
                    "Speeding detected $speedingCount times during trip"
                )
            )
        }

        val aggressiveCount = currentTrip.count { it.eventType == DrivingEventType.AGGRESSIVE_DRIVING }
        if (aggressiveCount > 0) {
            riskFactors.add(
                RiskFactor(
                    RiskFactorType.AGGRESSIVE_ACCELERATION,
                    -aggressiveCount * 10f, // Reduced from 15f to be less harsh
                    aggressiveCount,
                    "Aggressive driving patterns detected"
                )
            )
        }

        // Add context for road condition events
        val hardBrakingCount = currentTrip.count { it.eventType == DrivingEventType.HARD_BRAKING }
        val harshCorneringCount = currentTrip.count { it.eventType == DrivingEventType.HARSH_CORNERING }

        // Only penalize if excessive (accounting for road conditions)
        if (hardBrakingCount > 3) {
            riskFactors.add(
                RiskFactor(
                    RiskFactorType.HARD_BRAKING, // Using available enum value
                    -(hardBrakingCount - 3) * 4f, // Less penalty, threshold for road conditions
                    hardBrakingCount,
                    "Frequent hard braking detected (${hardBrakingCount} times)"
                )
            )
        }

        // Note: No specific harsh cornering risk factor type available, so we group it under aggressive acceleration
        if (harshCorneringCount > 3) {
            riskFactors.add(
                RiskFactor(
                    RiskFactorType.AGGRESSIVE_ACCELERATION, // Grouping harsh cornering under aggressive behavior
                    -(harshCorneringCount - 3) * 3f, // Less penalty, threshold for road conditions
                    harshCorneringCount,
                    "Frequent harsh cornering detected (${harshCorneringCount} times)"
                )
            )
        }

        return riskFactors
    }

    private fun getCurrentTimeOfDay(): TimeOfDay {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return TimeOfDay.values().find { hour in it.startHour until it.endHour } ?: TimeOfDay.NIGHT
    }

    private fun isRushHour(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour in 7..9 || hour in 17..19
    }

    private fun calculateDistance(start: LocationData, end: LocationData): Float {
        // Haversine formula for distance calculation
        val R = 6371000f // Earth's radius in meters
        val lat1Rad = Math.toRadians(start.latitude)
        val lat2Rad = Math.toRadians(end.latitude)
        val deltaLatRad = Math.toRadians(end.latitude - start.latitude)
        val deltaLonRad = Math.toRadians(end.longitude - start.longitude)

        val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2) * sin(deltaLonRad / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (R * c).toFloat() / 1000f // Convert to km
    }

    private fun <T> keepBufferSize(buffer: MutableList<T>, maxSize: Int) {
        while (buffer.size > maxSize) {
            buffer.removeAt(0)
        }
    }
}
