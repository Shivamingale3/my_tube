package io.github.shivamingale3.mytube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.shivamingale3.mytube.model.SignInProgress
import io.github.shivamingale3.mytube.model.UserAccount
import io.github.shivamingale3.mytube.screens.AppSettings
import io.github.shivamingale3.mytube.screens.Preferences
import io.github.shivamingale3.mytube.screens.SettingsUiState
import io.github.shivamingale3.mytube.screens.SignInStep
import io.github.shivamingale3.mytube.screens.UpdateStep
import io.github.shivamingale3.mytube.youtube.YouTubeAccounts
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Accounts, preferences and updates for the Settings screen. [onAccountChanged] reloads the feeds. */
class SettingsViewModel(
    private val accounts: YouTubeAccounts,
    private val settings: AppSettings,
    private val updater: AppUpdater,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            preferences = settings.current,
            skins = Skins.all.map { it.id to it.displayName },
            version = "${BuildConfig.VERSION_NAME} · build ${BuildConfig.BUILD_NUMBER}",
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** Bumped whenever the signed-in account changes, so Home can reload. */
    private val _accountVersion = MutableStateFlow(0)
    val accountVersion: StateFlow<Int> = _accountVersion.asStateFlow()

    private var signInJob: Job? = null
    private var release: AppUpdater.Release? = null

    init {
        refreshAccounts()
        viewModelScope.launch { settings.state.collect { p -> _state.update { it.copy(preferences = p) } } }
    }

    fun signIn() {
        signInJob?.cancel()
        _state.update { it.copy(signIn = SignInStep.Requesting) }
        signInJob = viewModelScope.launch {
            accounts.signIn()
                .catch { e -> _state.update { it.copy(signIn = SignInStep.Failed(e.message ?: e.toString())) } }
                .collect { progress ->
                    when (progress) {
                        is SignInProgress.EnterCode -> _state.update { it.copy(signIn = SignInStep.EnterCode(progress.code, progress.url)) }
                        SignInProgress.SignedIn -> {
                            _state.update { it.copy(signIn = SignInStep.Idle) }
                            refreshAccounts(changed = true)
                        }
                    }
                }
        }
    }

    fun cancelSignIn() {
        signInJob?.cancel()
        _state.update { it.copy(signIn = SignInStep.Idle) }
    }

    fun select(account: UserAccount) {
        viewModelScope.launch {
            accounts.select(account.id)
            refreshAccounts(changed = true)
        }
    }

    fun signOut(account: UserAccount) {
        viewModelScope.launch {
            accounts.signOut(account.id)
            refreshAccounts(changed = true)
        }
    }

    fun change(edit: (Preferences) -> Preferences) = settings.update(edit)

    fun checkUpdate() {
        _state.update { it.copy(update = UpdateStep.Checking) }
        viewModelScope.launch {
            runCatching { updater.check() }
                .onSuccess { r ->
                    release = r
                    _state.update { it.copy(update = if (r == null) UpdateStep.UpToDate else UpdateStep.Available(r.build)) }
                }
                .onFailure { e -> _state.update { it.copy(update = UpdateStep.Failed(e.message ?: e.toString())) } }
        }
    }

    fun installUpdate() {
        val r = release ?: return checkUpdate()
        _state.update { it.copy(update = UpdateStep.Downloading(0)) }
        viewModelScope.launch {
            runCatching { updater.install(r) { p -> _state.update { it.copy(update = UpdateStep.Downloading(p)) } } }
                .onSuccess { _state.update { it.copy(update = UpdateStep.Installing) } }
                .onFailure { e -> _state.update { it.copy(update = UpdateStep.Failed(e.message ?: e.toString())) } }
        }
    }

    private fun refreshAccounts(changed: Boolean = false) {
        viewModelScope.launch {
            val list = runCatching { accounts.accounts() }.getOrDefault(emptyList())
            _state.update { it.copy(accounts = list) }
            if (changed) _accountVersion.update { it + 1 }
        }
    }
}
