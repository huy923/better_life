package com.example.better_life.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.better_life.Animation
import com.example.better_life.R
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.data.entities.RunningRecord
import com.example.better_life.sensor.StepCounterManager
import com.example.better_life.services.TrackingService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class RunningManager(
    private val context: Context,
    private val database: AppDatabase,
    private val scope: LifecycleCoroutineScope,
    private val stepCounterManager: StepCounterManager
) {
    var isTracking = false
        private set
    
    var currentDistance = 0f
    var currentDuration = 0L

    fun setupRunningUI(view: View) {
        val btnPlay = view.findViewById<ImageButton>(R.id.btn_play_pause)
        btnPlay?.setOnClickListener { v ->
            Animation.applyClick(v) {
                if (isTracking) stopTracking()
                else startTracking()
                updateUI(view)
            }
        }
        updateUI(view)

        scope.launch {
            database.runningDao().getAllRecords().collectLatest { records ->
                val container = view.findViewById<LinearLayout>(R.id.ll_running_history_container)
                container?.removeAllViews()
                records.take(5).forEachIndexed { index, record ->
                    val item = LayoutInflater.from(context).inflate(R.layout.item_running_history, container, false)
                    item.findViewById<TextView>(R.id.tv_run_type).text = record.activityType
                    item.findViewById<TextView>(R.id.tv_run_distance).text = String.format(Locale.getDefault(), "%.2f km", record.distance)
                    container?.addView(item)
                    Animation.animateItemEntry(item, index)
                }
            }
        }
    }

    fun startTracking() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission should be handled by Activity
            return
        }
        isTracking = true
        val intent = Intent(context, TrackingService::class.java).apply { action = TrackingService.ACTION_START }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopTracking() {
        isTracking = false
        val intent = Intent(context, TrackingService::class.java).apply { action = TrackingService.ACTION_STOP }
        context.startService(intent)

        scope.launch {
            val record = RunningRecord(
                date = context.getString(R.string.today),
                activityType = context.getString(R.string.running),
                duration = formatDuration(currentDuration),
                steps = stepCounterManager.steps.value,
                calories = (currentDistance * 0.06).toInt(),
                distance = (currentDistance / 1000.0),
                timestamp = System.currentTimeMillis()
            )
            database.runningDao().insert(record)
            currentDistance = 0f
            currentDuration = 0L
        }
    }

    fun updateUI(view: View?) {
        view?.let {
            it.findViewById<TextView>(R.id.tv_timer)?.text = formatDuration(currentDuration)
            it.findViewById<TextView>(R.id.tv_distance)?.text = String.format(Locale.getDefault(), "%.2f", currentDistance / 1000.0)
            it.findViewById<TextView>(R.id.tv_steps)?.text = stepCounterManager.steps.value.toString()
            it.findViewById<TextView>(R.id.tv_calories)?.text = (currentDistance * 0.06).toInt().toString()

            val btnPlay = it.findViewById<ImageButton>(R.id.btn_play_pause)
            btnPlay?.setImageResource(if (isTracking) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
        }
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }
}
