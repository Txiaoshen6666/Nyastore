package com.example.githubappstore.util

/** URL helpers for the mirror-proxy feature. */
object ProxyUtils {
    fun applyPrefix(rawUrl: String, prefix: String): String {
        val p = prefix.trim().trimEnd('/')
        if (p.isEmpty()) return rawUrl
        return "$p/$rawUrl"
    }
    fun buildDownloadUrl(rawUrl: String, mirrorEnabled: Boolean, mirrorHost: String): String =
        if (mirrorEnabled) applyPrefix(rawUrl, mirrorHost) else rawUrl
}
