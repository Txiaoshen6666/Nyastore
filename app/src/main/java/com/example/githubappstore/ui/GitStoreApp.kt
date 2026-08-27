package com.example.githubappstore.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.githubappstore.R
import com.example.githubappstore.navigation.TopLevelRoute
import com.example.githubappstore.ui.dlupdates.DlUpdatesRoute
import com.example.githubappstore.ui.home.HomeRoute
import com.example.githubappstore.ui.settings.SettingsRoute

@Composable
fun GitStoreApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { GitStoreBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelRoute.Home.route,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            composable(TopLevelRoute.Home.route) { HomeRoute() }
            composable(TopLevelRoute.Downloads.route) { DlUpdatesRoute() }
            composable(TopLevelRoute.Settings.route) { SettingsRoute() }
        }
    }
}

@Composable
fun GitStoreBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        TopLevelRoute.Home,
        TopLevelRoute.Downloads,
        TopLevelRoute.Settings,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        windowInsets = WindowInsets.navigationBars
    ) {
        items.forEach { route ->
            val selected = currentRoute == route.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(route.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = route.iconRes),
                        contentDescription = route.label
                    )
                },
                label = { Text(route.label) }
            )
        }
    }
}
