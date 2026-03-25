package com.example.better_life.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    var endTime: Long = 0,
    var totalDuration: Int = 0, // in minutes
    var deepDuration: Int = 0,
    var lightDuration: Int = 0,
    var remDuration: Int = 0,
    var awakeDuration: Int = 0,
    var sleepQuality: Double = 0.0,
    var numAwakenings: Int = 0
)
