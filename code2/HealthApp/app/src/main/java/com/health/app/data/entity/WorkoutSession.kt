package com.health.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val distanceMeters: Float,
    val steps: Int,
    val durationSeconds: Long,
    val caloriesBurned: Float,
    val activityType: String
)
