package com.kiryusha.media.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kiryusha.media.MediaApplication
import com.kiryusha.media.R
import com.kiryusha.media.repository.UserRepository
import com.kiryusha.media.utils.SecurityUtils
import kotlinx.coroutines.launch
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {
    private lateinit var repository: UserRepository


    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        repository = UserRepository(MediaApplication.database.userDao())

        val loginButton = findViewById<Button>(R.id.btn_login)
        loginButton.setOnClickListener { loginUser(R.id.et_login.toString(), R.id.et_password.toString()) }

        val registerLink = findViewById<TextView>(R.id.tv_register)
        registerLink.setOnClickListener { startActivity(Intent(this, RegistrationActivity::class.java)) }
    }

    private fun loginUser(login: String, password: String) {
        lifecycleScope.launch{
            val hashedPassword = SecurityUtils.hashPassword(password)
            val user = repository.loginUser(login, hashedPassword)

            if(user != null) {
                saveUserSession(user.uid)

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }else{
                Toast.makeText(this@LoginActivity, "Неверные данные пользователя", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserSession(userId: Int) {
        getSharedPreferences("user_prefs", MODE_PRIVATE)
            .edit {
                putInt("user_id", userId)
                    .putBoolean("is_logged_in", true)
            }
    }
}