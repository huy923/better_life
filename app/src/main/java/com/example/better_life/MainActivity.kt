package com.example.better_life

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.better_life.data.DemoData
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.data.entities.MealRecord
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

        showLayout(currentLayoutId)
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("CURRENT_LAYOUT_ID", currentLayoutId)
    }

    private fun showLayout(layoutId: Int) {
        currentLayoutId = layoutId
        container.removeAllViews()
        val view = LayoutInflater.from(this).inflate(layoutId, container, false)
        container.addView(view)

        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            showLayout(R.layout.layout_home)
            syncBottomNav(R.layout.layout_home)
        }

        bindDataToView(layoutId, view)
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
        layoutLanguage.setOnClickListener { showLanguageDialog() }
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
                    // 1. User Info
                    launch { database.userDao().getUser().collectLatest { it?.let { view.findViewById<TextView>(R.id.tv_username)?.text = it.name } } }
                    
                    // 2. Heart Rate
                    launch { database.heartRateDao().getLatestRecord().collectLatest { it?.let { 
                        val cardHr = view.findViewById<View>(R.id.card_heart_rate)
                        cardHr?.findViewById<TextView>(R.id.tv_value)?.text = it.bpm.toString()
                        cardHr?.setOnClickListener { showLayout(R.layout.layout_heart_rate_detail) }
                    } } }
                    
                    // 3. Calories
                    launch { database.mealDao().getTodayTotalCalories().collectLatest { total ->
                        val cardCal = view.findViewById<View>(R.id.card_calories)
                        cardCal?.findViewById<TextView>(R.id.show_total_calo)?.text = (total ?: 0).toString()
                        // Thêm sự kiện Click tại đây
                        cardCal?.setOnClickListener { showLayout(R.layout.layout_calories_detail) }
                    } }
                    
                    // 4. Sleep
                    launch { database.sleepDao().getLatestRecord().collectLatest { it?.let { 
                        val cardSleep = view.findViewById<View>(R.id.card_sleep)
                        val totalMinutes = (it.endTime - it.startTime) / 60000
                        cardSleep?.findViewById<TextView>(R.id.tv_sleep_value)?.text = String.format(Locale.getDefault(), "%.1f", totalMinutes / 60.0)
                        cardSleep?.setOnClickListener { showLayout(R.layout.layout_sleep_detail) }
                    } } }

                    // Quick Actions
                    view.findViewById<View>(R.id.action_meal)?.setOnClickListener { showLayout(R.layout.layout_calories_detail) }
                    view.findViewById<View>(R.id.action_run)?.setOnClickListener { showLayout(R.layout.layout_running_detail) }
                }

                R.layout.layout_calories_detail -> {
                    launch { database.mealDao().getTodayTotalCalories().collectLatest { total ->
                        val current = total ?: 0
                        view.findViewById<TextView>(R.id.tv_calories_total)?.text = current.toString()
                        view.findViewById<ProgressBar>(R.id.pb_calories_circle)?.progress = current
                    } }
                    launch { database.mealDao().getAllMeals().collectLatest { meals ->
                        val container = view.findViewById<LinearLayout>(R.id.ll_meal_history_container)
                        container?.removeAllViews()
                        meals.take(7).forEach { meal ->
                            val item = LayoutInflater.from(this@MainActivity).inflate(R.layout.item_meal_history, container, false)
                            item.findViewById<TextView>(R.id.tv_meal_name).text = meal.name
                            item.findViewById<TextView>(R.id.tv_meal_calories).text = meal.calories.toString()
                            item.findViewById<TextView>(R.id.tv_meal_time).text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(meal.timestamp))
                            container?.addView(item)
                        }
                    } }
                    view.findViewById<View>(R.id.btn_camera_capture)?.setOnClickListener { checkCameraPermission() }
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
