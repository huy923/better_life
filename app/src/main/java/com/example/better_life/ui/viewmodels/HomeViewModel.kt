package com.example.better_life.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.data.entities.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * Handles data loading and business logic for dashboard stats.
 */
class HomeViewModel(private val database: AppDatabase) : ViewModel() {

    val user = database.userDao().getUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestHeartRate = database.heartRateDao().getLatestRecord()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayCalories = database.mealDao().getTodayTotalCalories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val latestSleep = database.sleepDao().getLatestRecord()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentWeight = database.userDao().getUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateWeight(newWeight: Double) {
        viewModelScope.launch {
            val currentUser = database.userDao().getUser().firstOrNull()
            if (currentUser != null) {
                database.userDao().update(currentUser.copy(weight = newWeight))
            } else {
                database.userDao().insertOrUpdate(
                    User(id = 1, name = "Người dùng", age = 25, height = 170, weight = newWeight)
                )
            }
            // Also insert a history record
            database.weightDao().insertWeight(com.example.better_life.data.entities.WeightRecord(weight = newWeight))
        }
    }

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}