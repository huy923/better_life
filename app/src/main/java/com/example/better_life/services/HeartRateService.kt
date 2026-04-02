package com.example.better_life.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.better_life.MainActivity
import com.example.better_life.R
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.data.entities.HeartRateRecord
import com.example.better_life.sensor.CameraHeartRateSensor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class HeartRateService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var sensor: CameraHeartRateSensor
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        sensor = CameraHeartRateSensor(this)
        database = AppDatabase.getDatabase(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                createNotificationChannel()
                val notification = buildNotification("Đang chuẩn bị đo...")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                startMeasurement()
            }
            ACTION_STOP -> {
                stopMeasurement()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startMeasurement() {
        serviceScope.launch {
            sensor.start()
        }

        serviceScope.launch {
            sensor.statusFlow.collectLatest { status ->
                val text = when (status) {
                    CameraHeartRateSensor.SensorStatus.STARTING -> "Đang khởi động camera..."
                    CameraHeartRateSensor.SensorStatus.MEASURING -> "Đang đo nhịp tim..."
                    CameraHeartRateSensor.SensorStatus.FINGER_NOT_DETECTED -> "Vui lòng đặt ngón tay lên camera và đèn flash"
                    CameraHeartRateSensor.SensorStatus.ERROR -> "Lỗi cảm biến"
                    else -> "Sẵn sàng"
                }
                updateNotification(text)
            }
        }

        serviceScope.launch {
            sensor.bpmFlow.collectLatest { bpm ->
                updateNotification("Nhịp tim: $bpm BPM")
                saveRecord(bpm)
            }
        }
    }

    private suspend fun saveRecord(bpm: Int) {
        withContext(Dispatchers.IO) {
            val record = HeartRateRecord(
                bpm = bpm,
                timestamp = System.currentTimeMillis(),
                status = "Đo bằng camera"
            )
            database.heartRateDao().insert(record)
        }
    }

    private fun stopMeasurement() {
        sensor.stop()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Better Life Heart Rate",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, HeartRateService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Theo dõi nhịp tim")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_heart)
            .setContentIntent(pendingIntent)
            .addAction(0, "Dừng đo", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        sensor.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START_HR"
        const val ACTION_STOP = "ACTION_STOP_HR"
        const val CHANNEL_ID = "heart_rate_channel"
        const val NOTIFICATION_ID = 1002
    }
}
