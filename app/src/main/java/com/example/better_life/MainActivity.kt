package com.example.better_life

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        ActionBar actionBar = getSh;
        container = findViewById(R.id.container)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Normal app will show home layout first
        showLayout(R.layout.layout_home)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showLayout(R.layout.layout_home)
                    true
                }
                R.id.nav_health -> {
                    showLayout(R.layout.fragment_health)
                    true
                }
                R.id.nav_nutrition -> {
                    showLayout(R.layout.fragment_nutrition)
                    true
                }
                R.id.nav_goals -> {
                    showLayout(R.layout.fragment_goals)
                    true
                }
                R.id.nav_settings -> {
                    showLayout(R.layout.fragment_settings)
                    true
                }
                else -> false
            }
        }
    }

    private fun showLayout(layoutId: Int) {
        container.removeAllViews()
        val view = LayoutInflater.from(this).inflate(layoutId, container, false)
        container.addView(view)
    }
}