package com.example.better_life.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

data class User(
    val id: Int = 1,
    val name: String,
    val age: Int,
    val height: Int,
    val weight: Double,
    val targetWeight: Double? = 65.0,
    val targetSteps: Int = 10000,
    val targetCalories: Int = 2500,
    val targetWater: Double = 2.5
)
