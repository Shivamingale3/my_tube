package io.github.shivamingale3.mytube.model

/** A subscribed channel as shown in channel lists. */
data class ChannelSummary(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    /** YouTube marks channels that uploaded since you last looked. */
    val hasNewContent: Boolean,
)
