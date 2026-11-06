package com.fongmi.android.tv.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Cache;
import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Value;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentTypeBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.ui.activity.VodActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.presenter.FilterPresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TypeFragment extends BaseFragment implements CustomScroller.Callback, VodPresenter.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private HashMap<String, String> mExtends;
    private FragmentTypeBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private ArrayObjectAdapter mLast;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private List<Filter> mFilters;
    private boolean headerVisible;
    private boolean filterVisible;
    private String mFirstPic;
    /** 当前行高（px）。海报尺寸变了这个值就变，列表要按新几何重排。 */
    private int mRowHeight = -1;
    /** 当前列数。-1 表示还没算过；变了说明用户改了海报尺寸。 */
    private int mColumn = -1;
    private int mLastFilterRow;
    private boolean firstVisible = true;

    public static TypeFragment newInstance(String key, String typeId, Style style, HashMap<String, String> extend, boolean folder) {
        Bundle args = new Bundle();
        args.putString("key", key);
        args.putString("typeId", typeId);
        args.putBoolean("folder", folder);
        args.putParcelable("style", style);
        args.putSerializable("extend", extend);
        TypeFragment fragment = new TypeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private String getKey() {
        return getArguments().getString("key");
    }

    private String getTypeId() {
        return getArguments().getString("typeId");
    }

    private boolean isFolder() {
        return getArguments().getBoolean("folder");
    }

    private Style getStyle() {
        return isFolder() ? Style.list() : getSite().getStyle(getArguments().getParcelable("style"));
    }

    private HashMap<String, String> getExtend() {
        return (HashMap<String, String>) getArguments().getSerializable("extend");
    }

    private List<Filter> getFilter() {
        return Cache.copy(getTypeId());
    }

    private Site getSite() {
        return VodConfig.get().getSite(getKey());
    }

    private FolderFragment getParent() {
        return ((FolderFragment) getParentFragment());
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentTypeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mScroller = new CustomScroller(this);
        mExtends = getExtend();
        mFilters = getFilter();
        setRecyclerView();
        setViewModel();
        setFilters();
        getVideo();
    }

    @Override
    protected void initEvent() {
        mBinding.swipeLayout.setOnRefreshListener(this);
        mBinding.recycler.addOnScrollListener(mScroller);
    }

    @SuppressLint("RestrictedApi")
    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(Vod.class, new VodPresenter(this, Style.list()));
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(8, FocusHighlight.ZOOM_FACTOR_NONE), FilterPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        applyRowMetrics(Style.rect());
    }

    /**
     * 分类页和首页共用同一套顶部几何：裁剪线钉在内容区顶边、选中行贴顶。
     * 这样换行以后上一行整行滚出去，第一排永远干净。
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
        // 内容到达会重算行距；筛选面板展开期间保持紧凑行距不被覆盖。
        if (filterVisible) mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(8));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getResult().observe(getViewLifecycleOwner(), this::setAdapter);
        mViewModel.getAction().observe(getViewLifecycleOwner(), result -> Notify.show(result.getMsg()));
    }

    private void setFilters() {
        for (Filter filter : mFilters) {
            if (mExtends.containsKey(filter.getKey())) {
                filter.setSelected(mExtends.get(filter.getKey()));
            }
        }
    }

    private void setClick(ArrayObjectAdapter adapter, String key, Value item) {
        for (int i = 0; i < adapter.size(); i++) ((Value) adapter.get(i)).setSelected(item);
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
        if (item.isSelected()) mExtends.put(key, item.getV());
        else mExtends.remove(key);
        // 记住是哪一排被点了：刷新会把内容行整个换掉，焦点会被系统回收，
        // 回来时要还给这一排，而不是跳回第一排或者干脆消失。
        mLastFilterRow = indexOfFilterKey(key);
        onRefresh();
    }

    private int indexOfFilterKey(String key) {
        for (int i = 0; i < mFilters.size(); i++) if (mFilters.get(i).getKey().equals(key)) return i;
        return 0;
    }

    /** 刷新后把焦点还给刚才那一排筛选项。 */
    private void restoreFilterFocus() {
        if (!filterVisible || mBinding == null) return;
        if (mBinding.recycler.findFocus() != null) return;
        int row = Math.max(0, mLastFilterRow);
        mBinding.recycler.setSelectedPosition(row);
        mBinding.recycler.requestFocus();
    }

    private void getVideo() {
        mLast = null;
        checkFilter();
        mScroller.reset();
        getVideo(getTypeId(), "1");
    }

    private void getVideo(String typeId, String page) {
        mViewModel.categoryContent(getKey(), typeId, page, true, mExtends);
    }

    private void setAdapter(Result result) {
        boolean first = mScroller.first();
        boolean flag = mExtends.isEmpty();
        int size = result.getList().size();
        mBinding.progressLayout.showContent(first & flag, size);
        mBinding.swipeLayout.setRefreshing(false);
        mScroller.endLoading(result);
        if (size > 0) addVideo(result);
        // 点筛选会重建列表，焦点在重建过程中被回收，这里补一次。
        App.post(this::restoreFilterFocus, 260);
    }

    private void addVideo(Result result) {
        if (mFirstPic == null) for (Vod vod : result.getList()) if (vod.getPic() != null && !vod.getPic().isEmpty()) {
            mFirstPic = vod.getPic();
            break;
        }
        notifyBackdrop();
        Style style = result.getStyle(getStyle());
        // 海报尺寸在设置里改过：列数变了，旧的分片行全部失效，必须清空按新列数重排。
        int column = Product.getColumn(style);
        if (mColumn != -1 && mColumn != column) {
            mAdapter.clear();
            mScroller.reset();
        }
        mColumn = column;
        applyRowMetrics(style);
        if (style.isList()) mAdapter.addAll(mAdapter.size(), result.getList());
        else addGrid(result.getList(), style);
        checkMore();
        // 同理：首屏数据落位后补一次贴顶校正，避免第一排放大的顶部被裁。
        App.post(() -> mBinding.recycler.requestSnap(), 160);
    }

    /** 当前分类第一张海报：给 VodActivity 的整屏氛围底图兜底。 */
    public String getFirstPic() {
        return mFirstPic;
    }

    private void notifyBackdrop() {
        if (mFirstPic == null || !(getActivity() instanceof VodActivity)) return;
        VodActivity activity = (VodActivity) getActivity();
        if (getParentFragment() == activity.getCurrentFolder()) activity.onFirstPic(mFirstPic);
    }

    private void checkMore() {
        if (mScroller.isDisable() || mAdapter.size() >= 5) return;
        mScroller.checkMore();
    }

    private boolean checkLastSize(List<Vod> items, Style style) {
        if (mLast == null || items.isEmpty()) return false;
        int size = Product.getColumn(style) - mLast.size();
        if (size == 0) return false;
        size = Math.min(size, items.size());
        mLast.addAll(mLast.size(), items.subList(0, size));
        addGrid(items.subList(size, items.size()), style);
        return true;
    }

    private void addGrid(List<Vod> items, Style style) {
        if (checkLastSize(items, style)) return;
        List<ListRow> rows = new ArrayList<>();
        VodPresenter presenter = new VodPresenter(this, style);
        for (List<Vod> part : Lists.partition(items, Product.getColumn(style))) {
            mLast = new ArrayObjectAdapter(presenter);
            mLast.addAll(0, part);
            rows.add(new ListRow(mLast));
        }
        mAdapter.addAll(mAdapter.size(), rows);
    }

    private ListRow getRow(Filter filter) {
        FilterPresenter presenter = new FilterPresenter(filter.getKey());
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
        presenter.setOnClickListener((key, item) -> setClick(adapter, key, item));
        adapter.setItems(filter.getValue(), null);
        return new ListRow(adapter);
    }

    private void showFilter() {
        List<ListRow> rows = new ArrayList<>();
        for (Filter filter : mFilters) rows.add(getRow(filter));
        // 先把内容起点压低，再插行：顺序反了会让列表先按 144dp 布局一次再重排，
        // 焦点在这两次布局之间容易被带到第二排去。
        // 第 29 轮：子行与 tab 间距 28dp → ~10dp（约 1/3），贴近 tab 但留 4dp 固定垫层保住分组感。
        mBinding.recycler.setContentTopTemp(getHeaderBottomPx());
        // 筛选行间距收紧：全局行距是为海报墙算的（≥24dp），chip 行沿用会显得稀疏。
        // 面板展开期间改 8dp（chip 34dp + 行间 16dp ≈ chip 半高，不拥挤的下限），
        // 收起时 applyRowMetrics 会恢复海报行距。
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(8));
        mAdapter.addAll(0, rows);
        // 筛选行不参与滚动贴顶，否则从第二排筛选按一下 ↓，整块筛选会被甩出屏幕。
        mBinding.recycler.setSnapStartIndex(mFilters.size());
        // 面板展开期间整体退出「逐行贴顶」：只保证焦点行可见，筛选面板固定在上方。
        mBinding.recycler.setSnapEnabled(false);
        mBinding.recycler.setRowsKeepVisible(true);
        // 展开后焦点直接进第一排第一个筛选项 —— 之前焦点留在原来选中的海报行上，
        // 用户按上会直接跳到左上角返回键，第一排根本选不到。
        mBinding.recycler.postDelayed(this::focusFirstFilter, 280);
    }

    private void focusFirstFilter() {
        if (mBinding == null || mFilters.isEmpty()) return;
        mBinding.recycler.scrollToPosition(0);
        mBinding.recycler.setSelectedPosition(0);
        if (mBinding.recycler.requestFocus()) return;
        // setSelectedPosition 有时只是把行选中，不会把焦点落到行内第一颗 chip 上；
        // 这时直接抓第一行里第一个可聚焦控件，保证「展开就能选第一排」。
        View first = firstFocusable(mBinding.recycler);
        if (first != null) first.requestFocus();
    }

    private View firstFocusable(View view) {
        if (view == null || view.getVisibility() != View.VISIBLE) return null;
        if (view.isFocusable() && view.isEnabled()) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = firstFocusable(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void hideFilter() {
        mAdapter.removeItems(0, mFilters.size());
        mBinding.recycler.setSnapStartIndex(0);
        mBinding.recycler.setSnapEnabled(true);
        mBinding.recycler.setRowsKeepVisible(false);
        mRowHeight = -1;
        applyRowMetrics(getStyle());
        // 焦点原来在被移除的筛选行上，收起来就悬空了：交还给第一排海报。
        App.post(() -> {
            if (mBinding == null) return;
            if (mBinding.recycler.findFocus() != null) return;
            mBinding.recycler.setSelectedPosition(0);
            mBinding.recycler.requestFocus();
        }, 220);
    }

    private int getHeaderBottomPx() {
        return getResources().getDimensionPixelSize(R.dimen.qbox_header_total);
    }

    public void toggleFilter(boolean visible) {
        if (mFilters.isEmpty()) return;
        this.filterVisible = visible;
        if (visible) showFilter();
        else hideFilter();
    }

    public boolean requestFocus() {
        if (mBinding == null) return false;
        return mBinding.recycler.requestFocus();
    }

    /**
     * 筛选面板展开时，从海报第一排按 ↑ 直接把焦点交给筛选项。
     * 之前第一次 ↑ 只把筛选行滚回视口、焦点还留在海报上（看不出发生了什么），
     * 用户得按两次；这里在 Activity 层拦截，第一次就落到 chip。
     */
    public boolean upToFilter() {
        if (!filterVisible || mBinding == null || mFilters == null || mFilters.isEmpty()) return false;
        if (mBinding.recycler.getSelectedPosition() != mFilters.size()) return false;
        int row = Math.max(0, Math.min(mLastFilterRow, mFilters.size() - 1));
        mBinding.recycler.setSelectedPosition(row);
        App.post(() -> {
            if (mBinding == null) return;
            androidx.recyclerview.widget.RecyclerView.ViewHolder holder = mBinding.recycler.findViewHolderForAdapterPosition(row);
            View target = holder == null ? null : firstFocusable(holder.itemView);
            if (target != null) target.requestFocus();
        });
        return true;
    }

    /** 内容区是否已经滚到第一行：决定「上」是回顶栏，还是继续在网格里换行。 */
    public boolean isAtTop() {
        if (mBinding == null) return true;
        androidx.recyclerview.widget.RecyclerView recycler = mBinding.recycler;
        if (recycler.getChildCount() == 0) return true;
        android.view.View first = recycler.getChildAt(0);
        if (first == null) return true;
        int tolerance = Math.max(2, Math.round(8 * getResources().getDisplayMetrics().density));
        return first.getTop() >= recycler.getPaddingTop() - tolerance;
    }

    private void checkFilter() {
        int adapterSize = mAdapter.size();
        int filterSize = filterVisible ? mFilters.size() : 0;
        if (adapterSize > filterSize) mAdapter.removeItems(filterSize, mAdapter.size() - filterSize);
        if (adapterSize == 0) mBinding.progressLayout.showProgress();
        else mBinding.swipeLayout.setRefreshing(true);
    }

    public void onRefresh() {
        getVideo();
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isAction()) {
            mViewModel.action(getKey(), item.getAction());
        } else if (item.isFolder()) {
            getParent().openFolder(item.getId(), mExtends);
            headerVisible = mBinding.recycler.isHeaderVisible();
        } else {
            if (getSite().isIndex()) CollectActivity.start(requireActivity(), item.getName());
            else VideoActivity.start(requireActivity(), getKey(), item.getId(), item.getName(), item.getPic(), isFolder() ? item.getName() : null);
        }
    }

    @Override
    public boolean onLongClick(Vod item) {
        if (item.isAction() || item.isFolder()) return false;
        CollectActivity.start(requireActivity(), item.getName());
        return true;
    }

    @Override
    public boolean onLoadMore(String page) {
        getVideo(getTypeId(), page);
        return true;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            mBinding.recycler.showHeader();
        } else {
            if (headerVisible) mBinding.recycler.showHeader();
            else mBinding.recycler.hideHeader();
            mBinding.recycler.requestFocus();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mBinding == null || !isVisibleToUser) return;
        // 只有第一次进入这个分类才回顶。切到别的 tab 再切回来，用户期望停在原来的位置，
        // 每次可见都 moveToTop 会让人以为"列表被重置了"。
        if (firstVisible) {
            firstVisible = false;
            mBinding.recycler.moveToTop();
        }
    }
}
