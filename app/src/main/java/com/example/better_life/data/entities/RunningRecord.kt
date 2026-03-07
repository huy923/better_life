package com.example.better_life.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running_records")
data class RunningRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // e.g., "Hôm nay", "Hôm qua"
    val activityType: String, // "Chạy bộ", "Đi bộ"
    val duration: String, // "00:30:00"
    val steps: Int,
    val calories: Int,
    val distance: Double,
    val timestamp: Long
)
