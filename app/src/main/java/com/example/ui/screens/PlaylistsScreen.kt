package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.PlaylistEntity
import com.example.data.model.Song
import com.example.ui.MusicViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var isNewPlaylistDialogOpen by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedPlaylist == null) "আমার প্লেলিস্ট (My Playlists)" else selectedPlaylist!!.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BengalGreenPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedPlaylist != null) {
                                selectedPlaylist = null
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BengalGreenPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (selectedPlaylist == null) {
                FloatingActionButton(
                    onClick = { isNewPlaylistDialogOpen = true },
                    containerColor = BengalGreenPrimary,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("create_playlist_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Playlist")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedPlaylist == null) {
                // List of custom user Playlists
                if (viewModel.playlists.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Empty",
                                tint = GrayText,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No custom playlists. Create one beneath!",
                                color = GrayText,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(viewModel.playlists) { playlist ->
                            PlaylistListItem(
                                playlist = playlist,
                                onClick = { selectedPlaylist = playlist },
                                onDelete = { viewModel.deletePlaylist(playlist.id) }
                            )
                        }
                    }
                }
            } else {
                // Playlist Content details
                val playlistSongsState = viewModel.getSongsInPlaylistFlow(selectedPlaylist!!.id)
                    .collectAsState(initial = emptyList())

                val playlistSongsList = playlistSongsState.value

                if (playlistSongsList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Empty",
                                tint = GrayText,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Playlist is empty. Add songs on Home screen!",
                                color = GrayText,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
                    ) {
                        items(playlistSongsList) { song ->
                            PlaylistSongListItem(
                                song = song,
                                isPlaying = viewModel.currentSong?.id == song.id,
                                onPlay = {
                                    viewModel.selectAndPlaySong(song, playlistSongsList)
                                },
                                onRemove = {
                                    viewModel.removeSongFromPlaylist(selectedPlaylist!!.id, song.id)
                                }
                            )
                        }
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
                        onClick = {
                            selectedPlaylist = null
                            onNavigateBack() // jumps out to allow central player view
                        }
                    )
                }
            }
        }
    }

    if (isNewPlaylistDialogOpen) {
        AlertDialog(
            onDismissRequest = { isNewPlaylistDialogOpen = false },
            title = { Text("নতুন প্লেলিস্ট (New Playlist)", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
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
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                        }
                        isNewPlaylistDialogOpen = false
                    }
                ) {
                    Text("Create", color = BengalGreenPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewPlaylistDialogOpen = false }) {
                    Text("Cancel", color = BengalCrimson)
                }
            }
        )
    }
}

@Composable
fun PlaylistListItem(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick)
            .testTag("playlist_item_${playlist.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(BengalGreenPrimary, BengalTeal)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = TextLight,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Persisted Custom Playlist",
                    fontSize = 12.sp,
                    color = GrayText
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_playlist_${playlist.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Playlist",
                    tint = BengalCrimson
                )
            }
        }
    }
}

@Composable
fun PlaylistSongListItem(
    song: Song,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isPlaying) BengalGreenPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 12.sp,
                    color = GrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.PlaylistRemove,
                    contentDescription = "Remove From Playlist",
                    tint = BengalCrimson
                )
            }
        }
    }
}
