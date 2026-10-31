package io.github.shivamingale3.mytube.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.shivamingale3.mytube.model.ChannelSummary
import io.github.shivamingale3.mytube.model.HomeFeed
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.model.VideoRow
import io.github.shivamingale3.mytube.youtube.YouTubeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything a skin needs to draw Home. Skins decide the layout; the content is the same. */
data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    /** False when signed out: YouTube has no home feed then, so rows are popular sections. */
    val personalized: Boolean = false,
    val featured: Video? = null,
    /** The most recent part-watched video. */
    val continueWatching: Video? = null,
    /** Subscribed channels, those with new uploads first. */
    val channels: List<ChannelSummary> = emptyList(),
    /** Rows under the featured video; the first is the rest of the featured video's row. */
    val rows: List<VideoRow> = emptyList(),
) {
    val newUploadCount: Int get() = channels.count { it.hasNewContent }
}

class HomeViewModel(private val youTube: YouTubeSource) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        _state.value = HomeUiState()
        loadJob = viewModelScope.launch {
            launch {
                youTube.home()
                    .catch { e -> _state.update { it.copy(loading = false, error = e.message ?: e.toString()) } }
                    .collect { feed -> _state.update { it.withFeed(feed) } }
            }
            launch {
                val watched = youTube.history().firstOrNull { it.percentWatched in 1..95 }
                _state.update { it.copy(continueWatching = watched) }
            }
            launch {
                val channels = youTube.subscribedChannels()
                _state.update { it.copy(channels = channels) }
            }
        }
    }

    private fun HomeUiState.withFeed(feed: HomeFeed): HomeUiState {
        val firstRow = feed.rows.firstOrNull()
        val featured = firstRow?.videos?.firstOrNull { !it.isShorts }
        val rest = buildList {
            firstRow?.let { row -> add(row.copy(videos = row.videos - setOfNotNull(featured))) }
            addAll(feed.rows.drop(1))
        }.filter { it.videos.isNotEmpty() }
        return copy(loading = false, error = null, personalized = feed.personalized, featured = featured, rows = rest)
    }
}
