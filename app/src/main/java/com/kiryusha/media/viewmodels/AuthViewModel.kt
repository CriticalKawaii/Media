package com.kiryusha.media.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiryusha.media.database.entities.User
import com.kiryusha.media.repository.UserRepository
import com.kiryusha.media.utils.SecurityUtils
import com.kiryusha.media.utils.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    fun login(
        login: String,
        password: String,
        rememberMe: Boolean = false
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val errors = validateLogin(login, password)
            if (errors.isNotEmpty()) {
                _validationErrors.value = errors
                _authState.value = AuthState.Error("Пожалуйста, исправьте ошибки валидации")
                return@launch
            }

            try {
                val hashedPassword = SecurityUtils.hashPassword(password)
                val user = userRepository.loginUser(login, hashedPassword)

                if (user != null) {
                    _currentUser.value = user
                    _authState.value = AuthState.LoginSuccess(user, rememberMe)
                    _validationErrors.value = emptyMap()
                } else {
                    _authState.value = AuthState.Error("Неверный логин или пароль")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Ошибка входа: ${e.message}")
            }
        }
    }

    fun register(
        login: String,
        username: String,
        password: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val errors = validateRegistration(login, username, password, confirmPassword)
            if (errors.isNotEmpty()) {
                _validationErrors.value = errors
                _authState.value = AuthState.Error("Пожалуйста, исправьте ошибки валидации")
                return@launch
            }

            try {
                if (userRepository.isLoginTaken(login)) {
                    _validationErrors.value = mapOf("login" to "Этот email уже зарегистрирован")
                    _authState.value = AuthState.Error("Email уже используется")
                    return@launch
                }

                val hashedPassword = SecurityUtils.hashPassword(password)
                val user = User(
                    login = login,
                    userName = username,
                    password = hashedPassword
                )

                val success = userRepository.registerUser(user)
                if (success) {
                    _authState.value = AuthState.RegistrationSuccess
                    _validationErrors.value = emptyMap()
                } else {
                    _authState.value = AuthState.Error("Ошибка регистрации")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Ошибка: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _currentUser.value = null
            _authState.value = AuthState.LoggedOut
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
        _validationErrors.value = emptyMap()
    }

    private fun validateLogin(login: String, password: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (login.isBlank()) {
            errors["login"] = "Введите email или имя пользователя"
        }

        if (password.isBlank()) {
            errors["password"] = "Введите пароль"
        }

        return errors
    }

    private fun validateRegistration(
        login: String,
        username: String,
        password: String,
        confirmPassword: String
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (!ValidationUtils.isValidEmail(login)) {
            errors["login"] = if (login.isEmpty()) {
                "Введите email"
            } else {
                "Введите корректный email"
            }
        }

        val usernameValidation = ValidationUtils.isValidUsername(username)
        if (!usernameValidation.first) {
            errors["username"] = usernameValidation.second ?: "Некорректное имя пользователя"
        }

        val passwordValidation = ValidationUtils.validatePassword(password)
        if (!passwordValidation.first) {
            errors["password"] = passwordValidation.second ?: "Некорректный пароль"
        }

        if (confirmPassword.isEmpty()) {
            errors["confirmPassword"] = "Подтвердите пароль"
        } else if (!ValidationUtils.doPasswordsMatch(password, confirmPassword)) {
            errors["confirmPassword"] = "Пароли не совпадают"
        }

        return errors
    }
}