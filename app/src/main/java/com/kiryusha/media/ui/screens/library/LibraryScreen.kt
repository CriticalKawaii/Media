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
import com.kiryusha.media.utils.OnboardingSteps
import com.kiryusha.media.utils.TooltipManager
import com.kiryusha.media.utils.rememberTooltipManager
import com.kiryusha.media.viewmodels.LibraryUiState
import com.kiryusha.media.viewmodels.LibraryViewModel
import com.kiryusha.media.viewmodels.PlaylistViewModel
import com.kiryusha.media.viewmodels.ViewMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
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
//                    EmptyLibraryView(
//                        onScanClick = { viewModel.scanMediaFiles() }
//                    )
                }
                is LibraryUiState.Error -> {
//                    ErrorView(
//                        message = (uiState as LibraryUiState.Error).message,
//                        onRetry = { viewModel.loadLibrary() }
//                    )
                }
                else -> {
                    Column {
                        // Show long press tooltip
                        if (showLongPressTooltip && tooltipManager.shouldShow(TooltipManager.TOOLTIP_LONG_PRESS_TRACK)) {
//                            ContextTooltip(
//                                message = "Long press any track to add it to a playlist or see more options",
//                                onDismiss = { showLongPressTooltip = false },
//                                onDontShowAgain = {
//                                    tooltipManager.dontShowAgain(TooltipManager.TOOLTIP_LONG_PRESS_TRACK)
//                                    showLongPressTooltip = false
//                                }
//                            )
                        }

                        if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                            TrackListView(
                                tracks = searchResults,
                                onTrackClick = onTrackClick,
                                onTrackLongClick = { track ->
                                    showAddToPlaylistDialog = track
                                    showLongPressTooltip = true
                                }
                            )
                        } else {
                            when (viewMode) {

                                ViewMode.ALBUMS -> {
                                    //Unresolved reference 'AlbumGridView'.
//                                    AlbumGridView(
//                                        albums = albums,
//                                        onAlbumClick = { album ->
//                                            viewModel.viewModelScope.launch {
//                                                val fullAlbum = viewModel.musicRepository.getAlbumWithTracks(album.name)
//                                                if (fullAlbum != null) {
//                                                    onAlbumClick(fullAlbum)
//                                                }
//                                            }
//                                        }
//                                    )
                                }
                                ViewMode.TRACKS -> {
                                    TrackListView(
                                        tracks = tracks,
                                        onTrackClick = onTrackClick,
                                        onTrackLongClick = { track ->
                                            showAddToPlaylistDialog = track
                                        }
                                    )
                                }
                                ViewMode.ARTISTS -> {
                                    Text("Artists view - Coming soon")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Onboarding dialog
        if (showOnboarding && currentOnboardingStep < OnboardingSteps.steps.size) {
//            TooltipDialog(
//                step = OnboardingSteps.steps[currentOnboardingStep],
//                currentStep = currentOnboardingStep,
//                totalSteps = OnboardingSteps.steps.size,
//                onNext = {
//                    if (currentOnboardingStep == OnboardingSteps.steps.size - 1) {
//                        tooltipManager.completeOnboarding()
//                        showOnboarding = false
//                    } else {
//                        currentOnboardingStep++
//                    }
//                },
//                onSkip = {
//                    tooltipManager.completeOnboarding()
//                    showOnboarding = false
//                },
//                onDismiss = {
//                    showOnboarding = false
//                }
//            )
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
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    var offsetX by remember { mutableStateOf(0f) }
    var showMenu by remember { mutableStateOf(false) }

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
                text = { Text("Add to playlist") },
                onClick = {
                    onLongClick?.invoke()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = { showMenu = false },
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