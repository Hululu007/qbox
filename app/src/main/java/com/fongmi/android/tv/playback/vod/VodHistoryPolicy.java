package com.fongmi.android.tv.playback.vod;

import android.text.TextUtils;

import androidx.media3.common.C;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.Task;

public class VodHistoryPolicy {

    public History findOrCreate(String key, String mark, Vod item) {
        History history = History.find(key);
        history = history == null ? create(key, item) : history;
        if (!TextUtils.isEmpty(mark)) history.setVodRemarks(mark);
        if (Setting.isIncognito()) history.delete();
        history.setVodName(item.getName());
        // 旧版本把进度只存在 History.position 里；升级后一次性迁移到「单集进度」，
        // 之后 History.position 退化为「最近观看」展示快照，不再是起播依据。
        EpisodeProgress.migrate(history.getKey(), EpisodeProgress.episodeKey(history.getEpisodeUrl(), history.getVodRemarks()), history.getPosition(), history.getDuration());
        return history;
    }

    private History create(String key, Vod item) {
        History history = new History();
        history.setKey(key);
        history.setCid(VodConfig.getCid());
        history.setVodName(item.getName());
        history.findEpisode(item.getFlags());
        return history;
    }

    public void save(History history) {
        save(history, false);
    }

    public void save(History history, boolean exit) {
        if (history == null || !history.canSave() || Setting.isIncognito()) return;
        History copy = copyForSave(history);
        Task.executeSerial(() -> {
            copy.merge().save();
            if (exit) RefreshEvent.history();
        });
    }

    public void saveProgress(History history, boolean exit, long time, long position, long duration) {
        applyProgress(history, time, position, duration);
        save(history, exit);
    }

    public void saveVisit(History history, boolean exit, long time) {
        if (history == null || Setting.isIncognito()) return;
        history.setCreateTime(time);
        history.setPosition(C.TIME_UNSET);
        history.setDuration(C.TIME_UNSET);
        History copy = copyForSave(history);
        Task.executeSerial(() -> {
            copy.save();
            if (exit) RefreshEvent.history();
        });
    }

    public void saveCurrent(History history) {
        if (history == null || Setting.isIncognito()) return;
        History copy = copyForSave(history);
        Task.executeSerial(copy::save);
    }

    /**
     * 用户主动换集。进度真值在 {@link EpisodeProgress}（按集独立保存），
     * 旧集进度不丢、也不带到新集；这里只把 History.position/duration 同步成
     * 「新集」的已存进度，供首页「最近观看」进度条展示（无记录则 TIME_UNSET）。
     */
    public void switchEpisode(History history, Flag flag, Episode episode) {
        if (history == null || flag == null || episode == null) return;
        syncDisplayProgress(history, episode);
        setEpisode(history, flag, episode);
    }

    /** 恢复/重进同一集：同样把展示快照对齐到该集的实际进度。 */
    public void updateEpisode(History history, Flag flag, Episode episode) {
        if (history == null || flag == null || episode == null) return;
        syncDisplayProgress(history, episode);
        setEpisode(history, flag, episode);
    }

    private void syncDisplayProgress(History history, Episode episode) {
        String key = EpisodeProgress.episodeKey(episode.getUrl(), episode.getName());
        history.setPosition(EpisodeProgress.position(history.getKey(), key));
        history.setDuration(EpisodeProgress.duration(history.getKey(), key));
    }

    private void setEpisode(History history, Flag flag, Episode episode) {
        history.setVodFlag(flag.getFlag());
        history.setVodRemarks(episode.getName());
        history.setEpisodeUrl(episode.getUrl());
    }

    /** 守卫对齐 FongMi 上游：非法进度（未就绪的 position/duration）不写、也不触发冗余 saveCurrent。 */
    public void updateProgress(History history, long time, long position, long duration) {
        if (position < 0 || duration <= 0) return;
        applyProgress(history, time, position, duration);
        if (history != null && history.canSave() && history.canScheduleSave()) saveCurrent(history);
    }

    private void applyProgress(History history, long time, long position, long duration) {
        if (history == null || position < 0 || duration <= 0) return;
        history.setCreateTime(time);
        history.setPosition(position);
        history.setDuration(duration);
        // 进度的真值按「集」独立保存；History.position 只是最近观看展示快照。
        if (!Setting.isIncognito()) {
            EpisodeProgress.put(history.getKey(), EpisodeProgress.episodeKey(history.getEpisodeUrl(), history.getVodRemarks()), position, duration);
        }
    }

    private History copyForSave(History history) {
        history.markSaveScheduled();
        return history.copy();
    }

    /**
     * 「这一集看到哪」的续播位置（每集独立进度），不含片头策略 ——
     * 片头/自动跳过/源站起点统一由 {@link VodSkipPolicy#startPositionMs} 合成（FongMi 上游分层）。
     */
    public long startPositionMs(History history) {
        if (history == null) return C.TIME_UNSET;
        String key = EpisodeProgress.episodeKey(history.getEpisodeUrl(), history.getVodRemarks());
        long position = EpisodeProgress.position(history.getKey(), key);
        long duration = EpisodeProgress.duration(history.getKey(), key);
        // GS-79：已播完（位置到/接近结尾）→ 从头重播。否则点已播完的当前集会从结尾续播，
        // 瞬间又触发 playbackEnded → 自动下一集，导致「想重看某一集永远点不进去」。
        if (duration > 0 && position >= duration - 1000) return C.TIME_UNSET;
        return position;
    }
}
