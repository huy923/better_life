package com.example.better_life.data.dao

import androidx.room.*
import com.example.better_life.data.entities.SleepDataEntity
import com.example.better_life.data.entities.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepTrackingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSessionEntity): Long

    @Update
    suspend fun updateSession(session: SleepSessionEntity)

    @Query("SELECT * FROM sleep_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SleepSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepDataBatch(dataList: List<SleepDataEntity>)

    @Query("SELECT * FROM sleep_raw_data WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSleepDataForSession(sessionId: Long): List<SleepDataEntity>
}
