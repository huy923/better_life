package com.health.app.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.health.app.data.db.HealthDatabase
import com.health.app.data.entity.DailyGoal
import com.health.app.data.entity.UserProfile
import com.health.app.repository.HealthRepository
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = HealthRepository(HealthDatabase.getInstance(application))
    val userProfile  = repo.getUserProfile().asLiveData()
    val goal         = repo.getGoal().asLiveData()

    fun saveProfile(profile: UserProfile, goal: DailyGoal) = viewModelScope.launch {
        repo.saveUserProfile(profile)
        repo.saveGoal(goal)
    }
}
