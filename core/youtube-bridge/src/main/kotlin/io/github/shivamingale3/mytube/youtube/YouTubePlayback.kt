package io.github.shivamingale3.mytube.youtube

import android.content.Context
import com.liskovsoft.googlecommon.common.helpers.DefaultHeaders
import com.liskovsoft.mediaserviceinterfaces.MediaItemService
import com.liskovsoft.mediaserviceinterfaces.ServiceManager
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import com.liskovsoft.youtubeapi.videoinfo.V2.VideoInfoService
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Resolves a video ID to ad-free streams through MediaServiceCore. */
class YouTubePlayback(context: Context) {
    private val services: ServiceManager by lazy { MediaServiceCore.services(context).also { restoreClient() } }
    private val prefs = context.applicationContext.getSharedPreferences("mytube.playback", Context.MODE_PRIVATE)
    private val items: MediaItemService by lazy { services.mediaItemService }

    // Client switching is global state in MediaServiceCore, so resolve one video at a time.
    private val resolving = Mutex()

    /** After no client offered DASH or HLS, don't cycle through them again for a while. */
    private var skipClientSearchUntil = 0L

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

    /** Saves the watch position to the signed-in account's history, so YouTube resumes it everywhere. */
    fun reportPosition(videoId: String, positionMs: Long) {
        scope.launch { runCatching { items.updateHistoryPosition(videoId, positionMs / 1000f) } }
    }

    private fun Pair<Long, Deferred<PlaybackInfo>>.isStale() =
        System.currentTimeMillis() - first > PREFETCH_TTL_MS || (second.isCompleted && second.getCompletionExceptionOrNull() != null)

    private companion object {
        // Stream URLs stay valid for hours; a short window keeps the cache honest.
        const val PREFETCH_TTL_MS = 10 * 60_000L

        // MediaServiceCore knows about ten YouTube clients; a few tries usually reach one with DASH.
        const val MAX_CLIENT_TRIES = 6
        const val CLIENT_SEARCH_BACKOFF_MS = 30 * 60_000L
        const val KEY_CLIENT = "client"
    }

    /**
     * Some YouTube clients now only offer SABR streams (which need SmartTube's modified ExoPlayer)
     * plus a single 360p file. Switch clients until one offers DASH or HLS; MediaServiceCore keeps
     * the working client for the next videos. Falls back to the first playable answer.
     */
    private suspend fun resolve(videoId: String): PlaybackInfo = withContext(Dispatchers.IO) {
        resolving.withLock {
            val started = System.currentTimeMillis()
            var fallback: MediaItemFormatInfo? = null
            var refusal: String? = null
            val tries = if (started < skipClientSearchUntil) 1 else MAX_CLIENT_TRIES
            for (attempt in 1..tries) {
                val info = items.getFormatInfo(videoId)
                when {
                    info == null -> Unit
                    info.isUnplayable -> refusal = refusal ?: info.playabilityReason
                    info.hasAdaptiveStreams() -> {
                        rememberClient(info)
                        return@withContext info.toPlaybackInfo(videoId, attempt, System.currentTimeMillis() - started)
                    }
                    fallback == null -> fallback = info
                }
                if (tries > 1) services.switchNextClientNow()
            }
            if (fallback != null && tries > 1) skipClientSearchUntil = System.currentTimeMillis() + CLIENT_SEARCH_BACKOFF_MS
            fallback?.toPlaybackInfo(videoId, tries, System.currentTimeMillis() - started)
                ?: throw PlaybackUnavailableException(refusal ?: "YouTube returned no stream information")
        }
    }

    // MediaServiceCore saves the client that worked but starts every launch from the first one
    // (TV, which now often offers only SABR). Remember the one that gave DASH or HLS ourselves.
    private fun rememberClient(info: MediaItemFormatInfo) {
        val name = (info.clientInfo as? Enum<*>)?.name ?: return
        if (prefs.getString(KEY_CLIENT, null) != name) prefs.edit().putString(KEY_CLIENT, name).apply()
    }

    /**
     * Points MediaServiceCore at the remembered client before the first request. Its client enum
     * and the field holding the next client are internal, hence reflection; any failure just
     * means the first video searches again.
     */
    private fun restoreClient() {
        val name = prefs.getString(KEY_CLIENT, null) ?: return
        runCatching {
            val service = VideoInfoService.instance()
            val field = VideoInfoService::class.java.getDeclaredField("mNextClient").apply { isAccessible = true }
            val client = field.type.enumConstants?.firstOrNull { (it as Enum<*>).name == name } ?: return
            field.set(service, client)
        }
    }
}

private fun MediaItemFormatInfo.hasAdaptiveStreams() =
    containsDashFormats() || containsHlsUrl() || containsDashUrl()

private fun MediaItemFormatInfo.toPlaybackInfo(videoId: String, tries: Int, resolveMs: Long): PlaybackInfo {
    val source = pickSource()
    return PlaybackInfo(
        videoId = videoId,
        title = title.orEmpty(),
        channelName = author,
        isLive = isLive,
        source = source,
        userAgent = clientInfo?.userAgent ?: DefaultHeaders.APP_USER_AGENT,
        captions = if (source is StreamSource.DashManifest) emptyList() else subtitles.orEmpty().mapNotNull { sub ->
            val url = sub.baseUrl ?: return@mapNotNull null
            CaptionTrack(
                url = url,
                languageCode = sub.languageCode,
                label = sub.name?.takeIf { it.isNotBlank() } ?: sub.languageCode ?: "Captions",
                mimeType = sub.mimeType ?: "text/vtt",
            )
        },
        clientName = clientInfo?.clientName,
        clientTries = tries,
        resolveMs = resolveMs,
    )
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
