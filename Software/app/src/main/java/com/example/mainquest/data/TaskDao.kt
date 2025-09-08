package com.example.mainquest.data

import androidx.room.*

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Int): Task?

    @Query("SELECT * FROM tasks WHERE userId = :userId")
    suspend fun getByUserId(userId: Int): List<Task>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<Task>
    
    // Nove metode za dnevne ciljeve
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isDailyGoal = 1")
    suspend fun getDailyGoalsByUserId(userId: Int): List<Task>
    
    @Query("SELECT * FROM tasks WHERE userId = :userId AND category = :category")
    suspend fun getByUserIdAndCategory(userId: Int, category: String): List<Task>
    
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isDailyGoal = 1 AND category = :category")
    suspend fun getDailyGoalsByUserIdAndCategory(userId: Int, category: String): List<Task>
    
    @Query("UPDATE tasks SET dailyProgress = :progress, lastCompletedDate = :date WHERE id = :taskId")
    suspend fun updateDailyProgress(taskId: Int, progress: Int, date: String)
    
    @Query("UPDATE tasks SET streakCount = :streakCount WHERE id = :taskId")
    suspend fun updateStreakCount(taskId: Int, streakCount: Int)
    
    @Query("UPDATE tasks SET dailyProgress = 0 WHERE isDailyGoal = 1")
    suspend fun resetAllDailyProgress()
} 