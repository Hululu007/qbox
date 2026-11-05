package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.ui.PlayerSeekView;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.DanmakuApi;
import com.fongmi.android.tv.api.SiteApi;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityVideoBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.CustomTarget;
import com.fongmi.android.tv.model.VideoViewModel;
import com.fongmi.android.tv.playback.PlaybackAction;
import com.fongmi.android.tv.playback.PlaybackIntent;
import com.fongmi.android.tv.playback.PlaybackReset;
import com.fongmi.android.tv.playback.PlaybackResult;
import com.fongmi.android.tv.playback.vod.VodDetailResult;
import com.fongmi.android.tv.playback.vod.VodPlayRequest;
import com.fongmi.android.tv.playback.vod.VodPlaybackController;
import com.fongmi.android.tv.playback.vod.VodPlaybackHost;
import com.fongmi.android.tv.playback.vod.VodPlaybackMedia;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.setting.DanmakuSetting;
import com.fongmi.android.tv.setting.DecodeSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.setting.SpeedSetting;
import com.fongmi.android.tv.ui.adapter.ArrayAdapter;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.adapter.FlagAdapter;
import com.fongmi.android.tv.ui.adapter.PartAdapter;
import com.fongmi.android.tv.ui.adapter.QualityAdapter;
import com.fongmi.android.tv.ui.adapter.QuickAdapter;
import com.fongmi.android.tv.ui.custom.CustomKeyDownVod;
import com.fongmi.android.tv.ui.custom.CustomMovement;
import com.fongmi.android.tv.ui.custom.CustomUpDownView;
import com.fongmi.android.tv.ui.dialog.ChapterDialog;
import com.fongmi.android.tv.ui.dialog.ContentDialog;
import com.fongmi.android.tv.ui.dialog.DanmakuDialog;
import com.fongmi.android.tv.ui.dialog.EditionDialog;
import com.fongmi.android.tv.ui.dialog.ParseDialog;
import com.fongmi.android.tv.ui.dialog.PlayerEngineDialog;
import com.fongmi.android.tv.ui.dialog.SpeedSettingDialog;
import com.fongmi.android.tv.ui.dialog.TrackDialog;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PartUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.Traffic;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class VideoActivity extends PlaybackActivity implements VodPlaybackHost, CustomKeyDownVod.Listener, ParseDialog.Listener, ArrayAdapter.OnClickListener, FlagAdapter.OnClickListener, EpisodeAdapter.OnClickListener, QualityAdapter.OnClickListener, QuickAdapter.OnClickListener, Clock.Callback {

    private ActivityVideoBinding mBinding;
    private VideoViewModel mViewModel;
    private VodPlaybackController mVod;
    private FlagAdapter mFlagAdapter;
    private EpisodeAdapter mEpisodeAdapter;
    private java.util.List<com.fongmi.android.tv.bean.Episode> mEpisodeItems;
    private QualityAdapter mQualityAdapter;
    private ArrayAdapter mArrayAdapter;
    private PartAdapter mPartAdapter;
    private QuickAdapter mQuickAdapter;
    private ViewGroup.LayoutParams mFrameParams;
    private Drawable mVideoBg;
    private CustomKeyDownVod mKeyDown;
    private Clock mClock;
    private View mFocus1;
    private View mFocus2;
    private Runnable mR1;
    private Runnable mR2;
    private Runnable mR3;
    private Runnable mR4;
    private Runnable mR5;
    private History mHistory;
    private boolean fullscreen;
    private boolean useParse;
    /** 快进复用的控制条：一次 10s，停手 1.8s 自动收起。 */
    private static final long SEEK_STEP_MS = 10000;

    private boolean seeking;
    private boolean seekFocus;
    private long seekBase;
    private long lastSeekKeyTick;
    private long seekHoldStartTick;
    /**
     * 用户刚按下的播放意图。MediaController 的 playWhenReady 是跨进程异步回传的，
     * 点完立刻读会读到上一拍的状态 —— 按钮文字就会「慢一次」，暂停时仍写暂停、播放时仍写播放。
     * 按下时先乐观地翻到目标状态，声音/画面状态回传（或 800ms 兜底）后再交还给真实状态。
     */
    private Boolean mPlayIntent;
    private int mPlayIntentSeq;
    private final Runnable mHideControl = this::hideControl;

    /** 播放器 buffering/ready 时会把焦点甩回画面层，这里按需把焦点钉回进度条。 */
    private final Runnable mRestoreSeekFocus = () -> {
        if (!seekFocus || mBinding == null) return;
        if (!isVisible(mBinding.control.getRoot())) return;
        if (!mBinding.control.seekBox.hasFocus()) mBinding.control.seekBox.requestFocus();
    };

    public static void push(FragmentActivity activity, String text) {
        Uri uri = UrlUtil.uri(text);
        if (FileChooser.isValid(activity, uri)) file(activity, FileChooser.getPathFromUri(uri));
        else start(activity, Sniffer.getUrl(text));
    }

    public static void file(FragmentActivity activity, String path) {
        if (TextUtils.isEmpty(path)) return;
        String name = new File(path).getName();
        start(activity, SiteApi.PUSH, "file://" + path, name);
    }

    public static void cast(Activity activity, History history) {
        start(activity, history.getSiteKey(), history.getVodId(), history.getVodName(), history.getVodPic(), null, false, true);
    }

    public static void collect(Activity activity, String key, String id, String name, String pic) {
        start(activity, key, id, name, pic, null, true, false);
    }

    public static void start(Activity activity, String url) {
        start(activity, SiteApi.PUSH, url, url);
    }

    public static void start(Activity activity, String key, String id, String name) {
        start(activity, key, id, name, null);
    }

    public static void start(Activity activity, String key, String id, String name, String pic) {
        start(activity, key, id, name, pic, null);
    }

    public static void start(Activity activity, String key, String id, String name, String pic, String mark) {
        start(activity, key, id, name, pic, mark, false, false);
    }

    public static void start(Activity activity, String key, String id, String name, String pic, String mark, boolean collect, boolean cast) {
        Intent intent = new Intent(activity, VideoActivity.class);
        intent.putExtra("collect", collect);
        intent.putExtra("cast", cast);
        intent.putExtra("mark", mark);
        intent.putExtra("name", name);
        intent.putExtra("pic", pic);
        intent.putExtra("key", key);
        intent.putExtra("id", id);
        activity.startActivity(intent);
    }

    private boolean isCast() {
        return getIntent().getBooleanExtra("cast", false);
    }

    private String getName() {
        return Objects.toString(getIntent().getStringExtra("name"), "");
    }

    private String getPic() {
        return Objects.toString(getIntent().getStringExtra("pic"), "");
    }

    private String getMark() {
        return Objects.toString(getIntent().getStringExtra("mark"), "");
    }

    private String getKey() {
        return Objects.toString(getIntent().getStringExtra("key"), "");
    }

    private String getId() {
        return Objects.toString(getIntent().getStringExtra("id"), "");
    }

    private boolean isSameVideo(Intent intent) {
        String key = Objects.toString(intent.getStringExtra("key"), "");
        String id = Objects.toString(intent.getStringExtra("id"), "");
        return TextUtils.equals(key, getKey()) && TextUtils.equals(id, getId());
    }

    @Override
    public String getHistoryKey() {
        return getKey().concat(AppDatabase.SYMBOL).concat(getId()).concat(AppDatabase.SYMBOL) + VodConfig.getCid();
    }

    private Site getSite() {
        return VodConfig.get().getSite(getKey());
    }

    private Episode getEpisode() {
        return mEpisodeAdapter.getActivated();
    }

    private int getScale() {
        return mHistory != null && mHistory.getScale() != -1 ? mHistory.getScale() : PlayerSetting.getScale();
    }

    private void setScale(int scale) {
        mVod.setScale(scale);
        mBinding.player.setResizeMode(scale);
        mBinding.control.action.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    @Override
    public boolean isFromCollect() {
        return getIntent().getBooleanExtra("collect", false);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityVideoBinding.inflate(getLayoutInflater());
    }

    @Override
    protected PlaybackService.NavigationCallback getNavigationCallback() {
        return mNavigationCallback;
    }

    @Override
    protected PlayerView getPlayerView() {
        return mBinding.player;
    }

    @Override
    protected PlayerSeekView getSeekView() {
        return mBinding.control.seek;
    }

    @Override
    protected void onServiceConnected() {
        mVod.onPlaybackServiceReady();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (TextUtils.isEmpty(intent.getStringExtra("id")) || isSameVideo(intent)) return;
        saveHistory(true);
        mVod.reset();
        setIntent(intent);
        updateNavigationKey();
        checkId();
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        mFrameParams = mBinding.video.getLayoutParams();
        mVideoBg = mBinding.video.getBackground();
        mClock = Clock.create(mBinding.widget.clock);
        mKeyDown = CustomKeyDownVod.create(this);
        styleSeekBar();
        mR1 = this::hideControl;
        mR2 = this::updateFocus;
        mR3 = this::setTraffic;
        mR4 = this::showEmpty;
        mR5 = this::updateStats;
        setRecyclerView();
        setVideoView();
        setViewModel();
        checkCast();
        checkId();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.keep.setOnClickListener(view -> onKeep());
        mBinding.video.setOnClickListener(view -> onVideo());
        mBinding.change.setOnClickListener(view -> onChange());
        mBinding.content.setOnClickListener(view -> onContent());
        mBinding.play.setOnClickListener(view -> onPrimaryPlay());
        mBinding.control.action.text.setOnClickListener(this::onTrack);
        mBinding.control.action.audio.setOnClickListener(this::onTrack);
        mBinding.control.action.video.setOnClickListener(this::onTrack);
        mBinding.control.action.ending.setUpListener(this::onEndingAdd);
        mBinding.control.action.ending.setDownListener(this::onEndingSub);
        mBinding.control.action.opening.setUpListener(this::onOpeningAdd);
        mBinding.control.action.opening.setDownListener(this::onOpeningSub);
        mBinding.control.action.next.setOnClickListener(view -> checkNext());
        mBinding.control.action.prev.setOnClickListener(view -> checkPrev());
        mBinding.control.action.playPause.setOnClickListener(view -> onPlayPause());
        mBinding.control.action.scale.setOnClickListener(view -> onScale());
        mBinding.control.action.speed.setOnClickListener(view -> onSpeed());
        mBinding.control.action.reset.setOnClickListener(view -> onReset());
        mBinding.control.action.replay.setOnClickListener(view -> onReplay());
        mBinding.control.action.parse.setOnClickListener(view -> onParse());
        mBinding.control.action.player.setOnClickListener(view -> onPlayer());
        mBinding.control.action.decode.setOnClickListener(view -> onDecode());
        mBinding.control.action.ending.setOnClickListener(view -> onEnding());
        mBinding.control.action.repeat.setOnClickListener(view -> onRepeat());
        mBinding.control.action.danmaku.setOnClickListener(view -> onDanmaku());
        mBinding.control.action.edition.setOnClickListener(view -> onEdition());
        mBinding.control.action.chapter.setOnClickListener(view -> onChapter());
        mBinding.control.action.opening.setOnClickListener(view -> onOpening());
        mBinding.control.action.speed.setOnLongClickListener(view -> onSpeedLong());
        mBinding.control.action.speedExtra.setOnClickListener(view -> onSpeedSetting());
        mBinding.control.action.ending.setOnLongClickListener(view -> onEndingReset());
        mBinding.control.action.opening.setOnLongClickListener(view -> onOpeningReset());
        mBinding.control.action.more.setOnClickListener(view -> onMore());
        mBinding.control.action.statsButton.setOnClickListener(view -> onStats());
        mBinding.stats.getRoot().setOnClickListener(view -> onStats());
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
        mBinding.flag.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mFlagAdapter.getItemCount() > 0) onItemClick(mFlagAdapter.get(position));
            }
        });
        mBinding.episode.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (child != null && mBinding.video != mFocus1) mFocus1 = child.itemView;
            }
        });
        mBinding.array.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mEpisodeAdapter.getItemCount() > 20 && position > 1) mBinding.episode.setSelectedPosition((position - 2) * 20);
            }
        });
    }

    private void setRecyclerView() {
        mBinding.flag.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.flag.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.flag.setAdapter(mFlagAdapter = new FlagAdapter(this));
        mBinding.episode.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.episode.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.episode.setAdapter(mEpisodeAdapter = new EpisodeAdapter(this));
        mBinding.quality.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.quality.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.quality.setAdapter(mQualityAdapter = new QualityAdapter(this));
        mBinding.array.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.array.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.array.setAdapter(mArrayAdapter = new ArrayAdapter(this));
        mBinding.part.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.part.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.part.setAdapter(mPartAdapter = new PartAdapter(item -> mVod.search(item)));
        mBinding.quick.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.quick.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.quick.setAdapter(mQuickAdapter = new QuickAdapter(this));
    }

    /** 进度条配色收敛为白色系（色彩收敛 B6：原品牌蓝 0xFF4C8DFF 在纯黑影院界面属突兀彩色）。 */
    private void styleSeekBar() {
        if (mBinding.control.seek.getTimeBar() instanceof androidx.media3.ui.DefaultTimeBar bar) {
            bar.setPlayedColor(0xFFFFFFFF);
            bar.setScrubberColor(0xFFFFFFFF);
            bar.setBufferedColor(0x66FFFFFF);
            bar.setUnplayedColor(0x33FFFFFF);
        }
    }

    private void setVideoView() {
        setSeekNextFocusDown(R.id.chapter);
        setActionFocusBoundary(mBinding.control.action.main);
        PlayerEngineDialog.setText(mBinding.control.action.player);
        // QBox: 不提供弹幕入口。
        mBinding.control.action.danmaku.setVisibility(View.GONE);
    }

    private void setPlaybackMode() {
        PlaybackAction.setPlaybackMode(player(), mBinding.control.action.player, mBinding.control.action.decode);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(VideoViewModel.class);
        mVod = mViewModel.createPlaybackController(this);
        observeWhenServiceReady(mViewModel.getDetail(), this::onDetailObserved);
        observeWhenServiceReady(mViewModel.getSearch(), this::onSearchObserved);
        observeWhenServiceReady(mViewModel.getPreload(), this::onPreloadObserved);
        observeWhenServiceReady(mViewModel.getPlayback(), this::onPlaybackObserved);
    }

    private void onDetailObserved(VodDetailResult result) {
        mVod.onDetailResult(result);
    }

    private void onPlaybackObserved(PlaybackResult<VodPlayRequest> result) {
        mVod.onPlaybackResult(result);
    }

    private void onPreloadObserved(PlaybackResult<VodPlayRequest> result) {
        mVod.onPreloadResult(result);
    }

    private void onSearchObserved(Result result) {
        mVod.onSearchResult(result);
    }

    @Override
    public String getVodKey() {
        return getKey();
    }

    @Override
    public String getVodId() {
        return getId();
    }

    @Override
    public void setVodId(String id) {
        getIntent().putExtra("id", id);
    }

    @Override
    public String getVodName() {
        String name = mBinding.name.getText().toString();
        return name.isEmpty() ? getName() : name;
    }

    @Override
    public String getVodPic() {
        return getPic();
    }

    @Override
    public String getVodMark() {
        return getMark();
    }

    @Override
    public boolean isSiteChangeable() {
        return getSite().isChangeable();
    }

    @Override
    public boolean isHostFinishing() {
        return isFinishing() || isDestroyed();
    }

    @Override
    public boolean isPlayerEmpty() {
        return player().isEmpty();
    }

    @Override
    public boolean hasPlaybackSession() {
        return service() != null && isOwner() && player().hasPlaySpec();
    }

    @Override
    public boolean isFullscreenForPlayback() {
        return isFullscreen();
    }

    @Override
    public boolean isLivePlayback() {
        return service() != null && isOwner() && player().isLive();
    }

    @Override
    public boolean canTrackPlaybackProgress() {
        return service() != null && isOwner() && player().isVod();
    }

    @Override
    public boolean canPreloadNext() {
        return service() != null && isOwner() && player().canPreloadNext();
    }

    @Override
    public long getPlayerPosition() {
        return player().getPosition();
    }

    @Override
    public long getPlayerDuration() {
        return player().getDuration();
    }

    @Override
    public void usePushId(String id) {
        getIntent().putExtra("key", SiteApi.PUSH).putExtra("id", id);
    }

    @Override
    public void requestDetail(String key, String id) {
        mViewModel.detailContent(key, id);
    }

    @Override
    public void requestPlayer(VodPlayRequest request) {
        mViewModel.playerContent(request);
        showProgress();
    }

    @Override
    public void requestPreload(VodPlayRequest request) {
        mViewModel.preloadContent(request);
    }

    @Override
    public void requestSearch(List<Site> sites, String keyword) {
        mQuickAdapter.clear();
        mViewModel.searchContent(sites, keyword, true);
    }

    @Override
    public void prepareSource(Vod item) {
        getIntent().putExtra("key", item.getSiteKey());
        getIntent().putExtra("pic", item.getPic());
        getIntent().putExtra("id", item.getId());
        mBinding.scroll.scrollTo(0, 0);
        mClock.setCallback(null);
        updateNavigationKey();
        player().reset();
        player().stop();
    }

    @Override
    public void stopPlaybackForRefresh() {
        player().stop();
        player().clear();
        mClock.setCallback(null);
    }

    @Override
    public void resetPlaybackForError(String msg) {
        PlaybackReset.afterError(player(), () -> mClock.setCallback(null));
        showError(msg);
    }

    @Override
    public void replay(long position) {
        player().replay(position);
    }

    @Override
    public void startPlayback(Result result, boolean useParse, long startPositionMs, MediaMetadata metadata) {
        startPlayer(getHistoryKey(), result, useParse, getSite().getTimeout(), startPositionMs, metadata);
    }

    @Override
    public boolean preloadPlayback(Result result, long startPositionMs, MediaMetadata metadata) {
        return player().preload(PlaySpec.from(result, getHistoryKey(), metadata), startPositionMs);
    }

    @Override
    public void clearPreload() {
        if (service() != null && isOwner()) player().clearPreload();
    }

    @Override
    public void loadDanmaku(Result result, History history, Episode episode) {
        // QBox: 弹幕云搜整体撤掉，不再请求第三方弹幕接口。
    }

    @Override
    public void renderDetail(Vod item, History history) {
        mHistory = history;
        mBinding.progressLayout.showContent();
        mBinding.name.setText(item.getName());
        mBinding.video.requestFocus();
        App.removeCallbacks(mR4);
        setArtwork(item.getPic());
        checkKeepImg();
        setText(item);
        updateKeep();
    }

    @Override
    public void renderVodUpdate(Vod item) {
        if (!item.getId().isEmpty()) updateNavigationKey();
        renderVodMetadata(item.getName(), item.getPic());
        setText(item);
    }

    private void renderVodMetadata(String name, String pic) {
        if (name.isEmpty() && pic.isEmpty()) return;
        if (!name.isEmpty()) mBinding.name.setText(name);
        if (!name.isEmpty()) setPartAdapter();
        if (!pic.isEmpty()) setArtwork();
        updateKeep();
    }

    @Override
    public void renderEmptyDetail() {
        showEmpty();
    }

    @Override
    public void renderFallbackName(String name) {
        mBinding.name.setText(name);
    }

    @Override
    public void renderFlags(List<Flag> items) {
        mBinding.flag.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        mFlagAdapter.addAll(items);
    }

    @Override
    public void renderEpisodes(List<Episode> items) {
        setEpisodeAdapter(items);
        // 单集（电影/单片）不需要「章节」入口：绝大多数场景用不上，默认收起。
        if (mBinding != null) mBinding.control.action.chapter.setVisibility(items != null && items.size() > 1 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void renderFlagSelection(Flag item) {
        mBinding.flag.setSelectedPosition(mFlagAdapter.indexOf(item));
        notifyItemChanged(mBinding.flag, mFlagAdapter);
    }

    @Override
    public void renderEpisodeSelection(Episode item) {
        notifyItemChanged(mBinding.episode, mEpisodeAdapter);
        mBinding.episode.setSelectedPosition(mEpisodeAdapter.getPosition());
    }

    @Override
    public void renderReverseEpisodes(List<Episode> items, boolean scroll) {
        setEpisodeAdapter(items);
        if (scroll) mBinding.episode.setSelectedPosition(mEpisodeAdapter.getPosition());
    }

    @Override
    public void renderQuality(Result result, boolean visible) {
        mQualityAdapter.addAll(result);
        setQualityVisible(visible);
    }

    @Override
    public void renderQualityVisible(boolean visible) {
        setQualityVisible(visible);
    }

    @Override
    public void renderSources(List<Vod> items) {
        mQuickAdapter.addAll(items);
        mBinding.quick.setVisibility(mQuickAdapter.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void renderHistory(History history) {
        mHistory = history;
        mBinding.control.action.opening.setText(history.getOpening() <= 0 ? getString(R.string.play_op) : Util.timeMs(history.getOpening()));
        mBinding.control.action.ending.setText(history.getEnding() <= 0 ? getString(R.string.play_ed) : Util.timeMs(history.getEnding()));
        player().setSpeed(SpeedSetting.getPlayback());
        player().setRepeatOne(Setting.isRepeatOne());
        mBinding.control.action.repeat.setSelected(player().isRepeatOne());
        updateSpeedButton();
        setScale(getScale());
        setPartAdapter();
    }

    @Override
    public void renderUseParse(boolean useParse) {
        setUseParse(useParse);
        mBinding.control.action.parse.setVisibility(isUseParse() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void renderArtwork(String url) {
        setArtwork(url);
    }

    @Override
    public void renderDescription(String desc) {
        mBinding.content.setTag(desc);
    }

    @Override
    public void renderPlaybackMetadata(MediaMetadata metadata) {
        if (service() != null && isOwner()) player().setMetadata(metadata);
        mBinding.widget.title.setText(metadata.displayTitle);
        mBinding.widget.title.setSelected(true);
        mBinding.control.title.setText(metadata.displayTitle);
        mBinding.control.title.setSelected(true);
    }

    @Override
    public void onDetailFallbackScheduled() {
        App.post(mR4, 10000);
    }

    @Override
    public void onDetailFallbackCancelled() {
        App.removeCallbacks(mR4);
    }

    @Override
    public void onSearchStarted(String keyword) {
        mBinding.part.setTag(keyword);
    }

    @Override
    public void onSearchResult() {
        App.removeCallbacks(mR4);
    }

    @Override
    public void showDetailMessage(String msg) {
        Notify.show(msg);
    }

    @Override
    public void showSwitchLine(Flag flag) {
        Notify.show(getString(R.string.play_switch_flag, flag.getFlag()));
    }

    @Override
    public void showSwitchSource(Vod item) {
        Notify.show(getString(R.string.play_switch_site, item.getSiteName()));
    }

    @Override
    public void showEpisodeReady(Episode item) {
        Notify.show(getString(R.string.play_ready, item.getName()));
    }

    @Override
    public void showNoNext(boolean reversed) {
        Notify.show(reversed ? R.string.error_play_prev : R.string.error_play_next);
    }

    @Override
    public void showNoPrev(boolean reversed) {
        Notify.show(reversed ? R.string.error_play_next : R.string.error_play_prev);
    }

    @Override
    public void finishVod() {
        finish();
    }

    private void checkCast() {
        if (isCast() && !isFullscreen()) enterFullscreen();
        else mBinding.progressLayout.showProgress();
    }

    private void checkId() {
        mVod.checkId();
    }

    private void showEmpty() {
        mBinding.progressLayout.showEmpty();
    }

    private void setText(Vod item) {
        mBinding.content.setTag(item.getContent());
        String intro = item.getContent();
        if (intro != null && !intro.trim().isEmpty()) {
            mBinding.intro.setVisibility(View.VISIBLE);
            mBinding.intro.setText(intro.trim());
        } else {
            mBinding.intro.setVisibility(View.GONE);
        }
        setMeta(item);
        setText(mBinding.director, R.string.detail_director, item.getDirector());
        setText(mBinding.actor, R.string.detail_actor, item.getActor());
        setText(mBinding.remark, 0, item.getRemarks());
    }

    /** 站源 / 年份 / 地区 / 类型合并成一行弱化信息，标签文案不上屏。 */
    private void setMeta(Vod item) {
        List<String> parts = new ArrayList<>();
        addMeta(parts, getSite().getName());
        addYear(parts, item.getYear());
        addMeta(parts, item.getArea());
        addMeta(parts, item.getTypeName());
        mBinding.year.setVisibility(View.GONE);
        mBinding.area.setVisibility(View.GONE);
        mBinding.type.setVisibility(View.GONE);
        setText(mBinding.site, 0, TextUtils.join("  ·  ", parts));
    }

    /** 年份归一：`2026-08-14` 只留 `2026`，`1970-01-01`（源没给年份）直接丢掉。 */
    private void addYear(List<String> parts, String year) {
        if (TextUtils.isEmpty(year)) return;
        String value = year.trim();
        if (value.length() < 4 || !value.substring(0, 4).matches("\\d{4}")) {
            parts.add(value);
            return;
        }
        int y = Integer.parseInt(value.substring(0, 4));
        if (y < 1900 || y > 2100) return;
        parts.add(String.valueOf(y));
    }

    private void addMeta(List<String> parts, String text) {
        if (!TextUtils.isEmpty(text)) parts.add(text.trim());
    }

    private void setText(TextView view, int resId, String text) {
        if (TextUtils.isEmpty(text) && !TextUtils.isEmpty(view.getText())) return;
        view.setText(Sniffer.buildClickable(resId > 0 ? getString(resId, text) : text, this::clickableSpan), TextView.BufferType.SPANNABLE);
        view.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        view.setLinkTextColor(ContextCompat.getColor(this, R.color.qbox_accent));
        CustomMovement.bind(view);
    }

    private ClickableSpan clickableSpan(Result result) {
        return new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                VodActivity.start(getActivity(), getKey(), result);
                setRedirect(true);
            }
        };
    }

    @Override
    public void onItemClick(Flag item) {
        mVod.selectFlag(item);
    }

    private void setEpisodeAdapter(List<Episode> items) {
        mEpisodeItems = items;
        mBinding.episode.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        // 只有一集时不需要「选集」入口。
        mBinding.control.action.chapter.setVisibility(items.size() > 1 ? View.VISIBLE : View.GONE);
        mEpisodeAdapter.addAll(items);
        setArrayAdapter(items.size());
        setR2Callback();
    }

    /** 主按钮「播放」：直接播第一个可播剧集；没有剧集时先选中第一条线路。 */
    private void onPrimaryPlay() {
        if (mEpisodeItems != null && !mEpisodeItems.isEmpty()) onItemClick(mEpisodeItems.get(0));
        else if (mFlagAdapter != null && mFlagAdapter.getItemCount() > 0) onItemClick(mFlagAdapter.get(0));
    }

    @Override
    public void onItemClick(Episode item) {
        if (shouldEnterFullscreen(item)) return;
        mVod.selectEpisode(item);
    }

    private void setQualityVisible(boolean visible) {
        mBinding.quality.setVisibility(visible ? View.VISIBLE : View.GONE);
        setR2Callback();
    }

    @Override
    public void onItemClick(Result result) {
        mVod.selectQuality(result);
    }

    private void setArrayAdapter(int size) {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.play_reverse));
        items.add(getString(mHistory.getRevPlayText()));
        mBinding.array.setVisibility(size > 1 ? View.VISIBLE : View.GONE);
        if (mHistory.isRevSort()) for (int i = size; i > 0; i -= 20) items.add(i + "-" + Math.max(i - 19, 1));
        else for (int i = 0; i < size; i += 20) items.add((i + 1) + "-" + Math.min(i + 20, size));
        mArrayAdapter.addAll(items);
    }

    private int findFocusDown(int index) {
        List<Integer> orders = Arrays.asList(R.id.flag, R.id.quality, R.id.episode, R.id.array, R.id.part, R.id.quick);
        for (int i = 0; i < orders.size(); i++) if (i > index) if (isVisible(findViewById(orders.get(i)))) return orders.get(i);
        return 0;
    }

    private int findFocusUp(int index) {
        List<Integer> orders = Arrays.asList(R.id.flag, R.id.quality, R.id.episode, R.id.array, R.id.part, R.id.quick);
        for (int i = orders.size() - 1; i >= 0; i--) if (i < index) if (isVisible(findViewById(orders.get(i)))) return orders.get(i);
        return 0;
    }

    private void updateFocus() {
        mPartAdapter.setNextFocusUp(findFocusUp(4));
        mEpisodeAdapter.setNextFocusUp(findFocusUp(2));
        mFlagAdapter.setNextFocusDown(findFocusDown(0));
        mEpisodeAdapter.setNextFocusDown(findFocusDown(2));
        notifyItemChanged(mBinding.episode, mEpisodeAdapter);
        notifyItemChanged(mBinding.part, mPartAdapter);
        notifyItemChanged(mBinding.flag, mFlagAdapter);
    }

    @Override
    public void onRevSort() {
        mVod.setRevSort(!mHistory.isRevSort());
        mVod.reverseEpisode(false);
    }

    @Override
    public void onRevPlay(TextView view) {
        mVod.setRevPlay(!mHistory.isRevPlay());
        view.setText(mHistory.getRevPlayText());
        Notify.show(mHistory.getRevPlayHint());
    }

    private boolean shouldEnterFullscreen(Episode item) {
        boolean enter = !isFullscreen() && item.isSelected();
        if (enter) enterFullscreen();
        return enter;
    }

    private void enterFullscreen() {
        mFocus1 = getCurrentFocus();
        mBinding.video.requestFocus();
        mBinding.video.setForeground(null);
        // 真全屏：黑底 + 去掉详情态的 12dp 圆角与裁剪。
        // 黑底很关键：非 16:9 片源的上下/左右黑边必须是纯黑，不能透出氛围底图。
        mBinding.video.setBackgroundColor(Color.BLACK);
        mBinding.video.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        mBinding.flag.setSelectedPosition(mFlagAdapter.getPosition());
        mKeyDown.setFull(true);
        setFullscreen(true);
        mFocus2 = null;
    }

    private void exitFullscreen() {
        mBinding.video.setForeground(ResUtil.getDrawable(R.drawable.selector_video));
        mBinding.video.setLayoutParams(mFrameParams);
        mBinding.video.setBackground(mVideoBg);
        getFocus1().requestFocus();
        mKeyDown.setFull(false);
        setFullscreen(false);
        mFocus2 = null;
        hideInfo();
    }

    private void onContent() {
        if (mBinding.content.getTag() == null) return;
        ContentDialog.create().content(mBinding.content.getTag().toString()).show(this);
    }

    private void onKeep() {
        Keep keep = Keep.find(getHistoryKey());
        Notify.show(keep != null ? R.string.keep_del : R.string.keep_add);
        if (keep != null) keep.delete();
        else createKeep();
        checkKeepImg();
    }

    private void onVideo() {
        if (!isFullscreen()) enterFullscreen();
    }

    private void onChange() {
        mVod.manualSwitchSource();
    }

    private void onRepeat() {
        player().setRepeatOne(!player().isRepeatOne());
        Setting.putRepeatOne(player().isRepeatOne());
        mBinding.control.action.repeat.setSelected(player().isRepeatOne());
        // 循环是开关型按钮，之前没有任何文案反馈，用户不知道按了发生什么。
        Notify.show(getString(R.string.play_repeat) + (player().isRepeatOne() ? "：开" : "：关"));
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        mBinding.control.action.repeat.setSelected(player().isRepeatOne());
    }

    private void checkNext() {
        checkNext(true);
    }

    private void checkNext(boolean notify) {
        mVod.nextEpisode(notify);
    }

    private void checkPrev() {
        mVod.prevEpisode(true);
    }

    private void onNext(boolean notify) {
        Episode item = mEpisodeAdapter.getNext();
        if (!item.isSelected()) onItemClick(item);
        else if (notify) Notify.show(mHistory.isRevPlay() ? R.string.error_play_prev : R.string.error_play_next);
    }

    private void onPrev(boolean notify) {
        Episode item = mEpisodeAdapter.getPrev();
        if (!item.isSelected()) onItemClick(item);
        else if (notify) Notify.show(mHistory.isRevPlay() ? R.string.error_play_next : R.string.error_play_prev);
    }

    private void onScale() {
        int index = getScale();
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        setScale(index == array.length - 1 ? 0 : ++index);
    }

    private static final float[] SPEED_STEPS = {1f, 1.5f, 1.75f, 2f, 3f, 0.75f};

    private void onSpeed() {
        // 点按循环常用倍速：1 → 1.5 → 1.75 → 2 → 3 → 0.75 → 1，并给 OSD 反馈；
        // 精细倍速留在长按（SpeedSettingDialog），不再默认弹设置窗。
        float current = player().getSpeed();
        float next = SPEED_STEPS[0];
        for (int i = 0; i < SPEED_STEPS.length; i++) {
            if (Math.abs(SPEED_STEPS[i] - current) < 0.01f) {
                next = SPEED_STEPS[(i + 1) % SPEED_STEPS.length];
                break;
            }
        }
        player().setSpeed(next);
        SpeedSetting.putPlayback(next);
        updateSpeedButton();
        String text = next == Math.round(next) ? String.valueOf((int) next) : String.valueOf(next);
        Notify.show("倍速 " + text + "x");
    }

    private void updateSpeedButton() {
        // 切换类按钮状态规则（N-28）：非 1x 时按钮直接显示当前倍速，1x 时回到简洁文案。
        float speed = player().getSpeed();
        mBinding.control.action.speed.setText(Math.abs(speed - 1f) < 0.01f ? getString(R.string.play_speed) : SpeedSetting.format(speed));
    }

    private boolean onSpeedLong() {
        onSpeedSetting();
        return true;
    }

    /** 精细倍速（收在「更多」二级行，也可以长按倍速按钮进入） */
    private void onSpeedSetting() {
        SpeedSettingDialog.create().player(player()).save(true).show(this);
        hideControl();
    }

    private void onReset() {
        onRefresh();
    }

    private void onParse() {
        ParseDialog.create().show(this);
        hideControl();
    }

    private void onReplay() {
        mVod.replay();
    }

    private void onRefresh() {
        mVod.refresh();
    }

    private void onOpening() {
        long position = player().getPosition();
        long duration = player().getDuration();
        if (player().canSetOpening(position, duration)) setOpening(position);
    }

    private void onOpeningAdd() {
        setOpening(Math.max(0, Math.max(0, mHistory.getOpening()) + 1000));
    }

    private void onOpeningSub() {
        setOpening(Math.max(0, Math.max(0, mHistory.getOpening()) - 1000));
    }

    private boolean onOpeningReset() {
        setOpening(0);
        return true;
    }

    private void setOpening(long opening) {
        mVod.setOpening(opening);
        mBinding.control.action.opening.setText(opening <= 0 ? getString(R.string.play_op) : Util.timeMs(mHistory.getOpening()));
    }

    private void onEnding() {
        long position = player().getPosition();
        long duration = player().getDuration();
        if (player().canSetEnding(position, duration)) setEnding(duration - position);
    }

    private void onEndingAdd() {
        setEnding(Math.max(0, Math.max(0, mHistory.getEnding()) + 1000));
    }

    private void onEndingSub() {
        setEnding(Math.max(0, Math.max(0, mHistory.getEnding()) - 1000));
    }

    private boolean onEndingReset() {
        setEnding(0);
        return true;
    }

    private void setEnding(long ending) {
        mVod.setEnding(ending);
        mBinding.control.action.ending.setText(ending <= 0 ? getString(R.string.play_ed) : Util.timeMs(mHistory.getEnding()));
    }

    private void onPlayer() {
        PlayerEngineDialog.show(this, mBinding.control.action.player, player());
        hideControl();
    }

    private void onDecode() {
        mClock.setCallback(null);
        player().toggleDecode();
    }

    private void onTrack(View view) {
        TrackDialog.create().type(Integer.parseInt(view.getTag().toString())).player(player()).view(mBinding.player.getSubtitleView()).show(this);
        hideControl();
    }

    private void onEdition() {
        EditionDialog.create().player(player()).show(this);
        hideControl();
    }

    private void onChapter() {
        ChapterDialog.create().player(player()).show(this);
        hideControl();
    }

    private void onDanmaku() {
        DanmakuDialog.create().player(player()).show(this);
        hideControl();
    }

    private void onToggle() {
        if (isVisible(mBinding.control.getRoot())) hideControl();
        else showControl(getFocus2());
    }

    private void showProgress() {
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
        App.post(mR3, 0);
        hideCenter();
        hideError();
    }

    private void hideProgress() {
        mBinding.progress.getRoot().setVisibility(View.GONE);
        mBinding.progress.traffic.setVisibility(View.GONE);
        App.removeCallbacks(mR3);
        Traffic.reset();
    }

    private void showError(String text) {
        PlaybackAction.hideSpeedHint(mBinding.widget.message);
        mBinding.widget.error.setVisibility(View.VISIBLE);
        mBinding.widget.text.setText(text);
        hideProgress();
    }

    private void hideError() {
        mBinding.widget.error.setVisibility(View.GONE);
        mBinding.widget.text.setText("");
    }

    private void showInfo() {
        // QBox: 暂停/加载统一走底部控制条，中央大播放键不再出现（详情页也会残留巨幅图标）。
        mBinding.widget.top.setVisibility(View.VISIBLE);
        mBinding.widget.center.setVisibility(View.GONE);
        mBinding.widget.duration.setText(player().getDurationTime());
        mBinding.widget.position.setText(player().getPositionTime(0));
    }

    private void hideInfo() {
        mBinding.widget.top.setVisibility(View.GONE);
        mBinding.widget.center.setVisibility(View.GONE);
    }

    private void showControl(View view) {
        seeking = false;
        seekFocus = false;
        App.removeCallbacks(mHideControl);
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        setExtra(view != null && isInExtra(view));
        updatePlayPause();
        updateSpeedButton();
        // 等控制条真正布局完再要焦点：setVisibility(VISIBLE) 只触发 requestLayout，
        // 单次 post 时按钮往往还没 measure/layout，isShown() 仍为 false，焦点就留在画面上，
        // →/← 被当成快进（用户反馈"按钮完全无法聚焦"的真正原因）。
        // 用 OnGlobalLayoutListener 等到布局真的完成后再挑一个可见按钮要焦点。
        requestControlFocus(view);
        setSubtitleLift(true);
        setR1Callback();
    }

    private void requestControlFocus(View view) {
        View target = view;
        View root = mBinding.control.getRoot();
        root.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                View focus = target != null && target.isShown() ? target : getFocus2();
                if (focus == null || !focus.isShown()) focus = mBinding.control.action.more;
                if (focus != null && focus.isShown()) focus.requestFocus();
            }
        });
    }

    private void hideControl() {
        seeking = false;
        seekFocus = false;
        App.removeCallbacks(mHideControl);
        mBinding.control.getRoot().setVisibility(View.GONE);
        setExtra(false);
        setSubtitleLift(false);
        App.removeCallbacks(mR1);
    }

    /** 控制条弹出来时把字幕抬高，避免字幕和「上集/下集/倍速…」压在同一个位置。 */
    private void setSubtitleLift(boolean lift) {
        try {
            if (mBinding.player.getSubtitleView() != null) mBinding.player.getSubtitleView().setBottomPaddingFraction(lift ? 0.2f : 0.08f);
        } catch (Throwable ignored) {
        }
    }

    /** 更多面板：非常用开关收在二级行，主行保持一屏放得下。 */
    private void onMore() {
        boolean show = mBinding.control.action.extra.getVisibility() != View.VISIBLE;
        setExtra(show);
        if (show) App.post(() -> mBinding.control.action.scale.requestFocus(), 50);
        setR1Callback();
    }

    private void setExtra(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        if (mBinding.control.action.extra.getVisibility() == visibility) return;
        mBinding.control.action.extra.setVisibility(visibility);
        mBinding.control.action.more.setText(show ? R.string.play_less : R.string.play_more);
    }

    private boolean isInExtra(View view) {
        View extra = mBinding.control.action.extra;
        while (view != null) {
            if (view == extra) return true;
            if (!(view.getParent() instanceof View)) return false;
            view = (View) view.getParent();
        }
        return false;
    }

    private void hideCenter() {
        mBinding.widget.action.setImageResource(R.drawable.ic_widget_play);
        hideInfo();
    }

    private void setTraffic() {
        // QBox: 加载态实时显示带宽速度，方便判断是网络慢还是源慢。
        // 之前只 post 一次，数字永远停在打开加载层那一瞬间（用户实测卡在 37 KB/s）。
        if (mBinding == null || mBinding.progress.getRoot().getVisibility() != View.VISIBLE) return;
        Traffic.setSpeed(mBinding.progress.traffic);
        App.post(mR3, 500);
    }

    private void onStats() {
        View root = mBinding.stats.getRoot();
        if (root.getVisibility() == View.VISIBLE) {
            root.setVisibility(View.GONE);
            App.removeCallbacks(mR5);
        } else {
            root.setVisibility(View.VISIBLE);
            updateStats();
        }
        setR1Callback();
    }

    private void updateStats() {
        if (mBinding.stats.getRoot().getVisibility() != View.VISIBLE) return;
        mBinding.stats.text.setText(buildStats());
        App.post(mR5, 1000);
    }

    /** Stats for nerds：引擎/解码/分辨率/码率/音轨/输出/网速/缓冲，1s 刷新。 */
    private String buildStats() {
        Player p = player().getPlayer();
        StringBuilder sb = new StringBuilder();
        sb.append("QBox stats\n");
        if (p == null) return sb.append("player idle").toString();
        sb.append("Engine   ").append(PlayerSetting.isMpv() ? "MPV" : "EXO").append(" · ").append(player().getDecodeText()).append(" · ").append(PlayerSetting.getRender() == 0 ? "Surface" : "Texture").append('\n');
        sb.append("State    ").append(stateText()).append(" · ").append(String.format(Locale.US, "%.1fx", player().getSpeed())).append('\n');
        Format video = findSelectedFormat(C.TRACK_TYPE_VIDEO);
        VideoSize size = p.getVideoSize();
        int width = video != null && video.width > 0 ? video.width : size.width;
        int height = video != null && video.height > 0 ? video.height : size.height;
        sb.append("Video    ").append(width).append('×').append(height);
        if (video != null && video.frameRate > 0) sb.append(String.format(Locale.US, " · %.2ffps", video.frameRate));
        sb.append(" · ").append(hdr(video)).append('\n');
        if (video != null) {
            sb.append("Codec    ").append(codec(video.sampleMimeType, video.codecs));
            String rate = bitrate(video.bitrate > 0 ? video.bitrate : video.averageBitrate);
            if (!"-".equals(rate)) sb.append(" · ").append(rate);
            sb.append('\n');
        }
        Format audio = findSelectedFormat(C.TRACK_TYPE_AUDIO);
        sb.append("Audio    ");
        if (audio == null) sb.append("-");
        else {
            sb.append(codec(audio.sampleMimeType, audio.codecs));
            if (audio.channelCount > 0) sb.append(" · ").append(audio.channelCount).append("ch");
            if (audio.sampleRate > 0) sb.append(" · ").append(audio.sampleRate / 1000f).append("kHz");
            if (!TextUtils.isEmpty(audio.language) && !"und".equals(audio.language)) sb.append(" · ").append(audio.language);
        }
        sb.append('\n');
        String output = DecodeSetting.isAudioPassThrough() ? "Passthrough" : "PCM";
        String surround = audio == null ? null : surround(audio.sampleMimeType);
        sb.append("Output   ").append(output).append(surround == null ? "" : " · " + surround).append('\n');
        sb.append("Net      ").append(Traffic.getText()).append('\n');
        sb.append("Buffer   ").append(String.format(Locale.US, "%.1fs", Math.max(0, p.getBufferedPosition() - p.getCurrentPosition()) / 1000f)).append('\n');
        String url = player().getUrl();
        if (!TextUtils.isEmpty(url)) {
            String host = Uri.parse(url).getHost();
            sb.append("Source   ").append(TextUtils.isEmpty(host) ? "-" : host);
        }
        return sb.toString();
    }

    private String stateText() {
        switch (player().getPlaybackState()) {
            case Player.STATE_BUFFERING:
                return "BUFFERING";
            case Player.STATE_READY:
                return player().isPlaying() ? "PLAYING" : "PAUSED";
            case Player.STATE_ENDED:
                return "ENDED";
            default:
                return "IDLE";
        }
    }

    private Format findSelectedFormat(int type) {
        for (Tracks.Group group : player().getCurrentTracks().getGroups()) {
            if (group.getType() != type) continue;
            for (int i = 0; i < group.length; i++) if (group.isTrackSelected(i)) return group.getTrackFormat(i);
        }
        return null;
    }

    private String codec(String mime, String codecs) {
        if (!TextUtils.isEmpty(codecs)) return codecs;
        if (TextUtils.isEmpty(mime)) return "-";
        return mime.substring(mime.indexOf('/') + 1);
    }

    private String bitrate(int bitrate) {
        if (bitrate <= 0) return "-";
        return String.format(Locale.US, "%.2f Mbps", bitrate / 1000000f);
    }

    private String hdr(Format format) {
        if (format == null) return "-";
        if (MimeTypes.VIDEO_DOLBY_VISION.equals(format.sampleMimeType)) return "Dolby Vision";
        if (format.colorInfo == null) return "SDR";
        switch (format.colorInfo.colorTransfer) {
            case C.COLOR_TRANSFER_ST2084:
                return "HDR10";
            case C.COLOR_TRANSFER_HLG:
                return "HLG";
            default:
                return "SDR";
        }
    }

    private String surround(String mime) {
        if (mime == null) return null;
        String value = mime.toLowerCase(Locale.ROOT);
        if (value.contains("ac-4") || value.contains("ac4")) return "Dolby AC-4";
        if (value.contains("eac3") || value.contains("e-ac-3")) return "Dolby Digital+";
        if (value.contains("ac3") || value.contains("ac-3")) return "Dolby Digital";
        if (value.contains("truehd")) return "Dolby TrueHD";
        if (value.contains("dts")) return "DTS";
        return null;
    }

    private void setR1Callback() {
        if (isScrubbing()) return;
        App.post(mR1, Constant.INTERVAL_HIDE);
    }

    @Override
    protected void onScrubbingChanged(boolean scrubbing) {
        if (scrubbing) App.removeCallbacks(mR1);
        else if (isVisible(mBinding.control.getRoot())) setR1Callback();
    }

    private void setR2Callback() {
        App.post(mR2, 500);
    }

    private void setArtwork(String url) {
        mHistory.setVodPic(url);
        setArtwork();
        ImgUtil.load(mHistory.getVodName(), url, mBinding.poster);
        mBinding.poster.setVisibility(View.VISIBLE);
    }

    private void setArtwork() {
        ImgUtil.load(this, mHistory.getVodPic(), new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                mBinding.player.setDefaultArtwork(resource);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                mBinding.player.setDefaultArtwork(errorDrawable);
            }
        });
        setBackdrop(mHistory.getVodPic());
    }

    /** 详情页氛围底图：源海报中心 16:9 横带截取，~12× 放大即柔和模糊（原 48px 方块 48× 放大只剩局部色块）。 */
    private void setBackdrop(String url) {
        try {
            if (TextUtils.isEmpty(url)) return;
            Glide.with(this).load(ImgUtil.getUrl(url)).override(160, 90).centerCrop()
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(700))
                    .into(mBinding.backdrop);
        } catch (Throwable ignored) {
        }
    }

    private void setPartAdapter() {
        mPartAdapter.addAll(PartUtil.split(mHistory.getVodName()));
        mBinding.part.setVisibility(View.VISIBLE);
        setR2Callback();
    }

    private void saveHistory(boolean exit) {
        PlaybackService service = service();
        boolean owner = service != null && getPlaybackKey().equals(service.player().getKey());
        long position = owner ? service.player().getPosition() : C.TIME_UNSET;
        long duration = owner ? service.player().getDuration() : C.TIME_UNSET;
        if (mVod != null) mVod.saveHistory(exit, System.currentTimeMillis(), position, duration);
    }

    private void checkKeepImg() {
        mBinding.keep.setCompoundDrawablesWithIntrinsicBounds(Keep.find(getHistoryKey()) == null ? R.drawable.ic_detail_keep_off : R.drawable.ic_detail_keep_on, 0, 0, 0);
    }

    private void createKeep() {
        Keep keep = new Keep();
        keep.setKey(getHistoryKey());
        keep.setCid(VodConfig.getCid());
        keep.setVodPic(mHistory.getVodPic());
        keep.setVodName(mHistory.getVodName());
        keep.setSiteName(getSite().getName());
        keep.setCreateTime(System.currentTimeMillis());
        keep.save();
    }

    private void updateKeep() {
        Keep keep = Keep.find(getHistoryKey());
        if (keep != null) {
            keep.setVodName(mHistory.getVodName());
            keep.setVodPic(mHistory.getVodPic());
            keep.save();
        }
    }

    private final PlaybackService.NavigationCallback mNavigationCallback = new PlaybackService.NavigationCallback() {
        @Override
        public void onNext() {
            checkNext();
        }

        @Override
        public void onPrev() {
            checkPrev();
        }

        @Override
        public void onStop() {
            finish();
        }

        @Override
        public void onReplay() {
            VideoActivity.this.onReplay();
        }
    };

    @Override
    protected String getPlaybackKey() {
        return getHistoryKey();
    }

    @Override
    protected void onPrepare() {
        setPlaybackMode();
    }

    @Override
    protected void onDecodeChanged() {
        setPlaybackMode();
    }

    @Override
    protected void onTracksChanged() {
        setTrackVisible();
    }

    @Override
    protected void onMediaOptionsChanged() {
        setMediaOptionVisible();
    }

    @Override
    protected void onError(String msg) {
        mVod.playbackError(msg);
    }

    @Override
    protected void onReclaim() {
        mVod.refresh();
    }

    @Override
    protected void onStateChanged(int state) {
        updatePlayPause();
        switch (state) {
            case Player.STATE_BUFFERING:
                if (seekFocus) App.post(mRestoreSeekFocus, 150);
                showProgress();
                mClock.setCallback(null);
                break;
            case Player.STATE_READY:
                if (seekFocus) App.post(mRestoreSeekFocus, 150);
                hideProgress();
                player().reset();
                mClock.setCallback(this);
                break;
            case Player.STATE_ENDED:
                hideProgress();
                mVod.playbackEnded();
                mClock.setCallback(null);
                hideInfo();
                showControl(mBinding.control.action.replay);
                break;
        }
    }

    @Override
    protected void onPlayingChanged(boolean isPlaying) {
        updatePlayPause();
        if (isPlaying) {
            mBinding.poster.setVisibility(View.GONE);
            hideCenter();
        } else if (isPaused()) {
            if (isFullscreen()) showInfo();
            else hideInfo();
        }
    }

    /** 暂停/恢复的意图一变就刷新按钮，不再等 isPlaying 跟着变（缓冲时两者不同步）。 */
    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
        if (mPlayIntent != null && playWhenReady == mPlayIntent) mPlayIntent = null;
        if (isOwner()) updatePlayPause();
    }

    @Override
    protected void onSizeChanged(VideoSize size) {
        mBinding.widget.size.setText(player().getSizeText());
    }

    @Override
    public void onTimeChanged(long time) {
        if (!isOwner() || !player().isVod()) return;
        long position = player().getPosition();
        long duration = player().getDuration();
        if (position < 0 || duration <= 0) return;
        mVod.onTimeChanged(time, position, duration);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (isRedirect()) return;
        if (event.getType() == RefreshEvent.Type.DETAIL) mVod.requestDetail();
        else if (event.getType() == RefreshEvent.Type.PLAYER) mVod.refresh();
        else if (event.getType() == RefreshEvent.Type.VOD) mVod.updateVod(event.getVod());
        else if (event.getType() == RefreshEvent.Type.SUBTITLE) player().setSub(Sub.from(event.getPath()));
        else if (event.getType() == RefreshEvent.Type.DANMAKU) player().setDanmaku(Danmaku.from(event.getPath()));
    }

    @Override
    protected long startPositionMs() {
        return mVod == null ? C.TIME_UNSET : mVod.startPositionMs();
    }

    private void setTrackVisible() {
        PlaybackAction.setTracks(player(), mBinding.control.action.text, mBinding.control.action.audio, mBinding.control.action.video);
    }

    private void setMediaOptionVisible() {
        PlaybackAction.setMediaOptions(player(), mBinding.control.action.edition, mBinding.control.action.chapter);
    }

    @Override
    public void onItemClick(Vod item) {
        mVod.selectSource(item);
    }

    @Override
    public void onParse(Parse item) {
        mVod.selectParse(item);
    }

    private void onPaused() {
        controller().pause();
    }

    private void onPlay() {
        if (mHistory != null && isEnded()) controller().seekTo(mHistory.getOpening());
        if (!player().isEmpty() && isIdle()) controller().prepare();
        controller().play();
    }

    /** 控制栏里的播放/暂停按钮：和 OK 键走同一套状态判断。 */
    private void onPlayPause() {
        if (service() == null) return;
        // 播完以后再按，用户想的是「重播」，不是「暂停一个已经停住的画面」。
        if (isEnded()) {
            onReplay();
            showPlayIntent(true);
            return;
        }
        boolean playing = player().isPlayWhenReady();
        if (playing) onPaused();
        else if (player().isEmpty()) onRefresh();
        else onPlay();
        showPlayIntent(!playing);
    }

    /** 先按用户意图刷新按钮，再在状态回传/超时后交还给真实状态。 */
    private void showPlayIntent(boolean playing) {
        mPlayIntent = playing;
        final int seq = ++mPlayIntentSeq;
        updatePlayPause();
        App.post(() -> {
            if (seq != mPlayIntentSeq) return;
            mPlayIntent = null;
            updatePlayPause();
        }, 800);
    }

    /**
     * 按钮文字跟随真实播放意图。
     * 之前只看 isPlayWhenReady：播完（ENDED）时它仍是 true，按钮会停在「暂停」，
     * 用户看到的是「已经播完了却写着暂停」。这里把 ENDED 也算成可播放态。
     * 另外点按瞬间 MediaController 状态还没回传，用 mPlayIntent 覆盖，避免慢一拍。
     */
    private void updatePlayPause() {
        if (service() == null || mBinding == null) return;
        try {
            boolean playing = mPlayIntent != null ? mPlayIntent : (!isEnded() && player().isPlayWhenReady());
            mBinding.control.action.playPause.setText(playing ? R.string.pause : R.string.play);
        } catch (Throwable ignored) {
        }
    }

    private boolean onSeekBack() {
        controller().seekBack();
        return true;
    }

    private boolean onSeekForward() {
        controller().seekForward();
        return true;
    }

    private boolean isFullscreen() {
        return fullscreen;
    }

    private void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public boolean isUseParse() {
        return useParse;
    }

    public void setUseParse(boolean useParse) {
        this.useParse = useParse;
    }

    private View getFocus1() {
        return mFocus1 == null || mFocus1.getVisibility() != View.VISIBLE ? mBinding.video : mFocus1;
    }

    private View getFocus2() {
        if (mFocus2 != null && mFocus2.isShown() && mFocus2 != mBinding.control.action.opening && mFocus2 != mBinding.control.action.ending) return mFocus2;
        if (mBinding.control.action.playPause.isShown()) return mBinding.control.action.playPause;
        if (mBinding.control.action.chapter.isShown()) return mBinding.control.action.chapter;
        if (mBinding.control.action.prev.isShown()) return mBinding.control.action.prev;
        return mBinding.control.action.more;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isFullscreen() && KeyUtil.isMenuKey(event)) onToggle();
        View focus = getCurrentFocus();
        boolean panelFocused = isVisible(mBinding.control.getRoot()) && isInside(mBinding.control.getRoot(), focus);
        if (isVisible(mBinding.control.getRoot())) {
            setR1Callback();
            // 只在焦点确实落在控制条内时才记 mFocus2，否则会把 video 记进去，
            // 之后 getFocus2() 一直接到 video，控制条按钮永远拿不到焦点。
            if (panelFocused) mFocus2 = focus;
        }
        // 详情页（非全屏）的纵向焦点链在 Activity 层显式路由。
        // 之前靠 nextFocusDown 指向 HorizontalGridView 的 id，而网格本身不可聚焦，
        // DOWN 会直接失败：焦点停在「线路」标签上，选集/倒序永远下不去（遥控器像坏了）。
        if (!isFullscreen() && event.getAction() == KeyEvent.ACTION_DOWN && routeDetailFocus(event)) return true;
        // 画面侧的操作（快进/上下/OK）只在控制条没拿到焦点时生效：
        // 快进时控制条是「借」出来显示进度的，焦点仍在画面上，所以连续按左右可以一直快进。
        if (isFullscreen() && !panelFocused && mKeyDown.hasEvent(event) && service() != null) {
            if (isGone(mBinding.control.getRoot()) && (KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event))) enterSeekMode();
            return mKeyDown.onKeyDown(event);
        }
        if (panelFocused && onControlKey(event)) return true;
        if (KeyUtil.isMediaFastForward(event)) return onSeekForward();
        if (KeyUtil.isMediaRewind(event)) return onSeekBack();
        return super.dispatchKeyEvent(event);
    }

    /** 详情页纵向焦点闭环：播放器/动作行 → 线路 → 选集 → 倒序/线路分页 → 快捷行，反向逐级回退。 */
    private boolean routeDetailFocus(KeyEvent event) {
        View focus = getCurrentFocus();
        if (focus == null) return false;
        boolean down = event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN;
        boolean up = event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP;
        if (!down && !up) return false;
        if (down) {
            if (isInside(mBinding.video, focus) || isInside(mBinding.row2, focus))
                return focusRow(mBinding.flag) || focusRow(mBinding.quality) || focusRow(mBinding.episode) || focusRow(mBinding.array);
            if (isInside(mBinding.flag, focus) || isInside(mBinding.quality, focus))
                return focusRow(mBinding.episode) || focusRow(mBinding.array) || focusRow(mBinding.part);
            if (isInside(mBinding.episode, focus))
                return focusRow(mBinding.array) || focusRow(mBinding.part) || focusRow(mBinding.quick);
            if (isInside(mBinding.array, focus))
                return focusRow(mBinding.part) || focusRow(mBinding.quick);
            if (isInside(mBinding.part, focus)) return focusRow(mBinding.quick);
            return false;
        }
        if (isInside(mBinding.episode, focus)) return focusRow(mBinding.flag) || focusRow(mBinding.quality) || mBinding.video.requestFocus();
        if (isInside(mBinding.flag, focus) || isInside(mBinding.quality, focus)) return mBinding.video.requestFocus();
        if (isInside(mBinding.row2, focus) || isInside(mBinding.video, focus)) return mBinding.video.requestFocus();
        if (isInside(mBinding.array, focus)) return focusRow(mBinding.episode) || focusRow(mBinding.flag) || mBinding.video.requestFocus();
        if (isInside(mBinding.part, focus)) return focusRow(mBinding.array) || focusRow(mBinding.episode);
        if (isInside(mBinding.quick, focus)) return focusRow(mBinding.part) || focusRow(mBinding.array);
        return false;
    }

    /** 聚焦到某个横向行里的第一个可聚焦 item（网格容器本身不可聚焦，不能直接 requestFocus）。 */
    private boolean focusRow(ViewGroup row) {
        if (row == null || row.getVisibility() != View.VISIBLE) return false;
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE && child.isFocusable() && child.requestFocus()) return true;
        }
        return false;
    }

    /**
     * 控制栏焦点闭环：左右到行尾不丢焦点；上键进入进度条，下键回到动作行。
     * CustomUpDownView（倍速/字幕/片头片尾）自己要吃上下键调值，这里直接放行。
     */
    private boolean onControlKey(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        View focus = getCurrentFocus();
        if (focus == null) return false;
        if (isInside(mBinding.control.seekBox, focus)) return onSeekBarKey(event);
        boolean inExtra = isInExtra(focus);
        boolean inMain = isInside(mBinding.control.action.main, focus);
        if (!inExtra && !inMain) return false;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT: {
                ViewGroup row = inExtra ? mBinding.control.action.extra : mBinding.control.action.main;
                View target = findFocusable(row, focus, event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT);
                if (target != null) target.requestFocus();
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_UP:
                if (focus instanceof CustomUpDownView) return false;
                if (!inExtra && focus == mBinding.control.action.more) {
                    if (!isVisible(mBinding.control.action.extra)) setExtra(true);
                    focusFirst(mBinding.control.action.extra);
                    return true;
                }
                // 其余动作键按「上」直接进进度条，可以逐秒拖拽，不再只能在动作行里横着滚。
                seekFocus = true;
                mBinding.control.seekBox.requestFocus();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (focus instanceof CustomUpDownView) return false;
                if (inExtra) mBinding.control.action.more.requestFocus();
                return true;
            default:
                return false;
        }
    }

    /** 进度条聚焦后的按键：左右 ±10s（立即 seek，进度条跟着走），下键回动作行。 */
    private boolean onSeekBarKey(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT: {
                long step = seekStep(event);
                if (step > 0) seekBy(-step);
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_RIGHT: {
                long step = seekStep(event);
                if (step > 0) seekBy(step);
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_DOWN:
                seekFocus = false;
                mBinding.control.action.playPause.requestFocus();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                onPlayPause();
                return true;
            default:
                return true;
        }
    }

    /**
     * 进度条焦点态也走阶梯加速，1.9.20 起与视频面手势统一为「按按住时长分档」（C9）：
     * 前 1.5s 每步 10s（秒级细调窗口）→ 20s → 30s → 60s → 120s → 180s → 300s。
     * repeat 事件间再加 300ms 节流，避免长按瞬间飞出很远。
     */
    private long seekStep(KeyEvent event) {
        int repeat = event.getRepeatCount();
        long now = SystemClock.uptimeMillis();
        if (repeat == 0) seekHoldStartTick = now;
        if (repeat > 0) {
            if (now - lastSeekKeyTick < 300) return 0;
            lastSeekKeyTick = now;
        }
        long elapsed = now - seekHoldStartTick;
        if (elapsed < 1_500) return SEEK_STEP_MS;
        if (elapsed < 3_500) return 20_000;
        if (elapsed < 6_000) return 30_000;
        if (elapsed < 9_000) return 60_000;
        if (elapsed < 13_000) return 120_000;
        if (elapsed < 17_000) return 180_000;
        return 300_000;
    }

    private void seekBy(long delta) {
        if (service() == null) return;
        long duration = Math.max(0, player().getDuration());
        long target = Math.max(0, player().getPosition() + delta);
        if (duration > 0) target = Math.min(duration, target);
        controller().seekTo(target);
        // seek 会让播放器进入 buffering，个别机型会把焦点甩回画面层，这里把焦点钉回进度条。
        if (!mBinding.control.seekBox.hasFocus()) mBinding.control.seekBox.requestFocus();
        App.post(mRestoreSeekFocus, 350);
        setR1Callback();
    }

    private View findFocusable(ViewGroup row, View current, boolean forward) {
        View previous = null;
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (!child.isShown() || !child.isFocusable()) continue;
            if (child == current) {
                if (!forward) return previous;
                for (int j = i + 1; j < row.getChildCount(); j++) {
                    View next = row.getChildAt(j);
                    if (next.isShown() && next.isFocusable()) return next;
                }
                return null;
            }
            previous = child;
        }
        return null;
    }

    private void focusFirst(ViewGroup row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (child.isShown() && child.isFocusable()) {
                child.requestFocus();
                return;
            }
        }
    }

    private static boolean isInside(View ancestor, View view) {
        while (view != null) {
            if (view == ancestor) return true;
            view = view.getParent() instanceof View ? (View) view.getParent() : null;
        }
        return false;
    }

    @Override
    public void onSeeking(long time) {
        // QBox 1.9.8: 快进直接复用底部控制条（进度条 + 时间），不再另做浮层。
        if (!seeking) {
            seeking = true;
            seekBase = player().getPosition();
        }
        long duration = Math.max(0, player().getDuration());
        long target = Math.max(0, seekBase + time);
        if (duration > 0) target = Math.min(duration, target);
        controller().seekTo(target);
        mBinding.widget.center.setVisibility(View.GONE);
        hideProgress();
    }

    /** 左右快进时把控制条「借」出来显示进度，但不抢焦点，方向键继续走快进。 */
    private void enterSeekMode() {
        if (isVisible(mBinding.control.getRoot())) return;
        seeking = true;
        seekBase = player().getPosition();
        App.removeCallbacks(mHideControl);
        App.removeCallbacks(mR1);
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        mBinding.control.action.extra.setVisibility(View.GONE);
        updatePlayPause();
        setSubtitleLift(true);
    }

    @Override
    public void onSeekEnd(long time) {
        seeking = false;
        mKeyDown.reset();
        App.removeCallbacks(mHideControl);
        App.post(mHideControl, 1800);
    }

    @Override
    public void onSpeedUp() {
        if (!player().isPlaying()) return;
        PlaybackAction.startSpeedPress(player(), mBinding.widget.message);
    }

    @Override
    public void onSpeedEnd() {
        PlaybackAction.hideSpeedHint(mBinding.widget.message);
        player().setSpeed(SpeedSetting.getPlayback());
    }

    @Override
    public void onKeyUp() {
        showControl(mBinding.control.action.playPause);
    }

    @Override
    public void onKeyDown() {
        showControl(mBinding.control.action.playPause);
    }

    @Override
    public void onKeyCenter() {
        // OK 键行为可配：默认播放/暂停；选「呼出菜单」时第一次按 OK 弹控制栏，再按才作用到焦点按钮。
        if (PlayerSetting.isOkMenu()) showControl(mBinding.control.action.playPause);
        else {
            onPlayPause();
            hideControl();
        }
    }

    @Override
    public void onSingleTap() {
        if (isFullscreen()) onToggle();
    }

    @Override
    public void onDoubleTap() {
        if (isFullscreen()) onKeyCenter();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 1001) PlaybackIntent.onExternalResult(data, service()::dispatchNext, controller()::seekTo);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mClock.stop().start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        seeking = false;
        App.removeCallbacks(mHideControl);
        saveHistory(false);
        if (PlayerSetting.isBackgroundOff()) mClock.stop();
    }

    @Override
    protected void onBackInvoked() {
        if (isVisible(mBinding.control.getRoot())) {
            hideControl();
        } else if (isVisible(mBinding.widget.center)) {
            hideCenter();
        } else if (isFullscreen()) {
            exitFullscreen();
        } else {
            mViewModel.stopSearch();
            if (isTaskRoot()) startActivity(new Intent(this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            super.onBackInvoked();
        }
    }

    @Override
    protected void onDestroy() {
        mClock.release();
        saveHistory(true);
        DanmakuApi.cancel();
        RefreshEvent.keep();
        App.removeCallbacks(mR1, mR2, mR3, mR4, mR5);
        App.removeCallbacks(mHideControl);
        super.onDestroy();
    }
}
