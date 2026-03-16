package com.example.better_life.data.dao

import androidx.room.*
import com.example.better_life.data.entities.SleepRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_records ORDER BY recordDate DESC LIMIT 1")
    fun getLatestRecord(): Flow<SleepRecord?>

    @Query("SELECT * FROM sleep_records ORDER BY recordDate DESC LIMIT 7")
    fun getWeeklyRecords(): Flow<List<SleepRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SleepRecord)

    @Query("DELETE FROM sleep_records")
    suspend fun deleteAll()
}
