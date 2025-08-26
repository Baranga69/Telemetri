package com.commerin.telemetri.core

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.commerin.telemetri.domain.model.*
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Comprehensive risk assessment engine for insurance telematics
 * Provides real-time risk scoring and driver profiling
 */
class RiskAssessmentEngine(private val context: Context) {
    companion object {
        private const val TAG = "RiskAssessmentEngine"
        private const val PROFILE_UPDATE_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
        private const val RISK_ANALYSIS_WINDOW = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    private val _driverProfile = MutableLiveData<DriverProfile>()
    val driverProfile: LiveData<DriverProfile> = _driverProfile

    private val _riskScore = MutableLiveData<Float>()
    val riskScore: LiveData<Float> = _riskScore

    private val _insurancePremiumEstimate = MutableLiveData<InsurancePremiumEstimate>()
    val insurancePremiumEstimate: LiveData<InsurancePremiumEstimate> = _insurancePremiumEstimate

    private var isAnalyzing = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Historical data storage
    private val tripHistory = mutableListOf<TripScore>()
    private val eventHistory = mutableListOf<DrivingEvent>()
    private val behaviorHistory = mutableListOf<BehaviorSnapshot>()

    data class BehaviorSnapshot(
        val timestamp: Long,
        val safetyScore: Float,
        val aggressivenessLevel: Float,
        val attentivenessLevel: Float,
        val complianceLevel: Float
    )

    data class InsurancePremiumEstimate(
        val basePremium: Float,
        val riskMultiplier: Float,
        val estimatedPremium: Float,
        val discountEligible: Boolean,
        val discountPercentage: Float,
        val riskFactors: List<RiskFactor>,
        val recommendations: List<String>
    )

    fun startRiskAssessment(driverId: String) {
        if (isAnalyzing) return

        isAnalyzing = true

        scope.launch {
            while (isAnalyzing) {
                analyzeRiskProfile(driverId)
                delay(PROFILE_UPDATE_INTERVAL)
            }
        }
    }

    fun stopRiskAssessment() {
        isAnalyzing = false
    }

    fun addTripScore(tripScore: TripScore) {
        tripHistory.add(tripScore)
        eventHistory.addAll(tripScore.events)

        // Maintain history size
        if (tripHistory.size > 100) {
            tripHistory.removeAt(0)
        }

        // Update real-time risk score
        updateRealTimeRiskScore()
    }

    fun addDrivingEvent(event: DrivingEvent) {
        eventHistory.add(event)

        // Immediate risk impact for critical events
        if (event.severity == EventSeverity.CRITICAL) {
            updateRealTimeRiskScore()
        }
    }

    private fun analyzeRiskProfile(driverId: String) {
        val recentTrips = getRecentTrips()
        val recentEvents = getRecentEvents()

        val drivingHistory = calculateDrivingHistory(recentTrips, recentEvents)
        val behaviorTrends = calculateBehaviorTrends()
        val riskCategory = determineRiskCategory(drivingHistory, behaviorTrends)
        val preferredRoutes = analyzeRoutePatterns()
        val drivingHabits = analyzeDrivingHabits()

        val profile = DriverProfile(
            driverId = driverId,
            totalMileage = drivingHistory.totalDistance,
            safetyRating = calculateSafetyRating(drivingHistory),
            riskCategory = riskCategory,
            drivingHistory = drivingHistory,
            behaviorTrends = behaviorTrends,
            preferredRoutes = preferredRoutes,
            drivingHabits = drivingHabits
        )

        _driverProfile.postValue(profile)

        // Calculate insurance premium estimate
        val premiumEstimate = calculateInsurancePremiumEstimate(profile)
        _insurancePremiumEstimate.postValue(premiumEstimate)
    }

    private fun updateRealTimeRiskScore() {
        val recentEvents = getRecentEvents(24 * 60 * 60 * 1000L) // Last 24 hours
        val recentTrips = getRecentTrips(7 * 24 * 60 * 60 * 1000L) // Last 7 days

        val immediateRisk = calculateImmediateRisk(recentEvents)
        val historicalRisk = calculateHistoricalRisk(recentTrips)
        val combinedRisk = (immediateRisk * 0.3f) + (historicalRisk * 0.7f)

        _riskScore.postValue(combinedRisk)
    }

    private fun calculateImmediateRisk(events: List<DrivingEvent>): Float {
        if (events.isEmpty()) return 0f

        val riskPoints = events.sumOf { event ->
            when (event.eventType) {
                DrivingEventType.HARD_BRAKING -> event.severity.value * 2
                DrivingEventType.RAPID_ACCELERATION -> event.severity.value * 2
                DrivingEventType.HARSH_CORNERING -> event.severity.value * 3
                DrivingEventType.SPEEDING -> event.severity.value * 4
                DrivingEventType.PHONE_USAGE -> event.severity.value * 5
                DrivingEventType.DISTRACTED_DRIVING -> event.severity.value * 6
                DrivingEventType.AGGRESSIVE_DRIVING -> event.severity.value * 4
                else -> 0
            }
        }

        // Normalize to 0-100 scale
        val maxPossibleRisk = events.size * 6 * 4 // Max severity * max weight
        return if (maxPossibleRisk > 0) {
            (riskPoints.toFloat() / maxPossibleRisk * 100f).coerceAtMost(100f)
        } else 0f
    }

    private fun calculateHistoricalRisk(trips: List<TripScore>): Float {
        if (trips.isEmpty()) return 50f // Neutral risk for new drivers

        val avgSafetyScore = trips.map { it.safetyScore }.average().toFloat()
        val avgComplianceScore = trips.map { it.legalComplianceScore }.average().toFloat()
        val avgSmoothnessScore = trips.map { it.smoothnessScore }.average().toFloat()

        val overallScore = (avgSafetyScore + avgComplianceScore + avgSmoothnessScore) / 3f

        // Convert score to risk (inverse relationship)
        return 100f - overallScore
    }

    private fun calculateDrivingHistory(trips: List<TripScore>, events: List<DrivingEvent>): DrivingHistory {
        val totalDistance = trips.sumOf { it.tripStatistics.totalDistance.toDouble() }.toFloat()
        val totalTime = trips.sumOf { it.tripStatistics.totalDuration }
        val avgTripScore = if (trips.isNotEmpty()) trips.map { it.overallScore }.average().toFloat() else 0f

        // Calculate improvement trend (last 30 days vs previous 30 days)
        val improvementTrend = calculateImprovementTrend(trips)

        return DrivingHistory(
            totalTrips = trips.size,
            totalDistance = totalDistance,
            totalDrivingTime = totalTime,
            accidentHistory = extractAccidentHistory(events),
            violationHistory = extractViolationHistory(events),
            averageTripScore = avgTripScore,
            improvementTrend = improvementTrend
        )
    }

    private fun calculateBehaviorTrends(): BehaviorTrends {
        val recentBehavior = behaviorHistory.takeLast(30) // Last 30 snapshots
        val olderBehavior = behaviorHistory.dropLast(30).takeLast(30)

        return BehaviorTrends(
            speedingTrend = calculateTrend(
                recentBehavior.map { it.complianceLevel },
                olderBehavior.map { it.complianceLevel }
            ),
            aggressivenessTrend = calculateTrend(
                recentBehavior.map { it.aggressivenessLevel },
                olderBehavior.map { it.aggressivenessLevel }
            ),
            smoothnessTrend = calculateTrend(
                recentBehavior.map { it.safetyScore },
                olderBehavior.map { it.safetyScore }
            ),
            attentivenessTrend = calculateTrend(
                recentBehavior.map { it.attentivenessLevel },
                olderBehavior.map { it.attentivenessLevel }
            ),
            overallSafetyTrend = calculateTrend(
                recentBehavior.map { (it.safetyScore + it.complianceLevel) / 2f },
                olderBehavior.map { (it.safetyScore + it.complianceLevel) / 2f }
            )
        )
    }

    private fun calculateTrend(recentValues: List<Float>, olderValues: List<Float>): Trend {
        val recentAvg = if (recentValues.isNotEmpty()) recentValues.average().toFloat() else 0f
        val olderAvg = if (olderValues.isNotEmpty()) olderValues.average().toFloat() else recentAvg

        val thirtyDayChange = recentAvg - olderAvg
        val sixMonthChange = thirtyDayChange * 6 // Approximate

        val direction = when {
            thirtyDayChange > 2f -> TrendDirection.IMPROVING
            thirtyDayChange < -2f -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }

        return Trend(
            currentValue = recentAvg,
            thirtyDayChange = thirtyDayChange,
            sixMonthChange = sixMonthChange,
            direction = direction
        )
    }

    private fun determineRiskCategory(history: DrivingHistory, trends: BehaviorTrends): RiskCategory {
        val safetyScore = history.averageTripScore
        val improvementTrend = history.improvementTrend
        val overallTrend = trends.overallSafetyTrend.direction

        return when {
            safetyScore >= 90f && overallTrend == TrendDirection.IMPROVING -> RiskCategory.VERY_LOW
            safetyScore >= 80f && improvementTrend >= 0f -> RiskCategory.LOW
            safetyScore >= 70f -> RiskCategory.MODERATE
            safetyScore >= 60f || overallTrend == TrendDirection.IMPROVING -> RiskCategory.HIGH
            else -> RiskCategory.VERY_HIGH
        }
    }

    private fun calculateInsurancePremiumEstimate(profile: DriverProfile): InsurancePremiumEstimate {
        val basePremium = 1200f // Base annual premium

        // Calculate risk multiplier based on various factors
        var riskMultiplier = 1f

        // Safety rating impact (50% weight)
        val safetyImpact = when {
            profile.safetyRating >= 90f -> 0.8f  // 20% discount
            profile.safetyRating >= 80f -> 0.9f  // 10% discount
            profile.safetyRating >= 70f -> 1.0f  // No change
            profile.safetyRating >= 60f -> 1.2f  // 20% increase
            else -> 1.5f                         // 50% increase
        }
        riskMultiplier *= safetyImpact

        // Risk category impact (30% weight)
        val categoryImpact = when (profile.riskCategory) {
            RiskCategory.VERY_LOW -> 0.85f
            RiskCategory.LOW -> 0.95f
            RiskCategory.MODERATE -> 1.0f
            RiskCategory.HIGH -> 1.3f
            RiskCategory.VERY_HIGH -> 1.6f
        }
        riskMultiplier *= categoryImpact

        // Mileage impact (20% weight)
        val mileageImpact = when {
            profile.totalMileage < 5000f -> 0.9f   // Low mileage discount
            profile.totalMileage < 15000f -> 1.0f  // Average
            profile.totalMileage < 25000f -> 1.1f  // High mileage
            else -> 1.2f                           // Very high mileage
        }
        riskMultiplier *= mileageImpact

        val estimatedPremium = basePremium * riskMultiplier

        // Determine discount eligibility
        val discountEligible = profile.safetyRating >= 85f &&
                              profile.riskCategory in listOf(RiskCategory.VERY_LOW, RiskCategory.LOW)

        val discountPercentage = if (discountEligible) {
            when {
                profile.safetyRating >= 95f -> 25f
                profile.safetyRating >= 90f -> 20f
                else -> 15f
            }
        } else 0f

        val riskFactors = identifyPremiumRiskFactors(profile)
        val recommendations = generateRecommendations(profile)

        return InsurancePremiumEstimate(
            basePremium = basePremium,
            riskMultiplier = riskMultiplier,
            estimatedPremium = estimatedPremium,
            discountEligible = discountEligible,
            discountPercentage = discountPercentage,
            riskFactors = riskFactors,
            recommendations = recommendations
        )
    }

    private fun identifyPremiumRiskFactors(profile: DriverProfile): List<RiskFactor> {
        val factors = mutableListOf<RiskFactor>()

        // Analyze violation history
        profile.drivingHistory.violationHistory.groupBy { it.violationType }
            .forEach { (type, violations) ->
                if (violations.isNotEmpty()) {
                    factors.add(
                        RiskFactor(
                            type = when (type) {
                                ViolationType.SPEEDING -> RiskFactorType.SPEEDING
                                ViolationType.PHONE_USAGE -> RiskFactorType.PHONE_USAGE
                                else -> RiskFactorType.SPEEDING
                            },
                            impact = -violations.size * 10f,
                            frequency = violations.size,
                            description = "${violations.size} ${type.name.lowercase()} violations"
                        )
                    )
                }
            }

        // Analyze behavior trends
        if (profile.behaviorTrends.aggressivenessTrend.direction == TrendDirection.DECLINING) {
            factors.add(
                RiskFactor(
                    type = RiskFactorType.AGGRESSIVE_ACCELERATION,
                    impact = -15f,
                    frequency = 1,
                    description = "Increasing aggressive driving behavior"
                )
            )
        }

        return factors
    }

    private fun generateRecommendations(profile: DriverProfile): List<String> {
        val recommendations = mutableListOf<String>()

        if (profile.safetyRating < 80f) {
            recommendations.add("Focus on smoother acceleration and braking to improve your safety score")
        }

        if (profile.behaviorTrends.speedingTrend.direction == TrendDirection.DECLINING) {
            recommendations.add("Monitor your speed more closely to avoid speeding violations")
        }

        if (profile.drivingHabits.phoneUsageFrequency > 0.1f) {
            recommendations.add("Reduce phone usage while driving to lower your risk profile")
        }

        if (profile.drivingHabits.preferredDrivingTimes.contains(TimeOfDay.NIGHT)) {
            recommendations.add("Consider avoiding night driving when possible to reduce risk")
        }

        return recommendations
    }

    // Helper methods
    private fun getRecentTrips(timeWindow: Long = RISK_ANALYSIS_WINDOW): List<TripScore> {
        val cutoffTime = System.currentTimeMillis() - timeWindow
        return tripHistory.filter { it.tripStatistics.totalDuration > cutoffTime }
    }

    private fun getRecentEvents(timeWindow: Long = RISK_ANALYSIS_WINDOW): List<DrivingEvent> {
        val cutoffTime = System.currentTimeMillis() - timeWindow
        return eventHistory.filter { it.timestamp > cutoffTime }
    }

    private fun calculateSafetyRating(history: DrivingHistory): Float {
        // Combine multiple factors for safety rating
        val baseScore = history.averageTripScore
        val accidentPenalty = history.accidentHistory.sumOf {
            when (it.severity) {
                AccidentSeverity.SEVERE -> 20.0  // Double values (no 'f' suffix)
                AccidentSeverity.MAJOR -> 15.0
                AccidentSeverity.MODERATE -> 10.0
                AccidentSeverity.MINOR -> 5.0
            }
        }.toFloat()  // Convert the final result to Float

        return (baseScore - accidentPenalty).coerceAtLeast(0f)
    }

    private fun calculateImprovementTrend(trips: List<TripScore>): Float {
        if (trips.size < 10) return 0f

        val recentTrips = trips.takeLast(5)
        val olderTrips = trips.dropLast(5).takeLast(5)

        val recentAvg = recentTrips.map { it.overallScore }.average().toFloat()
        val olderAvg = olderTrips.map { it.overallScore }.average().toFloat()

        return recentAvg - olderAvg
    }

    private fun extractAccidentHistory(events: List<DrivingEvent>): List<AccidentRecord> {
        // This would integrate with claims data or accident detection
        return emptyList() // Placeholder
    }

    private fun extractViolationHistory(events: List<DrivingEvent>): List<ViolationRecord> {
        // Convert driving events to violation records
        return events.filter {
            it.eventType in listOf(DrivingEventType.SPEEDING, DrivingEventType.PHONE_USAGE)
        }.map { event ->
            ViolationRecord(
                timestamp = event.timestamp,
                violationType = when (event.eventType) {
                    DrivingEventType.SPEEDING -> ViolationType.SPEEDING
                    DrivingEventType.PHONE_USAGE -> ViolationType.PHONE_USAGE
                    else -> ViolationType.RECKLESS_DRIVING
                },
                fineAmount = null,
                location = event.location,
                detectedByTelematics = true
            )
        }
    }

    private fun analyzeRoutePatterns(): List<RoutePattern> {
        // Analyze frequently used routes for risk assessment
        return emptyList() // Placeholder - would implement route clustering
    }

    private fun analyzeDrivingHabits(): DrivingHabits {
        // Analyze driving patterns and habits
        return DrivingHabits(
            preferredDrivingTimes = listOf(TimeOfDay.MORNING_RUSH, TimeOfDay.EVENING_RUSH),
            averageTripDistance = tripHistory.map { it.tripStatistics.totalDistance }.average().toFloat(),
            weekdayVsWeekendRatio = 0.7f, // Placeholder
            phoneUsageFrequency = 0.05f, // Placeholder
            speedingTendency = 0.1f, // Placeholder
            aggressivenessFactor = 0.2f // Placeholder
        )
    }
}
