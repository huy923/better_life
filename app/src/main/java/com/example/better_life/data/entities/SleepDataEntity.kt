package com.example.better_life.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_raw_data")
data class SleepDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val movementX: Float,
    val movementY: Float,
    val movementZ: Float,
    val magnitude: Float,
    val stage: Int // 0: Awake, 1: Light, 2: Deep, 3: REM
)
