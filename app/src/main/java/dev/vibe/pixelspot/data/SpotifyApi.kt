package dev.vibe.pixelspot.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Me(val id: String, val name: String, val imageUrl: String?)

data class TrackItem(
    val id: String,
    val uri: String,
    val name: String,
    val artists: String,
    val album: String,
    val imageUrl: String?,
    val durationMs: Long,
    /** A song from someone's phone added to Spotify ("local files"); Spotify has no artwork for it. */
    val isLocal: Boolean = false,
)

data class PlaylistItem(
    val id: String,
    val uri: String,
    val name: String,
    val owner: String,
    val ownerId: String = "",
    val collaborative: Boolean = false,
    val imageUrl: String?,
    val trackCount: Int,
)

data class AlbumItem(
    val id: String,
    val uri: String,
    val name: String,
    val artists: String,
    val imageUrl: String?,
    val year: String,
)

data class Page<T>(val items: List<T>, val total: Int, val hasMore: Boolean)

data class SearchResults(
    val tracks: List<TrackItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
    val playlists: List<PlaylistItem> = emptyList(),
)

data class QueueData(val current: TrackItem?, val items: List<TrackItem>)

class ApiException(val code: Int, message: String) : Exception(message)

/**
 * Spotify Web API calls, using the endpoint names after Spotify's February 2026 changes
 * (playlist "tracks" became "items", and the track inside each entry is under "item").
 */
class SpotifyApi(private val auth: SpotifyAuth) {

    suspend fun me(): Me {
        val j = get("/me")
        return Me(j.getString("id"), j.optString("display_name").ifBlank { j.getString("id") }, j.optJSONArray("images").firstImage())
    }

    suspend fun myPlaylists(offset: Int = 0): Page<PlaylistItem> {
        val j = get("/me/playlists?limit=50&offset=$offset")
        return j.page { it.toPlaylist() }
    }

    suspend fun playlist(id: String): PlaylistItem = get("/playlists/$id?fields=id,uri,name,owner(display_name,id),images,items(total),tracks(total)").toPlaylist()!!

    /** Only works for playlists you own or collaborate on (a 2026 Spotify rule). */
    suspend fun playlistTracks(id: String, offset: Int = 0): Page<TrackItem> {
        val j = get("/playlists/$id/items?limit=50&offset=$offset")
        return j.page { entry -> (entry.optJSONObject("item") ?: entry.optJSONObject("track"))?.toTrack() }
    }

    suspend fun likedSongs(offset: Int = 0): Page<TrackItem> {
        val j = get("/me/tracks?limit=50&offset=$offset")
        return j.page { it.optJSONObject("track")?.toTrack() }
    }

    suspend fun savedAlbums(offset: Int = 0): Page<AlbumItem> {
        val j = get("/me/albums?limit=50&offset=$offset")
        return j.page { it.optJSONObject("album")?.toAlbum() }
    }

    suspend fun album(id: String): Pair<AlbumItem, List<TrackItem>> {
        val j = get("/albums/$id")
        val album = j.toAlbum()!!
        val tracks = j.optJSONObject("tracks")?.optJSONArray("items").objects().mapNotNull { t ->
            t.toTrack(fallbackAlbum = album.name, fallbackImage = album.imageUrl)
        }
        return album to tracks
    }

    /** Every song link in a playlist you own or collaborate on (used to avoid adding duplicates). */
    suspend fun playlistUris(id: String): Set<String> {
        val uris = mutableSetOf<String>()
        var offset = 0
        do {
            val j = try {
                get("/playlists/$id/items?limit=50&offset=$offset&fields=items(item(uri),track(uri)),next")
            } catch (e: ApiException) {
                if (e.code != 400) throw e
                get("/playlists/$id/items?limit=50&offset=$offset") // in case Spotify rejects the field filter
            }
            j.optJSONArray("items").objects().forEach { entry ->
                (entry.optJSONObject("item") ?: entry.optJSONObject("track"))?.optString("uri")
                    ?.takeIf { it.isNotBlank() }?.let(uris::add)
            }
            offset += 50
        } while (!j.isNull("next") && j.has("next") && offset < 10_000)
        return uris
    }

    /** Adds a song to the end of a playlist you own or collaborate on. */
    suspend fun addToPlaylist(playlistId: String, trackUri: String) {
        request("POST", "/playlists/$playlistId/items", JSONObject().put("uris", JSONArray().put(trackUri)))
    }

    /** What's playing and what's next in the Spotify app. */
    suspend fun queue(): QueueData {
        val j = get("/me/player/queue")
        return QueueData(
            current = j.optJSONObject("currently_playing")?.toTrack(),
            items = j.optJSONArray("queue").objects().mapNotNull { it.toTrack() },
        )
    }

    suspend fun recentlyPlayed(): List<TrackItem> =
        get("/me/player/recently-played?limit=30").optJSONArray("items").objects()
            .mapNotNull { it.optJSONObject("track")?.toTrack() }
            .distinctBy { it.uri }

    suspend fun topTracks(): List<TrackItem> =
        get("/me/top/tracks?limit=20&time_range=short_term").optJSONArray("items").objects().mapNotNull { it.toTrack() }

    /** Search returns at most 10 results per type since February 2026. */
    suspend fun search(query: String): SearchResults {
        val q = URLEncoder.encode(query, "UTF-8")
        val j = get("/search?q=$q&type=track,album,playlist&limit=10")
        return SearchResults(
            tracks = j.optJSONObject("tracks")?.optJSONArray("items").objects().mapNotNull { it.toTrack() },
            albums = j.optJSONObject("albums")?.optJSONArray("items").objects().mapNotNull { it.toAlbum() },
            playlists = j.optJSONObject("playlists")?.optJSONArray("items").objects().mapNotNull { it.toPlaylist() },
        )
    }

    // --- JSON helpers ---

    private fun <T> JSONObject.page(map: (JSONObject) -> T?): Page<T> {
        val items = optJSONArray("items").objects().mapNotNull(map)
        return Page(items, optInt("total", items.size), !isNull("next") && has("next"))
    }

    private fun JSONObject.toTrack(fallbackAlbum: String = "", fallbackImage: String? = null): TrackItem? {
        val uri = optString("uri").takeIf {
            it.startsWith("spotify:track:") || it.startsWith("spotify:episode:") || it.startsWith("spotify:local:")
        } ?: return null
        val albumObj = optJSONObject("album")
        return TrackItem(
            id = optString("id"),
            uri = uri,
            name = optString("name"),
            artists = optJSONArray("artists").objects().joinToString(", ") { it.optString("name") },
            album = albumObj?.optString("name") ?: fallbackAlbum,
            imageUrl = albumObj?.optJSONArray("images").firstImage() ?: optJSONArray("images").firstImage() ?: fallbackImage,
            durationMs = optLong("duration_ms"),
            isLocal = optBoolean("is_local") || uri.startsWith("spotify:local:"),
        )
    }

    private fun JSONObject.toPlaylist(): PlaylistItem? {
        val uri = optString("uri").takeIf { it.isNotBlank() } ?: return null
        val counts = optJSONObject("items") ?: optJSONObject("tracks")
        return PlaylistItem(
            id = optString("id"),
            uri = uri,
            name = optString("name"),
            owner = optJSONObject("owner")?.optString("display_name").orEmpty(),
            ownerId = optJSONObject("owner")?.optString("id").orEmpty(),
            collaborative = optBoolean("collaborative"),
            imageUrl = optJSONArray("images").firstImage(),
            trackCount = counts?.optInt("total") ?: 0,
        )
    }

    private fun JSONObject.toAlbum(): AlbumItem? {
        val uri = optString("uri").takeIf { it.isNotBlank() } ?: return null
        return AlbumItem(
            id = optString("id"),
            uri = uri,
            name = optString("name"),
            artists = optJSONArray("artists").objects().joinToString(", ") { it.optString("name") },
            imageUrl = optJSONArray("images").firstImage(),
            year = optString("release_date").take(4),
        )
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONArray?.firstImage(): String? =
        objects().firstOrNull()?.optString("url")?.takeIf { it.isNotBlank() }

    private suspend fun get(path: String): JSONObject = request("GET", path, null)

    private suspend fun request(method: String, path: String, body: JSONObject?): JSONObject {
        val token = auth.accessToken() ?: throw ApiException(401, "Not logged in")
        return withContext(Dispatchers.IO) {
            val conn = URL("https://api.spotify.com/v1$path").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toString().toByteArray()) }
                }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val msg = runCatching { JSONObject(text).getJSONObject("error").optString("message") }.getOrNull()
                    throw ApiException(code, msg?.takeIf { it.isNotBlank() } ?: "Spotify error $code")
                }
                if (text.isBlank()) JSONObject() else JSONObject(text)
            } finally {
                conn.disconnect()
            }
        }
    }
}
