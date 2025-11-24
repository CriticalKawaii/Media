package com.kiryusha.media.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kiryusha.media.viewmodels.ProfileViewModel

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

    var showLogoutDialog by remember { mutableStateOf(false) }
    var darkTheme by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") }
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
            // Profile Header
            ProfileHeader(
                username = currentUser?.userName ?: "User",
                email = currentUser?.login ?: "user@example.com"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Statistics Card
            StatsCard(
                totalTracks = userStats?.totalTracks ?: 0,
                totalPlaylists = playlistCount,
                totalPlaytime = viewModel.formatPlaytime(userStats?.totalPlays ?: 0)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Settings Section
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Theme Toggle
            SettingsItem(
                icon = Icons.Filled.Face,
                title = "Dark Theme",
                trailing = {
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = { darkTheme = it }
                    )
                }
            )

            Divider()

            // Storage
            SettingsItem(
                icon = Icons.Filled.Info,
                title = "Storage Used",
                subtitle = "2.4 GB of audio files",
                onClick = { }
            )

            Divider()

            // About
            SettingsItem(
                icon = Icons.Filled.Info,
                title = "About",
                subtitle = "Version 1.0.0",
                onClick = { }
            )

            Divider()

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Filled.ExitToApp, "Logout")
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
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        }
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
            // Avatar
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
    return this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            onClick = onClick
        )
    )
}

