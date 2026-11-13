package io.github.shivamingale3.mytube.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.youtube.YouTubePlayback
import io.github.shivamingale3.mytube.youtube.YouTubeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything a skin needs to draw Search. */
data class SearchUiState(
    /** What is typed so far. */
    val query: String = "",
    /** YouTube's completions for [query]. */
    val suggestions: List<String> = emptyList(),
    /** The query the [results] are for, or null before the first search. */
    val searched: String? = null,
    val results: List<Video> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    /** The mic button: idle, listening (with the words heard so far), or a short failure message. */
    val voice: VoiceState = VoiceState.Idle,
)

class SearchViewModel(
    private val youTube: YouTubeSource,
    private val playback: YouTubePlayback,
    private val speech: SpeechInput,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())

    // What was heard is searched straight away; there is no manual submit after speaking.
    private val voice = VoiceSearch(speech, viewModelScope) { heard -> search(heard) }

    val state: StateFlow<SearchUiState> = combine(_state, voice.state) { s, v -> s.copy(voice = v) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchUiState())

    private var suggestJob: Job? = null
    private var searchJob: Job? = null
    private var focusJob: Job? = null

    /** The typed text changed; suggestions follow after a short pause in typing. */
    fun type(query: String) {
        voice.cancel()
        _state.update { it.copy(query = query) }
        suggestJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(suggestions = emptyList()) }
            return
        }
        suggestJob = viewModelScope.launch {
            delay(SUGGEST_DELAY_MS)
            val suggestions = youTube.searchSuggestions(query)
            _state.update { if (it.query == query) it.copy(suggestions = suggestions) else it }
        }
    }

    /** Runs a search for [query], or for the typed text. */
    fun search(query: String = _state.value.query) {
        val q = query.trim()
        if (q.isEmpty()) return
        searchJob?.cancel()
        suggestJob?.cancel()
        _state.update { it.copy(query = q, searched = q, loading = true, error = null, results = emptyList(), suggestions = emptyList()) }
        searchJob = viewModelScope.launch {
            runCatching { youTube.search(q) }
                .onSuccess { videos -> _state.update { it.copy(loading = false, results = videos) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: e.toString()) } }
        }
    }

    /** The mic button: starts listening, or finishes when already listening. */
    fun toggleVoice() = voice.toggle()

    /** The microphone permission was refused. */
    fun voiceDenied() = voice.denied()

    /** Stops listening and clears any message; the skin calls this when Search is left. */
    fun cancelVoice() = voice.cancel()

    override fun onCleared() {
        speech.cancel()
    }

    /** A result got focus; if it stays focused for a moment, start resolving its streams. */
    fun onFocus(video: Video) {
        focusJob?.cancel()
        focusJob = viewModelScope.launch {
            delay(FOCUS_PREFETCH_DELAY_MS)
            playback.prefetch(video.id)
        }
    }

    private companion object {
        const val SUGGEST_DELAY_MS = 250L
        const val FOCUS_PREFETCH_DELAY_MS = 800L
    }
}
