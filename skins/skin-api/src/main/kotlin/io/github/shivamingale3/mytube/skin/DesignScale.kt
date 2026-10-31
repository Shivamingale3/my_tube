package io.github.shivamingale3.mytube.skin

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Width of the skin designs, in design pixels. */
const val DESIGN_WIDTH = 1280f

/**
 * Rescales density so 1 dp (and 1 sp) equals one design pixel at the current screen width.
 * Skins can then use the mockups' numbers directly and look the same on any TV resolution.
 */
@Composable
fun DesignScale(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        CompositionLocalProvider(LocalDensity provides Density(density = widthPx / DESIGN_WIDTH, fontScale = 1f)) {
            content()
        }
    }
}
