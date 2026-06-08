package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- PLAYLISTS ---
    @Query("SELECT * FROM playlist")
    fun getAllPlaylistsFlow(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlist")
    suspend fun getAllPlaylists(): List<Playlist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("DELETE FROM playlist WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    // --- CHANNELS ---
    @Query("SELECT * FROM channel WHERE playlistId = :playlistId")
    fun getChannelsByPlaylistFlow(playlistId: Long): Flow<List<Channel>>

    @Query("SELECT * FROM channel WHERE playlistId = :playlistId")
    suspend fun getChannelsByPlaylist(playlistId: Long): List<Channel>

    @Query("SELECT * FROM channel")
    fun getAllChannelsFlow(): Flow<List<Channel>>

    @Query("SELECT DISTINCT category FROM channel WHERE category IS NOT NULL")
    fun getAllCategoriesFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel): Long

    @Update
    suspend fun updateChannel(channel: Channel)

    @Delete
    suspend fun deleteChannel(channel: Channel)

    @Query("DELETE FROM channel WHERE id = :channelId")
    suspend fun deleteChannelById(channelId: Long)

    @Query("DELETE FROM channel WHERE playlistId = :playlistId")
    suspend fun deleteChannelsByPlaylist(playlistId: Long)

    @Query("UPDATE channel SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun updateFavorite(channelId: Long, isFavorite: Boolean)

    @Query("UPDATE channel SET isLocked = :isLocked WHERE id = :channelId")
    suspend fun updateLocked(channelId: Long, isLocked: Boolean)

    // --- APP SETTINGS ---
    @Query("SELECT * FROM app_setting WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)

    @Query("DELETE FROM app_setting WHERE `key` = :key")
    suspend fun deleteSetting(key: String)
}
