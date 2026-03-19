package com.heartandbrain.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // TODO: pick random clip from Room, fire daily notification
        return Result.success()
    }
}
