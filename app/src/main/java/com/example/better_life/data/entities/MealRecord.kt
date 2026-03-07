package com.example.better_life.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_records")
data class MealRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val calories: Int,
    val timestamp: Long,
    val mealType: String, // "Sáng", "Trưa", "Chiều", "Tối"
    val imageUri: String? = null
)
