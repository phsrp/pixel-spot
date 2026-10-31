package dev.vibe.pixelspot.lock

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vibe.pixelspot.data.Prefs
import dev.vibe.pixelspot.data.ThemeMode
import dev.vibe.pixelspot.pixelplay.ui.theme.GoogleSansRounded
import dev.vibe.pixelspot.player.NowPlaying
import dev.vibe.pixelspot.player.RemotePlayer
import dev.vibe.pixelspot.ui.PixelSpotTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/** A simple, mostly-black player that sits on top of the lock screen while music plays. */
class LockPlayerActivity : ComponentActivity() {

    companion object {
        /** True from opening until closing, including while the screen is off. */
        @Volatile var isOpen = false
            private set
    }

    private lateinit var prefs: Prefs
    private lateinit var player: RemotePlayer

    // Unlocking the phone (fingerprint, PIN...) should take you to your phone, not to this screen.
    private val unlocked = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        enableEdgeToEdge()
        prefs = Prefs(this)
        player = RemotePlayer(applicationContext)
        getSystemService(NotificationManager::class.java).cancel(LockScreenService.ID_SHOW)

        val keepOn = prefs.settings.value.lockScreenKeepOn
        if (unlockedWhileAwake()) {
            finish()
            return
        }
        isOpen = true
        ContextCompat.registerReceiver(this, unlocked, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED)

        if (keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val base by prefs.settings.collectAsStateWithLifecycle()
            val artwork by player.artwork.collectAsStateWithLifecycle()
            val np by player.nowPlaying.collectAsStateWithLifecycle()
            // Always dark and pure black here: black pixels are switched off on OLED screens.
            val settings = base.copy(themeMode = ThemeMode.DARK, amoled = true)
            PixelSpotTheme(settings, artwork) {
                LockPlayer(
                    np = np,
                    artwork = artwork,
                    wavy = settings.wavyProgress,
                    onPlayPause = player::togglePlay,
                    onNext = player::next,
                    onPrevious = player::previous,
                    onUnlock = ::unlock,
                    onDim = { dim -> setBrightness(if (dim && keepOn) 0.02f else -1f) },
                )
            }
            // Don't stay on "Connecting…" if Spotify isn't answering.
            LaunchedEffect(np == null) {
                if (np == null) {
                    delay(15_000)
                    finish()
                }
            }
            // Leave once the music has been paused for a minute, so the screen can sleep normally.
            LaunchedEffect(np?.isPaused) {
                if (np?.isPaused == true) {
                    delay(60_000)
                    finish()
                }
            }
        }
    }

    /**
     * True if the phone is on and not locked, so this screen shouldn't be in the way: a quick
     * fingerprint unlock, say. While the screen is off the lock may just not have kicked in yet.
     */
    private fun unlockedWhileAwake(): Boolean =
        !prefs.settings.value.lockScreenKeepOn &&
            getSystemService(PowerManager::class.java).isInteractive &&
            !getSystemService(KeyguardManager::class.java).isKeyguardLocked

    private fun setBrightness(value: Float) {
        window.attributes = window.attributes.apply { screenBrightness = value }
    }

    private fun unlock() {
        val km = getSystemService(KeyguardManager::class.java)
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = finish()
        })
        if (!km.isKeyguardLocked) finish()
    }

    override fun onStart() {
        super.onStart()
        if (unlockedWhileAwake()) finish()
        if (isFinishing) return
        prefs.clientId?.let { player.connect(this, it) }
    }

    override fun onStop() {
        super.onStop()
        player.disconnect()
    }

    override fun onDestroy() {
        isOpen = false
        runCatching { unregisterReceiver(unlocked) }
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LockPlayer(
    np: NowPlaying?,
    artwork: android.graphics.Bitmap?,
    wavy: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onUnlock: () -> Unit,
    onDim: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastTouch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var shift by remember { mutableStateOf(0 to 0) }
    var dragUp by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            // Nudge everything a few pixels each minute so nothing burns into the screen.
            if ((now / 1000) % 60 == 0L) shift = Random.nextInt(-12, 13) to Random.nextInt(-16, 17)
            onDim(now - lastTouch > 10_000)
            delay(1000)
        }
    }
    val dx by animateDpAsState(shift.first.dp, label = "dx")
    val dy by animateDpAsState(shift.second.dp, label = "dy")

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .pointerInput(Unit) {
                detectTapGestures { lastTouch = System.currentTimeMillis(); onDim(false) }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { lastTouch = System.currentTimeMillis(); onDim(false) },
                    onDragEnd = { if (dragUp < -200f) onUnlock(); dragUp = 0f },
                    onVerticalDrag = { _, amount -> dragUp += amount },
                )
            }
            .safeDrawingPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .offset(dx, dy)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(now)),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp, lineHeight = 100.sp),
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                color = scheme.primary,
            )
            Text(
                SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(Date(now)),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant,
            )

            Spacer(Modifier.weight(1f))

            if (np != null) {
                Box(
                    Modifier
                        .size(200.dp)
                        .clip(MaterialShapes.Cookie12Sided.toShape())
                        .background(scheme.surfaceContainerHigh)
                ) {
                    artwork?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                }
                Spacer(Modifier.height(24.dp))
                Text(np.title, style = MaterialTheme.typography.headlineSmall, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold,
                    color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Text(np.artist, style = MaterialTheme.typography.titleMedium, color = scheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))

                var position by remember { mutableLongStateOf(np.livePosition()) }
                LaunchedEffect(np) {
                    while (true) { position = np.livePosition(); if (np.isPaused) break; delay(500) }
                }
                LinearWavyProgressIndicator(
                    progress = { (position.toFloat() / np.durationMs.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    amplitude = { if (wavy && !np.isPaused) 1f else 0f },
                    color = scheme.primary,
                    trackColor = scheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundButton(Icons.Rounded.SkipPrevious, "Previous", 56, scheme.surfaceContainerHigh, scheme.onSurface) {
                        lastTouch = System.currentTimeMillis(); onPrevious()
                    }
                    RoundButton(if (np.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, "Play or pause", 76, scheme.primary, scheme.onPrimary) {
                        lastTouch = System.currentTimeMillis(); onPlayPause()
                    }
                    RoundButton(Icons.Rounded.SkipNext, "Next", 56, scheme.surfaceContainerHigh, scheme.onSurface) {
                        lastTouch = System.currentTimeMillis(); onNext()
                    }
                }
            } else {
                Text("Connecting to Spotify…", color = scheme.onSurfaceVariant)
            }

            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.KeyboardArrowUp, null, tint = scheme.onSurfaceVariant)
            Text("Swipe up to unlock", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onUnlock))
        }
    }
}

@Composable
private fun RoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sizeDp: Int,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(50))
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = content, modifier = Modifier.size((sizeDp * 0.45f).dp))
    }
}
