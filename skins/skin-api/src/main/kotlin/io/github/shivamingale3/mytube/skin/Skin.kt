package io.github.shivamingale3.mytube.skin

import androidx.compose.runtime.Composable
import io.github.shivamingale3.mytube.model.UserAccount
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.screens.HomeUiState

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
}
