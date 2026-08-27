package com.example.githubappstore.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TopLevelRoute(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    data object Home : TopLevelRoute("home", Icons.Filled.Home, "首页")
    data object Downloads : TopLevelRoute("downloads", Icons.Filled.GetApp, "下载")
    data object Settings : TopLevelRoute("settings", Icons.Filled.Settings, "设置")
}
