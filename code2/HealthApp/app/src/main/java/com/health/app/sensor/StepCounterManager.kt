package com.health.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StepCounterManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val stepSensor    = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var baselineSteps = -1
    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()
    val isAvailable: Boolean get() = stepSensor != null

    fun start() {
        baselineSteps = -1; _steps.value = 0
        stepSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() = sensorManager?.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val total = event.values[0].toInt()
        if (baselineSteps < 0) baselineSteps = total
        _steps.value = total - baselineSteps
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
