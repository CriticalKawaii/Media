package com.kiryusha.media.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kiryusha.media.ui.screens.library.LibraryScreen
import com.kiryusha.media.ui.screens.playlists.PlayerScreen
import com.kiryusha.media.ui.screens.playlists.PlaylistsScreen
import com.kiryusha.media.ui.screens.playlists.PlaylistDetailScreen
import com.kiryusha.media.ui.screens.profile.ProfileScreen
import com.kiryusha.media.viewmodels.LibraryViewModel
import com.kiryusha.media.viewmodels.PlayerViewModel
import com.kiryusha.media.viewmodels.PlaylistViewModel
import com.kiryusha.media.viewmodels.ProfileViewModel

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Player : Screen("player")
    object Playlists : Screen("playlists")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
    object Profile : Screen("profile")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    profileViewModel: ProfileViewModel,
    userId: Int,
    onLogout: () -> Unit,
    startDestination: String = Screen.Library.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Library.route) {
            LibraryScreen(
                viewModel = libraryViewModel,
                onTrackClick = { track ->
                    playerViewModel.playTrack(track)
                    navController.navigate(Screen.Player.route)
                },
                onAlbumClick = { album ->
                    // ✅ Need to load album tracks first
                    playerViewModel.setPlaylist(
                        album.tracks.ifEmpty {
                            // Load tracks from repository
                            emptyList()
                        },
                        0
                    )
                    if (album.tracks.isNotEmpty()) {
                        navController.navigate(Screen.Player.route)
                    }
                },
                playlistViewModel = playlistViewModel
            )
        }



        composable(Screen.Player.route) {
            PlayerScreen(
                viewModel = playerViewModel,
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        composable(Screen.Playlists.route) {
            PlaylistsScreen(
                viewModel = playlistViewModel,
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                },
                onCreatePlaylist = {
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
                viewModel = playlistViewModel,
                onBackClick = {
                    navController.navigateUp()
                },
                onTrackClick = { track ->
                    playerViewModel.playTrack(track)
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = profileViewModel,
                userId = userId,
                onLogout = onLogout
            )
        }
    }
}
