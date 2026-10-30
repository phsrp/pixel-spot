package dev.vibe.pixelspot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.vibe.pixelspot.lock.LockScreenService
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.vibe.pixelspot.data.ColorSource
import dev.vibe.pixelspot.data.NamedPalette
import dev.vibe.pixelspot.data.ThemeMode
import dev.vibe.pixelspot.data.UiSettings
import dev.vibe.pixelspot.pixelplay.data.preferences.AlbumArtPaletteStyle
import dev.vibe.pixelspot.pixelplay.ui.theme.GoogleSansRounded
import dev.vibe.pixelspot.player.RemoteStatus

private val Accents = listOf(
    0xFF7C5CFF, 0xFF1DB954, 0xFFFF5C8A, 0xFFFF8A3D, 0xFF3DA9FF, 0xFFE5C33B, 0xFF00BFA5, 0xFFB0B0B0,
).map { Color(it) }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    settings: UiSettings,
    status: RemoteStatus,
    userName: String?,
    onChange: ((UiSettings) -> UiSettings) -> Unit,
    onReconnect: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, fontFamily = GoogleSansRounded,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp))

        Group("Theme") {
            ChipRow(ThemeMode.entries, settings.themeMode, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) { m -> onChange { it.copy(themeMode = m) } }
            SwitchRow("Pure black (AMOLED)", "Black backgrounds in dark mode", settings.amoled) { v -> onChange { it.copy(amoled = v) } }
        }

        Group("Colours come from") {
            ChipRow(ColorSource.entries, settings.colorSource, {
                when (it) { ColorSource.ALBUM_ART -> "Album art"; ColorSource.WALLPAPER -> "Wallpaper"; ColorSource.ACCENT -> "Fixed colour"; ColorSource.THEME -> "Theme" }
            }) { s -> onChange { it.copy(colorSource = s) } }
            if (settings.colorSource == ColorSource.THEME) {
                Text("Theme (follows light or dark mode)", style = MaterialTheme.typography.labelLarge)
                ChipRow(NamedPalette.entries, settings.namedPalette, { it.label }) { p -> onChange { it.copy(namedPalette = p) } }
            } else {
                Text("Palette style", style = MaterialTheme.typography.labelLarge)
                ChipRow(AlbumArtPaletteStyle.entries, settings.paletteStyle, { it.label }) { s -> onChange { it.copy(paletteStyle = s) } }
                Text("Accent (used for \"Fixed colour\" and as a fallback)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Accents.forEach { color ->
                        val selected = color.toArgb() == settings.accent
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { onChange { it.copy(accent = color.toArgb()) } }
                        )
                    }
                }
            }
        }

        Group("Player") {
            SwitchRow("Wavy progress bar", "The seek bar waves while music plays", settings.wavyProgress) { v -> onChange { it.copy(wavyProgress = v) } }
            SwitchRow("Rounded font", "PixelPlayer's Google Sans Flex (rounded)", settings.roundedFont) { v -> onChange { it.copy(roundedFont = v) } }
        }

        LockScreenGroup(settings, onChange)

        Group("Spotify") {
            val statusText = when (status) {
                RemoteStatus.Connected -> "Connected to the Spotify app"
                RemoteStatus.Connecting -> "Connecting…"
                RemoteStatus.Disconnected -> "Not connected"
                RemoteStatus.SpotifyNotInstalled -> "The Spotify app isn't installed"
                is RemoteStatus.Failed -> status.message
            }
            if (userName != null) Text("Logged in as $userName", style = MaterialTheme.typography.bodyLarge)
            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onReconnect) { Text("Reconnect") }
                OutlinedButton(onClick = onLogout) { Text("Log out") }
            }
        }

        Group("About") {
            Text(
                "Pixel Spot controls the official Spotify app, which does the playing. Lyrics, Spotify-made playlists' song lists " +
                    "and recommendations aren't available to apps like this one.\n\n" +
                    "Interface pieces adapted from PixelPlayer by theovilardo (MIT licence, version from before May 2026).",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LockScreenGroup(settings: UiSettings, onChange: ((UiSettings) -> UiSettings) -> Unit) {
    val context = LocalContext.current
    fun apply(change: (UiSettings) -> UiSettings) {
        onChange(change)
        LockScreenService.sync(context)
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        apply { it.copy(lockScreenPlayer = granted) }
    }
    // Android 14+ lets people switch off "full screen" notifications per app; we need them.
    val nm = context.getSystemService(NotificationManager::class.java)
    var fullScreenAllowed by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        fullScreenAllowed = Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent()
        onPauseOrDispose { }
    }

    Group("Lock screen") {
        SwitchRow(
            "Lock screen player",
            "Shows a simple black player with a clock over the lock screen while music plays",
            settings.lockScreenPlayer,
        ) { on ->
            if (on && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                apply { it.copy(lockScreenPlayer = on) }
            }
        }
        if (settings.lockScreenPlayer) {
            SwitchRow(
                "Always on",
                "Keeps it showing, dimmed, until the music stops (uses more battery)",
                settings.lockScreenKeepOn,
            ) { v -> apply { it.copy(lockScreenKeepOn = v) } }
            if (!fullScreenAllowed) {
                Text("Android is blocking this app from appearing over the lock screen.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
                    runCatching { context.startActivity(intent) }
                }) { Text("Allow it") }
            }
            Text(
                "A small \"Lock screen player is on\" notification stays while this is switched on. You can hide it by long-pressing it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = GoogleSansRounded,
                color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) }, shape = RoundedCornerShape(50))
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
