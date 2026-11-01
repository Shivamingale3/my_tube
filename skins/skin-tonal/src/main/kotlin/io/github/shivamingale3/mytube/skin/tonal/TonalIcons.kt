package io.github.shivamingale3.mytube.skin.tonal

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** Small line icons drawn on a 24-unit grid, like the designs' SVGs. */
internal enum class Icon { Search, Mic, Play, Pause, Back10, Forward10, Back }

@Composable
internal fun TonalIcon(icon: Icon, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 24f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        val line = Stroke(width = 2.4f * u, cap = StrokeCap.Round)
        when (icon) {
            Icon.Search -> {
                drawCircle(color, 7 * u, p(11f, 11f), style = line)
                drawLine(color, p(16.5f, 16.5f), p(20f, 20f), 2.4f * u, StrokeCap.Round)
            }
            Icon.Mic -> {
                drawRoundRect(color, p(9f, 3f), Size(6 * u, 11 * u), CornerRadius(3 * u), style = line)
                drawArc(color, 0f, 180f, false, p(5f, 4f), Size(14 * u, 14 * u), style = line)
                drawLine(color, p(12f, 18f), p(12f, 21f), 2.4f * u, StrokeCap.Round)
            }
            Icon.Play -> drawPath(Path().apply { moveTo(6 * u, 3 * u); lineTo(21 * u, 12 * u); lineTo(6 * u, 21 * u); close() }, color)
            Icon.Pause -> {
                drawRoundRect(color, p(6f, 4f), Size(4 * u, 16 * u), CornerRadius(1.5f * u))
                drawRoundRect(color, p(14f, 4f), Size(4 * u, 16 * u), CornerRadius(1.5f * u))
            }
            Icon.Back10 -> {
                drawLine(color, p(6f, 5f), p(6f, 19f), 2f * u, StrokeCap.Round)
                drawPath(Path().apply { moveTo(19 * u, 5 * u); lineTo(9 * u, 12 * u); lineTo(19 * u, 19 * u); close() }, color)
            }
            Icon.Forward10 -> {
                drawLine(color, p(18f, 5f), p(18f, 19f), 2f * u, StrokeCap.Round)
                drawPath(Path().apply { moveTo(5 * u, 5 * u); lineTo(15 * u, 12 * u); lineTo(5 * u, 19 * u); close() }, color)
            }
            Icon.Back -> {
                drawLine(color, p(19f, 12f), p(5f, 12f), 2.4f * u, StrokeCap.Round)
                drawPath(Path().apply { moveTo(11 * u, 6 * u); lineTo(5 * u, 12 * u); lineTo(11 * u, 18 * u) }, color, style = line)
            }
        }
    }
}
