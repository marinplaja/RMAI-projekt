package com.example.mainquest.data

import androidx.room.*

@Dao
interface UnlockedRewardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unlockedReward: UnlockedReward): Long

    @Update
    suspend fun update(unlockedReward: UnlockedReward)

    @Delete
    suspend fun delete(unlockedReward: UnlockedReward)

    @Query("SELECT * FROM unlocked_rewards WHERE id = :id")
    suspend fun getById(id: Int): UnlockedReward?

    @Query("SELECT * FROM unlocked_rewards WHERE userId = :userId")
    suspend fun getByUserId(userId: Int): List<UnlockedReward>

    @Query("SELECT * FROM unlocked_rewards")
    suspend fun getAll(): List<UnlockedReward>
} 