package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AppViewModel"
    private val dao = AppDatabase.getDatabase(application).appDao
    val repository = AppRepository(dao, application)

    // Screen State
    enum class Screen { PLAYER, PLAYLISTS, PARENTAL }
    private val _currentScreen = MutableStateFlow(Screen.PLAYER)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Playlist loading & Channels states
    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChannels: StateFlow<List<Channel>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val epgSources: StateFlow<List<EpgSource>> = repository.epgSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Selection State
    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel: StateFlow<Channel?> = _selectedChannel.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String>("Все")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Selected Archive / EPG schedule for active channel
    private val _archiveSchedule = MutableStateFlow<List<ProgramEpisode>>(emptySet<ProgramEpisode>().toList())
    val archiveSchedule: StateFlow<List<ProgramEpisode>> = _archiveSchedule.asStateFlow()

    // Loading & Refresh UI indicator status
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Parental Controls states
    private val _parentalEnabled = MutableStateFlow(false)
    val parentalEnabled: StateFlow<Boolean> = _parentalEnabled.asStateFlow()

    private val _parentalPin = MutableStateFlow("0000")
    val parentalPin: StateFlow<String> = _parentalPin.asStateFlow()

    private val _isParentalSessionUnlocked = MutableStateFlow(false)
    val isParentalSessionUnlocked: StateFlow<Boolean> = _isParentalSessionUnlocked.asStateFlow()

    // Full-screen state flow for edge-to-edge player visibility
    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    fun setFullscreen(enabled: Boolean) {
        _isFullscreen.value = enabled
    }

    // Video Aspect Ratio / Resize Mode
    private val _videoResizeMode = MutableStateFlow(0)
    val videoResizeMode: StateFlow<Int> = _videoResizeMode.asStateFlow()

    fun setVideoResizeMode(mode: Int) {
        _videoResizeMode.value = mode
        viewModelScope.launch {
            repository.setResizeMode(mode)
        }
    }

    // Active media source mode: direct stream, recording, or archive timeshift
    sealed interface PlayMediaMode {
        data object DirectLive : PlayMediaMode
        data class ArchivePlay(val episode: ProgramEpisode) : PlayMediaMode
    }
    private val _playMode = MutableStateFlow<PlayMediaMode>(PlayMediaMode.DirectLive)
    val playMode: StateFlow<PlayMediaMode> = _playMode.asStateFlow()

    init {
        viewModelScope.launch {
            // Check parental state
            _parentalEnabled.value = repository.isParentalControlEnabled()
            _parentalPin.value = repository.getParentalPin()
            _videoResizeMode.value = repository.getResizeMode()

            // Preload default built-in playlist if there are no playlists (safe one-shot check on startup)
            try {
                val list = repository.playlists.first()
                if (list.isEmpty()) {
                    _isRefreshing.value = true
                    repository.addBuiltInPlaylist(
                        name = "Стабильный Эфир ТВ",
                        url = "https://raw.githubusercontent.com/smolnp/IPTVru/refs/heads/gh-pages/IPTVstable.m3u8"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed seeding built-in list", e)
            } finally {
                _isRefreshing.value = false
            }
        }

        // Keep channel archive schedule in sync with active selected channel
        viewModelScope.launch {
            selectedChannel.collectLatest { channel ->
                if (channel != null) {
                    _archiveSchedule.value = repository.fetchChannelArchiveSchedule(channel)
                } else {
                    _archiveSchedule.value = emptyList()
                }
            }
        }

        // Initial EPG refresh
        viewModelScope.launch {
            repository.refreshEpg()
            _selectedChannel.value?.let { ch ->
                _archiveSchedule.value = repository.fetchChannelArchiveSchedule(ch)
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
        _playMode.value = PlayMediaMode.DirectLive
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, !channel.isFavorite)
            // Re-sync selected channel to reflect favorites toggled instantly
            if (_selectedChannel.value?.id == channel.id) {
                _selectedChannel.value = _selectedChannel.value?.copy(isFavorite = !channel.isFavorite)
            }
        }
    }

    fun toggleChannelLock(channel: Channel) {
        viewModelScope.launch {
            repository.toggleLocked(channel.id, !channel.isLocked)
            if (_selectedChannel.value?.id == channel.id) {
                _selectedChannel.value = _selectedChannel.value?.copy(isLocked = !channel.isLocked)
            }
        }
    }

    fun selectArchiveEpisode(episode: ProgramEpisode) {
        _playMode.value = PlayMediaMode.ArchivePlay(episode)
    }

    fun switchBackToLive() {
        _playMode.value = PlayMediaMode.DirectLive
    }

    // --- PLAYLIST CONFIG ENGINE ---
    fun addPlaylist(name: String, url: String, type: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.insertPlaylist(name, url, type)
            } catch (e: Exception) {
                Log.e(TAG, "Failed adding playlist", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun importPlaylist(name: String, content: String, type: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.importPlaylistFromContent(name, content, type)
            } catch (e: Exception) {
                Log.e(TAG, "Failed importing playlist", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedChannel.value?.playlistId == playlistId) {
                _selectedChannel.value = null
            }
        }
    }

    fun editPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.updatePlaylist(playlist)
            } catch (e: Exception) {
                Log.e(TAG, "Failed editing playlist", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun addChannel(channel: Channel) {
        viewModelScope.launch {
            try {
                repository.addChannel(channel)
            } catch (e: Exception) {
                Log.e(TAG, "Failed adding channel", e)
            }
        }
    }

    fun updateChannel(channel: Channel) {
        viewModelScope.launch {
            try {
                repository.updateChannel(channel)
                if (_selectedChannel.value?.id == channel.id) {
                    _selectedChannel.value = channel
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed updating channel", e)
            }
        }
    }

    fun deleteChannel(channelId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteChannel(channelId)
                if (_selectedChannel.value?.id == channelId) {
                    _selectedChannel.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed deleting channel", e)
            }
        }
    }

    private val _cleanResultCount = MutableStateFlow<Int?>(null)
    val cleanResultCount: StateFlow<Int?> = _cleanResultCount.asStateFlow()

    fun clearCleanResult() {
        _cleanResultCount.value = null
    }

    fun cleanUnavailableChannels(playlistId: Long) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val removed = repository.cleanUnavailableChannels(playlistId)
                _cleanResultCount.value = removed
                // Reset selected channel if it was removed
                val current = _selectedChannel.value
                if (current != null && current.playlistId == playlistId) {
                    val exists = repository.allChannels.first().any { it.id == current.id }
                    if (!exists) {
                        _selectedChannel.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed cleaning channels", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshPlaylist(playlistId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed refreshing playlist", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // --- EPG SOURCE ENGINE ---
    fun addEpgSource(name: String, url: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.addEpgSource(name, url)
            } catch (e: Exception) {
                Log.e(TAG, "Failed adding EPG source", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun deleteEpgSource(id: Long) {
        viewModelScope.launch {
            repository.deleteEpgSource(id)
        }
    }

    fun toggleEpgSource(id: Long, active: Boolean) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.toggleEpgSource(id, active)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshEpg() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refreshEpg()
                // Update current channel schedule after refresh
                _selectedChannel.value?.let { ch ->
                    _archiveSchedule.value = repository.fetchChannelArchiveSchedule(ch)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // --- PARENTAL LOCK ENGINE ---
    fun setParentalLockState(enabled: Boolean, pin: String) {
        viewModelScope.launch {
            repository.setParentalControlEnabled(enabled)
            repository.setParentalPin(pin)
            _parentalEnabled.value = enabled
            _parentalPin.value = pin
            _isParentalSessionUnlocked.value = false
        }
    }

    fun unlockParentalSession(inputPin: String): Boolean {
        return if (inputPin == _parentalPin.value) {
            _isParentalSessionUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun lockParentalSession() {
        _isParentalSessionUnlocked.value = false
    }
}
