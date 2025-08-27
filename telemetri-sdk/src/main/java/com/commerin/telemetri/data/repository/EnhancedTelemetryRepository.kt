package com.commerin.telemetri.data.repository

import com.commerin.telemetri.data.local.dao.DrivingEventDao
import com.commerin.telemetri.data.local.dao.TripSummaryDao
import com.commerin.telemetri.data.local.dao.TelemetryEventDao
import com.commerin.telemetri.data.local.entities.DrivingEventEntity
import com.commerin.telemetri.data.local.entities.TripSummaryEntity
import com.commerin.telemetri.data.local.mappers.DrivingEventMapper
import com.commerin.telemetri.data.local.mappers.DeviceInfo
import com.commerin.telemetri.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced repository for managing driving events and trip data
 * Integrates with the refined DrivingEventDetectionEngine for Kenyan road conditions
 */
@Singleton
class EnhancedTelemetryRepository @Inject constructor(
    private val drivingEventDao: DrivingEventDao,
    private val tripSummaryDao: TripSummaryDao,
    private val telemetryEventDao: TelemetryEventDao // For backward compatibility
) {

    // Current active trip tracking
    private var currentTripId: String? = null
    private var currentTripStartTime: Long = 0L
    private val currentTripEvents = mutableListOf<DrivingEvent>()

    // Device information for context
    private var deviceInfo: DeviceInfo? = null

    fun setDeviceInfo(deviceId: String, appVersion: String, sdkVersion: String) {
        deviceInfo = DeviceInfo(deviceId, appVersion, sdkVersion)
    }

    // ============ TRIP MANAGEMENT ============

    /**
     * Start a new trip session
     */
    suspend fun startTrip(): String {
        val tripId = UUID.randomUUID().toString()
        currentTripId = tripId
        currentTripStartTime = System.currentTimeMillis()
        currentTripEvents.clear()
        return tripId
    }

    /**
     * End the current trip and save trip summary
     */
    suspend fun endTrip(tripScore: TripScore): String? {
        val tripId = currentTripId ?: return null
        val endTime = System.currentTimeMillis()

        // Create and save trip summary
        val tripSummary = DrivingEventMapper.toTripSummaryEntity(
            tripScore = tripScore,
            tripId = tripId,
            startTimestamp = currentTripStartTime,
            endTimestamp = endTime,
            deviceInfo = deviceInfo
        )

        tripSummaryDao.insertTripSummary(tripSummary)

        // Clear current trip data
        currentTripId = null
        currentTripEvents.clear()

        return tripId
    }

    /**
     * Get the current active trip ID
     */
    fun getCurrentTripId(): String? = currentTripId

    // ============ DRIVING EVENT STORAGE ============

    /**
     * Store a driving event from the detection engine
     * This method is called automatically by the DrivingEventDetectionEngine
     */
    suspend fun storeDrivingEvent(event: DrivingEvent): Long {
        // Convert to database entity
        val eventEntity = DrivingEventMapper.toDatabaseEntity(
            event = event,
            tripId = currentTripId,
            deviceInfo = deviceInfo
        )

        // Add enhanced phone usage details if available
        val enhancedEntity = if (event.eventType == DrivingEventType.PHONE_USAGE) {
            enhancePhoneUsageEvent(eventEntity, event)
        } else {
            eventEntity
        }

        // Store in database
        val eventId = drivingEventDao.insertEvent(enhancedEntity)

        // Add to current trip tracking
        currentTripEvents.add(event)

        return eventId
    }

    /**
     * Store multiple driving events in batch
     */
    suspend fun storeDrivingEvents(events: List<DrivingEvent>): List<Long> {
        val entities = events.map { event ->
            DrivingEventMapper.toDatabaseEntity(
                event = event,
                tripId = currentTripId,
                deviceInfo = deviceInfo
            )
        }

        val eventIds = drivingEventDao.insertEvents(entities)
        currentTripEvents.addAll(events)

        return eventIds
    }

    // ============ ENHANCED PHONE USAGE STORAGE ============

    /**
     * Store phone usage event with enhanced detection scores
     */
    suspend fun storePhoneUsageEvent(
        event: DrivingEvent,
        handMovementScore: Float?,
        drivingDisruptionScore: Float?,
        orientationChangeScore: Float?,
        audioPatternScore: Float?,
        speedCorrelationScore: Float?
    ): Long {
        val baseEntity = DrivingEventMapper.toDatabaseEntity(
            event = event,
            tripId = currentTripId,
            deviceInfo = deviceInfo
        )

        // Add enhanced phone usage details
        val enhancedEntity = baseEntity.copy(
            handMovementScore = handMovementScore,
            drivingDisruptionScore = drivingDisruptionScore,
            orientationChangeScore = orientationChangeScore,
            audioPatternScore = audioPatternScore,
            speedCorrelationScore = speedCorrelationScore,
            phoneUsageDetails = createPhoneUsageDetailsJson(
                handMovementScore,
                drivingDisruptionScore,
                orientationChangeScore,
                audioPatternScore,
                speedCorrelationScore,
                event.confidence
            )
        )

        val eventId = drivingEventDao.insertEvent(enhancedEntity)
        currentTripEvents.add(event)

        return eventId
    }

    // ============ QUERY METHODS ============

    /**
     * Get all driving events for a specific trip
     */
    suspend fun getDrivingEventsByTrip(tripId: String): List<DrivingEvent> {
        return drivingEventDao.getEventsByTripId(tripId)
            .map { DrivingEventMapper.toDomainModel(it) }
    }

    /**
     * Get all driving events for a specific trip (returning entities)
     */
    suspend fun getEventsForTrip(tripId: String): List<DrivingEventEntity> {
        return drivingEventDao.getEventsByTripId(tripId)
    }

    /**
     * Get trip summaries in a date range
     */
    suspend fun getTripSummariesInRange(startTime: Long, endTime: Long): List<TripSummaryEntity> {
        return tripSummaryDao.getTripSummariesInRange(startTime, endTime)
    }

    /**
     * Get driving events in a date range
     */
    suspend fun getDrivingEventsInRange(startTime: Long, endTime: Long): List<DrivingEventEntity> {
        return drivingEventDao.getEventsInRange(startTime, endTime)
    }

    /**
     * Get driving events by type with Flow for reactive UI updates
     */
    fun getDrivingEventsByType(eventType: DrivingEventType): Flow<List<DrivingEvent>> {
        return drivingEventDao.getEventsByType(eventType.name)
            .map { entities -> entities.map { DrivingEventMapper.toDomainModel(it) } }
    }

    /**
     * Get phone usage events with high confidence
     */
    fun getHighConfidencePhoneUsageEvents(minConfidence: Float = 0.9f): Flow<List<DrivingEvent>> {
        return drivingEventDao.getPhoneUsageEvents(minConfidence)
            .map { entities -> entities.map { DrivingEventMapper.toDomainModel(it) } }
    }

    /**
     * Get recent trip summaries
     */
    fun getRecentTripSummaries(limit: Int = 20): Flow<List<TripSummaryEntity>> {
        return tripSummaryDao.getRecentTripSummaries(limit)
    }

    /**
     * Get trip summary by ID
     */
    suspend fun getTripSummary(tripId: String): TripSummaryEntity? {
        return tripSummaryDao.getTripSummaryById(tripId)
    }

    // ============ ANALYTICS METHODS ============

    /**
     * Get driving statistics for a time range
     */
    suspend fun getDrivingStatistics(startTime: Long, endTime: Long): DrivingStatistics {
        return DrivingStatistics(
            totalTrips = tripSummaryDao.getTripCountInRange(startTime, endTime),
            totalDistance = tripSummaryDao.getTotalDistanceInRange(startTime, endTime) ?: 0f,
            totalDuration = tripSummaryDao.getTotalDurationInRange(startTime, endTime) ?: 0L,
            averageScore = tripSummaryDao.getAverageOverallScore(startTime, endTime) ?: 0f,
            speedingEventCount = tripSummaryDao.getTotalSpeedingEvents(startTime, endTime) ?: 0,
            phoneUsageEventCount = tripSummaryDao.getTotalPhoneUsageEvents(startTime, endTime) ?: 0,
            criticalEventCount = tripSummaryDao.getTotalCriticalEvents(startTime, endTime) ?: 0,
            maxSpeedViolation = tripSummaryDao.getMaxSpeedViolation(startTime, endTime) ?: 0f,
            totalSpeedingDuration = tripSummaryDao.getTotalSpeedingDuration(startTime, endTime) ?: 0L,
            averageNightDriving = tripSummaryDao.getAverageNightDriving(startTime, endTime) ?: 0f
        )
    }

    /**
     * Get phone usage analytics for a time range
     */
    suspend fun getPhoneUsageAnalytics(startTime: Long, endTime: Long): PhoneUsageAnalytics {
        return PhoneUsageAnalytics(
            totalEvents = tripSummaryDao.getTotalPhoneUsageEvents(startTime, endTime) ?: 0,
            totalDuration = tripSummaryDao.getTotalPhoneUsageDuration(startTime, endTime) ?: 0L,
            averageConfidence = tripSummaryDao.getAveragePhoneUsageConfidence(startTime, endTime) ?: 0f,
            averageHandMovementScore = drivingEventDao.getAverageHandMovementScore(startTime, endTime) ?: 0f,
            averageDrivingDisruptionScore = drivingEventDao.getAverageDrivingDisruptionScore(startTime, endTime) ?: 0f,
            highConfidenceEvents = drivingEventDao.getEventCountByTypeInRange("PHONE_USAGE", startTime, endTime)
        )
    }

    // ============ SYNC METHODS ============

    /**
     * Get all unsynced data for upload
     */
    suspend fun getUnsyncedData(): UnsyncedData {
        return UnsyncedData(
            drivingEvents = drivingEventDao.getUnsyncedEvents(),
            tripSummaries = tripSummaryDao.getUnsyncedTripSummaries(),
            legacyEvents = telemetryEventDao.getUnsyncedEvents()
        )
    }

    /**
     * Mark events as synced after successful upload
     */
    suspend fun markEventsSynced(eventIds: List<String>) {
        drivingEventDao.markEventsSynced(eventIds)
    }

    /**
     * Mark trip summaries as synced after successful upload
     */
    suspend fun markTripsSynced(tripIds: List<String>) {
        tripSummaryDao.markTripsSynced(tripIds)
    }

    // ============ CLEANUP METHODS ============

    /**
     * Clean up old synced data
     */
    suspend fun cleanupOldData(cutoffTime: Long) {
        drivingEventDao.deleteSyncedEventsBefore(cutoffTime)
        tripSummaryDao.deleteSyncedTripSummariesBefore(cutoffTime)
        telemetryEventDao.deleteOldTelemetryEvents(cutoffTime)
    }

    // ============ HELPER METHODS ============

    private fun enhancePhoneUsageEvent(
        entity: DrivingEventEntity,
        event: DrivingEvent
    ): DrivingEventEntity {
        // This would be enhanced with actual phone usage detection scores
        // when the detection engine provides them
        return entity.copy(
            phoneUsageDetails = createPhoneUsageDetailsJson(
                handMovementScore = null,
                drivingDisruptionScore = null,
                orientationChangeScore = null,
                audioPatternScore = null,
                speedCorrelationScore = null,
                overallConfidence = event.confidence
            )
        )
    }

    private fun createPhoneUsageDetailsJson(
        handMovementScore: Float?,
        drivingDisruptionScore: Float?,
        orientationChangeScore: Float?,
        audioPatternScore: Float?,
        speedCorrelationScore: Float?,
        overallConfidence: Float
    ): String {
        val details = mapOf(
            "handMovementScore" to handMovementScore,
            "drivingDisruptionScore" to drivingDisruptionScore,
            "orientationChangeScore" to orientationChangeScore,
            "audioPatternScore" to audioPatternScore,
            "speedCorrelationScore" to speedCorrelationScore,
            "overallConfidence" to overallConfidence,
            "detectionMethod" to "multi_sensor_fusion",
            "timestamp" to System.currentTimeMillis()
        )

        return com.google.gson.Gson().toJson(details)
    }
}

// Data classes for analytics results
data class DrivingStatistics(
    val totalTrips: Int,
    val totalDistance: Float,
    val totalDuration: Long,
    val averageScore: Float,
    val speedingEventCount: Int,
    val phoneUsageEventCount: Int,
    val criticalEventCount: Int,
    val maxSpeedViolation: Float,
    val totalSpeedingDuration: Long,
    val averageNightDriving: Float
)

data class PhoneUsageAnalytics(
    val totalEvents: Int,
    val totalDuration: Long,
    val averageConfidence: Float,
    val averageHandMovementScore: Float,
    val averageDrivingDisruptionScore: Float,
    val highConfidenceEvents: Int
)

data class UnsyncedData(
    val drivingEvents: List<DrivingEventEntity>,
    val tripSummaries: List<TripSummaryEntity>,
    val legacyEvents: List<com.commerin.telemetri.data.local.entities.TelemetryEventEntity>
)
