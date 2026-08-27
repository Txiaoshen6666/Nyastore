package com.example.githubappstore.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.githubappstore.GitHubAppStoreApp
import com.example.githubappstore.data.settings.AppSettings.Companion.DEFAULT_MIRROR_HOST
import com.example.githubappstore.data.settings.AppSettings.Companion.MAX_THREADS
import com.example.githubappstore.data.settings.AppSettings.Companion.MIN_THREADS
import kotlinx.coroutines.launch

/**
 * Settings route. Sections: 镜像反代 / 多线程下载 / API release 回退 / 深色纯黑背景 /
 * 动态配色 / GitHub Token / 关于；"我的（GitHub 账户）"作为子项内嵌于底部。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute() {
    val app = GitHubAppStoreApp.container; val settings = app.settings; val scope = rememberCoroutineScope()
    val mirrorOn by settings.mirrorEnabled.collectAsState(initial = true)
    val mirrorHost by settings.mirrorHost.collectAsState(initial = DEFAULT_MIRROR_HOST)
    val multiOn by settings.multiThreadDownload.collectAsState(initial = false)
    val threadCount by settings.downloadThreadCount.collectAsState(initial = 4)
    val apiReleaseOn by settings.useApiForRelease.collectAsState(initial = false)
    val dynamic by settings.dynamicColor.collectAsState(initial = true)
    val pureBlack by settings.pureBlackDarkMode.collectAsState(initial = false)
    val token by settings.githubToken.collectAsState(initial = "")

    val presets = remember { listOf("https://ghfast.top", "https://ghproxy.com", "https://ghproxy.net", "https://ghproxy.homeboyc.cn") }
    var hostExpanded by remember { mutableStateOf(false) }
    var customHost by remember { mutableStateOf(mirrorHost) }
    var showCustomHost by remember { mutableStateOf(false) }
    var showThreadPicker by remember { mutableStateOf(false) }
    var sliderCount by remember { mutableStateOf(threadCount.coerceIn(MIN_THREADS, MAX_THREADS)) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("镜像反代", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("为 Release 下载链接添加镜像前缀以加速。默认启用 ghfast.top；可选择或自定义。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                    Column(modifier = Modifier.weight(1f)) { Text("启用镜像", style = MaterialTheme.typography.titleMedium); Text(if (mirrorOn) "当前：${mirrorHost.ifBlank { DEFAULT_MIRROR_HOST }}" else "已关闭，将直连下载", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(checked = mirrorOn, onCheckedChange = { scope.launch { settings.setMirrorEnabled(it) } })
                }
                if (mirrorOn) ExposedDropdownMenuBox(expanded = hostExpanded, onExpandedChange = { hostExpanded = it }, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(value = mirrorHost.ifBlank { DEFAULT_MIRROR_HOST }, onValueChange = {}, readOnly = true, label = { Text("镜像主机") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hostExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth())
                    ExposedDropdownMenu(expanded = hostExpanded, onDismissRequest = { hostExpanded = false }) { presets.forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { scope.launch { settings.setMirrorHost(p) }; hostExpanded = false }) }; DropdownMenuItem(text = { Text("自定义…") }, onClick = { hostExpanded = false; showCustomHost = true }) }
                }
            }
        }
        Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("多线程下载", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("开启后按线程数分块并发下载再合并，可提升大文件速度；默认使用系统单连接下载。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) { Column(modifier = Modifier.weight(1f)) { Text("启用多线程下载", style = MaterialTheme.typography.titleMedium); Text("下载时按线程数分块并发", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked = multiOn, onCheckedChange = { scope.launch { settings.setMultiThreadDownload(it) } }) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) { Column(modifier = Modifier.weight(1f)) { Text("下载线程数", style = MaterialTheme.typography.titleMedium); Text("当前：$threadCount（范围 $MIN_THREADS–$MAX_THREADS）", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; TextButton(onClick = { sliderCount = threadCount.coerceIn(MIN_THREADS, MAX_THREADS); showThreadPicker = true }) { Text("调整") } }
            }
        }
        Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) { Column(modifier = Modifier.weight(1f)) { Text("通过 API 获取 release 链接", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Release 页面/资产 CDN 访问失败时，改由 GitHub REST API 获取资产直链（可配合镜像）后下载。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked = apiReleaseOn, onCheckedChange = { scope.launch { settings.setUseApiForRelease(it) } }) }
        }
        Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) { Column(modifier = Modifier.weight(1f)) { Text("深色模式使用纯黑背景", style = MaterialTheme.typography.titleLarge); Text("开启后深色主题背景/表面使用 #FF000000（OLED 省电）；关闭则使用标准 Material 深色灰。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked = pureBlack, onCheckedChange = { scope.launch { settings.setPureBlackDarkMode(it) } }) }
        }
        Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) { Column(modifier = Modifier.weight(1f)) { Text("动态配色（Material You）", style = MaterialTheme.typography.titleLarge); Text("Android 12+ 从壁纸提取配色，重启生效。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked = dynamic, onCheckedChange = { scope.launch { settings.setDynamicColor(it) } }) }
        }
        Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column(modifier = Modifier.padding(16.dp)) { Text("GitHub Token（可选）", style = MaterialTheme.typography.titleLarge); Text("提高 API 限流额度，仅本地保存。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedTextField(value = token, onValueChange = { scope.launch { settings.setGithubToken(it) } }, label = { Text("Bearer token") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
        }
        Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column(modifier = Modifier.padding(16.dp)) { Text("关于", style = MaterialTheme.typography.titleLarge); Text("数据来自 GitHub 公开 API；本应用不托管任何二进制文件。镜像站为社区公益代理，可用性可能变化。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("本地 Room 缓存（1 小时 TTL）减少 API 限流并支持离线浏览；更新检测合并 F-Droid 索引以覆盖更多已装开源应用；版本比较采用 semver4j（正确处理 1.10 > 1.9 等语义化版本）。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) }
        }
        AccountSection(modifier = Modifier.padding(top = 14.dp))
    }

    if (showCustomHost) AlertDialog(onDismissRequest = { showCustomHost = false }, title = { Text("自定义镜像主机") }, text = { Column { Text("请输入镜像主机地址（含 https://，不含末尾斜杠），例如 https://my.mirror.com", style = MaterialTheme.typography.bodyMedium); OutlinedTextField(value = customHost, onValueChange = { customHost = it }, label = { Text("主机地址") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) } }, confirmButton = { TextButton(onClick = { scope.launch { settings.setMirrorHost(customHost.ifBlank { DEFAULT_MIRROR_HOST }) }; showCustomHost = false }) { Text("保存") } }, dismissButton = { TextButton(onClick = { showCustomHost = false }) { Text("取消") } })
    if (showThreadPicker) AlertDialog(onDismissRequest = { showThreadPicker = false }, title = { Text("选择下载线程数") }, text = { Column { Text("线程数：$sliderCount", style = MaterialTheme.typography.bodyLarge); Slider(value = sliderCount.toFloat(), onValueChange = { sliderCount = it.toInt().coerceIn(MIN_THREADS, MAX_THREADS) }, valueRange = MIN_THREADS.toFloat()..MAX_THREADS.toFloat(), steps = MAX_THREADS - MIN_THREADS - 1); Text("建议 3–5；过多线程可能触发服务端限流。", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, confirmButton = { TextButton(onClick = { scope.launch { settings.setDownloadThreadCount(sliderCount) }; showThreadPicker = false }) { Text("保存") } }, dismissButton = { TextButton(onClick = { showThreadPicker = false }) { Text("取消") } })
}
