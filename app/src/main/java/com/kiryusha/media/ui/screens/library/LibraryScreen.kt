package com.kiryusha.media.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kiryusha.media.database.entities.Album
import com.kiryusha.media.database.entities.PlaylistWithTracks
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.utils.OnboardingStep
import com.kiryusha.media.utils.OnboardingSteps
import com.kiryusha.media.utils.TooltipManager
import com.kiryusha.media.utils.rememberTooltipManager
import com.kiryusha.media.viewmodels.LibraryUiState
import com.kiryusha.media.viewmodels.LibraryViewModel
import com.kiryusha.media.viewmodels.PlayerViewModel
import com.kiryusha.media.viewmodels.PlaylistViewModel
import com.kiryusha.media.viewmodels.ViewMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onTrackClick: (Track) -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val tracks by viewModel.allTracks.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val playlists by playlistViewModel.userPlaylists.collectAsState()

    var showSearchBar by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf<Track?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    // Tooltip management
    val tooltipManager = rememberTooltipManager()
    var currentOnboardingStep by remember { mutableStateOf(0) }
    var showOnboarding by remember { mutableStateOf(tooltipManager.shouldShowOnboarding()) }
    var showLongPressTooltip by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            if (showSearchBar) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onSearch = { active = false },
                    active = active,
                    onActiveChange = { active = it },
                    placeholder = { Text("Search...") }
                ) {

                }
            } else {
                TopAppBar(
                    title = { Text("Library") },
                    actions = {
                        // View mode toggle
                        IconButton(onClick = {
                            viewModel.cycleViewMode()
                        }) {
                            Icon(
                                when(viewMode) {
                                    ViewMode.ALBUMS -> Icons.Filled.GridView
                                    ViewMode.TRACKS -> Icons.Filled.List
                                    ViewMode.ARTISTS -> Icons.Filled.Person
                                },
                                "View Mode"
                            )
                        }

                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Filled.Search, "Search")
                        }

                        IconButton(onClick = { viewModel.scanMediaFiles() }) {
                            Icon(Icons.Filled.Refresh, "Scan Media")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is LibraryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is LibraryUiState.Scanning -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Scanning for music files...")
                        }
                    }
                }
                is LibraryUiState.Empty -> {
                    EmptyLibraryView(
                        onScanClick = { viewModel.scanMediaFiles() }
                    )
                }
                is LibraryUiState.Error -> {
                    ErrorView(
                        message = (uiState as LibraryUiState.Error).message,
                        onRetry = { viewModel.loadLibrary() }
                    )
                }
                else -> {
                    Column {
                        // Show long press tooltip
                        if (showLongPressTooltip && tooltipManager.shouldShow(TooltipManager.TOOLTIP_LONG_PRESS_TRACK)) {
                            ContextTooltip(
                                message = "Long press any track to add it to a playlist or see more options",
                                onDismiss = { showLongPressTooltip = false },
                                onDontShowAgain = {
                                    tooltipManager.dontShowAgain(TooltipManager.TOOLTIP_LONG_PRESS_TRACK)
                                    showLongPressTooltip = false
                                }
                            )
                        }

                        if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                            TrackListView(
                                tracks = searchResults,
                                playerViewModel = playerViewModel,
                                onTrackClick = onTrackClick,
                                onTrackLongClick = { track ->
                                    showAddToPlaylistDialog = track
                                    showLongPressTooltip = true
                                }
                            )
                        } else {
                            when (viewMode) {

                                ViewMode.ALBUMS -> {
                                    AlbumGridView(
                                        albums = albums,
                                        onAlbumClick = { album ->
                                            viewModel.viewModelScope.launch {
                                                val fullAlbum = viewModel.musicRepository.getAlbumWithTracks(album.name)
                                                if (fullAlbum != null) {
                                                    onAlbumClick(fullAlbum)
                                                }
                                            }
                                        }
                                    )
                                }
                                ViewMode.TRACKS -> {
                                    TrackListView(
                                        tracks = tracks,
                                        playerViewModel = playerViewModel,
                                        onTrackClick = onTrackClick,
                                        onTrackLongClick = { track ->
                                            showAddToPlaylistDialog = track
                                        }
                                    )
                                }
                                ViewMode.ARTISTS -> {
                                    ArtistsView(
                                        tracks = tracks,
                                        onArtistClick = { artistName ->
                                            // Filter tracks by artist and play them
                                            val artistTracks = tracks.filter { it.artist == artistName }
                                            if (artistTracks.isNotEmpty()) {
                                                onTrackClick(artistTracks.first())
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Onboarding dialog
        if (showOnboarding && currentOnboardingStep < OnboardingSteps.steps.size) {
            TooltipDialog(
                step = OnboardingSteps.steps[currentOnboardingStep],
                currentStep = currentOnboardingStep,
                totalSteps = OnboardingSteps.steps.size,
                onNext = {
                    if (currentOnboardingStep == OnboardingSteps.steps.size - 1) {
                        tooltipManager.completeOnboarding()
                        showOnboarding = false
                    } else {
                        currentOnboardingStep++
                    }
                },
                onSkip = {
                    tooltipManager.completeOnboarding()
                    showOnboarding = false
                },
                onDismiss = {
                    showOnboarding = false
                }
            )
        }

        // Add to playlist dialog
        showAddToPlaylistDialog?.let { track ->
            AddToPlaylistDialog(
                track = track,
                playlists = playlists,
                onDismiss = { showAddToPlaylistDialog = null },
                onAddToPlaylist = { playlistId ->
                    playlistViewModel.addTrackToPlaylist(playlistId, track.trackId)
                    showAddToPlaylistDialog = null
                },
                onCreateNewPlaylist = {
                    showCreatePlaylistDialog = true
                }
            )
        }

        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onConfirm = { name, description ->
                    playlistViewModel.createPlaylist(name, description)
                    showCreatePlaylistDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackListView(
    tracks: List<Track>,
    playerViewModel: PlayerViewModel,
    onTrackClick: (Track) -> Unit,
    onTrackLongClick: ((Track) -> Unit)? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tracks) { track ->
            SwipeableTrackItem(
                track = track,
                playerViewModel = playerViewModel,
                onClick = { onTrackClick(track) },
                onLongClick = { onTrackLongClick?.invoke(track) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableTrackItem(
    track: Track,
    playerViewModel: PlayerViewModel,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    var offsetX by remember { mutableStateOf(0f) }
    var showMenu by remember { mutableStateOf(false) }
    var showQueueToast by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = offsetX
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(-200f, 200f)
                        }
                    )
                }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = track.albumArtUri,
                contentDescription = track.title,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = track.getDurationFormatted(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, "More options")
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Play next") },
                onClick = {
                    playerViewModel.addNextInQueue(track)
                    showMenu = false
                    android.widget.Toast.makeText(
                        context,
                        "\"${track.title}\" will play next",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                leadingIcon = {
                    Icon(Icons.Filled.PlaylistPlay, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Add to queue") },
                onClick = {
                    playerViewModel.addToQueue(track)
                    showMenu = false
                    android.widget.Toast.makeText(
                        context,
                        "\"${track.title}\" added to queue",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                leadingIcon = {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Add to playlist") },
                onClick = {
                    onLongClick?.invoke()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Filled.LibraryMusic, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    showMenu = false
                    val shareIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, "Check out: ${track.title} by ${track.artist}")
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share track"))
                },
                leadingIcon = {
                    Icon(Icons.Filled.Share, contentDescription = null)
                }
            )
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<PlaylistWithTracks>,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            Column {
                Text("Add \"${track.title}\" to:")
                Spacer(modifier = Modifier.height(16.dp))

                playlists.forEach { playlist ->
                    TextButton(
                        onClick = { onAddToPlaylist(playlist.playlist.playlistId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(playlist.playlist.name)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onCreateNewPlaylist,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Playlist")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), description.trim().ifBlank { null })
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AlbumGridView(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums) { album ->
            AlbumItem(
                album = album,
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

@Composable
fun AlbumItem(
    album: Album,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = album.coverUri,
                contentDescription = album.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryView(onScanClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = "No music",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Music Found",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the button below to scan your device for music files",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onScanClick) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan for Music")
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Oops!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

@Composable
fun ContextTooltip(
    message: String,
    onDismiss: () -> Unit,
    onDontShowAgain: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDontShowAgain) {
                    Text("Don't show again")
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Got it")
                }
            }
        }
    }
}

@Composable
fun TooltipDialog(
    step: OnboardingStep,
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(step.title)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Step ${currentStep + 1} of $totalSteps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Text(step.message)
        },
        confirmButton = {
            Button(onClick = onNext) {
                Text(if (currentStep == totalSteps - 1) "Finish" else "Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        }
    )
}

@Composable
fun ArtistsView(
    tracks: List<Track>,
    onArtistClick: (String) -> Unit
) {
    val artistsWithCounts = remember(tracks) {
        tracks.groupBy { it.artist }
            .map { (artist, artistTracks) ->
                ArtistInfo(
                    name = artist,
                    trackCount = artistTracks.size,
                    albumArtUri = artistTracks.firstOrNull()?.albumArtUri
                )
            }
            .sortedBy { it.name }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(artistsWithCounts) { artist ->
            ArtistItem(
                artistInfo = artist,
                onClick = { onArtistClick(artist.name) }
            )
        }
    }
}

data class ArtistInfo(
    val name: String,
    val trackCount: Int,
    val albumArtUri: String?
)

@Composable
fun ArtistItem(
    artistInfo: ArtistInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = artistInfo.albumArtUri,
                    contentDescription = artistInfo.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = artistInfo.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${artistInfo.trackCount} ${if (artistInfo.trackCount == 1) "track" else "tracks"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}