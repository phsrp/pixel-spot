// Layout and toggle row adapted from PixelPlayer's FullPlayerContent
// (github.com/PixelPlayerHQ/PixelPlayer, commit d51105d, MIT licence). See THIRD_PARTY_NOTICES.md.
package dev.vibe.pixelspot.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vibe.pixelspot.R
import dev.vibe.pixelspot.pixelplay.presentation.components.AutoScrollingText
import dev.vibe.pixelspot.pixelplay.presentation.components.LocalMaterialTheme
import dev.vibe.pixelspot.pixelplay.presentation.components.ToggleSegmentButton
import dev.vibe.pixelspot.pixelplay.presentation.components.WavySliderExpressive
import dev.vibe.pixelspot.pixelplay.presentation.components.player.AnimatedPlaybackControls
import dev.vibe.pixelspot.pixelplay.ui.theme.GoogleSansRounded
import dev.vibe.pixelspot.player.NowPlaying
import kotlinx.coroutines.delay
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FullPlayer(
    np: NowPlaying,
    artwork: Bitmap?,
    isLiked: Boolean,
    wavy: Boolean,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    val scheme = LocalMaterialTheme.current
    val onBase = scheme.onBackground
    val context = LocalContext.current
    val density = LocalDensity.current
    var dragY by remember { mutableFloatStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = dragY.coerceAtLeast(0f) }
            .background(scheme.background)
            .pointerInput(Unit) {
                // Swipe down to close, like PixelPlayer's sheet.
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragY > with(density) { 140.dp.toPx() }) onCollapse()
                        dragY = 0f
                    },
                    onDragCancel = { dragY = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragY += amount
                    },
                )
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            // Top bar: collapse button, "Now playing", open-in-Spotify pill.
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHigh)
                        .clickable(onClick = onCollapse),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.rounded_keyboard_arrow_down_24), "Close player", tint = scheme.tertiary)
                }
                Text(
                    "Now playing",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 18.dp),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    fontWeight = FontWeight.SemiBold,
                    color = onBase,
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .size(height = 42.dp, width = 50.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 50.dp, bottomStart = 6.dp, bottomEnd = 50.dp))
                        .background(scheme.surfaceContainerHigh)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(np.trackUri))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(intent) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Open in Spotify", tint = scheme.tertiary, modifier = Modifier.size(20.dp))
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                AlbumCover(artwork, isPaused = np.isPaused)

                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SongInfoRow(np, onBase, scheme.background, scheme.surfaceContainerHigh, scheme.tertiary, onAddToPlaylist)
                    ProgressSection(np, onBase, wavy, onSeek)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedPlaybackControls(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        isPlayingProvider = { !np.isPaused },
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        height = 80.dp,
                        pressAnimationSpec = remember { tween(durationMillis = 240) },
                        releaseDelay = 220L,
                        colorOtherButtons = scheme.tertiaryFixedDim,
                        colorPlayPause = scheme.tertiaryFixedDim,
                        tintPlayPauseIcon = scheme.onTertiaryFixed,
                        tintOtherIcons = scheme.onTertiaryFixed,
                    )
                    Spacer(Modifier.height(14.dp))
                    BottomToggleRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 66.dp, max = 86.dp)
                            .padding(horizontal = 26.dp)
                            .padding(bottom = 6.dp),
                        isShuffleEnabled = np.isShuffling,
                        repeatMode = np.repeatMode,
                        isFavorite = isLiked,
                        onShuffleToggle = onShuffle,
                        onRepeatToggle = onRepeat,
                        onFavoriteToggle = onLike,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumCover(artwork: Bitmap?, isPaused: Boolean) {
    // PixelPlayer shrinks the cover a little while paused.
    val scale by animateFloatAsState(
        if (isPaused) 0.95f else 1f,
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "AlbumArtScale",
    )
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(16.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(artwork.asImageBitmap(), "Album art", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(painterResource(R.drawable.ic_music_placeholder), null, Modifier.size(96.dp))
        }
    }
}

@Composable
private fun SongInfoRow(
    np: NowPlaying,
    textColor: Color,
    edge: Color,
    chipColor: Color,
    chipContent: Color,
    onAddToPlaylist: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            AutoScrollingText(
                text = np.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontFamily = GoogleSansRounded, color = textColor),
                gradientEdgeColor = edge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
            AutoScrollingText(
                text = np.artist,
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.sp, color = textColor.copy(alpha = 0.7f)),
                gradientEdgeColor = edge,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Where PixelPlayer has its lyrics button.
        FilledIconButton(
            onClick = onAddToPlaylist,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = chipColor, contentColor = chipContent),
        ) {
            Icon(painterResource(R.drawable.rounded_playlist_add_24), "Add to playlist")
        }
    }
}

@Composable
private fun ProgressSection(np: NowPlaying, textColor: Color, wavy: Boolean, onSeek: (Long) -> Unit) {
    val duration = np.durationMs.coerceAtLeast(1L)
    var position by remember { mutableLongStateOf(np.livePosition()) }
    var dragValue by remember { mutableStateOf<Float?>(null) }

    // Keep the bar moving between Spotify's updates.
    LaunchedEffect(np) {
        while (true) {
            position = np.livePosition()
            if (np.isPaused) break
            delay(200)
        }
    }

    val progress = dragValue ?: (position.toFloat() / duration).coerceIn(0f, 1f)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                // Stop the swipe-to-close gesture from stealing slider drags.
                .pointerInput(Unit) { detectVerticalDragGestures(onVerticalDrag = { _, _ -> }) }
        ) {
            WavySliderExpressive(
                value = progress,
                onValueChange = { dragValue = it },
                onValueChangeFinished = {
                    dragValue?.let { v ->
                        val target = (v * duration).toLong()
                        position = target
                        onSeek(target)
                    }
                    dragValue = null
                },
                enabled = np.canSeek,
                activeTrackColor = textColor,
                inactiveTrackColor = textColor.copy(alpha = 0.2f),
                thumbColor = textColor,
                isPlaying = wavy && !np.isPaused,
                trackEdgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = dragValue?.let { (it * duration).toLong() } ?: position
            Text(formatTime(shown), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), fontWeight = FontWeight.SemiBold, color = textColor)
            Text(formatTime(np.durationMs), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), fontWeight = FontWeight.SemiBold, color = textColor)
        }
    }
}

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun BottomToggleRow(
    modifier: Modifier,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    val rowCorners: Dp = 60.dp
    val scheme = LocalMaterialTheme.current
    val inactiveBg = scheme.onSurface.copy(alpha = 0.07f)
    val smooth = AbsoluteSmoothCornerShape(
        cornerRadiusTL = rowCorners, smoothnessAsPercentTL = 60,
        cornerRadiusTR = rowCorners, smoothnessAsPercentTR = 60,
        cornerRadiusBL = rowCorners, smoothnessAsPercentBL = 60,
        cornerRadiusBR = rowCorners, smoothnessAsPercentBR = 60,
    )
    Box(modifier.background(scheme.surfaceContainerLowest.copy(alpha = 0.7f), smooth)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .clip(smooth),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val m = Modifier.weight(1f)
            ToggleSegmentButton(
                modifier = m,
                active = isShuffleEnabled,
                activeColor = scheme.tertiaryFixed,
                activeCornerRadius = rowCorners,
                activeContentColor = scheme.onTertiaryFixed,
                inactiveColor = inactiveBg,
                inactiveContentColor = scheme.onSurface,
                onClick = onShuffleToggle,
                iconId = R.drawable.rounded_shuffle_24,
                contentDesc = "Shuffle",
            )
            ToggleSegmentButton(
                modifier = m,
                active = repeatMode != 0,
                activeColor = scheme.tertiaryFixed,
                activeCornerRadius = rowCorners,
                activeContentColor = scheme.onTertiaryFixed,
                inactiveColor = inactiveBg,
                inactiveContentColor = scheme.onSurface,
                onClick = onRepeatToggle,
                iconId = if (repeatMode == 1) R.drawable.rounded_repeat_one_24 else R.drawable.rounded_repeat_24,
                contentDesc = "Repeat",
            )
            ToggleSegmentButton(
                modifier = m,
                active = isFavorite,
                activeColor = scheme.tertiaryFixed,
                activeCornerRadius = rowCorners,
                activeContentColor = scheme.onTertiaryFixed,
                inactiveColor = inactiveBg,
                inactiveContentColor = scheme.onSurface,
                onClick = onFavoriteToggle,
                iconId = if (isFavorite) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24,
                contentDesc = "Like",
            )
        }
    }
}
