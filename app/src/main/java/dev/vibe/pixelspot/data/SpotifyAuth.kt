package dev.vibe.pixelspot.data

import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Spotify login with "Authorization Code + PKCE" (the only flow Spotify allows for
 * phone apps since November 2025). No client secret is stored in the app.
 */
class SpotifyAuth(private val prefs: Prefs) {

    companion object {
        const val REDIRECT_URI = "pixelspot://callback"
        val SCOPES = listOf(
            "user-read-private",
            "user-library-read",
            "user-library-modify",
            "playlist-read-private",
            "playlist-read-collaborative",
            "playlist-modify-public",
            "playlist-modify-private",
            "user-read-playback-state",
            "user-modify-playback-state",
            "user-read-currently-playing",
            "user-read-recently-played",
            "user-top-read",
            "app-remote-control",
        )
    }

    private val refreshLock = Mutex()

    val isLoggedIn: Boolean get() = prefs.refreshToken != null

    /** False when the login happened before this app asked for permission to edit playlists. */
    val canEditPlaylists: Boolean
        get() = prefs.grantedScopes?.split(' ')?.contains("playlist-modify-private") ?: true

    /** Builds the Spotify login page address and remembers the PKCE secret for later. */
    fun buildLoginUri(clientId: String): Uri {
        val random = SecureRandom()
        val verifierBytes = ByteArray(64).also { random.nextBytes(it) }
        val verifier = base64Url(verifierBytes)
        val challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        val state = base64Url(ByteArray(16).also { random.nextBytes(it) })
        prefs.pkceVerifier = verifier
        prefs.pkceState = state
        prefs.clientId = clientId

        return Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("state", state)
            .appendQueryParameter("scope", SCOPES.joinToString(" "))
            .build()
    }

    /** Handles pixelspot://callback?code=... and swaps the code for tokens. */
    suspend fun handleRedirect(uri: Uri): Result<Unit> = runCatching {
        uri.getQueryParameter("error")?.let { error("Spotify said: $it") }
        val code = uri.getQueryParameter("code") ?: error("Spotify didn't send a login code")
        if (uri.getQueryParameter("state") != prefs.pkceState) error("Login check failed, please try again")
        val clientId = prefs.clientId ?: error("Client ID missing")
        val verifier = prefs.pkceVerifier ?: error("Login expired, please try again")

        val json = postForm(
            mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to REDIRECT_URI,
                "client_id" to clientId,
                "code_verifier" to verifier,
            )
        )
        saveTokens(json)
        prefs.pkceVerifier = null
        prefs.pkceState = null
    }

    /** Returns a working access token, refreshing it first if it's about to expire. */
    suspend fun accessToken(): String? = refreshLock.withLock {
        val token = prefs.accessToken
        if (token != null && System.currentTimeMillis() < prefs.tokenExpiry - 60_000) return token
        val refresh = prefs.refreshToken ?: return null
        val clientId = prefs.clientId ?: return null
        return runCatching {
            saveTokens(
                postForm(
                    mapOf(
                        "grant_type" to "refresh_token",
                        "refresh_token" to refresh,
                        "client_id" to clientId,
                    )
                )
            )
            prefs.accessToken
        }.getOrNull()
    }

    fun logout() = prefs.clearTokens()

    private fun saveTokens(json: JSONObject) {
        prefs.accessToken = json.getString("access_token")
        json.optString("scope").takeIf { it.isNotBlank() }?.let { prefs.grantedScopes = it }
        prefs.tokenExpiry = System.currentTimeMillis() + json.optLong("expires_in", 3600) * 1000
        // Spotify may or may not hand out a new refresh token; keep the old one otherwise.
        json.optString("refresh_token").takeIf { it.isNotBlank() }?.let { prefs.refreshToken = it }
    }

    private suspend fun postForm(form: Map<String, String>): JSONObject = withContext(Dispatchers.IO) {
        val body = form.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        val conn = URL("https://accounts.spotify.com/api/token").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(body.toByteArray()) }
            val ok = conn.responseCode in 200..299
            val text = (if (ok) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (!ok) {
                val msg = runCatching { JSONObject(text).optString("error_description") }.getOrNull()
                error(msg?.takeIf { it.isNotBlank() } ?: "Login failed (HTTP ${conn.responseCode})")
            }
            JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
