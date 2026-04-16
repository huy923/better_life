package com.example.better_life.data.dao

import androidx.room.*
import com.example.better_life.data.entities.WeightRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_records WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllWeights(userId: Int = 1): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_records WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWeight(userId: Int = 1): Flow<WeightRecord?>

    @Query("SELECT * FROM weight_records WHERE userId = :userId AND timestamp >= :since ORDER BY timestamp ASC")
    fun getWeightsSince(since: Long, userId: Int = 1): Flow<List<WeightRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weightRecord: WeightRecord)

    @Delete
    suspend fun deleteWeight(weightRecord: WeightRecord)

    @Query("DELETE FROM weight_records WHERE userId = :userId")
    suspend fun deleteAllWeights(userId: Int = 1)
}