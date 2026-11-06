package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.splashscreen.SplashScreen;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Cache;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Func;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.extractor.Source;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.DLNARendererService;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.adapter.BaseDiffCallback;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.custom.CustomTitleView;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.ui.presenter.FuncPresenter;
import com.fongmi.android.tv.ui.presenter.HeaderPresenter;
import com.fongmi.android.tv.ui.presenter.HistoryPresenter;
import com.fongmi.android.tv.ui.presenter.ProgressPresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Task;
import com.fongmi.android.tv.ui.custom.ActionChip;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.net.OkHttp;
import com.google.common.collect.Lists;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class HomeActivity extends BaseActivity implements CustomTitleView.Listener, VodPresenter.OnClickListener, FuncPresenter.OnClickListener, HistoryPresenter.OnClickListener {

    private ActivityHomeBinding mBinding;
    private ArrayObjectAdapter mHistoryAdapter;
    private ArrayObjectAdapter mAdapter;
    private final List<com.fongmi.android.tv.bean.Class> mTypes = new ArrayList<>();
    private ArrayObjectAdapter mTypeAdapter;
    /** 「最近观看」行（标题画在行内，见 CustomRowPresenter）。 */
    private ListRow mHistoryRow;
    /** 「更新推荐」组第一行（带 HeaderItem 标题）。 */
    private ListRow mRecommendRow;
    private boolean historyShown;
    private int homeRetry;
    private HistoryPresenter mPresenter;
    private SiteViewModel mViewModel;
    private Result mResult;
    private Clock mClock;
    private String mBackdropUrl;
    private String mFallbackPic;
    /** 当前内容区行高（px），用来判断海报尺寸是否真的变了。 */
    private int mRowHeight = -1;
    /** 改了海报大小：数据回来后要把列表整个复位，否则会停在旧行高算出来的偏移上。 */
    private boolean mResizePending;

    /** 氛围底图：跟随当前焦点卡片的海报做低分辨率放大，形成柔和的背景光，避免纯黑死板。 */
    private final Runnable mBackdropTask = new Runnable() {
        @Override
        public void run() {
            updateBackdrop();
            App.post(this, 600);
        }
    };

    private Site getHome() {
        return VodConfig.get().getHome();
    }

    private Config getConfig() {
        return VodConfig.get().getConfig();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mResult = Result.empty();
        mClock = Clock.create(mBinding.clock).format("HH:mm  MM/dd EEE");
        mBinding.progressLayout.showProgress();
        setNavScrim();
        if (com.fongmi.android.tv.BuildConfig.DEBUG) {
            com.fongmi.android.tv.utils.EdgeTiers.apply(mBinding.backdropScrim, com.fongmi.android.tv.utils.EdgeTiers.get());
            androidx.core.content.ContextCompat.registerReceiver(this, mEdgeTierReceiver, new android.content.IntentFilter("com.qbox.app.DEBUG_BACKDROP"), androidx.core.content.ContextCompat.RECEIVER_EXPORTED);
        }
        PermissionUtil.requestNotify(this);
        DLNARendererService.start(this);
        Updater.create().start(this);
        setRecyclerView();
        setViewModel();
        setAdapter();
        initConfig();
        setTitle();
        setLogo();
        setFunc();
    }

    @Override
    protected void initEvent() {
        mBinding.source.setOnClickListener(v -> {
            // 官方包首次启动没有源：直接进设置页导入配置，不要弹空的站点列表。
            if (TextUtils.isEmpty(VodConfig.getUrl())) SettingActivity.start(this);
            else showDialog();
        });
        mBinding.source.setOnLongClickListener(v -> {
            showDiag();
            return true;
        });
        bindAction(mBinding.search, () -> SearchActivity.start(this));
        bindAction(mBinding.live, () -> LiveActivity.start(this));
        bindAction(mBinding.keep, () -> KeepActivity.start(this));
        bindAction(mBinding.push, () -> PushActivity.start(this));
        bindAction(mBinding.setting, () -> SettingActivity.start(this));
        mBinding.setting.setOnLongClickListener(v -> {
            showDiag();
            return true;
        });
        mBinding.recycler.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP && isRecyclerAtTop()) {
                if (mTypeAdapter != null && mTypeAdapter.size() > 0) mBinding.typeRecycler.requestFocus();
                else mBinding.search.requestFocus();
                return true;
            }
            return false;
        });
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mPresenter.isDelete()) setHistoryDelete(false);
            }
        });
    }

    /** 内容区是否还在最顶端：在顶端按上才回分类条，滚动中按上仍然滚内容。 */
    private boolean isRecyclerAtTop() {
        if (mBinding.recycler.getChildCount() == 0) return true;
        int tolerance = Math.max(2, Math.round(8 * getResources().getDisplayMetrics().density));
        return mBinding.recycler.getChildAt(0).getTop() >= mBinding.recycler.getPaddingTop() - tolerance;
    }

    /** 顶栏动作按钮：聚焦时在药丸内部横向展开文字，点击进页面，按下方向键回到内容区。 */
    private void bindAction(ActionChip view, Runnable action) {
        view.setOnClickListener(v -> action.run());
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (mTypeAdapter != null && mTypeAdapter.size() > 0) mBinding.typeRecycler.requestFocus();
                else focusContent();
                return true;
            }
            return false;
        });
    }

    /** QBox 自检：首页标题长按显示版本 / 配置 / 站点 / 直播 / 爬虫缓存，方便电视端排查。 */
    private void showDiag() {
        Config config = VodConfig.get().getConfig();
        java.util.List<Site> sites = VodConfig.get().getSites();
        String jar = sites.isEmpty() || sites.get(0).getJar() == null ? "-" : sites.get(0).getJar();
        if (jar.length() > 46) jar = "…" + jar.substring(jar.length() - 46);
        File dir = com.github.catvod.utils.Path.jar();
        int cached = dir != null && dir.listFiles() != null ? dir.listFiles().length : 0;
        String text = "版本：" + com.fongmi.android.tv.BuildConfig.VERSION_NAME + "\n"
                + "配置：" + (config == null ? "-" : config.getName()) + "\n"
                + "地址：" + (config == null ? "-" : config.getUrl()) + "\n"
                + "站点：" + sites.size() + "\n"
                + "直播：" + LiveConfig.get().getLives().size() + "\n"
                + "jar 缓存：" + cached + " 个文件\n"
                + "spider：" + jar;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("QBox 自检")
                .setMessage(text)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            VideoActivity.push(this, intent.getStringExtra(Intent.EXTRA_TEXT));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            PermissionUtil.requestFile(this, allGranted -> checkType(intent));
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String keyword = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(keyword)) SearchActivity.start(this, keyword);
        }
    }

    private void checkType(Intent intent) {
        if ("text/plain".equals(intent.getType()) || UrlUtil.path(intent.getData()).endsWith(".m3u")) {
            loadLive("file:/" + FileChooser.getPathFromUri(intent.getData()));
        } else {
            VideoActivity.push(this, intent.getData().toString());
        }
    }

    @SuppressLint("RestrictedApi")
    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(Integer.class, new HeaderPresenter());
        selector.addPresenter(String.class, new ProgressPresenter());
        selector.addPresenter(Vod.class, new VodPresenter(this, Style.list()));
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16, FocusHighlight.ZOOM_FACTOR_SMALL), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16, FocusHighlight.ZOOM_FACTOR_SMALL), FuncPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16, FocusHighlight.ZOOM_FACTOR_SMALL), HistoryPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        applyRowMetrics(Style.rect());
        mBinding.typeRecycler.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.typeRecycler.setRowHeight(android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.typeRecycler.setAdapter(new ItemBridgeAdapter(mTypeAdapter = new ArrayObjectAdapter(new FuncPresenter(this))));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getResult().observe(this, result -> {
            mAdapter.remove("progress");
            if (result == null || (result.getTypes().isEmpty() && result.getList().isEmpty())) {
                retryHome();
                return;
            }
            homeRetry = 0;
            addVideo(mResult = result);
            Cache.clear().put(result);
        });
    }

    /** 首页内容请求失败时自动重试，避免只剩一个「点播」入口的空壳状态。 */
    private void retryHome() {
        if (homeRetry >= 3) {
            Toast.makeText(this, "首页内容加载失败，可在设置里换源或稍后重进", Toast.LENGTH_LONG).show();
            return;
        }
        homeRetry++;
        App.post(() -> {
            mAdapter.add("progress");
            mViewModel.homeContent();
        }, 1500L * homeRetry);
    }

    private void setAdapter() {
        mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
    }

    private void setTitle() {
        // 官方包首次启动没有源时，左上角直接告诉用户下一步做什么。
        if (TextUtils.isEmpty(VodConfig.getUrl())) {
            mBinding.source.setText(R.string.add_source);
            return;
        }
        List<String> items = Arrays.asList(getHome().getName(), getConfig().getName(), getString(R.string.app_name));
        Optional<String> optional = items.stream().filter(s -> !TextUtils.isEmpty(s)).findFirst();
        optional.ifPresent(s -> mBinding.source.setText(s));
    }

    private void initConfig() {
        // 先让 debug 包应用内置测试源；官方 release 的 init() 不会写入任何源。
        VodConfig vod = VodConfig.get().init();
        if (TextUtils.isEmpty(vod.getConfig().getUrl())) {
            // 官方包默认不带源：保持空壳，不弹加载错误；点左上角「添加配置源」进设置页导入。
            setRecommendHeader(false);
            showContent();
            App.post(() -> mBinding.source.requestFocus(), 400);
            return;
        }
        vod.load(getCallback());
        // 直播/壁纸配置原来是主线程同步拉网络，会把首帧拖到十几秒（用户只看到 logo）；
        // 放后台后首页先出加载骨架（转圈 + 正在加载…），直播页自己会 ensureLoaded。
        Task.submit(() -> {
            LiveConfig.get().init().load();
            WallConfig.get().init();
        });
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                showContent();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                showContent();
            }
        };
    }

    private void showContent() {
        mBinding.progressLayout.showContent();
        checkAction(getIntent());
        setFocus();
    }

    private void loadLive(String url) {
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                LiveActivity.start(getActivity());
            }
        });
    }

    /**
     * 进内容区统一入口。
     * 选中位置可能停在「最近观看 / 更新推荐」这类不可聚焦的分组标题上，
     * 这时 requestFocus 只把焦点交给列表容器，屏幕上看不到任何焦点框（"不知道选中了哪里"）。
     * 所以进来前先校验：选中行不是内容行，就落到第一行。
     */
    private void focusContent() {
        int size = mAdapter == null ? 0 : mAdapter.size();
        int selected = mBinding.recycler.getSelectedPosition();
        boolean valid = selected >= 0 && selected < size && mAdapter.get(selected) instanceof ListRow;
        if (!valid) {
            for (int i = 0; i < size; i++) {
                if (mAdapter.get(i) instanceof ListRow) {
                    mBinding.recycler.setSelectedPosition(i);
                    break;
                }
            }
        }
        mBinding.recycler.requestFocus();
    }

    private void setFocus() {
        App.post(this::focusContent, 300);
    }

    private void updateBackdrop() {
        try {
            View focus = getCurrentFocus();
            if (focus == null) return;
            String url = null;
            RecyclerView grid = null;
            View child = focus;
            while (child != null) {
                if (child.getParent() instanceof RecyclerView) {
                    grid = (RecyclerView) child.getParent();
                    break;
                }
                child = child.getParent() instanceof View ? (View) child.getParent() : null;
            }
            if (grid != null && grid.getAdapter() instanceof ItemBridgeAdapter) {
                int position = grid.getChildAdapterPosition(child);
                RecyclerView.ViewHolder holder = position < 0 ? null : grid.findViewHolderForAdapterPosition(position);
                if (holder instanceof ItemBridgeAdapter.ViewHolder) {
                    Object item = ((ItemBridgeAdapter.ViewHolder) holder).getItem();
                    if (item instanceof Vod) url = ((Vod) item).getPic();
                    else if (item instanceof History) url = ((History) item).getVodPic();
                }
            }
            if (TextUtils.isEmpty(url)) {
                // 焦点在顶栏/分类条时保持上一张氛围图，避免每次上移都闪回推荐位海报。
                if (mBackdropUrl != null) return;
                url = mFallbackPic;
            }
            if (TextUtils.isEmpty(url) || url.equals(mBackdropUrl)) return;
            mBackdropUrl = url;
            // 氛围底图（第 28 轮统一详情页终态）：480×270 小解码 + BackdropBlur（降采 72×40→盒式×3→抖动），
            // 焦点连续移动时每张新图 Glide 后台一次性 ~110-150ms 且磁盘缓存，渲染线程零开销。
            Glide.with(this).asBitmap().load(ImgUtil.getUrl(url)).override(480, 270)
                    .transform(new com.bumptech.glide.load.MultiTransformation<>(new com.bumptech.glide.load.resource.bitmap.CenterCrop(), new com.fongmi.android.tv.utils.BackdropBlur()))
                    .transition(com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade(600))
                    .into(mBinding.backdrop);
            mBinding.backdrop.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {
        }
    }

    /** DEBUG 收边力度档切换（第 30 轮）：--ei t 0/1/2。 */
    private final android.content.BroadcastReceiver mEdgeTierReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            if (intent.hasExtra("t")) com.fongmi.android.tv.utils.EdgeTiers.apply(mBinding.backdropScrim, intent.getIntExtra("t", 0));
        }
    };

    /** 按需导航带（第 29 轮，Netflix 式）：焦点进顶栏/分类条（顶部 124dp 内）淡入短软渐变带，离开淡出恢复满屏。 */
    private boolean navScrimOn;

    private void setNavScrim() {
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener((o, f) -> updateNavScrim(f));
    }

    private void updateNavScrim(View focus) {
        boolean inNav = false;
        if (focus != null) {
            android.graphics.Rect r = new android.graphics.Rect();
            inNav = focus.getGlobalVisibleRect(r) && r.top < ResUtil.dp2px(124);
        }
        if (inNav == navScrimOn) return;
        navScrimOn = inNav;
        mBinding.navScrim.animate().cancel();
        mBinding.navScrim.animate().alpha(inNav ? 1f : 0f).setDuration(220).start();
    }

    private void getVideo() {
        mResult = Result.empty();
        if (mResizePending) {
            // 先回到顶部再换数据：不然新行高会套用旧偏移，出现半行和错位。
            mBinding.recycler.setSelectedPosition(0);
            mBinding.recycler.scrollToPosition(0);
        }
        int index = getRecommendIndex();
        boolean gone = mAdapter.indexOf("progress") == -1;
        boolean hasItem = gone && mAdapter.size() > index;
        if (hasItem) mAdapter.removeItems(index, mAdapter.size() - index);
        mRecommendRow = null;
        if (gone) mAdapter.add("progress");
        mViewModel.homeContent();
    }

    private void addVideo(Result result) {
        addTypes(result);
        boolean empty = result.getList() == null || result.getList().isEmpty();
        if (!empty) for (Vod vod : result.getList()) if (!TextUtils.isEmpty(vod.getPic())) { mFallbackPic = vod.getPic(); break; }
        setRecommendHeader(!empty);
        if (empty) return;
        Style style = result.getStyle(getHome().getStyle());
        if (style.isList()) {
            // list 风格的推荐是每行一张大卡的 Vod 行，没有 ListRow 载体；
            // 标题用一行「只带 HeaderItem 的空行」承载，视觉上就是原来的分组标题。
            if (mRecommendRow == null) {
                mRecommendRow = new ListRow(new HeaderItem(getString(R.string.home_recommend)), new ArrayObjectAdapter());
                mAdapter.add(mAdapter.size(), mRecommendRow);
            }
            mAdapter.addAll(mAdapter.size(), result.getList());
        } else addGrid(result.getList(), style);
        if (mResizePending) resetAfterResize();
        // 数据是异步回来的：applyContentTop 里那次校正跑得比数据早，首屏第一排会贴着
        // 内容区起点，放大后顶部被裁掉。数据落位后再校正一次。
        App.post(() -> mBinding.recycler.requestSnap(), 160);
    }

    /** 换完海报尺寸：行高、行距、裁剪线都变了，列表必须从头摆一遍，否则焦点和滚动都停在旧几何上。 */
    private void resetAfterResize() {
        mResizePending = false;
        App.post(() -> {
            if (mBinding == null) return;
            int target = 0;
            for (int i = 0; i < mAdapter.size(); i++) {
                if (mAdapter.get(i) instanceof ListRow) {
                    target = i;
                    break;
                }
            }
            mBinding.recycler.setSelectedPosition(target);
            mBinding.recycler.scrollToPosition(0);
            // 只有焦点本来就在内容区（或已经丢了）才抢焦点，用户停在顶栏时不要把他拽下来。
            View focus = getCurrentFocus();
            if (focus == null || isInside(mBinding.recycler, focus)) mBinding.recycler.requestFocus();
        }, 150);
    }

    private boolean isInside(View parent, View child) {
        View view = child;
        while (view != null) {
            if (view == parent) return true;
            view = view.getParent() instanceof View ? (View) view.getParent() : null;
        }
        return false;
    }

    /**
     * 分组标题（HeaderItem）现在直接画在行内部（见 CustomRowPresenter），
     * 所以「更新推荐」是否显示 = 推荐组第一行是否带 header。
     * empty 时推荐组根本没有行，引用作废即可。
     */
    private void setRecommendHeader(boolean show) {
        if (!show) mRecommendRow = null;
    }

    /** 分类行：「首页」+ 当前源自己的点播分类。每次内容刷新（含换源）都按源重建，不套模板。 */
    private void addTypes(Result result) {

        mTypes.clear();
        List<Func> items = new ArrayList<>();
        if (result.getTypes() != null) for (com.fongmi.android.tv.bean.Class type : result.getTypes()) {
            if (TextUtils.isEmpty(type.getTypeName())) continue;
            mTypes.add(type);
            items.add(Func.create(type.getTypeName()));
        }
        if (items.isEmpty()) items.add(Func.create(R.string.home_vod));
        mTypeAdapter.clear();
        mTypeAdapter.addAll(0, items);
        mBinding.typeRecycler.setSelectedPosition(0);
        mBinding.typeRecycler.scrollToPosition(0);
    }

    private void addGrid(List<Vod> items, Style style) {
        applyRowMetrics(style);
        List<ListRow> rows = new ArrayList<>();
        VodPresenter presenter = new VodPresenter(this, style);
        for (List<Vod> part : Lists.partition(items, Product.getColumn(style))) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
            adapter.addAll(0, part);
            rows.add(new ListRow(adapter));
        }
        // 推荐组第一行带上「更新推荐」标题（标题画在行内部，见 CustomRowPresenter）。
        if (!rows.isEmpty() && mRecommendRow == null) {
            rows.set(0, new ListRow(new HeaderItem(getString(R.string.home_recommend)), rows.get(0).getAdapter()));
        }
        if (!rows.isEmpty()) mRecommendRow = rows.get(0);
        mAdapter.addAll(mAdapter.size(), rows);
    }

    /**
     * 内容区顶部几何跟着海报尺寸走。
     * 之前裁切线 / 行距 / padding 都是按 5 列（Large）写死的常量，
     * 换成 6、7 列或 4 列以后行高变了，上一行就又能从分类条下面露出半截。
     */
    private void applyRowMetrics(Style style) {
        int height;
        try {
            height = Product.getSpec(style)[1] + ResUtil.dp2px(52);
        } catch (Throwable e) {
            height = ResUtil.dp2px(280);
        }
        if (height == mRowHeight) return;
        mRowHeight = height;
        mBinding.recycler.applyContentTop(getResources().getDimensionPixelSize(R.dimen.qbox_content_top), getResources().getDimensionPixelSize(R.dimen.qbox_header_total), height, true);
    }

    private void setFunc() {
        // 没有直播源时用 INVISIBLE 而不是 GONE：保留胶囊占位，搜索/收藏/推送/设置的图标位置
        // 不会因为换了个没有直播的配置源而整体左移，遥控器肌肉记忆不变。
        mBinding.live.setVisibility(LiveConfig.hasUrl() ? View.VISIBLE : View.INVISIBLE);
    }

    private void getHistory() {
        getHistory(false);
    }

    private void getHistory(boolean renew) {
        if (mAdapter == null) return;
        List<History> items = com.fongmi.android.tv.db.AppDatabase.get().getHistoryDao().findRecent();
        if (renew) mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
        mHistoryAdapter.setItems(items, new BaseDiffCallback<History>());
        if (items.isEmpty()) {
            if (historyShown) {
                int index = mHistoryRow == null ? -1 : mAdapter.indexOf(mHistoryRow);
                if (index >= 0) mAdapter.removeItems(index, 1);
                mHistoryRow = null;
                historyShown = false;
            }
        } else if (!historyShown) {
            int base = 0;
            mHistoryRow = new ListRow(new HeaderItem(getString(R.string.home_history)), mHistoryAdapter);
            mAdapter.add(base, mHistoryRow);
            historyShown = true;
        }
    }

    private void setHistoryDelete(boolean delete) {
        mPresenter.setDelete(delete);
        mHistoryAdapter.notifyArrayItemRangeChanged(0, mHistoryAdapter.size());
    }

    private void clearHistory() {
        History.clear(VodConfig.getCid());
        mPresenter.setDelete(false);
        mHistoryAdapter.clear();
        int index = mHistoryRow == null ? -1 : mAdapter.indexOf(mHistoryRow);
        if (index >= 0) mAdapter.removeItems(index, 1);
        mHistoryRow = null;
        historyShown = false;
    }

    private int getRecommendIndex() {
        int index = mRecommendRow == null ? -1 : mAdapter.indexOf(mRecommendRow);
        if (index >= 0) return index;
        return mHistoryRow != null ? mAdapter.indexOf(mHistoryRow) + 1 : mAdapter.size();
    }

    private void setLogo() {
        ImgUtil.logo(mBinding.logo);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        switch (event.type()) {
            case VOD:
                RefreshEvent.history();
                RefreshEvent.home();
                setLogo();
                setTitle();
                break;
            case COMMON:
                setFunc();
                break;
            case BOOT:
                LiveActivity.start(this);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case HOME:
                getVideo();
                setTitle();
                break;
            case HISTORY:
                getHistory();
                break;
            case SIZE:
                mResizePending = true;
                getVideo();
                getHistory(true);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.type()) {
            case SEARCH:
                SearchActivity.start(this, event.text());
                break;
            case PUSH:
                VideoActivity.push(this, event.text());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        if (VodConfig.get().getConfig().equals(event.config())) {
            VideoActivity.cast(this, event.history());
        } else {
            VodConfig.load(event.config(), getCallback(event));
        }
    }

    private Callback getCallback(CastEvent event) {
        return new Callback() {
            @Override
            public void success() {
                onCastEvent(event);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    @Override
    public void onItemClick(Func item) {
        if (item.getResId() == 0) {
            VodActivity.start(this, getHome().getKey(), mResult, indexOfType(item.getText()));
        }
        else if (item.getResId() == R.string.home_vod) VodActivity.start(this, mResult);
        else if (item.getResId() == R.string.home_live) LiveActivity.start(this);
        else if (item.getResId() == R.string.home_keep) KeepActivity.start(this);
        else if (item.getResId() == R.string.home_push) PushActivity.start(this);
        else if (item.getResId() == R.string.home_search) SearchActivity.start(this);
        else if (item.getResId() == R.string.home_setting) SettingActivity.start(this);
    }

    @Override
    public boolean onItemLongClick(Func item) {
        if (item.getResId() == R.string.home_setting) {
            showDiag();
            return true;
        }
        return false;
    }

    private int indexOfType(String name) {
        for (int i = 0; i < mTypes.size(); i++) if (name.equals(mTypes.get(i).getTypeName())) return i;
        return -1;
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isAction()) mViewModel.action(getHome().getKey(), item.getAction());
        else if (getHome().isIndex()) CollectActivity.start(this, item.getName());
        else VideoActivity.start(this, getHome().getKey(), item.getId(), item.getName(), item.getPic());
    }

    @Override
    public boolean onLongClick(Vod item) {
        if (item.isAction()) return false;
        CollectActivity.start(this, item.getName());
        return true;
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(History item) {
        mHistoryAdapter.remove(item.delete());
        if (mHistoryAdapter.size() > 0) return;
        int index = mHistoryRow == null ? -1 : mAdapter.indexOf(mHistoryRow);
        if (index >= 0) mAdapter.removeItems(index, 1);
        mHistoryRow = null;
        historyShown = false;
        mPresenter.setDelete(false);
    }

    @Override
    public boolean onLongClick() {
        if (mPresenter.isDelete()) clearHistory();
        else setHistoryDelete(true);
        return true;
    }

    @Override
    public void showDialog() {
        SiteDialog.create().show(this);
    }

    @Override
    public void onRefresh() {
        getVideo();
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) showDialog();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            // 顶栏、分类条、内容区三者之间的上下键在 Activity 层统一处理，避免 Leanback 把事件吃掉。
            // 焦点在卡片上时 recycler.hasFocus() 是 false（焦点在子 View），必须按归属判断；
            // 冷启动/数据刷新时 getCurrentFocus() 还会短暂为 null，这时也当成「在内容区」，
            // 否则第一次按 ↑ 会被列表吃掉、什么也不发生（用户得按第二次）。
            boolean inContent = isInside(mBinding.recycler, getCurrentFocus()) || focusLostInContent();
            boolean inTabs = isInside(mBinding.typeRecycler, getCurrentFocus()) || focusLostInTabs();
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP && inContent
                    && mTypeAdapter != null && mTypeAdapter.size() > 0) {
                int pos = mBinding.recycler.getSelectedPosition();
                // 有「最近观看」时它是第 0 行，「更新推荐」首行是第 1 行：
                // 从推荐首行按 ↑ 必须先回到最近观看，而不是直接跳到分类条（用户实测的跳级）。
                if (pos == 1 && historyShown) {
                    // 第 30 轮：回到最近观看要按列落位（x 最近），不再落行首。
                    View target = mBinding.recycler.nearestColumnInRow(0, getCurrentFocus());
                    mBinding.recycler.setSelectedPosition(0);
                    if (target != null) target.requestFocus();
                    else mBinding.recycler.requestFocus();
                    return true;
                }
                // 真正在内容区最上面一行时，才把焦点交给分类条。
                if (pos <= 0) {
                    mBinding.recycler.setSelectedPosition(0);
                    mBinding.typeRecycler.requestFocus();
                    return true;
                }
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN && inTabs) {
                focusContent();
                return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP && inTabs) {
                mBinding.source.requestFocus();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /** 焦点暂时为空（布局中）时，按列表可见性判断它属于哪个区域。只在窗口有焦点时生效，弹窗不会误判。 */
    private boolean focusLostInContent() {
        return getCurrentFocus() == null && hasWindowFocus()
                && mBinding.recycler.getVisibility() == View.VISIBLE
                && mBinding.typeRecycler.getVisibility() == View.VISIBLE;
    }

    private boolean focusLostInTabs() {
        return getCurrentFocus() == null && hasWindowFocus()
                && mBinding.typeRecycler.getVisibility() == View.VISIBLE;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClock.start();
        // 继续观看必须每次回到首页都重新读库，否则冷启动后只剩「更新推荐」。
        getHistory();
        App.post(mBackdropTask, 700);
        // 从设置页/详情页返回时，系统可能把焦点回收掉（uiautomator 里一个 focused 节点都没有），
        // 表现就是「遥控器按什么都没反应」。这里兜底把焦点放回顶栏源名。
        App.post(this::restoreFocusIfLost, 150);
        // 焦点守护：数据落位（addVideo/addHistory/reset）会把焦点清掉，
        // 单次兜底抓不住这种时机。每 500ms 自检一次，只在「窗口有焦点但没有任何
        // 焦点 view」时把焦点送回内容区，不干扰正常操作。onPause 停止。
        App.post(mFocusGuard, 500);
    }

    /** 焦点守护：见 onResume 注释。连续两次都丢了焦点才接管，避免和面板动画抢焦点。 */
    private int mFocusLostCount;

    private final Runnable mFocusGuard = new Runnable() {
        @Override
        public void run() {
            if (mBinding == null || isFinishing() || isDestroyed()) return;
            View focus = getCurrentFocus();
            boolean healthy = focus != null && focus.isAttachedToWindow() && focus.isShown();
            if (healthy || !hasWindowFocus()) mFocusLostCount = 0;
            else mFocusLostCount++;
            if (mFocusLostCount >= 2) {
                mFocusLostCount = 0;
                focusContent();
            }
            App.post(this, 500);
        }
    };

    private void restoreFocusIfLost() {
        if (mBinding == null || !hasWindowFocus()) return;
        View focus = getCurrentFocus();
        boolean healthy = focus != null && focus.isAttachedToWindow() && focus.isShown();
        if (healthy) return;
        if (mBinding.source.getVisibility() == View.VISIBLE) mBinding.source.requestFocus();
        else focusContent();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClock.stop();
        App.removeCallbacks(mBackdropTask);
        App.removeCallbacks(mFocusGuard);
    }

    @Override
    protected void onBackInvoked() {
        if (mBinding.progressLayout.isProgress()) {
            showContent();
        } else if (mPresenter.isDelete()) {
            setHistoryDelete(false);
        } else if (mBinding.recycler.getSelectedPosition() != 0) {
            mBinding.recycler.scrollToPosition(0);
        } else {
            if (PlaybackService.isRunning()) Util.moveToBackground(this);
            else super.onBackInvoked();
        }
    }

    @Override
    protected void onDestroy() {
        if (com.fongmi.android.tv.BuildConfig.DEBUG) unregisterReceiver(mEdgeTierReceiver);
        DLNARendererService.stop(this);
        LiveConfig.get().clear();
        VodConfig.get().clear();
        AppDatabase.backup();
        OkHttp.get().clear();
        Source.get().exit();
        Server.get().stop();
        super.onDestroy();
    }
}
