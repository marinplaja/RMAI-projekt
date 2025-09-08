package com.example.mainquest

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.mainquest.R
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPref.getInt("userId", -1)
        if (userId == -1) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Postavljanje window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Inicijalizacija ViewPager2 i TabLayout
        setupNavigation()
    }
    
    private fun setupNavigation() {
        viewPager = findViewById(R.id.viewpager)
        tabLayout = findViewById(R.id.tabs)
        
        // Postavljanje adaptera za ViewPager2
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        
        // Povezivanje TabLayout s ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.tab_tasks)
                    tab.icon = ContextCompat.getDrawable(this, R.drawable.pencil_svgrepo_com)
                }
                1 -> {
                    tab.text = getString(R.string.tab_progress)
                    tab.icon = ContextCompat.getDrawable(this, R.drawable.sunlight_svgrepo_com)
                }
                2 -> {
                    tab.text = getString(R.string.tab_rewards)
                    tab.icon = ContextCompat.getDrawable(this, R.drawable.bar_graph_svgrepo_com)
                }
                3 -> {
                    tab.text = getString(R.string.tab_profile)
                    tab.icon = ContextCompat.getDrawable(this, R.drawable.person_svgrepo_com)
                }
                4 -> {
                    tab.text = getString(R.string.tab_completed)
                    tab.icon = ContextCompat.getDrawable(this, R.drawable.ic_progress)
                }
            }
        }.attach()
    }
}