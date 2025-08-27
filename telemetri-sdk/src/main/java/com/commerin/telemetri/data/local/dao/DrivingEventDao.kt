package com.commerin.telemetri.data.local.dao

import androidx.room.*
import com.commerin.telemetri.data.local.entities.DrivingEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Enhanced DAO for driving events with comprehensive query capabilities
 * Supports the refined driving event detection system with Kenyan road adaptations
 */
@Dao
interface DrivingEventDao {

    // ============ INSERTION OPERATIONS ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DrivingEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<DrivingEventEntity>): List<Long>

    @Update
    suspend fun updateEvent(event: DrivingEventEntity)

    @Update
    suspend fun updateEvents(events: List<DrivingEventEntity>)

    // ============ BASIC RETRIEVAL OPERATIONS ============

    @Query("SELECT * FROM driving_events WHERE eventId = :eventId")
    suspend fun getEventById(eventId: String): DrivingEventEntity?

    @Query("SELECT * FROM driving_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<DrivingEventEntity>>

    @Query("SELECT * FROM driving_events WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getEventsByTripId(tripId: String): List<DrivingEventEntity>

    @Query("SELECT * FROM driving_events WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getEventsByTripIdFlow(tripId: String): Flow<List<DrivingEventEntity>>

    // ============ EVENT TYPE QUERIES ============

    @Query("SELECT * FROM driving_events WHERE eventType = :eventType ORDER BY timestamp DESC")
    fun getEventsByType(eventType: String): Flow<List<DrivingEventEntity>>

    @Query("SELECT * FROM driving_events WHERE eventType IN (:eventTypes) ORDER BY timestamp DESC")
    fun getEventsByTypes(eventTypes: List<String>): Flow<List<DrivingEventEntity>>

    @Query("SELECT * FROM driving_events WHERE eventType = 'PHONE_USAGE' AND confidence >= :minConfidence ORDER BY timestamp DESC")
    fun getPhoneUsageEvents(minConfidence: Float = 0.8f): Flow<List<DrivingEventEntity>>

    @Query("SELECT * FROM driving_events WHERE eventType = 'SPEEDING' AND speedOverLimit >= :minSpeedOver ORDER BY timestamp DESC")
    fun getSpeedingEvents(minSpeedOver: Float = 10f): Flow<List<DrivingEventEntity>>

    // ============ SEVERITY AND CONFIDENCE QUERIES ============

    @Query("SELECT * FROM driving_events WHERE severity = :severity ORDER BY timestamp DESC")
    fun getEventsBySeverity(severity: String): Flow<List<DrivingEventEntity>>

    @Query("SELECT * FROM driving_events WHERE severity IN ('HIGH', 'CRITICAL') ORDER BY timestamp DESC")
    fun getHighSeverityEvents(): Flow<List<DrivingEventEntity>>

    @Query("SELECT * FROM driving_events WHERE confidence >= :minConfidence ORDER BY confidence DESC, timestamp DESC")
    fun getHighConfidenceEvents(minConfidence: Float): Flow<List<DrivingEventEntity>>

    // ============ TIME-BASED QUERIES ============

    @Query("SELECT * FROM driving_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<DrivingEventEntity>>

    @Query("SELECT * FROM driving_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getEventsInRange(startTime: Long, endTime: Long): List<DrivingEventEntity>

    @Query("SELECT * FROM driving_events WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getEventsAfterTime(startTime: Long): Flow<List<DrivingEventEntity>>

    @Query("SELECT * FROM driving_events WHERE DATE(timestamp/1000, 'unixepoch') = DATE(:dateTimestamp/1000, 'unixepoch') ORDER BY timestamp DESC")
    fun getEventsByDate(dateTimestamp: Long): Flow<List<DrivingEventEntity>>

    @Query("SELECT * FROM driving_events WHERE timeOfDay = :timeOfDay ORDER BY timestamp DESC")
    fun getEventsByTimeOfDay(timeOfDay: String): Flow<List<DrivingEventEntity>>

    // ============ LOCATION AND ROAD CONTEXT QUERIES ============

    @Query("SELECT * FROM driving_events WHERE roadType = :roadType ORDER BY timestamp DESC")
    fun getEventsByRoadType(roadType: String): Flow<List<DrivingEventEntity>>

    @Query("SELECT * FROM driving_events WHERE isRushHour = 1 ORDER BY timestamp DESC")
    fun getRushHourEvents(): Flow<List<DrivingEventEntity>>

    @Query("SELECT * FROM driving_events WHERE weatherConditions = :weather ORDER BY timestamp DESC")
    fun getEventsByWeather(weather: String): Flow<List<DrivingEventEntity>>

    @Query("SELECT * FROM driving_events WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng ORDER BY timestamp DESC")
    fun getEventsByLocationBounds(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Flow<List<DrivingEventEntity>>

    // ============ ADVANCED ANALYTICS QUERIES ============

    @Query("SELECT COUNT(*) FROM driving_events WHERE eventType = :eventType AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getEventCountByTypeInRange(eventType: String, startTime: Long, endTime: Long): Int

    @Query("SELECT eventType, COUNT(*) as count FROM driving_events WHERE timestamp BETWEEN :startTime AND :endTime GROUP BY eventType")
    suspend fun getEventCountsByType(startTime: Long, endTime: Long): List<EventTypeCount>

    @Query("SELECT severity, COUNT(*) as count FROM driving_events WHERE timestamp BETWEEN :startTime AND :endTime GROUP BY severity")
    suspend fun getEventCountsBySeverity(startTime: Long, endTime: Long): List<SeverityCount>

    @Query("SELECT AVG(confidence) FROM driving_events WHERE eventType = :eventType AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageConfidenceByType(eventType: String, startTime: Long, endTime: Long): Float?

    @Query("SELECT roadType, COUNT(*) as count FROM driving_events WHERE roadType IS NOT NULL AND timestamp BETWEEN :startTime AND :endTime GROUP BY roadType")
    suspend fun getEventCountsByRoadType(startTime: Long, endTime: Long): List<RoadTypeCount>

    // ============ PHONE USAGE SPECIFIC QUERIES ============

    @Query("SELECT AVG(handMovementScore) FROM driving_events WHERE eventType = 'PHONE_USAGE' AND handMovementScore IS NOT NULL AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageHandMovementScore(startTime: Long, endTime: Long): Float?

    @Query("SELECT AVG(drivingDisruptionScore) FROM driving_events WHERE eventType = 'PHONE_USAGE' AND drivingDisruptionScore IS NOT NULL AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageDrivingDisruptionScore(startTime: Long, endTime: Long): Float?

    @Query("SELECT * FROM driving_events WHERE eventType = 'PHONE_USAGE' AND duration > :minDuration ORDER BY duration DESC")
    fun getLongPhoneUsageEvents(minDuration: Long = 5000L): Flow<List<DrivingEventEntity>>

    // ============ SPEEDING SPECIFIC QUERIES ============

    @Query("SELECT MAX(speedOverLimit) FROM driving_events WHERE eventType = 'SPEEDING' AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getMaxSpeedingViolation(startTime: Long, endTime: Long): Float?

    @Query("SELECT speedingThresholdType, COUNT(*) as count FROM driving_events WHERE eventType = 'SPEEDING' AND speedingThresholdType IS NOT NULL AND timestamp BETWEEN :startTime AND :endTime GROUP BY speedingThresholdType")
    suspend fun getSpeedingCountsByRoadType(startTime: Long, endTime: Long): List<SpeedingRoadTypeCount>

    @Query("SELECT SUM(speedingDuration) FROM driving_events WHERE eventType = 'SPEEDING' AND speedingDuration IS NOT NULL AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalSpeedingDuration(startTime: Long, endTime: Long): Long?

    // ============ SYNC AND PROCESSING QUERIES ============

    @Query("SELECT * FROM driving_events WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedEvents(): List<DrivingEventEntity>

    @Query("SELECT * FROM driving_events WHERE processed = 0 ORDER BY timestamp ASC")
    suspend fun getUnprocessedEvents(): List<DrivingEventEntity>

    @Query("UPDATE driving_events SET synced = 1, lastUploadAttempt = :timestamp, updatedAt = :timestamp WHERE eventId IN (:eventIds)")
    suspend fun markEventsSynced(eventIds: List<String>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE driving_events SET processed = 1, updatedAt = :timestamp WHERE eventId IN (:eventIds)")
    suspend fun markEventsProcessed(eventIds: List<String>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE driving_events SET uploadAttempts = uploadAttempts + 1, lastUploadAttempt = :timestamp, updatedAt = :timestamp WHERE eventId = :eventId")
    suspend fun incrementUploadAttempts(eventId: String, timestamp: Long = System.currentTimeMillis())

    // ============ CLEANUP OPERATIONS ============

    @Query("DELETE FROM driving_events WHERE eventId = :eventId")
    suspend fun deleteEventById(eventId: String)

    @Query("DELETE FROM driving_events WHERE tripId = :tripId")
    suspend fun deleteEventsByTripId(tripId: String)

    @Query("DELETE FROM driving_events WHERE timestamp < :cutoffTime")
    suspend fun deleteEventsBefore(cutoffTime: Long)

    @Query("DELETE FROM driving_events WHERE synced = 1 AND timestamp < :cutoffTime")
    suspend fun deleteSyncedEventsBefore(cutoffTime: Long)

    @Query("DELETE FROM driving_events")
    suspend fun deleteAllEvents()

    // ============ COUNT AND STATISTICS ============

    @Query("SELECT COUNT(*) FROM driving_events")
    suspend fun getTotalEventCount(): Int

    @Query("SELECT COUNT(*) FROM driving_events WHERE synced = 0")
    suspend fun getUnsyncedEventCount(): Int

    @Query("SELECT COUNT(*) FROM driving_events WHERE tripId = :tripId")
    suspend fun getEventCountForTrip(tripId: String): Int

    @Query("SELECT COUNT(*) FROM driving_events WHERE severity IN ('HIGH', 'CRITICAL') AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getHighSeverityEventCount(startTime: Long, endTime: Long): Int
}

// Data classes for aggregated query results
data class EventTypeCount(val eventType: String, val count: Int)
data class SeverityCount(val severity: String, val count: Int)
data class RoadTypeCount(val roadType: String, val count: Int)
data class SpeedingRoadTypeCount(val speedingThresholdType: String, val count: Int)
