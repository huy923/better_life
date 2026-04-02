package com.health.heartrate.phone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.health.heartrate.phone.healthconnect.HealthConnectSyncWorker
import com.health.heartrate.phone.service.PhoneHeartRateService
import com.health.heartrate.phone.worker.RetentionWorker
import com.health.heartrate.phone.worker.StatAggregatorWorker

class HeartRateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        RetentionWorker.schedule(this)
        StatAggregatorWorker.schedule(this)
        HealthConnectSyncWorker.schedule(this)
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(PhoneHeartRateService.CHANNEL_ID,
                "Dịch vụ đo nhịp tim", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel("heart_rate_alert",
                "Cảnh báo nhịp tim", NotificationManager.IMPORTANCE_HIGH)
        )
    }
}
