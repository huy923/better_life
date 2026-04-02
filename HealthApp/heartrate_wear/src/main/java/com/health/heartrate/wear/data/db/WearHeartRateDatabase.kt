package com.health.heartrate.wear.data.db

import android.content.Context
import androidx.room.*
import com.health.heartrate.wear.data.dao.WearHeartRateDao
import com.health.heartrate.wear.data.entity.WearHeartRateRecord
import com.health.heartrate.wear.data.entity.WearHeartRateSession

@Database(
    entities = [WearHeartRateRecord::class, WearHeartRateSession::class],
    version = 1,
    exportSchema = false
)
abstract class WearHeartRateDatabase : RoomDatabase() {
    abstract fun heartRateDao(): WearHeartRateDao

    companion object {
        @Volatile private var INSTANCE: WearHeartRateDatabase? = null
        fun getInstance(context: Context): WearHeartRateDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext,
                    WearHeartRateDatabase::class.java, "wear_hr.db")
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
