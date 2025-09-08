package com.example.mainquest

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.mainquest.data.MainQuestDatabase
import com.example.mainquest.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class GameManager private constructor(private val context: Context) {
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    companion object {
        @Volatile
        private var INSTANCE: GameManager? = null
        
        fun getInstance(context: Context): GameManager {
            return INSTANCE ?: synchronized(this) {
                val instance = GameManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    data class GameStats(
        val totalSpins: Int,
        val totalXpWon: Int,
        val lastPlayDate: String?,
        val streakDays: Int,
        val bestSpin: String,
        val totalBonusTurns: Int
    )
    
    fun calculateLevel(xp: Int): Int {
        return when {
            xp < 100 -> 1
            xp < 250 -> 2
            xp < 500 -> 3
            xp < 1000 -> 4
            xp < 2000 -> 5
            else -> 6
        }
    }
    
    fun getXpForNextLevel(currentXp: Int): Int {
        val currentLevel = calculateLevel(currentXp)
        return when (currentLevel) {
            1 -> 100
            2 -> 250
            3 -> 500
            4 -> 1000
            5 -> 2000
            else -> currentXp
        }
    }
    
    fun getXpProgress(currentXp: Int): Float {
        val currentLevel = calculateLevel(currentXp)
        val previousLevelXp = when (currentLevel) {
            1 -> 0
            2 -> 100
            3 -> 250
            4 -> 500
            5 -> 1000
            else -> 2000
        }
        val nextLevelXp = getXpForNextLevel(currentXp)
        
        if (currentLevel >= 6) return 1.0f
        
        val progress = (currentXp - previousLevelXp).toFloat() / (nextLevelXp - previousLevelXp)
        return progress.coerceIn(0f, 1f)
    }
    
    fun canPlayWheel(userId: Int): Boolean {
        val today = dateFormat.format(Date())
        val dailySpinsUsed = sharedPrefs.getInt("daily_spins_${userId}_$today", 0)
        val maxDailySpins = 5
        
        return dailySpinsUsed < maxDailySpins
    }
    
    fun getRemainingSpins(userId: Int): Int {
        val today = dateFormat.format(Date())
        val dailySpinsUsed = sharedPrefs.getInt("daily_spins_${userId}_$today", 0)
        val maxDailySpins = 5
        return (maxDailySpins - dailySpinsUsed).coerceAtLeast(0)
    }
    
    fun recordWheelSpin(userId: Int, result: String, xpWon: Int) {
        val today = dateFormat.format(Date())
        val currentSpins = sharedPrefs.getInt("daily_spins_${userId}_$today", 0)
        
        with(sharedPrefs.edit()) {
            putInt("daily_spins_${userId}_$today", currentSpins + 1)
            putString("last_wheel_play_$userId", today)
            putInt("total_spins_$userId", sharedPrefs.getInt("total_spins_$userId", 0) + 1)
            putInt("total_xp_won_$userId", sharedPrefs.getInt("total_xp_won_$userId", 0) + xpWon)
            
            val currentBest = sharedPrefs.getString("best_spin_$userId", "5 XP")
            if (isBetterSpin(result, currentBest ?: "5 XP")) {
                putString("best_spin_$userId", result)
            }
            
            if (result == "Bonus Turn") {
                putInt("total_bonus_turns_$userId", sharedPrefs.getInt("total_bonus_turns_$userId", 0) + 1)
            }
            
            apply()
        }
        
        updateStreak(userId)
        Log.d("GameManager", "User $userId recorded wheel spin: $result, XP: $xpWon")
    }
    
    private fun isBetterSpin(newResult: String, currentBest: String): Boolean {
        val newValue = extractXpValue(newResult)
        val currentValue = extractXpValue(currentBest)
        return newValue > currentValue
    }
    
    private fun extractXpValue(result: String): Int {
        return when {
            result.contains("XP") -> result.filter { it.isDigit() }.toIntOrNull() ?: 0
            result == "Lucky Day!" -> 50
            result == "Bonus Turn" -> 25
            else -> 0
        }
    }
    
    private fun updateStreak(userId: Int) {
        val today = dateFormat.format(Date())
        val lastPlayDate = sharedPrefs.getString("last_streak_date_$userId", null)
        val currentStreak = sharedPrefs.getInt("streak_days_$userId", 0)
        
        val newStreak = if (lastPlayDate == null) {
            1
        } else {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = dateFormat.format(calendar.time)
            
            when {
                lastPlayDate == today -> currentStreak
                lastPlayDate == yesterday -> currentStreak + 1
                else -> 1
            }
        }
        
        with(sharedPrefs.edit()) {
            putInt("streak_days_$userId", newStreak)
            putString("last_streak_date_$userId", today)
            apply()
        }
    }
    
    fun getGameStats(userId: Int): GameStats {
        return GameStats(
            totalSpins = sharedPrefs.getInt("total_spins_$userId", 0),
            totalXpWon = sharedPrefs.getInt("total_xp_won_$userId", 0),
            lastPlayDate = sharedPrefs.getString("last_wheel_play_$userId", null),
            streakDays = sharedPrefs.getInt("streak_days_$userId", 0),
            bestSpin = sharedPrefs.getString("best_spin_$userId", "None") ?: "None",
            totalBonusTurns = sharedPrefs.getInt("total_bonus_turns_$userId", 0)
        )
    }
    
    fun checkAchievements(user: User): List<String> {
        val achievements = mutableListOf<String>()
        val stats = getGameStats(user.id)
        
        when {
            stats.totalSpins >= 100 -> achievements.add("🎡 Spin Master - 100 spins!")
            stats.totalSpins >= 50 -> achievements.add("🎲 Wheel Warrior - 50 spins!")
            stats.totalSpins >= 10 -> achievements.add("🎯 Beginner Spinner - 10 spins!")
        }
        
        when {
            user.xp >= 1000 -> achievements.add("⭐ XP Legend - 1000+ XP!")
            user.xp >= 500 -> achievements.add("💎 XP Expert - 500+ XP!")
            user.xp >= 100 -> achievements.add("🏆 XP Novice - 100+ XP!")
        }
        
        when {
            stats.streakDays >= 7 -> achievements.add("🔥 Week Warrior - 7 day streak!")
            stats.streakDays >= 3 -> achievements.add("📅 Daily Player - 3 day streak!")
        }
        
        return achievements
    }
    
    fun performDailyReset() {
        val today = dateFormat.format(Date())
        val lastResetDate = sharedPrefs.getString("last_reset_date", null)
        
        if (lastResetDate != today) {
            with(sharedPrefs.edit()) {
                putString("last_reset_date", today)
                apply()
            }
            Log.d("GameManager", "Daily reset performed for $today")
        }
    }
} 