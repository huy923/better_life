package com.health.heartrate.phone.sync

import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.health.heartrate.phone.data.db.HeartRateDatabase
import com.health.heartrate.phone.manager.HeartRateSessionManager
import com.health.heartrate.phone.repository.HeartRateRepository
import com.health.heartrate.shared.constants.DataLayerConstants
import com.health.heartrate.shared.model.HeartRateSample
import com.health.heartrate.shared.protocol.WearMessageSerializer
import kotlinx.coroutines.*

/**
 * Receives real-time heart rate samples from Wear OS watch via Data Layer.
 * Registered in AndroidManifest as a WearableListenerService.
 */
class WearSyncListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sessionManager: HeartRateSessionManager
    private var activeWatchSessionId: String? = null

    override fun onCreate() {
        super.onCreate()
        val db = HeartRateDatabase.getInstance(applicationContext)
        sessionManager = HeartRateSessionManager(db.sessionDao(), db.recordDao())
    }

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            DataLayerConstants.PATH_HEART_RATE_LIVE -> {
                scope.launch { handleLiveSample(event.data) }
            }
            DataLayerConstants.PATH_HEART_RATE_SESSION -> {
                scope.launch { handleSessionSummary(event.data) }
            }
            DataLayerConstants.PATH_STATUS -> {
                Log.d(TAG, "Watch status: ${String(event.data)}")
            }
        }
    }

    private suspend fun handleLiveSample(data: ByteArray) {
        try {
            val sample = HeartRateSample.fromByteArray(data)
            val sessionId = activeWatchSessionId ?: run {
                val id = sessionManager.startSession(DataLayerConstants.SOURCE_WATCH)
                activeWatchSessionId = id
                id
            }
            sessionManager.recordSample(
                bpm       = sample.bpm,
                accuracy  = sample.accuracy,
                source    = DataLayerConstants.SOURCE_WATCH,
                sessionId = sessionId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing live sample", e)
        }
    }

    private suspend fun handleSessionSummary(data: ByteArray) {
        try {
            val summary = WearMessageSerializer.deserializeSession(data)
            activeWatchSessionId?.let { id ->
                sessionManager.endSession(id)
                activeWatchSessionId = null
            }
            Log.d(TAG, "Watch session complete: avg=${summary.avgBpm} BPM")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing session summary", e)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object { private const val TAG = "WearSyncListener" }
}
