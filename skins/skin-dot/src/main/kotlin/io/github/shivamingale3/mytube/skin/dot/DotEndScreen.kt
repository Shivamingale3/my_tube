package io.github.shivamingale3.mytube.skin.dot

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
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.screens.PlayerUiState
import io.github.shivamingale3.mytube.skin.PlayerActions

/**
 * Shown when a video ends: "Up next" with a countdown that starts it on its own, Replay, and two
 * rows of related videos. Back leaves the player.
 */
@Composable
internal fun BoxScope.DotEndScreen(state: PlayerUiState, actions: PlayerActions) {
    val first = remember { FocusRequester() }
    LaunchedEffect(state.upNext?.id) { runCatching { first.requestFocus() } }

    AsyncImage(model = state.video?.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.82f)).halftoneScreen())

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        val next = state.upNext
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            if (next != null) {
                DotFocusable(onClick = { actions.play(next) }, radius = 22.dp, focusedScale = CardFocusScale, modifier = Modifier.focusRequester(first)) { focused ->
                    Box {
                        DotThumbnail(next.thumbnailUrl, focused = focused, radius = 22.dp, modifier = Modifier.size(400.dp, 225.dp))
                        next.durationLabel()?.let {
                            Text(
                                it,
                                style = DotType.Meta,
                                color = DotColors.Text,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).background(Color.Black.copy(alpha = 0.75f)).padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            Column(Modifier.width(560.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (next != null) "UP NEXT" else "THAT'S IT", style = DotType.Label.copy(fontSize = 14.sp), color = DotColors.TextDim)
                    state.countdown?.let { Text("%02d".format(it), style = DotType.MatrixNumber.copy(fontSize = 44.sp, lineHeight = 40.sp), color = DotColors.Accent) }
                }
                if (next != null) {
                    Text(next.title, style = DotType.Hero, color = DotColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    next.channelName?.let { Text(it.uppercase(), style = DotType.Label, color = DotColors.TextMuted) }
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (next != null) Pill("PLAY NOW", onClick = { actions.play(next) }, filled = true)
                    Pill("REPLAY", onClick = actions.replay, modifier = if (next == null) Modifier.focusRequester(first) else Modifier)
                }
            }
        }
        if (state.related.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("RELATED", style = DotType.Label, color = DotColors.TextDim)
                state.related.take(8).chunked(4).forEach { row -> RelatedRow(row, actions) }
            }
        }
    }
}

@Composable
private fun RelatedRow(videos: List<Video>, actions: PlayerActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        videos.forEach { video -> VideoCard(video, onFocus = {}, width = 240.dp) { actions.play(video) } }
    }
}

/** The "Up next" shelf under the player controls. */
@Composable
internal fun UpNextRow(state: PlayerUiState, actions: PlayerActions) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    val videos = listOfNotNull(state.upNext) + state.related
    Column(Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("UP NEXT", style = DotType.Label, color = DotColors.TextDim)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(videos, key = { it.id }) { video ->
                VideoCard(video, onFocus = {}, width = 224.dp, modifier = if (video == videos.first()) Modifier.focusRequester(first) else Modifier) { actions.play(video) }
            }
        }
    }
}
