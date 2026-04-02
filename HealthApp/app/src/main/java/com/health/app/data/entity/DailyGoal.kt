package com.health.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoal(
    @PrimaryKey val id: Int = 1,
    val targetSteps: Int = 8000,
    val targetCalories: Float = 500f,
    val targetDistanceMeters: Float = 5000f
)
