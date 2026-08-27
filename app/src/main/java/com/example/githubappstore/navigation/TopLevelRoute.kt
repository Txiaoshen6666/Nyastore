package com.example.githubappstore.navigation

import com.example.githubappstore.R

sealed class TopLevelRoute(
    val route: String,
    val iconRes: Int,
    val label: String
) {
    data object Home : TopLevelRoute("home", R.mipmap.ic_launcher, "首页")
    data object Downloads : TopLevelRoute("downloads", R.mipmap.ic_launcher, "下载")
    data object Settings : TopLevelRoute("settings", R.mipmap.ic_launcher, "设置")
}
