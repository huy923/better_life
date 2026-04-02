package com.health.heartrate.wear.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wear_sessions")
data class WearHeartRateSession(
    @PrimaryKey val sessionId: String,
    val startTime:   Long,
    val endTime:     Long? = null,
    val avgBpm:      Float = 0f,
    val maxBpm:      Int   = 0,
    val minBpm:      Int   = 999,
    val sampleCount: Int   = 0,
    val synced:      Boolean = false
)
