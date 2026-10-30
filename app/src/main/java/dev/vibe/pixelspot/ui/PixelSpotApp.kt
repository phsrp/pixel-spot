package dev.vibe.pixelspot.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vibe.pixelspot.Detail
import dev.vibe.pixelspot.MainViewModel
import dev.vibe.pixelspot.R
import dev.vibe.pixelspot.Tab
import dev.vibe.pixelspot.pixelplay.presentation.components.MiniPlayerContentInternal
import dev.vibe.pixelspot.pixelplay.presentation.components.scoped.CustomNavigationBarItem
import dev.vibe.pixelspot.player.RemoteStatus

@Composable
fun PixelSpotApp(vm: MainViewModel, onLogin: (String) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val artwork by vm.player.artwork.collectAsStateWithLifecycle()
    val loggedIn by vm.loggedIn.collectAsStateWithLifecycle()

    PixelSpotTheme(settings, artwork) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!loggedIn) {
                val error by vm.loginError.collectAsStateWithLifecycle()
                SetupScreen(vm.prefs.clientId, error, onLogin)
            } else {
                MainScaffold(vm, onLogin)
            }

            val update by vm.updater.state.collectAsStateWithLifecycle()
            UpdateDialog(update, onUpdate = vm::installUpdate, onLater = vm.updater::dismiss)
        }
    }
}

@Composable
private fun MainScaffold(vm: MainViewModel, onLogin: (String) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val details by vm.details.collectAsStateWithLifecycle()
    val np by vm.player.nowPlaying.collectAsStateWithLifecycle()
    val artwork by vm.player.artwork.collectAsStateWithLifecycle()
    val liked by vm.player.isLiked.collectAsStateWithLifecycle()
    val status by vm.player.status.collectAsStateWithLifecycle()
    val me by vm.me.collectAsStateWithLifecycle()
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var addSheetOpen by rememberSaveable { mutableStateOf(false) }
    val needsPlaylistPermission by vm.needsPlaylistPermission.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Connect to the Spotify app as soon as we're logged in (e.g. right after the login page).
    LaunchedEffect(Unit) { vm.connectPlayer(context) }

    val actions = ListActions(
        currentUri = np?.trackUri,
        isPlaying = np?.isPaused == false,
        playTrack = { vm.player.play(it.uri) },
        queue = { vm.player.queue(it.uri) },
        openPlaylist = { vm.open(Detail.Playlist(it)) },
        openAlbum = { vm.open(Detail.Album(it)) },
        openLiked = { vm.open(Detail.Liked) },
    )

    BackHandler(enabled = playerOpen || details.isNotEmpty() || tab != Tab.HOME) {
        if (playerOpen) playerOpen = false else vm.back()
    }

    // Re-read saved local-file artwork whenever a new picture gets saved.
    val artVersion by vm.player.localArt.version.collectAsStateWithLifecycle()
    val artLookup: (dev.vibe.pixelspot.data.TrackItem) -> Any? = remember(artVersion) {
        { t -> if (t.isLocal) vm.player.localArt.find(t.name) else null }
    }

    CompositionLocalProvider(LocalTrackArt provides artLookup) {
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = details.lastOrNull() to tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen",
        ) { (detail, currentTab) ->
            if (detail != null) {
                val c by vm.collection.collectAsStateWithLifecycle()
                c?.let { col ->
                    CollectionScreen(
                        c = col,
                        actions = actions,
                        onBack = { vm.back() },
                        onPlay = { i -> vm.playCollection(col, i) },
                        onShuffle = { vm.shuffleCollection(col) },
                        onLoadMore = { vm.loadMore() },
                    )
                }
            } else when (currentTab) {
                Tab.HOME -> {
                    val home by vm.home.collectAsStateWithLifecycle()
                    HomeScreen(me, home, actions, onRetry = { vm.refreshHome() }, onSettings = { vm.selectTab(Tab.SETTINGS) })
                }
                Tab.SEARCH -> {
                    val results by vm.search.collectAsStateWithLifecycle()
                    val searching by vm.searching.collectAsStateWithLifecycle()
                    SearchScreen(results, searching, vm::search, actions)
                }
                Tab.LIBRARY -> {
                    val lib by vm.library.collectAsStateWithLifecycle()
                    LibraryScreen(lib, actions)
                }
                Tab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    status = status,
                    userName = me?.name,
                    onChange = vm::updateSettings,
                    onReconnect = { vm.player.disconnect(); vm.connectPlayer(context) },
                    onLogout = vm::logout,
                    appVersion = vm.updater.currentVersion,
                    onCheckForUpdates = {
                        vm.checkForUpdatesNow { msg ->
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }

        // Mini player sitting on top of the floating navigation bar, PixelPlayer style.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val current = np
            if (current != null) {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { playerOpen = true },
                ) {
                    MiniPlayerContentInternal(
                        title = current.title,
                        artist = current.artist,
                        artModel = artwork,
                        isPlaying = !current.isPaused,
                        isCastConnecting = false,
                        isPreparingPlayback = false,
                        onPlayPause = vm.player::togglePlay,
                        onPrevious = vm.player::previous,
                        cornerRadiusAlb = 8.dp,
                        onNext = vm.player::next,
                    )
                }
                Spacer(Modifier.height(4.dp))
            } else if (status !is RemoteStatus.Connected && status !is RemoteStatus.Disconnected) {
                StatusPill(status, onRetry = { vm.player.disconnect(); vm.connectPlayer(context) })
                Spacer(Modifier.height(4.dp))
            }
            Surface(
                shape = if (current != null) RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                else RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavItem(tab, Tab.HOME, "Home", R.drawable.rounded_home_24, R.drawable.round_home_24, vm::selectTab, details.isEmpty())
                    NavItem(tab, Tab.SEARCH, "Search", R.drawable.rounded_search_24, R.drawable.rounded_search_24, vm::selectTab, details.isEmpty())
                    NavItem(tab, Tab.LIBRARY, "Library", R.drawable.rounded_library_music_24, R.drawable.round_library_music_24, vm::selectTab, details.isEmpty())
                    NavItem(tab, Tab.SETTINGS, "Settings", R.drawable.rounded_settings_24, R.drawable.rounded_settings_24, vm::selectTab, details.isEmpty())
                }
            }
        }

        AnimatedVisibility(
            visible = playerOpen && np != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            np?.let { current ->
                FullPlayer(
                    np = current,
                    artwork = artwork,
                    isLiked = liked,
                    wavy = settings.wavyProgress,
                    onCollapse = { playerOpen = false },
                    onPlayPause = vm.player::togglePlay,
                    onNext = vm.player::next,
                    onPrevious = vm.player::previous,
                    onSeek = vm.player::seekTo,
                    onShuffle = vm.player::toggleShuffle,
                    onRepeat = vm.player::toggleRepeat,
                    onLike = vm.player::toggleLike,
                    onAddToPlaylist = { addSheetOpen = true },
                )
            }
        }

        val current = np
        if (addSheetOpen && current != null) {
            val editable = remember(addSheetOpen) { vm.editablePlaylists() }
            val contents by vm.playlistContents.collectAsStateWithLifecycle()
            LaunchedEffect(editable) { vm.loadPlaylistContents(editable) }
            AddToPlaylistSheet(
                songTitle = current.title,
                playlists = editable,
                contents = contents,
                trackUri = current.trackUri,
                needsPermission = needsPlaylistPermission,
                onAllow = { vm.prefs.clientId?.let(onLogin) },
                onPick = { p ->
                    addSheetOpen = false
                    vm.addToPlaylist(p, current.trackUri) { msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { addSheetOpen = false },
            )
        }
    }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    current: Tab,
    tab: Tab,
    label: String,
    icon: Int,
    selectedIcon: Int,
    onSelect: (Tab) -> Unit,
    noDetailOpen: Boolean,
) {
    CustomNavigationBarItem(
        selected = current == tab && noDetailOpen,
        onClick = { onSelect(tab) },
        icon = { Icon(painterResource(icon), label) },
        selectedIcon = { Icon(painterResource(selectedIcon), label) },
        label = { Text(label) },
    )
}

@Composable
private fun StatusPill(status: RemoteStatus, onRetry: () -> Unit) {
    val text = when (status) {
        RemoteStatus.Connecting -> "Connecting to Spotify…"
        RemoteStatus.SpotifyNotInstalled -> "Install the Spotify app to play music"
        is RemoteStatus.Failed -> status.message + "  Tap to retry."
        else -> ""
    }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = status is RemoteStatus.Failed, onClick = onRetry),
    ) {
        Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}
