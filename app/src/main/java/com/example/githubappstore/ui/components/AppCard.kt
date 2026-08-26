package com.example.githubappstore.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.githubappstore.domain.AppItem

/** M3 Expressive app card: 20dp corners, repo avatar + name + stars + language. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCard(app: AppItem, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AsyncImage(model = app.avatarUrl, contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.displayName, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.fullName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StarBadge(stars = app.stars)
                    app.language?.let { LanguageBadge(name = it) }
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun StarBadge(stars: Int) {
    AssistChip(onClick = {}, enabled = false, label = { Text(formatStars(stars), style = MaterialTheme.typography.labelLarge) },
        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = AssistChipDefaults.assistChipColors(disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer, disabledLeadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer),
        border = null, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun LanguageBadge(name: String) {
    Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, shape = RoundedCornerShape(16.dp), colors = FilterChipDefaults.filterChipColors())
}

internal fun formatStars(n: Int): String = when { n >= 1000 -> "${"%.1f".format(n / 1000.0)}k" else -> n.toString() }
