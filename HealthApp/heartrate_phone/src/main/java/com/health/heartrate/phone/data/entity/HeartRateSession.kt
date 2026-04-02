package com.health.heartrate.phone.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "heart_rate_sessions",
    indices = [Index("startTime"), Index("source")]
)
data class HeartRateSession(
    @PrimaryKey val sessionId: String,
    val source:          String,
    val startTime:       Long,
    val endTime:         Long? = null,
    val avgBpm:          Float = 0f,
    val maxBpm:          Int   = 0,
    val minBpm:          Int   = 999,
    val sampleCount:     Int   = 0,
    val durationSeconds: Long  = 0L,
    val isCompleted:     Boolean = false
)
