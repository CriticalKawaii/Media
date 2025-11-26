package com.kiryusha.media.ui.screens.profile

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiryusha.media.utils.AppPreferences
import com.kiryusha.media.viewmodels.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    userId: Int,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val userStats by viewModel.userStats.collectAsState()
    val playlistCount by viewModel.playlistCount.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var storageInfo by remember { mutableStateOf("Calculating...") }

    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }

    LaunchedEffect(Unit) {
        storageInfo = withContext(Dispatchers.IO) {
            calculateAudioStorage(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ProfileHeader(
                username = currentUser?.userName ?: "User",
                email = currentUser?.login ?: "user@example.com"
            )

            Spacer(modifier = Modifier.height(24.dp))

            StatsCard(
                totalTracks = userStats?.totalTracks ?: 0,
                totalPlaylists = playlistCount,
                totalPlaytime = viewModel.formatPlaytime(userStats?.totalPlays ?: 0)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsItem(
                icon = Icons.Filled.Storage,
                title = "Storage Used",
                subtitle = storageInfo,
                onClick = { }
            )

            HorizontalDivider()

            SettingsItem(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                subtitle = "Manage notification preferences",
                onClick = {
                    android.widget.Toast.makeText(
                        context,
                        "Notification settings coming soon",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )

            HorizontalDivider()

            SettingsItem(
                icon = Icons.Filled.Info,
                title = "About",
                subtitle = "Version 1.0.0",
                onClick = { showAboutDialog = true }
            )

            HorizontalDivider()

            SettingsItem(
                icon = Icons.Filled.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "View our privacy policy",
                onClick = {
                    android.widget.Toast.makeText(
                        context,
                        "Privacy policy will be displayed here",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )

            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to logout?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Logout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Settings Dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Settings") },
                text = {
                    Column {
                        Text("App Settings", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Version: 1.0.0")
                        Text("• Storage: $storageInfo")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Additional settings can be configured below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // About Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About Media Player") },
                text = {
                    Column {
                        Text("Version 1.0.0", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "A modern music player for Android with playlist management, " +
                            "favorites, and playback history tracking.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "© 2024 Media Player Team",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileHeader(
    username: String,
    email: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatsCard(
    totalTracks: Int,
    totalPlaylists: Int,
    totalPlaytime: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = totalTracks.toString(),
                    label = "Tracks"
                )
                StatItem(
                    value = totalPlaylists.toString(),
                    label = "Playlists"
                )
                StatItem(
                    value = totalPlaytime,
                    label = "Playtime"
                )
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = trailing,
        modifier = if (onClick != null) {
            Modifier.clickableWithoutRipple(onClick = onClick)
        } else {
            Modifier
        }
    )
}

@Composable
fun Modifier.clickableWithoutRipple(onClick: () -> Unit): Modifier {
    return this.clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    )
}

private fun calculateAudioStorage(context: Context): String {
    return try {
        val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val totalSize = musicDir?.walkTopDown()
            ?.filter { it.isFile }
            ?.sumOf { it.length() } ?: 0L

        when {
            totalSize < 1024 -> "${totalSize}B"
            totalSize < 1024 * 1024 -> "${totalSize / 1024}KB"
            totalSize < 1024 * 1024 * 1024 -> String.format("%.1fMB", totalSize / (1024f * 1024f))
            else -> String.format("%.2fGB", totalSize / (1024f * 1024f * 1024f))
        }
    } catch (e: Exception) {
        "Unable to calculate"
    }
}
