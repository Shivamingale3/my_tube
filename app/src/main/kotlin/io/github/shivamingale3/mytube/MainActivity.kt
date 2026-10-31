package io.github.shivamingale3.mytube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                DiagnosticsScreen()
            }
        }
    }
}

/** Temporary step-1 screen; replaced by the skins in later steps. */
@Composable
private fun DiagnosticsScreen(viewModel: DiagnosticsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("MyTube", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text("Step 1 · data layer check", color = Color(0xFFA3A3A3), fontSize = 16.sp)
        Button(onClick = viewModel::loadHome) { Text("Load home feed") }
        when (val s = state) {
            DiagnosticsViewModel.State.Idle -> Unit
            DiagnosticsViewModel.State.Loading -> Text("Loading…", color = Color.White)
            is DiagnosticsViewModel.State.Failed -> Text("Failed: ${s.message}", color = Color(0xFFFF2E4D))
            is DiagnosticsViewModel.State.Loaded -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    val source = if (s.feed.personalized) "your home feed" else "signed out · popular sections"
                    Text("${s.feed.rows.size} rows loaded ($source)", color = Color(0xFF00D26A))
                }
                items(s.feed.rows) { row ->
                    Text(
                        "${row.title} — ${row.videos.size} videos · first: ${row.videos.first().title}",
                        color = Color.White,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
