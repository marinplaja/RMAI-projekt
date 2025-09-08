package com.example.mainquest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mainquest.data.MainQuestDatabase
import com.example.mainquest.data.Task
import com.example.mainquest.data.TaskHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class StatisticsFragment : Fragment() {
    
    private lateinit var totalTasksText: TextView
    private lateinit var completedTasksText: TextView
    private lateinit var totalXpText: TextView
    private lateinit var currentLevelText: TextView
    private lateinit var averageXpText: TextView
    private lateinit var totalRewardsText: TextView
    private lateinit var streakText: TextView
    private lateinit var todayProgressText: TextView
    private lateinit var weeklyProgressText: TextView
    private lateinit var monthlyProgressText: TextView
    private lateinit var completionRateText: TextView
    private lateinit var dailyGoalRateText: TextView
    private lateinit var topCategoryText: TextView
    private lateinit var recentActivityRecyclerView: RecyclerView
    private lateinit var viewReportsButton: Button
    
    private lateinit var gameManager: GameManager
    private var userId: Int = -1
    private lateinit var recentActivityAdapter: RecentActivityAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)
        
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        
        gameManager = GameManager.getInstance(requireContext())
        
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("userId", -1)
        
        loadStatistics()
        
        return view
    }
    
    private fun initViews(view: View) {
        totalTasksText = view.findViewById(R.id.total_tasks_text)
        completedTasksText = view.findViewById(R.id.completed_tasks_text)
        totalXpText = view.findViewById(R.id.total_xp_text)
        currentLevelText = view.findViewById(R.id.current_level_text)
        averageXpText = view.findViewById(R.id.average_xp_text)
        totalRewardsText = view.findViewById(R.id.total_rewards_text)
        streakText = view.findViewById(R.id.streak_text)
        todayProgressText = view.findViewById(R.id.today_progress_text)
        weeklyProgressText = view.findViewById(R.id.weekly_progress_text)
        monthlyProgressText = view.findViewById(R.id.monthly_progress_text)
        completionRateText = view.findViewById(R.id.completion_rate_text)
        dailyGoalRateText = view.findViewById(R.id.daily_goal_rate_text)
        topCategoryText = view.findViewById(R.id.top_category_text)
        recentActivityRecyclerView = view.findViewById(R.id.recent_activity_recycler_view)
        viewReportsButton = view.findViewById(R.id.view_reports_button)
    }
    
    private fun setupRecyclerView() {
        recentActivityAdapter = RecentActivityAdapter(listOf())
        recentActivityRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recentActivityRecyclerView.adapter = recentActivityAdapter
    }
    
    private fun setupClickListeners() {
        viewReportsButton.setOnClickListener {
            val intent = Intent(requireContext(), ReportsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loadStatistics() {
        if (userId == -1) return
        
        CoroutineScope(Dispatchers.IO).launch {
            val db = MainQuestDatabase.getDatabase(requireContext())
            
            // Osnovne statistike
            val user = db.userDao().getById(userId)
            val allTasks = db.taskDao().getByUserId(userId)
            val completedTasks = allTasks.filter { it.isCompleted }
            val unlockedRewards = db.unlockedRewardDao().getByUserId(userId)
            val taskHistory = db.taskHistoryDao().getByUserId(userId)
            
            // Dnevni ciljevi
            val dailyGoals = db.taskDao().getDailyGoalsByUserId(userId)
            val completedDailyGoals = dailyGoals.filter { it.dailyProgress >= it.dailyTarget }
            
            // Datumi
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val weekAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -23) // Total 30 days ago
            val monthAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            
            // Izračunaj statistike
            val level = user?.let { gameManager.calculateLevel(it.xp) } ?: 1
            val averageXp = if (completedTasks.isNotEmpty()) completedTasks.map { it.xpReward }.average() else 0.0
            val maxStreak = if (dailyGoals.isNotEmpty()) dailyGoals.maxOf { it.streakCount } else 0
            
            // Progress po periodima
            val todayCompleted = taskHistory.count { it.completedAt?.startsWith(today) == true }
            val weeklyCompleted = taskHistory.count { it.completedAt?.let { date -> date >= weekAgo } == true }
            val monthlyCompleted = taskHistory.count { it.completedAt?.let { date -> date >= monthAgo } == true }
            
            // Dodatne statistike za prikaz
            val categoryStats = allTasks.groupBy { it.category ?: "Ostalo" }
                .mapValues { (_, tasks) -> tasks.count { it.isCompleted } }
                .toList()
                .sortedByDescending { it.second }
            
            val completionRate = if (allTasks.isNotEmpty()) {
                (completedTasks.size.toDouble() / allTasks.size * 100).toInt()
            } else 0
            
            val dailyGoalCompletionRate = if (dailyGoals.isNotEmpty()) {
                (completedDailyGoals.size.toDouble() / dailyGoals.size * 100).toInt()
            } else 0
            
            // Nedavne aktivnosti (zadnjih 10)
            val recentActivities = taskHistory.sortedByDescending { it.completedAt }.take(10)
            val recentActivitiesWithTasks = recentActivities.mapNotNull { history ->
                allTasks.find { it.id == history.taskId }?.let { task ->
                    RecentActivity(
                        taskName = task.title,
                        completedAt = history.completedAt ?: "",
                        xpEarned = task.xpReward,
                        category = task.category ?: "Ostalo"
                    )
                }
            }
            
            withContext(Dispatchers.Main) {
                // Ažuriraj UI
                totalTasksText.text = allTasks.size.toString()
                completedTasksText.text = completedTasks.size.toString()
                totalXpText.text = "${user?.xp ?: 0} XP"
                currentLevelText.text = "Level $level"
                averageXpText.text = "${averageXp.toInt()} XP"
                totalRewardsText.text = unlockedRewards.size.toString()
                streakText.text = maxStreak.toString()
                todayProgressText.text = "$todayCompleted zadataka"
                weeklyProgressText.text = "$weeklyCompleted zadataka"
                monthlyProgressText.text = "$monthlyCompleted zadataka"
                
                // Dodatne statistike
                completionRateText.text = "$completionRate%"
                dailyGoalRateText.text = "$dailyGoalCompletionRate%"
                topCategoryText.text = if (categoryStats.isNotEmpty()) {
                    "${categoryStats.first().first} (${categoryStats.first().second})"
                } else {
                    "Nema podataka"
                }
                
                recentActivityAdapter.updateActivities(recentActivitiesWithTasks)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadStatistics()
    }

    companion object {
        fun newInstance(): StatisticsFragment {
            return StatisticsFragment()
        }
    }
}

// Data klasa za nedavne aktivnosti
data class RecentActivity(
    val taskName: String,
    val completedAt: String,
    val xpEarned: Int,
    val category: String
) 