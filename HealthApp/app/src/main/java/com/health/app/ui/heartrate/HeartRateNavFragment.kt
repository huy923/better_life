package com.health.app.ui.heartrate

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.health.app.R
import com.health.heartrate.phone.ui.dashboard.HeartRateDashboardFragment
import com.health.heartrate.phone.ui.history.HeartRateHistoryFragment

class HeartRateNavFragment : Fragment(R.layout.fragment_heartrate_nav) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.hrNavContainer, HeartRateDashboardFragment()).commit()
        }
        view.findViewById<Button>(R.id.btnHrDash).setOnClickListener {
            childFragmentManager.beginTransaction()
                .replace(R.id.hrNavContainer, HeartRateDashboardFragment()).commit()
        }
        view.findViewById<Button>(R.id.btnHrHistory).setOnClickListener {
            childFragmentManager.beginTransaction()
                .replace(R.id.hrNavContainer, HeartRateHistoryFragment()).commit()
        }
    }
}
