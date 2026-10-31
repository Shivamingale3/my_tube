package io.github.shivamingale3.mytube.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.dash.manifest.DashManifestParser
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import io.github.shivamingale3.mytube.model.PlaybackInfo
import io.github.shivamingale3.mytube.model.StreamSource

/**
 * Owns the app's single ExoPlayer. Skins attach their own surface and controls to [player];
 * switching skins never interrupts playback.
 */
@OptIn(UnstableApi::class)
class PlayerController(context: Context) {
    private val appContext = context.applicationContext

    val player: Player get() = exoPlayer

    private val exoPlayer: ExoPlayer by lazy { ExoPlayer.Builder(appContext).build() }

    fun play(info: PlaybackInfo) {
        exoPlayer.setMediaSource(info.toMediaSource())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    private fun PlaybackInfo.toMediaSource(): MediaSource {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(TIMEOUT_MS)
            .setReadTimeoutMs(TIMEOUT_MS)
        val item = MediaItem.Builder()
            .setMediaId(videoId)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(channelName).build())
        return when (val source = source) {
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
    }

    private companion object {
        const val TIMEOUT_MS = 15_000
        val LOCAL_MANIFEST_URI: Uri = Uri.parse("https://mytube.local/manifest.mpd")
    }
}
