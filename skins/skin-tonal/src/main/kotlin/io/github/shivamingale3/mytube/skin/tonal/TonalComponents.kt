package io.github.shivamingale3.mytube.skin.tonal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import io.github.shivamingale3.mytube.model.Video
import kotlin.math.PI
import kotlin.math.sin

/** A focusable, clickable area with Tonal's focus: a 3 dp white ring and a slight lift. */
@Composable
internal fun TonalFocusable(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    focusedScale: Float = 1f,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(radius)
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            pressedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale, pressedScale = 0.97f),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, TonalColors.Text), shape = shape)),
        glow = ClickableSurfaceDefaults.glow(),
        interactionSource = interaction,
    ) {
        Box { content(focused) }
    }
}

/** Pill button: white with black text (primary), grey otherwise; inverts on focus. */
@Composable
internal fun TonalButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    leading: (@Composable (Color) -> Unit)? = null,
) {
    TonalFocusable(onClick = if (enabled) onClick else null, radius = 999.dp, modifier = modifier) { focused ->
        val bg = when {
            !enabled -> TonalColors.Chip
            primary || focused -> TonalColors.Text
            else -> TonalColors.Raised
        }
        val fg = if (enabled && (primary || focused)) Color.Black else if (enabled) TonalColors.Text else TonalColors.Dim
        Row(
            Modifier.height(height).background(bg, RoundedCornerShape(999.dp)).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            leading?.invoke(fg)
            Text(label, style = TonalType.Button.copy(fontSize = if (height < 48.dp) 15.sp else 17.sp), color = fg, maxLines = 1)
        }
    }
}

/** Outlined chip used for player and filter options. */
@Composable
internal fun TonalChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false, enabled: Boolean = true) {
    TonalFocusable(onClick = if (enabled) onClick else null, radius = 12.dp, modifier = modifier) { focused ->
        Box(
            Modifier.height(40.dp)
                .background(if (focused || selected) TonalColors.Text else Color.Transparent, RoundedCornerShape(12.dp))
                .border(1.dp, if (focused || selected) TonalColors.Text else TonalColors.Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = TonalType.Chip, color = if (focused || selected) Color.Black else if (enabled) TonalColors.Text else TonalColors.Dim, maxLines = 1)
        }
    }
}

@Composable
internal fun TonalThumbnail(url: String?, modifier: Modifier = Modifier, radius: Dp = 20.dp) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(radius)).background(TonalColors.Surface),
    )
}

@Composable
internal fun TonalAvatar(url: String?, size: Dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(CircleShape).background(TonalColors.Raised),
    )
}

/** A video card: rounded thumbnail with a duration badge and a one-line title. */
@Composable
internal fun TonalVideoCard(video: Video, width: Dp, onFocus: () -> Unit, modifier: Modifier = Modifier, onPlay: () -> Unit) {
    TonalFocusable(onClick = onPlay, radius = 20.dp, focusedScale = 1.05f, modifier = modifier) { focused ->
        if (focused) LaunchedEffect(video.id) { onFocus() }
        Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                TonalThumbnail(video.thumbnailUrl, Modifier.size(width, width * 9 / 16))
                video.badge()?.let { badge ->
                    Text(
                        badge,
                        style = TonalType.Meta.copy(fontFamily = TonalFonts.Display),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                            .background(if (video.isLive) TonalColors.Accent else Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                if (video.percentWatched in 1..100) {
                    Box(Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 6.dp).width(width - 20.dp).height(4.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))) {
                        Box(Modifier.width((width - 20.dp) * video.percentWatched / 100).height(4.dp).background(TonalColors.Accent, RoundedCornerShape(2.dp)))
                    }
                }
            }
            Text(
                video.title,
                style = TonalType.CardTitle,
                color = if (focused) TonalColors.Text else TonalColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

internal fun Video.badge(): String? {
    if (isLive) return "LIVE"
    if (durationMs <= 0) return null
    return formatDuration(durationMs)
}

internal fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Three accent dots bouncing in turn: Tonal's loading indicator. */
@Composable
internal fun TonalLoader(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loader")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "phase")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val lift = sin(((phase - i / 3f) * 2 * PI).toFloat()).coerceAtLeast(0f)
            Box(Modifier.padding(bottom = (lift * 10).dp).size(12.dp).background(if (i == 1) TonalColors.Text else TonalColors.Accent, CircleShape))
        }
    }
}

/**
 * The wavy progress bar: played part as an accent sine wave, the rest a flat grey line,
 * SponsorBlock segments in green and a white bar for the playhead.
 */
@Composable
internal fun WavyProgress(progress: Float, segments: List<Pair<Float, Float>>, modifier: Modifier = Modifier, animate: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "wave")
    val shift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "shift")
    val accent = TonalColors.Accent
    Canvas(modifier.height(20.dp)) {
        val stroke = 5.dp.toPx()
        val mid = size.height / 2
        val playedX = size.width * progress.coerceIn(0f, 1f)
        val wavelength = 12.dp.toPx() * 2
        val amplitude = 4.dp.toPx()
        drawLine(Color(0xFF3A3A3A), Offset(playedX + stroke, mid), Offset(size.width, mid), stroke, StrokeCap.Round)
        segments.forEach { (start, end) ->
            val sx = (size.width * start).coerceAtLeast(playedX)
            val ex = size.width * end
            if (ex > sx) drawLine(TonalColors.Sponsor, Offset(sx, mid), Offset(ex, mid), stroke)
        }
        if (playedX > 0f) {
            val path = Path()
            val phase = if (animate) shift * 2 * PI.toFloat() else 0f
            var x = 0f
            path.moveTo(0f, mid)
            while (x <= playedX) {
                path.lineTo(x, mid + amplitude * sin(x / wavelength * 2 * PI.toFloat() - phase))
                x += 2f
            }
            drawPath(path, accent, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        val w = 6.dp.toPx()
        drawRoundRect(Color.White, Offset(playedX - w / 2, 0f), Size(w, size.height), CornerRadius(w / 2))
    }
}
