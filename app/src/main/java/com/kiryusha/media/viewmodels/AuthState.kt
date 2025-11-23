package com.kiryusha.media.viewmodels

import com.kiryusha.media.database.entities.User

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class LoginSuccess(val user: User, val rememberMe: Boolean) : AuthState()
    object RegistrationSuccess : AuthState()
    object LoggedOut : AuthState()
    data class Error(val message: String) : AuthState()
}