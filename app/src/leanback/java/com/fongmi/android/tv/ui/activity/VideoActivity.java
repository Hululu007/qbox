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
import androidx.leanback.widget.HorizontalGridView;
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
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.setting.DecodeSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.setting.SpeedSetting;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.adapter.EpisodeGroupAdapter;
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
import com.fongmi.android.tv.utils.EpisodeInfo;
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

public class VideoActivity extends PlaybackActivity implements VodPlaybackHost, CustomKeyDownVod.Listener, ParseDialog.Listener, EpisodeGroupAdapter.OnClickListener, FlagAdapter.OnClickListener, EpisodeAdapter.OnClickListener, QualityAdapter.OnClickListener, QuickAdapter.OnClickListener, Clock.Callback {

    private ActivityVideoBinding mBinding;
    private VideoViewModel mViewModel;
    private VodPlaybackController mVod;
    private FlagAdapter mFlagAdapter;
    private EpisodeAdapter mEpisodeAdapter;
    private EpisodeGroupAdapter mEpisodeGroupAdapter;
    private java.util.List<com.fongmi.android.tv.bean.Episode> mEpisodeItems;
    private QualityAdapter mQualityAdapter;
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
    private long mLastFlip;
    private int mKeepGroup = -1;
    // GS-78：renderDetail 只在首次进入设一次初始焦点；点集重入不再把焦点抢到播放框。
    private boolean mDetailFocusSet;
    private int mFlagSwitchPosition = -1;
    private History mHistory;
    private boolean fullscreen;
    private boolean useParse;

    private boolean seeking;
    private boolean seekFocus;
    private long seekBase;
    /** 进度条拖动：进入拖动前的按钮（退出时归还焦点）；本次 DOWN 链的 repeat 计数（0=单点）。 */
    private View mScrubReturn;
    private int scrubRepeats;
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
        // B：curveMode/setCurveMode 已删，无需再读 debug_seek_curve。
        if (com.fongmi.android.tv.BuildConfig.DEBUG) {
            androidx.core.content.ContextCompat.registerReceiver(this, mBackdropVariantReceiver, new android.content.IntentFilter("com.qbox.app.DEBUG_BACKDROP"), androidx.core.content.ContextCompat.RECEIVER_EXPORTED);
            applyBackdropVariant(com.github.catvod.utils.Prefers.getInt("debug_backdrop_variant", BACKDROP_VARIANT_DEFAULT));
        }
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
        mBinding.control.action.speed.setUpListener(() -> stepSpeed(true));
        mBinding.control.action.speed.setDownListener(() -> stepSpeed(false));
        mBinding.control.action.next.setOnClickListener(view -> checkNext());
        mBinding.control.action.prev.setOnClickListener(view -> checkPrev());
        mBinding.control.action.scale.setOnClickListener(view -> onScale());
        mBinding.control.action.speed.setOnClickListener(view -> onSpeedSetting());
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
        mBinding.control.action.ending.setOnLongClickListener(view -> onEndingReset());
        mBinding.control.action.opening.setOnLongClickListener(view -> onOpeningReset());
        mBinding.control.action.statsButton.setOnClickListener(view -> onStats());
        mBinding.stats.getRoot().setOnClickListener(view -> onStats());
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
        // 换源：光标停在某个源上「停稳」才切，快速左右扫过不切，避免跟光标抢（用户 2026-11-13 反馈）。
        mBinding.flag.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                scheduleFlagSwitch(position);
            }
        });
    }

    /** 焦点在源的胶囊上停 600ms 才真正换源；期间再移动就取消，人的控制权优先。 */
    private void scheduleFlagSwitch(int position) {
        App.removeCallbacks(mFlagSwitch);
        mFlagSwitchPosition = position;
        App.post(mFlagSwitch, 600);
    }

    private final Runnable mFlagSwitch = () -> {
        if (mFlagAdapter == null || mFlagSwitchPosition < 0 || mFlagSwitchPosition >= mFlagAdapter.getItemCount()) return;
        if (mBinding.flag.hasFocus() || mBinding.flag.getSelectedPosition() == mFlagSwitchPosition) {
            onItemClick(mFlagAdapter.get(mFlagSwitchPosition));
        }
    };

    private void setRecyclerView() {
        mBinding.flag.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.flag.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.flag.setAdapter(mFlagAdapter = new FlagAdapter(this));
        mBinding.episodeGroup.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.episodeGroup.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.episodeGroup.setAdapter(mEpisodeGroupAdapter = new EpisodeGroupAdapter(this));
        mBinding.episode.setAdapter(mEpisodeAdapter = new EpisodeAdapter(this));
        mBinding.quality.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.quality.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.quality.setAdapter(mQualityAdapter = new QualityAdapter(this));
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
        // 进度条拖动选中态：只用颜色表达——已播放段与圆球变标记色（qbox_accent），
        // 圆球保持原尺寸；禁缩放整个控件（会把文字/圆球拉变形）、禁白边框（P0 用户打回）。
        mBinding.control.seekBox.setOnFocusChangeListener((v, hasFocus) -> {
            if (mBinding.control.seek.getTimeBar() instanceof androidx.media3.ui.DefaultTimeBar bar) {
                int marker = getColor(com.fongmi.android.tv.R.color.qbox_accent);
                bar.setPlayedColor(hasFocus ? marker : 0xFFFFFFFF);
                bar.setScrubberColor(hasFocus ? marker : 0xFFFFFFFF);
            }
            if (!hasFocus) mKeyDown.setScrubMode(false);
        });
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
    public void onPlaybackRequested() {
        showProgress();
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
        // GS-78：只在首次进入设一次初始焦点（GS-15）。点集/换源重入 renderDetail 时不再抢焦点，
        // 否则每次切集焦点都被拉回播放框（GS-19：自动逻辑不得改变用户当前焦点）。
        if (!mDetailFocusSet) {
            mDetailFocusSet = true;
            mBinding.video.requestFocus();
        }
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
        if (com.fongmi.android.tv.BuildConfig.DEBUG) {
            Site site = getSite();
            StringBuilder sb = new StringBuilder();
            for (Flag item : items) sb.append("[").append(item.getFlag()).append("] ");
            android.util.Log.d("QBoxFlags", "site=" + (site == null ? "?" : site.getName()) + " count=" + items.size() + " " + sb);
        }
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
        // GS-78：不用 notifyDataSetChanged（会重建 ViewHolder 丢焦点，被系统就近挑到 video 播放框）。
        // 只就地刷新可见格的 activated 态，焦点自然保持在原地，方便用户连续切换集。
        for (int i = 0; i < mBinding.episode.getChildCount(); i++) {
            View cell = mBinding.episode.getChildAt(i);
            int pos = mBinding.episode.getChildAdapterPosition(cell);
            if (pos != RecyclerView.NO_POSITION) cell.setActivated(mEpisodeAdapter.isSelectedAt(pos));
        }
        showEpisodePage(mEpisodeAdapter.getPosition());
        // GS-79：setPage 的 notifyDataSetChanged 因 EpisodeAdapter 稳定 id 让焦点格被 scrap 复用、
        // 不被 dispatchLayoutStep3 清除，框架不发生 rootViewRequestFocus，焦点自然保持原地。
    }

    @Override
    public void renderReverseEpisodes(List<Episode> items, boolean scroll) {
        setEpisodeAdapter(items);
        if (scroll) showEpisodePage(mEpisodeAdapter.getPosition());
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
        mQuickAdapter.clear();
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
        // 换源会重建线路/分组/选集三块数据，notifyDataSetChanged 会把焦点整个丢掉——
        // 丢掉的焦点会被系统重新就近挑一个，于是「一移过去就被切到下一个」，跟光标抢。
        // 这里在重建后把焦点还给用户原来所在的行（GS-72②）。
        View before = getCurrentFocus();
        boolean inFlag = before != null && isInside(mBinding.flag, before);
        boolean inGroup = before != null && isInside(mBinding.episodeGroup, before);
        boolean inEpisode = before != null && isInside(mBinding.episode, before);
        mVod.selectFlag(item);
        if (!inFlag && !inGroup && !inEpisode) return;
        App.post(() -> {
            // notifyDataSetChanged 之后旧 View 会先保持住焦点、再被回收，所以这里要二次确认：
            // 只要焦点不在原来那三行里（说明被丢掉了），就把焦点还给原来的行。
            View now = getCurrentFocus();
            if (now != null && (isInside(mBinding.flag, now) || isInside(mBinding.episodeGroup, now) || isInside(mBinding.episode, now))) return;
            if (inFlag) focusGrid(mBinding.flag);
            else if (inGroup) focusGrid(mBinding.episodeGroup);
            else focusEpisodeGrid();
        });
    }

    private void setEpisodeAdapter(List<Episode> items) {
        mEpisodeItems = items;
        mBinding.episode.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        // 只有一集时不需要「选集」入口。
        mBinding.control.action.chapter.setVisibility(items.size() > 1 ? View.VISIBLE : View.GONE);
        // 播放顺序始终跟随显示顺序（不再有单独的「正序播放/倒序播放」开关）。
        if (mHistory != null && mHistory.isRevPlay()) mVod.setRevPlay(false);
        mEpisodeAdapter.addAll(items);
        // 分组条：0 号是「正序/倒序」小标志，后面才是各组。
        applyEpisodeLayoutManager();
        setEpisodeGroups();
        mEpisodeGroupAdapter.setDesc(mHistory != null && mHistory.isRevSort());
        mBinding.episodeGroup.setVisibility(items.size() > 1 ? View.VISIBLE : View.GONE);
        if (mKeepGroup >= 0) {
            int group = Math.min(mKeepGroup, Math.max(0, mEpisodeGroupAdapter.getGroupCount() - 1));
            mKeepGroup = -1;
            showEpisodePage(group, 0, false);
        } else {
            showEpisodePage(mEpisodeAdapter.getPosition());
        }
        setR2Callback();
    }

    /** 选集网格用纵向 GridLayoutManager：第一排从左往右填满，再换第二排（不是 Leanback 的按列填）。
     *  列数随排版方案变化时只调 spanCount，不再重建 LayoutManager（重建会丢掉 RecyclerView 的焦点/滚动状态）。 */
    private void applyEpisodeLayoutManager() {
        int columns = Math.max(1, mEpisodeAdapter.getColumns());
        androidx.recyclerview.widget.RecyclerView.LayoutManager current = mBinding.episode.getLayoutManager();
        if (current instanceof androidx.recyclerview.widget.GridLayoutManager) {
            ((androidx.recyclerview.widget.GridLayoutManager) current).setSpanCount(columns);
            return;
        }
        androidx.recyclerview.widget.GridLayoutManager manager = new androidx.recyclerview.widget.GridLayoutManager(this, columns);
        manager.setOrientation(androidx.recyclerview.widget.GridLayoutManager.VERTICAL);
        mBinding.episode.setLayoutManager(manager);
        mBinding.episode.setItemAnimator(null);
    }

    /** 选集分组：按排版方案实时计算，一组 = 一页（几列 × 几行）。 */
    private void setEpisodeGroups() {
        List<EpisodeGroupAdapter.Group> groups = new ArrayList<>();
        EpisodeInfo.Plan plan = mEpisodeAdapter.getPlan();
        if (plan != null) {
            int count = mEpisodeAdapter.getTotalCount();
            for (int start = 0; start < count; start += plan.groupSize) {
                int end = Math.min(start + plan.groupSize, count);
                int low = start + 1;
                String label = low == end ? String.valueOf(end) : low + "-" + end;
                groups.add(new EpisodeGroupAdapter.Group(label, start, end));
            }
        }
        mEpisodeGroupAdapter.addAll(groups);
    }

    /** 当前集所在分组即当前页：切页时同步上方组标和选集网格。 */
    private void showEpisodePage(int absolutePosition) {
        if (mEpisodeGroupAdapter == null || mEpisodeGroupAdapter.getGroupCount() == 0) return;
        int index = mEpisodeGroupAdapter.indexOf(absolutePosition);
        if (index < 0) return;
        EpisodeGroupAdapter.Group group = mEpisodeGroupAdapter.get(index);
        mEpisodeGroupAdapter.setSelected(index);
        mEpisodeAdapter.setPage(group.getStart(), group.getEnd());
        int local = Math.max(0, Math.min(absolutePosition - group.getStart(), group.getEnd() - group.getStart() - 1));
        focusEpisode(local, false);
        if (isVisible(mBinding.episodeGroup)) mBinding.episodeGroup.setSelectedPosition(index + EpisodeGroupAdapter.OFFSET);
    }

    /** 按组切页并聚焦指定页内位置；翻页、点组标都走这里。 */
    private void showEpisodePage(int groupIndex, int localPosition, boolean focus) {
        if (mEpisodeGroupAdapter == null || groupIndex < 0 || groupIndex >= mEpisodeGroupAdapter.getGroupCount()) return;
        EpisodeGroupAdapter.Group group = mEpisodeGroupAdapter.get(groupIndex);
        mEpisodeGroupAdapter.setSelected(groupIndex);
        int size = group.getEnd() - group.getStart();
        int local = Math.max(0, Math.min(localPosition, size - 1));
        // GS-79：换页会改变 itemCount。只有当前焦点格会被裁掉（local 超出新 itemCount）时，
        // 才先把焦点移到目标格（目标 < size，必存活），避免焦点被 remove 后由框架
        // rootViewRequestFocus 挑到 video；否则保持焦点不动，避免「中间落点」造成视觉跳动
        // （不满页↔满页切换时 target 在旧页会被 clamp 成另一格，形成闪）。
        if (focus) {
            int current = getEpisodePosition(getCurrentFocus());
            if (current >= size) focusEpisode(local, true);
        }
        mEpisodeAdapter.setPage(group.getStart(), group.getEnd());
        if (isVisible(mBinding.episodeGroup)) mBinding.episodeGroup.setSelectedPosition(groupIndex + EpisodeGroupAdapter.OFFSET);
        if (focus) focusEpisode(local, true);
        else focusEpisode(local, false);
    }

    @Override
    public void onGroupClick(EpisodeGroupAdapter.Group item) {
        if (mEpisodeGroupAdapter == null || mEpisodeAdapter.getTotalCount() == 0) return;
        int index = mEpisodeGroupAdapter.indexOf(item.getStart());
        if (index < 0) return;
        showEpisodePage(index, 0, true);
    }

    @Override
    public void onSortClick() {
        onRevSort();
    }

    /** 主按钮「播放」：直接播第一个可播剧集；没有剧集时先选中第一条线路。 */
    private void onPrimaryPlay() {
        if (mEpisodeItems != null && !mEpisodeItems.isEmpty()) {
            // 播放按钮：当前集已在播，直接进全屏；否则播第一集（原语义保留）。
            Episode first = mEpisodeItems.get(0);
            if (!isFullscreen() && first.isSelected()) enterFullscreen();
            else onItemClick(first);
        } else if (mFlagAdapter != null && mFlagAdapter.getItemCount() > 0) onItemClick(mFlagAdapter.get(0));
    }

    @Override
    public void onItemClick(Episode item) {
        // GS-78/79：选集格只切集，焦点保持原地方便连续切换；不抢焦点、不自动进全屏。
        // 焦点保持由 EpisodeAdapter 稳定 id（GS-79）从机制上保证，无需守卫/状态位。
        // 但若点击的集就是当前正在播放的集，则进入全屏（恢复原交互语义）。
        if (item.isSelected() && !isFullscreen()) {
            enterFullscreen();
            return;
        }
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

    private int findFocusDown(int index) {
        List<Integer> orders = Arrays.asList(R.id.flag, R.id.quality, R.id.episode_group, R.id.episode, R.id.part, R.id.quick);
        for (int i = 0; i < orders.size(); i++) if (i > index) if (isVisible(findViewById(orders.get(i)))) return orders.get(i);
        return 0;
    }

    private int findFocusUp(int index) {
        List<Integer> orders = Arrays.asList(R.id.flag, R.id.quality, R.id.episode_group, R.id.episode, R.id.part, R.id.quick);
        for (int i = orders.size() - 1; i >= 0; i--) if (i < index) if (isVisible(findViewById(orders.get(i)))) return orders.get(i);
        return 0;
    }

    private void updateFocus() {
        mPartAdapter.setNextFocusUp(findFocusUp(4));
        mEpisodeAdapter.setNextFocusUp(findFocusUp(3));
        mEpisodeAdapter.setNextFocusDown(findFocusDown(3));
        mEpisodeGroupAdapter.setNextFocusUp(findFocusUp(2));
        mEpisodeGroupAdapter.setNextFocusDown(findFocusDown(2));
        mQualityAdapter.setNextFocusDown(findFocusDown(1));
        mFlagAdapter.setNextFocusDown(findFocusDown(0));
        notifyItemChanged(mBinding.episodeGroup, mEpisodeGroupAdapter);
        notifyItemChanged(mBinding.quality, mQualityAdapter);
        notifyItemChanged(mBinding.episode, mEpisodeAdapter);
        notifyItemChanged(mBinding.part, mPartAdapter);
        notifyItemChanged(mBinding.flag, mFlagAdapter);
    }

    /** 排序标志（分组条最前面的小按钮）：切换正序 / 倒序排列，播放顺序始终跟随显示顺序。 */
    public void onRevSort() {
        int group = mEpisodeGroupAdapter == null ? 0 : mEpisodeGroupAdapter.getSelected();
        boolean desc = mHistory == null || !mHistory.isRevSort();
        mVod.setRevSort(desc);
        mKeepGroup = group;
        mVod.sortEpisodes(desc);
        mEpisodeGroupAdapter.setDesc(desc);
        // 排序后保持焦点在「正序/倒序」小标志上，不跳到组标或选集。
        mBinding.episodeGroup.setSelectedPosition(0, holder -> holder.itemView.requestFocus());
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
        Notify.show(getString(R.string.play_repeat) + getString(player().isRepeatOne() ? R.string.play_repeat_on : R.string.play_repeat_off));
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

    private void onScale() {
        int index = getScale();
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        setScale(index == array.length - 1 ? 0 : ++index);
    }

    /**
     * 倍速为「可调值类」控件：聚焦时 ↑/↓ 沿 PRESETS 就地调档（即时生效、不弹窗、不动焦点），
     * OK/点击打开倍速面板（onSpeedSetting）。到顶/到底不环绕，防止误按跳变。
     * CustomUpDownView.dispatchKeyEvent 已消费 ↑/↓，不会串到快进/快退（GS 防串键规则）。
     */
    private void stepSpeed(boolean up) {
        float current = player().getSpeed();
        float next = up ? SpeedSetting.next(current) : SpeedSetting.prev(current);
        if (up && next < current) next = SpeedSetting.MAX;   // next() 到顶环绕回 MIN，这里改成钉住 MAX
        if (!up && next > current) next = SpeedSetting.MIN;  // prev() 同理钉住 MIN
        player().setSpeed(next);
        SpeedSetting.putPlayback(next);
        updateSpeedButton();
        Notify.show(getString(R.string.play_speed_toast, SpeedSetting.formatValue(next)));
    }

    private void updateSpeedButton() {
        // 切换类按钮状态规则（N-28）：非 1x 时按钮直接显示当前倍速，1x 时回到简洁文案。
        float speed = player().getSpeed();
        mBinding.control.action.speed.setText(Math.abs(speed - 1f) < 0.01f ? getString(R.string.play_speed) : SpeedSetting.format(speed));
    }

    /** 精细倍速面板（倍速按钮 OK 或「更多」二级行进入）。 */
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
        // 用户拍板（2026-11-14）：播放中的「章节」入口必须是选集——媒体章节（player chapters）基本为空，
        // 点了什么都不出现，这就是「全屏里无法选集」的死路。有多集时退回详情页并聚焦当前集所在格。
        if (mEpisodeAdapter != null && mEpisodeAdapter.getTotalCount() > 1) {
            if (isFullscreen()) exitFullscreen();
            App.post(() -> focusEpisodeGrid(), 250);
        } else {
            ChapterDialog.create().player(player()).show(this);
        }
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
        // 加载态 = 简易转圈（view_progress.loading_cube 为 CircularProgressIndicator）+ 实时网速（mR3/setTraffic）。
        // 详情页点播放（requestPlayer）与 buffering/快进（onSeeking）共用此入口。
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

    private void hideInfo() {
        mBinding.widget.top.setVisibility(View.GONE);
        mBinding.widget.center.setVisibility(View.GONE);
    }

    /** 收起动画代次：hideControl 的分段渐隐结束后才 GONE；期间重新展开时用代次使旧的结束回调失效。 */
    private int mControlAnimSeq;

    private void showControl(View view) {
        seeking = false;
        seekFocus = false;
        App.removeCallbacks(mHideControl);
        // 展开即恢复完整态：按钮列可见（seek 精简态把它 GONE 了），并清掉进行中的收起动画。
        ++mControlAnimSeq;
        View root = mBinding.control.getRoot();
        View action = mBinding.control.action.getRoot();
        root.animate().cancel();
        action.animate().cancel();
        root.setAlpha(1f);
        action.setAlpha(1f);
        action.setVisibility(View.VISIBLE);
        root.setVisibility(View.VISIBLE);
        updateSpeedButton();
        updateTrackLabels();
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
                // 末位保险：目标不可见时回到恒在的「下集」，保证控制条呼出必有焦点。
                if (focus == null || !focus.isShown()) focus = mBinding.control.action.next;
                focus.requestFocus();
            }
        });
    }

    private void hideControl() {
        seeking = false;
        seekFocus = false;
        App.removeCallbacks(mHideControl);
        final View root = mBinding.control.getRoot();
        if (root.getVisibility() != View.VISIBLE) return;
        final View action = mBinding.control.action.getRoot();
        final int seq = ++mControlAnimSeq;
        root.animate().cancel();
        action.animate().cancel();
        // 分段渐隐：先收下面按钮列，再整体（标题 + 进度条）渐隐，不一次性整块消失。
        // 结束回调带代次守卫：渐隐途中重新展开时旧回调不得把面板 GONE 掉（cancel 也会触发 withEndAction）。
        if (action.getVisibility() == View.VISIBLE) action.animate().alpha(0f).setDuration(150).start();
        root.animate().alpha(0f).setStartDelay(action.getVisibility() == View.VISIBLE ? 120 : 0).setDuration(180).withEndAction(() -> {
            if (seq != mControlAnimSeq) return;
            root.setVisibility(View.GONE);
            root.setAlpha(1f);
            action.setAlpha(1f);
            action.setVisibility(View.VISIBLE);
            setSubtitleLift(false);
        }).start();
        App.removeCallbacks(mR1);
    }

    /** 控制条弹出来时把字幕抬高，避免字幕和「上集/下集/倍速…」压在同一个位置。 */
    private void setSubtitleLift(boolean lift) {
        try {
            if (mBinding.player.getSubtitleView() != null) mBinding.player.getSubtitleView().setBottomPaddingFraction(lift ? 0.2f : 0.08f);
        } catch (Throwable ignored) {
        }
    }

    private void hideCenter() {
        mBinding.widget.action.setImageResource(R.drawable.ic_widget_play);
        hideInfo();
    }

    /** 用户主动暂停：必须已离开 buffering/idle/ended，且 playWhenReady 确实为 false。
     *  seek 过程中播放器会短暂 isPlaying=false，但 playWhenReady 仍为 true，不能当成暂停。 */
    private boolean isPausedByUser() {
        return service() != null && !isIdle() && !isBuffering() && !isEnded() && !player().isPlayWhenReady();
    }

    /** 暂停中央标志：只保留暂停图标；进度条本身已显示时间，不再重复时间和背景。 */
    private void showPausedCenter() {
        if (mBinding == null || !isPausedByUser()) return;
        try {
            mBinding.widget.action.setImageResource(R.drawable.ic_widget_pause);
            mBinding.widget.center.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {
        }
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

    /** 详情页氛围底图：源海报中心 16:9 横带，保持 480×270 小尺寸解码（方向：不提清晰度，
     *  用 BackdropBlur 多级降采样+模糊+抖动把放大颗粒抹平，只要色调/明暗氛围）。
     *  fitCenter 完整海报叠加层被用户判为「牛皮癣」，已整体移除，回到纯模糊铺满。 */
    private void setBackdrop(String url) {
        try {
            if (TextUtils.isEmpty(url)) return;
            Glide.with(this).asBitmap().load(ImgUtil.getUrl(url)).override(480, 270)
                    .transform(new com.bumptech.glide.load.MultiTransformation<>(new com.bumptech.glide.load.resource.bitmap.CenterCrop(), new com.fongmi.android.tv.utils.BackdropBlur()))
                    .transition(com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade(700))
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

        @Override
        public void onMediaSeek(KeyEvent event, boolean forward) {
            // FF/REW 与左右键同一套斜坡 + 同一条 seek UI；只在全屏播放时响应。
            if (!isFullscreen() || service() == null) return;
            if (isGone(mBinding.control.getRoot())) enterSeekMode();
            mKeyDown.onMediaKey(event, forward);
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
                // seek/buffering 期间暂停中央标志被 showProgress 收起，READY 后仅在用户确实暂停时恢复它。
                if (isPausedByUser()) showPausedCenter();
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
        if (isPlaying) {
            mBinding.poster.setVisibility(View.GONE);
            hideCenter();
        } else if (isPausedByUser()) {
            // 暂停顶部去标题留时钟——标题/时间底部精简态已有，
            // 但右上角时钟用户明确要留：top 只亮时钟，标题/分辨率保持 GONE。
            mBinding.widget.title.setVisibility(View.GONE);
            mBinding.widget.size.setVisibility(View.GONE);
            mBinding.widget.top.setVisibility(View.VISIBLE);
            // 暂停中央标志（用户拍板）：屏幕中央显示暂停图标 + 当前/总时长，用户一眼知道已暂停。
            showPausedCenter();
        }
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

    /** OK 键 = 暂停/播放（菜单不再有独立按钮，与 fongmi 一致）。 */
    private void onPlayPause() {
        if (service() == null) return;
        // 播完以后再按，用户想的是「重播」，不是「暂停一个已经停住的画面」。
        if (isEnded()) {
            onReplay();
            return;
        }
        boolean playing = player().isPlayWhenReady();
        if (playing) onPaused();
        else if (player().isEmpty()) onRefresh();
        else onPlay();
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
        if (mBinding.control.action.chapter.isShown()) return mBinding.control.action.chapter;
        // 默认落点与 fongmi 一致：行首「下集」。
        return mBinding.control.action.next;
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
            // seekBox 不记：它是拖动选中态的临时焦点位，归还焦点走 exitScrubFocus 的 mScrubReturn。
            if (panelFocused && !isInside(mBinding.control.seekBox, focus)) mFocus2 = focus;
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
        return super.dispatchKeyEvent(event);
    }

    /** 详情页纵向焦点闭环：播放器/动作行 → 线路 → 分组（含排序标志）→ 选集 → 线路分页 → 快捷行，反向逐级回退。 */
    private boolean routeDetailFocus(KeyEvent event) {
        View focus = getCurrentFocus();
        if (focus == null) return false;
        boolean down = event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN;
        boolean up = event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP;
        boolean left = event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT;
        boolean right = event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT;
        if (left || right) return isInside(mBinding.episode, focus) && routeEpisodePage(right);
        if (!down && !up) return false;
        if (down) {
            if (isInside(mBinding.video, focus) || isInside(mBinding.row2, focus))
                return focusGrid(mBinding.flag) || focusGrid(mBinding.quality) || focusGrid(mBinding.episodeGroup) || focusEpisodeGrid() || focusGrid(mBinding.part);
            if (isInside(mBinding.flag, focus) || isInside(mBinding.quality, focus))
                return focusGrid(mBinding.episodeGroup) || focusEpisodeGrid() || focusGrid(mBinding.part);
            if (isInside(mBinding.episodeGroup, focus))
                return focusEpisodeGrid() || focusGrid(mBinding.part) || focusGrid(mBinding.quick);
            if (isInside(mBinding.episode, focus)) {
                int local = getEpisodePosition(focus);
                int columns = Math.max(1, mEpisodeAdapter.getColumns());
                if (local >= 0 && local + columns < mEpisodeAdapter.getPageSize()) {
                    focusEpisode(local + columns, true);
                    return true;
                }
                return focusGrid(mBinding.part) || focusGrid(mBinding.quick);
            }
            if (isInside(mBinding.part, focus)) return focusGrid(mBinding.quick);
            return false;
        }
        if (isInside(mBinding.episode, focus)) {
            int local = getEpisodePosition(focus);
            int columns = Math.max(1, mEpisodeAdapter.getColumns());
            if (local - columns >= 0) {
                focusEpisode(local - columns, true);
                return true;
            }
            return focusGrid(mBinding.episodeGroup) || focusGrid(mBinding.quality) || focusGrid(mBinding.flag) || mBinding.video.requestFocus();
        }
        if (isInside(mBinding.episodeGroup, focus))
            return focusGrid(mBinding.quality) || focusGrid(mBinding.flag) || mBinding.video.requestFocus();
        if (isInside(mBinding.flag, focus) || isInside(mBinding.quality, focus)) return mBinding.video.requestFocus();
        if (isInside(mBinding.row2, focus) || isInside(mBinding.video, focus)) return mBinding.video.requestFocus();
        if (isInside(mBinding.part, focus)) return focusEpisodeGrid() || focusGrid(mBinding.episodeGroup) || focusGrid(mBinding.flag) || mBinding.video.requestFocus();
        if (isInside(mBinding.quick, focus)) return focusGrid(mBinding.part) || focusEpisodeGrid();
        return false;
    }

    /** 选集格子在当前页内的位置（逻辑序号：第一排从左到右）。 */
    private int getEpisodePosition(View focus) {
        if (focus == null) return -1;
        int position = mBinding.episode.getChildAdapterPosition(focus);
        if (position < 0) {
            RecyclerView.ViewHolder holder = mBinding.episode.findContainingViewHolder(focus);
            if (holder != null) position = holder.getBindingAdapterPosition();
        }
        return position;
    }

    /** 从别的行进入选集网格：落在当前集；当前集不在本页时先切到它所在的页再聚焦（GS-72：焦点不落错格）。 */
    private boolean focusEpisodeGrid() {
        if (mEpisodeAdapter == null || mEpisodeAdapter.getPageSize() <= 0) {
            logFocus("focusEpisodeGrid SKIP adapter=" + (mEpisodeAdapter == null ? "null" : "pageSize=" + mEpisodeAdapter.getPageSize()) + " episodeVis=" + (mBinding.episode.getVisibility() == View.VISIBLE));
            return false;
        }
        int local = mEpisodeAdapter.getLocalPosition();
        if (local < 0) {
            showEpisodePage(mEpisodeAdapter.getPosition());
            local = mEpisodeAdapter.getLocalPosition();
        }
        return focusEpisode(Math.max(0, local), true);
    }

    /** 页内第 local 个选集（逻辑序号：第一排从左到右，满了接第二排）；focus=true 时把焦点落上去。
     *  先 scrollToPosition 保证目标格被布局出来（页面高度不够时 findViewHolder 会拿不到）。 */
    private boolean focusEpisode(int local, boolean focus) {
        if (mEpisodeAdapter == null || mEpisodeAdapter.getPageSize() <= 0) return false;
        int target = Math.max(0, Math.min(local, mEpisodeAdapter.getPageSize() - 1));
        if (!focus) return true;
        mBinding.episode.scrollToPosition(target);
        RecyclerView.ViewHolder holder = mBinding.episode.findViewHolderForAdapterPosition(target);
        if (holder != null && holder.itemView.requestFocus()) return true;
        logFocus("focusEpisode retry target=" + target + " holder=" + (holder == null ? "null" : "ok") + " vis=" + (mBinding.episode.getVisibility() == View.VISIBLE));
        // 落点必须在「布局完成之后」再交给目标格：setPage 的 notifyDataSetChanged 会让目标格
        // 尚未创建，post/postOnAnimation 都早于本次 layout（Choreographer 的 ANIMATION 相位在
        // TRAVERSAL 之前），findViewHolderForAdapterPosition 仍返回 null，requestFocus 静默失败，
        // 光标会停在旧页被 scrap 复用的格子上。OneShotPreDrawListener 在 layout 后、draw 前触发。
        androidx.core.view.OneShotPreDrawListener.add(mBinding.episode, () -> {
            RecyclerView.ViewHolder item = mBinding.episode.findViewHolderForAdapterPosition(target);
            if (item != null) item.itemView.requestFocus();
            logFocus("focusEpisode preDrawRetry target=" + target + " holder=" + (item == null ? "null" : "ok") + " focused=" + (getCurrentFocus() != null));
        });
        return true;
    }

    private static void logFocus(String msg) {
        if (com.fongmi.android.tv.BuildConfig.DEBUG) android.util.Log.d("QBoxFocus", msg);
    }

    /**
     * 选集左右移动/翻页：页内同一排左右移动；本页最右再按右 → 下一组同一排第一集；
     * 本页最左再按左 → 上一组同一排最后一集。组标亮度同步。
     */
    private boolean routeEpisodePage(boolean forward) {
        if (mEpisodeGroupAdapter == null || mEpisodeGroupAdapter.getGroupCount() == 0) return false;
        View focus = getCurrentFocus();
        int local = getEpisodePosition(focus);
        if (local < 0) return false;
        int columns = Math.max(1, mEpisodeAdapter.getColumns());
        int row = local / columns;
        int col = local % columns;
        int pageSize = mEpisodeAdapter.getPageSize();
        int groupIndex = mEpisodeGroupAdapter.getSelected();
        // GS-78「立体运动」：左右只在「本行」内走（col 推进），上下换行交给 routeDetailFocus；
        // 走到本行最右/最左（或页末/页首）即在分组边缘翻到相邻分组「同排」临近位置。
        // 旧实现用 local±1 页内线性推进，把「第一行最右 →」错跳到第二行第一，而不是翻页。
        if (forward) {
            if (col < columns - 1 && local + 1 < pageSize) {
                focusEpisode(local + 1, true);
                return true;
            }
            long now = android.os.SystemClock.uptimeMillis();
            if (now - mLastFlip < 350) return true;
            int next = groupIndex + 1;
            if (next >= mEpisodeGroupAdapter.getGroupCount()) return true;
            int size = mEpisodeAdapter.getGroupSize(next);
            // 下一组「同行第一集」；若下一组没有该行（不满页），退化到组开头（相邻），见 EpisodeInfo.forwardLanding。
            showEpisodePage(next, com.fongmi.android.tv.utils.EpisodeInfo.forwardLanding(row, columns, size), true);
            mLastFlip = now;
        } else {
            if (col > 0) {
                focusEpisode(local - 1, true);
                return true;
            }
            long now = android.os.SystemClock.uptimeMillis();
            if (now - mLastFlip < 350) return true;
            int prev = groupIndex - 1;
            if (prev < 0) return true;
            int size = mEpisodeAdapter.getGroupSize(prev);
            showEpisodePage(prev, com.fongmi.android.tv.utils.EpisodeInfo.backwardLanding(row, columns, size), true);
            mLastFlip = now;
        }
        return true;
    }


    /** 聚焦到某个横向行里的当前选中 item（网格容器本身不可聚焦，不能直接 requestFocus）。 */
    private boolean focusGrid(HorizontalGridView grid) {
        if (grid == null || grid.getVisibility() != View.VISIBLE) return false;
        int position = Math.max(0, grid.getSelectedPosition());
        RecyclerView.ViewHolder holder = grid.findViewHolderForAdapterPosition(position);
        if (holder != null && holder.itemView.requestFocus()) return true;
        grid.setSelectedPosition(position, h -> h.itemView.requestFocus());
        return true;
    }

    /**
     * 控制栏焦点闭环：左右到行尾不丢焦点；上键进入进度条，下键回到动作行。
     * CustomUpDownView（倍速/字幕/片头片尾）自己要吃上下键调值，这里直接放行。
     */
    private boolean onControlKey(KeyEvent event) {
        View focus = getCurrentFocus();
        if (focus == null) return false;
        if (isInside(mBinding.control.seekBox, focus)) return onSeekBarKey(event);
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (!isInside(mBinding.control.action.main, focus)) return false;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT: {
                boolean forward = event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT;
                View target = findFocusable(mBinding.control.action.main, focus, forward);
                // 行内巡回——最左再按左跳到最右，最右再按右回到最左（仅限本动作行，其它行边界行为不动）。
                if (target == null) target = findEdgeFocusable(mBinding.control.action.main, forward);
                if (target != null) target.requestFocus();
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_UP:
                // 可调值类（倍速/片头/片尾，CustomUpDownView）：↑ 放行给控件就地调档（其 dispatch 消费，不串键）。
                // 其余按钮：↑ 进进度条拖动选中态。
                if (focus instanceof CustomUpDownView) return false;
                enterScrubFocus(focus);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (focus instanceof CustomUpDownView) return false;
                return true;
            default:
                return false;
        }
    }

    /** 控制条「上」进进度条：记住来处按钮，进入拖动选中态（变色 + 圆球放大由 focus 监听统一施加）。 */
    private void enterScrubFocus(View from) {
        mScrubReturn = from;
        seekFocus = true;
        mKeyDown.setScrubMode(true);
        mBinding.control.seekBox.requestFocus();
    }

    /** 退出拖动：焦点归还进入前按钮（不丢焦点）；选中态视觉由 focus 监听自动还原。 */
    private void exitScrubFocus() {
        seekFocus = false;
        mKeyDown.setScrubMode(false);
        View target = (mScrubReturn != null && mScrubReturn.isShown()) ? mScrubReturn : mBinding.control.action.next;
        target.requestFocus();
        mScrubReturn = null;
    }

    /** 进度条聚焦后的按键：
        按住左右 = 与全局快进同一条阻尼曲线（CustomKeyDownVod 拖动模式，逐 tick 落位、progress 实时跟随）；
        单点（DOWN→UP 无 repeat）= 固定百分比步长：±1% 片长，钳 5-30s（依据见 uiux-review-log 36/37 业内对照与选型）；
        ↓/BACK 退出并回到进入前按钮；OK=播放/暂停；左右被消费不脱离进度条焦点。 */
    private boolean onSeekBarKey(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT: {
                boolean forward = event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT;
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getRepeatCount() == 0) scrubRepeats = 0;
                    else scrubRepeats++;
                    mKeyDown.onKeyDown(event);
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (scrubRepeats == 0) {
                        // 单点：固定百分比步长（±1% 片长，钳 5s-30s）。
                        long duration = Math.max(0, player().getDuration());
                        long step = duration > 0 ? Math.max(5_000, Math.min(30_000, duration / 100)) : 5_000;
                        seekBy(forward ? step : -step);
                        mKeyDown.reset();
                        if (com.fongmi.android.tv.BuildConfig.DEBUG) {
                            android.util.Log.d("QBoxSeek", "scrubTap step=" + step + " dur=" + duration);
                        }
                    } else {
                        // 按住释放：交还曲线链路（onSeekEnd 落位稳定）。
                        mKeyDown.onKeyDown(event);
                    }
                }
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_BACK:
                if (event.getAction() == KeyEvent.ACTION_DOWN) exitScrubFocus();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (event.getAction() == KeyEvent.ACTION_UP) onPlayPause();
                return true;
            default:
                return true;
        }
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

    /** 巡回端点：forward=true 取行内第一个可见可聚焦（最右再按右回最左），否则取最后一个（最左再按左跳最右）。 */
    private static View findEdgeFocusable(ViewGroup row, boolean forward) {
        int count = row.getChildCount();
        for (int k = 0; k < count; k++) {
            View child = row.getChildAt(forward ? k : count - 1 - k);
            if (child.isShown() && child.isFocusable()) return child;
        }
        return null;
    }

    private static boolean isInside(View ancestor, View view) {
        return com.fongmi.android.tv.utils.ViewUtil.isInside(ancestor, view);
    }

    @Override
    public void onSeeking(long time) {
        // QBox 1.9.8: 快进直接复用底部控制条（进度条 + 时间），不再另做浮层。
        if (!seeking) {
            seeking = true;
            seekBase = player().getPosition();
        }
        // 曲线 cap 片长挂钩（模式 B），每 tick 都注入（duration 就绪时序不定，只首 tick 注入实测会拿到 0）。
        mKeyDown.setDuration(Math.max(0, player().getDuration()));
        long duration = Math.max(0, player().getDuration());
        long target = Math.max(0, seekBase + time);
        if (duration > 0) target = Math.min(duration, target);
        controller().seekTo(target);
        mBinding.widget.center.setVisibility(View.GONE);
        // 快进/快退时显示转圈 + 实时网速（KB/s），让用户能判断是否已载入（fongmi 加载态同类）。
        showProgress();
    }

    /** 左右快进时把控制条「借」出来显示进度，但不抢焦点，方向键继续走快进。
     *  seek 触发的是精简态——只留「名称/信息行 + 进度条」，整排按钮 GONE，不占屏幕。 */
    private void enterSeekMode() {
        if (isVisible(mBinding.control.getRoot())) return;
        seeking = true;
        seekBase = player().getPosition();
        App.removeCallbacks(mHideControl);
        App.removeCallbacks(mR1);
        ++mControlAnimSeq;
        View root = mBinding.control.getRoot();
        root.animate().cancel();
        root.setAlpha(1f);
        mBinding.control.action.getRoot().setVisibility(View.GONE);
        root.setVisibility(View.VISIBLE);
        updateTrackLabels();
        setSubtitleLift(true);
    }

    /** 名称行右侧的源质量胶囊：分辨率/视频编码/HDR｜音频编码/声道。
     *  只显示能从 ExoPlayer Format 可靠拿到的项（sampleMimeType/height/channelCount/colorTransfer），
     *  拿不到或映射表外的一律不显示，不猜写、不凑占位；Atmos 只在 E-AC-3 JOC（audio/eac3-joc）时标出（码流级可靠信号）。 */
    private void updateTrackLabels() {
        java.util.List<String> video = new java.util.ArrayList<>();
        java.util.List<String> audio = new java.util.ArrayList<>();
        try {
            for (Tracks.Group group : player().getCurrentTracks().getGroups()) {
                for (int i = 0; i < group.length; i++) {
                    if (!group.isTrackSelected(i)) continue;
                    Format f = group.getTrackFormat(i);
                    if (group.getType() == C.TRACK_TYPE_VIDEO) {
                        if (f.height > 0) video.add(f.height + "p");
                        String vc = videoCodecLabel(f.sampleMimeType);
                        if (vc != null) video.add(vc);
                        String hdr = hdrLabel(f.colorInfo);
                        if (hdr != null) video.add(hdr);
                    } else if (group.getType() == C.TRACK_TYPE_AUDIO) {
                        String ac = audioCodecLabel(f.sampleMimeType);
                        if (ac != null) audio.add(ac);
                        String ch = channelLabel(f.channelCount);
                        if (ch != null) audio.add(ch);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        mBinding.control.trackVideo.setText(TextUtils.join(" · ", video));
        mBinding.control.trackVideo.setVisibility(video.isEmpty() ? View.GONE : View.VISIBLE);
        mBinding.control.trackAudio.setText(TextUtils.join(" · ", audio));
        mBinding.control.trackAudio.setVisibility(audio.isEmpty() ? View.GONE : View.VISIBLE);
        mBinding.control.trackRow.setVisibility(video.isEmpty() && audio.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String videoCodecLabel(String mime) {
        if (MimeTypes.VIDEO_H264.equals(mime)) return "H.264";
        if (MimeTypes.VIDEO_H265.equals(mime)) return "H.265";
        if (MimeTypes.VIDEO_DOLBY_VISION.equals(mime)) return "Dolby Vision";
        if (MimeTypes.VIDEO_AV1.equals(mime)) return "AV1";
        if (MimeTypes.VIDEO_VP9.equals(mime)) return "VP9";
        return null;
    }

    private String hdrLabel(@Nullable androidx.media3.common.ColorInfo color) {
        if (color == null) return null;
        if (color.colorTransfer == C.COLOR_TRANSFER_ST2084) return "HDR10";
        if (color.colorTransfer == C.COLOR_TRANSFER_HLG) return "HLG";
        return null;
    }

    /** 音频 mime 白名单（本 media3 分支 MimeTypes 音频常量不全，统一用字面量）。 */
    private String audioCodecLabel(String mime) {
        if (mime == null) return null;
        switch (mime) {
            case "audio/eac3-joc": return "Atmos";
            case "audio/eac3": return "DD+";
            case "audio/ac3": return "DD";
            case "audio/mp4a-latm":
            case "audio/mp4a.40.2": return "AAC";
            case "audio/truehd": return "TrueHD";
            case "audio/ac4": return "AC-4";
            case "audio/vnd.dts.hd": return "DTS-HD";
            case "audio/vnd.dts": return "DTS";
            default: return null;
        }
    }

    private String channelLabel(int count) {
        switch (count) {
            case 1: return "1.0";
            case 2: return "2.0";
            case 3: return "2.1";
            case 6: return "5.1";
            case 8: return "7.1";
            default: return null;
        }
    }

    @Override
    public void onSeekEnd(long time) {
        seeking = false;
        mKeyDown.reset();
        // seek 结束收起转圈/网速；若仍在 buffering，onStateChanged 会重新 showProgress。
        hideProgress();
        if (isPausedByUser()) showPausedCenter();
        App.removeCallbacks(mHideControl);
        // 进度条拖动释放：控制条是用户显式呼出的，回常规 5s 计时，不按「借出显示」1.8s 收起。
        if (seekFocus) setR1Callback();
        else App.post(mHideControl, 1800);
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
        showControl(getFocus2());
    }

    @Override
    public void onKeyDown() {
        showControl(getFocus2());
    }

    @Override
    public void onKeyCenter() {
        // （完全对齐 fongmi）：OK 直接暂停/播放，菜单不再有独立播放/暂停按钮。
        // 暂停时只出「快进快退那种」时间/状态精简显示（enterSeekMode：按钮行 GONE），恢复播放即收起。
        if (service() == null) return;
        boolean wasPlaying = !isEnded() && player().isPlayWhenReady();
        onPlayPause();
        if (wasPlaying) enterSeekMode();
        else hideControl();
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

    /** 详情页底图变体（DEBUG 对比用）：`adb shell am broadcast -a com.qbox.app.DEBUG_BACKDROP --ei v <0|1|2|3>` 运行时切换并持久化（Prefers），重启/重建详情页不丢。 */
    private static final int[] BACKDROP_SCRIM_VARIANTS = {R.drawable.shape_backdrop_scrim_a, R.drawable.shape_backdrop_scrim_b, R.drawable.shape_backdrop_scrim_c, R.drawable.shape_backdrop_scrim_d};
    private static final int[] DETAIL_SCRIM_VARIANTS = {R.drawable.shape_detail_scrim, R.drawable.shape_detail_scrim, R.drawable.shape_detail_scrim_light, R.drawable.shape_detail_scrim_d};
    private static final float[] BACKDROP_ALPHA_VARIANTS = {0.80f, 0.72f, 0.85f, 0.85f};
    /** 默认 D 版；改默认只动这里与 layout 三处引用。 */
    private static final int BACKDROP_VARIANT_DEFAULT = 3;

    private final android.content.BroadcastReceiver mBackdropVariantReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            // --ei t 0/1/2：D 版四边收边力度档，只对 D 生效并持久化。
            if (intent.hasExtra("t")) com.github.catvod.utils.Prefers.put("debug_edge_tier", Math.max(0, Math.min(2, intent.getIntExtra("t", 0))));
            // 快进曲线定案 B（片长挂钩），A 模式与 --ei s 切换入口移除。
            applyBackdropVariant(intent.getIntExtra("v", com.github.catvod.utils.Prefers.getInt("debug_backdrop_variant", BACKDROP_VARIANT_DEFAULT)));
        }
    };

    private void applyBackdropVariant(int variant) {
        int v = Math.max(0, Math.min(BACKDROP_SCRIM_VARIANTS.length - 1, variant));
        mBinding.backdropScrim.setBackgroundResource(v == 3 ? com.fongmi.android.tv.utils.EdgeTiers.TIERS[com.fongmi.android.tv.utils.EdgeTiers.get()] : BACKDROP_SCRIM_VARIANTS[v]);
        mBinding.detailScrim.setBackgroundResource(DETAIL_SCRIM_VARIANTS[v]);
        mBinding.backdrop.setAlpha(BACKDROP_ALPHA_VARIANTS[v]);
        if (com.fongmi.android.tv.BuildConfig.DEBUG) com.github.catvod.utils.Prefers.put("debug_backdrop_variant", v);
    }

    @Override
    protected void onDestroy() {
        if (com.fongmi.android.tv.BuildConfig.DEBUG) unregisterReceiver(mBackdropVariantReceiver);
        mClock.release();
        saveHistory(true);
        DanmakuApi.cancel();
        RefreshEvent.keep();
        App.removeCallbacks(mR1, mR2, mR3, mR4, mR5);
        App.removeCallbacks(mHideControl);
        super.onDestroy();
    }
}
