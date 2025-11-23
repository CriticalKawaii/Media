package com.kiryusha.media.viewmodels

sealed class PlayerState {
    object Idle : PlayerState()
    object Playing : PlayerState()
    object Paused : PlayerState()
    object Stopped : PlayerState()
    data class Error(val message: String) : PlayerState()
}