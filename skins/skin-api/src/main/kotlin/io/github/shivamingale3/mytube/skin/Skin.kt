package io.github.shivamingale3.mytube.skin

import androidx.compose.runtime.Composable
import io.github.shivamingale3.mytube.model.UserAccount
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.player.CaptionChoice
import io.github.shivamingale3.mytube.player.VideoQuality
import io.github.shivamingale3.mytube.screens.HomeUiState
import io.github.shivamingale3.mytube.screens.PlayerUiState

/** Top-level destinations every skin offers in its navigation. */
enum class Tab { Home, Subscriptions, Library, Search }

data class ShellState(
    val tab: Tab,
    val account: UserAccount?,
)

class ShellActions(
    val selectTab: (Tab) -> Unit,
    val openAccount: () -> Unit,
)

class HomeActions(
    val play: (Video) -> Unit,
    val openSubscriptions: () -> Unit,
    val openAccount: () -> Unit,
    /** A video card got focus; used to start loading it ahead of a press. */
    val focus: (Video) -> Unit = {},
)

class PlayerActions(
    val togglePlay: () -> Unit,
    val seekBy: (deltaMs: Long) -> Unit,
    val skipSegment: () -> Unit,
    val setQuality: (VideoQuality?) -> Unit,
    val setCaption: (CaptionChoice?) -> Unit,
    val setSpeed: (Float) -> Unit,
    val retry: () -> Unit,
    /** The stats panel opened (true) or closed; stats are gathered only while it's open. */
    val showStats: (Boolean) -> Unit = {},
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

    /** The app frame around every tab: navigation, account entry and the tab's [content]. */
    @Composable
    fun Shell(state: ShellState, actions: ShellActions, content: @Composable () -> Unit)

    @Composable
    fun Home(state: HomeUiState, actions: HomeActions)

    /**
     * The full-screen player. [surface] draws the video itself; the skin places it and draws
     * loading, controls, settings and the skip button around it. Back exits unless the skin
     * consumes it first (for example to close a menu or hide the controls).
     */
    @Composable
    fun Player(state: PlayerUiState, actions: PlayerActions, surface: @Composable () -> Unit)
}
