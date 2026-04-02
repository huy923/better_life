package com.health.heartrate.phone.manager

import android.app.Application
import android.content.Intent
import androidx.lifecycle.*
import com.health.heartrate.phone.alert.HeartRateAlertManager
import com.health.heartrate.phone.chart.RealTimeChartDataManager
import com.health.heartrate.phone.data.db.HeartRateDatabase
import com.health.heartrate.phone.healthconnect.HealthConnectManager
import com.health.heartrate.phone.repository.HeartRateRepository
import com.health.heartrate.phone.service.PhoneHeartRateService
import com.health.heartrate.phone.sync.PhoneCommandSender
import com.health.heartrate.phone.usecase.GetStatisticsUseCase
import com.health.heartrate.shared.constants.DataLayerConstants
import kotlinx.coroutines.launch

class HeartRateViewModel(application: Application) : AndroidViewModel(application) {

    private val db           = HeartRateDatabase.getInstance(application)
    private val repo         = HeartRateRepository(db)
    private val statsUseCase = GetStatisticsUseCase(repo)
    val chartManager         = RealTimeChartDataManager(windowSize = 60)
    val alertManager         = HeartRateAlertManager(application)
    private val commandSender = PhoneCommandSender(application)
    val healthConnect        = HealthConnectManager(application)

    // ── Data streams ──────────────────────────────────────────────────────────
    val phoneSessions     = statsUseCase.getDailySessions(DataLayerConstants.SOURCE_PHONE).asLiveData()
    val watchSessions     = statsUseCase.getDailySessions(DataLayerConstants.SOURCE_WATCH).asLiveData()
    val allSessions       = statsUseCase.getDailySessions().asLiveData()
    val weeklyPhoneStats  = statsUseCase.getWeeklyStats(DataLayerConstants.SOURCE_PHONE).asLiveData()
    val weeklyWatchStats  = statsUseCase.getWeeklyStats(DataLayerConstants.SOURCE_WATCH).asLiveData()
    val monthlyStats      = statsUseCase.getMonthlyStats().asLiveData()
    val anomalies         = statsUseCase.getAnomalies().asLiveData()
    val alerts            = alertManager.alertFlow.asLiveData()
    val chartData         = chartManager.chartDataFlow.asLiveData()
    val chartStats        = chartManager.statsFlow.asLiveData()

    private val _isPhoneMonitoring = MutableLiveData(false)
    val isPhoneMonitoring: LiveData<Boolean> = _isPhoneMonitoring

    private val _isWatchMonitoring = MutableLiveData(false)
    val isWatchMonitoring: LiveData<Boolean> = _isWatchMonitoring

    private val _hcAvailable = MutableLiveData(false)
    val hcAvailable: LiveData<Boolean> = _hcAvailable

    init {
        _hcAvailable.value = healthConnect.isAvailable
    }

    // ── Phone monitoring ──────────────────────────────────────────────────────
    fun startPhoneMonitoring() {
        val ctx = getApplication<Application>()
        ctx.startForegroundService(Intent(ctx, PhoneHeartRateService::class.java).apply {
            action = PhoneHeartRateService.ACTION_START
        })
        _isPhoneMonitoring.value = true
    }

    fun stopPhoneMonitoring() {
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, PhoneHeartRateService::class.java).apply {
            action = PhoneHeartRateService.ACTION_STOP
        })
        _isPhoneMonitoring.value = false
        chartManager.reset()
    }

    // ── Watch monitoring ──────────────────────────────────────────────────────
    fun startWatchMonitoring() {
        viewModelScope.launch {
            commandSender.startWatchMonitoring()
            _isWatchMonitoring.value = true
        }
    }

    fun stopWatchMonitoring() {
        viewModelScope.launch {
            commandSender.stopWatchMonitoring()
            _isWatchMonitoring.value = false
        }
    }

    // ── Health Connect ────────────────────────────────────────────────────────
    fun syncHealthConnect() {
        viewModelScope.launch {
            healthConnect.syncFromHealthConnect { sample ->
                // HC samples flow into local chart for display
            }
        }
    }

    fun observeSessionChart(sessionId: String): LiveData<List<RealTimeChartDataManager.ChartPoint>> =
        repo.observeLatestSamples(sessionId, 60)
            .map { records -> records.mapIndexed { i, r ->
                RealTimeChartDataManager.ChartPoint(i.toFloat(), r.bpm.toFloat(), r.timestamp)
            }}.asLiveData()
}
