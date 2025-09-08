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

class EditPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_password)

        val passwordEdit = findViewById<EditText>(R.id.edit_password_field)
        val saveButton = findViewById<Button>(R.id.save_password_button)
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPref.getInt("userId", -1)
        val userDao = MainQuestDatabase.getDatabase(this).userDao()

        saveButton.setOnClickListener {
            val newPassword = passwordEdit.text.toString()
            if (newPassword.isBlank()) {
                Toast.makeText(this, "Unesite novu lozinku", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                val user = userDao.getById(userId)
                user?.let {
                    val updatedUser = it.copy(password = newPassword)
                    userDao.update(updatedUser)
                    runOnUiThread {
                        Toast.makeText(this@EditPasswordActivity, "Lozinka promijenjena", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }
} 