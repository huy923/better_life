package com.example.better_life.data.dao

import androidx.room.*
import com.example.better_life.data.entities.MealRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meal_records ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealRecord)

    @Query("SELECT SUM(calories) FROM meal_records WHERE date(timestamp / 1000, 'unixepoch') = date('now')")
    fun getTodayTotalCalories(): Flow<Int?>

    @Query("SELECT * FROM meal_records WHERE mealType = :type AND date(timestamp / 1000, 'unixepoch') = date('now')")
    suspend fun getMealByTypeToday(type: String): MealRecord?

    @Query("SELECT * FROM meal_records WHERE timestamp < :threshold")
    suspend fun getOldMeals(threshold: Long): List<MealRecord>

    @Delete
    suspend fun deleteMeals(meals: List<MealRecord>)
}
