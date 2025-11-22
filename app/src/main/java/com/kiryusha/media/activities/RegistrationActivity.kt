package com.kiryusha.media.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.kiryusha.media.R
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kiryusha.media.MediaApplication
import com.kiryusha.media.database.entities.User
import com.kiryusha.media.repository.UserRepository
import com.kiryusha.media.utils.SecurityUtils
import kotlinx.coroutines.launch
import androidx.core.content.edit

class RegistrationActivity : AppCompatActivity() {
    private lateinit var repository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        repository = UserRepository(MediaApplication.database.userDao())

        val registerButton = findViewById<Button>(R.id.btn_register).setOnClickListener { registerUser(
                R.id.et_login.toString(),
            R.id.et_username.toString(),
            R.id.et_password.toString(),
            R.id.et_confirm_password.toString()) }

        val loginLink = findViewById<TextView>(R.id.tv_login).setOnClickListener { startActivity(
            Intent(this, LoginActivity::class.java)) }
    }

    private fun registerUser(login: String, username: String, password: String, confirmPassword: String){
        lifecycleScope.launch {
            val hashedPassword = SecurityUtils.hashPassword(password)
            repository.registerUser(User(login = login, userName = username, password = hashedPassword))

            val user = repository.loginUser(login, hashedPassword)
            if(user != null){
                saveUserSession(user.uid)
                startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                finish()
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