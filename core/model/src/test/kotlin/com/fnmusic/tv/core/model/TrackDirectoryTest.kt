package com.fnmusic.tv.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackDirectoryTest {
    private fun track(
        guid: String,
        title: String = guid,
        path: String?,
        coverId: String? = null,
    ): Track = Track(
        guid = TrackGuid(guid),
        title = title,
        artistName = null,
        albumName = null,
        coverId = coverId,
        durationMs = null,
        isCue = false,
        path = path,
    )

    @Test
    fun `directoryPath returns parent directory and null when unresolvable`() {
        assertEquals("/vol2/1000/歌曲", track("a", path = "/vol2/1000/歌曲/黄土谣.flac").directoryPath())
        assertEquals("/vol2/1000/歌曲", track("b", path = " /vol2/1000/歌曲/m.mp3 ").directoryPath())
        assertEquals("/", track("c", path = "/root.flac").directoryPath())
        assertNull(track("d", path = null).directoryPath())
        assertNull(track("e", path = "  ").directoryPath())
        assertNull(track("f", path = "plain-name.mp3").directoryPath())
    }

    @Test
    fun `root view groups every library by its first path segment`() {
        val view = resolveTrackDirectoryView(
            tracks = listOf(
                track("1", path = "/vol2/1000/歌曲/all/2026/a.flac"),
                track("2", path = "/vol2/1000/歌曲/b.flac"),
                track("3", path = "/vol1/音乐/c.mp3"),
            ),
            parentPath = null,
        )
        assertEquals(
            listOf(
                TrackDirectory(path = "/vol1", trackCount = 1, directTrackCount = 0),
                TrackDirectory(path = "/vol2", trackCount = 2, directTrackCount = 0),
            ),
            view.directories,
        )
        assertEquals(emptyList<Track>(), view.tracks)
    }

    @Test
    fun `directory view lists direct children and direct tracks only`() {
        val tracks = listOf(
            track("a", path = "/lib/Rock/one.flac"),
            track("b", path = "/lib/Rock/inner/two.flac"),
            track("c", path = "/lib/Rock/inner/three.mp3"),
            track("d", path = "/lib/Pop/four.flac"),
        )
        val view = resolveTrackDirectoryView(tracks, "/lib/Rock")

        assertEquals(
            listOf(TrackDirectory(path = "/lib/Rock/inner", trackCount = 2, directTrackCount = 2)),
            view.directories,
        )
        assertEquals(listOf("a"), view.tracks.map { it.guid.value })
    }

    @Test
    fun `intermediate directories without direct songs remain navigable`() {
        // 典型“歌手/专辑”两层嵌套库：每一层都没有直接歌曲，必须仍能逐层下钻。
        val tracks = listOf(
            track("1", path = "/music/周杰伦/范特西/爱在西元前.flac"),
            track("2", path = "/music/周杰伦/八度空间/半岛铁盒.flac"),
            track("3", path = "/music/林俊杰/第二天堂/江南.flac"),
        )

        val libraryRoot = resolveTrackDirectoryView(tracks, "/music")
        assertEquals(
            listOf(
                TrackDirectory(path = "/music/周杰伦", trackCount = 2, directTrackCount = 0),
                TrackDirectory(path = "/music/林俊杰", trackCount = 1, directTrackCount = 0),
            ),
            libraryRoot.directories,
        )
        assertEquals(emptyList<Track>(), libraryRoot.tracks)

        val artist = resolveTrackDirectoryView(tracks, "/music/周杰伦")
        assertEquals(
            // 范特西(U+8303) < 八度空间(U+516B)？ 否：八 U+516B < 范 U+8303，八度空间在前。
            listOf(
                TrackDirectory(path = "/music/周杰伦/八度空间", trackCount = 1, directTrackCount = 1),
                TrackDirectory(path = "/music/周杰伦/范特西", trackCount = 1, directTrackCount = 1),
            ),
            artist.directories,
        )
        assertEquals(emptyList<Track>(), artist.tracks)

        val album = resolveTrackDirectoryView(tracks, "/music/周杰伦/范特西")
        assertEquals(emptyList<TrackDirectory>(), album.directories)
        assertEquals(listOf("1"), album.tracks.map { it.guid.value })
    }

    @Test
    fun `child directories sample distinct cover ids from their subtree`() {
        val tracks = listOf(
            track("1", path = "/lib/Rock/one.flac", coverId = "cover-a"),
            track("2", path = "/lib/Rock/inner/two.flac", coverId = "cover-b"),
            // 同专辑重复封面：只采一次
            track("3", path = "/lib/Rock/inner/three.mp3", coverId = "cover-b"),
            // 无封面歌曲：跳过
            track("4", path = "/lib/Rock/inner/four.mp3", coverId = null),
            track("5", path = "/lib/Pop/five.flac", coverId = "cover-c"),
            // 顶层直挂歌曲：不属于任何子目录，不影响采样
            track("6", path = "/lib/top.flac", coverId = "cover-d"),
        )
        val view = resolveTrackDirectoryView(tracks, "/lib")

        assertEquals(
            listOf("cover-a", "cover-b"),
            view.directories.first { it.path == "/lib/Rock" }.sampleCoverIds,
        )
        assertEquals(
            listOf("cover-c"),
            view.directories.first { it.path == "/lib/Pop" }.sampleCoverIds,
        )
    }

    @Test
    fun `cover samples keep at most four per directory in server order`() {
        val tracks = (1..6).map { index ->
            track(
                guid = index.toString(),
                path = "/lib/Rock/$index.flac",
                coverId = "cover-$index",
            )
        }
        val view = resolveTrackDirectoryView(tracks, "/lib")

        assertEquals(
            listOf("cover-1", "cover-2", "cover-3", "cover-4"),
            view.directories.single().sampleCoverIds,
        )
    }

    @Test
    fun `directory cover samples cover the subtree and cap at four`() {
        val tracks = listOf(
            track("1", path = "/lib/Rock/one.flac", coverId = "cover-a"),
            track("2", path = "/lib/Rock/inner/two.flac", coverId = "cover-b"),
            track("3", path = "/lib/Rock/inner/three.mp3", coverId = "cover-b"),
            track("4", path = "/lib/Pop/four.flac", coverId = "cover-c"),
            (5..8).map { index ->
                track(index.toString(), path = "/lib/Rock/nested/$index.flac", coverId = "cover-$index")
            },
        )

        assertEquals(
            listOf("cover-a", "cover-b", "cover-5", "cover-6"),
            directoryCoverSamples(tracks, "/lib/Rock"),
        )
        assertEquals(emptyList<String>(), directoryCoverSamples(tracks, null))
    }

    @Test
    fun `root view samples covers per first path segment`() {
        val tracks = listOf(
            track("1", path = "/vol2/1000/歌曲/all/2026/a.flac", coverId = "cover-a"),
            track("2", path = "/vol2/1000/歌曲/b.flac", coverId = "cover-b"),
            track("3", path = "/vol1/音乐/c.mp3", coverId = "cover-c"),
        )
        val view = resolveTrackDirectoryView(tracks, null)

        assertEquals(
            listOf("cover-a", "cover-b"),
            view.directories.first { it.path == "/vol2" }.sampleCoverIds,
        )
        assertEquals(
            listOf("cover-c"),
            view.directories.first { it.path == "/vol1" }.sampleCoverIds,
        )
    }

    @Test
    fun `subtree and direct counts differ when a directory has nested tracks`() {        val tracks = listOf(
            track("a", path = "/lib/all/one.flac"),
            track("b", path = "/lib/all/two.flac"),
            track("c", path = "/lib/all/2026/three.mp3"),
        )
        val view = resolveTrackDirectoryView(tracks, "/lib")

        assertEquals(
            listOf(TrackDirectory(path = "/lib/all", trackCount = 3, directTrackCount = 2)),
            view.directories,
        )
    }

    @Test
    fun `root directory aggregates nested paths of a single volume`() {
        val view = resolveTrackDirectoryView(
            tracks = listOf(
                track("1", path = "/vol2/a.mp3"),
                track("2", path = "/vol2/1000/b.flac"),
            ),
            parentPath = null,
        )
        assertEquals(
            listOf(TrackDirectory(path = "/vol2", trackCount = 2, directTrackCount = 1)),
            view.directories,
        )
    }

    @Test
    fun `display name falls back through path segments`() {
        assertEquals("music", directoryDisplayName("/vol2/1000/歌曲/英语/music"))
        assertEquals("歌曲", directoryDisplayName("/歌曲/"))
        assertEquals("/", directoryDisplayName("/"))
    }
}
