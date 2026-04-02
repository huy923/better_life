package com.health.heartrate.wear.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.health.heartrate.shared.constants.DataLayerConstants
import com.health.heartrate.shared.model.HeartRateSample
import com.health.heartrate.shared.model.SessionSummary
import com.health.heartrate.shared.protocol.WearMessageSerializer
import com.health.heartrate.wear.data.entity.WearHeartRateRecord
import com.health.heartrate.wear.data.entity.WearHeartRateSession
import com.health.heartrate.wear.manager.WearSessionManager
import kotlinx.coroutines.tasks.await

/**
 * Sends heart rate data from watch to phone via Wearable Message API.
 * Live readings: PATH_HEART_RATE_LIVE (one message per sample)
 * Session summary: PATH_HEART_RATE_SESSION (on session end)
 */
class WearToPhoneSyncManager(
    private val context: Context,
    private val sessionManager: WearSessionManager
) {
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient    = Wearable.getNodeClient(context)

    suspend fun sendLiveSample(record: WearHeartRateRecord, sessionId: String) {
        val sample = HeartRateSample(
            bpm       = record.bpm,
            timestamp = record.timestamp,
            accuracy  = record.accuracy,
            source    = DataLayerConstants.SOURCE_WATCH,
            sessionId = sessionId
        )
        sendToPhone(DataLayerConstants.PATH_HEART_RATE_LIVE, sample.toByteArray())
    }

    suspend fun sendSessionSummary(session: WearHeartRateSession) {
        val summary = SessionSummary(
            sessionId       = session.sessionId,
            source          = DataLayerConstants.SOURCE_WATCH,
            startTime       = session.startTime,
            endTime         = session.endTime ?: System.currentTimeMillis(),
            avgBpm          = session.avgBpm,
            maxBpm          = session.maxBpm,
            minBpm          = session.minBpm,
            sampleCount     = session.sampleCount,
            durationSeconds = ((session.endTime ?: System.currentTimeMillis()) - session.startTime) / 1000L
        )
        sendToPhone(
            DataLayerConstants.PATH_HEART_RATE_SESSION,
            WearMessageSerializer.serializeSession(summary)
        )
    }

    suspend fun flushUnsynced() {
        val unsynced = sessionManager.getUnsyncedRecords()
        unsynced.forEach { record ->
            record.sessionId.let { sid ->
                sendLiveSample(record, sid)
            }
        }
        if (unsynced.isNotEmpty()) {
            sessionManager.markSynced(unsynced.map { it.id })
            Log.d(TAG, "Flushed ${unsynced.size} unsynced records")
        }
    }

    private suspend fun sendToPhone(path: String, data: ByteArray) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, path, data).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Send to phone failed: $path", e)
        }
    }

    companion object { private const val TAG = "WearSync" }
}
