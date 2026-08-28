package com.example.githubappstore.ui.downloads
import kotlinx.coroutines.flow.first

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubappstore.GitHubAppStoreApp
import com.example.githubappstore.data.model.GhAsset
import com.example.githubappstore.domain.DownloadStatus
import com.example.githubappstore.util.ApkDownloader
import com.example.githubappstore.util.MultiThreadDownloader
import com.example.githubappstore.util.ProxyUtils
import com.example.githubappstore.util.DownloadEvent as DEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** True when the app may write APKs into the public /Download directory. */
fun hasStorageAccess(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    Environment.isExternalStorageManager()
} else {
    ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
}

/**
 * Download queue ViewModel. Uses [MultiThreadDownloader] when multi-threaded
 * download is enabled, else the system [ApkDownloader] (DownloadManager). On
 * failure with [useApiForRelease] enabled, retries once via the GitHub API
 * release-asset link (optionally re-proxied through the mirror).
 */
class DownloadViewModel : ViewModel() {
    private val app get() = GitHubAppStoreApp.container
    private val settings get() = app.settings
    private val repo get() = app.cachedRepository
    private val sysDownloader get() = app.apkDownloader
    private val multiCache = mutableMapOf<Int, MultiThreadDownloader>()
    private fun multi(threadCount: Int): MultiThreadDownloader = multiCache.getOrPut(threadCount) { MultiThreadDownloader(app, threadCount) }
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    data class Task(val asset: GhAsset, val status: DownloadStatus)

    fun enqueue(asset: GhAsset) { val file = resolveDownloadFile(asset); _tasks.value += Task(asset, DownloadStatus.Queued(asset.name)); viewModelScope.launch { val (owner, repoName) = parseOwnerRepo(asset); val url = ProxyUtils.buildDownloadUrl(asset.browserDownloadUrl, settings.mirrorEnabled.first(), settings.mirrorHost.first()); runDownload(asset, url, file, owner, repoName) } }

    fun retry(asset: GhAsset) { val file = resolveDownloadFile(asset); _tasks.value = _tasks.value.map { if (it.asset.id != asset.id) it else it.copy(status = DownloadStatus.Queued(asset.name)) }; viewModelScope.launch { val (owner, repoName) = parseOwnerRepo(asset); val useApi = settings.useApiForRelease.first(); val url = if (useApi) { val p = repo.apiReleaseDownloadUrls(owner, repoName).firstOrNull { it.first == asset.name }; ProxyUtils.buildDownloadUrl(p?.second ?: asset.browserDownloadUrl, settings.mirrorEnabled.first(), settings.mirrorHost.first()) } else ProxyUtils.buildDownloadUrl(asset.browserDownloadUrl, settings.mirrorEnabled.first(), settings.mirrorHost.first()); runDownload(asset, url, file, owner, repoName) } }

    private fun parseOwnerRepo(asset: GhAsset): Pair<String, String> {
        // browser_download_url形如 https://github.com/owner/repo/releases/download/tag/name.apk
        val parts = asset.browserDownloadUrl.split("/").filter { it.isNotEmpty() }
        val idx = parts.indexOf("github.com")
        return if (idx >= 0 && idx + 2 < parts.size) parts[idx + 1] to parts[idx + 2] else "" to ""
    }

    private fun resolveDownloadFile(asset: GhAsset): File {
        if (hasStorageAccess(app)) {
            val pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if ((pub.mkdirs() || pub.exists()) && pub.canWrite()) return File(pub, asset.name)
        }
        val fallback = app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: app.filesDir
        fallback.mkdirs()
        return File(fallback, asset.name)
    }

    private fun runDownload(asset: GhAsset, url: String, file: File, owner: String, repoName: String) { viewModelScope.launch { val useMulti = settings.multiThreadDownload.first(); val tc = settings.downloadThreadCount.first(); val flow = if (useMulti) multi(tc).download(url, file) else sysDownloader.enqueue(asset, url, file); flow.collect { e -> _tasks.value = _tasks.value.map { if (it.asset.id != asset.id) it else when (e) { is DEvent.Queued -> it.copy(status = DownloadStatus.Queued(e.fileName)); is DEvent.Progress -> it.copy(status = DownloadStatus.Progress(e.fileName, e.bytes, e.total)); is DEvent.Succeeded -> it.copy(status = DownloadStatus.Succeeded(e.fileName, e.localUri)); is DEvent.Failed -> it.copy(status = DownloadStatus.Failed(e.fileName, e.reason)) } } } } }

    /** Launch the package installer for a completed task's local APK (content:// via FileProvider). */
    fun install(context: Context, task: Task) {
        val s = task.status; if (s !is DownloadStatus.Succeeded) return
        val uri = Uri.parse(s.localPath)
        val apkUri = if (uri.scheme == "file") {
            val file = File(uri.path ?: return)
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        } else uri
        context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(apkUri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    fun openInstallUnknownSources(context: Context) { context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply { data = Uri.parse("package:${context.packageName}"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
}
