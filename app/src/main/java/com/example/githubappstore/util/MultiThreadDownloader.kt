package com.example.githubappstore.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil

/**
 * Multi-threaded (chunked) APK downloader. Probes remote size, splits into N
 * byte ranges downloaded concurrently via OkHttp `Range`, then concatenates into
 * the final APK. Used when the user enables multi-threaded download in Settings.
 */
class MultiThreadDownloader(private val context: Context, private val threadCount: Int = 4) {
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()

    fun download(url: String, fileName: String): Flow<DownloadEvent> = callbackFlow {
        val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: run { trySend(DownloadEvent.Failed(fileName, "No external files dir")); close(); return@callbackFlow }
        destDir.mkdirs(); val finalFile = File(destDir, fileName)
        trySend(DownloadEvent.Queued(fileName))
        val totalSize = probeSize(url)
        if (totalSize <= 0) { downloadSingle(this, url, finalFile, fileName); close(); return@callbackFlow }
        val n = threadCount.coerceIn(2, 8)
        val chunkSize = ceil(totalSize.toDouble() / n).toLong()
        val chunks = List(n) { i -> Chunk(i * chunkSize, ((i + 1) * chunkSize - 1).coerceAtMost(totalSize - 1), File(destDir, "$fileName.part$i")) }
        val downloadedBytes = AtomicLong(0L)
        val jobs = chunks.map { chunk ->
            launch(Dispatchers.IO) {
                try { downloadRange(url, chunk)
                    downloadedBytes.addAndGet(chunk.end - chunk.start + 1)
                    trySend(DownloadEvent.Progress(fileName, downloadedBytes.get(), totalSize))
                } catch (t: Throwable) { trySend(DownloadEvent.Failed(fileName, t.message ?: "chunk error")) }
            }
        }
        jobs.forEach { it.join() }
        try { mergeChunks(chunks, finalFile); chunks.forEach { it.file.delete() }; trySend(DownloadEvent.Succeeded(fileName, Uri.fromFile(finalFile).toString()))
        } catch (t: Throwable) { trySend(DownloadEvent.Failed(fileName, t.message ?: "merge failed")) }
        close()
    }.conflate()

    private data class Chunk(val start: Long, val end: Long, val file: File) { val length: Long get() = end - start + 1 }

    private fun probeSize(url: String): Long = try {
        client.newCall(Request.Builder().url(url).head().build()).execute().use { it.header("Content-Length")?.toLongOrNull()?.takeIf { s -> s > 0 } }
    } catch (_: Throwable) { null } ?: try {
        client.newCall(Request.Builder().url(url).header("Range", "bytes=0-0").build()).execute().use { resp ->
            if (resp.code == 206) resp.header("Content-Range")?.let { cr -> cr.substringAfterLast("/").trim().toLongOrNull() } else resp.body?.contentLength()?.takeIf { it > 0 }
        }
    } catch (_: Throwable) { null } ?: -1L

    private fun downloadRange(url: String, chunk: Chunk) {
        val req = Request.Builder().url(url).header("Range", "bytes=${chunk.start}-${chunk.end}").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful && resp.code != 206) throw RuntimeException("HTTP ${resp.code}")
            val body = resp.body ?: throw RuntimeException("empty body")
            writeBody(body, chunk.file)
        }
    }

    private fun writeBody(body: ResponseBody, out: File) { out.outputStream().use { os -> body.byteStream().use { ins -> val buf = ByteArray(64 * 1024); var read: Int; while (ins.read(buf).also { read = it } != -1) os.write(buf, 0, read) } } }

    private suspend fun downloadSingle(channel: SendChannel<DownloadEvent>, url: String, dest: File, fileName: String) {
        withContext(Dispatchers.IO) {
            try { val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
                    val body = resp.body ?: throw RuntimeException("empty body")
                    val total = body.contentLength().takeIf { it > 0 } ?: -1L
                    dest.outputStream().use { os -> body.byteStream().use { ins -> val buf = ByteArray(64 * 1024); var written = 0L; var read: Int; while (ins.read(buf).also { read = it } != -1) { os.write(buf, 0, read); written += read; if (total > 0) channel.trySend(DownloadEvent.Progress(fileName, written, total)) } } }
                    channel.trySend(DownloadEvent.Succeeded(fileName, Uri.fromFile(dest).toString()))
                }
            } catch (t: Throwable) { channel.trySend(DownloadEvent.Failed(fileName, t.message ?: "download failed")) }
        }
    }

    private fun mergeChunks(chunks: List<Chunk>, finalFile: File) { RandomAccessFile(finalFile, "rw").use { raf -> raf.setLength(0); chunks.forEach { raf.write(it.file.readBytes()) } } }
}
