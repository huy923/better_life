package com.example.better_life

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Thiết lập giao diện tràn viền (Edge-to-Edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.container)
        bottomNav = findViewById(R.id.bottom_navigation)

        // Hiển thị trang chủ làm mặc định
        showLayout(R.layout.layout_home)

        bottomNav.setOnItemSelectedListener { item ->
            val layoutId = when (item.itemId) {
                R.id.nav_home -> R.layout.layout_home
                R.id.nav_health -> R.layout.layout_heart_rate_detail      // Đã sửa: Trỏ đúng về Sức khỏe
                R.id.nav_chatbox -> R.layout.fragment_nutrition
                R.id.nav_goals -> R.layout.fragment_goals
                R.id.nav_settings -> R.layout.fragment_settings
                else -> return@setOnItemSelectedListener false
            }
            showLayout(layoutId)
            true
        }
    }

    private fun showLayout(layoutId: Int) {
        container.removeAllViews()
        val view = LayoutInflater.from(this).inflate(layoutId, container, false)
        container.addView(view)

        // Đồng bộ nút Back trên tất cả các trang con
        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_home
        }

        // Click listener cho các thẻ ở Trang chủ
        if (layoutId == R.layout.layout_home) {
            view.findViewById<View>(R.id.card_heart_rate)?.setOnClickListener {
                showLayout(R.layout.layout_heart_rate_detail)
            }
        }
    }
}