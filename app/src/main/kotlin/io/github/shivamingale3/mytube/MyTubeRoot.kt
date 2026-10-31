package io.github.shivamingale3.mytube

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import io.github.shivamingale3.mytube.screens.HomeViewModel
import io.github.shivamingale3.mytube.skin.DesignScale
import io.github.shivamingale3.mytube.skin.HomeActions
import io.github.shivamingale3.mytube.skin.ShellActions
import io.github.shivamingale3.mytube.skin.ShellState
import io.github.shivamingale3.mytube.skin.Skin
import io.github.shivamingale3.mytube.skin.Tab
import io.github.shivamingale3.mytube.skin.dot.DotSkin
import org.koin.androidx.compose.koinViewModel

/** Top-level navigation: the active skin's frame and tabs, the player, and the account screen. */
@Composable
fun MyTubeRoot(skin: Skin = DotSkin) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var playing by rememberSaveable { mutableStateOf<String?>(null) }
    var accountOpen by rememberSaveable { mutableStateOf(false) }
    val shell: ShellViewModel = koinViewModel()
    val home: HomeViewModel = koinViewModel()
    val account by shell.account.collectAsStateWithLifecycle()

    skin.Theme {
        val videoId = playing
        when {
            videoId != null -> PlayerScreen(videoId, onBack = { playing = null })
            accountOpen -> {
                BackHandler {
                    accountOpen = false
                    shell.refresh(onChanged = home::refresh)
                }
                DiagnosticsScreen(onPlay = { playing = it })
            }
            else -> {
                BackHandler(enabled = tab != Tab.Home) { tab = Tab.Home }
                DesignScale {
                    skin.Shell(
                        state = ShellState(tab, account),
                        actions = ShellActions(selectTab = { tab = it }, openAccount = { accountOpen = true }),
                    ) {
                        when (tab) {
                            Tab.Home -> {
                                val state by home.state.collectAsStateWithLifecycle()
                                skin.Home(
                                    state,
                                    HomeActions(
                                        play = { playing = it.id },
                                        openSubscriptions = { tab = Tab.Subscriptions },
                                        openAccount = { accountOpen = true },
                                    ),
                                )
                            }
                            else -> ComingNext(tab)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComingNext(tab: Tab) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("${tab.name.uppercase()} · COMING NEXT", color = Color(0xFF7A7A7A), fontSize = 16.sp)
    }
}
