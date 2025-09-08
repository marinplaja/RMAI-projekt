package com.example.mainquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String?,
    val category: String?,
    val dueDate: String?,
    val isCompleted: Boolean = false,
    val xpReward: Int = 0,
    val userId: Int,
    val isDailyGoal: Boolean = false,
    val dailyTarget: Int = 1,
    val dailyProgress: Int = 0,
    val lastCompletedDate: String? = null,
    val streakCount: Int = 0
) 