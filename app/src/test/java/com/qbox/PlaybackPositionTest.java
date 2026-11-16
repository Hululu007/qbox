package com.qbox;

import static org.junit.Assert.assertEquals;

import androidx.media3.common.C;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.playback.vod.EpisodeProgress;
import com.fongmi.android.tv.playback.vod.VodHistoryPolicy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * 播放进度回归（GS-79 / GS-80）：
 * - 「最近观看」用的 History.position 与「单集进度」是两份独立数据；
 * - 每集各自记住进度：58 集 5:00，切 57 集从头，切回 58 集仍 5:00；
 * - 已播完的集再次点击必须从「头」重播。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PlaybackPositionTest {

    private static final String VOD = "site@@@vod123@@@1";

    private final VodHistoryPolicy policy = new VodHistoryPolicy();

    @Before
    public void reset() {
        EpisodeProgress.clear();
    }

    private static History history(String episodeUrl, String remarks) {
        History h = new History();
        h.setKey(VOD);
        h.setEpisodeUrl(episodeUrl);
        h.setVodRemarks(remarks);
        return h;
    }

    private static void progress(String url, long position, long duration) {
        EpisodeProgress.put(VOD, EpisodeProgress.episodeKey(url, ""), position, duration);
    }

    @Test
    public void nullHistoryStartsFromBeginning() {
        assertEquals(C.TIME_UNSET, policy.startPositionMs(null));
    }

    @Test
    public void midEpisodeResumesAtPosition() {
        progress("ep58", 5000, 10000);
        assertEquals(5000, policy.startPositionMs(history("ep58", "58")));
    }

    @Test
    public void nearEndIsTreatedAsFinished() {
        progress("ep58", 9500, 10000);
        assertEquals(C.TIME_UNSET, policy.startPositionMs(history("ep58", "58")));
    }

    @Test
    public void exactlyEndedRestarts() {
        progress("ep58", 10000, 10000);
        assertEquals(C.TIME_UNSET, policy.startPositionMs(history("ep58", "58")));
    }

    @Test
    public void pastEndRestarts() {
        progress("ep58", 12000, 10000);
        assertEquals(C.TIME_UNSET, policy.startPositionMs(history("ep58", "58")));
    }

    /** opening 属于起播策略层，不再混进 VodHistoryPolicy 的每集续播位置（GS-81②）。 */
    @Test
    public void resumePositionIgnoresOpening() {
        progress("ep58", 5000, 60000);
        History h = history("ep58", "58");
        h.setOpening(30000);
        assertEquals(5000, policy.startPositionMs(h));
    }

    @Test
    public void freshEpisodeStartsFromBeginning() {
        assertEquals(C.TIME_UNSET, policy.startPositionMs(history("ep58", "58")));
    }

    /** 核心：History.position 只是最近观看展示快照，不能驱动起播（GS-80 两部分分离）。 */
    @Test
    public void displaySnapshotDoesNotDrivePlayback() {
        History h = history("ep58", "58");
        h.setPosition(5000);
        h.setDuration(60000);
        assertEquals(C.TIME_UNSET, policy.startPositionMs(h));
    }

    /** 核心：58 集 5:00 → 切 57 集从头 → 切回 58 集仍 5:00。 */
    @Test
    public void eachEpisodeKeepsItsOwnProgress() {
        History h = history("ep58", "58");
        progress("ep58", 5000, 60000);
        assertEquals(5000, policy.startPositionMs(h));

        policy.switchEpisode(h, Flag.create("line"), Episode.create("57", "ep57"));
        assertEquals(C.TIME_UNSET, policy.startPositionMs(h));
        assertEquals(C.TIME_UNSET, h.getPosition()); // 最近观看展示快照同步归零

        progress("ep57", 2000, 60000);
        assertEquals(2000, policy.startPositionMs(h));

        policy.switchEpisode(h, Flag.create("line"), Episode.create("58", "ep58"));
        assertEquals(5000, policy.startPositionMs(h));
        assertEquals(5000, h.getPosition());
    }

    /** 切集不得把旧集进度写回 History，也不得串到新集。 */
    @Test
    public void secondUpdateAfterSwitchStaysCleared() {
        History h = history("ep58", "58");
        progress("ep58", 5000, 60000);
        Episode next = Episode.create("57", "ep57");
        policy.switchEpisode(h, Flag.create("line"), next);
        policy.updateEpisode(h, Flag.create("line"), next);
        assertEquals(C.TIME_UNSET, policy.startPositionMs(h));
    }

    /** 同集刷新/恢复仍要能续播，不能被「两部分分离」破坏。 */
    @Test
    public void sameEpisodeUpdateKeepsProgress() {
        History h = history("ep58", "58");
        progress("ep58", 5000, 60000);
        policy.updateEpisode(h, Flag.create("line"), Episode.create("58", "ep58"));
        assertEquals(5000, policy.startPositionMs(h));
    }

    /** 已播完的集，切走再切回仍从头播（GS-79⑧ 与每集进度互不冲突）。 */
    @Test
    public void switchingFromFinishedEpisodeStartsFromBeginning() {
        History h = history("ep58", "58");
        progress("ep58", 60000, 60000);
        policy.switchEpisode(h, Flag.create("line"), Episode.create("57", "ep57"));
        assertEquals(C.TIME_UNSET, policy.startPositionMs(h));
        policy.switchEpisode(h, Flag.create("line"), Episode.create("58", "ep58"));
        assertEquals(C.TIME_UNSET, policy.startPositionMs(h));
    }

    /** 播放器周期回调必须把进度写进「单集进度」存储。 */
    @Test
    public void playbackProgressIsRecordedPerEpisode() {
        History h = history("ep58", "58");
        h.markSaveScheduled(); // 阻止 saveCurrent 落库，测试不依赖 Room
        policy.updateProgress(h, System.currentTimeMillis(), 5000, 60000);
        assertEquals(5000, EpisodeProgress.position(VOD, "ep58"));
        assertEquals(5000, policy.startPositionMs(h));
    }

    /**
     * 上游守卫回归（GS-82，log §50）：播放器发出「未就绪」进度（position&lt;0 / duration&lt;=0）时，
     * updateProgress 必须整体返回。旧实现虽有 applyProgress 内部守卫不写数据，但仍会用 History 里
     * 上一帧的旧值触发一次冗余 saveCurrent（连带 Room 写入 + markSaveScheduled 吃掉 5s 调度窗口）。
     * 用 canScheduleSave() 作探针：守卫生效则 saveTime 不变，仍为 true。
     */
    @Test
    public void invalidProgressDoesNotScheduleStaleSave() {
        History h = history("ep58", "58");
        h.setPosition(5000);
        h.setDuration(60000); // canSave() 为 true；saveTime=0 -> canScheduleSave() 为 true
        assertEquals(true, h.canSave());
        assertEquals(true, h.canScheduleSave());
        policy.updateProgress(h, System.currentTimeMillis(), -1, 60000);
        policy.updateProgress(h, System.currentTimeMillis(), 5000, 0);
        assertEquals(true, h.canScheduleSave()); // 守卫返回，未 markSaveScheduled
        assertEquals(C.TIME_UNSET, EpisodeProgress.position(VOD, "ep58")); // 未写入脏进度
    }

    /** 重播只清当前集，不影响其它集已存进度。 */
    @Test
    public void replayClearsCurrentEpisodeOnly() {
        progress("ep58", 5000, 60000);
        progress("ep57", 2000, 60000);
        EpisodeProgress.remove(VOD, EpisodeProgress.episodeKey("ep58", ""));
        assertEquals(C.TIME_UNSET, policy.startPositionMs(history("ep58", "58")));
        assertEquals(2000, policy.startPositionMs(history("ep57", "57")));
    }

    /** 删一部剧的历史，该剧所有集进度一并清除。 */
    @Test
    public void removingVodClearsAllItsEpisodes() {
        progress("ep58", 5000, 60000);
        progress("ep57", 2000, 60000);
        EpisodeProgress.removeVod(VOD);
        assertEquals(C.TIME_UNSET, policy.startPositionMs(history("ep58", "58")));
        assertEquals(C.TIME_UNSET, policy.startPositionMs(history("ep57", "57")));
    }

    /** 旧版本遗留在 History.position 的进度升级时迁移一次，且不被重复覆盖。 */
    @Test
    public void legacyHistoryProgressMigratesOnce() {
        EpisodeProgress.migrate(VOD, "ep58", 5000, 60000);
        assertEquals(5000, policy.startPositionMs(history("ep58", "58")));
        progress("ep58", 9000, 60000);
        EpisodeProgress.migrate(VOD, "ep58", 5000, 60000);
        assertEquals(9000, policy.startPositionMs(history("ep58", "58")));
    }
}
