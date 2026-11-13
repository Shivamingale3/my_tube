package io.github.shivamingale3.mytube.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlin.math.roundToInt

/** Speech to text, behind an interface so the voice logic can be tested without a microphone. */
interface SpeechInput {
    /** False when the TV has no speech recognition service installed. */
    fun isAvailable(): Boolean

    /** Starts listening; [events] are called on the main thread. */
    fun start(events: Events)

    /** Stops listening and delivers what was heard so far as the final result. */
    fun stop()

    /** Drops the current session without a result. */
    fun cancel()

    interface Events {
        /** Words heard so far, while the person is still speaking. */
        fun onPartial(text: String)

        /** Microphone loudness from 0 (quiet) to 4 (loud). */
        fun onLevel(level: Int)

        fun onResult(text: String)

        fun onError(failure: Failure)
    }

    enum class Failure(val message: String) {
        NothingHeard("Didn't catch that. Try again."),
        NoPermission("Allow the microphone to use voice search."),
        Network("Voice search needs an internet connection."),
        Busy("Voice search is busy. Try again."),
        Unavailable("Voice search isn't available on this TV."),
        Other("Voice search failed. Try again."),
    }
}

/**
 * Android's [SpeechRecognizer] run inside the app, so listening and live words are drawn by our
 * own screens rather than a system dialog. Uses the TV's language.
 */
class AndroidSpeechInput(private val context: Context) : SpeechInput {
    private var recognizer: SpeechRecognizer? = null

    override fun isAvailable() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(events: SpeechInput.Events) {
        cancel()
        val r = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        if (r == null) {
            events.onError(SpeechInput.Failure.Unavailable)
            return
        }
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            // rmsdB runs from about -2 (silence) to 10 (loud); five steps are enough to animate.
            override fun onRmsChanged(rmsdB: Float) = events.onLevel(((rmsdB + 2f) / 12f * 4f).roundToInt().coerceIn(0, 4))

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults.firstText()?.let(events::onPartial)
            }

            override fun onResults(results: Bundle?) {
                release(r)
                events.onResult(results.firstText().orEmpty())
            }

            override fun onError(error: Int) {
                release(r)
                events.onError(failureFor(error))
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        runCatching { r.startListening(intent) }.onFailure {
            release(r)
            events.onError(SpeechInput.Failure.Other)
        }
    }

    override fun stop() {
        recognizer?.stopListening()
    }

    override fun cancel() {
        recognizer?.let { r ->
            recognizer = null
            runCatching { r.cancel() }
            runCatching { r.destroy() }
        }
    }

    private fun release(r: SpeechRecognizer) {
        if (recognizer === r) recognizer = null
        runCatching { r.destroy() }
    }

    private fun Bundle?.firstText(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }

    private fun failureFor(error: Int) = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechInput.Failure.NothingHeard
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechInput.Failure.NoPermission
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechInput.Failure.Network
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechInput.Failure.Busy
        else -> SpeechInput.Failure.Other
    }
}
