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
    private View mOldView;

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
        if (index >= 0 && index < mAdapter.getItemCount()) {
            mBinding.typeRecycler.setSelectedPosition(index);
            mBinding.pager.setCurrentItem(index);
        }
        updateFilterState();
    }

    /** 顶栏「筛选」是独立入口：分类 tab 只负责切页，不再兼职开关筛选。 */
    private void bindFilter() {
        mBinding.home.setOnClickListener(v -> finish());
        mBinding.home.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return focusGrid();
            return false;
        });
        mBinding.filter.setOnClickListener(v -> updateFilter());
        mBinding.filter.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                mBinding.typeRecycler.requestFocus();
                return true;
            }
            return false;
        });
        mBinding.back.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                mBinding.typeRecycler.requestFocus();
                return true;
            }
            return false;
        });
    }

    /** 当前分类没有筛选项时直接隐藏按钮，避免按下去没反应。 */
    private void updateFilterState() {
        Class type = getType();
        boolean hasFilter = type != null && !Cache.get(type).isEmpty();
        boolean opened = type != null && type.getFilter();
        mBinding.filter.setVisibility(hasFilter ? View.VISIBLE : View.GONE);
        mBinding.filter.setText(opened ? R.string.filter_close : R.string.filter);
    }

    @Override
    protected void initEvent() {
        mBinding.typeRecycler.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return focusGrid();
            return false;
        });
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.typeRecycler.setSelectedPosition(position);
                mBinding.typeRecycler.requestFocus();
                updateFilterState();
            }
        });
        mBinding.typeRecycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                onChildSelected(child);
            }
        });
    }

    private void setRecyclerView() {
        mBinding.typeRecycler.requestFocus();
        mBinding.typeRecycler.setHorizontalSpacing(ResUtil.dp2px(20));
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

    private void onChildSelected(@Nullable RecyclerView.ViewHolder child) {
        if (mOldView != null) mOldView.setSelected(false);
        if ((mOldView = child != null ? child.itemView : null) == null) return;
        mOldView.setSelected(true);
        App.post(mRunnable, 100);
    }

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mBinding.pager.setCurrentItem(mBinding.typeRecycler.getSelectedPosition());
        }
    };

    private boolean isFilterVisible() {
        return Optional.ofNullable(getType()).map(Class::getFilter).orElse(false);
    }

    private void updateFilter() {
        Optional.ofNullable(getType()).ifPresent(this::updateFilter);
    }

    private void updateFilter(Class item) {
        item.setFilter(!item.getFilter());
        getFragment().toggleFilter(item.getFilter());
        mAdapter.notifyItemRangeChanged(mAdapter.indexOf(item), 1);
        updateFilterState();
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
        // 再次点击当前分类 = 展开/收起筛选；点其他分类只切页。
        if (index == mBinding.pager.getCurrentItem() && !Cache.get(item).isEmpty()) updateFilter(item);
        else mBinding.pager.setCurrentItem(index);
        updateFilterState();
    }

    @Override
    public void onRefresh(Class item) {
        getFragment().onRefresh();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) updateFilter();
        return super.dispatchKeyEvent(event);
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
