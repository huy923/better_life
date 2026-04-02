package com.health.heartrate.phone.data.db

import android.content.Context
import androidx.room.*
import com.health.heartrate.phone.data.dao.*
import com.health.heartrate.phone.data.entity.*

@Database(
    entities = [
        HeartRateRecord::class,
        HeartRateSession::class,
        DailyHeartRateStat::class
    ],
    version = 1,
    exportSchema = true
)
abstract class HeartRateDatabase : RoomDatabase() {

    abstract fun recordDao(): HeartRateRecordDao
    abstract fun sessionDao(): HeartRateSessionDao
    abstract fun dailyStatDao(): DailyStatDao

    companion object {
        @Volatile private var INSTANCE: HeartRateDatabase? = null

        fun getInstance(context: Context): HeartRateDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HeartRateDatabase::class.java,
                    "heart_rate.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
