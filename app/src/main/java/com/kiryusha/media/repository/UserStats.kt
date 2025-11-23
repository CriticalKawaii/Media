package com.kiryusha.media.repository

data class UserStats(
    val totalTracks: Int,
    val totalPlays: Int,
    val uniqueTracksPlayed: Int
)