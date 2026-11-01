package io.github.shivamingale3.mytube.skin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import io.github.shivamingale3.mytube.model.UserAccount
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.player.CaptionChoice
import io.github.shivamingale3.mytube.player.VideoQuality
import io.github.shivamingale3.mytube.screens.HomeUiState
import io.github.shivamingale3.mytube.screens.PlayerUiState
import io.github.shivamingale3.mytube.screens.Preferences
import io.github.shivamingale3.mytube.screens.SearchUiState
import io.github.shivamingale3.mytube.screens.SettingsUiState

/** Top-level destinations every skin offers in its navigation. Settings opens from the profile icon. */
enum class Tab { Home, Search }

/** The accent colour picked in Settings. Skins read it while drawing, so a change shows at once. */
object SkinAccent {
    var color by mutableStateOf(Color(Preferences.ACCENTS.first()))
}

data class ShellState(
    val tab: Tab,
    val account: UserAccount?,
)

class ShellActions(
    val selectTab: (Tab) -> Unit,
    val openSettings: () -> Unit,
)

class HomeActions(
    val play: (Video) -> Unit,
    val openSettings: () -> Unit,
    /** A video card got focus; used to start loading it ahead of a press. */
    val focus: (Video) -> Unit = {},
)

class SearchActions(
    val type: (String) -> Unit,
    /** Search for the given text (a suggestion, or the typed query). */
    val search: (String) -> Unit,
    val play: (Video) -> Unit,
    val focus: (Video) -> Unit = {},
    /** Starts voice input; null when the TV has no speech recognizer. */
    val voice: (() -> Unit)? = null,
)

class SettingsActions(
    val signIn: () -> Unit,
    val cancelSignIn: () -> Unit,
    val selectAccount: (UserAccount) -> Unit,
    val signOut: (UserAccount) -> Unit,
    val change: ((Preferences) -> Preferences) -> Unit,
    val checkUpdate: () -> Unit,
    val installUpdate: () -> Unit,
)

class PlayerActions(
    val togglePlay: () -> Unit,
    val seekBy: (deltaMs: Long) -> Unit,
    val seekTo: (positionMs: Long) -> Unit,
    val skipSegment: () -> Unit,
    val setQuality: (VideoQuality?) -> Unit,
    val setCaption: (CaptionChoice?) -> Unit,
    val setSpeed: (Float) -> Unit,
    val retry: () -> Unit,
    /** The stats panel opened (true) or closed; stats are gathered only while it's open. */
    val showStats: (Boolean) -> Unit = {},
    /** Plays another video in the player (Up next, a related video). */
    val play: (Video) -> Unit = {},
    val replay: () -> Unit = {},
    /** Cycles audio-only mode: video → thumbnail → black → video. */
    val cycleAudioMode: () -> Unit = {},
)

/**
 * A complete look for the app. Skins only draw: screen state and behaviour come from the shared
 * view models, so every skin behaves identically and a new skin only needs new layouts.
 * Skins are drawn inside [DesignScale], so 1 dp equals 1 px of the 1280×720 designs.
 */
interface Skin {
    val id: String
    val displayName: String

    @Composable
    fun Theme(content: @Composable () -> Unit)

    /** The app frame around every tab: navigation, the profile icon and the tab's [content]. */
    @Composable
    fun Shell(state: ShellState, actions: ShellActions, content: @Composable () -> Unit)

    @Composable
    fun Home(state: HomeUiState, actions: HomeActions)

    @Composable
    fun Search(state: SearchUiState, actions: SearchActions)

    /** Full screen, opened from the profile icon; Back leaves it. */
    @Composable
    fun Settings(state: SettingsUiState, actions: SettingsActions)

    /**
     * The full-screen player. [surface] draws the video itself; the skin places it and draws
     * loading, controls, settings and the skip button around it. Back exits unless the skin
     * consumes it first (for example to close a menu or hide the controls).
     */
    @Composable
    fun Player(state: PlayerUiState, actions: PlayerActions, surface: @Composable () -> Unit)
}
