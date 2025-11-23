package com.kiryusha.media.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["playlistId"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(entity = Track::class, parentColumns = ["trackId"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long,
    val position: Int
)