package io.github.shivamingale3.mytube.skin.dot

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import coil3.compose.AsyncImage

/** Thumbnails stay grey until focused; Dot's signature move. */
private val GreyFilter = ColorFilter.colorMatrix(
    ColorMatrix().apply {
        setToSaturation(0f)
        timesAssign(ColorMatrix(floatArrayOf(0.8f, 0f, 0f, 0f, 0f, 0f, 0.8f, 0f, 0f, 0f, 0f, 0f, 0.8f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
    },
)

/**
 * A focusable, clickable area with Dot's focus treatment: a thin white ring, no scaling or glow.
 * [content] receives whether it is focused.
 */
@Composable
internal fun DotFocusable(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    radius: Dp = 18.dp,
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
        scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale, pressedScale = 0.98f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, DotColors.Focus), shape = shape),
        ),
        glow = ClickableSurfaceDefaults.glow(),
        interactionSource = interaction,
    ) {
        Box { content(focused) }
    }
}

/** How thumbnails look when not focused. Under review: the grey look didn't work on a real TV. */
internal enum class ThumbStyle { Grey, Lift, Dim, Halftone }

internal val LocalThumbStyle = staticCompositionLocalOf { ThumbStyle.Grey }

@Composable
internal fun DotThumbnail(url: String?, focused: Boolean, modifier: Modifier = Modifier, radius: Dp = 18.dp) {
    val style = LocalThumbStyle.current
    val shape = RoundedCornerShape(radius)
    Box(modifier.clip(shape).background(DotColors.Surface)) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = if (style == ThumbStyle.Grey && !focused) GreyFilter else null,
            modifier = Modifier.matchParentSize(),
        )
        if (!focused) {
            when (style) {
                ThumbStyle.Dim -> Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.6f)))
                ThumbStyle.Halftone -> Box(Modifier.matchParentSize().halftone())
                else -> Unit
            }
        }
    }
}

/** A fine screen of dark dots over the image, like a dot-matrix display; cleared on focus. */
@Composable
private fun Modifier.halftone(): Modifier {
    val density = LocalDensity.current
    val brush = remember(density) {
        val step = with(density) { 4.dp.toPx() }.coerceAtLeast(3f)
        val size = step.toInt().coerceAtLeast(3)
        val tile = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(tile)
        canvas.drawColor(android.graphics.Color.argb(150, 0, 0, 0))
        val clear = android.graphics.Paint().apply {
            isAntiAlias = true
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        }
        canvas.drawCircle(size / 2f, size / 2f, size * 0.36f, clear)
        ShaderBrush(ImageShader(tile.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
    return background(brush)
}

@Composable
internal fun DotAvatar(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    inColor: Boolean = LocalThumbStyle.current != ThumbStyle.Grey,
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        colorFilter = if (inColor) null else GreyFilter,
        modifier = modifier.size(size).clip(CircleShape).background(DotColors.Track),
    )
}

/** A filled play triangle, drawn rather than typed so no font can turn it into an emoji. */
@Composable
internal fun PlayGlyph(modifier: Modifier = Modifier, color: Color = DotColors.Text) {
    androidx.compose.foundation.Canvas(modifier) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.18f, 0f)
            lineTo(size.width, size.height / 2)
            lineTo(size.width * 0.18f, size.height)
            close()
        }
        drawPath(path, color)
    }
}

/** Progress as a row of dots: watched white, the current position in the accent colour. */
@Composable
internal fun DotProgress(percent: Int, modifier: Modifier = Modifier, count: Int = 32) {
    val filled = (percent.coerceIn(0, 100) * count) / 100
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(count) { i ->
            val color = when {
                i < filled -> DotColors.Text
                i == filled -> DotColors.Accent
                else -> DotColors.Track
            }
            Box(Modifier.size(6.dp).background(color, CircleShape))
        }
    }
}

/** The faint dot grid behind every Dot screen, drawn from one repeated tile. */
@Composable
internal fun Modifier.dotGrid(): Modifier {
    val density = LocalDensity.current
    val brush = remember(density) {
        val step = with(density) { 16.dp.toPx() }.coerceAtLeast(2f)
        val tile = ImageBitmap(step.toInt(), step.toInt())
        Canvas(tile).drawCircle(Offset(step / 2, step / 2), with(density) { 1.dp.toPx() }, Paint().apply { color = DotColors.GridDot })
        ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    }
    return this.background(DotColors.Background).background(brush)
}
