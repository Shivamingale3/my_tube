package io.github.shivamingale3.mytube.skin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Which video should get focus back after the player closes: the one that was played. Screens
 * also skip their own initial focus while [videoId] is set, so they don't steal it.
 */
class FocusRestore(val videoId: String?, val done: () -> Unit)

val LocalFocusRestore = staticCompositionLocalOf { FocusRestore(null) {} }

/** Put on a card or button that plays [videoId]: it takes focus back when returning from the player. */
@Composable
fun Modifier.restoreFocus(videoId: String): Modifier {
    val restore = LocalFocusRestore.current
    if (restore.videoId != videoId) return this
    val requester = remember { FocusRequester() }
    LaunchedEffect(videoId) {
        if (runCatching { requester.requestFocus() }.isSuccess) restore.done()
    }
    return focusRequester(requester)
}
