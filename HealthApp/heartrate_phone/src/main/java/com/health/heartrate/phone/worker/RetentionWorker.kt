package com.health.heartrate.phone.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.health.heartrate.phone.data.db.HeartRateDatabase
import com.health.heartrate.phone.repository.HeartRateRepository
import com.health.heartrate.shared.constants.DataLayerConstants
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Runs nightly at 02:00 AM to delete data older than 30 days.
 * Scheduled via WorkManager periodic task.
 */
class RetentionWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo      = HeartRateRepository(HeartRateDatabase.getInstance(applicationContext))
            val cutoffMs  = System.currentTimeMillis() - DataLayerConstants.RETENTION_DAYS * 24 * 3600 * 1000L
            val sdf       = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cutoffDate = sdf.format(Date(cutoffMs))

            val deletedRecords  = repo.purgeOldRecords(cutoffMs)
            val deletedSessions = repo.purgeOldSessions(cutoffMs)
            val deletedStats    = repo.purgeOldStats(cutoffDate)

            Log.i(TAG, "Retention purge: records=$deletedRecords sessions=$deletedSessions stats=$deletedStats")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Retention worker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG       = "RetentionWorker"
        private const val WORK_NAME = "heart_rate_retention"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<RetentionWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(computeInitialDelay(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Delay until next 02:00 AM */
        private fun computeInitialDelay(): Long {
            val now    = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 2)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}
