package io.github.shivamingale3.mytube.skin.dot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import io.github.shivamingale3.mytube.model.UserAccount
import io.github.shivamingale3.mytube.screens.Preferences
import io.github.shivamingale3.mytube.screens.SettingsUiState
import io.github.shivamingale3.mytube.screens.SignInStep
import io.github.shivamingale3.mytube.screens.UpdateStep
import io.github.shivamingale3.mytube.skin.SettingsActions

/** The Settings sections every skin shows. */
internal enum class Section(val title: String) { Account("ACCOUNT"), Playback("PLAYBACK"), SponsorBlock("SPONSORBLOCK"), Look("LOOK"), About("ABOUT") }

private val PanelShape = RoundedCornerShape(24.dp)

/** Dot's Settings: dot-matrix heading and section list on the left; the focused section's cards on the right. */
@Composable
internal fun DotSettings(state: SettingsUiState, actions: SettingsActions) {
    var section by rememberSaveable { mutableStateOf(Section.Account) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Row(Modifier.fillMaxSize().dotGrid().padding(horizontal = 48.dp, vertical = 44.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
        Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("SETTINGS", style = DotType.MatrixNumber.copy(fontSize = 64.sp, lineHeight = 60.sp), color = DotColors.Text)
            Box(Modifier.height(18.dp))
            Section.entries.forEachIndexed { i, s ->
                DotFocusable(
                    onClick = { section = s },
                    radius = 12.dp,
                    modifier = Modifier
                        .then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                        .onFocusChanged { if (it.isFocused) section = s },
                ) { focused ->
                    Row(
                        Modifier.width(300.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(7.dp).background(if (s == section) DotColors.Accent else Color.Transparent, CircleShape))
                        Text(s.title, style = DotType.Nav.copy(fontSize = 15.sp), color = if (s == section || focused) DotColors.Text else DotColors.TextDim)
                    }
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (section) {
                Section.Account -> AccountSection(state, actions)
                Section.Playback -> Panel {
                    Toggle("Resume where you left off", "Start videos from your watch history position", state.preferences.resume) { v -> actions.change { it.copy(resume = v) } }
                    Toggle("Stats button in the player", "Shows streams, decoders and dropped frames", state.preferences.statsButton) { v -> actions.change { it.copy(statsButton = v) } }
                }
                Section.SponsorBlock -> Panel {
                    Toggle("SponsorBlock", "Find sponsor, self-promo and reminder segments", state.preferences.sponsorBlock) { v -> actions.change { it.copy(sponsorBlock = v) } }
                    Toggle("Skip automatically", "Jump past segments without asking", state.preferences.autoSkip, enabled = state.preferences.sponsorBlock) { v ->
                        actions.change { it.copy(autoSkip = v) }
                    }
                }
                Section.Look -> LookSection(state, actions)
                Section.About -> AboutSection(state, actions)
            }
        }
    }
}

@Composable
private fun AccountSection(state: SettingsUiState, actions: SettingsActions) {
    when (val step = state.signIn) {
        is SignInStep.EnterCode -> Panel(padding = 28) {
            Text("ON YOUR PHONE OR COMPUTER, GO TO", style = DotType.Label, color = DotColors.TextDim)
            Text(step.url, style = DotType.Hero, color = DotColors.Text)
            Text("AND ENTER", style = DotType.Label, color = DotColors.TextDim, modifier = Modifier.padding(top = 8.dp))
            Text(step.code, style = DotType.MatrixNumber.copy(fontSize = 72.sp, lineHeight = 70.sp), color = DotColors.Text)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                DotLoader()
                Pill("CANCEL", onClick = actions.cancelSignIn)
            }
        }
        SignInStep.Requesting -> Panel(padding = 28) {
            Text("GETTING A CODE", style = DotType.Label, color = DotColors.TextDim)
            DotLoader()
        }
        else -> {
            if (state.accounts.isEmpty()) {
                Panel(padding = 28) {
                    Text("NOT SIGNED IN", style = DotType.Label, color = DotColors.TextDim)
                    Text("Sign in for your home feed, history and resume.", style = DotType.Title, color = DotColors.TextMuted)
                }
            } else {
                Panel {
                    state.accounts.forEach { AccountRow(it, actions) }
                }
            }
            (step as? SignInStep.Failed)?.let { Text("Sign-in failed: ${it.message}", style = DotType.Card, color = DotColors.Accent) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(if (state.accounts.isEmpty()) "SIGN IN" else "ADD ACCOUNT", onClick = actions.signIn, filled = state.accounts.isEmpty())
                state.account?.let { current -> Pill("SIGN OUT", onClick = { actions.signOut(current) }) }
            }
        }
    }
}

@Composable
private fun AccountRow(account: UserAccount, actions: SettingsActions) {
    DotFocusable(onClick = { if (!account.isSelected) actions.selectAccount(account) }, radius = 14.dp, modifier = Modifier.fillMaxWidth()) { focused ->
        Row(
            Modifier.fillMaxWidth().background(if (focused) Color(0xFF1A1A1A) else Color.Transparent, RoundedCornerShape(14.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DotAvatar(account.avatarUrl, size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(account.name ?: "YouTube account", style = DotType.Title.copy(fontSize = 16.sp), color = DotColors.Text, maxLines = 1)
                account.email?.let { Text(it, style = DotType.Meta, color = DotColors.TextDim, maxLines = 1) }
            }
            Text(if (account.isSelected) "CURRENT" else "SWITCH", style = DotType.Label, color = if (account.isSelected) DotColors.Accent else DotColors.TextDim)
        }
    }
}

@Composable
private fun LookSection(state: SettingsUiState, actions: SettingsActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        state.skins.forEach { (id, name) ->
            val selected = id == state.preferences.skin
            DotFocusable(onClick = { actions.change { it.copy(skin = id) } }, radius = 24.dp, modifier = Modifier.weight(1f)) { _ ->
                Column(
                    Modifier.fillMaxWidth().background(DotColors.Surface, PanelShape)
                        .border(if (selected) 2.dp else 1.dp, if (selected) DotColors.Accent else DotColors.SurfaceBorder, PanelShape)
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("SKIN", style = DotType.Label, color = DotColors.TextDim)
                    Text(name.uppercase(), style = DotType.MatrixNumber.copy(fontSize = 44.sp, lineHeight = 42.sp), color = DotColors.Text)
                    Text(if (id == "dot") "Dot-matrix, thumbnails under a dot screen" else "Rounded, bold, wavy progress", style = DotType.Card, color = DotColors.TextMuted)
                }
            }
        }
    }
    Panel(padding = 22) {
        Text("ACCENT", style = DotType.Label, color = DotColors.TextDim)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Preferences.ACCENTS.forEach { argb ->
                val selected = argb == state.preferences.accent
                DotFocusable(onClick = { actions.change { it.copy(accent = argb) } }, radius = 999.dp) { _ ->
                    Box(
                        Modifier.size(44.dp).border(2.dp, if (selected) DotColors.Text else Color.Transparent, CircleShape).padding(5.dp)
                            .background(Color(argb), CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSection(state: SettingsUiState, actions: SettingsActions) {
    Panel(padding = 22) {
        Text("MYTUBE", style = DotType.MatrixNumber.copy(fontSize = 48.sp, lineHeight = 46.sp), color = DotColors.Text)
        Text(state.version, style = DotType.Meta, color = DotColors.TextMuted)
        Text(updateText(state.update), style = DotType.Title, color = DotColors.Text, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when (val u = state.update) {
                is UpdateStep.Available -> Pill("INSTALL BUILD ${u.build}", onClick = actions.installUpdate, filled = true)
                is UpdateStep.Failed -> Pill("TRY AGAIN", onClick = actions.checkUpdate)
                UpdateStep.UpToDate, UpdateStep.Idle -> Pill("CHECK FOR UPDATES", onClick = actions.checkUpdate)
                else -> Unit
            }
        }
    }
    Text(
        "Ad-free YouTube for TV. Streams and sign-in use SmartTube's MediaServiceCore (MIT, yuliskov); sponsor data from SponsorBlock.",
        style = DotType.Card,
        color = DotColors.TextDim,
    )
}

internal fun updateText(step: UpdateStep): String = when (step) {
    UpdateStep.Idle -> "Updates come from GitHub releases."
    UpdateStep.Checking -> "Checking for updates…"
    UpdateStep.UpToDate -> "You have the latest build."
    is UpdateStep.Available -> "Build ${step.build} is available."
    is UpdateStep.Downloading -> "Downloading… ${step.percent}%"
    UpdateStep.Installing -> "Opening the installer. Allow MyTube to install apps if Android asks."
    is UpdateStep.Failed -> "Couldn't update: ${step.message}"
}

@Composable
private fun Panel(padding: Int = 6, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(DotColors.Surface, PanelShape).border(1.dp, DotColors.SurfaceBorder, PanelShape)
            .padding(horizontal = 22.dp, vertical = padding.dp),
        verticalArrangement = Arrangement.spacedBy(if (padding > 6) 12.dp else 0.dp),
        content = content,
    )
}

@Composable
private fun Toggle(name: String, description: String, on: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    DotFocusable(onClick = if (enabled) ({ onChange(!on) }) else null, radius = 14.dp, modifier = Modifier.fillMaxWidth()) { focused ->
        Row(
            Modifier.fillMaxWidth().height(66.dp)
                .background(if (focused) Color(0xFF1A1A1A) else Color.Transparent, RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = DotType.Title.copy(fontSize = 16.sp), color = if (enabled) DotColors.Text else DotColors.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(description, style = DotType.Card.copy(fontSize = 13.sp), color = DotColors.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.size(54.dp, 30.dp)
                    .background(if (on) DotColors.Text else Color.Transparent, RoundedCornerShape(999.dp))
                    .border(1.5.dp, if (on) DotColors.Text else Color(0xFF5A5A5A), RoundedCornerShape(999.dp)),
                contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(Modifier.padding(horizontal = 5.dp).size(17.dp).background(if (on) DotColors.Accent else Color(0xFF5A5A5A), CircleShape))
            }
        }
    }
}
