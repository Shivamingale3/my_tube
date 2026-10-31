package io.github.shivamingale3.mytube

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.screens.PlayerViewModel
import io.github.shivamingale3.mytube.skin.DesignScale
import io.github.shivamingale3.mytube.skin.PlayerActions
import io.github.shivamingale3.mytube.skin.Skin
import org.koin.androidx.compose.koinViewModel

/** Opens [video] in the shared player and lets the active skin draw everything around the picture. */
@OptIn(UnstableApi::class)
@Composable
fun PlayerRoute(video: Video, skin: Skin, onBack: () -> Unit, viewModel: PlayerViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(video.id) { viewModel.open(video) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    BackHandler(onBack = onBack)

    val actions = PlayerActions(
        togglePlay = viewModel::togglePlay,
        seekBy = viewModel::seekBy,
        skipSegment = viewModel::skipSegment,
        setQuality = viewModel::setQuality,
        setCaption = viewModel::setCaption,
        setSpeed = viewModel::setSpeed,
        retry = viewModel::retry,
    )
    DesignScale {
        skin.Player(state, actions) {
            val presentation = rememberPresentationState(viewModel.player)
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                PlayerSurface(
                    player = viewModel.player,
                    surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                    modifier = Modifier.resizeWithContentScale(ContentScale.Fit, presentation.videoSizeDp),
                )
                if (presentation.coverSurface) Box(Modifier.fillMaxSize().background(Color.Black))
            }
        }
    }
}
