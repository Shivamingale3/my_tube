package io.github.shivamingale3.mytube.skin.tonal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import io.github.shivamingale3.mytube.player.StreamTrack
import io.github.shivamingale3.mytube.screens.PlayerUiState
import io.github.shivamingale3.mytube.screens.PlayerUiState.Phase
import io.github.shivamingale3.mytube.skin.PlayerActions
import io.github.shivamingale3.mytube.skin.Scrubber
import io.github.shivamingale3.mytube.skin.UpNextShelf
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Sheet { Quality, Speed, Captions, Stats }

private const val HIDE_AFTER_MS = 5_000L
private const val SEEK_STEP_MS = 10_000L

/**
 * Tonal's player. Remote behaviour matches Dot: with controls hidden, left/right seek 10 s and
 * OK plays/pauses (or skips a sponsor); Back closes a sheet, then hides the controls, then exits.
 */
@Composable
internal fun TonalPlayer(state: PlayerUiState, actions: PlayerActions, surface: @Composable () -> Unit) {
    var controlsVisible by remember { mutableStateOf(true) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var inputCount by remember { mutableIntStateOf(0) }
    val rootFocus = remember { FocusRequester() }
    // One per video, so a seek in progress can never land on the next video.
    val scrubber = remember(state.video?.id) { Scrubber() }
    val shelf = remember { UpNextShelf() }
    val ended = state.phase == Phase.Ended
    val started = state.phase == Phase.Ready || state.phase == Phase.Ended || (state.phase == Phase.Buffering && state.positionMs > 0)

    LaunchedEffect(inputCount, controlsVisible, sheet, state.isPlaying) {
        if (controlsVisible && sheet == null && !shelf.open && state.isPlaying) {
            delay(HIDE_AFTER_MS)
            controlsVisible = false
        }
    }
    // Controls hiding or a sheet opening ends a seek in progress.
    LaunchedEffect(controlsVisible, sheet) { if (!controlsVisible || sheet != null) scrubber.commit(actions.seekTo) }
    LaunchedEffect(sheet == Sheet.Stats) { actions.showStats(sheet == Sheet.Stats) }
    LaunchedEffect(controlsVisible) { if (!controlsVisible) shelf.open = false }
    LaunchedEffect(controlsVisible, started) { if (!ended && (!controlsVisible || !started)) runCatching { rootFocus.requestFocus() } }

    Box(
        Modifier.fillMaxSize().background(TonalColors.Background)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                val seekKey = event.key == Key.DirectionLeft || event.key == Key.DirectionRight
                if (seekKey && started && !ended && !state.isLive && sheet == null) {
                    if (event.type == KeyEventType.KeyUp && scrubber.pendingMs != null) {
                        scrubber.commit(actions.seekTo)
                        return@onPreviewKeyEvent true
                    }
                    if (event.type == KeyEventType.KeyDown && (!controlsVisible || scrubber.barFocused || scrubber.pendingMs != null)) {
                        inputCount++
                        scrubber.step(event.key == Key.DirectionRight, event.nativeKeyEvent.repeatCount, state.positionMs, state.durationMs)
                        controlsVisible = true
                        return@onPreviewKeyEvent true
                    }
                }
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                inputCount++
                when {
                    event.key == Key.DirectionDown && controlsVisible && sheet == null && !shelf.open && shelf.buttonsFocused && state.related.isNotEmpty() -> {
                        shelf.open = true
                        true
                    }
                    event.key == Key.Back -> when {
                        sheet != null -> { sheet = null; true }
                        shelf.open -> { shelf.open = false; true }
                        controlsVisible && started && state.isPlaying -> { controlsVisible = false; true }
                        else -> false
                    }
                    event.key == Key.MediaPlayPause || event.key == Key.MediaPlay || event.key == Key.MediaPause -> {
                        actions.togglePlay(); controlsVisible = true; true
                    }
                    event.key == Key.MediaFastForward -> { actions.seekBy(SEEK_STEP_MS); true }
                    event.key == Key.MediaRewind -> { actions.seekBy(-SEEK_STEP_MS); true }
                    !controlsVisible && started && !ended -> {
                        when (event.key) {
                            Key.DirectionLeft -> actions.seekBy(-SEEK_STEP_MS)
                            Key.DirectionRight -> actions.seekBy(SEEK_STEP_MS)
                            Key.DirectionCenter, Key.Enter -> if (state.activeSegment != null) actions.skipSegment() else actions.togglePlay()
                            else -> Unit
                        }
                        controlsVisible = true
                        true
                    }
                    else -> false
                }
            },
    ) {
        Box(Modifier.fillMaxSize()) { surface() }
        AudioOnlyLayer(state)

        when {
            state.phase == Phase.Failed -> ErrorLayer(state, actions)
            !started -> LoadingLayer(state)
            ended -> TonalEndScreen(state, actions)
            else -> {
                AnimatedVisibility(controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                    ControlsLayer(state, actions, scrubber, shelf, sheetOpen = sheet != null, onOpen = { sheet = it })
                }
                if (state.phase == Phase.Buffering) TonalLoader(Modifier.align(Alignment.Center))
            }
        }

        if (started && state.activeSegment != null) {
            TonalFocusable(onClick = actions.skipSegment, radius = 20.dp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 392.dp, end = 64.dp)) { _ ->
                Row(
                    Modifier.height(60.dp).background(TonalColors.Text, RoundedCornerShape(20.dp)).padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TonalIcon(Icon.Forward10, Color.Black, Modifier.size(20.dp))
                    Text("Skip sponsor", style = TonalType.Button, color = Color.Black)
                }
            }
        }

        AnimatedVisibility(
            visible = sheet != null,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
        ) {
            when (sheet) {
                Sheet.Quality -> Options(
                    "Quality",
                    listOf("Auto") + state.qualities.map { it.label },
                    selected = state.pinnedQuality?.let { state.qualities.indexOf(it) + 1 } ?: 0,
                ) { i -> actions.setQuality(if (i == 0) null else state.qualities[i - 1]); sheet = null }
                Sheet.Speed -> Options(
                    "Speed",
                    PlayerUiState.Speeds.map { speedLabel(it) },
                    selected = PlayerUiState.Speeds.indexOfFirst { it == state.speed }.coerceAtLeast(0),
                ) { i -> actions.setSpeed(PlayerUiState.Speeds[i]); sheet = null }
                Sheet.Captions -> Options(
                    "Captions",
                    listOf("Off") + state.captions.map { it.label },
                    selected = state.caption?.let { c -> state.captions.indexOfFirst { it.id == c.id } + 1 } ?: 0,
                ) { i -> actions.setCaption(if (i == 0) null else state.captions[i - 1]); sheet = null }
                Sheet.Stats -> Stats(state)
                null -> Unit
            }
        }
    }
}

@Composable
private fun BoxScope.LoadingLayer(state: PlayerUiState) {
    AsyncImage(model = state.video?.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.6f)))
    Column(Modifier.align(Alignment.BottomStart).padding(start = 64.dp, bottom = 80.dp).width(820.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.channelName?.let { Text(it, style = TonalType.Body, color = TonalColors.Muted) }
        Text(state.title, style = TonalType.Hero.copy(fontSize = 34.sp, lineHeight = 40.sp), color = TonalColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TonalLoader()
            Text(if (state.phase == Phase.Resolving) "Getting streams" else "Buffering", style = TonalType.Chip, color = TonalColors.Muted)
        }
    }
}

@Composable
private fun BoxScope.ErrorLayer(state: PlayerUiState, actions: PlayerActions) {
    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.85f)))
    Column(Modifier.align(Alignment.Center).width(720.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Can't play this video", style = TonalType.Heading, color = TonalColors.Text)
        Text(state.error.orEmpty(), style = TonalType.Body, color = TonalColors.Muted, maxLines = 4, overflow = TextOverflow.Ellipsis)
        val retry = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { retry.requestFocus() } }
        TonalButton("Try again", onClick = actions.retry, primary = true, modifier = Modifier.focusRequester(retry))
    }
}

@Composable
private fun ControlsLayer(live: PlayerUiState, actions: PlayerActions, scrubber: Scrubber, shelf: UpNextShelf, sheetOpen: Boolean, onOpen: (Sheet) -> Unit) {
    // While seeking, everything shows the target position.
    val state = live.copy(positionMs = scrubber.pendingMs ?: live.positionMs)
    val barFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    // Like YouTube, the progress bar is selected first, so left and right seek straight away.
    // Play/pause is always the highlighted button when the controls appear, and focus comes back to it
    // after a sheet (captions, quality, …) or the Up next shelf closes, so something is always highlighted.
    LaunchedEffect(sheetOpen, shelf.open) { if (!sheetOpen && !shelf.open) runCatching { playFocus.requestFocus() } }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(320.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
        Column(
            Modifier.align(Alignment.BottomCenter).padding(start = 64.dp, end = 64.dp, bottom = 32.dp).fillMaxWidth()
                .background(Color(0xE00A0A0A), RoundedCornerShape(32.dp)).padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(state.title, style = TonalType.Title.copy(fontSize = 20.sp), color = TonalColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(state.channelName, state.sourceLabel).joinToString(" · "),
                    style = TonalType.Body.copy(fontSize = 14.sp),
                    color = TonalColors.Muted,
                    maxLines = 1,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.isLive) {
                    Text("LIVE", style = TonalType.Chip, color = TonalColors.OnAccent, modifier = Modifier.background(TonalColors.Accent, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                } else {
                    val duration = state.durationMs.coerceAtLeast(1)
                    Text(formatDuration(state.positionMs), style = TonalType.Chip, color = TonalColors.Muted)
                    TonalFocusable(
                        onClick = actions.togglePlay,
                        radius = 999.dp,
                        modifier = Modifier.weight(1f).focusRequester(barFocus).onFocusChanged { scrubber.barFocused = it.isFocused },
                    ) { focused ->
                        WavyProgress(
                            progress = state.positionMs.toFloat() / duration,
                            segments = state.segments.map { it.startMs.toFloat() / duration to it.endMs.toFloat() / duration },
                            animate = state.isPlaying && scrubber.pendingMs == null,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (focused) 10.dp else 2.dp),
                        )
                    }
                    Text(formatDuration(state.durationMs), style = TonalType.Chip, color = TonalColors.Muted)
                }
            }
            Row(Modifier.fillMaxWidth().onFocusChanged { shelf.buttonsFocused = it.hasFocus }, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!state.isLive) RoundIcon(Icon.Back10) { actions.seekBy(-SEEK_STEP_MS) }
                    TonalFocusable(onClick = actions.togglePlay, radius = 20.dp, modifier = Modifier.focusRequester(playFocus)) { _ ->
                        Box(Modifier.size(76.dp, 56.dp).background(TonalColors.Accent, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                            TonalIcon(if (state.isPlaying) Icon.Pause else Icon.Play, TonalColors.OnAccent, Modifier.size(24.dp))
                        }
                    }
                    if (!state.isLive) RoundIcon(Icon.Forward10) { actions.seekBy(SEEK_STEP_MS) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TonalChip(state.qualityLabel.lowercase().replaceFirstChar { it.uppercase() }, onClick = { onOpen(Sheet.Quality) }, enabled = state.qualities.isNotEmpty())
                    TonalChip(speedLabel(state.speed), onClick = { onOpen(Sheet.Speed) })
                    TonalChip(if (state.caption != null) "Captions on" else "Captions", onClick = { onOpen(Sheet.Captions) }, enabled = state.captions.isNotEmpty())
                    TonalChip(
                        when (state.audioMode) {
                            PlayerUiState.AudioMode.Off -> "Video"
                            PlayerUiState.AudioMode.Thumbnail -> "Audio · art"
                            PlayerUiState.AudioMode.Black -> "Audio · dark"
                        },
                        onClick = actions.cycleAudioMode,
                        selected = state.audioMode != PlayerUiState.AudioMode.Off,
                    )
                    if (state.showStatsButton) TonalChip("Stats", onClick = { onOpen(Sheet.Stats) })
                }
            }
            if (shelf.open) {
                TonalUpNextRow(state, actions)
            } else if (state.related.isNotEmpty()) {
                Text("Press ↓ for Up next", style = TonalType.Meta, color = TonalColors.Dim)
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: Icon, onClick: () -> Unit) {
    TonalFocusable(onClick = onClick, radius = 999.dp) { focused ->
        Box(Modifier.size(52.dp).background(if (focused) TonalColors.Text else TonalColors.Raised, CircleShape), contentAlignment = Alignment.Center) {
            TonalIcon(icon, if (focused) Color.Black else TonalColors.Text, Modifier.size(20.dp))
        }
    }
}

@Composable
private fun Options(title: String, options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Column(
        Modifier.width(400.dp).fillMaxHeight().padding(vertical = 24.dp).padding(end = 24.dp)
            .background(Color(0xF0141414), RoundedCornerShape(32.dp)).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = TonalType.Heading.copy(fontSize = 28.sp), color = TonalColors.Text)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(options) { i, label ->
                TonalFocusable(onClick = { onPick(i) }, radius = 16.dp, modifier = Modifier.fillMaxWidth().then(if (i == selected) Modifier.focusRequester(first) else Modifier)) { focused ->
                    Row(
                        Modifier.fillMaxWidth().height(54.dp).background(if (focused) TonalColors.Raised else Color.Transparent, RoundedCornerShape(16.dp)).padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(10.dp).background(if (i == selected) TonalColors.Accent else Color.Transparent, CircleShape))
                        Text(label, style = TonalType.Tab, color = if (focused || i == selected) TonalColors.Text else TonalColors.Muted, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Stats for nerds, in Tonal's type; up and down scroll it. */
@Composable
private fun Stats(state: PlayerUiState) {
    val focus = remember { FocusRequester() }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(
        Modifier.width(600.dp).fillMaxHeight().padding(vertical = 24.dp).padding(end = 24.dp)
            .background(Color(0xF0141414), RoundedCornerShape(32.dp))
            .focusRequester(focus).focusable()
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                val step = when (e.key) { Key.DirectionDown -> 120f; Key.DirectionUp -> -120f; else -> return@onKeyEvent false }
                scope.launch { scroll.animateScrollBy(step) }
                true
            }
            .verticalScroll(scroll).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Stats", style = TonalType.Heading.copy(fontSize = 28.sp), color = TonalColors.Text)
        Line("Source", state.sourceLabel ?: "—")
        Line("Found after", state.resolveLabel ?: "—")
        Line("Waited", state.waitedMs?.let(::seconds) ?: "—")
        Line("First frame", state.firstFrameMs?.let(::seconds) ?: "—")
        val stats = state.stats ?: return@Column
        Line("Screen", stats.display)
        Line("Video decoder", stats.videoDecoder ?: "—")
        Line("Audio decoder", stats.audioDecoder ?: "—")
        Line("Bandwidth", "%.1f Mbps".format(stats.bandwidthKbps / 1000f))
        Line("Buffered", seconds(stats.bufferedMs))
        Line("Dropped frames", stats.droppedFrames.toString())
        Text("Video · ${stats.video.size}", style = TonalType.Title, color = TonalColors.Accent, modifier = Modifier.padding(top = 14.dp))
        stats.video.forEach { Track(it) }
        Text("Audio · ${stats.audio.size}", style = TonalType.Title, color = TonalColors.Accent, modifier = Modifier.padding(top = 14.dp))
        stats.audio.forEach { Track(it) }
    }
}

@Composable
private fun Line(name: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(name, style = TonalType.Meta, color = TonalColors.Dim, modifier = Modifier.width(140.dp))
        Text(value, style = TonalType.Meta, color = TonalColors.Text)
    }
}

@Composable
private fun Track(track: StreamTrack) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(if (track.playing) TonalColors.Accent else Color.Transparent, CircleShape))
        Text(track.label, style = TonalType.Meta, color = if (track.playing) TonalColors.Text else TonalColors.Muted, modifier = Modifier.width(70.dp))
        Text(track.codec, style = TonalType.Meta, color = TonalColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(150.dp))
        Text("${track.bitrateKbps} kbps", style = TonalType.Meta, color = TonalColors.Muted, modifier = Modifier.width(90.dp))
        Text(
            when (track.support) {
                StreamTrack.Support.Ok -> "OK"
                StreamTrack.Support.Lifted -> "OK · past limit"
                StreamTrack.Support.Unsupported -> "Can't play"
            },
            style = TonalType.Meta,
            color = if (track.support == StreamTrack.Support.Lifted) TonalColors.Sponsor else TonalColors.Dim,
        )
    }
}

private fun seconds(ms: Long) = "%.1f s".format(ms / 1000f)

private fun speedLabel(speed: Float): String {
    val plain = java.math.BigDecimal(speed.toString()).stripTrailingZeros().toPlainString()
    return "$plain×"
}

/** Audio-only mode covers the (switched-off) video with the thumbnail, or with black. */
@Composable
private fun BoxScope.AudioOnlyLayer(state: PlayerUiState) {
    when (state.audioMode) {
        PlayerUiState.AudioMode.Off -> Unit
        PlayerUiState.AudioMode.Thumbnail -> AsyncImage(
            model = state.video?.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize().background(Color.Black),
        )
        PlayerUiState.AudioMode.Black -> Box(Modifier.matchParentSize().background(Color.Black))
    }
}
