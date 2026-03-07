package com.example.better_life

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.better_life.data.DemoData
import com.example.better_life.data.database.AppDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout
    private lateinit var bottomNav: BottomNavigationView
    private var currentLayoutId: Int = R.layout.layout_home
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(this)
        
        // Chèn dữ liệu mẫu nếu DB trống (Pro check)
        lifecycleScope.launch {
            val user = database.userDao().getUser().first()
            if (user == null) {
                DemoData.insertSampleData(database)
            }
        }

        savedInstanceState?.let {
            currentLayoutId = it.getInt("CURRENT_LAYOUT_ID", R.layout.layout_home)
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

    private fun showLayout(layoutId: Int) {
        currentLayoutId = layoutId
        container.removeAllViews()
        val view = LayoutInflater.from(this).inflate(layoutId, container, false)
        container.addView(view)

        // Tự động gán nút Back
        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            showLayout(R.layout.layout_home)
            syncBottomNav(R.layout.layout_home)
        }

        // Đổ dữ liệu từ Database vào Interface (Pro Logic)
        bindDataToView(layoutId, view)
    }

    private fun bindDataToView(layoutId: Int, view: View) {
        lifecycleScope.launch {
            when (layoutId) {
                R.layout.layout_home -> {
                    // 1. User Info
                    database.userDao().getUser().collect { user ->
                        user?.let {
                            view.findViewById<TextView>(R.id.tv_username)?.text = it.name
                        }
                    }
                    
                    // 2. Heart Rate
                    val lastHr = database.heartRateDao().getLatestRecord()
                    lastHr?.let {
                        val cardHr = view.findViewById<View>(R.id.card_heart_rate)
                        cardHr?.findViewById<TextView>(R.id.tv_value)?.text = it.bpm.toString()
                        cardHr?.setOnClickListener { showLayout(R.layout.layout_heart_rate_detail) }
                    }

                    // 3. Calories
                    database.mealDao().getTodayTotalCalories().collect { total ->
                        val cardCal = view.findViewById<View>(R.id.card_calories)
                        cardCal?.findViewById<TextView>(R.id.tv_value)?.text = (total ?: 0).toString()
                        cardCal?.setOnClickListener { showLayout(R.layout.layout_calories_detail) }
                    }

                    view.findViewById<View>(R.id.card_sleep)?.setOnClickListener { showLayout(R.layout.layout_sleep_detail) }
                    view.findViewById<View>(R.id.action_run)?.setOnClickListener { showLayout(R.layout.layout_running_detail) }
                }

                R.layout.layout_heart_rate_detail -> {
                    val latest = database.heartRateDao().getLatestRecord()
                    latest?.let {
                        view.findViewById<TextView>(R.id.tv_value)?.text = it.bpm.toString()
                    }
                    val avg = database.heartRateDao().getAverageBpm()
                }

                R.layout.fragment_goals -> {
                    database.goalDao().getAllGoals().collect { goals ->
                        if (goals.isNotEmpty()) {
                            view.findViewById<TextView>(R.id.tv_goal1_title)?.text = goals[0].title
                        }
                    }
                }

                R.layout.fragment_settings -> setupSettingsPage(view)
            }
        }
    }

    private fun showRunningNotificationsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_running_notifications, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme).setView(dialogView).create()
        dialogView.findViewById<View>(R.id.btn_close_dialog).setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showSetGoalDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_running_goal, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTheme).setView(dialogView).create()

        val etValue = dialogView.findViewById<EditText>(R.id.et_goal_value)
        val options = listOf(
            dialogView.findViewById<View>(R.id.option_steps),
            dialogView.findViewById<View>(R.id.option_time),
            dialogView.findViewById<View>(R.id.option_calories),
            dialogView.findViewById<View>(R.id.option_none)
        )

        options.forEach { option ->
            option.setOnClickListener { selected ->
                options.forEach { opt ->
                    opt.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F0F4F8"))
                }
                selected.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.running_green))
            }
        }

        dialogView.findViewById<View>(R.id.btn_close_goal_dialog).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btn_save_goal).setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setupSettingsPage(view: View) {
        val switchDarkMode = view.findViewById<SwitchCompat>(R.id.switch_dark_mode)
        val tvDarkModeTitle = view.findViewById<TextView>(R.id.tv_dark_mode_title)
        val isNightMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        switchDarkMode.isChecked = isNightMode
        tvDarkModeTitle.text = getString(R.string.dark_mode)

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
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
        val languageCodes = arrayOf("vi", "en")
        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "vi"
        val checkedItem = if (currentLocale == "en") 1 else 0

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.language))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val appLocales = LocaleListCompat.forLanguageTags(languageCodes[which])
                AppCompatDelegate.setApplicationLocales(appLocales)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}