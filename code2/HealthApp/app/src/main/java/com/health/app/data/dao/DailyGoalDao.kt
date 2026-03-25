package com.health.app.data.dao

import androidx.room.*
import com.health.app.data.entity.DailyGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: DailyGoal)

    @Update
    suspend fun update(goal: DailyGoal)

    @Query("SELECT * FROM daily_goals WHERE id = 1 LIMIT 1")
    fun getGoal(): Flow<DailyGoal?>
}
