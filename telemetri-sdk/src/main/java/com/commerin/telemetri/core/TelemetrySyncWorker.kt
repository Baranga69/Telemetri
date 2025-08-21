package com.commerin.telemetri.core

import android.content.Context
import androidx.work.WorkerParameters
import android.util.Log
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.commerin.telemetri.data.repository.TelemetryEventRepository
import androidx.work.CoroutineWorker
/**
 * Worker for syncing unsynced telemetry events to the backend API.
 *
 * This worker is scheduled by [BackgroundSyncService] and uses Hilt for dependency injection.
 * It fetches unsynced events from the local database, uploads them to the API, and marks them as synced.
 *
 * @param context Application context
 * @param workerParams Worker parameters
 * @param repository Repository for accessing telemetry events
 * @param api API for uploading events and checking network status
 */
@HiltWorker
class TelemetrySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TelemetryEventRepository,
    private val api: TelemetryApi
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val unsyncedEvents = repository.getUnsyncedEvents()
            if (unsyncedEvents.isNotEmpty() && api.isNetworkAvailable()) {
                val successIds = api.uploadEvents(unsyncedEvents)
                successIds.forEach { id -> repository.markEventSynced(id) }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("TelemetrySyncWorker", "Sync failed", e)
            Result.retry()
        }
    }
}
