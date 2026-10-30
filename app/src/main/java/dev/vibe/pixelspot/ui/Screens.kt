package dev.vibe.pixelspot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.size.Size
import dev.vibe.pixelspot.Collection
import dev.vibe.pixelspot.HomeData
import dev.vibe.pixelspot.LibraryData
import dev.vibe.pixelspot.data.AlbumItem
import dev.vibe.pixelspot.data.Me
import dev.vibe.pixelspot.data.PlaylistItem
import dev.vibe.pixelspot.data.SearchResults
import dev.vibe.pixelspot.data.TrackItem
import dev.vibe.pixelspot.pixelplay.presentation.components.SmartImage
import dev.vibe.pixelspot.pixelplay.ui.theme.GoogleSansRounded
import java.util.Calendar

/** Shared actions every list screen needs. */
class ListActions(
    val currentUri: String?,
    val isPlaying: Boolean,
    val playTrack: (TrackItem) -> Unit,
    val queue: (TrackItem) -> Unit,
    val openPlaylist: (PlaylistItem) -> Unit,
    val openAlbum: (AlbumItem) -> Unit,
    val openLiked: () -> Unit,
)

@Composable
private fun ScreenHeader(title: String, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = GoogleSansRounded,
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )
        trailing()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(me: Me?, data: HomeData, actions: ListActions, onRetry: () -> Unit, onSettings: () -> Unit) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomBarSpace)) {
        item {
            ScreenHeader(if (me != null) "$greeting,\n${me.name.substringBefore(' ')}" else greeting) {
                SmartImage(
                    model = me?.imageUrl,
                    contentDescription = "Settings",
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    targetSize = Size(120, 120),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onSettings),
                )
            }
        }
        if (data.error != null) {
            item { Notice(data.error) }
            item { TextButton(onClick = onRetry, modifier = Modifier.padding(start = 12.dp)) { Text("Try again") } }
        }
        item { LikedSongsBanner(actions.openLiked) }

        if (data.loading && data.recent.isEmpty()) item { Loading() }

        if (data.recent.isNotEmpty()) {
            item { SectionTitle("Jump back in") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(data.recent.take(15), key = { it.uri }) { t ->
                        ArtCard(t.name, t.artists, t.imageUrl, 150.dp, onClick = { actions.playTrack(t) })
                    }
                }
            }
        }
        if (data.playlists.isNotEmpty()) {
            item { SectionTitle("Your playlists") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(data.playlists.take(20), key = { it.id }) { p ->
                        ArtCard(p.name, "${p.trackCount} songs", p.imageUrl, 150.dp, onClick = { actions.openPlaylist(p) },
                            shape = MaterialShapes.Square.toShape())
                    }
                }
            }
        }
        if (data.top.isNotEmpty()) {
            item { SectionTitle("On repeat lately") }
            items(data.top, key = { "top" + it.uri }) { t ->
                TrackRow(t, t.uri == actions.currentUri, actions.isPlaying, { actions.playTrack(t) }, { actions.queue(t) })
            }
        }
    }
}

@Composable
private fun LikedSongsBanner(onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(c.primary, c.tertiary)))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(c.onPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Favorite, null, tint = c.onPrimary) }
            Spacer(Modifier.width(16.dp))
            Text("Liked Songs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = GoogleSansRounded, color = c.onPrimary)
        }
    }
}

@Composable
fun SearchScreen(results: SearchResults, searching: Boolean, onQuery: (String) -> Unit, actions: ListActions) {
    var query by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomBarSpace)) {
        item { ScreenHeader("Search") }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; onQuery(it) },
                placeholder = { Text("Songs, albums, playlists") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (searching) item { Loading() }
        if (results.tracks.isNotEmpty()) {
            item { SectionTitle("Songs") }
            items(results.tracks, key = { "s" + it.uri }) { t ->
                TrackRow(t, t.uri == actions.currentUri, actions.isPlaying, { actions.playTrack(t) }, { actions.queue(t) })
            }
        }
        if (results.albums.isNotEmpty()) {
            item { SectionTitle("Albums") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(results.albums, key = { it.uri }) { a ->
                        ArtCard(a.name, a.artists, a.imageUrl, 140.dp, onClick = { actions.openAlbum(a) })
                    }
                }
            }
        }
        if (results.playlists.isNotEmpty()) {
            item { SectionTitle("Playlists") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(results.playlists, key = { it.uri }) { p ->
                        ArtCard(p.name, p.owner, p.imageUrl, 140.dp, onClick = { actions.openPlaylist(p) })
                    }
                }
            }
        }
    }
}

private enum class LibTab(val label: String) { PLAYLISTS("Playlists"), ALBUMS("Albums") }

@Composable
fun LibraryScreen(data: LibraryData, actions: ListActions) {
    var tab by rememberSaveable { mutableStateOf(LibTab.PLAYLISTS) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = BottomBarSpace),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(2) }) { Box(Modifier.padding(start = 0.dp)) { ScreenHeader("Library") } }
        item(span = { GridItemSpan(2) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LibTab.entries.forEach { t ->
                    FilterChip(selected = tab == t, onClick = { tab = t }, label = { Text(t.label) }, shape = RoundedCornerShape(50))
                }
            }
        }
        item(span = { GridItemSpan(2) }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = actions.openLiked),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Liked Songs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = GoogleSansRounded)
                        Text("${data.likedCount} songs", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (data.loading) item(span = { GridItemSpan(2) }) { Loading() }
        when (tab) {
            LibTab.PLAYLISTS -> items(data.playlists, key = { it.id }) { p ->
                ArtCard(p.name, "${p.owner} · ${p.trackCount}", p.imageUrl, null, onClick = { actions.openPlaylist(p) })
            }
            LibTab.ALBUMS -> items(data.albums, key = { it.id }) { a ->
                ArtCard(a.name, a.artists, a.imageUrl, null, onClick = { actions.openAlbum(a) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CollectionScreen(
    c: Collection,
    actions: ListActions,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, c.tracks.size) { if (nearEnd) onLoadMore() }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomBarSpace)) {
        item {
            Row(Modifier.statusBarsPadding().padding(8.dp)) {
                FilledTonalIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (c.imageUrl != null) {
                    SmartImage(
                        model = c.imageUrl,
                        contentDescription = c.title,
                        shape = MaterialShapes.Cookie12Sided.toShape(),
                        targetSize = Size(600, 600),
                        modifier = Modifier.size(240.dp),
                    )
                } else {
                    Box(
                        Modifier
                            .size(240.dp)
                            .clip(MaterialShapes.Cookie12Sided.toShape())
                            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Favorite, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onPrimary) }
                }
                Spacer(Modifier.height(18.dp))
                Text(c.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, fontFamily = GoogleSansRounded,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (c.subtitle.isNotBlank()) {
                    Text(c.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onPlay(0) }, enabled = c.contextUri != null || c.tracks.isNotEmpty(), modifier = Modifier.height(56.dp)) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play")
                    }
                    FilledTonalButton(onClick = onShuffle, enabled = c.contextUri != null, modifier = Modifier.height(56.dp)) {
                        Icon(Icons.Rounded.Shuffle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Shuffle")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        c.notice?.let { item { Notice(it) } }
        itemsIndexed(c.tracks, key = { i, t -> "$i${t.uri}" }) { i, t ->
            TrackRow(t, t.uri == actions.currentUri, actions.isPlaying, { onPlay(i) }, { actions.queue(t) })
        }
        if (c.loading) item { Loading() }
    }
}
