package com.example.githubappstore.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.getSystemService
import com.example.githubappstore.data.model.GhAsset
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.delay

/** Wraps the system [DownloadManager] to fetch a release APK and expose progress as a Flow. */
class ApkDownloader(private val context: Context) {
    fun enqueue(asset: GhAsset, proxiedUrl: String): Flow<DownloadEvent> = callbackFlow {
        val dm = context.getSystemService<DownloadManager>() ?: run {
            trySend(DownloadEvent.Failed(asset.name, "DownloadManager unavailable")); close(); return@callbackFlow
        }
        val request = DownloadManager.Request(Uri.parse(proxiedUrl))
            .setTitle(asset.name).setDescription("GitStore 下载")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, asset.name)
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true).setAllowedOverRoaming(true)
        val id = dm.enqueue(request)
        trySend(DownloadEvent.Queued(asset.name))
        val query = DownloadManager.Query().setFilterById(id)
        while (true) {
            val cursor = dm.query(query)
            if (cursor == null || !cursor.moveToFirst()) { cursor?.close(); trySend(DownloadEvent.Failed(asset.name, "Download cancelled")); break }
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            when (status) {
                DownloadManager.STATUS_RUNNING -> {
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).coerceAtLeast(1)
                    trySend(DownloadEvent.Progress(asset.name, downloaded, total))
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    trySend(DownloadEvent.Succeeded(asset.name, uri)); cursor.close(); break
                }
                DownloadManager.STATUS_FAILED -> {
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    trySend(DownloadEvent.Failed(asset.name, "reason=$reason")); cursor.close(); break
                }
                DownloadManager.STATUS_PAUSED -> { /* keep polling */ }
            }
            cursor.close(); delay(700)
        }
        close()
    }.conflate()
}
