package com.example.better_life.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.better_life.R
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.databinding.LayoutHomeBinding
import com.example.better_life.ui.viewmodels.HomeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Fragment responsible for displaying the main dashboard.
 */
class HomeFragment : Fragment() {

    private var _binding: LayoutHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(AppDatabase.getDatabase(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe User Data
            launch {
                viewModel.user.collectLatest { user ->
                    binding.tvUsername.text = user?.name ?: getString(R.string.default_user)
                }
            }

            // Observe Heart Rate
            launch {
                viewModel.latestHeartRate.collectLatest { record ->
                    binding.cardHeartRate.root.findViewById<android.widget.TextView>(R.id.tv_value).text =
                        record?.bpm?.toString() ?: "0"
                }
            }

            // Observe Calories
            launch {
                viewModel.todayCalories.collectLatest { calories ->
                    binding.cardCalories.root.findViewById<android.widget.TextView>(R.id.tv_value).text = 
                        calories.toString()
                }
            }
            
            // Observe Sleep
            launch {
                viewModel.latestSleep.collectLatest { sleep ->
                    sleep?.let {
                        val totalMinutes = (it.endTime - it.startTime) / 60000
                        binding.cardSleep.root.findViewById<android.widget.TextView>(R.id.tv_value).text = 
                            String.format(Locale.getDefault(), "%.1f", totalMinutes / 60.0)
                    }
                }
            }

            // Observe Weight
            launch {
                viewModel.currentWeight.collectLatest { user ->
                    binding.cardWeight.root.findViewById<android.widget.TextView>(R.id.tv_value).text = 
                        user?.weight?.toString() ?: "0.0"
                }
            }
        }
    }

    private fun setupListeners() {
        binding.actionMeal.root.setOnClickListener {
            // Navigation logic
        }
        binding.actionWeight.root.setOnClickListener {
            showUpdateWeightDialog()
        }
    }

    private fun showUpdateWeightDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_weight, null)
        val etWeight = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_weight)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<android.widget.Button>(R.id.btn_save)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val weightStr = etWeight.text.toString()
            if (weightStr.isNotEmpty()) {
                val weight = weightStr.toDoubleOrNull()
                if (weight != null) {
                    viewModel.updateWeight(weight)
                    dialog.dismiss()
                } else {
                    etWeight.error = getString(R.string.invalid_number)
                }
            } else {
                etWeight.error = getString(R.string.empty_field)
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
