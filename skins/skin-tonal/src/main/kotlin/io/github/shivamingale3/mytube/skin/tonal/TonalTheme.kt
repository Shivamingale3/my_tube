package io.github.shivamingale3.mytube.skin.tonal

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
import io.github.shivamingale3.mytube.skin.SkinAccent

internal object TonalColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF141414)
    val Chip = Color(0xFF1C1C1C)
    val Raised = Color(0xFF262626)
    val Border = Color(0xFF3A3A3A)
    val Text = Color(0xFFFFFFFF)
    val Muted = Color(0xFFA3A3A3)
    val Dim = Color(0xFF6B6B6B)
    val Sponsor = Color(0xFF00D26A)

    /** The accent picked in Settings. */
    val Accent: Color get() = SkinAccent.color

    /** Text drawn on the accent: black on a white accent, white otherwise. */
    val OnAccent: Color get() = if (Accent.red + Accent.green + Accent.blue > 2.6f) Color.Black else Color.White
}

@OptIn(ExperimentalTextApi::class)
private fun lexend(weight: Int) = Font(R.font.lexend, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

internal object TonalFonts {
    val Display = FontFamily(listOf(400, 500, 600, 700).map(::lexend))
    val Body = FontFamily(Font(R.font.roboto_flex_400, FontWeight.Normal))
}

internal object TonalType {
    val Hero = TextStyle(fontFamily = TonalFonts.Display, fontWeight = FontWeight(600), fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.02).em)
    val Heading = TextStyle(fontFamily = TonalFonts.Display, fontWeight = FontWeight(600), fontSize = 34.sp, letterSpacing = (-0.02).em)
    val Section = TextStyle(fontFamily = TonalFonts.Display, fontWeight = FontWeight(500), fontSize = 20.sp)
    val Button = TextStyle(fontFamily = TonalFonts.Display, fontWeight = FontWeight(600), fontSize = 17.sp)
    val Tab = TextStyle(fontFamily = TonalFonts.Display, fontWeight = FontWeight(500), fontSize = 16.sp)
    val Chip = TextStyle(fontFamily = TonalFonts.Display, fontWeight = FontWeight(500), fontSize = 14.sp)
    val CardTitle = TextStyle(fontFamily = TonalFonts.Display, fontWeight = FontWeight(500), fontSize = 15.sp, lineHeight = 20.sp)
    val Title = TextStyle(fontFamily = TonalFonts.Display, fontWeight = FontWeight(600), fontSize = 17.sp, lineHeight = 22.sp)
    val Body = TextStyle(fontFamily = TonalFonts.Body, fontSize = 15.sp, lineHeight = 20.sp)
    val Meta = TextStyle(fontFamily = TonalFonts.Body, fontSize = 13.sp)
}

@Composable
internal fun TonalMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = TonalColors.Text,
            background = TonalColors.Background,
            surface = TonalColors.Background,
            onSurface = TonalColors.Text,
        ),
        content = content,
    )
}
