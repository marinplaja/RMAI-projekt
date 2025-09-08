package com.example.mainquest

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mainquest.data.MainQuestDatabase
import com.example.mainquest.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class WheelActivity : AppCompatActivity() {
    
    private lateinit var wheelImage: CustomWheelView
    private lateinit var spinButton: Button
    private lateinit var resultText: TextView
    private lateinit var xpDisplay: TextView
    private lateinit var backButton: Button
    private lateinit var remainingSpinsText: TextView
    
    private var userId: Int = -1
    private var currentUser: User? = null
    private var isSpinning = false
    private lateinit var gameManager: GameManager
    
    // Wheel rewards
    private val rewards = listOf(
        "10 XP",
        "5 XP", 
        "20 XP",
        "Bonus Turn",
        "15 XP",
        "Nothing",
        "25 XP",
        "Lucky Day!"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wheel)
        
        initViews()
        setupClickListeners()
        loadUserData()
    }
    
    private fun initViews() {
        wheelImage = findViewById(R.id.wheel_image)
        spinButton = findViewById(R.id.spin_button)
        resultText = findViewById(R.id.result_text)
        xpDisplay = findViewById(R.id.xp_display)
        backButton = findViewById(R.id.back_button)
        remainingSpinsText = findViewById(R.id.remaining_spins_text)
        
        // Initialize GameManager
        gameManager = GameManager.getInstance(this)
        gameManager.performDailyReset()
        
        // Debug logging
        Log.d("WheelActivity", "Views initialized, wheelImage: $wheelImage")
        
        // Ensure wheel image is properly loaded
        wheelImage.post {
            Log.d("WheelActivity", "WheelImage dimensions: ${wheelImage.width}x${wheelImage.height}")
        }
    }
    
    private fun setupClickListeners() {
        spinButton.setOnClickListener {
            if (!isSpinning) {
                spinWheel()
            }
        }
        
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun loadUserData() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = sharedPref.getInt("userId", -1)
        
        if (userId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = MainQuestDatabase.getDatabase(applicationContext)
                    currentUser = db.userDao().getById(userId)
                    
                    withContext(Dispatchers.Main) {
                        currentUser?.let { user ->
                            val level = gameManager.calculateLevel(user.xp)
                            val progress = gameManager.getXpProgress(user.xp)
                            val nextLevelXp = gameManager.getXpForNextLevel(user.xp)
                            
                            xpDisplay.text = "Level $level | XP: ${user.xp}/$nextLevelXp"
                            updateRemainingSpins()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WheelActivity", "Error loading user data: ${e.message}", e)
                }
            }
        }
    }
    
    private fun updateRemainingSpins() {
        if (userId == -1) return
        
        val remaining = gameManager.getRemainingSpins(userId)
        remainingSpinsText.text = "Remaining spins today: $remaining"
        spinButton.isEnabled = remaining > 0 && !isSpinning
        
        if (remaining <= 0) {
            spinButton.text = "❌ No spins left today"
            resultText.text = "Come back tomorrow for more spins!"
        } else {
            spinButton.text = "🎲 SPIN THE WHEEL! ($remaining left)"
        }
    }
    
    private fun spinWheel() {
        if (userId == -1) {
            Toast.makeText(this, "Error: User not found!", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!gameManager.canPlayWheel(userId)) {
            Toast.makeText(this, "No more spins today! Come back tomorrow.", Toast.LENGTH_LONG).show()
            return
        }
        
        isSpinning = true
        spinButton.isEnabled = false
        resultText.text = "Spinning..."
        
        // Realistična animacija kao u YouTube videu
        // 5-10 punih okretaja + nasumični završni dio
        val fullRotations = (5 + Random.nextFloat() * 5) * 360f
        val finalSectorAngle = Random.nextFloat() * 360f
        val totalRotation = fullRotations + finalSectorAngle
        
        val currentRotation = wheelImage.rotation
        val finalRotation = currentRotation + totalRotation
        
        Log.d("WheelActivity", "Realistic spin: fullRotations=$fullRotations, finalSector=$finalSectorAngle, total=$totalRotation")
        
        // Animacija s realističnim timing-om (4-6 sekundi)
        val animationDuration = (4000 + Random.nextInt(2000)).toLong() // 4-6 sekundi
        
        wheelImage.animate()
            .rotationBy(totalRotation)
            .setDuration(animationDuration)
            .setInterpolator(DecelerateInterpolator(1.5f)) // Sporiji završetak
            .withStartAction {
                Log.d("WheelActivity", "🎡 Wheel spinning started!")
                // Dodaj zvučni efekt (opcionalno)
            }
            .withEndAction {
                // Izračunaj rezultat na osnovu konačne pozicije
                val finalWheelRotation = wheelImage.rotation % 360f
                val normalizedRotation = if (finalWheelRotation < 0) finalWheelRotation + 360f else finalWheelRotation
                
                // Sektor size je 45 stupnjeva (360/8)
                val sectorSize = 45f
                val resultIndex = ((360f - normalizedRotation + sectorSize/2) / sectorSize).toInt() % rewards.size
                
                Log.d("WheelActivity", "🎯 Wheel stopped: rotation=$finalWheelRotation, normalized=$normalizedRotation, sector=$resultIndex")
                
                // Dodaj kratku pauzu prije prikazivanja rezultata (kao u stvarnom wheel-u)
                wheelImage.postDelayed({
                    processWheelResult(rewards[resultIndex])
                    isSpinning = false
                    updateRemainingSpins()
                }, 500) // 0.5 sekunde pauze
            }
            .start()
        
        Log.d("WheelActivity", "🎲 Realistic wheel animation started (${animationDuration}ms)")
    }
    
    private fun processWheelResult(result: String) {
        Log.d("WheelActivity", "🎯 Wheel result: $result")
        
        // Dramatični prikaz rezultata
        val resultEmoji = when {
            result.contains("25") -> "🎉💰"
            result.contains("20") -> "🎊💎"
            result.contains("15") -> "⭐✨"
            result.contains("10") -> "🎯🔥"
            result.contains("5") -> "👍💫"
            result == "Bonus Turn" -> "🎁🎲"
            result == "Lucky Day!" -> "🍀🌟"
            result == "Nothing" -> "😔💔"
            else -> "🎪"
        }
        
        resultText.text = "$resultEmoji $result $resultEmoji"
        
        var xpWon = 0
        
        when {
            result.contains("XP") -> {
                xpWon = result.filter { it.isDigit() }.toIntOrNull() ?: 0
                if (xpWon > 0) {
                    giveXpReward(xpWon)
                    // Različiti toast-ovi ovisno o nagradi
                    val toastMessage = when {
                        xpWon >= 20 -> "🎉 EXCELLENT! +$xpWon XP!"
                        xpWon >= 15 -> "🎊 GREAT! +$xpWon XP!"
                        xpWon >= 10 -> "⭐ GOOD! +$xpWon XP!"
                        else -> "👍 Nice! +$xpWon XP!"
                    }
                    Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
                }
            }
            result == "Bonus Turn" -> {
                Toast.makeText(this, "🎁 BONUS TURN! Spin again for FREE! 🎲", Toast.LENGTH_LONG).show()
                resultText.text = "🎁 $result - FREE SPIN! 🎲"
                // Ne povećavaj spin count za bonus turn
                isSpinning = false
                updateRemainingSpins()
                return
            }
            result == "Lucky Day!" -> {
                xpWon = 50
                giveXpReward(xpWon)
                Toast.makeText(this, "🍀 LUCKY DAY! MEGA BONUS +$xpWon XP! 🌟", Toast.LENGTH_LONG).show()
            }
            result == "Nothing" -> {
                Toast.makeText(this, "😔 Aww, better luck next time! 💔", Toast.LENGTH_LONG).show()
            }
        }
        
        // Record the spin in GameManager
        gameManager.recordWheelSpin(userId, result, xpWon)
        
        // Check for achievements s dramatičnijim prikazom
        currentUser?.let { user ->
            val achievements = gameManager.checkAchievements(user)
            if (achievements.isNotEmpty()) {
                val latestAchievement = achievements.last()
                // Prikaži achievement s pojavom nakon kratke pauze
                resultText.postDelayed({
                    Toast.makeText(this, "🏆 NEW ACHIEVEMENT! $latestAchievement", Toast.LENGTH_LONG).show()
                }, 1000)
            }
        }
    }
    
    private fun giveXpReward(xpAmount: Int) {
        currentUser?.let { user ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = MainQuestDatabase.getDatabase(applicationContext)
                    val updatedUser = user.copy(xp = user.xp + xpAmount)
                    db.userDao().update(updatedUser)
                    currentUser = updatedUser
                    
                    withContext(Dispatchers.Main) {
                        val level = gameManager.calculateLevel(updatedUser.xp)
                        val nextLevelXp = gameManager.getXpForNextLevel(updatedUser.xp)
                        xpDisplay.text = "Level $level | XP: ${updatedUser.xp}/$nextLevelXp"
                        
                        Toast.makeText(this@WheelActivity, "🎯 Gained $xpAmount XP!", Toast.LENGTH_SHORT).show()
                        Log.d("WheelActivity", "User gained $xpAmount XP, new total: ${updatedUser.xp}")
                    }
                } catch (e: Exception) {
                    Log.e("WheelActivity", "Error updating user XP: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@WheelActivity, "Error updating XP!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    // Extension function za ObjectAnimator
    private fun ObjectAnimator.doOnEnd(action: () -> Unit) {
        addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) {
                action()
            }
        })
    }
} 