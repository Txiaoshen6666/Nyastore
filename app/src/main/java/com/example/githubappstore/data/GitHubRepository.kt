package com.example.githubappstore.data

import com.example.githubappstore.data.model.GhRelease
import com.example.githubappstore.data.model.GhRepo
import com.example.githubappstore.data.remote.GitHubApiService
import com.example.githubappstore.domain.AppCategory
import com.example.githubappstore.domain.AppItem
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Single source of truth for GitHub data. The "trending" view is approximated
 * by a recent-push, highly-starred Android search (GitHub has no public trending
 * API); this is intentionally simple and fully replaceable.
 */
class GitHubRepository(
    private val api: GitHubApiService,
    private val settings: com.example.githubappstore.data.settings.AppSettings
) {
    private suspend fun authHeader(): String? =
        settings.githubToken.first().takeIf { it.isNotBlank() }?.let { "Bearer $it" }

    /** Search Android/Kotlin/Java repos by query, sorted by stars. */
    suspend fun searchAndroidApps(query: String, category: AppCategory, page: Int = 1): List<AppItem> {
        val q = buildString {
            append(query.trim().ifBlank { "" })
            if (isNotEmpty()) append(" ")
            append("language:Kotlin OR language:Java")
            append(" has:releases")
        }.trim()
        return api.searchRepos(q = q, sort = "stars", order = "desc", perPage = 30, page = page, auth = authHeader())
            .items.map { AppItem(repo = it, category = category) }
    }

    /** Approximate "trending today": recently updated + popular Android projects. */
    suspend fun trendingAndroidApps(): List<AppItem> {
        val since = LocalDate.now().minusMonths(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return api.searchRepos(
            q = "language:Kotlin stars:>500 pushed:>$since",
            sort = "updated", order = "desc", perPage = 25, auth = authHeader()
        ).items.map { AppItem(repo = it, category = AppCategory.Trending) }
    }

    suspend fun getRepo(owner: String, repo: String): GhRepo =
        api.getRepo(owner = owner, repo = repo, auth = authHeader())

    suspend fun latestRelease(owner: String, repo: String): GhRelease? =
        api.listReleases(owner = owner, repo = repo, perPage = 10, auth = authHeader())
            .firstOrNull { !it.draft }

    /** Latest non-draft release's APK asset URLs via REST API (fallback for CDN failures). */
    suspend fun apiReleaseDownloadUrls(owner: String, repo: String): List<Pair<String, String>> =
        api.listReleases(owner = owner, repo = repo, perPage = 5, auth = authHeader())
            .firstOrNull { !it.draft }
            ?.assets
            ?.filter { it.name.endsWith(".apk", ignoreCase = true) }
            ?.map { it.name to it.browserDownloadUrl }
            ?: emptyList()

    /** Authenticated user's starred repos, filtered to Android/Kotlin/Java projects. */
    suspend fun starredAndroidApps(): List<AppItem> {
        val collected = mutableListOf<GhRepo>()
        var page = 1
        while (page <= 5) {
            val batch = api.listStarred(perPage = 100, page = page, auth = authHeader())
            if (batch.isEmpty()) break
            collected += batch
            if (batch.size < 100) break
            page++
        }
        return collected
            .filter { repo ->
                val lang = repo.language
                lang.equals("Kotlin", ignoreCase = true) ||
                    lang.equals("Java", ignoreCase = true) ||
                    (repo.topics?.any { it.equals("android", ignoreCase = true) } == true)
            }
            .map { AppItem(repo = it, category = AppCategory.Trending) }
    }

    suspend fun authenticatedUser(): com.example.githubappstore.data.model.GhUser =
        api.getAuthenticatedUser(auth = authHeader())

    /** High-star open-source Android apps for the home "popular" feed. */
    suspend fun popularAndroidApps(perPage: Int = 25): List<AppItem> {
        val resp = api.searchRepos(
            q = "language:Kotlin stars:>3000 has:releases",
            sort = "stars", order = "desc", perPage = perPage, auth = authHeader()
        )
        return resp.items.map { AppItem(repo = it, category = AppCategory.Trending) }
    }
}
