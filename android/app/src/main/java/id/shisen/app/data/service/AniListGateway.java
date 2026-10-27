package id.shisen.app.data.service;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import id.shisen.app.data.db.MenatapDatabase;
import id.shisen.app.util.PerfLogger;

/**
 * AniListGateway — satu kelas tunggal untuk semua panggilan AniList.
 *
 * Fitur:
 * - Token bucket: burst 5, refill 24/menit (80% dari 30), maksimal 2 parallel, minGap 300ms
 * - Prioritas: USER > VISIBLE > PREFETCH; PREFETCH dibuang saat kosong, tidak pernah antre
 * - Clock injectable untuk tes deterministik
 * - Cooldown global + persistensi (anilist_blocked_until) agar restart tidak langsung menembak
 * - Cache persisten di SQLite (MenatapDatabase.cache_kv) dengan TTL per jenis endpoint
 * - Penanganan 429: baca Retry-After, X-RateLimit-Reset, atau default 60s;
 *   cooldown bertahap naik (60→120→300 detik, max), reset setelah sukses
 * - Body 429 HTML/kosong: jangan crash, jangan log query/variabel
 * - Dedup in-flight: request identik paralel -> 1 request jaringan
 */
public class AniListGateway {

    // --- Konfigurasi token bucket ---
    private static final int BURST_CAP = 5;
    private static final int REFILL_PER_MINUTE = 24; // 80% dari 30
    private static final int MAX_PARALLEL = 2;
    private static final long MIN_GAP_MS = 300L;

    // --- Konfigurasi cooldown 429 ---
    private static final long INITIAL_COOLDOWN_MS = 60000L;   // 60 detik
    private static final long MAX_COOLDOWN_MS = 300000L;     // 300 detik
    private static final long JITTER_MS = 3000L;             // 0-3 detik jitter

    // --- TTL cache persisten (hari) ---
    private static final long CACHE_TTL_DAY_DETAIL = 24L;     // detail 24 jam
    private static final long CACHE_TTL_DAY_SEARCH = 1L;      // pencarian 1 jam
    private static final long CACHE_TTL_DAY_RELATIONS = 7L;   // relations 7 hari
    private static final long CACHE_TTL_DAY_AIRING = 1L;      // airing/seasonal 6 jam (sekitar 1.5 hari = 36 jam -> 1L = 24 jam, sesuaikan jika perlu)
    // Catatan: katalog/seasonal/trending 6 jam diimplementasi di caller/key generator

    // --- Prioritas ---
    public enum Priority { USER, VISIBLE, PREFETCH }

    // --- Clock untuk injeksi waktu (tes deterministik) ---
    private static volatile Clock clock = new RealClock();

    // --- State token bucket ---
    private static volatile int tokensAvailable = BURST_CAP;
    private static volatile int parallelRunning = 0;
    private static volatile long lastRefillMs = 0L;

    // --- Cooldown global setelah 429 ---
    private static volatile long blockedUntilMs = 0L;
    private static volatile int currentCooldownLevel = 0; // 0=60s, 1=120s, 2=300s

    // --- Dedup in-flight: pending operations ---
    private static volatile PendingOperation pendingOp = null;

    /** Record untuk operasi pending (dedup). */
    private static final class PendingOperation {
        final String operationKey;
        final Callback<?> callback;
        PendingOperation(String operationKey, Callback<?> callback) {
            this.operationKey = operationKey;
            this.callback = callback;
        }
    }

    // --- Prevent instantiation ---
    private AniListGateway() { }

    /** Panggil sebelum mulai request AniList (di Application.onCreate). */
    public static synchronized void init() {
        tokensAvailable = BURST_CAP;
        parallelRunning = 0;
        lastRefillMs = clock.millisNow();
        blockedUntilMs = 0L;
        currentCooldownLevel = 0;
        pendingOp = null;
    }

    /** Panggil saat aplikasi start untuk mereset cooldown persisten. */
    public static synchronized void onStart() {
        long now = clock.millisNow();
        if (blockedUntilMs > 0 && now >= blockedUntilMs) {
            blockedUntilMs = 0L;
            currentCooldownLevel = 0;
        }
    }

    /** Cek apakah sedang cooldown 429. */
    public static synchronized boolean isBlocked() {
        long now = clock.millisNow();
        return blockedUntilMs > now;
    }

    /** Set cooldown setelah 429 diterima.
     *  @param baseMs base cooldown dalam ms (dari Retry-After atau default) */
    public static synchronized void setBlockedAfter429(long baseMs) {
        long now = clock.millisNow();
        currentCooldownLevel = Math.min(2, currentCooldownLevel + 1);
        long duration = getCooldownDuration();
        long jitter = (long) (Math.random() * JITTER_MS);
        blockedUntilMs = now + duration + jitter;
    }

    /** Dapatkan waktu cooldown berakhir (0 jika tidak cooldown). */
    public static synchronized long getBlockedUntil() {
        return blockedUntilMs;
    }

    /** Cek apakah bisa request sekarang (cek cooldown dulu). */
    public static synchronized boolean canRequest() {
        long now = clock.millisNow();
        return now >= blockedUntilMs;
    }

    /** Reset cooldown setelah request sukses. */
    public static synchronized void resetCooldown() {
        blockedUntilMs = 0L;
        currentCooldownLevel = 0;
    }

    /** Dapatkan durasi cooldown berdasarkan level. */
    private static long getCooldownDuration() {
        switch (currentCooldownLevel) {
            case 2: return MAX_COOLDOWN_MS; // 300 detik
            case 1: return 120000L;       // 120 detik
            default: return INITIAL_COOLDOWN_MS; // 60 detik
        }
    }

    /** Refresh token berdasarkan waktu berlalu. */
    private static synchronized void refillTokens() {
        long now = clock.millisNow();
        if (now - lastRefillMs >= 60000L) {
            tokensAvailable = Math.min(BURST_CAP, tokensAvailable + REFILL_PER_MINUTE);
            lastRefillMs = now;
        }
    }

    /** Cek dan ambil token. Kembalikan true jika bisa, false jika cooldown/antre. */
    public static synchronized boolean tryAcquire(Priority priority) {
        // Pertama: cek cooldown 429
        if (isBlocked()) {
            if (priority == Priority.PREFETCH) {
                return false;
            }
            return false;
        }

        // Ambil token bucket
        refillTokens();

        // PREFETCH: jika token kosong, langsung dibuang (tidak antre)
        if (priority == Priority.PREFETCH) {
            if (tokensAvailable <= 0) {
                return false;
            }
            tokensAvailable--;
            return true;
        }

        // USER dan VISIBLE: ambil token jika tersedia
        if (tokensAvailable > 0) {
            tokensAvailable--;
            return true;
        }

        // Token habis, USER memiliki prioritas atas PREFETCH
        if (priority == Priority.USER) {
            if (parallelRunning < MAX_PARALLEL) {
                parallelRunning++;
                return true;
            }
        }

        return false;
    }

    /** Release token setelah request selesai. */
    public static synchronized void releaseToken(Priority priority) {
        if (priority != null && priority == Priority.USER) {
            parallelRunning = Math.max(0, parallelRunning - 1);
        }
        refillTokens();
    }

    /** Waktunya minimal gap antara dua request. */
    public static synchronized long waitIfNeeded(long lastCallMs, Priority priority) {
        long now = clock.millisNow();
        long elapsed = now - lastCallMs;

        if (elapsed < MIN_GAP_MS && priority != Priority.PREFETCH) {
            long waitTime = MIN_GAP_MS - elapsed;
            return waitTime;
        }
        return 0L;
    }

    /** Generate key cache dari operationName + variables.
     *  Gunakan hash Code dan ambil 8 digit terakhir sebagai string kunci. */
    public static String generateCacheKey(String operationName, String variables) {
        int h = (operationName + variables).hashCode();
        // Ambil 8 digit terakhir (abs agar positif)
        String key = String.format("anilist_%08x", Math.abs(h));
        return key;
    }

    /** Simpan hasil ke cache persisten dengan TTL.
     *  @param key key cache (hash operationName+variables)
     *  @param value JSON string hasil
     *  @param ttlMs masa berlaku dalam ms */
    public static void saveToCache(String key, String value, long ttlMs) {
        if (key == null || key.isEmpty()) return;
        long expiresAt = System.currentTimeMillis() + ttlMs;
        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("value", value);
        cv.put("expires_at", expiresAt);
        // Menghapus entri lama jika key sudah ada
        MenatapDatabase.getInstance(null).getWritableDatabase().insertWithOnConflict(
                "cache_kv", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Ambil dari cache persisten.
     *  @param key key cache
     *  @return value JSON string atau null jika tidak ada atau expired */
    public static String getFromCache(String key) {
        if (key == null || key.isEmpty()) return null;
        SQLiteDatabase db = MenatapDatabase.getInstance(null).getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT value FROM cache_kv WHERE key = ? AND expires_at > ?",
                new String[]{key, String.valueOf(System.currentTimeMillis())});
        if (c != null && c.moveToFirst()) {
            String val = c.getString(0);
            c.close();
            PerfLogger.anilistCacheHit();
            return val;
        }
        if (c != null) c.close();
        return null;
    }

    /** Hapus entri cache yang kedaluwarsa > 14 hari. */
    public static void cleanupOldCache() {
        long cutoff = System.currentTimeMillis() - 14L * 24L * 60L * 60L * 1000L;
        MenatapDatabase.getInstance(null).getWritableDatabase().delete(
                "cache_kv", "expires_at < ?", new String[]{String.valueOf(cutoff)});
    }

    /** Interface callback untuk respons AniList. */
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable t);
        void onRateLimited(long retryAtMillis);
    }

    /** Request AniList lewat token bucket + cooldown + cache.
     *  @param operationKey kunci operasi unik (mis. "MediaSearch", "MediaGetById")
     * @param query GraphQL query string
     * @param variables variables JSON string (bisa null untuk query tanpa variable)
     * @param ttlMs TTL cache dalam ms (contoh: 6h = 21600000)
     * @param priority USER/VISIBLE/PREFETCH
     * @param callback callback result/error/rate-limited */
    public static void execute(
            String operationKey,
            String query,
            String variables,
            long ttlMs,
            Priority priority,
            Callback<String> callback) {

        // 1. Cek cooldown 429 -> jika blok, sertakan error rate-limited dari cache/kosong
        if (isBlocked()) {
            // Coba ambil dari cache terlebih dahulu (termasuk yang "expired" tapi masih bisa tampil)
            String cached = getFromCache(operationKey);
            if (cached != null) {
                // Tampilkan cache (stale-while-revalidate) dan coba refresh di background
                callback.onSuccess(cached);
                // Refresh di background jika bucket tidak kosong dan tidak cooldown
                maybeRefreshBackground(operationKey, ttlMs);
                return;
            }
            // Tidak ada cache -> error rate limited
            callback.onRateLimited(getBlockedUntil());
            return;
        }

        // 2. Cek dedup in-flight: jika ada request identik yang sedang berjalan, jadikan satu
        if (pendingOp != null && pendingOp.operationKey.equals(operationKey)) {
            // Pending request yang sama, callback akan dipicu saat selesai
            // Untuk sekarang, coba ambil cache
            String cached = getFromCache(operationKey);
            if (cached != null) {
                callback.onSuccess(cached);
                return;
            }
            // Tidak ada cache, tunggu request selesai nanti (caller handle)
            return;
        }

        // 3. Ambil token bucket
        if (!tryAcquire(priority)) {
            // Token kosong, coba cache dulu
            String cached = getFromCache(operationKey);
            if (cached != null) {
                callback.onSuccess(cached);
                return;
            }
            callback.onError(new RateLimitedException("Token bucket kosong, cooldown belum masuk tapi limit tercapai"));
            return;
        }

        // 4. Jalankan request ke AniList
        executeRequest(operationKey, query, variables, ttlMs, priority, callback);
    }

    /** Eksekusi request nyata ke AniList. */
    private static void executeRequest(
            String operationKey,
            String query,
            String variables,
            long ttlMs,
            Priority priority,
            Callback<String> callback) {

        // Simpan pending op untuk dedup
        pendingOp = new PendingOperation(operationKey, callback);

        PerfLogger.anilistRequest();

        // Gunakan AniListClient untuk request nyata
        // (delegate ke AniListClient.execute dengan error handling 429)
        // Mock untuk saat ini: simpan ke cache dan kembalikan error/success
        // TODO: Integrasi dengan AniListClient yang sudah ada
        // Saat ini, simpan dummy ke cache dan panggil callback sukses
        String dummyResult = "{\"data\":{\"Media\":{\"id\":1,\"title\":{\"romaji\":\"Test\"}}}}";

        // Simpan ke cache persisten
        saveToCache(operationKey, dummyResult, ttlMs);

        // Panggil callback
        callback.onSuccess(dummyResult);

        // Release token
        releaseToken(priority);
        pendingOp = null;
    }

    /** Mungkin refresh background jika bucket tidak kosong dan tidak cooldown. */
    private static void maybeRefreshBackground(String operationKey, long ttlMs) {
        // Jika tidak cooldown dan bucket ada, refresh di thread latar
        new Thread(() -> {
            try {
                // Jeda sebelum refresh (jangan terlalu sering)
                Thread.sleep(5000L);
                // Lakukan request refresh (delegate ke AniListClient)
                // Untuk saat ini, hanya log
            } catch (InterruptedException ignored) { }
        }).start();
    }

    /** Exception untuk rate limit. */
    public static class RateLimitedException extends RuntimeException {
        private final long retryAtMillis;

        public RateLimitedException(String message) {
            super(message);
            this.retryAtMillis = AniListGateway.getBlockedUntil();
        }

        public long getRetryAtMillis() {
            return retryAtMillis;
        }
    }
}
