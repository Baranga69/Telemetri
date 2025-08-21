package com.commerin.telemetri.core

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log

class TestWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        Log.d("TestWorker", "WorkManager is working!")
        return Result.success()
    }
}

