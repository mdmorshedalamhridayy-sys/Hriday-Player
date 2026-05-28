package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // --- Playlists ---
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Int)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    // --- Playlist Songs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(playlistSong: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Int, songId: String)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getSongsInPlaylist(playlistId: Int): Flow<List<String>>

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Int)

    // --- Favorites ---
    @Query("SELECT songId FROM favorites ORDER BY favoritedAt DESC")
    fun getAllFavoriteSongIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun isFavorite(songId: String): Boolean

    // --- Recently Played ---
    @Query("SELECT songId FROM recently_played ORDER BY playedAt DESC LIMIT 25")
    fun getRecentlyPlayedSongIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecentlyPlayed(recent: RecentlyPlayedEntity)
}
