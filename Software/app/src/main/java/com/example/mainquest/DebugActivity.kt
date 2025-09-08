package com.example.mainquest

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mainquest.data.MainQuestDatabase
import com.example.mainquest.data.Task
import com.example.mainquest.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DebugActivity : AppCompatActivity() {
    
    private lateinit var debugTextView: TextView
    private lateinit var checkUsersButton: Button
    private lateinit var createUserButton: Button
    private lateinit var checkTasksButton: Button
    private lateinit var addTasksButton: Button
    private lateinit var testXpButton: Button
    private lateinit var updateTasksButton: Button
    private lateinit var createRewardsButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        
        debugTextView = findViewById(R.id.debug_text_view)
        checkUsersButton = findViewById(R.id.check_users_button)
        createUserButton = findViewById(R.id.create_user_button)
        checkTasksButton = findViewById(R.id.check_tasks_button)
        addTasksButton = findViewById(R.id.add_tasks_button)
        testXpButton = findViewById(R.id.test_xp_button)
        updateTasksButton = findViewById(R.id.update_tasks_button)
        createRewardsButton = findViewById(R.id.create_rewards_button)
        
        checkUsersButton.setOnClickListener {
            checkUsers()
        }
        
        createUserButton.setOnClickListener {
            createTestUser()
        }
        
        checkTasksButton.setOnClickListener {
            checkTasks()
        }
        
        addTasksButton.setOnClickListener {
            addSampleTasks()
        }
        
        testXpButton.setOnClickListener {
            testXpSystem()
        }
        
        updateTasksButton.setOnClickListener {
            updateExistingTasks()
        }
        
        createRewardsButton.setOnClickListener {
            createRewards()
        }
    }
    
    private fun checkUsers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MainQuestDatabase.getDatabase(applicationContext)
                val users = db.userDao().getAll()
                
                withContext(Dispatchers.Main) {
                    val userInfo = StringBuilder()
                    userInfo.append("KORISNICI U BAZI:\n")
                    if (users.isEmpty()) {
                        userInfo.append("Nema korisnika u bazi!\n")
                    } else {
                        users.forEach { user ->
                            userInfo.append("ID: ${user.id}, Username: ${user.username}, Email: ${user.email}\n")
                        }
                    }
                    
                    val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val currentUserId = sharedPref.getInt("userId", -1)
                    userInfo.append("\nTrenutni userId iz SharedPrefs: $currentUserId\n")
                    
                    debugTextView.text = userInfo.toString()
                    Log.d("DebugActivity", userInfo.toString())
                }
            } catch (e: Exception) {
                Log.e("DebugActivity", "Greška pri provjeri korisnika: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    debugTextView.text = "Greška: ${e.message}"
                }
            }
        }
    }
    
    private fun createTestUser() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MainQuestDatabase.getDatabase(applicationContext)
                val testUser = User(
                    username = "debug_user",
                    email = "debug@test.com",
                    password = "debug123",
                    avatar = null,
                    xp = 0
                )
                
                val userId = db.userDao().insert(testUser)
                
                // Spremi u SharedPreferences
                val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putInt("userId", userId.toInt())
                editor.apply()
                
                withContext(Dispatchers.Main) {
                    debugTextView.text = "Kreiran novi korisnik s ID: $userId\nSpravljeno u SharedPrefs"
                    Log.d("DebugActivity", "Kreiran novi korisnik s ID: $userId")
                }
            } catch (e: Exception) {
                Log.e("DebugActivity", "Greška pri kreiranju korisnika: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    debugTextView.text = "Greška pri kreiranju: ${e.message}"
                }
            }
        }
    }
    
    private fun checkTasks() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MainQuestDatabase.getDatabase(applicationContext)
                val allTasks = db.taskDao().getAll()
                
                withContext(Dispatchers.Main) {
                    val taskInfo = StringBuilder()
                    taskInfo.append("ZADACI U BAZI:\n")
                    if (allTasks.isEmpty()) {
                        taskInfo.append("Nema zadataka u bazi!\n")
                    } else {
                        allTasks.forEach { task ->
                            taskInfo.append("ID: ${task.id}, Naziv: ${task.title}, UserID: ${task.userId}\n")
                        }
                    }
                    
                    debugTextView.text = taskInfo.toString()
                    Log.d("DebugActivity", taskInfo.toString())
                }
            } catch (e: Exception) {
                Log.e("DebugActivity", "Greška pri provjeri zadataka: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    debugTextView.text = "Greška: ${e.message}"
                }
            }
        }
    }
    
    private fun testXpSystem() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPref.getInt("userId", -1)
        
        if (userId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = MainQuestDatabase.getDatabase(applicationContext)
                val user = db.userDao().getById(userId)
                
                user?.let { currentUser ->
                    val newXp = currentUser.xp + 100
                    val updatedUser = currentUser.copy(xp = newXp)
                    db.userDao().update(updatedUser)
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DebugActivity, "Test: Dodano 100 XP! Ukupno: ${updatedUser.xp}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun updateExistingTasks() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPref.getInt("userId", -1)
        
        if (userId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = MainQuestDatabase.getDatabase(applicationContext)
                val tasks = db.taskDao().getByUserId(userId)
                var updatedCount = 0
                
                tasks.forEach { task ->
                    val newXp = calculateXpReward(task.category, task.isDailyGoal, task.dailyTarget)
                    if (task.xpReward != newXp) {
                        val updatedTask = task.copy(xpReward = newXp)
                        db.taskDao().update(updatedTask)
                        updatedCount++
                        Log.d("DebugActivity", "Updated task '${task.title}' from ${task.xpReward} to $newXp XP")
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DebugActivity, "Ažurirano $updatedCount zadataka s novim XP nagradama!", Toast.LENGTH_LONG).show()
                    debugTextView.text = "Ažurirano $updatedCount zadataka s novim XP sustavom"
                }
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
            else -> 40 // Default vrijednost
        }
        
        return if (isDailyGoal) {
            // Dnevni ciljevi nose manje XP po jedinici
            (baseXp * 0.7).toInt()
        } else {
            baseXp
        }
    }
    
    private fun addSampleTasks() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPref.getInt("userId", -1)
        
        if (userId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = MainQuestDatabase.getDatabase(applicationContext)
                
                // Dodaj test zadatke s XP nagradama
                val testTasks: List<Task> = listOf(
                    Task(
                        title = "Popij vodu", 
                        description = "Popij barem 8 čaša vode", 
                        category = "zdravlje", 
                        dueDate = null, 
                        isCompleted = false, 
                        xpReward = 40, 
                        userId = userId,
                        isDailyGoal = false,
                        dailyTarget = 1,
                        dailyProgress = 0,
                        lastCompletedDate = null,
                        streakCount = 0
                    ),
                    Task(
                        title = "Vježbaj 30 min", 
                        description = "Kardio ili snaga", 
                        category = "fitness", 
                        dueDate = null, 
                        isCompleted = false, 
                        xpReward = 40, 
                        userId = userId,
                        isDailyGoal = false,
                        dailyTarget = 1,
                        dailyProgress = 0,
                        lastCompletedDate = null,
                        streakCount = 0
                    ),
                    Task(
                        title = "Uči programiranje", 
                        description = "Kotlin ili Android", 
                        category = "učenje", 
                        dueDate = null, 
                        isCompleted = false, 
                        xpReward = 50, 
                        userId = userId,
                        isDailyGoal = false,
                        dailyTarget = 1,
                        dailyProgress = 0,
                        lastCompletedDate = null,
                        streakCount = 0
                    ),
                    Task(
                        title = "Završi projekt", 
                        description = "Završi glavni zadatak", 
                        category = "posao", 
                        dueDate = null, 
                        isCompleted = false, 
                        xpReward = 60, 
                        userId = userId,
                        isDailyGoal = false,
                        dailyTarget = 1,
                        dailyProgress = 0,
                        lastCompletedDate = null,
                        streakCount = 0
                    ),
                    Task(
                        title = "Čitaj knjigu", 
                        description = "Barem 30 stranica", 
                        category = "osobni razvoj", 
                        dueDate = null, 
                        isCompleted = false, 
                        xpReward = 45, 
                        userId = userId,
                        isDailyGoal = false,
                        dailyTarget = 1,
                        dailyProgress = 0,
                        lastCompletedDate = null,
                        streakCount = 0
                    )
                )
                
                testTasks.forEach { task ->
                    db.taskDao().insert(task)
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DebugActivity, "Dodano ${testTasks.size} test zadataka s XP nagradama!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun createRewards() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = MainQuestDatabase.getDatabase(applicationContext)
            
            // Provjeri postoje li već nagrade
            val existingRewards = db.rewardDao().getAll()
            if (existingRewards.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DebugActivity, "Nagrade već postoje! (${existingRewards.size})", Toast.LENGTH_SHORT).show()
                    debugTextView.text = "Postojeće nagrade:\n${existingRewards.joinToString("\n") { "• ${it.name} (${it.xpCost} XP)" }}"
                }
                return@launch
            }
            
            // Kreiraj početne nagrade
            val rewards = listOf(
                com.example.mainquest.data.Reward(
                    name = "🎨 Zlatni Avatar",
                    description = "Ekskluzivni zlatni avatar frame",
                    image = null,
                    xpCost = 100
                ),
                com.example.mainquest.data.Reward(
                    name = "🌟 Premium Theme",
                    description = "Lijepa tema s gradijentima",
                    image = null,
                    xpCost = 150
                ),
                com.example.mainquest.data.Reward(
                    name = "🏆 Champion Badge",
                    description = "Pokazuje da si pravi prvak!",
                    image = null,
                    xpCost = 200
                ),
                com.example.mainquest.data.Reward(
                    name = "💎 Diamond Status",
                    description = "Dijamantski status za elitne igrače",
                    image = null,
                    xpCost = 300
                ),
                com.example.mainquest.data.Reward(
                    name = "🎯 Double XP Boost",
                    description = "Dupli XP za sljedeće 3 dana",
                    image = null,
                    xpCost = 250
                ),
                com.example.mainquest.data.Reward(
                    name = "🎪 Bonus Wheel Spins",
                    description = "5 dodatnih okretaja kotača sreće",
                    image = null,
                    xpCost = 120
                ),
                com.example.mainquest.data.Reward(
                    name = "👑 VIP Title",
                    description = "Ekskluzivni VIP naslov",
                    image = null,
                    xpCost = 400
                ),
                com.example.mainquest.data.Reward(
                    name = "🌈 Rainbow Theme",
                    description = "Šarena tema s rainbow efektima",
                    image = null,
                    xpCost = 180
                ),
                com.example.mainquest.data.Reward(
                    name = "⚡ Lightning Badge",
                    description = "Za brze i efikasne igrače",
                    image = null,
                    xpCost = 160
                ),
                com.example.mainquest.data.Reward(
                    name = "🔥 Streak Master",
                    description = "Za održavanje dugih streakova",
                    image = null,
                    xpCost = 220
                )
            )
            
            var createdCount = 0
            rewards.forEach { reward ->
                try {
                    db.rewardDao().insert(reward)
                    createdCount++
                } catch (e: Exception) {
                    Log.e("DebugActivity", "Error creating reward: ${reward.name}", e)
                }
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DebugActivity, "🏆 Kreirano $createdCount nagrada!", Toast.LENGTH_LONG).show()
                debugTextView.text = "Kreirane nagrade:\n${rewards.joinToString("\n") { "• ${it.name} (${it.xpCost} XP)" }}"
            }
        }
    }
} 