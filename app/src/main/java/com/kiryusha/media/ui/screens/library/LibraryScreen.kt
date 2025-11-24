package com.kiryusha.media.ui.screens.library

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kiryusha.media.database.entities.Album
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.viewmodels.LibraryUiState
import com.kiryusha.media.viewmodels.LibraryViewModel
import com.kiryusha.media.viewmodels.ViewMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    onTrackClick: (Track) -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val tracks by viewModel.allTracks.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var showSearchBar by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            if (showSearchBar) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onClose = {
                        showSearchBar = false
                        viewModel.setSearchQuery("")
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Library") },
                    actions = {
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
                is LibraryUiState.Loading, is LibraryUiState.Scanning -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
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
                    if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                        TrackListView(
                            tracks = searchResults,
                            onTrackClick = onTrackClick
                        )
                    } else {
                        when (viewMode) {
                            ViewMode.ALBUMS -> {
                                AlbumGridView(
                                    albums = albums,
                                    onAlbumClick = onAlbumClick
                                )
                            }
                            ViewMode.TRACKS -> {
                                TrackListView(
                                    tracks = tracks,
                                    onTrackClick = onTrackClick
                                )
                            }
                            ViewMode.ARTISTS -> {
                                // TODO: Artists view
                                Text("Artists view - Coming soon")
                            }
                        }
                    }
                }
            }
        }
    }
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
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = album.coverUri,
                contentDescription = album.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
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
fun TrackListView(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tracks) { track ->
            TrackItem(
                track = track,
                onClick = { onTrackClick(track) }
            )
        }
    }
}

@Composable
fun TrackItem(
    track: Track,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search tracks...") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.ArrowBack, "Close search")
            }
        }
    )
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
            imageVector = Icons.Filled.Info,
            contentDescription = "Empty library",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No music found",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Scan your device to find music files",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onScanClick) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Device")
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
