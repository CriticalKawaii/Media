package com.kiryusha.media.ui.screens.settings

import android.app.Activity
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiryusha.media.R
import com.kiryusha.media.utils.LocaleManager
import com.kiryusha.media.viewmodels.SettingsViewModel

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
    val darkTheme by viewModel.darkTheme.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_title))
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
                text = stringResource(R.string.settings_appearance),
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
                        title = stringResource(R.string.settings_theme),
                        subtitle = when (themeMode) {
                            "light" -> stringResource(R.string.settings_theme_light)
                            "dark" -> stringResource(R.string.settings_theme_dark)
                            "system" -> stringResource(R.string.settings_theme_system)
                            else -> stringResource(R.string.settings_theme_system)
                        },
                        onClick = { showThemeDialog = true }
                    )

                    HorizontalDivider()

                    SettingsItem(
                        icon = Icons.Filled.Language,
                        title = stringResource(R.string.settings_language),
                        subtitle = when (language) {
                            "en" -> stringResource(R.string.settings_language_en)
                            "ru" -> stringResource(R.string.settings_language_ru)
                            "zh" -> stringResource(R.string.settings_language_zh)
                            else -> stringResource(R.string.settings_language_en)
                        },
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.settings_notifications),
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
                        title = stringResource(R.string.settings_notifications_enable),
                        subtitle = stringResource(R.string.settings_notifications_enable_desc),
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
                        title = stringResource(R.string.settings_notifications_playback),
                        subtitle = stringResource(R.string.settings_notifications_playback_desc),
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
                        title = stringResource(R.string.settings_notifications_sound),
                        subtitle = stringResource(R.string.settings_notifications_sound_desc),
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
                text = stringResource(R.string.settings_app_info),
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
                        title = stringResource(R.string.settings_version),
                        subtitle = "1.0.0"
                    )
                }
            }
        }

        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = themeMode,
                onThemeSelected = { newTheme ->
                    viewModel.setThemeMode(newTheme)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = language,
                onLanguageSelected = { newLanguage ->
                    viewModel.setLanguage(newLanguage)
                    showLanguageDialog = false
                    LocaleManager.updateAppLocale(context, newLanguage)
                    (context as? Activity)?.recreate()
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
        title = { Text(stringResource(R.string.settings_select_language)) },
        text = {
            Column {
                LanguageOption(
                    language = "en",
                    displayName = stringResource(R.string.settings_language_en),
                    isSelected = currentLanguage == "en",
                    onSelect = { onLanguageSelected("en") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LanguageOption(
                    language = "ru",
                    displayName = stringResource(R.string.settings_language_ru),
                    isSelected = currentLanguage == "ru",
                    onSelect = { onLanguageSelected("ru") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LanguageOption(
                    language = "zh",
                    displayName = stringResource(R.string.settings_language_zh),
                    isSelected = currentLanguage == "zh",
                    onSelect = { onLanguageSelected("zh") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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

@Composable
fun ThemeSelectionDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_select_theme)) },
        text = {
            Column {
                ThemeOption(
                    theme = "light",
                    displayName = stringResource(R.string.settings_theme_light),
                    isSelected = currentTheme == "light",
                    onSelect = { onThemeSelected("light") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThemeOption(
                    theme = "dark",
                    displayName = stringResource(R.string.settings_theme_dark),
                    isSelected = currentTheme == "dark",
                    onSelect = { onThemeSelected("dark") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThemeOption(
                    theme = "system",
                    displayName = stringResource(R.string.settings_theme_system),
                    isSelected = currentTheme == "system",
                    onSelect = { onThemeSelected("system") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ThemeOption(
    theme: String,
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
