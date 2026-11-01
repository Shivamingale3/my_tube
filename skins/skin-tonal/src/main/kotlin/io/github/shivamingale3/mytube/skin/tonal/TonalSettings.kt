package io.github.shivamingale3.mytube.skin.tonal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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

private enum class Section(val title: String) { Account("Account"), Playback("Playback"), Skip("Skip"), Look("Look"), About("About") }

private val CardShape = RoundedCornerShape(28.dp)

/** Tonal's Settings: a rail of sections on the left, the focused section on the right. */
@Composable
internal fun TonalSettings(state: SettingsUiState, actions: SettingsActions) {
    var section by rememberSaveable { mutableStateOf(Section.Account) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Row(Modifier.fillMaxSize().background(TonalColors.Background)) {
        Column(
            Modifier.width(120.dp).fillMaxHeight().background(Color(0xFF0E0E0E)),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Section.entries.forEachIndexed { i, s ->
                TonalFocusable(
                    onClick = { section = s },
                    radius = 16.dp,
                    modifier = Modifier.then(if (i == 0) Modifier.focusRequester(first) else Modifier).onFocusChanged { if (it.isFocused) section = s },
                ) { focused ->
                    Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val on = s == section
                        Box(Modifier.size(60.dp, 34.dp).background(if (on) TonalColors.Text else Color.Transparent, RoundedCornerShape(999.dp)), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(12.dp).border(2.4.dp, if (on) Color.Black else TonalColors.Muted, RoundedCornerShape(4.dp)))
                        }
                        Text(s.title, style = TonalType.Chip.copy(fontSize = 13.sp), color = if (on || focused) TonalColors.Text else TonalColors.Muted)
                    }
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(start = 48.dp, end = 64.dp, top = 48.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(section.title, style = TonalType.Heading, color = TonalColors.Text)
            when (section) {
                Section.Account -> AccountSection(state, actions)
                Section.Playback -> Card {
                    Toggle("Resume where you left off", state.preferences.resume) { v -> actions.change { it.copy(resume = v) } }
                    Toggle("Stats button in the player", state.preferences.statsButton) { v -> actions.change { it.copy(statsButton = v) } }
                }
                Section.Skip -> Card {
                    Toggle("SponsorBlock", state.preferences.sponsorBlock) { v -> actions.change { it.copy(sponsorBlock = v) } }
                    Toggle("Skip segments automatically", state.preferences.autoSkip, enabled = state.preferences.sponsorBlock) { v -> actions.change { it.copy(autoSkip = v) } }
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
        is SignInStep.EnterCode -> Card(padding = 28) {
            Text("On your phone or computer, go to", style = TonalType.Body, color = TonalColors.Muted)
            Text(step.url, style = TonalType.Hero.copy(fontSize = 30.sp), color = TonalColors.Text)
            Text("and enter this code", style = TonalType.Body, color = TonalColors.Muted)
            Text(step.code, style = TonalType.Hero.copy(fontSize = 56.sp, letterSpacing = 4.sp), color = TonalColors.Accent)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TonalLoader()
                TonalButton("Cancel", onClick = actions.cancelSignIn, height = 44.dp)
            }
        }
        SignInStep.Requesting -> Card(padding = 28) {
            Text("Getting a sign-in code…", style = TonalType.Title, color = TonalColors.Text)
            TonalLoader()
        }
        else -> {
            Card {
                if (state.accounts.isEmpty()) {
                    Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Not signed in", style = TonalType.Title, color = TonalColors.Text)
                        Text("Sign in for your recommendations, history and resume.", style = TonalType.Body, color = TonalColors.Muted)
                    }
                } else {
                    state.accounts.forEach { AccountRow(it, actions) }
                }
            }
            (step as? SignInStep.Failed)?.let { Text("Sign-in failed: ${it.message}", style = TonalType.Body, color = TonalColors.Accent) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TonalButton(if (state.accounts.isEmpty()) "Sign in" else "Add account", onClick = actions.signIn, primary = state.accounts.isEmpty())
                state.account?.let { TonalButton("Sign out", onClick = { actions.signOut(it) }) }
            }
        }
    }
}

@Composable
private fun AccountRow(account: UserAccount, actions: SettingsActions) {
    TonalFocusable(onClick = { if (!account.isSelected) actions.selectAccount(account) }, radius = 18.dp, modifier = Modifier.fillMaxWidth()) { focused ->
        Row(
            Modifier.fillMaxWidth().background(if (focused) TonalColors.Raised else Color.Transparent, RoundedCornerShape(18.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TonalAvatar(account.avatarUrl, size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(account.name ?: "YouTube account", style = TonalType.Title, color = TonalColors.Text, maxLines = 1)
                account.email?.let { Text(it, style = TonalType.Meta, color = TonalColors.Muted, maxLines = 1) }
            }
            Text(if (account.isSelected) "Current" else "Switch", style = TonalType.Chip, color = if (account.isSelected) TonalColors.Accent else TonalColors.Muted)
        }
    }
}

@Composable
private fun LookSection(state: SettingsUiState, actions: SettingsActions) {
    Text("Theme", style = TonalType.Title, color = TonalColors.Text)
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        state.skins.forEach { (id, name) ->
            val selected = id == state.preferences.skin
            TonalFocusable(onClick = { actions.change { it.copy(skin = id) } }, radius = 28.dp) { _ ->
                Column(
                    Modifier.width(330.dp).background(TonalColors.Surface, CardShape)
                        .border(3.dp, if (selected) TonalColors.Accent else Color.Transparent, CardShape)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (id == "tonal") {
                        WavyProgress(0.6f, emptyList(), Modifier.fillMaxWidth(), animate = false)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(24) { i -> Box(Modifier.size(6.dp).background(if (i < 14) Color.White else if (i == 14) TonalColors.Accent else TonalColors.Border, CircleShape)) }
                        }
                    }
                    Text(name, style = TonalType.Title.copy(fontSize = 18.sp), color = TonalColors.Text, modifier = Modifier.padding(top = 8.dp))
                    Text(if (id == "tonal") "Rounded, wavy progress" else "Dot-matrix, dot-screen thumbnails", style = TonalType.Meta, color = TonalColors.Muted)
                }
            }
        }
    }
    Card(padding = 20) {
        Text("Accent colour", style = TonalType.Title, color = TonalColors.Text)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Preferences.ACCENTS.forEach { argb ->
                val selected = argb == state.preferences.accent
                TonalFocusable(onClick = { actions.change { it.copy(accent = argb) } }, radius = 999.dp) { _ ->
                    Box(Modifier.size(46.dp).border(2.dp, if (selected) Color.White else Color.Transparent, CircleShape).padding(5.dp).background(Color(argb), CircleShape))
                }
            }
        }
    }
}

@Composable
private fun AboutSection(state: SettingsUiState, actions: SettingsActions) {
    Card(padding = 24) {
        Text("MyTube", style = TonalType.Hero.copy(fontSize = 32.sp), color = TonalColors.Text)
        Text(state.version, style = TonalType.Meta, color = TonalColors.Muted)
        Text(updateText(state.update), style = TonalType.Body, color = TonalColors.Text, modifier = Modifier.padding(top = 6.dp))
        when (val u = state.update) {
            is UpdateStep.Available -> TonalButton("Install build ${u.build}", onClick = actions.installUpdate, primary = true)
            is UpdateStep.Failed -> TonalButton("Try again", onClick = actions.checkUpdate)
            UpdateStep.UpToDate, UpdateStep.Idle -> TonalButton("Check for updates", onClick = actions.checkUpdate)
            else -> Unit
        }
    }
    Text(
        "Ad-free YouTube for TV. Streams and sign-in use SmartTube's MediaServiceCore (MIT, yuliskov); sponsor data from SponsorBlock.",
        style = TonalType.Meta,
        color = TonalColors.Dim,
    )
}

private fun updateText(step: UpdateStep): String = when (step) {
    UpdateStep.Idle -> "Updates come from GitHub releases."
    UpdateStep.Checking -> "Checking for updates…"
    UpdateStep.UpToDate -> "You have the latest build."
    is UpdateStep.Available -> "Build ${step.build} is available."
    is UpdateStep.Downloading -> "Downloading… ${step.percent}%"
    UpdateStep.Installing -> "Opening the installer. Allow MyTube to install apps if Android asks."
    is UpdateStep.Failed -> "Couldn't update: ${step.message}"
}

@Composable
private fun Card(padding: Int = 6, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(TonalColors.Surface, CardShape).padding(horizontal = 22.dp, vertical = padding.dp),
        verticalArrangement = Arrangement.spacedBy(if (padding > 6) 12.dp else 0.dp),
        content = content,
    )
}

@Composable
private fun Toggle(name: String, on: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    TonalFocusable(onClick = if (enabled) ({ onChange(!on) }) else null, radius = 18.dp, modifier = Modifier.fillMaxWidth()) { focused ->
        Row(
            Modifier.fillMaxWidth().height(64.dp).background(if (focused) TonalColors.Raised else Color.Transparent, RoundedCornerShape(18.dp)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, style = TonalType.Title.copy(fontSize = 16.sp), color = if (enabled) TonalColors.Text else TonalColors.Dim, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(
                Modifier.size(52.dp, 32.dp)
                    .background(if (on) TonalColors.Accent else TonalColors.Raised, RoundedCornerShape(999.dp))
                    .border(2.dp, if (on) TonalColors.Accent else TonalColors.Dim, RoundedCornerShape(999.dp)),
                contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(Modifier.padding(horizontal = if (on) 2.dp else 8.dp).size(if (on) 24.dp else 12.dp).background(if (on) Color.White else TonalColors.Dim, CircleShape))
            }
        }
    }
}
