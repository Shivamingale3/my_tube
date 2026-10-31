package io.github.shivamingale3.mytube.model

/** Everything the player needs to start a video, already free of ads. */
data class PlaybackInfo(
    val videoId: String,
    val title: String,
    val channelName: String?,
    val isLive: Boolean,
    val source: StreamSource,
    /** Stream hosts expect requests with the same client identity the streams were issued to. */
    val userAgent: String,
)

/** Where the player reads the audio and video from. */
sealed interface StreamSource {
    /** A DASH manifest built locally from the video's adaptive formats. */
    data class DashManifest(val xml: String) : StreamSource

    /** A DASH manifest hosted by YouTube (some live streams). */
    data class DashUrl(val url: String) : StreamSource

    /** An HLS playlist (live streams). */
    data class Hls(val url: String) : StreamSource

    /** A single file with audio and video together (fallback for older formats). */
    data class Progressive(val url: String) : StreamSource
}

/** YouTube refused or could not provide a playable stream; [reason] is shown to the user. */
class PlaybackUnavailableException(val reason: String) : Exception(reason)
