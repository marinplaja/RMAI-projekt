package com.example.mainquest

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mainquest.data.MainQuestDatabase
import com.example.mainquest.data.Task
import com.example.mainquest.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class EditTaskActivity : AppCompatActivity() {
    private lateinit var titleEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var doneButton: Button
    private lateinit var datePickerButton: Button
    private lateinit var selectedDateDisplay: TextView
    
    // Nova polja za dnevne ciljeve
    private lateinit var isDailyGoalCheckBox: CheckBox
    private lateinit var dailyTargetEditText: EditText
    private lateinit var dailyTargetLabel: TextView
    
    private var userId: Int = -1
    private var isDailyGoalMode: Boolean = false
    private var selectedDate: String? = null
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd. MMMM yyyy", Locale("hr", "HR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_task)

        try {
            initViews()
            setupCategorySpinner()
            setupDatePicker()
            
            val taskId = intent.getIntExtra("task_id", -1)
            isDailyGoalMode = intent.getBooleanExtra("is_daily_goal", false)
            
            // Ako je dnevni cilj mode, postavi checkbox
            if (isDailyGoalMode) {
                isDailyGoalCheckBox.isChecked = true
                showDailyGoalFields(true)
                showDateFields(false) // Sakrij kalendar za dnevne ciljeve
            }
            
            setupCheckboxListener()
            
            // Postavi početno stanje kalendara na osnovu checkbox-a
            showDateFields(!isDailyGoalCheckBox.isChecked)
            
            if (taskId != -1) {
                loadExistingTask(taskId)
            }

            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            userId = sharedPref.getInt("userId", -1)
            
            if (userId == -1) {
                // Pokušaj kreirati test korisnika
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = MainQuestDatabase.getDatabase(applicationContext)
                        val testUser = db.userDao().getByEmail("test@test.com")
                        if (testUser != null) {
                            userId = testUser.id
                            withContext(Dispatchers.Main) {
                                val editor = sharedPref.edit()
                                editor.putInt("userId", userId)
                                editor.apply()
                                Log.d("EditTaskActivity", "Koristim postojećeg test korisnika: $userId")
                            }
                        } else {
                            // Kreiraj novog test korisnika
                            val newUser = User(
                                username = "test_user",
                                email = "test@test.com",
                                password = "test123",
                                avatar = null,
                                xp = 0
                            )
                            val newUserId = db.userDao().insert(newUser).toInt()
                            userId = newUserId
                            withContext(Dispatchers.Main) {
                                val editor = sharedPref.edit()
                                editor.putInt("userId", newUserId)
                                editor.apply()
                                Log.d("EditTaskActivity", "Kreiran novi test korisnik: $newUserId")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("EditTaskActivity", "Greška pri kreiranju korisnika: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@EditTaskActivity, "Greška pri kreiranju korisnika!", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }
            } else {
                Log.d("EditTaskActivity", "Koristim postojećeg korisnika iz SharedPreferences: $userId")
            }

            doneButton.setOnClickListener {
                saveTask(taskId)
            }
        } catch (e: Exception) {
            Log.e("EditTaskActivity", "Greška u onCreate: ${e.message}", e)
            Toast.makeText(this, "Greška pri inicijalizaciji!", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun initViews() {
        titleEditText = findViewById(R.id.edit_task_title)
        categorySpinner = findViewById(R.id.edit_task_category)
        doneButton = findViewById(R.id.edit_task_done_button)
        datePickerButton = findViewById(R.id.edit_task_date_picker_button)
        selectedDateDisplay = findViewById(R.id.selected_date_display)
        
        // Nova polja
        isDailyGoalCheckBox = findViewById(R.id.edit_task_is_daily_goal)
        dailyTargetEditText = findViewById(R.id.edit_task_daily_target)
        dailyTargetLabel = findViewById(R.id.edit_task_daily_target_label)
    }
    
    private fun setupCategorySpinner() {
        val categories = listOf("zdravlje", "fitness", "učenje", "posao", "osobni razvoj", "ostalo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }
    
    private fun setupDatePicker() {
        datePickerButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = dateFormat.format(calendar.time)
                    selectedDateDisplay.text = "Odabrani datum: ${displayDateFormat.format(calendar.time)}"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            
            datePickerDialog.show()
        }
    }
    
    private fun setupCheckboxListener() {
        isDailyGoalCheckBox.setOnCheckedChangeListener { _, isChecked ->
            showDailyGoalFields(isChecked)
            showDateFields(!isChecked) // Sakrij kalendar ako je dnevni cilj
        }
    }
    
    private fun showDailyGoalFields(show: Boolean) {
        val visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        dailyTargetLabel.visibility = visibility
        dailyTargetEditText.visibility = visibility
        
        if (show && dailyTargetEditText.text.isEmpty()) {
            dailyTargetEditText.setText("1")
        }
    }
    
    private fun showDateFields(show: Boolean) {
        val visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        datePickerButton.visibility = visibility
        selectedDateDisplay.visibility = visibility
        
        // Također trebamo pronaći label za datum
        findViewById<TextView>(R.id.edit_task_due_date_label).visibility = visibility
    }
    
    private fun loadExistingTask(taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MainQuestDatabase.getDatabase(applicationContext)
                val task = db.taskDao().getById(taskId)
                task?.let {
                    withContext(Dispatchers.Main) {
                        titleEditText.setText(it.title)
                        
                        val categories = (categorySpinner.adapter as ArrayAdapter<String>)
                        val catIndex = (0 until categories.count).find { index ->
                            categories.getItem(index) == it.category
                        } ?: 0
                        categorySpinner.setSelection(catIndex)
                        
                        // Postavi datum ako postoji
                        it.dueDate?.let { dueDate ->
                            selectedDate = dueDate
                            try {
                                val date = dateFormat.parse(dueDate)
                                date?.let { d ->
                                    selectedDateDisplay.text = "Odabrani datum: ${displayDateFormat.format(d)}"
                                }
                            } catch (e: Exception) {
                                Log.e("EditTaskActivity", "Greška pri parsiranju datuma: ${e.message}")
                            }
                        }
                        
                        // Postavi nova polja
                        isDailyGoalCheckBox.isChecked = it.isDailyGoal
                        dailyTargetEditText.setText(it.dailyTarget.toString())
                        showDailyGoalFields(it.isDailyGoal)
                        showDateFields(!it.isDailyGoal) // Sakrij kalendar ako je dnevni cilj
                    }
                }
            } catch (e: Exception) {
                Log.e("EditTaskActivity", "Greška pri učitavanju zadatka: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditTaskActivity, "Greška pri učitavanju zadatka!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun saveTask(taskId: Int) {
        val title = titleEditText.text.toString().trim()
        val category = categorySpinner.selectedItem.toString()
        val isDailyGoal = isDailyGoalCheckBox.isChecked
        val dailyTarget = dailyTargetEditText.text.toString().toIntOrNull() ?: 1
        
        // Validacija
        if (title.isBlank()) {
            Toast.makeText(this, "Naziv ne smije biti prazan!", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isDailyGoal && dailyTarget <= 0) {
            Toast.makeText(this, "Dnevni cilj mora biti veći od 0!", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (userId == -1) {
            Toast.makeText(this, "Greška: Korisnik nije prijavljen!", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Debug - provjeri userId
        Log.d("EditTaskActivity", "Pokušavam spremiti zadatak za userId: $userId")
        
        // Prikaži loading
        doneButton.isEnabled = false
        doneButton.text = "Spremanje..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MainQuestDatabase.getDatabase(applicationContext)
                
                // Debug - provjeri postoji li korisnik
                val existingUser = db.userDao().getById(userId)
                Log.d("EditTaskActivity", "Pronađen korisnik: ${existingUser?.username ?: "NEMA KORISNIKA"}")
                
                if (existingUser == null) {
                    withContext(Dispatchers.Main) {
                        doneButton.isEnabled = true
                        doneButton.text = "Završi"
                        Toast.makeText(this@EditTaskActivity, "Greška: Korisnik ne postoji u bazi! ID: $userId", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                if (taskId != -1) {
                    // Ažuriranje postojećeg zadatka
                    val oldTask = db.taskDao().getById(taskId)
                    if (oldTask != null) {
                        val updatedTask = oldTask.copy(
                            title = title,
                            category = category,
                            dueDate = if (isDailyGoal) null else selectedDate, // Dnevni ciljevi nemaju datum
                            isDailyGoal = isDailyGoal,
                            dailyTarget = if (isDailyGoal) dailyTarget else 1,
                            dailyProgress = if (isDailyGoal) oldTask.dailyProgress else 0
                        )
                        db.taskDao().update(updatedTask)
                        Log.d("EditTaskActivity", "Zadatak ažuriran: ${updatedTask.title}")
                    }
                } else {
                    // Kreiranje novog zadatka
                    // Automatski izračunaj XP nagradu na osnovu kategorije i tipa zadatka
                    val autoXpReward = calculateXpReward(category, isDailyGoal, dailyTarget)
                    
                    val task = Task(
                        title = title,
                        description = null,
                        category = category,
                        dueDate = if (isDailyGoal) null else selectedDate, // Dnevni ciljevi nemaju datum
                        isCompleted = false,
                        xpReward = autoXpReward,
                        userId = userId,
                        isDailyGoal = isDailyGoal,
                        dailyTarget = if (isDailyGoal) dailyTarget else 1,
                        dailyProgress = 0,
                        lastCompletedDate = null,
                        streakCount = 0
                    )
                    
                    Log.d("EditTaskActivity", "Pokušavam umetnuti zadatak: $task")
                    val insertId = db.taskDao().insert(task)
                    Log.d("EditTaskActivity", "Novi zadatak kreiran s ID: $insertId, naziv: $title")
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditTaskActivity, "Zadatak uspješno spremljen!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("EditTaskActivity", "Greška pri spremanju zadatka: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    doneButton.isEnabled = true
                    doneButton.text = "Završi"
                    Toast.makeText(this@EditTaskActivity, "Greška pri spremanju: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun calculateXpReward(category: String?, isDailyGoal: Boolean, dailyTarget: Int): Int {
        val baseXp = when (category?.lowercase()) {
            "učenje", "learning" -> 50
            "fitness", "zdravlje", "health" -> 40
            "posao", "work" -> 60
            "osobni razvoj", "personal development" -> 45
            "hobiji", "hobbies" -> 30
            "kućanski poslovi", "household" -> 25
            else -> 35
        }
        
        return if (isDailyGoal) {
            (baseXp * dailyTarget * 0.8).toInt()
        } else {
            baseXp
        }
    }
    

} 