package io.github.shivamingale3.mytube.skin.tonal

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.model.VideoRow
import io.github.shivamingale3.mytube.screens.HomeUiState
import io.github.shivamingale3.mytube.skin.HomeActions

private val HeroShape = RoundedCornerShape(32.dp)

/** Tonal's Home: a big rounded hero for the featured video, then rows of rounded cards. */
@Composable
internal fun TonalHome(state: HomeUiState, actions: HomeActions) {
    when {
        state.loading && state.featured == null -> TonalHomeSkeleton()
        state.error != null && state.featured == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Can't load Home\n${state.error}", style = TonalType.Section, color = TonalColors.Muted)
        }
        else -> LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 64.dp, end = 64.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            state.featured?.let { featured -> item(key = "hero") { Hero(featured, actions) } }
            if (!state.personalized) item(key = "signin") { SignInNudge(actions) }
            state.continueWatching?.let { video ->
                item(key = "continue") { VideoRowSection(VideoRow("Continue watching", listOf(video)), actions) }
            }
            itemsIndexed(state.rows, key = { i, row -> "$i:${row.title}" }) { index, row ->
                VideoRowSection(if (index == 0) row.copy(title = "Top picks for you") else row, actions)
            }
        }
    }
}

@Composable
private fun Hero(video: Video, actions: HomeActions) {
    Box(Modifier.fillMaxWidth().height(368.dp).clip(HeroShape).background(TonalColors.Surface)) {
        TonalThumbnail(video.thumbnailUrl, Modifier.align(Alignment.CenterEnd).width(760.dp).fillMaxHeight(), radius = 0.dp)
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0.36f to TonalColors.Surface, 0.52f to TonalColors.Surface.copy(alpha = 0.75f), 0.74f to Color.Transparent),
            ),
        )
        Column(Modifier.padding(44.dp).width(500.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            video.channelName?.let {
                Text(
                    it,
                    style = TonalType.Chip,
                    color = TonalColors.Text,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(999.dp)).padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
            Text(video.title, style = TonalType.Hero, color = TonalColors.Text, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                video.badge()?.let { badge ->
                    Text(
                        badge,
                        style = TonalType.Meta.copy(fontFamily = TonalFonts.Display),
                        color = TonalColors.OnAccent,
                        modifier = Modifier.background(TonalColors.Accent, RoundedCornerShape(4.dp)).padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                video.subtitle?.let { Text(it, style = TonalType.Body, color = TonalColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            TonalButton("Watch", onClick = { actions.play(video) }, primary = true, modifier = Modifier.padding(top = 6.dp), leading = { c ->
                TonalIcon(Icon.Play, c, Modifier.size(16.dp))
            })
        }
    }
}

@Composable
private fun SignInNudge(actions: HomeActions) {
    Row(
        Modifier.fillMaxWidth().background(TonalColors.Surface, RoundedCornerShape(28.dp)).padding(horizontal = 28.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Make it yours", style = TonalType.Title, color = TonalColors.Text)
            Text("Sign in for your recommendations, history and resume.", style = TonalType.Body, color = TonalColors.Muted)
        }
        TonalButton("Sign in", onClick = actions.openSettings, height = 44.dp)
    }
}

@Composable
private fun VideoRowSection(row: VideoRow, actions: HomeActions) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(row.title, style = TonalType.Section, color = TonalColors.Text, maxLines = 1)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(row.videos, key = { it.id }) { video ->
                TonalVideoCard(video, width = 280.dp, onFocus = { actions.focus(video) }) { actions.play(video) }
            }
        }
    }
}

/** Home's layout in grey while the feed loads; one shared pulse keeps it cheap. */
@Composable
private fun TonalHomeSkeleton() {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    val block = Modifier.background(SkeletonGrey, RoundedCornerShape(20.dp))
    Column(
        Modifier.fillMaxSize().padding(horizontal = 64.dp).graphicsLayer { alpha = pulse },
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(368.dp).background(SkeletonGrey, HeroShape))
        Box(Modifier.size(200.dp, 20.dp).then(block))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            repeat(5) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(280.dp, 158.dp).then(block))
                    Box(Modifier.size(210.dp, 15.dp).then(block))
                }
            }
        }
    }
}

private val SkeletonGrey = Color(0xFF1C1C1C)
