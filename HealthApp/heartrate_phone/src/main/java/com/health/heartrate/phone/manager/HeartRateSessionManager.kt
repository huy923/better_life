package com.health.heartrate.phone.manager

import com.health.heartrate.phone.data.dao.HeartRateRecordDao
import com.health.heartrate.phone.data.dao.HeartRateSessionDao
import com.health.heartrate.phone.data.entity.HeartRateRecord
import com.health.heartrate.phone.data.entity.HeartRateSession
import com.health.heartrate.shared.constants.DataLayerConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manages lifecycle of a heart rate measurement session.
 * Thread-safe: all mutations via suspend functions.
 */
class HeartRateSessionManager(
    private val sessionDao: HeartRateSessionDao,
    private val recordDao:  HeartRateRecordDao
) {
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    suspend fun startSession(source: String): String {
        val id = UUID.randomUUID().toString()
        sessionDao.insert(HeartRateSession(
            sessionId = id,
            source    = source,
            startTime = System.currentTimeMillis()
        ))
        _activeSessionId.value = id
        _isActive.value        = true
        return id
    }

    suspend fun recordSample(
        bpm: Int,
        accuracy: Int,
        source: String,
        sessionId: String
    ): HeartRateRecord {
        val isAnomaly = bpm > DataLayerConstants.ALERT_HIGH_BPM ||
                        bpm < DataLayerConstants.ALERT_LOW_BPM
        val record = HeartRateRecord(
            sessionId = sessionId,
            bpm       = bpm,
            accuracy  = accuracy,
            source    = source,
            isAnomaly = isAnomaly
        )
        recordDao.insert(record)
        return record
    }

    suspend fun endSession(sessionId: String) {
        val avg   = recordDao.avgBpmForSession(sessionId) ?: 0f
        val max   = recordDao.maxBpmForSession(sessionId) ?: 0
        val min   = recordDao.minBpmForSession(sessionId) ?: 0
        val count = recordDao.countForSession(sessionId)
        val session = sessionDao.getById(sessionId) ?: return

        sessionDao.update(session.copy(
            endTime         = System.currentTimeMillis(),
            avgBpm          = avg,
            maxBpm          = max,
            minBpm          = min,
            sampleCount     = count,
            durationSeconds = (System.currentTimeMillis() - session.startTime) / 1000L,
            isCompleted     = true
        ))
        if (_activeSessionId.value == sessionId) {
            _activeSessionId.value = null
            _isActive.value        = false
        }
    }

    suspend fun resumeOrphanSession(): String? {
        val orphan = sessionDao.getActiveSession() ?: return null
        _activeSessionId.value = orphan.sessionId
        _isActive.value        = true
        return orphan.sessionId
    }
}
