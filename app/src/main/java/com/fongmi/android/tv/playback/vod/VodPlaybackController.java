package com.fongmi.android.tv.playback.vod;

import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.playback.PlaybackResult;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.EpisodeInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VodPlaybackController {

    private static final String PUSH_PREFIX = "push://";
    private static final String SEARCH_PREFIX = "msearch:";

    private final VodPlaybackHost host;
    private final VodDataSource dataSource;
    private final VodPlaybackState state;
    private final VodHistoryPolicy historyPolicy;
    private final VodFallbackPolicy fallbackPolicy;
    private final VodPreloader preloader;
    private History lastHistory;

    public VodPlaybackController(VodPlaybackHost host, VodDataSource dataSource, VodPlaybackState state) {
        this.host = host;
        this.dataSource = dataSource;
        this.state = state;
        this.historyPolicy = new VodHistoryPolicy();
        this.fallbackPolicy = new VodFallbackPolicy(this, state, host, dataSource);
        this.preloader = new VodPreloader(host, dataSource, state);
    }

    public void reset() {
        preloader.clear();
        state.reset();
    }

    public void checkId() {
        String id = resolveVodId();
        if (id.isEmpty() || id.startsWith(SEARCH_PREFIX)) detailEmpty(false);
        else if (!state.isDetailRequested(host.getVodKey(), id)) requestDetail();
    }

    private String resolveVodId() {
        String id = host.getVodId();
        if (!id.startsWith(PUSH_PREFIX)) return id;
        host.usePushId(id.substring(PUSH_PREFIX.length()));
        return host.getVodId();
    }

    public void requestDetail() {
        String key = host.getVodKey();
        String id = host.getVodId();
        state.setDetailRequest(key, id);
        dataSource.detailContent(key, id);
    }

    public void onDetailResult(VodDetailResult detail) {
        if (detail == null || !detail.matches(host.getVodKey(), host.getVodId())) return;
        Result result = detail.result();
        if (result.getList().isEmpty()) detailEmpty(result.hasMsg());
        else detailLoaded(result.getVod());
        host.showDetailMessage(result.getMsg());
    }

    public void updateVod(Vod item) {
        if (preloader.isRequestPending()) return;
        History history = state.getHistory();
        replaceVodId(history, item.getId());
        mergeFlags(item.getFlags());
        updateHistory(history, item);
        host.renderVodUpdate(item);
        publishActivePlaybackMetadata();
    }

    private void updateHistory(History history, Vod item) {
        String pic = item.getPic();
        String name = item.getName();
        boolean hasPic = !pic.isEmpty();
        boolean hasName = !name.isEmpty();
        if (hasPic) history.setVodPic(pic);
        if (hasName) history.setVodName(name);
        if (hasName || hasPic) historyPolicy.saveCurrent(history);
    }

    private void publishActivePlaybackMetadata() {
        Episode episode = state.findEpisode(host.getVodKey(), state.getActiveRequest());
        if (episode != null) publishPlaybackMetadata(episode);
    }

    private void replaceVodId(History history, String id) {
        if (id.isEmpty() || id.equals(host.getVodId())) return;
        String oldKey = host.getHistoryKey();
        host.setVodId(id);
        String newKey = host.getHistoryKey();
        history.replace(newKey);
        Keep.replace(oldKey, newKey);
    }

    public void onPlaybackResult(PlaybackResult<VodPlayRequest> playback) {
        if (playback == null || cannotApply(playback)) return;
        applyPlaybackResult(playback.result(), playback.request());
    }

    private void applyPlaybackResult(Result result, VodPlayRequest request) {
        Episode episode = state.findEpisode(host.getVodKey(), request);
        if (episode == null) return;
        applyPlaybackState(result, request);
        renderPlaybackResult(result);
        updatePlaybackPosition(result, request);
        host.loadDanmaku(result, state.getHistory(), episode);
        // 起播位置优先用「请求里定死的值」，未指定才回落到 startPositionMs()（每集独立进度）。
        startPlayback(result, request.getStartPositionMs() == C.TIME_UNSET ? startPositionMs() : request.getStartPositionMs(), episode);
        preloader.update(result);
    }

    private void applyPlaybackState(Result result, VodPlayRequest request) {
        state.setQuality(result);
        state.setPlayingRequest(request);
        state.setUseParse(result.isUseParse());
        result.getUrl().set(state.getQualityPosition());
        if (request.getStartPositionMs() != C.TIME_UNSET && state.getHistory() != null) {
            state.getHistory().setPosition(request.getStartPositionMs());
        }
    }

    private void renderPlaybackResult(Result result) {
        host.renderUseParse(state.isUseParse());
        host.renderQuality(result, result.getUrl().isMulti());
        if (result.hasDesc()) host.renderDescription(result.getDesc());
        if (result.hasArtwork()) host.renderArtwork(result.getArtwork());
    }

    private void updatePlaybackPosition(Result result, VodPlayRequest request) {
        if (!result.hasPosition()) return;
        // 请求已显式指定起点时，源站下发的 position 不得覆盖（对齐上游优先级 request > result）。
        if (request.getStartPositionMs() != C.TIME_UNSET) return;
        History history = state.getHistory();
        if (history == null || Setting.isIncognito()) return;
        long start = result.getPosition();
        // 内容源下发的起播点：仅当该集还没有本地进度时才作为初始值写入，
        // 不覆盖用户已经看过的进度。duration 未知（TIME_UNSET），播放后由 applyProgress 补上。
        EpisodeProgress.migrate(history.getKey(), EpisodeProgress.episodeKey(history.getEpisodeUrl(), history.getVodRemarks()), start, C.TIME_UNSET);
        history.setPosition(start);
    }

    private void startPlayback(Result result, long startPositionMs, Episode episode) {
        MediaMetadata metadata = publishPlaybackMetadata(episode);
        host.startPlayback(result, state.isUseParse(), startPositionMs, metadata);
    }

    public void onSearchResult(Result result) {
        fallbackPolicy.onSearchResult(result);
    }

    public void selectFlag(Flag item) {
        selectFlag(item, false);
    }

    private void selectFlag(Flag item, boolean force) {
        if (!state.hasFlags()) return;
        Flag selected = resolveFlag(item);
        if (!force && selected.isSelected()) return;
        preloader.clear();
        for (Flag flag : state.getFlags()) flag.setSelected(selected);
        // GS-70：渲染前按「时间 > 集数 > 源顺序」应用默认排序（正序升序），播放顺序跟随显示顺序。
        sortEpisodes(selected, isRevSort());
        host.renderFlagSelection(selected);
        host.renderEpisodes(selected.getEpisodes());
        host.renderQualityVisible(false);
        seamless(selected);
    }

    public void selectEpisode(Episode item) {
        if (!state.hasFlags()) return;
        saveCurrentHistory();
        Flag selected = state.getFlag();
        for (Flag flag : state.getFlags()) flag.toggle(flag == selected, item);
        // 用户主动换集：显式同步新集进度，不再依赖 updateEpisode 的 name-match 猜测。
        historyPolicy.switchEpisode(state.getHistory(), state.getFlag(), item);
        host.renderEpisodeSelection(item);
        if (host.isFullscreenForPlayback()) host.showEpisodeReady(item);
        playEpisode(item);
    }

    private void playEpisode(Episode item) {
        // 不能在这里再 saveCurrentHistory()：此时播放器仍停在「旧集」，
        // getPlayerPosition() 会把旧集进度重新写回刚被 switchEpisode 归零的 History，
        // 导致新集沿用旧集位置（所有集共享一个进度）。旧集进度已在 selectEpisode 开头保存。
        Result result = preloader.consume(item);
        host.stopPlaybackForRefresh();
        if (result == null) requestSelectedEpisode();
        else applyPlaybackResult(result, VodPlayRequest.create(host.getVodKey(), state.getFlag(), item));
    }

    public void selectQuality(Result result) {
        if (!state.hasEpisode()) return;
        state.setQuality(result);
        state.setQualityPosition(result.getUrl().getPosition());
        preloader.clear();
        startPlayback(result, host.getPlayerPosition(), state.getEpisode());
        preloader.preloadNext();
    }

    public void selectParse(Parse item) {
        VodConfig.get().setParse(item);
        refresh();
    }

    private void mergeFlags(List<Flag> items) {
        if (items.isEmpty()) return;
        if (state.hasFlags()) {
            Flag activated = state.getFlag();
            for (Flag item : items) mergeFlag(activated, item);
            host.renderFlags(state.getFlags());
            // FlagAdapter.addAll 会丢失 RecyclerView 的 selectedPosition；这里把当前线路重新选中，
            // 否则详情页在 Vod 更新后视觉上会落回第一项（典型：切到备用线路后跳回默认线路）。
            host.renderFlagSelection(state.getFlag());
        } else {
            state.setFlags(items);
            host.renderFlags(state.getFlags());
            host.renderFlagSelection(state.getFlag());
        }
    }

    public void selectSource(Vod item) {
        switchSource(item, false);
    }

    void fallbackSource(Vod item) {
        switchSource(item, true);
    }

    private void switchSource(Vod item, boolean autoFallback) {
        state.setAutoFallback(autoFallback);
        saveCurrentHistory();
        preloader.clear();
        state.clearPlayRequest();
        host.prepareSource(item);
        requestDetail();
    }

    public void search(String keyword) {
        fallbackPolicy.search(keyword, false);
    }

    public void manualSwitchSource() {
        fallbackPolicy.manualSwitchSource();
    }

    public void playbackError(String msg) {
        preloader.clear();
        host.resetPlaybackForError(msg);
        fallbackPolicy.playbackError();
    }

    public void playbackEnded() {
        nextEpisode(true);
    }

    public void replay() {
        History history = state.getHistory();
        if (history != null) {
            EpisodeProgress.remove(history.getKey(), EpisodeProgress.episodeKey(history.getEpisodeUrl(), history.getVodRemarks()));
            history.setPosition(C.TIME_UNSET);
            history.setDuration(C.TIME_UNSET);
        }
        if (host.isPlayerEmpty()) refresh();
        else host.replay(startPositionMs());
    }

    public void refresh() {
        saveCurrentHistory();
        preloader.clear();
        host.stopPlaybackForRefresh();
        restorePlaybackSelection(state.getPlayingRequest());
        requestSelectedEpisode();
    }

    public void onPlaybackServiceReady() {
        MediaMetadata metadata = state.getPlaybackMetadata();
        if (metadata != null) host.renderPlaybackMetadata(metadata);
        VodPlayRequest request = state.getPlayingRequest();
        if (request == null || host.hasPlaybackSession()) return;
        restorePlaybackSelection(request);
        requestSelectedEpisode();
    }

    private void requestSelectedEpisode() {
        if (state.hasEpisode()) requestPlayer(state.getFlag(), state.getEpisode());
    }

    public void nextEpisode(boolean notify) {
        boolean reversed = state.getHistory() != null && state.getHistory().isRevPlay();
        moveEpisode(reversed ? -1 : 1, notify, reversed);
    }

    public void prevEpisode(boolean notify) {
        boolean reversed = state.getHistory() != null && state.getHistory().isRevPlay();
        moveEpisode(reversed ? 1 : -1, notify, reversed);
    }

    private void moveEpisode(int offset, boolean notify, boolean reversed) {
        if (!state.hasEpisode()) return;
        Episode item = state.getRelativeEpisode(offset);
        if (!item.isSelected()) selectEpisode(item);
        else if (notify && offset > 0) host.showNoNext(reversed);
        else if (notify) host.showNoPrev(reversed);
    }

    public void reverseEpisode(boolean scroll) {
        if (!state.hasFlags()) return;
        for (Flag flag : state.getFlags()) reverseEpisodes(flag);
        host.renderReverseEpisodes(state.getFlag().getEpisodes(), scroll);
    }

    private void reverseEpisodes(Flag flag) {
        List<Episode> episodes = flag.getEpisodes();
        int position = flag.getPosition();
        Collections.reverse(episodes);
        if (position >= 0 && position < episodes.size()) flag.setPosition(episodes.size() - position - 1);
    }

    /**
     * 选集排列：能识别出时间信息的按时间排（正序升序 / 倒序降序），
     * 识别不出的按原序（倒序则整体反过来）。排列后同步播放顺序与选中集位置。
     */
    public void sortEpisodes(boolean desc) {
        if (!state.hasFlags()) return;
        for (Flag flag : state.getFlags()) sortEpisodes(flag, desc);
        host.renderEpisodes(state.getFlag().getEpisodes());
    }

    private void sortEpisodes(Flag flag, boolean desc) {
        List<Episode> episodes = flag.getEpisodes();
        if (episodes.size() < 2) return;
        for (int i = 0; i < episodes.size(); i++) if (episodes.get(i).getIndex() < 0) episodes.get(i).setIndex(i);
        int position = flag.getPosition();
        Episode selected = position >= 0 && position < episodes.size() ? episodes.get(position) : null;
        // 始终从「源原始顺序」重排，保证倒序再切回正序能恢复；不是拿当前顺序二次反转。
        List<Episode> base = new ArrayList<>(episodes);
        base.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
        List<String> names = new ArrayList<>();
        for (Episode episode : base) names.add(episode.getDesc() + " " + episode.getName());
        int[] order = EpisodeInfo.order(names, desc);
        List<Episode> sorted = new ArrayList<>(episodes.size());
        for (int index : order) sorted.add(base.get(index));
        episodes.clear();
        episodes.addAll(sorted);
        if (selected != null) flag.toggle(true, selected);
    }

    private void saveCurrentHistory() {
        if (state.getPlayingRequest() == null || !host.canTrackPlaybackProgress()) historyPolicy.save(currentHistory());
        else saveHistory(false, System.currentTimeMillis(), host.getPlayerPosition(), host.getPlayerDuration());
    }

    public void saveHistory(boolean exit, long time, long position, long duration) {
        History history = exit ? historyForExit() : currentHistory();
        if (host.isLivePlayback()) historyPolicy.saveVisit(history, exit, time);
        else historyPolicy.saveProgress(history, exit, time, position, duration);
    }

    public void onTimeChanged(long time, long position, long duration) {
        // 守卫对齐 FongMi：无在播请求直接忽略；片尾判定必须 duration>0（未就绪不得跳集）。
        if (state.getPlayingRequest() == null) return;
        History history = currentHistory();
        historyPolicy.updateProgress(history, time, position, duration);
        if (VodSkipPolicy.isEndingReached(history, position, duration)) nextEpisode(false);
    }

    /** 起播 = 每集续播位置（VodHistoryPolicy）+ 片头/跳过片段策略（VodSkipPolicy，FongMi 上游分层）。 */
    public long startPositionMs() {
        History history = state.getHistory();
        long resume = historyPolicy.startPositionMs(history);
        return VodSkipPolicy.startPositionMs(history, state.getQuality(), resume);
    }

    private History currentHistory() {
        History history = state.getHistory();
        if (history != null) lastHistory = history;
        return history;
    }

    private History historyForExit() {
        History history = currentHistory();
        return history == null ? lastHistory : history;
    }

    public void onPreloadResult(PlaybackResult<VodPlayRequest> preload) {
        preloader.onResult(preload);
    }

    public void setOpening(long opening) {
        if (state.getHistory() != null) state.getHistory().setOpening(opening);
    }

    public void setEnding(long ending) {
        if (state.getHistory() != null) state.getHistory().setEnding(ending);
    }

    public void setScale(int scale) {
        if (state.getHistory() != null) state.getHistory().setScale(scale);
    }

    public void setRevSort(boolean revSort) {
        if (state.getHistory() != null) state.getHistory().setRevSort(revSort);
    }

    public void setRevPlay(boolean revPlay) {
        if (state.getHistory() != null) state.getHistory().setRevPlay(revPlay);
    }

    private void detailEmpty(boolean shouldFinish) {
        if (host.isFromCollect() || shouldFinish) host.finishVod();
        else showDetailFallback();
    }

    private void showDetailFallback() {
        String name = host.getVodName();
        if (name.isEmpty()) host.renderEmptyDetail();
        else fallbackDetail(name);
    }

    private void fallbackDetail(String name) {
        host.renderFallbackName(name);
        host.onDetailFallbackScheduled();
        fallbackPolicy.emptyDetail();
    }

    private void detailLoaded(Vod item) {
        VodPlayRequest activeRequest = state.getActiveRequest();
        // 首次打开详情时 state 里还没有 history；此时 history 才是「最近观看」的权威状态。
        // 刷新/换源后 state 已有 history + request，继续以正在使用的 request 优先。
        boolean freshDetail = state.getHistory() == null;
        History history = applyDetail(item);
        if (state.hasFlags()) restoreDetail(activeRequest, history, freshDetail);
        else fallbackPolicy.emptyFlag();
    }

    private History applyDetail(Vod item) {
        item.checkPic(host.getVodPic());
        item.checkName(host.getVodName());
        List<Flag> flags = item.getFlags();
        state.setFlags(flags);
        History history = historyPolicy.findOrCreate(host.getHistoryKey(), host.getVodMark(), item);
        state.setHistory(history);
        lastHistory = history;
        host.renderDetail(item, history);
        host.renderFlags(flags);
        host.renderHistory(history);
        host.onDetailFallbackCancelled();
        return history;
    }

    private void restoreDetail(VodPlayRequest activeRequest, History history, boolean freshDetail) {
        // 从「最近观看」进入时，history 里保存的 vodFlag/集数就是用户上次的选择。
        // 先按 history 精确恢复，避免一个携带旧线路的 activeRequest（或首个 Flag）把用户
        // 上次选择的线路覆盖回列表第一项；刷新/换源再走 activeRequest 路径。
        boolean restoredHistory = freshDetail && restoreHistorySelection(history);
        if (restoredHistory) {
            publishPlaybackMetadata(state.getEpisode());
        } else {
            restoreDetailSelection(activeRequest, history);
        }
        restoreEpisodeOrder(history);
        resumePlaybackIfNeeded(restoredHistory);
    }

    private void restoreDetailSelection(VodPlayRequest activeRequest, History history) {
        if (restorePlaybackSelection(activeRequest)) {
            publishPlaybackMetadata(state.getEpisode());
        } else {
            selectFlag(history.getFlag(), true);
        }
    }

    private void restoreEpisodeOrder(History history) {
        if (history.isRevSort()) sortEpisodes(true);
    }

    /** 当前历史的倒序标志（没有历史 = 正序升序，GS-70：默认就能从 1 看到尾）。 */
    private boolean isRevSort() {
        return state.getHistory() != null && state.getHistory().isRevSort();
    }

    /**
     * 首次从「最近观看」进入详情时，state 里还没有 playingRequest（这是正常的），
     * 但 history 已经把线路和集恢复好了。旧逻辑只认 playingRequest，导致播放器一直空转，
     * 必须手动暂停/播放或换集才真正发起请求。历史恢复成功时也要主动起播一次。
     */
    private void resumePlaybackIfNeeded(boolean restoredHistory) {
        if (host.hasPlaybackSession()) return;
        if (restoredHistory || state.getPlayingRequest() != null) requestSelectedEpisode();
    }

    private boolean restoreHistorySelection(History history) {
        if (history == null) return false;
        Flag flag = findFlag(history.getFlag());
        if (flag == null || flag.getEpisodes().isEmpty()) return false;
        Episode episode = findHistoryEpisode(flag, history);
        if (episode == null) return false;
        applySelection(flag, episode);
        return true;
    }

    private Episode findHistoryEpisode(Flag flag, History history) {
        if (history == null) return null;
        String url = history.getEpisodeUrl();
        if (!url.isEmpty()) for (Episode episode : flag.getEpisodes()) if (url.equals(episode.getUrl())) return episode;
        // URL 在源更新后可能变化；再用历史里的集名做严格匹配，最后才宽松回落到位置/首集。
        Episode episode = flag.find(history.getVodRemarks(), true);
        return episode != null ? episode : flag.find(history.getVodRemarks(), false);
    }

    private boolean restorePlaybackSelection(VodPlayRequest request) {
        Flag flag = state.findFlag(host.getVodKey(), request);
        Episode episode = state.findEpisode(host.getVodKey(), flag, request);
        if (flag == null || episode == null) return false;
        applySelection(flag, episode);
        return true;
    }

    private void applySelection(Flag flag, Episode episode) {
        if (flag == null || episode == null) return;
        for (Flag item : state.getFlags()) item.setSelected(flag);
        // GS-70：历史恢复路径同样套默认排序，保证显示顺序与播放顺序一致。
        sortEpisodes(flag, isRevSort());
        for (Flag item : state.getFlags()) item.toggle(item == flag, episode);
        historyPolicy.updateEpisode(state.getHistory(), flag, episode);
        host.renderFlagSelection(flag);
        host.renderEpisodes(flag.getEpisodes());
        host.renderEpisodeSelection(episode);
        host.renderQualityVisible(state.getQuality().getUrl().isMulti());
    }

    private void requestPlayer(Flag flag, Episode episode) {
        historyPolicy.updateEpisode(state.getHistory(), flag, episode);
        VodPlayRequest request = VodPlayRequest.create(host.getVodKey(), flag, episode);
        state.setPendingRequest(request);
        publishPlaybackMetadata(episode);
        dataSource.playerContent(request);
        host.onPlaybackRequested();
    }

    private MediaMetadata publishPlaybackMetadata(Episode episode) {
        MediaMetadata metadata = VodPlaybackMedia.metadata(state.getHistory(), episode);
        state.setPlaybackMetadata(metadata);
        host.renderPlaybackMetadata(metadata);
        return metadata;
    }

    private void seamless(Flag flag) {
        History history = state.getHistory();
        Episode episode = history == null ? null : flag.find(history.getVodRemarks(), host.getVodMark().isEmpty());
        // 手动切线路时，历史里的集名通常来自另一路；不能因为找不到同名集就只改视觉选中、
        // 把历史线路留在旧路（实机表现为切到备用线路后退出再进又回到默认线路）。
        // 找不到同名集就取该线路的当前/首集，至少把线路和该集一起写回历史。
        if (episode == null && !flag.getEpisodes().isEmpty()) episode = flag.getEpisodes().get(0);
        if (episode == null) return;
        host.renderQualityVisible(episode.isSelected() && state.getQuality().getUrl().isMulti());
        if (episode.isSelected()) {
            historyPolicy.updateEpisode(history, flag, episode);
            return;
        }
        history.setVodRemarks(episode.getName());
        selectEpisode(episode);
    }

    private void mergeFlag(Flag activated, Flag item) {
        Flag target = findFlag(item);
        if (target == null) {
            state.getFlags().add(item);
        } else {
            target.mergeEpisodes(item.getEpisodes(), state.getHistory() != null && state.getHistory().isRevSort());
            if (target.equals(activated)) {
                sortEpisodes(target, isRevSort());
                host.renderEpisodes(target.getEpisodes());
            }
        }
    }

    private Flag resolveFlag(Flag item) {
        Flag flag = findFlag(item);
        if (flag != null) return flag;
        return state.getFlags().get(0);
    }

    private Flag findFlag(Flag item) {
        if (item == null) return null;
        for (Flag flag : state.getFlags()) if (flag.equals(item)) return flag;
        // 某些源/历史会在名称前后带空格；历史恢复不能被这种差异悄悄降级到第一个 Flag。
        String name = item.getFlag() == null ? "" : item.getFlag().trim();
        if (!name.isEmpty()) for (Flag flag : state.getFlags()) {
            if (flag.getFlag() != null && name.equals(flag.getFlag().trim())) return flag;
        }
        return null;
    }

    private boolean cannotApply(PlaybackResult<VodPlayRequest> playback) {
        VodPlayRequest pending = state.getPendingRequest();
        VodPlayRequest request = playback.request();
        if (host.isHostFinishing() || pending == null || request == null) return true;
        return !pending.matches(request) || state.findEpisode(host.getVodKey(), request) == null || !request.accepts(playback.result());
    }
}
