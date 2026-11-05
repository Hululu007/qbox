package com.fongmi.android.tv.ui.dialog;

import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogContentBinding;
import com.fongmi.android.tv.ui.custom.CustomMovement;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ContentDialog extends BaseAlertDialog {

    private DialogContentBinding binding;
    private CharSequence content;

    public static ContentDialog create() {
        return new ContentDialog();
    }

    public ContentDialog content(CharSequence content) {
        this.content = content;
        return this;
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogContentBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        binding.text.setText(content, TextView.BufferType.SPANNABLE);
        // 链接色收敛到语义蓝（与详情页链接一致），原为突兀的 MD 黄。
        binding.text.setLinkTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), com.fongmi.android.tv.R.color.qbox_accent));
        CustomMovement.bind(binding.text);
    }
}