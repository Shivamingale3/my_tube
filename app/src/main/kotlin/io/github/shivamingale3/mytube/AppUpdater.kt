package io.github.shivamingale3.mytube

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Updates from the repository's GitHub releases: CI publishes every main build as `build-<n>`
 * with MyTube.apk attached, and this build knows its own number.
 */
class AppUpdater(private val context: Context) {

    /** The newest release's build number and APK link, or null when this build is the newest. */
    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        val json = JSONObject(get(LATEST_RELEASE).bufferedReader().use { it.readText() })
        val build = json.getString("tag_name").removePrefix("build-").toIntOrNull() ?: return@withContext null
        val assets = json.getJSONArray("assets")
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.getString("name").endsWith(".apk") }
            ?: return@withContext null
        Release(build, apk.getString("browser_download_url")).takeIf { build > BuildConfig.BUILD_NUMBER }
    }

    /** Downloads [release] and opens Android's installer for it. */
    suspend fun install(release: Release, onProgress: (Int) -> Unit) {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(dir, "MyTube.apk")
            val connection = open(release.apkUrl)
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        if (total > 0) onProgress((done * 100 / total).toInt())
                    }
                }
            }
            target
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    data class Release(val build: Int, val apkUrl: String)

    private fun get(url: String) = open(url).inputStream

    private fun open(url: String): HttpURLConnection {
        var current = url
        repeat(5) {
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "MyTube")
            }
            when (connection.responseCode) {
                in 300..399 -> current = connection.getHeaderField("Location") ?: error("Redirect without location")
                in 200..299 -> return connection
                else -> error("HTTP ${connection.responseCode}")
            }
        }
        error("Too many redirects")
    }

    private companion object {
        const val LATEST_RELEASE = "https://api.github.com/repos/Shivamingale3/my_tube/releases/latest"
    }
}
