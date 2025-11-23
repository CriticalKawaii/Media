package com.kiryusha.media.database.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation


data class TrackWithPlaylists(
    @Embedded val track: Track,
    @Relation(
        parentColumn = "trackId",
        entityColumn = "playlistId",
        associateBy = Junction(
            PlaylistTrackCrossRef::class,
            parentColumn = "trackId",
            entityColumn = "playlistId"
        )
    )
    val playlists: List<Playlist>
)