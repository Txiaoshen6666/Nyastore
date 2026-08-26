package com.example.githubappstore.ui.components
import androidx.compose.ui.unit.dp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * [LazyColumn] whose first-viewport items play a staggered fade+slide-in on
 * first composition (Material 3 "fade through"-style entrance). Items already
 * off-screen at first frame are revealed normally to avoid paying the animation
 * cost for the whole list. Reuses the standard [LazyListScope] DSL so callers
 * use it exactly like [LazyColumn].
 */
@Composable
fun StaggeredLazyColumn(
    modifier: Modifier = Modifier, state: LazyListState = rememberLazyListState(),
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
    verticalArrangement: androidx.compose.foundation.layout.Arrangement.Vertical = androidx.compose.foundation.layout.Arrangement.Top,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(modifier = modifier, state = state, contentPadding = contentPadding, verticalArrangement = verticalArrangement, content = content)
}

/** Wraps a single list item with an entrance fade+slide-in animation. */
@Composable
fun StaggerItem(content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(initialAlpha = 0.35f) + slideInVertically(initialOffsetY = { it / 6 })
    ) { content() }
}
