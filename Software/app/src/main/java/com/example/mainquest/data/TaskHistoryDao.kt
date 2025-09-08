package com.example.mainquest.data

import androidx.room.*

@Dao
interface TaskHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(taskHistory: TaskHistory): Long

    @Update
    suspend fun update(taskHistory: TaskHistory)

    @Delete
    suspend fun delete(taskHistory: TaskHistory)

    @Query("SELECT * FROM task_history WHERE id = :id")
    suspend fun getById(id: Int): TaskHistory?

    @Query("SELECT * FROM task_history WHERE userId = :userId")
    suspend fun getByUserId(userId: Int): List<TaskHistory>

    @Query("SELECT * FROM task_history WHERE taskId = :taskId")
    suspend fun getByTaskId(taskId: Int): List<TaskHistory>

    @Query("SELECT * FROM task_history")
    suspend fun getAll(): List<TaskHistory>
} 