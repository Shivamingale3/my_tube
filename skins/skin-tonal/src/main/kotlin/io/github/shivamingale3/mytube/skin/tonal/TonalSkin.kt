package io.github.shivamingale3.mytube.skin.tonal

import androidx.compose.runtime.Composable
import io.github.shivamingale3.mytube.screens.HomeUiState
import io.github.shivamingale3.mytube.screens.PlayerUiState
import io.github.shivamingale3.mytube.screens.SearchUiState
import io.github.shivamingale3.mytube.screens.SettingsUiState
import io.github.shivamingale3.mytube.skin.HomeActions
import io.github.shivamingale3.mytube.skin.PlayerActions
import io.github.shivamingale3.mytube.skin.SearchActions
import io.github.shivamingale3.mytube.skin.SettingsActions
import io.github.shivamingale3.mytube.skin.ShellActions
import io.github.shivamingale3.mytube.skin.ShellState
import io.github.shivamingale3.mytube.skin.Skin

/** Tonal: black, white pill buttons, Lexend, rounded everything and a wavy progress bar. */
object TonalSkin : Skin {
    override val id = "tonal"
    override val displayName = "Tonal"

    @Composable
    override fun Theme(content: @Composable () -> Unit) = TonalMaterialTheme(content)

    @Composable
    override fun Shell(state: ShellState, actions: ShellActions, content: @Composable () -> Unit) = TonalShell(state, actions, content)

    @Composable
    override fun Home(state: HomeUiState, actions: HomeActions) = TonalHome(state, actions)

    @Composable
    override fun Search(state: SearchUiState, actions: SearchActions) = TonalSearch(state, actions)

    @Composable
    override fun Settings(state: SettingsUiState, actions: SettingsActions) = TonalSettings(state, actions)

    @Composable
    override fun Player(state: PlayerUiState, actions: PlayerActions, surface: @Composable () -> Unit) = TonalPlayer(state, actions, surface)
}
