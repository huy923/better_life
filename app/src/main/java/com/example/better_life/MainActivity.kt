package com.example.better_life

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.transition.Fade
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
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
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.data.entities.MealRecord
import com.example.better_life.data.entities.RunningRecord
import com.example.better_life.sensor.StepCounterManager
import com.example.better_life.services.TrackingService
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout
    private lateinit var bottomNav: BottomNavigationView
    private var currentLayoutId: Int = R.layout.layout_home
    private lateinit var database: AppDatabase
    
    private lateinit var stepCounterManager: StepCounterManager
    private var isTrackingRunning = false
    private var currentDistance = 0f
    private var currentDuration = 0L

    private val trackingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TrackingService.ACTION_UPDATE) {
                currentDistance = intent.getFloatExtra(TrackingService.EXTRA_DISTANCE, 0f)
                currentDuration = intent.getLongExtra(TrackingService.EXTRA_DURATION, 0L)
                updateRunningUI()
            }
        }
    }

    private var photoFile: File? = null
    
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera()
        else Toast.makeText(this, "Cần quyền Camera để chụp ảnh món ăn", Toast.LENGTH_SHORT).show()
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
        stepCounterManager = StepCounterManager(this)
        
        savedInstanceState?.let {
            currentLayoutId = it.getInt("CURRENT_LAYOUT_ID", R.layout.layout_home)
        }

        lifecycleScope.launch {
            val user = database.userDao().getUser().first()
            if (user == null) DemoData.insertSampleData(database)
            cleanupOldData()
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
                R.id.nav_health -> R.layout.layout_heart_rate_detail
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
        
        if (animate) {
            val fade = Fade()
            fade.duration = 300
            TransitionManager.beginDelayedTransition(container, fade)
        }
        
        container.removeAllViews()
        val view = LayoutInflater.from(this).inflate(layoutId, container, false)
        container.addView(view)

        // Slide-up animation for the new view content
        view.translationY = 100f
        view.alpha = 0f
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            applyClickAnimation(it) {
                showLayout(R.layout.layout_home)
                syncBottomNav(R.layout.layout_home)
            }
        }

        bindDataToView(layoutId, view)
        if (layoutId == R.layout.fragment_settings) setupSettingsPage(view)
    }

    private fun applyClickAnimation(view: View, onAnimationEnd: () -> Unit) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction { onAnimationEnd() }
                    .start()
            }
            .start()
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
            applyClickAnimation(it) { showLanguageDialog() }
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.lang_vi), getString(R.string.lang_en))
        AlertDialog.Builder(this).setTitle(R.string.language).setItems(languages) { _, which ->
            val localeTag = if (which == 0) "vi" else "en"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
        }.show()
    }

    private fun bindDataToView(layoutId: Int, view: View) {
        lifecycleScope.launch {
            when (layoutId) {
                R.layout.layout_home -> {
                    launch { database.userDao().getUser().collectLatest { it?.let { view.findViewById<TextView>(R.id.tv_username)?.text = it.name } } }
                    
                    launch { database.heartRateDao().getLatestRecord().collectLatest { it?.let { 
                        val cardHr = view.findViewById<View>(R.id.card_heart_rate)
                        cardHr?.findViewById<TextView>(R.id.tv_value)?.text = it.bpm.toString()
                        cardHr?.setOnClickListener { v -> applyClickAnimation(v) { showLayout(R.layout.layout_heart_rate_detail) } }
                    } } }
                    
                    launch { database.mealDao().getTodayTotalCalories().collectLatest { total ->
                        val cardCal = view.findViewById<View>(R.id.card_calories)
                        val tvValue = cardCal?.findViewById<TextView>(R.id.tv_value)
                        tvValue?.text = (total ?: 0).toString()
                        cardCal?.setOnClickListener { v -> applyClickAnimation(v) { showLayout(R.layout.layout_calories_detail) } }
                    } }
                    
                    launch { database.sleepDao().getLatestRecord().collectLatest { it?.let { 
                        val cardSleep = view.findViewById<View>(R.id.card_sleep)
                        val totalMinutes = (it.endTime - it.startTime) / 60000
                        cardSleep?.findViewById<TextView>(R.id.tv_value)?.text = String.format(Locale.getDefault(), "%.1f", totalMinutes / 60.0)
                        cardSleep?.setOnClickListener { v -> applyClickAnimation(v) { showLayout(R.layout.layout_sleep_detail) } }
                    } } }

                    // Real-time Step Counter on Home
                    launch {
                        stepCounterManager.steps.collectLatest { steps ->
                            val tvSteps = view.findViewById<TextView>(R.id.tv_home_steps)
                            val pbSteps = view.findViewById<ProgressBar>(R.id.pb_home_steps)
                            tvSteps?.text = String.format(Locale.getDefault(), "%,d", steps)
                            pbSteps?.progress = steps
                        }
                    }

                    view.findViewById<View>(R.id.action_meal)?.setOnClickListener { v -> applyClickAnimation(v) { showLayout(R.layout.layout_calories_detail) } }
                    view.findViewById<View>(R.id.action_run)?.setOnClickListener { v -> applyClickAnimation(v) { showLayout(R.layout.layout_running_detail) } }
                }

                R.layout.layout_running_detail -> {
                    val btnPlay = view.findViewById<ImageButton>(R.id.btn_play_pause)
                    btnPlay?.setOnClickListener { v ->
                        applyClickAnimation(v) {
                            if (isTrackingRunning) stopRunningTracking()
                            else startRunningTracking()
                        }
                    }
                    updateRunningUI()
                    
                    launch { database.runningDao().getAllRecords().collectLatest { records ->
                        val container = view.findViewById<LinearLayout>(R.id.ll_running_history_container)
                        container?.removeAllViews()
                        records.take(5).forEach { record ->
                            val item = LayoutInflater.from(this@MainActivity).inflate(R.layout.item_running_history, container, false)
                            item.findViewById<TextView>(R.id.tv_run_type).text = record.activityType
                            item.findViewById<TextView>(R.id.tv_run_distance).text = String.format(Locale.getDefault(), "%.2f km", record.distance)
                            item.findViewById<TextView>(R.id.tv_run_duration).text = record.duration
                            item.findViewById<TextView>(R.id.tv_run_date).text = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(record.timestamp))
                            item.findViewById<TextView>(R.id.tv_run_steps).text = String.format(Locale.getDefault(), "%,d", record.steps)
                            item.findViewById<TextView>(R.id.tv_run_calories).text = record.calories.toString()
                            container?.addView(item)
                        }
                    } }
                }

                R.layout.layout_calories_detail -> {
                    launch { database.mealDao().getTodayTotalCalories().collectLatest { total ->
                        val current = total ?: 0
                        val tvTotal = view.findViewById<TextView>(R.id.tv_calories_total)
                        animateTextValue(tvTotal, 0, current)
                        
                        val pbCircle = view.findViewById<ProgressBar>(R.id.pb_calories_circle)
                        animateProgressBar(pbCircle, 0, current)
                    } }
                    
                    launch { database.mealDao().getAllMeals().collectLatest { meals ->
                        val container = view.findViewById<LinearLayout>(R.id.ll_meal_history_container)
                        container?.removeAllViews()
                        meals.take(7).forEachIndexed { index, meal ->
                            val item = LayoutInflater.from(this@MainActivity).inflate(R.layout.item_meal_history, container, false)
                            item.findViewById<TextView>(R.id.tv_meal_name).text = meal.name
                            item.findViewById<TextView>(R.id.tv_meal_calories).text = meal.calories.toString()
                            item.findViewById<TextView>(R.id.tv_meal_time).text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(meal.timestamp))
                            container?.addView(item)
                            
                            // Animate item entry
                            item.alpha = 0f
                            item.translationX = 50f
                            item.animate()
                                .alpha(1f)
                                .translationX(0f)
                                .setDuration(300)
                                .setStartDelay(index * 50L)
                                .start()
                        }
                    } }
                    view.findViewById<View>(R.id.btn_camera_capture)?.setOnClickListener { v -> applyClickAnimation(v) { checkCameraPermission() } }
                }

                R.layout.layout_sleep_detail -> {
                    launch { database.sleepDao().getLatestRecord().collectLatest { it?.let { 
                        val totalMinutes = (it.endTime - it.startTime) / 60000
                        view.findViewById<TextView>(R.id.tv_sleep_duration)?.text = "${totalMinutes / 60}h ${totalMinutes % 60}p"
                        view.findViewById<TextView>(R.id.tv_sleep_score)?.text = it.score.toString()
                    } } }
                }
            }
        }
    }

    private fun startRunningTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 102)
            return
        }
        isTrackingRunning = true
        val intent = Intent(this, TrackingService::class.java).apply { action = TrackingService.ACTION_START }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        updateRunningUI()
    }

    private fun stopRunningTracking() {
        isTrackingRunning = false
        val intent = Intent(this, TrackingService::class.java).apply { action = TrackingService.ACTION_STOP }
        stopService(intent)
        
        // Save record to DB
        lifecycleScope.launch {
            val record = RunningRecord(
                date = "Hôm nay",
                activityType = "Chạy bộ",
                duration = formatDuration(currentDuration),
                steps = stepCounterManager.steps.value,
                calories = (currentDistance * 0.06).toInt(), // Basic estimation
                distance = (currentDistance / 1000.0),
                timestamp = System.currentTimeMillis()
            )
            database.runningDao().insert(record)
            currentDistance = 0f
            currentDuration = 0L
            updateRunningUI()
        }
    }

    private fun updateRunningUI() {
        if (currentLayoutId != R.layout.layout_running_detail) return
        val view = container.getChildAt(0) ?: return
        
        view.findViewById<TextView>(R.id.tv_timer)?.text = formatDuration(currentDuration)
        view.findViewById<TextView>(R.id.tv_distance)?.text = String.format(Locale.getDefault(), "%.2f", currentDistance / 1000.0)
        view.findViewById<TextView>(R.id.tv_steps)?.text = stepCounterManager.steps.value.toString()
        view.findViewById<TextView>(R.id.tv_calories)?.text = (currentDistance * 0.06).toInt().toString()
        
        val btnPlay = view.findViewById<ImageButton>(R.id.btn_play_pause)
        btnPlay?.setImageResource(if (isTrackingRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }

    private fun animateTextValue(textView: TextView?, from: Int, to: Int) {
        val animator = ValueAnimator.ofInt(from, to)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            textView?.text = animation.animatedValue.toString()
        }
        animator.start()
    }

    private fun animateProgressBar(progressBar: ProgressBar?, from: Int, to: Int) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", from, to)
        animator.duration = 1000
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    private suspend fun cleanupOldData() {
        val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
        val oldMeals = database.mealDao().getOldMeals(threeDaysAgo)
        oldMeals.forEach { it.imageUri?.let { path -> File(path).let { if (it.exists()) it.delete() } } }
        database.mealDao().deleteMeals(oldMeals)
    }

    private fun syncBottomNav(layoutId: Int) {
        val itemId = when(layoutId) {
            R.layout.layout_home -> R.id.nav_home
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
        } catch (e: Exception) { Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun processMealImage(file: File) {
        val food = listOf("Phở Bò" to 450, "Cơm Tấm" to 600, "Bánh Mì" to 320, "Salad Ức Gà" to 280).random()
        AlertDialog.Builder(this, R.style.CustomDialogTheme).setTitle("Kết quả AI").setMessage("Phát hiện: ${food.first}\nCalories: ${food.second} kcal\n\nBạn có muốn lưu không?")
            .setPositiveButton("Lưu") { _, _ -> saveMealToDb(food.first, food.second, file.absolutePath) }
            .setNegativeButton("Hủy", null).show()
    }

    private fun saveMealToDb(n: String, c: Int, p: String) {
        lifecycleScope.launch { database.mealDao().insert(MealRecord(name = n, calories = c, timestamp = System.currentTimeMillis(), mealType = "Auto", imageUri = p)) }
    }
}
