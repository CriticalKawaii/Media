package com.kiryusha.media.utils

import android.content.Context

class TooltipManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("tooltips", Context.MODE_PRIVATE)

    fun shouldShow(tooltipId: String): Boolean {
        return !prefs.getBoolean(tooltipId, false)
    }

    fun markAsShown(tooltipId: String) {
        prefs.edit().putBoolean(tooltipId, true).apply()
    }
}