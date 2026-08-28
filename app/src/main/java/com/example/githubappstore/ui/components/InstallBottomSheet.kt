package com.example.githubappstore.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.githubappstore.data.model.GhRelease
import com.example.githubappstore.domain.AppItem

/** Bottom sheet: app info + latest release APK assets + download/open-repo actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallBottomSheet(
    app: AppItem, release: GhRelease?, releaseError: String? = null, onDismiss: () -> Unit,
    onDownload: (com.example.githubappstore.data.model.GhAsset) -> Unit, onOpenRepo: () -> Unit,
    onRetryRelease: () -> Unit = {},
    sheetState: androidx.compose.material3.SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(app.displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(app.fullName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (app.description.isNotBlank()) Text(app.description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
            if (release == null) {
                if (releaseError != null) {
                    Text("获取发布信息失败：$releaseError", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetryRelease, modifier = Modifier.padding(top = 10.dp)) { Text("重试") }
                } else {
                    Text("加载发布信息中…", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text("最新发布 · ${release.tagName}", style = MaterialTheme.typography.titleLarge)
                release.publishedAt?.let { Text(it.take(10), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                val apks = release.assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
                if (apks.isEmpty()) Text("该版本未提供 Android APK 资产", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                else LazyColumn(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(apks) { asset -> ApkAssetRow(asset = asset, onDownload = { onDownload(asset) }) }
                }
            }
            Row(modifier = Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onOpenRepo, modifier = Modifier.weight(1f)) { Text("打开仓库") }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("关闭") }
            }
        }
    }
}

@Composable
private fun ApkAssetRow(asset: com.example.githubappstore.data.model.GhAsset, onDownload: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(asset.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(formatSize(asset.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onDownload) { Text("下载") }
    }
}

internal fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "未知大小"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
}
