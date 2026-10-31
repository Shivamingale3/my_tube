package io.github.shivamingale3.mytube.youtube

import android.content.Context
import com.liskovsoft.mediaserviceinterfaces.SignInService
import com.liskovsoft.mediaserviceinterfaces.oauth.Account
import io.github.shivamingale3.mytube.model.SignInProgress
import io.github.shivamingale3.mytube.model.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext

/** Google account sign-in through YouTube's TV device-code flow. MediaServiceCore stores the accounts. */
class YouTubeAccounts(context: Context) {
    private val signIn: SignInService by lazy { MediaServiceCore.services(context).signInService }

    /**
     * Starts a sign-in: emits the code to show, then [SignInProgress.SignedIn] once the user has
     * entered it. Cancelling the collector abandons the attempt; collecting again gets a new code.
     */
    fun signIn(): Flow<SignInProgress> = flow {
        signIn.signInObserve().asFlow().collect { code -> emit(SignInProgress.EnterCode(code, ACTIVATE_URL)) }
        emit(SignInProgress.SignedIn)
    }

    suspend fun accounts(): List<UserAccount> = withContext(Dispatchers.IO) {
        signIn.accounts.orEmpty().filterNot { it.isEmpty }.map { it.toUserAccount() }
    }

    suspend fun select(accountId: Int) = withContext(Dispatchers.IO) {
        signIn.accounts.orEmpty().firstOrNull { it.id == accountId }?.let { signIn.selectAccount(it) }
    }

    suspend fun signOut(accountId: Int) = withContext(Dispatchers.IO) {
        signIn.accounts.orEmpty().firstOrNull { it.id == accountId }?.let { signIn.removeAccount(it) }
    }

    private companion object {
        const val ACTIVATE_URL = "youtube.com/activate"
    }
}

private fun Account.toUserAccount() = UserAccount(
    id = id,
    name = name,
    email = email,
    avatarUrl = avatarImageUrl,
    isSelected = isSelected,
)
