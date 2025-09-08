package com.example.mainquest

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mainquest.data.MainQuestDatabase
import com.example.mainquest.data.Reward
import com.example.mainquest.data.UnlockedReward
import com.example.mainquest.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class RewardsActivity : AppCompatActivity() {
    
    private lateinit var userXpText: TextView
    private lateinit var userLevelText: TextView
    private lateinit var rewardsRecyclerView: RecyclerView
    private lateinit var backButton: Button
    private lateinit var rewardsAdapter: RewardsAdapter
    private lateinit var gameManager: GameManager
    
    private var userId: Int = -1
    private var currentUser: User? = null
    private var availableRewards: List<Reward> = listOf()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards)
        
        initViews()
        setupClickListeners()
        loadUserData()
        loadRewards()
    }
    
    override fun onResume() {
        super.onResume()
        loadUserData()
        loadRewards()
    }
    
    private fun initViews() {
        userXpText = findViewById(R.id.user_xp_text)
        userLevelText = findViewById(R.id.user_level_text)
        rewardsRecyclerView = findViewById(R.id.rewards_recycler_view)
        backButton = findViewById(R.id.back_button)
        
        gameManager = GameManager.getInstance(this)
        
        rewardsAdapter = RewardsAdapter(availableRewards) { reward ->
            purchaseReward(reward)
        }
        
        rewardsRecyclerView.layoutManager = LinearLayoutManager(this)
        rewardsRecyclerView.adapter = rewardsAdapter
    }
    
    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun loadUserData() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = sharedPref.getInt("userId", -1)
        
        if (userId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = MainQuestDatabase.getDatabase(applicationContext)
                currentUser = db.userDao().getById(userId)
                
                withContext(Dispatchers.Main) {
                    currentUser?.let { user ->
                        val level = gameManager.calculateLevel(user.xp)
                        userLevelText.text = "Level $level"
                        userXpText.text = "${user.xp} XP"
                        rewardsAdapter.updateUserXp(user.xp)
                    }
                }
            }
        }
    }
    
    private fun loadRewards() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = MainQuestDatabase.getDatabase(applicationContext)
            var rewards = db.rewardDao().getAll()
            
            // Ako nema nagrada, kreiraj ih automatski
            if (rewards.isEmpty()) {
                createInitialRewards(db)
                rewards = db.rewardDao().getAll()
            }
            
            val unlockedRewards = if (userId != -1) db.unlockedRewardDao().getByUserId(userId) else listOf()
            val unlockedRewardIds = unlockedRewards.map { it.rewardId }.toSet()
            
            // Filtriraj nagrade koje nisu još otključane
            val availableRewards = rewards.filter { it.id !in unlockedRewardIds }
            
            withContext(Dispatchers.Main) {
                this@RewardsActivity.availableRewards = availableRewards
                rewardsAdapter.updateRewards(availableRewards)
                
                // Prikaži poruku ako su sve nagrade otključane
                if (availableRewards.isEmpty() && rewards.isNotEmpty()) {
                    Toast.makeText(this@RewardsActivity, "🎉 Sve nagrade su otključane! Odličan posao!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private suspend fun createInitialRewards(db: MainQuestDatabase) {
        val rewards = listOf(
            Reward(name = "🎨 Zlatni Avatar", description = "Ekskluzivni zlatni avatar frame", image = null, xpCost = 100),
            Reward(name = "🌟 Premium Theme", description = "Lijepa tema s gradijentima", image = null, xpCost = 150),
            Reward(name = "🏆 Champion Badge", description = "Pokazuje da si pravi prvak!", image = null, xpCost = 200),
            Reward(name = "💎 Diamond Status", description = "Dijamantski status za elitne igrače", image = null, xpCost = 300),
            Reward(name = "🎯 Double XP Boost", description = "Dupli XP za sljedeće 3 dana", image = null, xpCost = 250),
            Reward(name = "🎪 Bonus Wheel Spins", description = "5 dodatnih okretaja kotača sreće", image = null, xpCost = 120),
            Reward(name = "👑 VIP Title", description = "Ekskluzivni VIP naslov", image = null, xpCost = 400),
            Reward(name = "🌈 Rainbow Theme", description = "Šarena tema s rainbow efektima", image = null, xpCost = 180),
            Reward(name = "⚡ Lightning Badge", description = "Za brze i efikasne igrače", image = null, xpCost = 160),
            Reward(name = "🔥 Streak Master", description = "Za održavanje dugih streakova", image = null, xpCost = 220)
        )
        
        rewards.forEach { reward ->
            db.rewardDao().insert(reward)
        }
    }
    
    private fun purchaseReward(reward: Reward) {
        currentUser?.let { user ->
            if (user.xp >= reward.xpCost) {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = MainQuestDatabase.getDatabase(applicationContext)
                    
                    // Oduzmi XP od korisnika
                    val updatedUser = user.copy(xp = user.xp - reward.xpCost)
                    db.userDao().update(updatedUser)
                    
                    // Dodaj nagradu kao otključanu
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val unlockedReward = UnlockedReward(
                        userId = userId,
                        rewardId = reward.id,
                        unlockedAt = dateFormat.format(Date())
                    )
                    db.unlockedRewardDao().insert(unlockedReward)
                    
                    currentUser = updatedUser
                    
                    withContext(Dispatchers.Main) {
                        // Prikaži lijepu poruku o kupnji
                        val dialog = androidx.appcompat.app.AlertDialog.Builder(this@RewardsActivity)
                            .setTitle("🎉 Nagrada kupljena!")
                            .setMessage("Uspješno ste kupili:\n\n🏆 ${reward.name}\n${reward.description}\n\n💰 Potrošeno: ${reward.xpCost} XP\n💎 Novo stanje: ${updatedUser.xp} XP")
                            .setPositiveButton("Odlično!") { dialog, _ -> dialog.dismiss() }
                            .setNeutralButton("💎 Moje nagrade") { _, _ ->
                                // Možemo dodati intent za prikaz mojih nagrada
                                finish()
                            }
                            .create()
                        
                        dialog.show()
                        
                        // Ažuriraj prikaz
                        loadUserData()
                        loadRewards()
                    }
                }
            } else {
                Toast.makeText(this, "❌ Nema dovoljno XP bodova! Potrebno: ${reward.xpCost} XP", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 