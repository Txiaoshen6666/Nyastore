package com.example.githubappstore.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import com.example.githubappstore.ui.home.HomeRoute
import com.example.githubappstore.ui.dlupdates.DlUpdatesRoute
import com.example.githubappstore.ui.settings.SettingsRoute

/**
 * Root composable. 3-destination M3 Expressive bottom navigation with an
 * [AnimatedContent] shared-axis/fade transition between destinations, giving
 * the bottom-bar switches a calm, directional motion (Material 3 motion).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitStoreApp() {
    val items = listOf(Tab("home", "主页", Icons.Filled.Home, 0), Tab("dlupdates", "下载与更新", Icons.Filled.Download, 1), Tab("settings", "设置", Icons.Filled.Settings, 2))
    var current by remember { mutableStateOf(items.first()) }
    Scaffold(modifier = Modifier, topBar = { CenterAlignedTopAppBar(title = { Text(current.label) }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors()) },
        bottomBar = { NavigationBar {
            items.forEach { tab -> NavigationBarItem(selected = current.key == tab.key, onClick = { current = tab },
                selectedIcon = { Icon(tab.icon, contentDescription = tab.label) },
                unselectedIcon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }, colors = NavigationBarItemDefaults.colors())
            }
        } },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        AnimatedContent(targetState = current, transitionSpec = {
            val direction = if (targetState.order > initialState.order) 1 else -1
            (slideInHorizontally { it * direction } + fadeIn()) togetherWith
                (slideOutHorizontally { -it * direction } + fadeOut())
        }, modifier = Modifier.padding(innerPadding), label = "tab-transition"
        ) { tab ->
            when (tab.key) {
                "home" -> HomeRoute()
                "dlupdates" -> DlUpdatesRoute()
                "settings" -> SettingsRoute()
            }
        }
    }
}

private data class Tab(val key: String, val label: String, val icon: ImageVector, val order: Int)
