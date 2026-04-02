package com.health.heartrate.phone.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Single BPM reading stored on the phone.
 * Rows older than 30 days are purged nightly by RetentionWorker.
 */
@Entity(
    tableName = "heart_rate_records",
    indices = [
        Index("sessionId"),
        Index("timestamp"),
        Index("source")
    ]
)
data class HeartRateRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId:  String,
    val bpm:        Int,
    val timestamp:  Long = System.currentTimeMillis(),
    val accuracy:   Int,          // 0=unreliable 1=low 2=medium 3=high
    val source:     String,       // "phone_camera" | "wear_os"
    val isAnomaly:  Boolean = false
)
