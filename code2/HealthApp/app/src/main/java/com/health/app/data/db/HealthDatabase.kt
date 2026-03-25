package com.health.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.health.app.data.dao.DailyGoalDao
import com.health.app.data.dao.UserDao
import com.health.app.data.dao.WorkoutDao
import com.health.app.data.entity.DailyGoal
import com.health.app.data.entity.UserProfile
import com.health.app.data.entity.WorkoutSession

@Database(
    entities = [WorkoutSession::class, UserProfile::class, DailyGoal::class],
    version = 1,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun userDao(): UserDao
    abstract fun goalDao(): DailyGoalDao

    companion object {
        @Volatile private var INSTANCE: HealthDatabase? = null
        fun getInstance(context: Context): HealthDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, HealthDatabase::class.java, "health_db")
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
