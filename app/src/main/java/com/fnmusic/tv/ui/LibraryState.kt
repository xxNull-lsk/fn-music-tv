package com.fnmusic.tv.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fnmusic.tv.core.model.Album
import com.fnmusic.tv.core.model.AppError
import com.fnmusic.tv.core.model.AppException
import com.fnmusic.tv.core.model.Artist
import com.fnmusic.tv.core.model.Genre
import com.fnmusic.tv.core.model.Page
import com.fnmusic.tv.core.model.Playlist
import com.fnmusic.tv.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal sealed interface LibraryRoute {
    data object Home : LibraryRoute
    data object My : LibraryRoute
    data object AllPlaylists : LibraryRoute
    data class PlaylistDetail(val playlist: Playlist) : LibraryRoute
    data object Artists : LibraryRoute
    data object Albums : LibraryRoute
    data object AllTracks : LibraryRoute
    data object Favorites : LibraryRoute
    data class ArtistDetail(val artist: Artist) : LibraryRoute
    data class AlbumDetail(val album: Album) : LibraryRoute
    data object Genres : LibraryRoute
    data class GenreDetail(val genre: Genre) : LibraryRoute
    data object Folders : LibraryRoute
    data class FolderDetail(val directory: String) : LibraryRoute
    data class Player(val track: Track?) : LibraryRoute
    data object Search : LibraryRoute
    data object Settings : LibraryRoute
}

internal data class RetainedPageSnapshot<T>(
    val entries: List<T> = emptyList(),
    val page: Int = 0,
    /** 加载这些条目时使用的网络分页大小；0 表示未知（旧快照/测试构造）。 */
    val pageSize: Int = 0,
    val hasNext: Boolean = false,
    val total: Int? = null,
    val error: AppError? = null,
    val initialLoadCompleted: Boolean = false,
)

internal fun <T> retainLoadedPage(
    current: RetainedPageSnapshot<T>,
    loaded: Page<T>,
    key: (T) -> String,
): RetainedPageSnapshot<T> = current.copy(
    entries = (current.entries + loaded.items).distinctBy(key),
    page = loaded.page,
    pageSize = loaded.pageSize,
    hasNext = loaded.hasNext,
    total = loaded.total,
    error = null,
    initialLoadCompleted = current.initialLoadCompleted || loaded.page == 1,
)

internal enum class CatalogPagerTarget { Previous, Next }

/** 自适应网格的行列规格与内容缩放；分页网格的每页条数随屏幕能容纳的瓦片数变化。 */
internal data class AdaptiveGridSpec(
    val columns: Int,
    val rows: Int,
    /** 瓦片内容（文字/图片）的缩放倍数：dp 空间越大越接近 [ADAPTIVE_CONTENT_SCALE_MAX]。 */
    val contentScale: Float = 1f,
) {
    val pageSize: Int get() = columns * rows
}

/**
 * 根据可用空间与目标瓦片尺寸自适应计算网格行列数，替代写死的“几行几列”，
 * 让 720p / 1080p / 4K 等不同分辨率都能充分利用屏幕空间。
 *
 * 行列数按“目标瓦片尺寸 + 间距”向下取整：保证每个单元格不小于瓦片内容所需宽度，
 * 多出来的空间由瓦片（fillMaxWidth）摊平吸收，而不是留一大段空白或挤压内容。
 *
 * 当可用宽度超过 [ADAPTIVE_CONTENT_SCALE_REFERENCE_WIDTH]（1080p TV 的 dp 基准）时，
 * 内容按比例放大（最高 [ADAPTIVE_CONTENT_SCALE_MAX]，即 1.5 倍）：4K 等大分辨率下
 * 若仍用原始尺寸，文字和图片的物理尺寸会过小。行列数按缩放后的有效空间重新计算，
 * 因此大屏上是“更大更少”的瓦片，而不是“更小更多”。
 */
internal fun adaptiveGridSpec(
    availableWidth: Dp,
    availableHeight: Dp,
    minTileWidth: Dp,
    tileHeight: Dp,
    spacing: Dp,
    maxColumns: Int = MAX_ADAPTIVE_GRID_COLUMNS,
    maxRows: Int = MAX_ADAPTIVE_GRID_ROWS,
    minContentScale: Float = 1f,
): AdaptiveGridSpec {
    require(maxColumns > 0 && maxRows > 0)
    require(minContentScale >= 1f)
    val contentScale = maxOf(
        minContentScale,
        availableWidth / ADAPTIVE_CONTENT_SCALE_REFERENCE_WIDTH,
    ).coerceIn(1f, ADAPTIVE_CONTENT_SCALE_MAX)
    // 缩放后瓦片在“缩放密度”里仍按原始 dp 布局，行列数按有效（除以缩放）空间计算。
    val effectiveWidth = availableWidth / contentScale
    val effectiveHeight = availableHeight / contentScale
    val columns = ((effectiveWidth + spacing) / (minTileWidth + spacing))
        .toInt()
        .coerceIn(1, maxColumns)
    val rows = ((effectiveHeight + spacing) / (tileHeight + spacing))
        .toInt()
        .coerceIn(1, maxRows)
    return AdaptiveGridSpec(columns = columns, rows = rows, contentScale = contentScale)
}

/** 列数上限：避免超宽屏出现大量过小的瓦片，同时限制单页请求条数。 */
internal const val MAX_ADAPTIVE_GRID_COLUMNS: Int = 8

/** 行数上限：避免超高屏把单页条数撑得过大。 */
internal const val MAX_ADAPTIVE_GRID_ROWS: Int = 6

/** 内容缩放上限：大分辨率下瓦片内容最多放大到 1.5 倍。 */
internal const val ADAPTIVE_CONTENT_SCALE_MAX: Float = 1.5f

/**
 * 文件夹列表的内容缩放下限：文件夹瓦片的路径副标题字号很小、图标偏淡，
 * 任何分辨率下都至少放大 1.4 倍，保证电视上看得清。
 */
internal const val FOLDER_GRID_CONTENT_SCALE: Float = 1.4f

/** 内容缩放基准宽度：1080p TV 的可用 dp 宽度；低于该基准不缩放，超过按比例放大。 */
internal val ADAPTIVE_CONTENT_SCALE_REFERENCE_WIDTH: Dp = 960.dp

/** 文件夹浏览共享的整库扫描列表状态键，所有 FolderDetail 路由在栈内复用同一次扫描。 */
internal const val FOLDER_SCAN_STATE_KEY: String = "folder-scan"

internal fun shouldLoadInitialPage(snapshot: RetainedPageSnapshot<*>): Boolean =
    !snapshot.initialLoadCompleted

internal data class RetainedListSnapshot<T>(
    val entries: List<T> = emptyList(),
    val error: AppError? = null,
    val initialLoadCompleted: Boolean = false,
)

internal fun <T> retainLoadedList(
    current: RetainedListSnapshot<T>,
    loaded: List<T>,
): RetainedListSnapshot<T> = current.copy(
    entries = loaded,
    error = null,
    initialLoadCompleted = true,
)

internal fun shouldLoadInitialList(snapshot: RetainedListSnapshot<*>): Boolean =
    !snapshot.initialLoadCompleted || snapshot.entries.isEmpty() && snapshot.error != null

internal data class RetainedTrackCollectionSnapshot(
    val tracks: List<Track> = emptyList(),
    val loadedPages: List<Page<Track>> = emptyList(),
    val page: Int = 0,
    val hasNext: Boolean = false,
    val error: AppError? = null,
    val expectedTotal: Int? = null,
    val expectedSort: String? = null,
    val initialLoadCompleted: Boolean = false,
)

internal fun retainTrackCollectionPage(
    current: RetainedTrackCollectionSnapshot,
    loaded: Page<Track>,
    targetPage: Int,
): RetainedTrackCollectionSnapshot {
    val existing = current.tracks.asSequence().map { it.guid.value }.toHashSet()
    val firstPage = current.loadedPages.firstOrNull()
    val drifted = loaded.page != targetPage || loaded.items.size > loaded.pageSize || targetPage > 1 && (
        current.expectedTotal != loaded.total ||
            current.expectedSort != loaded.sort ||
            firstPage?.pageSize != loaded.pageSize ||
            loaded.items.any { it.guid.value in existing }
        )
    if (drifted) {
        return current.copy(
            hasNext = false,
            error = AppError.CollectionChanged,
            initialLoadCompleted = current.initialLoadCompleted || targetPage == 1,
        )
    }
    return current.copy(
        tracks = current.tracks + loaded.items,
        loadedPages = current.loadedPages + loaded,
        page = targetPage,
        hasNext = loaded.hasNext,
        error = null,
        expectedTotal = loaded.total,
        expectedSort = loaded.sort,
        initialLoadCompleted = current.initialLoadCompleted || targetPage == 1,
    )
}

internal class RetainedPagedGridState<T> {
    var snapshot by mutableStateOf(RetainedPageSnapshot<T>())
    var loading by mutableStateOf(false)
    var contentRevision by mutableStateOf<Long?>(null)
}

internal class RetainedListState<T> {
    var snapshot by mutableStateOf(RetainedListSnapshot<T>())
    var loading by mutableStateOf(false)
}

internal class RetainedTrackCollectionState {
    var snapshot by mutableStateOf(RetainedTrackCollectionSnapshot())
    var loading by mutableStateOf(false)
}

internal class LibraryRetainedStateStore(val scope: CoroutineScope) {
    private val pagedStates = mutableMapOf<String, RetainedPagedGridState<*>>()
    private val trackStates = mutableMapOf<String, RetainedTrackCollectionState>()
    private val listStates = mutableMapOf<String, RetainedListState<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> paged(key: String): RetainedPagedGridState<T> =
        pagedStates.getOrPut(key) { RetainedPagedGridState<T>() } as RetainedPagedGridState<T>

    fun tracks(key: String): RetainedTrackCollectionState =
        trackStates.getOrPut(key) { RetainedTrackCollectionState() }

    @Suppress("UNCHECKED_CAST")
    fun <T> list(key: String): RetainedListState<T> =
        listStates.getOrPut(key) { RetainedListState<T>() } as RetainedListState<T>

    fun remove(keys: Set<String>) {
        keys.forEach { key ->
            pagedStates.remove(key)
            trackStates.remove(key)
            listStates.remove(key)
        }
    }

    internal fun contains(key: String): Boolean =
        key in pagedStates || key in trackStates || key in listStates

    internal val size: Int
        get() = pagedStates.size + trackStates.size + listStates.size

    fun <T> loadListOnce(state: RetainedListState<T>, loader: suspend () -> List<T>) {
        if (state.loading || !shouldLoadInitialList(state.snapshot)) return
        state.loading = true
        scope.launch {
            runCatching { loader() }
                .onSuccess { state.snapshot = retainLoadedList(state.snapshot, it) }
                .onFailure {
                    state.snapshot = state.snapshot.copy(
                        error = (it as? AppException)?.error ?: AppError.Unknown(),
                        initialLoadCompleted = true,
                    )
                }
            state.loading = false
        }
    }

    fun <T> loadFirstPageOnce(
        state: RetainedPagedGridState<T>,
        loader: suspend (Int) -> Page<T>,
        key: (T) -> String,
    ) {
        if (state.loading || !shouldLoadInitialPage(state.snapshot)) return
        state.loading = true
        scope.launch {
            runCatching { loader(1) }
                .onSuccess { state.snapshot = retainLoadedPage(state.snapshot, it, key) }
                .onFailure {
                    state.snapshot = state.snapshot.copy(
                        error = (it as? AppException)?.error ?: AppError.Unknown(),
                        initialLoadCompleted = true,
                    )
                }
            state.loading = false
        }
    }

    fun <T> loadFirstPageForRevision(
        state: RetainedPagedGridState<T>,
        revision: Long,
        loader: suspend (Int) -> Page<T>,
        key: (T) -> String,
    ) {
        val revisionChanged = shouldRefreshPagedPreview(state.contentRevision, revision)
        if (!revisionChanged && (state.loading || !shouldLoadInitialPage(state.snapshot))) return
        if (revisionChanged) {
            state.contentRevision = revision
            state.snapshot = RetainedPageSnapshot()
        }
        if (state.loading) return

        state.loading = true
        scope.launch {
            while (true) {
                val loadingRevision = state.contentRevision ?: break
                val result = runCatching { loader(1) }
                if (state.contentRevision == loadingRevision) {
                    result
                        .onSuccess { state.snapshot = retainLoadedPage(state.snapshot, it, key) }
                        .onFailure {
                            state.snapshot = state.snapshot.copy(
                                error = (it as? AppException)?.error ?: AppError.Unknown(),
                                initialLoadCompleted = true,
                            )
                        }
                    break
                }
            }
            state.loading = false
        }
    }
}

internal fun shouldRefreshPagedPreview(loadedRevision: Long?, currentRevision: Long): Boolean =
    loadedRevision != currentRevision

internal class LibraryRouteStateLifecycle {
    private var activeRoutesByKey = emptyMap<String, LibraryRoute>()

    fun update(stack: List<LibraryRoute>): List<LibraryRoute> {
        val nextRoutesByKey = stack.associateBy(LibraryRoute::storageKey)
        val removed = activeRoutesByKey.filterKeys { it !in nextRoutesByKey }.values.toList()
        activeRoutesByKey = nextRoutesByKey
        return removed
    }
}

internal fun LibraryRoute.storageKey(): String = when (this) {
    LibraryRoute.Home -> "home"
    LibraryRoute.My -> "my"
    LibraryRoute.AllPlaylists -> "playlists"
    is LibraryRoute.PlaylistDetail -> "playlist:${playlist.guid.value}"
    LibraryRoute.Artists -> "artists"
    LibraryRoute.Albums -> "albums"
    LibraryRoute.AllTracks -> "tracks"
    LibraryRoute.Favorites -> "favorites"
    is LibraryRoute.ArtistDetail -> "artist:${artist.guid.value}"
    is LibraryRoute.AlbumDetail -> "album:${album.guid.value}"
    LibraryRoute.Genres -> "genres"
    is LibraryRoute.GenreDetail -> "genre:${genre.guid.value}"
    LibraryRoute.Folders -> "folders"
    is LibraryRoute.FolderDetail -> "folder:${directory}"
    is LibraryRoute.Player -> "player"
    LibraryRoute.Search -> "search"
    LibraryRoute.Settings -> "settings"
}

internal fun LibraryRoute.retainedStateKeys(): Set<String> = when (this) {
    is LibraryRoute.PlaylistDetail -> setOf("playlist:${playlist.guid.value}:tracks")
    is LibraryRoute.ArtistDetail -> setOf(
        "artist:${artist.guid.value}:albums",
        "artist:${artist.guid.value}:tracks",
    )
    is LibraryRoute.AlbumDetail -> setOf(
        "album:${album.guid.value}:tracks",
        // 合并同名专辑后的详情页使用独立状态键，一并纳入清理。
        "album-merged:${album.guid.value}:tracks",
    )
    is LibraryRoute.GenreDetail -> setOf("genre:${genre.guid.value}:tracks")
    LibraryRoute.Folders -> setOf("shared-libraries:folders", FOLDER_SCAN_STATE_KEY)
    // 共享的整库扫描由 LibraryRoute.Folders 弹出时统一清理；子文件夹返回上一级
    // 不能清掉它，否则父目录会重新扫描并与孤儿扫描协程竞态、卡在加载中。
    is LibraryRoute.FolderDetail -> setOf("folder:${directory}:tracks")
    LibraryRoute.Favorites -> setOf("favorites:tracks")
    else -> emptySet()
}
