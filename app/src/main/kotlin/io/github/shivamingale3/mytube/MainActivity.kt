package io.github.shivamingale3.mytube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import org.koin.androidx.compose.koinViewModel

private val Muted = Color(0xFFA3A3A3)
private val Accent = Color(0xFFFF2E4D)
private val Ok = Color(0xFF00D26A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var playing by rememberSaveable { mutableStateOf<String?>(null) }
                val videoId = playing
                if (videoId == null) {
                    DiagnosticsScreen(onPlay = { playing = it })
                } else {
                    PlayerScreen(videoId, onBack = { playing = null })
                }
            }
        }
    }
}

/** Temporary diagnostics screen; replaced by the skins in later steps. */
@Composable
private fun DiagnosticsScreen(onPlay: (String) -> Unit, viewModel: DiagnosticsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signIn by viewModel.signIn.collectAsStateWithLifecycle()
    val accounts by viewModel.accountList.collectAsStateWithLifecycle()
    val signedIn = accounts.firstOrNull { it.isSelected } ?: accounts.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("MyTube", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(
            if (signedIn != null) "Signed in as ${signedIn.name ?: signedIn.email ?: "your account"}" else "Not signed in",
            color = if (signedIn != null) Ok else Muted,
            fontSize = 16.sp,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = viewModel::loadHome) { Text("Load home feed") }
            when (signIn) {
                DiagnosticsViewModel.SignInState.Idle, is DiagnosticsViewModel.SignInState.Failed ->
                    Button(onClick = viewModel::startSignIn) { Text(if (signedIn == null) "Sign in" else "Add account") }
                else -> Button(onClick = viewModel::cancelSignIn) { Text("Cancel sign-in") }
            }
            if (signedIn != null) {
                Button(onClick = { viewModel.signOut(signedIn) }) { Text("Sign out") }
            }
        }

        when (val s = signIn) {
            DiagnosticsViewModel.SignInState.Idle -> Unit
            DiagnosticsViewModel.SignInState.Requesting -> Text("Getting a sign-in code…", color = Color.White)
            is DiagnosticsViewModel.SignInState.Failed -> Text("Sign-in failed: ${s.message}", color = Accent)
            is DiagnosticsViewModel.SignInState.Waiting -> SignInCode(s.code, s.url)
        }

        when (val s = state) {
            DiagnosticsViewModel.State.Idle -> Unit
            DiagnosticsViewModel.State.Loading -> Text("Loading…", color = Color.White)
            is DiagnosticsViewModel.State.Failed -> Text("Failed: ${s.message}", color = Accent)
            is DiagnosticsViewModel.State.Loaded -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    val source = if (s.feed.personalized) "your home feed" else "signed out · popular sections"
                    Text("${s.feed.rows.size} rows loaded ($source)", color = Ok)
                }
                items(s.feed.rows) { row ->
                    val first = row.videos.first()
                    Button(onClick = { onPlay(first.id) }) {
                        Text("▶  ${row.title} — ${first.title}", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SignInCode(code: String, url: String) {
    Column(
        modifier = Modifier
            .background(Color(0xFF141414), RoundedCornerShape(16.dp))
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("On your phone or computer, go to", color = Muted, fontSize = 16.sp)
        Text(url, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("and enter this code:", color = Muted, fontSize = 16.sp)
        Text(code, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Text("Waiting for you to sign in…", color = Muted, fontSize = 14.sp)
    }
}
