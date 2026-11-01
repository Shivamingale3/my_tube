package io.github.shivamingale3.mytube

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val SPLASH_MIN_MS = 700L
private const val SPLASH_MAX_MS = 8_000L

/** Top-level navigation: splash, the active skin's frame with Home and Search, Settings and the player. */
@Composable
fun MyTubeRoot() {
    val settings: AppSettings = koinInject()
    val prefs by settings.state.collectAsStateWithLifecycle()
    val skin = Skins.byId(prefs.skin)
    SideEffect { SkinAccent.color = Color(prefs.accent) }

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var playing by remember { mutableStateOf<Video?>(null) }
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

    val voice = rememberVoiceSearch { spoken -> tab = Tab.Search; search.search(spoken) }

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
                DesignScale {
                    skin.Shell(
                        state = ShellState(tab, account),
                        actions = ShellActions(selectTab = { tab = it }, openSettings = { settingsOpen = true }),
                    ) {
                        when (tab) {
                            Tab.Home -> skin.Home(
                                homeState,
                                HomeActions(play = { playing = it }, openSettings = { settingsOpen = true }, focus = home::onFocus),
                            )
                            Tab.Search -> skin.Search(
                                searchState,
                                SearchActions(
                                    type = search::type,
                                    search = search::search,
                                    play = { playing = it },
                                    focus = search::onFocus,
                                    voice = voice,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Android's speech recognizer as a one-tap action, or null when the TV has none. */
@Composable
private fun rememberVoiceSearch(onResult: (String) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Search YouTube")
    }
    val available = remember { context.packageManager.queryIntentActivities(intent, 0).isNotEmpty() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let(onResult)
        }
    }
    return if (available) ({ runCatching { launcher.launch(intent) } }) else null
}
