package com.health.heartrate.shared.protocol

import com.health.heartrate.shared.model.SessionSummary
import org.json.JSONObject

object WearMessageSerializer {
    fun serializeSession(summary: SessionSummary): ByteArray {
        val json = JSONObject().apply {
            put("sessionId",       summary.sessionId)
            put("source",          summary.source)
            put("startTime",       summary.startTime)
            put("endTime",         summary.endTime)
            put("avgBpm",          summary.avgBpm.toDouble())
            put("maxBpm",          summary.maxBpm)
            put("minBpm",          summary.minBpm)
            put("sampleCount",     summary.sampleCount)
            put("durationSeconds", summary.durationSeconds)
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    fun deserializeSession(bytes: ByteArray): SessionSummary {
        val j = JSONObject(bytes.toString(Charsets.UTF_8))
        return SessionSummary(
            sessionId       = j.getString("sessionId"),
            source          = j.getString("source"),
            startTime       = j.getLong("startTime"),
            endTime         = j.getLong("endTime"),
            avgBpm          = j.getDouble("avgBpm").toFloat(),
            maxBpm          = j.getInt("maxBpm"),
            minBpm          = j.getInt("minBpm"),
            sampleCount     = j.getInt("sampleCount"),
            durationSeconds = j.getLong("durationSeconds")
        )
    }
}
