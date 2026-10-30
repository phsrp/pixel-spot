package dev.vibe.pixelspot.player

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.Image
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.PlayerState
import dev.vibe.pixelspot.data.SpotifyAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RemoteStatus {
    data object Disconnected : RemoteStatus
    data object Connecting : RemoteStatus
    data object Connected : RemoteStatus
    data object SpotifyNotInstalled : RemoteStatus
    data class Failed(val message: String) : RemoteStatus
}

/** What's playing right now, as reported by the Spotify app. */
data class NowPlaying(
    val trackUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val positionMs: Long,
    /** SystemClock.elapsedRealtime() when [positionMs] was reported, to keep the bar moving. */
    val positionAt: Long,
    val isPaused: Boolean,
    val isShuffling: Boolean,
    /** 0 = off, 1 = repeat one, 2 = repeat all. */
    val repeatMode: Int,
    val canSeek: Boolean,
    val imageUri: ImageUri?,
) {
    fun livePosition(): Long =
        if (isPaused) positionMs
        else (positionMs + (SystemClock.elapsedRealtime() - positionAt)).coerceAtMost(durationMs)
}

/**
 * Controls the official Spotify app through Spotify's App Remote SDK.
 * The Spotify app does the actual playing; this class sends it commands and listens for changes.
 */
class RemotePlayer(private val context: Context) {

    private var remote: SpotifyAppRemote? = null
    private var stateSub: Subscription<PlayerState>? = null
    private var lastImageUri: ImageUri? = null

    val localArt = LocalArtCache(context)

    private val _status = MutableStateFlow<RemoteStatus>(RemoteStatus.Disconnected)
    val status: StateFlow<RemoteStatus> = _status.asStateFlow()

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _artwork = MutableStateFlow<Bitmap?>(null)
    val artwork: StateFlow<Bitmap?> = _artwork.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    /** [activity] should be the visible Activity, so Spotify can show its "allow access" screen. */
    fun connect(activity: Context, clientId: String) {
        if (remote?.isConnected == true || _status.value == RemoteStatus.Connecting) return
        if (!SpotifyAppRemote.isSpotifyInstalled(context)) {
            _status.value = RemoteStatus.SpotifyNotInstalled
            return
        }
        _status.value = RemoteStatus.Connecting
        val params = ConnectionParams.Builder(clientId)
            .setRedirectUri(SpotifyAuth.REDIRECT_URI)
            .showAuthView(true)
            .build()
        SpotifyAppRemote.connect(activity, params, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                remote = appRemote
                _status.value = RemoteStatus.Connected
                subscribe(appRemote)
            }

            override fun onFailure(error: Throwable) {
                remote = null
                _status.value = RemoteStatus.Failed(explain(error))
            }
        })
    }

    fun disconnect() {
        stateSub?.cancel()
        stateSub = null
        remote?.let { SpotifyAppRemote.disconnect(it) }
        remote = null
        _status.value = RemoteStatus.Disconnected
    }

    private fun subscribe(appRemote: SpotifyAppRemote) {
        stateSub?.cancel()
        stateSub = appRemote.playerApi.subscribeToPlayerState().setEventCallback { state ->
            val track = state.track ?: return@setEventCallback
            val previousUri = _nowPlaying.value?.trackUri
            _nowPlaying.value = NowPlaying(
                trackUri = track.uri,
                title = track.name.orEmpty(),
                artist = track.artists?.joinToString(", ") { it.name }?.ifBlank { null } ?: track.artist?.name.orEmpty(),
                album = track.album?.name.orEmpty(),
                durationMs = track.duration,
                positionMs = state.playbackPosition,
                positionAt = SystemClock.elapsedRealtime(),
                isPaused = state.isPaused,
                isShuffling = state.playbackOptions?.isShuffling == true,
                repeatMode = state.playbackOptions?.repeatMode ?: 0,
                canSeek = state.playbackRestrictions?.canSeek != false,
                imageUri = track.imageUri,
            )
            if (track.imageUri != null && track.imageUri != lastImageUri) {
                lastImageUri = track.imageUri
                appRemote.imagesApi.getImage(track.imageUri, Image.Dimension.LARGE)
                    .setResultCallback { bitmap ->
                        _artwork.value = bitmap
                        if (track.uri.startsWith("spotify:local:")) localArt.save(track.name.orEmpty(), bitmap)
                    }
            }
            if (track.uri != previousUri) refreshLiked(track.uri)
        }
    }

    private fun refreshLiked(uri: String) {
        remote?.userApi?.getLibraryState(uri)?.setResultCallback { _isLiked.value = it.isAdded }
    }

    // --- Commands ---

    fun play(uri: String) { remote?.playerApi?.play(uri) }

    /** Plays a playlist/album starting at [index]. Falls back to just the track if that fails. */
    fun playInContext(contextUri: String, index: Int, trackUri: String?) {
        val api = remote?.playerApi ?: return
        api.skipToIndex(contextUri, index).setErrorCallback {
            if (trackUri != null) api.play(trackUri) else api.play(contextUri)
        }
    }

    fun queue(uri: String) { remote?.playerApi?.queue(uri) }

    fun togglePlay() {
        val api = remote?.playerApi ?: return
        if (_nowPlaying.value?.isPaused != false) api.resume() else api.pause()
    }

    fun next() { remote?.playerApi?.skipNext() }
    fun previous() { remote?.playerApi?.skipPrevious() }
    fun seekTo(ms: Long) { remote?.playerApi?.seekTo(ms) }
    fun toggleShuffle() { remote?.playerApi?.toggleShuffle() }
    fun toggleRepeat() { remote?.playerApi?.toggleRepeat() }

    fun toggleLike() {
        val uri = _nowPlaying.value?.trackUri ?: return
        val user = remote?.userApi ?: return
        val liked = _isLiked.value
        _isLiked.value = !liked
        (if (liked) user.removeFromLibrary(uri) else user.addToLibrary(uri))
            .setErrorCallback { _isLiked.value = liked }
    }

    private fun explain(error: Throwable): String = when (error.javaClass.simpleName) {
        "CouldNotFindSpotifyApp" -> "The Spotify app isn't installed."
        "NotLoggedInException" -> "Open the Spotify app and log in first."
        "UserNotAuthorizedException" ->
            "Spotify refused the connection. Check the package name and SHA-1 fingerprint in your Spotify developer dashboard."
        "AuthenticationFailedException" -> "Spotify couldn't confirm this app. Check the redirect URI in the dashboard."
        "OfflineModeException" -> "Spotify is in offline mode."
        "SpotifyConnectionTerminatedException", "SpotifyDisconnectedException" -> "Lost the connection to Spotify."
        else -> error.message ?: error.javaClass.simpleName
    }
}
