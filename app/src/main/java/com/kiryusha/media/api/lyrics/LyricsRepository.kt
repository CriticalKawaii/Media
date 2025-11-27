package com.kiryusha.media.api.lyrics

import android.util.Log
import com.kiryusha.media.api.NetworkModule
import com.kiryusha.media.database.dao.TrackDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/**
 * Repository for managing lyrics fetching and caching
 */
class LyricsRepository(private val trackDao: TrackDao) {

    private val apiService = NetworkModule.lyricsApiService

    /**
     * Fetches lyrics for a track, using cache if available
     * @param trackId The track ID from the database
     * @param artist The artist name
     * @param title The song title
     * @return LyricsResult indicating success, error, or not found
     */
    suspend fun getLyrics(trackId: Long, artist: String, title: String): LyricsResult {
        return withContext(Dispatchers.IO) {
            try {
                // First check if lyrics are already cached in database
                val track = trackDao.getTrackById(trackId)
                if (track?.lyrics != null) {
                    Log.d("LyricsRepository", "Returning cached lyrics for: $title by $artist")
                    return@withContext LyricsResult.Success(track.lyrics)
                }

                // If not cached, fetch from API
                Log.d("LyricsRepository", "Fetching lyrics from API for: $title by $artist")

                // Clean and encode artist and title for URL
                val cleanArtist = cleanString(artist)
                val cleanTitle = cleanString(title)

                val response = apiService.getLyrics(cleanArtist, cleanTitle)

                if (response.isSuccessful) {
                    val lyrics = response.body()?.lyrics
                    if (!lyrics.isNullOrBlank()) {
                        // Cache the lyrics in database
                        trackDao.updateLyrics(trackId, lyrics)
                        Log.d("LyricsRepository", "Successfully fetched and cached lyrics")
                        LyricsResult.Success(lyrics)
                    } else {
                        Log.w("LyricsRepository", "Lyrics not found for: $title by $artist")
                        LyricsResult.NotFound
                    }
                } else {
                    Log.e("LyricsRepository", "API error: ${response.code()} - ${response.message()}")
                    LyricsResult.Error("Failed to fetch lyrics: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("LyricsRepository", "Exception fetching lyrics", e)
                LyricsResult.Error("Network error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    /**
     * Cleans a string for use in API URLs
     * Removes special characters and extra whitespace
     */
    private fun cleanString(input: String): String {
        // Remove content in parentheses (like "feat.", "Remastered", etc.)
        var cleaned = input.replace(Regex("\\([^)]*\\)"), "")
        // Remove content in brackets
        cleaned = cleaned.replace(Regex("\\[[^]]*]"), "")
        // Remove special characters except spaces, hyphens, and apostrophes
        cleaned = cleaned.replace(Regex("[^a-zA-Z0-9\\s'-]"), "")
        // Trim and encode for URL
        return URLEncoder.encode(cleaned.trim(), "UTF-8")
    }

    /**
     * Clears cached lyrics for a track
     */
    suspend fun clearLyrics(trackId: Long) {
        withContext(Dispatchers.IO) {
            trackDao.updateLyrics(trackId, null)
        }
    }
}
