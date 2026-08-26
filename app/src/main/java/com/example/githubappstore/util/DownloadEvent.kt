package com.example.githubappstore.util

/** Shared download-event types produced by [ApkDownloader] and [MultiThreadDownloader]. */
sealed class DownloadEvent {
    data class Queued(val fileName: String) : DownloadEvent()
    data class Progress(val fileName: String, val bytes: Long, val total: Long) : DownloadEvent()
    data class Succeeded(val fileName: String, val localUri: String) : DownloadEvent()
    data class Failed(val fileName: String, val reason: String) : DownloadEvent()
}
