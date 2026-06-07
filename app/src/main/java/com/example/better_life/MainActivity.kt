package com.example.better_life

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.transition.Fade
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.better_life.data.DemoData
import com.example.better_life.data.UserManager
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.data.entities.MealRecord
import com.example.better_life.data.entities.User
import com.example.better_life.sensor.StepCounterManager
import com.example.better_life.services.TrackingService
import com.example.better_life.ui.HeartRateManager
import com.example.better_life.ui.RunningManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout
    private lateinit var bottomNav: BottomNavigationView
    private var currentLayoutId: Int = R.layout.layout_home
    private lateinit var database: AppDatabase
    private lateinit var userManager: UserManager
    
    private lateinit var stepCounterManager: StepCounterManager
    private lateinit var heartRateManager: HeartRateManager
    private lateinit var runningManager: RunningManager

    private var bindingJob: kotlinx.coroutines.Job? = null

    private val trackingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TrackingService.ACTION_UPDATE) {
                runningManager.currentDistance = intent.getFloatExtra(TrackingService.EXTRA_DISTANCE, 0f)
                runningManager.currentDuration = intent.getLongExtra(TrackingService.EXTRA_DURATION, 0L)
                if (currentLayoutId == R.layout.layout_running_detail) {
                    runningManager.updateUI()
                }
            }
        }
    }

    private var photoFile: File? = null
    
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera()
        else Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photoFile?.let { processMealImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        database = AppDatabase.getDatabase(this)
        userManager = UserManager(this)
        stepCounterManager = StepCounterManager(this)
        heartRateManager = HeartRateManager(this, database)
        runningManager = RunningManager(this, database, lifecycleScope, stepCounterManager)
        
        savedInstanceState?.let {
            currentLayoutId = it.getInt("CURRENT_LAYOUT_ID", R.layout.layout_home)
        }

        lifecycleScope.launch {
            val user = userManager.getUser()
            if (user.name == "Nguyễn Văn A" && user.weight == 65.5) {
                DemoData.insertSampleData(database, userManager)
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        setContentView(R.layout.activity_main)
        container = findViewById(R.id.container)
        bottomNav = findViewById(R.id.bottom_navigation)

        showLayout(currentLayoutId, animate = false)
        syncBottomNav(currentLayoutId)

        bottomNav.setOnItemSelectedListener { item ->
            val layoutId = when (item.itemId) {
                R.id.nav_home -> R.layout.layout_home
                R.id.nav_health -> R.layout.fragment_weight_detail
                R.id.nav_chatbox -> R.layout.fragment_chat_ai
                R.id.nav_goals -> R.layout.fragment_goals
                R.id.nav_settings -> R.layout.fragment_settings
                else -> return@setOnItemSelectedListener false
            }
            showLayout(layoutId)
            true
        }

        checkAndRequestStepPermissions()
    }

    private fun checkAndRequestStepPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 101)
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        stepCounterManager.start()
        val filter = IntentFilter(TrackingService.ACTION_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(trackingReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(trackingReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        stepCounterManager.stop()
        unregisterReceiver(trackingReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("CURRENT_LAYOUT_ID", currentLayoutId)
    }

    private fun showLayout(layoutId: Int, animate: Boolean = true) {
        currentLayoutId = layoutId
        
        bindingJob?.cancel()
        
        if (animate) {
            val fade = Fade()
            fade.duration = 300
            TransitionManager.beginDelayedTransition(container, fade)
        }
        
        container.removeAllViews()
        val view = LayoutInflater.from(this).inflate(layoutId, container, false)
        container.addView(view)

        Animation.animateLayoutEntry(view)

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            Animation.applyClick(it) {
                showLayout(R.layout.layout_home)
                syncBottomNav(R.layout.layout_home)
            }
        }

        bindingJob = lifecycleScope.launch {
            bindDataToView(layoutId, view)
        }
        
        if (layoutId == R.layout.fragment_settings) setupSettingsPage(view)
    }

    private fun setupSettingsPage(view: View) {
        val switchDarkMode = view.findViewById<SwitchCompat>(R.id.switch_dark_mode)
        val tvDarkModeTitle = view.findViewById<TextView>(R.id.tv_dark_mode_title)
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        switchDarkMode.isChecked = isNightMode
        tvDarkModeTitle.text = getString(R.string.dark_mode)

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }

        val layoutLanguage = view.findViewById<View>(R.id.layout_language)
        val tvCurrentLanguage = view.findViewById<TextView>(R.id.tv_current_language)
        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "vi"
        tvCurrentLanguage.text = if (currentLocale == "en") getString(R.string.lang_en) else getString(R.string.lang_vi)
        layoutLanguage.setOnClickListener {
            Animation.applyClick(it) { showLanguageDialog() }
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.lang_vi), getString(R.string.lang_en))
        AlertDialog.Builder(this).setTitle(R.string.language).setItems(languages) { _, which ->
            val localeTag = if (which == 0) "vi" else "en"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
        }.show()
    }

    @SuppressLint("StringFormatMatches", "StringFormatInvalid")
    private suspend fun bindDataToView(layoutId: Int, view: View) {
        coroutineScope {
            when (layoutId) {
                R.layout.layout_home -> {
                    launch { userManager.userFlow.collectLatest { it?.let { view.findViewById<TextView>(R.id.tv_username)?.text = it.name } } }
                    
                    launch {
                        combine(stepCounterManager.steps, userManager.userFlow) { steps, user ->
                            Pair(steps, user.targetSteps)
                        }.collectLatest { (steps, target) ->
                            view.findViewById<TextView>(R.id.tv_home_steps)?.text = steps.toString()
                            val pb = view.findViewById<ProgressBar>(R.id.pb_home_steps)
                            pb?.max = target
                            pb?.progress = steps
                            view.findViewById<TextView>(R.id.tv_steps_goal)?.text = getString(R.string.steps_goal_format, target)
                        }
                    }

                    launch { database.heartRateDao().getLatestRecord().collectLatest { it?.let { 
                        val cardHr = view.findViewById<View>(R.id.card_heart_rate)
                        cardHr?.findViewById<TextView>(R.id.tv_value)?.text = it.bpm.toString()
                        cardHr?.setOnClickListener { v -> Animation.applyClick(v) { showLayout(R.layout.layout_heart_rate_detail) } }
                    } } }
                    
                    launch { 
                        combine(
                            database.mealDao().getTodayTotalCalories(),
                            userManager.userFlow
                        ) { total: Int?, user: User ->
                            Pair(total ?: 0, user.targetCalories)
                        }.collectLatest { (total, target) ->
                            val cardCal = view.findViewById<View>(R.id.card_calories)
                            cardCal?.findViewById<TextView>(R.id.tv_value)?.text = total.toString()
                            val pb = cardCal?.findViewById<ProgressBar>(R.id.pb_calories)
                            pb?.max = target
                            pb?.progress = total
                            cardCal?.setOnClickListener { v -> Animation.applyClick(v) { showLayout(R.layout.layout_calories_detail) } }
                        }
                    }
                    
                    launch { database.sleepDao().getLatestRecord().collectLatest { it?.let { 
                        val cardSleep = view.findViewById<View>(R.id.card_sleep)
                        val totalMinutes = (it.endTime - it.startTime) / 60000
                        cardSleep?.findViewById<TextView>(R.id.tv_value)?.text = String.format(Locale.getDefault(), "%.1f", totalMinutes / 60.0)
                        cardSleep?.findViewById<ProgressBar>(R.id.pb_progress)?.progress = ((totalMinutes / 60.0) / 8.0 * 100).toInt().coerceAtMost(100)
                        cardSleep?.setOnClickListener { v -> Animation.applyClick(v) { showLayout(R.layout.layout_sleep_detail) } }
                    } } }

                    launch { database.weightDao().getLatestWeight().collectLatest { record ->
                            val cardWeight = view.findViewById<View>(R.id.card_weight)
                            cardWeight?.findViewById<TextView>(R.id.tv_value)?.text = String.format(Locale.getDefault(), "%.1f", record?.weight ?: 0.0)
                            cardWeight?.setOnClickListener { v -> Animation.applyClick(v) { showLayout(R.layout.fragment_weight_detail) } }
                        }
                    }

                    view.findViewById<View>(R.id.action_meal)?.setOnClickListener { v -> Animation.applyClick(v) { showLayout(R.layout.layout_calories_detail) } }
                    view.findViewById<View>(R.id.action_run)?.setOnClickListener { v -> Animation.applyClick(v) { showLayout(R.layout.layout_running_detail) } }
                }

                R.layout.fragment_weight_detail -> {
                    setupWeightDetailUI(view)
                }

                R.layout.layout_heart_rate_detail -> heartRateManager.setupHeartRateUI(view, this)

                R.layout.layout_running_detail -> runningManager.setupRunningUI(view, this)

                R.layout.layout_sleep_detail -> {
                    launch {
                        database.sleepDao().getLatestRecord().collectLatest { record ->
                            record?.let {
                                val totalMinutes = (it.endTime - it.startTime) / 60000
                                val hours = totalMinutes / 60
                                val minutes = totalMinutes % 60
                                view.findViewById<TextView>(R.id.tv_sleep_duration)?.text = String.format(Locale.getDefault(), "%dh %02dp", hours, minutes)
                                view.findViewById<TextView>(R.id.tv_sleep_score)?.text = it.score.toString()
                                
                                val sdf = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                                view.findViewById<TextView>(R.id.tv_sleep_start)?.text = sdf.format(java.util.Date(it.startTime))
                                view.findViewById<TextView>(R.id.tv_sleep_end)?.text = sdf.format(java.util.Date(it.endTime))
                                
                                view.findViewById<TextView>(R.id.tv_deep_sleep_duration_summary)?.text = String.format(Locale.getDefault(), "%dh %02dp", it.deepSleepMinutes / 60, it.deepSleepMinutes % 60)
                                view.findViewById<TextView>(R.id.tv_deep_sleep_duration)?.text = String.format(Locale.getDefault(), "%dh %02dp", it.deepSleepMinutes / 60, it.deepSleepMinutes % 60)
                                view.findViewById<TextView>(R.id.tv_light_sleep_duration)?.text = String.format(Locale.getDefault(), "%dh %02dp", it.lightSleepMinutes / 60, it.lightSleepMinutes % 60)
                                view.findViewById<TextView>(R.id.tv_rem_duration)?.text = String.format(Locale.getDefault(), "%dh %02dp", it.remSleepMinutes / 60, it.remSleepMinutes % 60)
                                view.findViewById<TextView>(R.id.tv_awake_duration)?.text = String.format(Locale.getDefault(), "%d phút", it.awakeMinutes)
                            }
                        }
                    }
                    view.findViewById<View>(R.id.btn_back)?.setOnClickListener { v -> Animation.applyClick(v) { showLayout(R.layout.layout_home) } }
                }

                R.layout.layout_calories_detail -> {
                    val tvTotal = view.findViewById<TextView>(R.id.tv_calories_total)
                    val tvTarget = view.findViewById<TextView>(R.id.tv_calories_target)
                    val tvRemaining = view.findViewById<TextView>(R.id.tv_calories_remaining)
                    val tvTodayStatus = view.findViewById<TextView>(R.id.tv_today_status)
                    val pbCircle = view.findViewById<ProgressBar>(R.id.pb_calories_circle)

                    var lastTotal = 0
                    var lastProgress = 0
                    
                    launch {
                        combine(
                            database.mealDao().getTodayTotalCalories(),
                            userManager.userFlow
                        ) { total: Int?, user: User ->
                            Pair(total ?: 0, user)
                        }.collectLatest { (total, user) ->
                            val current = total
                            Animation.animateTextValue(tvTotal, lastTotal, current)
                            lastTotal = current
                            
                            val target = user.targetCalories
                            val remaining = (target - current).coerceAtLeast(0)
                            tvRemaining.text = "$remaining kcal"
                            
                            tvTarget.text = "/ $target kcal"
                            pbCircle.max = target
                            Animation.animateProgress(pbCircle, lastProgress, current)
                            lastProgress = current

                            val status = when {
                                target <= 1800 -> getString(R.string.lose_weight)
                                target <= 2500 -> getString(R.string.maintain_weight)
                                target <= 3000 -> getString(R.string.gain_weight)
                                else -> getString(R.string.custom_goal)
                            }
                            tvTodayStatus.text = "Hôm nay · $status"
                        }
                    }
                    
                    launch {
                        database.mealDao().getTodayMeals().collectLatest { meals ->
                            val morning = meals.filter { it.mealType == "Sáng" }.sumOf { it.calories }
                            val lunch = meals.filter { it.mealType == "Trưa" }.sumOf { it.calories }
                            val afternoon = meals.filter { it.mealType == "Chiều" }.sumOf { it.calories }
                            val dinner = meals.filter { it.mealType == "Tối" }.sumOf { it.calories }
                            
                            val maxCal = listOf(morning, lunch, afternoon, dinner).maxOrNull()?.coerceAtLeast(1) ?: 1
                            val maxHeightPx = 130 * resources.displayMetrics.density
                            
                            view.findViewById<View>(R.id.bar_morning)?.layoutParams?.height = (morning.toFloat() / maxCal * maxHeightPx).toInt().coerceAtLeast(20)
                            view.findViewById<View>(R.id.bar_lunch)?.layoutParams?.height = (lunch.toFloat() / maxCal * maxHeightPx).toInt().coerceAtLeast(20)
                            view.findViewById<View>(R.id.bar_afternoon)?.layoutParams?.height = (afternoon.toFloat() / maxCal * maxHeightPx).toInt().coerceAtLeast(20)
                            view.findViewById<View>(R.id.bar_dinner)?.layoutParams?.height = (dinner.toFloat() / maxCal * maxHeightPx).toInt().coerceAtLeast(20)
                            view.findViewById<View>(R.id.bar_morning)?.requestLayout()
                        }
                    }
                    
                    launch { database.mealDao().getAllMeals().collectLatest { meals ->
                        val container = view.findViewById<LinearLayout>(R.id.ll_meal_history_container)
                        container?.removeAllViews()
                        meals.take(7).forEachIndexed { index, meal ->
                            val item = LayoutInflater.from(this@MainActivity).inflate(R.layout.item_meal_history, container, false)
                            item.findViewById<TextView>(R.id.tv_meal_name).text = meal.name
                            item.findViewById<TextView>(R.id.tv_meal_calories).text = meal.calories.toString()
                            container?.addView(item)
                            Animation.animateItemEntry(item, index)
                        }
                    } }
                    
                    view.findViewById<View>(R.id.btn_back)?.setOnClickListener { v -> Animation.applyClick(v) { showLayout(R.layout.layout_home) } }
                    view.findViewById<View>(R.id.btn_set_goal)?.setOnClickListener { v -> Animation.applyClick(v) { showCaloriesGoalDialog() } }
                    view.findViewById<View>(R.id.btn_camera_capture)?.setOnClickListener { v -> Animation.applyClick(v) { checkCameraPermission() } }
                }
                else -> {}
            }
        }
    }

    private fun syncBottomNav(layoutId: Int) {
        val itemId = when(layoutId) {
            R.layout.layout_home -> R.id.nav_home
            R.layout.fragment_weight_detail -> R.id.nav_health
            R.layout.layout_heart_rate_detail -> R.id.nav_health
            R.layout.fragment_chat_ai -> R.id.nav_chatbox
            R.layout.fragment_goals -> R.id.nav_goals
            R.layout.fragment_settings -> R.id.nav_settings
            else -> R.id.nav_home
        }
        bottomNav.selectedItemId = itemId
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        try {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            photoFile = File.createTempFile("MEAL_${System.currentTimeMillis()}_", ".jpg", storageDir)
            val photoURI = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile!!)
            takePhotoLauncher.launch(photoURI)
        } catch (e: Exception) { Toast.makeText(this, getString(R.string.camera_error, e.message), Toast.LENGTH_SHORT).show() }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun setupWeightDetailUI(view: View) {
        view.findViewById<View>(R.id.btn_add_weight)?.setOnClickListener { v ->
            Animation.applyClick(v) { showUpdateWeightDialog() }
        }

        database.weightDao().getAllWeights().collectLatest { weights ->
            val latest = weights.firstOrNull()

            // Current weight
            latest?.let {
                view.findViewById<TextView>(R.id.tv_current_weight)?.text = String.format(Locale.getDefault(), "%.1f", it.weight)
            }

            // Weight diff badges
            if (weights.size >= 2 && latest != null) {
                val diffLast = latest.weight - weights[1].weight
                val signLast = if (diffLast >= 0) "↗" else "↘"
                view.findViewById<TextView>(R.id.tv_diff_last)?.text = "$signLast ${String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(diffLast))} kg lần trước"

                val firstWeight = weights.last().weight
                val diffTotal = latest.weight - firstWeight
                val signTotal = if (diffTotal >= 0) "↗" else "↘"
                view.findViewById<TextView>(R.id.tv_diff_total)?.text = "$signTotal ${String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(diffTotal))} kg tổng"
            }

            // Goal
            val user = userManager.getUser()
            val targetW = user.targetWeight ?: 65.0
            view.findViewById<TextView>(R.id.tv_goal_weight)?.text = String.format(Locale.getDefault(), "%.0f", targetW)
            if (latest != null) {
                val remaining = latest.weight - targetW
                val startWeight = weights.last().weight
                val totalChange = startWeight - targetW
                val achievedChange = startWeight - latest.weight
                val progress = if (kotlin.math.abs(totalChange) > 0.1) {
                    ((achievedChange / totalChange) * 100).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                view.findViewById<ProgressBar>(R.id.progress_goal)?.progress = progress
                if (remaining > 0) {
                    view.findViewById<TextView>(R.id.tv_goal_remaining)?.text = "Còn ${String.format(Locale.getDefault(), "%.1f", remaining)} kg"
                } else {
                    view.findViewById<TextView>(R.id.tv_goal_remaining)?.text = "Đã đạt mục tiêu!"
                }
            }

            // Height info
            if (user.height > 0) {
                view.findViewById<TextView>(R.id.tv_height_info)?.text = "Chiều cao: ${user.height} cm"
            }

            // BMI
            if (user.height > 0 && latest != null) {
                val heightM = user.height / 100.0
                val bmi = latest.weight / (heightM * heightM)
                view.findViewById<TextView>(R.id.tv_bmi_value)?.text = String.format(Locale.getDefault(), "%.1f", bmi)
                val statusText = when {
                    bmi < 18.5 -> getString(R.string.bmi_status_under)
                    bmi < 25 -> getString(R.string.bmi_status_normal)
                    bmi < 30 -> getString(R.string.bmi_status_over)
                    else -> getString(R.string.bmi_status_obese)
                }
                view.findViewById<TextView>(R.id.tv_bmi_status)?.text = statusText
                view.findViewById<TextView>(R.id.tv_bmi_indicator_val)?.text = String.format(Locale.getDefault(), "%.1f", bmi)

                // Position indicator on BMI scale
                val bmiClamped = bmi.coerceIn(13.0, 40.0)
                val position = ((bmiClamped - 13.0) / (40.0 - 13.0)).coerceIn(0.0, 1.0)
                val scaleBg = view.findViewById<View>(R.id.bmi_scale_bg)
                if (scaleBg != null) {
                    scaleBg.post {
                        val parent = scaleBg.parent as? android.view.ViewGroup
                        val scaleWidth = parent?.width ?: scaleBg.width
                        if (scaleWidth > 0) {
                            val indicatorX = (position * (scaleWidth - 18)).toInt()
                            val indicator = view.findViewById<View>(R.id.iv_bmi_indicator)
                            indicator?.let {
                                val lp = it.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                                lp?.marginStart = indicatorX
                                if (lp != null) it.layoutParams = lp
                            }
                        }
                    }
                }
            }

            // History container
            val container = view.findViewById<LinearLayout>(R.id.ll_weight_history_container) ?: return@collectLatest
            container.removeAllViews()
            view.findViewById<TextView>(R.id.tv_history_count)?.text = weights.size.toString()
            weights.take(10).forEachIndexed { index, record ->
                val item = LayoutInflater.from(this@MainActivity).inflate(R.layout.item_weight_history, container, false)
                item.findViewById<TextView>(R.id.tv_weight_value).text = String.format(Locale.getDefault(), "%.1f kg", record.weight)
                val date = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date(record.timestamp))
                item.findViewById<TextView>(R.id.tv_date).text = date
                container.addView(item)
                Animation.animateItemEntry(item, index)
            }
            if (weights.isNotEmpty()) {
                updateWeightChart(view, weights)
            }
        }
    }

    private fun updateWeightChart(view: View, weights: List<com.example.better_life.data.entities.WeightRecord>) {
        val chart = view.findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.weight_chart) ?: return
        
        val entries = weights.reversed().mapIndexed { index, record ->
            com.github.mikephil.charting.data.Entry(index.toFloat(), record.weight.toFloat())
        }

        val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, getString(R.string.weight_chart_label)).apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.primary_teal)
            setCircleColor(ContextCompat.getColor(this@MainActivity, R.color.primary_teal))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(false)
            valueTextSize = 10f
            mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@MainActivity, R.color.primary_teal)
            fillAlpha = 50
        }

        chart.apply {
            data = com.github.mikephil.charting.data.LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.apply {
                textColor = ContextCompat.getColor(this@MainActivity, R.color.text_secondary)
                gridColor = Color.parseColor("#EEEEEE")
            }
            animateX(1000)
            invalidate()
        }
    }

    private fun showCaloriesGoalDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calories_goal, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val user = userManager.getUser()
        val etCustom = dialogView.findViewById<EditText>(R.id.et_custom_goal)
        etCustom.setText(user.targetCalories.toString())

        val options = listOf(
            dialogView.findViewById<View>(R.id.option_lose_weight),
            dialogView.findViewById<View>(R.id.option_maintain),
            dialogView.findViewById<View>(R.id.option_gain_weight),
            dialogView.findViewById<View>(R.id.option_custom)
        )

        fun selectOption(selectedId: Int) {
            options.forEach { option ->
                if (option.id == selectedId) {
                    option.setBackgroundResource(R.drawable.bg_goal_item_selected)
                } else {
                    option.setBackgroundResource(R.drawable.bg_goal_item_normal)
                }
            }
        }

        options[0].setOnClickListener { 
            selectOption(it.id)
            etCustom.setText("1800")
        }
        options[1].setOnClickListener { 
            selectOption(it.id)
            etCustom.setText("2500")
        }
        options[2].setOnClickListener { 
            selectOption(it.id)
            etCustom.setText("3000")
        }
        options[3].setOnClickListener { 
            selectOption(it.id)
        }

        when(user.targetCalories) {
            1800 -> selectOption(R.id.option_lose_weight)
            2500 -> selectOption(R.id.option_maintain)
            3000 -> selectOption(R.id.option_gain_weight)
            else -> selectOption(R.id.option_custom)
        }

        dialogView.findViewById<View>(R.id.btn_close).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btn_save_goal).setOnClickListener {
            val newVal = etCustom.text.toString().toIntOrNull() ?: 2500
            userManager.saveUser(user.copy(targetCalories = newVal))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showUpdateWeightDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_weight, null)
        val etWeight = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_weight)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<android.widget.Button>(R.id.btn_save)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val weightStr = etWeight.text.toString()
            if (weightStr.isNotEmpty()) {
                val weight = weightStr.toDoubleOrNull()
                if (weight != null) {
                    lifecycleScope.launch {
                        database.weightDao().insertWeight(com.example.better_life.data.entities.WeightRecord(weight = weight))
                        val user = userManager.getUser()
                        userManager.saveUser(user.copy(weight = weight))
                    }
                    dialog.dismiss()
                } else {
                    etWeight.error = getString(R.string.invalid_number)
                }
            } else {
                etWeight.error = getString(R.string.empty_field)
            }
        }

        dialog.show()
    }

    private fun processMealImage(file: File) {
        val food = listOf("Phở Bò" to 450, "Cơm Tấm" to 600, "Bánh Mì" to 320, "Salad Ức Gà" to 280).random()
        AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setTitle(getString(R.string.ai_result_title))
            .setMessage(getString(R.string.ai_detect_format, food.first, food.second))
            .setPositiveButton(getString(R.string.save)) { _, _ -> saveMealToDb(food.first, food.second, file.absolutePath) }
            .setNegativeButton(getString(R.string.cancel), null).show()
    }

    private fun saveMealToDb(n: String, c: Int, p: String) {
        lifecycleScope.launch { database.mealDao().insert(MealRecord(name = n, calories = c, timestamp = System.currentTimeMillis(), mealType = "Auto", imageUri = p)) }
    }
}
