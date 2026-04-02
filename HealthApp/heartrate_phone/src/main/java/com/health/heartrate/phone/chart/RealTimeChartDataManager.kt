package com.health.heartrate.phone.chart

import com.health.heartrate.phone.data.entity.HeartRateRecord
import kotlinx.coroutines.flow.*

/**
 * Manages a sliding window of BPM data points for a real-time chart.
 * Consumers collect [chartDataFlow] and feed it directly to MPAndroidChart.
 */
class RealTimeChartDataManager(private val windowSize: Int = 60) {

    private val _window = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartDataFlow: StateFlow<List<ChartPoint>> = _window.asStateFlow()

    private val _statsFlow = MutableStateFlow(ChartStats())
    val statsFlow: StateFlow<ChartStats> = _statsFlow.asStateFlow()

    data class ChartPoint(val x: Float, val y: Float, val timestamp: Long)

    data class ChartStats(
        val current: Int   = 0,
        val avg:     Float = 0f,
        val max:     Int   = 0,
        val min:     Int   = 999,
        val trend:   Trend = Trend.STABLE
    )

    enum class Trend { RISING, FALLING, STABLE }

    fun addRecord(record: HeartRateRecord) {
        val current = _window.value.toMutableList()
        current.add(ChartPoint(
            x         = current.size.toFloat(),
            y         = record.bpm.toFloat(),
            timestamp = record.timestamp
        ))
        if (current.size > windowSize) current.removeAt(0)
        // Re-index X so chart always shows 0..N
        val reindexed = current.mapIndexed { i, p -> p.copy(x = i.toFloat()) }
        _window.value = reindexed
        updateStats(reindexed)
    }

    fun addRecords(records: List<HeartRateRecord>) = records.forEach { addRecord(it) }

    fun reset() { _window.value = emptyList(); _statsFlow.value = ChartStats() }

    private fun updateStats(pts: List<ChartPoint>) {
        if (pts.isEmpty()) return
        val bpms = pts.map { it.y.toInt() }
        val trend = when {
            pts.size < 5 -> Trend.STABLE
            else -> {
                val recent = pts.takeLast(5).map { it.y }
                val earlier = pts.dropLast(5).takeLast(5).map { it.y }
                val recentAvg  = recent.average()
                val earlierAvg = earlier.average()
                when {
                    recentAvg - earlierAvg > 3.0 -> Trend.RISING
                    earlierAvg - recentAvg > 3.0 -> Trend.FALLING
                    else -> Trend.STABLE
                }
            }
        }
        _statsFlow.value = ChartStats(
            current = bpms.last(),
            avg     = bpms.average().toFloat(),
            max     = bpms.max(),
            min     = bpms.min(),
            trend   = trend
        )
    }
}
