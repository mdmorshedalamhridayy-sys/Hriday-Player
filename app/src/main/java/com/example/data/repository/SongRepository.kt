package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.data.database.*
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class SongRepository(private val musicDao: MusicDao) {

    // Gorgeous built-in collection of traditional Bangladeshi folk, rabindra, and pop-rock songs.
    private val preloadedSongs = listOf(
        Song(
            id = "preloaded_0",
            title = "Mon Amar (Lalon Geeti)",
            artist = "Fakir Lalon Shah",
            album = "Mystic Folk Fusion",
            durationMs = 180000L,
            coverColorStart = 0xFF047857, // Deep Mangrove Green
            coverColorEnd = 0xFF064E3B,   // Dark Forest
            description = "A deep, mystical folk song about internal self-exploration and spiritual tranquility."
        ),
        Song(
            id = "preloaded_1",
            title = "Noya Daman",
            artist = "Sylheti Folk Beat",
            album = "Bangla Folk Wedding",
            durationMs = 215000L,
            coverColorStart = 0xFFBE123C, // Flag Crimson Red
            coverColorEnd = 0xFF4C0519,   // Deep Rose Night
            description = "A bright, high-energy wedding dance folk tune popular in Sylhet, tea capital of Bangladesh."
        ),
        Song(
            id = "preloaded_2",
            title = "Ekla Cholo Re",
            artist = "Rabindranath Tagore",
            album = "Tagore Classics",
            durationMs = 156000L,
            coverColorStart = 0xFFD97706, // Sonali Mustard Gold
            coverColorEnd = 0xFF78350F,   // Warm Earth
            description = "Tagore's legendary anthem encouraging courage, unity, and walking alone in adversity."
        ),
        Song(
            id = "preloaded_3",
            title = "Banshiri (Bhatiali Flute)",
            artist = "Bhatiali River Flute",
            album = "River Songs of Bengal",
            durationMs = 240000L,
            coverColorStart = 0xFF0D9488, // Bhatiali River Teal
            coverColorEnd = 0xFF115E59,   // Deep River Jamuna
            description = "A beautiful river flute composition mirroring the quiet rhythm of boats drifting on Padma and Meghna."
        ),
        Song(
            id = "preloaded_4",
            title = "Rock Gaan (Kothao Keu Nei)",
            artist = "Bangla Rock",
            album = "Midnight Srijon",
            durationMs = 195000L,
            coverColorStart = 0xFF4B5563, // Steel Charcoal Dark
            coverColorEnd = 0xFF111827,   // Deep Slate
            description = "A modern upbeat Bangla alternative rock ballad featuring rich synthesizers and drum beats."
        ),
        Song(
            id = "preloaded_5",
            title = "Ami Banglay Gaan Gai",
            artist = "Sujon Kanti",
            album = "Sovereign Acoustic",
            durationMs = 220000L,
            coverColorStart = 0xFF0369A1, // Sky Bengal Blue
            coverColorEnd = 0xFF075985,   // Indigo Bengal water
            description = "An acoustic rendering expressing absolute love for the Bengali mother-tongue and identity."
        )
    )

    fun getPreloadedSongs(): List<Song> = preloadedSongs

    // Scans local audio folder files if user has permissions
    suspend fun getLocalMusic(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Song>()
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION
            )
            
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val query = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )

            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Track"
                    var artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    if (artist == "<unknown>") artist = "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val duration = cursor.getLong(durationColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    ).toString()

                    tracks.add(
                        Song(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            mediaUri = contentUri,
                            coverColorStart = 0xFF4B5563,
                            coverColorEnd = 0xFF374151,
                            isLocal = true,
                            description = "Standard local audio track stored offline on your Android device."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracks
    }

    // Merge preloaded and local files dynamically, filtering by search matches
    fun getMergedSongsFlow(context: Context, searchQuery: String): Flow<List<Song>> = flow {
        val localTracks = try { getLocalMusic(context) } catch (e: Exception) { emptyList() }
        val all = preloadedSongs + localTracks
        emit(all)
    }.map { list ->
        if (searchQuery.isBlank()) {
            list
        } else {
            list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }.flowOn(Dispatchers.IO)

    // Find song by id across preloaded or local files
    suspend fun getSongById(context: Context, songId: String): Song? {
        val pre = preloadedSongs.find { it.id == songId }
        if (pre != null) return pre
        val locals = getLocalMusic(context)
        return locals.find { it.id == songId }
    }

    // --- DB DAO abstraction ---
    val allPlaylists: Flow<List<PlaylistEntity>> = musicDao.getAllPlaylists()
    val favoriteSongIds: Flow<List<String>> = musicDao.getAllFavoriteSongIds()
    val recentlyPlayedSongIds: Flow<List<String>> = musicDao.getRecentlyPlayedSongIds()

    // Query playlist song IDs
    fun getSongsInPlaylist(playlistId: Int): Flow<List<String>> = musicDao.getSongsInPlaylist(playlistId)

    suspend fun createPlaylist(name: String) = withContext(Dispatchers.IO) {
        musicDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlistId: Int) = withContext(Dispatchers.IO) {
        musicDao.clearPlaylistSongs(playlistId)
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Int, songId: String) = withContext(Dispatchers.IO) {
        musicDao.addSongToPlaylist(PlaylistSongEntity(playlistId = playlistId, songId = songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Int, songId: String) = withContext(Dispatchers.IO) {
        musicDao.removeSongFromPlaylist(playlistId = playlistId, songId = songId)
    }

    suspend fun toggleFavorite(songId: String) = withContext(Dispatchers.IO) {
        if (musicDao.isFavorite(songId)) {
            musicDao.removeFavorite(songId)
        } else {
            musicDao.addFavorite(FavoriteEntity(songId = songId))
        }
    }

    suspend fun addRecentlyPlayed(songId: String) = withContext(Dispatchers.IO) {
        musicDao.addRecentlyPlayed(RecentlyPlayedEntity(songId = songId))
    }
}
