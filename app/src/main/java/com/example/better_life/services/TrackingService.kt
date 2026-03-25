package com.example.better_life.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlin.getValue

class TrackingService : Service() {
    private val locationClient: FusedLocationProviderClient by lazy { 
        LocationServices.getFusedLocationProviderClient(this) 
    }
    private var totalDistanceMeters = 0f
    private var lastLocation: Location? = null
    private var startTime = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val current = result.lastLocation ?: return
            lastLocation?.let { prev ->
                val delta = prev.distanceTo(current)
                if (delta > 1.5f) { // Only count if moved more than 1.5m to avoid jitter
                    totalDistanceMeters += delta
                }
            }
            lastLocation = current
            val durationSeconds = (System.currentTimeMillis() - startTime) / 1000L
            
            // Broadcast updates to Activity
            sendBroadcast(Intent(ACTION_UPDATE).apply {
                setPackage(packageName)
                putExtra(EXTRA_DISTANCE, totalDistanceMeters)
                putExtra(EXTRA_DURATION, durationSeconds)
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startTime = System.currentTimeMillis()
                startForeground(NOTIFICATION_ID, createNotification())
                requestLocationUpdates()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun requestLocationUpdates() {
        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateDistanceMeters(2f)
                .build()
            locationClient.requestLocationUpdates(request, locationCallback, mainLooper)
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Better Life Tracking", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Better Life")
        .setContentText("Đang theo dõi hoạt động của bạn...")
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setOngoing(true)
        .build()

    override fun onDestroy() {
        locationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE = "ACTION_UPDATE"
        const val EXTRA_DISTANCE = "EXTRA_DISTANCE"
        const val EXTRA_DURATION = "EXTRA_DURATION"
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 1001
    }
}
