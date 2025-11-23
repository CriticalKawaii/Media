package com.kiryusha.media.navigation

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Player : Screen("player")
    object Playlists : Screen("playlists")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
    object Profile : Screen("profile")
}