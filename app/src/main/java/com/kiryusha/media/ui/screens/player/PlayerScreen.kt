package com.kiryusha.media.ui.screens.playlists

import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kiryusha.media.database.entities.PlaylistWithTracks
import com.kiryusha.media.viewmodels.PlayerViewModel
import com.kiryusha.media.viewmodels.PlaylistUiState
import com.kiryusha.media.viewmodels.PlaylistViewModel
import com.kiryusha.media.viewmodels.RepeatMode
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    var offsetX by remember { mutableStateOf(0f) }
    var showExpandedArt by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        currentTrack?.let { track ->
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Now playing: ${track.title} by ${track.artist}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                        }
                    }) {
                        Icon(Icons.Filled.Share, "Share")
                    }
                    IconButton(onClick = { /* Queue */ }) {
                        Icon(Icons.Filled.PlaylistPlay, "Queue")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            currentTrack?.let { track ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (abs(offsetX) > 100) {
                                        if (offsetX > 0) {
                                            viewModel.skipPrevious()
                                        } else {
                                            viewModel.skipNext()
                                        }
                                    }
                                    offsetX = 0f
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount
                                }
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SwipeableAlbumArt(
                        albumArtUri = track.albumArtUri,
                        isPlaying = isPlaying,
                        offsetX = offsetX,
                        onClick = { showExpandedArt = true }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    TrackInfo(
                        title = track.title,
                        artist = track.artist
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ProgressBar(
                        progress = progress,
                        currentPosition = currentPosition,
                        duration = track.durationMs,
                        onSeek = { newPosition ->
                            viewModel.seekTo(newPosition)
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    PlaybackControls(
                        isPlaying = isPlaying,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipNext() },
                        onSkipPrevious = { viewModel.skipPrevious() },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.cycleRepeatMode() }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Additional Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { /* Add to playlist */ }) {
                            Icon(Icons.Filled.Add, "Add to playlist")
                        }
                        /* unresolved toggleFavorite
                        IconButton(onClick = {
                            viewModel.toggleFavorite(track.trackId, !track.isFavorite)
                        }) {
                            Icon(
                                if (track.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                "Favorite",
                                tint = if (track.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        */
                    }
                }
            } ?: run {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "No track",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No track playing",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }

        // Expanded Album Art Dialog
        if (showExpandedArt) {
            ExpandedAlbumArtDialog(
                albumArtUri = currentTrack?.albumArtUri,
                onDismiss = { showExpandedArt = false }
            )
        }
    }
}

@Composable
fun SwipeableAlbumArt(
    albumArtUri: String?,
    isPlaying: Boolean,
    offsetX: Float,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "album_art_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        AsyncImage(
            model = albumArtUri,
            contentDescription = "Album Art",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX * 0.5f
                    rotationY = offsetX * 0.05f
                }
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
            contentScale = ContentScale.Crop
        )

        // Swipe hint indicators
        if (abs(offsetX) > 50) {
            Icon(
                imageVector = if (offsetX > 0) Icons.Filled.SkipPrevious else Icons.Filled.SkipNext,
                contentDescription = null,
                modifier = Modifier
                    .align(if (offsetX > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(32.dp)
                    .size(48.dp)
                    .graphicsLayer {
                        alpha = (abs(offsetX) / 200f).coerceIn(0f, 1f)
                    },
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ExpandedAlbumArtDialog(
    albumArtUri: String?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = albumArtUri,
                contentDescription = "Expanded Album Art",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.FillWidth
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    "Close",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun TrackInfo(
    title: String,
    artist: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun ProgressBar(
    progress: Float,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    Column {
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = { newProgress ->
                if (duration > 0) {
                    val newPosition = (duration * newProgress).toLong()
                    onSeek(newPosition)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(onClick = onToggleShuffle) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Previous
        IconButton(
            onClick = onSkipPrevious,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(36.dp)
            )
        }

        // Play/Pause
        FloatingActionButton(
            onClick = onPlayPause,
            modifier = Modifier.size(64.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(32.dp)
            )
        }

        // Next
        IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(36.dp)
            )
        }

        // Repeat
        IconButton(onClick = onToggleRepeat) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.OFF -> Icons.Filled.Repeat
                    RepeatMode.ALL -> Icons.Filled.Repeat
                    RepeatMode.ONE -> Icons.Filled.RepeatOne
                },
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.OFF) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
