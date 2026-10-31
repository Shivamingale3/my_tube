package io.github.shivamingale3.mytube.model

/** A signed-in YouTube account. Name, email and avatar can be missing right after sign-in. */
data class UserAccount(
    val id: Int,
    val name: String?,
    val email: String?,
    val avatarUrl: String?,
    val isSelected: Boolean,
)

/** Progress of the TV sign-in flow, where the user enters a code on another device. */
sealed interface SignInProgress {
    /** Show [code] and ask the user to enter it at [url] on their phone or computer. */
    data class EnterCode(val code: String, val url: String) : SignInProgress

    /** The code was accepted and the account is saved and selected. */
    data object SignedIn : SignInProgress
}
