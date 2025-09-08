package com.example.mainquest

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mainquest.data.MainQuestDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditEmailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_email)

        val emailEdit = findViewById<EditText>(R.id.edit_email_field)
        val saveButton = findViewById<Button>(R.id.save_email_button)
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPref.getInt("userId", -1)
        val userDao = MainQuestDatabase.getDatabase(this).userDao()

        saveButton.setOnClickListener {
            val newEmail = emailEdit.text.toString()
            if (newEmail.isBlank()) {
                Toast.makeText(this, "Unesite novi email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                val existingUser = userDao.getByEmail(newEmail)
                if (existingUser != null && existingUser.id != userId) {
                    runOnUiThread {
                        Toast.makeText(this@EditEmailActivity, "Email je već zauzet", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val user = userDao.getById(userId)
                    user?.let {
                        val updatedUser = it.copy(email = newEmail)
                        userDao.update(updatedUser)
                        runOnUiThread {
                            Toast.makeText(this@EditEmailActivity, "Email promijenjen", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            }
        }
    }
} 