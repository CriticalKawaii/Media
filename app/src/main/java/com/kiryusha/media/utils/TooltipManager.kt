package com.kiryusha.media.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class TooltipManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("tooltips", Context.MODE_PRIVATE)

    companion object {
        const val TOOLTIP_WELCOME = "welcome_tooltip"
        const val TOOLTIP_SCAN_LIBRARY = "scan_library_tooltip"
        const val TOOLTIP_PLAYER_SWIPE = "player_swipe_tooltip"
        const val TOOLTIP_ADD_TO_PLAYLIST = "add_to_playlist_tooltip"

        const val TOOLTIP_LONG_PRESS_TRACK = "long_press_track"
        const val TOOLTIP_SHUFFLE_MODE = "shuffle_mode"
        const val TOOLTIP_REORDER_PLAYLIST = "reorder_playlist"
        const val TOOLTIP_SWIPE_DELETE = "swipe_delete"

        const val ONBOARDING_COMPLETED = "onboarding_completed"
    }

    fun shouldShow(tooltipId: String): Boolean {
        return !prefs.getBoolean(tooltipId, false)
    }

    fun markAsShown(tooltipId: String) {
        prefs.edit().putBoolean(tooltipId, true).apply()
    }

    fun shouldShowOnboarding(): Boolean {
        return !prefs.getBoolean(ONBOARDING_COMPLETED, false)
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean(ONBOARDING_COMPLETED, true).apply()
    }

    fun resetAllTooltips() {
        prefs.edit().clear().apply()
    }

    fun dontShowAgain(tooltipId: String) {
        markAsShown(tooltipId)
    }
}

@Composable
fun rememberTooltipManager(): TooltipManager {
    val context = LocalContext.current
    return remember { TooltipManager(context) }
}

data class OnboardingStep(
    val id: String,
    val title: String,
    val message: String,
    val targetScreen: String? = null
)

object OnboardingSteps {
    val steps = listOf(
        OnboardingStep(
            id = TooltipManager.TOOLTIP_WELCOME,
            title = "Welcome to Media Player!",
            message = "Let's take a quick tour of the app",
            targetScreen = "library"
        ),
        OnboardingStep(
            id = TooltipManager.TOOLTIP_SCAN_LIBRARY,
            title = "Scan Your Music",
            message = "Tap the refresh button to scan your device for music files",
            targetScreen = "library"
        ),
        OnboardingStep(
            id = TooltipManager.TOOLTIP_PLAYER_SWIPE,
            title = "Swipe to Navigate",
            message = "Swipe left/right in the player to change tracks",
            targetScreen = "player"
        ),
        OnboardingStep(
            id = TooltipManager.TOOLTIP_ADD_TO_PLAYLIST,
            title = "Create Playlists",
            message = "Long press any track to add it to a playlist",
            targetScreen = "library"
        )
    )
}
