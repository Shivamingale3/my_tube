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
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import android.text.Layout
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import io.github.shivamingale3.mytube.screens.PlayerUiState
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

    // Hold off the TV's screensaver while a video plays or loads; once paused, it may come on again.
    val view = LocalView.current
    val keepOn = state.isPlaying || state.phase == PlayerUiState.Phase.Resolving || state.phase == PlayerUiState.Phase.Buffering
    DisposableEffect(view, keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    val actions = PlayerActions(
        togglePlay = viewModel::togglePlay,
        seekBy = viewModel::seekBy,
        seekTo = viewModel::seekTo,
        skipSegment = viewModel::skipSegment,
        setQuality = viewModel::setQuality,
        setCaption = viewModel::setCaption,
        setSpeed = viewModel::setSpeed,
        retry = viewModel::retry,
        showStats = viewModel::showStats,
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
                Captions(viewModel.player, Modifier.fillMaxSize())
            }
        }
    }
}

/** Captions in YouTube's plain style: white text on a translucent black box, bottom centre. */
@OptIn(UnstableApi::class)
@Composable
private fun Captions(player: Player, modifier: Modifier) {
    val context = LocalContext.current
    val view = remember {
        SubtitleView(context).apply {
            setStyle(
                CaptionStyleCompat(
                    android.graphics.Color.WHITE,
                    0xBF000000.toInt(),
                    android.graphics.Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_NONE,
                    android.graphics.Color.WHITE,
                    null,
                ),
            )
            setApplyEmbeddedStyles(false)
            setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 1.1f)
        }
    }
    DisposableEffect(player) {
        view.setCues(player.currentCues.cues.map(::centred))
        val listener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) = view.setCues(cueGroup.cues.map(::centred))
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    AndroidView(factory = { view }, modifier = modifier)
}

/**
 * YouTube's captions (auto-generated ones especially) are timed for its web player and pin every
 * line to the left edge (`align:start position:0%`). Drop the placement so they sit bottom centre.
 */
@OptIn(UnstableApi::class)
private fun centred(cue: Cue): Cue = cue.buildUpon()
    .setTextAlignment(Layout.Alignment.ALIGN_CENTER)
    .setPosition(Cue.DIMEN_UNSET)
    .setPositionAnchor(Cue.TYPE_UNSET)
    .setLine(Cue.DIMEN_UNSET, Cue.TYPE_UNSET)
    .setLineAnchor(Cue.TYPE_UNSET)
    .setSize(Cue.DIMEN_UNSET)
    .build()
