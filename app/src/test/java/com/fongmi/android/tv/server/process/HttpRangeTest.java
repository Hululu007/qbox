package com.fongmi.android.tv.server.process;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Map;

public class HttpRangeTest {

    private Local.HttpRange range(long size, String header) {
        return Local.HttpRange.from(size, Map.of("range", header), "etag");
    }

    @Test
    public void suffixReturnsLastBytes() {
        Local.HttpRange range = range(100, "bytes=-10");
        assertTrue(range.valid());
        assertEquals(90, range.start());
        assertEquals(99, range.end());
        assertEquals(10, range.length());
    }

    @Test
    public void fullFileRangeStillRequiresPartialResponse() {
        assertTrue(range(100, "bytes=0-").partial());
        assertTrue(range(100, "bytes=-200").partial());
        assertEquals(100, range(100, "bytes=-200").length());
    }

    @Test
    public void endIsClampedAndOpenEndIsSupported() {
        assertEquals(90, range(100, "bytes=10-200").length());
        assertEquals(90, range(100, "bytes=10-").length());
    }

    @Test
    public void invalidAndUnsupportedRangesAreRejected() {
        for (String header : new String[]{"bytes=-0", "bytes=-", "bytes=100-", "bytes=20-10", "bytes=1", "bytes=--1", "bytes=+1-2", "bytes=0-1,4-5", "bytes=9223372036854775808-"}) {
            assertFalse(header, range(100, header).valid());
        }
        assertFalse(range(0, "bytes=0-").valid());
    }

    @Test
    public void mismatchedIfRangeFallsBackToFullResponse() {
        Local.HttpRange range = Local.HttpRange.from(100, Map.of("range", "bytes=10-", "if-range", "old"), "etag");
        assertFalse(range.partial());
        assertEquals(100, range.length());
    }

    @Test
    public void emptyFileWithoutRangeHasEmptyBody() {
        Local.HttpRange range = Local.HttpRange.from(0, Map.of(), "etag");
        assertTrue(range.valid());
        assertFalse(range.partial());
        assertEquals(0, range.length());
    }
}
