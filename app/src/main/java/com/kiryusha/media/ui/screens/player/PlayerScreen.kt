package com.kiryusha.media.ui.screens.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kiryusha.media.viewmodels.PlayerViewModel
import com.kiryusha.media.viewmodels.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

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
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Filled.Share, "Share")
                    }
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Filled.MoreVert, "More")
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
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Album Art
                    AnimatedAlbumArt(
                        albumArtUri = track.albumArtUri,
                        isPlaying = isPlaying
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Track Info
                    TrackInfo(
                        title = track.title,
                        artist = track.artist
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress Bar
                    ProgressBar(
                        progress = progress,
                        currentPosition = currentPosition,
                        duration = track.durationMs,
                        onSeek = { newPosition ->
                            viewModel.seekTo(newPosition)
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Playback Controls
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
                        IconButton(onClick = { /* Favorite */ }) {
                            Icon(
                                if (track.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                "Favorite"
                            )
                        }
                    }
                }
            } ?: run {
                // No track playing
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
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
    }
}

@Composable
fun AnimatedAlbumArt(
    albumArtUri: String?,
    isPlaying: Boolean
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
                .clip(RoundedCornerShape(16.dp))
                .clickable { /* Expand album art */ },
            contentScale = ContentScale.Crop
        )
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
            maxLines = 1
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
            value = progress,
            onValueChange = { newProgress ->
                val newPosition = (duration * newProgress).toLong()
                onSeek(newPosition)
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
