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

        View hero = root.findViewById(R.id.hero);
        assertNotNull("hero missing", hero);
        hero.measure(MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        assertTrue("hero height = " + hero.getMeasuredHeight(), hero.getMeasuredHeight() > 0);

        View title = root.findViewById(R.id.heroTitle);
        assertNotNull("heroTitle missing", title);
        title.measure(MeasureSpec.makeMeasureSpec(1600, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        assertTrue("heroTitle height = " + title.getMeasuredHeight(), title.getMeasuredHeight() > 0);

        for (int id : new int[]{R.id.tabHome, R.id.tabLive, R.id.tabSetting}) {
            View tab = root.findViewById(id);
            assertNotNull("tab missing: " + id, tab);
            assertTrue("tab not focusable: " + id, tab.isFocusable());
        }
        System.out.println("QBOX_UI_OK hero=" + hero.getMeasuredHeight() + "px title=" + title.getMeasuredHeight() + "px");
    }
}
