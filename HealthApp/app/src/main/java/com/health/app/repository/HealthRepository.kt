package com.health.app.repository

import com.health.app.data.db.HealthDatabase
import com.health.app.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class HealthRepository(private val db: HealthDatabase) {
    fun getAllSessions(): Flow<List<WorkoutSession>> = db.workoutDao().getAllSessions()

    fun getWeeklyStats(): Flow<List<DailyStat>> {
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        return db.workoutDao().getWeeklyStats(weekAgo)
    }

    fun getTodayCalories(): Flow<Float?> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return db.workoutDao().getTodayCalories(cal.timeInMillis)
    }

    suspend fun saveSession(session: WorkoutSession): Long = db.workoutDao().insert(session)
    fun getUserProfile(): Flow<UserProfile?> = db.userDao().getProfile()
    suspend fun getUserProfileOnce(): UserProfile? = db.userDao().getProfileOnce()
    suspend fun saveUserProfile(profile: UserProfile) = db.userDao().insert(profile)
    fun getGoal(): Flow<DailyGoal?> = db.goalDao().getGoal()
    suspend fun saveGoal(goal: DailyGoal) = db.goalDao().insert(goal)
}
