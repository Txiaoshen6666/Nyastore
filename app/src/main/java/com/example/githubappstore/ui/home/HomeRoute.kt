package com.example.githubappstore.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubappstore.GitHubAppStoreApp
import com.example.githubappstore.data.model.GhRelease
import com.example.githubappstore.domain.AppCategory
import com.example.githubappstore.domain.AppItem
import com.example.githubappstore.ui.components.AppCard
import com.example.githubappstore.ui.components.LoadingPraying
import com.example.githubappstore.ui.components.StaggeredLazyColumn
import com.example.githubappstore.ui.components.StaggerItem
import com.example.githubappstore.ui.components.InstallBottomSheet
import com.example.githubappstore.ui.downloads.DownloadViewModel
import kotlinx.coroutines.delay

/**
 * Home route: recommendation feed (My Stars first + popular high-star) + search
 * (300ms debounce -> GitHub Search API) + pull-to-refresh. Category switching is
 * hidden from the UI but the [selected] state is retained.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(homeVm: HomeViewModel = viewModel(), dlVm: DownloadViewModel = viewModel()) {
    val feedState by homeVm.feedState.collectAsState()
    val searchState by homeVm.searchState.collectAsState()
    var selected by remember { mutableStateOf(AppCategory.All) }
    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var activeApp by remember { mutableStateOf<AppItem?>(null) }
    var release by remember { mutableStateOf<GhRelease?>(null) }

    LaunchedEffect(query, selected) { if (query.trim().length >= 2) { delay(300); homeVm.search(selected, query) } else homeVm.search(selected, "") }
    LaunchedEffect(activeApp) { activeApp?.let { app -> release = runCatching { GitHubAppStoreApp.container.cachedRepository.latestRelease(app.repo.ownerLogin, app.repo.name) }.getOrNull() } ?: run { release = null } }

    val refreshState = rememberPullToRefreshState()
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(feedState, searchState) {
        if (feedState !is HomeViewModel.FeedUiState.Loading && searchState !is HomeViewModel.SearchUiState.Loading) {
            refreshing = false
        }
    }
    val isSearching = query.trim().length >= 2 && searchState !is HomeViewModel.SearchUiState.Idle

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("GitStore", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { searchActive = true }) {
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
        }
        if (searchActive) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searchActive = false }),
                trailingIcon = {
                    IconButton(onClick = { searchActive = false; query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
            )
        }

        PullToRefreshBox(modifier = Modifier.fillMaxSize(), state = refreshState, isRefreshing = refreshing, onRefresh = { refreshing = true; if (isSearching) homeVm.reloadSearch() else homeVm.load() }) {
            if (isSearching) when (val s = searchState) {
                is HomeViewModel.SearchUiState.Loading -> LoadingPraying()
                is HomeViewModel.SearchUiState.Empty -> Text("没有找到相关应用", modifier = Modifier.padding(16.dp))
                is HomeViewModel.SearchUiState.Error -> Text(s.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                is HomeViewModel.SearchUiState.Success -> StaggeredLazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item { Text("搜索结果", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp)) }
                    items(s.items, key = { it.repo.id }) { app -> StaggerItem { AppCard(app = app, onClick = { activeApp = app }) } }
                }
                else -> Unit
            } else when (val hs = feedState) {
                is HomeViewModel.FeedUiState.Loading -> LoadingPraying()
                is HomeViewModel.FeedUiState.Error -> Text(hs.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                is HomeViewModel.FeedUiState.Success -> { val feed = hs.feed; StaggeredLazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    if (feed.stars.isNotEmpty()) { item { Text("我 Star 的", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) }; items(feed.stars, key = { "star-${it.repo.id}" }) { app -> StaggerItem { AppCard(app = app, onClick = { activeApp = app }) } } }
                    item { Text("热门推荐", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = if (feed.stars.isNotEmpty()) 12.dp else 4.dp, bottom = 4.dp)) }
                    if (feed.popular.isEmpty()) item { Text("暂无推荐数据", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    else items(feed.popular, key = { "pop-${it.repo.id}" }) { app -> StaggerItem { AppCard(app = app, onClick = { activeApp = app }) } }
                } }
            }
    if (activeApp != null) InstallBottomSheet(app = activeApp!!, release = release, onDismiss = { activeApp = null }, onDownload = { asset -> dlVm.enqueue(asset); activeApp = null }, onOpenRepo = { activeApp = null })
}
