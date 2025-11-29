package com.kiryusha.media.activities

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.kiryusha.media.R
import androidx.lifecycle.lifecycleScope
import com.kiryusha.media.MediaApplication
import com.kiryusha.media.database.entities.User
import com.kiryusha.media.repository.UserRepository
import com.kiryusha.media.utils.SecurityUtils
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kiryusha.media.utils.ValidationUtils

class RegistrationActivity : BaseActivity() {
    private lateinit var repository: UserRepository
    private lateinit var etLogin: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var tilLogin: TextInputLayout
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var cbPrivacyPolicy: MaterialCheckBox
    private lateinit var tvPrivacyError: TextView

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
        cbPrivacyPolicy = findViewById(R.id.cb_privacy_policy)
        tvPrivacyError = findViewById(R.id.tv_privacy_error)

        val registerButton = findViewById<MaterialButton>(R.id.btn_register)
        val loginLink = findViewById<TextView>(R.id.tv_login)
        val privacyText = findViewById<TextView>(R.id.tv_privacy_policy)

        registerButton.setOnClickListener {
            if (validateInput()) {
                registerUser()
            }
        }

        loginLink.setOnClickListener {
            finish()
        }

        setupPrivacyPolicyText(privacyText)

        cbPrivacyPolicy.setOnCheckedChangeListener { _, _ ->
            tvPrivacyError.visibility = TextView.GONE
        }
    }

    private fun openPrivacyPolicy() {
        val url = "http://www.consultant.ru/document/cons_doc_LAW_61801/"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Не удалось открыть ссылку",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupPrivacyPolicyText(textView: TextView) {
        val fullText = "Я согласен на обработку персональных данных в соответствии с ФЗ-152"
        val spannableString = SpannableString(fullText)

        val linkText = "ФЗ-152"
        val startIndex = fullText.indexOf(linkText)
        val endIndex = startIndex + linkText.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openPrivacyPolicy()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = Color.parseColor("#6200EE") // Цвет ссылки (Primary color)
            }
        }

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun validateInput(): Boolean {
        var isValid = true

        val login = etLogin.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        tilLogin.error = null
        tilUsername.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        if (!ValidationUtils.isValidEmail(login)) {
            tilLogin.error = if (login.isEmpty()) {
                "Введите email"
            } else {
                "Введите корректный email"
            }
            isValid = false
        }

        val usernameValidation = ValidationUtils.isValidUsername(username)
        if (!usernameValidation.first) {
            tilUsername.error = usernameValidation.second
            isValid = false
        }

        val passwordValidation = ValidationUtils.validatePassword(password)
        if (!passwordValidation.first) {
            tilPassword.error = passwordValidation.second
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Подтвердите пароль"
            isValid = false
        } else if (!ValidationUtils.doPasswordsMatch(password, confirmPassword)) {
            tilConfirmPassword.error = "Пароли не совпадают"
            isValid = false
        }

        if (!cbPrivacyPolicy.isChecked) {
            tvPrivacyError.text = "Необходимо согласие на обработку персональных данных"
            tvPrivacyError.visibility = TextView.VISIBLE
            isValid = false
        }

        return isValid
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