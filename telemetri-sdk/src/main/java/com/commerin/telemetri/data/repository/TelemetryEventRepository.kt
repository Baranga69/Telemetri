package com.commerin.telemetri.data.repository

import android.util.Log
import com.commerin.telemetri.data.local.dao.TelemetryEventDao
import com.commerin.telemetri.data.mappers.TelemetryEventMapper
import com.commerin.telemetri.domain.model.TelemetryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing telemetry events in the local database.
 *
 * Provides methods for adding, querying, updating, and deleting events.
 * Handles mapping between domain model and Room entity.
 * Includes robust error handling and logging for all operations.
 */
class TelemetryEventRepository(private val dao: TelemetryEventDao) {

    companion object {
        private const val TAG = "TelemetryEventRepo"
    }

    suspend fun addEvent(event: TelemetryEvent) = try {
        dao.insertEvent(TelemetryEventMapper.toEntity(event))
        Log.d(TAG, "Event added: ${event.eventId}")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to add event", e)
    }

    suspend fun addEvents(events: List<TelemetryEvent>) = try {
        dao.insertEvents(events.map { TelemetryEventMapper.toEntity(it) })
        Log.d(TAG, "Events added: ${events.size}")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to add events", e)
    }

    suspend fun getAllEvents(): List<TelemetryEvent> = try {
        dao.getAllEvents().map { TelemetryEventMapper.toModel(it) }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get all events", e)
        emptyList()
    }

    suspend fun getUnsyncedEvents(): List<TelemetryEvent> = try {
        dao.getUnsyncedEvents().map { TelemetryEventMapper.toModel(it) }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get unsynced events", e)
        emptyList()
    }

    suspend fun markEventSynced(eventId: String) = try {
        val event = dao.getAllEvents().find { it.eventId == eventId }
        if (event != null) {
            dao.updateEvent(event.copy(synced = true))
            Log.d(TAG, "Event marked as synced: $eventId")
        } else {
            Log.w(TAG, "Event not found: $eventId")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to mark event as synced", e)
    }

    suspend fun deleteEvent(eventId: String) = try {
        dao.deleteEventById(eventId)
        Log.d(TAG, "Event deleted: $eventId")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to delete event", e)
    }

    suspend fun deleteAllEvents() = try {
        dao.deleteAllEvents()
        Log.d(TAG, "All events deleted")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to delete all events", e)
    }
}
