package com.health.heartrate.wear.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local buffer on the watch.
 * Sent to phone via Data Layer then kept for 24h as fallback.
 */
@Entity(
    tableName = "wear_heart_rate_records",
    indices = [Index("sessionId"), Index("synced")]
)
data class WearHeartRateRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val bpm:       Int,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy:  Int,
    val synced:    Boolean = false
)
