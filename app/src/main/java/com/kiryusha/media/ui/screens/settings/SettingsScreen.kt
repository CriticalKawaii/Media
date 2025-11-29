package com.kiryusha.media.ui.screens.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiryusha.media.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val notificationSoundEnabled by viewModel.notificationSoundEnabled.collectAsState()
    val showPlaybackNotifications by viewModel.showPlaybackNotifications.collectAsState()
    val darkTheme by viewModel.darkTheme.collectAsState()
    val language by viewModel.language.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }

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
            // Appearance Section
            Text(
                text = "Appearance",
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
                        icon = Icons.Filled.DarkMode,
                        title = "Dark Theme",
                        subtitle = "Enable dark mode",
                        trailing = {
                            Switch(
                                checked = darkTheme,
                                onCheckedChange = { viewModel.setDarkTheme(it) }
                            )
                        }
                    )

                    HorizontalDivider()

                    SettingsItem(
                        icon = Icons.Filled.Language,
                        title = "Language",
                        subtitle = when (language) {
                            "en" -> "English"
                            "ru" -> "Русский"
                            "zh" -> "中文"
                            else -> "English"
                        },
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notifications Section
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

            // App Info Section
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

        // Language Selection Dialog
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = language,
                onLanguageSelected = { newLanguage ->
                    viewModel.setLanguage(newLanguage)
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
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

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language") },
        text = {
            Column {
                LanguageOption(
                    language = "en",
                    displayName = "English",
                    isSelected = currentLanguage == "en",
                    onSelect = { onLanguageSelected("en") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LanguageOption(
                    language = "ru",
                    displayName = "Русский",
                    isSelected = currentLanguage == "ru",
                    onSelect = { onLanguageSelected("ru") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LanguageOption(
                    language = "zh",
                    displayName = "中文",
                    isSelected = currentLanguage == "zh",
                    onSelect = { onLanguageSelected("zh") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LanguageOption(
    language: String,
    displayName: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
