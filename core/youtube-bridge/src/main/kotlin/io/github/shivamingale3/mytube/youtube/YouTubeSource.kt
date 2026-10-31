package io.github.shivamingale3.mytube.youtube

import android.content.Context
import com.liskovsoft.mediaserviceinterfaces.ContentService
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem
import io.github.shivamingale3.mytube.model.HomeFeed
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.model.VideoRow
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The only door into SmartTube's MediaServiceCore. Everything above this class works with
 * our own model types, so the data layer underneath can be swapped or updated in one place.
 */
class YouTubeSource(context: Context) {
    private val content: ContentService by lazy { MediaServiceCore.services(context).contentService }

    /**
     * The signed-in home feed, or, when YouTube returns nothing (no account), rows built from
     * the sections that still work signed out. Rows without videos are dropped.
     */
    suspend fun home(): HomeFeed = withContext(Dispatchers.IO) {
        val personal = content.home.orEmpty().mapNotNull { it.toRow() }
        if (personal.isNotEmpty()) return@withContext HomeFeed(personal, personalized = true)
        HomeFeed(signedOutRows(), personalized = false)
    }

    private suspend fun signedOutRows(): List<VideoRow> = coroutineScope {
        val sections = listOf(
            "Live" to { content.liveObserve },
            "Gaming" to { content.gamingObserve },
            "Sports" to { content.sportsObserve },
            "News" to { content.newsObserve },
            "Shorts" to { content.shortsObserve.map { listOf(it) } },
        )
        sections
            .map { (name, feed) -> async { collect(feed()).mapNotNull { it.toRow(prefix = name) } } }
            .awaitAll()
            .flatten()
    }

    /** MediaServiceCore feeds emit groups over time and then complete; gather them all. */
    private suspend fun collect(feed: Observable<List<MediaGroup>>): List<MediaGroup> =
        withTimeoutOrNull(FEED_TIMEOUT_MS) {
            runCatching { feed.toList().await().flatten() }.getOrNull()
        }.orEmpty()

    private companion object {
        const val FEED_TIMEOUT_MS = 30_000L
    }
}

private fun MediaGroup.toRow(prefix: String? = null): VideoRow? {
    val videos = mediaItems.orEmpty().mapNotNull { it.toVideo() }
    if (videos.isEmpty()) return null
    val name = title?.takeIf { it.isNotBlank() }
    val rowTitle = when {
        prefix == null -> name.orEmpty()
        name == null -> prefix
        else -> "$prefix · $name"
    }
    return VideoRow(title = rowTitle, videos = videos)
}

private fun MediaItem.toVideo(): Video? {
    val id = videoId ?: return null
    return Video(
        id = id,
        title = title.orEmpty(),
        channelName = author,
        channelId = channelId,
        thumbnailUrl = cardImageUrl,
        durationMs = durationMs,
        subtitle = secondTitle?.toString(),
        percentWatched = percentWatched,
        isLive = isLive,
        isShorts = isShorts,
    )
}
