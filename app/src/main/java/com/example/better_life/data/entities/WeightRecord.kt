package com.example.better_life.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_records")
data class WeightRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int = 1,
    val weight: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)