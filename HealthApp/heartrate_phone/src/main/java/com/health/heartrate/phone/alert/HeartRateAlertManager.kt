package com.health.heartrate.phone.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.health.heartrate.shared.constants.DataLayerConstants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class HeartRateAlert(
    val type: AlertType,
    val bpm: Int,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AlertType {
    HIGH_BPM, LOW_BPM, RESTING_HIGH, IRREGULAR_RHYTHM
}

class HeartRateAlertManager(private val context: Context) {

    private val notifManager =
        context.getSystemService(NotificationManager::class.java)

    private val _alertFlow = MutableSharedFlow<HeartRateAlert>(replay = 0)
    val alertFlow: SharedFlow<HeartRateAlert> = _alertFlow.asSharedFlow()

    private var lastAlertTime = mutableMapOf<AlertType, Long>()
    private val COOLDOWN_MS = 60_000L  // max 1 alert/type per minute

    init { createNotificationChannel() }

    suspend fun evaluate(bpm: Int, source: String) {
        when {
            bpm > DataLayerConstants.ALERT_HIGH_BPM ->
                triggerAlert(HeartRateAlert(AlertType.HIGH_BPM, bpm, source))
            bpm < DataLayerConstants.ALERT_LOW_BPM ->
                triggerAlert(HeartRateAlert(AlertType.LOW_BPM, bpm, source))
            bpm > DataLayerConstants.ALERT_RESTING_HIGH ->
                triggerAlert(HeartRateAlert(AlertType.RESTING_HIGH, bpm, source))
        }
    }

    private suspend fun triggerAlert(alert: HeartRateAlert) {
        val now = System.currentTimeMillis()
        val last = lastAlertTime[alert.type] ?: 0L
        if (now - last < COOLDOWN_MS) return

        lastAlertTime[alert.type] = now
        _alertFlow.emit(alert)
        showNotification(alert)
    }

    private fun showNotification(alert: HeartRateAlert) {
        val (title, body) = when (alert.type) {
            AlertType.HIGH_BPM ->
                "⚠️ Nhịp tim cao" to "Nhịp tim ${alert.bpm} BPM vượt ngưỡng an toàn (>${DataLayerConstants.ALERT_HIGH_BPM})"
            AlertType.LOW_BPM ->
                "⚠️ Nhịp tim thấp" to "Nhịp tim ${alert.bpm} BPM dưới ngưỡng an toàn (<${DataLayerConstants.ALERT_LOW_BPM})"
            AlertType.RESTING_HIGH ->
                "ℹ️ Nhịp tim nghỉ cao" to "Nhịp tim nghỉ ${alert.bpm} BPM cao hơn bình thường"
            AlertType.IRREGULAR_RHYTHM ->
                "⚠️ Nhịp tim bất thường" to "Phát hiện nhịp tim không đều"
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notifManager.notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun createNotificationChannel() {
        notifManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Cảnh báo nhịp tim",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Thông báo khi nhịp tim bất thường"
            }
        )
    }

    companion object { const val CHANNEL_ID = "heart_rate_alert" }
}
