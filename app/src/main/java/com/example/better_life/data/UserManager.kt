package com.example.better_life.data

import android.content.Context
import android.content.SharedPreferences
import com.example.better_life.data.entities.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class UserManager(context: Context){
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    private val _userFlow = MutableStateFlow(getUser())
    val userFlow: StateFlow<User> = _userFlow.asStateFlow()

    fun getUserFlow(): Flow<User> = userFlow

    fun getUser(): User {
        return User(
            id = 1,
            name = prefs.getString("name", "Nguyễn Văn A") ?: "Nguyễn Văn A",
            age = prefs.getInt("age", 25),
            height = prefs.getInt("height", 170),
            weight = prefs.getFloat("weight", 65.5f).toDouble(),
            targetWeight = prefs.getFloat("targetWeight", 65.0f).toDouble(),
            targetSteps = prefs.getInt("targetSteps", 10000),
            targetCalories = prefs.getInt("targetCalories", 2500),
            targetWater = prefs.getFloat("targetWater", 2.5f).toDouble()
        )
    }

    fun saveUser(user: User) {
        prefs.edit().apply {
            putString("name", user.name)
            putInt("age", user.age)
            putInt("height", user.height)
            putFloat("weight", user.weight.toFloat())
            putFloat("targetWeight", (user.targetWeight ?: 0.0).toFloat())
            putInt("targetSteps", user.targetSteps)
            putInt("targetCalories", user.targetCalories)
            putFloat("targetWater", user.targetWater.toFloat())
            apply()
        }
        _userFlow.value = user
    }
}
