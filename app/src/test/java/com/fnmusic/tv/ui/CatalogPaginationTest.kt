package com.fnmusic.tv.ui

import androidx.compose.ui.unit.dp
import com.fnmusic.tv.core.model.Page
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogPaginationTest {
    @Test
    fun `retained page keeps the server total for the television pager`() {
        val retained = retainLoadedPage(
            current = RetainedPageSnapshot<String>(),
            loaded = Page(
                items = listOf("a", "b"),
                page = 1,
                pageSize = 20,
                total = 36,
                sort = "name",
            ),
            key = { it },
        )

        assertEquals(36, retained.total)
        assertEquals(20, retained.pageSize)
    }

    @Test
    fun `adaptive grid spec scales columns and rows with available space`() {
        // 1080p 常见布局：可放 4 列 3 行
        assertEquals(
            AdaptiveGridSpec(columns = 4, rows = 3),
            adaptiveGridSpec(
                availableWidth = 832.dp,
                availableHeight = 324.dp,
                minTileWidth = 170.dp,
                tileHeight = 95.dp,
                spacing = 14.dp,
            ),
        )
        // 更宽的屏幕（4K 等大分辨率）：内容放大 1.5 倍，行列按缩放后的有效空间计算
        val scaled = adaptiveGridSpec(
            availableWidth = 1920.dp,
            availableHeight = 864.dp,
            minTileWidth = 170.dp,
            tileHeight = 95.dp,
            spacing = 14.dp,
        )
        assertEquals(ADAPTIVE_CONTENT_SCALE_MAX, scaled.contentScale)
        assertEquals(7, scaled.columns)
        assertEquals(5, scaled.rows)
        // 更高的屏幕（不触发缩放）放下更多行
        assertEquals(
            4,
            adaptiveGridSpec(
                availableWidth = 832.dp,
                availableHeight = 504.dp,
                minTileWidth = 170.dp,
                tileHeight = 95.dp,
                spacing = 14.dp,
            ).rows,
        )
    }

    @Test
    fun `adaptive grid spec keeps at least one cell and respects caps`() {
        // 空间不足时至少保留 1×1
        assertEquals(
            AdaptiveGridSpec(columns = 1, rows = 1),
            adaptiveGridSpec(
                availableWidth = 40.dp,
                availableHeight = 40.dp,
                minTileWidth = 170.dp,
                tileHeight = 95.dp,
                spacing = 14.dp,
            ),
        )
        // 超大屏幕不超过列/行上限，避免单页条数失控
        val spec = adaptiveGridSpec(
            availableWidth = 4000.dp,
            availableHeight = 2000.dp,
            minTileWidth = 170.dp,
            tileHeight = 95.dp,
            spacing = 14.dp,
        )
        assertEquals(MAX_ADAPTIVE_GRID_COLUMNS, spec.columns)
        assertEquals(MAX_ADAPTIVE_GRID_ROWS, spec.rows)
        assertEquals(MAX_ADAPTIVE_GRID_COLUMNS * MAX_ADAPTIVE_GRID_ROWS, spec.pageSize)
    }

    @Test
    fun `folder grids keep a readable content scale floor`() {
        val spec = adaptiveGridSpec(
            availableWidth = 832.dp,
            availableHeight = 472.dp,
            minTileWidth = 170.dp,
            tileHeight = 95.dp,
            spacing = 14.dp,
            minContentScale = FOLDER_GRID_CONTENT_SCALE,
        )
        assertEquals(FOLDER_GRID_CONTENT_SCALE, spec.contentScale)
        assertEquals(3, spec.columns)
        assertEquals(3, spec.rows)
    }
}
