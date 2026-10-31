package io.github.shivamingale3.mytube.skin.dot

import androidx.compose.runtime.Composable
import io.github.shivamingale3.mytube.screens.HomeUiState
import io.github.shivamingale3.mytube.skin.HomeActions
import io.github.shivamingale3.mytube.skin.ShellActions
import io.github.shivamingale3.mytube.skin.ShellState
import io.github.shivamingale3.mytube.skin.Skin

/** Dot: dot-matrix type, black and white, thumbnails grey until focused. The default skin. */
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
}
