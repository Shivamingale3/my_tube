package io.github.shivamingale3.mytube.skin.dot

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** Dot palette: black and white, with one accent for live state. */
internal object DotColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF0D0D0D)
    val SurfaceBorder = Color(0xFF1E1E1E)
    val Focus = Color(0xFFFFFFFF)
    val Text = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFF9A9A9A)
    val TextDim = Color(0xFF7A7A7A)
    val Track = Color(0xFF2E2E2E)
    val GridDot = Color(0xFF1C1C1C)
    val Accent = Color(0xFFFF2E4D)
    val Sponsor = Color(0xFF00D26A)
}

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: Int) =
    Font(resId, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

internal object DotFonts {
    /** Dot-matrix display face for big numbers and headings. */
    val Matrix = FontFamily(variable(R.font.doto, 900), variable(R.font.doto, 700))
    val Sans = FontFamily(listOf(400, 500, 600, 700).map { variable(R.font.geist, it) })
    val Mono = FontFamily(listOf(400, 500, 600).map { variable(R.font.geist_mono, it) })
}

internal object DotType {
    val Wordmark = TextStyle(fontFamily = DotFonts.Matrix, fontWeight = FontWeight(900), fontSize = 34.sp, letterSpacing = 0.04.em)
    val MatrixNumber = TextStyle(fontFamily = DotFonts.Matrix, fontWeight = FontWeight(900), fontSize = 40.sp)
    val Label = TextStyle(fontFamily = DotFonts.Mono, fontWeight = FontWeight(500), fontSize = 12.sp, letterSpacing = 0.14.em)
    val Nav = TextStyle(fontFamily = DotFonts.Mono, fontWeight = FontWeight(500), fontSize = 14.sp, letterSpacing = 0.12.em)
    val Hero = TextStyle(fontFamily = DotFonts.Sans, fontWeight = FontWeight(600), fontSize = 30.sp, letterSpacing = (-0.02).em, lineHeight = 33.sp)
    val Title = TextStyle(fontFamily = DotFonts.Sans, fontWeight = FontWeight(600), fontSize = 15.sp, lineHeight = 20.sp)
    val Card = TextStyle(fontFamily = DotFonts.Sans, fontWeight = FontWeight(500), fontSize = 14.sp)
    val Meta = TextStyle(fontFamily = DotFonts.Mono, fontWeight = FontWeight(400), fontSize = 12.sp, letterSpacing = 0.06.em)
}

@Composable
internal fun DotMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DotColors.Background,
            surface = DotColors.Surface,
            primary = DotColors.Focus,
            onSurface = DotColors.Text,
        ),
        content = content,
    )
}
