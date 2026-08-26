package com.example.githubappstore.ui.account
import kotlinx.coroutines.flow.first

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubappstore.GitHubAppStoreApp
import com.example.githubappstore.data.model.GhUser
import com.example.githubappstore.domain.AppItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Account ViewModel: GitHub PAT sign-in (local only). [stars] filtered to Android/Kotlin/JVM by the repo, so only starred Android apps are shown. */
class AccountViewModel : ViewModel() {
    private val app get() = GitHubAppStoreApp.container; private val settings get() = app.settings; private val repo get() = app.cachedRepository
    private val _token = MutableStateFlow(""); val token: StateFlow<String> = _token.asStateFlow()
    private val _user = MutableStateFlow<GhUser?>(null); val user: StateFlow<GhUser?> = _user.asStateFlow()
    private val _starsState = MutableStateFlow<StarsState>(StarsState.Idle); val starsState: StateFlow<StarsState> = _starsState.asStateFlow()

    init { viewModelScope.launch { val t = settings.githubToken.first(); _token.value = t; if (t.isNotBlank()) verifyAndLoad(t) } }
    fun setToken(t: String) { _token.value = t; viewModelScope.launch { settings.setGithubToken(t) } }
    fun signIn() { val t = _token.value.trim(); if (t.isBlank()) return; viewModelScope.launch { settings.setGithubToken(t) }; verifyAndLoad(t) }
    fun signOut() { setToken(""); _user.value = null; _starsState.value = StarsState.Idle }
    fun refreshStars() { val t = _token.value.trim(); if (t.isBlank()) return; loadStars() }
    private fun verifyAndLoad(token: String) { _starsState.value = StarsState.Loading; viewModelScope.launch { val u = runCatching { repo.authenticatedUser() }.getOrNull(); _user.value = u; if (u == null) _starsState.value = StarsState.Error("Token 无效或网络不可用，无法登录") else loadStars() } }
    private fun loadStars() { _starsState.value = StarsState.Loading; viewModelScope.launch { runCatching { repo.starredAndroidApps() }.onSuccess { _starsState.value = if (it.isEmpty()) StarsState.Empty else StarsState.Success(it) }.onFailure { _starsState.value = StarsState.Error(it.message ?: "加载 starred 仓库失败") } } }

    sealed class StarsState { data object Idle : StarsState(); data object Loading : StarsState(); data object Empty : StarsState(); data class Success(val items: List<AppItem>) : StarsState(); data class Error(val message: String) : StarsState() }
}
