package io.github.shivamingale3.mytube.player

import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.view.Display
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.dash.manifest.DashManifestParser
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import io.github.shivamingale3.mytube.model.PlaybackInfo
import io.github.shivamingale3.mytube.model.StreamSource
import kotlin.math.roundToInt

/** A video resolution the current stream offers, e.g. 2160 → "2160p", 1080 at 60 fps → "1080p60". */
data class VideoQuality(val height: Int, val frameRate: Int) {
    val label: String get() = "${height}p" + if (frameRate > 30) frameRate.toString() else ""
}

/** A caption language the current video offers. */
data class CaptionChoice(val id: String, val label: String)

/** One stream YouTube offered, as the stats panel lists it. */
data class StreamTrack(
    /** e.g. "2160p60" for video, "eng" for audio. */
    val label: String,
    /** e.g. "vp09.00.51.08", "avc1.640028", "opus". */
    val codec: String,
    val bitrateKbps: Int,
    val support: Support,
    val playing: Boolean,
) {
    enum class Support {
        /** The decoder says it can play it. */
        Ok,
        /** Beyond the decoder's declared speed, allowed anyway (see TvRenderersFactory). */
        Lifted,
        /** No decoder, or too big. */
        Unsupported,
    }
}

/** What the player knows right now, for the stats panel. */
data class PlayerStats(
    /** The screen mode Android reports, e.g. "1920×1080 @60". */
    val display: String,
    val videoDecoder: String?,
    val audioDecoder: String?,
    val video: List<StreamTrack>,
    val audio: List<StreamTrack>,
    val droppedFrames: Int,
    val bandwidthKbps: Long,
    val bufferedMs: Long,
)

/**
 * Owns the app's single ExoPlayer. Skins draw their own surface and controls on top of [player];
 * switching skins never interrupts playback.
 */
@OptIn(UnstableApi::class)
class PlayerController(context: Context) {
    private val appContext = context.applicationContext

    val player: Player get() = exoPlayer

    /** IDs of formats allowed past the decoder's declared limits; written on the playback thread. */
    private val lifted: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    @Volatile private var videoDecoder: String? = null
    @Volatile private var audioDecoder: String? = null
    @Volatile private var droppedFrames = 0
    @Volatile private var bandwidthKbps = 0L

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(appContext, TvRenderersFactory(appContext, lifted)).build().apply {
            // Many TVs report a 1080p screen mode on a 4K panel; don't let that cap the picture.
            trackSelectionParameters = trackSelectionParameters.buildUpon().clearViewportSizeConstraints().build()
            addAnalyticsListener(object : AnalyticsListener {
                override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
                    videoDecoder = decoderName
                }
                override fun onAudioDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
                    audioDecoder = decoderName
                }
                override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
                    this@PlayerController.droppedFrames += droppedFrames
                }
                override fun onBandwidthEstimate(eventTime: AnalyticsListener.EventTime, totalLoadTimeMs: Int, totalBytesLoaded: Long, bitrateEstimate: Long) {
                    bandwidthKbps = bitrateEstimate / 1000
                }
            })
        }
    }

    fun play(info: PlaybackInfo, startPositionMs: Long = 0) {
        lifted.clear()
        videoDecoder = null
        audioDecoder = null
        droppedFrames = 0
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .clearOverrides()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        exoPlayer.setMediaSource(info.toMediaSource(), startPositionMs)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    fun togglePlay() {
        when {
            exoPlayer.playbackState == Player.STATE_ENDED -> { exoPlayer.seekTo(0); exoPlayer.play() }
            exoPlayer.isPlaying -> exoPlayer.pause()
            else -> exoPlayer.play()
        }
    }

    fun seekBy(deltaMs: Long) {
        val duration = exoPlayer.duration.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
        exoPlayer.seekTo((exoPlayer.currentPosition + deltaMs).coerceIn(0, duration))
    }

    fun seekTo(positionMs: Long) = exoPlayer.seekTo(positionMs)

    // --- Quality -------------------------------------------------------------------------------

    /** Resolutions on offer, highest first. */
    fun qualities(): List<VideoQuality> = videoFormats().map { (_, _, format) ->
        VideoQuality(format.height, format.frameRate.takeIf { it > 0 }?.toInt() ?: 0)
    }.filter { it.height > 0 }.distinct().sortedWith(compareByDescending<VideoQuality> { it.height }.thenByDescending { it.frameRate })

    /** The resolution pinned by the user, or null when the player picks automatically. */
    fun pinnedQuality(): VideoQuality? {
        val overrides = exoPlayer.trackSelectionParameters.overrides.values.filter { it.type == C.TRACK_TYPE_VIDEO }
        val override = overrides.firstOrNull() ?: return null
        val format = override.mediaTrackGroup.getFormat(override.trackIndices.firstOrNull() ?: return null)
        return VideoQuality(format.height, format.frameRate.takeIf { it > 0 }?.toInt() ?: 0)
    }

    /** Pins a resolution, or returns to automatic with null. */
    fun setQuality(quality: VideoQuality?) {
        val builder = exoPlayer.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        if (quality != null) {
            val match = videoFormats()
                .filter { (_, _, f) -> f.height == quality.height && (f.frameRate.takeIf { it > 0 }?.toInt() ?: 0) == quality.frameRate }
                .maxByOrNull { (_, _, f) -> f.bitrate }
            if (match != null) builder.addOverride(TrackSelectionOverride(match.first.mediaTrackGroup, match.second))
        }
        exoPlayer.trackSelectionParameters = builder.build()
    }

    private fun videoFormats() = exoPlayer.currentTracks.groups
        .filter { it.type == C.TRACK_TYPE_VIDEO }
        .flatMap { group -> (0 until group.length).filter { group.isTrackSupported(it) }.map { Triple(group, it, group.getTrackFormat(it)) } }

    // --- Captions ------------------------------------------------------------------------------

    fun captions(): List<CaptionChoice> = exoPlayer.currentTracks.groups
        .filter { it.type == C.TRACK_TYPE_TEXT }
        .flatMapIndexed { g, group ->
            (0 until group.length).map { i ->
                val f = group.getTrackFormat(i)
                CaptionChoice(id = "$g:$i", label = f.label ?: f.language ?: "Captions")
            }
        }

    /** The caption track being shown, or null when captions are off. */
    fun shownCaption(): CaptionChoice? {
        if (exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)) return null
        val groups = exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        groups.forEachIndexed { g, group ->
            (0 until group.length).forEach { i ->
                if (group.isTrackSelected(i)) {
                    val f = group.getTrackFormat(i)
                    return CaptionChoice("$g:$i", f.label ?: f.language ?: "Captions")
                }
            }
        }
        return null
    }

    fun setCaption(choice: CaptionChoice?) {
        val builder = exoPlayer.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT)
        if (choice == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            val (g, i) = choice.id.split(":").map(String::toInt)
            val group = exoPlayer.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }.getOrNull(g)
            if (group != null) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                builder.addOverride(TrackSelectionOverride(group.mediaTrackGroup, i))
            }
        }
        exoPlayer.trackSelectionParameters = builder.build()
    }

    // --- Speed ---------------------------------------------------------------------------------

    val speed: Float get() = exoPlayer.playbackParameters.speed

    fun setSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    // --- Stats ---------------------------------------------------------------------------------

    fun stats(): PlayerStats {
        fun tracks(type: Int) = exoPlayer.currentTracks.groups.filter { it.type == type }.flatMap { group ->
            (0 until group.length).map { i ->
                val f = group.getTrackFormat(i)
                StreamTrack(
                    label = if (type == C.TRACK_TYPE_VIDEO) VideoQuality(f.height, f.frameRate.takeIf { it > 0 }?.toInt() ?: 0).label else f.language ?: f.label ?: "audio",
                    codec = f.codecs ?: f.sampleMimeType ?: "?",
                    bitrateKbps = f.bitrate.takeIf { it > 0 }?.div(1000) ?: 0,
                    support = when {
                        !group.isTrackSupported(i) -> StreamTrack.Support.Unsupported
                        f.id in lifted -> StreamTrack.Support.Lifted
                        else -> StreamTrack.Support.Ok
                    },
                    playing = group.isTrackSelected(i),
                )
            }
        }.sortedByDescending { it.bitrateKbps }
        val display = (appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(Display.DEFAULT_DISPLAY)
        val size = Util.getCurrentDisplayModeSize(appContext, display)
        return PlayerStats(
            display = "${size.x}×${size.y} @${display.refreshRate.roundToInt()}",
            videoDecoder = videoDecoder,
            audioDecoder = audioDecoder,
            video = tracks(C.TRACK_TYPE_VIDEO),
            audio = tracks(C.TRACK_TYPE_AUDIO),
            droppedFrames = droppedFrames,
            bandwidthKbps = bandwidthKbps,
            bufferedMs = (exoPlayer.bufferedPosition - exoPlayer.currentPosition).coerceAtLeast(0),
        )
    }

    // --- Sources -------------------------------------------------------------------------------

    private fun PlaybackInfo.toMediaSource(): MediaSource {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(TIMEOUT_MS)
            .setReadTimeoutMs(TIMEOUT_MS)
        val item = MediaItem.Builder()
            .setMediaId(videoId)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(channelName).build())
        val video = when (val source = source) {
            is StreamSource.DashManifest -> {
                // SmartTube builds the manifest locally; feed it to Media3 as an already-parsed manifest.
                val manifest = DashManifestParser().parse(LOCAL_MANIFEST_URI, source.xml.byteInputStream())
                DashMediaSource.Factory(DefaultDashChunkSource.Factory(http), null)
                    .createMediaSource(manifest, item.setUri(LOCAL_MANIFEST_URI).build())
            }
            is StreamSource.DashUrl -> DashMediaSource.Factory(http).createMediaSource(item.setUri(source.url).build())
            is StreamSource.Hls -> HlsMediaSource.Factory(http).createMediaSource(item.setUri(source.url).build())
            is StreamSource.Progressive -> ProgressiveMediaSource.Factory(http).createMediaSource(item.setUri(source.url).build())
        }
        if (captions.isEmpty()) return video
        val textSources = captions.map { track ->
            val config = MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url))
                .setMimeType(track.mimeType)
                .setLanguage(track.languageCode)
                .setLabel(track.label)
                .build()
            // A caption that fails to load must never stop the video.
            SingleSampleMediaSource.Factory(http).setTreatLoadErrorsAsEndOfStream(true).createMediaSource(config, C.TIME_UNSET)
        }
        return MergingMediaSource(video, *textSources.toTypedArray())
    }

    private companion object {
        const val TIMEOUT_MS = 15_000
        val LOCAL_MANIFEST_URI: Uri = Uri.parse("https://mytube.local/manifest.mpd")
    }
}
