package dev.vibe.pixelspot.player

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.concurrent.thread

/**
 * Spotify's Web API has no artwork for "local files" (songs from your phone added to Spotify),
 * but the Spotify app shows it while one plays. We save that picture so playlists can show it too.
 */
class LocalArtCache(context: Context) {
    private val dir = File(context.filesDir, "local-art").apply { mkdirs() }

    /** Goes up by one every time a picture is saved, so the screen knows to refresh. */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    // Keyed by song title: local-file links can be spelled slightly differently in different places.
    private fun fileFor(title: String) = File(dir, "${title.trim().lowercase().hashCode().toUInt()}.jpg")

    fun find(title: String): File? = fileFor(title).takeIf { it.exists() }

    fun save(title: String, bitmap: Bitmap) {
        if (title.isBlank()) return
        val file = fileFor(title)
        if (file.exists()) return
        thread {
            runCatching {
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                _version.value += 1
            }
        }
    }
}
