package io.github.shivamingale3.mytube

import android.app.Application
import io.github.shivamingale3.mytube.youtube.YouTubeAccounts
import io.github.shivamingale3.mytube.youtube.YouTubeSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class MyTubeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyTubeApp)
            modules(appModule)
        }
    }
}

private val appModule = module {
    single { YouTubeSource(androidContext()) }
    single { YouTubeAccounts(androidContext()) }
    viewModel { DiagnosticsViewModel(get(), get()) }
}
