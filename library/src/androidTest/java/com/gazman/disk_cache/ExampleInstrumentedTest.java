package com.gazman.disk_cache;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private DiskCache diskCache;

    @Before
    public void setUp() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        diskCache = new DiskCache(appContext, "test_db_" + System.currentTimeMillis(), 1024 * 1024 * 2);
    }

    @After
    public void tearDown() {
        diskCache.shutDownAndWait();
    }

    @Test
    public void testPutGetAsObject() throws Exception {
        final String message = "Hello there";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBytes("UTF-8"));

        diskCache.put("key1", inputStream);
        diskCache.getAsObject("key1", new DiskCache.ParserCallback<String>() {
            @Override
            public String parse(@NonNull File file) throws IOException {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[fileInputStream.available()];
                //noinspection ResultOfMethodCallIgnored
                fileInputStream.read(buffer);
                return new String(buffer, "UTF-8");
            }

            @Override
            public void onError(@NonNull Throwable e) {
                fail(e.getMessage());
            }

            @Override
            public void onResult(@Nullable String result) {
                assertEquals(message, result);
            }
        });
    }

    @Test
    public void testPutGetAsFile() throws Exception {
        final String message = "Hello there";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBytes("UTF-8"));

        diskCache.put("key1", inputStream);
        diskCache.getAsFile("key1", new DiskCache.FileCallback() {
            @Override
            public void onResult(@Nullable File file) {
                if (file != null) {
                    assertTrue(file.exists());
                } else {
                    fail("File is null");
                }
            }
        });
    }

    @Test
    public void testDelete() throws Exception {
        final String message = "Hello there";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(message.getBytes("UTF-8"));

        diskCache.put("key1", inputStream);
        diskCache.delete("key1");

        diskCache.getAsObject("key1", new DiskCache.ParserCallback<String>() {
            @Override
            public String parse(@NonNull File file) throws Exception {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[fileInputStream.available()];
                //noinspection ResultOfMethodCallIgnored
                fileInputStream.read(buffer);
                return new String(buffer, "UTF-8");
            }

            @Override
            public void onError(@NonNull Throwable e) {
                fail(e.getMessage());
            }

            @Override
            public void onResult(@Nullable String result) {
                assertEquals(result, null);
            }
        });
    }

    @Test
    public void testAutoDelete() throws Exception {
        diskCache.put("object1", createObject(1024 * 1024));
        diskCache.put("object2", createObject(1024 * 1024));
        validateObject("object1", false);
        diskCache.put("object3", createObject(1024 * 1024));
        validateObject("object1", false);
        validateObject("object3", false);
        validateObject("object2", true);
    }

    private void validateObject(final String key, final boolean deleted) {
        diskCache.getAsFile(key, new DiskCache.FileCallback() {
            @Override
            public void onResult(@Nullable File file) {
                assertEquals(key, deleted, file == null || !file.exists());
            }
        });
    }

    private InputStream createObject(int size) {
        Random random = new Random(size);
        byte[] data = new byte[size];
        random.nextBytes(data);
        return new ByteArrayInputStream(data);
    }
}
