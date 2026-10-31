package io.github.shivamingale3.mytube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.shivamingale3.mytube.model.UserAccount
import io.github.shivamingale3.mytube.youtube.YouTubeAccounts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** App-frame state shared by every skin: who is signed in. */
class ShellViewModel(private val accounts: YouTubeAccounts) : ViewModel() {
    private val _account = MutableStateFlow<UserAccount?>(null)
    val account: StateFlow<UserAccount?> = _account.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads the selected account; returns true if it changed. */
    fun refresh(onChanged: () -> Unit = {}) {
        viewModelScope.launch {
            val list = runCatching { accounts.accounts() }.getOrDefault(emptyList())
            val current = list.firstOrNull { it.isSelected } ?: list.firstOrNull()
            if (current?.id != _account.value?.id) {
                _account.value = current
                onChanged()
            } else {
                _account.value = current
            }
        }
    }
}
