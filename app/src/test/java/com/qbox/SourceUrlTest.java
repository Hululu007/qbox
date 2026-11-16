package com.qbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fongmi.android.tv.playback.vod.EpisodeProgress;
import com.fongmi.android.tv.utils.SourceUrl;
import com.fongmi.android.tv.utils.TorrentUrl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** FongMi 上游 SourceUrl/TorrentUrl backport 回归（GS-81③）。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SourceUrlTest {

    private static final String SRC = "source://episode?id=vod123&target=AAA";
    private static final String SRC_OTHER_TARGET = "source://episode?id=vod123&target=BBB";
    private static final String MAGNET = "magnet://file?id=vod123&name=ep01.mp4&index=0";

    @Test
    public void plainUrlIsUnchanged() {
        assertEquals("https://example.com/a.m3u8", EpisodeProgress.episodeKey("https://example.com/a.m3u8", "ep01"));
    }

    @Test
    public void emptyUrlFallsBackToName() {
        assertEquals("ep01", EpisodeProgress.episodeKey(null, "ep01"));
        assertEquals("ep01", EpisodeProgress.episodeKey("  ", "ep01"));
    }

    /** 解析型 URL 的不同 target 归一化到同一个源集 id —— 同一集共享进度。 */
    @Test
    public void sourceUrlIsNormalizedToItsId() {
        assertEquals("vod123", SourceUrl.original(SRC));
        assertEquals(EpisodeProgress.episodeKey(SRC, "ep01"), EpisodeProgress.episodeKey(SRC_OTHER_TARGET, "ep01"));
    }

    @Test
    public void magnetUrlIsNormalizedToItsId() {
        assertEquals("vod123", TorrentUrl.original(MAGNET));
        assertEquals("vod123", EpisodeProgress.episodeKey(MAGNET, "ep01"));
    }

    @Test
    public void sameComparesResolvedUrlsByIdAndTarget() {
        assertTrue(SourceUrl.isResolved(SRC));
        assertTrue(SourceUrl.isResolved(MAGNET));
        assertFalse(SourceUrl.isResolved("https://example.com/a.m3u8"));
        assertTrue(SourceUrl.same(SRC, SRC));
        assertFalse(SourceUrl.same(SRC, SRC_OTHER_TARGET));
    }

    @Test
    public void malformedSourceUrlDoesNotBreakEpisodeKey() {
        String broken = "source://episode?";
        assertEquals(broken, EpisodeProgress.episodeKey(broken, "ep01"));
    }
}
