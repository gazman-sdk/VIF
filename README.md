VIF(Very Important Files) is LRU(Least Recently Used) disk cache library, with SqlLite journal.

### Features

* Light library - No third party dependencies, 15.8KB size.
* Corrupted files protection - It is guarantee that each time you access cache you will get non
  corrupted files
* LRU memory management - When more memory needed least recently used files will be deleted first
* Developers friendly - You can access both InputStream and File objects

### Usage

```Java
    @Override
    public void run() {
        appCache = new DiskCache(this, "app_cache", 1024 * 1024 * 20);// 20 MB cache
        downloadImage(url);
        loadImage(imageView, url);
    }

    private void downloadImage(String imageUrl) {
        URL url = new URL(imageUrl);
        URLConnection connection = url.openConnection();
        connection.connect();

        appCache.put(imageUrl, connection.getInputStream());
    }

    private void loadImage(final ImageView imageView, String imageUrl) {
        appCache.getAsObject(MY_IMAGE, new DiskCache.ParserCallback<Bitmap>() {
            @Override
            public Bitmap parse(@NonNull File file) throws Exception {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            }

            @Override
            public void onError(@NonNull Throwable e) {

            }

            @Override
            public void onResult(@Nullable Bitmap result) {
                imageView.setImageBitmap(result);
            }
        });
    }
```

* **VIF.put(String key, InputStream inputStream, Runnable callback)**: Asynchronously adds entry to
  the cache and call the optional callback on the main thread when complete
* **VIF.delete(String key)**: Deletes the cache entry
* **VIF.getAsFile(String key, FileCallback callback)**: Retrieve the cached entry as file
* **VIF.getAsObject(String key, ParserCallback<T> callback)**: Retrieve the cached entry as an
  Object
* **VIF.shutDown()**: Gracefully asynchronously shut down the cache
* **VIF.shutDownAndWait()**: Gracefully synchronously shut down the cache

### License

MIT
