package io.github.shivamingale3.mytube.skin.tonal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.decode.DataSource
import coil3.request.SuccessResult
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.shivamingale3.mytube.model.SponsorSegment
import io.github.shivamingale3.mytube.model.UserAccount
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.model.VideoRow
import io.github.shivamingale3.mytube.player.VideoQuality
import io.github.shivamingale3.mytube.screens.HomeUiState
import io.github.shivamingale3.mytube.screens.PlayerUiState
import io.github.shivamingale3.mytube.screens.Preferences
import io.github.shivamingale3.mytube.screens.SearchUiState
import io.github.shivamingale3.mytube.screens.SettingsUiState
import io.github.shivamingale3.mytube.screens.SignInStep
import io.github.shivamingale3.mytube.screens.UpdateStep
import io.github.shivamingale3.mytube.skin.DesignScale
import io.github.shivamingale3.mytube.skin.HomeActions
import io.github.shivamingale3.mytube.skin.PlayerActions
import io.github.shivamingale3.mytube.skin.SearchActions
import io.github.shivamingale3.mytube.skin.SettingsActions
import io.github.shivamingale3.mytube.skin.ShellActions
import io.github.shivamingale3.mytube.skin.ShellState
import io.github.shivamingale3.mytube.skin.Skin
import io.github.shivamingale3.mytube.skin.Tab
import io.github.shivamingale3.mytube.skin.dot.DotSkin
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders Home, Search, Settings and the player for both skins to PNGs at 1920×1080.
 * Skipped unless run with -Ppreview; real thumbnails from -PpreviewImages=<dir>.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-xhdpi")
class SkinPreviewTest {
    @get:Rule
    val compose = createComposeRule()

    private val imageDir = System.getProperty("mytube.previewImages").orEmpty().takeIf { it.isNotBlank() }?.let(::File)

    @OptIn(DelicateCoilApi::class)
    @Before
    fun setUp() {
        assumeTrue("previews disabled", System.getProperty("mytube.preview") == "true")
        val palette = listOf(0xFF3A4767, 0xFF2B4540, 0xFF46304D, 0xFF4A3832).map { it.toInt() }
        val engine = FakeImageLoaderEngine.Builder()
            .intercept({ it is String }, { value ->
                val name = value.request.data.toString()
                val bitmap = imageDir?.resolve("$name.jpg")?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
                    ?: Bitmap.createBitmap(16, 9, Bitmap.Config.ARGB_8888).apply { eraseColor(palette[name.hashCode().mod(palette.size)]) }
                SuccessResult(bitmap.asImage(), value.request, DataSource.MEMORY)
            })
            .build()
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(RuntimeEnvironment.getApplication()).components { add(engine) }.build())
    }

    private fun render(name: String, skin: Skin, content: @Composable () -> Unit) {
        compose.setContent { DesignScale { skin.Theme { content() } } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/previews/$name.png")
    }

    @Composable
    private fun shell(skin: Skin, tab: Tab, content: @Composable () -> Unit) =
        skin.Shell(ShellState(tab, UserAccount(1, "Shivam", null, null, true)), ShellActions({}, {}), content)

    @Test fun tonalHome() = render("tonal-home", TonalSkin) { shell(TonalSkin, Tab.Home) { TonalSkin.Home(home, HomeActions({}, {})) } }
    @Test fun tonalSearch() = render("tonal-search", TonalSkin) { shell(TonalSkin, Tab.Search) { TonalSkin.Search(search, searchActions) } }
    @Test fun tonalSettings() = render("tonal-settings", TonalSkin) { TonalSkin.Settings(settings, settingsActions) }
    @Test fun tonalSignIn() = render("tonal-signin", TonalSkin) { TonalSkin.Settings(settings.copy(signIn = SignInStep.EnterCode("XKCD-42QZ", "youtube.com/activate")), settingsActions) }
    @Test fun tonalPlayer() = render("tonal-player", TonalSkin) { TonalSkin.Player(player, playerActions) { thumbnail() } }
    @Test fun dotSearch() = render("dot-search", DotSkin) { shell(DotSkin, Tab.Search) { DotSkin.Search(search, searchActions) } }
    @Test fun dotSettings() = render("dot-settings", DotSkin) { DotSkin.Settings(settings, settingsActions) }
    @Test fun dotHome() = render("dot-home-final", DotSkin) { shell(DotSkin, Tab.Home) { DotSkin.Home(home, HomeActions({}, {})) } }

    @Composable
    private fun thumbnail() = coil3.compose.AsyncImage(
        model = "t_ESABTN4VtrA",
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )

    private fun video(id: String, title: String, channel: String, minutes: Int, watched: Int = 0) = Video(
        id = id, title = title, channelName = channel, channelId = channel, thumbnailUrl = "t_$id",
        durationMs = minutes * 60_000L, subtitle = "$channel • 2.1M views • 3 weeks ago", percentWatched = watched, isLive = false, isShorts = false,
    )

    private val videos = listOf(
        video("26P1KyvrmjI", "I Solved Every Board Game", "Mark Rober", 28),
        video("2reQut8WNGc", "Macbook PROLED XDR Impressions", "Marques Brownlee", 10, watched = 40),
        video("5_Fayfx2NUM", "How I Survived Late-Stage Cancer", "Kurzgesagt", 14),
        video("rPpYqNOdjW4", "David Blaine Uncovers Magic Around the Globe", "National Geographic", 111),
        video("PXbVn-f_wuM", "You Will Only Like Half of This Video", "Kurzgesagt", 3),
        video("eyZEqSPeMwg", "No device could prove Einstein wrong... until now", "Veritasium", 34),
    )

    private val home = HomeUiState(
        loading = false,
        personalized = true,
        featured = video("NiCgcUTL9_Q", "The Insane Engineering of Fiber Optics", "Veritasium", 34),
        continueWatching = video("ujkD4SxPKOI", "AI Just Crossed the Terrifying Line - Now What?", "Kurzgesagt", 22, watched = 53),
        rows = listOf(VideoRow("Recommended", videos), VideoRow("Recently uploaded", videos.reversed())),
    )

    private val search = SearchUiState(query = "engineering", searched = "engineering", results = videos, suggestions = listOf("engineering explained", "engineering memes", "engineering vs physics"))
    private val searchActions = SearchActions({}, {}, {}, {}, voice = {})

    private val settings = SettingsUiState(
        accounts = listOf(UserAccount(1, "Shivam", "shivam@example.com", null, true)),
        preferences = Preferences(),
        skins = listOf("dot" to "Dot", "tonal" to "Tonal"),
        version = "1.0.0 · build 20",
        update = UpdateStep.Available(21),
    )
    private val settingsActions = SettingsActions({}, {}, {}, {}, {}, {}, {})

    private val player = PlayerUiState(
        video = home.featured,
        phase = PlayerUiState.Phase.Ready,
        title = "iPhone Duo Review: First Generation Fun!",
        channelName = "Marques Brownlee",
        positionMs = 680_000,
        durationMs = 1_745_000,
        isPlaying = true,
        segments = listOf(SponsorSegment(960_000, 1_040_000, "sponsor")),
        qualities = listOf(VideoQuality(2160, 60), VideoQuality(1080, 60)),
        playingHeight = 2160,
        sourceLabel = "DASH · VISIONOS",
    )
    private val playerActions = PlayerActions({}, {}, {}, {}, {}, {}, {})
}
