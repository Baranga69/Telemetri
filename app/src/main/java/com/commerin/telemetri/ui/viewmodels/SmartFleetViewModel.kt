package com.commerin.telemetri.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commerin.telemetri.core.*
import com.commerin.telemetri.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartFleetViewModel @Inject constructor(
    private val telemetriManager: TelemetriManager
) : ViewModel() {

    private val _isCollecting = MutableLiveData<Boolean>()
    val isCollecting: LiveData<Boolean> = _isCollecting

    private val _driverState = MutableLiveData<DriverState>()
    val driverState: LiveData<DriverState> = _driverState

    private val _currentTrip = MutableLiveData<TripScore>()
    val currentTrip: LiveData<TripScore> = _currentTrip

    private val _drivingEvents = MutableLiveData<List<DrivingEvent>>()
    val drivingEvents: LiveData<List<DrivingEvent>> = _drivingEvents

    private val _powerState = MutableLiveData<AdaptivePowerManager.PowerState>()
    val powerState: LiveData<AdaptivePowerManager.PowerState> = _powerState

    private val _batteryStats = MutableLiveData<TelemetriManager.BatteryOptimizationStats>()
    val batteryStats: LiveData<TelemetriManager.BatteryOptimizationStats> = _batteryStats

    private val _riskScore = MutableLiveData<Float>()
    val riskScore: LiveData<Float> = _riskScore

    private val _insurancePremium = MutableLiveData<RiskAssessmentEngine.InsurancePremiumEstimate>()
    val insurancePremium: LiveData<RiskAssessmentEngine.InsurancePremiumEstimate> = _insurancePremium

    // Enhanced engines
    private var driverDetectionEngine: DriverDetectionEngine? = null
    private var drivingEventDetectionEngine: DrivingEventDetectionEngine? = null
    private var riskAssessmentEngine: RiskAssessmentEngine? = null

    // Event storage
    private val eventsList = mutableListOf<DrivingEvent>()

    fun initializeFleetManagement() {
        viewModelScope.launch {
            try {
                // Initialize enhanced engines
                driverDetectionEngine = DriverDetectionEngine(telemetriManager.context)
                drivingEventDetectionEngine = DrivingEventDetectionEngine(telemetriManager.context)
                riskAssessmentEngine = RiskAssessmentEngine(telemetriManager.context)

                // Setup observers for enhanced data
                setupEnhancedObservers()

                // Update initial states
                updateBatteryStats()

            } catch (e: Exception) {
                // Handle initialization errors gracefully
                android.util.Log.e("SmartFleetViewModel", "Error initializing fleet management", e)
            }
        }
    }

    private fun setupEnhancedObservers() {
        // Observe driver detection
        driverDetectionEngine?.driverState?.observeForever { driverState ->
            _driverState.postValue(driverState)
        }

        // Observe driving events
        drivingEventDetectionEngine?.drivingEvents?.observeForever { event ->
            eventsList.add(event)
            _drivingEvents.postValue(eventsList.toList())

            // Feed events to risk assessment
            riskAssessmentEngine?.addDrivingEvent(event)
        }

        // Observe trip scoring
        drivingEventDetectionEngine?.tripScore?.observeForever { tripScore ->
            _currentTrip.postValue(tripScore)

            // Feed trip data to risk assessment
            riskAssessmentEngine?.addTripScore(tripScore)
        }

        // Observe risk scoring
        riskAssessmentEngine?.riskScore?.observeForever { riskScore ->
            _riskScore.postValue(riskScore)
        }

        // Observe insurance premium estimates
        riskAssessmentEngine?.insurancePremiumEstimate?.observeForever { premium ->
            _insurancePremium.postValue(premium)
        }

        // Observe power state
        telemetriManager.getCurrentPowerState().observeForever { powerState ->
            _powerState.postValue(powerState)
        }

        // Observe telemetry data to feed into engines
        telemetriManager.motionAnalysis.observeForever { motionData ->
            drivingEventDetectionEngine?.updateMotionData(motionData)
        }

        telemetriManager.locationData.observeForever { locationData ->
            drivingEventDetectionEngine?.updateLocationData(locationData)
        }
    }

    fun startFleetMonitoring() {
        viewModelScope.launch {
            try {
                // Start comprehensive telemetry with insurance configuration
                val config = TelemetryConfig().insuranceTelematicsUseCase()
                telemetriManager.startTelemetryCollection(config)

                // Start enhanced engines
                driverDetectionEngine?.startDriverDetection()
                drivingEventDetectionEngine?.startEventDetection()
                riskAssessmentEngine?.startRiskAssessment("demo_driver_001")

                _isCollecting.postValue(true)

                // Clear previous data for new session
                eventsList.clear()
                _drivingEvents.postValue(emptyList())

            } catch (e: Exception) {
                android.util.Log.e("SmartFleetViewModel", "Error starting fleet monitoring", e)
            }
        }
    }

    fun stopFleetMonitoring() {
        viewModelScope.launch {
            try {
                // Stop telemetry collection
                telemetriManager.stopTelemetryCollection()

                // Stop enhanced engines
                driverDetectionEngine?.stopDriverDetection()
                drivingEventDetectionEngine?.stopEventDetection()
                riskAssessmentEngine?.stopRiskAssessment()

                _isCollecting.postValue(false)

            } catch (e: Exception) {
                android.util.Log.e("SmartFleetViewModel", "Error stopping fleet monitoring", e)
            }
        }
    }

    fun optimizePowerSettings() {
        viewModelScope.launch {
            try {
                // Get current power recommendations
                val recommendations = telemetriManager.getPowerOptimizationRecommendations()

                // Apply automatic power optimizations based on current state
                val powerState = telemetriManager.getCurrentPowerState().value

                when (powerState?.powerMode) {
                    AdaptivePowerManager.PowerMode.CRITICAL_BATTERY -> {
                        // Force critical battery mode for maximum savings
                        telemetriManager.forcePowerMode(AdaptivePowerManager.PowerMode.CRITICAL_BATTERY)
                    }
                    AdaptivePowerManager.PowerMode.BATTERY_SAVER -> {
                        // Optimize for battery saver mode
                        telemetriManager.forcePowerMode(AdaptivePowerManager.PowerMode.BATTERY_SAVER)
                    }
                    else -> {
                        // Return to automatic mode
                        // The adaptive power manager will handle optimization automatically
                    }
                }

                updateBatteryStats()

                android.util.Log.d("SmartFleetViewModel", "Power optimization applied: $recommendations")

            } catch (e: Exception) {
                android.util.Log.e("SmartFleetViewModel", "Error optimizing power settings", e)
            }
        }
    }

    fun exportEventReport() {
        viewModelScope.launch {
            try {
                val events = _drivingEvents.value ?: emptyList()
                val currentTrip = _currentTrip.value
                val driverState = _driverState.value

                // Create comprehensive report
                val report = generateEventReport(events, currentTrip, driverState)

                android.util.Log.d("SmartFleetViewModel", "Event report generated: $report")

                // In a real app, you would export this to a file or send to a server

            } catch (e: Exception) {
                android.util.Log.e("SmartFleetViewModel", "Error exporting event report", e)
            }
        }
    }

    fun generateInsuranceReport() {
        viewModelScope.launch {
            try {
                val premium = _insurancePremium.value
                val riskScore = _riskScore.value
                val events = _drivingEvents.value ?: emptyList()
                val driverState = _driverState.value

                // Create insurance-specific report
                val report = generateInsuranceAnalysisReport(premium, riskScore, events, driverState)

                android.util.Log.d("SmartFleetViewModel", "Insurance report generated: $report")

                // In a real app, this would be sent to insurance provider's API

            } catch (e: Exception) {
                android.util.Log.e("SmartFleetViewModel", "Error generating insurance report", e)
            }
        }
    }

    private fun updateBatteryStats() {
        viewModelScope.launch {
            try {
                val stats = telemetriManager.getBatteryOptimizationStats()
                _batteryStats.postValue(stats)
            } catch (e: Exception) {
                android.util.Log.e("SmartFleetViewModel", "Error updating battery stats", e)
            }
        }
    }

    private fun generateEventReport(
        events: List<DrivingEvent>,
        trip: TripScore?,
        driverState: DriverState?
    ): String {
        return buildString {
            appendLine("=== FLEET MANAGEMENT EVENT REPORT ===")
            appendLine("Generated: ${java.util.Date()}")
            appendLine()

            // Driver Information
            appendLine("DRIVER STATUS:")
            if (driverState != null) {
                appendLine("  Status: ${if (driverState.isDriver) "Confirmed Driver" else "Passenger/Unknown"}")
                appendLine("  Confidence: ${(driverState.confidence * 100).toInt()}%")
                appendLine("  Phone Position: ${driverState.phonePosition}")
                appendLine("  Movement Pattern: ${driverState.movementPattern}")
            } else {
                appendLine("  No driver detection data available")
            }
            appendLine()

            // Trip Summary
            appendLine("TRIP SUMMARY:")
            if (trip != null) {
                appendLine("  Overall Score: ${trip.overallScore.toInt()}/100")
                appendLine("  Safety Score: ${trip.safetyScore.toInt()}/100")
                appendLine("  Legal Compliance: ${trip.legalComplianceScore.toInt()}/100")
                appendLine("  Distance: ${trip.tripStatistics.totalDistance} km")
                appendLine("  Duration: ${trip.tripStatistics.totalDuration / 60000} minutes")
                appendLine("  Average Speed: ${trip.tripStatistics.averageSpeed} km/h")
                appendLine("  Max Speed: ${trip.tripStatistics.maxSpeed} km/h")
            } else {
                appendLine("  No trip data available")
            }
            appendLine()

            // Events Summary
            appendLine("DRIVING EVENTS:")
            if (events.isNotEmpty()) {
                val eventSummary = events.groupBy { it.eventType }.mapValues { it.value.size }
                eventSummary.forEach { (type, count) ->
                    appendLine("  ${type.name}: $count events")
                }
                appendLine()

                appendLine("RECENT EVENTS DETAIL:")
                events.takeLast(10).forEach { event ->
                    appendLine("  ${event.eventType.name} - ${event.severity.name}")
                    appendLine("    Speed: ${String.format("%.1f", event.speed * 3.6f)} km/h")
                    appendLine("    Confidence: ${(event.confidence * 100).toInt()}%")
                    appendLine("    Time: ${java.util.Date(event.timestamp)}")
                    appendLine()
                }
            } else {
                appendLine("  No events recorded")
            }

            appendLine("=== END REPORT ===")
        }
    }

    private fun generateInsuranceAnalysisReport(
        premium: RiskAssessmentEngine.InsurancePremiumEstimate?,
        riskScore: Float?,
        events: List<DrivingEvent>,
        driverState: DriverState?
    ): String {
        return buildString {
            appendLine("=== INSURANCE ANALYTICS REPORT ===")
            appendLine("Generated: ${java.util.Date()}")
            appendLine()

            // Risk Assessment
            appendLine("RISK ASSESSMENT:")
            if (riskScore != null) {
                appendLine("  Current Risk Score: ${riskScore.toInt()}/100")
                appendLine("  Risk Category: ${when {
                    riskScore < 20f -> "Very Low Risk"
                    riskScore < 40f -> "Low Risk"
                    riskScore < 60f -> "Moderate Risk"
                    riskScore < 80f -> "High Risk"
                    else -> "Very High Risk"
                }}")
            }
            appendLine()

            // Premium Analysis
            appendLine("PREMIUM ANALYSIS:")
            if (premium != null) {
                appendLine("  Base Premium: $${premium.basePremium.toInt()}/year")
                appendLine("  Risk Multiplier: ${String.format("%.2f", premium.riskMultiplier)}x")
                appendLine("  Estimated Premium: $${premium.estimatedPremium.toInt()}/year")
                appendLine("  Discount Eligible: ${if (premium.discountEligible) "Yes" else "No"}")
                if (premium.discountEligible) {
                    appendLine("  Potential Discount: ${premium.discountPercentage.toInt()}%")
                    val discountAmount = premium.estimatedPremium * (premium.discountPercentage / 100f)
                    appendLine("  Annual Savings: $${discountAmount.toInt()}")
                }
                appendLine()

                if (premium.recommendations.isNotEmpty()) {
                    appendLine("RECOMMENDATIONS:")
                    premium.recommendations.forEach { recommendation ->
                        appendLine("  • $recommendation")
                    }
                    appendLine()
                }

                if (premium.riskFactors.isNotEmpty()) {
                    appendLine("RISK FACTORS:")
                    premium.riskFactors.forEach { factor ->
                        appendLine("  • ${factor.description} (Impact: ${factor.impact.toInt()})")
                    }
                    appendLine()
                }
            }

            // Driver Verification
            appendLine("DRIVER VERIFICATION:")
            if (driverState != null) {
                appendLine("  Driver Confidence: ${(driverState.confidence * 100).toInt()}%")
                appendLine("  Verification Status: ${if (driverState.isDriver) "Verified Driver" else "Unverified"}")
                appendLine("  Data Quality: ${if (driverState.confidence > 0.8f) "High" else if (driverState.confidence > 0.6f) "Medium" else "Low"}")
            }
            appendLine()

            // Event Impact Analysis
            appendLine("EVENT IMPACT ANALYSIS:")
            val criticalEvents = events.filter { it.severity == EventSeverity.CRITICAL }
            val highSeverityEvents = events.filter { it.severity == EventSeverity.HIGH }
            val totalEvents = events.size

            appendLine("  Total Events: $totalEvents")
            appendLine("  Critical Events: ${criticalEvents.size}")
            appendLine("  High Severity Events: ${highSeverityEvents.size}")

            if (totalEvents > 0) {
                val eventRate = (criticalEvents.size + highSeverityEvents.size).toFloat() / totalEvents * 100f
                appendLine("  High-Risk Event Rate: ${eventRate.toInt()}%")
            }

            appendLine()
            appendLine("=== END INSURANCE REPORT ===")
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                // Clean up engines
                driverDetectionEngine?.stopDriverDetection()
                drivingEventDetectionEngine?.stopEventDetection()
                riskAssessmentEngine?.stopRiskAssessment()
            } catch (e: Exception) {
                android.util.Log.e("SmartFleetViewModel", "Error during cleanup", e)
            }
        }
    }
}
