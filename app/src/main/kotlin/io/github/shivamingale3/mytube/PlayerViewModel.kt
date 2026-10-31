package io.github.shivamingale3.mytube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.shivamingale3.mytube.model.PlaybackInfo
import io.github.shivamingale3.mytube.player.PlayerController
import io.github.shivamingale3.mytube.youtube.YouTubePlayback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Temporary player screen logic: resolves a video's ad-free streams and starts the shared player. */
class PlayerViewModel(
    private val playback: YouTubePlayback,
    val controller: PlayerController,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Playing(val info: PlaybackInfo) : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    fun open(videoId: String) {
        _state.value = State.Loading
        viewModelScope.launch {
            runCatching { playback.load(videoId) }
                .onSuccess { info ->
                    controller.play(info)
                    _state.value = State.Playing(info)
                }
                .onFailure { _state.value = State.Failed(it.message ?: it.toString()) }
        }
    }

    override fun onCleared() {
        controller.stop()
    }
}
