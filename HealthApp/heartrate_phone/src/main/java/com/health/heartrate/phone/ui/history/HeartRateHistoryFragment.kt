package com.health.heartrate.phone.ui.history

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.health.heartrate.phone.R
import com.health.heartrate.phone.manager.HeartRateViewModel
import com.health.heartrate.shared.constants.DataLayerConstants
import java.text.SimpleDateFormat
import java.util.*

/**
 * History & statistics screen:
 *  - Tab: 7-day bar chart (avg BPM per day)
 *  - Tab: 30-day trend line
 *  - Source filter: Phone / Watch / All
 *  - Session list with summary cards
 */
class HeartRateHistoryFragment : Fragment(R.layout.fragment_heart_rate_history) {

    private val vm: HeartRateViewModel by viewModels()
    private lateinit var barChart:     BarChart
    private lateinit var lineChart:    LineChart
    private lateinit var tvNoData:     TextView
    private lateinit var rgPeriod:     RadioGroup
    private lateinit var spSource:     Spinner
    private lateinit var lvSessions:   ListView
    private lateinit var tvAnomalies:  TextView

    private var selectedSource = "all"
    private val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindViews(view)
        setupSourceSpinner()
        setupCharts()
        observeViewModel()
    }

    private fun bindViews(v: View) {
        barChart    = v.findViewById(R.id.weeklyBarChart)
        lineChart   = v.findViewById(R.id.monthlyLineChart)
        tvNoData    = v.findViewById(R.id.tvNoData)
        rgPeriod    = v.findViewById(R.id.rgPeriod)
        spSource    = v.findViewById(R.id.spSourceFilter)
        lvSessions  = v.findViewById(R.id.lvSessions)
        tvAnomalies = v.findViewById(R.id.tvAnomalies)

        rgPeriod.setOnCheckedChangeListener { _, id ->
            barChart.visibility  = if (id == R.id.rbWeek)  View.VISIBLE else View.GONE
            lineChart.visibility = if (id == R.id.rbMonth) View.VISIBLE else View.GONE
        }
    }

    private fun setupSourceSpinner() {
        val options = listOf("Tất cả", "Điện thoại", "Đồng hồ")
        spSource.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, options).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spSource.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedSource = when (pos) {
                    1 -> DataLayerConstants.SOURCE_PHONE
                    2 -> DataLayerConstants.SOURCE_WATCH
                    else -> "all"
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupCharts() {
        barChart.apply {
            description.isEnabled = false
            legend.isEnabled      = false
            setDrawGridBackground(false)
            axisRight.isEnabled   = false
            axisLeft.apply {
                axisMinimum = 40f; axisMaximum = 200f
                textColor   = 0xFF888888.toInt()
                setDrawGridLines(true)
                gridColor   = 0x22888888.toInt()
            }
        }
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled      = false
            setDrawGridBackground(false)
            axisRight.isEnabled   = false
            axisLeft.apply {
                axisMinimum = 40f; axisMaximum = 200f
                textColor   = 0xFF888888.toInt()
            }
        }
    }

    private fun observeViewModel() {
        // 7-day bar chart
        vm.weeklyPhoneStats.observe(viewLifecycleOwner) { stats ->
            if (stats.isEmpty()) { tvNoData.visibility = View.VISIBLE; return@observe }
            tvNoData.visibility = View.GONE
            val entries = stats.mapIndexed { i, s -> BarEntry(i.toFloat(), s.avgBpm) }
            val labels  = stats.map { it.date.substring(5) }  // MM-dd
            val dataSet = BarDataSet(entries, "Avg BPM").apply {
                color = 0xFF378ADD.toInt()
                setDrawValues(true)
                valueTextColor = 0xFF888888.toInt()
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(v: Float) = v.toInt().toString()
                }
            }
            barChart.data = BarData(dataSet).apply { barWidth = 0.6f }
            barChart.xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                textColor      = 0xFF888888.toInt()
                granularity    = 1f
            }
            barChart.invalidate()
        }

        // 30-day line chart
        vm.monthlyStats.observe(viewLifecycleOwner) { stats ->
            val entries = stats.mapIndexed { i, s -> Entry(i.toFloat(), s.avgBpm) }
            val dataSet = LineDataSet(entries.toMutableList(), "Avg BPM/ngày").apply {
                color         = 0xFF1D9E75.toInt()
                lineWidth     = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode          = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor     = 0xFF1D9E75.toInt()
                fillAlpha     = 25
            }
            lineChart.data = LineData(dataSet)
            lineChart.invalidate()
        }

        // Session list
        vm.allSessions.observe(viewLifecycleOwner) { sessions ->
            val items = sessions.map { s ->
                val date = sdf.format(Date(s.startTime))
                val src  = if (s.source == DataLayerConstants.SOURCE_PHONE) "📱" else "⌚"
                "$src $date  Avg: ${"%.0f".format(s.avgBpm)} bpm  Max: ${s.maxBpm}  ${s.sampleCount} mẫu"
            }
            lvSessions.adapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_list_item_1, items)
        }

        // Anomalies badge
        vm.anomalies.observe(viewLifecycleOwner) { anomalies ->
            tvAnomalies.text = if (anomalies.isEmpty()) "Không có bất thường"
            else "⚠ ${anomalies.size} nhịp tim bất thường được phát hiện"
            tvAnomalies.setTextColor(
                if (anomalies.isEmpty()) 0xFF1D9E75.toInt() else 0xFFE24B4A.toInt()
            )
        }
    }
}
