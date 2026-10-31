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
    private boolean historyShown;
    private int homeRetry;
    private HistoryPresenter mPresenter;
    private SiteViewModel mViewModel;
    private Result mResult;
    private Clock mClock;
    private String mBackdropUrl;
    private String mFallbackPic;

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
        mBinding.source.setOnClickListener(v -> showDialog());
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
                else mBinding.recycler.requestFocus();
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
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16, FocusHighlight.ZOOM_FACTOR_NONE), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16, FocusHighlight.ZOOM_FACTOR_NONE), FuncPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16, FocusHighlight.ZOOM_FACTOR_NONE, HorizontalGridView.FOCUS_SCROLL_ALIGNED), HistoryPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(24));
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
        mAdapter.add(R.string.home_recommend);
    }

    private void setTitle() {
        List<String> items = Arrays.asList(getHome().getName(), getConfig().getName(), getString(R.string.app_name));
        Optional<String> optional = items.stream().filter(s -> !TextUtils.isEmpty(s)).findFirst();
        optional.ifPresent(s -> mBinding.source.setText(s));
    }

    private void initConfig() {
        VodConfig.get().init().load(getCallback());
        LiveConfig.get().init().load();
        WallConfig.get().init();
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

    private void setFocus() {
        App.post(() -> mBinding.recycler.requestFocus(), 300);
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
            if (TextUtils.isEmpty(url)) url = mFallbackPic;
            if (TextUtils.isEmpty(url) || url.equals(mBackdropUrl)) return;
            mBackdropUrl = url;
            Glide.with(this).load(ImgUtil.getUrl(url)).override(24, 24).centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(600))
                    .into(mBinding.backdrop);
            mBinding.backdrop.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {
        }
    }

    private void getVideo() {
        mResult = Result.empty();
        int index = getRecommendIndex();
        boolean gone = mAdapter.indexOf("progress") == -1;
        boolean hasItem = gone && mAdapter.size() > index;
        if (hasItem) mAdapter.removeItems(index, mAdapter.size() - index);
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
        if (style.isList()) mAdapter.addAll(mAdapter.size(), result.getList());
        else addGrid(result.getList(), style);
    }

    /** 源没有推荐位（网盘/配置类源）时不显示空的「更新推荐」标题。 */
    private void setRecommendHeader(boolean show) {
        int index = mAdapter.indexOf(R.string.home_recommend);
        if (show && index < 0) mAdapter.add(getRecommendInsertIndex(), R.string.home_recommend);
        else if (!show && index >= 0) mAdapter.removeItems(index, 1);
    }

    /** 「更新推荐」要排在「继续观看」之后，不能因为晚到而插到历史行上面。 */
    private int getRecommendInsertIndex() {
        int history = mAdapter.indexOf(R.string.home_history);
        return history < 0 ? 0 : history + 2;
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
        List<ListRow> rows = new ArrayList<>();
        VodPresenter presenter = new VodPresenter(this, style);
        for (List<Vod> part : Lists.partition(items, Product.getColumn(style))) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
            adapter.addAll(0, part);
            rows.add(new ListRow(adapter));
        }
        mAdapter.addAll(mAdapter.size(), rows);
    }

    private void setFunc() {
        mBinding.live.setVisibility(LiveConfig.hasUrl() ? View.VISIBLE : View.GONE);
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
                int index = mAdapter.indexOf(R.string.home_history);
                if (index >= 0) mAdapter.removeItems(index, 2);
                historyShown = false;
            }
        } else if (!historyShown) {
            int base = 0;
            mAdapter.add(base, R.string.home_history);
            mAdapter.add(base + 1, new ListRow(mHistoryAdapter));
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
        int index = mAdapter.indexOf(R.string.home_history);
        if (index >= 0) mAdapter.removeItems(index, 2);
        historyShown = false;
    }

    private int getRecommendIndex() {
        int index = mAdapter.indexOf(R.string.home_recommend);
        if (index >= 0) return index + 1;
        int history = mAdapter.indexOf(R.string.home_history);
        return history < 0 ? 0 : history + 2;
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
        int index = mAdapter.indexOf(R.string.home_history);
        if (index >= 0) mAdapter.removeItems(index, 2);
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
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP && mBinding.recycler.hasFocus()
                    && mBinding.recycler.getSelectedPosition() <= 0
                    && mTypeAdapter != null && mTypeAdapter.size() > 0) {
                mBinding.recycler.setSelectedPosition(0);
                mBinding.typeRecycler.requestFocus();
                return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN && mBinding.typeRecycler.hasFocus()) {
                mBinding.recycler.requestFocus();
                return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP && mBinding.typeRecycler.hasFocus()) {
                mBinding.source.requestFocus();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mClock.start();
        // 继续观看必须每次回到首页都重新读库，否则冷启动后只剩「更新推荐」。
        getHistory();
        App.post(mBackdropTask, 700);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mClock.stop();
        App.removeCallbacks(mBackdropTask);
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
