package id.shisen.app.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import id.shisen.app.data.model.Anime;

public class MenatapDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "menatap.db";
    private static final int DATABASE_VERSION = 1;

    private static volatile MenatapDatabase instance;

    public static MenatapDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (MenatapDatabase.class) {
                if (instance == null) {
                    instance = new MenatapDatabase(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private MenatapDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS history (" +
                "slug TEXT PRIMARY KEY, " +
                "title TEXT, " +
                "poster TEXT, " +
                "last_ep INTEGER, " +
                "progress_ms INTEGER, " +
                "duration_ms INTEGER, " +
                "updated_at INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS bookmarks (" +
                "slug TEXT PRIMARY KEY, " +
                "title TEXT, " +
                "poster TEXT, " +
                "score REAL, " +
                "format TEXT, " +
                "eps_counter TEXT, " +
                "created_at INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS cache_kv (" +
                "key TEXT PRIMARY KEY, " +
                "value TEXT, " +
                "expires_at INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS watched_eps (" +
                "slug TEXT, " +
                "ep INTEGER, " +
                "PRIMARY KEY(slug, ep))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS history");
        db.execSQL("DROP TABLE IF EXISTS bookmarks");
        db.execSQL("DROP TABLE IF EXISTS cache_kv");
        db.execSQL("DROP TABLE IF EXISTS watched_eps");
        onCreate(db);
    }

    // --- History Methods ---
    public void saveHistory(String slug, String title, String poster, int ep, long progressMs, long durationMs) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("slug", slug);
        cv.put("title", title);
        cv.put("poster", poster);
        cv.put("last_ep", ep);
        cv.put("progress_ms", progressMs);
        cv.put("duration_ms", durationMs);
        cv.put("updated_at", System.currentTimeMillis());
        db.insertWithOnConflict("history", null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        // Mark episode watched
        markEpisodeWatched(slug, ep);
    }

    public void clearAllHistory() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("history", null, null);
    }

    public List<Anime> getHistoryList() {
        List<Anime> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT slug, title, poster, last_ep, progress_ms, duration_ms, updated_at FROM history ORDER BY updated_at DESC LIMIT 50", null);
        if (c != null) {
            while (c.moveToNext()) {
                Anime a = new Anime();
                a.slug = c.getString(0);
                a.title = c.getString(1);
                a.poster = c.getString(2);
                a.lastPlayedEp = c.getInt(3);
                a.lastPlayedProgressMs = c.getLong(4);
                long dur = c.getLong(5);
                a.epsCounter = "Ep " + a.lastPlayedEp;
                list.add(a);
            }
            c.close();
        }
        return list;
    }

    public int getLastWatchedEpisode(String slug) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT last_ep FROM history WHERE slug = ?", new String[]{slug});
        int ep = 0;
        if (c != null) {
            if (c.moveToFirst()) {
                ep = c.getInt(0);
            }
            c.close();
        }
        return ep;
    }

    // --- Watched Episodes ---
    public void markEpisodeWatched(String slug, int ep) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("slug", slug);
        cv.put("ep", ep);
        db.insertWithOnConflict("watched_eps", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public boolean isEpisodeWatched(String slug, int ep) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM watched_eps WHERE slug = ? AND ep = ?", new String[]{slug, String.valueOf(ep)});
        boolean watched = (c != null && c.moveToFirst());
        if (c != null) c.close();
        return watched;
    }

    // --- Bookmarks Methods ---
    public void toggleBookmark(Anime anime) {
        if (isBookmarked(anime.slug)) {
            removeBookmark(anime.slug);
        } else {
            addBookmark(anime);
        }
    }

    public void addBookmark(Anime anime) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("slug", anime.slug);
        cv.put("title", anime.title);
        cv.put("poster", anime.poster);
        cv.put("score", anime.score);
        cv.put("format", anime.format);
        cv.put("eps_counter", anime.epsCounter);
        cv.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict("bookmarks", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removeBookmark(String slug) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("bookmarks", "slug = ?", new String[]{slug});
    }

    public boolean isBookmarked(String slug) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM bookmarks WHERE slug = ?", new String[]{slug});
        boolean exists = (c != null && c.moveToFirst());
        if (c != null) c.close();
        return exists;
    }

    public List<Anime> getBookmarkList() {
        List<Anime> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT slug, title, poster, score, format, eps_counter FROM bookmarks ORDER BY created_at DESC", null);
        if (c != null) {
            while (c.moveToNext()) {
                Anime a = new Anime();
                a.slug = c.getString(0);
                a.title = c.getString(1);
                a.poster = c.getString(2);
                a.score = c.getDouble(3);
                a.format = c.getString(4);
                a.epsCounter = c.getString(5);
                a.isFavorite = true;
                list.add(a);
            }
            c.close();
        }
        return list;
    }

    // --- Key-Value Cache with TTL ---
    public void setCache(String key, String value, long ttlMs) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("value", value);
        cv.put("expires_at", System.currentTimeMillis() + ttlMs);
        db.insertWithOnConflict("cache_kv", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getCache(String key) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT value, expires_at FROM cache_kv WHERE key = ?", new String[]{key});
        if (c != null && c.moveToFirst()) {
            long expiresAt = c.getLong(1);
            if (expiresAt > System.currentTimeMillis()) {
                String val = c.getString(0);
                c.close();
                return val;
            }
            c.close();
        }
        return null;
    }
}
