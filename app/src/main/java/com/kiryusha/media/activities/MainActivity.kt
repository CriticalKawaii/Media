package com.kiryusha.media.activities

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kiryusha.media.MediaApplication
import com.kiryusha.media.navigation.AppNavigation
import com.kiryusha.media.navigation.Screen
import com.kiryusha.media.repository.MusicRepository
import com.kiryusha.media.repository.PlaylistRepository
import com.kiryusha.media.ui.theme.MediaTheme
import com.kiryusha.media.utils.MediaScanner
import com.kiryusha.media.viewmodels.LibraryViewModel
import com.kiryusha.media.viewmodels.PlayerViewModel
import com.kiryusha.media.viewmodels.PlaylistViewModel

class MainActivity : ComponentActivity() {

    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var playerViewModel: PlayerViewModel
    private lateinit var playlistViewModel: PlaylistViewModel

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewModels
        val database = MediaApplication.database
        val musicRepository = MusicRepository(
            database.trackDao(),
            database.playbackHistoryDao()
        )
        val playlistRepository = PlaylistRepository(database.playlistDao())
        val mediaScanner = MediaScanner(this)

        libraryViewModel = LibraryViewModel(musicRepository, mediaScanner)
        playerViewModel = PlayerViewModel(musicRepository)
        playlistViewModel = PlaylistViewModel(playlistRepository)

        // Get user ID from SharedPreferences
        val userId = getUserId()
        if (userId != -1) {
            playerViewModel.setUserId(userId)
            playlistViewModel.setUserId(userId)
        }

        setContent {
            MediaTheme {
                val permissionState = rememberMultiplePermissionsState(
                    permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        listOf(Manifest.permission.READ_MEDIA_AUDIO)
                    } else {
                        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                )

                LaunchedEffect(Unit) {
                    permissionState.launchMultiplePermissionRequest()
                }

                if (permissionState.allPermissionsGranted) {
                    MainScreen(
                        libraryViewModel = libraryViewModel,
                        playerViewModel = playerViewModel,
                        playlistViewModel = playlistViewModel,
                        onLogout = { handleLogout() }
                    )
                } else {
                    PermissionRequiredScreen(
                        onRequestPermission = {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    )
                }
            }
        }
    }

    private fun getUserId(): Int {
        return getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getInt("user_id", -1)
    }

    private fun handleLogout() {
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit {
            clear()
        }
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

@Composable
fun MainScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val items = listOf(
                    BottomNavItem("Library", Screen.Library.route, Icons.Filled.Home),
                    BottomNavItem("Playlists", Screen.Playlists.route, Icons.Filled.List),
                    BottomNavItem("Player", Screen.Player.route, Icons.Filled.PlayArrow),
                    BottomNavItem("Profile", Screen.Profile.route, Icons.Filled.Person)
                )

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AppNavigation(
                navController = navController,
                startDestination = Screen.Library.route
            )
        }
    }
}

@Composable
fun PermissionRequiredScreen(onRequestPermission: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Permission Required",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Permission Required",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This app needs access to your media files to play music.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}
