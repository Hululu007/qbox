package com.qbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.C;

import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.playback.vod.VodSkipPolicy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** FongMi 上游 VodSkipPolicy backport 回归（GS-81②）。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class VodSkipPolicyTest {

    private static History history(long opening, long ending) {
        History h = new History();
        h.setOpening(opening);
        h.setEnding(ending);
        return h;
    }

    private static Result result(String skipSegmentsJson) {
        return Result.objectFrom("{\"skipSegments\":" + skipSegmentsJson + "}");
    }

    @Test
    public void fixedOpeningWinsOverResume() {
        assertEquals(30000, VodSkipPolicy.startPositionMs(history(30000, C.TIME_UNSET), new Result(), 5000));
    }

    @Test
    public void autoSkipFallsBackToResume() {
        assertEquals(5000, VodSkipPolicy.startPositionMs(history(C.TIME_UNSET, C.TIME_UNSET), new Result(), 5000));
        assertEquals(5000, VodSkipPolicy.startPositionMs(history(History.AUTO_SKIP, C.TIME_UNSET), new Result(), 5000));
    }

    @Test
    public void sourceProvidedPositionWins() {
        Result result = new Result();
        result.setPosition(20000L);
        assertEquals(20000, VodSkipPolicy.startPositionMs(history(C.TIME_UNSET, C.TIME_UNSET), result, 5000));
    }

    @Test
    public void openingSegmentSkipsWhenAutoSkip() {
        Result result = result("[{\"type\":\"opening\",\"start\":0,\"end\":42000}]");
        assertEquals(42000, VodSkipPolicy.startPositionMs(history(C.TIME_UNSET, C.TIME_UNSET), result, 5000));
    }

    @Test
    public void openingSegmentIgnoredWhenOpeningIsFixed() {
        Result result = result("[{\"type\":\"opening\",\"start\":0,\"end\":42000}]");
        assertEquals(30000, VodSkipPolicy.startPositionMs(history(30000, C.TIME_UNSET), result, 5000));
    }

    @Test
    public void nullResultDoesNotCrash() {
        assertEquals(5000, VodSkipPolicy.startPositionMs(history(C.TIME_UNSET, C.TIME_UNSET), null, 5000));
    }

    @Test
    public void middleSegmentIsAlwaysActive() {
        Result result = result("[{\"type\":\"middle\",\"start\":10000,\"end\":20000}]");
        assertNotNull(VodSkipPolicy.activeSegment(history(C.TIME_UNSET, C.TIME_UNSET), result, 15000));
        assertNull(VodSkipPolicy.activeSegment(history(C.TIME_UNSET, C.TIME_UNSET), result, 25000));
    }

    @Test
    public void openingSegmentNeedsAutoSkip() {
        Result result = result("[{\"type\":\"opening\",\"start\":0,\"end\":42000}]");
        assertNull(VodSkipPolicy.activeSegment(history(30000, C.TIME_UNSET), result, 10000));
        assertNotNull(VodSkipPolicy.activeSegment(history(C.TIME_UNSET, C.TIME_UNSET), result, 10000));
    }

    @Test
    public void invalidSegmentIsIgnored() {
        Result result = result("[{\"type\":\"opening\",\"start\":-1,\"end\":42000}]");
        assertEquals(5000, VodSkipPolicy.startPositionMs(history(C.TIME_UNSET, C.TIME_UNSET), result, 5000));
    }

    /** GS-84：duration 未就绪（<=0 / UNSET）绝不触发片尾跳集——这是 FongMi 有、我们 fork 弄丢的守卫。 */
    @Test
    public void endingNotReachedWhenDurationNotReady() {
        History h = history(C.TIME_UNSET, 30000);
        assertFalse(VodSkipPolicy.isEndingReached(h, 0, 0));
        assertFalse(VodSkipPolicy.isEndingReached(h, 5000, -1));
        assertFalse(VodSkipPolicy.isEndingReached(h, 5000, C.TIME_UNSET));
    }

    /** duration 就绪后，进入片尾窗口（position >= duration - ending）才算到片尾。 */
    @Test
    public void endingReachedOnlyInsideEndingWindow() {
        History h = history(C.TIME_UNSET, 30000);
        assertFalse(VodSkipPolicy.isEndingReached(h, 29999, 60000));
        assertTrue(VodSkipPolicy.isEndingReached(h, 30000, 60000));
        assertTrue(VodSkipPolicy.isEndingReached(h, 60000, 60000));
    }

    @Test
    public void endingNotConfiguredNeverTriggers() {
        assertFalse(VodSkipPolicy.isEndingReached(history(C.TIME_UNSET, C.TIME_UNSET), 999999, 1000000));
    }

    @Test
    public void nullHistoryNeverTriggers() {
        assertFalse(VodSkipPolicy.isEndingReached(null, 999999, 1000000));
    }
}
