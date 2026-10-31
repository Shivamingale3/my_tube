package io.github.shivamingale3.mytube.youtube

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.sharedutils.prefs.GlobalPreferences
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager
import io.reactivex.Observable
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import android.os.Looper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.robolectric.Shadows.shadowOf

/**
 * Live probe against YouTube: which feeds return videos while signed out, and what our bridge
 * builds from them. Skipped unless run with `./gradlew :core:youtube-bridge:testDebugUnitTest -PliveProbe`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeFeedProbe {
    @Before
    fun onlyWhenAsked() {
        assumeTrue("live probe disabled", System.getProperty("mytube.liveProbe") == "true")
    }

    @Test
    fun probeBridgeHome() {
        val source = YouTubeSource(RuntimeEnvironment.getApplication())
        val feed = pumpUntilDone { runBlocking { source.home() } }
        println("PROBE bridge home personalized=${feed.personalized} rows=${feed.rows.size}")
        feed.rows.forEach { println("PROBE   row '${it.title}' videos=${it.videos.size}") }
    }

    @Test
    fun probeSignInCode() {
        val accounts = YouTubeAccounts(RuntimeEnvironment.getApplication())
        // Only fetch a code; collecting stops at the first emission, which abandons the attempt.
        val first = pumpUntilDone { runBlocking { accounts.signIn().first() } }
        println("PROBE sign-in first step: $first")
        println("PROBE accounts: ${pumpUntilDone { runBlocking { accounts.accounts() } }}")
    }

    @Test
    fun probeSignedOutFeeds() {
        GlobalPreferences.instance(RuntimeEnvironment.getApplication())
        val content = YouTubeServiceManager.instance().contentService
        report("search") { content.getSearch("veritasium") }
        reportObs("trending") { content.trendingObserve }
        reportObs("shorts") { content.shortsObserve.map { listOf(it) } }
        reportObs("sports") { content.sportsObserve }
        reportObs("live") { content.liveObserve }
        reportObs("music") { content.musicObserve }
        reportObs("news") { content.newsObserve }
        reportObs("gaming") { content.gamingObserve }
    }

    // RxHelper delivers on the main thread, which Robolectric only runs when the test pumps it.
    private fun reportObs(name: String, source: () -> Observable<List<MediaGroup>>) = report(name) {
        val result = mutableListOf<MediaGroup>()
        var done = false
        var error: Throwable? = null
        val sub = source().subscribe({ result += it }, { error = it; done = true }, { done = true })
        val deadline = System.currentTimeMillis() + 40_000
        while (!done && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(50)
        }
        sub.dispose()
        error?.let { throw it }
        if (!done) println("PROBE $name (timed out, partial)")
        result
    }

    /** Runs [block] off the main thread while pumping the main looper, which RxHelper delivers on. */
    private fun <T> pumpUntilDone(block: () -> T): T {
        val future = Executors.newSingleThreadExecutor().submit(Callable(block))
        val deadline = System.currentTimeMillis() + 120_000
        while (!future.isDone && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(50)
        }
        return future.get(1, TimeUnit.SECONDS)
    }

    private fun report(name: String, load: () -> List<MediaGroup?>?) {
        val groups = runCatching(load).getOrElse { println("PROBE $name FAILED $it"); return }
        val videos = groups.orEmpty().sumOf { g -> g?.mediaItems.orEmpty().count { it.videoId != null } }
        println("PROBE $name groups=${groups?.size} videos=$videos rows=${groups.orEmpty().take(4).map { "'${it?.title}':${it?.mediaItems?.size}" }}")
        groups.orEmpty().firstOrNull { !it?.mediaItems.isNullOrEmpty() }?.mediaItems?.take(2)?.forEach {
            println("PROBE   $name e.g. '${it.title}' by ${it.author} id=${it.videoId}")
        }
    }
}
