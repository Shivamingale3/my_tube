package io.github.shivamingale3.mytube

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import org.koin.androidx.compose.koinViewModel

/** Temporary full-screen player; the skins draw their own controls later. Back returns. */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(videoId: String, onBack: () -> Unit, viewModel: PlayerViewModel = koinViewModel(key = videoId)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var playerError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId) { viewModel.open(videoId) }
    BackHandler(onBack = onBack)
    // The screen's logic outlives it (activity-scoped), so stop the shared player when leaving.
    DisposableEffect(Unit) { onDispose { viewModel.controller.stop() } }

    DisposableEffect(viewModel.controller) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playerError = "${error.errorCodeName}: ${error.cause?.message ?: error.message}"
            }
        }
        viewModel.controller.player.addListener(listener)
        onDispose { viewModel.controller.player.removeListener(listener) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    player = viewModel.controller.player
                    useController = true
                    setShowSubtitleButton(true)
                    keepScreenOn = true
                    isFocusable = true
                    requestFocus()
                }
            },
            onRelease = { it.player = null },
        )
        val message = when (val s = state) {
            PlayerViewModel.State.Loading -> "Getting streams…"
            is PlayerViewModel.State.Failed -> "Can't play: ${s.message}"
            is PlayerViewModel.State.Playing -> playerError?.let { "Player error: $it" }
        }
        if (message != null) {
            Text(
                message,
                color = if (state is PlayerViewModel.State.Loading) Color.White else Color(0xFFFF2E4D),
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Center).padding(48.dp),
            )
        }
    }
}
