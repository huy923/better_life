package com.example.better_life.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.better_life.data.UserManager
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.data.entities.WeightRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WeightDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val weightDao = AppDatabase.getDatabase(application).weightDao()
    private val userManager = UserManager(application)

    val weightHistory = weightDao.getAllWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestWeight = weightDao.getLatestWeight()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val user = userManager.userFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), userManager.getUser())

    fun addWeight(weight: Double) {
        viewModelScope.launch {
            val record = WeightRecord(weight = weight)
            weightDao.insertWeight(record)
            
            // Also update user's current weight
            val currentUser = userManager.getUser()
            userManager.saveUser(currentUser.copy(weight = weight))
        }
    }

    fun deleteWeight(record: WeightRecord) {
        viewModelScope.launch {
            weightDao.deleteWeight(record)
        }
    }

    fun updateTargetWeight(target: Double) {
        viewModelScope.launch {
            val currentUser = userManager.getUser()
            userManager.saveUser(currentUser.copy(targetWeight = target))
        }
    }
}