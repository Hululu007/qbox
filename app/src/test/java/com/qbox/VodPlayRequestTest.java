package com.qbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.C;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.playback.vod.VodPlayRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * GS-81①：起播位置随播放请求作为「值」传递（FongMi 主线设计）。
 * 请求创建时定死，开播时优先使用，不被后续对 History 的写入污染。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class VodPlayRequestTest {

    private static final String KEY = "site@@@vod123@@@1";

    private static VodPlayRequest request(long startPositionMs) {
        return VodPlayRequest.create(KEY, Flag.create("line"), Episode.create("58", "ep58"), startPositionMs);
    }

    @Test
    public void defaultRequestDoesNotPinStartPosition() {
        assertEquals(C.TIME_UNSET, VodPlayRequest.create(KEY, Flag.create("line"), Episode.create("58", "ep58")).getStartPositionMs());
    }

    @Test
    public void explicitStartPositionIsCarriedByRequest() {
        assertEquals(5000, request(5000).getStartPositionMs());
        assertEquals(0, request(0).getStartPositionMs());
    }

    /** 起播位置只是「起点」，不参与请求身份比较（否则同一集不同起点会被当成不同请求，破坏预载/结果匹配）。 */
    @Test
    public void startPositionDoesNotChangeRequestIdentity() {
        assertTrue(request(C.TIME_UNSET).matches(request(9000)));
    }

    @Test
    public void identityStillDependsOnKeyFlagAndEpisode() {
        VodPlayRequest a = request(5000);
        assertTrue(a.matches(KEY, Flag.create("line"), Episode.create("58", "ep58")));
        assertFalse(a.matches(KEY, Flag.create("line"), Episode.create("57", "ep57")));
        assertFalse(a.matches(KEY, Flag.create("other"), Episode.create("58", "ep58")));
        assertFalse(a.matches("other", Flag.create("line"), Episode.create("58", "ep58")));
    }
}
