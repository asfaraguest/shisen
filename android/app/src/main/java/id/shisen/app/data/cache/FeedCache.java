package id.shisen.app.data.cache;

import android.content.Context;
import androidx.collection.LruCache;

import id.shisen.app.data.db.MenatapDatabase;
import id.shisen.app.data.model.Anime;
import id.shisen.app.data.model.Episode;
import id.shisen.app.data.service.AniListClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeedCache {

    private static volatile FeedCache instance;
    private final Context context;
    private final LruCache<String, CachePayload> memoryCache = new LruCache<>(50);
    private final LruCache<String, List<Episode>> episodesMemoryCache = new LruCache<>(50);
    private final LruCache<Integer, AniListClient.RelationsResult> relationsMemoryCache = new LruCache<>(50);
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public static class CachePayload {
        public final List<Anime> items;
        public final boolean hasNextPage;
        public final int total;
        public final long timestamp;
        public final long nextRevalidateEpochSec;

        public CachePayload(List<Anime> items, boolean hasNextPage, int total, long timestamp, long nextRevalidateEpochSec) {
            this.items = items != null ? items : new ArrayList<>();
            this.hasNextPage = hasNextPage;
            this.total = total;
            this.timestamp = timestamp;
            this.nextRevalidateEpochSec = nextRevalidateEpochSec;
        }
    }

    private FeedCache(Context context) {
        this.context = context.getApplicationContext();
    }

    public static FeedCache getInstance(Context context) {
        if (instance == null) {
            synchronized (FeedCache.class) {
                if (instance == null) {
                    instance = new FeedCache(context);
                }
            }
        }
        return instance;
    }

    /**
     * Menghitung epoch detik rilis anime terdekat di masa depan dari daftar anime.
     * Jika ada anime yang akan tayang, waktu itu dijadikan target revalidasi.
     * Jika tidak ada (semua sudah tayang / tamat), gunakan default 6 jam.
     */
    public static long calculateNextRevalidateEpoch(List<Anime> items) {
        long nowSec = System.currentTimeMillis() / 1000L;
        long earliestAiringSec = Long.MAX_VALUE;

        if (items != null) {
            for (Anime a : items) {
                if (a == null) continue;
                if (a.airingAt > nowSec) {
                    if (a.airingAt < earliestAiringSec) {
                        earliestAiringSec = a.airingAt;
                    }
                }
            }
        }

        if (earliestAiringSec != Long.MAX_VALUE) {
            return earliestAiringSec;
        }

        return nowSec + (6L * 3600L); // 6 jam
    }

    /**
     * Memeriksa apakah cache memerlukan revalidasi ke server.
     * Revalidasi diperlukan jika:
     * 1. Jadwal tayang anime terdekat telah tiba / terlewati (nowSec >= nextRevalidateEpochSec)
     * 2. Atau usia cache umum telah melewati 6 jam.
     */
    public static boolean isRevalidationNeeded(CachePayload payload) {
        if (payload == null || payload.items.isEmpty()) return true;
        long nowSec = System.currentTimeMillis() / 1000L;

        if (payload.nextRevalidateEpochSec > 0 && nowSec >= payload.nextRevalidateEpochSec) {
            return true;
        }

        long maxTtlMs = 6L * 3600L * 1000L;
        return (System.currentTimeMillis() - payload.timestamp) > maxTtlMs;
    }

    /**
     * Ambil data feed secara instan (RAM LruCache terlebih dahulu, lalu SQLite).
     * Tidak memblokir UI thread lebih dari beberapa milidetik.
     */
    public CachePayload getImmediate(String key) {
        if (key == null || key.isEmpty()) return null;

        // 1. Cek Memory LruCache (0ms)
        CachePayload mem = memoryCache.get(key);
        if (mem != null) {
            return mem;
        }

        // 2. Cek SQLite cache_kv
        try {
            String jsonStr = MenatapDatabase.getInstance(context).getCacheStale(key);
            if (jsonStr != null && !jsonStr.isEmpty()) {
                CachePayload payload = deserializePayload(jsonStr);
                if (payload != null && !payload.items.isEmpty()) {
                    memoryCache.put(key, payload);
                    return payload;
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    /**
     * Simpan feed ke RAM dan tulis ke SQLite di thread terpisah.
     */
    public void put(String key, List<Anime> items, boolean hasNextPage, int total) {
        if (key == null || key.isEmpty() || items == null) return;

        long now = System.currentTimeMillis();
        long nextAiringEpoch = calculateNextRevalidateEpoch(items);
        CachePayload payload = new CachePayload(new ArrayList<>(items), hasNextPage, total, now, nextAiringEpoch);
        memoryCache.put(key, payload);

        ioExecutor.execute(() -> {
            try {
                String jsonStr = serializePayload(payload);
                long ttlMs = 14L * 24L * 3600L * 1000L; // 14 hari simpanan di SQLite
                MenatapDatabase.getInstance(context).setCache(key, jsonStr, ttlMs);
            } catch (Throwable ignored) {}
        });
    }

    // --- DETAIL EPISODE CACHE ---

    public List<Episode> getCachedEpisodes(String slug) {
        if (slug == null || slug.isEmpty()) return null;
        List<Episode> mem = episodesMemoryCache.get(slug);
        if (mem != null) return mem;

        try {
            String jsonStr = MenatapDatabase.getInstance(context).getCacheStale("detail_eps_" + slug);
            if (jsonStr != null && !jsonStr.isEmpty()) {
                List<Episode> list = deserializeEpisodes(jsonStr);
                if (list != null && !list.isEmpty()) {
                    episodesMemoryCache.put(slug, list);
                    return list;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public void saveEpisodeList(String slug, List<Episode> episodes) {
        if (slug == null || slug.isEmpty() || episodes == null || episodes.isEmpty()) return;
        episodesMemoryCache.put(slug, new ArrayList<>(episodes));

        ioExecutor.execute(() -> {
            try {
                String jsonStr = serializeEpisodes(episodes);
                long ttlMs = 30L * 24L * 3600L * 1000L; // 30 hari di SQLite
                MenatapDatabase.getInstance(context).setCache("detail_eps_" + slug, jsonStr, ttlMs);
            } catch (Throwable ignored) {}
        });
    }

    // --- DETAIL RELATIONS CACHE ---

    public AniListClient.RelationsResult getCachedRelations(int anilistId) {
        if (anilistId <= 0) return null;
        AniListClient.RelationsResult mem = relationsMemoryCache.get(anilistId);
        if (mem != null) return mem;

        try {
            String jsonStr = MenatapDatabase.getInstance(context).getCacheStale("detail_rel_" + anilistId);
            if (jsonStr != null && !jsonStr.isEmpty()) {
                AniListClient.RelationsResult rel = deserializeRelations(jsonStr);
                if (rel != null) {
                    relationsMemoryCache.put(anilistId, rel);
                    return rel;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public void saveRelations(int anilistId, AniListClient.RelationsResult rel) {
        if (anilistId <= 0 || rel == null) return;
        relationsMemoryCache.put(anilistId, rel);

        ioExecutor.execute(() -> {
            try {
                String jsonStr = serializeRelations(rel);
                long ttlMs = 14L * 24L * 3600L * 1000L;
                MenatapDatabase.getInstance(context).setCache("detail_rel_" + anilistId, jsonStr, ttlMs);
            } catch (Throwable ignored) {}
        });
    }

    // --- CACHE HYGIENE & CLEANUP ---

    public void cleanupOldCache() {
        ioExecutor.execute(() -> {
            try {
                long cutoff = System.currentTimeMillis() - (14L * 24L * 3600L * 1000L);
                MenatapDatabase.getInstance(context).cleanupOldCache(cutoff);
            } catch (Throwable ignored) {}
        });
    }

    public void clearAllCache() {
        memoryCache.evictAll();
        episodesMemoryCache.evictAll();
        relationsMemoryCache.evictAll();

        ioExecutor.execute(() -> {
            try {
                MenatapDatabase.getInstance(context).clearCacheTable();
                deleteDirContent(new File(context.getCacheDir(), "http_cache"));
                deleteDirContent(new File(context.getCacheDir(), "sub_cache"));
                deleteDirContent(new File(context.getCacheDir(), "menatap_img_cache"));
            } catch (Throwable ignored) {}
        });
    }

    public long calculateTotalCacheSizeBytes() {
        long total = 0;
        total += getDirSize(new File(context.getCacheDir(), "http_cache"));
        total += getDirSize(new File(context.getCacheDir(), "sub_cache"));
        total += getDirSize(new File(context.getCacheDir(), "menatap_img_cache"));
        total += getDirSize(context.getDatabasePath("menatap.db"));
        return total;
    }

    private static void deleteDirContent(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirContent(f);
                f.delete();
            }
        }
    }

    private static long getDirSize(File file) {
        if (file == null || !file.exists()) return 0;
        if (!file.isDirectory()) return file.length();
        long size = 0;
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                size += getDirSize(f);
            }
        }
        return size;
    }

    // --- JSON SERIALIZATION HELPERS ---

    public static String serializePayload(CachePayload p) {
        if (p == null) return "";
        try {
            JSONObject obj = new JSONObject();
            obj.put("has_next", p.hasNextPage);
            obj.put("total", p.total);
            obj.put("timestamp", p.timestamp);
            obj.put("next_revalidate", p.nextRevalidateEpochSec);
            JSONArray arr = new JSONArray();
            for (Anime a : p.items) {
                if (a != null) arr.put(a.toJson());
            }
            obj.put("items", arr);
            return obj.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static CachePayload deserializePayload(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) return null;
        try {
            JSONObject obj = new JSONObject(jsonStr);
            boolean hasNext = obj.optBoolean("has_next", false);
            int total = obj.optInt("total", 0);
            long timestamp = obj.optLong("timestamp", System.currentTimeMillis());
            long nextRevalidate = obj.optLong("next_revalidate", 0L);
            JSONArray arr = obj.optJSONArray("items");
            List<Anime> list = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject aObj = arr.optJSONObject(i);
                    if (aObj != null) {
                        list.add(Anime.fromJson(aObj));
                    }
                }
            }
            return new CachePayload(list, hasNext, total, timestamp, nextRevalidate);
        } catch (Exception e) {
            return null;
        }
    }

    private static String serializeEpisodes(List<Episode> list) {
        try {
            JSONArray arr = new JSONArray();
            for (Episode ep : list) {
                if (ep == null) continue;
                JSONObject o = new JSONObject();
                o.put("num", ep.number);
                o.put("title", ep.title != null ? ep.title : "");
                o.put("slug", ep.slug != null ? ep.slug : "");
                o.put("ep_id", ep.epId != null ? ep.epId : "");
                o.put("provider", ep.provider != null ? ep.provider : "");
                o.put("url", ep.url != null ? ep.url : "");
                arr.put(o);
            }
            return arr.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static List<Episode> deserializeEpisodes(String jsonStr) {
        try {
            JSONArray arr = new JSONArray(jsonStr);
            List<Episode> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    Episode ep = new Episode(o.optInt("num", i + 1));
                    ep.title = o.optString("title", "Episode " + ep.number);
                    ep.slug = o.optString("slug", "");
                    ep.epId = o.optString("ep_id", "");
                    ep.provider = o.optString("provider", "");
                    ep.url = o.optString("url", "");
                    list.add(ep);
                }
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String serializeRelations(AniListClient.RelationsResult rel) {
        try {
            JSONObject root = new JSONObject();
            JSONArray sArr = new JSONArray();
            if (rel.seasons != null) {
                for (Anime a : rel.seasons) {
                    if (a != null) sArr.put(a.toJson());
                }
            }
            root.put("seasons", sArr);

            JSONArray mArr = new JSONArray();
            if (rel.movies != null) {
                for (Anime a : rel.movies) {
                    if (a != null) mArr.put(a.toJson());
                }
            }
            root.put("movies", mArr);
            return root.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static AniListClient.RelationsResult deserializeRelations(String jsonStr) {
        try {
            JSONObject root = new JSONObject(jsonStr);
            AniListClient.RelationsResult res = new AniListClient.RelationsResult();
            JSONArray sArr = root.optJSONArray("seasons");
            if (sArr != null) {
                for (int i = 0; i < sArr.length(); i++) {
                    JSONObject o = sArr.optJSONObject(i);
                    if (o != null) res.seasons.add(Anime.fromJson(o));
                }
            }
            JSONArray mArr = root.optJSONArray("movies");
            if (mArr != null) {
                for (int i = 0; i < mArr.length(); i++) {
                    JSONObject o = mArr.optJSONObject(i);
                    if (o != null) res.movies.add(Anime.fromJson(o));
                }
            }
            return res;
        } catch (Exception e) {
            return null;
        }
    }
}
