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
        })
    }

    fun playTrack(track: Track) {
        val mediaItem = MediaItem.Builder()
            .setUri(track.filePath)
            .setMediaId(track.trackId.toString())
            .build()

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
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
        exoPlayer?.seekToNext()
    }

    fun skipToPrevious() {
        exoPlayer?.seekToPrevious()
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
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.filePath)
                .setMediaId(track.trackId.toString())
                .build()
        }

        exoPlayer?.apply {
            setMediaItems(mediaItems, startIndex, 0)
            prepare()
            play()
        }
    }

    fun release() {
        unbindService()
        exoPlayer?.release()
    }
}
