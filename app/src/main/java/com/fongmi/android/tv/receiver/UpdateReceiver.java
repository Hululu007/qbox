package com.fongmi.android.tv.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.Notify;

/** 接收 PackageInstaller 会话的安装结果；系统需要用户确认时把确认页拉起来。 */
public class UpdateReceiver extends BroadcastReceiver {

    public static final String ACTION = "com.qbox.app.UPDATE_STATUS";

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(confirm);
            }
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            Notify.show(R.string.update_install_success);
        } else {
            Notify.show(R.string.update_install_failed);
        }
    }
}
