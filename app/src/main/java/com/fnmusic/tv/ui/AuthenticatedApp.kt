package com.fnmusic.tv.ui

import android.graphics.Bitmap
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import androidx.tv.material3.Button as TvMaterialButton
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Border
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.fnmusic.tv.AuthenticatedAppDependencies
import com.fnmusic.tv.R
import com.fnmusic.tv.NowPlayingPresentation
import com.fnmusic.tv.NowPlayingResourceState
import com.fnmusic.tv.decodeArtwork
import com.fnmusic.tv.core.data.repository.CurrentLyrics
import com.fnmusic.tv.core.data.repository.SessionState
import com.fnmusic.tv.core.model.Album
import com.fnmusic.tv.core.model.AppError
import com.fnmusic.tv.core.model.AppException
import com.fnmusic.tv.core.model.Artist
import com.fnmusic.tv.core.model.CoverVariant
import com.fnmusic.tv.core.model.Genre
import com.fnmusic.tv.core.model.Page
import com.fnmusic.tv.core.model.PlayerStyle
import com.fnmusic.tv.core.model.Playlist
import com.fnmusic.tv.core.model.SharedLibrary
import com.fnmusic.tv.core.model.Track
import com.fnmusic.tv.core.model.TrackDirectory
import com.fnmusic.tv.core.model.directoryCoverSamples
import com.fnmusic.tv.core.model.directoryDisplayName
import com.fnmusic.tv.core.model.playback.QueueSource
import com.fnmusic.tv.core.model.playback.QueueKind
import com.fnmusic.tv.core.model.playback.PlayMode
import com.fnmusic.tv.core.model.playback.PlaybackQueueItem
import com.fnmusic.tv.core.model.playback.NowPlayingIdentity
import com.fnmusic.tv.core.model.playback.MAX_ACTIVE_QUEUE_ITEMS
import com.fnmusic.tv.core.model.playback.QueuePageItem
import com.fnmusic.tv.core.model.playback.QueuePageSegment
import com.fnmusic.tv.core.model.playback.boundedQueueWindow
import com.fnmusic.tv.core.model.resolveTrackDirectoryView
import com.fnmusic.tv.core.playback.PlaybackUiState
import com.fnmusic.tv.core.playback.PlaybackProgressState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

private val LocalAuthenticatedDependencies = staticCompositionLocalOf<AuthenticatedAppDependencies> {
    error("Missing authenticated app dependencies")
}

internal val LocalLibraryRetainedState = staticCompositionLocalOf<LibraryRetainedStateStore> {
    error("Missing library retained state")
}

/** 滚动目录列表（歌手/专辑/风格）每页加载数量：一次至少 48 条，减少滚动中的等待。
 *  首页/我的页预览与列表共享同一 retained 状态，必须使用同一分页大小。 */
private const val CATALOG_LIST_PAGE_SIZE = 48

/** 歌手/专辑/风格瓦片统一高度；图标随高度放大（内边距 7dp×2 后图标可占 98dp）。 */
private val CATALOG_TILE_HEIGHT = 112.dp

/** 歌手瓦片的圆形头像尺寸。 */
private val CATALOG_ARTIST_AVATAR_SIZE = 86.dp

/** 专辑/风格瓦片的方形封面尺寸（恰好填满瓦片内容高度）。 */
private val CATALOG_ARTWORK_SIZE = 98.dp

@Composable
internal fun AuthenticatedApp(
    container: AuthenticatedAppDependencies,
    session: SessionState.SignedIn,
    playback: PlaybackUiState,
    onExitApplication: () -> Unit,
) {
    var stack by remember(session.user.guid) { mutableStateOf(listOf<LibraryRoute>(LibraryRoute.Home)) }
    var lastHomeBackAt by remember(session.user.guid) { mutableStateOf(0L) }
    val route = stack.last()
    val context = LocalContext.current
    val stateHolder = rememberSaveableStateHolder()
    val retainedScope = rememberCoroutineScope()
    val retainedState = remember(session.user.guid, retainedScope) {
        LibraryRetainedStateStore(retainedScope)
    }
    val routeStateLifecycle = remember(session.user.guid) { LibraryRouteStateLifecycle() }
    val playerVisualContinuity = remember(session.user.guid) { PlayerVisualContinuity() }
    val open: (LibraryRoute) -> Unit = { stack = stack + it }
    val root: (LibraryRoute) -> Unit = { stack = listOf(it) }
    val back: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) }
    LaunchedEffect(route) {
        container.updateController.setAutomaticPromptAllowed(route !is LibraryRoute.Player)
    }
    DisposableEffect(container.updateController) {
        onDispose { container.updateController.setAutomaticPromptAllowed(true) }
    }
    LaunchedEffect(route.storageKey()) { lastHomeBackAt = 0L }
    LaunchedEffect(stack) {
        routeStateLifecycle.update(stack).forEach { removedRoute ->
            stateHolder.removeState(removedRoute.storageKey())
            retainedState.remove(removedRoute.retainedStateKeys())
        }
    }
    BackHandler {
        when {
            stack.size > 1 -> stack = stack.dropLast(1)
            route == LibraryRoute.My -> root(LibraryRoute.Home)
            route == LibraryRoute.Home -> {
                val now = SystemClock.elapsedRealtime()
                if (isHomeBackConfirmed(lastHomeBackAt, now)) {
                    lastHomeBackAt = 0L
                    onExitApplication()
                } else {
                    lastHomeBackAt = now
                    Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    LaunchedEffect(playback.error) {
        if (playback.error?.requiresSessionVerification == true) {
            container.authenticatedActions.verifyCurrentSession()
        }
    }

    stateHolder.SaveableStateProvider(route.storageKey()) {
        CompositionLocalProvider(
            LocalAuthenticatedDependencies provides container,
            LocalLibraryRetainedState provides retainedState,
        ) {
            when (route) {
        LibraryRoute.Home -> BrowseHome(
            container,
            playback,
            onMy = { root(LibraryRoute.My) },
            onSearch = { open(LibraryRoute.Search) },
            onPlaylist = { open(LibraryRoute.PlaylistDetail(it)) },
            onFavorites = { open(LibraryRoute.Favorites) },
            onAll = { open(LibraryRoute.AllPlaylists) },
            onFolders = { open(LibraryRoute.Folders) },
            onArtists = { open(LibraryRoute.Artists) },
            onAlbums = { open(LibraryRoute.Albums) },
            onGenres = { open(LibraryRoute.Genres) },
            onPlayer = { open(LibraryRoute.Player(null)) },
        )
        LibraryRoute.My -> BrowseMy(
            container,
            session,
            playback,
            onHome = { root(LibraryRoute.Home) },
            onSearch = { open(LibraryRoute.Search) },
            onArtists = { open(LibraryRoute.Artists) },
            onAlbums = { open(LibraryRoute.Albums) },
            onGenres = { open(LibraryRoute.Genres) },
            onAllTracks = { open(LibraryRoute.AllTracks) },
            onFolders = { open(LibraryRoute.Folders) },
            onArtist = { open(LibraryRoute.ArtistDetail(it)) },
            onAlbum = { open(LibraryRoute.AlbumDetail(it)) },
            onGenre = { open(LibraryRoute.GenreDetail(it)) },
            onSettings = { open(LibraryRoute.Settings) },
            onPlayer = { open(LibraryRoute.Player(null)) },
        )
        LibraryRoute.Search -> SearchScreen(
            container,
            onOpenArtist = { open(LibraryRoute.ArtistDetail(it)) },
            onOpenAlbum = { open(LibraryRoute.AlbumDetail(it)) },
            onOpenPlaylist = { open(LibraryRoute.PlaylistDetail(it)) },
            onPlayer = { open(LibraryRoute.Player(it)) },
        )
        LibraryRoute.AllPlaylists -> AllPlaylists(container, onOpen = { open(LibraryRoute.PlaylistDetail(it)) })
        is LibraryRoute.PlaylistDetail -> TrackCollection(
            container = container,
            stateKey = "playlist:${route.playlist.guid.value}:tracks",
            title = route.playlist.name,
            coverId = route.playlist.coverId,
            loader = { container.musicRepository.playlistTracks(route.playlist.guid.value, it) },
            queueSource = { sort -> QueueSource.Playlist(route.playlist.guid.value, sort) },
            onPlayer = { open(LibraryRoute.Player(it)) },
            detailHeader = TrackDetailHeader(
                kind = "歌单",
                declaredTrackCount = route.playlist.trackCount,
                onBack = back,
            ),
        )
        LibraryRoute.Artists -> ArtistGrid(container, onOpen = { open(LibraryRoute.ArtistDetail(it)) })
        LibraryRoute.Albums -> AlbumGrid(container, onOpen = { open(LibraryRoute.AlbumDetail(it)) })
        LibraryRoute.Genres -> GenreGrid(container, onOpen = { open(LibraryRoute.GenreDetail(it)) })
        LibraryRoute.Folders -> FolderBrowseRoot(container, onOpenLibrary = { open(LibraryRoute.FolderDetail(it)) })
        is LibraryRoute.FolderDetail -> FolderDetailScreen(
            container = container,
            directory = route.directory,
            onBack = back,
            onSubFolder = { open(LibraryRoute.FolderDetail(it)) },
            onPlayer = { open(LibraryRoute.Player(it)) },
        )
        LibraryRoute.AllTracks -> TrackCollection(
            container = container,
            stateKey = "all-tracks",
            title = "全部歌曲",
            coverId = null,
            loader = container.musicRepository::allTracks,
            queueSource = QueueSource::LibraryAllTracks,
            onPlayer = { open(LibraryRoute.Player(it)) },
            detailHeader = TrackDetailHeader(
                kind = "音乐库",
                artworkFallback = CollectionArtworkFallback.Collection,
                onBack = back,
            ),
            primaryAction = TrackCollectionPrimaryAction.StartRoam {
                open(LibraryRoute.Player(null))
            },
        )
        LibraryRoute.Favorites -> {
            val favoriteState by container.musicRepository.favoriteState.collectAsStateWithLifecycle()
            TrackCollection(
                container = container,
                stateKey = "favorites:tracks",
                title = "收藏",
                loader = container.musicRepository::favoriteTracks,
                queueSource = QueueSource::Favorites,
                onPlayer = { open(LibraryRoute.Player(it)) },
                detailHeader = TrackDetailHeader(
                    kind = "收藏",
                    artworkFallback = CollectionArtworkFallback.Favorites,
                    onBack = back,
                ),
                contentRevision = favoriteState.revision,
                emptyMessage = "还没有收藏歌曲",
            )
        }
        is LibraryRoute.ArtistDetail -> ArtistDetail(
            container = container,
            artist = route.artist,
            onBack = back,
            onAlbum = { open(LibraryRoute.AlbumDetail(it)) },
            onPlayer = { open(LibraryRoute.Player(it)) },
        )
        is LibraryRoute.AlbumDetail -> {
            // 同名同歌手的专辑（如自动归档的“纯音乐”）在专辑列表已合并展示；
            // 详情页同样聚合全部成员专辑的歌曲，保证列表与播放队列一致。
            val retainedStore = LocalLibraryRetainedState.current
            val albumListState = retainedStore.paged<Album>("grid:albums")
            val mergedGuids = remember(route.album.guid, albumListState.snapshot.entries) {
                albumListState.snapshot.entries
                    .filter {
                        it.name == route.album.name && (it.artistName ?: "") == (route.album.artistName ?: "")
                    }
                    .map { it.guid.value }
                    .distinct()
            }
            val merged = mergedGuids.size > 1
            TrackCollection(
                container = container,
                stateKey = if (merged) "album-merged:${route.album.guid.value}:tracks"
                else "album:${route.album.guid.value}:tracks",
                title = route.album.name,
                subtitle = route.album.artistName.orEmpty(),
                coverId = route.album.coverId,
                loader = { page ->
                    if (merged) {
                        container.musicRepository.albumTracksMerged(mergedGuids, page)
                    } else {
                        container.musicRepository.albumTracks(route.album.guid.value, page)
                    }
                },
                queueSource = { sort ->
                    if (merged) {
                        QueueSource.MergedAlbum(mergedGuids, sort)
                    } else {
                        QueueSource.Album(route.album.guid.value, sort)
                    }
                },
                onPlayer = { open(LibraryRoute.Player(it)) },
                detailHeader = TrackDetailHeader(
                    kind = "专辑",
                    extraMetadata = route.album.releaseDate,
                    // 合并专辑的总数以加载结果为准，代表专辑的声明数量（如 1 首）会误导。
                    declaredTrackCount = if (merged) null else route.album.trackCount,
                    onBack = back,
                ),
            )
        }
        is LibraryRoute.GenreDetail -> TrackCollection(
            container = container,
            stateKey = "genre:${route.genre.guid.value}:tracks",
            title = route.genre.name,
            coverId = route.genre.coverId,
            loader = { container.musicRepository.genreTracks(route.genre.guid.value, it) },
            queueSource = { sort -> QueueSource.Genre(route.genre.guid.value, sort) },
            onPlayer = { open(LibraryRoute.Player(it)) },
            detailHeader = TrackDetailHeader(
                kind = "风格",
                declaredTrackCount = route.genre.trackCount,
                extraMetadata = route.genre.name,
                onBack = back,
            ),
        )
        is LibraryRoute.Player -> ImmersivePlayer(
            container = container,
            playback = playback,
            visualContinuity = playerVisualContinuity,
            onExitRoam = {
                retainedScope.launch {
                    stack = if (container.playbackController.exitRoamDurably()) {
                        stack.dropLast(1) + LibraryRoute.Player(null)
                    } else {
                        listOf(LibraryRoute.Home)
                    }
                }
            },
        )
                LibraryRoute.Settings -> SettingsScreen(container)
            }
        }
    }
}

@Composable
private fun LibraryTopBar(
    playback: PlaybackUiState,
    selectedHome: Boolean,
    onHome: () -> Unit,
    onMy: () -> Unit,
    onSearch: () -> Unit,
    onPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    nowPlayingFocus: FocusRequester,
    homeTabFocus: FocusRequester,
    myTabFocus: FocusRequester,
    searchFocus: FocusRequester,
    contentDownFocus: FocusRequester,
) {
    Row(
        Modifier.fillMaxWidth().height(70.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (playback.hasMedia) {
            NowPlayingPill(
                playback = playback,
                onClick = onPlayer,
                modifier = modifier
                    .focusProperties {
                        right = homeTabFocus
                        down = contentDownFocus
                    }
                    .focusRequester(nowPlayingFocus),
            )
        } else {
            Spacer(Modifier)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SegmentedLibraryTabs(
                selectedHome = selectedHome,
                playbackAvailable = playback.hasMedia,
                homeFocus = homeTabFocus,
                myFocus = myTabFocus,
                searchFocus = searchFocus,
                nowPlayingFocus = nowPlayingFocus,
                contentDownFocus = contentDownFocus,
                onHome = onHome,
                onMy = onMy,
            )
            Spacer(Modifier.width(12.dp))
            SearchTopBarIconButton(
                myTabFocus = myTabFocus,
                searchFocus = searchFocus,
                contentDownFocus = contentDownFocus,
                onClick = onSearch,
            )
        }
    }
}

@Composable
private fun SearchTopBarIconButton(
    myTabFocus: FocusRequester,
    searchFocus: FocusRequester,
    contentDownFocus: FocusRequester,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(49.dp)
            .background(Color(0xFF171B1D), shape)
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape)
            .focusProperties {
                left = myTabFocus
                down = contentDownFocus
            }
            .focusRequester(searchFocus)
            .semantics { contentDescription = "搜索" },
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.025f),
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = FnColors.Text,
            focusedContainerColor = FnColors.FocusFill,
            focusedContentColor = FnColors.Text,
            pressedContainerColor = FnColors.FocusFill,
            pressedContentColor = FnColors.Text,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SearchIconGlyph()
        }
    }
}

@Composable
private fun SearchIconGlyph() {
    val iconColor = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val stroke = 2.2.dp.toPx()
        val center = this.center
        val radius = size.minDimension * 0.32f
        drawCircle(
            color = iconColor,
            radius = radius,
            center = center - androidx.compose.ui.geometry.Offset(radius * 0.18f, radius * 0.18f),
            style = Stroke(stroke),
        )
        drawLine(
            color = iconColor,
            start = androidx.compose.ui.geometry.Offset(
                center.x + radius * 0.62f,
                center.y + radius * 0.62f,
            ),
            end = androidx.compose.ui.geometry.Offset(
                center.x + radius * 1.08f,
                center.y + radius * 1.08f,
            ),
            strokeWidth = stroke * 1.25f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SegmentedLibraryTabs(
    selectedHome: Boolean,
    playbackAvailable: Boolean,
    homeFocus: FocusRequester,
    myFocus: FocusRequester,
    searchFocus: FocusRequester,
    nowPlayingFocus: FocusRequester,
    contentDownFocus: FocusRequester,
    onHome: () -> Unit,
    onMy: () -> Unit,
) {
    val containerShape = CircleShape
    Row(
        modifier = Modifier
            .size(width = 162.dp, height = 49.dp)
            .background(Color(0xFF171B1D), containerShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), containerShape)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibraryTab(
            label = "首页",
            selected = selectedHome,
            modifier = Modifier
                .focusProperties {
                    left = if (playbackAvailable) nowPlayingFocus else FocusRequester.Cancel
                    right = myFocus
                    down = contentDownFocus
                }
                .focusRequester(homeFocus),
            onClick = { if (!selectedHome) onHome() },
        )
        LibraryTab(
            label = "我的",
            selected = !selectedHome,
            modifier = Modifier
                .focusProperties {
                    left = homeFocus
                    right = searchFocus
                    down = contentDownFocus
                }
                .focusRequester(myFocus),
            onClick = { if (selectedHome) onMy() },
        )
    }
}

@Composable
private fun LibraryTab(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Button(
        onClick = onClick,
        modifier = modifier
            .size(width = 78.dp, height = 43.dp)
            .semantics { this.selected = selected },
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.025f),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) FnColors.Coral else Color.Transparent,
            contentColor = if (selected) FnColors.Background else Color(0xFFADB0B6),
            focusedContainerColor = if (selected) Color(0xFFFF866D) else FnColors.FocusFill,
            focusedContentColor = if (selected) FnColors.Background else FnColors.Text,
            pressedContainerColor = FnColors.Coral,
            pressedContentColor = FnColors.Background,
        ),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(0.dp, Color.Transparent), shape = shape),
            focusedBorder = Border(BorderStroke(1.5.dp, FnColors.Coral), shape = shape),
            pressedBorder = Border(BorderStroke(1.5.dp, FnColors.Coral), shape = shape),
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 21.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
internal fun NowPlayingPill(
    playback: PlaybackUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onTitleTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val shape = CircleShape
    val coverId = playback.coverId
    val fontScale = LocalDensity.current.fontScale
    val pillHeight = (42f + (fontScale - 1f).coerceAtLeast(0f) * 28f).dp
    Button(
        onClick = onClick,
        modifier = modifier
            .size(width = 186.dp, height = pillHeight)
            .semantics { contentDescription = "当前播放：${playback.title}" },
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.04f),
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF232827),
            contentColor = FnColors.Text,
            focusedContainerColor = Color(0xFF343A38),
            focusedContentColor = FnColors.Text,
            pressedContainerColor = Color(0xFF3B413F),
            pressedContentColor = FnColors.Text,
        ),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(0.5.dp, Color(0xFF454C49)), shape = shape),
            focusedBorder = Border(BorderStroke(1.5.dp, FnColors.Coral), shape = shape),
            pressedBorder = Border(BorderStroke(1.5.dp, FnColors.Coral), shape = shape),
        ),
        contentPadding = PaddingValues(start = 5.dp, top = 4.5.dp, end = 9.dp, bottom = 4.5.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (coverId != null) {
                RemoteArtwork(
                    container = LocalAuthenticatedDependencies.current,
                    coverId = coverId,
                    variant = CoverVariant.Compact,
                    modifier = Modifier.size(27.dp),
                    shape = CircleShape,
                    contentScale = ContentScale.Crop,
                    placeholderContent = { NowPlayingArtworkFallback(playback.title) },
                )
            } else {
                NowPlayingArtworkFallback(playback.title)
            }
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(3.5.dp).background(if (playback.isPlaying) FnColors.Coral else FnColors.Muted, CircleShape))
                    Spacer(Modifier.width(3.5.dp))
                    Text(
                        if (playback.isPlaying) "正在播放" else "已暂停",
                        color = Color(0xFFB7BBB7),
                        fontSize = 9.sp,
                        lineHeight = 9.sp,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(1.5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        playback.title.ifBlank { "正在播放" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = true),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = onTitleTextLayout,
                        modifier = Modifier.weight(1f),
                    )
                    if (playback.artist.isNotBlank()) {
                        Spacer(Modifier.width(4.5.dp))
                        Text(
                            playback.artist,
                            color = Color(0xFFA7ABA7),
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(0.62f, fill = false),
                        )
                    }
                }
            }
            Spacer(Modifier.width(5.dp))
            Text("›", color = Color(0xFFC6C9C5), fontSize = 17.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun NowPlayingArtworkFallback(title: String) {
    Box(
        Modifier.size(27.dp).background(Color(0xFF31413D), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title.trim().take(1).ifBlank { "音" }.uppercase(),
            color = FnColors.Teal,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BrowseHome(
    container: AuthenticatedAppDependencies,
    playback: PlaybackUiState,
    onMy: () -> Unit,
    onSearch: () -> Unit,
    onPlaylist: (Playlist) -> Unit,
    onFavorites: () -> Unit,
    onAll: () -> Unit,
    onFolders: () -> Unit,
    onArtists: () -> Unit,
    onAlbums: () -> Unit,
    onGenres: () -> Unit,
    onPlayer: () -> Unit,
) {
    val retainedStore = LocalLibraryRetainedState.current
    val playlistState = retainedStore.list<Playlist>("playlists")
    val albumState = retainedStore.paged<Album>("grid:albums")
    val favoritePreviewState = retainedStore.paged<Track>("home:favorites-preview")
    val favoriteLibraryState by container.musicRepository.favoriteState.collectAsStateWithLifecycle()
    val playlistSnapshot = playlistState.snapshot
    val playlists = playlistSnapshot.entries
    val playlistsLoaded = playlistSnapshot.initialLoadCompleted
    val albums = albumState.snapshot.entries
    val favoriteTracks = favoritePreviewState.snapshot.entries
    val roamArtwork = remember(albums, playlists) {
        featureArtworkSlots(
            primary = albums.map { FeatureArtworkItem(it.name, it.coverId) },
            fallback = playlists.map { FeatureArtworkItem(it.name, it.coverId) },
        )
    }
    val favoriteArtwork = remember(favoriteTracks) {
        featureArtworkSlots(
            primary = favoriteTracks.map { FeatureArtworkItem(it.title, it.coverId) },
        )
    }
    var actionError by remember { mutableStateOf<AppError?>(null) }
    var roamActionRunning by remember { mutableStateOf(false) }
    var focusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var initialFocusRequested by remember { mutableStateOf(false) }
    val contentFocus = remember { FocusRequester() }
    val nowPlayingFocus = remember { FocusRequester() }
    val homeTabFocus = remember { FocusRequester() }
    val myTabFocus = remember { FocusRequester() }
    val searchTabFocus = remember { FocusRequester() }
    val roamFocus = remember { FocusRequester() }
    val favoritesFocus = remember { FocusRequester() }
    val folderEntryFocus = remember { FocusRequester() }
    val artistEntryFocus = remember { FocusRequester() }
    val albumEntryFocus = remember { FocusRequester() }
    val genreEntryFocus = remember { FocusRequester() }
    val playlistRowFocus = remember { FocusRequester() }
    val homeScrollState = rememberScrollState()
    val rowState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        retainedStore.loadListOnce(playlistState, container.musicRepository::playlists)
        // 首页预览与专辑列表共享同一 retained 状态：分页大小必须一致（48），
        // 否则列表进入时只有预览的 12 条且不再重新加载。
        retainedStore.loadFirstPageOnce(
            albumState,
            { page -> container.musicRepository.albums(page, CATALOG_LIST_PAGE_SIZE) },
        ) { it.guid.value }
    }
    LaunchedEffect(favoriteLibraryState.revision) {
        retainedStore.loadFirstPageForRevision(
            state = favoritePreviewState,
            revision = favoriteLibraryState.revision,
            loader = container.musicRepository::favoriteTracks,
            key = { it.guid.value },
        )
    }
    LaunchedEffect(playlistsLoaded, playback.hasMedia, focusedKey) {
        if (initialFocusRequested) return@LaunchedEffect
        val availableKeys = buildList {
            add("roam")
            add("favorites")
            addAll(HOME_LIBRARY_META.map { "entry:${it.key}" })
            if (playlistsLoaded) {
                addAll(playlists.map { "playlist:${it.guid.value}" })
                add("all-playlists")
            }
            if (playback.hasMedia) add("now-playing")
        }
        focusedKey = focusedKey?.takeIf(availableKeys::contains) ?: availableKeys.firstOrNull()
        yield()
        if (focusedKey != null) {
            runCatching { contentFocus.requestFocus() }
            initialFocusRequested = true
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 24.dp)
            .verticalScroll(homeScrollState),
    ) {
        LibraryTopBar(
            playback = playback,
            selectedHome = true,
            onHome = {},
            onMy = onMy,
            onSearch = onSearch,
            onPlayer = {
                focusedKey = "now-playing"
                onPlayer()
            },
            modifier = Modifier
                .then(if (focusedKey == "now-playing") Modifier.focusRequester(contentFocus) else Modifier)
                .onFocusChanged { if (it.isFocused) focusedKey = "now-playing" },
            nowPlayingFocus = nowPlayingFocus,
            homeTabFocus = homeTabFocus,
            myTabFocus = myTabFocus,
            searchFocus = searchTabFocus,
            contentDownFocus = roamFocus,
        )
        Spacer(Modifier.height(12.dp))
        Text("听点什么", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            HomeFeatureCard(
                title = "随机漫游",
                kind = HomeArtworkKind.Roam,
                artwork = roamArtwork,
                cardHeight = 130.dp,
                modifier = Modifier
                    .weight(1.6f)
                    .focusProperties {
                        up = if (playback.hasMedia) nowPlayingFocus else homeTabFocus
                        right = favoritesFocus
                        down = folderEntryFocus
                    }
                    .focusRequester(roamFocus)
                    .then(if (focusedKey == "roam") Modifier.focusRequester(contentFocus) else Modifier)
                    .onFocusChanged { if (it.isFocused) focusedKey = "roam" },
                onClick = {
                focusedKey = "roam"
                if (playback.queueKind == QueueKind.Roam) {
                    onPlayer()
                    return@HomeFeatureCard
                }
                if (roamActionRunning || playback.roamBusy) return@HomeFeatureCard
                roamActionRunning = true
                scope.launch {
                    try {
                        if (container.playbackController.startRoam()) {
                            actionError = null
                            onPlayer()
                        } else {
                            actionError = container.playbackController.state.value.roamError ?: AppError.Unknown()
                        }
                    } finally {
                        roamActionRunning = false
                    }
                }
                },
            )
            HomeFeatureCard(
                title = "收藏",
                kind = HomeArtworkKind.Favorites,
                artwork = favoriteArtwork,
                cardHeight = 130.dp,
                modifier = Modifier
                    .weight(1f)
                    .focusProperties {
                        up = myTabFocus
                        left = roamFocus
                        right = FocusRequester.Cancel
                        down = folderEntryFocus
                    }
                    .focusRequester(favoritesFocus)
                    .then(if (focusedKey == "favorites") Modifier.focusRequester(contentFocus) else Modifier)
                    .onFocusChanged { if (it.isFocused) focusedKey = "favorites" },
                onClick = {
                    focusedKey = "favorites"
                    onFavorites()
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            HOME_LIBRARY_META.forEach { entry ->
                val focusRequester = when (entry.key) {
                    "folders" -> folderEntryFocus
                    "artists" -> artistEntryFocus
                    "albums" -> albumEntryFocus
                    else -> genreEntryFocus
                }
                val onOpen: () -> Unit = when (entry.key) {
                    "folders" -> onFolders
                    "artists" -> onArtists
                    "albums" -> onAlbums
                    else -> onGenres
                }
                HomeEntryTile(
                    title = entry.title,
                    subtitle = entry.subtitle,
                    accent = entry.accent,
                    iconRes = entry.iconRes,
                    modifier = Modifier
                        .weight(1f)
                        .focusProperties {
                            left = if (entry.key == "folders") FocusRequester.Cancel else FocusRequester.Default
                            right = if (entry.key == "genres") FocusRequester.Cancel else FocusRequester.Default
                            up = if (entry.key == "folders" || entry.key == "artists") roamFocus else favoritesFocus
                            down = playlistRowFocus
                        }
                        .focusRequester(focusRequester)
                        .then(if (focusedKey == "entry:${entry.key}") Modifier.focusRequester(contentFocus) else Modifier)
                        .onFocusChanged { if (it.isFocused) focusedKey = "entry:${entry.key}" },
                    onClick = {
                        focusedKey = "entry:${entry.key}"
                        onOpen()
                    },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            itemsIndexed(playlists, key = { _, playlist -> playlist.guid.value }) { index, playlist ->
                val key = "playlist:${playlist.guid.value}"
                HomePlaylistLockup(
                    title = playlist.name,
                    subtitle = "歌单",
                    coverId = playlist.coverId,
                    modifier = Modifier
                        .focusProperties {
                            up = if (index == 0) folderEntryFocus else FocusRequester.Default
                            left = if (index == 0) FocusRequester.Cancel else FocusRequester.Default
                        }
                        .then(if (index == 0) Modifier.focusRequester(playlistRowFocus) else Modifier)
                        .then(if (focusedKey == key) Modifier.focusRequester(contentFocus) else Modifier)
                        .onFocusChanged { if (it.isFocused) focusedKey = key },
                    onClick = {
                        focusedKey = key
                        onPlaylist(playlist)
                    },
                )
            }
            item {
                HomePlaylistLockup(
                    title = "全部歌单",
                    subtitle = "浏览全部",
                    coverId = null,
                    fallback = HomeArtworkKind.PlaylistGrid,
                    modifier = Modifier
                        .focusProperties {
                            up = playlistRowFocus.takeIf { playlists.isNotEmpty() } ?: folderEntryFocus
                            right = FocusRequester.Cancel
                        }
                        .then(if (playlists.isEmpty()) Modifier.focusRequester(playlistRowFocus) else Modifier)
                        .then(if (focusedKey == "all-playlists") Modifier.focusRequester(contentFocus) else Modifier)
                        .onFocusChanged { if (it.isFocused) focusedKey = "all-playlists" },
                    onClick = {
                        focusedKey = "all-playlists"
                        onAll()
                    },
                )
            }
        }
        (actionError ?: playlistSnapshot.error)?.let { InlineError(it) }
    }
}

private data class HomeLibraryEntryMeta(
    val key: String,
    val title: String,
    val subtitle: String,
    val accent: Color,
    val iconRes: Int,
)

private val HOME_LIBRARY_META = listOf(
    HomeLibraryEntryMeta("folders", "文件夹", "按目录浏览歌曲", FnColors.Teal, R.drawable.ic_home_folder),
    HomeLibraryEntryMeta("artists", "歌手", "全部歌手", FnColors.Coral, R.drawable.ic_home_artist),
    HomeLibraryEntryMeta("albums", "专辑", "全部专辑", Color(0xFFE8C36A), R.drawable.ic_home_album),
    HomeLibraryEntryMeta("genres", "风格", "全部风格", Color(0xFF9F72D8), R.drawable.ic_home_genre),
)

internal data class FeatureArtworkItem(
    val title: String,
    val coverId: String?,
)

/** 首页乐库入口磁贴（文件夹/歌手/专辑/风格），点击跳转对应浏览页。 */
@Composable
private fun HomeEntryTile(
    title: String,
    subtitle: String,
    accent: Color,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Button(
        onClick = onClick,
        modifier = modifier.height(88.dp),
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.03f),
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF141918),
            contentColor = FnColors.Text,
            focusedContainerColor = Color(0xFF1D2321),
            focusedContentColor = FnColors.Text,
            pressedContainerColor = Color(0xFF1D2321),
        ),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), shape = shape),
            focusedBorder = Border(BorderStroke(1.5.dp, FnColors.Coral), shape = shape),
            pressedBorder = Border(BorderStroke(1.5.dp, FnColors.Coral), shape = shape),
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(shape = RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    colorFilter = ColorFilter.tint(accent),
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = FnColors.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

internal fun featureArtworkSlots(
    primary: List<FeatureArtworkItem>,
    fallback: List<FeatureArtworkItem> = emptyList(),
    limit: Int = 3,
): List<FeatureArtworkItem> {
    if (limit <= 0) return emptyList()
    return (primary + fallback)
        .asSequence()
        .mapNotNull { item ->
            item.coverId?.trim()?.takeIf(String::isNotEmpty)?.let { item.copy(coverId = it) }
        }
        .distinctBy { it.coverId }
        .take(limit)
        .toList()
}

@Composable
private fun HomeFeatureCard(
    title: String,
    kind: HomeArtworkKind,
    artwork: List<FeatureArtworkItem>,
    modifier: Modifier = Modifier,
    cardHeight: Dp = 150.dp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val background = when (kind) {
        HomeArtworkKind.Roam -> Brush.horizontalGradient(listOf(Color(0xFF071D19), Color(0xFF102823)))
        HomeArtworkKind.Favorites -> Brush.horizontalGradient(listOf(Color(0xFF1C1110), Color(0xFF2A1615)))
        HomeArtworkKind.Collection,
        HomeArtworkKind.PlaylistGrid,
        -> Brush.horizontalGradient(listOf(Color(0xFF17201E), Color(0xFF22302D)))
    }
    Button(
        onClick = onClick,
        modifier = modifier.height(cardHeight),
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.018f),
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = FnColors.Text,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = FnColors.Text,
            pressedContainerColor = Color.Transparent,
            pressedContentColor = FnColors.Text,
        ),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f)), shape = shape),
            focusedBorder = Border(BorderStroke(1.5.dp, FnColors.Coral), shape = shape),
            pressedBorder = Border(BorderStroke(1.5.dp, FnColors.Coral), shape = shape),
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxSize().background(background)) {
            FeatureCoverDeck(kind, artwork, Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = 0.27f),
                            0.38f to Color.Black.copy(alpha = 0.06f),
                            1f to Color.Black.copy(alpha = 0.13f),
                        ),
                    ),
            )
            FeatureGlyph(
                kind = kind,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 18.dp).size(48.dp),
            )
            Text(
                title,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 18.dp),
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FeatureCoverDeck(
    kind: HomeArtworkKind,
    artwork: List<FeatureArtworkItem>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val placements = deckPlacements(kind, artwork.size)
    Box(modifier.clipToBounds()) {
        placements.forEach { placement ->
            val item = artwork.getOrNull(placement.itemIndex) ?: return@forEach
            val artworkShape = RoundedCornerShape(7.dp)
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(112.dp)
                    .graphicsLayer {
                        translationX = with(density) { placement.translationXDp.dp.toPx() }
                        translationY = with(density) { placement.translationYDp.dp.toPx() }
                        rotationZ = placement.rotation
                        shadowElevation = with(density) { 7.dp.toPx() }
                        shape = artworkShape
                        clip = true
                    }
                    .border(0.5.dp, Color.White.copy(alpha = 0.22f), artworkShape),
            ) {
                val coverId = item.coverId
                if (coverId != null) {
                    RemoteArtwork(
                        container = LocalAuthenticatedDependencies.current,
                        coverId = coverId,
                        variant = CoverVariant.Grid,
                        fallbackVariant = CoverVariant.Compact,
                        modifier = Modifier.fillMaxSize(),
                        shape = artworkShape,
                        contentScale = ContentScale.Crop,
                        placeholderContent = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF242927), artworkShape),
                            )
                        },
                    )
                } else {
                    InitialArtworkPlaceholder(
                        text = item.title,
                        accent = if (kind == HomeArtworkKind.Roam) FnColors.Teal else FnColors.Coral,
                        modifier = Modifier.fillMaxSize(),
                        shape = artworkShape,
                    )
                }
            }
        }
    }
}

private fun deckPlacements(kind: HomeArtworkKind, artworkCount: Int): List<DeckPlacement> =
    when (kind) {
        HomeArtworkKind.Roam -> when (artworkCount.coerceAtMost(3)) {
            1 -> listOf(DeckPlacement(0, -72f, 3f, 0f))
            2 -> listOf(
                DeckPlacement(0, -118f, 7f, -8f),
                DeckPlacement(1, -24f, 5f, 8f),
            )
            3 -> listOf(
                DeckPlacement(0, -146f, 6f, -11f),
                DeckPlacement(1, -76f, 8f, 0f),
                DeckPlacement(2, -8f, 3f, 10f),
            )
            else -> emptyList()
        }
        HomeArtworkKind.Favorites -> when (artworkCount.coerceAtMost(3)) {
            1 -> listOf(DeckPlacement(0, -72f, 0f, 0f))
            2 -> listOf(
                DeckPlacement(0, -118f, 7f, -6f),
                DeckPlacement(1, -24f, 7f, 6f),
            )
            3 -> listOf(
                DeckPlacement(0, -140f, 8f, -7f),
                DeckPlacement(2, -8f, 8f, 7f),
                DeckPlacement(1, -72f, 0f, 0f),
            )
            else -> emptyList()
        }
        HomeArtworkKind.Collection,
        HomeArtworkKind.PlaylistGrid,
        -> emptyList()
    }

private data class DeckPlacement(
    val itemIndex: Int,
    val translationXDp: Float,
    val translationYDp: Float,
    val rotation: Float,
)

@Composable
private fun FeatureGlyph(kind: HomeArtworkKind, modifier: Modifier = Modifier) {
    val accent = if (kind == HomeArtworkKind.Roam) FnColors.Teal else FnColors.Coral
    Box(
        modifier
            .background(Color(0xFF0E1314).copy(alpha = 0.92f), CircleShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(25.dp)) {
            if (kind == HomeArtworkKind.Favorites) {
                drawPath(heartPath(size), color = accent, style = Stroke(width = 2.2.dp.toPx()))
            } else {
                val stroke = 2.1.dp.toPx()
                val startX = size.width * 0.08f
                val endX = size.width * 0.88f
                fun point(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(x, y)
                drawLine(accent, point(startX, size.height * 0.24f), point(size.width * 0.34f, size.height * 0.24f), stroke, StrokeCap.Round)
                drawLine(accent, point(size.width * 0.34f, size.height * 0.24f), point(size.width * 0.67f, size.height * 0.76f), stroke, StrokeCap.Round)
                drawLine(accent, point(size.width * 0.67f, size.height * 0.76f), point(endX, size.height * 0.76f), stroke, StrokeCap.Round)
                drawLine(accent, point(startX, size.height * 0.76f), point(size.width * 0.34f, size.height * 0.76f), stroke, StrokeCap.Round)
                drawLine(accent, point(size.width * 0.34f, size.height * 0.76f), point(size.width * 0.67f, size.height * 0.24f), stroke, StrokeCap.Round)
                drawLine(accent, point(size.width * 0.67f, size.height * 0.24f), point(endX, size.height * 0.24f), stroke, StrokeCap.Round)
                drawLine(accent, point(endX, size.height * 0.24f), point(size.width * 0.75f, size.height * 0.12f), stroke, StrokeCap.Round)
                drawLine(accent, point(endX, size.height * 0.24f), point(size.width * 0.75f, size.height * 0.36f), stroke, StrokeCap.Round)
                drawLine(accent, point(endX, size.height * 0.76f), point(size.width * 0.75f, size.height * 0.64f), stroke, StrokeCap.Round)
                drawLine(accent, point(endX, size.height * 0.76f), point(size.width * 0.75f, size.height * 0.88f), stroke, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun HomePlaylistLockup(
    title: String,
    subtitle: String,
    coverId: String?,
    modifier: Modifier = Modifier,
    fallback: HomeArtworkKind? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Button(
        onClick = onClick,
        modifier = modifier
            .size(width = 190.dp, height = 174.dp)
            .onFocusChanged { focused = it.isFocused },
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.018f),
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = FnColors.Text,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = FnColors.Text,
            pressedContainerColor = Color.Transparent,
            pressedContentColor = FnColors.Text,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .then(if (focused) Modifier.border(1.5.dp, FnColors.Coral, shape) else Modifier)
                    .clip(shape),
            ) {
                PlaylistTileArtwork(
                    title = title,
                    coverId = coverId,
                    accent = FnColors.Coral,
                    modifier = Modifier.fillMaxSize(),
                    featureArtwork = fallback,
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(title, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = FnColors.Muted, fontSize = 12.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

internal fun isHomeBackConfirmed(
    previousBackAt: Long,
    currentBackAt: Long,
    windowMs: Long = 2_000L,
): Boolean = previousBackAt > 0L && currentBackAt >= previousBackAt &&
    currentBackAt - previousBackAt <= windowMs

@Composable
private fun BrowseMy(
    container: AuthenticatedAppDependencies,
    session: SessionState.SignedIn,
    playback: PlaybackUiState,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onArtists: () -> Unit,
    onAlbums: () -> Unit,
    onGenres: () -> Unit,
    onAllTracks: () -> Unit,
    onFolders: () -> Unit,
    onArtist: (Artist) -> Unit,
    onAlbum: (Album) -> Unit,
    onGenre: (Genre) -> Unit,
    onSettings: () -> Unit,
    onPlayer: () -> Unit,
) {
    val retainedStore = LocalLibraryRetainedState.current
    val artistState = retainedStore.paged<Artist>("grid:artists")
    val albumState = retainedStore.paged<Album>("grid:albums")
    val genreState = retainedStore.paged<Genre>("grid:genres")
    val artists = artistState.snapshot.entries
    val albums = albumState.snapshot.entries
    val genres = genreState.snapshot.entries
    val artistsLoaded = artistState.snapshot.initialLoadCompleted
    val albumsLoaded = albumState.snapshot.initialLoadCompleted
    val genresLoaded = genreState.snapshot.initialLoadCompleted
    var focusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var initialFocusRequested by remember { mutableStateOf(false) }
    val contentFocus = remember { FocusRequester() }
    val nowPlayingFocus = remember { FocusRequester() }
    val homeTabFocus = remember { FocusRequester() }
    val myTabFocus = remember { FocusRequester() }
    val searchTabFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    val switchAccountFocus = remember { FocusRequester() }
    val artistRowFocus = remember { FocusRequester() }
    val albumRowFocus = remember { FocusRequester() }
    val genreRowFocus = remember { FocusRequester() }
    val libraryRowFocus = remember { FocusRequester() }
    val folderRowFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = retainedStore.scope
    LaunchedEffect(Unit) {
        // 预览与滚动列表共享同一 retained 状态：分页大小必须一致（48），
        // 否则列表进入时只有预览的 12 条，且后续按 48 取页会跳过中间条目。
        retainedStore.loadFirstPageOnce(
            artistState,
            { page -> container.musicRepository.artists(page, CATALOG_LIST_PAGE_SIZE) },
        ) { it.guid.value }
        retainedStore.loadFirstPageOnce(
            albumState,
            { page -> container.musicRepository.albums(page, CATALOG_LIST_PAGE_SIZE) },
        ) { it.guid.value }
        retainedStore.loadFirstPageOnce(
            genreState,
            { page -> container.musicRepository.genres(page, CATALOG_LIST_PAGE_SIZE) },
        ) { it.guid.value }
    }
    LaunchedEffect(artistsLoaded, albumsLoaded, genresLoaded, playback.hasMedia, focusedKey) {
        if (initialFocusRequested) return@LaunchedEffect
        val allContentLoaded = artistsLoaded && albumsLoaded && genresLoaded
        val restoringChrome = focusedKey == "settings" || focusedKey == "switch-account" ||
            playback.hasMedia && focusedKey == "now-playing"
        if (!allContentLoaded && !restoringChrome) return@LaunchedEffect
        val availableKeys = buildList {
            if (allContentLoaded) {
                addAll(artists.take(8).map { "artist:${it.guid.value}" })
                add("all-artists")
                addAll(albums.take(8).map { "album:${it.guid.value}" })
                add("all-albums")
                addAll(genres.take(8).map { "genre:${it.guid.value}" })
                add("all-genres")
                add("all-tracks")
                add("folders")
            }
            if (playback.hasMedia) add("now-playing")
            add("settings")
            add("switch-account")
        }
        focusedKey = focusedKey?.takeIf(availableKeys::contains) ?: availableKeys.firstOrNull()
        yield()
        if (focusedKey != null) {
            runCatching { contentFocus.requestFocus() }
            initialFocusRequested = true
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp)
            .padding(top = 24.dp),
    ) {
        LibraryTopBar(
            playback = playback,
            selectedHome = false,
            onHome = onHome,
            onMy = {},
            onSearch = onSearch,
            onPlayer = {
                focusedKey = "now-playing"
                onPlayer()
            },
            modifier = Modifier
                .then(if (focusedKey == "now-playing") Modifier.focusRequester(contentFocus) else Modifier)
                .onFocusChanged { if (it.isFocused) focusedKey = "now-playing" },
            nowPlayingFocus = nowPlayingFocus,
            homeTabFocus = homeTabFocus,
            myTabFocus = myTabFocus,
            searchFocus = searchTabFocus,
            contentDownFocus = settingsFocus,
        )
        Spacer(Modifier.height(12.dp))
        ProfileStrip(
            username = session.user.username,
            serverName = session.server.name,
            focusedKey = focusedKey,
            restoredFocus = contentFocus,
            settingsFocus = settingsFocus,
            switchAccountFocus = switchAccountFocus,
            homeTabFocus = homeTabFocus,
            myTabFocus = myTabFocus,
            artistRowFocus = artistRowFocus,
            onFocused = { focusedKey = it },
            onSettings = {
                focusedKey = "settings"
                onSettings()
            },
            onSwitchAccount = {
                focusedKey = "switch-account"
                scope.launch { container.authenticatedActions.switchAccount() }
            },
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(state = listState) {
            item {
                MediaBand(
                    "歌手",
                    artists.take(8).map {
                        val key = "artist:${it.guid.value}"
                        BandEntry(it.name, "${it.trackCount ?: 0} 首歌曲", it.coverId, BandKind.Artist, key) {
                            focusedKey = key
                            onArtist(it)
                        }
                    },
                    BandEntry("全部歌手", "浏览完整列表", null, BandKind.Artist, "all-artists") {
                        focusedKey = "all-artists"
                        onArtists()
                    },
                    focusedKey,
                    contentFocus,
                    rowFocusRequester = artistRowFocus,
                    upFocusRequester = settingsFocus,
                    downFocusRequester = albumRowFocus,
                    onFocused = { focusedKey = it },
                )
            }
            item {
                MediaBand(
                    "专辑",
                    // 同名同歌手的专辑合并展示，与专辑列表的去重规则一致。
                    albums.distinctBy { it.name to (it.artistName ?: "") }.take(8).map {
                        val key = "album:${it.guid.value}"
                        BandEntry(it.name, it.artistName.orEmpty(), it.coverId, BandKind.Album, key) {
                            focusedKey = key
                            onAlbum(it)
                        }
                    },
                    BandEntry("全部专辑", "浏览完整列表", null, BandKind.Album, "all-albums") {
                        focusedKey = "all-albums"
                        onAlbums()
                    },
                    focusedKey,
                    contentFocus,
                    rowFocusRequester = albumRowFocus,
                    upFocusRequester = artistRowFocus,
                    downFocusRequester = genreRowFocus,
                    onFocused = { focusedKey = it },
                )
            }
            item {
                MediaBand(
                    "风格",
                    genres.take(8).map {
                        val key = "genre:${it.guid.value}"
                        BandEntry(it.name, "${it.trackCount ?: 0} 首歌曲", it.coverId, BandKind.Genre, key) {
                            focusedKey = key
                            onGenre(it)
                        }
                    },
                    BandEntry("全部风格", "浏览完整列表", null, BandKind.Genre, "all-genres") {
                        focusedKey = "all-genres"
                        onGenres()
                    },
                    focusedKey,
                    contentFocus,
                    rowFocusRequester = genreRowFocus,
                    upFocusRequester = albumRowFocus,
                    downFocusRequester = libraryRowFocus,
                    onFocused = { focusedKey = it },
                )
            }
            item {
                MediaBand(
                    "音乐库",
                    emptyList(),
                    BandEntry("全部歌曲", "完整曲库", null, BandKind.Library, "all-tracks") {
                        focusedKey = "all-tracks"
                        onAllTracks()
                    },
                    focusedKey,
                    contentFocus,
                    rowFocusRequester = libraryRowFocus,
                    upFocusRequester = genreRowFocus,
                    downFocusRequester = folderRowFocus,
                    onFocused = { focusedKey = it },
                )
            }
            item {
                MediaBand(
                    "文件夹",
                    emptyList(),
                    BandEntry("文件夹浏览", "按目录筛选歌曲", null, BandKind.Folder, "folders") {
                        focusedKey = "folders"
                        onFolders()
                    },
                    focusedKey,
                    contentFocus,
                    rowFocusRequester = folderRowFocus,
                    upFocusRequester = libraryRowFocus,
                    downFocusRequester = FocusRequester.Cancel,
                    onFocused = { focusedKey = it },
                )
            }
        }
    }
}

@Composable
private fun ProfileStrip(
    username: String,
    serverName: String,
    focusedKey: String?,
    restoredFocus: FocusRequester,
    settingsFocus: FocusRequester,
    switchAccountFocus: FocusRequester,
    homeTabFocus: FocusRequester,
    myTabFocus: FocusRequester,
    artistRowFocus: FocusRequester,
    onFocused: (String) -> Unit,
    onSettings: () -> Unit,
    onSwitchAccount: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(username, Modifier.size(40.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                username.ifBlank { "音乐用户" },
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            ServerChip(serverName)
        }
        Spacer(Modifier.width(12.dp))
        ProfileActionButton(
            label = "设置",
            glyph = ProfileGlyph.Settings,
            modifier = Modifier
                .width(72.dp)
                .focusProperties {
                    left = FocusRequester.Cancel
                    right = switchAccountFocus
                    up = homeTabFocus
                    down = artistRowFocus
                }
                .focusRequester(settingsFocus)
                .then(if (focusedKey == "settings") Modifier.focusRequester(restoredFocus) else Modifier)
                .onFocusChanged { if (it.isFocused) onFocused("settings") },
            onClick = onSettings,
        )
        Spacer(Modifier.width(8.dp))
        ProfileActionButton(
            label = "切换账号",
            glyph = ProfileGlyph.SwitchAccount,
            modifier = Modifier
                .width(102.dp)
                .focusProperties {
                    left = settingsFocus
                    right = FocusRequester.Cancel
                    up = myTabFocus
                    down = artistRowFocus
                }
                .focusRequester(switchAccountFocus)
                .then(if (focusedKey == "switch-account") Modifier.focusRequester(restoredFocus) else Modifier)
                .onFocusChanged { if (it.isFocused) onFocused("switch-account") },
            onClick = onSwitchAccount,
        )
    }
}

@Composable
private fun ProfileAvatar(username: String, modifier: Modifier = Modifier) {
    Box(
        modifier.background(
            brush = Brush.linearGradient(
                listOf(Color(0xFFFF8A70), Color(0xFFD6A35E), Color(0xFF70C8AF)),
            ),
            shape = CircleShape,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            username.trim().take(1).ifBlank { "音" }.uppercase(),
            color = FnColors.Background,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ServerChip(serverName: String) {
    val shape = CircleShape
    Row(
        modifier = Modifier
            .width(132.dp)
            .height(18.dp)
            .border(0.5.dp, Color.White.copy(alpha = 0.17f), shape)
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileGlyphCanvas(ProfileGlyph.Server, Modifier.size(11.dp), Color(0xFF9FA5A8))
        Spacer(Modifier.width(5.dp))
        Text(
            serverName.ifBlank { "NAS" },
            color = Color(0xFFB7BBBE),
            fontSize = 10.sp,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class ProfileGlyph { Settings, SwitchAccount, Server }

@Composable
private fun ProfileActionButton(
    label: String,
    glyph: ProfileGlyph,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.035f),
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color(0xFFC7CACC),
            focusedContainerColor = FnColors.Coral.copy(alpha = 0.08f),
            focusedContentColor = FnColors.Coral,
            pressedContainerColor = FnColors.Coral.copy(alpha = 0.13f),
            pressedContentColor = FnColors.Coral,
        ),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f)), shape = shape),
            focusedBorder = Border(BorderStroke(1.2.dp, FnColors.Coral), shape = shape),
            pressedBorder = Border(BorderStroke(1.2.dp, FnColors.Coral), shape = shape),
        ),
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
    ) {
        ProfileGlyphCanvas(glyph, Modifier.size(15.dp), LocalContentColor.current)
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, lineHeight = 15.sp, maxLines = 1)
    }
}

@Composable
private fun ProfileGlyphCanvas(
    glyph: ProfileGlyph,
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier) {
        val stroke = 1.45.dp.toPx()
        when (glyph) {
            ProfileGlyph.Settings -> {
                drawCircle(color, size.minDimension * 0.24f, center, style = Stroke(stroke))
                drawCircle(color, size.minDimension * 0.06f, center)
                repeat(8) { index ->
                    val angle = index * Math.PI.toFloat() / 4f
                    val inner = size.minDimension * 0.34f
                    val outer = size.minDimension * 0.46f
                    drawLine(
                        color,
                        androidx.compose.ui.geometry.Offset(center.x + kotlin.math.cos(angle) * inner, center.y + kotlin.math.sin(angle) * inner),
                        androidx.compose.ui.geometry.Offset(center.x + kotlin.math.cos(angle) * outer, center.y + kotlin.math.sin(angle) * outer),
                        stroke,
                        StrokeCap.Round,
                    )
                }
            }
            ProfileGlyph.SwitchAccount -> {
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.33f), androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.33f), stroke, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.33f), androidx.compose.ui.geometry.Offset(size.width * 0.66f, size.height * 0.17f), stroke, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.33f), androidx.compose.ui.geometry.Offset(size.width * 0.66f, size.height * 0.49f), stroke, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.69f), androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.69f), stroke, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.69f), androidx.compose.ui.geometry.Offset(size.width * 0.34f, size.height * 0.53f), stroke, StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.69f), androidx.compose.ui.geometry.Offset(size.width * 0.34f, size.height * 0.85f), stroke, StrokeCap.Round)
            }
            ProfileGlyph.Server -> {
                val shape = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
                drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.06f, size.height * 0.08f), size = androidx.compose.ui.geometry.Size(size.width * 0.88f, size.height * 0.34f), cornerRadius = shape, style = Stroke(stroke))
                drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.06f, size.height * 0.58f), size = androidx.compose.ui.geometry.Size(size.width * 0.88f, size.height * 0.34f), cornerRadius = shape, style = Stroke(stroke))
            }
        }
    }
}

private enum class BandKind { Artist, Album, Genre, Folder, Library }

private data class BandEntry(
    val title: String,
    val subtitle: String,
    val coverId: String?,
    val kind: BandKind,
    val focusKey: String,
    val action: (() -> Unit)?,
)

internal fun mediaBandReturnFocusKey(
    entryKeys: List<String>,
    terminalKey: String,
    lastFocusedKey: String?,
): String = lastFocusedKey
    ?.takeIf { it == terminalKey || it in entryKeys }
    ?: entryKeys.firstOrNull()
    ?: terminalKey

@Composable
private fun MediaBand(
    title: String,
    entries: List<BandEntry>,
    terminalEntry: BandEntry,
    focusedKey: String?,
    focusRequester: FocusRequester,
    rowFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    onFocused: (String) -> Unit,
) {
    var lastFocusedEntryKey by rememberSaveable(terminalEntry.focusKey) {
        mutableStateOf<String?>(null)
    }
    val entryKeys = entries.map(BandEntry::focusKey)
    val returnFocusKey = mediaBandReturnFocusKey(
        entryKeys = entryKeys,
        terminalKey = terminalEntry.focusKey,
        lastFocusedKey = lastFocusedEntryKey,
    )
    Column {
        Text(title, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.focusKey }) { index, entry ->
                BandLockup(
                    entry,
                    Modifier
                        .focusProperties {
                            if (index == 0) left = FocusRequester.Cancel
                            up = upFocusRequester
                            down = downFocusRequester
                        }
                        .then(if (returnFocusKey == entry.focusKey) Modifier.focusRequester(rowFocusRequester) else Modifier)
                        .then(if (focusedKey == entry.focusKey) Modifier.focusRequester(focusRequester) else Modifier)
                        .onFocusChanged {
                            if (it.isFocused) {
                                lastFocusedEntryKey = entry.focusKey
                                onFocused(entry.focusKey)
                            }
                        },
                )
            }
            item {
                BandLockup(
                    terminalEntry,
                    Modifier
                        .focusProperties {
                            if (entries.isEmpty()) left = FocusRequester.Cancel
                            right = FocusRequester.Cancel
                            up = upFocusRequester
                            down = downFocusRequester
                        }
                        .then(if (returnFocusKey == terminalEntry.focusKey) Modifier.focusRequester(rowFocusRequester) else Modifier)
                        .then(if (focusedKey == terminalEntry.focusKey) Modifier.focusRequester(focusRequester) else Modifier)
                        .onFocusChanged {
                            if (it.isFocused) {
                                lastFocusedEntryKey = terminalEntry.focusKey
                                onFocused(terminalEntry.focusKey)
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun BandLockup(entry: BandEntry, modifier: Modifier = Modifier) {
    when (entry.kind) {
        BandKind.Artist -> ArtistLockup(
            entry.title,
            entry.subtitle,
            entry.coverId,
            modifier = modifier,
            enabled = entry.action != null,
        ) { entry.action?.invoke() }
        BandKind.Album -> AlbumLockup(
            entry.title,
            entry.subtitle,
            entry.coverId,
            modifier = modifier,
            enabled = entry.action != null,
        ) { entry.action?.invoke() }
        BandKind.Genre -> GenreTile(
            entry.title,
            entry.subtitle,
            modifier = modifier,
            enabled = entry.action != null,
        ) { entry.action?.invoke() }
        BandKind.Folder -> MyLibraryLockup(
            entry.title,
            entry.subtitle,
            modifier = modifier,
            enabled = entry.action != null,
        ) { entry.action?.invoke() }
        BandKind.Library -> MyLibraryLockup(
            entry.title,
            entry.subtitle,
            modifier = modifier,
            enabled = entry.action != null,
        ) { entry.action?.invoke() }
    }
}

@Composable
private fun MyLibraryLockup(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    coverIds: List<String> = emptyList(),
    onClick: () -> Unit,
) {
    val container = LocalAuthenticatedDependencies.current
    val shape = RoundedCornerShape(8.dp)
    val artworkShape = RoundedCornerShape(4.dp)
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
            .then(
                if (fillWidth) Modifier.fillMaxWidth().height(95.dp)
                else Modifier.size(width = 170.dp, height = 95.dp),
            )
            .onFocusChanged { state ->
                if (state.isFocused) {
                    // 聚焦时预热拼贴封面，滚动进入的瓦片边走边加载。
                    coverIds.take(FOLDER_COVER_MOSAIC_LIMIT).forEach { coverId ->
                        container.artworkBitmapCache.prefetch(coverId, CoverVariant.Compact)
                    }
                }
            },
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.025f),
        colors = lockupButtonColors(),
        border = lockupButtonBorder(shape),
        contentPadding = PaddingValues(7.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (coverIds.isEmpty()) {
                FolderTileArtwork(
                    title = title,
                    modifier = Modifier.size(73.dp),
                    shape = artworkShape,
                )
            } else {
                FolderCoverMosaic(
                    coverIds = coverIds,
                    accent = folderAccentColor(title),
                    modifier = Modifier.size(73.dp),
                    shape = artworkShape,
                )
            }
            Spacer(Modifier.width(9.dp))
            // 文件夹名与路径副标题是列表的主要信息；字号与歌手/专辑瓦片统一为 18/13sp。
            LockupLabels(title, subtitle, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AllPlaylists(container: AuthenticatedAppDependencies, onOpen: (Playlist) -> Unit) {
    val retainedStore = LocalLibraryRetainedState.current
    val playlistState = retainedStore.list<Playlist>("playlists")
    val playlists = playlistState.snapshot.entries
    LaunchedEffect(Unit) {
        retainedStore.loadListOnce(playlistState, container.musicRepository::playlists)
    }
    GridPage("全部歌单", playlists, { it.guid.value }, minTileWidth = 193.dp) { playlist, modifier ->
        PlaylistTile(
            playlist.name,
            "歌单",
            playlist.coverId,
            FnColors.Coral,
            modifier = modifier,
            fillWidth = true,
        ) { onOpen(playlist) }
    }
}

/**
 * 文件夹浏览根页：列出已加入音乐库的共享库（文件夹）。官方 APK 的 folderView 基于音乐库
 * 文件夹逐层下钻；回声台从 shared-library/list 的根路径开始。
 */
@Composable
private fun FolderBrowseRoot(
    container: AuthenticatedAppDependencies,
    onOpenLibrary: (String) -> Unit,
) {
    val retainedStore = LocalLibraryRetainedState.current
    val libraryState = retainedStore.list<SharedLibrary>("shared-libraries:folders")
    val libraries = libraryState.snapshot.entries.filter { it.path != null && it.accessStatus == 0 }
    LaunchedEffect(Unit) {
        retainedStore.loadListOnce(libraryState, container.musicRepository::sharedLibraries)
    }
    if (libraries.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 64.dp).padding(top = 24.dp, bottom = 44.dp)) {
            Text("文件夹", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            when {
                libraryState.snapshot.error != null -> Text("读取音乐库文件夹失败", color = FnColors.Muted, fontSize = 13.sp)
                !libraryState.snapshot.initialLoadCompleted -> {}
                else -> Text(
                    "没有可浏览的音乐库文件夹（需先在飞牛音乐里加入音乐库）",
                    color = FnColors.Muted,
                    fontSize = 13.sp,
                )
            }
        }
        return
    }
    GridPage(
        "文件夹",
        libraries,
        { it.guid.value },
        minContentScale = FOLDER_GRID_CONTENT_SCALE,
    ) { library, modifier ->
        val path = library.path.orEmpty()
        MyLibraryLockup(
            library.name.ifBlank { directoryDisplayName(path) },
            path,
            modifier = modifier,
            enabled = true,
            fillWidth = true,
        ) { onOpenLibrary(path) }
    }
    // 进入文件夹浏览即后台预热整库扫描：下钻任意目录时无需等待首次扫描。
    LaunchedEffect(Unit) {
        retainedStore.scope.launch {
            runCatching { container.musicRepository.scanAllTracks() }
        }
    }
}

private enum class FolderDetailSection { Songs, Folders }

/**
 * 某个目录下的浏览页。与官方 APK folderView 一致：目录树完全由歌曲文件路径在客户端
 * 构建（服务端无文件夹接口）；歌曲与队列都来自 `folderTracks(directory, …)` 客户端投影。
 */
@Composable
private fun FolderDetailScreen(
    container: AuthenticatedAppDependencies,
    directory: String,
    onBack: () -> Unit,
    onSubFolder: (String) -> Unit,
    onPlayer: (Track) -> Unit,
) {
    val retainedStore = LocalLibraryRetainedState.current
    val scanState = retainedStore.list<Track>(FOLDER_SCAN_STATE_KEY)
    LaunchedEffect(Unit) {
        retainedStore.loadListOnce(scanState) { container.musicRepository.scanAllTracks() }
    }
    // 整库扫描列表重组时会变化；目录投影（含封面采样）按扫描内容缓存，避免每次重组重算。
    val scanEntries = scanState.snapshot.entries
    val directoryView = remember(scanEntries, directory) {
        resolveTrackDirectoryView(scanEntries, directory)
    }
    // 页头展示当前目录自身子树的封面拼贴（无封面时回退彩色文件夹插画）。
    val headerCoverIds = remember(scanEntries, directory) { directoryCoverSamples(scanEntries, directory) }
    // 标签未被用户手动选择前按数据自动决定默认：有歌曲默认“歌曲”，没有则默认“文件夹”。
    var manualSection by rememberSaveable(directory) { mutableStateOf<FolderDetailSection?>(null) }
    var focusedAreaOverride by rememberSaveable(directory) { mutableStateOf<String?>(null) }
    val hasSongs = directoryView.tracks.isNotEmpty()
    val hasDirectories = directoryView.directories.isNotEmpty()
    val selectedSection = manualSection
        ?: if (hasSongs) FolderDetailSection.Songs else FolderDetailSection.Folders
    val focusedArea = focusedAreaOverride
        ?: if (selectedSection == FolderDetailSection.Songs) "songs" else "folders"
    // “歌曲/文件夹”筛选与“播放全部”操作始终展示，即使当前目录为空也可切换查看。
    val tabs = buildList {
        add(
            TrackDetailTab(
                key = "folder-tab:songs",
                label = "歌曲",
                selected = selectedSection == FolderDetailSection.Songs,
                onFocus = { focusedAreaOverride = "tabs" },
                onSelect = {
                    manualSection = FolderDetailSection.Songs
                    focusedAreaOverride = "tabs"
                },
            ),
        )
        add(
            TrackDetailTab(
                key = "folder-tab:folders",
                label = "文件夹",
                selected = selectedSection == FolderDetailSection.Folders,
                onFocus = { focusedAreaOverride = "tabs" },
                onSelect = {
                    manualSection = FolderDetailSection.Folders
                    focusedAreaOverride = "tabs"
                },
            ),
        )
    }
    // 页头信息行：子文件夹数量（有则显示）+ 目录完整路径；歌曲数量由歌曲列表加载结果补充。
    val headerMetadata = buildList {
        if (hasDirectories) add("${directoryView.directories.size} 个子文件夹")
        add(directory)
    }.joinToString("  ·  ")
    // 整库扫描未完成时，歌曲列表的加载器可能先于扫描返回空结果：
    // 以扫描状态门控加载文案，并在扫描完成后触发一次重载，保证最终一致。
    val scanLoading = scanState.loading || !scanState.snapshot.initialLoadCompleted
    TrackCollection(
        container = container,
        stateKey = "folder:${directory}:tracks",
        title = directoryDisplayName(directory),
        coverId = null,
        loader = { page -> container.musicRepository.folderTracks(directory, page) },
        queueSource = { sort -> QueueSource.Folder(directory, sort) },
        onPlayer = onPlayer,
        initialFocusEnabled = selectedSection == FolderDetailSection.Songs && focusedArea == "songs",
        onFocusOwnerChanged = { focusedAreaOverride = "songs" },
        externalLoading = scanLoading,
        contentRevision = if (scanLoading) 0L else 1L,
        detailHeader = TrackDetailHeader(
            kind = "文件夹",
            extraMetadata = headerMetadata,
            headerArtwork = { modifier ->
                val name = directoryDisplayName(directory)
                if (headerCoverIds.isEmpty()) {
                    FolderTileArtwork(
                        title = name,
                        modifier = modifier,
                        shape = RoundedCornerShape(6.dp),
                    )
                } else {
                    FolderCoverMosaic(
                        coverIds = headerCoverIds,
                        accent = folderAccentColor(name),
                        modifier = modifier,
                        shape = RoundedCornerShape(6.dp),
                    )
                }
            },
            tabs = tabs,
            onBack = onBack,
        ),
        showTrackList = selectedSection == FolderDetailSection.Songs,
        alternateContent = {
            FolderDirectoryGrid(
                directory = directory,
                directories = directoryView.directories,
                loading = scanState.loading || !scanState.snapshot.initialLoadCompleted,
                initialFocusEnabled = selectedSection == FolderDetailSection.Folders && focusedArea == "folders",
                onFocused = { focusedAreaOverride = "folders" },
                onDirectory = onSubFolder,
            )
        },
        emptyMessage = "该目录中没有歌曲",
    )
}

@Composable
private fun FolderDirectoryGrid(
    directory: String,
    directories: List<TrackDirectory>,
    loading: Boolean,
    initialFocusEnabled: Boolean,
    onFocused: () -> Unit,
    onDirectory: (String) -> Unit,
) {
    var focusedKey by rememberSaveable(directory) { mutableStateOf<String?>(null) }
    var initialFocusRequested by remember(directory) { mutableStateOf(false) }
    val restoredFocus = remember(directory) { FocusRequester() }
    val gridState = rememberLazyGridState()
    LaunchedEffect(directories, initialFocusEnabled, focusedKey) {
        if (!initialFocusEnabled || directories.isEmpty() || initialFocusRequested) return@LaunchedEffect
        val availableKeys = directories.map { it.path }
        val targetKey = focusedKey?.takeIf(availableKeys::contains) ?: availableKeys.first()
        if (focusedKey != targetKey) {
            focusedKey = targetKey
            return@LaunchedEffect
        }
        repeat(3) {
            withFrameNanos { }
            if (runCatching { restoredFocus.requestFocus() }.getOrDefault(false)) {
                initialFocusRequested = true
                return@LaunchedEffect
            }
        }
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(top = 12.dp),
    ) {
        // 子目录列数按可用宽度自适应，宽屏多列、窄屏少列；内容至少放大 1.3 倍保证可读。
        val spec = adaptiveGridSpec(
            availableWidth = maxWidth,
            availableHeight = maxHeight,
            minTileWidth = 170.dp,
            tileHeight = 95.dp,
            spacing = 14.dp,
            minContentScale = FOLDER_GRID_CONTENT_SCALE,
        )
        AdaptiveGridContentScale(spec) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(spec.columns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (directories.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            // 整库扫描未完成时目录树尚不可用，显示加载中而不是空态。
                            Text(
                                if (loading) "正在加载子文件夹" else "该目录下没有子文件夹",
                                color = FnColors.Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
                items(directories, key = { it.path }) { directory ->
                    val entryKey = directory.path
                    val subtitle = if (directory.directTrackCount == 0) {
                        // 中间层目录：直接歌曲为 0，显示子树总数，避免“0 首歌曲”的误导。
                        "含子目录 ${directory.trackCount} 首"
                    } else {
                        "${directory.trackCount} 首歌曲"
                    }
                    MyLibraryLockup(
                        title = directoryDisplayName(directory.path),
                        subtitle = subtitle,
                        fillWidth = true,
                        coverIds = directory.sampleCoverIds,
                        modifier = Modifier
                            .then(if (focusedKey == entryKey) Modifier.focusRequester(restoredFocus) else Modifier)
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    focusedKey = entryKey
                                    onFocused()
                                }
                            },
                    ) { onDirectory(directory.path) }
                }
            }
        }
    }
}

@Composable
private fun ArtistGrid(container: AuthenticatedAppDependencies, onOpen: (Artist) -> Unit) {
    CatalogListPage(
        stateKey = "artists",
        title = "全部歌手",
        totalLabel = { "$it 位歌手" },
        loader = { page -> container.musicRepository.artists(page, CATALOG_LIST_PAGE_SIZE) },
        key = { it.guid.value },
    ) { artist, modifier ->
        ArtistLockup(
            artist.name,
            "${artist.trackCount ?: 0} 首歌曲",
            artist.coverId,
            modifier = modifier,
            fillWidth = true,
        ) { onOpen(artist) }
    }
}

@Composable
private fun AlbumGrid(container: AuthenticatedAppDependencies, onOpen: (Album) -> Unit) {
    CatalogListPage(
        stateKey = "albums",
        title = "全部专辑",
        totalLabel = { "$it 张专辑" },
        loader = { page -> container.musicRepository.albums(page, CATALOG_LIST_PAGE_SIZE) },
        key = { it.guid.value },
        // 同名同歌手的专辑（如自动归档的“纯音乐”）合并为一个条目。
        mergeKey = { it.name to (it.artistName ?: "") },
    ) { album, modifier ->
        AlbumLockup(
            album.name,
            album.artistName.orEmpty(),
            album.coverId,
            modifier = modifier,
            fillWidth = true,
        ) { onOpen(album) }
    }
}

@Composable
private fun GenreGrid(container: AuthenticatedAppDependencies, onOpen: (Genre) -> Unit) {
    CatalogListPage(
        stateKey = "genres",
        title = "全部风格",
        totalLabel = { "$it 种风格" },
        loader = { page -> container.musicRepository.genres(page, CATALOG_LIST_PAGE_SIZE) },
        key = { it.guid.value },
    ) { genre, modifier ->
        GenreTile(
            genre.name,
            "${genre.trackCount ?: 0} 首歌曲",
            modifier = modifier,
            fillWidth = true,
        ) { onOpen(genre) }
    }
}

/**
 * 大分辨率下按网格规格放大列表内容：用放大后的 Density 覆盖局部组合，
 * 让网格内所有 dp 尺寸与 sp 文字同步缩放（最多 [ADAPTIVE_CONTENT_SCALE_MAX] = 1.5 倍）。
 */
@Composable
internal fun AdaptiveGridContentScale(
    spec: AdaptiveGridSpec,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    if (spec.contentScale <= 1f) {
        content()
    } else {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = density.density * spec.contentScale,
                fontScale = density.fontScale,
            ),
            content = content,
        )
    }
}

@Composable
internal fun <T> CatalogListPage(
    stateKey: String,
    title: String,
    totalLabel: (Int) -> String,
    loader: suspend (page: Int) -> Page<T>,
    key: (T) -> String,
    /** 可选的合并键：名称相同（如同名“纯音乐”专辑）的条目只保留最先加载的一个。 */
    mergeKey: ((T) -> Any)? = null,
    item: @Composable (T, Modifier) -> Unit,
) {
    val retainedStore = LocalLibraryRetainedState.current
    val retained = retainedStore.paged<T>("grid:$stateKey")
    val snapshot = retained.snapshot
    val entries = snapshot.entries
    val displayEntries = if (mergeKey != null) entries.distinctBy(mergeKey) else entries
    // 滚动列表：全部已加载条目纵向滚动，按焦点位置自动预取下一页，遥控器无需翻页。
    var focusedKey by rememberSaveable(stateKey) { mutableStateOf<String?>(null) }
    var initialFocusRequested by remember(stateKey) { mutableStateOf(false) }
    val restoredFocus = remember(stateKey) { FocusRequester() }
    val retryFocus = remember(stateKey) { FocusRequester() }
    val gridState = rememberLazyGridState()

    fun load(page: Int) {
        if (retained.loading || page > 1 && !snapshot.hasNext) return
        retained.loading = true
        retainedStore.scope.launch {
            runCatching { loader(page) }
                .onSuccess { retained.snapshot = retainLoadedPage(retained.snapshot, it, key) }
                .onFailure {
                    retained.snapshot = retained.snapshot.copy(
                        error = (it as? AppException)?.error ?: AppError.Unknown(),
                        initialLoadCompleted = retained.snapshot.initialLoadCompleted || page == 1,
                    )
                }
            retained.loading = false
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontalPadding = 64.dp
        // 列数按可用宽度自适应：宽屏多列、窄屏少列。
        val spec = adaptiveGridSpec(
            availableWidth = maxWidth - horizontalPadding * 2,
            availableHeight = maxHeight,
            minTileWidth = 170.dp,
            tileHeight = CATALOG_TILE_HEIGHT,
            spacing = 14.dp,
        )
        LaunchedEffect(stateKey) {
            if (shouldLoadInitialPage(retained.snapshot)) {
                load(1)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 44.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                snapshot.total?.let { total ->
                    Spacer(Modifier.width(14.dp))
                    Text(totalLabel(total), color = FnColors.Muted, fontSize = 12.sp)
                }
                snapshot.error?.let { error ->
                    Spacer(Modifier.width(18.dp))
                    Text(appErrorMessage(error), color = FnColors.Coral, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            AdaptiveGridContentScale(spec) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(spec.columns),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(displayEntries, key = key) { entry ->
                        val entryKey = key(entry)
                        item(
                            entry,
                            Modifier
                                .then(
                                    if (focusedKey == entryKey) {
                                        Modifier.focusRequester(restoredFocus)
                                    } else {
                                        Modifier
                                    },
                                )
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        focusedKey = entryKey
                                        // 以原始列表判断接近程度：合并可能隐藏部分条目。
                                        val index = entries.indexOfFirst { key(it) == entryKey }
                                        // 滚动到已加载内容的一半就开始预取下一页。
                                        if (
                                            snapshot.hasNext &&
                                            !retained.loading &&
                                            index >= entries.size / 2
                                        ) {
                                            load(snapshot.page + 1)
                                        }
                                    }
                                },
                        )
                    }

                    if (displayEntries.isEmpty() && snapshot.error != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CatalogRetryButton(
                                loading = retained.loading,
                                modifier = Modifier.focusRequester(retryFocus),
                            ) { load(1) }
                        }
                    }

                    // 冷启动首次加载时的提示，避免标题下整片空白。
                    if (
                        displayEntries.isEmpty() &&
                        snapshot.error == null &&
                        (retained.loading || !snapshot.initialLoadCompleted)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Text("正在加载", color = FnColors.Muted, fontSize = 15.sp)
                            }
                        }
                    }

                    // 全部加载完成后的“到底”标示；加载中不出现在正常滚动的视线里。
                    if (
                        snapshot.initialLoadCompleted &&
                        !snapshot.hasNext &&
                        displayEntries.isNotEmpty()
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 14.dp)
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.08f)),
                                )
                                Text("已经到底了", color = FnColors.Muted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(snapshot.initialLoadCompleted, displayEntries, focusedKey) {
        if (!snapshot.initialLoadCompleted || displayEntries.isEmpty() || initialFocusRequested) return@LaunchedEffect
        focusedKey = focusedKey?.takeIf { candidate -> displayEntries.any { key(it) == candidate } }
            ?: displayEntries.firstOrNull()?.let(key)
            ?: return@LaunchedEffect
        yield()
        runCatching { restoredFocus.requestFocus() }
        initialFocusRequested = true
    }

    LaunchedEffect(snapshot.initialLoadCompleted, entries, snapshot.error) {
        if (
            !snapshot.initialLoadCompleted ||
            entries.isNotEmpty() ||
            snapshot.error == null ||
            initialFocusRequested
        ) return@LaunchedEffect
        yield()
        runCatching { retryFocus.requestFocus() }
        initialFocusRequested = true
    }
}

@Composable
private fun <T> GridPage(
    title: String,
    entries: List<T>,
    key: (T) -> String,
    minTileWidth: Dp = 170.dp,
    minContentScale: Float = 1f,
    item: @Composable (T, Modifier) -> Unit,
) {
    var focusedKey by rememberSaveable(title) { mutableStateOf<String?>(null) }
    var initialFocusRequested by remember(title) { mutableStateOf(false) }
    val contentFocus = remember(title) { FocusRequester() }
    val gridState = rememberLazyGridState()
    LaunchedEffect(entries, focusedKey) {
        if (entries.isEmpty() || initialFocusRequested) return@LaunchedEffect
        val keys = entries.map(key)
        focusedKey = focusedKey?.takeIf(keys::contains) ?: keys.first()
        yield()
        runCatching { contentFocus.requestFocus() }
        initialFocusRequested = true
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // 列数按可用宽度自适应，不同分辨率下都能铺满屏幕而不是固定几列。
        val spec = adaptiveGridSpec(
            availableWidth = maxWidth - 128.dp,
            availableHeight = maxHeight - 68.dp,
            minTileWidth = minTileWidth,
            tileHeight = 142.dp,
            spacing = 14.dp,
            minContentScale = minContentScale,
        )
        // 紧凑标题：大号标题+大间距会把列表内容挤下去。
        Column(Modifier.fillMaxSize().padding(horizontal = 64.dp).padding(top = 24.dp, bottom = 44.dp)) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            AdaptiveGridContentScale(spec) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(spec.columns),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(entries, key = key) { entry ->
                        val entryKey = key(entry)
                        item(
                            entry,
                            Modifier.then(if (focusedKey == entryKey) Modifier.focusRequester(contentFocus) else Modifier)
                                .onFocusChanged { if (it.isFocused) focusedKey = entryKey },
                        )
                    }
                }
            }
        }
    }
}

private enum class ArtistDetailSection { Songs, Albums }

internal data class TrackDetailTab(
    val key: String,
    val label: String,
    val selected: Boolean,
    val onFocus: () -> Unit,
    val onSelect: () -> Unit,
)

internal data class TrackDetailHeader(
    val kind: String,
    val declaredTrackCount: Int? = null,
    val extraMetadata: String? = null,
    val artworkFallback: CollectionArtworkFallback = CollectionArtworkFallback.Initial,
    val tabs: List<TrackDetailTab> = emptyList(),
    /**
     * 页头大图自定义插槽（可选）：接收当前图位尺寸修饰符，绘制整块页头视觉。
     * 未提供时按 [artworkFallback] 渲染默认占位图。
     */
    val headerArtwork: (@Composable (Modifier) -> Unit)? = null,
    val onBack: () -> Unit,
)

internal enum class CollectionArtworkFallback { Initial, Artist, Favorites, Collection }
internal sealed interface TrackCollectionPrimaryAction {
    data object PlayAll : TrackCollectionPrimaryAction
    data class StartRoam(val onStarted: () -> Unit) : TrackCollectionPrimaryAction
}

@Composable
private fun ArtistDetail(
    container: AuthenticatedAppDependencies,
    artist: Artist,
    onBack: () -> Unit,
    onAlbum: (Album) -> Unit,
    onPlayer: (Track) -> Unit,
) {
    val stateKey = "artist:${artist.guid.value}"
    val retainedStore = LocalLibraryRetainedState.current
    val albumState = retainedStore.paged<Album>("$stateKey:albums")
    val albumSnapshot = albumState.snapshot
    val albums = albumSnapshot.entries
    var selectedSection by rememberSaveable(stateKey) { mutableStateOf(ArtistDetailSection.Songs) }
    var focusedArea by rememberSaveable(stateKey) { mutableStateOf("songs") }
    fun loadAlbums(target: Int) {
        if (albumState.loading || target > 1 && !albumState.snapshot.hasNext) return
        albumState.loading = true
        retainedStore.scope.launch {
            runCatching { container.musicRepository.artistAlbums(artist.guid.value, target) }
                .onSuccess {
                    albumState.snapshot = retainLoadedPage(albumState.snapshot, it) { album -> album.guid.value }
                }
                .onFailure {
                    albumState.snapshot = albumState.snapshot.copy(
                        error = (it as? AppException)?.error ?: AppError.Unknown(),
                        initialLoadCompleted = albumState.snapshot.initialLoadCompleted || target == 1,
                    )
                }
            albumState.loading = false
        }
    }
    LaunchedEffect(stateKey) {
        if (shouldLoadInitialPage(albumState.snapshot)) loadAlbums(1)
    }
    val tabs = ArtistDetailSection.entries.map { section ->
        TrackDetailTab(
            key = "artist-tab:${section.name}",
            label = if (section == ArtistDetailSection.Songs) "歌曲" else "专辑",
            selected = selectedSection == section,
            onFocus = { focusedArea = "tabs" },
            onSelect = {
                selectedSection = section
                focusedArea = "tabs"
            },
        )
    }
    TrackCollection(
        container = container,
        stateKey = "$stateKey:tracks",
        title = artist.name,
        coverId = artist.coverId,
        loader = { container.musicRepository.artistTracks(artist.guid.value, it) },
        queueSource = { sort -> QueueSource.Artist(artist.guid.value, sort) },
        onPlayer = onPlayer,
        initialFocusEnabled = selectedSection == ArtistDetailSection.Songs && focusedArea == "songs",
        onFocusOwnerChanged = { focusedArea = "songs" },
        detailHeader = TrackDetailHeader(
            kind = "歌手",
            declaredTrackCount = artist.trackCount,
            extraMetadata = artist.albumCount?.let { "$it 张专辑" },
            artworkFallback = CollectionArtworkFallback.Artist,
            tabs = tabs,
            onBack = onBack,
        ),
        showTrackList = selectedSection == ArtistDetailSection.Songs,
        alternateContent = {
            ArtistAlbumGrid(
                stateKey = "$stateKey:album-grid",
                snapshot = albumSnapshot,
                loading = albumState.loading,
                initialFocusEnabled = selectedSection == ArtistDetailSection.Albums && focusedArea == "albums",
                onLoad = ::loadAlbums,
                onFocused = { focusedArea = "albums" },
                onAlbum = onAlbum,
            )
        },
    )
}

@Composable
private fun ArtistAlbumGrid(
    stateKey: String,
    snapshot: RetainedPageSnapshot<Album>,
    loading: Boolean,
    initialFocusEnabled: Boolean,
    onLoad: (Int) -> Unit,
    onFocused: () -> Unit,
    onAlbum: (Album) -> Unit,
) {
    val albums = snapshot.entries
    var focusedKey by rememberSaveable(stateKey) { mutableStateOf<String?>(null) }
    var initialFocusRequested by remember(stateKey) { mutableStateOf(false) }
    val restoredFocus = remember(stateKey) { FocusRequester() }
    val gridState = rememberLazyGridState()
    LaunchedEffect(snapshot.initialLoadCompleted, albums, initialFocusEnabled, focusedKey) {
        if (!initialFocusEnabled || !snapshot.initialLoadCompleted || albums.isEmpty() || initialFocusRequested) return@LaunchedEffect
        val availableKeys = albums.map { it.guid.value }
        val targetKey = focusedKey?.takeIf(availableKeys::contains) ?: availableKeys.first()
        if (focusedKey != targetKey) {
            focusedKey = targetKey
            return@LaunchedEffect
        }
        repeat(3) {
            withFrameNanos { }
            if (runCatching { restoredFocus.requestFocus() }.getOrDefault(false)) {
                initialFocusRequested = true
                return@LaunchedEffect
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().padding(top = 12.dp)) {
        // 专辑卡片列数按可用宽度自适应，不同分辨率下铺满屏幕。
        val spec = adaptiveGridSpec(
            availableWidth = maxWidth,
            availableHeight = maxHeight,
            minTileWidth = 220.dp,
            tileHeight = 106.dp,
            spacing = 12.dp,
        )
        AdaptiveGridContentScale(spec) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(spec.columns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (albums.isEmpty() && loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("正在加载专辑", color = FnColors.Muted, fontSize = 16.sp, modifier = Modifier.padding(vertical = 20.dp))
                    }
                }
                items(albums, key = { it.guid.value }) { album ->
                    val index = albums.indexOf(album)
                    DetailAlbumCard(
                        album = album,
                        modifier = Modifier
                            .then(if (focusedKey == album.guid.value) Modifier.focusRequester(restoredFocus) else Modifier)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    focusedKey = album.guid.value
                                    onFocused()
                                    if (snapshot.hasNext && index >= albums.size - 6) onLoad(snapshot.page + 1)
                                }
                            },
                        onClick = {
                            focusedKey = album.guid.value
                            onAlbum(album)
                        },
                    )
                }
                if (albums.isEmpty() && !loading && snapshot.error != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Button(onClick = { onLoad(1) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Text("专辑加载失败，重试")
                        }
                    }
                }
                if (snapshot.hasNext) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Button(
                            enabled = !loading,
                            onClick = { onLoad(snapshot.page + 1) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Text(if (loading) "正在加载" else "加载更多专辑")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailAlbumCard(album: Album, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val container = LocalAuthenticatedDependencies.current
    val albumCoverId = album.coverId
    val shape = RoundedCornerShape(6.dp)
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(106.dp)
            .onFocusChanged { state ->
                if (state.isFocused && albumCoverId != null) {
                    container.artworkBitmapCache.prefetch(albumCoverId, CoverVariant.Grid)
                }
            },
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.025f),
        colors = ButtonDefaults.colors(
            containerColor = FnColors.Surface,
            contentColor = FnColors.Text,
            focusedContainerColor = FnColors.FocusFill,
            focusedContentColor = FnColors.Text,
            pressedContainerColor = FnColors.FocusFill,
            pressedContentColor = FnColors.Text,
        ),
        contentPadding = PaddingValues(9.dp),
    ) {
        val contentColor = LocalContentColor.current
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            val artworkShape = RoundedCornerShape(4.dp)
            if (albumCoverId != null) {
                RemoteArtwork(
                    container = container,
                    coverId = albumCoverId,
                    variant = CoverVariant.Compact,
                    modifier = Modifier.size(88.dp),
                    shape = artworkShape,
                    contentScale = ContentScale.Crop,
                    placeholderContent = {
                        InitialArtworkPlaceholder(album.name, FnColors.Coral, Modifier.fillMaxSize(), artworkShape)
                    },
                )
            } else {
                InitialArtworkPlaceholder(album.name, FnColors.Coral, Modifier.size(88.dp), artworkShape)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    album.name,
                    color = contentColor,
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val metadata = listOfNotNull(
                    album.releaseDate?.takeIf(String::isNotBlank),
                    album.trackCount?.let { "$it 首" },
                ).joinToString(" · ")
                if (metadata.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        metadata,
                        color = contentColor.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun TrackCollection(
    container: AuthenticatedAppDependencies,
    stateKey: String,
    title: String,
    subtitle: String = "",
    coverId: String? = null,
    loader: suspend (Int) -> Page<Track>,
    queueSource: (String) -> QueueSource,
    onPlayer: (Track) -> Unit,
    initialFocusEnabled: Boolean = true,
    onFocusOwnerChanged: () -> Unit = {},
    detailHeader: TrackDetailHeader,
    primaryAction: TrackCollectionPrimaryAction = TrackCollectionPrimaryAction.PlayAll,
    showTrackList: Boolean = true,
    alternateContent: @Composable () -> Unit = {},
    contentRevision: Long = 0L,
    /** 上游数据源（如文件夹的整库扫描）仍在加载时置真：加载中文案优先于空态文案。 */
    externalLoading: Boolean = false,
    emptyMessage: String = "暂无歌曲",
) {
    val retainedStore = LocalLibraryRetainedState.current
    val retained = retainedStore.tracks(stateKey)
    val snapshot = retained.snapshot
    val tracks = snapshot.tracks
    val loadedPages = snapshot.loadedPages
    val page = snapshot.page
    val hasNext = snapshot.hasNext
    val loading = retained.loading || externalLoading
    val error = snapshot.error
    val expectedTotal = snapshot.expectedTotal
    val expectedSort = snapshot.expectedSort
    var focusedKey by rememberSaveable(stateKey) { mutableStateOf<String?>(null) }
    var initialFocusRequested by remember(stateKey) { mutableStateOf(false) }
    val restoredFocus = remember(stateKey) { FocusRequester() }
    val listState = rememberLazyListState()
    val actionScope = rememberCoroutineScope()
    var primaryActionRunning by remember(stateKey) { mutableStateOf(false) }
    var loadedContentRevision by rememberSaveable(stateKey) { mutableStateOf<Long?>(null) }
    fun setError(value: AppError?) {
        retained.snapshot = retained.snapshot.copy(error = value)
    }
    fun load(target: Int) {
        if (retained.loading) return
        retained.loading = true
        retainedStore.scope.launch {
            runCatching { loader(target) }
                .onSuccess {
                    retained.snapshot = retainTrackCollectionPage(retained.snapshot, it, target)
                }
                .onFailure {
                    retained.snapshot = retained.snapshot.copy(
                        error = (it as? AppException)?.error ?: AppError.Unknown(),
                        initialLoadCompleted = retained.snapshot.initialLoadCompleted || target == 1,
                    )
                }
            retained.loading = false
        }
    }
    fun play(index: Int) {
        val target = tracks.getOrNull(index)?.takeIf(::isTrackPlayable) ?: return
        val window = exactTrackQueueWindow(loadedPages, index) ?: run {
            setError(AppError.CollectionChanged)
            return
        }
        val queue = container.musicRepository.prepareQueue(window.items)
        val queueIds = queue.map { it.track.guid.value }
        val segmentIds = window.segments.flatMap(QueuePageSegment::mediaIds)
        val queueIndex = queue.indexOfFirst { it.track.guid == target.guid }
        if (queueIds != segmentIds || queueIndex < 0) {
            setError(AppError.TranscodeUnavailable)
            return
        }
        if (queue.isEmpty()) {
            setError(AppError.TranscodeUnavailable)
            return
        }
        val sort = expectedSort ?: run {
            setError(AppError.CollectionChanged)
            return
        }
        actionScope.launch {
            val transition = runCatching {
                container.playbackController.playQueue(
                    tracks = queue,
                    startIndex = queueIndex,
                    source = queueSource(sort),
                    windowStart = window.segments.first().sourceStartIndex,
                    firstPage = window.segments.first().page,
                    lastPage = window.segments.last().page,
                    knownTotal = expectedTotal,
                    segments = window.segments,
                )
            }.getOrElse {
                setError(AppError.Unknown(it.message))
                return@launch
            }
            if (transition == null) {
                setError(AppError.TranscodeUnavailable)
                return@launch
            }
            runCatching { transition.awaitCommitted() }
                .onSuccess {
                    setError(null)
                    onPlayer(target)
                }
                .onFailure { setError(AppError.Unknown(it.message)) }
        }
    }
    fun runPrimaryAction() {
        when (val action = primaryAction) {
            TrackCollectionPrimaryAction.PlayAll -> play(tracks.indexOfFirst(::isTrackPlayable))
            is TrackCollectionPrimaryAction.StartRoam -> {
                if (primaryActionRunning) return
                if (container.playbackController.state.value.queueKind == QueueKind.Roam) {
                    action.onStarted()
                    return
                }
                primaryActionRunning = true
                actionScope.launch {
                    if (container.playbackController.startRoam()) {
                        setError(null)
                        action.onStarted()
                    } else {
                        setError(container.playbackController.state.value.roamError ?: AppError.Unknown())
                    }
                    primaryActionRunning = false
                }
            }
        }
    }
    LaunchedEffect(stateKey, contentRevision) {
        if (loadedContentRevision != null && loadedContentRevision != contentRevision) {
            retained.snapshot = RetainedTrackCollectionSnapshot()
            initialFocusRequested = false
        }
        loadedContentRevision = contentRevision
        if (!retained.snapshot.initialLoadCompleted) {
            load(1)
        }
    }
    val playableTracks = tracks.filter(::isTrackPlayable)
    val primaryActionEnabled = when (primaryAction) {
        TrackCollectionPrimaryAction.PlayAll -> playableTracks.isNotEmpty()
        is TrackCollectionPrimaryAction.StartRoam -> true
    }
    val primaryActionLabel = when (primaryAction) {
        TrackCollectionPrimaryAction.PlayAll -> "播放全部"
        is TrackCollectionPrimaryAction.StartRoam -> "随机漫游"
    }
    LaunchedEffect(snapshot.initialLoadCompleted, tracks, focusedKey, initialFocusEnabled, showTrackList, primaryActionEnabled) {
        if (!initialFocusEnabled || !snapshot.initialLoadCompleted || initialFocusRequested) return@LaunchedEffect
        val availableKeys = buildList {
            if (primaryActionEnabled) add("primary-action")
            detailHeader.tabs.filter { it.selected }.forEach { add(it.key) }
            if (showTrackList) addAll(playableTracks.map { it.guid.value })
            add("detail-back")
        }
        val targetKey = focusedKey?.takeIf(availableKeys::contains) ?: availableKeys.firstOrNull()
        if (focusedKey != targetKey) {
            focusedKey = targetKey
            return@LaunchedEffect
        }
        if (targetKey != null) {
            repeat(3) {
                withFrameNanos { }
                if (runCatching { restoredFocus.requestFocus() }.getOrDefault(false)) {
                    initialFocusRequested = true
                    return@LaunchedEffect
                }
            }
        }
    }
    DetailTrackCollection(
        container = container,
        header = detailHeader,
        title = title,
        subtitle = subtitle,
        coverId = coverId,
        trackCount = detailHeader.declaredTrackCount ?: expectedTotal?.takeIf { it > 0 },
        tracks = tracks,
        loading = loading,
        error = error,
        hasNext = hasNext,
        listState = listState,
        focusedKey = focusedKey,
        restoredFocus = restoredFocus,
        primaryActionLabel = primaryActionLabel,
        primaryActionEnabled = primaryActionEnabled,
        showTrackList = showTrackList,
        onFocusKey = {
            focusedKey = it
            onFocusOwnerChanged()
        },
        onPrimaryAction = ::runPrimaryAction,
        onTrackFocused = { index, key ->
            focusedKey = key
            onFocusOwnerChanged()
            if (hasNext && index >= tracks.size - 15) load(page + 1)
        },
        onTrack = ::play,
        onLoadMore = { load(page + 1) },
        alternateContent = alternateContent,
        emptyMessage = emptyMessage,
    )
}

@Composable
private fun DetailTrackCollection(
    container: AuthenticatedAppDependencies,
    header: TrackDetailHeader,
    title: String,
    subtitle: String,
    coverId: String?,
    trackCount: Int?,
    tracks: List<Track>,
    loading: Boolean,
    error: AppError?,
    hasNext: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    focusedKey: String?,
    restoredFocus: FocusRequester,
    primaryActionLabel: String,
    primaryActionEnabled: Boolean,
    showTrackList: Boolean,
    onFocusKey: (String) -> Unit,
    onPrimaryAction: () -> Unit,
    onTrackFocused: (Int, String) -> Unit,
    onTrack: (Int) -> Unit,
    onLoadMore: () -> Unit,
    alternateContent: @Composable () -> Unit,
    emptyMessage: String,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 42.dp, vertical = 24.dp)) {
        // 行高随内容自适应（不低于图高），避免固定高度把页头撑得过高。
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.Top) {
            DetailBackButton(
                modifier = Modifier
                    .then(if (focusedKey == "detail-back") Modifier.focusRequester(restoredFocus) else Modifier)
                    .onFocusChanged { if (it.isFocused) onFocusKey("detail-back") },
                onClick = header.onBack,
            )
            Spacer(Modifier.width(18.dp))
            val artworkShape = RoundedCornerShape(6.dp)
            val headerArtwork = header.headerArtwork
            if (headerArtwork != null) {
                headerArtwork(Modifier.size(148.dp))
            } else {
                CollectionArtwork(
                    container = container,
                    title = title,
                    coverId = coverId,
                    fallback = header.artworkFallback,
                    modifier = Modifier.size(148.dp),
                    shape = artworkShape,
                )
            }
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (header.kind.isNotBlank() && header.kind != title) {
                    Text(header.kind, color = FnColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(7.dp))
                }
                Text(
                    title,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val metadata = buildList {
                    if (subtitle.isNotBlank()) add(subtitle)
                    trackCount?.let { add("$it 首歌曲") }
                    header.extraMetadata?.takeIf(String::isNotBlank)?.let(::add)
                }.joinToString("  ·  ")
                if (metadata.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(metadata, color = FnColors.Muted, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    header.tabs.forEach { tab ->
                        DetailTabButton(
                            label = tab.label,
                            selected = tab.selected,
                            modifier = Modifier
                                .then(if (focusedKey == tab.key) Modifier.focusRequester(restoredFocus) else Modifier)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        onFocusKey(tab.key)
                                        tab.onFocus()
                                    }
                                },
                            onClick = tab.onSelect,
                        )
                    }
                    val primaryShape = RoundedCornerShape(6.dp)
                    Button(
                        enabled = primaryActionEnabled,
                        onClick = onPrimaryAction,
                        modifier = Modifier
                            .then(if (focusedKey == "primary-action") Modifier.focusRequester(restoredFocus) else Modifier)
                            .onFocusChanged { if (it.isFocused) onFocusKey("primary-action") }
                            .height(44.dp),
                        shape = ButtonDefaults.shape(
                            primaryShape,
                            primaryShape,
                            primaryShape,
                            primaryShape,
                            primaryShape,
                        ),
                        scale = ButtonDefaults.scale(focusedScale = 1.035f),
                        colors = ButtonDefaults.colors(
                            containerColor = FnColors.Surface,
                            contentColor = FnColors.Text,
                            focusedContainerColor = FnColors.FocusFill,
                            focusedContentColor = FnColors.Text,
                            pressedContainerColor = FnColors.FocusFill,
                            pressedContentColor = FnColors.Text,
                        ),
                        border = ButtonDefaults.border(
                            border = Border(BorderStroke(1.dp, FnColors.Coral.copy(alpha = 0.76f)), shape = primaryShape),
                            focusedBorder = Border(BorderStroke(1.5.dp, FnColors.Coral), shape = primaryShape),
                            pressedBorder = Border(BorderStroke(1.5.dp, FnColors.Coral), shape = primaryShape),
                        ),
                        contentPadding = PaddingValues(horizontal = 19.dp, vertical = 0.dp),
                    ) {
                        Text("▶  $primaryActionLabel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
        if (showTrackList) {
            DetailTrackColumnHeader()
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.09f)))
            LazyColumn(Modifier.weight(1f), state = listState) {
                if (error != null) {
                    item { InlineError(error) }
                }
                if (tracks.isEmpty() && loading) {
                    item {
                        Text("正在加载中，请稍候", color = FnColors.Muted, fontSize = 16.sp, modifier = Modifier.padding(vertical = 20.dp))
                    }
                }
                if (tracks.isEmpty() && !loading && error == null) {
                    item {
                        Text(
                            emptyMessage,
                            color = FnColors.Muted,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                itemsIndexed(tracks, key = { _, track -> track.guid.value }) { index, track ->
                    DetailTrackRow(
                        index = index,
                        track = track,
                        enabled = isTrackPlayable(track),
                        modifier = Modifier
                            .then(if (focusedKey == track.guid.value) Modifier.focusRequester(restoredFocus) else Modifier)
                            .onFocusChanged { if (it.isFocused) onTrackFocused(index, track.guid.value) },
                        onClick = { onTrack(index) },
                    )
                }
                if (hasNext) {
                    item {
                        Button(
                            enabled = !loading,
                            onClick = onLoadMore,
                            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 5.dp),
                        ) {
                            Text(if (loading) "正在加载" else "加载更多")
                        }
                    }
                }
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) { alternateContent() }
        }
    }
}

@Composable
private fun CollectionArtwork(
    container: AuthenticatedAppDependencies,
    title: String,
    coverId: String?,
    fallback: CollectionArtworkFallback,
    modifier: Modifier,
    shape: Shape,
) {
    val resolvedShape = if (fallback == CollectionArtworkFallback.Artist) CircleShape else shape
    if (coverId != null) {
        RemoteArtwork(
            container = container,
            coverId = coverId,
            variant = CoverVariant.Grid,
            modifier = modifier,
            shape = resolvedShape,
            contentScale = ContentScale.Crop,
            placeholderContent = {
                CollectionArtworkFallbackContent(title, fallback, Modifier.fillMaxSize(), resolvedShape)
            },
        )
    } else {
        CollectionArtworkFallbackContent(title, fallback, modifier, resolvedShape)
    }
}

@Composable
private fun CollectionArtworkFallbackContent(
    title: String,
    fallback: CollectionArtworkFallback,
    modifier: Modifier,
    shape: Shape,
) {
    when (fallback) {
        CollectionArtworkFallback.Artist -> ArtistAvatarPlaceholder(
            title = title,
            modifier = modifier,
            fontSize = 58.sp,
        )
        CollectionArtworkFallback.Favorites -> Box(modifier.clip(shape)) {
            HomeFeatureArtwork(HomeArtworkKind.Favorites, Modifier.fillMaxSize())
        }
        CollectionArtworkFallback.Collection -> Box(modifier.clip(shape)) {
            HomeFeatureArtwork(HomeArtworkKind.Collection, Modifier.fillMaxSize())
        }
        CollectionArtworkFallback.Initial -> InitialArtworkPlaceholder(
            text = title,
            accent = FnColors.Coral,
            modifier = modifier,
            shape = shape,
        )
    }
}

@Composable
private fun DetailBackButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = CircleShape
    Button(
        onClick = onClick,
        modifier = modifier.size(46.dp).semantics { contentDescription = "返回" },
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.08f),
        colors = ButtonDefaults.colors(
            containerColor = FnColors.Surface,
            contentColor = FnColors.Text,
            focusedContainerColor = FnColors.FocusFill,
            focusedContentColor = FnColors.Text,
            pressedContainerColor = FnColors.FocusFill,
            pressedContentColor = FnColors.Text,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text("‹", fontSize = 36.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailTabButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(5.dp)
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp).semantics { this.selected = selected },
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.04f),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) FnColors.Coral.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (selected) FnColors.Coral else FnColors.Muted,
            focusedContainerColor = FnColors.FocusFill,
            focusedContentColor = FnColors.Text,
            pressedContainerColor = FnColors.FocusFill,
            pressedContentColor = FnColors.Text,
        ),
        contentPadding = PaddingValues(horizontal = 17.dp, vertical = 0.dp),
    ) {
        Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun DetailTrackColumnHeader() {
    Row(
        Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("#", color = FnColors.Muted, fontSize = 12.sp, modifier = Modifier.width(54.dp))
        Text("歌曲", color = FnColors.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("歌手", color = FnColors.Muted, fontSize = 12.sp, modifier = Modifier.width(220.dp))
        Text("时长", color = FnColors.Muted, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.width(70.dp))
    }
}

@Composable
internal fun DetailTrackRow(
    index: Int,
    track: Track,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(5.dp)
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .semantics {
                contentDescription = "${index + 1}. ${track.title} ${track.artistName.orEmpty()}"
            },
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = FnColors.Text,
            focusedContainerColor = FnColors.FocusFill,
            focusedContentColor = FnColors.Text,
            pressedContainerColor = FnColors.FocusFill,
            pressedContentColor = FnColors.Text,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = FnColors.Muted.copy(alpha = 0.5f),
        ),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 0.dp),
    ) {
        val contentColor = LocalContentColor.current
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                (index + 1).toString().padStart(2, '0'),
                color = contentColor.copy(alpha = 0.66f),
                fontSize = 13.sp,
                modifier = Modifier.width(44.dp),
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.CenterVertically),
            ) {
                Text(track.title, color = contentColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                track.artistName.orEmpty().takeIf(String::isNotBlank)?.let {
                    Text(it, color = contentColor.copy(alpha = 0.72f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(
                Modifier
                    .width(104.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.CenterVertically),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    if (track.isCue) "需兼容" else formatDuration(track.durationMs ?: 0),
                    color = contentColor.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                )
                track.albumName?.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

internal data class ExactTrackQueueWindow(
    val items: List<Track>,
    val segments: List<QueuePageSegment>,
)

internal fun exactTrackQueueWindow(
    pages: List<Page<Track>>,
    selectedIndex: Int,
    maxSize: Int = MAX_ACTIVE_QUEUE_ITEMS,
): ExactTrackQueueWindow? {
    val first = pages.firstOrNull() ?: return null
    if (first.pageSize <= 0 || maxSize < first.pageSize) return null
    val rawWindowSize = maxSize / first.pageSize * first.pageSize
    if (pages.withIndex().any { (index, page) ->
            page.page != first.page + index ||
                page.pageSize != first.pageSize ||
                page.total != first.total ||
                page.sort != first.sort ||
                page.items.size > page.pageSize
        }
    ) return null

    val allItems = pages.flatMap(Page<Track>::items)
    val selected = allItems.getOrNull(selectedIndex)?.takeIf(::isTrackPlayable) ?: return null
    val bounded = boundedQueueWindow(
        items = allItems,
        selectedIndex = selectedIndex,
        maxSize = rawWindowSize,
        pageSize = first.pageSize,
    )
    val windowStart = bounded.startIndex
    val windowEnd = windowStart + bounded.items.size
    var loadedOffset = 0
    val retainedPages = buildList {
        pages.forEach { page ->
            val pageStart = loadedOffset
            val pageEnd = pageStart + page.items.size
            loadedOffset = pageEnd
            if (pageEnd <= windowStart || pageStart >= windowEnd) return@forEach
            if (pageStart < windowStart || pageEnd > windowEnd) return null
            add(page)
        }
    }
    if (retainedPages.flatMap(Page<Track>::items) != bounded.items) return null
    val playableIds = bounded.items.filter(::isTrackPlayable).map { it.guid.value }
    if (playableIds.distinct().size != playableIds.size) return null

    val segments = retainedPages.map { page ->
        val sourceStartIndex = (page.page - 1) * page.pageSize
        QueuePageSegment(
            page = page.page,
            rawRowCount = page.items.size,
            playableItems = page.items.mapIndexedNotNull { index, track ->
                track.takeIf(::isTrackPlayable)?.let {
                    QueuePageItem(it.guid.value, sourceStartIndex + index)
                }
            },
            sort = page.sort,
            knownTotal = page.total,
            pageSize = page.pageSize,
            sourceStartIndex = sourceStartIndex,
        )
    }
    if (segments.flatMap(QueuePageSegment::mediaIds) != playableIds) return null
    if (selected.guid.value !in playableIds) return null
    return ExactTrackQueueWindow(items = bounded.items, segments = segments)
}

internal fun isTrackPlayable(track: Track): Boolean =
    !track.isCue && (track.accessStatus == null || track.accessStatus == 0)

@Composable
internal fun PlaylistTile(
    title: String,
    subtitle: String,
    coverId: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    featureArtwork: HomeArtworkKind? = null,
    onClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(8.dp)
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
            .then(
                if (fillWidth) Modifier.fillMaxWidth().height(142.dp)
                else Modifier.size(width = 193.dp, height = 142.dp),
            ),
        shape = ButtonDefaults.shape(
            shape = cardShape,
            focusedShape = cardShape,
            pressedShape = cardShape,
            disabledShape = cardShape,
            focusedDisabledShape = cardShape,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.025f),
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF1B201F),
            contentColor = FnColors.Text,
            focusedContainerColor = Color(0xFF303634),
            focusedContentColor = FnColors.Text,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            PlaylistTileArtwork(
                title = title,
                coverId = coverId,
                accent = accent,
                modifier = Modifier.fillMaxWidth().height(108.dp),
                featureArtwork = featureArtwork,
            )
            Row(
                Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 9.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    maxLines = if (subtitle.isBlank()) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = FnColors.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun PlaylistTileArtwork(
    title: String,
    coverId: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    featureArtwork: HomeArtworkKind? = null,
) {
    val container = LocalAuthenticatedDependencies.current
    val shape = RectangleShape
    if (featureArtwork != null) {
        HomeFeatureArtwork(featureArtwork, modifier)
    } else if (coverId != null) {
        RemoteArtwork(
            container = container,
            coverId = coverId,
            variant = CoverVariant.Grid,
            modifier = modifier,
            shape = shape,
            contentScale = ContentScale.Crop,
            placeholderContent = {
                InitialArtworkPlaceholder(title, accent, Modifier.fillMaxSize(), shape)
            },
        )
    } else {
        InitialArtworkPlaceholder(title, accent, modifier, shape)
    }
}

internal enum class HomeArtworkKind { Roam, Favorites, Collection, PlaylistGrid }

@Composable
private fun HomeFeatureArtwork(kind: HomeArtworkKind, modifier: Modifier) {
    Canvas(modifier) {
        val shortEdge = minOf(size.width, size.height)
        fun point(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(size.width * x, size.height * y)
        fun drawVinyl(center: androidx.compose.ui.geometry.Offset, radius: Float, label: Color) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38393D),
                        Color(0xFF0B0C0E),
                        Color(0xFF27282C),
                        Color(0xFF08090A),
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
            listOf(0.58f, 0.72f, 0.86f).forEach { scale ->
                drawCircle(
                    Color.White.copy(alpha = 0.08f),
                    radius * scale,
                    center,
                    style = Stroke(shortEdge * 0.006f),
                )
            }
            drawCircle(label, radius * 0.24f, center)
            drawCircle(Color(0xFFF8F2E7), radius * 0.055f, center)
        }

        when (kind) {
            HomeArtworkKind.Roam -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFEAE6DF), Color(0xFFC5CDCB)),
                        start = point(0.08f, 0f),
                        end = point(0.94f, 1f),
                    ),
                )
                drawVinyl(point(0.69f, 0.51f), shortEdge * 0.43f, FnColors.Warning)

                val sleeveEdge = size.height * 0.84f
                val sleeveTopLeft = point(0.12f, 0.08f)
                val sleeveSize = androidx.compose.ui.geometry.Size(sleeveEdge, sleeveEdge)
                val sleeveCorner = androidx.compose.ui.geometry.CornerRadius(shortEdge * 0.065f)
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.20f),
                    topLeft = sleeveTopLeft + androidx.compose.ui.geometry.Offset(shortEdge * 0.035f, shortEdge * 0.045f),
                    size = sleeveSize,
                    cornerRadius = sleeveCorner,
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF4938),
                            FnColors.Coral,
                            Color(0xFFEC477E),
                            Color(0xFF7569D8),
                        ),
                        start = sleeveTopLeft,
                        end = sleeveTopLeft + androidx.compose.ui.geometry.Offset(sleeveEdge, sleeveEdge),
                    ),
                    topLeft = sleeveTopLeft,
                    size = sleeveSize,
                    cornerRadius = sleeveCorner,
                )
                val orangeWave = Path().apply {
                    moveTo(sleeveTopLeft.x, sleeveTopLeft.y + sleeveEdge * 0.68f)
                    cubicTo(
                        sleeveTopLeft.x + sleeveEdge * 0.18f, sleeveTopLeft.y + sleeveEdge * 0.48f,
                        sleeveTopLeft.x + sleeveEdge * 0.32f, sleeveTopLeft.y + sleeveEdge * 0.56f,
                        sleeveTopLeft.x + sleeveEdge * 0.48f, sleeveTopLeft.y + sleeveEdge * 0.42f,
                    )
                    cubicTo(
                        sleeveTopLeft.x + sleeveEdge * 0.66f, sleeveTopLeft.y + sleeveEdge * 0.24f,
                        sleeveTopLeft.x + sleeveEdge * 0.78f, sleeveTopLeft.y + sleeveEdge * 0.24f,
                        sleeveTopLeft.x + sleeveEdge, sleeveTopLeft.y + sleeveEdge * 0.05f,
                    )
                    lineTo(sleeveTopLeft.x + sleeveEdge, sleeveTopLeft.y + sleeveEdge)
                    lineTo(sleeveTopLeft.x, sleeveTopLeft.y + sleeveEdge)
                    close()
                }
                drawPath(orangeWave, Color(0xFFFFA538).copy(alpha = 0.84f))
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        start = sleeveTopLeft,
                        end = sleeveTopLeft + androidx.compose.ui.geometry.Offset(sleeveEdge * 0.72f, sleeveEdge),
                    ),
                    topLeft = sleeveTopLeft,
                    size = sleeveSize,
                    cornerRadius = sleeveCorner,
                )
            }

            HomeArtworkKind.Favorites -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF304D),
                            Color(0xFFFF563F),
                            Color(0xFFFF9D37),
                            Color(0xFFFFD45A),
                        ),
                        start = point(0.08f, 0.06f),
                        end = point(0.94f, 0.92f),
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFD91442).copy(alpha = 0.72f), Color.Transparent),
                        center = point(0.20f, 0.20f),
                        radius = shortEdge * 0.78f,
                    ),
                    radius = shortEdge * 0.78f,
                    center = point(0.20f, 0.20f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFDA70).copy(alpha = 0.65f), Color.Transparent),
                        center = point(0.89f, 0.89f),
                        radius = shortEdge * 0.62f,
                    ),
                    radius = shortEdge * 0.62f,
                    center = point(0.89f, 0.89f),
                )
                val heartSize = androidx.compose.ui.geometry.Size(shortEdge * 0.56f, shortEdge * 0.56f)
                val heartOrigin = androidx.compose.ui.geometry.Offset(
                    (size.width - heartSize.width) / 2f,
                    (size.height - heartSize.height) / 2f - shortEdge * 0.015f,
                )
                withTransform({ translate(heartOrigin.x + shortEdge * 0.035f, heartOrigin.y + shortEdge * 0.055f) }) {
                    drawPath(heartPath(heartSize), Color(0xFF9E1731).copy(alpha = 0.18f))
                }
                withTransform({ translate(heartOrigin.x, heartOrigin.y) }) {
                    drawPath(heartPath(heartSize), Color(0xFFFFDDD8).copy(alpha = 0.78f))
                }
            }

            HomeArtworkKind.Collection -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF124A3A), Color(0xFF267A60), FnColors.Teal),
                        start = point(0f, 0.10f),
                        end = point(1f, 0.90f),
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFB4ECD7).copy(alpha = 0.30f), Color.Transparent),
                        center = point(0.88f, 0.12f),
                        radius = shortEdge * 0.72f,
                    ),
                    radius = shortEdge * 0.72f,
                    center = point(0.88f, 0.12f),
                )
                val discCenter = point(0.70f, 0.50f)
                drawCircle(Color.Black.copy(alpha = 0.18f), shortEdge * 0.36f, discCenter + point(0.015f, 0.035f))
                drawVinyl(discCenter, shortEdge * 0.35f, FnColors.Coral)

                val coverEdge = size.height * 0.70f
                val corner = androidx.compose.ui.geometry.CornerRadius(shortEdge * 0.045f)
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.18f),
                    topLeft = point(0.16f, 0.20f),
                    size = androidx.compose.ui.geometry.Size(coverEdge, coverEdge),
                    cornerRadius = corner,
                )
                drawRoundRect(
                    color = Color(0xFFE7C95D),
                    topLeft = point(0.12f, 0.13f),
                    size = androidx.compose.ui.geometry.Size(coverEdge, coverEdge),
                    cornerRadius = corner,
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFF4EFE5), Color(0xFFD5E0DB)),
                        start = point(0.19f, 0.17f),
                        end = point(0.53f, 0.84f),
                    ),
                    topLeft = point(0.19f, 0.17f),
                    size = androidx.compose.ui.geometry.Size(coverEdge, coverEdge),
                    cornerRadius = corner,
                )
                drawRoundRect(
                    color = FnColors.Coral,
                    topLeft = point(0.24f, 0.24f),
                    size = androidx.compose.ui.geometry.Size(coverEdge * 0.23f, coverEdge * 0.62f),
                    cornerRadius = corner,
                )
                drawRoundRect(
                    color = Color(0xFF202528),
                    topLeft = point(0.35f, 0.24f),
                    size = androidx.compose.ui.geometry.Size(coverEdge * 0.14f, coverEdge * 0.62f),
                    cornerRadius = corner,
                )
                drawRoundRect(
                    color = FnColors.Teal,
                    topLeft = point(0.43f, 0.24f),
                    size = androidx.compose.ui.geometry.Size(coverEdge * 0.09f, coverEdge * 0.62f),
                    cornerRadius = corner,
                )
            }

            HomeArtworkKind.PlaylistGrid -> {
                drawRect(Color(0xFF171C1F))
                val borderInset = shortEdge * 0.012f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.13f),
                    topLeft = androidx.compose.ui.geometry.Offset(borderInset, borderInset),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - borderInset * 2f,
                        size.height - borderInset * 2f,
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(shortEdge * 0.07f),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                        ),
                    ),
                )
                val cellSize = shortEdge * 0.15f
                val gap = shortEdge * 0.055f
                val gridWidth = cellSize * 2f + gap
                val gridHeight = cellSize * 2f + gap
                val gridOrigin = androidx.compose.ui.geometry.Offset(
                    (size.width - gridWidth) / 2f,
                    (size.height - gridHeight) / 2f,
                )
                repeat(2) { row ->
                    repeat(2) { column ->
                        drawRoundRect(
                            color = Color(0xFF858B8E),
                            topLeft = androidx.compose.ui.geometry.Offset(
                                gridOrigin.x + column * (cellSize + gap),
                                gridOrigin.y + row * (cellSize + gap),
                            ),
                            size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(shortEdge * 0.035f),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ArtistLockup(
    title: String,
    subtitle: String,
    coverId: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    onClick: () -> Unit,
) {
    val container = LocalAuthenticatedDependencies.current
    val shape = RoundedCornerShape(8.dp)
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
            .then(
                if (fillWidth) Modifier.fillMaxWidth().height(CATALOG_TILE_HEIGHT)
                else Modifier.size(width = 170.dp, height = CATALOG_TILE_HEIGHT),
            )
            .onFocusChanged { state ->
                if (state.isFocused && coverId != null) {
                    container.artworkBitmapCache.prefetch(coverId, CoverVariant.Grid)
                }
            },
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.025f),
        colors = lockupButtonColors(),
        border = lockupButtonBorder(shape),
        contentPadding = PaddingValues(7.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (coverId != null) {
                RemoteArtwork(
                    container = container,
                    coverId = coverId,
                    variant = CoverVariant.Compact,
                    modifier = Modifier.size(CATALOG_ARTIST_AVATAR_SIZE),
                    shape = CircleShape,
                    contentScale = ContentScale.Crop,
                    placeholderContent = { ArtistAvatarPlaceholder(title, Modifier.size(CATALOG_ARTIST_AVATAR_SIZE)) },
                )
            } else {
                ArtistAvatarPlaceholder(title, Modifier.size(CATALOG_ARTIST_AVATAR_SIZE))
            }
            Spacer(Modifier.width(10.dp))
            LockupLabels(title, subtitle, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ArtistAvatarPlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 23.sp,
) {
    Box(
        modifier.background(Color(0xFF2D4A46), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title.trim().take(1).ifBlank { "音" }.uppercase(),
            color = FnColors.Teal,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun AlbumLockup(
    title: String,
    subtitle: String,
    coverId: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    onClick: () -> Unit,
) {
    val container = LocalAuthenticatedDependencies.current
    val shape = RoundedCornerShape(8.dp)
    val artworkShape = RoundedCornerShape(4.dp)
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
            .then(
                if (fillWidth) Modifier.fillMaxWidth().height(CATALOG_TILE_HEIGHT)
                else Modifier.size(width = 165.dp, height = CATALOG_TILE_HEIGHT),
            )
            .onFocusChanged { state ->
                if (state.isFocused && coverId != null) {
                    container.artworkBitmapCache.prefetch(coverId, CoverVariant.Grid)
                }
            },
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.025f),
        colors = lockupButtonColors(),
        border = lockupButtonBorder(shape),
        contentPadding = PaddingValues(7.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (coverId != null) {
                RemoteArtwork(
                    container = container,
                    coverId = coverId,
                    variant = CoverVariant.Compact,
                    modifier = Modifier.size(CATALOG_ARTWORK_SIZE),
                    shape = artworkShape,
                    contentScale = ContentScale.Crop,
                    placeholderContent = {
                        InitialArtworkPlaceholder(title, FnColors.Coral, Modifier.fillMaxSize(), artworkShape)
                    },
                )
            } else {
                InitialArtworkPlaceholder(title, FnColors.Coral, Modifier.size(CATALOG_ARTWORK_SIZE), artworkShape)
            }
            Spacer(Modifier.width(9.dp))
            LockupLabels(title, subtitle, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun GenreTile(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
            .then(
                if (fillWidth) Modifier.fillMaxWidth().height(CATALOG_TILE_HEIGHT)
                else Modifier.size(width = 165.dp, height = CATALOG_TILE_HEIGHT),
            ),
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.025f),
        colors = lockupButtonColors(),
        border = lockupButtonBorder(shape),
        contentPadding = PaddingValues(7.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            InitialArtworkPlaceholder(
                title,
                genreAccentColor(title),
                Modifier.size(CATALOG_ARTWORK_SIZE),
                RoundedCornerShape(4.dp),
            )
            Spacer(Modifier.width(9.dp))
            LockupLabels(title, subtitle, Modifier.weight(1f))
        }
    }
}

/** 官方 APK 为风格页准备了多色占位图；这里按名称散列出同等的固定色板（官方 5 色的近似）。 */
internal fun genreAccentColor(name: String): Color {
    val palette = listOf(
        Color(0xFFE86A6A),
        Color(0xFFE8934F),
        Color(0xFF5C9BE8),
        Color(0xFF9F72D8),
        Color(0xFF55C5A5),
    )
    val digest = name.trim().fold(17) { acc, char -> acc * 31 + char.code }
    return palette[((digest % palette.size) + palette.size) % palette.size]
}

@Composable
private fun LockupLabels(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleFontSize: TextUnit = 15.sp,
    subtitleFontSize: TextUnit = 11.sp,
) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        Text(title, fontSize = titleFontSize, lineHeight = titleFontSize * 1.2f, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                color = FnColors.Muted,
                fontSize = subtitleFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 文件夹瓦片的按名称着色的文件夹插画：背景渐变与夹体颜色由文件夹名称散列决定，
 * 不同文件夹获得不同配色，一眼可区分（同风格页的多色占位图思路一致）。
 */
internal fun folderAccentColor(name: String): Color {
    val palette = listOf(
        Color(0xFF2E9A77), // 青绿
        Color(0xFFE0785A), // 珊瑚橙
        Color(0xFF5C9BE8), // 蓝
        Color(0xFF9F72D8), // 紫
        Color(0xFFE0A63F), // 琥珀
        Color(0xFFC9558E), // 玫红
    )
    val digest = name.trim().fold(17) { acc, char -> acc * 31 + char.code }
    return palette[((digest % palette.size) + palette.size) % palette.size]
}

/** 目录瓦片封面拼贴的槽数：2×2 均等四宫格。 */
internal const val FOLDER_COVER_MOSAIC_LIMIT = 4

/**
 * 目录瓦片的封面拼贴：子树内前几首歌曲的封面按 2×2 均等铺满，
 * 封面不足的槽位用目录主题色补位，保持与彩色文件夹插画一致的观感。
 */
@Composable
private fun FolderCoverMosaic(
    coverIds: List<String>,
    accent: Color,
    modifier: Modifier = Modifier,
    shape: Shape,
) {
    val container = LocalAuthenticatedDependencies.current
    val slotColor = lerp(accent, Color.Black, 0.20f)
    Box(modifier.clip(shape).background(slotColor)) {
        Column(Modifier.fillMaxSize()) {
            repeat(2) { row ->
                Row(Modifier.weight(1f)) {
                    repeat(2) { column ->
                        val coverId = coverIds.getOrNull(row * 2 + column)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(0.5.dp),
                        ) {
                            if (coverId != null) {
                                RemoteArtwork(
                                    container = container,
                                    coverId = coverId,
                                    variant = CoverVariant.Compact,
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RectangleShape,
                                    contentScale = ContentScale.Crop,
                                    placeholderContent = {
                                        Box(Modifier.fillMaxSize().background(slotColor))
                                    },
                                )
                            } else {
                                Box(Modifier.fillMaxSize().background(slotColor))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTileArtwork(title: String, modifier: Modifier = Modifier, shape: Shape) {    val accent = folderAccentColor(title)
    Canvas(modifier.clip(shape)) {
        val width = size.width
        val height = size.height
        val shortEdge = minOf(width, height)
        val corner = androidx.compose.ui.geometry.CornerRadius(shortEdge * 0.06f)
        // 背景渐变：深色 → 主题色
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(lerp(accent, Color.Black, 0.55f), accent),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(width, height),
            ),
        )
        // 右上角柔光，增加层次
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(width * 0.82f, height * 0.10f),
                radius = shortEdge * 0.70f,
            ),
            radius = shortEdge * 0.70f,
            center = androidx.compose.ui.geometry.Offset(width * 0.82f, height * 0.10f),
        )
        val tabColor = lerp(accent, Color.Black, 0.40f)
        val frontColor = lerp(accent, Color.White, 0.78f)
        // 夹背 + 标签耳朵
        drawRoundRect(
            color = tabColor,
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.10f, height * 0.24f),
            size = androidx.compose.ui.geometry.Size(width * 0.34f, height * 0.30f),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = tabColor,
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.10f, height * 0.36f),
            size = androidx.compose.ui.geometry.Size(width * 0.80f, height * 0.46f),
            cornerRadius = corner,
        )
        // 前面板：浅色调，形成开盖层次
        drawRoundRect(
            color = frontColor,
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.10f, height * 0.50f),
            size = androidx.compose.ui.geometry.Size(width * 0.80f, height * 0.32f),
            cornerRadius = corner,
        )
        // 前面板顶部一条高光
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.14f, height * 0.52f),
            size = androidx.compose.ui.geometry.Size(width * 0.72f, height * 0.05f),
            cornerRadius = corner,
        )
    }
}

@Composable
private fun lockupButtonColors() = ButtonDefaults.colors(
    containerColor = Color.Transparent,
    contentColor = FnColors.Text,
    focusedContainerColor = Color(0xFF303634),
    focusedContentColor = FnColors.Text,
    pressedContainerColor = Color(0xFF303634),
    pressedContentColor = FnColors.Text,
    disabledContainerColor = Color.Transparent,
)

@Composable
private fun lockupButtonBorder(shape: Shape) = ButtonDefaults.border(
    border = Border(BorderStroke(1.5.dp, Color.Transparent), shape = shape),
    focusedBorder = Border(BorderStroke(1.5.dp, FnColors.Coral), shape = shape),
    pressedBorder = Border(BorderStroke(1.5.dp, FnColors.Coral), shape = shape),
)

@Composable
private fun CatalogRetryButton(
    loading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Button(
        enabled = !loading,
        onClick = onClick,
        modifier = modifier.size(width = 170.dp, height = 95.dp),
        shape = ButtonDefaults.shape(shape, shape, shape, shape, shape),
        scale = ButtonDefaults.scale(focusedScale = 1.025f),
        colors = lockupButtonColors(),
        border = lockupButtonBorder(shape),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(if (loading) "正在加载" else "重试", fontSize = 12.sp)
            Text("重新加载列表", color = FnColors.Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun RemoteArtwork(
    container: AuthenticatedAppDependencies,
    coverId: String,
    variant: CoverVariant,
    modifier: Modifier = Modifier,
    fallbackVariant: CoverVariant? = null,
    shape: Shape = RoundedCornerShape(8.dp),
    contentScale: ContentScale = ContentScale.Fit,
    placeholderContent: (@Composable () -> Unit)? = null,
) {
    val bitmap = rememberRemoteArtworkBitmap(container, coverId, variant, fallbackVariant)
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), null, modifier.clip(shape), contentScale = contentScale)
    } else {
        Box(modifier.background(FnColors.Surface, shape), contentAlignment = Alignment.Center) {
            if (placeholderContent != null) {
                placeholderContent()
            } else {
                InitialArtworkPlaceholder(
                    text = "",
                    accent = FnColors.Teal,
                    modifier = Modifier.fillMaxSize(),
                    shape = shape,
                )
            }
        }
    }
}

@Composable
private fun rememberRemoteArtworkBitmap(
    container: AuthenticatedAppDependencies,
    coverId: String?,
    variant: CoverVariant,
    fallbackVariant: CoverVariant? = null,
): Bitmap? {
    val initialBitmap = remember(container, coverId, variant, fallbackVariant) {
        coverId?.let { id ->
            container.artworkBitmapCache.peek(id, variant)
                ?: fallbackVariant?.let { container.artworkBitmapCache.peek(id, it) }
        }
    }
    val bitmap by produceState(initialBitmap, container, coverId, variant, fallbackVariant) {
        value = coverId?.let { id ->
            container.artworkBitmapCache.getProgressively(
                coverId = id,
                variant = variant,
                fallbackVariant = fallbackVariant,
                onIntermediate = { value = it },
            )
        }
    }
    return if (coverId == null) null else bitmap
}

@Composable
internal fun InlineError(error: AppError) {
    Text(appErrorMessage(error), color = FnColors.Coral, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
}

internal fun appErrorMessage(error: AppError): String = when (error) {
    AppError.NetworkUnavailable -> "NAS 暂时不可用"
    AppError.Empty -> "暂无内容"
    AppError.UnavailableTrack -> "歌曲不可访问"
    AppError.TranscodeUnavailable -> "兼容播放参数尚未确认"
    AppError.CollectionChanged -> "列表已更新，请返回后重新载入"
    else -> "加载失败，请重试"
}

internal fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1_000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
