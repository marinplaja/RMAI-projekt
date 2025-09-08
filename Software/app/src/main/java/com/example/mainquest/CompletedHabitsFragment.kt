package com.example.mainquest

import android.content.Context
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
import com.example.mainquest.data.TaskHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CompletedHabitsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var summaryText: TextView
    private lateinit var filterSpinner: Spinner
    private lateinit var completedHistoryAdapter: CompletedHistoryAdapter
    
    private var allHistory: List<CompletedTaskItem> = listOf()
    private var filteredHistory: List<CompletedTaskItem> = listOf()
    private var userId: Int = -1
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("hr", "HR"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_completed_habits, container, false)
        
        initViews(view)
        setupRecyclerView()
        setupSpinner()
        
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("userId", -1)
        
        loadCompletedHistory()
        
        return view
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.completed_habits_recycler_view)
        emptyText = view.findViewById(R.id.completed_habits_empty_text)
        summaryText = view.findViewById(R.id.completed_summary_text)
        filterSpinner = view.findViewById(R.id.completed_filter_spinner)
    }
    
    private fun setupRecyclerView() {
        completedHistoryAdapter = CompletedHistoryAdapter(filteredHistory)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = completedHistoryAdapter
    }
    
    private fun setupSpinner() {
        val filterOptions = listOf(
            "Svi zadaci",
            "Danas", 
            "Ovaj tjedan",
            "Ovaj mjesec",
            "Zadnjih 7 dana",
            "Zadnjih 30 dana"
        )
        
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = spinnerAdapter
        
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedFilter = parent.getItemAtPosition(position) as String
                filterHistory(selectedFilter)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadCompletedHistory() {
        if (userId == -1) return
        
        CoroutineScope(Dispatchers.IO).launch {
            val db = MainQuestDatabase.getDatabase(requireContext())
            
            // Dohvati povijest zadataka
            val taskHistory = db.taskHistoryDao().getByUserId(userId)
            val allTasks = db.taskDao().getByUserId(userId).associateBy { it.id }
            
            // Kreiraj listu završenih zadataka s detaljima
            val completedItems = taskHistory.mapNotNull { history ->
                allTasks[history.taskId]?.let { task ->
                    CompletedTaskItem(
                        taskId = task.id,
                        title = task.title,
                        category = task.category ?: "Ostalo",
                        xpEarned = task.xpReward,
                        completedAt = history.completedAt ?: "",
                        isDailyGoal = task.isDailyGoal
                    )
                }
            }.sortedByDescending { it.completedAt }
            
            withContext(Dispatchers.Main) {
                allHistory = completedItems
                filterHistory("Svi zadaci") // Početni filter
                updateSummary()
            }
        }
    }
    
    private fun filterHistory(filter: String) {
        val today = Calendar.getInstance()
        val calendar = Calendar.getInstance()
        
        filteredHistory = when (filter) {
            "Danas" -> {
                val todayStr = dateFormat.format(today.time)
                allHistory.filter { it.completedAt.startsWith(todayStr) }
            }
            "Ovaj tjedan" -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val weekStart = dateFormat.format(calendar.time)
                allHistory.filter { it.completedAt >= weekStart }
            }
            "Ovaj mjesec" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val monthStart = dateFormat.format(calendar.time)
                allHistory.filter { it.completedAt >= monthStart }
            }
            "Zadnjih 7 dana" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = dateFormat.format(calendar.time)
                allHistory.filter { it.completedAt >= weekAgo }
            }
            "Zadnjih 30 dana" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val monthAgo = dateFormat.format(calendar.time)
                allHistory.filter { it.completedAt >= monthAgo }
            }
            else -> allHistory // "Svi zadaci"
        }
        
        completedHistoryAdapter.updateHistory(filteredHistory)
        
        // Ažuriraj prikaz
        emptyText.visibility = if (filteredHistory.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (filteredHistory.isEmpty()) View.GONE else View.VISIBLE
        
        // Ažuriraj tekst za prazan prikaz
        emptyText.text = if (allHistory.isEmpty()) {
            "Još nema završenih zadataka.\nZavršite prvi zadatak da vidite povijest!"
        } else {
            "Nema zadataka za odabrani period."
        }
    }
    
    private fun updateSummary() {
        val totalCompleted = allHistory.size
        val totalXp = allHistory.sumOf { it.xpEarned }
        val regularTasks = allHistory.count { !it.isDailyGoal }
        val dailyGoals = allHistory.count { it.isDailyGoal }
        
        val summaryStr = "📊 SAŽETAK: $totalCompleted završenih zadataka | ⭐ $totalXp XP | 🎯 $regularTasks navika | 📅 $dailyGoals dnevnih ciljeva"
        summaryText.text = summaryStr
    }
    
    override fun onResume() {
        super.onResume()
        loadCompletedHistory()
    }
}

// Data klasa za završene zadatke
data class CompletedTaskItem(
    val taskId: Int,
    val title: String,
    val category: String,
    val xpEarned: Int,
    val completedAt: String,
    val isDailyGoal: Boolean
) 