package com.example.better_life.ui.fragments

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.core.content.res.ResourcesCompat
import com.example.better_life.R
import com.example.better_life.data.entities.WeightRecord
import com.example.better_life.databinding.DialogAddWeightBinding
import com.example.better_life.databinding.DialogUpdateGoalWeightBinding
import com.example.better_life.databinding.FragmentWeightDetailBinding
import com.example.better_life.databinding.ItemBmiRangeBinding
import com.example.better_life.databinding.ItemWeightStatMiniBinding
import com.example.better_life.ui.viewmodels.WeightDetailViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WeightDetailFragment : Fragment() {

    private var _binding: FragmentWeightDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WeightDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeightDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAddWeight.setOnClickListener {
            showAddWeightDialog()
        }

        binding.btnEditGoal.setOnClickListener {
            showUpdateGoalDialog()
        }

        setupChart()
        setupBMILegend()
    }

    private fun showAddWeightDialog() {
        val dialogBinding = DialogAddWeightBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Prefill with latest weight if available
        val latest = viewModel.latestWeight.value
        dialogBinding.etWeight.setText(latest?.weight?.toString() ?: "")

        dialogBinding.btnSave.setOnClickListener {
            val weightStr = dialogBinding.etWeight.text.toString()
            if (weightStr.isNotEmpty()) {
                val newWeight = weightStr.toDouble()
                viewModel.addWeight(newWeight)
                dialog.dismiss()
            }
        }

        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showUpdateGoalDialog() {
        val dialogBinding = DialogUpdateGoalWeightBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val currentUser = viewModel.user.value
        dialogBinding.etGoalWeight.setText(currentUser?.targetWeight?.toString() ?: "")

        dialogBinding.btnUpdate.setOnClickListener {
            val weightStr = dialogBinding.etGoalWeight.text.toString()
            if (weightStr.isNotEmpty()) {
                val newTarget = weightStr.toDouble()
                viewModel.updateTargetWeight(newTarget)
                dialog.dismiss()
            }
        }

        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun setupBMILegend() {
        // Underweight
        ItemBmiRangeBinding.bind(binding.rangeUnder.root).apply {
            dot.setBackgroundResource(R.drawable.bg_indicator_dot)
            dot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4FC3F7"))
            label.text = "Thiếu cân"
            range.text = "< 18.5"
        }
        // Normal
        ItemBmiRangeBinding.bind(binding.rangeNormal.root).apply {
            dot.setBackgroundResource(R.drawable.bg_indicator_dot)
            dot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2DD1AC"))
            label.text = "Bình thường"
            range.text = "18.5-24.9"
        }
        // Overweight
        ItemBmiRangeBinding.bind(binding.rangeOver.root).apply {
            dot.setBackgroundResource(R.drawable.bg_indicator_dot)
            dot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFB74D"))
            label.text = "Thừa cân"
            range.text = "25-29.9"
        }
        // Obese
        ItemBmiRangeBinding.bind(binding.rangeObese.root).apply {
            dot.setBackgroundResource(R.drawable.bg_indicator_dot)
            dot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#EF5350"))
            label.text = "Béo phì"
            range.text = "≥ 30"
        }
    }

    private fun setupChart() {
        binding.weightChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#999999")
                textSize = 10f
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    private val mFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        return mFormat.format(Date(value.toLong()))
                    }
                }
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F2F5")
                textColor = Color.parseColor("#999999")
                textSize = 10f
                setDrawAxisLine(false)
            }

            axisRight.isEnabled = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.weightHistory.collectLatest { history ->
                updateStats(history)
                updateChartData(history)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.latestWeight.collectLatest { record ->
                record?.let {
                    binding.tvCurrentWeight.text = String.format(Locale.getDefault(), "%.1f", it.weight)
                    calculateAndDisplayBMI(it.weight)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.user.collectLatest { user ->
                user?.let {
                    binding.tvGoalWeight.text = String.format(Locale.getDefault(), "%.0f", it.targetWeight ?: 65.0)
                    binding.tvHeightInfo.text = "Chiều cao: ${it.height.toInt()} cm"
                    updateGoalProgress(it.weight, it.targetWeight ?: 65.0)
                }
            }
        }
    }

    private fun updateStats(history: List<WeightRecord>) {
        if (history.isEmpty()) return

        binding.tvHistoryCount.text = history.size.toString()
        
        val avg = history.map { it.weight }.average()
        val min = history.minByOrNull { it.weight }?.weight ?: 0.0
        val max = history.maxByOrNull { it.weight }?.weight ?: 0.0

        val avgBinding = ItemWeightStatMiniBinding.bind(binding.statAvg.root)
        avgBinding.tvMiniLabel.text = "TB tuần"
        avgBinding.tvMiniValue.text = String.format(Locale.getDefault(), "%.1f", avg)

        val minBinding = ItemWeightStatMiniBinding.bind(binding.statMin.root)
        minBinding.tvMiniLabel.text = "Thấp nhất"
        minBinding.tvMiniValue.text = String.format(Locale.getDefault(), "%.1f", min)

        val maxBinding = ItemWeightStatMiniBinding.bind(binding.statMax.root)
        maxBinding.tvMiniLabel.text = "Cao nhất"
        maxBinding.tvMiniValue.text = String.format(Locale.getDefault(), "%.1f", max)

        if (history.size >= 2) {
            val latest = history.last().weight
            val previous = history[history.size - 2].weight
            val diff = latest - previous
            val diffSign = if (diff >= 0) "↗ +" else "↘ "
            binding.tvDiffLast.text = String.format(Locale.getDefault(), "%s%.1f kg lần trước", diffSign, diff)
        }

        val initial = history.first().weight
        val latest = history.last().weight
        val totalDiff = latest - initial
        val totalDiffSign = if (totalDiff >= 0) "↗ +" else "↘ "
        binding.tvDiffTotal.text = String.format(Locale.getDefault(), "%s%.1f kg tổng", totalDiffSign, totalDiff)
    }

    private fun updateGoalProgress(current: Double, target: Double) {
        // Animate Weight Text
        val startValue = binding.tvGoalWeight.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
        ValueAnimator.ofFloat(startValue.toFloat(), target.toFloat()).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                binding.tvGoalWeight.text = String.format(Locale.getDefault(), "%.1f", it.animatedValue as Float)
            }
            start()
        }

        val remaining = current - target
        binding.tvGoalRemaining.text = if (remaining > 0) {
            String.format(Locale.getDefault(), "Còn %.1f kg", remaining)
        } else {
            "Đã đạt mục tiêu!"
        }
        
        // Progress bar logic
        val startWeight = target + 10.0 // Mock start weight
        val targetProgress = (((startWeight - current) / (startWeight - target)) * 100).toInt().coerceIn(0, 100)
        
        ValueAnimator.ofInt(binding.progressGoal.progress, targetProgress).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { 
                binding.progressGoal.progress = it.animatedValue as Int 
            }
            start()
        }
    }

    private fun calculateAndDisplayBMI(weight: Double) {
        val user = viewModel.user.value ?: return
        if (user.height <= 0) return

        val heightInMeters = user.height / 100.0
        val bmi = weight / (heightInMeters * heightInMeters)
        
        binding.tvBmiMiniValue.text = String.format(Locale.getDefault(), "%.1f", bmi)
        binding.tvBmiIndicatorVal.text = String.format(Locale.getDefault(), "%.1f", bmi)
        
        val (status, color) = when {
            bmi < 18.5 -> "Thiếu cân" to "#4FC3F7"
            bmi < 25 -> "Bình thường" to "#2DD1AC"
            bmi < 30 -> "Thừa cân" to "#FFB74D"
            else -> "Béo phì" to "#EF5350"
        }
        
        binding.tvBmiMiniStatus.text = status
        binding.tvBmiMiniStatus.setTextColor(Color.parseColor(color))
        
        // Update BMI Indicator Position
        binding.root.post {
            val scaleWidth = binding.bmiScaleBg.width
            if (scaleWidth > 0) {
                // BMI scale from 15 to 35
                val position = ((bmi - 15) / (35 - 15)).coerceIn(0.0, 1.0)
                val indicatorPos = (position * scaleWidth).toInt()
                
                val targetX = (indicatorPos - binding.ivBmiIndicator.width / 2).toFloat()
                val currentX = binding.ivBmiIndicator.translationX
                
                ValueAnimator.ofFloat(currentX, targetX).apply {
                    duration = 1000
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener {
                        val v = it.animatedValue as Float
                        binding.ivBmiIndicator.translationX = v
                        binding.tvBmiIndicatorVal.translationX = v
                    }
                    start()
                }
            }
        }
    }

    private fun updateChartData(history: List<WeightRecord>) {
        if (history.isEmpty()) return

        val entries = history.map { 
            Entry(it.timestamp.toFloat(), it.weight.toFloat())
        }

        val dataSet = LineDataSet(entries, "Weight").apply {
            color = Color.parseColor("#2DD1AC")
            setCircleColor(Color.parseColor("#2DD1AC"))
            lineWidth = 3f
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleColor = Color.WHITE
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable = ResourcesCompat.getDrawable(resources, R.drawable.bg_weight_chart_gradient, null)
        }

        binding.weightChart.data = LineData(dataSet)
        binding.weightChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}