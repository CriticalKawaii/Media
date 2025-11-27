package com.kiryusha.media.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.kiryusha.media.R
import com.kiryusha.media.activities.MainActivity
import com.kiryusha.media.utils.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var notificationManager: NotificationManager
    private lateinit var appPreferences: AppPreferences
    private val binder = MusicBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_player_channel"
        private const val CHANNEL_NAME = "Music Playback"

        private const val ACTION_PLAY_PAUSE = "com.kiryusha.media.action.PLAY_PAUSE"
        private const val ACTION_PREVIOUS = "com.kiryusha.media.action.PREVIOUS"
        private const val ACTION_NEXT = "com.kiryusha.media.action.NEXT"
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    private val notificationActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                }
                ACTION_PREVIOUS -> {
                    if (player.hasPreviousMediaItem()) {
                        player.seekToPreviousMediaItem()
                    }
                }
                ACTION_NEXT -> {
                    if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        appPreferences = AppPreferences(this)
        player = ExoPlayer.Builder(this).build()

        mediaSession = MediaSession.Builder(this, player)
            .build()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // Register notification action receiver
        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_PREVIOUS)
            addAction(ACTION_NEXT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationActionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(notificationActionReceiver, filter)
        }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                serviceScope.launch {
                    if (shouldShowNotification()) {
                        if (isPlaying) {
                            startForeground(NOTIFICATION_ID, createNotification())
                        } else {
                            // Update notification to show play button when paused
                            notificationManager.notify(NOTIFICATION_ID, createNotification())
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        serviceScope.launch {
                            if (shouldShowNotification()) {
                                notificationManager.notify(NOTIFICATION_ID, createNotification())
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        if (player.hasNextMediaItem()) {
                            player.seekToNextMediaItem()
                        }
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                serviceScope.launch {
                    if (shouldShowNotification()) {
                        notificationManager.notify(NOTIFICATION_ID, createNotification())
                    }
                }
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(notificationActionReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was already unregistered
        }
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    fun getPlayer(): ExoPlayer = player

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun shouldShowNotification(): Boolean {
        val notificationsEnabled = appPreferences.areNotificationsEnabled().first()
        val showPlaybackNotifications = appPreferences.shouldShowPlaybackNotifications().first()
        return notificationsEnabled && showPlaybackNotifications
    }

    @OptIn(UnstableApi::class)
    private suspend fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val currentItem = player.currentMediaItem
        val title = currentItem?.mediaMetadata?.title?.toString() ?: "Music Player"
        val artist = currentItem?.mediaMetadata?.artist?.toString() ?: ""

        val notificationSoundEnabled = appPreferences.isNotificationSoundEnabled().first()

        // Create notification actions
        val previousIntent = Intent(ACTION_PREVIOUS).apply {
            setPackage(packageName)
        }
        val previousPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            previousIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val previousAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_previous,
            "Previous",
            previousPendingIntent
        ).build()

        val playPauseIntent = Intent(ACTION_PLAY_PAUSE).apply {
            setPackage(packageName)
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            playPauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPauseIcon = if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseAction = NotificationCompat.Action.Builder(
            playPauseIcon,
            if (player.isPlaying) "Pause" else "Play",
            playPausePendingIntent
        ).build()

        val nextIntent = Intent(ACTION_NEXT).apply {
            setPackage(packageName)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_next,
            "Next",
            nextPendingIntent
        ).build()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionCompatToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        if (!notificationSoundEnabled) {
            builder.setSilent(true)
        }

        // Load and set album artwork
        val artworkUri = currentItem?.mediaMetadata?.artworkUri
        if (artworkUri != null) {
            val bitmap = loadArtwork(artworkUri.toString())
            if (bitmap != null) {
                builder.setLargeIcon(bitmap)
            }
        }

        return builder.build()
    }

    private suspend fun loadArtwork(uri: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val imageLoader = ImageLoader(this@MusicPlayerService)
            val request = ImageRequest.Builder(this@MusicPlayerService)
                .data(uri)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

}
