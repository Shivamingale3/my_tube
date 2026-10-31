package io.github.shivamingale3.mytube.youtube

import android.content.Context
import com.liskovsoft.googlecommon.common.helpers.DefaultHeaders
import com.liskovsoft.mediaserviceinterfaces.MediaItemService
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import io.github.shivamingale3.mytube.model.PlaybackInfo
import io.github.shivamingale3.mytube.model.PlaybackUnavailableException
import io.github.shivamingale3.mytube.model.StreamSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resolves a video ID to ad-free streams through MediaServiceCore. */
class YouTubePlayback(context: Context) {
    private val items: MediaItemService by lazy { MediaServiceCore.services(context).mediaItemService }

    suspend fun load(videoId: String): PlaybackInfo = withContext(Dispatchers.IO) {
        val info = items.getFormatInfo(videoId)
            ?: throw PlaybackUnavailableException("YouTube returned no stream information")
        if (info.isUnplayable) {
            throw PlaybackUnavailableException(info.playabilityReason ?: "This video can't be played")
        }
        PlaybackInfo(
            videoId = videoId,
            title = info.title.orEmpty(),
            channelName = info.author,
            isLive = info.isLive,
            source = info.pickSource(),
            userAgent = info.clientInfo?.userAgent ?: DefaultHeaders.APP_USER_AGENT,
        )
    }
}

/** Same preference order as SmartTube, minus SABR, which needs its modified ExoPlayer. */
private fun MediaItemFormatInfo.pickSource(): StreamSource = when {
    isLive && containsHlsUrl() -> StreamSource.Hls(hlsManifestUrl)
    containsDashFormats() -> StreamSource.DashManifest(createMpdStream().bufferedReader().use { it.readText() })
    containsDashUrl() -> StreamSource.DashUrl(dashManifestUrl)
    containsHlsUrl() -> StreamSource.Hls(hlsManifestUrl)
    containsUrlFormats() -> StreamSource.Progressive(createUrlList().first())
    containsSabrFormats() -> throw PlaybackUnavailableException("Only SABR streams were offered; not supported yet")
    else -> throw PlaybackUnavailableException("No playable stream found")
}
