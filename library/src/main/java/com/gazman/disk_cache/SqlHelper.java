package com.gazman.disk_cache;

import android.content.Context;
import io.requery.android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Ilya Gazman on 10/16/2017.
 */

class SqlHelper extends SQLiteOpenHelper {
    SqlHelper(Context context, String name) {
        super(context, name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE cache\n" +
                "(\n" +
                "    client_key TEXT NOT NULL,\n" +
                "    file_size INT NOT NULL,\n" +
                "    finalized INT NOT NULL,\n" +
                "    last_used INT NOT NULL,\n" +
                "    created_on INT NOT NULL\n" +
                ")");
        db.execSQL("CREATE UNIQUE INDEX cache_client_key_uindex ON cache (client_key)");
        db.execSQL("CREATE INDEX cache_last_used_index ON cache (last_used DESC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
