package com.health.heartrate.phone.data.dao

import androidx.room.*
import com.health.heartrate.phone.data.entity.HeartRateSession
import kotlinx.coroutines.flow.Flow

@Dao
interface HeartRateSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: HeartRateSession)

    @Update
    suspend fun update(session: HeartRateSession)

    @Query("SELECT * FROM heart_rate_sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<HeartRateSession>>

    @Query("SELECT * FROM heart_rate_sessions WHERE source = :source ORDER BY startTime DESC")
    fun observeBySource(source: String): Flow<List<HeartRateSession>>

    @Query("SELECT * FROM heart_rate_sessions WHERE sessionId = :id LIMIT 1")
    suspend fun getById(id: String): HeartRateSession?

    @Query("SELECT * FROM heart_rate_sessions WHERE sessionId = :id LIMIT 1")
    fun observeById(id: String): Flow<HeartRateSession?>

    @Query("SELECT * FROM heart_rate_sessions WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): HeartRateSession?

    @Query("""
        SELECT * FROM heart_rate_sessions
        WHERE startTime BETWEEN :from AND :to
        ORDER BY startTime DESC
    """)
    fun observeInRange(from: Long, to: Long): Flow<List<HeartRateSession>>

    @Query("DELETE FROM heart_rate_sessions WHERE startTime < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int
}
