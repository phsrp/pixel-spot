// Song row styling follows PixelPlayer's EnhancedSongListItem
// (github.com/PixelPlayerHQ/PixelPlayer, commit d51105d, MIT licence). See THIRD_PARTY_NOTICES.md.
package dev.vibe.pixelspot.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.size.Size
import dev.vibe.pixelspot.R
import dev.vibe.pixelspot.data.TrackItem
import dev.vibe.pixelspot.pixelplay.presentation.components.SmartImage
import dev.vibe.pixelspot.pixelplay.presentation.components.subcomps.PlayingEqIcon
import dev.vibe.pixelspot.pixelplay.ui.theme.GoogleSansRounded

/** Finds saved artwork for local files (see LocalArtCache). */
val LocalTrackArt = staticCompositionLocalOf<(TrackItem) -> Any?> { { null } }

@Composable
fun TrackRow(
    track: TrackItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onQueue: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    // The playing song turns into a pill, like in PixelPlayer.
    val corner by animateDpAsState(if (isCurrent) 50.dp else 22.dp, spring(), label = "rowCorner")
    val artCorner by animateDpAsState(if (isCurrent) 50.dp else 10.dp, spring(), label = "artCorner")
    val container by animateColorAsState(if (isCurrent) colors.primaryContainer else colors.surfaceContainerLow, label = "rowColor")
    val content by animateColorAsState(if (isCurrent) colors.onPrimaryContainer else colors.onSurface, label = "rowContent")

    Surface(
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                SmartImage(
                    model = track.imageUrl ?: LocalTrackArt.current(track),
                    contentDescription = null,
                    shape = RoundedCornerShape(artCorner),
                    targetSize = Size(128, 128),
                    modifier = Modifier.size(50.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GoogleSansRounded,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (track.isLocal) listOf("Local file", track.artists).filter { it.isNotBlank() }.joinToString(" · ") else track.artists,
                    style = MaterialTheme.typography.bodyMedium,
                    color = content.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isCurrent) {
                PlayingEqIcon(color = colors.primary, isPlaying = isPlaying, modifier = Modifier.size(width = 18.dp, height = 16.dp))
                Spacer(Modifier.width(8.dp))
            }
            // Spotify can't queue local files from outside its own app.
            if (onQueue != null && !track.isLocal) {
                IconButton(onClick = onQueue) {
                    Icon(painterResource(R.drawable.rounded_playlist_play_24), "Add to queue")
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        fontFamily = GoogleSansRounded,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
    )
}

/** Square artwork with a caption, used in carousels and grids. */
@Composable
fun ArtCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    width: Dp?,
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(28.dp),
    modifier: Modifier = Modifier,
) {
    Column(
        (if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .then(modifier)
            .clickable(onClick = onClick)
    ) {
        SmartImage(
            model = imageUrl,
            contentDescription = title,
            shape = shape,
            targetSize = Size(400, 400),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GoogleSansRounded,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        LoadingIndicator()
    }
}

@Composable
fun Notice(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Room at the bottom of lists so the mini player and nav bar don't cover the last items. */
val BottomBarSpace = 170.dp
