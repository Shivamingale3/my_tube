package io.github.shivamingale3.mytube.screens

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the voice button is doing, for the skins to draw. */
sealed interface VoiceState {
    data object Idle : VoiceState

    /** [heard] is the words so far; [level] is the microphone loudness, 0..4. */
    data class Listening(val heard: String = "", val level: Int = 0) : VoiceState

    /** Shown for a few seconds, then back to [Idle]. */
    data class Failed(val message: String) : VoiceState
}

/**
 * Voice input for the search field. Pressing the mic starts listening, pressing it again stops;
 * what was heard goes to [onHeard], which searches for it straight away.
 */
class VoiceSearch(
    private val speech: SpeechInput,
    private val scope: CoroutineScope,
    private val onHeard: (String) -> Unit,
) {
    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private var clearJob: Job? = null

    /** The mic was pressed: start listening, or finish if already listening. */
    fun toggle() {
        if (_state.value is VoiceState.Listening) {
            speech.stop()
            return
        }
        clearJob?.cancel()
        if (!speech.isAvailable()) {
            fail(SpeechInput.Failure.Unavailable)
            return
        }
        _state.value = VoiceState.Listening()
        speech.start(object : SpeechInput.Events {
            override fun onPartial(text: String) {
                _state.update<VoiceState.Listening> { it.copy(heard = text) }
            }

            override fun onLevel(level: Int) {
                _state.update<VoiceState.Listening> { if (it.level == level) it else it.copy(level = level) }
            }

            override fun onResult(text: String) {
                if (_state.value !is VoiceState.Listening) return
                _state.value = VoiceState.Idle
                if (text.isBlank()) fail(SpeechInput.Failure.NothingHeard) else onHeard(text.trim())
            }

            override fun onError(failure: SpeechInput.Failure) {
                if (_state.value is VoiceState.Listening) fail(failure)
            }
        })
    }

    /** The microphone permission was refused. */
    fun denied() = fail(SpeechInput.Failure.NoPermission)

    /** Stops listening and clears any message, e.g. when the person types or leaves Search. */
    fun cancel() {
        if (_state.value == VoiceState.Idle) return
        speech.cancel()
        clearJob?.cancel()
        _state.value = VoiceState.Idle
    }

    private fun fail(failure: SpeechInput.Failure) {
        speech.cancel()
        _state.value = VoiceState.Failed(failure.message)
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(FAILURE_SHOWN_MS)
            if (_state.value is VoiceState.Failed) _state.value = VoiceState.Idle
        }
    }

    /** Updates the state only while it is of type [T]; events can arrive after listening ended. */
    private inline fun <reified T : VoiceState> MutableStateFlow<VoiceState>.update(change: (T) -> VoiceState) {
        val s = value
        if (s is T) value = change(s)
    }

    private companion object {
        const val FAILURE_SHOWN_MS = 3_500L
    }
}
