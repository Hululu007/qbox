package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Cache;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityVodBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.fragment.FolderFragment;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Optional;

public class VodActivity extends BaseActivity implements TypeAdapter.OnClickListener {

    private ActivityVodBinding mBinding;
    private TypeAdapter mAdapter;
    private String mBackdropUrl;
    private String mFallbackPic;

    /** 当前聚焦海报的 24px 缩略图放大成氛围底图，切页/滚动都不再露黑底。 */
    private final Runnable mBackdropTask = new Runnable() {
        @Override
        public void run() {
            updateBackdrop();
            App.post(this, 600);
        }
    };

    public static void start(Activity activity, Result result) {
        start(activity, VodConfig.get().getHome().getKey(), result, -1);
    }

    public static void start(Activity activity, String key, Result result) {
        start(activity, key, result, -1);
    }

    public static void start(Activity activity, String key, Result result, int index) {
        if (result == null || result.getTypes().isEmpty()) return;
        Intent intent = new Intent(activity, VodActivity.class);
        intent.putExtra("key", key);
        intent.putExtra("result", result);
        intent.putExtra("index", index);
        activity.startActivity(intent);
    }

    private String getKey() {
        return getIntent().getStringExtra("key");
    }

    private Result getResult() {
        return getIntent().getParcelableExtra("result");
    }

    private Class getType() {
        return mAdapter.get(mBinding.pager.getCurrentItem());
    }

    private FolderFragment getFragment() {
        return (FolderFragment) mBinding.pager.getAdapter().instantiateItem(mBinding.pager, mBinding.pager.getCurrentItem());
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityVodBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mBinding.back.setOnClickListener(v -> finish());
        mBinding.name.setText(VodConfig.get().getHome().getName());
        com.fongmi.android.tv.utils.Clock.create(mBinding.clock).start();
        setNavScrim();
        if (com.fongmi.android.tv.BuildConfig.DEBUG) {
            com.fongmi.android.tv.utils.EdgeTiers.apply(mBinding.backdropScrim, com.fongmi.android.tv.utils.EdgeTiers.get());
            androidx.core.content.ContextCompat.registerReceiver(this, mEdgeTierReceiver, new android.content.IntentFilter("com.qbox.app.DEBUG_BACKDROP"), androidx.core.content.ContextCompat.RECEIVER_EXPORTED);
        }
        setRecyclerView();
        setTypes();
        setPager();
        bindFilter();
        int index = getIntent().getIntExtra("index", -1);
        if (index < 0 || index >= mAdapter.getItemCount()) index = 0;
        mBinding.typeRecycler.setSelectedPosition(index);
        mBinding.pager.setCurrentItem(index);
        mAdapter.setPosition(index);
        updateFilterState();
        mFallbackPic = firstPic(getResult());
        updateBackdrop();
    }

    /** DEBUG 收边力度档切换（第 30 轮）：--ei t 0/1/2。第 35 轮加 --ei m 0/1 切导航带变体。 */
    private final android.content.BroadcastReceiver mEdgeTierReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            if (intent.hasExtra("t")) com.fongmi.android.tv.utils.EdgeTiers.apply(mBinding.backdropScrim, intent.getIntExtra("t", 0));
            if (intent.hasExtra("m")) {
                com.github.catvod.utils.Prefers.put("debug_nav_variant", intent.getIntExtra("m", 0));
                applyNavVariant();
            }
        }
    };

    /** 按需导航带（第 29 轮起，Netflix 式）：焦点真实位于导航区（顶栏/分类条容器内）时淡入短软渐变带，离开淡出恢复满屏。
        第 31 轮判据改结构式：不再看焦点 bounds——滚动/落焦动画中 getGlobalVisibleRect 会瞬时说谎
        （焦点还在内容区就误报 top<124dp，进「最近观看」偶发触发带子），改为焦点是否挂在 header 容器内。 */
    private boolean navScrimOn;

    private void setNavScrim() {
        applyNavVariant();
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener((o, f) -> updateNavScrim(f));
    }

    /** 导航带变体（第 35 轮起）：0=二次曲线渐变带（190dp），1=Apple TV+ 式「栏」。
        第 37 轮：栏版改「实色主体（@color/qbox_panel，与控制条面板同材质）+ 4dp 极细收边」；
        且在 filter 面板展开时主体向下延展到最后一行 chip（同一块连续表面），收起归位（240ms 过渡）。 */
    private void applyNavVariant() {
        int v = com.github.catvod.utils.Prefers.getInt("debug_nav_variant", 0);
        boolean bar = v == 1;
        filterBandOpen = filterBandOpen && bar;
        mBinding.navScrim.setBackgroundResource(bar
                ? com.fongmi.android.tv.R.color.qbox_nav_bar
                : com.fongmi.android.tv.R.drawable.shape_nav_scrim);
        android.view.ViewGroup.LayoutParams lp = mBinding.navScrim.getLayoutParams();
        lp.height = com.fongmi.android.tv.utils.ResUtil.dp2px(bar ? NAV_BAR_SOLID_DP : 190);
        mBinding.navScrim.setLayoutParams(lp);
        mBinding.navScrimEdge.setVisibility(bar ? android.view.View.VISIBLE : android.view.View.GONE);
        if (filterBandOpen) mBinding.getRoot().postDelayed(() -> animateBandToFilter(0), 120);
    }

    private static final int NAV_BAR_SOLID_DP = 136;   // 栏版主体高（tab 底 118dp + 18dp 余量）
    /** filter 面板展开触发的主体延展标记（仅栏版）；为真时导航带常显（不等焦点进导航区）。 */
    private boolean filterBandOpen;
    private android.animation.ValueAnimator navBandAnim;

    /** filter 面板开关（第 37 轮）：栏版下导航带主体延展/归位，并把它计入显隐判据。 */
    private void setFilterBand(boolean open) {
        boolean bar = com.github.catvod.utils.Prefers.getInt("debug_nav_variant", 0) == 1;
        filterBandOpen = open && bar;
        if (filterBandOpen) {
            mBinding.getRoot().postDelayed(() -> animateBandToFilter(0), 120);
        } else {
            animateBandHeight(com.fongmi.android.tv.utils.ResUtil.dp2px(NAV_BAR_SOLID_DP));
        }
        updateNavScrim(getCurrentFocus());
    }

    /** 等 fragment 布局完成后量最后一行 chip 的屏幕底缘；没布局好重试（最多 3 次），量不到就不动。 */
    private void animateBandToFilter(int retry) {
        int bottom = getFragment() == null ? 0 : getFragment().getFilterBottomPx();
        if (bottom <= 0 && retry < 3) {
            mBinding.getRoot().postDelayed(() -> animateBandToFilter(retry + 1), 120);
            return;
        }
        if (bottom <= 0) return;
        animateBandHeight(bottom + com.fongmi.android.tv.utils.ResUtil.dp2px(8));
    }

    /** 主体高度 240ms 过渡（延展/归位同一条路径）；收边视图在容器内随主体下移。 */
    private void animateBandHeight(int targetPx) {
        android.view.View solid = mBinding.navScrim;
        if (solid.getLayoutParams().height == targetPx) return;
        if (navBandAnim != null) navBandAnim.cancel();
        navBandAnim = android.animation.ValueAnimator.ofInt(solid.getLayoutParams().height, targetPx);
        navBandAnim.setDuration(240);
        navBandAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        navBandAnim.addUpdateListener(a -> {
            android.view.ViewGroup.LayoutParams lp = solid.getLayoutParams();
            lp.height = (Integer) a.getAnimatedValue();
            solid.setLayoutParams(lp);
        });
        navBandAnim.start();
    }

    private void updateNavScrim(View focus) {
        // 第 37 轮 P0-1：filter 展开时栏只在「焦点位于筛选 chip 行内」时显示；
        // 焦点移出到海报区，栏与 filter 一起消失（不再留在屏幕上）。
        boolean inFilter = filterBandOpen && focus != null && getFragment() != null && getFragment().isFilterRowFocus(focus);
        boolean inNav = (focus != null && isInHeader(focus)) || inFilter;
        if (com.fongmi.android.tv.BuildConfig.DEBUG) {
            android.graphics.Rect r = new android.graphics.Rect();
            boolean vis = focus != null && focus.getGlobalVisibleRect(r);
            android.util.Log.d("QBoxNav", "focus=" + (focus == null ? "null" : focus.getClass().getSimpleName())
                    + " vis=" + vis + " bounds=" + r.toShortString() + " inHeader=" + inNav + " inFilter=" + inFilter + " bandOn=" + navScrimOn);
        }
        if (inNav == navScrimOn) return;
        navScrimOn = inNav;
        mBinding.navBand.animate().cancel();
        mBinding.navBand.animate().alpha(inNav ? 1f : 0f).setDuration(220).start();
    }

    /** filter 内容滚动时（第 37 轮 P0-1，由 TypeFragment onScrolled 回调）：实色主体高度即时贴住
        「当前可见最后一行」底沿——不跑动画、不盖屏幕固定区域，随内容一起走。 */
    public void syncFilterBand() {
        if (!filterBandOpen) return;
        int bottom = getFragment() == null ? 0 : getFragment().getFilterBottomPx();
        if (bottom <= 0) return;
        if (navBandAnim != null) navBandAnim.cancel();
        android.view.ViewGroup.LayoutParams lp = mBinding.navScrim.getLayoutParams();
        int target = bottom + com.fongmi.android.tv.utils.ResUtil.dp2px(8);
        if (lp.height != target) {
            lp.height = target;
            mBinding.navScrim.setLayoutParams(lp);
        }
    }

    /** 焦点是否位于导航区：沿父链找 header 容器（顶栏 + 分类条）。结构判据不受滚动/动画中的瞬时坐标影响。 */
    private boolean isInHeader(View focus) {
        for (View v = focus; v != null; ) {
            if (v == mBinding.header) return true;
            if (!(v.getParent() instanceof View)) return false;
            v = (View) v.getParent();
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.post(mBackdropTask, 400);
    }

    @Override
    protected void onPause() {
        super.onPause();
        App.removeCallbacks(mBackdropTask);
    }

    @Override
    protected void onDestroy() {
        if (com.fongmi.android.tv.BuildConfig.DEBUG) unregisterReceiver(mEdgeTierReceiver);
        super.onDestroy();
    }

    private static String firstPic(Result result) {
        if (result == null || result.getList() == null) return null;
        for (Vod vod : result.getList()) if (!TextUtils.isEmpty(vod.getPic())) return vod.getPic();
        return null;
    }

    /** 当前焦点如果在海报上，返回该海报地址；在顶栏/分类条上则返回 null。 */
    private String focusPic() {
        View focus = getCurrentFocus();
        if (focus == null) return null;
        View child = focus;
        RecyclerView grid = null;
        while (child != null) {
            if (child.getParent() instanceof RecyclerView) {
                grid = (RecyclerView) child.getParent();
                break;
            }
            child = child.getParent() instanceof View ? (View) child.getParent() : null;
        }
        if (grid == null || !(grid.getAdapter() instanceof ItemBridgeAdapter)) return null;
        int position = grid.getChildAdapterPosition(child);
        RecyclerView.ViewHolder holder = position < 0 ? null : grid.findViewHolderForAdapterPosition(position);
        if (!(holder instanceof ItemBridgeAdapter.ViewHolder)) return null;
        Object item = ((ItemBridgeAdapter.ViewHolder) holder).getItem();
        return item instanceof Vod ? ((Vod) item).getPic() : null;
    }

    private void updateBackdrop() {
        String url = focusPic();
        if (TextUtils.isEmpty(url)) {
            // 焦点在顶栏/分类条时保持当前氛围图，避免回到首页或上移焦点时背景闪一下。
            if (mBackdropUrl != null) return;
            url = mFallbackPic;
        }
        if (TextUtils.isEmpty(url) || url.equals(mBackdropUrl)) return;
        mBackdropUrl = url;
        // 氛围底图（第 28 轮统一详情页终态）：480×270 小解码 + BackdropBlur 氛围模糊管线。
        Glide.with(this).asBitmap().load(ImgUtil.getUrl(url)).override(480, 270)
                .transform(new com.bumptech.glide.load.MultiTransformation<>(new com.bumptech.glide.load.resource.bitmap.CenterCrop(), new com.fongmi.android.tv.utils.BackdropBlur()))
                .transition(com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade(500))
                .into(mBinding.backdrop);
    }

    /** 当前分类第一张海报加载完成：顶栏没有焦点海报时用它做氛围底图。 */
    public void onFirstPic(String pic) {
        if (TextUtils.isEmpty(pic) || pic.equals(mFallbackPic)) return;
        mFallbackPic = pic;
        if (focusPic() == null) {
            mBackdropUrl = null;
            updateBackdrop();
        }
    }

    public FolderFragment getCurrentFolder() {
        return getFragment();
    }

    /** 顶栏「筛选」：始终显示（没有筛选项时置灰不可聚焦），焦点链路固定为 返回 ↔ 筛选 ↓ 分类。 */
    private void bindFilter() {
        mBinding.filter.setOnClickListener(v -> {
            if (mBinding.filter.isEnabled()) updateFilter();
        });
        mBinding.filter.setNextFocusLeftId(R.id.back);
        mBinding.filter.setNextFocusUpId(R.id.back);
        mBinding.back.setNextFocusRightId(R.id.filter);
    }

    /** 没有筛选项时按钮置灰且不进焦点，文案变「无筛选」，用户一眼能看出这个源没有筛选。 */
    private void updateFilterState() {
        Class type = getType();
        boolean hasFilter = type != null && !Cache.get(type).isEmpty();
        boolean opened = type != null && type.getFilter();
        mBinding.filter.setText(!hasFilter ? R.string.filter_none : opened ? R.string.filter_close : R.string.filter);
        mBinding.filter.setEnabled(hasFilter);
        mBinding.filter.setFocusable(hasFilter);
        mBinding.filter.setAlpha(hasFilter ? 1f : 0.3f);
        mBinding.filter.setSelected(opened);
    }

    @Override
    protected void initEvent() {
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.typeRecycler.setSelectedPosition(position);
                mAdapter.setPosition(position);
                updateFilterState();
                FolderFragment folder = getFragment();
                String pic = folder == null ? null : folder.getFirstPic();
                if (!TextUtils.isEmpty(pic)) mFallbackPic = pic;
                if (focusPic() == null) {
                    mBackdropUrl = null;
                    updateBackdrop();
                }
            }
        });
    }

    private void setRecyclerView() {
        mBinding.typeRecycler.requestFocus();
        mBinding.typeRecycler.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.typeRecycler.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.typeRecycler.setAdapter(mAdapter = new TypeAdapter(this));
        App.post(() -> mBinding.typeRecycler.requestFocus(), 200);
    }

    /** 从分类 tab 按下方向键直接进内容网格，不要先在 pager 上空转一格。 */
    private boolean focusGrid() {
        try {
            return getFragment().requestFocus();
        } catch (Exception e) {
            return false;
        }
    }

    private void setTypes() {
        // QBox: 每次进入分类页都从「筛选收起」开始，不沿用上次的展开状态。
        for (com.fongmi.android.tv.bean.Class type : getResult().getTypes()) type.setFilter(false);
        mAdapter.addAll(getResult().getTypes());
    }

    private void setPager() {
        mBinding.pager.setAdapter(new PageAdapter(getSupportFragmentManager()));
    }

    private boolean isFilterVisible() {
        return Optional.ofNullable(getType()).map(Class::getFilter).orElse(false);
    }

    private void updateFilter() {
        Optional.ofNullable(getType()).ifPresent(this::updateFilter);
    }

    private void updateFilter(Class item) {
        boolean open = !item.getFilter();
        item.setFilter(open);
        getFragment().toggleFilter(open);
        setFilterBand(open);
        mAdapter.notifyItemRangeChanged(mAdapter.indexOf(item), 1);
        updateFilterState();
        if (open) App.post(this::focusGrid, 90);
    }

    public void closeFilter() {
        if (isFilterVisible()) updateFilter();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.CATEGORY) getFragment().onRefresh();
    }

    @Override
    public void onItemClick(Class item) {
        int index = mAdapter.indexOf(item);
        if (index == mBinding.pager.getCurrentItem()) {
            // 再点一次当前分类 = 展开/收起筛选；与首页「点一次进分类」保持一致的点击语义。
            if (!Cache.get(item).isEmpty()) updateFilter(item);
            return;
        }
        mBinding.pager.setCurrentItem(index);
        updateFilterState();
    }

    @Override
    public void onRefresh(Class item) {
        getFragment().onRefresh();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) updateFilter();
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            // 上下键统一在 Activity 层路由，避免 tab 内部的 CustomTypeView 把事件吃掉。
            View focus = getCurrentFocus();
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && isInside(mBinding.filter, focus)) {
                mBinding.back.requestFocus();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && isInside(mBinding.filter, focus)) {
                mBinding.typeRecycler.requestFocus();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && isInside(mBinding.typeRecycler, focus)) {
                mBinding.back.requestFocus();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && isInside(mBinding.back, focus)) {
                if (mBinding.filter.isFocusable()) mBinding.filter.requestFocus();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && isInside(mBinding.typeRecycler, focus)) {
                return focusGrid();
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && isInside(mBinding.back, focus)) {
                mBinding.typeRecycler.requestFocus();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && isInside(mBinding.pager, focus)) {
                FolderFragment folder = getFragment();
                if (folder != null && folder.upToFilter()) return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && isInside(mBinding.pager, focus) && isPagerAtTop()) {
                return focusTopBar();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /** 内容区在最上面按上：回到分类条（分类条再按上才到返回键，返回键按右才是筛选）。 */
    private boolean focusTopBar() {
        mBinding.typeRecycler.requestFocus();
        return true;
    }

    private boolean isPagerAtTop() {
        try {
            return getFragment().isAtTop();
        } catch (Exception e) {
            return false;
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
    protected void onBackInvoked() {
        if (isFilterVisible()) updateFilter();
        else if (getFragment().canBack()) getFragment().goBack();
        else super.onBackInvoked();
    }

    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Class type = mAdapter.get(position);
            return FolderFragment.newInstance(getKey(), type);
        }

        @Override
        public int getCount() {
            return mAdapter.getItemCount();
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }
    }
}
