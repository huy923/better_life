package com.health.heartrate.phone.ui.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.health.heartrate.phone.R
import com.health.heartrate.phone.ui.history.HeartRateHistoryFragment

class HeartRateMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hr_main)
        if (savedInstanceState == null) load(HeartRateDashboardFragment())
        findViewById<BottomNavigationView>(R.id.hrBottomNav).setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_hr_live    -> load(HeartRateDashboardFragment())
                R.id.nav_hr_history -> load(HeartRateHistoryFragment())
            }
            true
        }
    }
    private fun load(f: Fragment) =
        supportFragmentManager.beginTransaction().replace(R.id.hrFragContainer, f).commit()
}
