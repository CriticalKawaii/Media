package com.kiryusha.media.database.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation


data class PlaylistWithTracks(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "trackId",
        associateBy = Junction(
            PlaylistTrackCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "trackId"
        )
    )
    val tracks: List<Track>
)

