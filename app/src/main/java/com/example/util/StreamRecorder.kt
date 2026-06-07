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

    // Map of recordingId to URL so we can clear on completion/error/cancellation
    private val jobUrls = java.util.concurrent.ConcurrentHashMap<Long, String>()

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
                jobUrls[id] = streamUrl
                _recordingUrls.value = _recordingUrls.value + streamUrl
                onRecordingStarted(id)

                // Start downloading job
                val job = scope.launch(Dispatchers.IO) {
                    var outStream: FileOutputStream? = null
                    var connection: HttpURLConnection? = null
                    var bytesWritten = 0L
                    val startTime = System.currentTimeMillis()
                    var successCompletion = false

                    var httpErrorDetails: String? = null
                    try {
                        outStream = FileOutputStream(file)
                        
                        val url = URL(streamUrl)
                        connection = url.openConnection() as HttpURLConnection
                        connection.connectTimeout = 30000
                        connection.readTimeout = 30000
                        connection.instanceFollowRedirects = true
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        connection.setRequestProperty("Accept", "*/*")
                        
                        // Initial update to show we have started
                        dao.updateRecording(Recording(
                            id = id,
                            channelName = channelName,
                            streamUrl = streamUrl,
                            startTime = timestamp,
                            durationMs = 0L,
                            filePath = file.absolutePath,
                            fileSize = 0L,
                            status = "Recording"
                        ))
                        
                        val responseCode = connection.responseCode
                        Log.d(TAG, "Recording $channelName: Response code $responseCode for $streamUrl")
                        
                        if (responseCode in 200..299) {
                            val inStream = connection.inputStream
                            val buffer = ByteArray(128 * 1024)
                            var bytesRead: Int
                            
                            while (isActive) {
                                bytesRead = try { 
                                    inStream.read(buffer) 
                                } catch (e: Exception) { 
                                    Log.e(TAG, "Read error for $channelName", e)
                                    -1 
                                }
                                
                                if (bytesRead == -1) {
                                    successCompletion = true
                                    break
                                }
                                outStream.write(buffer, 0, bytesRead)
                                bytesWritten += bytesRead
                                
                                // Update every 1MB or every 5 seconds
                                val currentTime = System.currentTimeMillis()
                                if (bytesWritten % (1024 * 1024) == 0L) {
                                    val currentDuration = currentTime - startTime
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
                            httpErrorDetails = "HTTP $responseCode"
                            if (responseCode == 403) httpErrorDetails = "Forbidden (403)"
                            if (responseCode == 404) httpErrorDetails = "Not Found (404)"
                            Log.e(TAG, "Server returned response code $responseCode")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Recording error for $channelName", e)
                        httpErrorDetails = e.localizedMessage ?: e.message
                    } finally {
                        try {
                            outStream?.flush()
                            outStream?.close()
                        } catch (e: Exception) {}
                        try {
                            connection?.disconnect()
                        } catch (e: Exception) {}

                        withContext(NonCancellable) {
                            _recordingUrls.value = _recordingUrls.value - streamUrl
                            activeJobs.remove(id)
                            jobUrls.remove(id)

                            val finalDuration = System.currentTimeMillis() - startTime
                            val finalSize = if (file.exists()) file.length() else bytesWritten
                            
                            // If we have downloaded more than 100KB, mark completed
                            val isSufficientSize = finalSize > 100 * 1024
                            val finalStatus = if (isSufficientSize) "Completed" else (httpErrorDetails ?: "Failed")

                            val completedRecording = Recording(
                                id = id,
                                channelName = channelName,
                                streamUrl = streamUrl,
                                startTime = timestamp,
                                durationMs = finalDuration,
                                filePath = file.absolutePath,
                                fileSize = finalSize,
                                status = finalStatus
                            )
                            dao.updateRecording(completedRecording)
                            Log.i(TAG, "Finished recording task for $channelName, status: $finalStatus, size: $finalSize bytes")
                        }
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
            val streamUrl = jobUrls[id]
            if (streamUrl != null) {
                _recordingUrls.value = _recordingUrls.value - streamUrl
            }
            val job = activeJobs.remove(id)
            if (job != null) {
                job.cancelAndJoin()
            }
        }
    }
    
    fun cancelRecordingByStreamUrl(streamUrl: String, dao: AppDao) {
        scope.launch {
            _recordingUrls.value = _recordingUrls.value - streamUrl
            // Find and cancel jobs for this streamUrl
            val idToCancel = jobUrls.entries.find { it.value == streamUrl }?.key
            if (idToCancel != null) {
                val job = activeJobs.remove(idToCancel)
                if (job != null) {
                    job.cancelAndJoin()
                }
            }
        }
    }

    fun isRecording(streamUrl: String): Boolean {
        return _recordingUrls.value.contains(streamUrl)
    }
}
