package com.example.mainquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mainquest.data.MainQuestDatabase
import com.example.mainquest.data.UserDao
import com.example.mainquest.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailEditText = findViewById(R.id.login_email)
        passwordEditText = findViewById(R.id.login_password)
        loginButton = findViewById(R.id.login_button)
        registerLink = findViewById(R.id.login_register_link)
        userDao = MainQuestDatabase.getDatabase(this).userDao()

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Unesite email i lozinku", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                val user = userDao.getByEmail(email)
                runOnUiThread {
                    if (user != null && user.password == password) {
                        // Prijava uspješna, pokreni MainActivity
                        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        sharedPref.edit().putInt("userId", user.id).apply()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("userId", user.id)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Neispravan email ili lozinka", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
} 