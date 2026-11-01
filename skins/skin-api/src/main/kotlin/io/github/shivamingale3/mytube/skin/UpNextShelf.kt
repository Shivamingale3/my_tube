package io.github.shivamingale3.mytube.skin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The player's "Up next" shelf: pressing down from the control buttons opens a row of related
 * videos under them; Back closes it. Shared by both skins.
 */
class UpNextShelf {
    var open by mutableStateOf(false)

    /** Focus is on the control buttons, so down should open the shelf. */
    var buttonsFocused by mutableStateOf(false)
}
