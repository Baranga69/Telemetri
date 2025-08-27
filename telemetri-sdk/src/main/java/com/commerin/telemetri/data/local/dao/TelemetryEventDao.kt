package com.commerin.telemetri.data.local.dao

import androidx.room.*
import com.commerin.telemetri.data.local.entities.TelemetryEventEntity
import com.commerin.telemetri.data.local.entities.DrivingEventEntity
import com.commerin.telemetri.data.local.entities.TripSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryEventDao {
    // ============ LEGACY TELEMETRY EVENT OPERATIONS ============
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TelemetryEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<TelemetryEventEntity>)

    @Query("SELECT * FROM telemetry_events")
    suspend fun getAllEvents(): List<TelemetryEventEntity>

    @Query("SELECT * FROM telemetry_events WHERE synced = 0")
    suspend fun getUnsyncedEvents(): List<TelemetryEventEntity>

    @Query("DELETE FROM telemetry_events WHERE eventId = :eventId")
    suspend fun deleteEventById(eventId: String)

    @Query("DELETE FROM telemetry_events")
    suspend fun deleteAllEvents()

    @Update
    suspend fun updateEvent(event: TelemetryEventEntity)

    // ============ ENHANCED DRIVING EVENT OPERATIONS ============
    // These methods delegate to the enhanced DrivingEventDao

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrivingEvent(event: DrivingEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrivingEvents(events: List<DrivingEventEntity>): List<Long>

    @Query("SELECT * FROM driving_events WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedDrivingEvents(): List<DrivingEventEntity>

    @Query("SELECT * FROM driving_events WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getDrivingEventsByTripId(tripId: String): List<DrivingEventEntity>

    // ============ TRIP SUMMARY OPERATIONS ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripSummary(trip: TripSummaryEntity): Long

    @Query("SELECT * FROM trip_summaries WHERE synced = 0 ORDER BY startTimestamp ASC")
    suspend fun getUnsyncedTripSummaries(): List<TripSummaryEntity>

    // ============ CLEANUP AND MAINTENANCE ============

    @Query("DELETE FROM telemetry_events WHERE timestamp < :cutoffTime")
    suspend fun deleteOldTelemetryEvents(cutoffTime: Long)

    @Query("DELETE FROM driving_events WHERE timestamp < :cutoffTime AND synced = 1")
    suspend fun deleteOldSyncedDrivingEvents(cutoffTime: Long)

    @Query("DELETE FROM trip_summaries WHERE startTimestamp < :cutoffTime AND synced = 1")
    suspend fun deleteOldSyncedTripSummaries(cutoffTime: Long)

    // ============ STATISTICS ============

    @Query("SELECT COUNT(*) FROM driving_events WHERE synced = 0")
    suspend fun getUnsyncedDrivingEventCount(): Int

    @Query("SELECT COUNT(*) FROM trip_summaries WHERE synced = 0")
    suspend fun getUnsyncedTripSummaryCount(): Int

    @Query("SELECT COUNT(*) FROM telemetry_events WHERE synced = 0")
    suspend fun getUnsyncedTelemetryEventCount(): Int
}
