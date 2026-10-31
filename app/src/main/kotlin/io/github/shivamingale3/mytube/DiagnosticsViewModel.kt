package io.github.shivamingale3.mytube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.shivamingale3.mytube.model.VideoRow
import io.github.shivamingale3.mytube.youtube.YouTubeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Temporary step-1 screen logic: proves the YouTube data layer runs on the device. */
class DiagnosticsViewModel(private val youTube: YouTubeSource) : ViewModel() {

    sealed interface State {
        data object Idle : State
        data object Loading : State
        data class Loaded(val rows: List<VideoRow>) : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun loadHome() {
        _state.value = State.Loading
        viewModelScope.launch {
            _state.value = runCatching { youTube.home() }
                .fold(
                    onSuccess = { State.Loaded(it) },
                    onFailure = { State.Failed(it.toString()) },
                )
        }
    }
}
