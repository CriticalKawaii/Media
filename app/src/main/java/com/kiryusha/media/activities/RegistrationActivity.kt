package com.kiryusha.media.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.kiryusha.media.R
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kiryusha.media.MediaApplication
import com.kiryusha.media.database.entities.User
import com.kiryusha.media.repository.UserRepository
import com.kiryusha.media.utils.SecurityUtils
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kiryusha.media.utils.ValidationUtils

class RegistrationActivity : AppCompatActivity() {
    private lateinit var repository: UserRepository
    private lateinit var etLogin: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var tilLogin: TextInputLayout
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        repository = UserRepository(MediaApplication.database.userDao())

        etLogin = findViewById(R.id.et_login)
        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        tilLogin = findViewById(R.id.til_login)
        tilUsername = findViewById(R.id.til_username)
        tilPassword = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)

        val registerButton = findViewById<MaterialButton>(R.id.btn_register)
        val loginLink = findViewById<TextView>(R.id.tv_login)

        registerButton.setOnClickListener {
            if (ValidationUtils.isValidEmail(etLogin.text.toString().trim()) &&
                ValidationUtils.isValidUsername(etUsername.text.toString().trim()).first &&
                ValidationUtils.validatePassword(etPassword.text.toString()).first &&
                ValidationUtils.doPasswordsMatch(etPassword.text.toString(), etConfirmPassword.text.toString())
            ) {
                registerUser()
            }
        }

        loginLink.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val login = etLogin.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString()

        lifecycleScope.launch {
            try{
                if (repository.isLoginTaken(login)){
                    tilLogin.error = "Этот email уже зарегистрирован"
                    Toast.makeText(this@RegistrationActivity, "Email уже используется", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val hashedPassword = SecurityUtils.hashPassword(password)
                val user = User(
                    login = login,
                    userName = username,
                    password = hashedPassword
                )

                val success = repository.registerUser(user)

                if (success) {
                    Toast.makeText(
                        this@RegistrationActivity,
                        "Регистрация успешна! Теперь войдите в систему",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                } else {
                    Toast.makeText(
                        this@RegistrationActivity,
                        "Ошибка регистрации. Попробуйте снова ",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception){
                Toast.makeText(
                    this@RegistrationActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}