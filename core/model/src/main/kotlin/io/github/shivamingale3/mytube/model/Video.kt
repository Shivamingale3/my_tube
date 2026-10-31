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
