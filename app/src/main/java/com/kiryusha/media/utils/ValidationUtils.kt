package com.kiryusha.media.utils

import android.util.Patterns

object ValidationUtils {
    fun isValidEmail(email: String): Boolean{
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validatePassword(password: String): Pair<Boolean, String?>{
        return when {
            password.isEmpty() -> Pair(false, "Пароль не может быть пустым")
            password.length < 8 -> Pair(false, "Пароль должен содержать минимум 8 символов")
            !password.any { it.isDigit() } -> Pair(false, "Пароль должен содержать хотя бы одну цифру")
            !password.any { it.isLetter() } -> Pair(false,"Пароль должен содержать хотя бы одну букву")
            else -> Pair(true, null)
        }
    }

    fun isValidUsername(username: String): Pair<Boolean, String?>{
        return when {
            username.isEmpty() -> Pair(false, "Имя пользователя не может быть пустым")
            username.length < 3 -> Pair(false, "Имя должно содержать минимум 3 символа")
            username.length > 20 -> Pair(false, "Имя должно содержать максимум 20 символов")
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) ->
                Pair(false, "Используйте только буквы, цифры и подчеркивание")
            else -> Pair(true, null)
        }
    }

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean{
        return password == confirmPassword && password.isNotEmpty()
    }
}