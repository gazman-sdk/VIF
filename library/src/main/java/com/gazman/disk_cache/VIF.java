package com.gazman.disk_cache;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ilya Gazman on 10/15/2017.
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class VIF {

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private File cacheDir;
    private byte[] buffer = new byte[1024 * 10]; // 10 KB buffer
    private CacheDb cacheDb;
    private Handler handler = new Handler(Looper.getMainLooper());
    private long totalSize;
    private Context context;
    private long maxSize;

    /**
     * Creates or restores the cache from given db state.
     *
     * @param context app context
     * @param dbName  Database name to be used by the cache service
     * @param maxSize maximum cache size
     */
    public VIF(Context context, String dbName, long maxSize) {
        this.context = context.getApplicationContext();
        this.maxSize = maxSize;
        cacheDb = new CacheDb(context, dbName);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                removeKeys(cacheDb.getCorruptedFiles());
                totalSize = cacheDb.getTotalSize();
            }
        });
    }

    /**
     * Sets the cache dir, if not set the default is context.getExternalCacheDir()
     *
     * @param cacheDir cache dir directory, if null or if not set then context.getExternalCacheDir() will be used
     */
    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * Asynchronously adds entry to the cache
     *
     * @param key         any String value will do, there is no restriction on the name
     * @param inputStream cache source, all the exceptions will be handled quietly
     *                    and stream will be closed once the reading is complete
     */
    public void put(final String key, final InputStream inputStream) {
        put(key, inputStream, null);
    }

    /**
     * Add entry to the cache
     *
     * @param key              any String value will do, there is no restriction on the name
     * @param inputStream      cache source, all the exceptions will be handled quietly
     *                         and stream will be closed once the reading is complete
     * @param completeCallback will be called once the writing is complete
     */
    public void put(final String key, final InputStream inputStream, final Runnable completeCallback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                long k = cacheDb.prepareKey(key);
                File file = toFile(k);
                saveFile(key, file, inputStream);
                long fileSize = file.length();
                cacheDb.finalizeKey(k, fileSize);
                totalSize += fileSize;
                if (completeCallback != null) {
                    handler.post(completeCallback);
                }
                if (totalSize > maxSize) {
                    freeSpace();
                }
            }
        });
    }

    /**
     * Deletes the given key
     *
     * @param key key to be deleted
     */
    public void delete(final String key) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                int k = cacheDb.prepareKey(key);
                if (k == -1) {
                    return;
                }
                totalSize = cacheDb.getTotalSize();
                File file = toFile(k);
                if (deleteFile(file)) {
                    cacheDb.deleteKeys(Collections.singletonList(k));
                } else {
                    logErrorDeletingFile(file);
                }
            }
        });
    }

    /**
     * Return the cache entry as a file and move it to the top of the queue,
     * so it will be the last file to deleted. Note that put and delete actions
     * may override this file, use with caution.
     *
     * @param key      cache entry key
     * @param callback callback for fetching the file
     */
    public void getAsFile(final String key, final FileCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                long k = cacheDb.getKey(key);
                final File file = k != -1 ? toFile(k) : null;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(file);
                    }
                });
            }
        });
    }

    /**
     * This is a save way to retrieve a cache entry, callback will be used to parse the file on
     * the cache thread, it will block all the other actions untill parsing is complete.
     * The callback.onResult will be delivered on the main thread
     *
     * @param key      cache entry key
     * @param callback result and parsing callback
     * @param <T>      Result type
     */
    public <T> void getAsObject(final String key, final ParserCallback<T> callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                long k = cacheDb.getKey(key);
                if (k == -1) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(null);
                        }
                    });
                    return;
                }
                final File file = toFile(k);
                final T result;
                try {
                    result = callback.parse(file);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(result);
                        }
                    });
                } catch (final Throwable e) {
                    e.printStackTrace();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e);
                        }
                    });
                }

            }
        });
    }

    /**
     * Asynchronously shut down the cache, any request to the cache after this call may fail
     */
    public void shutDown() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                cacheDb.close();
                cacheDb = null;
            }
        });
        executor.shutdown();
    }

    /**
     * Synchronously shut down the cache, any request to the cache after this call will fail
     */
    public void shutDownAndWait() {
        if (!executor.isShutdown()) {
            shutDown();
        }
        try {
            executor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void freeSpace() {
        removeKeys(cacheDb.getKeysToDelete(maxSize));
    }

    private void logErrorDeletingFile(File file) {
        Log.e("VIF", "Error removing file " + file + ". Will retry on next reboot");
    }

    private boolean deleteFile(File file) {
        return !file.exists() || file.delete();
    }

    private File toFile(long k) {
        if (cacheDir == null) {
            cacheDir = context.getExternalCacheDir();
        }
        return new File(cacheDir, k + ".vif");
    }

    private void removeKeys(List<Integer> keys) {
        ArrayList<Integer> cleanedKeys = new ArrayList<>();
        for (int key : keys) {
            File file = toFile(key);
            if (deleteFile(file)) {
                cleanedKeys.add(key);
            } else {
                logErrorDeletingFile(file);
            }
        }
        cacheDb.deleteKeys(cleanedKeys);
    }

    private void saveFile(String originalKey, File file, InputStream inputStream) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file, false);
            int count;
            while ((count = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
        } catch (IOException e) {
            Log.e("diskCache", "Error saving file " + originalKey);
        } finally {
            close(outputStream);
            close(inputStream);
        }
    }

    private void close(Closeable outputStream) {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback interface for retrieving the file
     */
    public interface FileCallback {
        void onResult(File file);
    }

    /**
     * Callback interface for parsing and retrieving the T result
     */
    public interface ParserCallback<T> {

        T parse(File file) throws Exception;

        void onError(Throwable e);

        void onResult(T result);
    }
}
