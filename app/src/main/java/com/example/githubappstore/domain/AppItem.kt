package com.example.githubappstore.domain

import com.example.githubappstore.data.model.GhRepo

data class AppItem(val repo: GhRepo, val category: AppCategory = AppCategory.All) {
    val fullName: String get() = repo.fullName
    val displayName: String get() = repo.name
    val description: String get() = repo.description ?: ""
    val stars: Int get() = repo.stars
    val language: String? get() = repo.language
    val htmlUrl: String get() = repo.htmlUrl
    val avatarUrl: String get() = repo.owner.avatarUrl
}

enum class AppCategory(val key: String, val label: String) {
    All("all", "全部"),
    Trending("trending", "今日热榜"),
    Tools("tools", "工具"),
    Media("media", "多媒体"),
    Social("social", "社交通讯"),
    Productivity("productivity", "效率"),
    Privacy("privacy", "隐私安全"),
    Game("game", "游戏");

    companion object { fun fromKey(k: String) = values().firstOrNull { it.key == k } ?: All }
}

sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data class Queued(val fileName: String) : DownloadStatus()
    data class Progress(val fileName: String, val bytes: Long, val total: Long) : DownloadStatus()
    data class Succeeded(val fileName: String, val localPath: String) : DownloadStatus()
    data class Failed(val fileName: String, val reason: String) : DownloadStatus()
}
