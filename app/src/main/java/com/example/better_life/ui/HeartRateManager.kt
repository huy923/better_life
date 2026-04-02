package com.example.better_life.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.better_life.Animation
import com.example.better_life.R
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.services.HeartRateService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HeartRateManager(
    private val context: Context,
    private val database: AppDatabase,
    private val scope: LifecycleCoroutineScope
) {
    var isTracking = false
        private set

    fun setupHeartRateUI(view: View) {
        val btnStart = view.findViewById<View>(R.id.btn_start_hr)
        btnStart?.setOnClickListener { v ->
            Animation.applyClick(v) {
                if (isTracking) stopTracking()
                else startTracking()
                updateStatusUI(view)
            }
        }
        updateStatusUI(view)

        scope.launch {
            database.heartRateDao().getLatestRecord().collectLatest { it?.let { 
                view.findViewById<TextView>(R.id.tv_current_bpm)?.text = it.bpm.toString()
            } }
        }

        scope.launch {
            database.heartRateDao().getAllRecords().collectLatest { records ->
                val container = view.findViewById<LinearLayout>(R.id.ll_hr_history_container)
                container?.removeAllViews()
                records.take(10).forEachIndexed { index, record ->
                    val item = LayoutInflater.from(context).inflate(R.layout.item_heart_history, container, false)
                    item.findViewById<TextView>(R.id.tv_history_value).text = record.bpm.toString()
                    item.findViewById<TextView>(R.id.tv_history_time).text = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(record.timestamp))
                    item.findViewById<TextView>(R.id.tv_history_status).text = record.status
                    container?.addView(item)
                    Animation.animateItemEntry(item, index)
                }
            }
        }
    }

    private fun startTracking() {
        isTracking = true
        val intent = Intent(context, HeartRateService::class.java).apply { action = HeartRateService.ACTION_START }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopTracking() {
        isTracking = false
        val intent = Intent(context, HeartRateService::class.java).apply { action = HeartRateService.ACTION_STOP }
        context.startService(intent)
    }

    private fun updateStatusUI(view: View) {
        val btnStart = view.findViewById<TextView>(R.id.btn_start_hr)
        btnStart?.text = if (isTracking) "DỪNG ĐO" else "BẮT ĐẦU ĐO"
        btnStart?.setBackgroundResource(if (isTracking) R.drawable.bg_red_border else R.drawable.button_green)
    }
}
