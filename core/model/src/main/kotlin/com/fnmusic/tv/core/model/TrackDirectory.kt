package com.fnmusic.tv.core.model

/**
 * 文件夹视图的客户端目录投影。
 *
 * 官方飞牛音乐 APK（Flutter）无服务端文件夹浏览接口：客户端以歌曲文件的完整路径
 * （`audioSpec.path`）自己构建目录树。回声台复刻同一思路，这里保留纯函数投影，
 * 便于脱离 Composable 做单元测试。
 *
 * 目录语义与 NAS 文件系统一致：绝对路径、以 `/` 分隔；`parentPath == null` 表示库根。
 */
internal const val PATH_SEPARATOR = '/'

/** `track.path` 所在目录；无路径或路径无父目录时为 null。 */
fun Track.directoryPath(): String? {
    val raw = path?.trim().orEmpty()
    if (raw.isEmpty()) return null
    val index = raw.lastIndexOf(PATH_SEPARATOR)
    return when {
        index < 0 -> null
        index == 0 -> PATH_SEPARATOR.toString()
        else -> raw.substring(0, index)
    }
}

/** 目录的显示名：取路径最后一个非空段落，全部段落为空时回退整个路径。 */
fun directoryDisplayName(directory: String): String = directory
    .trim(PATH_SEPARATOR)
    .takeIf { it.isNotEmpty() }
    ?.substringAfterLast(PATH_SEPARATOR)
    ?.takeUnless { it.isEmpty() }
    ?: directory

/**
 * `parentPath` 下的一层目录视图。
 *
 * `parentPath == null`（库根）时 [tracks] 为空，[directories] 是每个最深目录的
 * 根级前缀（按目录去重后的第一层路径段），[TrackDirectory.trackCount] 为落在该
 * 前缀内的全部歌曲数（含子目录）。
 *
 * `parentPath` 非空时 [tracks] 是直接位于该目录下的歌曲（按服务器给定顺序），
 * [directories] 是它的直接子目录——子目录由所有更深的目录路径折算出第一段得到，
 * 因此即使某一层目录没有任何直接歌曲（典型的“歌手/专辑”两层嵌套库），中间层
 * 也会照常列出，逐层下钻不会出现死路；[TrackDirectory.trackCount] 为该子目录内
 * （含子目录）的歌曲总数，[TrackDirectory.directTrackCount] 为直接位于该子目录
 * 内的歌曲数。
 */
data class TrackDirectoryView(
    val directories: List<TrackDirectory>,
    val tracks: List<Track>,
)

data class TrackDirectory(
    /** 完整目录路径，可直接用作品种（目录浏览、队列过滤）的业务键。 */
    val path: String,
    /** 该目录内（含子目录）的歌曲总数。 */
    val trackCount: Int,
    /** 直接位于该目录内的歌曲数（不含子目录）。 */
    val directTrackCount: Int,
    /**
     * 子树内按服务器顺序取的前几首歌曲的去重封面 id（同一专辑封面只取一次），
     * 供目录瓦片做封面拼贴；无封面或旧缓存时为空。
     */
    val sampleCoverIds: List<String> = emptyList(),
)

fun resolveTrackDirectoryView(
    tracks: List<Track>,
    parentPath: String?,
): TrackDirectoryView {
    val directoryOf = tracks.mapNotNull(Track::directoryPath)
    if (parentPath == null) {
        val counted = directoryOf
            .mapNotNull(::rootSegmentOf)
            .groupingBy { it }
            .eachCount()
        val samples = coverSamplesByBucket(tracks, ::rootSegmentOf)
        return TrackDirectoryView(
            directories = counted.map { (root, count) ->
                TrackDirectory(
                    path = root,
                    trackCount = count,
                    directTrackCount = directoryOf.count { it == root },
                    sampleCoverIds = samples[root].orEmpty(),
                )
            }
                .sortedBy(TrackDirectory::path),
            tracks = emptyList(),
        )
    }
    val inDirectory = tracks.filter { it.directoryPath() == parentPath }
    val childDirectories = directoryOf
        .mapNotNull { directory -> directChildDirectory(directory, parentPath) }
        .distinct()
        .map { child ->
            TrackDirectory(
                path = child,
                trackCount = directoryOf.count { it.isWithin(child) },
                directTrackCount = directoryOf.count { it == child },
            )
        }
        .sortedBy(TrackDirectory::path)
    // 单趟扫描：任意深度的歌曲按其所属一级子目录归桶，采集去重封面样本。
    val samples = coverSamplesByBucket(tracks) { directChildDirectory(it, parentPath) }
    return TrackDirectoryView(
        directories = childDirectories.map { child ->
            child.copy(sampleCoverIds = samples[child.path].orEmpty())
        },
        tracks = inDirectory,
    )
}

/**
 * `directory` 子树内按服务器顺序取的前几个去重封面 id（同一专辑只取一次），
 * 供文件夹详情页头做封面拼贴；`directory == null` 或无封面时为空。
 */
fun directoryCoverSamples(tracks: List<Track>, directory: String?): List<String> {
    if (directory == null) return emptyList()
    val prefix = if (directory == PATH_SEPARATOR.toString()) {
        PATH_SEPARATOR.toString()
    } else {
        "$directory$PATH_SEPARATOR"
    }
    val samples = coverSamplesByBucket(tracks) { dir ->
        if (dir == directory || dir.startsWith(prefix)) directory else null
    }
    return samples[directory].orEmpty()
}

/** 目录瓦片封面拼贴的采样上限。 */
private const val DIRECTORY_COVER_SAMPLE_LIMIT = 4

/**
 * 按桶键归集每首歌曲的封面 id：保持服务器顺序、跳过无封面歌曲、同一封面只采一次、
 * 每个桶最多 [DIRECTORY_COVER_SAMPLE_LIMIT] 个。
 */
private fun coverSamplesByBucket(
    tracks: List<Track>,
    bucketOf: (String) -> String?,
): Map<String, List<String>> {
    val samples = mutableMapOf<String, MutableList<String>>()
    for (track in tracks) {
        val bucket = track.directoryPath()?.let(bucketOf) ?: continue
        val coverId = track.coverId ?: continue
        val bucketSamples = samples.getOrPut(bucket) { mutableListOf() }
        if (bucketSamples.size >= DIRECTORY_COVER_SAMPLE_LIMIT) continue
        if (coverId !in bucketSamples) bucketSamples.add(coverId)
    }
    return samples
}

/** 本目录是否等于 `directory` 或位于其下（任意深度）。 */
private fun String.isWithin(directory: String): Boolean =
    this == directory || startsWith(
        if (directory == PATH_SEPARATOR.toString()) PATH_SEPARATOR.toString()
        else "$directory$PATH_SEPARATOR"
    )

/** 目录的根级前缀：`/vol2/…` → `/vol2`；无首个路径段（`/`）时回自身。 */
private fun rootSegmentOf(directory: String): String? {
    if (directory.isEmpty()) return null
    if (!directory.startsWith(PATH_SEPARATOR)) return directory.substringBefore(PATH_SEPARATOR)
    val next = directory.indexOf(PATH_SEPARATOR, startIndex = 1)
    val root = if (next < 0) directory else directory.substring(0, next)
    return root.takeIf { it.isNotEmpty() } ?: PATH_SEPARATOR.toString()
}

/**
 * `directory` 相对 `parent` 折算出的直接子目录：取 `parent` 之后的第一段。
 * `directory` 无论嵌套多深都折算成一级子目录，保证没有直接歌曲的中间层依然可下钻；
 * 与 `parent` 同级或不在其下的目录返回 null。
 */
private fun directChildDirectory(directory: String, parent: String): String? {
    val prefix = if (parent == PATH_SEPARATOR.toString()) "" else parent
    val remainder = directory.removePrefix("$prefix$PATH_SEPARATOR").takeIf { it != directory } ?: return null
    val firstSegment = remainder.substringBefore(PATH_SEPARATOR)
    if (firstSegment.isEmpty()) return null
    return "$prefix$PATH_SEPARATOR$firstSegment"
}
