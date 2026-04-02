package com.health.heartrate.phone.usecase

import com.health.heartrate.phone.data.entity.DailyHeartRateStat
import com.health.heartrate.phone.data.entity.HeartRateRecord
import com.health.heartrate.phone.data.entity.HeartRateSession
import com.health.heartrate.phone.repository.HeartRateRepository
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class GetStatisticsUseCase(private val repo: HeartRateRepository) {

    fun getDailySessions(source: String? = null): Flow<List<HeartRateSession>> =
        if (source != null) repo.observeSessionsBySource(source)
        else repo.observeAllSessions()

    fun getWeeklyStats(source: String? = null): Flow<List<DailyHeartRateStat>> =
        repo.observeDailyStats(days = 7, source = source)

    fun getMonthlyStats(source: String? = null): Flow<List<DailyHeartRateStat>> =
        repo.observeDailyStats(days = 30, source = source)

    fun getAnomalies(): Flow<List<HeartRateRecord>> = repo.observeAnomalies()

    fun getRecordsInRange(from: Long, to: Long): Flow<List<HeartRateRecord>> =
        repo.observeRecordsInRange(from, to)
}
