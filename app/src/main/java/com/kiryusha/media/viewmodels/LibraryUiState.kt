package com.kiryusha.media.viewmodels

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    object Scanning : LibraryUiState()
    object Empty : LibraryUiState()
    data class Loaded(val trackCount: Int) : LibraryUiState()
    data class Success(val message: String) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}