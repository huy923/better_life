package com.example.better_life.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.better_life.MainActivity
import com.example.better_life.R
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.data.entities.SleepDataEntity
import com.example.better_life.data.entities.SleepSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class SleepTrackingService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var database: AppDatabase
    
    private var currentSessionId: Long = -1
    private val dataBuffer = mutableListOf<SleepDataEntity>()
    private val samplingHandler = Handler(Looper.getMainLooper())
    
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "SleepTrackingChannel"
        const val NOTIFICATION_ID = 1001
        const val SAMPLING_INTERVAL_MS = 10000L // 10 seconds
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BetterLife::SleepWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_TRACKING") {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Đang phân tích giấc ngủ..."))
        
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        wakeLock?.acquire(10 * 60 * 60 * 1000L) // 10 hours max
        
        scope.launch {
            val session = SleepSessionEntity(startTime = System.currentTimeMillis())
            currentSessionId = database.sleepTrackingDao().insertSession(session)
            
            samplingHandler.post(object : Runnable {
                override fun run() {
                    recordData()
                    samplingHandler.postDelayed(this, SAMPLING_INTERVAL_MS)
                }
            })
        }

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun recordData() {
        if (currentSessionId == -1L) return
        
        val magnitude = sqrt(lastX * lastX + lastY * lastY + lastZ * lastZ)
        val stage = classifyStage(magnitude)
        
        val data = SleepDataEntity(
            sessionId = currentSessionId,
            timestamp = System.currentTimeMillis(),
            movementX = lastX,
            movementY = lastY,
            movementZ = lastZ,
            magnitude = magnitude,
            stage = stage
        )
        
        dataBuffer.add(data)
        if (dataBuffer.size >= 10) {
            val batch = dataBuffer.toList()
            dataBuffer.clear()
            scope.launch { database.sleepTrackingDao().insertSleepDataBatch(batch) }
        }
        
        updateNotification("Trạng thái: ${getStageName(stage)}")
    }

    private fun classifyStage(mag: Float): Int {
        return when {
            mag < 0.5f -> 2 // Deep
            mag < 1.5f -> 1 // Light
            mag < 3.0f -> 3 // REM
            else -> 0 // Awake
        }
    }

    private fun getStageName(stage: Int) = when(stage) {
        0 -> "Thức"
        1 -> "Ngủ nông"
        2 -> "Ngủ sâu"
        3 -> "REM"
        else -> "N/A"
    }

    override fun onSensorChanged(event: SensorEvent) {
        lastX = event.values[0]
        lastY = event.values[1]
        lastZ = event.values[2]
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Sleep Tracker", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        val stopIntent = Intent(this, SleepTrackingService::class.java).apply { action = "STOP_TRACKING" }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Better Life - Theo dõi giấc ngủ")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_power_off, "Dừng theo dõi", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        samplingHandler.removeCallbacksAndMessages(null)
        sensorManager.unregisterListener(this)
        wakeLock?.let { if (it.isHeld) it.release() }
        
        scope.launch {
            val session = database.sleepTrackingDao().getSessionById(currentSessionId)
            session?.let {
                it.endTime = System.currentTimeMillis()
                it.totalDuration = ((it.endTime - it.startTime) / 60000).toInt()
                database.sleepTrackingDao().updateSession(it)
            }
        }
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null
}
