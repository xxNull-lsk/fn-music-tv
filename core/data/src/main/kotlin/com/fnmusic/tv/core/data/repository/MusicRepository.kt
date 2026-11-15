package com.fnmusic.tv.core.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import com.fnmusic.tv.core.data.api.AlbumDto
import com.fnmusic.tv.core.data.api.ApiDecoder
import com.fnmusic.tv.core.data.api.ArtistDto
import com.fnmusic.tv.core.data.api.GenreDto
import com.fnmusic.tv.core.data.api.LyricListDto
import com.fnmusic.tv.core.data.api.PlaylistDetailDto
import com.fnmusic.tv.core.data.api.PlaylistDto
import com.fnmusic.tv.core.data.api.SharedLibraryDto
import com.fnmusic.tv.core.data.api.SortedPageListDto
import com.fnmusic.tv.core.data.api.TrackDto
import com.fnmusic.tv.core.data.api.TrackMetadataDto
import com.fnmusic.tv.core.data.api.isRetryableRequestFailure
import com.fnmusic.tv.core.data.local.CachedIndexEntity
import com.fnmusic.tv.core.data.local.CachedLyricEntity
import com.fnmusic.tv.core.data.local.CachedPageEntity
import com.fnmusic.tv.core.data.local.LocalStore
import com.fnmusic.tv.core.data.preferences.AppPreferences
import com.fnmusic.tv.core.model.Album
import com.fnmusic.tv.core.model.AppError
import com.fnmusic.tv.core.model.AppException
import com.fnmusic.tv.core.model.Artist
import com.fnmusic.tv.core.model.CoverVariant
import com.fnmusic.tv.core.model.Genre
import com.fnmusic.tv.core.model.LyricDocument
import com.fnmusic.tv.core.model.Page
import com.fnmusic.tv.core.model.PlaybackTrack
import com.fnmusic.tv.core.model.Playlist
import com.fnmusic.tv.core.model.RoamWindow
import com.fnmusic.tv.core.model.SharedLibrary
import com.fnmusic.tv.core.model.Track
import com.fnmusic.tv.core.model.directoryPath
import com.fnmusic.tv.core.model.playback.QueueSource
import com.fnmusic.tv.core.model.preferences.CacheUsage
import com.fnmusic.tv.core.lyrics.DefaultLyricsSources
import com.fnmusic.tv.core.lyrics.LyricsMatchCoordinator
import com.fnmusic.tv.core.lyrics.LyricsMatchRequest
import com.fnmusic.tv.core.lyrics.LyricsMatchResult
import com.fnmusic.tv.core.lyrics.hasUsableLines
import com.fnmusic.tv.core.lyrics.parseLyrics
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private const val MAX_ARTWORK_DOWNLOAD_BYTES = 20 * 1024 * 1024
private const val MAX_ARTWORK_EDGE = 8_192
private const val MAX_ARTWORK_PIXELS = 16_000_000L
private const val ARTWORK_VALIDATION_LONG_EDGE = 128

internal data class ArtworkBounds(val width: Int, val height: Int)

internal fun TrackDto.toFavoriteDomain(): Track = toDomain().copy(isFavorite = true)

data class FavoriteLibraryState(
    val namespace: String? = null,
    val statuses: Map<String, Boolean> = emptyMap(),
    val pending: Set<String> = emptySet(),
    val revision: Long = 0L,
    val error: AppError? = null,
)

internal data class FavoriteMutation(
    val namespace: String,
    val trackGuid: String,
    val confirmed: Boolean,
    val desired: Boolean,
)

internal fun FavoriteLibraryState.bindNamespace(namespace: String): FavoriteLibraryState =
    if (this.namespace == namespace) this else FavoriteLibraryState(namespace = namespace)

internal fun FavoriteLibraryState.observe(trackGuid: String, favorite: Boolean): FavoriteLibraryState =
    if (trackGuid in pending) this else copy(statuses = statuses + (trackGuid to favorite))

internal fun FavoriteLibraryState.beginMutation(
    trackGuid: String,
    fallbackFavorite: Boolean,
): Pair<FavoriteLibraryState, FavoriteMutation> {
    val boundNamespace = checkNotNull(namespace)
    val confirmed = statuses[trackGuid] ?: fallbackFavorite
    val mutation = FavoriteMutation(boundNamespace, trackGuid, confirmed, !confirmed)
    return copy(
        statuses = statuses + (trackGuid to mutation.desired),
        pending = pending + trackGuid,
        error = null,
    ) to mutation
}

internal fun FavoriteLibraryState.complete(mutation: FavoriteMutation): FavoriteLibraryState =
    if (namespace != mutation.namespace) this else copy(
        statuses = statuses + (mutation.trackGuid to mutation.desired),
        pending = pending - mutation.trackGuid,
        revision = revision + 1L,
        error = null,
    )

internal fun FavoriteLibraryState.rollback(
    mutation: FavoriteMutation,
    error: AppError? = this.error,
): FavoriteLibraryState = if (namespace != mutation.namespace) this else copy(
    statuses = statuses + (mutation.trackGuid to mutation.confirmed),
    pending = pending - mutation.trackGuid,
    error = error,
)

internal fun decodeLyrics(response: LyricListDto): Pair<LyricDocument?, SyncedLyrics?> {
    val selected = response.list.firstOrNull { it.guid == response.preferred }
        ?: response.list.firstOrNull { it.isLRC }
        ?: response.list.firstOrNull()
    val document = selected?.toDomain()
    val syncedLyrics = document?.let { parseLyrics(it.content) }?.takeIf(SyncedLyrics::hasUsableLines)
    return document to syncedLyrics
}

internal fun isValidArtworkBytes(bytes: ByteArray): Boolean = isValidArtworkBytes(
    bytes = bytes,
    readBounds = { encoded ->
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(encoded, 0, encoded.size, options)
        ArtworkBounds(options.outWidth, options.outHeight)
    },
    decodeSampled = { encoded, sample ->
        BitmapFactory.decodeByteArray(
            encoded,
            0,
            encoded.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )?.let { bitmap ->
            bitmap.recycle()
            true
        } ?: false
    },
)

internal fun isValidArtworkBytes(
    bytes: ByteArray,
    readBounds: (ByteArray) -> ArtworkBounds?,
    decodeSampled: (ByteArray, Int) -> Boolean,
): Boolean {
    if (bytes.isEmpty() || bytes.size > MAX_ARTWORK_DOWNLOAD_BYTES) return false
    val bounds = runCatching { readBounds(bytes) }.getOrNull() ?: return false
    if (
        bounds.width !in 1..MAX_ARTWORK_EDGE ||
        bounds.height !in 1..MAX_ARTWORK_EDGE ||
        bounds.width.toLong() * bounds.height > MAX_ARTWORK_PIXELS
    ) {
        return false
    }

    var sample = 1
    while (
        bounds.width / sample > ARTWORK_VALIDATION_LONG_EDGE ||
        bounds.height / sample > ARTWORK_VALIDATION_LONG_EDGE
    ) {
        sample *= 2
    }
    return runCatching { decodeSampled(bytes, sample) }.getOrDefault(false)
}

class MusicRepository internal constructor(
    context: Context,
    private val session: SessionRepository,
    private val preferences: AppPreferences,
    private val localStore: LocalStore,
    metadataCapacityBytes: Int,
    repositoryScope: CoroutineScope,
    onlineLyricsMatcher: suspend (LyricsMatchRequest) -> LyricsMatchResult,
) {
    constructor(
        context: Context,
        session: SessionRepository,
        preferences: AppPreferences,
        localStore: LocalStore,
    ) : this(
        context = context,
        session = session,
        preferences = preferences,
        localStore = localStore,
        metadataCapacityBytes = METADATA_CAPACITY_BYTES,
        repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        onlineLyricsMatcher = defaultOnlineLyricsMatcher(),
    )

    private val responses = SerializedResponseCache(metadataCapacityBytes, repositoryScope)
    private val onlineLyricsResolver = OnlineLyricsResolver(
        localStore = localStore,
        responses = responses,
        namespace = session::cacheNamespace,
        matcher = onlineLyricsMatcher,
    )
    private val favoriteMutationMutex = Mutex()
    private val _favoriteState = MutableStateFlow(FavoriteLibraryState())
    val favoriteState: StateFlow<FavoriteLibraryState> = _favoriteState.asStateFlow()
    /** 整库扫描单飞协程的后台作用域：调用方取消不会中断进行中的扫描。 */
    private val scanScope: CoroutineScope = repositoryScope
    private val scanMutex = Mutex()
    private var activeScan: Deferred<List<Track>>? = null
    private val artworkCache = ArtworkCache(
        root = context.cacheDir.resolve("artwork"),
        memoryCapacityBytes = ARTWORK_MEMORY_CAPACITY_BYTES,
        diskBudgetBytes = { preferences.state.value.cacheBudget.artworkBytes },
        isValid = ::isValidArtworkBytes,
        scope = repositoryScope,
    )

    init {
        repositoryScope.launch { artworkCache.initialize() }
    }

    suspend fun playlists(): List<Playlist> = cachedIndex<List<PlaylistDto>, List<Playlist>>(
        key = "playlists",
        fetch = { session.authenticated { it.playlists() } },
    ) { list -> list.map(PlaylistDto::toDomain) }

    suspend fun playlist(guid: String): Playlist = cachedIndex<PlaylistDetailDto, Playlist>(
        key = "playlist:$guid",
        fetch = { session.authenticated { it.playlist(guid) } },
    ) { it.toDomain() }

    suspend fun playlistTracks(guid: String, page: Int) = cachedPage<TrackDto, Track>(
        sourceKey = "playlist:$guid",
        page = page,
        fetch = { session.authenticated { it.playlistTracks(guid, page) } },
    ) { it.toDomain() }.also(::observeFavoriteTracks)

    suspend fun artists(page: Int, size: Int = 50) = cachedPage<ArtistDto, Artist>(
        sourceKey = sizedPageSourceKey("artists", size),
        page = page,
        pageSize = size,
        fetch = { session.authenticated { it.artists(page, size) } },
    ) { it.toDomain() }

    suspend fun artist(guid: String): Artist = cachedIndex<ArtistDto, Artist>(
        key = "artist:$guid",
        fetch = { session.authenticated { it.artist(guid) } },
    ) { it.toDomain() }

    suspend fun artistTracks(guid: String, page: Int) = cachedPage<TrackDto, Track>(
        sourceKey = "artist-tracks:$guid",
        page = page,
        fetch = { session.authenticated { it.artistTracks(guid, page) } },
    ) { it.toDomain() }.also(::observeFavoriteTracks)

    suspend fun artistAlbums(guid: String, page: Int) = cachedPage<AlbumDto, Album>(
        sourceKey = "artist-albums:$guid",
        page = page,
        fetch = { session.authenticated { it.artistAlbums(guid, page) } },
    ) { it.toDomain() }

    suspend fun albums(page: Int, size: Int = 50) = cachedPage<AlbumDto, Album>(
        sourceKey = sizedPageSourceKey("albums", size),
        page = page,
        pageSize = size,
        fetch = { session.authenticated { it.albums(page, size) } },
    ) { it.toDomain() }

    suspend fun album(guid: String): Album = cachedIndex<AlbumDto, Album>(
        key = "album:$guid",
        fetch = { session.authenticated { it.album(guid) } },
    ) { it.toDomain() }

    suspend fun albumTracks(guid: String, page: Int) = cachedPage<TrackDto, Track>(
        sourceKey = "album-tracks:$guid",
        page = page,
        fetch = { session.authenticated { it.albumTracks(guid, page) } },
    ) { it.toDomain() }.also(::observeFavoriteTracks)

    /**
     * 风格分页。官方 APK 的乐库筛选（文件夹/风格）复刻：genre/list 实测响应解析见
     * `origin/analysis/fn_genre_folder_probe2.py`。
     */
    suspend fun genres(page: Int, size: Int = 50) = cachedPage<GenreDto, Genre>(
        sourceKey = sizedPageSourceKey("genres", size),
        page = page,
        pageSize = size,
        fetch = { session.authenticated { it.genres(page, size) } },
    ) { it.toDomain() }

    suspend fun genre(guid: String): Genre = cachedIndex<GenreDto, Genre>(
        key = "genre:$guid",
        fetch = { session.authenticated { it.genre(guid) } },
    ) { it.toDomain() }

    suspend fun genreTracks(guid: String, page: Int) = cachedPage<TrackDto, Track>(
        sourceKey = "genre-tracks:$guid",
        page = page,
        fetch = { session.authenticated { it.genreTracks(guid, page) } },
    ) { it.toDomain() }.also(::observeFavoriteTracks)

    suspend fun allTracks(page: Int, size: Int = PAGE_SIZE) = cachedPage<TrackDto, Track>(
        sourceKey = sizedPageSourceKey("all-tracks", size),
        page = page,
        pageSize = size,
        fetch = { session.authenticated { it.allTracks(page, size) } },
    ) { it.toDomain() }.also(::observeFavoriteTracks)

    /**
     * 顺序扫描整库（文件夹筛选的基础）。服务端实测（2026-11-06）`track/list`
     * 不支持任何按路径过滤的参数，文件夹视图就像官方 APK 一样在客户端分组。
     *
     * 单飞执行：并发调用（如文件夹详情页的扫描与歌曲列表加载器）共享同一个
     * 进行中的扫描，调用方等待其完整结束——避免竞态下拿到不完整/空的扫描结果。
     * 成功结果短期备忘（[SCAN_MEMO_TTL_MS]）：文件夹详情页的多次 folderTracks
     * 调用直接复用，不再逐页读缓存解码。
     */
    private var scanMemoResult: List<Track>? = null
    private var scanMemoAt: Long = 0L

    suspend fun scanAllTracks(pageSize: Int = LIBRARY_SCAN_PAGE_SIZE): List<Track> {
        while (true) {
            val memoHit = scanMutex.withLock {
                scanMemoResult?.takeIf { memo ->
                    memo.isNotEmpty() && now() - scanMemoAt < SCAN_MEMO_TTL_MS
                }
            }
            if (memoHit != null) return memoHit

            val deferred: Deferred<List<Track>> = scanMutex.withLock {
                scanMemoResult?.takeIf { memo ->
                    memo.isNotEmpty() && now() - scanMemoAt < SCAN_MEMO_TTL_MS
                }?.let { return it }
                    ?: scanScope.async { scanAllTracksLoop(pageSize) }.also { activeScan = it }
            }
            try {
                val result = deferred.await()
                scanMutex.withLock {
                    // 空结果不备忘（可能是瞬时异常），下次调用重新扫描。
                    if (result.isNotEmpty()) {
                        scanMemoResult = result
                        scanMemoAt = now()
                    }
                }
                return result
            } finally {
                scanMutex.withLock {
                    // 扫描已结束（含失败）则清除；被取消的调用不打断后台仍在进行的扫描。
                    if (activeScan === deferred && deferred.isCompleted) activeScan = null
                }
            }
        }
    }

    /**
     * 扫描循环：第一页拿总数推算页数，其余页并发抓取（限流）后按页序拼接，
     * 把原本串行的 N 次请求压缩到约一次请求的时延。
     */
    private suspend fun scanAllTracksLoop(pageSize: Int): List<Track> = coroutineScope {
        val first = allTracks(1, pageSize)
        if (first.items.isEmpty()) return@coroutineScope emptyList()
        if (first.total <= first.items.size) return@coroutineScope first.items
        val lastPage = (first.total + pageSize - 1) / pageSize
        if (lastPage <= 1) return@coroutineScope first.items
        val semaphore = Semaphore(4)
        val rest = (2..lastPage)
            .map { page ->
                async { semaphore.withPermit { page to allTracks(page, pageSize) } }
            }
            .awaitAll()
            .sortedBy { it.first }
            .map { it.second.items }
        first.items + rest.flatten()
    }

    /**
     * 某个目录下的歌曲分页（客户端投影：整库扫描后按 `Track.path` 父目录过滤）。
     * `sort` 采用扫描顺序的 `createdAt,desc`，与 QueueSource.Folder 的分页契约一致。
     */
    suspend fun folderTracks(directory: String, page: Int): Page<Track> {
        val matches = scanAllTracks().filter { it.directoryPath() == directory }
        val pageSize = PAGE_SIZE
        val start = (page.coerceAtLeast(1) - 1) * pageSize
        val slice = matches.drop(start).take(pageSize)
        return Page(
            items = slice,
            page = page,
            pageSize = pageSize,
            total = matches.size,
            sort = SCAN_SORT,
        )
    }

    /**
     * 同名同歌手专辑的合并曲目分页：逐个成员 guid 取全量曲目后拼接，再按页切片。
     * 成员专辑通常很小（每个只有少量歌曲），曲目请求会命中响应缓存。
     */
    suspend fun albumTracksMerged(guids: List<String>, page: Int): Page<Track> {
        require(guids.isNotEmpty())
        if (guids.size == 1) return albumTracks(guids.first(), page)
        val all = guids.flatMap { guid ->
            var memberPage = 1
            val memberTracks = mutableListOf<Track>()
            while (true) {
                val loaded = albumTracks(guid, memberPage)
                memberTracks += loaded.items
                if (!loaded.hasNext || loaded.items.isEmpty()) break
                memberPage++
            }
            memberTracks
        }
        val start = (page.coerceAtLeast(1) - 1) * PAGE_SIZE
        return Page(
            items = all.drop(start).take(PAGE_SIZE),
            page = page,
            pageSize = PAGE_SIZE,
            total = all.size,
            sort = ALBUM_MERGED_SORT,
        )
    }

    suspend fun favoriteTracks(page: Int): Page<Track> {
        val namespace = session.cacheNamespace()
        val response = session.authenticated { it.favoriteTracks(page, FAVORITE_PAGE_SIZE) }
        val result = Page(
            items = response.list.map(TrackDto::toFavoriteDomain),
            page = page,
            pageSize = FAVORITE_PAGE_SIZE,
            total = response.total,
            sort = FAVORITE_SORT,
        )
        observeFavoriteTracks(result, namespace)
        return result
    }

    suspend fun searchTracks(keyword: String, page: Int): Page<Track> {
        val response = session.authenticated { it.searchTracks(keyword, page) }
        val result = Page(
            items = response.list.map(TrackDto::toDomain),
            page = page,
            pageSize = response.list.size,
            total = response.total,
            sort = response.sort.ifBlank { SEARCH_TRACKS_SORT },
        ).also {
            observeFavoriteTracks(it, session.cacheNamespace())
        }
        return result
    }

    suspend fun searchArtists(keyword: String, page: Int): Page<Artist> {
        val response = session.authenticated { it.searchArtists(keyword, page) }
        return Page(
            items = response.list.map(ArtistDto::toDomain),
            page = page,
            pageSize = response.list.size,
            total = response.total,
            sort = response.sort.ifBlank { SEARCH_SORT },
        )
    }

    suspend fun searchAlbums(keyword: String, page: Int): Page<Album> {
        val response = session.authenticated { it.searchAlbums(keyword, page) }
        return Page(
            items = response.list.map(AlbumDto::toDomain),
            page = page,
            pageSize = response.list.size,
            total = response.total,
            sort = response.sort.ifBlank { SEARCH_SORT },
        )
    }

    /**
     * Playlist search is a client-side name filter: the server `search/playlist` endpoint ignores
     * the keyword (verified live), so relying on it would present an unfiltered list as results.
     */
    suspend fun searchPlaylists(keyword: String): Page<Playlist> {
        val playlists = playlists()
        val keywordNorm = keyword.trim()
        val filtered = playlists.filter { it.name.contains(keywordNorm, ignoreCase = true) }
        return Page(
            items = filtered,
            page = 1,
            pageSize = filtered.size,
            total = filtered.size,
            sort = "name",
        )
    }

    suspend fun toggleFavorite(trackGuid: String, fallbackFavorite: Boolean): Result<Boolean> =
        favoriteMutationMutex.withLock {
            val namespace = session.cacheNamespace()
            bindFavoriteNamespace(namespace)
            val (optimisticState, mutation) = _favoriteState.value.beginMutation(trackGuid, fallbackFavorite)
            _favoriteState.value = optimisticState
            try {
                session.authenticated { api ->
                    if (mutation.desired) api.createFavorite(trackGuid) else api.deleteFavorite(trackGuid)
                }
                _favoriteState.update { it.complete(mutation) }
                Result.success(mutation.desired)
            } catch (cause: CancellationException) {
                _favoriteState.update { it.rollback(mutation) }
                throw cause
            } catch (cause: Exception) {
                val error = (cause as? AppException)?.error ?: AppError.Unknown(cause.message)
                _favoriteState.update { it.rollback(mutation, error) }
                Result.failure(cause)
            }
        }

    fun clearFavoriteState() {
        _favoriteState.value = FavoriteLibraryState()
    }

    suspend fun queuePage(source: QueueSource, page: Int): Page<Track> = when (source) {
        is QueueSource.Playlist -> playlistTracks(source.guid, page)
        is QueueSource.Artist -> artistTracks(source.guid, page)
        is QueueSource.Album -> albumTracks(source.guid, page)
        is QueueSource.MergedAlbum -> albumTracksMerged(source.guids, page)
        is QueueSource.LibraryAllTracks -> allTracks(page)
        is QueueSource.Favorites -> favoriteTracks(page)
        is QueueSource.SearchTracks -> searchTracks(source.keyword, page)
        is QueueSource.Genre -> genreTracks(source.guid, page)
        is QueueSource.Folder -> folderTracks(source.directory, page)
    }

    suspend fun sharedLibraries(): List<SharedLibrary> = cachedIndex<List<SharedLibraryDto>, List<SharedLibrary>>(
        key = "shared-libraries",
        fetch = { session.authenticated { it.sharedLibraries() } },
    ) { list -> list.map(SharedLibraryDto::toDomain) }

    suspend fun trackMetadata(trackGuid: String): Track = cachedIndex<TrackMetadataDto, Track>(
        key = "track-metadata:$trackGuid",
        fetch = { session.authenticated { it.metadata(trackGuid) } },
    ) { it.toDomain() }.also(::observeFavoriteTrack)

    suspend fun currentTrackMetadata(trackGuid: String): CurrentResourceResult<Track> = currentResource {
        withCurrentResourceRetry { trackMetadata(trackGuid) }
    }

    suspend fun prepare(track: Track): PlaybackTrack {
        if (track.accessStatus != null && track.accessStatus != 0) throw AppException(AppError.UnavailableTrack)
        val refreshed = trackMetadata(track.guid.value)
        if (refreshed.isCue) throw AppException(AppError.TranscodeUnavailable)
        val api = session.requireApi()
        return PlaybackTrack(
            refreshed,
            api.streamUrl(refreshed.guid.value).toString(),
            refreshed.coverId?.let { api.coverUrl(it, CoverVariant.Player.width).toString() },
        )
    }

    fun prepareQueue(tracks: List<Track>): List<PlaybackTrack> {
        val api = session.requireApi()
        return tracks.asSequence()
            .filter { it.accessStatus == null || it.accessStatus == 0 }
            .filterNot(Track::isCue)
            .take(250)
            .map { track ->
                PlaybackTrack(
                    track,
                    api.streamUrl(track.guid.value).toString(),
                    track.coverId?.let { api.coverUrl(it, CoverVariant.Player.width).toString() },
                )
            }
            .toList()
    }

    suspend fun lyrics(trackGuid: String): Pair<LyricDocument?, SyncedLyrics?> =
        decodeLyrics(lyricResponse(trackGuid))

    private suspend fun firstPartyCurrentLyrics(trackGuid: String): CurrentResourceResult<CurrentLyrics> = currentResource {
        val (document, syncedLyrics) = withCurrentResourceRetry { lyrics(trackGuid) }
        document?.takeIf { it.content.isNotBlank() }?.let { CurrentLyrics(it, syncedLyrics) }
    }

    suspend fun currentLyrics(
        track: Track,
        onlineMatchingEnabled: Boolean = preferences.state.value.onlineLyricsMatchingEnabled,
    ): CurrentResourceResult<CurrentLyrics> = resolveLyricsWithFallback(
        onlineMatchingEnabled = onlineMatchingEnabled,
        online = { onlineLyricsResolver.resolve(track) },
        firstParty = { firstPartyCurrentLyrics(track.guid.value) },
    )

    suspend fun startRoam(): RoamWindow? {
        val namespace = session.cacheNamespace()
        return session.authenticated { it.roamStart(session.deviceId) }?.let {
            RoamWindow(null, it.current.toDomain(), it.next?.toDomain())
        }.also { observeFavoriteWindow(it, namespace) }
    }

    suspend fun nextRoam(roamId: String): RoamWindow {
        val namespace = session.cacheNamespace()
        return session.authenticated { it.roamNext(session.deviceId, roamId).toDomain() }
            .also { observeFavoriteWindow(it, namespace) }
    }

    suspend fun previousRoam(roamId: String): RoamWindow {
        val namespace = session.cacheNamespace()
        return session.authenticated { it.roamPrevious(session.deviceId, roamId).toDomain() }
            .also { observeFavoriteWindow(it, namespace) }
    }

    suspend fun artwork(coverId: String, variant: CoverVariant): ByteArray? = try {
        loadArtwork(coverId, variant)
    } catch (cause: CancellationException) {
        throw cause
    } catch (_: AppException) {
        null
    }

    suspend fun currentArtwork(
        coverId: String,
        variant: CoverVariant,
    ): CurrentResourceResult<ByteArray> = currentResource {
        withCurrentResourceRetry { loadArtwork(coverId, variant) }
    }

    suspend fun invalidateNamespace(namespace: String, includeEssential: Boolean = false) {
        responses.invalidateNamespace(namespace)
        artworkCache.clearNamespace(namespace)
        localStore.clearNamespace(namespace, includeEssential)
    }

    suspend fun clearLocalNamespace(includeEssential: Boolean) {
        val namespace = runCatching(session::cacheNamespace).getOrNull() ?: return
        invalidateNamespace(namespace, includeEssential)
    }

    suspend fun clearArtwork() = artworkCache.clearAll()

    suspend fun clearAllEvictableCaches() {
        responses.invalidateAll()
        artworkCache.clearAll()
        localStore.clearAllEvictable()
    }

    suspend fun cacheUsage(): CacheUsage = CacheUsage(
        artworkBytes = artworkCache.usageBytes(),
        indexBytes = localStore.physicalBytes(),
    )

    suspend fun applyArtworkBudget() = artworkCache.applyBudget()

    private fun observeFavoriteTracks(page: Page<Track>) {
        page.items.forEach(::observeFavoriteTrack)
    }

    private fun observeFavoriteTracks(page: Page<Track>, namespace: String) {
        page.items.forEach { observeFavoriteTrack(it, namespace) }
    }

    private fun observeFavoriteWindow(window: RoamWindow?, namespace: String) {
        window ?: return
        listOfNotNull(window.previous, window.current, window.next)
            .forEach { observeFavoriteTrack(it.track, namespace) }
    }

    private fun observeFavoriteTrack(track: Track) {
        val namespace = runCatching(session::cacheNamespace).getOrNull() ?: return
        observeFavoriteTrack(track, namespace)
    }

    private fun observeFavoriteTrack(track: Track, namespace: String) {
        if (runCatching(session::cacheNamespace).getOrNull() != namespace) return
        bindFavoriteNamespace(namespace)
        _favoriteState.update { it.observe(track.guid.value, track.isFavorite) }
    }

    private fun bindFavoriteNamespace(namespace: String) {
        if (_favoriteState.value.namespace != namespace) {
            _favoriteState.update { it.bindNamespace(namespace) }
        }
    }

    private suspend fun loadArtwork(coverId: String, variant: CoverVariant): ByteArray? {
        val namespace = session.cacheNamespace()
        return artworkCache.get(namespace, coverId, variant) {
            session.authenticated { it.cover(coverId, variant.width) }
        }
    }

    private suspend fun lyricResponse(trackGuid: String): LyricListDto {
        val namespace = session.cacheNamespace()
        val key = ResponseCacheKey(namespace, "lyric", trackGuid)
        var decodedResponse: LyricListDto? = null
        val payload = responses.getOrFetch(
            key = key,
            persist = { encoded ->
                bestEffort {
                    localStore.saveLyric(CachedLyricEntity(namespace, trackGuid, encoded, now()))
                }
            },
        ) {
            try {
                session.authenticated { it.lyrics(trackGuid) }
                    .also { decodedResponse = it }
                    .let(ApiDecoder.json::encodeToString)
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: AppException) {
                if (cause.error != AppError.NetworkUnavailable) throw cause
                val cached = fallback { localStore.lyric(namespace, trackGuid) } ?: throw cause
                decodedResponse = validatePayload<LyricListDto>(cached.payload) ?: throw cause
                cached.payload
            }
        }
        return decodedResponse ?: ApiDecoder.json.decodeFromString(payload)
    }

    private suspend inline fun <reified Dto, Domain> cachedPage(
        sourceKey: String,
        page: Int,
        pageSize: Int = PAGE_SIZE,
        crossinline fetch: suspend () -> SortedPageListDto<Dto>,
        noinline transform: (Dto) -> Domain,
    ): Page<Domain> {
        val namespace = session.cacheNamespace()
        val key = ResponseCacheKey(namespace, "page", sourceKey, page)
        var decodedResponse: SortedPageListDto<Dto>? = null
        val payload = responses.getOrFetch(
            key = key,
            persist = { encoded ->
                bestEffort {
                    val response = decodedResponse
                        ?: ApiDecoder.json.decodeFromString<SortedPageListDto<Dto>>(encoded)
                    localStore.savePage(
                        CachedPageEntity(
                            namespace = namespace,
                            sourceKey = sourceKey,
                            page = page,
                            payload = encoded,
                            total = response.total,
                            sort = response.sort,
                            accessedAt = now(),
                        ),
                    )
                }
            },
        ) {
            try {
                fetch().also { decodedResponse = it }.let(ApiDecoder.json::encodeToString)
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: AppException) {
                if (cause.error != AppError.NetworkUnavailable) throw cause
                val cached = fallback { localStore.page(namespace, sourceKey, page) } ?: throw cause
                decodedResponse = validatePayload<SortedPageListDto<Dto>>(cached.payload) ?: throw cause
                cached.payload
            }
        }
        return (decodedResponse ?: ApiDecoder.json.decodeFromString<SortedPageListDto<Dto>>(payload))
            .toDomainPage(page, pageSize, transform)
    }

    private suspend inline fun <reified Dto, Domain> cachedIndex(
        key: String,
        crossinline fetch: suspend () -> Dto,
        transform: (Dto) -> Domain,
    ): Domain {
        val namespace = session.cacheNamespace()
        val cacheKey = ResponseCacheKey(namespace, "index", key)
        var decodedResponse: Dto? = null
        val payload = responses.getOrFetch(
            key = cacheKey,
            persist = { encoded ->
                bestEffort {
                    localStore.saveIndex(CachedIndexEntity(namespace, key, encoded, now()))
                }
            },
        ) {
            try {
                fetch().also { decodedResponse = it }.let(ApiDecoder.json::encodeToString)
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: AppException) {
                if (cause.error != AppError.NetworkUnavailable) throw cause
                val cached = fallback { localStore.index(namespace, key) } ?: throw cause
                decodedResponse = validatePayload<Dto>(cached.payload) ?: throw cause
                cached.payload
            }
        }
        return transform(decodedResponse ?: ApiDecoder.json.decodeFromString(payload))
    }

    private suspend fun <T> currentResource(block: suspend () -> T?): CurrentResourceResult<T> = try {
        block()?.let { CurrentResourceResult.Ready(it) } ?: CurrentResourceResult.Absent
    } catch (cause: CancellationException) {
        throw cause
    } catch (cause: AppException) {
        when (cause.error) {
            AppError.NotFound, AppError.Empty -> CurrentResourceResult.Absent
            else -> CurrentResourceResult.Failure(cause.error, cause.isRetryableRequestFailure)
        }
    }

    private suspend fun bestEffort(block: suspend () -> Unit) {
        try {
            block()
        } catch (cause: CancellationException) {
            throw cause
        } catch (_: Exception) {
            // Room remains a fallback tier; a disk write failure must not fail a valid network response.
        }
    }

    private suspend fun <T> fallback(block: suspend () -> T): T? = try {
        block()
    } catch (cause: CancellationException) {
        throw cause
    } catch (_: Exception) {
        null
    }

    private inline fun <reified T> validatePayload(payload: String): T? = try {
        ApiDecoder.json.decodeFromString(payload)
    } catch (_: Exception) {
        null
    }

    private fun now(): Long = System.currentTimeMillis()

    private companion object {
        const val METADATA_CAPACITY_BYTES = 8 * 1024 * 1024
        const val ARTWORK_MEMORY_CAPACITY_BYTES = 24 * 1024 * 1024
        const val PAGE_SIZE = 50
        const val FAVORITE_PAGE_SIZE = 50
        const val FAVORITE_SORT = "favoriteAt,desc"
        const val SEARCH_SORT = "createdAt,desc"
        const val SEARCH_TRACKS_SORT = SEARCH_SORT

        /** 文件夹扫描（queuePage(Folder) / folderTracks）整库分页大小：实测服务端 ≥400 可用。 */
        const val LIBRARY_SCAN_PAGE_SIZE = 400
        const val SCAN_SORT = "createdAt,desc"

        /** 整库扫描成功结果的备忘时长：文件夹详情页的多次调用在窗口内直接复用。 */
        const val SCAN_MEMO_TTL_MS = 30_000L

        /** 合并专辑的曲目排序标识（成员专辑各自按 trackNo 升序拼接）。 */
        const val ALBUM_MERGED_SORT = "trackNo,asc"

        fun defaultOnlineLyricsMatcher(): suspend (LyricsMatchRequest) -> LyricsMatchResult {
            val client = okhttp3.OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .connectTimeout(1_200L, TimeUnit.MILLISECONDS)
                .readTimeout(1_800L, TimeUnit.MILLISECONDS)
                .writeTimeout(1_800L, TimeUnit.MILLISECONDS)
                .build()
            val coordinator = LyricsMatchCoordinator(DefaultLyricsSources.create(client))
            return coordinator::match
        }
    }
}

internal fun sizedPageSourceKey(sourceKey: String, size: Int): String {
    require(size > 0)
    return "$sourceKey:size=$size"
}

internal fun <Dto, Domain> SortedPageListDto<Dto>.toDomainPage(
    page: Int,
    pageSize: Int,
    transform: (Dto) -> Domain,
): Page<Domain> {
    require(pageSize > 0)
    return Page(list.map(transform), page, pageSize, total, sort)
}

internal suspend fun resolveLyricsWithFallback(
    onlineMatchingEnabled: Boolean,
    online: suspend () -> CurrentLyrics?,
    firstParty: suspend () -> CurrentResourceResult<CurrentLyrics>,
): CurrentResourceResult<CurrentLyrics> {
    if (!onlineMatchingEnabled) return firstParty()
    return try {
        online()?.let { CurrentResourceResult.Ready(it) } ?: firstParty()
    } catch (cause: CancellationException) {
        throw cause
    } catch (_: Exception) {
        firstParty()
    }
}
