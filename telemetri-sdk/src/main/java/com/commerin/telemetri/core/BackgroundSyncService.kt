package com.commerin.telemetri.core

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules periodic background sync of telemetry events using WorkManager.
 *
 * Call [scheduleSync] from your Application or main entry point to enable automatic syncing.
 */
object BackgroundSyncService {
    private const val WORK_NAME = "TelemetrySyncWorker"

    fun scheduleSync(context: Context) {
        val syncRequest = PeriodicWorkRequestBuilder<TelemetrySyncWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
