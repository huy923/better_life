package com.health.app.ui.history

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.health.app.R
import com.health.app.utils.CalorieCalculator

class HistoryFragment : Fragment(R.layout.fragment_history) {
    private val vm: HistoryViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvSummary = view.findViewById<TextView>(R.id.tvHistorySummary)
        vm.allSessions.observe(viewLifecycleOwner) { sessions ->
            if (sessions.isEmpty()) { tvSummary.text = "Chưa có dữ liệu"; return@observe }
            val sb = StringBuilder()
            sessions.forEach { s ->
                sb.appendLine("📅 ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(s.date))}")
                sb.appendLine("   🏃 ${CalorieCalculator.formatDistance(s.distanceMeters)}")
                sb.appendLine("   👣 ${s.steps} bước   🔥 ${CalorieCalculator.formatCalories(s.caloriesBurned)}")
                sb.appendLine()
            }
            tvSummary.text = sb.toString()
        }
    }
}
