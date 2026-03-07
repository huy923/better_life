package com.example.better_life.data.dao

import androidx.room.*
import com.example.better_life.data.entities.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: Goal)

    @Update
    suspend fun update(goal: Goal)

    @Delete
    suspend fun delete(goal: Goal)

    @Query("SELECT COUNT(*) FROM goals WHERE isCompleted = 1")
    fun getCompletedGoalsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM goals")
    fun getTotalGoalsCount(): Flow<Int>
}
