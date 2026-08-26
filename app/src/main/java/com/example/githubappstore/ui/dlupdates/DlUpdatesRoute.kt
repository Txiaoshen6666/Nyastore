package com.example.githubappstore.ui.dlupdates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.githubappstore.domain.DownloadStatus
import com.example.githubappstore.ui.components.InstallBottomSheet
import com.example.githubappstore.ui.components.formatSize
import com.example.githubappstore.ui.components.StaggeredLazyColumn
import com.example.githubappstore.ui.components.StaggerItem
import com.example.githubappstore.ui.downloads.DownloadViewModel
import com.example.githubappstore.ui.updates.UpdatesViewModel
import com.example.githubappstore.util.UpdateCandidate

/**
 * Downloads & Updates (combined tab). Two sections in one scrollable surface:
 *  - 下载任务: active/cached download tasks with progress + Install when finished.
 *  - 更新检测: scan installed open-source apps, compare with latest GitHub release, offer one-tap update.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DlUpdatesRoute(dlVm: DownloadViewModel = viewModel(), upVm: UpdatesViewModel = viewModel()) {
    val tasks by dlVm.tasks.collectAsState()
    val upState by upVm.uiState.collectAsState()
    val refreshing = upState is UpdatesViewModel.UiState.Scanning
    val refreshState = rememberPullToRefreshState()
    var sheetCandidate by remember { mutableStateOf<UpdateCandidate?>(null) }
    val context = LocalContext.current

    PullToRefreshBox(isRefreshing = refreshing, state = refreshState, onRefresh = { upVm.scan() }) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text("下载任务", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            if (tasks.isEmpty()) Text("暂无下载任务", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            else Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) { tasks.forEach { DownloadTaskCard(task = it, onInstall = { dlVm.install(context, it) }) } }
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Text("更新检测", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
            when (val s = upState) {
                is UpdatesViewModel.UiState.Idle -> { Text("点击下方按钮检测设备上已安装的开源应用是否有更新。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp)); Button(onClick = { upVm.scan() }, modifier = Modifier.fillMaxWidth()) { Text("开始检测更新") } }
                is UpdatesViewModel.UiState.Scanning -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) { CircularProgressIndicator(); Text("正在扫描已安装的开源应用…", style = MaterialTheme.typography.bodyMedium) }
                is UpdatesViewModel.UiState.ScanningWithProgress -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) { CircularProgressIndicator(progress = { s.done.toFloat() / s.total.coerceAtLeast(1) }); Text("已检测 ${s.done} / ${s.total}…", style = MaterialTheme.typography.bodyMedium) }
                is UpdatesViewModel.UiState.NoInstalledGithubApps -> { Text("未检测到已安装且受支持的 GitHub 开源应用。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("提示：可从「主页」安装 Aurora Store、NewPipe、OsmAnd 等应用后再来检测。", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                is UpdatesViewModel.UiState.Result -> { val candidates = s.candidates; val updatable = candidates.filter { it.hasUpdate }; val upToDate = candidates.filter { !it.hasUpdate }; Text("检测到 ${candidates.size} 个受支持应用 · 可更新 ${updatable.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp)); StaggeredLazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxWidth()) {
                    if (updatable.isNotEmpty()) { item { Text("可更新", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp)) }; items(updatable, key = { "u-${it.installed.packageName}" }) { StaggerItem { UpdateCandidateCard(candidate = it, onUpdate = { sheetCandidate = it }) } } }
                    if (upToDate.isNotEmpty()) { item { Text("已是最新", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) }; items(upToDate, key = { "utd-${it.installed.packageName}" }) { StaggerItem { UpdateCandidateCard(candidate = it, onUpdate = { sheetCandidate = it }, upToDate = true) } } }
                } }
            }
        }
    }

    sheetCandidate?.let { c -> InstallBottomSheet(app = c.appItem, release = c.latestRelease, onDismiss = { sheetCandidate = null }, onDownload = { asset -> dlVm.enqueue(asset) }, onOpenRepo = {}) }
}

@Composable
private fun DownloadTaskCard(task: DownloadViewModel.Task, onInstall: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(task.asset.name, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            when (val st = task.status) {
                is DownloadStatus.Queued -> Text("排队中…", style = MaterialTheme.typography.bodyMedium)
                is DownloadStatus.Progress -> { val frac = (st.bytes.toFloat() / st.total.coerceAtLeast(1)).coerceIn(0f, 1f); LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)); Text("${formatSize(st.bytes)} / ${formatSize(st.total)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                is DownloadStatus.Succeeded -> { Text("下载完成", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary); Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = onInstall) { Text("安装") } } }
                is DownloadStatus.Failed -> Text("下载失败：${st.reason}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
        }
    }
}

@Composable
private fun UpdateCandidateCard(candidate: UpdateCandidate, onUpdate: () -> Unit, upToDate: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(candidate.installed.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(candidate.appItem.fullName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) { Text("已安装：${candidate.installed.versionName ?: candidate.installed.versionCode}", style = MaterialTheme.typography.bodyMedium); Text("最新：${candidate.latestVersionTag ?: "未知"}", style = MaterialTheme.typography.bodyMedium, color = if (upToDate) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary) }
                if (upToDate) FilledTonalButton(onClick = onUpdate) { Text("查看") } else Button(onClick = onUpdate) { Text("更新") }
            }
        }
    }
}
