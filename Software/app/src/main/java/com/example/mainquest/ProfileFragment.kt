package com.example.mainquest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mainquest.data.MainQuestDatabase
import com.example.mainquest.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import android.widget.TextView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog

class ProfileFragment : Fragment() {
    private var userId: Int = -1
    private var user: User? = null
    private lateinit var avatarImage: ImageView
    private lateinit var gameManager: GameManager
    private val PICK_IMAGE_REQUEST = 1

    private fun setAvatar(base64: String?) {
        if (base64.isNullOrBlank()) {
            avatarImage.setImageResource(R.drawable.ic_profile)
        } else {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            avatarImage.setImageBitmap(bmp)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val usernameDisplay = view.findViewById<TextView>(R.id.profile_username_display)
        val levelDisplay = view.findViewById<TextView>(R.id.profile_level)
        val xpBar = view.findViewById<ProgressBar>(R.id.profile_xp_bar)
        val xpText = view.findViewById<TextView>(R.id.profile_xp_text)
        val editButton = view.findViewById<Button>(R.id.profile_edit_button)
        val logoutButton = view.findViewById<Button>(R.id.profile_logout_button)
        val minigameButton = view.findViewById<Button>(R.id.profile_minigame_button)
        val achievementsButton = view.findViewById<Button>(R.id.profile_achievements_button)
        val myRewardsButton = view.findViewById<Button>(R.id.profile_my_rewards_button)
        val rewardsButton = view.findViewById<Button>(R.id.profile_rewards_button)
        avatarImage = view.findViewById(R.id.profile_avatar)

        // Initialize GameManager
        gameManager = GameManager.getInstance(requireContext())
        gameManager.performDailyReset()

        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("userId", -1)
        val userDao = MainQuestDatabase.getDatabase(requireContext()).userDao()

        if (userId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                user = userDao.getById(userId)
                user?.let { u ->
                    requireActivity().runOnUiThread {
                        setAvatar(u.avatar)
                        usernameDisplay.text = u.username
                        
                        // Update level and XP display with GameManager
                        updateLevelDisplay(u, levelDisplay, xpBar, xpText)
                    }
                }
            }
        }

        avatarImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        editButton.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().remove("userId").apply()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        minigameButton.setOnClickListener {
            val intent = Intent(requireContext(), WheelActivity::class.java)
            startActivity(intent)
        }

        achievementsButton.setOnClickListener {
            showAchievements()
        }

        myRewardsButton.setOnClickListener {
            showMyRewards()
        }

        rewardsButton.setOnClickListener {
            val intent = Intent(requireContext(), RewardsActivity::class.java)
            startActivity(intent)
        }
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val uri = data.data
            val inputStream = requireContext().contentResolver.openInputStream(uri!!)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 320, 320, true)
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
            avatarImage.setImageBitmap(resizedBitmap)
            // Spremi avatar u user objekt i bazu
            user?.let { u ->
                val userDao = MainQuestDatabase.getDatabase(requireContext()).userDao()
                val updatedUser = u.copy(avatar = base64)
                user = updatedUser
                CoroutineScope(Dispatchers.IO).launch {
                    userDao.update(updatedUser)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val view = view ?: return
        val usernameDisplay = view.findViewById<TextView>(R.id.profile_username_display)
        val levelDisplay = view.findViewById<TextView>(R.id.profile_level)
        val xpBar = view.findViewById<ProgressBar>(R.id.profile_xp_bar)
        val xpText = view.findViewById<TextView>(R.id.profile_xp_text)
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("userId", -1)
        val userDao = MainQuestDatabase.getDatabase(requireContext()).userDao()
        if (userId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val user = userDao.getById(userId)
                user?.let { u ->
                    requireActivity().runOnUiThread {
                        setAvatar(u.avatar)
                        usernameDisplay.text = u.username
                        updateLevelDisplay(u, levelDisplay, xpBar, xpText)
                    }
                }
            }
        }
    }

    private fun updateLevelDisplay(user: User, levelDisplay: TextView, xpBar: ProgressBar, xpText: TextView) {
        val level = gameManager.calculateLevel(user.xp)
        val progress = gameManager.getXpProgress(user.xp)
        val nextLevelXp = gameManager.getXpForNextLevel(user.xp)
        
        levelDisplay.text = "Level $level"
        xpBar.progress = (progress * 100).toInt()
        
        if (level >= 6) {
            xpText.text = "MAX LEVEL REACHED!"
        } else {
            xpText.text = "${user.xp}/$nextLevelXp XP"
        }
    }

    private fun showAchievements() {
        if (userId == -1) {
            Toast.makeText(requireContext(), "Error: User not found!", Toast.LENGTH_SHORT).show()
            return
        }
        
        user?.let { u ->
            val achievements = gameManager.checkAchievements(u)
            val gameStats = gameManager.getGameStats(userId)
            
            val achievementText = if (achievements.isEmpty()) {
                "No achievements yet!\n\nStart spinning the wheel and completing tasks to unlock achievements!"
            } else {
                "🏆 YOUR ACHIEVEMENTS:\n\n" + achievements.joinToString("\n\n")
            }
            
            val statsText = "\n\n📊 GAME STATS:\n" +
                    "🎯 Total Spins: ${gameStats.totalSpins}\n" +
                    "⭐ Total XP Won: ${gameStats.totalXpWon}\n" +
                    "🔥 Current Streak: ${gameStats.streakDays} days\n" +
                    "🎲 Best Spin: ${gameStats.bestSpin}\n" +
                    "🎉 Bonus Turns: ${gameStats.totalBonusTurns}"
            
            val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("🏆 Achievements & Stats")
                .setMessage(achievementText + statsText)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .create()
            
            dialog.show()
        }
    }

    private fun showMyRewards() {
        if (userId == -1) {
            Toast.makeText(requireContext(), "Error: User not found!", Toast.LENGTH_SHORT).show()
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            val db = MainQuestDatabase.getDatabase(requireContext())
            val unlockedRewards = db.unlockedRewardDao().getByUserId(userId)
            val allRewards = db.rewardDao().getAll().associateBy { it.id }
            
            val myRewardsList = unlockedRewards.mapNotNull { unlockedReward ->
                allRewards[unlockedReward.rewardId]
            }
            
            withContext(Dispatchers.Main) {
                val rewardsText = if (myRewardsList.isEmpty()) {
                    "Nemaš još kupljenih nagrada! 😔\n\nIdi u Trgovinu nagrada i kupi nešto lijepo za sebe! 🛒"
                } else {
                    "💎 TVOJE NAGRADE:\n\n" + myRewardsList.joinToString("\n\n") { reward ->
                        "🏆 ${reward.name}\n${reward.description ?: "Specijalna nagrada"}\n💰 Cijena bila: ${reward.xpCost} XP"
                    }
                }
                
                val totalSpent = myRewardsList.sumOf { it.xpCost }
                val statsText = if (myRewardsList.isNotEmpty()) {
                    "\n\n📊 STATISTIKE:\n🎯 Ukupno nagrada: ${myRewardsList.size}\n💸 Ukupno potrošeno: $totalSpent XP"
                } else ""
                
                val dialog = AlertDialog.Builder(requireContext())
                    .setTitle("💎 Moje Nagrade")
                    .setMessage(rewardsText + statsText)
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .setNeutralButton("🛒 Trgovina") { _, _ ->
                        val intent = Intent(requireContext(), RewardsActivity::class.java)
                        startActivity(intent)
                    }
                    .create()
                
                dialog.show()
            }
        }
    }

    companion object {
        fun newInstance(): ProfileFragment {
            return ProfileFragment()
        }
    }
} 