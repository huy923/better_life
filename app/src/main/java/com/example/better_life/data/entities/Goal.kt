package com.example.better_life.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String,
    val streak: Int,
    val iconName: String,
    val isCompleted: Boolean = false
)
