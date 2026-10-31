package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Cache;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.databinding.ActivityVodBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.fragment.FolderFragment;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Optional;

public class VodActivity extends BaseActivity implements TypeAdapter.OnClickListener {

    private ActivityVodBinding mBinding;
    private TypeAdapter mAdapter;

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
                return focusTopBar(true);
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && isInside(mBinding.typeRecycler, focus)) {
                return focusGrid();
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && isInside(mBinding.back, focus)) {
                mBinding.typeRecycler.requestFocus();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && isInside(mBinding.pager, focus) && isPagerAtTop()) {
                return focusTopBar(false);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /** 分类条向上：先筛选、没有筛选再回返回键。 */
    private boolean focusTopBar(boolean fromTabs) {
        if (fromTabs && !mBinding.filter.isFocusable()) {
            mBinding.back.requestFocus();
            return true;
        }
        if (!fromTabs) {
            mBinding.typeRecycler.requestFocus();
            return true;
        }
        mBinding.filter.requestFocus();
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
