package com.example.githubappstore.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached repository search/list result (stars/popular/trending feeds). */
@Entity(tableName = "cached_repos")
data class CachedRepo(
    @PrimaryKey val id: Long,
    val name: String, val fullName: String,
    val description: String?, val htmlUrl: String,
    val ownerLogin: String, val ownerAvatar: String,
    val stars: Int, val forks: Int,
    val language: String?, val updatedAt: String?,
    val topicsJson: String?, val feed: String, val cachedAt: Long
)

/** Cached latest (non-draft) release for a repo, keyed by "owner/repo". */
@Entity(tableName = "cached_releases")
data class CachedRelease(
    @PrimaryKey val key: String, val tagName: String,
    val name: String?, val body: String?, val htmlUrl: String,
    val publishedAt: String?, val prerelease: Boolean,
    val apkAssetsJson: String, val cachedAt: Long
)

/** Cached starred-repos result for the authenticated user. */
@Entity(tableName = "cached_starred")
data class CachedStarred(@PrimaryKey val id: Long, val json: String, val cachedAt: Long)
