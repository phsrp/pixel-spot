package dev.vibe.pixelspot.data

import android.content.Context
import dev.vibe.pixelspot.pixelplay.data.preferences.AlbumArtPaletteStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Where the app's colours come from. */
enum class ColorSource { ALBUM_ART, WALLPAPER, ACCENT, THEME }

/** Ready-made themes. Each has a light and a dark version that follow the light/dark setting. */
enum class NamedPalette(val label: String) {
    ROSE_PINE("Rosé Pine"),
    GRUVBOX("Gruvbox"),
    CATPPUCCIN("Catppuccin"),
    NORD("Nord"),
    ATOM_ONE("Atom One"),
    ZINC("Zinc"),
}

/** Everything the user can customise. Saved on the phone, applied instantly. */
data class UiSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorSource: ColorSource = ColorSource.ALBUM_ART,
    val paletteStyle: AlbumArtPaletteStyle = AlbumArtPaletteStyle.default,
    val namedPalette: NamedPalette = NamedPalette.ROSE_PINE,
    val accent: Int = 0xFF7C5CFF.toInt(),
    val amoled: Boolean = false,
    val wavyProgress: Boolean = true,
    val roundedFont: Boolean = true,
    /** Show a simple player over the lock screen while music plays. */
    val lockScreenPlayer: Boolean = false,
    /** Keep that screen on (dimmed) like an always-on display. */
    val lockScreenKeepOn: Boolean = true,
)

/** Small wrapper around SharedPreferences for settings and login tokens. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("pixelspot", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<UiSettings> = _settings.asStateFlow()

    fun update(change: (UiSettings) -> UiSettings) {
        val s = change(_settings.value)
        sp.edit()
            .putString("themeMode", s.themeMode.name)
            .putString("colorSource", s.colorSource.name)
            .putString("paletteStyle", s.paletteStyle.storageKey)
            .putString("namedPalette", s.namedPalette.name)
            .putInt("accent", s.accent)
            .putBoolean("amoled", s.amoled)
            .putBoolean("wavyProgress", s.wavyProgress)
            .putBoolean("roundedFont", s.roundedFont)
            .putBoolean("lockScreenPlayer", s.lockScreenPlayer)
            .putBoolean("lockScreenKeepOn", s.lockScreenKeepOn)
            .apply()
        _settings.value = s
    }

    private fun readSettings(): UiSettings {
        val d = UiSettings()
        return UiSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == sp.getString("themeMode", null) } ?: d.themeMode,
            colorSource = ColorSource.entries.firstOrNull { it.name == sp.getString("colorSource", null) } ?: d.colorSource,
            paletteStyle = AlbumArtPaletteStyle.fromStorageKey(sp.getString("paletteStyle", null)),
            namedPalette = NamedPalette.entries.firstOrNull { it.name == sp.getString("namedPalette", null) } ?: d.namedPalette,
            accent = sp.getInt("accent", d.accent),
            amoled = sp.getBoolean("amoled", d.amoled),
            wavyProgress = sp.getBoolean("wavyProgress", d.wavyProgress),
            roundedFont = sp.getBoolean("roundedFont", d.roundedFont),
            lockScreenPlayer = sp.getBoolean("lockScreenPlayer", d.lockScreenPlayer),
            lockScreenKeepOn = sp.getBoolean("lockScreenKeepOn", d.lockScreenKeepOn),
        )
    }

    var clientId: String?
        get() = sp.getString("clientId", null)?.takeIf { it.isNotBlank() }
        set(v) = sp.edit().putString("clientId", v?.trim()).apply()

    var accessToken: String?
        get() = sp.getString("accessToken", null)
        set(v) = sp.edit().putString("accessToken", v).apply()

    var refreshToken: String?
        get() = sp.getString("refreshToken", null)
        set(v) = sp.edit().putString("refreshToken", v).apply()

    /** Permissions Spotify actually granted, space-separated (null = not known yet). */
    var grantedScopes: String?
        get() = sp.getString("grantedScopes", null)
        set(v) = sp.edit().putString("grantedScopes", v).apply()

    /** Wall-clock millis when the access token stops working. */
    var tokenExpiry: Long
        get() = sp.getLong("tokenExpiry", 0)
        set(v) = sp.edit().putLong("tokenExpiry", v).apply()

    /** PKCE secret kept between opening the login page and getting the redirect back. */
    var pkceVerifier: String?
        get() = sp.getString("pkceVerifier", null)
        set(v) = sp.edit().putString("pkceVerifier", v).apply()

    var pkceState: String?
        get() = sp.getString("pkceState", null)
        set(v) = sp.edit().putString("pkceState", v).apply()

    fun clearTokens() {
        sp.edit().remove("accessToken").remove("refreshToken").remove("tokenExpiry").remove("grantedScopes").apply()
    }
}
