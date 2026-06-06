package com.example.data

data class ProgramEpisode(
    val title: String,
    val description: String?,
    val startTimeString: String,
    val endTimeString: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val isArchive: Boolean
)
