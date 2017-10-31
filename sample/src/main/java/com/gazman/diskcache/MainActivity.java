package com.gazman.diskcache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.gazman.disk_cache.VIF;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements Runnable {

    public static final String URL = "https://lh3.googleusercontent.com/dWU6kgfEhlLIoVwmYXcOU_bQNzQQaymLsnfeiUuHBlcOTG-56ji0e3QZLV3GP_YtEdQ=w300";
    private VIF appCache;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageView = new ImageView(this);
        setContentView(imageView);
        appCache = new VIF(this, "app_cache", 1024 * 1024 * 20);// 20 MB cache

        new Thread(this).start();
    }

    @Override
    public void run() {
        downloadImage();
        loadImage();
    }

    private void downloadImage() {
        try {
            URL url = new URL(URL);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }

            appCache.put(URL, connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadImage() {
        appCache.getAsObject(URL, new VIF.ParserCallback<Bitmap>() {
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
}
