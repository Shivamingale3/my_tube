package io.github.shivamingale3.mytube.screens

import io.github.shivamingale3.mytube.model.UserAccount

/** Everything a skin needs to draw Settings. */
data class SettingsUiState(
    val accounts: List<UserAccount> = emptyList(),
    val signIn: SignInStep = SignInStep.Idle,
    val preferences: Preferences = Preferences(),
    /** Skins to pick from, as id to display name. */
    val skins: List<Pair<String, String>> = emptyList(),
    val version: String = "",
    val update: UpdateStep = UpdateStep.Idle,
) {
    val account: UserAccount? get() = accounts.firstOrNull { it.isSelected } ?: accounts.firstOrNull()
}

sealed interface SignInStep {
    data object Idle : SignInStep
    data object Requesting : SignInStep
    /** Show [code]; the user enters it at [url] on a phone or computer. */
    data class EnterCode(val code: String, val url: String) : SignInStep
    data class Failed(val message: String) : SignInStep
}

sealed interface UpdateStep {
    data object Idle : UpdateStep
    data object Checking : UpdateStep
    data object UpToDate : UpdateStep
    data class Available(val build: Int) : UpdateStep
    data class Downloading(val percent: Int) : UpdateStep
    /** The installer was opened. */
    data object Installing : UpdateStep
    data class Failed(val message: String) : UpdateStep
}
