package com.example.mainquest.data

import androidx.room.*

@Dao
interface RewardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reward: Reward): Long

    @Update
    suspend fun update(reward: Reward)

    @Delete
    suspend fun delete(reward: Reward)

    @Query("SELECT * FROM rewards WHERE id = :id")
    suspend fun getById(id: Int): Reward?

    @Query("SELECT * FROM rewards")
    suspend fun getAll(): List<Reward>
} 