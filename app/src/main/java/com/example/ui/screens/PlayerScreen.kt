package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.MusicViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit
) {
    val song = viewModel.currentSong

    if (song == null) {
        // Fallback empty state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = onNavigateBack) {
                Text("Back to Library")
            }
        }
        return
    }

    // Beautiful continuous rotation animation for album record when playing
    val transition = rememberInfiniteTransition(label = "Record rotation")
    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    // Pulsating scale animation matching song play activity
    val scaleFactor by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = SineTransitionSpec),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "এখন বাজছে (Now Playing)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BengalGreenPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BengalGreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Artistic Animated Album Record Disk ---
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(if (viewModel.isPlaying) scaleFactor else 1.0f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(song.coverColorStart), Color(song.coverColorEnd), Color.Black)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl grooves lines inside the circle and center hole
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f))
                )
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(BengalGold.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- Song Information ---
            Text(
                text = song.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = BengalGreenPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().testTag("player_song_title")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                fontSize = 16.sp,
                color = GrayText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().testTag("player_song_artist")
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Album: ${song.album}",
                fontSize = 12.sp,
                color = GrayText.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Audio Seek Bar ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = viewModel.playbackPositionMs.toFloat().coerceIn(0f, song.durationMs.toFloat()),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..song.durationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        activeTrackColor = BengalGreenPrimary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = BengalGold
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("player_progress_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(viewModel.playbackPositionMs),
                        fontSize = 12.sp,
                        color = GrayText
                    )
                    Text(
                        text = formatTime(song.durationMs),
                        fontSize = 12.sp,
                        color = GrayText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Playback Controller Cluster ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = { viewModel.isShuffleEnabled = !viewModel.isShuffleEnabled },
                    modifier = Modifier.testTag("player_shuffle_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (viewModel.isShuffleEnabled) BengalGold else GrayText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = { viewModel.previousSong() },
                    modifier = Modifier.testTag("player_prev")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Song",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Play / Pause Circle Action Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(BengalGreenPrimary)
                        .scale(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.fillMaxSize().testTag("player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Next
                IconButton(
                    onClick = { viewModel.nextSong() },
                    modifier = Modifier.testTag("player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Song",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Repeat Button
                IconButton(
                    onClick = { viewModel.isRepeatEnabled = !viewModel.isRepeatEnabled },
                    modifier = Modifier.testTag("player_repeat_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (viewModel.isRepeatEnabled) BengalGold else GrayText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stop control
            IconButton(
                onClick = { viewModel.stopSong() },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(40.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop Playback", tint = BengalCrimson)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- AI Lyrics (Translations) Panel ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = BengalSurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Waves, contentDescription = null, tint = BengalGold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "লিরিক্সের অর্থ ও উদ্দীপনা (AI Lyrical Inspiration)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BengalGreenPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (viewModel.aiLyricsLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = BengalGreenPrimary
                        )
                    } else {
                        Text(
                            text = viewModel.aiLyricsSnippet,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            color = TextLight.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- AI Similar Recommendations Panel ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = BengalSurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = BengalGold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "অনুরূপ গানের পরামর্শ (AI Recommendations)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BengalGreenPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (viewModel.aiRecommendationLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BengalGreenPrimary, strokeWidth = 2.dp)
                        }
                    } else {
                        Text(
                            text = viewModel.aiRecommendation,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = TextLight.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// Sine scale spec for procedural pulsating
private val SineTransitionSpec = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

fun formatTime(milliseconds: Long): String {
    val totalSecs = milliseconds / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format("%02d:%02d", minutes, seconds)
}
