package com.example.githubappstore.ui.home
import kotlinx.coroutines.flow.first

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubappstore.GitHubAppStoreApp
import com.example.githubappstore.domain.AppCategory
import com.example.githubappstore.domain.AppItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Home feed: "My Stars" (if signed in) first, then popular high-star Android apps. */
class HomeViewModel : ViewModel() {
    private val app get() = GitHubAppStoreApp.container
    private val repo get() = app.cachedRepository
    private val settings get() = app.settings
    private val _feedState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val feedState: StateFlow<FeedUiState> = _feedState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private var loadedCategory: AppCategory = AppCategory.All
    private var loadedQuery: String = ""

    private var currentPage = 1
    private val maxPages = 5
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    init { load() }

    fun load() {
        currentPage = 1
        _canLoadMore.value = true
        _isLoadingMore.value = false
        _feedState.value = FeedUiState.Loading
        viewModelScope.launch {
            runCatching {
                val token = settings.githubToken.first().takeIf { it.isNotBlank() }
                val stars = if (token != null) runCatching { repo.starredAndroidApps() }.getOrDefault(emptyList()) else emptyList()
                val page1 = runCatching { repo.popularAndroidApps(page = 1, perPage = 20) }.getOrDefault(emptyList()).shuffled()
                _canLoadMore.value = page1.size >= 20 && maxPages > 1
                FeedUiState.Success(HomeFeed(stars = stars, popular = page1))
            }.getOrElse { FeedUiState.Error(it.message ?: "加载失败") }.also { _feedState.value = it }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !_canLoadMore.value || currentPage >= maxPages) { _canLoadMore.value = currentPage >= maxPages; return }
        _isLoadingMore.value = true
        viewModelScope.launch {
            val result = runCatching {
                val next = repo.popularAndroidApps(page = currentPage + 1, perPage = 20).shuffled()
                currentPage += 1
                if (next.size < 20 || currentPage >= maxPages) _canLoadMore.value = false
                val cur = (_feedState.value as? FeedUiState.Success)?.feed ?: HomeFeed(emptyList(), emptyList())
                cur.copy(popular = cur.popular + next)
            }
            if (result.isSuccess) {
                _feedState.value = FeedUiState.Success(result.getOrThrow())
            } else {
                _canLoadMore.value = false
            }
            _isLoadingMore.value = false
        }
    }

    fun search(category: AppCategory, query: String) {
        loadedCategory = category; loadedQuery = query
        if (query.trim().length < 2) { _searchState.value = SearchUiState.Idle; return }
        _searchState.value = SearchUiState.Loading
        viewModelScope.launch {
            runCatching {
                val items = if (category == AppCategory.Trending) repo.trendingAndroidApps()
                else repo.searchAndroidApps(query = query, category = category)
                if (items.isEmpty()) SearchUiState.Empty else SearchUiState.Success(items)
            }.getOrElse { SearchUiState.Error(it.message ?: "搜索失败") }.also { _searchState.value = it }
        }
    }

    fun reloadSearch() = search(loadedCategory, loadedQuery)

    data class HomeFeed(val stars: List<AppItem>, val popular: List<AppItem>)
    sealed class FeedUiState { data object Loading : FeedUiState(); data class Success(val feed: HomeFeed) : FeedUiState(); data class Error(val message: String) : FeedUiState() }
    sealed class SearchUiState { data object Idle : SearchUiState(); data object Loading : SearchUiState(); data object Empty : SearchUiState(); data class Success(val items: List<AppItem>) : SearchUiState(); data class Error(val message: String) : SearchUiState() }
}
