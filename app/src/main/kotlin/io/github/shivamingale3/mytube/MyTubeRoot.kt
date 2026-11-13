package io.github.shivamingale3.mytube

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import io.github.shivamingale3.mytube.skin.FocusRestore
import io.github.shivamingale3.mytube.skin.LocalFocusRestore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.screens.AppSettings
import io.github.shivamingale3.mytube.screens.HomeViewModel
import io.github.shivamingale3.mytube.screens.SearchViewModel
import io.github.shivamingale3.mytube.skin.DesignScale
import io.github.shivamingale3.mytube.skin.HomeActions
import io.github.shivamingale3.mytube.skin.SearchActions
import io.github.shivamingale3.mytube.skin.SettingsActions
import io.github.shivamingale3.mytube.skin.ShellActions
import io.github.shivamingale3.mytube.skin.ShellState
import io.github.shivamingale3.mytube.skin.SkinAccent
import io.github.shivamingale3.mytube.skin.Tab
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val SPLASH_MS = 700L

/** Top-level navigation: splash, the active skin's frame with Home and Search, Settings and the player. */
@Composable
fun MyTubeRoot() {
    val settings: AppSettings = koinInject()
    val prefs by settings.state.collectAsStateWithLifecycle()
    val skin = Skins.byId(prefs.skin)
    SideEffect { SkinAccent.color = Color(prefs.accent) }

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var playing by remember { mutableStateOf<Video?>(null) }
    // The video played from Home or Search; it gets focus back when the player closes.
    var playedFrom by remember { mutableStateOf<String?>(null) }
    // Keeps each tab's scroll positions while the player or Settings is open.
    val tabStates = rememberSaveableStateHolder()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var splash by rememberSaveable { mutableStateOf(true) }

    val shell: ShellViewModel = koinViewModel()
    val home: HomeViewModel = koinViewModel()
    val search: SearchViewModel = koinViewModel()
    val settingsModel: SettingsViewModel = koinViewModel()
    val account by shell.account.collectAsStateWithLifecycle()
    val homeState by home.state.collectAsStateWithLifecycle()
    val searchState by search.state.collectAsStateWithLifecycle()
    val settingsState by settingsModel.state.collectAsStateWithLifecycle()
    val accountVersion by settingsModel.accountVersion.collectAsStateWithLifecycle()

    LaunchedEffect(accountVersion) {
        if (accountVersion > 0) shell.refresh(onChanged = home::refresh)
    }

    val voice = rememberVoiceButton(onGranted = search::toggleVoice, onDenied = search::voiceDenied)

    if (splash) {
        // Just long enough to read; Home then shows its own skeleton until the feed arrives.
        LaunchedEffect(Unit) {
            delay(SPLASH_MS)
            splash = false
        }
        Splash()
        return
    }

    skin.Theme {
        val video = playing
        when {
            video != null -> PlayerRoute(video, skin, onBack = { playing = null })
            settingsOpen -> {
                BackHandler { settingsOpen = false }
                LaunchedEffect(Unit) { settingsModel.checkUpdate() }
                DesignScale {
                    skin.Settings(
                        settingsState,
                        SettingsActions(
                            signIn = settingsModel::signIn,
                            cancelSignIn = settingsModel::cancelSignIn,
                            selectAccount = settingsModel::select,
                            signOut = settingsModel::signOut,
                            change = settingsModel::change,
                            checkUpdate = settingsModel::checkUpdate,
                            installUpdate = settingsModel::installUpdate,
                        ),
                    )
                }
            }
            else -> {
                BackHandler(enabled = tab != Tab.Home) { tab = Tab.Home }
                val play: (Video) -> Unit = { playedFrom = it.id; playing = it }
                CompositionLocalProvider(LocalFocusRestore provides FocusRestore(playedFrom) { playedFrom = null }) {
                DesignScale {
                    skin.Shell(
                        state = ShellState(tab, account),
                        actions = ShellActions(selectTab = { playedFrom = null; tab = it }, openSettings = { settingsOpen = true }),
                    ) {
                        when (tab) {
                            Tab.Home -> tabStates.SaveableStateProvider("home") { skin.Home(
                                homeState,
                                HomeActions(play = play, openSettings = { settingsOpen = true }, focus = home::onFocus),
                            ) }
                            Tab.Search -> tabStates.SaveableStateProvider("search") { skin.Search(
                                searchState,
                                SearchActions(
                                    type = search::type,
                                    search = search::search,
                                    play = play,
                                    focus = search::onFocus,
                                    voice = voice,
                                    cancelVoice = search::cancelVoice,
                                ),
                            ) }
                        }
                    }
                }
                }
            }
        }
    }
}

/**
 * The mic button's action: listen right away when the microphone is allowed, otherwise ask once
 * (Android shows its own dialog) and listen if the answer is yes.
 */
@Composable
private fun rememberVoiceButton(onGranted: () -> Unit, onDenied: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onGranted() else onDenied()
    }
    return {
        val allowed = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (allowed) onGranted() else runCatching { launcher.launch(Manifest.permission.RECORD_AUDIO) }.onFailure { onDenied() }
    }
}
