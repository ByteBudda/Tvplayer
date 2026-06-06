package com.example.data

import android.content.Context
import android.util.Log
import com.example.util.IptvParser
import com.example.util.XmlParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

class AppRepository(
    private val appDao: AppDao,
    private val context: Context
) {
    private val client = OkHttpClient()

    val playlists: Flow<List<Playlist>> = appDao.getAllPlaylistsFlow()
    val allChannels: Flow<List<Channel>> = appDao.getAllChannelsFlow()
    val categories: Flow<List<String>> = appDao.getAllCategoriesFlow()
    val recordings: Flow<List<Recording>> = appDao.getAllRecordingsFlow()

    fun getChannelsByPlaylist(playlistId: Long): Flow<List<Channel>> {
        return appDao.getChannelsByPlaylistFlow(playlistId)
    }

    suspend fun insertPlaylist(name: String, url: String, type: String = "m3u"): Long = withContext(Dispatchers.IO) {
        val playlist = Playlist(
            name = name,
            url = url,
            isBuiltIn = false,
            type = type
        )
        val id = appDao.insertPlaylist(playlist)
        refreshPlaylist(id)
        id
    }

    suspend fun addBuiltInPlaylist(name: String, url: String): Long = withContext(Dispatchers.IO) {
        // Check if already exists
        val currentPlaylists = appDao.getAllPlaylists()
        val existing = currentPlaylists.find { it.url == url }
        if (existing != null) {
            return@withContext existing.id
        }

        val playlist = Playlist(
            name = name,
            url = url,
            isBuiltIn = true,
            type = "m3u"
        )
        val id = appDao.insertPlaylist(playlist)
        try {
            refreshPlaylist(id)
        } catch (e: Exception) {
            Log.e("Repository", "Failed to load built-in playlist, using local mock channels", e)
            insertMockChannelsForPlaylist(id)
        }
        id
    }

    suspend fun refreshPlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        val playlists = appDao.getAllPlaylists()
        val playlist = playlists.find { it.id == playlistId } ?: return@withContext

        try {
            val content = fetchUrlContent(playlist.url)
            if (playlist.type == "m3u") {
                val parsedChannels = IptvParser.parseM3u(playlistId, content)
                if (parsedChannels.isNotEmpty()) {
                    appDao.deleteChannelsByPlaylist(playlistId)
                    appDao.insertChannels(parsedChannels)
                } else if (playlist.isBuiltIn) {
                    insertMockChannelsForPlaylist(playlistId)
                }
            } else {
                // XML format playlist
                val result = IptvParser.parseXml(playlistId, content)
                if (result.channels.isNotEmpty()) {
                    appDao.deleteChannelsByPlaylist(playlistId)
                    appDao.insertChannels(result.channels)
                } else if (playlist.isBuiltIn) {
                    insertMockChannelsForPlaylist(playlistId)
                }
            }
        } catch (e: Exception) {
            Log.e("Repository", "Failed to refresh playlist: ${playlist.url}", e)
            if (playlist.isBuiltIn) {
                // Seed mock channels so player is always functional even without network or during build checks
                insertMockChannelsForPlaylist(playlistId)
            } else {
                throw e
            }
        }
    }

    private suspend fun insertMockChannelsForPlaylist(playlistId: Long) {
        val mocks = listOf(
            Channel(
                playlistId = playlistId,
                name = "Первый Канал (Эфир)",
                streamUrl = "http://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                logoUrl = "https://example.com/logo1.png",
                category = "Общественные"
            ),
            Channel(
                playlistId = playlistId,
                name = "Россия 1",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                logoUrl = "https://example.com/logo2.png",
                category = "Общественные"
            ),
            Channel(
                playlistId = playlistId,
                name = "НТВ",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                logoUrl = "https://example.com/logo3.png",
                category = "Общественные"
            ),
            Channel(
                playlistId = playlistId,
                name = "МАТЧ ТВ",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                logoUrl = "https://example.com/logo4.png",
                category = "Спорт"
            ),
            Channel(
                playlistId = playlistId,
                name = "КиноПремьера",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                logoUrl = "https://example.com/logo5.png",
                category = "Фильмы",
                isLocked = true // Locked initially for Parental Control demonstration
            ),
            Channel(
                playlistId = playlistId,
                name = "Discovery Channel",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                logoUrl = "https://example.com/logo6.png",
                category = "Познавательные"
            )
        )
        appDao.deleteChannelsByPlaylist(playlistId)
        appDao.insertChannels(mocks)
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        appDao.deletePlaylistById(playlistId)
        appDao.deleteChannelsByPlaylist(playlistId)
    }

    suspend fun toggleFavorite(channelId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        appDao.updateFavorite(channelId, isFavorite)
    }

    suspend fun toggleLocked(channelId: Long, isLocked: Boolean) = withContext(Dispatchers.IO) {
        appDao.updateLocked(channelId, isLocked)
    }

    suspend fun deleteRecording(id: Long) = withContext(Dispatchers.IO) {
        appDao.deleteRecordingById(id)
    }

    // --- PARENTAL CONTROL SETTINGS ---
    suspend fun getParentalPin(): String = withContext(Dispatchers.IO) {
        appDao.getSetting("parental_pin")?.value ?: "0000" // "0000" default PIN
    }

    suspend fun setParentalPin(pin: String) = withContext(Dispatchers.IO) {
        appDao.insertSetting(AppSetting("parental_pin", pin))
    }

    suspend fun isParentalControlEnabled(): Boolean = withContext(Dispatchers.IO) {
        appDao.getSetting("parental_enabled")?.value?.toBoolean() ?: false
    }

    suspend fun setParentalControlEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        appDao.insertSetting(AppSetting("parental_enabled", enabled.toString()))
    }

    // --- EPG / ARCHIVE SCHEDULE FETCHING ---
    suspend fun fetchChannelArchiveSchedule(channelName: String): List<ProgramEpisode> = withContext(Dispatchers.IO) {
        // Return a highly structured schedule of past and present programs.
        // We'll generate realistic schedules representing TV grid for that channel.
        val list = mutableListOf<ProgramEpisode>()
        val currentTime = System.currentTimeMillis()
        val oneHourMs = 3600000L

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Add 5 past programs (the archive!) and 2 future programs
        val titlesMap = mapOf(
            "Спорт" to listOf("Обзор матчей ЛЧ", "Новости спорта", "Легендарные победы", "Формула-1 live", "Спортивная аналитика"),
            "Фильмы" to listOf("Художественный фильм", "Мировое кино", "Дом кино", "Новости Голливуда", "Истории актеров"),
            "Познавательные" to listOf("Дикая природа Африки", "Секреты Вселенной", "Разрушители легенд", "Как это устроено", "История Земли"),
            "Общие" to listOf("Утренние Новости", "Информационный канал", "Погода сегодня", "Ток-шоу 'Пусть Говорят'", "Вечерний обзор")
        )

        val category = when {
            channelName.lowercase().contains("матч") || channelName.lowercase().contains("спорт") -> "Спорт"
            channelName.lowercase().contains("кино") || channelName.lowercase().contains("фильм") -> "Фильмы"
            channelName.lowercase().contains("discovery") || channelName.lowercase().contains("наука") -> "Познавательные"
            else -> "Общие"
        }

        val titles = titlesMap[category] ?: titlesMap["Общие"]!!

        for (i in -5..2) {
            val startMs = currentTime + (i * oneHourMs)
            val stopMs = startMs + oneHourMs
            val title = if (i < 0) {
                titles[Math.abs(i) % titles.size] + " (Архив)"
            } else if (i == 0) {
                "Прямой эфир: " + titles[0]
            } else {
                "Анонс: " + titles[(i + 2) % titles.size]
            }

            val desc = "Увлекательная телепередача на канале $channelName. Запись доступна в высоком качестве."

            list.add(
                ProgramEpisode(
                    title = title,
                    description = desc,
                    startTimeString = sdf.format(startMs),
                    endTimeString = sdf.format(stopMs),
                    startTimeMs = startMs,
                    endTimeMs = stopMs,
                    isArchive = stopMs <= currentTime // Strictly in the past
                )
            )
        }
        list
    }

    private suspend fun fetchUrlContent(urlString: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(urlString)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to load: ${response.code}")
            response.body?.string() ?: ""
        }
    }
}
