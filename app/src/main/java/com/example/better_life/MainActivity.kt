package com.example.better_life

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.container)
        bottomNav = findViewById(R.id.bottom_navigation)

        showLayout(R.layout.layout_home)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showLayout(R.layout.layout_home)
                    true
                }
                R.id.nav_health -> {
                    showLayout(R.id.nav_health)
                    true
                }
                R.id.nav_nutrition -> {
                    showLayout(R.id.nav_nutrition)
                    true
                }
                R.id.nav_goals -> {
                    showLayout(R.id.nav_goals)
                    true
                }
                R.id.nav_settings -> {
                    showLayout(R.id.nav_settings)
                    true
                }
                else -> false
            }
        }
    }

    private fun showLayout(id: Int) {
        container.removeAllViews()
        val layoutId = when(id) {
            R.id.nav_home, R.layout.layout_home -> R.layout.layout_home
            R.id.nav_health, R.layout.fragment_health -> R.layout.fragment_health
            R.id.nav_nutrition, R.layout.fragment_nutrition -> R.layout.fragment_nutrition
            R.id.nav_goals, R.layout.fragment_goals -> R.layout.fragment_goals
            R.id.nav_settings, R.layout.fragment_settings -> R.layout.fragment_settings
            else -> R.layout.layout_home
        }
        
        val view = LayoutInflater.from(this).inflate(layoutId, container, false)
        container.addView(view)


        view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_home
        }
    }
}