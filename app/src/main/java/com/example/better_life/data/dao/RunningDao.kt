package com.example.better_life.data.dao

import androidx.room.*
import com.example.better_life.data.entities.RunningRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningDao  {
    @Query("SELECT * FROM running_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<RunningRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RunningRecord)

    @Query("SELECT SUM(calories) FROM running_records WHERE date(timestamp / 1000, 'unixepoch') = date('now')")
    fun getTodayTotalCalories(): Flow<Int?>

    @Query("SELECT SUM(steps) FROM running_records WHERE date(timestamp / 1000, 'unixepoch') = date('now')")
    fun getTodayTotalSteps(): Flow<Int?>

    @Query("SELECT SUM(distance) FROM running_records WHERE date(timestamp / 1000, 'unixepoch') = date('now')")
    fun getTodayTotalDistance(): Flow<Double?>
}
