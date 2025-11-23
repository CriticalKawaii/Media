package com.kiryusha.media.database.entities

data class Album(
    val name: String,
    val artist: String,
    val coverUri: String?,
    val tracks: List<Track>,
    val year: String? = null
)