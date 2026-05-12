package com.example.better_life.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.better_life.data.UserManager
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
class HomeViewModel(private val database: AppDatabase, private val userManager: UserManager) : ViewModel() {

    val user = userManager.userFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), userManager.getUser())

    val latestHeartRate = database.heartRateDao().getLatestRecord()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayCalories = database.mealDao().getTodayTotalCalories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val latestSleep = database.sleepDao().getLatestRecord()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentWeight = userManager.userFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), userManager.getUser())

    fun updateWeight(newWeight: Double) {
        viewModelScope.launch {
            val currentUser = userManager.getUser()
            userManager.saveUser(currentUser.copy(weight = newWeight))
            
            // Also insert a history record
            database.weightDao().insertWeight(com.example.better_life.data.entities.WeightRecord(weight = newWeight))
        }
    }

    class Factory(private val database: AppDatabase, private val userManager: UserManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(database, userManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}