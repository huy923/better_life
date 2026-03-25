package com.health.app.service

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class TrackingService : Service() {
    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var totalDistanceMeters = 0f
    private var lastLocation: Location? = null
    private val startTime = System.currentTimeMillis()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val current = result.lastLocation ?: return
            lastLocation?.let { prev ->
                val delta = prev.distanceTo(current)
                if (delta > 1f) totalDistanceMeters += delta
            }
            lastLocation = current
            val dur = (System.currentTimeMillis() - startTime) / 1000L
            sendBroadcast(Intent(ACTION_LOCATION_UPDATE).apply {
                putExtra(EXTRA_DISTANCE, totalDistanceMeters)
                putExtra(EXTRA_DURATION, dur)
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP  -> stopSelf()
        }
        return START_STICKY
    }

    private fun startTracking() {
        startForeground(NOTIFICATION_ID, NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Đang theo dõi lộ trình")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true).build())
        try {
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateDistanceMeters(5f).build()
            locationClient.requestLocationUpdates(req, locationCallback, mainLooper)
        } catch (e: SecurityException) { stopSelf() }
    }

    override fun onDestroy() { locationClient.removeLocationUpdates(locationCallback) }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START           = "ACTION_START_TRACKING"
        const val ACTION_STOP            = "ACTION_STOP_TRACKING"
        const val ACTION_LOCATION_UPDATE = "ACTION_LOCATION_UPDATE"
        const val EXTRA_DISTANCE         = "extra_distance"
        const val EXTRA_DURATION         = "extra_duration"
        const val CHANNEL_ID             = "tracking_channel"
        const val NOTIFICATION_ID        = 1
    }
}
