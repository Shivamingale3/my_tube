package io.github.shivamingale3.mytube.youtube

import android.content.Context
import com.liskovsoft.googlecommon.common.helpers.DefaultHeaders
import com.liskovsoft.mediaserviceinterfaces.MediaItemService
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import io.github.shivamingale3.mytube.model.CaptionTrack
import io.github.shivamingale3.mytube.model.PlaybackInfo
import io.github.shivamingale3.mytube.model.PlaybackUnavailableException
import io.github.shivamingale3.mytube.model.StreamSource
import io.github.shivamingale3.mytube.model.SponsorSegment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/** Resolves a video ID to ad-free streams through MediaServiceCore. */
class YouTubePlayback(context: Context) {
    private val items: MediaItemService by lazy { MediaServiceCore.services(context).mediaItemService }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefetched = mutableMapOf<String, Pair<Long, Deferred<PlaybackInfo>>>()

    /** Starts resolving a video likely to be played next, so pressing play can start right away. */
    fun prefetch(videoId: String) = synchronized(prefetched) {
        val existing = prefetched[videoId]
        if (existing != null && !existing.isStale()) return
        prefetched.keys.retainAll { key -> prefetched[key]?.isStale() == false }
        prefetched[videoId] = System.currentTimeMillis() to scope.async { resolve(videoId) }
    }

    suspend fun load(videoId: String): PlaybackInfo {
        val cached = synchronized(prefetched) { prefetched.remove(videoId)?.takeUnless { it.isStale() }?.second }
        return cached?.let { runCatching { it.await() }.getOrNull() } ?: resolve(videoId)
    }

    /** SponsorBlock segments for the video; empty when there are none or the lookup fails. */
    suspend fun sponsorSegments(videoId: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
        runCatching {
            items.getSponsorSegments(videoId).orEmpty().map { SponsorSegment(it.startMs, it.endMs, it.category.orEmpty()) }
        }.getOrDefault(emptyList())
    }

    private fun Pair<Long, Deferred<PlaybackInfo>>.isStale() =
        System.currentTimeMillis() - first > PREFETCH_TTL_MS || (second.isCompleted && second.getCompletionExceptionOrNull() != null)

    private companion object {
        // Stream URLs stay valid for hours; a short window keeps the cache honest.
        const val PREFETCH_TTL_MS = 10 * 60_000L
    }

    private suspend fun resolve(videoId: String): PlaybackInfo = withContext(Dispatchers.IO) {
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
            captions = info.subtitles.orEmpty().mapNotNull { sub ->
                val url = sub.baseUrl ?: return@mapNotNull null
                CaptionTrack(
                    url = url,
                    languageCode = sub.languageCode,
                    label = sub.name?.takeIf { it.isNotBlank() } ?: sub.languageCode ?: "Captions",
                    mimeType = sub.mimeType ?: "text/vtt",
                )
            },
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
