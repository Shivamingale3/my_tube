package io.github.shivamingale3.mytube.screens

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSearchTest {
    private class FakeSpeech(var available: Boolean = true) : SpeechInput {
        var events: SpeechInput.Events? = null
        var stops = 0
        var cancels = 0
        override fun isAvailable() = available
        override fun start(events: SpeechInput.Events) { this.events = events }
        override fun stop() { stops++ }
        override fun cancel() { cancels++ }
    }

    private fun TestScopeVoice(speech: SpeechInput, scope: kotlinx.coroutines.CoroutineScope, heard: MutableList<String>) =
        VoiceSearch(speech, scope) { heard += it }

    @Test
    fun `live words and level are shown while listening, the final text goes to the field`() = runTest {
        val speech = FakeSpeech()
        val heard = mutableListOf<String>()
        val voice = TestScopeVoice(speech, this, heard)

        voice.toggle()
        assertEquals(VoiceState.Listening(), voice.state.value)

        speech.events!!.onLevel(3)
        speech.events!!.onPartial("how to")
        assertEquals(VoiceState.Listening(heard = "how to", level = 3), voice.state.value)

        speech.events!!.onResult(" how to cook rice ")
        assertEquals(VoiceState.Idle, voice.state.value)
        assertEquals(listOf("how to cook rice"), heard)
    }

    @Test
    fun `pressing the mic again finishes listening instead of starting over`() = runTest {
        val speech = FakeSpeech()
        val voice = TestScopeVoice(speech, this, mutableListOf())
        voice.toggle()
        voice.toggle()
        assertEquals(1, speech.stops)
        assertTrue(voice.state.value is VoiceState.Listening) // the result arrives from the recognizer
    }

    @Test
    fun `nothing heard shows a message that clears by itself`() = runTest {
        val speech = FakeSpeech()
        val heard = mutableListOf<String>()
        val voice = TestScopeVoice(speech, this, heard)
        voice.toggle()
        speech.events!!.onError(SpeechInput.Failure.NothingHeard)
        assertEquals(VoiceState.Failed(SpeechInput.Failure.NothingHeard.message), voice.state.value)
        advanceTimeBy(4_000)
        assertEquals(VoiceState.Idle, voice.state.value)
        assertTrue(heard.isEmpty())
    }

    @Test
    fun `an empty final result counts as nothing heard`() = runTest {
        val speech = FakeSpeech()
        val voice = TestScopeVoice(speech, this, mutableListOf())
        voice.toggle()
        speech.events!!.onResult("  ")
        assertEquals(VoiceState.Failed(SpeechInput.Failure.NothingHeard.message), voice.state.value)
    }

    @Test
    fun `no recognizer on the TV gives a clear message and never starts listening`() = runTest {
        val speech = FakeSpeech(available = false)
        val voice = TestScopeVoice(speech, this, mutableListOf())
        voice.toggle()
        assertEquals(VoiceState.Failed(SpeechInput.Failure.Unavailable.message), voice.state.value)
        assertEquals(null, speech.events)
    }

    @Test
    fun `cancel stops listening, and late events are ignored`() = runTest {
        val speech = FakeSpeech()
        val heard = mutableListOf<String>()
        val voice = TestScopeVoice(speech, this, heard)
        voice.toggle()
        val events = speech.events!!
        voice.cancel()
        assertEquals(VoiceState.Idle, voice.state.value)
        events.onPartial("late")
        events.onResult("late")
        assertEquals(VoiceState.Idle, voice.state.value)
        assertTrue(heard.isEmpty())
    }

    @Test
    fun `a refused permission shows a message`() = runTest {
        val voice = TestScopeVoice(FakeSpeech(), this, mutableListOf())
        voice.denied()
        assertEquals(VoiceState.Failed(SpeechInput.Failure.NoPermission.message), voice.state.value)
    }
}
