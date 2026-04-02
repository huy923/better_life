package com.health.heartrate.phone.repository

import com.health.heartrate.phone.data.dao.*
import com.health.heartrate.phone.data.db.HeartRateDatabase
import com.health.heartrate.phone.data.entity.*
import kotlinx.coroutines.flow.Flow

class HeartRateRepository(db: HeartRateDatabase) {

    private val recordDao = db.recordDao()
    private val sessionDao = db.sessionDao()
    private val statDao = db.dailyStatDao()

    fun observeAllSessions(): Flow<List<HeartRateSession>> = sessionDao.observeAll()

    fun observeSessionsBySource(source: String): Flow<List<HeartRateSession>> =
        sessionDao.observeBySource(source)

    fun observeSessionById(id: String): Flow<HeartRateSession?> =
        sessionDao.observeById(id)

    fun observeRecordsBySession(sessionId: String): Flow<List<HeartRateRecord>> =
        recordDao.observeBySession(sessionId)

    fun observeLatestSamples(sessionId: String, limit: Int = 60): Flow<List<HeartRateRecord>> =
        recordDao.observeLatestSamples(sessionId, limit)

    fun observeRecordsBySource(source: String): Flow<List<HeartRateRecord>> =
        recordDao.observeBySource(source)

    fun observeRecordsInRange(from: Long, to: Long): Flow<List<HeartRateRecord>> =
        recordDao.observeInRange(from, to)

    fun observeAnomalies(): Flow<List<HeartRateRecord>> =
        recordDao.observeAnomalies()

    fun observeDailyStats(days: Int, source: String? = null): Flow<List<DailyHeartRateStat>> =
        if (source != null) statDao.observeLastNDaysBySource(source, days)
        else statDao.observeLastNDays(days)

    suspend fun upsertDailyStat(stat: DailyHeartRateStat) = statDao.upsert(stat)

    // Retention
    suspend fun purgeOldRecords(cutoffMs: Long): Int = recordDao.deleteOlderThan(cutoffMs)
    suspend fun purgeOldSessions(cutoffMs: Long): Int = sessionDao.deleteOlderThan(cutoffMs)
    suspend fun purgeOldStats(cutoffDate: String): Int = statDao.deleteOlderThan(cutoffDate)
}
