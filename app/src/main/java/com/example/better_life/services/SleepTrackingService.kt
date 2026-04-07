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

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.content.IntentFilter
import android.content.BroadcastReceiver
import kotlin.math.log10

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

    // Audio tracking variables
    private var audioRecord: AudioRecord? = null
    private var isAudioRunning = false
    private val bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    private var lastAmplitude = 0.0

    private val chargingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            if (!isCharging) {
                updateNotification("Đã dừng: Vui lòng cắm sạc để tiếp tục theo dõi giấc ngủ")
                pauseTracking()
            } else {
                resumeTracking()
            }
        }
    }

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

        registerReceiver(chargingReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_TRACKING") {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Đang chờ cắm sạc để bắt đầu..."))
        
        return START_STICKY
    }

    private fun startTracking() {
        if (isAudioRunning) return
        
        wakeLock?.acquire(10 * 60 * 60 * 1000L) // 10 hours max
        
        scope.launch {
            val session = SleepSessionEntity(startTime = System.currentTimeMillis())
            currentSessionId = database.sleepTrackingDao().insertSession(session)
            
            startAudioRecording()
            
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

    private fun startAudioRecording() {
        try {
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
            audioRecord?.startRecording()
            isAudioRunning = true
            
            scope.launch {
                val buffer = ShortArray(bufferSize)
                while (isAudioRunning) {
                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (read > 0) {
                        var max = 0
                        for (i in 0 until read) {
                            if (Math.abs(buffer[i].toInt()) > max) max = Math.abs(buffer[i].toInt())
                        }
                        lastAmplitude = max.toDouble()
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun pauseTracking() {
        isAudioRunning = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        sensorManager.unregisterListener(this)
    }

    private fun resumeTracking() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            registerReceiver(null, ifilter)
        }
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
            startTracking()
            updateNotification("Đang phân tích giấc ngủ qua mic và cảm biến...")
        }
    }

    private fun recordData() {
        if (currentSessionId == -1L || !isAudioRunning) return
        
        val magnitude = sqrt(lastX * lastX + lastY * lastY + lastZ * lastZ)
        // Combine movement and sound (dB) for stage classification
        val db = if (lastAmplitude > 0) 20 * log10(lastAmplitude) else 0.0
        val stage = classifyStage(magnitude, db)
        
        val data = SleepDataEntity(
            sessionId = currentSessionId,
            timestamp = System.currentTimeMillis(),
            movementX = lastX,
            movementY = lastY,
            movementZ = lastZ,
            magnitude = magnitude,
            stage = stage
            // Note: Ideally SleepDataEntity should have a field for noise level
        )
        
        dataBuffer.add(data)
        if (dataBuffer.size >= 10) {
            val batch = dataBuffer.toList()
            dataBuffer.clear()
            scope.launch { database.sleepTrackingDao().insertSleepDataBatch(batch) }
        }
        
        updateNotification("Trạng thái: ${getStageName(stage)} (Tiếng ồn: ${db.toInt()}dB)")
    }

    private fun classifyStage(mag: Float, db: Double): Int {
        return when {
            mag > 3.0f || db > 60.0 -> 0 // Awake
            mag < 0.5f && db < 30.0 -> 2 // Deep
            mag < 1.5f && db < 45.0 -> 1 // Light
            else -> 3 // REM
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
