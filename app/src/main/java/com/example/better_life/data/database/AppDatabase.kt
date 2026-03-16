package com.example.better_life.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.better_life.data.dao.*
import com.example.better_life.data.entities.*

@Database(
    entities = [
        User::class,
        HeartRateRecord::class,
        MealRecord::class,
        RunningRecord::class,
        Goal::class,
        SleepRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun mealDao(): MealDao
    abstract fun runningDao(): RunningDao
    abstract fun goalDao(): GoalDao
    abstract fun sleepDao(): SleepDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "better_life_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
