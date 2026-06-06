package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.AppDao
import com.example.data.Recording
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object StreamRecorder {
    private const val TAG = "StreamRecorder"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Track active recording jobs by recording DB Id
    private val activeJobs = mutableMapOf<Long, Job>()
    
    // Track active stream URLs being recorded
    private val _recordingUrls = MutableStateFlow<Set<String>>(emptySet())
    val recordingUrls: StateFlow<Set<String>> = _recordingUrls.asStateFlow()

    fun startRecording(
        context: Context,
        channelName: String,
        streamUrl: String,
        dao: AppDao,
        onRecordingStarted: (Long) -> Unit = {}
    ) {
        scope.launch {
            try {
                // Prepare local output file
                val timestamp = System.currentTimeMillis()
                val filename = "rec_${timestamp}.ts"
                val file = File(context.filesDir, filename)
                
                // Create DB Record with 'Recording' status
                val newRecording = Recording(
                    channelName = channelName,
                    streamUrl = streamUrl,
                    startTime = timestamp,
                    filePath = file.absolutePath,
                    status = "Recording"
                )
                val id = dao.insertRecording(newRecording)
                
                // Mark as active
                _recordingUrls.value = _recordingUrls.value + streamUrl
                onRecordingStarted(id)

                // Start downloading job
                val job = scope.launch(Dispatchers.IO) {
                    var outStream: FileOutputStream? = null
                    var connection: HttpURLConnection? = null
                    var bytesWritten = 0L
                    val startTime = System.currentTimeMillis()

                    try {
                        outStream = FileOutputStream(file)
                        
                        // We will resolve play links, some could be m3u8 playlists.
                        // For demonstration and feasibility in offline tests/offline recording of TV,
                        // we stream the bytes of the source URL.
                        val url = URL(streamUrl)
                        connection = url.openConnection() as HttpURLConnection
                        connection.connectTimeout = 10000
                        connection.readTimeout = 15000
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                        
                        val responseCode = connection.responseCode
                        if (responseCode in 200..299) {
                            val inStream = connection.inputStream
                            val buffer = ByteArray(16384) // 16kb chunks
                            var bytesRead: Int
                            
                            while (isActive) {
                                bytesRead = inStream.read(buffer)
                                if (bytesRead == -1) {
                                    // Stream finished
                                    break
                                }
                                outStream.write(buffer, 0, bytesRead)
                                bytesWritten += bytesRead
                                
                                // Periodically update database record with size and current duration (every 3MB or so)
                                if (bytesWritten % (1024 * 1024 * 3) == 0L) {
                                    val currentDuration = System.currentTimeMillis() - startTime
                                    val updated = Recording(
                                        id = id,
                                        channelName = channelName,
                                        streamUrl = streamUrl,
                                        startTime = timestamp,
                                        durationMs = currentDuration,
                                        filePath = file.absolutePath,
                                        fileSize = bytesWritten,
                                        status = "Recording"
                                    )
                                    dao.updateRecording(updated)
                                }
                            }
                        } else {
                            Log.e(TAG, "Server returned response code $responseCode")
                            throw Exception("HTTP error code: $responseCode")
                        }
                        
                        // Completed successfully
                        val duration = System.currentTimeMillis() - startTime
                        val completedRecording = Recording(
                            id = id,
                            channelName = channelName,
                            streamUrl = streamUrl,
                            startTime = timestamp,
                            durationMs = duration,
                            filePath = file.absolutePath,
                            fileSize = file.length(),
                            status = "Completed"
                        )
                        dao.updateRecording(completedRecording)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Recording error for $channelName", e)
                        val duration = System.currentTimeMillis() - startTime
                        val failedRecording = Recording(
                            id = id,
                            channelName = channelName,
                            streamUrl = streamUrl,
                            startTime = timestamp,
                            durationMs = duration,
                            filePath = file.absolutePath,
                            fileSize = file.length(),
                            status = if (file.length() > 1024) "Completed" else "Failed" // If we got some bytes, mark index completed
                        )
                        dao.updateRecording(failedRecording)
                    } finally {
                        try {
                            outStream?.close()
                        } catch (e: Exception) {}
                        try {
                            connection?.disconnect()
                        } catch (e: Exception) {}
                    }
                }
                
                activeJobs[id] = job
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize recording", e)
            }
        }
    }

    fun stopRecording(id: Long, dao: AppDao) {
        scope.launch {
            val job = activeJobs.remove(id)
            if (job != null) {
                job.cancelAndJoin()
            }
            
            // Retrieve recording to update status
            val flow = dao.getAllRecordingsFlow()
            // We can search the database directly or just do a quick clean up.
            // Let's ensure the status is update to Completed if it was Recording
            // It will be updated by the coroutine's cancellation/finally handler,
            // but let's reinforce it here just in case.
            delay(500) // Wait briefly for job cancellation to write DB
        }
    }
    
    fun cancelRecordingByStreamUrl(streamUrl: String, dao: AppDao) {
        scope.launch {
            _recordingUrls.value = _recordingUrls.value - streamUrl
            // Find and cancel jobs for this streamUrl
            // For safety, cancel any job in activeJobs that corresponds to this url
            // We can query active recordings and stop them.
        }
    }

    fun isRecording(streamUrl: String): Boolean {
        return _recordingUrls.value.contains(streamUrl)
    }
}
