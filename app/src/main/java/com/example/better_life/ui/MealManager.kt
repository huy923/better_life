package com.example.better_life.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.better_life.Animation
import com.example.better_life.R
import com.example.better_life.data.database.AppDatabase
import com.example.better_life.data.entities.MealRecord
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class MealManager(
    private val context: Context,
    private val database: AppDatabase,
    private val scope: LifecycleCoroutineScope
) {

    fun setupMealUI(view: View, onCameraClick: () -> Unit) {
        scope.launch {
            database.mealDao().getTodayTotalCalories().collectLatest { total ->
                val current = total ?: 0
                val tvTotal = view.findViewById<TextView>(R.id.tv_calories_total)
                Animation.animateTextValue(tvTotal, 0, current)
                
                val pbCircle = view.findViewById<ProgressBar>(R.id.pb_calories_circle)
                Animation.animateProgress(pbCircle, 0, current)
            }
        }
        
        scope.launch {
            database.mealDao().getAllMeals().collectLatest { meals ->
                val container = view.findViewById<LinearLayout>(R.id.ll_meal_history_container)
                container?.removeAllViews()
                meals.take(7).forEachIndexed { index, meal ->
                    val item = LayoutInflater.from(context).inflate(R.layout.item_meal_history, container, false)
                    item.findViewById<TextView>(R.id.tv_meal_name).text = meal.name
                    item.findViewById<TextView>(R.id.tv_meal_calories).text = meal.calories.toString()
                    container?.addView(item)
                    Animation.animateItemEntry(item, index)
                }
            }
        }

        view.findViewById<View>(R.id.btn_camera_capture)?.setOnClickListener { v ->
            Animation.applyClick(v) { onCameraClick() }
        }
    }

    fun processMealImage(filePath: String) {
        val food = listOf("Phở Bò" to 450, "Cơm Tấm" to 600, "Bánh Mì" to 320, "Salad Ức Gà" to 280).random()
        AlertDialog.Builder(context, R.style.CustomDialogTheme)
            .setTitle(context.getString(R.string.ai_result_title))
            .setMessage(context.getString(R.string.ai_detect_format, food.first, food.second))
            .setPositiveButton(context.getString(R.string.save)) { _, _ -> saveMealToDb(food.first, food.second, filePath) }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }

    private fun saveMealToDb(name: String, calories: Int, path: String) {
        scope.launch {
            database.mealDao().insert(
                MealRecord(
                    name = name,
                    calories = calories,
                    timestamp = System.currentTimeMillis(),
                    mealType = "Auto",
                    imageUri = path
                )
            )
        }
    }
}
