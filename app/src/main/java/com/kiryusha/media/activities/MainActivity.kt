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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
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
import com.kiryusha.media.repository.UserRepository
import com.kiryusha.media.ui.components.MiniPlayer
import com.kiryusha.media.ui.theme.MediaTheme
import com.kiryusha.media.utils.AppPreferences
import com.kiryusha.media.utils.MediaScanner
import com.kiryusha.media.utils.MusicPlayerController
import com.kiryusha.media.viewmodels.LibraryViewModel
import com.kiryusha.media.viewmodels.PlayerViewModel
import com.kiryusha.media.viewmodels.PlaylistViewModel
import com.kiryusha.media.viewmodels.ProfileViewModel
import com.kiryusha.media.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var playerViewModel: PlayerViewModel
    private lateinit var playlistViewModel: PlaylistViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var appPreferences: AppPreferences
    private lateinit var musicPlayerController: MusicPlayerController

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appPreferences = AppPreferences(this)

        musicPlayerController = MusicPlayerController(this)
        musicPlayerController.bindService()

        val database = MediaApplication.database
        val musicRepository = MusicRepository(
            database.trackDao(),
            database.playbackHistoryDao()
        )
        val playlistRepository = PlaylistRepository(database.playlistDao())
        val userRepository = UserRepository(database.userDao())
        val mediaScanner = MediaScanner(this)

        libraryViewModel = LibraryViewModel(musicRepository, mediaScanner)
        playerViewModel = PlayerViewModel(musicRepository, musicPlayerController)
        playlistViewModel = PlaylistViewModel(playlistRepository)
        profileViewModel = ProfileViewModel(userRepository, musicRepository, playlistRepository)
        settingsViewModel = SettingsViewModel(appPreferences)

        setContent {
            var currentUserId by remember { mutableStateOf(-1) }

            LaunchedEffect(Unit) {
                val userId = appPreferences.getUserId().first() ?: -1
                currentUserId = userId
                if (userId != -1) {
                    playerViewModel.setUserId(userId)
                    playlistViewModel.setUserId(userId)
                    profileViewModel.loadUserProfile(userId)
                }
            }

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
                        profileViewModel = profileViewModel,
                        settingsViewModel = settingsViewModel,
                        userId = currentUserId,
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

    private fun handleLogout() {
        lifecycleScope.launch {
            appPreferences.clearSession()
            musicPlayerController.release()
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayerController.unbindService()
    }
}

@Composable
fun MainScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    profileViewModel: ProfileViewModel,
    settingsViewModel: SettingsViewModel,
    userId: Int,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            Column {
                // MiniPlayer above bottom navigation
                MiniPlayer(
                    onNavigateToPlayer = {
                        navController.navigate(Screen.Player.route) {
                            launchSingleTop = true
                        }
                    },
                    playerViewModel = playerViewModel
                )

                // Bottom Navigation Bar (without Player tab)
                NavigationBar {
                    val items = listOf(
                        BottomNavItem("Library", Screen.Library.route, Icons.Filled.Home),
                        BottomNavItem("Playlists", Screen.Playlists.route, Icons.Filled.List),
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
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AppNavigation(
                navController = navController,
                libraryViewModel = libraryViewModel,
                playerViewModel = playerViewModel,
                playlistViewModel = playlistViewModel,
                profileViewModel = profileViewModel,
                settingsViewModel = settingsViewModel,
                userId = userId,
                onLogout = onLogout,
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
            horizontalAlignment = Alignment.CenterHorizontally
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
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}


