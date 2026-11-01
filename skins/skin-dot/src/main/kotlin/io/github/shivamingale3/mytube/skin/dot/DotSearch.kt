package io.github.shivamingale3.mytube.skin.dot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import io.github.shivamingale3.mytube.screens.SearchUiState
import io.github.shivamingale3.mytube.skin.SearchActions

private val Letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".map { it.toString() }

/** Dot's Search: the query in dot-matrix type, a dot keyboard and suggestions on the left, results on the right. */
@Composable
internal fun DotSearch(state: SearchUiState, actions: SearchActions) {
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstKey.requestFocus() } }

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp)) {
        QueryLine(state)
        Row(Modifier.fillMaxSize().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.width(368.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Keyboard(state, actions, firstKey)
                Suggestions(state, actions)
            }
            Results(state, actions, Modifier.weight(1f))
        }
    }
}

@Composable
private fun QueryLine(state: SearchUiState) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val empty = state.query.isEmpty()
        Text(
            if (empty) "SEARCH" else state.query.uppercase(),
            style = DotType.MatrixNumber.copy(fontSize = 64.sp, lineHeight = 60.sp),
            color = if (empty) Color(0xFF3A3A3A) else DotColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Box(Modifier.size(28.dp, 52.dp).background(DotColors.Accent))
        Spacer(Modifier.weight(1f))
        Text(
            when {
                state.loading -> "SEARCHING"
                state.searched != null -> "${state.results.size} RESULTS"
                else -> "TYPE, PICK A SUGGESTION OR SPEAK"
            },
            style = DotType.Nav.copy(fontSize = 13.sp),
            color = DotColors.TextDim,
            maxLines = 1,
        )
    }
    Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(1.dp).background(Color(0xFF262626)))
}

@Composable
private fun Keyboard(state: SearchUiState, actions: SearchActions, firstKey: FocusRequester) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Letters.chunked(9).forEachIndexed { row, keys ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                keys.forEachIndexed { i, key ->
                    Key(key, modifier = if (row == 0 && i == 0) Modifier.focusRequester(firstKey) else Modifier) {
                        actions.type(state.query + key.lowercase())
                    }
                }
            }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill("SPACE", onClick = { actions.type(state.query + " ") })
            Pill("DEL", onClick = { actions.type(state.query.dropLast(1)) }, enabled = state.query.isNotEmpty())
            Pill("CLEAR", onClick = { actions.type("") }, enabled = state.query.isNotEmpty())
            actions.voice?.let { Pill("MIC", onClick = it) }
        }
        Row(Modifier.padding(top = 6.dp)) {
            Pill("SEARCH", onClick = { actions.search(state.query) }, enabled = state.query.isNotBlank(), filled = state.query.isNotBlank())
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    DotFocusable(onClick = onClick, radius = 999.dp, modifier = modifier) { focused ->
        Box(
            Modifier.size(37.dp).background(if (focused) DotColors.Text else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = DotType.Nav.copy(fontSize = 16.sp, letterSpacing = 0.sp), color = if (focused) DotColors.Background else DotColors.TextMuted)
        }
    }
}

@Composable
private fun Suggestions(state: SearchUiState, actions: SearchActions) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        state.suggestions.take(5).forEach { suggestion ->
            DotFocusable(onClick = { actions.search(suggestion) }, radius = 10.dp, modifier = Modifier.fillMaxWidth()) { focused ->
                Text(
                    "→ $suggestion",
                    style = DotType.Nav,
                    color = if (focused) DotColors.Text else DotColors.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Results(state: SearchUiState, actions: SearchActions, modifier: Modifier) {
    Box(modifier.fillMaxSize()) {
        when {
            state.loading -> DotLoader(Modifier.align(Alignment.Center))
            state.error != null -> Message("CAN'T SEARCH", state.error.orEmpty())
            state.searched != null && state.results.isEmpty() -> Message("NO RESULTS", "Try other words.")
            state.searched == null -> Message("FIND ANYTHING", "Videos from all of YouTube, without ads.")
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(top = 6.dp, bottom = 48.dp, start = 6.dp, end = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                items(state.results, key = { it.id }) { video ->
                    VideoCard(video, onFocus = { actions.focus(video) }, width = 240.dp) { actions.play(video) }
                }
            }
        }
    }
}

@Composable
private fun Message(title: String, body: String) {
    Column(Modifier.padding(top = 40.dp, start = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = DotType.Wordmark, color = DotColors.Text)
        Text(body, style = DotType.Title, color = DotColors.TextMuted)
    }
}
