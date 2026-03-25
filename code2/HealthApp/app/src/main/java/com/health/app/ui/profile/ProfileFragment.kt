package com.health.app.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.health.app.R
import com.health.app.data.entity.DailyGoal
import com.health.app.data.entity.UserProfile

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private val vm: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etWeight   = view.findViewById<EditText>(R.id.etWeight)
        val etHeight   = view.findViewById<EditText>(R.id.etHeight)
        val etAge      = view.findViewById<EditText>(R.id.etAge)
        val etGoalStep = view.findViewById<EditText>(R.id.etGoalSteps)
        val etGoalCal  = view.findViewById<EditText>(R.id.etGoalCalories)
        val btnSave    = view.findViewById<Button>(R.id.btnSaveProfile)

        vm.userProfile.observe(viewLifecycleOwner) { p ->
            p ?: return@observe
            etWeight.setText(p.weightKg.toString())
            etHeight.setText(p.heightCm.toString())
            etAge.setText(p.ageYears.toString())
        }
        vm.goal.observe(viewLifecycleOwner) { g ->
            g ?: return@observe
            etGoalStep.setText(g.targetSteps.toString())
            etGoalCal.setText(g.targetCalories.toString())
        }

        btnSave.setOnClickListener {
            vm.saveProfile(
                UserProfile(
                    weightKg = etWeight.text.toString().toFloatOrNull() ?: 60f,
                    heightCm = etHeight.text.toString().toFloatOrNull() ?: 165f,
                    ageYears = etAge.text.toString().toIntOrNull() ?: 25,
                    gender   = "other"
                ),
                DailyGoal(
                    targetSteps          = etGoalStep.text.toString().toIntOrNull() ?: 8000,
                    targetCalories       = etGoalCal.text.toString().toFloatOrNull() ?: 500f,
                    targetDistanceMeters = 5000f
                )
            )
            Toast.makeText(requireContext(), "Đã lưu!", Toast.LENGTH_SHORT).show()
        }
    }
}
