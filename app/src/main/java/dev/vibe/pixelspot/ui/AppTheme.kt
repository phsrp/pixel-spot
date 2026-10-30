package dev.vibe.pixelspot.ui

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.vibe.pixelspot.data.ColorSource
import dev.vibe.pixelspot.data.ThemeMode
import dev.vibe.pixelspot.data.UiSettings
import dev.vibe.pixelspot.pixelplay.presentation.components.LocalMaterialTheme
import dev.vibe.pixelspot.pixelplay.presentation.viewmodel.ColorSchemePair
import dev.vibe.pixelspot.pixelplay.ui.theme.Typography as PixelPlayTypography
import dev.vibe.pixelspot.pixelplay.ui.theme.extractSeedColor
import dev.vibe.pixelspot.pixelplay.ui.theme.generateColorSchemeFromSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun isDark(settings: UiSettings): Boolean = when (settings.themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * Builds the colour scheme for the whole app from the user's choice:
 * album art (PixelPlayer's colour engine), the phone's wallpaper, or a fixed accent.
 */
@Composable
fun rememberColorScheme(settings: UiSettings, artwork: Bitmap?): ColorScheme {
    val dark = isDark(settings)
    val context = LocalContext.current

    val artPair by produceState<ColorSchemePair?>(null, artwork, settings.paletteStyle, settings.colorSource) {
        value = if (settings.colorSource == ColorSource.ALBUM_ART && artwork != null && !artwork.isRecycled) {
            withContext(Dispatchers.Default) {
                runCatching { generateColorSchemeFromSeed(extractSeedColor(artwork), settings.paletteStyle) }.getOrNull()
            }
        } else null
    }
    val accentPair = remember(settings.accent, settings.paletteStyle) {
        generateColorSchemeFromSeed(Color(settings.accent), settings.paletteStyle)
    }
    val wallpaper = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null

    val base = when (settings.colorSource) {
        ColorSource.ALBUM_ART -> artPair?.pick(dark) ?: wallpaper ?: accentPair.pick(dark)
        ColorSource.WALLPAPER -> wallpaper ?: accentPair.pick(dark)
        ColorSource.ACCENT -> accentPair.pick(dark)
        ColorSource.THEME -> settings.namedPalette.scheme(dark)
    }
    val scheme = if (settings.amoled && dark) base.amoled() else base
    return animated(scheme)
}

private fun ColorSchemePair.pick(dark: Boolean) = if (dark) this.dark else light

/** Pure black backgrounds for OLED screens. */
private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF202020),
)

@Composable
private fun c(target: Color): Color {
    val v by animateColorAsState(target, tween(600), label = "scheme")
    return v
}

/** Fades every important colour so song changes recolour the app smoothly. */
@Composable
private fun animated(s: ColorScheme): ColorScheme = s.copy(
    primary = c(s.primary),
    onPrimary = c(s.onPrimary),
    primaryContainer = c(s.primaryContainer),
    onPrimaryContainer = c(s.onPrimaryContainer),
    secondary = c(s.secondary),
    onSecondary = c(s.onSecondary),
    secondaryContainer = c(s.secondaryContainer),
    onSecondaryContainer = c(s.onSecondaryContainer),
    tertiary = c(s.tertiary),
    onTertiary = c(s.onTertiary),
    tertiaryContainer = c(s.tertiaryContainer),
    onTertiaryContainer = c(s.onTertiaryContainer),
    background = c(s.background),
    onBackground = c(s.onBackground),
    surface = c(s.surface),
    onSurface = c(s.onSurface),
    surfaceVariant = c(s.surfaceVariant),
    onSurfaceVariant = c(s.onSurfaceVariant),
    surfaceContainerLowest = c(s.surfaceContainerLowest),
    surfaceContainerLow = c(s.surfaceContainerLow),
    surfaceContainer = c(s.surfaceContainer),
    surfaceContainerHigh = c(s.surfaceContainerHigh),
    surfaceContainerHighest = c(s.surfaceContainerHighest),
    outline = c(s.outline),
    outlineVariant = c(s.outlineVariant),
    primaryFixed = c(s.primaryFixed),
    onPrimaryFixed = c(s.onPrimaryFixed),
    primaryFixedDim = c(s.primaryFixedDim),
    secondaryFixed = c(s.secondaryFixed),
    onSecondaryFixed = c(s.onSecondaryFixed),
    secondaryFixedDim = c(s.secondaryFixedDim),
    tertiaryFixed = c(s.tertiaryFixed),
    onTertiaryFixed = c(s.onTertiaryFixed),
    tertiaryFixedDim = c(s.tertiaryFixedDim),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PixelSpotTheme(settings: UiSettings, artwork: Bitmap?, content: @Composable () -> Unit) {
    val scheme = rememberColorScheme(settings, artwork)
    val typography = if (settings.roundedFont) PixelPlayTypography else Typography()

    // The app's light/dark setting can differ from the phone's, so set the status bar icons to match.
    val view = LocalView.current
    val lightBars = scheme.background.luminance() > 0.5f
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        typography = typography,
    ) {
        // PixelPlayer's components read their colours from LocalMaterialTheme.
        // Text that isn't on a card takes this colour; without it, it would stay black in dark mode.
        CompositionLocalProvider(
            LocalMaterialTheme provides scheme,
            LocalContentColor provides scheme.onBackground,
            content = content,
        )
    }
}
