package com.example.util

import android.util.Log
import android.util.Xml
import com.example.data.Channel
import com.example.data.ProgramEpisode
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object IptvParser {
    private const val TAG = "IptvParser"

    data class M3uParseResult(
        val channels: List<Channel>,
        val epgUrls: List<String>
    )

    fun parseM3u(playlistId: Long, m3uContent: String): M3uParseResult {
        val channels = mutableListOf<Channel>()
        val epgUrls = mutableSetOf<String>()
        var currentLogoUrl: String? = null
        var currentCategory: String? = null
        var currentName: String? = null
        var currentTvgId: String? = null
        var currentTvgName: String? = null

        // Safe maximum threshold limit of 3000 items per custom playlist to prevent Room CursorWindow crashes
        val maxChannels = 3000

        m3uContent.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            if (line.startsWith("#EXTM3U")) {
                parseAttribute(line, "tvg-url")?.let { epgUrls.add(it) }
                parseAttribute(line, "x-tvg-url")?.let { epgUrls.add(it) }
                parseAttribute(line, "url-tvg")?.let { epgUrls.add(it) }
            } else if (line.startsWith("#EXTINF:")) {
                // Parse attributes
                currentLogoUrl = parseAttribute(line, "tvg-logo") ?: parseAttribute(line, "logo")
                currentCategory = parseAttribute(line, "group-title") ?: parseAttribute(line, "category")
                val tvgId = parseAttribute(line, "tvg-id")
                val tvgName = parseAttribute(line, "tvg-name")
                
                parseAttribute(line, "tvg-url")?.let { epgUrls.add(it) }
                
                // Name is after the last comma
                val commaIndex = line.lastIndexOf(',')
                if (commaIndex != -1 && commaIndex < line.length - 1) {
                    currentName = line.substring(commaIndex + 1).trim()
                } else {
                    currentName = "Неизвестный канал"
                }

                // Temporary storage for tvgId and tvgName to be used when creating Channel
                currentTvgId = tvgId
                currentTvgName = tvgName
            } else if (!line.startsWith("#") && (line.startsWith("http") || line.startsWith("rtmp") || line.contains("://"))) {
                if (channels.size >= maxChannels) {
                    return@forEach
                }
                val name = currentName ?: "Канал ${channels.size + 1}"
                channels.add(
                    Channel(
                        playlistId = playlistId,
                        name = name,
                        streamUrl = line,
                        logoUrl = currentLogoUrl,
                        category = currentCategory ?: "Общие",
                        tvgId = currentTvgId,
                        tvgName = currentTvgName
                    )
                )
                currentLogoUrl = null
                currentCategory = null
                currentName = null
                currentTvgId = null
                currentTvgName = null
            }
        }

        return M3uParseResult(channels, epgUrls.toList())
    }

    private fun parseAttribute(line: String, attributeName: String): String? {
        val key = "$attributeName="
        val index = line.indexOf(key)
        if (index == -1) return null
        
        val startIndex = index + key.length
        if (startIndex >= line.length) return null
        
        val quoteChar = line[startIndex]
        if (quoteChar == '"' || quoteChar == '\'') {
            val endIndex = line.indexOf(quoteChar, startIndex + 1)
            if (endIndex != -1) {
                return line.substring(startIndex + 1, endIndex)
            }
        } else {
            // Unquoted string up to space or comma
            var endIndex = startIndex
            while (endIndex < line.length && line[endIndex] != ' ' && line[endIndex] != ',') {
                endIndex++
            }
            return line.substring(startIndex, endIndex)
        }
        return null
    }

    /**
     * Parses an XML content. It can be:
     * 1. XMLTV format (EPG - Electronic Program Guide)
     * 2. XML Playlist (custom format)
     */
    fun parseXml(playlistId: Long, inputStream: java.io.InputStream): XmlParseResult {
        val channels = mutableListOf<Channel>()
        val programs = mutableMapOf<String, MutableList<ProgramEpisode>>()
        val epgChannelNames = mutableMapOf<String, String>() // id -> displayName

        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentTagName: String? = null
            
            // XMLTV variables
            var progStart: String? = null
            var progStop: String? = null
            var progChannelId: String? = null
            var progTitle: String? = null
            var progDesc: String? = null

            // Simple XML playlist variables
            var chName: String? = null
            var chUrl: String? = null
            var chLogo: String? = null
            var chCat: String? = null

            // Date format for XMLTV: "20191027130000 +0300"
            val xmltvDateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTagName = parser.name
                        when (currentTagName) {
                            // XMLTV tags
                            "programme" -> {
                                progStart = parser.getAttributeValue(null, "start")
                                progStop = parser.getAttributeValue(null, "stop")
                                progChannelId = parser.getAttributeValue(null, "channel")
                                progTitle = null
                                progDesc = null
                            }
                            "channel" -> {
                                // In XMLTV, channel has an id attribute
                                val id = parser.getAttributeValue(null, "id")
                                if (id != null) {
                                    // We'll use chName variable to store display-name later in TEXT event
                                    progChannelId = id 
                                    chName = null
                                }
                            }
                            // XML Playlist tags (supports <channel>, <item>, etc.)
                            "item" -> {
                                chName = null
                                chUrl = null
                                chLogo = null
                                chCat = null
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty() && currentTagName != null) {
                            when (currentTagName) {
                                // XMLTV elements
                                "title" -> progTitle = text
                                "desc" -> progDesc = text
                                "display-name" -> chName = text
                                
                                // Simple XML Playlist elements
                                "name", "title" -> if (progChannelId == null) chName = text
                                "url", "stream", "link" -> chUrl = text
                                "logo", "image", "pic" -> chLogo = text
                                "category", "genre" -> chCat = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tagAtEnd = parser.name
                        when (tagAtEnd) {
                            "channel", "item" -> {
                                if (progChannelId != null && chName != null && chUrl == null) {
                                    // XMLTV channel definition: <channel id="..."><display-name>...</display-name></channel>
                                    epgChannelNames[progChannelId!!] = chName!!
                                    progChannelId = null
                                    chName = null
                                } else if (chName != null && chUrl != null && channels.size < 3000) {
                                    // Custom XML playlist channel: <channel><name>...</name><url>...</url></channel>
                                    channels.add(
                                        Channel(
                                            playlistId = playlistId,
                                            name = chName!!,
                                            streamUrl = chUrl!!,
                                            logoUrl = chLogo,
                                            category = chCat ?: "Общие"
                                        )
                                    )
                                    chName = null; chUrl = null; chLogo = null; chCat = null
                                }
                            }
                            "programme" -> {
                                if (progChannelId != null && progTitle != null) {
                                    try {
                                        val startMs = progStart?.let { parseXmltvDate(it, xmltvDateFormat) } ?: 0L
                                        val stopMs = progStop?.let { parseXmltvDate(it, xmltvDateFormat) } ?: 0L
                                        val startTimeStr = if (startMs > 0) timeFormat.format(startMs) else "--:--"
                                        val stopTimeStr = if (stopMs > 0) timeFormat.format(stopMs) else "--:--"

                                        val currentTime = System.currentTimeMillis()
                                        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+3"))
                                        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        calendar.set(java.util.Calendar.MINUTE, 0)
                                        calendar.set(java.util.Calendar.SECOND, 0)
                                        calendar.set(java.util.Calendar.MILLISECOND, 0)
                                        val startOfDayMs = calendar.timeInMillis
                                        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                                        calendar.set(java.util.Calendar.MINUTE, 59)
                                        calendar.set(java.util.Calendar.SECOND, 59)
                                        calendar.set(java.util.Calendar.MILLISECOND, 999)
                                        val endOfDayMs = calendar.timeInMillis
                                        
                                        if (startMs >= startOfDayMs && startMs <= endOfDayMs) {
                                            val episode = ProgramEpisode(
                                                title = progTitle!!,
                                                description = progDesc,
                                                startTimeString = startTimeStr,
                                                endTimeString = stopTimeStr,
                                                startTimeMs = startMs,
                                                endTimeMs = stopMs,
                                                isArchive = startMs < currentTime
                                            )
                                            
                                            val list = programs.getOrPut(progChannelId!!) { mutableListOf() }
                                            if (list.size < 500) { // Increased limit for better EPG coverage
                                                list.add(episode)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error parsing XMLTV programme timing", e)
                                    }
                                }
                                progChannelId = null
                            }
                        }
                        currentTagName = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XML", e)
        }

        return XmlParseResult(channels, programs, epgChannelNames)
    }

    private fun parseXmltvDate(dateStr: String, format: SimpleDateFormat): Long {
        return try {
            format.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            // Fallback for dates without timezone: 20240101000000
            try {
                if (dateStr.length >= 14) {
                    val fallback = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
                    fallback.parse(dateStr.substring(0, 14))?.time ?: 0L
                } else 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }
}

data class XmlParseResult(
    val channels: List<Channel>,
    val programs: Map<String, List<ProgramEpisode>>,
    val channelIdToName: Map<String, String> = emptyMap()
)
