package dev.vibe.pixelspot.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(val version: String, val notes: String, val apkUrl: String)

sealed interface UpdateState {
    data object None : UpdateState
    data class Available(val update: AppUpdate) : UpdateState
    /** [progress] goes from 0 to 1. */
    data class Downloading(val update: AppUpdate, val progress: Float) : UpdateState
    data class Failed(val update: AppUpdate, val message: String) : UpdateState
}

/**
 * Looks for a newer release on GitHub and downloads it. Android doesn't let apps outside
 * an app store update themselves silently, so the last step is the system's Install prompt.
 */
class AppUpdater(private val context: Context) {

    companion object {
        private const val LATEST_RELEASE = "https://api.github.com/repos/phsrp/pixel-spot/releases/latest"
    }

    val currentVersion: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.None)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** "Later" hides that version until the app next opens fresh. */
    private var dismissedVersion: String? = null

    /** Returns the newer version found, or null if the app is up to date. Throws if the check fails. */
    suspend fun check(ignoreDismissed: Boolean = false): AppUpdate? {
        if (_state.value is UpdateState.Downloading) return null
        val update = fetchLatest()?.takeIf { isNewer(it.version, currentVersion) } ?: return null
        if (ignoreDismissed || update.version != dismissedVersion) _state.value = UpdateState.Available(update)
        return update
    }

    fun dismiss() {
        (_state.value as? UpdateState.Available)?.let { dismissedVersion = it.update.version }
        (_state.value as? UpdateState.Failed)?.let { dismissedVersion = it.update.version }
        _state.value = UpdateState.None
    }

    suspend fun downloadAndInstall(update: AppUpdate) {
        _state.value = UpdateState.Downloading(update, 0f)
        try {
            val apk = withContext(Dispatchers.IO) { download(update) }
            _state.value = UpdateState.None
            install(apk)
        } catch (e: Exception) {
            _state.value = UpdateState.Failed(update, e.message ?: "The download failed.")
        }
    }

    private suspend fun fetchLatest(): AppUpdate? = withContext(Dispatchers.IO) {
        val conn = URL(LATEST_RELEASE).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "PixelSpot/$currentVersion")
            if (conn.responseCode == 404) return@withContext null // nothing released yet
            if (conn.responseCode !in 200..299) error("GitHub error ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets") ?: return@withContext null
            val apkUrl = (0 until assets.length())
                .mapNotNull { assets.optJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk") }
                ?.optString("browser_download_url")
                ?: return@withContext null
            AppUpdate(json.optString("tag_name").removePrefix("v"), json.optString("body"), apkUrl)
        } finally {
            conn.disconnect()
        }
    }

    private fun download(update: AppUpdate): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // remove older downloads
        val file = File(dir, "PixelSpot-${update.version}.apk")
        val conn = URL(update.apkUrl).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true
            if (conn.responseCode !in 200..299) error("Download failed (${conn.responseCode})")
            val total = conn.contentLengthLong
            var done = 0L
            conn.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) _state.value = UpdateState.Downloading(update, done.toFloat() / total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        return file
    }

    private fun install(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Compares versions number by number, so 1.10.0 counts as newer than 1.9.0. */
    private fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split(".").map { it.toIntOrNull() ?: 0 }
        val b = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (diff != 0) return diff > 0
        }
        return false
    }
}
