package io.github.shivamingale3.mytube.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import io.github.shivamingale3.mytube.model.PlaybackInfo
import io.github.shivamingale3.mytube.model.SponsorSegment
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.player.CaptionChoice
import io.github.shivamingale3.mytube.player.PlayerController
import io.github.shivamingale3.mytube.player.PlayerStats
import io.github.shivamingale3.mytube.player.VideoQuality
import io.github.shivamingale3.mytube.youtube.YouTubePlayback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Everything a skin needs to draw the player. */
data class PlayerUiState(
    /** What was opened; lets the loading screen show the thumbnail and title right away. */
    val video: Video? = null,
    val phase: Phase = Phase.Resolving,
    val error: String? = null,
    val title: String = "",
    val channelName: String? = null,
    val isLive: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isPlaying: Boolean = false,
    val segments: List<SponsorSegment> = emptyList(),
    /** A skippable segment the playhead is inside right now. */
    val activeSegment: SponsorSegment? = null,
    val qualities: List<VideoQuality> = emptyList(),
    /** Null means automatic. */
    val pinnedQuality: VideoQuality? = null,
    /** Height of the video actually playing, e.g. 2160. */
    val playingHeight: Int? = null,
    val captions: List<CaptionChoice> = emptyList(),
    val caption: CaptionChoice? = null,
    val speed: Float = 1f,
    /** Stream type and YouTube client, e.g. "DASH · TV"; shown for troubleshooting. */
    val sourceLabel: String? = null,
    /** e.g. "2 clients · 9.8 s"; how the streams were found. */
    val resolveLabel: String? = null,
    /** Seconds from pressing play until the streams were in hand, and until the first frame. */
    val waitedMs: Long? = null,
    val firstFrameMs: Long? = null,
    /** Filled only while the skin shows its stats panel; see [PlayerViewModel.showStats]. */
    val stats: PlayerStats? = null,
    /** Whether the skin should offer its stats panel (a setting). */
    val showStatsButton: Boolean = true,
) {
    enum class Phase { Resolving, Buffering, Ready, Ended, Failed }

    val qualityLabel: String get() = pinnedQuality?.label ?: playingHeight?.let { "AUTO · ${it}p" } ?: "AUTO"

    companion object {
        val Speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    }
}

class PlayerViewModel(
    private val playback: YouTubePlayback,
    private val controller: PlayerController,
    private val settings: AppSettings,
) : ViewModel() {

    val player: Player get() = controller.player

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var openJob: Job? = null
    private var tickJob: Job? = null
    private val skipped = mutableSetOf<SponsorSegment>()
    private var openedAt = 0L
    private var lastReportAt = 0L
    private var statsShown = false

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) = refresh()
        override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
        override fun onTracksChanged(tracks: Tracks) = refresh()
        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(phase = PlayerUiState.Phase.Failed, error = "${error.errorCodeName}: ${error.cause?.message ?: error.message}") }
        }
    }

    init {
        controller.player.addListener(listener)
    }

    fun open(video: Video) {
        openJob?.cancel()
        skipped.clear()
        reportPosition()
        openedAt = System.currentTimeMillis()
        lastReportAt = openedAt
        val prefs = settings.current
        _state.value = PlayerUiState(video = video, title = video.title, channelName = video.channelName, showStatsButton = prefs.statsButton)
        // Resume where the account's history left off, unless the video was nearly finished.
        val startMs = if (prefs.resume && !video.isLive && video.percentWatched in 1..95) video.durationMs * video.percentWatched / 100 else 0L
        openJob = viewModelScope.launch {
            if (prefs.sponsorBlock) launch {
                val segments = playback.sponsorSegments(video.id).filter { it.category in SKIPPABLE }
                _state.update { it.copy(segments = segments) }
            }
            runCatching { playback.load(video.id) }
                .onSuccess { info -> start(info, startMs) }
                .onFailure { e -> _state.update { it.copy(phase = PlayerUiState.Phase.Failed, error = e.message ?: e.toString()) } }
        }
    }

    private fun start(info: PlaybackInfo, startMs: Long) {
        controller.play(info, startMs)
        _state.update {
            it.copy(
                phase = PlayerUiState.Phase.Buffering,
                title = info.title.ifBlank { it.title },
                channelName = info.channelName ?: it.channelName,
                isLive = info.isLive,
                sourceLabel = listOfNotNull(info.source.label, info.clientName).joinToString(" · "),
                resolveLabel = "${info.clientTries} ${if (info.clientTries == 1) "client" else "clients"} · ${seconds(info.resolveMs)}",
                waitedMs = System.currentTimeMillis() - openedAt,
            )
        }
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(TICK_MS)
            }
        }
    }

    fun togglePlay() = controller.togglePlay()

    fun seekBy(deltaMs: Long) {
        controller.seekBy(deltaMs)
        refresh()
    }

    fun skipSegment() {
        val segment = _state.value.activeSegment ?: return
        skipped += segment
        controller.seekTo(segment.endMs)
        refresh()
    }

    fun setQuality(quality: VideoQuality?) {
        controller.setQuality(quality)
        refresh()
    }

    fun setCaption(choice: CaptionChoice?) {
        controller.setCaption(choice)
        refresh()
    }

    fun setSpeed(speed: Float) {
        controller.setSpeed(speed)
        refresh()
    }

    /** The skin opened or closed its stats panel; stats are only gathered while it's open. */
    fun showStats(shown: Boolean) {
        statsShown = shown
        if (!shown) _state.update { it.copy(stats = null) }
        refresh()
    }

    fun retry() {
        _state.value.video?.let(::open)
    }

    fun close() {
        reportPosition()
        openJob?.cancel()
        tickJob?.cancel()
        controller.stop()
        _state.value = PlayerUiState()
    }

    /** Saves the position of the current video to the account's watch history. */
    private fun reportPosition() {
        val s = _state.value
        val video = s.video ?: return
        if (s.isLive || s.positionMs < MIN_REPORT_POSITION_MS) return
        val position = s.positionMs
        lastReportAt = System.currentTimeMillis()
        // Runs in the bridge's own scope, so closing the player doesn't cancel it.
        playback.reportPosition(video.id, position)
    }

    private fun refresh() {
        val p = controller.player
        if (_state.value.phase == PlayerUiState.Phase.Resolving || _state.value.phase == PlayerUiState.Phase.Failed) {
            if (p.playbackState == Player.STATE_IDLE) return
        }
        val position = p.currentPosition
        val phase = when (p.playbackState) {
            Player.STATE_BUFFERING -> if (_state.value.phase == PlayerUiState.Phase.Ready) PlayerUiState.Phase.Ready else PlayerUiState.Phase.Buffering
            Player.STATE_READY -> PlayerUiState.Phase.Ready
            Player.STATE_ENDED -> PlayerUiState.Phase.Ended
            else -> _state.value.phase
        }
        val firstFrame = _state.value.firstFrameMs ?: (System.currentTimeMillis() - openedAt).takeIf { phase == PlayerUiState.Phase.Ready }
        _state.update { s ->
            s.copy(
                phase = phase,
                firstFrameMs = firstFrame,
                stats = if (statsShown) controller.stats() else null,
                positionMs = position,
                durationMs = p.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: s.durationMs,
                isPlaying = p.isPlaying,
                activeSegment = s.segments.firstOrNull { position in it.startMs until it.endMs - SEGMENT_END_MARGIN_MS && it !in skipped },
                qualities = controller.qualities(),
                pinnedQuality = controller.pinnedQuality(),
                playingHeight = p.videoSize.height.takeIf { it > 0 },
                captions = controller.captions(),
                caption = controller.shownCaption(),
                speed = controller.speed,
            )
        }
        afterRefresh()
    }

    /** Called after every refresh: skips segments when asked to, and saves the position now and then. */
    private fun afterRefresh() {
        val s = _state.value
        if (settings.current.autoSkip && s.activeSegment != null) skipSegment()
        if (s.isPlaying && System.currentTimeMillis() - lastReportAt > REPORT_EVERY_MS) reportPosition()
    }

    override fun onCleared() {
        controller.player.removeListener(listener)
        close()
    }

    private companion object {
        fun seconds(ms: Long) = "%.1f s".format(ms / 1000f)

        const val TICK_MS = 250L
        const val REPORT_EVERY_MS = 30_000L
        const val MIN_REPORT_POSITION_MS = 5_000L
        const val SEGMENT_END_MARGIN_MS = 500L
        val SKIPPABLE = setOf("sponsor", "selfpromo", "interaction")
    }
}
