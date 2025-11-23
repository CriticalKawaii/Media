package com.kiryusha.media.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kiryusha.media.ui.screens.library.LibraryScreen
import com.kiryusha.media.ui.screens.player.PlayerScreen
import com.kiryusha.media.ui.screens.playlists.PlaylistsScreen
import com.kiryusha.media.ui.screens.playlists.PlaylistDetailScreen
import com.kiryusha.media.ui.screens.profile.ProfileScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Library.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Library.route) {
            LibraryScreen(
                onTrackClick = { track ->
                    // Navigate to player or start playback
                    navController.navigate(Screen.Player.route)
                },
                onAlbumClick = { album ->
                    // Handle album click
                }
            )
        }

        composable(Screen.Player.route) {
            PlayerScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        composable(Screen.Playlists.route) {
            PlaylistsScreen(
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                },
                onCreatePlaylist = {
                    // Show create playlist dialog
                }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBackClick = {
                    navController.navigateUp()
                },
                onTrackClick = { track ->
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onLogout = {
                    // Handle logout - navigate back to login
                }
            )
        }
    }
}
