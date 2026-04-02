package com.health.heartrate.phone.healthconnect

import android.content.Context
import android.util.Log
import androidx.work.*
import com.health.heartrate.phone.data.db.HeartRateDatabase
import com.health.heartrate.phone.data.entity.HeartRateRecord
import com.health.heartrate.phone.repository.HeartRateRepository
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Periodic worker: pulls fresh data from Health Connect every 6 hours.
 * Deduplicates by timestamp before inserting into local Room DB.
 */
class HealthConnectSyncWorker(ctx: Context, params: WorkerParameters)
    : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val hcManager = HealthConnectManager(applicationContext)
            if (!hcManager.isAvailable || !hcManager.hasPermissions()) {
                Log.i(TAG, "Health Connect not available or no permissions — skipping")
                return Result.success()
            }

            val db   = HeartRateDatabase.getInstance(applicationContext)
            val repo = HeartRateRepository(db)

            var synced = 0
            val sessionId = "hc_sync_${UUID.randomUUID()}"

            hcManager.syncFromHealthConnect { sample ->
                repo.saveSession(
                    com.health.heartrate.phone.data.entity.HeartRateSession(
                        sessionId   = sessionId,
                        source      = sample.source,
                        startTime   = sample.timestamp,
                        endTime     = sample.timestamp,
                        isCompleted = true
                    )
                )
                db.recordDao().insert(
                    HeartRateRecord(
                        sessionId  = sessionId,
                        bpm        = sample.bpm,
                        timestamp  = sample.timestamp,
                        accuracy   = 3,
                        source     = sample.source,
                        isAnomaly  = sample.bpm > 120 || sample.bpm < 45
                    )
                )
                synced++
            }
            Log.i(TAG, "HC sync complete: $synced records")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "HC sync failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG       = "HCSyncWorker"
        private const val WORK_NAME = "health_connect_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<HealthConnectSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()
                ).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
