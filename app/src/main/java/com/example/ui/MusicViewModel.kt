package com.example.ui

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.MusicDatabase
import com.example.data.database.PlaylistEntity
import com.example.data.model.Song
import com.example.data.repository.GeminiRepository
import com.example.data.repository.SongRepository
import com.example.player.BangladeshInstrumentSynthPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MusicViewModel(
    private val app: Application,
    private val songRepository: SongRepository,
    private val geminiRepository: GeminiRepository
) : AndroidViewModel(app) {

    // --- State Variables ---
    var songsList by mutableStateOf<List<Song>>(emptyList())
        private set

    var playlists by mutableStateOf<List<PlaylistEntity>>(emptyList())
        private set

    var favoriteSongIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var recentlyPlayedSongs by mutableStateOf<List<Song>>(emptyList())
        private set

    var currentSong by mutableStateOf<Song?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var playbackPositionMs by mutableStateOf(0L)
    
    var isShuffleEnabled by mutableStateOf(false)
    var isRepeatEnabled by mutableStateOf(false)

    var searchQuery by mutableStateOf("")

    // List of songs currently in the active playlist queue
    var activeQueue by mutableStateOf<List<Song>>(emptyList())
        private set

    // --- AI Suggestions / Lyrics States ---
    var aiRecommendation by mutableStateOf("")
        private set
    var aiRecommendationLoading by mutableStateOf(false)
        private set

    var aiLyricsSnippet by mutableStateOf("")
        private set
    var aiLyricsLoading by mutableStateOf(false)
        private set

    // --- Services ---
    private var mediaPlayer: MediaPlayer? = null
    private val synthPlayer = BangladeshInstrumentSynthPlayer()
    private var tickerJob: Job? = null

    init {
        // Load initial offline catalog & register database observers
        loadSongs()
        observeDatabase()
        startPlaybackProgressTicker()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            songRepository.getMergedSongsFlow(app, searchQuery)
                .catch { e -> Log.e("MusicViewModel", "Error loading songs", e) }
                .collectLatest { mergedList ->
                    songsList = mergedList
                    if (activeQueue.isEmpty()) {
                        activeQueue = mergedList
                    }
                }
        }
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            songRepository.allPlaylists.collect {
                playlists = it
            }
        }

        viewModelScope.launch {
            songRepository.favoriteSongIds.collect { ids ->
                favoriteSongIds = ids.toSet()
            }
        }

        viewModelScope.launch {
            // Join histry song ids to detailed custom Songs
            songRepository.recentlyPlayedSongIds.collect { ids ->
                recentlyPlayedSongs = ids.mapNotNull { id ->
                    songRepository.getSongById(app, id)
                }
            }
        }
    }

    // --- Media Controls ---
    fun selectAndPlaySong(song: Song, customQueue: List<Song> = songsList) {
        activeQueue = customQueue.ifEmpty { songsList }
        playSong(song)
    }

    private fun playSong(song: Song) {
        // Stop current playbacks
        synthPlayer.stopPlaying()
        mediaPlayer?.release()
        mediaPlayer = null

        currentSong = song
        isPlaying = true
        playbackPositionMs = 0L

        // Log to database history
        viewModelScope.launch {
            songRepository.addRecentlyPlayed(song.id)
        }

        if (song.isLocal) {
            // Execute physical offline media
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(app, Uri.parse(song.mediaUri))
                    prepare()
                    start()
                    setOnCompletionListener {
                        onTrackFinished()
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Failed to play local track: ${song.mediaUri}", e)
                isPlaying = false
            }
        } else {
            // Execute beautiful pure synthesis engine on preloaded index
            val songIndex = songRepository.getPreloadedSongs().indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            synthPlayer.startPlaying(songIndex, 0L) { progress ->
                playbackPositionMs = progress
                if (progress >= song.durationMs) {
                    onTrackFinished()
                }
            }
        }

        // Fetch AI recommendations & Lyrics in the background using Gemini!
        fetchAISuggestions(song)
    }

    fun togglePlayPause() {
        val song = currentSong ?: return
        if (isPlaying) {
            isPlaying = false
            if (song.isLocal) {
                mediaPlayer?.pause()
            } else {
                synthPlayer.pause()
            }
        } else {
            isPlaying = true
            if (song.isLocal) {
                mediaPlayer?.start()
            } else {
                synthPlayer.resume { progress ->
                    playbackPositionMs = progress
                    if (progress >= song.durationMs) {
                        onTrackFinished()
                    }
                }
            }
        }
    }

    fun stopSong() {
        isPlaying = false
        synthPlayer.stopPlaying()
        mediaPlayer?.release()
        mediaPlayer = null
        playbackPositionMs = 0L
    }

    fun nextSong() {
        if (activeQueue.isEmpty()) return
        val currentIndex = activeQueue.indexOfFirst { it.id == currentSong?.id }
        if (currentIndex == -1) {
            playSong(activeQueue.first())
            return
        }

        val nextIndex = if (isShuffleEnabled) {
            (activeQueue.indices).random()
        } else {
            (currentIndex + 1) % activeQueue.size
        }
        playSong(activeQueue[nextIndex])
    }

    fun previousSong() {
        if (activeQueue.isEmpty()) return
        val currentIndex = activeQueue.indexOfFirst { it.id == currentSong?.id }
        if (currentIndex == -1) {
            playSong(activeQueue.first())
            return
        }

        val prevIndex = if (isShuffleEnabled) {
            (activeQueue.indices).random()
        } else {
            if (currentIndex - 1 < 0) activeQueue.size - 1 else currentIndex - 1
        }
        playSong(activeQueue[prevIndex])
    }

    private fun onTrackFinished() {
        if (isRepeatEnabled) {
            currentSong?.let { playSong(it) }
        } else {
            nextSong()
        }
    }

    fun seekTo(positionMs: Long) {
        val song = currentSong ?: return
        playbackPositionMs = positionMs
        if (song.isLocal) {
            mediaPlayer?.seekTo(positionMs.toInt())
        } else {
            synthPlayer.seekTo(positionMs)
        }
    }

    fun toggleFavorite(songId: String) {
        viewModelScope.launch {
            songRepository.toggleFavorite(songId)
        }
    }

    // --- Playlist Actions ---
    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            songRepository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            songRepository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Int, songId: String) {
        viewModelScope.launch {
            songRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Int, songId: String) {
        viewModelScope.launch {
            songRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsInPlaylistFlow(playlistId: Int): Flow<List<Song>> {
        return songRepository.getSongsInPlaylist(playlistId).map { songIds ->
            songIds.mapNotNull { id ->
                songRepository.getSongById(app, id)
            }
        }.flowOn(Dispatchers.IO)
    }

    // --- Gemini API integrations ---
    private fun fetchAISuggestions(song: Song) {
        aiRecommendation = ""
        aiRecommendationLoading = true
        aiLyricsSnippet = ""
        aiLyricsLoading = true

        viewModelScope.launch {
            // Async fetch recommendation
            try {
                aiRecommendation = geminiRepository.getSongRecommendation(song.title, song.artist)
            } catch (e: Exception) {
                aiRecommendation = "Check your connection to get AI Suggestions."
            } finally {
                aiRecommendationLoading = false
            }
        }

        viewModelScope.launch {
            // Async fetch lyrics translation / quote
            try {
                aiLyricsSnippet = geminiRepository.getLyricsSnippet(song.title, song.artist, song.description)
            } catch (e: Exception) {
                aiLyricsSnippet = "Music flows like yellow mustard fields under the sweet spring sun."
            } finally {
                aiLyricsLoading = false
            }
        }
    }

    // Ticker running in VM background to continuously query local MediaPlayer position (approx 4Hz updates)
    private fun startPlaybackProgressTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch(Dispatchers.Main) {
            while (true) {
                if (isPlaying && currentSong?.isLocal == true) {
                    playbackPositionMs = mediaPlayer?.currentPosition?.toLong() ?: 0L
                }
                delay(250)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        synthPlayer.release()
        mediaPlayer?.release()
        tickerJob?.cancel()
    }
}

class MusicViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            val database = MusicDatabase.getDatabase(application)
            val songRepository = SongRepository(database.musicDao())
            val geminiRepository = GeminiRepository()
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(application, songRepository, geminiRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
