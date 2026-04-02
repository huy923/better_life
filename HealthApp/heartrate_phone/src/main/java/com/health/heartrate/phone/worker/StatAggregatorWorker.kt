package com.health.heartrate.phone.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.health.heartrate.phone.data.db.HeartRateDatabase
import com.health.heartrate.phone.data.entity.DailyHeartRateStat
import com.health.heartrate.phone.repository.HeartRateRepository
import com.health.heartrate.shared.constants.DataLayerConstants
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Aggregates raw records into DailyHeartRateStat every midnight.
 * Runs after RetentionWorker so stats reflect clean data.
 */
class StatAggregatorWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = HeartRateRepository(HeartRateDatabase.getInstance(applicationContext))
            val sdf  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            for (source in listOf(DataLayerConstants.SOURCE_PHONE, DataLayerConstants.SOURCE_WATCH)) {
                val sessions = repo.observeSessionsBySource(source).first()
                val grouped  = sessions.groupBy { sdf.format(Date(it.startTime)) }

                grouped.forEach { (date, daySessions) ->
                    val completedSessions = daySessions.filter { it.isCompleted && it.sampleCount > 0 }
                    if (completedSessions.isEmpty()) return@forEach

                    val allAvg   = completedSessions.map { it.avgBpm }
                    val avgBpm   = allAvg.average().toFloat()
                    val maxBpm   = completedSessions.maxOf { it.maxBpm }
                    val minBpm   = completedSessions.minOf { it.minBpm }
                    val totalCount = completedSessions.sumOf { it.sampleCount }

                    repo.upsertDailyStat(DailyHeartRateStat(
                        date         = date,
                        source       = source,
                        avgBpm       = avgBpm,
                        maxBpm       = maxBpm,
                        minBpm       = minBpm,
                        restingBpm   = avgBpm,   // simplified; could filter by low-activity sessions
                        sampleCount  = totalCount,
                        sessionCount = completedSessions.size
                    ))
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Stat aggregation failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG       = "StatAggregator"
        private const val WORK_NAME = "heart_rate_stat_aggregator"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StatAggregatorWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(RetentionWorker.run { 0L }, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
