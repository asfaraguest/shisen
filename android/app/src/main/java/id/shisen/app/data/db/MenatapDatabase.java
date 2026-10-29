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

/** Database SQLite untuk Menatap (100% Pure Native Java). */
public class MenatapDatabase extends SQLiteOpenHelper {
    private static final String TAG = "ShisenDatabase";
    private static final String DATABASE_NAME = "menatap.db";
    private static final int DATABASE_VERSION = 3; // Dinaikkan untuk tabel watch_progress (paritas Tatap)

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
                "created_at INTEGER, " +
                "anilist_id INTEGER DEFAULT 0, " +   // BARU: ID AniList
                "mal_id INTEGER DEFAULT 0)");        // BARU: MAL ID

        db.execSQL("CREATE TABLE IF NOT EXISTS cache_kv (" +
                "key TEXT PRIMARY KEY, " +
                "value TEXT, " +
                "expires_at INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS watched_eps (" +
                "slug TEXT, " +
                "ep INTEGER, " +
                "PRIMARY KEY(slug, ep))");

        // Tabel watch_progress: simpan progres tontonan per episode (paritas backend Tatap)
        db.execSQL("CREATE TABLE IF NOT EXISTS watch_progress (" +
                "slug TEXT, " +
                "ep INTEGER, " +
                "progress_ms INTEGER, " +
                "duration_ms INTEGER, " +
                "updated_at INTEGER, " +
                "PRIMARY KEY(slug, ep))");

        // Optimal Indexes untuk lookup cepat
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_slug ON history (slug)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_slug ON bookmarks (slug)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_anilist_id ON bookmarks (anilist_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_mal_id ON bookmarks (mal_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_watched_eps_slug ON watched_eps (slug)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_watch_progress_slug ON watch_progress (slug)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            if (oldVersion < 2) {
                try {
                    db.execSQL("ALTER TABLE bookmarks ADD COLUMN anilist_id INTEGER DEFAULT 0");
                } catch (Exception e) {
                    android.util.Log.w(TAG, "Gagal menambahkan kolom anilist_id: " + e.getMessage());
                }
                try {
                    db.execSQL("ALTER TABLE bookmarks ADD COLUMN mal_id INTEGER DEFAULT 0");
                } catch (Exception e) {
                    android.util.Log.w(TAG, "Gagal menambahkan kolom mal_id: " + e.getMessage());
                }
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_anilist_id ON bookmarks (anilist_id)");
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_mal_id ON bookmarks (mal_id)");
                } catch (Exception ignored) {}
            }
            if (oldVersion < 3) {
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS watch_progress (" +
                            "slug TEXT, " +
                            "ep INTEGER, " +
                            "progress_ms INTEGER, " +
                            "duration_ms INTEGER, " +
                            "updated_at INTEGER, " +
                            "PRIMARY KEY(slug, ep))");
                } catch (Exception e) {
                    android.util.Log.w(TAG, "Gagal membuat tabel watch_progress: " + e.getMessage());
                }
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS idx_watch_progress_slug ON watch_progress (slug)");
                } catch (Exception e) {
                    android.util.Log.w(TAG, "Gagal membuat index idx_watch_progress_slug: " + e.getMessage());
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Safe downgrade: Pertahankan data pengguna (bookmarks/history/watch_progress)
        android.util.Log.w(TAG, "Permintaan downgrade database dari v" + oldVersion + " ke v" + newVersion + ". Mempertahankan tabel data pengguna.");
        try {
            onCreate(db);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error pada onDowngrade aman: " + e.getMessage());
        }
    }

    // --- History & Watch Progress Methods ---

    public static class PlaybackProgress {
        public final long progressMs;
        public final long durationMs;

        public PlaybackProgress(long progressMs, long durationMs) {
            this.progressMs = progressMs;
            this.durationMs = durationMs;
        }

        public boolean shouldResume() {
            // Paritas Tatap: minimal 15 detik, dan belum mendekati 30 detik sebelum video selesai
            boolean minThreshold = progressMs >= 15_000;
            boolean notNearEnd = durationMs <= 30_000 || progressMs < (durationMs - 30_000);
            return minThreshold && notNearEnd;
        }

        public String getFormattedTime() {
            long totalSec = Math.max(0, progressMs / 1000);
            long m = totalSec / 60;
            long s = totalSec % 60;
            return String.format(java.util.Locale.US, "%02d:%02d", m, s);
        }
    }

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

        saveWatchProgress(slug, ep, progressMs, durationMs);
        markEpisodeWatched(slug, ep);
    }

    public void saveWatchProgress(String slug, int ep, long progressMs, long durationMs) {
        if (slug == null || slug.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("slug", slug);
        cv.put("ep", ep);
        cv.put("progress_ms", progressMs);
        cv.put("duration_ms", durationMs);
        cv.put("updated_at", System.currentTimeMillis());
        db.insertWithOnConflict("watch_progress", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public PlaybackProgress getEpisodeProgress(String slug, int ep) {
        if (slug == null || slug.isEmpty()) return new PlaybackProgress(0, 0);
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT progress_ms, duration_ms FROM watch_progress WHERE slug = ? AND ep = ?",
                new String[]{slug, String.valueOf(ep)});
        if (c != null) {
            if (c.moveToFirst()) {
                long prog = c.getLong(0);
                long dur = c.getLong(1);
                c.close();
                return new PlaybackProgress(prog, dur);
            }
            c.close();
        }

        // Fallback ke history jika slug dan last_ep cocok
        Cursor cHist = db.rawQuery("SELECT progress_ms, duration_ms FROM history WHERE slug = ? AND last_ep = ?",
                new String[]{slug, String.valueOf(ep)});
        if (cHist != null) {
            if (cHist.moveToFirst()) {
                long prog = cHist.getLong(0);
                long dur = cHist.getLong(1);
                cHist.close();
                return new PlaybackProgress(prog, dur);
            }
            cHist.close();
        }

        return new PlaybackProgress(0, 0);
    }

    public void clearEpisodeProgress(String slug, int ep) {
        if (slug == null || slug.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        db.delete("watch_progress", "slug = ? AND ep = ?", new String[]{slug, String.valueOf(ep)});
        ContentValues cv = new ContentValues();
        cv.put("progress_ms", 0);
        db.update("history", cv, "slug = ? AND last_ep = ?", new String[]{slug, String.valueOf(ep)});
    }

    public void clearAllHistory() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("history", null, null);
        db.delete("watch_progress", null, null);
    }

    public void markEpisodeWatched(String slug, int ep) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("slug", slug);
        cv.put("ep", ep);
        db.insertWithOnConflict("watched_eps", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
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
                a.epsCounter = "Ep " + c.getInt(3);
                list.add(a);
            }
            c.close();
        }
        return list;
    }

    // --- Watched Episode Methods ---

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
        if (anime.anilistId > 0) cv.put("anilist_id", anime.anilistId);
        if (anime.idMal > 0) cv.put("mal_id", anime.idMal);
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

    public String getCacheStale(String key) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT value FROM cache_kv WHERE key = ?", new String[]{key});
        if (c != null && c.moveToFirst()) {
            String val = c.getString(0);
            c.close();
            return val;
        }
        if (c != null) c.close();
        return null;
    }

    public void cleanupOldCache(long cutoffMs) {
        try {
            getWritableDatabase().delete("cache_kv", "expires_at < ?", new String[]{String.valueOf(cutoffMs)});
        } catch (Exception ignored) {}
    }

    public void clearCacheTable() {
        try {
            getWritableDatabase().delete("cache_kv", null, null);
        } catch (Exception ignored) {}
    }

    // --- AniList Mapping Methods ---

    /** Simpan mapping slug -> anilist_id dan mal_id (saat pertama berhasil dari AniList) */
    public void saveAniListMapping(String slug, int anilistId, int malId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("anilist_id", anilistId);
        cv.put("mal_id", malId);
        // Update jika sudah ada, atau insert jika belum
        int rows = db.update("bookmarks", cv, "slug = ?", new String[]{slug});
        if (rows == 0) {
            // Insert baru karena slug belum ada
            ContentValues insertCv = new ContentValues();
            insertCv.put("slug", slug);
            insertCv.put("title", ""); // placeholder title
            insertCv.put("poster", "");
            insertCv.put("score", 0.0);
            insertCv.put("format", "TV");
            insertCv.put("eps_counter", "-");
            insertCv.put("created_at", System.currentTimeMillis());
            insertCv.put("anilist_id", anilistId);
            insertCv.put("mal_id", malId);
            db.insertWithOnConflict("bookmarks", null, insertCv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    /** Dapatkan anilist_id dari slug */
    public int getAniListId(String slug) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT anilist_id FROM bookmarks WHERE slug = ?", new String[]{slug});
        int id = 0;
        if (c != null) {
            if (c.moveToFirst()) {
                id = c.getInt(0);
            }
            c.close();
        }
        return id;
    }

    /** Dapatkan mal_id dari slug */
    public int getMalId(String slug) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT mal_id FROM bookmarks WHERE slug = ?", new String[]{slug});
        int id = 0;
        if (c != null) {
            if (c.moveToFirst()) {
                id = c.getInt(0);
            }
            c.close();
        }
        return id;
    }

    /** Cek apakah ada mapping AniList untuk slug */
    public boolean hasAniListMapping(String slug) {
        return getAniListId(slug) > 0;
    }
}
