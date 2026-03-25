package com.example.better_life

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.services.SleepTrackingService
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.*

class SleepTrackingActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnStop: MaterialButton
    private lateinit var database: AppDatabase
    
    private var isTracking = true
    private var startTime: Long = System.currentTimeMillis()
    private val handler = Handler(Looper.getMainLooper())
    
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTracking) {
                val elapsed = System.currentTimeMillis() - startTime
                val hours = elapsed / 3600000
                val minutes = (elapsed % 3600000) / 60000
                val seconds = (elapsed % 60000) / 1000
                tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sleep_tracking)
        
        database = AppDatabase.getDatabase(this)
        tvStatus = findViewById(R.id.tv_tracking_status)
        tvTimer = findViewById(R.id.tv_tracking_timer)
        btnStop = findViewById(R.id.btn_stop_tracking)

        checkPermissions()
        
        btnStop.setOnClickListener {
            stopTracking()
        }

        handler.post(timerRunnable)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun stopTracking() {
        isTracking = false
        val intent = Intent(this, SleepTrackingService::class.java).apply { action = "STOP_TRACKING" }
        startService(intent)
        Toast.makeText(this, "Đã lưu phiên ngủ!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(timerRunnable)
        super.onDestroy()
    }
}
