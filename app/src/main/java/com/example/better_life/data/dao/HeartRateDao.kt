package com.example.better_life.data.dao

import androidx.room.*
import com.example.better_life.data.entities.HeartRateRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HeartRateDao {
    @Query("SELECT * FROM heart_rate_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<HeartRateRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HeartRateRecord)

    @Query("SELECT * FROM heart_rate_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRecord(): Flow<HeartRateRecord?>

    @Query("SELECT AVG(bpm) FROM heart_rate_records")
    fun getAverageBpm(): Flow<Int?>

    @Query("SELECT MIN(bpm) FROM heart_rate_records")
    fun getMinBpm(): Flow<Int?>

    @Query("SELECT MAX(bpm) FROM heart_rate_records")
    fun getMaxBpm(): Flow<Int?>
}
