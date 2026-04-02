package com.health.heartrate.wear.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.health.heartrate.wear.data.db.WearHeartRateDatabase
import com.health.heartrate.wear.manager.WearSessionManager
import com.health.heartrate.wear.sensor.WearHeartRateSensor
import com.health.heartrate.wear.sync.WearToPhoneSyncManager
import kotlinx.coroutines.*

/**
 * Foreground service on the watch.
 * - Reads PPG sensor at ~1 Hz
 * - Caches locally in Room
 * - Streams every sample to phone via Wearable Message API
 */
class WearHeartRateService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var sensor:         WearHeartRateSensor
    private lateinit var sessionManager: WearSessionManager
    private lateinit var syncManager:    WearToPhoneSyncManager
    private var currentSessionId: String? = null

    override fun onCreate() {
        super.onCreate()
        val db          = WearHeartRateDatabase.getInstance(applicationContext)
        sessionManager  = WearSessionManager(db.heartRateDao())
        sensor          = WearHeartRateSensor(applicationContext)
        syncManager     = WearToPhoneSyncManager(applicationContext, sessionManager)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP  -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        startForeground(NOTIF_ID, buildNotification("Đang đo nhịp tim..."))
        sensor.start()

        scope.launch {
            val sessionId = sessionManager.startSession()
            currentSessionId = sessionId

            sensor.readings.collect { reading ->
                // 1. Save locally
                val record = sessionManager.recordSample(reading.bpm, reading.accuracy, sessionId)
                // 2. Stream to phone immediately
                syncManager.sendLiveSample(record, sessionId)
                // 3. Update notification
                updateNotification("❤️ ${reading.bpm} BPM")
            }
        }
    }

    private fun stopMonitoring() {
        sensor.stop()
        scope.launch {
            currentSessionId?.let { id ->
                sessionManager.endSession(id)
                val db      = WearHeartRateDatabase.getInstance(applicationContext)
                val session = db.heartRateDao().getSession(id)
                session?.let { syncManager.sendSessionSummary(it) }
                sessionManager.purgeOldBuffer()
            }
            withContext(Dispatchers.Main) { stopSelf() }
        }
    }

    private fun updateNotification(text: String) {
        val notifManager = getSystemService(android.app.NotificationManager::class.java)
        notifManager?.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Heart Rate Monitor")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

    companion object {
        const val ACTION_START = "ACTION_START_WEAR_HR"
        const val ACTION_STOP  = "ACTION_STOP_WEAR_HR"
        const val CHANNEL_ID   = "wear_hr_service"
        const val NOTIF_ID     = 20
    }
}
