package com.health.heartrate.wear.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class WearHrReading(val bpm: Int, val accuracy: Int, val timestamp: Long)

/**
 * Reads TYPE_HEART_RATE from Wear OS PPG hardware sensor (e.g. Samsung BioActive, Pixel Watch).
 * Uses SENSOR_DELAY_FASTEST for maximum resolution (~1 Hz from most Wear OS watches).
 */
class WearHeartRateSensor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val hrSensor      = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    private val _readings = MutableSharedFlow<WearHrReading>(replay = 1)
    val readings: SharedFlow<WearHrReading> = _readings.asSharedFlow()

    val isAvailable: Boolean get() = hrSensor != null

    fun start() {
        hrSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    fun stop() = sensorManager?.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_HEART_RATE) return
        val bpm = event.values[0].toInt()
        if (bpm <= 0) return
        kotlinx.coroutines.runBlocking {
            _readings.emit(WearHrReading(bpm, event.accuracy, System.currentTimeMillis()))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
