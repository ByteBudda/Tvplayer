package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.StreamRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AppViewModel"
    private val dao = AppDatabase.getDatabase(application).appDao
    val repository = AppRepository(dao, application)

    // Screen State
    enum class Screen { PLAYER, PLAYLISTS, RECORDINGS, PARENTAL }
    private val _currentScreen = MutableStateFlow(Screen.PLAYER)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Playlist loading & Channels states
    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChannels: StateFlow<List<Channel>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordings: StateFlow<List<Recording>> = repository.recordings
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

    // Active Recording states tracked from StreamRecorder
    val activeRecordingUrls: StateFlow<Set<String>> = StreamRecorder.recordingUrls

    // Parental Controls states
    private val _parentalEnabled = MutableStateFlow(false)
    val parentalEnabled: StateFlow<Boolean> = _parentalEnabled.asStateFlow()

    private val _parentalPin = MutableStateFlow("0000")
    val parentalPin: StateFlow<String> = _parentalPin.asStateFlow()

    private val _isParentalSessionUnlocked = MutableStateFlow(false)
    val isParentalSessionUnlocked: StateFlow<Boolean> = _isParentalSessionUnlocked.asStateFlow()

    // Active media source mode: direct stream, recording, or archive timeshift
    sealed interface PlayMediaMode {
        data object DirectLive : PlayMediaMode
        data class ArchivePlay(val episode: ProgramEpisode) : PlayMediaMode
        data class RecordingPlay(val recording: Recording) : PlayMediaMode
    }
    private val _playMode = MutableStateFlow<PlayMediaMode>(PlayMediaMode.DirectLive)
    val playMode: StateFlow<PlayMediaMode> = _playMode.asStateFlow()

    init {
        viewModelScope.launch {
            // Check parental state
            _parentalEnabled.value = repository.isParentalControlEnabled()
            _parentalPin.value = repository.getParentalPin()

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
                    _archiveSchedule.value = repository.fetchChannelArchiveSchedule(channel.name)
                } else {
                    _archiveSchedule.value = emptyList()
                }
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

    fun selectRecordingForPlay(recording: Recording) {
        _playMode.value = PlayMediaMode.RecordingPlay(recording)
        _currentScreen.value = Screen.PLAYER
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

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedChannel.value?.playlistId == playlistId) {
                _selectedChannel.value = null
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

    // --- RECORDINGS ENGINE ---
    fun toggleRecordingActiveChannel() {
        val channel = _selectedChannel.value ?: return
        viewModelScope.launch {
            if (StreamRecorder.isRecording(channel.streamUrl)) {
                // To safely stop from our stream recorder, search the active database recording
                val activeRecs = recordings.value.find { 
                    it.channelName == channel.name && it.status == "Recording" 
                }
                if (activeRecs != null) {
                    StreamRecorder.stopRecording(activeRecs.id, dao)
                }
                StreamRecorder.cancelRecordingByStreamUrl(channel.streamUrl, dao)
            } else {
                StreamRecorder.startRecording(
                    context = getApplication(),
                    channelName = channel.name,
                    streamUrl = channel.streamUrl,
                    dao = dao
                )
            }
        }
    }

    fun deleteRecording(id: Long) {
        viewModelScope.launch {
            repository.deleteRecording(id)
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
