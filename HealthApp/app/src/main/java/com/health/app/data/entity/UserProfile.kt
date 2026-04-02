package com.health.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val weightKg: Float,
    val heightCm: Float,
    val ageYears: Int,
    val gender: String
)
