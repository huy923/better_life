package com.health.heartrate.phone.usecase

import com.health.heartrate.phone.alert.HeartRateAlertManager
import com.health.heartrate.phone.chart.RealTimeChartDataManager
import com.health.heartrate.phone.manager.HeartRateSessionManager
import com.health.heartrate.phone.sensor.CameraHeartRateSensor
import com.health.heartrate.shared.constants.DataLayerConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class StartPhoneMonitoringUseCase(
    private val sensor:         CameraHeartRateSensor,
    private val sessionManager: HeartRateSessionManager,
    private val alertManager:   HeartRateAlertManager,
    private val chartManager:   RealTimeChartDataManager
) {
    private var monitoringJob: Job? = null

    suspend fun execute(scope: CoroutineScope): String {
        val sessionId = sessionManager.startSession(DataLayerConstants.SOURCE_PHONE)
        sensor.start()

        monitoringJob = scope.launch {
            sensor.bpmFlow.collect { bpm ->
                val record = sessionManager.recordSample(
                    bpm = bpm, accuracy = 2,
                    source = DataLayerConstants.SOURCE_PHONE,
                    sessionId = sessionId
                )
                chartManager.addRecord(record)
                alertManager.evaluate(bpm, DataLayerConstants.SOURCE_PHONE)
            }
        }
        return sessionId
    }

    suspend fun stop(sessionId: String) {
        monitoringJob?.cancel()
        sensor.stop()
        sessionManager.endSession(sessionId)
        chartManager.reset()
    }
}
