package com.example.better_life.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.better_life.Animation
import com.example.better_life.R
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.data.entities.RunningRecord
import com.example.better_life.sensor.StepCounterManager
import com.example.better_life.services.TrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class RunningManager(
    private val context: Context,
    private val database: AppDatabase,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val stepCounterManager: StepCounterManager
) {
    var isTracking = false
        private set
    
    var isWalking = false
        private set
    
    var currentDistance = 0f
    var currentDuration = 0L
    private var startSteps = 0
    private var startTimestamp = 0L
    private var timerJob: Job? = null
    private var currentView: View? = null

    fun setupRunningUI(view: View, uiScope: CoroutineScope) {
        currentView = view

        view.findViewById<View>(R.id.toggle_walking)?.setOnClickListener { v ->
            Animation.applyClick(v) {
                isWalking = true
                updateToggleUI(view)
            }
        }

        view.findViewById<View>(R.id.toggle_running)?.setOnClickListener { v ->
            Animation.applyClick(v) {
                isWalking = false
                updateToggleUI(view)
            }
        }

        val btnPlay = view.findViewById<ImageButton>(R.id.btn_play_pause)
        btnPlay?.setOnClickListener { v ->
            Animation.applyClick(v) {
                if (isTracking) stopTracking()
                else startTracking()
            }
        }

        updateUI()
        updateToggleUI(view)

        uiScope.launch {
            database.runningDao().getAllRecords().collectLatest { records ->
                val container = view.findViewById<LinearLayout>(R.id.ll_running_history_container)
                container?.removeAllViews()
                records.take(5).forEachIndexed { index, record ->
                    val item = LayoutInflater.from(context).inflate(R.layout.item_running_history, container, false)
                    item.findViewById<TextView>(R.id.tv_run_date)?.text = record.date
                    item.findViewById<TextView>(R.id.tv_run_type)?.text = record.activityType
                    item.findViewById<TextView>(R.id.tv_run_duration)?.text = record.duration
                    item.findViewById<TextView>(R.id.tv_run_steps)?.text = String.format(Locale.getDefault(), "%,d", record.steps)
                    item.findViewById<TextView>(R.id.tv_run_calories)?.text = record.calories.toString()
                    item.findViewById<TextView>(R.id.tv_run_distance)?.text = String.format(Locale.getDefault(), "%.1f km", record.distance)
                    container?.addView(item)
                    Animation.animateItemEntry(item, index)
                }
            }
        }
    }

    private fun updateToggleUI(view: View) {
        val walkingBg = view.findViewById<LinearLayout>(R.id.toggle_walking)
        val runningBg = view.findViewById<LinearLayout>(R.id.toggle_running)

        if (isWalking) {
            walkingBg?.setBackgroundResource(R.drawable.bg_pill_shape)
            walkingBg?.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            runningBg?.setBackgroundColor(Color.TRANSPARENT)
            runningBg?.backgroundTintList = null
            view.findViewById<TextView>(R.id.tv_walking_label)?.setTextColor(Color.parseColor("#26CE8D"))
            view.findViewById<TextView>(R.id.tv_running_label)?.setTextColor(Color.WHITE)
            view.findViewById<TextView>(R.id.tv_running_label)?.alpha = 0.8f
        } else {
            runningBg?.setBackgroundResource(R.drawable.bg_pill_shape)
            runningBg?.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            walkingBg?.setBackgroundColor(Color.TRANSPARENT)
            walkingBg?.backgroundTintList = null
            view.findViewById<TextView>(R.id.tv_running_label)?.setTextColor(Color.parseColor("#26CE8D"))
            view.findViewById<TextView>(R.id.tv_walking_label)?.setTextColor(Color.WHITE)
            view.findViewById<TextView>(R.id.tv_walking_label)?.alpha = 0.8f
        }
    }

    fun startTracking() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        isTracking = true
        currentDuration = 0L
        currentDistance = 0f
        startSteps = stepCounterManager.steps.value
        startTimestamp = System.currentTimeMillis()

        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            while (isTracking) {
                delay(1000L)
                currentDuration = (System.currentTimeMillis() - startTimestamp) / 1000L
                updateUI()
            }
        }

        updateUI()

        val intent = Intent(context, TrackingService::class.java).apply { action = TrackingService.ACTION_START }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopTracking() {
        isTracking = false
        timerJob?.cancel()
        timerJob = null

        val intent = Intent(context, TrackingService::class.java).apply { action = TrackingService.ACTION_STOP }
        context.startService(intent)

        lifecycleScope.launch {
            val record = RunningRecord(
                date = context.getString(R.string.today),
                activityType = if (isWalking) context.getString(R.string.walking) else context.getString(R.string.running),
                duration = formatDuration(currentDuration),
                steps = stepCounterManager.steps.value - startSteps,
                calories = (currentDistance * 0.06).toInt(),
                distance = (currentDistance / 1000.0),
                timestamp = System.currentTimeMillis()
            )
            database.runningDao().insert(record)
            currentDistance = 0f
            currentDuration = 0L
            startSteps = 0
        }

        updateUI()
    }

    fun updateUI(view: View? = currentView) {
        view?.let {
            it.findViewById<TextView>(R.id.tv_timer)?.text = formatDuration(currentDuration)
            it.findViewById<TextView>(R.id.tv_distance)?.text = String.format(Locale.getDefault(), "%.2f", currentDistance / 1000.0)
            it.findViewById<TextView>(R.id.tv_steps)?.text = (stepCounterManager.steps.value - startSteps).coerceAtLeast(0).toString()
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
