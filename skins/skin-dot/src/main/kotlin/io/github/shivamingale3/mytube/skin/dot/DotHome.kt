package io.github.shivamingale3.mytube.skin.dot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.shivamingale3.mytube.model.ChannelSummary
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.model.VideoRow
import io.github.shivamingale3.mytube.screens.HomeUiState
import io.github.shivamingale3.mytube.skin.HomeActions

private val CardShape = RoundedCornerShape(28.dp)

@Composable
internal fun DotHome(state: HomeUiState, actions: HomeActions) {
    when {
        state.loading && state.featured == null -> Centered("LOADING", DotColors.Text)
        state.error != null && state.featured == null -> Centered("CAN'T LOAD HOME\n${state.error}", DotColors.Accent)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item(key = "top") { TopCards(state, actions) }
            itemsIndexed(state.rows, key = { i, row -> "$i:${row.title}" }) { index, row ->
                VideoRowSection(row, label = if (index == 0) "UP NEXT" else row.title.uppercase(), onFocus = actions.focus, onPlay = actions.play)
            }
        }
    }
}

@Composable
private fun Centered(text: String, color: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = DotType.Wordmark, color = color)
    }
}

@Composable
private fun TopCards(state: HomeUiState, actions: HomeActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        state.featured?.let { Featured(it) { actions.play(it) } }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ContinueCard(state, actions)
            NewUploadsCard(state, actions)
        }
    }
}

@Composable
private fun Featured(video: Video, onPlay: () -> Unit) {
    DotFocusable(onClick = onPlay, radius = 28.dp, modifier = Modifier.size(760.dp, 360.dp)) { _ ->
        DotThumbnail(video.thumbnailUrl, focused = true, radius = 28.dp, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.9f)),
                CardShape,
            ),
        )
        Row(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(28.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    listOfNotNull(video.channelName?.uppercase(), video.durationLabel()).joinToString(" · "),
                    style = DotType.Label,
                    color = Color(0xFFBDBDBD),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(video.title, style = DotType.Hero, color = DotColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(64.dp).background(DotColors.Accent, CircleShape), contentAlignment = Alignment.Center) {
                PlayGlyph(Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun SideCard(onClick: (() -> Unit)?, content: @Composable (focused: Boolean) -> Unit) {
    DotFocusable(onClick = onClick, radius = 28.dp, modifier = Modifier.size(408.dp, 172.dp)) { focused ->
        Box(
            Modifier.fillMaxSize()
                .background(DotColors.Surface, CardShape)
                .border(1.dp, DotColors.SurfaceBorder, CardShape)
                .padding(20.dp),
        ) { content(focused) }
    }
}

@Composable
private fun ContinueCard(state: HomeUiState, actions: HomeActions) {
    val video = state.continueWatching
    SideCard(onClick = if (video != null) ({ actions.play(video) }) else if (!state.personalized) actions.openAccount else null) { focused ->
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("CONTINUE", style = DotType.Label, color = DotColors.TextDim)
            if (video == null) {
                Text(
                    if (state.personalized) "Nothing to continue yet." else "Sign in to pick up where you left off.",
                    style = DotType.Title,
                    color = DotColors.TextMuted,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    DotThumbnail(video.thumbnailUrl, focused = focused, radius = 12.dp, modifier = Modifier.size(128.dp, 72.dp))
                    Text(video.title, style = DotType.Title, color = DotColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DotProgress(video.percentWatched, Modifier.weight(1f), count = 22)
                    video.minutesLeft()?.let { Text("$it MIN LEFT", style = DotType.Meta, color = Color(0xFFBDBDBD)) }
                }
            }
        }
    }
}

@Composable
private fun NewUploadsCard(state: HomeUiState, actions: HomeActions) {
    val channels = state.channels
    SideCard(onClick = if (channels.isNotEmpty()) actions.openSubscriptions else if (!state.personalized) actions.openAccount else null) { _ ->
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text("NEW UPLOADS", style = DotType.Label, color = DotColors.TextDim)
                if (channels.isNotEmpty()) {
                    Text("%02d".format(state.newUploadCount.coerceAtMost(99)), style = DotType.MatrixNumber, color = DotColors.Accent)
                }
            }
            if (channels.isEmpty()) {
                Text(
                    if (state.personalized) "No subscriptions yet." else "Sign in to see new uploads from your channels.",
                    style = DotType.Title,
                    color = DotColors.TextMuted,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    channels.take(5).forEach { ChannelDot(it) }
                }
            }
        }
    }
}

@Composable
private fun ChannelDot(channel: ChannelSummary) {
    Box {
        DotAvatar(channel.avatarUrl, size = 60.dp)
        if (channel.hasNewContent) {
            Box(
                Modifier.align(Alignment.TopEnd).size(12.dp)
                    .border(3.dp, DotColors.Surface, CircleShape)
                    .background(DotColors.Accent, CircleShape),
            )
        }
    }
}

@Composable
private fun VideoRowSection(row: VideoRow, label: String, onFocus: (Video) -> Unit, onPlay: (Video) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "$label — ${row.videos.size.toString().padStart(2, '0')}",
            style = DotType.Label,
            color = DotColors.TextDim,
            maxLines = 1,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(row.videos, key = { it.id }) { video -> VideoCard(video, onFocus = { onFocus(video) }) { onPlay(video) } }
        }
    }
}

@Composable
private fun VideoCard(video: Video, onFocus: () -> Unit, onPlay: () -> Unit) {
    DotFocusable(onClick = onPlay, radius = 18.dp, focusedScale = CardFocusScale) { focused ->
        if (focused) LaunchedEffect(video.id) { onFocus() }
        VideoCardContent(video, focused)
    }
}

@Composable
internal fun VideoCardContent(video: Video, focused: Boolean) {
    Column(
        Modifier.width(224.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DotThumbnail(video.thumbnailUrl, focused = focused, modifier = Modifier.size(224.dp, 126.dp))
        Text(
            video.title,
            style = DotType.Card,
            color = if (focused) DotColors.Text else DotColors.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

private fun Video.durationLabel(): String? {
    if (isLive) return "LIVE"
    if (durationMs <= 0) return null
    val totalSeconds = durationMs / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun Video.minutesLeft(): Long? {
    if (durationMs <= 0) return null
    return ((durationMs * (100 - percentWatched) / 100) / 60_000).coerceAtLeast(1)
}
