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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import io.github.shivamingale3.mytube.skin.SearchActions

/** Tonal's Search: a pill search field with a mic, suggestion chips, results in two columns. */
@Composable
internal fun TonalSearch(state: SearchUiState, actions: SearchActions) {
    val field = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { field.requestFocus() } }

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
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
    TonalFocusable(onClick = { actions.play(video) }, radius = 24.dp, modifier = Modifier.fillMaxWidth()) { focused ->
        if (focused) LaunchedEffect(video.id) { actions.focus(video) }
        Row(
            Modifier.fillMaxWidth().background(if (focused) TonalColors.Chip else Color.Transparent, RoundedCornerShape(24.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box {
                TonalThumbnail(video.thumbnailUrl, Modifier.size(216.dp, 122.dp), radius = 16.dp)
                video.badge()?.let {
                    Text(
                        it,
                        style = TonalType.Meta.copy(fontFamily = TonalFonts.Display),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                            .background(if (video.isLive) TonalColors.Accent else Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(video.title, style = TonalType.Title, color = TonalColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                video.channelName?.let { Text(it, style = TonalType.Body.copy(fontSize = 14.sp), color = TonalColors.Muted, maxLines = 1) }
                video.subtitle?.let { Text(it, style = TonalType.Meta, color = TonalColors.Dim, maxLines = 1, overflow = TextOverflow.Ellipsis) }
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
