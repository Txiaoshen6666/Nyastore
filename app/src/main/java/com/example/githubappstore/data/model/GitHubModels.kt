package com.example.githubappstore.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GhSearchResponse(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("incomplete_results") val incompleteResults: Boolean = false,
    val items: List<GhRepo> = emptyList()
)

@Serializable
data class GhRepo(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val description: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val owner: GhOwner,
    @SerialName("stargazers_count") val stars: Int = 0,
    @SerialName("forks_count") val forks: Int = 0,
    val language: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("default_branch") val defaultBranch: String? = null,
    val topics: List<String>? = null,
    @SerialName("has_apk") val hasApk: Boolean? = null
) { val ownerLogin: String get() = owner.login }

@Serializable
data class GhOwner(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("html_url") val htmlUrl: String
)

@Serializable
data class GhRelease(
    val id: Long,
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("draft") val draft: Boolean = false,
    val assets: List<GhAsset> = emptyList()
) { val apkAsset: GhAsset? get() = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } }

@Serializable
data class GhAsset(
    val id: Long,
    val name: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    @SerialName("size") val size: Long = 0
)

@Serializable
data class GhUser(
    val id: Long,
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val name: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null
)
