package com.kiryusha.media.ui.screens.settings

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiryusha.media.viewmodels.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val notificationSoundEnabled by viewModel.notificationSoundEnabled.collectAsState()
    val showPlaybackNotifications by viewModel.showPlaybackNotifications.collectAsState()

    var storageInfo by remember { mutableStateOf("Calculating...") }

    LaunchedEffect(Unit) {
        storageInfo = withContext(Dispatchers.IO) {
            calculateAudioStorage(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingsItem(
                        icon = Icons.Filled.Notifications,
                        title = "Enable Notifications",
                        subtitle = "Allow the app to show notifications",
                        trailing = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                            )
                        }
                    )

                    HorizontalDivider()

                    SettingsItem(
                        icon = Icons.Filled.MusicNote,
                        title = "Playback Notifications",
                        subtitle = "Show now playing notifications",
                        trailing = {
                            Switch(
                                checked = showPlaybackNotifications,
                                onCheckedChange = { viewModel.setShowPlaybackNotifications(it) },
                                enabled = notificationsEnabled
                            )
                        }
                    )

                    HorizontalDivider()

                    SettingsItem(
                        icon = Icons.Filled.VolumeUp,
                        title = "Notification Sounds",
                        subtitle = "Play sound for notifications",
                        trailing = {
                            Switch(
                                checked = notificationSoundEnabled,
                                onCheckedChange = { viewModel.setNotificationSoundEnabled(it) },
                                enabled = notificationsEnabled
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Storage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingsItem(
                        icon = Icons.Filled.Storage,
                        title = "Storage Used",
                        subtitle = storageInfo
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "App Info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingsItem(
                        icon = Icons.Filled.Info,
                        title = "Version",
                        subtitle = "1.0.0"
                    )
                }
            }
        }
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
            totalSize == 0L -> "0 MB"
            totalSize < 1024 -> "${totalSize}B"
            totalSize < 1024 * 1024 -> String.format("%.1fKB", totalSize / 1024f)
            totalSize < 1024 * 1024 * 1024 -> String.format("%.1fMB", totalSize / (1024f * 1024f))
            else -> String.format("%.2fGB", totalSize / (1024f * 1024f * 1024f))
        }
    } catch (e: Exception) {
        "Unable to calculate"
    }
}
