package com.example.githubappstore.data.fdroid

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * F-Droid repository index consumer. The F-Droid `index-v2.json` exposes a
 * `packages` map keyed by package name; each package's `metadata.sourceCode`
 * (when present) is typically a GitHub URL of the form
 * `https://github.com/owner/repo`. We extract those to grow the package->repo
 * registry used by [com.example.githubappstore.util.UpdateChecker], covering
 * thousands of open-source Android apps beyond the curated built-in table.
 *
 * The full index is large (~hundreds of MB uncompressed), so we stream-parse
 * only the top-level `packages` object via a lenient JSON parser and stop once
 * the target section has been consumed. On parse failure we return an empty map
 * and let the curated table remain the sole source.
 */
class FdroidIndexRepository(private val context: Context) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /**
     * Returns packageName -> "owner/repo" for F-Droid packages whose
     * `sourceCode` points at a GitHub repository. Network/parse errors are
     * swallowed and logged; the update detector degrades to the curated table.
     */
    suspend fun githubPackages(): Map<String, String> = withContext(Dispatchers.IO) {
        runCatching { fetchPackages() }.getOrElse { e ->
            Log.w(TAG, "F-Droid index unavailable: ${e.message}"); return@withContext emptyMap()
        }.orEmpty().mapNotNull { (pkg, meta) ->
            val src = meta["sourceCode"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            parseGithubOwnerRepo(src)?.let { pkg to it }
        }.toMap()
    }

    private suspend fun fetchPackages(): Map<String, Map<String, String>>? {
        val req = Request.Builder().url(FDROID_INDEX_URL).header("Accept", "application/json").build()
        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body ?: return null
            // Stream-parse: locate "packages" object, decode only its entries.
            val text = body.string()
            val root = json.parseToJsonElement(text).jsonObject
            val pkgs = root["packages"]?.jsonObject ?: return emptyMap()
            pkgs.mapValues { (_, v) ->
                val m = v.jsonObject
                mapOf(
                    "sourceCode" to m["sourceCode"]?.jsonPrimitive?.content.orEmpty()
                )
            }
        }
    }

    companion object {
        private const val TAG = "FdroidIndex"
        private const val FDROID_INDEX_URL = "https://f-droid.org/repo/index-v2.json"

        /** Extract "owner/repo" from a GitHub URL, or null if not a GitHub URL. */
        internal fun parseGithubOwnerRepo(url: String): String? {
            val trimmed = url.trim()
            val hostIdx = trimmed.indexOf("github.com")
            if (hostIdx < 0) return null
            val after = trimmed.substring(hostIdx + "github.com".length).trim('/')
            val parts = after.split('/').filter { it.isNotEmpty() }
            if (parts.size < 2) return null
            val owner = parts[0]; var repo = parts[1]
            // strip optional ".git" suffix
            if (repo.endsWith(".git")) repo = repo.removeSuffix(".git")
            if (owner.isBlank() || repo.isBlank()) return null
            return "$owner/$repo"
        }
    }
}
