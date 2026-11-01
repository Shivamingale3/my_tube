package io.github.shivamingale3.mytube.screens

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The user's choices, saved on the device. Every screen reads them from here. */
data class Preferences(
    /** Skin id, e.g. "dot" or "tonal". */
    val skin: String = "dot",
    /** Accent colour as ARGB. */
    val accent: Long = ACCENTS.first(),
    /** Look up SponsorBlock segments and offer to skip them. */
    val sponsorBlock: Boolean = true,
    /** Skip sponsor segments without asking. */
    val autoSkip: Boolean = false,
    /** Start videos where you stopped watching. */
    val resume: Boolean = true,
    /** Show the STATS button in the player. */
    val statsButton: Boolean = true,
) {
    companion object {
        /** The accent choices offered in Settings. */
        val ACCENTS = listOf(0xFFFF2E4D, 0xFFFF7A1A, 0xFF3D8BFF, 0xFF00D26A, 0xFFFFFFFF)
    }
}

class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("mytube.settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<Preferences> = _state.asStateFlow()

    val current: Preferences get() = _state.value

    fun update(change: (Preferences) -> Preferences) {
        val next = change(_state.value)
        _state.value = next
        prefs.edit()
            .putString("skin", next.skin)
            .putLong("accent", next.accent)
            .putBoolean("sponsorBlock", next.sponsorBlock)
            .putBoolean("autoSkip", next.autoSkip)
            .putBoolean("resume", next.resume)
            .putBoolean("statsButton", next.statsButton)
            .apply()
    }

    private fun read(): Preferences {
        val d = Preferences()
        return Preferences(
            skin = prefs.getString("skin", d.skin) ?: d.skin,
            accent = prefs.getLong("accent", d.accent),
            sponsorBlock = prefs.getBoolean("sponsorBlock", d.sponsorBlock),
            autoSkip = prefs.getBoolean("autoSkip", d.autoSkip),
            resume = prefs.getBoolean("resume", d.resume),
            statsButton = prefs.getBoolean("statsButton", d.statsButton),
        )
    }
}
