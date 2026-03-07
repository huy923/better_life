package com.example.better_life.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heart_rate_records")
data class HeartRateRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bpm: Int,
    val timestamp: Long,
    val status: String // e.g., "Tập luyện", "Hoạt động", "Bình thường", "Nghỉ ngơi"
)
