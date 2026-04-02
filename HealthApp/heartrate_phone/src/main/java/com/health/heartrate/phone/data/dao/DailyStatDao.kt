package com.health.heartrate.phone.data.dao

import androidx.room.*
import com.health.heartrate.phone.data.entity.DailyHeartRateStat
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DailyHeartRateStat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stats: List<DailyHeartRateStat>)

    @Query("SELECT * FROM daily_heart_rate_stats ORDER BY date DESC")
    fun observeAll(): Flow<List<DailyHeartRateStat>>

    @Query("SELECT * FROM daily_heart_rate_stats ORDER BY date DESC LIMIT :days")
    fun observeLastNDays(days: Int): Flow<List<DailyHeartRateStat>>

    @Query("SELECT * FROM daily_heart_rate_stats WHERE source = :source ORDER BY date DESC LIMIT :days")
    fun observeLastNDaysBySource(source: String, days: Int): Flow<List<DailyHeartRateStat>>

    @Query("SELECT * FROM daily_heart_rate_stats WHERE date = :date")
    suspend fun getByDate(date: String): DailyHeartRateStat?

    @Query("DELETE FROM daily_heart_rate_stats WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String): Int
}
