// Header and song row adapted from PixelPlayer's QueueBottomSheet
// (github.com/PixelPlayerHQ/PixelPlayer, commit d51105d, MIT licence). See THIRD_PARTY_NOTICES.md.
package dev.vibe.pixelspot.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.size.Size
import dev.vibe.pixelspot.QueueState
import dev.vibe.pixelspot.data.TrackItem
import dev.vibe.pixelspot.pixelplay.presentation.components.SmartImage
import dev.vibe.pixelspot.pixelplay.presentation.components.subcomps.PlayingEqIcon
import dev.vibe.pixelspot.pixelplay.ui.theme.GoogleSansRounded

/** Spotify lets apps read the queue, but not reorder it or jump to a song in it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(queue: QueueState, isPlaying: Boolean, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp, bottom = 14.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.onSurface.copy(alpha = 0.14f))
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Next up",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.onSurface,
                )
                Text(
                    when {
                        queue.loading && queue.items.isEmpty() -> "Loading your queue…"
                        queue.items.isEmpty() -> "Nothing lined up"
                        queue.items.size == 1 -> "1 song lined up"
                        else -> "${queue.items.size} songs lined up"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }

            queue.error?.let { Notice(it) }
            if (queue.loading && queue.current == null) Loading()

            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                queue.current?.let { current ->
                    item(key = "current") { QueueSongItem(current, isCurrent = true, isPlaying = isPlaying) }
                }
                // Spotify can list the same song twice, so its position is part of the key.
                itemsIndexed(queue.items, key = { i, t -> "$i${t.uri}" }) { _, track ->
                    QueueSongItem(track, isCurrent = false, isPlaying = false)
                }
            }
        }
    }
}

@Composable
private fun QueueSongItem(track: TrackItem, isCurrent: Boolean, isPlaying: Boolean) {
    val colors = MaterialTheme.colorScheme
    val corner by animateDpAsState(if (isCurrent) 60.dp else 22.dp, label = "queueCorner")
    val artCorner by animateDpAsState(if (isCurrent) 60.dp else 8.dp, label = "queueArtCorner")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(corner),
        color = colors.surfaceContainerLowest,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmartImage(
                model = track.imageUrl ?: LocalTrackArt.current(track),
                contentDescription = null,
                shape = RoundedCornerShape(artCorner),
                contentScale = ContentScale.Crop,
                targetSize = Size(96, 96),
                modifier = Modifier.size(42.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrent) colors.primary else colors.onSurface,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    track.artists,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) colors.primary.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                )
            }
            if (isCurrent) {
                PlayingEqIcon(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(width = 18.dp, height = 16.dp),
                    color = colors.secondary,
                    isPlaying = isPlaying,
                )
            }
        }
    }
}
