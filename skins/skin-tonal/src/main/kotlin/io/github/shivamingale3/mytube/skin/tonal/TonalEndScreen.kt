package io.github.shivamingale3.mytube.skin.tonal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import io.github.shivamingale3.mytube.screens.PlayerUiState
import io.github.shivamingale3.mytube.skin.PlayerActions

/**
 * Shown when a video ends: "Up next" with a countdown that starts it on its own, Replay, and two
 * rows of related videos. Back leaves the player.
 */
@Composable
internal fun BoxScope.TonalEndScreen(state: PlayerUiState, actions: PlayerActions) {
    val first = remember { FocusRequester() }
    LaunchedEffect(state.upNext?.id) { runCatching { first.requestFocus() } }

    AsyncImage(model = state.video?.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.85f)))

    Column(Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        val next = state.upNext
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            if (next != null) {
                TonalFocusable(onClick = { actions.play(next) }, radius = 24.dp, focusedScale = 1.04f, modifier = Modifier.focusRequester(first)) { _ ->
                    TonalThumbnail(next.thumbnailUrl, Modifier.size(400.dp, 225.dp), radius = 24.dp)
                }
            }
            Column(Modifier.width(600.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.countdown?.let {
                        Box(Modifier.size(44.dp).background(TonalColors.Accent, CircleShape), contentAlignment = Alignment.Center) {
                            Text("$it", style = TonalType.Title, color = TonalColors.OnAccent)
                        }
                    }
                    Text(if (next != null) "Up next" else "That's the end", style = TonalType.Section, color = TonalColors.Muted)
                }
                if (next != null) {
                    Text(next.title, style = TonalType.Hero.copy(fontSize = 30.sp, lineHeight = 36.sp), color = TonalColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    next.channelName?.let { Text(it, style = TonalType.Body, color = TonalColors.Muted) }
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (next != null) TonalButton("Play now", onClick = { actions.play(next) }, primary = true, height = 48.dp)
                    TonalButton("Replay", onClick = actions.replay, height = 48.dp, modifier = if (next == null) Modifier.focusRequester(first) else Modifier)
                }
            }
        }
        if (state.related.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Related", style = TonalType.Section, color = TonalColors.Text)
                state.related.take(8).chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { video -> TonalVideoCard(video, width = 240.dp, onFocus = {}) { actions.play(video) } }
                    }
                }
            }
        }
    }
}

/** The "Up next" shelf under the player controls. */
@Composable
internal fun TonalUpNextRow(state: PlayerUiState, actions: PlayerActions) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    val videos = listOfNotNull(state.upNext) + state.related
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Up next", style = TonalType.Title, color = TonalColors.Text)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(videos, key = { it.id }) { video ->
                TonalVideoCard(video, width = 220.dp, onFocus = {}, modifier = if (video == videos.first()) Modifier.focusRequester(first) else Modifier) { actions.play(video) }
            }
        }
    }
}
