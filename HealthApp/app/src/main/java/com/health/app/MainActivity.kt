package com.health.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.health.app.service.TrackingService
import com.health.app.ui.history.HistoryFragment
import com.health.app.ui.profile.ProfileFragment
import com.health.app.ui.tracking.TrackingFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()
        if (savedInstanceState == null) loadFragment(TrackingFragment())
        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_track   -> loadFragment(TrackingFragment())
                R.id.nav_history -> loadFragment(HistoryFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }

    private fun loadFragment(f: Fragment) =
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, f).commit()

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(TrackingService.CHANNEL_ID, "GPS Tracking", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Theo dõi lộ trình" }
        )
    }
}
