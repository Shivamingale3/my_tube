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
import org.robolectric.Shadows.shadowOf

/** Live probe against YouTube: which feeds return videos while signed out. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeFeedProbe {
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

    private fun report(name: String, load: () -> List<MediaGroup?>?) {
        val groups = runCatching(load).getOrElse { println("PROBE $name FAILED $it"); return }
        val videos = groups.orEmpty().sumOf { g -> g?.mediaItems.orEmpty().count { it.videoId != null } }
        println("PROBE $name groups=${groups?.size} videos=$videos rows=${groups.orEmpty().take(4).map { "'${it?.title}':${it?.mediaItems?.size}" }}")
        groups.orEmpty().firstOrNull { !it?.mediaItems.isNullOrEmpty() }?.mediaItems?.take(2)?.forEach {
            println("PROBE   $name e.g. '${it.title}' by ${it.author} id=${it.videoId}")
        }
    }
}
