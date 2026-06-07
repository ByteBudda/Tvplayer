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

    fun parseM3u(playlistId: Long, m3uContent: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var currentLogoUrl: String? = null
        var currentCategory: String? = null
        var currentName: String? = null

        // Safe maximum threshold limit of 1000 items per custom playlist to prevent Room CursorWindow crashes
        val maxChannels = 1000

        m3uContent.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            if (line.startsWith("#EXTINF:")) {
                // Parse attributes
                currentLogoUrl = parseAttribute(line, "tvg-logo") ?: parseAttribute(line, "logo")
                currentCategory = parseAttribute(line, "group-title") ?: parseAttribute(line, "category")
                
                // Name is after the last comma
                val commaIndex = line.lastIndexOf(',')
                if (commaIndex != -1 && commaIndex < line.length - 1) {
                    currentName = line.substring(commaIndex + 1).trim()
                } else {
                    currentName = "Неизвестный канал"
                }
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
                        category = currentCategory ?: "Общие"
                    )
                )
                currentLogoUrl = null
                currentCategory = null
                currentName = null
            }
        }

        return channels
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
    fun parseXml(playlistId: Long, xmlContent: String): XmlParseResult {
        val channels = mutableListOf<Channel>()
        val programs = mutableMapOf<String, MutableList<ProgramEpisode>>()

        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var currentTag: String?
            
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
                currentTag = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (currentTag) {
                            // XMLTV tag
                            "programme" -> {
                                progStart = parser.getAttributeValue(null, "start")
                                progStop = parser.getAttributeValue(null, "stop")
                                progChannelId = parser.getAttributeValue(null, "channel")
                                progTitle = null
                                progDesc = null
                            }
                            // XML Playlist tags (supports <channel>, <item>, etc.)
                            "channel", "item" -> {
                                chName = null
                                chUrl = null
                                chLogo = null
                                chCat = null
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty() && currentTag != null) {
                            when (currentTag) {
                                // XMLTV elements
                                "title" -> progTitle = text
                                "desc" -> progDesc = text
                                
                                // Simple XML Playlist elements
                                "name", "title" -> chName = text
                                "url", "stream", "link" -> chUrl = text
                                "logo", "image", "pic" -> chLogo = text
                                "category", "genre" -> chCat = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (currentTag) {
                            "programme" -> {
                                if (progChannelId != null && progTitle != null) {
                                    try {
                                        val startMs = progStart?.let { xmltvDateFormat.parse(it)?.time } ?: 0L
                                        val stopMs = progStop?.let { xmltvDateFormat.parse(it)?.time } ?: 0L
                                        val startTimeStr = if (startMs > 0) timeFormat.format(startMs) else "--:--"
                                        val stopTimeStr = if (stopMs > 0) timeFormat.format(stopMs) else "--:--"

                                        val currentTime = System.currentTimeMillis()
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
                                        if (list.size < 50) {
                                            list.add(episode)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error parsing XMLTV programme timing", e)
                                    }
                                }
                            }
                            "channel", "item" -> {
                                if (chName != null && chUrl != null && channels.size < 1000) {
                                    channels.add(
                                        Channel(
                                            playlistId = playlistId,
                                            name = chName!!,
                                            streamUrl = chUrl!!,
                                            logoUrl = chLogo,
                                            category = chCat ?: "Общие"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XML", e)
        }

        return XmlParseResult(channels, programs)
    }
}

data class XmlParseResult(
    val channels: List<Channel>,
    val programs: Map<String, List<ProgramEpisode>>
)
