package io.github.shivamingale3.mytube.skin.dot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import io.github.shivamingale3.mytube.skin.ShellActions
import io.github.shivamingale3.mytube.skin.ShellState
import io.github.shivamingale3.mytube.skin.Tab

/** Dot's frame: dot-matrix wordmark, mono tab labels with a red dot on the current one, account circle. */
@Composable
internal fun DotShell(state: ShellState, actions: ShellActions, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().dotGrid()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("MYTUBE", style = DotType.Wordmark, color = DotColors.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Tab.entries.forEach { tab -> NavItem(tab, selected = tab == state.tab) { actions.selectTab(tab) } }
            }
            AccountCircle(state, actions.openAccount)
        }
        Box(Modifier.fillMaxWidth().weight(1f)) { content() }
    }
}

@Composable
private fun NavItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    DotFocusable(onClick = onClick, radius = 999.dp) { focused ->
        Row(
            modifier = Modifier
                .background(if (focused) Color(0xFF1A1A1A) else Color.Transparent, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (selected) Box(Modifier.size(6.dp).background(DotColors.Accent, CircleShape))
            Text(
                tab.name.uppercase(),
                style = DotType.Nav,
                color = if (selected || focused) DotColors.Text else DotColors.TextDim,
            )
        }
    }
}

@Composable
private fun AccountCircle(state: ShellState, onClick: () -> Unit) {
    val initial = (state.account?.name ?: state.account?.email)?.firstOrNull()?.uppercase() ?: "+"
    DotFocusable(onClick = onClick, radius = 999.dp) { _ ->
        Box(
            modifier = Modifier.size(36.dp).border(1.5.dp, DotColors.Text, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val avatar = state.account?.avatarUrl
            if (avatar != null) {
                DotAvatar(avatar, size = 32.dp)
            } else {
                Text(initial, style = DotType.Wordmark.copy(fontSize = 18.sp), color = DotColors.Text)
            }
        }
    }
}
