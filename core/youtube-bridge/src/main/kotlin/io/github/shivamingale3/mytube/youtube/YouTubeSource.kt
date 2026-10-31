package io.github.shivamingale3.mytube.youtube

import android.content.Context
import com.liskovsoft.mediaserviceinterfaces.ServiceManager
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem
import com.liskovsoft.sharedutils.prefs.GlobalPreferences
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.model.VideoRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The only door into SmartTube's MediaServiceCore. Everything above this class works with
 * our own model types, so the data layer underneath can be swapped or updated in one place.
 */
class YouTubeSource(context: Context) {
    init {
        // MediaServiceCore reads its settings and caches through this context-backed singleton.
        GlobalPreferences.instance(context.applicationContext)
    }

    private val services: ServiceManager by lazy { YouTubeServiceManager.instance() }

    suspend fun home(): List<VideoRow> = withContext(Dispatchers.IO) {
        services.contentService.home.orEmpty().mapNotNull { it.toRow() }
    }
}

private fun MediaGroup.toRow(): VideoRow? {
    val videos = mediaItems.orEmpty().mapNotNull { it.toVideo() }
    if (videos.isEmpty()) return null
    return VideoRow(title = title.orEmpty(), videos = videos)
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
