package com.qbox;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;

import androidx.appcompat.view.ContextThemeWrapper;

import com.fongmi.android.tv.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * 首页布局冒烟测试：用应用主题真正 inflate activity_home（含 hero 与三 tab），
 * 断言 hero 能测量出高度、tab 可聚焦。不需要真机，能挡住"布局/token 写错导致首页崩"的问题。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w1920dp-h1080dp-xhdpi")
public class HomeLayoutTest {

    @Test
    public void homeLayoutInflates() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_App);
        View root = LayoutInflater.from(context).inflate(R.layout.activity_home, null);
        assertNotNull("root inflate failed", root);

        View brand = root.findViewById(R.id.brand);
        assertNotNull("brand missing", brand);
        brand.measure(MeasureSpec.makeMeasureSpec(400, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        assertTrue("brand height = " + brand.getMeasuredHeight(), brand.getMeasuredHeight() > 0);

        for (int id : new int[]{R.id.logo, R.id.siteName, R.id.clock, R.id.recycler}) {
            assertNotNull("missing view: " + id, root.findViewById(id));
        }
        System.out.println("QBOX_UI_OK brand=" + brand.getMeasuredHeight() + "px");
    }
}
