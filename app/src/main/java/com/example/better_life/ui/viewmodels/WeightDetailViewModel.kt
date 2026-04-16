package com.example.better_life.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.data.entities.WeightRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WeightDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val weightDao = AppDatabase.getDatabase(application).weightDao()
    private val userDao = AppDatabase.getDatabase(application).userDao()

    val weightHistory = weightDao.getAllWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestWeight = weightDao.getLatestWeight()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val user = userDao.getUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addWeight(weight: Double) {
        viewModelScope.launch {
            val record = WeightRecord(weight = weight)
            AppDatabase.getDatabase(getApplication()).weightDao().insertWeight(record)
            
            // Also update user's current weight
            user.value?.let { currentUser ->
                userDao.update(currentUser.copy(weight = weight))
            }
        }
    }

    fun deleteWeight(record: WeightRecord) {
        viewModelScope.launch {
            weightDao.deleteWeight(record)
        }
    }

    fun updateTargetWeight(target: Double) {
        viewModelScope.launch {
            user.value?.let { currentUser ->
                userDao.update(currentUser.copy(targetWeight = target))
            }
        }
    }
}