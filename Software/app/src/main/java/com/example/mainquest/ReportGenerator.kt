package com.example.mainquest

import android.content.Context
import com.example.mainquest.data.MainQuestDatabase
import com.example.mainquest.data.Task
import com.example.mainquest.data.TaskHistory
import com.example.mainquest.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ReportGenerator(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("hr", "HR"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    data class DetailedReport(
        val reportDate: String,
        val period: String,
        val userInfo: UserReportInfo,
        val taskSummary: TaskSummary,
        val categoryAnalysis: List<CategoryStats>,
        val progressAnalysis: ProgressAnalysis,
        val streakAnalysis: StreakAnalysis,
        val recommendations: List<String>,
        val detailedActivities: List<ActivityReport>
    )
    
    data class UserReportInfo(
        val username: String,
        val currentLevel: Int,
        val totalXp: Int,
        val xpToNextLevel: Int,
        val joinDate: String
    )
    
    data class TaskSummary(
        val totalTasks: Int,
        val completedTasks: Int,
        val completionRate: Double,
        val totalDailyGoals: Int,
        val completedDailyGoals: Int,
        val dailyGoalRate: Double,
        val totalXpEarned: Int,
        val averageXpPerTask: Double
    )
    
    data class CategoryStats(
        val categoryName: String,
        val totalTasks: Int,
        val completedTasks: Int,
        val completionRate: Double,
        val totalXp: Int,
        val averageXp: Double
    )
    
    data class ProgressAnalysis(
        val todayCompleted: Int,
        val weeklyCompleted: Int,
        val monthlyCompleted: Int,
        val weeklyAverage: Double,
        val monthlyAverage: Double,
        val bestDay: String,
        val bestDayCount: Int
    )
    
    data class StreakAnalysis(
        val currentStreak: Int,
        val longestStreak: Int,
        val streakCategories: Map<String, Int>,
        val streakTrend: String
    )
    
    data class ActivityReport(
        val date: String,
        val time: String,
        val taskName: String,
        val category: String,
        val xpEarned: Int,
        val taskType: String
    )
    
    suspend fun generateDetailedReport(userId: Int, period: String = "Sveukupno"): DetailedReport {
        return withContext(Dispatchers.IO) {
            val db = MainQuestDatabase.getDatabase(context)
            
            // Dohvati osnovne podatke
            val user = db.userDao().getById(userId) ?: throw Exception("Korisnik nije pronađen")
            val allTasks = db.taskDao().getByUserId(userId)
            val taskHistory = db.taskHistoryDao().getByUserId(userId)
            val unlockedRewards = db.unlockedRewardDao().getByUserId(userId)
            
            // Filtriraj podatke po periodu
            val filteredHistory = filterHistoryByPeriod(taskHistory, period)
            val filteredTasks = allTasks.filter { task ->
                filteredHistory.any { it.taskId == task.id }
            }
            
            // Generiraj izvještaj
            DetailedReport(
                reportDate = displayDateFormat.format(Date()),
                period = period,
                userInfo = generateUserInfo(user),
                taskSummary = generateTaskSummary(allTasks, filteredTasks, filteredHistory),
                categoryAnalysis = generateCategoryAnalysis(allTasks, filteredHistory),
                progressAnalysis = generateProgressAnalysis(filteredHistory),
                streakAnalysis = generateStreakAnalysis(allTasks),
                recommendations = generateRecommendations(allTasks, filteredHistory),
                detailedActivities = generateDetailedActivities(filteredHistory, allTasks)
            )
        }
    }
    
    private fun filterHistoryByPeriod(history: List<TaskHistory>, period: String): List<TaskHistory> {
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        
        return when (period) {
            "Danas" -> history.filter { it.completedAt?.startsWith(today) == true }
            "Ovaj tjedan" -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val weekStart = dateFormat.format(calendar.time)
                history.filter { it.completedAt?.let { date -> date >= weekStart } == true }
            }
            "Ovaj mjesec" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val monthStart = dateFormat.format(calendar.time)
                history.filter { it.completedAt?.let { date -> date >= monthStart } == true }
            }
            "Zadnjih 7 dana" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = dateFormat.format(calendar.time)
                history.filter { it.completedAt?.let { date -> date >= weekAgo } == true }
            }
            "Zadnjih 30 dana" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val monthAgo = dateFormat.format(calendar.time)
                history.filter { it.completedAt?.let { date -> date >= monthAgo } == true }
            }
            else -> history // "Sveukupno"
        }
    }
    
    private fun generateUserInfo(user: User): UserReportInfo {
        val gameManager = GameManager.getInstance(context)
        val currentLevel = gameManager.calculateLevel(user.xp)
        val xpForNext = gameManager.getXpForNextLevel(user.xp)
        
        return UserReportInfo(
            username = user.username,
            currentLevel = currentLevel,
            totalXp = user.xp,
            xpToNextLevel = xpForNext - user.xp,
            joinDate = "Registriran korisnik"
        )
    }
    
    private fun generateTaskSummary(allTasks: List<Task>, filteredTasks: List<Task>, history: List<TaskHistory>): TaskSummary {
        val completedTasks = allTasks.filter { it.isCompleted }
        val dailyGoals = allTasks.filter { it.isDailyGoal }
        val completedDailyGoals = dailyGoals.filter { it.dailyProgress >= it.dailyTarget }
        
        val totalXpFromHistory = history.mapNotNull { h ->
            allTasks.find { it.id == h.taskId }?.xpReward
        }.sum()
        
        return TaskSummary(
            totalTasks = allTasks.size,
            completedTasks = completedTasks.size,
            completionRate = if (allTasks.isNotEmpty()) completedTasks.size.toDouble() / allTasks.size * 100 else 0.0,
            totalDailyGoals = dailyGoals.size,
            completedDailyGoals = completedDailyGoals.size,
            dailyGoalRate = if (dailyGoals.isNotEmpty()) completedDailyGoals.size.toDouble() / dailyGoals.size * 100 else 0.0,
            totalXpEarned = totalXpFromHistory,
            averageXpPerTask = if (history.isNotEmpty()) totalXpFromHistory.toDouble() / history.size else 0.0
        )
    }
    
    private fun generateCategoryAnalysis(allTasks: List<Task>, history: List<TaskHistory>): List<CategoryStats> {
        return allTasks.groupBy { it.category ?: "Ostalo" }
            .map { (category, tasks) ->
                val completedInCategory = tasks.filter { it.isCompleted }
                val historyInCategory = history.filter { h -> tasks.any { it.id == h.taskId } }
                val xpInCategory = historyInCategory.mapNotNull { h ->
                    tasks.find { it.id == h.taskId }?.xpReward
                }.sum()
                
                CategoryStats(
                    categoryName = category,
                    totalTasks = tasks.size,
                    completedTasks = completedInCategory.size,
                    completionRate = if (tasks.isNotEmpty()) completedInCategory.size.toDouble() / tasks.size * 100 else 0.0,
                    totalXp = xpInCategory,
                    averageXp = if (historyInCategory.isNotEmpty()) xpInCategory.toDouble() / historyInCategory.size else 0.0
                )
            }
            .sortedByDescending { it.completedTasks }
    }
    
    private fun generateProgressAnalysis(history: List<TaskHistory>): ProgressAnalysis {
        val today = dateFormat.format(Date())
        val calendar = Calendar.getInstance()
        
        // Danas
        val todayCompleted = history.count { it.completedAt?.startsWith(today) == true }
        
        // Ovaj tjedan
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val weekStart = dateFormat.format(calendar.time)
        val weeklyCompleted = history.count { it.completedAt?.let { date -> date >= weekStart } == true }
        
        // Ovaj mjesec
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = dateFormat.format(calendar.time)
        val monthlyCompleted = history.count { it.completedAt?.let { date -> date >= monthStart } == true }
        
        // Najbolji dan
        val dailyStats = history.groupBy { it.completedAt?.substring(0, 10) }
            .mapValues { it.value.size }
            .maxByOrNull { it.value }
        
        return ProgressAnalysis(
            todayCompleted = todayCompleted,
            weeklyCompleted = weeklyCompleted,
            monthlyCompleted = monthlyCompleted,
            weeklyAverage = weeklyCompleted / 7.0,
            monthlyAverage = monthlyCompleted / 30.0,
            bestDay = dailyStats?.key?.let { 
                try {
                    displayDateFormat.format(dateFormat.parse(it))
                } catch (e: Exception) { it }
            } ?: "Nema podataka",
            bestDayCount = dailyStats?.value ?: 0
        )
    }
    
    private fun generateStreakAnalysis(allTasks: List<Task>): StreakAnalysis {
        val dailyGoals = allTasks.filter { it.isDailyGoal }
        val currentStreak = dailyGoals.maxOfOrNull { it.streakCount } ?: 0
        val longestStreak = dailyGoals.maxOfOrNull { it.streakCount } ?: 0
        
        val streakCategories = dailyGoals.groupBy { it.category ?: "Ostalo" }
            .mapValues { (_, tasks) -> tasks.maxOfOrNull { it.streakCount } ?: 0 }
        
        val trend = when {
            currentStreak >= longestStreak * 0.8 -> "Odličan trend!"
            currentStreak >= longestStreak * 0.5 -> "Dobar trend"
            currentStreak > 0 -> "Umjeren trend"
            else -> "Potrebno poboljšanje"
        }
        
        return StreakAnalysis(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            streakCategories = streakCategories,
            streakTrend = trend
        )
    }
    
    private fun generateRecommendations(allTasks: List<Task>, history: List<TaskHistory>): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Analiza aktivnosti
        if (history.isEmpty()) {
            recommendations.add("🎯 Počnite s prvim zadatkom da vidite svoj napredak!")
            return recommendations
        }
        
        // Analiza kategorija
        val categoryStats = allTasks.groupBy { it.category ?: "Ostalo" }
            .mapValues { (_, tasks) -> tasks.count { it.isCompleted } }
        
        val leastActiveCategory = categoryStats.minByOrNull { it.value }
        if (leastActiveCategory != null && leastActiveCategory.value == 0) {
            recommendations.add("📚 Pokušajte dodati zadatke u kategoriju '${leastActiveCategory.key}'")
        }
        
        // Analiza frekvencije
        val recentDays = history.count { 
            try {
                val completedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it.completedAt ?: "")
                val daysDiff = ((Date().time - (completedDate?.time ?: 0)) / (1000 * 60 * 60 * 24))
                daysDiff <= 3
            } catch (e: Exception) {
                false
            }
        }
        
        if (recentDays < 3) {
            recommendations.add("⚡ Pokušajte biti aktivniji - cilj je barem jedan zadatak dnevno!")
        }
        
        // Analiza XP
        val xpValues = history.mapNotNull { h ->
            allTasks.find { it.id == h.taskId }?.xpReward
        }
        val avgXp = if (xpValues.isNotEmpty()) xpValues.average() else 0.0
        
        if (avgXp < 50) {
            recommendations.add("🚀 Dodajte zahtjevnije zadatke za više XP bodova!")
        }
        
        // Pozitivne poruke
        if (history.size >= 10) {
            recommendations.add("🏆 Odličan rad! Završili ste ${history.size} zadataka!")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("✨ Nastavi odličan rad! Vaš napredak je impresivan!")
        }
        
        return recommendations
    }
    
    private fun generateDetailedActivities(history: List<TaskHistory>, allTasks: List<Task>): List<ActivityReport> {
        return history.sortedByDescending { it.completedAt }
            .take(50) // Ograniči na zadnjih 50 aktivnosti
            .mapNotNull { h ->
                allTasks.find { it.id == h.taskId }?.let { task ->
                    val completedAt = h.completedAt ?: ""
                    val date = try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(completedAt)
                        displayDateFormat.format(parsed)
                    } catch (e: Exception) {
                        completedAt.substring(0, 10)
                    }
                    
                    val time = try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(completedAt)
                        timeFormat.format(parsed)
                    } catch (e: Exception) {
                        "N/A"
                    }
                    
                    ActivityReport(
                        date = date,
                        time = time,
                        taskName = task.title,
                        category = task.category ?: "Ostalo",
                        xpEarned = task.xpReward,
                        taskType = if (task.isDailyGoal) "Dnevni cilj" else "Navika"
                    )
                }
            }
    }
} 