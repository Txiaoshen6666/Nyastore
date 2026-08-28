package com.example.githubappstore.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import android.widget.Toast
import com.example.githubappstore.GitHubAppStoreApp
import com.example.githubappstore.data.model.GhRelease
import com.example.githubappstore.data.model.GhAsset
import com.example.githubappstore.domain.AppCategory
import com.example.githubappstore.domain.AppItem
import com.example.githubappstore.ui.components.AppCard
import com.example.githubappstore.ui.components.LoadingPraying
import com.example.githubappstore.ui.components.StaggeredLazyColumn
import com.example.githubappstore.ui.components.StaggerItem
import com.example.githubappstore.ui.components.InstallBottomSheet
import com.example.githubappstore.ui.downloads.DownloadViewModel

/**
 * Home route: recommendation feed (My Stars first + popular high-star) + search
 * (submitted on click/keyboard Search, not per keystroke) + pull-to-refresh.
 * Pull-to-refresh fetches a fresh, varied batch (random sort + random page) and
 * shuffles it. Category switching is hidden from the UI but the [selected] state
 * is retained.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(homeVm: HomeViewModel = viewModel(), dlVm: DownloadViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)) {
    val feedState by homeVm.feedState.collectAsState()
    val searchState by homeVm.searchState.collectAsState()
    val isLoadingMore by homeVm.isLoadingMore.collectAsState()
    val canLoadMore by homeVm.canLoadMore.collectAsState()
    val listState = rememberLazyListState()
    var selected by remember { mutableStateOf(AppCategory.All) }
    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var activeApp by remember { mutableStateOf<AppItem?>(null) }
    var release by remember { mutableStateOf<GhRelease?>(null) }
    var releaseError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun submitSearch() {
        if (query.trim().length >= 2) homeVm.search(selected, query) else homeVm.search(selected, "")
    }
    LaunchedEffect(activeApp) {
        release = null; releaseError = null
        activeApp?.let { app ->
            runCatching { GitHubAppStoreApp.container.cachedRepository.latestRelease(app.repo.ownerLogin, app.repo.name) }
                .onSuccess { release = it; releaseError = null }
                .onFailure { releaseError = it.message ?: "获取发布信息失败" }
        } ?: run { release = null }
    }

    val refreshState = rememberPullToRefreshState()
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(feedState, searchState) {
        if (feedState !is HomeViewModel.FeedUiState.Loading && searchState !is HomeViewModel.SearchUiState.Loading) {
            refreshing = false
        }
    }
    val isSearching = query.trim().length >= 2 && searchState !is HomeViewModel.SearchUiState.Idle

    LaunchedEffect(listState, feedState, isLoadingMore, canLoadMore, isSearching) {
        if (isSearching || !canLoadMore || isLoadingMore) return@LaunchedEffect
        val layoutInfo = listState.layoutInfo
        val total = layoutInfo.totalItemsCount
        if (total == 0) return@LaunchedEffect
        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastVisible >= total - 4) homeVm.loadMore()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("GitStore", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { searchActive = true }) {
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
        }
        AnimatedVisibility(
            visible = searchActive,
            enter = fadeIn(initialAlpha = 0.4f) + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch(); searchActive = false }),
                leadingIcon = {
                    IconButton(onClick = { submitSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { searchActive = false; query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
            )
        }

        PullToRefreshBox(modifier = Modifier.fillMaxWidth().weight(1f), state = refreshState, isRefreshing = refreshing, onRefresh = { refreshing = true; if (isSearching) homeVm.reloadSearch() else homeVm.load(forceRefresh = true) }) {
            if (isSearching) when (val s = searchState) {
                is HomeViewModel.SearchUiState.Loading -> LoadingPraying()
                is HomeViewModel.SearchUiState.Empty -> Text("没有找到相关应用", modifier = Modifier.padding(16.dp))
                is HomeViewModel.SearchUiState.Error -> Text(s.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                is HomeViewModel.SearchUiState.Success -> StaggeredLazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item { Text("搜索结果", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp)) }
                    items(s.items, key = { it.repo.id }) { app -> StaggerItem { AppCard(app = app, onClick = { activeApp = app }) } }
                }
                else -> Unit
            } else when (val hs = feedState) {
                is HomeViewModel.FeedUiState.Loading -> LoadingPraying()
                is HomeViewModel.FeedUiState.Error -> Text(hs.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                is HomeViewModel.FeedUiState.Success -> { val feed = hs.feed; StaggeredLazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    if (feed.stars.isNotEmpty()) { item { Text("我 Star 的", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) }; items(feed.stars, key = { "star-${it.repo.id}" }) { app -> StaggerItem { AppCard(app = app, onClick = { activeApp = app }) } } }
                    if (feed.popular.isEmpty()) item { Text("暂无推荐数据", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    else items(feed.popular, key = { "pop-${it.repo.id}" }) { app -> StaggerItem { AppCard(app = app, onClick = { activeApp = app }) } }
                    if (isLoadingMore) item { CircularProgressIndicator(modifier = Modifier.fillMaxWidth().padding(16.dp)) }
                } }
            }
        }
        if (activeApp != null) InstallBottomSheet(app = activeApp!!, release = release, releaseError = releaseError, onDismiss = { activeApp = null }, onDownload = { asset -> Toast.makeText(context, "开始下载: ${asset.name}", Toast.LENGTH_SHORT).show(); dlVm.enqueue(asset); activeApp = null }, onOpenRepo = { activeApp = null }, onRetryRelease = { activeApp?.let { app -> releaseError = null; scope.launch { release = runCatching { GitHubAppStoreApp.container.cachedRepository.latestRelease(app.repo.ownerLogin, app.repo.name) }.getOrNull() } } })
    }
}
