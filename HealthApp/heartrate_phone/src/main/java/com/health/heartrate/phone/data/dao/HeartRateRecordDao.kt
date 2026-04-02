package com.health.heartrate.phone.data.dao

import androidx.room.*
import com.health.heartrate.phone.data.entity.HeartRateRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HeartRateRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HeartRateRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HeartRateRecord>)

    /** Live stream for real-time chart – emits on every new insert */
    @Query("""
        SELECT * FROM heart_rate_records
        WHERE sessionId = :sessionId
        ORDER BY timestamp ASC
    """)
    fun observeBySession(sessionId: String): Flow<List<HeartRateRecord>>

    /** Last N samples for live chart window */
    @Query("""
        SELECT * FROM heart_rate_records
        WHERE sessionId = :sessionId
        ORDER BY timestamp DESC LIMIT :limit
    """)
    fun observeLatestSamples(sessionId: String, limit: Int = 60): Flow<List<HeartRateRecord>>

    @Query("SELECT * FROM heart_rate_records WHERE source = :source ORDER BY timestamp DESC")
    fun observeBySource(source: String): Flow<List<HeartRateRecord>>

    @Query("""
        SELECT * FROM heart_rate_records
        WHERE timestamp BETWEEN :from AND :to
        ORDER BY timestamp ASC
    """)
    fun observeInRange(from: Long, to: Long): Flow<List<HeartRateRecord>>

    @Query("SELECT AVG(bpm) FROM heart_rate_records WHERE sessionId = :sessionId")
    suspend fun avgBpmForSession(sessionId: String): Float?

    @Query("SELECT MAX(bpm) FROM heart_rate_records WHERE sessionId = :sessionId")
    suspend fun maxBpmForSession(sessionId: String): Int?

    @Query("SELECT MIN(bpm) FROM heart_rate_records WHERE sessionId = :sessionId")
    suspend fun minBpmForSession(sessionId: String): Int?

    @Query("SELECT COUNT(*) FROM heart_rate_records WHERE sessionId = :sessionId")
    suspend fun countForSession(sessionId: String): Int

    /** Retention: delete records older than cutoff */
    @Query("DELETE FROM heart_rate_records WHERE timestamp < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int

    @Query("SELECT * FROM heart_rate_records WHERE isAnomaly = 1 ORDER BY timestamp DESC")
    fun observeAnomalies(): Flow<List<HeartRateRecord>>
}
