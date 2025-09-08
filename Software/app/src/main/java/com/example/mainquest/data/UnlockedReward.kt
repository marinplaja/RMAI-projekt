package com.example.mainquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "unlocked_rewards",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Reward::class,
            parentColumns = ["id"],
            childColumns = ["rewardId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UnlockedReward(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val rewardId: Int,
    val unlockedAt: String?
) 