package com.health.heartrate.shared.model

data class SessionSummary(
    val sessionId: String,
    val source: String,
    val startTime: Long,
    val endTime: Long,
    val avgBpm: Float,
    val maxBpm: Int,
    val minBpm: Int,
    val sampleCount: Int,
    val durationSeconds: Long
)
