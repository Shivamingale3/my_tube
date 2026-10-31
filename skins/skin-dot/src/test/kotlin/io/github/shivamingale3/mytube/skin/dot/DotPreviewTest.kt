package io.github.shivamingale3.mytube.skin.dot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import io.github.shivamingale3.mytube.model.ChannelSummary
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
