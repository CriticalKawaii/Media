package com.kiryusha.media.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.service.MusicPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MusicPlayerController(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private var playerService: MusicPlayerService? = null
    private var isBound = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _currentMediaItemIndex = MutableStateFlow(0)
    val currentMediaItemIndex: StateFlow<Int> = _currentMediaItemIndex

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicPlayerService.MusicBinder
            playerService = binder?.getService()
            exoPlayer = playerService?.getPlayer()
            setupPlayerListener()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerService = null
            exoPlayer = null
            isBound = false
        }
    }

    fun bindService() {
        val intent = Intent(context, MusicPlayerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        context.startService(intent)
    }

    fun unbindService() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun setupPlayerListener() {
        exoPlayer?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = exoPlayer?.duration ?: 0L
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentMediaItemIndex.value = exoPlayer?.currentMediaItemIndex ?: 0
                _duration.value = exoPlayer?.duration ?: 0L
            }
        })

        // Initialize current media item index
        _currentMediaItemIndex.value = exoPlayer?.currentMediaItemIndex ?: 0
    }

    fun playTrack(track: Track) {
        exoPlayer?.let { player ->
            try {
                val mediaItem = MediaItem.Builder()
                    .setUri(track.filePath.toUri())
                    .setMediaId(track.trackId.toString())
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setArtworkUri(track.albumArtUri?.toUri())
                            .build()
                    )
                    .build()

                player.apply {
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }

    fun skipToNext() {
        exoPlayer?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            }
        }
    }

    fun skipToPrevious() {
        exoPlayer?.let { player ->
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            }
        }
    }

    fun skipToIndex(index: Int) {
        exoPlayer?.let { player ->
            if (index >= 0 && index < player.mediaItemCount) {
                player.seekTo(index, 0)
            }
        }
    }

    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }

    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }

    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    fun setPlaylist(tracks: List<Track>, startIndex: Int) {
        exoPlayer?.let { player ->
            try {
                val mediaItems = tracks.map { track ->
                    MediaItem.Builder()
                        .setUri(track.filePath.toUri())
                        .setMediaId(track.trackId.toString())
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artist)
                                .setArtworkUri(track.albumArtUri?.toUri())
                                .build()
                        )
                        .build()
                }

                player.apply {
                    setMediaItems(mediaItems, startIndex, 0)
                    prepare()
                    play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addTrackToQueue(track: Track) {
        exoPlayer?.let { player ->
            try {
                val mediaItem = MediaItem.Builder()
                    .setUri(track.filePath.toUri())
                    .setMediaId(track.trackId.toString())
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setArtworkUri(track.albumArtUri?.toUri())
                            .build()
                    )
                    .build()

                player.addMediaItem(mediaItem)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addTracksToQueue(tracks: List<Track>) {
        exoPlayer?.let { player ->
            try {
                val mediaItems = tracks.map { track ->
                    MediaItem.Builder()
                        .setUri(track.filePath.toUri())
                        .setMediaId(track.trackId.toString())
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artist)
                                .setArtworkUri(track.albumArtUri?.toUri())
                                .build()
                        )
                        .build()
                }

                player.addMediaItems(mediaItems)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeTrackFromQueue(index: Int) {
        exoPlayer?.let { player ->
            try {
                if (index >= 0 && index < player.mediaItemCount) {
                    player.removeMediaItem(index)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getCurrentMediaItemIndex(): Int {
        return exoPlayer?.currentMediaItemIndex ?: 0
    }

    fun getMediaItemCount(): Int {
        return exoPlayer?.mediaItemCount ?: 0
    }

    fun setShuffleMode(enabled: Boolean) {
        exoPlayer?.shuffleModeEnabled = enabled
    }

    fun setRepeatMode(mode: Int) {
        exoPlayer?.repeatMode = mode
    }

    fun addTrackNext(track: Track) {
        exoPlayer?.let { player ->
            try {
                val mediaItem = MediaItem.Builder()
                    .setUri(track.filePath.toUri())
                    .setMediaId(track.trackId.toString())
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setArtworkUri(track.albumArtUri?.toUri())
                            .build()
                    )
                    .build()

                // Add after current item (current index + 1)
                val nextPosition = player.currentMediaItemIndex + 1
                player.addMediaItem(nextPosition, mediaItem)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addTracksNext(tracks: List<Track>) {
        exoPlayer?.let { player ->
            try {
                val mediaItems = tracks.map { track ->
                    MediaItem.Builder()
                        .setUri(track.filePath.toUri())
                        .setMediaId(track.trackId.toString())
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artist)
                                .setArtworkUri(track.albumArtUri?.toUri())
                                .build()
                        )
                        .build()
                }

                // Add after current item (current index + 1)
                val nextPosition = player.currentMediaItemIndex + 1
                player.addMediaItems(nextPosition, mediaItems)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        exoPlayer?.let { player ->
            try {
                if (fromIndex >= 0 && fromIndex < player.mediaItemCount &&
                    toIndex >= 0 && toIndex < player.mediaItemCount &&
                    fromIndex != toIndex) {
                    player.moveMediaItem(fromIndex, toIndex)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun release() {
        // Stop playback and clear the playlist
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        // Unbind from service - let the service manage its own lifecycle
        unbindService()
        // Don't release the player here as it's owned by the service
        exoPlayer = null
    }
}
