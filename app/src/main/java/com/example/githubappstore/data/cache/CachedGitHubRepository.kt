package com.example.githubappstore.data.cache

import com.example.githubappstore.data.GitHubRepository
import com.example.githubappstore.data.model.GhAsset
import com.example.githubappstore.data.model.GhRelease
import com.example.githubappstore.data.model.GhRepo
import com.example.githubappstore.data.model.GhUser
import com.example.githubappstore.domain.AppCategory
import com.example.githubappstore.domain.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Decorator over [GitHubRepository] that caches expensive read-only calls in
 * Room with a 1-hour TTL. Keeps the network as the source of truth on cache
 * miss/expiry while letting the UI render instantly from local data and
 * drastically reducing GitHub API rate-limit pressure (60/h unauthenticated,
 * 5000/h with PAT).
 */
class CachedGitHubRepository(
    private val upstream: GitHubRepository, private val db: AppDatabase
) {
    private val dao get() = db.cacheDao()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val ttlMs = 60 * 60 * 1000L // 1 hour

    private fun GhRepo.cached(fullNameKey: String, feed: String, now: Long) = CachedRepo(
        id = id, name = name, fullName = fullName, description = description, htmlUrl = htmlUrl,
        ownerLogin = owner.login, ownerAvatar = owner.avatarUrl, stars = stars, forks = forks,
        language = language, updatedAt = updatedAt, topicsJson = null, feed = feed, cachedAt = now
    )

    private fun CachedRepo.toRepo() = GhRepo(
        id = id, name = name, fullName = fullName, description = description, htmlUrl = htmlUrl,
        owner = com.example.githubappstore.data.model.GhOwner(login = ownerLogin, avatarUrl = ownerAvatar, htmlUrl = "https://github.com/$ownerLogin"),
        stars = stars, forks = forks, language = language, updatedAt = updatedAt, defaultBranch = null, topics = null, hasApk = null
    )

    private fun isFresh(cachedAt: Long) = System.currentTimeMillis() - cachedAt < ttlMs

    suspend fun searchAndroidApps(query: String, category: AppCategory, page: Int = 1): List<AppItem> {
        val feed = "search:${query}:${category.key}:$page"
        val cached = dao.reposByFeed(feed).takeIf { it.isNotEmpty() && isFresh(it.first().cachedAt) }
        if (cached != null) return cached.map { AppItem(repo = it.toRepo(), category = category) }
        val fresh = upstream.searchAndroidApps(query, category, page)
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) { dao.clearFeed(feed); dao.upsertRepos(fresh.map { it.repo.cached(fullName, feed, now) }) }
        return fresh
    }

    suspend fun trendingAndroidApps(): List<AppItem> {
        val feed = "trending"; val cached = dao.reposByFeed(feed).takeIf { it.isNotEmpty() && isFresh(it.first().cachedAt) }
        if (cached != null) return cached.map { AppItem(repo = it.toRepo(), category = AppCategory.Trending) }
        val fresh = upstream.trendingAndroidApps()
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) { dao.clearFeed(feed); dao.upsertRepos(fresh.map { it.repo.cached(it.repo.fullName, feed, now) }) }
        return fresh
    }

    suspend fun popularAndroidApps(perPage: Int = 25): List<AppItem> {
        val feed = "popular"; val cached = dao.reposByFeed(feed).takeIf { it.isNotEmpty() && isFresh(it.first().cachedAt) }
        if (cached != null) return cached.map { AppItem(repo = it.toRepo(), category = AppCategory.Trending) }
        val fresh = upstream.popularAndroidApps(perPage)
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) { dao.clearFeed(feed); dao.upsertRepos(fresh.map { it.repo.cached(it.repo.fullName, feed, now) }) }
        return fresh
    }

    suspend fun starredAndroidApps(): List<AppItem> {
        val row = dao.latestStarredRow()
        val cached = row?.json?.let { runCatching { json.decodeFromString(ListSerializer(GhRepo.serializer()), it) }.getOrNull() }
        if (cached != null && cached.isNotEmpty() && isFresh(row.cachedAt)) {
            return cached.map { AppItem(repo = it, category = AppCategory.Trending) }
        }
        val fresh = upstream.starredAndroidApps()
        val ser = json.encodeToString(ListSerializer(GhRepo.serializer()), fresh.map { it.repo })
        withContext(Dispatchers.IO) { dao.upsertStarred(CachedStarred(id = 1L, json = ser, cachedAt = System.currentTimeMillis())) }
        return fresh
    }

    suspend fun latestRelease(owner: String, repo: String): GhRelease? {
        val key = "$owner/$repo"; val cached = dao.release(key)
        if (cached != null && isFresh(cached.cachedAt)) return cached.toRelease(key)
        val fresh = upstream.latestRelease(owner, repo)
        if (fresh != null) withContext(Dispatchers.IO) {
            dao.upsertRelease(CachedRelease(key = key, tagName = fresh.tagName, name = fresh.name, body = fresh.body, htmlUrl = fresh.htmlUrl, publishedAt = fresh.publishedAt, prerelease = fresh.prerelease, apkAssetsJson = json.encodeToString(ListSerializer(GhAsset.serializer()), fresh.assets), cachedAt = System.currentTimeMillis()))
        }
        return fresh
    }

    suspend fun apiReleaseDownloadUrls(owner: String, repo: String): List<Pair<String, String>> =
        upstream.apiReleaseDownloadUrls(owner, repo)

    suspend fun getRepo(owner: String, repo: String): GhRepo = upstream.getRepo(owner, repo)
    suspend fun authenticatedUser(): GhUser = upstream.authenticatedUser()

    private fun CachedRelease.toRelease(key: String): GhRelease {
        val assets = runCatching { json.decodeFromString(ListSerializer(GhAsset.serializer()), apkAssetsJson) }.getOrDefault(emptyList())
        return GhRelease(id = key.hashCode().toLong(), tagName = tagName, name = name, body = body, htmlUrl = htmlUrl, publishedAt = publishedAt, prerelease = prerelease, draft = false, assets = assets)
    }
}
