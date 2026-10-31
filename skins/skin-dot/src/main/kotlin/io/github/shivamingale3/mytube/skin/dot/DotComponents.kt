package io.github.shivamingale3.mytube.skin.dot

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
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

/** Card lift on focus; together with the white ring it marks the focused card. */
internal const val CardFocusScale = 1.06f

/** A thumbnail under a fine dot screen, like a dot-matrix display; the screen clears on focus. */
@Composable
internal fun DotThumbnail(url: String?, focused: Boolean, modifier: Modifier = Modifier, radius: Dp = 18.dp) {
    Box(modifier.clip(RoundedCornerShape(radius)).background(DotColors.Surface)) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        if (!focused) Box(Modifier.matchParentSize().halftoneScreen())
    }
}

/** Dark overlay with round holes in a 4 dp grid, so the image shows through as dots. */
@Composable
internal fun Modifier.halftoneScreen(): Modifier {
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
internal fun DotAvatar(url: String?, size: Dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
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

/** Five dots with a red one sweeping across: Dot's loading indicator. */
@Composable
internal fun DotLoader(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loader")
    val step by transition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "step",
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(5) { i ->
            Box(Modifier.size(10.dp).background(if (i == step.toInt() % 5) DotColors.Accent else Color(0xFF3A3A3A), CircleShape))
        }
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
