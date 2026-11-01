package io.github.shivamingale3.mytube.skin.dot

import androidx.compose.runtime.Composable
import io.github.shivamingale3.mytube.screens.HomeUiState
import io.github.shivamingale3.mytube.screens.PlayerUiState
import io.github.shivamingale3.mytube.screens.SearchUiState
import io.github.shivamingale3.mytube.screens.SettingsUiState
import io.github.shivamingale3.mytube.skin.SearchActions
import io.github.shivamingale3.mytube.skin.SettingsActions
import io.github.shivamingale3.mytube.skin.HomeActions
import io.github.shivamingale3.mytube.skin.PlayerActions
import io.github.shivamingale3.mytube.skin.ShellActions
import io.github.shivamingale3.mytube.skin.ShellState
import io.github.shivamingale3.mytube.skin.Skin

/** Dot: dot-matrix type, black and white, thumbnails under a dot screen until focused. The default skin. */
object DotSkin : Skin {
    override val id = "dot"
    override val displayName = "Dot"

    @Composable
    override fun Theme(content: @Composable () -> Unit) = DotMaterialTheme(content)

    @Composable
    override fun Shell(state: ShellState, actions: ShellActions, content: @Composable () -> Unit) =
        DotShell(state, actions, content)

    @Composable
    override fun Home(state: HomeUiState, actions: HomeActions) = DotHome(state, actions)

    @Composable
    override fun Search(state: SearchUiState, actions: SearchActions) = DotSearch(state, actions)

    @Composable
    override fun Settings(state: SettingsUiState, actions: SettingsActions) = DotSettings(state, actions)

    @Composable
    override fun Player(state: PlayerUiState, actions: PlayerActions, surface: @Composable () -> Unit) =
        DotPlayer(state, actions, surface)
}
