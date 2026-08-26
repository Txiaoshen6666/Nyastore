package com.example.githubappstore.ui.updates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubappstore.GitHubAppStoreApp
import com.example.githubappstore.data.model.GhRelease
import com.example.githubappstore.domain.AppItem
import com.example.githubappstore.util.effectiveGithubPackages
import com.example.githubappstore.util.UpdateCandidate
import com.example.githubappstore.util.listInstalledGithubApps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Scans installed apps present in the known-GitHub-apps registry (curated + F-Droid index)
 * and compares with the latest non-draft release, using [semver4j] for version comparison. */
class UpdatesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo get() = GitHubAppStoreApp.container.cachedRepository
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun scan() {
        _uiState.value = UiState.Scanning
        viewModelScope.launch {
            val known = effectiveGithubPackages(getApplication())
            val installed = listInstalledGithubApps(getApplication(), known)
            if (installed.isEmpty()) { _uiState.value = UiState.NoInstalledGithubApps; return@launch }
            val candidates = mutableListOf<UpdateCandidate>()
            installed.forEachIndexed { idx, info ->
                val fullName = known[info.packageName] ?: return@forEachIndexed
                val (owner, name) = fullName.split("/", limit = 2).let { it[0] to it.getOrNull(1).orEmpty() }
                val release: GhRelease? = runCatching { repo.latestRelease(owner, name) }.getOrNull()
                val appItem = AppItem(
                    repo = com.example.githubappstore.data.model.GhRepo(
                        id = info.packageName.hashCode().toLong(), name = info.appName.ifBlank { name },
                        fullName = fullName, description = null, htmlUrl = "https://github.com/$fullName",
                        owner = com.example.githubappstore.data.model.GhOwner(login = owner, avatarUrl = "", htmlUrl = "https://github.com/$owner"),
                        stars = 0, forks = 0, language = "Kotlin", updatedAt = null, defaultBranch = null, topics = null, hasApk = null
                    ), category = com.example.githubappstore.domain.AppCategory.Tools
                )
                candidates += UpdateCandidate(installed = info, appItem = appItem, latestRelease = release, latestVersionTag = release?.tagName)
                _uiState.value = UiState.ScanningWithProgress(idx + 1, installed.size)
            }
            _uiState.value = UiState.Result(candidates)
        }
    }

    sealed class UiState {
        data object Idle : UiState()
        data object Scanning : UiState()
        data class ScanningWithProgress(val done: Int, val total: Int) : UiState()
        data object NoInstalledGithubApps : UiState()
        data class Result(val candidates: List<UpdateCandidate>) : UiState()
    }
}
