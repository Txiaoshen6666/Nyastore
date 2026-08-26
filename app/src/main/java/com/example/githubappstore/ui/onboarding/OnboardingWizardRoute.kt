package com.example.githubappstore.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.githubappstore.GitHubAppStoreApp
import kotlinx.coroutines.launch

/**
 * First-run onboarding wizard (minSdk 33). Introduces the project and requests
 * the two relevant runtime permissions: notifications (POST_NOTIFICATIONS) and
 * install-unknown-apps (via ACTION_MANAGE_UNKNOWN_APP_SOURCES for this package).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWizardRoute(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    var notifGranted by remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> notifGranted = ok }

    fun openInstallUnknownSourcesSettings() { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply { data = android.net.Uri.parse("package:${context.packageName}"); addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }) }
    fun finish() { scope.launch { GitHubAppStoreApp.container.settings.setWizardCompleted(true); onFinished() } }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 36.dp, bottom = 20.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page -> WizardPageView(page = PAGES[page]) }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) { repeat(PAGES.size) { i -> val sel = pagerState.currentPage == i; Spacer(Modifier.size(if (sel) 10.dp else 7.dp).background(color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), shape = CircleShape)); if (i < PAGES.size - 1) Spacer(Modifier.size(8.dp)) } }
        HorizontalDivider()
        if (pagerState.currentPage == PERMS_PAGE_INDEX) {
            Column(modifier = Modifier.padding(vertical = 14.dp)) {
                if (!notifGranted) { Text("通知权限（可选）：允许后在下载完成或检测到更新时收到通知。", style = MaterialTheme.typography.bodyMedium); Button(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) { Text("授予通知权限") } }
                else Text("通知权限已就绪。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text("安装未知来源应用：下载的 APK 需要此权限才能调起系统安装器。点击按钮前往设置开启（按本应用包名）。", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { openInstallUnknownSourcesSettings() }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) { Text("前往设置开启安装权限") }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { scope.launch { GitHubAppStoreApp.container.settings.setWizardCompleted(true); onFinished() } }, enabled = pagerState.currentPage < PAGES.size - 1) { Text("跳过") }
            Button(onClick = { if (pagerState.currentPage < PAGES.size - 1) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } else finish() }) { Text(if (pagerState.currentPage < PAGES.size - 1) "下一步" else "开始使用") }
        }
    }
}

@Composable
private fun WizardPageView(page: WizardPage) { Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text(page.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center); Spacer(Modifier.height(18.dp)); Text(page.body, style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

private const val PERMS_PAGE_INDEX = 2
private data class WizardPage(val title: String, val body: String)
private val PAGES = listOf(
    WizardPage("欢迎使用 GitStore", "GitStore 是一个聚合 GitHub 上优质开源 Android 应用的商店。它从 GitHub 公开 API 拉取数据，帮你发现、下载并管理开源 App 的更新——本应用不托管任何二进制文件。"),
    WizardPage("核心能力", "• 主页：推荐你 Star 的 Android 应用和高星热门项目，支持搜索与分类浏览。\n• 下载与更新：多线程/镜像加速下载，自动检测已装开源 App 并提供更新。\n• 设置：镜像反代、多线程下载、深色纯黑背景、动态配色、GitHub 登录等。"),
    WizardPage("权限说明", "为提供完整体验，本应用需要以下权限：\n• 网络（INTERNET）：访问 GitHub API 与下载 APK。\n• 通知（可选）：下载完成/更新提醒。\n• 安装未知来源应用：调起系统安装器安装下载的 APK。\n点击「开始使用」即表示你了解并同意上述用途，可随时在系统设置中调整。"),
    WizardPage("准备好了", "现在开始探索开源 Android 应用吧！你可以随时在「设置 → 关于」中查看本介绍，或在系统设置中管理已授予的权限。")
)
