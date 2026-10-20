package com.qbox.tv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayParserTest {

    @Test
    fun `standard multi group url`() {
        val from = "线路1\$\$\$线路2"
        val url = "第01集\$http://a/1.m3u8#第02集\$http://a/2.m3u8\$\$\$第01集\$http://b/1.m3u8"
        val groups = PlayParser.parse(from, url)
        assertEquals(2, groups.size)
        assertEquals("线路1", groups[0].name)
        assertEquals(2, groups[0].episodes.size)
        assertEquals("第01集", groups[0].episodes[0].name)
        assertEquals("http://a/1.m3u8", groups[0].episodes[0].url)
        assertEquals(1, groups[1].episodes.size)
    }

    @Test
    fun `missing from uses fallback name`() {
        val url = "第01集\$http://a/1.m3u8"
        val groups = PlayParser.parse(null, url)
        assertEquals(1, groups.size)
        assertEquals("线路1", groups[0].name)
    }

    @Test
    fun `url containing dollar is preserved`() {
        // 集名$u r l 里带 $ 的情况：subList 拼接回完整 url
        val url = "第01集\$http://a/1.m3u8?x=\$1#第02集\$http://a/2.m3u8"
        val groups = PlayParser.parse("线路", url)
        assertEquals("http://a/1.m3u8?x=\$1", groups[0].episodes[0].url)
        assertEquals(2, groups[0].episodes.size)
    }

    @Test
    fun `empty and blank input`() {
        assertTrue(PlayParser.parse(null, null).isEmpty())
        assertTrue(PlayParser.parse("", "").isEmpty())
        assertTrue(PlayParser.parse("线路", "   ").isEmpty())
    }

    @Test
    fun `filters empty episodes`() {
        val url = "\$\$\$第01集\$http://a/1.m3u8"
        val groups = PlayParser.parse("A\$\$\$B", url)
        assertEquals(1, groups.size)
        assertEquals("B", groups[0].name)
    }
}
