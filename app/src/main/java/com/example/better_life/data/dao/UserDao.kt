package com.example.better_life.data.dao

import androidx.room.*
import com.example.better_life.data.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = 1")
    fun getUser(): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: User)

    @Update
    suspend fun update(user: User)
}
