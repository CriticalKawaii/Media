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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kiryusha.media.utils.ValidationUtils

class LoginActivity : AppCompatActivity() {
    private lateinit var repository: UserRepository
    private lateinit var etLogin: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilLogin: TextInputLayout
    private lateinit var tilPassword: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        repository = UserRepository(MediaApplication.database.userDao())

        etLogin = findViewById(R.id.et_login)
        etPassword = findViewById(R.id.et_password)
        tilLogin = findViewById(R.id.til_login)
        tilPassword = findViewById(R.id.til_password)

        val loginButton = findViewById<MaterialButton>(R.id.btn_login)
        val registerLink = findViewById<TextView>(R.id.tv_register)

        loginButton.setOnClickListener {
            if (validateInput()) {loginUser()}
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }
    }

    private fun validateInput(): Boolean{
        var isValid = true

        val login = etLogin.text.toString().trim()
        val password = etPassword.text.toString()

        tilLogin.error = null
        tilPassword.error = null

        if(login.isEmpty()){
            tilLogin.error = "Введите логин"
            isValid = false
        }

        if(password.isEmpty()){
            tilPassword.error = "Введите пароль"
            isValid = false
        }

        return isValid
    }

    private fun loginUser() {
        val login = etLogin.text.toString().trim()
        val password = etPassword.text.toString()

        lifecycleScope.launch{
            try{
                val hashedPassword = SecurityUtils.hashPassword(password)
                val user = repository.loginUser(login, hashedPassword)

                if(user != null) {
                    saveUserSession(user.uid)

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }else{
                    Toast.makeText(this@LoginActivity, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception){
                Toast.makeText(this@LoginActivity, "Ошибка входа: ${e.message}", Toast.LENGTH_SHORT).show()
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