package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.MusicViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylists: () -> Unit
) {
    val context = LocalContext.current
    var isAddPlaylistDialogOpen by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }

    // Permission Launcher for scanning local files
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.updateSearchQuery(viewModel.searchQuery) // reloads with local songs
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(audioPermission)
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.background)
                        )
                    )
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                // Cultural header title of Bangladesh: Amar Gaan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Sunrise emblem / glowing golden dotara musical indicator
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BengalCrimson),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Ektara Logo",
                                tint = BengalGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "আমার গান", // "Amar Gaan" in Bangla
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = BengalGreenPrimary
                            )
                            Text(
                                text = "Amar Gaan • Bengali Rhythms",
                                fontSize = 12.sp,
                                color = GrayText
                            )
                        }
                    }
                    
                    // Playlist button in top header
                    IconButton(
                        onClick = onNavigateToPlaylists,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .testTag("playlists_navigation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "My Playlists",
                            tint = BengalGreenPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Modern search input rounded
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search songs or Baul artists...", color = GrayText) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = BengalGreenPrimary) },
                    trailingIcon = {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = BengalGreenPrimary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BengalGreenPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp) // space for MiniPlayer
            ) {
                // --- Favorites Lanes ---
                val favorites = viewModel.songsList.filter { viewModel.favoriteSongIds.contains(it.id) }
                if (favorites.isNotEmpty()) {
                    item {
                        SectionHeader(title = "পছন্দের গান (Favorites)")
                        LazyRow(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favorites) { song ->
                                FavoriteCard(
                                    song = song,
                                    onClick = { viewModel.selectAndPlaySong(song, favorites) }
                                )
                            }
                        }
                    }
                }

                // --- Recently Played Lane ---
                if (viewModel.recentlyPlayedSongs.isNotEmpty()) {
                    item {
                        SectionHeader(title = "সম্প্রতি বাজানো (Recently Played)")
                        LazyRow(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(viewModel.recentlyPlayedSongs) { song ->
                                RecentCard(
                                    song = song,
                                    onClick = { viewModel.selectAndPlaySong(song, viewModel.recentlyPlayedSongs) }
                                )
                            }
                        }
                    }
                }

                // --- All Songs Title ---
                item {
                    SectionHeader(title = if (viewModel.searchQuery.isEmpty()) "সব গান (All Songs)" else "অনুসন্ধানের ফলাফল (Search Results)")
                }

                if (viewModel.songsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MusicOff,
                                    contentDescription = "Empty",
                                    tint = GrayText,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No offline audio files found.",
                                    color = GrayText,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    items(viewModel.songsList) { song ->
                        SongListItem(
                            song = song,
                            isPlaying = viewModel.currentSong?.id == song.id,
                            isFavorited = viewModel.favoriteSongIds.contains(song.id),
                            onPlay = { viewModel.selectAndPlaySong(song, viewModel.songsList) },
                            onToggleFavorite = { viewModel.toggleFavorite(song.id) },
                            onAddToPlaylist = { selectedSongForPlaylist = song }
                        )
                    }
                }
            }

            // Floating Mini-Player persistent overlay
            if (viewModel.currentSong != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
                ) {
                    MiniPlayer(
                        viewModel = viewModel,
                        onClick = onNavigateToPlayer
                    )
                }
            }
        }
    }

    // Modal Sheet or Dialog for Custom Playlist selection
    if (selectedSongForPlaylist != null) {
        AlertDialog(
            onDismissRequest = { selectedSongForPlaylist = null },
            title = { Text("প্লেলিস্টে যুক্ত করুন", fontWeight = FontWeight.Bold, color = BengalGreenPrimary) }, // "Add to playlist"
            text = {
                Column {
                    Text(
                        text = "Choose a playlist to save '${selectedSongForPlaylist?.title}':",
                        fontSize = 14.sp,
                        color = GrayText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (viewModel.playlists.isEmpty()) {
                        Text(
                            text = "No custom playlists created yet.",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = GrayText,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(viewModel.playlists) { playlist ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            selectedSongForPlaylist?.id?.let { songId ->
                                                viewModel.addSongToPlaylist(playlist.id, songId)
                                            }
                                            selectedSongForPlaylist = null
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.QueueMusic,
                                            contentDescription = null,
                                            tint = BengalGreenPrimary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = playlist.name)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            isAddPlaylistDialogOpen = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BengalGreenPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Playlist")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedSongForPlaylist = null }) {
                    Text("Close", color = BengalCrimson)
                }
            }
        )
    }

    // Dialog to construct a playlist name
    if (isAddPlaylistDialogOpen) {
        var playlistNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { isAddPlaylistDialogOpen = false },
            title = { Text("নতুন প্লেলিস্ট তৈরি করুন") }, // "Create new playlist"
            text = {
                OutlinedTextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BengalGreenPrimary,
                        focusedLabelColor = BengalGreenPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            viewModel.createPlaylist(playlistNameInput)
                        }
                        isAddPlaylistDialogOpen = false
                    }
                ) {
                    Text("Save", color = BengalGreenPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddPlaylistDialogOpen = false }) {
                    Text("Cancel", color = BengalCrimson)
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun SongListItem(
    song: Song,
    isPlaying: Boolean,
    isFavorited: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onPlay)
            .testTag("song_item_${song.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art using custom gradients of Bengal Gold, Crimson and Green
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(song.coverColorStart), Color(song.coverColorEnd))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = BengalGold,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = TextLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isPlaying) BengalGreenPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    color = GrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorited) BengalCrimson else GrayText
                    )
                }

                IconButton(onClick = onAddToPlaylist) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = BengalGreenPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteCard(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
            .testTag("favorite_card_${song.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(song.coverColorStart), Color(song.coverColorEnd))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = BengalCrimson.copy(alpha = 0.9f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecentCard(
    song: Song,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
            .testTag("recent_card_${song.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(song.coverColorStart), Color(song.coverColorEnd))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = TextLight,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = GrayText,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MiniPlayer(
    viewModel: MusicViewModel,
    onClick: () -> Unit
) {
    val song = viewModel.currentSong ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("mini_player_surface"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BengalSurfaceDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(song.coverColorStart), Color(song.coverColorEnd))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextLight
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BengalGreenPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 12.sp,
                    color = TextLight.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Play/Pause Control
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = BengalGold
                )
            }

            // Quick Next Control
            IconButton(
                onClick = { viewModel.nextSong() }
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = TextLight
                )
            }
        }
    }
}
