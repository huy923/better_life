package com.health.app.ui.tracking

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.*
import com.health.app.data.db.HealthDatabase
import com.health.app.data.entity.WorkoutSession
import com.health.app.repository.HealthRepository
import com.health.app.sensor.StepCounterManager
import com.health.app.service.TrackingService
import com.health.app.utils.CalorieCalculator
import kotlinx.coroutines.launch

class TrackingViewModel(application: Application) : AndroidViewModel(application) {
    private val repo    = HealthRepository(HealthDatabase.getInstance(application))
    private val stepMgr = StepCounterManager(application)

    val steps         = stepMgr.steps.asLiveData()
    val userProfile   = repo.getUserProfile().asLiveData()
    val goal          = repo.getGoal().asLiveData()
    val todayCalories = repo.getTodayCalories().asLiveData()

    private val _isTracking      = MutableLiveData(false)
    val isTracking: LiveData<Boolean> = _isTracking

    private val _distanceMeters  = MutableLiveData(0f)
    val distanceMeters: LiveData<Float> = _distanceMeters

    private val _caloriesLive    = MutableLiveData(0f)
    val caloriesLive: LiveData<Float> = _caloriesLive

    private val _durationSeconds = MutableLiveData(0L)
    val durationSeconds: LiveData<Long> = _durationSeconds

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val dist = intent.getFloatExtra(TrackingService.EXTRA_DISTANCE, 0f)
            val dur  = intent.getLongExtra(TrackingService.EXTRA_DURATION, 0L)
            _distanceMeters.value  = dist
            _durationSeconds.value = dur
            userProfile.value?.let {
                _caloriesLive.value = CalorieCalculator.fromGps(dur, it.weightKg, "walking")
            }
        }
    }

    init {
        getApplication<Application>().registerReceiver(
            locationReceiver,
            IntentFilter(TrackingService.ACTION_LOCATION_UPDATE),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    fun startTracking() {
        _isTracking.value = true
        stepMgr.start()
        val ctx = getApplication<Application>()
        ctx.startForegroundService(Intent(ctx, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
        })
    }

    fun stopAndSave() {
        _isTracking.value = false
        stepMgr.stop()
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, TrackingService::class.java).apply { action = TrackingService.ACTION_STOP })
        viewModelScope.launch {
            repo.saveSession(WorkoutSession(
                distanceMeters  = _distanceMeters.value ?: 0f,
                steps           = steps.value ?: 0,
                durationSeconds = _durationSeconds.value ?: 0L,
                caloriesBurned  = _caloriesLive.value ?: 0f,
                activityType    = "walking"
            ))
            _distanceMeters.value  = 0f
            _caloriesLive.value    = 0f
            _durationSeconds.value = 0L
        }
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(locationReceiver)
        super.onCleared()
    }
}
