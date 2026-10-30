package dev.vibe.pixelspot

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.vibe.pixelspot.data.AlbumItem
import dev.vibe.pixelspot.data.ApiException
import dev.vibe.pixelspot.data.Me
import dev.vibe.pixelspot.data.PlaylistItem
import dev.vibe.pixelspot.data.Prefs
import dev.vibe.pixelspot.data.SearchResults
import dev.vibe.pixelspot.data.SpotifyApi
import dev.vibe.pixelspot.data.SpotifyAuth
import dev.vibe.pixelspot.data.TrackItem
import dev.vibe.pixelspot.data.UiSettings
import dev.vibe.pixelspot.player.RemotePlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Screens you can open on top of the three main tabs. */
sealed interface Detail {
    data class Playlist(val item: PlaylistItem) : Detail
    data class Album(val item: AlbumItem) : Detail
    data object Liked : Detail
}

enum class Tab { HOME, SEARCH, LIBRARY, SETTINGS }

/** A list of tracks plus where to play them from. */
data class Collection(
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val contextUri: String?,
    val tracks: List<TrackItem> = emptyList(),
    val total: Int = 0,
    val loading: Boolean = true,
    val canLoadMore: Boolean = false,
    /** Shown instead of tracks, e.g. when Spotify won't reveal a playlist's songs. */
    val notice: String? = null,
)

data class HomeData(
    val recent: List<TrackItem> = emptyList(),
    val top: List<TrackItem> = emptyList(),
    val playlists: List<PlaylistItem> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

data class LibraryData(
    val playlists: List<PlaylistItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
    val likedCount: Int = 0,
    val loading: Boolean = true,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    val auth = SpotifyAuth(prefs)
    private val api = SpotifyApi(auth)
    val player = RemotePlayer(app)

    val settings: StateFlow<UiSettings> = prefs.settings

    private val _loggedIn = MutableStateFlow(auth.isLoggedIn)
    val loggedIn = _loggedIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    private val _me = MutableStateFlow<Me?>(null)
    val me = _me.asStateFlow()

    private val _tab = MutableStateFlow(Tab.HOME)
    val tab = _tab.asStateFlow()

    private val _details = MutableStateFlow<List<Detail>>(emptyList())
    val details = _details.asStateFlow()

    private val _home = MutableStateFlow(HomeData())
    val home = _home.asStateFlow()

    private val _library = MutableStateFlow(LibraryData())
    val library = _library.asStateFlow()

    private val _collection = MutableStateFlow<Collection?>(null)
    val collection = _collection.asStateFlow()

    private val _search = MutableStateFlow(SearchResults())
    val search = _search.asStateFlow()
    private val _searching = MutableStateFlow(false)
    val searching = _searching.asStateFlow()
    private var searchJob: Job? = null
    private var collectionJob: Job? = null

    init {
        if (auth.isLoggedIn) loadEverything()
    }

    // --- Login ---

    fun loginUri(clientId: String): Uri = auth.buildLoginUri(clientId.trim())

    fun handleRedirect(uri: Uri) {
        viewModelScope.launch {
            auth.handleRedirect(uri)
                .onSuccess {
                    _loginError.value = null
                    _needsPlaylistPermission.value = !auth.canEditPlaylists
                    _loggedIn.value = true
                    loadEverything()
                }
                .onFailure { _loginError.value = it.message }
        }
    }

    fun logout() {
        player.disconnect()
        auth.logout()
        _loggedIn.value = false
        _home.value = HomeData()
        _library.value = LibraryData()
        _details.value = emptyList()
        _tab.value = Tab.HOME
    }

    fun connectPlayer(activity: android.content.Context) {
        val id = prefs.clientId ?: return
        if (_loggedIn.value) player.connect(activity, id)
    }

    private fun loadEverything() {
        viewModelScope.launch { runCatching { api.me() }.onSuccess { _me.value = it } }
        refreshHome()
        refreshLibrary()
    }

    fun refreshHome() {
        viewModelScope.launch {
            _home.update { it.copy(loading = true, error = null) }
            try {
                val recent = runCatching { api.recentlyPlayed() }.getOrDefault(emptyList())
                val top = runCatching { api.topTracks() }.getOrDefault(emptyList())
                val playlists = api.myPlaylists().items
                _home.value = HomeData(recent, top, playlists, loading = false)
            } catch (e: Exception) {
                _home.update { it.copy(loading = false, error = friendly(e)) }
            }
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _library.update { it.copy(loading = true) }
            val playlists = mutableListOf<PlaylistItem>()
            runCatching {
                var offset = 0
                do {
                    val page = api.myPlaylists(offset)
                    playlists += page.items
                    offset += 50
                } while (page.hasMore && offset < 1000)
            }
            val albums = runCatching { api.savedAlbums().items }.getOrDefault(emptyList())
            val liked = runCatching { api.likedSongs().total }.getOrDefault(0)
            _library.value = LibraryData(playlists, albums, liked, loading = false)
            _playlistContents.value = emptyMap() // re-check playlists next time they're needed
        }
    }

    // --- Navigation ---

    fun selectTab(t: Tab) {
        _tab.value = t
        _details.value = emptyList()
    }

    fun open(detail: Detail) {
        _details.update { it + detail }
        loadCollection(detail)
    }

    /** Returns false when there was nothing to go back from. */
    fun back(): Boolean {
        val stack = _details.value
        if (stack.isEmpty()) {
            if (_tab.value != Tab.HOME) { _tab.value = Tab.HOME; return true }
            return false
        }
        _details.value = stack.dropLast(1)
        stack.dropLast(1).lastOrNull()?.let { loadCollection(it) } ?: run { _collection.value = null }
        return true
    }

    private fun loadCollection(detail: Detail) {
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            when (detail) {
                is Detail.Playlist -> {
                    val p = detail.item
                    _collection.value = Collection(p.name, "${p.owner} · ${p.trackCount} songs", p.imageUrl, p.uri, total = p.trackCount)
                    try {
                        val page = api.playlistTracks(p.id)
                        _collection.update { it?.copy(tracks = page.items, total = page.total, loading = false, canLoadMore = page.hasMore) }
                    } catch (e: ApiException) {
                        val notice = if (e.code == 403 || e.code == 404)
                            "Spotify only lets apps list the songs of playlists you made or collaborate on. You can still play this playlist."
                        else friendly(e)
                        _collection.update { it?.copy(loading = false, notice = notice) }
                    } catch (e: Exception) {
                        _collection.update { it?.copy(loading = false, notice = friendly(e)) }
                    }
                }
                is Detail.Album -> {
                    val a = detail.item
                    _collection.value = Collection(a.name, listOf(a.artists, a.year).filter { it.isNotBlank() }.joinToString(" · "), a.imageUrl, a.uri)
                    runCatching { api.album(a.id) }
                        .onSuccess { (_, tracks) -> _collection.update { it?.copy(tracks = tracks, total = tracks.size, loading = false) } }
                        .onFailure { e -> _collection.update { it?.copy(loading = false, notice = friendly(e)) } }
                }
                Detail.Liked -> {
                    val userId = _me.value?.id ?: runCatching { api.me() }.getOrNull()?.also { _me.value = it }?.id
                    _collection.value = Collection("Liked Songs", "", null, userId?.let { "spotify:user:$it:collection" })
                    runCatching { api.likedSongs() }
                        .onSuccess { page ->
                            _collection.update { it?.copy(tracks = page.items, total = page.total, subtitle = "${page.total} songs", loading = false, canLoadMore = page.hasMore) }
                        }
                        .onFailure { e -> _collection.update { it?.copy(loading = false, notice = friendly(e)) } }
                }
            }
        }
    }

    fun loadMore() {
        val c = _collection.value ?: return
        if (!c.canLoadMore || c.loading) return
        val detail = _details.value.lastOrNull() ?: return
        _collection.update { it?.copy(loading = true) }
        viewModelScope.launch {
            runCatching {
                when (detail) {
                    is Detail.Playlist -> api.playlistTracks(detail.item.id, c.tracks.size)
                    Detail.Liked -> api.likedSongs(c.tracks.size)
                    is Detail.Album -> null
                }
            }.onSuccess { page ->
                _collection.update { cur ->
                    cur?.copy(
                        tracks = cur.tracks + (page?.items ?: emptyList()),
                        loading = false,
                        canLoadMore = page?.hasMore == true,
                    )
                }
            }.onFailure { _collection.update { it?.copy(loading = false, canLoadMore = false) } }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _search.value = SearchResults()
            _searching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // wait until typing pauses
            _searching.value = true
            runCatching { api.search(query) }.onSuccess { _search.value = it }
            _searching.value = false
        }
    }

    // --- Playback ---

    fun playCollection(c: Collection, index: Int = 0) {
        val track = c.tracks.getOrNull(index)
        if (c.contextUri != null) player.playInContext(c.contextUri, index, track?.uri)
        else track?.let { player.play(it.uri) }
    }

    fun shuffleCollection(c: Collection) {
        val uri = c.contextUri ?: return
        player.play(uri)
        if (player.nowPlaying.value?.isShuffling != true) player.toggleShuffle()
    }

    // --- Add to playlist ---

    /** True when Spotify hasn't given this app permission to change playlists yet. */
    private val _needsPlaylistPermission = MutableStateFlow(!auth.canEditPlaylists)
    val needsPlaylistPermission = _needsPlaylistPermission.asStateFlow()

    /** Spotify only lets apps change playlists you made or collaborate on. */
    fun editablePlaylists(): List<PlaylistItem> {
        val myId = _me.value?.id
        return _library.value.playlists.filter { (myId != null && it.ownerId == myId) || it.collaborative }
    }

    /** Song links in each of your playlists, filled in as the add-to-playlist sheet needs them. */
    private val _playlistContents = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val playlistContents = _playlistContents.asStateFlow()

    /** Looks up which playlists already contain songs (only ones not checked yet this session). */
    fun loadPlaylistContents(playlists: List<PlaylistItem>) {
        playlists.filter { it.id !in _playlistContents.value }.forEach { p ->
            viewModelScope.launch {
                runCatching { api.playlistUris(p.id) }
                    .onSuccess { uris -> _playlistContents.update { it + (p.id to uris) } }
            }
        }
    }

    fun addToPlaylist(playlist: PlaylistItem, trackUri: String, onResult: (String) -> Unit) {
        if (trackUri.startsWith("spotify:local:")) {
            onResult("Spotify doesn't let apps add local files to playlists.")
            return
        }
        if (_playlistContents.value[playlist.id]?.contains(trackUri) == true) {
            onResult("Already in ${playlist.name}")
            return
        }
        // Count it as added straight away, so a second tap can't add it twice.
        _playlistContents.update { it + (playlist.id to (it[playlist.id].orEmpty() + trackUri)) }
        viewModelScope.launch {
            try {
                api.addToPlaylist(playlist.id, trackUri)
                val bump = { p: PlaylistItem -> if (p.id == playlist.id) p.copy(trackCount = p.trackCount + 1) else p }
                _library.update { it.copy(playlists = it.playlists.map(bump)) }
                _home.update { it.copy(playlists = it.playlists.map(bump)) }
                onResult("Added to ${playlist.name}")
            } catch (e: ApiException) {
                _playlistContents.update { it + (playlist.id to (it[playlist.id].orEmpty() - trackUri)) }
                if (e.code == 401 || e.code == 403) {
                    _needsPlaylistPermission.value = true
                    onResult("Spotify needs your OK to edit playlists first.")
                } else onResult(friendly(e))
            } catch (e: Exception) {
                _playlistContents.update { it + (playlist.id to (it[playlist.id].orEmpty() - trackUri)) }
                onResult(friendly(e))
            }
        }
    }

    fun updateSettings(change: (UiSettings) -> UiSettings) = prefs.update(change)

    private fun friendly(e: Throwable): String = when {
        e is ApiException && e.code == 403 ->
            "Spotify said no (403). Make sure your Spotify account is added under \"User Management\" in the developer dashboard."
        e is ApiException && e.code == 429 -> "Spotify says we're asking too often. Wait a minute and try again."
        e is java.io.IOException -> "No internet connection."
        else -> e.message ?: "Something went wrong."
    }

    override fun onCleared() {
        player.disconnect()
    }
}
