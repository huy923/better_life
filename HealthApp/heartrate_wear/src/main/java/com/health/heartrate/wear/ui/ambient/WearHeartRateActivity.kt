package com.health.heartrate.wear.ui.ambient

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.health.heartrate.wear.R
import com.health.heartrate.wear.data.db.WearHeartRateDatabase
import com.health.heartrate.wear.sensor.WearHeartRateSensor
import com.health.heartrate.wear.service.WearHeartRateService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

/**
 * Main Wear OS activity.
 * Displays live BPM on the watch screen.
 * Supports ambient mode (dim display when wrist lowered).
 */
class WearHeartRateActivity : FragmentActivity() {

    private lateinit var tvBpm:     TextView
    private lateinit var tvStatus:  TextView
    private lateinit var btnToggle: Button
    private lateinit var pbAnim:    ProgressBar

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isMonitoring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wear_heart_rate)

        tvBpm     = findViewById(R.id.tvWearBpm)
        tvStatus  = findViewById(R.id.tvWearStatus)
        btnToggle = findViewById(R.id.btnWearToggle)
        pbAnim    = findViewById(R.id.pbHeartAnim)

        btnToggle.setOnClickListener { toggleMonitoring() }

        observeDatabase()
    }

    private fun toggleMonitoring() {
        isMonitoring = !isMonitoring
        val action = if (isMonitoring) WearHeartRateService.ACTION_START
                     else              WearHeartRateService.ACTION_STOP
        startService(Intent(this, WearHeartRateService::class.java).apply {
            this.action = action
        })
        btnToggle.text  = if (isMonitoring) "Dừng" else "Bắt đầu"
        tvStatus.text   = if (isMonitoring) "Đang đo..." else "Chờ"
        pbAnim.visibility = if (isMonitoring) android.view.View.VISIBLE else android.view.View.INVISIBLE
    }

    private fun observeDatabase() {
        val db = WearHeartRateDatabase.getInstance(applicationContext)
        scope.launch {
            db.heartRateDao().observeBySession("").collect { records ->
                val latest = records.lastOrNull() ?: return@collect
                tvBpm.text = "${latest.bpm}"
                tvBpm.setTextColor(bpmColor(latest.bpm))
            }
        }
    }

    private fun bpmColor(bpm: Int): Int = when {
        bpm > 120 -> android.graphics.Color.parseColor("#E24B4A")
        bpm < 45  -> android.graphics.Color.parseColor("#378ADD")
        else      -> android.graphics.Color.parseColor("#1D9E75")
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
