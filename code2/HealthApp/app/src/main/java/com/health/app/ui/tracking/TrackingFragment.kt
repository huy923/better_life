package com.health.app.ui.tracking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.health.app.R
import com.health.app.utils.CalorieCalculator

class TrackingFragment : Fragment(R.layout.fragment_tracking) {
    private val vm: TrackingViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted -> if (granted.values.all { it }) vm.startTracking() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvDistance = view.findViewById<TextView>(R.id.tvDistance)
        val tvCalories = view.findViewById<TextView>(R.id.tvCalories)
        val tvSteps    = view.findViewById<TextView>(R.id.tvSteps)
        val tvDuration = view.findViewById<TextView>(R.id.tvDuration)
        val btnStart   = view.findViewById<Button>(R.id.btnStart)
        val btnStop    = view.findViewById<Button>(R.id.btnStop)

        vm.distanceMeters.observe(viewLifecycleOwner) { tvDistance.text = CalorieCalculator.formatDistance(it) }
        vm.caloriesLive.observe(viewLifecycleOwner)   { tvCalories.text = CalorieCalculator.formatCalories(it) }
        vm.steps.observe(viewLifecycleOwner)           { tvSteps.text = "$it bước" }
        vm.durationSeconds.observe(viewLifecycleOwner) {
            tvDuration.text = "%02d:%02d".format(it / 60, it % 60)
        }
        vm.isTracking.observe(viewLifecycleOwner) { tracking ->
            btnStart.isEnabled = !tracking
            btnStop.isEnabled  = tracking
        }

        btnStart.setOnClickListener { checkPermissionsAndStart() }
        btnStop.setOnClickListener  { vm.stopAndSave() }
    }

    private fun checkPermissionsAndStart() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION)
        val denied = perms.filter { ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED }
        if (denied.isEmpty()) vm.startTracking() else permissionLauncher.launch(denied.toTypedArray())
    }
}
