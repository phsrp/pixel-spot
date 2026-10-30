package dev.vibe.pixelspot.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.size.Size
import dev.vibe.pixelspot.data.PlaylistItem
import dev.vibe.pixelspot.pixelplay.presentation.components.SmartImage
import dev.vibe.pixelspot.pixelplay.ui.theme.GoogleSansRounded

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    songTitle: String,
    playlists: List<PlaylistItem>,
    /** Song links per playlist id; a missing entry means it's still being checked. */
    contents: Map<String, Set<String>>,
    trackUri: String,
    needsPermission: Boolean,
    onAllow: () -> Unit,
    onPick: (PlaylistItem) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                "Add \"$songTitle\" to…",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansRounded,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (needsPermission) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Spotify needs your OK before this app can add songs to playlists. You'll see the Spotify login page once more.")
                        Spacer(Modifier.size(12.dp))
                        Button(onClick = onAllow) { Text("Allow editing playlists") }
                    }
                }
            }
            if (playlists.isEmpty()) {
                Notice("You don't have any playlists of your own yet. Spotify only lets apps add to playlists you made or collaborate on.")
            }
            LazyColumn(Modifier.padding(bottom = 16.dp)) {
                items(playlists, key = { it.id }) { p ->
                    val known = contents[p.id]
                    val alreadyIn = known?.contains(trackUri) == true
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .alpha(if (alreadyIn) 0.6f else 1f)
                            .clickable(enabled = known != null && !alreadyIn) { onPick(p) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SmartImage(
                            model = p.imageUrl,
                            contentDescription = null,
                            shape = RoundedCornerShape(12.dp),
                            targetSize = Size(128, 128),
                            modifier = Modifier.size(52.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                when {
                                    alreadyIn -> "Already added"
                                    known == null -> "Checking…"
                                    else -> "${p.trackCount} songs"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (alreadyIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        when {
                            alreadyIn -> Icon(Icons.Rounded.CheckCircle, "Already added", tint = MaterialTheme.colorScheme.primary)
                            known == null -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }
}
