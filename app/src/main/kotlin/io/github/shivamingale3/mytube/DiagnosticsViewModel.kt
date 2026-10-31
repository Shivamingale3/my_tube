package io.github.shivamingale3.mytube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.shivamingale3.mytube.model.HomeFeed
import io.github.shivamingale3.mytube.model.SignInProgress
import io.github.shivamingale3.mytube.model.UserAccount
import io.github.shivamingale3.mytube.youtube.YouTubeAccounts
import io.github.shivamingale3.mytube.youtube.YouTubeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Temporary screen logic that proves the data layer and sign-in run on the device. */
class DiagnosticsViewModel(
    private val youTube: YouTubeSource,
    private val accounts: YouTubeAccounts,
) : ViewModel() {

    sealed interface State {
        data object Idle : State
        data object Loading : State
        data class Loaded(val feed: HomeFeed) : State
        data class Failed(val message: String) : State
    }

    sealed interface SignInState {
        data object Idle : SignInState
        data object Requesting : SignInState
        data class Waiting(val code: String, val url: String) : SignInState
        data class Failed(val message: String) : SignInState
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _signIn = MutableStateFlow<SignInState>(SignInState.Idle)
    val signIn: StateFlow<SignInState> = _signIn.asStateFlow()

    private val _accounts = MutableStateFlow<List<UserAccount>>(emptyList())
    val accountList: StateFlow<List<UserAccount>> = _accounts.asStateFlow()

    private var signInJob: Job? = null

    init {
        refreshAccounts()
    }

    fun loadHome() {
        _state.value = State.Loading
        viewModelScope.launch {
            _state.value = runCatching { youTube.home() }
                .fold(
                    onSuccess = { State.Loaded(it) },
                    onFailure = { State.Failed(it.toString()) },
                )
        }
    }

    fun startSignIn() {
        signInJob?.cancel()
        _signIn.value = SignInState.Requesting
        signInJob = viewModelScope.launch {
            accounts.signIn()
                .catch { _signIn.value = SignInState.Failed(it.message ?: it.toString()) }
                .collect { progress ->
                    when (progress) {
                        is SignInProgress.EnterCode -> _signIn.value = SignInState.Waiting(progress.code, progress.url)
                        SignInProgress.SignedIn -> {
                            _signIn.value = SignInState.Idle
                            refreshAccounts()
                            loadHome()
                        }
                    }
                }
        }
    }

    fun cancelSignIn() {
        signInJob?.cancel()
        _signIn.value = SignInState.Idle
    }

    fun signOut(account: UserAccount) {
        viewModelScope.launch {
            accounts.signOut(account.id)
            refreshAccounts()
            loadHome()
        }
    }

    private fun refreshAccounts() {
        viewModelScope.launch { _accounts.value = runCatching { accounts.accounts() }.getOrDefault(emptyList()) }
    }
}
