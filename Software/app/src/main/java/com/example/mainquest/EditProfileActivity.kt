package com.example.mainquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class EditProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val usernameButton = findViewById<Button>(R.id.edit_username_button)
        val emailButton = findViewById<Button>(R.id.edit_email_button)
        val passwordButton = findViewById<Button>(R.id.edit_password_button)

        usernameButton.setOnClickListener {
            val intent = Intent(this, EditUsernameActivity::class.java)
            startActivity(intent)
        }
        emailButton.setOnClickListener {
            val intent = Intent(this, EditEmailActivity::class.java)
            startActivity(intent)
        }
        passwordButton.setOnClickListener {
            val intent = Intent(this, EditPasswordActivity::class.java)
            startActivity(intent)
        }
    }
} 