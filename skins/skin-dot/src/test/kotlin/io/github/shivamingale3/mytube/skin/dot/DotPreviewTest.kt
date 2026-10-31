package io.github.shivamingale3.mytube.skin.dot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.compose.ui.test.onRoot
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.decode.DataSource
import coil3.request.SuccessResult
import coil3.test.FakeImageLoaderEngine
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.shivamingale3.mytube.model.ChannelSummary
import io.github.shivamingale3.mytube.model.SponsorSegment
import io.github.shivamingale3.mytube.player.VideoQuality
import io.github.shivamingale3.mytube.screens.PlayerUiState
import io.github.shivamingale3.mytube.skin.PlayerActions
import io.github.shivamingale3.mytube.model.UserAccount
import io.github.shivamingale3.mytube.model.Video
import io.github.shivamingale3.mytube.model.VideoRow
import io.github.shivamingale3.mytube.screens.HomeUiState
import io.github.shivamingale3.mytube.skin.DesignScale
import io.github.shivamingale3.mytube.skin.HomeActions
import io.github.shivamingale3.mytube.skin.ShellActions
import io.github.shivamingale3.mytube.skin.ShellState
import io.github.shivamingale3.mytube.skin.Tab
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
 * Renders Dot screens to PNGs at 1920×1080 for design review. Skipped unless run with -Ppreview.
 * Thumbnails are flat colours, or real images from -PpreviewImages=<dir> when given.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-xhdpi")
class DotPreviewTest {
    @get:Rule
    val compose = createComposeRule()

    private val imageDir = System.getProperty("mytube.previewImages").orEmpty().takeIf { it.isNotBlank() }?.let(::File)

    @OptIn(DelicateCoilApi::class)
    @Before
    fun setUp() {
        assumeTrue("previews disabled", System.getProperty("mytube.preview") == "true")
        val palette = listOf(0xFF3A4767, 0xFF2B4540, 0xFF46304D, 0xFF4A3832, 0xFF2A3550, 0xFF36442B).map { it.toInt() }
        val engine = FakeImageLoaderEngine.Builder()
            .intercept({ it is String }, { value ->
                val name = value.request.data.toString()
                val file = imageDir?.resolve("$name.jpg")?.takeIf { it.exists() }
                val bitmap = file?.let { BitmapFactory.decodeFile(it.path) }
                    ?: Bitmap.createBitmap(16, 9, Bitmap.Config.ARGB_8888).apply { eraseColor(palette[name.hashCode().mod(palette.size)]) }
                SuccessResult(bitmap.asImage(), value.request, DataSource.MEMORY)
            })
            .build()
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(RuntimeEnvironment.getApplication()).components { add(engine) }.build())
    }

    @Test
    fun home() {
        compose.setContent {
            DesignScale {
                DotSkin.Theme {
                    DotSkin.Shell(ShellState(Tab.Home, sampleAccount), ShellActions({}, {})) {
                        DotSkin.Home(sampleHome, HomeActions({}, {}, {}))
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/previews/dot-home.png")
    }

    @Test
    fun homeWithFocus() {
        compose.setContent {
            DesignScale {
                DotSkin.Theme {
                    DotSkin.Shell(ShellState(Tab.Home, sampleAccount), ShellActions({}, {})) {
                        DotSkin.Home(sampleHome, HomeActions({}, {}, {}))
                    }
                }
            }
        }
        // Focus the second card of the first row: it should lift, clear its dot screen and get the ring.
        compose.onAllNodes(isFocusable())[9].requestFocus()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/previews/dot-home-focused.png")
    }

    private fun playerState(phase: PlayerUiState.Phase) = PlayerUiState(
        video = sampleHome.featured,
        phase = phase,
        title = "The Insane Engineering of Fiber Optics",
        channelName = "Veritasium",
        positionMs = 754_000,
        durationMs = 2_072_000,
        isPlaying = true,
        segments = listOf(SponsorSegment(900_000, 1_010_000, "sponsor")),
        qualities = listOf(VideoQuality(2160, 30), VideoQuality(1440, 30), VideoQuality(1080, 60), VideoQuality(720, 30)),
        playingHeight = 2160,
        speed = 1f,
    )

    private fun renderPlayer(state: PlayerUiState, name: String) {
        compose.setContent {
            DesignScale {
                DotSkin.Theme {
                    DotSkin.Player(state, PlayerActions({}, {}, {}, {}, {}, {}, {})) {
                        coil3.compose.AsyncImage(
                            model = "t_NiCgcUTL9_Q",
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/previews/$name.png")
    }

    @Test
    fun playerControls() = renderPlayer(playerState(PlayerUiState.Phase.Ready), "dot-player")

    @Test
    fun playerLoading() = renderPlayer(playerState(PlayerUiState.Phase.Resolving).copy(positionMs = 0), "dot-player-loading")

    @Test
    fun playerSponsor() = renderPlayer(
        playerState(PlayerUiState.Phase.Ready).let { it.copy(positionMs = 930_000, activeSegment = it.segments.first()) },
        "dot-player-sponsor",
    )

    @Test
    fun qualitySheet() {
        compose.setContent {
            DesignScale {
                DotSkin.Theme {
                    Box(Modifier.fillMaxSize().dotGrid(), contentAlignment = androidx.compose.ui.Alignment.CenterEnd) {
                        OptionSheet("QUALITY", listOf("AUTO", "2160P", "1440P", "1080P60", "720P"), selected = 0, onPick = {})
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/previews/dot-quality.png")
    }

    private val sampleAccount = UserAccount(1, "Shivam", null, null, isSelected = true)

    private fun video(id: String, title: String, channel: String, minutes: Int, watched: Int = 0) = Video(
        id = id, title = title, channelName = channel, channelId = channel, thumbnailUrl = "t_$id",
        durationMs = minutes * 60_000L, subtitle = null, percentWatched = watched, isLive = false, isShorts = false,
    )

    private val sampleHome = HomeUiState(
        loading = false,
        personalized = true,
        featured = video("NiCgcUTL9_Q", "The Insane Engineering of Fiber Optics", "Veritasium", 34),
        continueWatching = video("ujkD4SxPKOI", "AI Just Crossed the Terrifying Line - Now What?", "Kurzgesagt", 22, watched = 53),
        channels = listOf(
            ChannelSummary("k", "Kurzgesagt", "a_kurzgesagt", hasNewContent = true),
            ChannelSummary("m", "Mark Rober", "a_MarkRober", hasNewContent = true),
            ChannelSummary("b", "Marques Brownlee", "a_mkbhd", hasNewContent = true),
            ChannelSummary("v", "Veritasium", "a_veritasium", hasNewContent = false),
            ChannelSummary("n", "National Geographic", "a_NatGeo", hasNewContent = true),
        ),
        rows = listOf(
            VideoRow(
                "Recommended",
                listOf(
                    video("26P1KyvrmjI", "I Solved Every Board Game", "Mark Rober", 28),
                    video("2reQut8WNGc", "Macbook PROLED XDR Impressions", "Marques Brownlee", 10),
                    video("5_Fayfx2NUM", "How I Survived Late-Stage Cancer", "Kurzgesagt", 14),
                    video("rPpYqNOdjW4", "David Blaine Uncovers Magic Around the Globe", "National Geographic", 111),
                    video("PXbVn-f_wuM", "You Will Only Like Half of This Video", "Kurzgesagt", 3),
                ),
            ),
            VideoRow(
                "Recently uploaded",
                listOf(
                    video("eyZEqSPeMwg", "No device could prove Einstein wrong... until now", "Veritasium", 34),
                    video("ESABTN4VtrA", "iPhone Duo Review: First Generation Fun!", "Marques Brownlee", 29),
                    video("h0EGCnBjTVk", "I Outsmarted Pro Car Thieves", "Mark Rober", 25),
                ),
            ),
        ),
    )
}
