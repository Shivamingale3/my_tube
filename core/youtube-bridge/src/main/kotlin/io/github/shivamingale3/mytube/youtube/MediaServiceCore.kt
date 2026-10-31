package io.github.shivamingale3.mytube.youtube

import android.content.Context
import com.liskovsoft.mediaserviceinterfaces.ServiceManager
import com.liskovsoft.sharedutils.prefs.GlobalPreferences
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager

/** One-time setup shared by every bridge class that talks to MediaServiceCore. */
internal object MediaServiceCore {
    fun services(context: Context): ServiceManager {
        // MediaServiceCore reads its settings, caches and saved accounts through this singleton.
        GlobalPreferences.instance(context.applicationContext)
        return YouTubeServiceManager.instance()
    }
}
