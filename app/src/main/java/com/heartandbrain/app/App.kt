package com.heartandbrain.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.WorkManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Vlog processing",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        // Clear stale failed/cancelled work from previous sessions so the UI starts clean
        WorkManager.getInstance(this).pruneWork()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "processing"
    }
}
