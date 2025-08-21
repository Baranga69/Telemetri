package com.commerin.telemetri.data.local.dao

import androidx.room.*
import com.commerin.telemetri.data.local.entities.TelemetryEventEntity

@Dao
interface TelemetryEventDao {
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
}

