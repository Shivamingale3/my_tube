package io.github.shivamingale3.mytube.skin.tonal

import androidx.compose.foundation.background
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
import androidx.tv.material3.Text
import io.github.shivamingale3.mytube.skin.ShellActions
import io.github.shivamingale3.mytube.skin.ShellState
import io.github.shivamingale3.mytube.skin.Tab

/** Tonal's frame: a search circle and pill tabs on the left, the profile avatar (Settings) on the right. */
@Composable
internal fun TonalShell(state: ShellState, actions: ShellActions, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(TonalColors.Background)) {
        Row(
            Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val searching = state.tab == Tab.Search
                TonalFocusable(onClick = { actions.selectTab(Tab.Search) }, radius = 999.dp) { _ ->
                    Box(Modifier.size(44.dp).background(if (searching) TonalColors.Text else TonalColors.Chip, CircleShape), contentAlignment = Alignment.Center) {
                        TonalIcon(Icon.Search, if (searching) Color.Black else TonalColors.Text, Modifier.size(20.dp))
                    }
                }
                Box(Modifier.size(8.dp))
                TabPill("Home", selected = state.tab == Tab.Home) { actions.selectTab(Tab.Home) }
                TabPill("Search", selected = searching) { actions.selectTab(Tab.Search) }
            }
            TonalFocusable(onClick = actions.openSettings, radius = 999.dp) { _ ->
                val account = state.account
                if (account?.avatarUrl != null) {
                    TonalAvatar(account.avatarUrl, size = 40.dp)
                } else {
                    Box(Modifier.size(40.dp).background(TonalColors.Accent, CircleShape), contentAlignment = Alignment.Center) {
                        Text(((account?.name ?: account?.email)?.firstOrNull()?.uppercase() ?: "+"), style = TonalType.Tab, color = TonalColors.OnAccent)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) { content() }
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    TonalFocusable(onClick = onClick, radius = 999.dp) { focused ->
        Box(
            Modifier.height(44.dp)
                .background(if (selected) TonalColors.Text else if (focused) TonalColors.Raised else Color.Transparent, RoundedCornerShape(999.dp))
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = TonalType.Tab, color = if (selected) Color.Black else if (focused) TonalColors.Text else TonalColors.Muted)
        }
    }
}
