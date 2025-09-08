package com.example.mainquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mainquest.data.MainQuestDatabase
import com.example.mainquest.data.User
import com.example.mainquest.data.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        emailEditText = findViewById(R.id.register_email)
        passwordEditText = findViewById(R.id.register_password)
        usernameEditText = findViewById(R.id.register_username)
        registerButton = findViewById(R.id.register_button)
        userDao = MainQuestDatabase.getDatabase(this).userDao()

        val loginLink = findViewById<TextView>(R.id.register_login_link)
        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val username = usernameEditText.text.toString()
            if (email.isBlank() || password.isBlank() || username.isBlank()) {
                Toast.makeText(this, "Sva polja su obavezna", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                val existingUser = userDao.getByEmail(email)
                if (existingUser != null) {
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, "Korisnik s tim emailom već postoji", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val user = User(username = username, email = email, password = password, avatar = null)
                    userDao.insert(user)
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, "Registracija uspješna! Prijavite se.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }
} 