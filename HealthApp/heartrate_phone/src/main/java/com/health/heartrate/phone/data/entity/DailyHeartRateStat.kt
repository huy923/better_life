package com.health.heartrate.phone.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Pre-aggregated daily stats — recomputed each midnight by StatAggregatorWorker */
@Entity(tableName = "daily_heart_rate_stats")
data class DailyHeartRateStat(
    @PrimaryKey val date: String,   // "yyyy-MM-dd"
    val source:       String,
    val avgBpm:       Float,
    val maxBpm:       Int,
    val minBpm:       Int,
    val restingBpm:   Float,
    val sampleCount:  Int,
    val sessionCount: Int
)
