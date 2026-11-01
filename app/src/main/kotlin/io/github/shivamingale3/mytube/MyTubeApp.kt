package io.github.shivamingale3.mytube

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import io.github.shivamingale3.mytube.screens.AppSettings
import io.github.shivamingale3.mytube.screens.HomeViewModel
import io.github.shivamingale3.mytube.screens.SearchViewModel
import io.github.shivamingale3.mytube.screens.PlayerViewModel
import io.github.shivamingale3.mytube.player.PlayerController
import io.github.shivamingale3.mytube.youtube.YouTubeAccounts
import io.github.shivamingale3.mytube.youtube.YouTubePlayback
import io.github.shivamingale3.mytube.youtube.YouTubeSource
import io.github.shivamingale3.mytube.youtube.warmUpYouTube
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class MyTubeApp : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyTubeApp)
            modules(appModule)
        }
        // Start YouTube's one-time setup now, behind the splash screen, not on the first play.
        warmUpYouTube(this)
    }

    // Images load over Ktor rather than OkHttp: SmartTube's code pins OkHttp 3, which Coil can't use.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
}

private val appModule = module {
    single { YouTubeSource(androidContext()) }
    single { YouTubeAccounts(androidContext()) }
    single { YouTubePlayback(androidContext()) }
    single { PlayerController(androidContext()) }
    single { AppSettings(androidContext()) }
    single { AppUpdater(androidContext()) }
    viewModel { ShellViewModel(get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { PlayerViewModel(get(), get(), get()) }
}
