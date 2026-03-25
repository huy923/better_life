package com.health.app.data.dao

import androidx.room.*
import com.health.app.data.entity.DailyStat
import com.health.app.data.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("""
        SELECT date(date/1000, 'unixepoch') as day,
               SUM(distanceMeters) as totalDistance,
               SUM(caloriesBurned) as totalCalories,
               SUM(steps) as totalSteps
        FROM workout_sessions
        WHERE date >= :fromTimestamp
        GROUP BY day ORDER BY day DESC
    """)
    fun getWeeklyStats(fromTimestamp: Long): Flow<List<DailyStat>>

    @Query("SELECT SUM(caloriesBurned) FROM workout_sessions WHERE date >= :todayStart")
    fun getTodayCalories(todayStart: Long): Flow<Float?>

    @Delete
    suspend fun delete(session: WorkoutSession)
}
