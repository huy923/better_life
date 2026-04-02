package com.health.heartrate.wear.data.dao

import androidx.room.*
import com.health.heartrate.wear.data.entity.WearHeartRateRecord
import com.health.heartrate.wear.data.entity.WearHeartRateSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WearHeartRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: WearHeartRateRecord): Long

    @Query("SELECT * FROM wear_heart_rate_records WHERE sessionId = :id ORDER BY timestamp ASC")
    fun observeBySession(id: String): Flow<List<WearHeartRateRecord>>

    @Query("SELECT * FROM wear_heart_rate_records WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsynced(): List<WearHeartRateRecord>

    @Query("UPDATE wear_heart_rate_records SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("DELETE FROM wear_heart_rate_records WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WearHeartRateSession)

    @Update
    suspend fun updateSession(session: WearHeartRateSession)

    @Query("SELECT * FROM wear_sessions WHERE sessionId = :id LIMIT 1")
    suspend fun getSession(id: String): WearHeartRateSession?
}
