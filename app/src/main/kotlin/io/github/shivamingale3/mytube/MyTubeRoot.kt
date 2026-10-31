package io.github.shivamingale3.mytube

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.screens.HomeViewModel
import io.github.shivamingale3.mytube.skin.DesignScale
import io.github.shivamingale3.mytube.skin.HomeActions
import io.github.shivamingale3.mytube.skin.ShellActions
import io.github.shivamingale3.mytube.skin.ShellState
import io.github.shivamingale3.mytube.skin.Skin
import io.github.shivamingale3.mytube.skin.Tab
import io.github.shivamingale3.mytube.skin.dot.DotSkin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.androidx.compose.koinViewModel

private const val SPLASH_MIN_MS = 700L
private const val SPLASH_MAX_MS = 8_000L

/** Top-level navigation: splash, the active skin's frame and tabs, the player, and the account screen. */
@Composable
fun MyTubeRoot(skin: Skin = DotSkin) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var playing by remember { mutableStateOf<Video?>(null) }
    var accountOpen by rememberSaveable { mutableStateOf(false) }
    var splash by rememberSaveable { mutableStateOf(true) }
    val shell: ShellViewModel = koinViewModel()
    val home: HomeViewModel = koinViewModel()
    val account by shell.account.collectAsStateWithLifecycle()
    val homeState by home.state.collectAsStateWithLifecycle()

    if (splash) {
        LaunchedEffect(Unit) {
            delay(SPLASH_MIN_MS)
            withTimeoutOrNull(SPLASH_MAX_MS - SPLASH_MIN_MS) {
                snapshotFlow { homeState.featured != null || homeState.error != null }.first { it }
            }
            splash = false
        }
        Splash()
        return
    }

    skin.Theme {
        val video = playing
        when {
            video != null -> PlayerRoute(video, skin, onBack = { playing = null })
            accountOpen -> {
                BackHandler {
                    accountOpen = false
                    shell.refresh(onChanged = home::refresh)
                }
                DiagnosticsScreen(onPlay = { id -> playing = Video(id, "", null, null, null, 0, null, 0, isLive = false, isShorts = false) })
            }
            else -> {
                BackHandler(enabled = tab != Tab.Home) { tab = Tab.Home }
                DesignScale {
                    skin.Shell(
                        state = ShellState(tab, account),
                        actions = ShellActions(selectTab = { tab = it }, openAccount = { accountOpen = true }),
                    ) {
                        when (tab) {
                            Tab.Home -> skin.Home(
                                homeState,
                                HomeActions(
                                    play = { playing = it },
                                    openSubscriptions = { tab = Tab.Subscriptions },
                                    openAccount = { accountOpen = true },
                                    focus = home::onFocus,
                                ),
                            )
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
