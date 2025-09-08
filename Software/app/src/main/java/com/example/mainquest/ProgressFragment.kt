package com.example.mainquest

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mainquest.data.MainQuestDatabase
import com.example.mainquest.data.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ProgressFragment : Fragment() {

    private lateinit var categorySpinner: Spinner
    private lateinit var dateDisplay: TextView
    private lateinit var dailyGoalsRecyclerView: RecyclerView
    private lateinit var completedGoalsCount: TextView
    private lateinit var totalGoalsCount: TextView
    private lateinit var streakCount: TextView
    private lateinit var addDailyGoalButton: ImageButton
    
    private lateinit var dailyGoalsAdapter: DailyGoalsAdapter
    private var allDailyGoals: List<Task> = listOf()
    private var filteredDailyGoals: List<Task> = listOf()
    private var categories: List<String> = listOf("Sve")
    private var selectedCategory: String = "Sve"
    private var userId: Int = -1
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd. MMMM yyyy", Locale("hr", "HR"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupSpinner()
        setupClickListeners()
        
        // Dohvati userId iz SharedPreferences
        val sharedPref = requireContext().getSharedPreferences("user_prefs", 0)
        userId = sharedPref.getInt("userId", -1)
        
        // Prikaži današnji datum
        updateDateDisplay()
        
        // Učitaj dnevne ciljeve
        loadDailyGoals()
    }
    
    private fun initViews(view: View) {
        categorySpinner = view.findViewById(R.id.category_spinner)
        dateDisplay = view.findViewById(R.id.date_display)
        dailyGoalsRecyclerView = view.findViewById(R.id.daily_goals_recycler_view)
        completedGoalsCount = view.findViewById(R.id.completed_goals_count)
        totalGoalsCount = view.findViewById(R.id.total_goals_count)
        streakCount = view.findViewById(R.id.streak_count)
        addDailyGoalButton = view.findViewById(R.id.add_daily_goal_button)
    }
    
    private fun setupRecyclerView() {
        dailyGoalsAdapter = DailyGoalsAdapter(
            filteredDailyGoals,
            onCompleteClick = { task -> completeDailyGoal(task) },
            onEditClick = { task -> editDailyGoal(task) },
            onDeleteClick = { task -> deleteDailyGoal(task) }
        )
        
        dailyGoalsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        dailyGoalsRecyclerView.adapter = dailyGoalsAdapter
    }
    
    private fun setupSpinner() {
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter
        
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = parent.getItemAtPosition(position) as String
                filterDailyGoals()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    
    private fun setupClickListeners() {
        addDailyGoalButton.setOnClickListener {
            val intent = Intent(requireContext(), EditTaskActivity::class.java)
            intent.putExtra("is_daily_goal", true)
            startActivity(intent)
        }
    }
    
    private fun updateDateDisplay() {
        val today = Date()
        dateDisplay.text = "Danas: ${displayDateFormat.format(today)}"
    }
    
    private fun loadDailyGoals() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = MainQuestDatabase.getDatabase(requireContext())
            val goals = if (userId != -1) {
                db.taskDao().getDailyGoalsByUserId(userId)
            } else {
                listOf()
            }
            
            // Resetiraj progress ako je novi dan
            resetProgressIfNewDay(goals)
            
            val uniqueCategories = goals.mapNotNull { it.category }.distinct().sorted()
            
            withContext(Dispatchers.Main) {
                allDailyGoals = goals
                categories = listOf("Sve") + uniqueCategories
                updateSpinner()
                filterDailyGoals()
                updateSummary()
            }
        }
    }
    
    private suspend fun resetProgressIfNewDay(goals: List<Task>) {
        val today = dateFormat.format(Date())
        val db = MainQuestDatabase.getDatabase(requireContext())
        
        goals.forEach { goal ->
            if (goal.lastCompletedDate != today && goal.dailyProgress > 0) {
                // Resetiraj dnevni progress ako nije danas
                db.taskDao().updateDailyProgress(goal.id, 0, today)
            }
        }
    }
    
    private fun updateSpinner() {
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter
        
        val selectedIndex = categories.indexOf(selectedCategory)
        if (selectedIndex >= 0) {
            categorySpinner.setSelection(selectedIndex)
        }
    }
    
    private fun filterDailyGoals() {
        filteredDailyGoals = if (selectedCategory == "Sve") {
            allDailyGoals
        } else {
            allDailyGoals.filter { it.category == selectedCategory }
        }
        dailyGoalsAdapter.updateGoals(filteredDailyGoals)
    }
    
    private fun updateSummary() {
        val completed = allDailyGoals.count { it.dailyProgress >= it.dailyTarget }
        val total = allDailyGoals.size
        val maxStreak = if (allDailyGoals.isNotEmpty()) allDailyGoals.maxOf { it.streakCount } else 0
        
        completedGoalsCount.text = completed.toString()
        totalGoalsCount.text = total.toString()
        streakCount.text = maxStreak.toString()
    }
    
    private fun completeDailyGoal(task: Task) {
        if (task.dailyProgress >= task.dailyTarget) {
            Toast.makeText(requireContext(), "Cilj je već završen za danas!", Toast.LENGTH_SHORT).show()
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            val db = MainQuestDatabase.getDatabase(requireContext())
            val today = dateFormat.format(Date())
            val newProgress = task.dailyProgress + 1
            
            // Ažuriraj progress
            db.taskDao().updateDailyProgress(task.id, newProgress, today)
            
            // Ako je cilj završen, ažuriraj streak i dodijeli XP
            if (newProgress >= task.dailyTarget) {
                val newStreak = if (task.lastCompletedDate == yesterday()) {
                    task.streakCount + 1
                } else if (task.lastCompletedDate != today) {
                    1 // Reset streak if not consecutive
                } else {
                    task.streakCount
                }
                db.taskDao().updateStreakCount(task.id, newStreak)
                
                // DODIJELI XP ZA ZAVRŠENI DAILY GOAL
                val user = db.userDao().getById(userId)
                user?.let { currentUser ->
                    val gameManager = GameManager.getInstance(requireContext())
                    val oldLevel = gameManager.calculateLevel(currentUser.xp)
                    val newXp = currentUser.xp + task.xpReward
                    val newLevel = gameManager.calculateLevel(newXp)
                    
                    // Ažuriraj korisnikov XP
                    val updatedUser = currentUser.copy(xp = newXp)
                    db.userDao().update(updatedUser)
                    
                    // Dodaj u TaskHistory
                    val historyDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    val taskHistory = com.example.mainquest.data.TaskHistory(
                        taskId = task.id,
                        userId = userId,
                        completedAt = historyDateFormat.format(java.util.Date())
                    )
                    db.taskHistoryDao().insert(taskHistory)
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "🎉 Dnevni cilj završen! +${task.xpReward} XP | Streak: $newStreak", Toast.LENGTH_LONG).show()
                        
                        // Provjeri level up
                        if (newLevel > oldLevel) {
                            showLevelUpDialog(newLevel)
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "✅ +1 napredak! (${newProgress}/${task.dailyTarget})", Toast.LENGTH_SHORT).show()
                }
            }
            
            withContext(Dispatchers.Main) {
                loadDailyGoals()
            }
        }
    }
    
    private fun showLevelUpDialog(newLevel: Int) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🎉 LEVEL UP! 🎉")
            .setMessage("Čestitamo! Dosegnuli ste Level $newLevel!\n\nNastavi odličan rad!")
            .setPositiveButton("Awesome!") { _, _ -> }
            .setCancelable(false)
            .show()
    }
    
    private fun editDailyGoal(task: Task) {
        val intent = Intent(requireContext(), EditTaskActivity::class.java)
        intent.putExtra("task_id", task.id)
        intent.putExtra("is_daily_goal", true)
        startActivity(intent)
    }
    
    private fun deleteDailyGoal(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("Brisanje dnevnog cilja")
            .setMessage("Jeste li sigurni da želite obrisati ovaj dnevni cilj?")
            .setPositiveButton("Obriši") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    val db = MainQuestDatabase.getDatabase(requireContext())
                    db.taskDao().delete(task)
                    withContext(Dispatchers.Main) {
                        loadDailyGoals()
                        Toast.makeText(requireContext(), "Dnevni cilj obrisan", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Odustani", null)
            .show()
    }
    
    private fun yesterday(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(calendar.time)
    }

    override fun onResume() {
        super.onResume()
        loadDailyGoals()
    }

    companion object {
        fun newInstance(): ProgressFragment {
            return ProgressFragment()
        }
    }
} 