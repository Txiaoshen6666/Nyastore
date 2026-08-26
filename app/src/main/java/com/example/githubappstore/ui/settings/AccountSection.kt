package com.example.githubappstore.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.githubappstore.ui.components.AppCard
import com.example.githubappstore.ui.components.LoadingPraying
import com.example.githubappstore.ui.components.InstallBottomSheet
import com.example.githubappstore.ui.downloads.DownloadViewModel
import com.example.githubappstore.ui.account.AccountViewModel

/** "我的（GitHub 账户）" section, embedded as a child of Settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSection(modifier: Modifier = Modifier, vm: AccountViewModel = viewModel(), dlVm: DownloadViewModel = viewModel()) {
    val token by vm.token.collectAsState(); val user by vm.user.collectAsState(); val starsState by vm.starsState.collectAsState()
    var draft by remember { mutableStateOf(token) }
    var activeApp by remember { mutableStateOf<com.example.githubappstore.domain.AppItem?>(null) }
    var release by remember { mutableStateOf<GhRelease?>(null) }
    LaunchedEffect(activeApp) { activeApp?.let { app -> release = runCatching { GitHubAppStoreApp.container.gitHubRepository.latestRelease(app.repo.ownerLogin, app.repo.name) }.getOrNull() } ?: run { release = null } }

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("我的（GitHub 账户）", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("使用 GitHub PAT 登录，仅本地保存，用于拉取你 Star 的 Android/Kotlin 项目（仅展示 Android 项目）。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (user != null) {
                Text("${user!!.name ?: user!!.login}  (@${user!!.login})", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = { vm.refreshStars() }) { Text("刷新 Star 列表") }; FilledTonalButton(onClick = { vm.signOut() }) { Text("退出登录") } }
            } else {
                OutlinedTextField(value = draft, onValueChange = { draft = it; vm.setToken(it) }, label = { Text("GitHub PAT") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Text("获取方式：GitHub → Settings → Developer settings → Personal access tokens → Fine-grained，授予 repo:read 权限即可。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Button(onClick = { vm.signIn() }, modifier = Modifier.padding(top = 10.dp)) { Text("登录") }
            }
            if (user != null) {
                Text("我 Star 的 Android 应用", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                when (val s = starsState) {
                    is AccountViewModel.StarsState.Loading -> {
                        LoadingPraying()
                    }
                    is AccountViewModel.StarsState.Empty -> {
                        Text("你 Star 的仓库中没有检测到 Android/Kotlin 项目。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is AccountViewModel.StarsState.Error -> {
                        Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    is AccountViewModel.StarsState.Success -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 6.dp), modifier = Modifier.fillMaxWidth()) {
                            items(s.items, key = { it.repo.id }) { app ->
                                AppCard(app = app, onClick = { activeApp = app })
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
    if (activeApp != null) {
        InstallBottomSheet(
            app = activeApp!!,
            release = release,
            onDismiss = { activeApp = null },
            onDownload = { asset -> dlVm.enqueue(asset) },
            onOpenRepo = { activeApp = null }
        )
    }
}
