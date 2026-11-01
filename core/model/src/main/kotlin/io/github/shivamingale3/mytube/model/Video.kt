package io.github.shivamingale3.mytube.model

/** A playable YouTube video as the app sees it, independent of the data layer underneath. */
data class Video(
    val id: String,
    val title: String,
    val channelName: String?,
    val channelId: String?,
    val thumbnailUrl: String?,
    val durationMs: Long,
    /** Ready-to-show secondary line, e.g. "Veritasium • 14M views • 2 weeks ago". */
    val subtitle: String?,
    val percentWatched: Int,
    val isLive: Boolean,
    val isShorts: Boolean,
)

/** A titled row of videos, such as a section of the home feed. */
data class VideoRow(
    val title: String,
    val videos: List<Video>,
)

/**
 * The Home screen's rows. [personalized] is false when no account is signed in: YouTube no
 * longer serves a signed-out home feed, so the rows come from sections that still work.
 */
data class HomeFeed(
    val rows: List<VideoRow>,
    val personalized: Boolean,
)

/** YouTube's suggestions for after a video: the one it would autoplay, and related videos. */
data class Related(
    val next: Video? = null,
    val videos: List<Video> = emptyList(),
)
