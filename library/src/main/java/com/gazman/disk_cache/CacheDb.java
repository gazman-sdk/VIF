package com.gazman.disk_cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ilya Gazman on 10/16/2017.
 */

class CacheDb {
    private final SqlHelper sqlHelper;

    CacheDb(Context context, String dbName) {
        sqlHelper = new SqlHelper(context, dbName);
    }

    long getTotalSize() {
        return new QueryHelper<Long>(-1L) {
            @Override
            Long parse(Cursor cursor) {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
                return null;
            }
        }.query("SELECT sum(file_size) FROM cache where finalized = 1");
    }

    int prepareKey(String key) {
        long currentTimeMillis = System.currentTimeMillis();
        execute("INSERT or REPLACE into cache\n" +
                        "(ROWID, client_key, file_size, finalized, last_used, created_on)\n" +
                        "VALUES\n" +
                        "((SELECT ROWID from cache WHERE client_key = ? " +
                        "UNION " +
                        "SELECT coalesce(max(ROWID), 0) + 1 from cache limit 1), ?, 0, 0, ?, ?);"
                , key, key, currentTimeMillis, currentTimeMillis);
        return new QueryHelper<Integer>(-1) {
            @Override
            Integer parse(Cursor cursor) {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
                return null;
            }
        }.query("select max(ROWID) from cache");
    }

    void finalizeKey(long key, long length) {
        execute("update cache set finalized = 1, file_size = ? WHERE rowId = ?", length, key);
    }

    long getKey(String key) {
        long k = new QueryHelper<Long>(-1L) {
            @Override
            Long parse(Cursor cursor) {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
                return null;
            }
        }.query("SELECT rowId FROM cache WHERE client_key = ? and finalized = 1", key);
        if (k != -1) {
            execute("UPDATE cache set last_used = ? WHERE client_key = ?", System.currentTimeMillis(), key);
        }
        return k;
    }

    List<Integer> getCorruptedFiles() {
        return new QueryHelper<List<Integer>>(new ArrayList<Integer>()) {
            @Override
            List<Integer> parse(Cursor cursor) {
                ArrayList<Integer> list = new ArrayList<>();
                while (cursor.moveToNext()) {
                    list.add(cursor.getInt(0));
                }
                return list;
            }
        }.query("SELECT ROWID from cache WHERE finalized = 0");
    }

    void deleteKeys(List<Integer> cleanedKeys) {
        StringBuilder stringBuilder = new StringBuilder("DELETE from cache WHERE ROWID in (");
        boolean first = true;
        for (int cleanedKey : cleanedKeys) {
            if (!first) {
                stringBuilder.append(",");
            } else {
                first = false;
            }
            stringBuilder.append(cleanedKey);
        }
        stringBuilder.append(")");
        execute(stringBuilder.toString());
    }

    private void execute(String sql, Object... bindArgs) {
        sqlHelper.getWritableDatabase().execSQL(sql, bindArgs);
    }

    List<Integer> getKeysToDelete(final long maxSize) {
        return new QueryHelper<List<Integer>>(new ArrayList<Integer>()) {
            @Override
            List<Integer> parse(Cursor cursor) {
                ArrayList<Integer> list = new ArrayList<>();
                long count = 0;
                while (cursor.moveToNext()){
                    count += cursor.getLong(1);
                    if(count > maxSize){
                        list.add(cursor.getInt(0));
                    }
                }
                return list;
            }
        }.query("SELECT ROWID, file_size from cache ORDER BY last_used desc");
    }

    void close() {
        sqlHelper.close();
    }

    private abstract class QueryHelper<T> {

        private T defaultValue;

        QueryHelper(T defaultValue) {
            this.defaultValue = defaultValue;
        }

        T query(String sql) {
            return query(sql, (String[]) null);
        }

        T query(String sql, String... selectionArgs) {
            Cursor cursor = null;
            try {
                SQLiteDatabase db = sqlHelper.getReadableDatabase();
                cursor = db.rawQuery(sql, selectionArgs);
                T parse = parse(cursor);
                return parse != null ? parse : defaultValue;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return defaultValue;
        }

        abstract T parse(Cursor cursor);


    }
}
