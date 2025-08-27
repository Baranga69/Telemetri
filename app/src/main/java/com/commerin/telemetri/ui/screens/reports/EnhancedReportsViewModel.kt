package com.commerin.telemetri.ui.screens.reports

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commerin.telemetri.data.local.entities.DrivingEventEntity
import com.commerin.telemetri.data.local.entities.TripSummaryEntity
import com.commerin.telemetri.data.repository.DrivingStatistics
import com.commerin.telemetri.data.repository.EnhancedTelemetryRepository
import com.commerin.telemetri.data.repository.PhoneUsageAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for the Enhanced Reports Screen
 * Provides data for trip analytics, phone usage analysis, speeding analysis, and event details
 */
@HiltViewModel
class EnhancedReportsViewModel @Inject constructor(
    private val repository: EnhancedTelemetryRepository
) : ViewModel() {

    // LiveData for UI state
    private val _tripSummaries = MutableLiveData<List<TripSummaryEntity>>()
    val tripSummaries: LiveData<List<TripSummaryEntity>> = _tripSummaries

    private val _drivingEvents = MutableLiveData<List<DrivingEventEntity>>()
    val drivingEvents: LiveData<List<DrivingEventEntity>> = _drivingEvents

    private val _phoneUsageEvents = MutableLiveData<List<DrivingEventEntity>>()
    val phoneUsageEvents: LiveData<List<DrivingEventEntity>> = _phoneUsageEvents

    private val _speedingEvents = MutableLiveData<List<DrivingEventEntity>>()
    val speedingEvents: LiveData<List<DrivingEventEntity>> = _speedingEvents

    private val _drivingStatistics = MutableLiveData<DrivingStatistics>()
    val drivingStatistics: LiveData<DrivingStatistics> = _drivingStatistics

    private val _phoneUsageAnalytics = MutableLiveData<PhoneUsageAnalytics>()
    val phoneUsageAnalytics: LiveData<PhoneUsageAnalytics> = _phoneUsageAnalytics

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Filter states
    private var currentEventTypeFilter: String = "ALL"
    private var currentSeverityFilter: String = "ALL"
    private var currentRoadTypeFilter: String = "ALL"

    // Cache for original unfiltered data
    private var allDrivingEvents: List<DrivingEventEntity> = emptyList()
    private var allSpeedingEvents: List<DrivingEventEntity> = emptyList()

    init {
        loadEnhancedReports()
    }

    /**
     * Load all enhanced reports data
     */
    fun loadEnhancedReports() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val now = System.currentTimeMillis()
                val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000) // Last 30 days

                // Load trip summaries
                val trips = repository.getTripSummariesInRange(thirtyDaysAgo, now)
                _tripSummaries.value = trips

                // Load all driving events
                val events = repository.getDrivingEventsInRange(thirtyDaysAgo, now)
                allDrivingEvents = events
                _drivingEvents.value = events

                // Filter phone usage events
                val phoneEvents = events.filter { it.eventType == "PHONE_USAGE" }
                _phoneUsageEvents.value = phoneEvents

                // Filter speeding events
                val speedingEventsList = events.filter { it.eventType == "SPEEDING" }
                allSpeedingEvents = speedingEventsList
                _speedingEvents.value = speedingEventsList

                // Load analytics
                val drivingStats = repository.getDrivingStatistics(thirtyDaysAgo, now)
                _drivingStatistics.value = drivingStats

                val phoneStats = repository.getPhoneUsageAnalytics(thirtyDaysAgo, now)
                _phoneUsageAnalytics.value = phoneStats

            } catch (e: Exception) {
                _error.value = "Failed to load reports: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Refresh all data
     */
    fun refreshData() {
        loadEnhancedReports()
    }

    /**
     * Navigate to trip details
     */
    fun viewTripDetails(tripId: String) {
        viewModelScope.launch {
            try {
                // Here you would typically navigate to a trip details screen
                // For now, we'll just log or handle the navigation intent
                // You can implement navigation logic based on your app's navigation setup

                // Example: If using Navigation Component
                // _navigationEvent.value = NavigationEvent.ToTripDetails(tripId)

                // For now, just a placeholder
                _error.value = "Trip details view for $tripId (implement navigation)"
            } catch (e: Exception) {
                _error.value = "Failed to load trip details: ${e.message}"
            }
        }
    }

    /**
     * Navigate to event details
     */
    fun viewEventDetails(eventId: String) {
        viewModelScope.launch {
            try {
                // Here you would typically navigate to an event details screen
                // For now, we'll just log or handle the navigation intent

                // Example: If using Navigation Component
                // _navigationEvent.value = NavigationEvent.ToEventDetails(eventId)

                // For now, just a placeholder
                _error.value = "Event details view for $eventId (implement navigation)"
            } catch (e: Exception) {
                _error.value = "Failed to load event details: ${e.message}"
            }
        }
    }

    /**
     * Filter speeding events by road type
     */
    fun filterSpeedingByRoadType(roadType: String) {
        currentRoadTypeFilter = roadType

        val filteredEvents = when (roadType) {
            "ALL" -> allSpeedingEvents
            "URBAN", "RURAL", "HIGHWAY" -> allSpeedingEvents.filter {
                it.roadType?.uppercase() == roadType
            }
            else -> allSpeedingEvents
        }

        _speedingEvents.value = filteredEvents
    }

    /**
     * Filter events by type
     */
    fun filterEventsByType(eventType: String) {
        currentEventTypeFilter = eventType
        applyEventFilters()
    }

    /**
     * Filter events by severity
     */
    fun filterEventsBySeverity(severity: String) {
        currentSeverityFilter = severity
        applyEventFilters()
    }

    /**
     * Apply combined filters to driving events
     */
    private fun applyEventFilters() {
        var filteredEvents = allDrivingEvents

        // Apply event type filter
        if (currentEventTypeFilter != "ALL") {
            filteredEvents = filteredEvents.filter {
                it.eventType == currentEventTypeFilter
            }
        }

        // Apply severity filter
        if (currentSeverityFilter != "ALL") {
            filteredEvents = filteredEvents.filter {
                it.severity == currentSeverityFilter
            }
        }

        _drivingEvents.value = filteredEvents
    }

    /**
     * Reset all filters
     */
    fun resetFilters() {
        currentEventTypeFilter = "ALL"
        currentSeverityFilter = "ALL"
        currentRoadTypeFilter = "ALL"

        _drivingEvents.value = allDrivingEvents
        _speedingEvents.value = allSpeedingEvents
    }

    /**
     * Load events for a specific trip
     */
    fun loadTripEvents(tripId: String) {
        viewModelScope.launch {
            try {
                val events = repository.getEventsForTrip(tripId)
                _drivingEvents.value = events
            } catch (e: Exception) {
                _error.value = "Failed to load trip events: ${e.message}"
            }
        }
    }

    /**
     * Load events for a specific date range
     */
    fun loadEventsForDateRange(startTime: Long, endTime: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val events = repository.getDrivingEventsInRange(startTime, endTime)
                allDrivingEvents = events
                _drivingEvents.value = events

                // Update filtered lists
                val phoneEvents = events.filter { it.eventType == "PHONE_USAGE" }
                _phoneUsageEvents.value = phoneEvents

                val speedingEventsList = events.filter { it.eventType == "SPEEDING" }
                allSpeedingEvents = speedingEventsList
                _speedingEvents.value = speedingEventsList

                // Update analytics for the new range
                val drivingStats = repository.getDrivingStatistics(startTime, endTime)
                _drivingStatistics.value = drivingStats

                val phoneStats = repository.getPhoneUsageAnalytics(startTime, endTime)
                _phoneUsageAnalytics.value = phoneStats

            } catch (e: Exception) {
                _error.value = "Failed to load events for date range: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get summary statistics for display
     */
    fun getSummaryStatistics(): Map<String, Any> {
        val stats = _drivingStatistics.value
        val phoneStats = _phoneUsageAnalytics.value

        return mapOf(
            "totalTrips" to (stats?.totalTrips ?: 0),
            "totalDistance" to (stats?.totalDistance ?: 0f),
            "averageScore" to (stats?.averageScore ?: 0f),
            "totalEvents" to allDrivingEvents.size,
            "phoneUsageEvents" to (phoneStats?.totalEvents ?: 0),
            "speedingEvents" to allSpeedingEvents.size,
            "criticalEvents" to (stats?.criticalEventCount ?: 0)
        )
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
}
