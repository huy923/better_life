package com.health.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.health.app.data.db.HealthDatabase
import com.health.app.repository.HealthRepository

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = HealthRepository(HealthDatabase.getInstance(application))
    val allSessions  = repo.getAllSessions().asLiveData()
    val weeklyStats  = repo.getWeeklyStats().asLiveData()
}
