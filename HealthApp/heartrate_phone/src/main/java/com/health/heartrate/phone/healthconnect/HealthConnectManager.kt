package com.health.heartrate.phone.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.beatsPerMinute
import com.health.heartrate.phone.data.entity.HeartRateRecord as LocalRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Integrates with Android Health Connect (replaces Google Fit).
 * Reads/writes HeartRateRecord — compatible with Samsung Health, Google Health, Fitbit.
 *
 * Health Connect acts as a unified data store:
 *   Phone sensor → HealthConnectManager.writeRecords() → Health Connect DB
 *   Health Connect DB ← Samsung Health / Google Health sync
 *   HealthConnectManager.readRecords() → Room DB (local cache)
 *
 * Requires Health Connect app installed (pre-installed on Android 14+).
 */
class HealthConnectManager(private val context: Context) {

    private val client: HealthConnectClient? by lazy {
        runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) ==
                HealthConnectClient.SDK_AVAILABLE

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class)
    )

    // ── Check & request permissions ───────────────────────────────────────────

    suspend fun hasPermissions(): Boolean {
        val hc = client ?: return false
        return hc.permissionController.getGrantedPermissions()
            .containsAll(requiredPermissions)
    }

    // ── Write local records to Health Connect ─────────────────────────────────

    suspend fun writeRecords(records: List<LocalRecord>): Result<Unit> {
        val hc = client ?: return Result.failure(IllegalStateException("Health Connect not available"))
        return runCatching {
            val hcRecords = records.map { r ->
                val instant = Instant.ofEpochMilli(r.timestamp)
                HeartRateRecord(
                    startTime       = instant,
                    endTime         = instant.plusSeconds(1),
                    startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(instant),
                    endZoneOffset   = ZoneOffset.systemDefault().rules.getOffset(instant),
                    samples         = listOf(
                        HeartRateRecord.Sample(instant, r.bpm.toLong().beatsPerMinute)
                    )
                )
            }
            hc.insertRecords(hcRecords)
            Log.d(TAG, "Wrote ${hcRecords.size} records to Health Connect")
        }
    }

    // ── Read from Health Connect (last N days) ────────────────────────────────

    fun readRecordsLast30Days(): Flow<List<HealthConnectSample>> = flow {
        val hc = client ?: return@flow
        if (!hasPermissions()) return@flow

        val end   = Instant.now()
        val start = end.minus(30, ChronoUnit.DAYS)

        runCatching {
            val response = hc.readRecords(
                ReadRecordsRequest(
                    recordType     = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val samples = response.records.flatMap { record ->
                record.samples.map { sample ->
                    HealthConnectSample(
                        bpm       = sample.beatsPerMinute.toInt(),
                        timestamp = sample.time.toEpochMilli(),
                        source    = record.metadata.dataOrigin.packageName
                    )
                }
            }
            emit(samples)
        }.onFailure { Log.e(TAG, "Read from Health Connect failed", it) }
    }

    suspend fun readTodayAvgBpm(): Float? {
        val hc = client ?: return null
        if (!hasPermissions()) return null

        val startOfDay = ZonedDateTime.now()
            .truncatedTo(ChronoUnit.DAYS).toInstant()
        val now = Instant.now()

        return runCatching {
            val response = hc.readRecords(
                ReadRecordsRequest(
                    recordType      = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            val allBpm = response.records.flatMap { it.samples }.map { it.beatsPerMinute }
            if (allBpm.isEmpty()) null else allBpm.average().toFloat()
        }.getOrNull()
    }

    // ── Sync: Pull from Health Connect → Room ─────────────────────────────────

    suspend fun syncFromHealthConnect(
        onRecord: suspend (HealthConnectSample) -> Unit
    ) {
        val hc = client ?: return
        if (!hasPermissions()) { Log.w(TAG, "No Health Connect permissions"); return }

        val end   = Instant.now()
        val start = end.minus(30, ChronoUnit.DAYS)

        runCatching {
            val response = hc.readRecords(
                ReadRecordsRequest(
                    recordType      = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    pageSize        = 5000
                )
            )
            response.records.flatMap { it.samples }.forEach { sample ->
                onRecord(HealthConnectSample(
                    bpm       = sample.beatsPerMinute.toInt(),
                    timestamp = sample.time.toEpochMilli(),
                    source    = "health_connect"
                ))
            }
            Log.d(TAG, "Synced ${response.records.size} HC records")
        }.onFailure { Log.e(TAG, "Sync from HC failed", it) }
    }

    companion object { private const val TAG = "HealthConnect" }
}

data class HealthConnectSample(
    val bpm: Int,
    val timestamp: Long,
    val source: String
)
