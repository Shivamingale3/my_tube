package io.github.shivamingale3.mytube.skin.dot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.key.onKeyEvent
import io.github.shivamingale3.mytube.player.StreamTrack
import io.github.shivamingale3.mytube.screens.PlayerUiState
import io.github.shivamingale3.mytube.screens.PlayerUiState.Phase
import io.github.shivamingale3.mytube.skin.PlayerActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.animateScrollBy

private enum class Sheet { Quality, Speed, Captions, Stats }

private const val HIDE_AFTER_MS = 5_000L
private const val SEEK_STEP_MS = 10_000L

@Composable
internal fun DotPlayer(state: PlayerUiState, actions: PlayerActions, surface: @Composable () -> Unit) {
    var controlsVisible by remember { mutableStateOf(true) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var inputCount by remember { mutableIntStateOf(0) }
    val rootFocus = remember { FocusRequester() }
    val started = state.phase == Phase.Ready || state.phase == Phase.Ended || (state.phase == Phase.Buffering && state.positionMs > 0)

    LaunchedEffect(inputCount, controlsVisible, sheet, state.isPlaying) {
        if (controlsVisible && sheet == null && state.isPlaying) {
            delay(HIDE_AFTER_MS)
            controlsVisible = false
        }
    }
    LaunchedEffect(sheet == Sheet.Stats) { actions.showStats(sheet == Sheet.Stats) }
    LaunchedEffect(controlsVisible, started) { if (!controlsVisible || !started) rootFocus.requestFocus() }

    Box(
        Modifier.fillMaxSize().background(DotColors.Background)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                inputCount++
                when {
                    event.key == Key.Back -> when {
                        sheet != null -> { sheet = null; true }
                        controlsVisible && started && state.isPlaying -> { controlsVisible = false; true }
                        else -> false
                    }
                    event.key == Key.MediaPlayPause || event.key == Key.MediaPlay || event.key == Key.MediaPause -> {
                        actions.togglePlay(); controlsVisible = true; true
                    }
                    event.key == Key.MediaFastForward -> { actions.seekBy(SEEK_STEP_MS); true }
                    event.key == Key.MediaRewind -> { actions.seekBy(-SEEK_STEP_MS); true }
                    !controlsVisible && started -> {
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

        when {
            state.phase == Phase.Failed -> ErrorLayer(state, actions)
            !started -> LoadingLayer(state)
            else -> {
                AnimatedVisibility(controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                    ControlsLayer(state, actions, onOpen = { sheet = it })
                }
                if (state.phase == Phase.Buffering) {
                    DotLoader(Modifier.align(Alignment.Center))
                }
            }
        }

        if (started && state.activeSegment != null) {
            SkipButton(actions.skipSegment, Modifier.align(Alignment.TopEnd).padding(top = 440.dp, end = 64.dp))
        }

        AnimatedVisibility(
            visible = sheet != null,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
        ) {
            when (sheet) {
                Sheet.Quality -> OptionSheet(
                    title = "QUALITY",
                    options = listOf("AUTO") + state.qualities.map { it.label.uppercase() },
                    selected = state.pinnedQuality?.let { state.qualities.indexOf(it) + 1 } ?: 0,
                    onPick = { i -> actions.setQuality(if (i == 0) null else state.qualities[i - 1]); sheet = null },
                )
                Sheet.Speed -> OptionSheet(
                    title = "SPEED",
                    options = PlayerUiState.Speeds.map { speedLabel(it) },
                    selected = PlayerUiState.Speeds.indexOfFirst { it == state.speed }.coerceAtLeast(0),
                    onPick = { i -> actions.setSpeed(PlayerUiState.Speeds[i]); sheet = null },
                )
                Sheet.Captions -> OptionSheet(
                    title = "CAPTIONS",
                    options = listOf("OFF") + state.captions.map { it.label },
                    selected = state.caption?.let { c -> state.captions.indexOfFirst { it.id == c.id } + 1 } ?: 0,
                    onPick = { i -> actions.setCaption(if (i == 0) null else state.captions[i - 1]); sheet = null },
                )
                Sheet.Stats -> StatsPanel(state)
                null -> Unit
            }
        }
    }
}

@Composable
private fun BoxScope.LoadingLayer(state: PlayerUiState) {
    AsyncImage(
        model = state.video?.thumbnailUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
    )
    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.55f)).halftoneScreen())
    Column(
        Modifier.align(Alignment.BottomStart).padding(start = 64.dp, bottom = 88.dp).width(820.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.channelName?.let { Text(it.uppercase(), style = DotType.Label.copy(fontSize = 13.sp), color = Color(0xFFBDBDBD)) }
        Text(state.title, style = DotType.Hero.copy(fontSize = 34.sp, lineHeight = 38.sp), color = DotColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DotLoader()
            Text(
                if (state.phase == Phase.Resolving) "GETTING STREAMS" else "BUFFERING",
                style = DotType.Label.copy(fontSize = 13.sp),
                color = DotColors.TextMuted,
            )
        }
    }
}

@Composable
private fun BoxScope.ErrorLayer(state: PlayerUiState, actions: PlayerActions) {
    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.85f)))
    Column(
        Modifier.align(Alignment.Center).width(760.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("CAN'T PLAY", style = DotType.MatrixNumber.copy(fontSize = 56.sp), color = DotColors.Accent)
        Text(state.error.orEmpty(), style = DotType.Title, color = DotColors.TextMuted, maxLines = 4, overflow = TextOverflow.Ellipsis)
        val retryFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { retryFocus.requestFocus() }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("RETRY", onClick = actions.retry, modifier = Modifier.focusRequester(retryFocus), filled = true)
            Text("BACK TO EXIT", style = DotType.Label, color = DotColors.TextDim)
        }
    }
}

@Composable
private fun ControlsLayer(state: PlayerUiState, actions: PlayerActions, onOpen: (Sheet) -> Unit) {
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { playFocus.requestFocus() }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxWidth().height(360.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.5f to Color.Black.copy(alpha = 0.5f), 1f to Color.Black.copy(alpha = 0.92f))),
        )
        Row(
            Modifier.padding(start = 64.dp, top = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(8.dp).background(if (state.isPlaying) DotColors.Accent else DotColors.TextDim, CircleShape))
            Text(
                listOfNotNull(if (state.isPlaying) "PLAYING" else "PAUSED", state.qualityLabel.uppercase(), speedLabel(state.speed)).joinToString(" · "),
                style = DotType.Nav.copy(fontSize = 13.sp),
                color = DotColors.Text,
            )
            state.sourceLabel?.let { Text(it.uppercase(), style = DotType.Nav.copy(fontSize = 13.sp), color = DotColors.TextDim) }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 64.dp, end = 64.dp, bottom = 40.dp)) {
            state.channelName?.let { Text(it.uppercase(), style = DotType.Label.copy(fontSize = 13.sp), color = Color(0xFFBDBDBD)) }
            Text(
                state.title,
                style = DotType.Hero.copy(fontSize = 26.sp, lineHeight = 30.sp),
                color = DotColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp),
            )
            if (!state.isLive) DotTimeline(state)
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.Bottom) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(if (state.isLive) "LIVE" else formatTime(state.positionMs), style = DotType.MatrixNumber.copy(fontSize = 88.sp, lineHeight = 80.sp), color = if (state.isLive) DotColors.Accent else DotColors.Text)
                    if (!state.isLive) Text("/ ${formatTime(state.durationMs)}", style = DotType.Meta.copy(fontSize = 16.sp), color = DotColors.TextDim, modifier = Modifier.padding(bottom = 10.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (!state.isLive) RoundButton("-10", size = 56) { actions.seekBy(-SEEK_STEP_MS) }
                    PlayButton(playing = state.isPlaying, ended = state.phase == Phase.Ended, onClick = actions.togglePlay, modifier = Modifier.focusRequester(playFocus))
                    if (!state.isLive) RoundButton("+10", size = 56) { actions.seekBy(SEEK_STEP_MS) }
                    Box(Modifier.size(1.dp, 32.dp).background(Color(0xFF333333)))
                    Pill(if (state.caption != null) "CC ON" else "CC", onClick = { onOpen(Sheet.Captions) }, enabled = state.captions.isNotEmpty())
                    Pill("QUALITY", onClick = { onOpen(Sheet.Quality) }, enabled = state.qualities.isNotEmpty())
                    Pill(speedLabel(state.speed), onClick = { onOpen(Sheet.Speed) })
                    Pill("STATS", onClick = { onOpen(Sheet.Stats) })
                }
            }
        }
    }
}

/** 96 dots across the screen: watched white, sponsor parts green, the playhead red and larger. */
@Composable
private fun DotTimeline(state: PlayerUiState, count: Int = 96) {
    val duration = state.durationMs.coerceAtLeast(1)
    val current = ((state.positionMs * count) / duration).toInt().coerceIn(0, count - 1)
    Row(Modifier.fillMaxWidth().height(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val atMs = (i.toLong() * duration) / count
            val sponsor = state.segments.any { atMs in it.startMs until it.endMs }
            val color = when {
                i == current -> DotColors.Accent
                sponsor -> DotColors.Sponsor
                i < current -> DotColors.Text
                else -> Color(0xFF3A3A3A)
            }
            Box(Modifier.size(if (i == current) 12.dp else 6.dp).background(color, CircleShape))
        }
    }
}

@Composable
private fun PlayButton(playing: Boolean, ended: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DotFocusable(onClick = onClick, radius = 999.dp, modifier = modifier) { focused ->
        Box(
            Modifier.size(68.dp)
                .border(if (focused) 3.dp else 0.dp, if (focused) DotColors.Accent else Color.Transparent, CircleShape)
                .padding(if (focused) 5.dp else 0.dp)
                .background(DotColors.Text, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (playing && !ended) PauseGlyph(Modifier.size(22.dp)) else PlayGlyph(Modifier.size(22.dp), color = DotColors.Background)
        }
    }
}

@Composable
private fun PauseGlyph(modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(6.dp).fillMaxHeight().background(DotColors.Background, RoundedCornerShape(1.dp)))
        Box(Modifier.width(6.dp).fillMaxHeight().background(DotColors.Background, RoundedCornerShape(1.dp)))
    }
}

@Composable
private fun RoundButton(label: String, size: Int, onClick: () -> Unit) {
    DotFocusable(onClick = onClick, radius = 999.dp) { focused ->
        Box(
            Modifier.size(size.dp)
                .background(if (focused) DotColors.Text else Color.Transparent, CircleShape)
                .border(1.5.dp, Color(0xFF5A5A5A), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(label, style = DotType.Nav, color = if (focused) DotColors.Background else DotColors.Text) }
    }
}

@Composable
private fun Pill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, filled: Boolean = false) {
    DotFocusable(onClick = if (enabled) onClick else null, radius = 999.dp, modifier = modifier) { focused ->
        val inverted = focused || filled
        Box(
            Modifier.height(44.dp)
                .background(if (inverted) DotColors.Text else Color.Transparent, RoundedCornerShape(999.dp))
                .border(1.5.dp, if (enabled) Color(0xFF5A5A5A) else Color(0xFF2E2E2E), RoundedCornerShape(999.dp))
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = DotType.Nav.copy(fontSize = 13.sp), color = if (inverted) DotColors.Background else if (enabled) DotColors.Text else DotColors.TextDim)
        }
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit, modifier: Modifier) {
    DotFocusable(onClick = onClick, radius = 999.dp, modifier = modifier) { _ ->
        Row(
            Modifier.height(50.dp).background(DotColors.Text, RoundedCornerShape(999.dp)).padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("SKIP SPONSOR", style = DotType.Nav, color = DotColors.Background)
            Box(Modifier.size(8.dp).background(DotColors.Sponsor, CircleShape))
        }
    }
}

@Composable
internal fun OptionSheet(title: String, options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    Column(
        Modifier.width(420.dp).fillMaxHeight()
            .background(Color(0xF00A0A0A))
            .border(1.dp, DotColors.SurfaceBorder)
            .padding(horizontal = 36.dp, vertical = 44.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(title, style = DotType.MatrixNumber, color = DotColors.Text)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(options) { i, label ->
                DotFocusable(
                    onClick = { onPick(i) },
                    radius = 14.dp,
                    modifier = Modifier.fillMaxWidth().then(if (i == selected) Modifier.focusRequester(first) else Modifier),
                ) { focused ->
                    Row(
                        Modifier.fillMaxWidth().height(54.dp)
                            .background(if (focused) Color(0xFF1A1A1A) else Color.Transparent, RoundedCornerShape(14.dp))
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(8.dp).background(if (i == selected) DotColors.Accent else Color.Transparent, CircleShape))
                        Text(label, style = DotType.Nav.copy(fontSize = 16.sp), color = if (focused || i == selected) DotColors.Text else DotColors.TextMuted, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * "Stats for nerds": every stream YouTube offered and whether this TV can play it, plus how long
 * finding the streams took. Up and down scroll it.
 */
@Composable
internal fun StatsPanel(state: PlayerUiState) {
    val focus = remember { FocusRequester() }
    val scroll = rememberScrollState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(Unit) { focus.requestFocus() }
    val stats = state.stats
    Column(
        Modifier.width(600.dp).fillMaxHeight()
            .background(Color(0xF00A0A0A))
            .border(1.dp, DotColors.SurfaceBorder)
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val step = when (event.key) { Key.DirectionDown -> 120f; Key.DirectionUp -> -120f; else -> return@onKeyEvent false }
                scope.launch { scroll.animateScrollBy(step) }
                true
            }
            .verticalScroll(scroll)
            .padding(horizontal = 36.dp, vertical = 44.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("STATS", style = DotType.MatrixNumber, color = DotColors.Text)
        StatsHeading("STREAMS")
        StatsLine("Source", state.sourceLabel ?: "—")
        StatsLine("Found after", state.resolveLabel ?: "—")
        StatsLine("Waited", state.waitedMs?.let(::seconds) ?: "—")
        StatsLine("First frame", state.firstFrameMs?.let(::seconds) ?: "—")
        if (stats == null) return@Column
        StatsHeading("PLAYBACK")
        StatsLine("Screen", stats.display)
        StatsLine("Video decoder", stats.videoDecoder ?: "—")
        StatsLine("Audio decoder", stats.audioDecoder ?: "—")
        StatsLine("Bandwidth", "${"%.1f".format(stats.bandwidthKbps / 1000f)} Mbps")
        StatsLine("Buffered", seconds(stats.bufferedMs))
        StatsLine("Dropped frames", stats.droppedFrames.toString())
        StatsHeading("VIDEO · ${stats.video.size}")
        stats.video.forEach { TrackLine(it) }
        StatsHeading("AUDIO · ${stats.audio.size}")
        stats.audio.forEach { TrackLine(it) }
    }
}

@Composable
private fun StatsHeading(text: String) {
    Text(text, style = DotType.Label, color = DotColors.Accent, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
}

@Composable
private fun StatsLine(name: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(name.uppercase(), style = DotType.Meta, color = DotColors.TextDim, modifier = Modifier.width(150.dp))
        Text(value, style = DotType.Meta, color = DotColors.Text)
    }
}

@Composable
private fun TrackLine(track: StreamTrack) {
    val (status, color) = when (track.support) {
        StreamTrack.Support.Ok -> "OK" to DotColors.TextMuted
        StreamTrack.Support.Lifted -> "OK · PAST LIMIT" to DotColors.Sponsor
        StreamTrack.Support.Unsupported -> "CAN'T PLAY" to DotColors.TextDim
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(6.dp).background(if (track.playing) DotColors.Accent else Color.Transparent, CircleShape))
        Text(track.label.uppercase(), style = DotType.Meta, color = if (track.playing) DotColors.Text else DotColors.TextMuted, modifier = Modifier.width(80.dp))
        Text(track.codec, style = DotType.Meta, color = DotColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(150.dp))
        Text("${track.bitrateKbps} kbps", style = DotType.Meta, color = DotColors.TextMuted, modifier = Modifier.width(100.dp))
        Text(status, style = DotType.Meta, color = color)
    }
}

private fun seconds(ms: Long) = "%.1f s".format(ms / 1000f)

/** 1f → "1.0×", 1.25f → "1.25×". */
private fun speedLabel(speed: Float): String {
    val plain = java.math.BigDecimal(speed.toString()).stripTrailingZeros().toPlainString()
    return (if ('.' in plain) plain else "$plain.0") + "×"
}

internal fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
