package com.example.mainquest

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.AlertDialog
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mainquest.data.MainQuestDatabase
import com.example.mainquest.data.Task
import com.example.mainquest.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.content.SharedPreferences
import android.util.Log
import kotlin.math.max

class TasksFragment : Fragment() {

    private lateinit var habitsAdapter: HabitsAdapter
    private lateinit var recyclerView: RecyclerView
    private var tasks: List<Task> = listOf()
    private lateinit var categoryFilterSpinner: Spinner
    private var allTasks: List<Task> = listOf()
    private var categories: List<String> = listOf()
    private var selectedCategory: String = "Sve"
    private var userId: Int = -1
    private lateinit var gameManager: GameManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)
        recyclerView = view.findViewById(R.id.habits_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Inicijaliziraj GameManager
        gameManager = GameManager.getInstance(requireContext())
        
        habitsAdapter = HabitsAdapter(tasks,
            onEditClick = { task ->
                val intent = Intent(requireContext(), EditTaskActivity::class.java)
                intent.putExtra("task_id", task.id)
                startActivity(intent)
            },
            onDeleteClick = { task ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Brisanje navike")
                    .setMessage("Jeste li sigurni da želite obrisati ovu naviku?")
                    .setPositiveButton("Obriši") { _, _ ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val db = MainQuestDatabase.getDatabase(requireContext())
                            db.taskDao().delete(task)
                            withContext(Dispatchers.Main) { loadTasks() }
                        }
                    }
                    .setNegativeButton("Odustani", null)
                    .show()
            },
            onCompleteClick = { task ->
                completeTask(task)
            }
        )
        recyclerView.adapter = habitsAdapter
        loadTasks()

        val addHabitButton: View = view.findViewById(R.id.add_habit_button)
        addHabitButton.setOnClickListener {
            val intent = Intent(requireContext(), EditTaskActivity::class.java)
            startActivity(intent)
        }

        categoryFilterSpinner = view.findViewById(R.id.category_filter_spinner)
        val initialCategories = listOf("Sve")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, initialCategories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryFilterSpinner.adapter = spinnerAdapter
        categoryFilterSpinner.setSelection(0)
        categoryFilterSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = parent.getItemAtPosition(position) as String
                filterTasks()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })

        val sharedPref = requireContext().getSharedPreferences("user_prefs", 0)
        userId = sharedPref.getInt("userId", -1)

        return view
    }
    
    private fun completeTask(task: Task) {
        Log.d("TasksFragment", "completeTask called for: ${task.title}, current XP reward: ${task.xpReward}")
        
        CoroutineScope(Dispatchers.IO).launch {
            val db = MainQuestDatabase.getDatabase(requireContext())
            
            // Osiguraj da zadatak ima XP nagradu
            val taskWithXp = if (task.xpReward == 0) {
                val newXp = calculateXpReward(task.category, task.isDailyGoal, task.dailyTarget)
                val updatedTask = task.copy(xpReward = newXp)
                db.taskDao().update(updatedTask)
                Log.d("TasksFragment", "Updated task ${task.title} with XP: $newXp")
                updatedTask
            } else {
                task
            }
            
            // Ažuriraj zadatak kao završen/nezavršen
            val updatedTask = taskWithXp.copy(isCompleted = !taskWithXp.isCompleted)
            db.taskDao().update(updatedTask)
            Log.d("TasksFragment", "Task updated: ${updatedTask.title}, completed: ${updatedTask.isCompleted}")
            
            // Dohvati korisnika
            val user = db.userDao().getById(userId)
            Log.d("TasksFragment", "User before XP update: ${user?.xp}")
            
            user?.let { currentUser ->
                val oldLevel = gameManager.calculateLevel(currentUser.xp)
                var newXp = currentUser.xp
                
                if (updatedTask.isCompleted && !taskWithXp.isCompleted) {
                    // Zadatak je označen kao završen - dodijeli XP
                    newXp = currentUser.xp + taskWithXp.xpReward
                    Log.d("TasksFragment", "Adding XP: ${currentUser.xp} + ${taskWithXp.xpReward} = $newXp")
                } else if (!updatedTask.isCompleted && taskWithXp.isCompleted) {
                    // Zadatak je poništen - oduzmi XP
                    newXp = max(0, currentUser.xp - taskWithXp.xpReward)
                    Log.d("TasksFragment", "Removing XP: ${currentUser.xp} - ${taskWithXp.xpReward} = $newXp")
                }
                
                val newLevel = gameManager.calculateLevel(newXp)
                Log.d("TasksFragment", "Level change: $oldLevel -> $newLevel")
                
                // Ažuriraj korisnikov XP u bazi
                val updatedUser = currentUser.copy(xp = newXp)
                db.userDao().update(updatedUser)
                Log.d("TasksFragment", "User updated with new XP: ${updatedUser.xp}")
                
                                    // Dodaj u TaskHistory ako je zadatak završen
                    if (updatedTask.isCompleted && !taskWithXp.isCompleted) {
                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val taskHistory = com.example.mainquest.data.TaskHistory(
                            taskId = updatedTask.id,
                            userId = userId,
                            completedAt = dateFormat.format(java.util.Date())
                        )
                        db.taskHistoryDao().insert(taskHistory)
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (updatedTask.isCompleted && !taskWithXp.isCompleted) {
                            if (taskWithXp.xpReward > 0) {
                                Toast.makeText(requireContext(), "🎯 Zadatak završen! +${taskWithXp.xpReward} XP", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "✅ Zadatak završen!", Toast.LENGTH_SHORT).show()
                            }
                            
                            // Provjeri je li korisnik prešao na novi level
                            if (newLevel > oldLevel) {
                                showLevelUpDialog(newLevel)
                                checkForLevelRewards(newLevel)
                            }
                        } else if (!updatedTask.isCompleted && taskWithXp.isCompleted) {
                            Toast.makeText(requireContext(), "❌ Zadatak poništen! -${taskWithXp.xpReward} XP", Toast.LENGTH_SHORT).show()
                        }
                        
                        loadTasks()
                    }
            }
        }
    }
    
    private fun showLevelUpDialog(newLevel: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("🎉 LEVEL UP! 🎉")
            .setMessage("Čestitamo! Dosegnuli ste Level $newLevel!\n\nNastavi odličan rad!")
            .setPositiveButton("Awesome!") { _, _ -> }
            .setCancelable(false)
            .show()
    }
    
    private fun checkForLevelRewards(level: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = MainQuestDatabase.getDatabase(requireContext())
            
            // Definiraj nagrade po levelima
            val levelRewards = mapOf(
                2 to "Bonus avatar",
                3 to "Zlatni skin", 
                4 to "Epic theme",
                5 to "Legend status",
                6 to "Master title"
            )
            
            levelRewards[level]?.let { rewardName ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "🏆 Nova nagrada otključana: $rewardName!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadTasks() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = MainQuestDatabase.getDatabase(requireContext())
            val tasksFromDb = if (userId != -1) db.taskDao().getByUserId(userId) else listOf()
            
            // Filtriraj da uzmeš samo obične zadatke (ne dnevne ciljeve)
            val regularTasks = tasksFromDb.filter { !it.isDailyGoal }
            
            // Ažuriraj zadatke koji nemaju XP nagrade
            val updatedTasks = regularTasks.map { task ->
                if (task.xpReward == 0) {
                    val newXp = calculateXpReward(task.category, task.isDailyGoal, task.dailyTarget)
                    val updatedTask = task.copy(xpReward = newXp)
                    db.taskDao().update(updatedTask)
                    Log.d("TasksFragment", "Updated task ${task.title} with XP: $newXp")
                    updatedTask
                } else {
                    task
                }
            }
            
            val uniqueCategories = updatedTasks.mapNotNull { it.category }.distinct().sorted()
            withContext(Dispatchers.Main) {
                allTasks = updatedTasks
                categories = listOf("Sve") + uniqueCategories
                val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categoryFilterSpinner.adapter = spinnerAdapter
                filterTasks()
                
                Log.d("TasksFragment", "Loaded ${updatedTasks.size} regular tasks (non-daily goals)")
            }
        }
    }
    
    private fun calculateXpReward(category: String?, isDailyGoal: Boolean, dailyTarget: Int): Int {
        val baseXp = when (category?.lowercase()?.trim()) {
            // Učenje i obrazovanje
            "učenje", "learning", "education", "study", "studiranje" -> 60
            "čitanje", "reading", "books", "knjige" -> 50
            
            // Fitness i zdravlje
            "fitness", "zdravlje", "health", "sport", "vježbanje" -> 55
            "trčanje", "running", "jogging" -> 50
            "teretana", "gym", "workout" -> 45
            
            // Posao i karijera
            "posao", "work", "career", "karijera", "business" -> 70
            "projekt", "project", "programming", "programiranje" -> 65
            
            // Osobni razvoj
            "osobni razvoj", "personal development", "self improvement" -> 55
            "meditacija", "meditation", "mindfulness" -> 40
            "planiranje", "planning", "organization" -> 35
            
            // Hobiji i kreativnost
            "hobiji", "hobbies", "creative", "kreativnost" -> 35
            "glazba", "music", "instrument" -> 40
            "crtanje", "drawing", "art", "umjetnost" -> 35
            
            // Kućanski poslovi
            "kućanski poslovi", "household", "cleaning", "čišćenje" -> 30
            "kuhanje", "cooking", "food prep" -> 35
            "vrtlarstvo", "gardening" -> 30
            
            // Socijalni i obiteljski
            "obitelj", "family", "friends", "prijatelji" -> 40
            "volontiranje", "volunteering", "community" -> 50
            
            // Ostalo
            "home", "dom", "kuća" -> 30
            else -> 40 // Povećao default vrijednost
        }
        
        return if (isDailyGoal) {
            // Dnevni ciljevi nose manje XP po jedinici, ali mogu se ponavljati
            (baseXp * 0.7).toInt()
        } else {
            baseXp
        }
    }

    private fun filterTasks() {
        val filtered = if (selectedCategory == "Sve") allTasks else allTasks.filter { it.category == selectedCategory }
        habitsAdapter.updateHabits(filtered)
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    companion object {
        fun newInstance(): TasksFragment {
            return TasksFragment()
        }
    }
} 