package com.gazman.diskcache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.gazman.disk_cache.DiskCache;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    public static final String MY_IMAGE = "myImage";
    private DiskCache appCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appCache = new DiskCache(this, "app_cache", 1024 * 1024 * 20);// 20 MB cache
        final ImageView imageView = (ImageView) findViewById(R.id.myImage);

        new Thread() {
            @Override
            public void run() {
                downloadImage("https://lh3.googleusercontent.com/dWU6kgfEhlLIoVwmYXcOU_bQNzQQaymLsnfeiUuHBlcOTG-56ji0e3QZLV3GP_YtEdQ=w300");
                loadImage(imageView);
            }
        }.start();
    }

    private void loadImage(final ImageView imageView) {
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

    private void downloadImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }

            appCache.put(MY_IMAGE, connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
