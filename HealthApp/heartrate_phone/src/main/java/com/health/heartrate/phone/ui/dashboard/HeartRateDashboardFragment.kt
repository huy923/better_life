package com.health.heartrate.phone.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.health.heartrate.phone.R
import com.health.heartrate.phone.chart.RealTimeChartDataManager
import com.health.heartrate.phone.manager.HeartRateViewModel
import com.health.heartrate.shared.constants.DataLayerConstants

/**
 * Main dashboard:
 * - Live BPM large display + color-coded ring
 * - Real-time MPAndroidChart LineChart (60-point rolling window)
 * - Session stats: avg / max / min / trend
 * - Source selector: Phone Camera vs Watch
 * - Start / Stop control
 */
class HeartRateDashboardFragment : Fragment(R.layout.fragment_heart_rate_dashboard) {

    private val vm: HeartRateViewModel by viewModels()

    private lateinit var chart:       LineChart
    private lateinit var tvBpm:       TextView
    private lateinit var tvAvg:       TextView
    private lateinit var tvMax:       TextView
    private lateinit var tvMin:       TextView
    private lateinit var tvTrend:     TextView
    private lateinit var tvStatus:    TextView
    private lateinit var btnStart:    Button
    private lateinit var btnStop:     Button
    private lateinit var rgSource:    RadioGroup
    private lateinit var pbRing:      ProgressBar

    private val chartEntries = mutableListOf<Entry>()
    private var dataSet: LineDataSet? = null

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.startPhoneMonitoring()
        else toast("Cần quyền Camera để đo nhịp tim")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindViews(view)
        setupChart()
        observeViewModel()
    }

    private fun bindViews(v: View) {
        chart    = v.findViewById(R.id.heartRateChart)
        tvBpm    = v.findViewById(R.id.tvCurrentBpm)
        tvAvg    = v.findViewById(R.id.tvAvgBpm)
        tvMax    = v.findViewById(R.id.tvMaxBpm)
        tvMin    = v.findViewById(R.id.tvMinBpm)
        tvTrend  = v.findViewById(R.id.tvTrend)
        tvStatus = v.findViewById(R.id.tvStatus)
        btnStart = v.findViewById(R.id.btnStartHr)
        btnStop  = v.findViewById(R.id.btnStopHr)
        rgSource = v.findViewById(R.id.rgSource)
        pbRing   = v.findViewById(R.id.progressRing)

        btnStart.setOnClickListener { onStartClicked() }
        btnStop.setOnClickListener  { vm.stopPhoneMonitoring() }
    }

    private fun setupChart() {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled      = false
            setTouchEnabled(false)
            setDrawGridBackground(false)
            xAxis.isEnabled       = false
            axisLeft.apply {
                axisMinimum = 40f; axisMaximum = 200f
                setDrawGridLines(true)
                gridColor   = 0x22888888.toInt()
                textColor   = 0xFF888888.toInt()
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(v: Float) = v.toInt().toString()
                }
            }
            axisRight.isEnabled   = false
            setViewPortOffsets(8f, 8f, 8f, 8f)
        }
        dataSet = LineDataSet(chartEntries, "BPM").apply {
            color         = 0xFFE24B4A.toInt()
            lineWidth     = 2f
            setDrawCircles(false)
            setDrawValues(false)
            mode          = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawFilled(true)
            fillColor     = 0xFFE24B4A.toInt()
            fillAlpha     = 30
        }
        chart.data = LineData(dataSet)
    }

    private fun observeViewModel() {
        vm.chartData.observe(viewLifecycleOwner) { points ->
            chartEntries.clear()
            points.forEach { chartEntries.add(Entry(it.x, it.y)) }
            dataSet?.notifyDataSetChanged()
            chart.data?.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        }

        vm.chartStats.observe(viewLifecycleOwner) { s ->
            if (s.current > 0) {
                tvBpm.text = "${s.current}"
                tvBpm.setTextColor(bpmColor(s.current))
                pbRing.progress = s.current.coerceIn(40, 200)
            }
            tvAvg.text   = "Avg: ${"%.0f".format(s.avg)} bpm"
            tvMax.text   = "Max: ${s.max}"
            tvMin.text   = "Min: ${if (s.min == 999) "--" else s.min}"
            tvTrend.text = when (s.trend) {
                RealTimeChartDataManager.Trend.RISING  -> "▲ Tăng"
                RealTimeChartDataManager.Trend.FALLING -> "▼ Giảm"
                else                                   -> "→ Ổn định"
            }
        }

        vm.isPhoneMonitoring.observe(viewLifecycleOwner) { on ->
            btnStart.isEnabled    = !on
            btnStop.isEnabled     = on
            pbRing.visibility     = if (on) View.VISIBLE else View.INVISIBLE
            tvStatus.text         = if (on) "Đang đo nhịp tim..." else "Sẵn sàng"
        }

        vm.alerts.observe(viewLifecycleOwner) { alert ->
            alert ?: return@observe
            toast("⚠ ${alert.bpm} BPM — ${alert.type.name.replace('_', ' ')}")
        }
    }

    private fun onStartClicked() {
        val isPhone = rgSource.checkedRadioButtonId == R.id.rbPhone
        if (isPhone) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                vm.startPhoneMonitoring()
            } else {
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        } else {
            vm.startPhoneMonitoring()
        }
    }

    private fun bpmColor(bpm: Int) = when {
        bpm > DataLayerConstants.ALERT_HIGH_BPM -> 0xFFE24B4A.toInt()
        bpm < DataLayerConstants.ALERT_LOW_BPM  -> 0xFF378ADD.toInt()
        else                                     -> 0xFF1D9E75.toInt()
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
