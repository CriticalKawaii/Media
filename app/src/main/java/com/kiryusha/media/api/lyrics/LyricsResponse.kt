package com.kiryusha.media.api.lyrics

import com.google.gson.annotations.SerializedName

/**
 * Response from Lyrics.ovh API
 */
data class LyricsOvhResponse(
    @SerializedName("lyrics")
    val lyrics: String?
)

/**
 * Sealed class representing the result of a lyrics fetch operation
 */
sealed class LyricsResult {
    data class Success(val lyrics: String) : LyricsResult()
    data class Error(val message: String) : LyricsResult()
    object NotFound : LyricsResult()
    object Loading : LyricsResult()
}
