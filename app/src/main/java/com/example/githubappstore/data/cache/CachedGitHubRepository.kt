package com.example.githubappstore.data.cache

import com.example.githubappstore.data.model.GhAsset
import com.example.githubappstore.data.model.GhOwner
import com.example.githubappstore.data.model.GhRelease
import com.example.githubappstore.data.model.GhRepo
import com.example.githubappstore.data.model.GhUser
import com.example.githubappstore.data.remote.GitHubApiService
import com.example.githubappstore.domain.AppCategory
import com.example.githubappstore.domain.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Decorator over [GitHubApiService] that caches expensive read-only calls in
 * Room with a 1-hour TTL. Keeps the network as the source of truth on cache
 * miss/expiry while letting the UI render instantly from local data and
 * drastically reducing GitHub API rate-limit pressure (60/h unauthenticated,
 * 5000/h with PAT).
 *
 * Exposes the higher-level Android-app oriented queries consumed by the
 * ViewModels; the optional [getToken] supplies the GitHub PAT used for the
 * authenticated "my stars" / account calls.
 */
class CachedGitHubRepository(
    private val upstream: GitHubApiService,
    private val db: AppDatabase,
    private val getToken: suspend () -> String? = { null }
) {
    private val dao get() = db.cacheDao()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val ttlMs = 60 * 60 * 1000L

    private fun isFresh(cachedAt: Long) = System.currentTimeMillis() - cachedAt < ttlMs

    private fun GhRepo.cached(feed: String, now: Long) = CachedRepo(
        id = id, name = name, fullName = fullName, description = description, htmlUrl = htmlUrl,
        ownerLogin = owner.login, ownerAvatar = owner.avatarUrl, stars = stars, forks = forks,
        language = language, updatedAt = updatedAt, topicsJson = null, feed = feed, cachedAt = now
    )

    private fun CachedRepo.toRepo() = GhRepo(
        id = id, name = name, fullName = fullName, description = description, htmlUrl = htmlUrl,
        owner = GhOwner(login = ownerLogin, avatarUrl = ownerAvatar, htmlUrl = "https://github.com/$ownerLogin"),
        stars = stars, forks = forks, language = language, updatedAt = updatedAt, defaultBranch = null, topics = null, hasApk = null
    )

    private fun CachedRelease.toRelease(): GhRelease {
        val assets = runCatching {
            json.decodeFromString(ListSerializer(GhAsset.serializer()), apkAssetsJson)
        }.getOrDefault(emptyList())
        return GhRelease(
            id = key.hashCode().toLong(), tagName = tagName, name = name, body = body,
            htmlUrl = htmlUrl, publishedAt = publishedAt, prerelease = prerelease,
            draft = false, assets = assets
        )
    }

    private fun buildSearchQuery(query: String, category: AppCategory): String {
        val base = query.trim().ifBlank { "android" }
        val topic = when (category) {
            AppCategory.All, AppCategory.Trending -> "android"
            else -> category.key
        }
        return "$base topic:$topic"
    }

    suspend fun searchAndroidApps(query: String, category: AppCategory, page: Int = 1): List<AppItem> {
        val q = buildSearchQuery(query, category)
        val feed = "search:${query}:${category.key}:$page"
        val cached = dao.reposByFeed(feed).takeIf { it.isNotEmpty() && isFresh(it.first().cachedAt) }
        if (cached != null) return cached.map { AppItem(repo = it.toRepo(), category = category) }
        val fresh = upstream.searchRepos(q = q, page = page).items
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            dao.clearFeed(feed)
            dao.upsertRepos(fresh.map { it.cached(feed, now) })
        }
        return fresh.map { AppItem(repo = it, category = category) }
    }

    suspend fun trendingAndroidApps(): List<AppItem> {
        val feed = "trending"
        val cached = dao.reposByFeed(feed).takeIf { it.isNotEmpty() && isFresh(it.first().cachedAt) }
        if (cached != null) return cached.map { AppItem(repo = it.toRepo(), category = AppCategory.Trending) }
        val fresh = upstream.searchRepos(q = "topic:android", sort = "updated", order = "desc", perPage = 30).items
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            dao.clearFeed(feed)
            dao.upsertRepos(fresh.map { it.cached(feed, now) })
        }
        return fresh.map { AppItem(repo = it, category = AppCategory.Trending) }
    }

    suspend fun popularAndroidApps(perPage: Int = 25): List<AppItem> {
        val feed = "popular"
        val cached = dao.reposByFeed(feed).takeIf { it.isNotEmpty() && isFresh(it.first().cachedAt) }
        if (cached != null) return cached.map { AppItem(repo = it.toRepo(), category = AppCategory.Trending) }
        val fresh = upstream.searchRepos(
            q = "topic:android stars:>1000", sort = "stars", order = "desc", perPage = perPage
        ).items
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            dao.clearFeed(feed)
            dao.upsertRepos(fresh.map { it.cached(feed, now) })
        }
        return fresh.map { AppItem(repo = it, category = AppCategory.Trending) }
    }

    suspend fun starredAndroidApps(): List<AppItem> {
        val token = getToken() ?: return emptyList()
        val row = dao.latestStarredRow()
        val cached = row?.json?.let {
            runCatching { json.decodeFromString(ListSerializer(GhRepo.serializer()), it) }.getOrNull()
        }
        if (cached != null && cached.isNotEmpty() && isFresh(row.cachedAt)) {
            return cached.map { AppItem(repo = it, category = AppCategory.Trending) }
        }
        val fresh = upstream.listStarred(auth = "Bearer $token")
        val ser = json.encodeToString(ListSerializer(GhRepo.serializer()), fresh)
        withContext(Dispatchers.IO) {
            dao.upsertStarred(CachedStarred(id = 1L, json = ser, cachedAt = System.currentTimeMillis()))
        }
        return fresh.map { AppItem(repo = it, category = AppCategory.Trending) }
    }

    suspend fun latestRelease(owner: String, repo: String): GhRelease? {
        val key = "$owner/$repo"
        val cached = dao.release(key)
        if (cached != null && isFresh(cached.cachedAt)) return cached.toRelease()
        val releases = upstream.listReleases(owner, repo)
        val chosen = releases.firstOrNull { !it.draft && !it.prerelease } ?: releases.firstOrNull()
        if (chosen != null) withContext(Dispatchers.IO) {
            dao.upsertRelease(
                CachedRelease(
                    key = key, tagName = chosen.tagName, name = chosen.name, body = chosen.body,
                    htmlUrl = chosen.htmlUrl, publishedAt = chosen.publishedAt, prerelease = chosen.prerelease,
                    apkAssetsJson = json.encodeToString(ListSerializer(GhAsset.serializer()), chosen.assets),
                    cachedAt = System.currentTimeMillis()
                )
            )
        }
        return chosen
    }

    suspend fun apiReleaseDownloadUrls(owner: String, repo: String): List<Pair<String, String>> =
        upstream.listReleases(owner, repo).flatMap { rel ->
            rel.assets.map { it.name to it.browserDownloadUrl }
        }

    suspend fun getRepo(owner: String, repo: String): GhRepo = upstream.getRepo(owner, repo)

    suspend fun authenticatedUser(): GhUser {
        val token = getToken() ?: throw IllegalStateException("No GitHub PAT configured")
        return upstream.getAuthenticatedUser(auth = "Bearer $token")
    }
}
