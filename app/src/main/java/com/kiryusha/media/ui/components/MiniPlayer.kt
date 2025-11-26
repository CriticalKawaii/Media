package com.kiryusha.media.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.viewmodels.PlayerViewModel

@Composable
fun MiniPlayer(
    onNavigateToPlayer: () -> Unit,
    playerViewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val progress by playerViewModel.progress.collectAsState()

    currentTrack?.let { track ->
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onNavigateToPlayer),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Progress indicator
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album art
                    AsyncImage(
                        model = track.albumArtUri,
                        contentDescription = "Album art",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Track info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Play/Pause button
                    IconButton(
                        onClick = { playerViewModel.togglePlayPause() }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Skip next button
                    IconButton(
                        onClick = { playerViewModel.skipNext() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip next",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
