package io.github.shivamingale3.mytube.youtube

import android.content.Context
import com.liskovsoft.mediaserviceinterfaces.ContentService
import com.liskovsoft.mediaserviceinterfaces.MediaItemService
import io.github.shivamingale3.mytube.model.Related
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem
import io.github.shivamingale3.mytube.model.ChannelSummary
import io.github.shivamingale3.mytube.model.HomeFeed
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.model.VideoRow
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The only door into SmartTube's MediaServiceCore. Everything above this class works with
 * our own model types, so the data layer underneath can be swapped or updated in one place.
 */
class YouTubeSource(context: Context) {
    private val content: ContentService by lazy { MediaServiceCore.services(context).contentService }
    private val items: MediaItemService by lazy { MediaServiceCore.services(context).mediaItemService }

    /**
     * What YouTube suggests after [videoId]: its "Up next" pick and the related videos (without
     * [videoId] itself, Shorts or duplicates). Empty when the lookup fails.
     */
    suspend fun related(videoId: String): Related = withContext(Dispatchers.IO) {
        runCatching {
            val metadata = items.getMetadata(videoId) ?: return@runCatching Related()
            val next = metadata.nextVideo?.toVideo()?.takeIf { it.id != videoId }
            val videos = metadata.suggestions.orEmpty()
                .flatMap { it.mediaItems.orEmpty() }
                .mapNotNull { it.toVideo() }
                .filter { it.id != videoId && !it.isShorts }
                .distinctBy { it.id }
            Related(next = next ?: videos.firstOrNull(), videos = videos.filter { it.id != (next ?: videos.firstOrNull())?.id }.take(MAX_RELATED))
        }.getOrDefault(Related())
    }

    /**
     * The home feed, emitted again each time YouTube sends another page of rows, so the first
     * rows show up quickly. When YouTube returns nothing (no account), emits once with rows built
     * from the sections that still work signed out. Rows without videos are dropped.
     */
    fun home(): Flow<HomeFeed> = flow {
        val rows = mutableListOf<VideoRow>()
        runCatching {
            content.homeObserve.asFlow().collect { page ->
                rows += page.mapNotNull { it.toRow() }
                if (rows.isNotEmpty()) emit(HomeFeed(rows.toList(), personalized = true))
                if (rows.size >= MAX_HOME_ROWS) throw HomeIsFull()
            }
        }.onFailure { if (it !is HomeIsFull) throw it }
        if (rows.isEmpty()) emit(HomeFeed(signedOutRows(), personalized = false))
    }.flowOn(Dispatchers.IO)

    /** Stops paging once Home has enough rows; cancelling the collection disposes the request. */
    private class HomeIsFull : RuntimeException()

    /**
     * Watch history, newest first. Empty when signed out. YouTube Music tracks are left out:
     * they need YouTube Music's own client and fail with HTTP 403 through the regular one.
     */
    suspend fun history(): List<Video> = withContext(Dispatchers.IO) {
        runCatching {
            content.history?.mediaItems.orEmpty().filterNot { it.isYouTubeMusic() }.mapNotNull { it.toVideo() }
        }.getOrDefault(emptyList())
    }

    /** Subscribed channels, those with new uploads first. Empty when signed out. */
    suspend fun subscribedChannels(): List<ChannelSummary> = withContext(Dispatchers.IO) {
        runCatching {
            content.subscribedChannelsByNewContent?.mediaItems.orEmpty().mapNotNull { it.toChannel() }
        }.getOrDefault(emptyList())
    }

    /**
     * Videos matching [query]; channels and playlists in the results are left out, and so are
     * entries without a title (YouTube mixes in promoted or shelf items that only carry a video id
     * and a thumbnail, which showed up as empty rows).
     */
    suspend fun search(query: String): List<Video> = withContext(Dispatchers.IO) {
        content.getSearch(query).orEmpty()
            .flatMap { it.mediaItems.orEmpty() }
            .mapNotNull { it.toVideo() }
            .filter { it.title.isNotBlank() }
            .distinctBy { it.id }
    }

    /** YouTube's completions for a partly typed query. Empty when there are none or the lookup fails. */
    suspend fun searchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        runCatching { content.getSearchTags(query).orEmpty().filter { it.isNotBlank() }.take(MAX_SUGGESTIONS) }.getOrDefault(emptyList())
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
        const val MAX_HOME_ROWS = 40
        const val MAX_SUGGESTIONS = 6
        const val MAX_RELATED = 24
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

/** Music tracks come typed as music or from auto-generated "Artist - Topic" channels. */
private fun MediaItem.isYouTubeMusic() = type == MediaItem.TYPE_MUSIC || author?.trimEnd()?.endsWith("- Topic") == true

private fun MediaItem.toChannel(): ChannelSummary? {
    val id = channelId ?: return null
    return ChannelSummary(
        id = id,
        name = title?.takeIf { it.isNotBlank() } ?: author.orEmpty(),
        avatarUrl = cardImageUrl,
        hasNewContent = hasNewContent(),
    )
}

private fun MediaItem.toVideo(): Video? {
    val id = videoId ?: return null
    return Video(
        id = id,
        title = title.orEmpty(),
        // YouTube often sends "Channel • 7M views • 8 days ago" as both author and second title.
        channelName = author?.substringBefore(" • ")?.trim()?.takeIf { it.isNotEmpty() },
        channelId = channelId,
        thumbnailUrl = cardImageUrl,
        durationMs = durationMs,
        subtitle = secondTitle?.toString()?.let { line ->
            val channel = author?.substringBefore(" • ")?.trim()
            if (channel != null && line.startsWith("$channel • ")) line.removePrefix("$channel • ") else line
        },
        percentWatched = percentWatched,
        isLive = isLive,
        isShorts = isShorts,
    )
}
