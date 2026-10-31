package io.github.shivamingale3.mytube

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource

/**
 * Continues the launch window's wordmark (same image, same place, so nothing jumps) and adds a
 * dot loader underneath while Home loads and YouTube's one-time setup runs behind it.
 */
@Composable
fun Splash() {
    val density = LocalDensity.current
    val painter = painterResource(R.drawable.splash_wordmark)
    // The launch window draws the bitmap at its pixel size; match that exactly.
    val width = with(density) { painter.intrinsicSize.width.toDp() }
    val height = with(density) { painter.intrinsicSize.height.toDp() }
    val step by rememberInfiniteTransition(label = "splash").animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "dots",
    )
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Image(painter, contentDescription = "MyTube", contentScale = ContentScale.None, modifier = Modifier.size(width, height))
        Row(
            Modifier.offset(y = height * 0.9f),
            horizontalArrangement = Arrangement.spacedBy(height * 0.08f),
        ) {
            repeat(5) { i ->
                Box(
                    Modifier.size(height * 0.09f).background(
                        if (i == step.toInt() % 5) Color(0xFFFF2E4D) else Color(0xFF3A3A3A),
                        CircleShape,
                    ),
                )
            }
        }
    }
}
