package com.qbox;

import static org.junit.Assert.*;

import com.fongmi.android.tv.bean.Url;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class UrlTest {

    @Test
    public void selectingEmptyUrlDoesNotPoisonLaterValues() {
        Url url = Url.create().set(0).add("first");
        assertEquals("first", url.v());
        assertEquals(0, url.getPosition());
    }

    @Test
    public void negativeAndOversizedSelectionsAreClamped() {
        Url url = Url.create().add("A", "a").add("B", "b");
        assertEquals("a", url.set(-1).v());
        assertEquals("b", url.set(100).v());
        assertEquals("", url.v(-1));
        assertEquals("", url.n(-1));
        assertEquals("", url.v(2));
    }

    @Test
    public void replacementHandlesSelectionInvalidatedByListChanges() {
        Url url = Url.create().add("a").add("b").set(1);
        url.getValues().remove(1);
        assertSame(url, url.replace("new"));
        assertEquals("new", url.v());
    }
}
