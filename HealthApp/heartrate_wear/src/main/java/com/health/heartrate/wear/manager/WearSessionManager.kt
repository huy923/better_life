package com.health.heartrate.wear.manager

import com.health.heartrate.wear.data.dao.WearHeartRateDao
import com.health.heartrate.wear.data.entity.WearHeartRateRecord
import com.health.heartrate.wear.data.entity.WearHeartRateSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class WearSessionManager(private val dao: WearHeartRateDao) {

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

    suspend fun startSession(): String {
        val id = UUID.randomUUID().toString()
        dao.insertSession(WearHeartRateSession(sessionId = id, startTime = System.currentTimeMillis()))
        _sessionId.value = id
        return id
    }

    suspend fun recordSample(bpm: Int, accuracy: Int, sessionId: String): WearHeartRateRecord {
        val record = WearHeartRateRecord(sessionId = sessionId, bpm = bpm, accuracy = accuracy)
        dao.insertRecord(record)
        return record
    }

    suspend fun endSession(sessionId: String) {
        val session = dao.getSession(sessionId) ?: return
        val records = dao.getUnsynced()
        if (records.isEmpty()) return
        val bpms = records.filter { it.sessionId == sessionId }.map { it.bpm }
        if (bpms.isEmpty()) return
        dao.updateSession(session.copy(
            endTime     = System.currentTimeMillis(),
            avgBpm      = bpms.average().toFloat(),
            maxBpm      = bpms.max(),
            minBpm      = bpms.min(),
            sampleCount = bpms.size
        ))
        _sessionId.value = null
    }

    suspend fun getUnsyncedRecords() = dao.getUnsynced()
    suspend fun markSynced(ids: List<Long>) = dao.markSynced(ids)

    suspend fun purgeOldBuffer() {
        val cutoff = System.currentTimeMillis() - 24 * 3600 * 1000L
        dao.deleteOlderThan(cutoff)
    }
}
