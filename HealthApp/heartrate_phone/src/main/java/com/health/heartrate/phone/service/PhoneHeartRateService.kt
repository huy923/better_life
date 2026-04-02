package com.health.heartrate.phone.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.health.heartrate.phone.alert.HeartRateAlertManager
import com.health.heartrate.phone.chart.RealTimeChartDataManager
import com.health.heartrate.phone.data.db.HeartRateDatabase
import com.health.heartrate.phone.manager.HeartRateSessionManager
import com.health.heartrate.phone.sensor.CameraHeartRateSensor
import com.health.heartrate.phone.usecase.StartPhoneMonitoringUseCase
import kotlinx.coroutines.*

/**
 * Foreground service for phone-based heart rate monitoring.
 * Keeps measurement alive when app is in background.
 */
class PhoneHeartRateService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var useCase: StartPhoneMonitoringUseCase
    private var currentSessionId: String? = null

    override fun onCreate() {
        super.onCreate()
        val db             = HeartRateDatabase.getInstance(applicationContext)
        val sessionManager = HeartRateSessionManager(db.sessionDao(), db.recordDao())
        val sensor         = CameraHeartRateSensor(applicationContext)
        val alertManager   = HeartRateAlertManager(applicationContext)
        val chartManager   = RealTimeChartDataManager()
        useCase = StartPhoneMonitoringUseCase(sensor, sessionManager, alertManager, chartManager)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIF_ID, buildNotification("Đang đo nhịp tim..."))
                scope.launch {
                    currentSessionId = useCase.execute(scope)
                }
            }
            ACTION_STOP -> {
                scope.launch {
                    currentSessionId?.let { useCase.stop(it) }
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Theo dõi nhịp tim")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

    companion object {
        const val ACTION_START = "ACTION_START_PHONE_HR"
        const val ACTION_STOP  = "ACTION_STOP_PHONE_HR"
        const val CHANNEL_ID   = "phone_hr_service"
        const val NOTIF_ID     = 10
    }
}
