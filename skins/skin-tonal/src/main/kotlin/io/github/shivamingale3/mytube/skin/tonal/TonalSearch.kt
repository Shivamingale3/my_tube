package io.github.shivamingale3.mytube.skin.tonal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.screens.SearchUiState
import io.github.shivamingale3.mytube.skin.LocalFocusRestore
import io.github.shivamingale3.mytube.skin.SearchActions
import io.github.shivamingale3.mytube.skin.restoreFocus

/** Tonal's Search: a pill search field with a mic, suggestion chips, then one column of large results. */
@Composable
internal fun TonalSearch(state: SearchUiState, actions: SearchActions) {
    val field = remember { FocusRequester() }
    val restoring = LocalFocusRestore.current.videoId != null
    LaunchedEffect(Unit) { if (!restoring) runCatching { field.requestFocus() } }

    Column(Modifier.fillMaxSize().padding(horizontal = 64.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SearchField(state, actions, field)
        val chips = state.suggestions
        if (chips.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chips) { s -> TonalChip(s, onClick = { actions.search(s) }) }
            }
        }
        Box(Modifier.fillMaxSize()) {
            when {
                state.loading -> TonalLoader(Modifier.align(Alignment.Center))
                state.error != null -> Note("Can't search", state.error.orEmpty())
                state.searched != null && state.results.isEmpty() -> Note("No results", "Try other words.")
                state.searched == null -> Note("Search YouTube", "Type with the remote's keyboard, or press the mic and speak.")
                else -> LazyColumn(
                    contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.results, key = { it.id }) { video -> ResultRow(video, actions) }
                }
            }
        }
    }
}

@Composable
private fun SearchField(state: SearchUiState, actions: SearchActions, focus: FocusRequester) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(64.dp)
            .background(if (focused) TonalColors.Raised else TonalColors.Chip, RoundedCornerShape(999.dp))
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TonalIcon(Icon.Search, TonalColors.Muted, Modifier.size(22.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (state.query.isEmpty()) Text("Search", style = TonalType.Section.copy(fontSize = 22.sp), color = TonalColors.Dim)
            BasicTextField(
                value = state.query,
                onValueChange = actions.type,
                singleLine = true,
                textStyle = TonalType.Section.copy(fontSize = 22.sp, color = TonalColors.Text),
                cursorBrush = SolidColor(TonalColors.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { actions.search(state.query) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { focused = it.isFocused },
            )
        }
        actions.voice?.let { voice ->
            TonalFocusable(onClick = voice, radius = 999.dp) { f ->
                Box(Modifier.size(48.dp).background(if (f) TonalColors.Accent else TonalColors.Text, CircleShape), contentAlignment = Alignment.Center) {
                    TonalIcon(Icon.Mic, if (f) TonalColors.OnAccent else Color.Black, Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultRow(video: Video, actions: SearchActions) {
    TonalFocusable(onClick = { actions.play(video) }, radius = 28.dp, modifier = Modifier.fillMaxWidth().restoreFocus(video.id)) { focused ->
        if (focused) LaunchedEffect(video.id) { actions.focus(video) }
        Row(
            Modifier.fillMaxWidth().background(if (focused) TonalColors.Chip else Color.Transparent, RoundedCornerShape(28.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // Fixed size: the watched bar inside must not stretch this box across the row,
            // which squeezed the title column to nothing on watched videos.
            Box(Modifier.size(400.dp, 225.dp)) {
                TonalThumbnail(video.thumbnailUrl, Modifier.size(400.dp, 225.dp), radius = 20.dp)
                video.badge()?.let {
                    Text(
                        it,
                        style = TonalType.Chip,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                            .background(if (video.isLive) TonalColors.Accent else Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                if (video.percentWatched in 1..100) {
                    Box(Modifier.align(Alignment.BottomStart).padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))) {
                        Box(Modifier.fillMaxWidth(video.percentWatched / 100f).height(4.dp).background(TonalColors.Accent, RoundedCornerShape(2.dp)))
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(video.title, style = TonalType.Hero.copy(fontSize = 26.sp, lineHeight = 32.sp), color = TonalColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                video.channelName?.let { Text(it, style = TonalType.Body.copy(fontSize = 17.sp), color = TonalColors.Muted, maxLines = 1) }
                video.subtitle?.let { Text(it, style = TonalType.Body, color = TonalColors.Dim, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
private fun Note(title: String, body: String) {
    Column(Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = TonalType.Heading, color = TonalColors.Text)
        Text(body, style = TonalType.Body, color = TonalColors.Muted)
    }
}
