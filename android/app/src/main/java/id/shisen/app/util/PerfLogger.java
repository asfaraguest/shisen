package id.shisen.app.util;

import android.os.SystemClock;
import android.util.Log;

import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;

import id.shisen.app.BuildConfig;

/** Logger performa DEBUG-ONLY (tag SHISEN_PERF). Seluruh metode no-op di build release.
 * Metrik: cold start→reportFullyDrawn, tap episode→onRenderedFirstFrame,
 * dropped frames, durasi/kejadian rebuffering, total byte jaringan, puncak memori.
 * Tidak ada URL berparameter sensitif yang dibaca — hanya total byte dan nama host publik.
 */
public final class PerfLogger {
    private static final String TAG = "SHISEN_PERF";
    private static volatile long appStartMs = 0L;
    private static volatile long episodeTapMs = 0L;
    private static volatile boolean firstFrameLogged = false;
    private static volatile long totalNetworkBytes = 0L;
    private static volatile long peakMemoryBytes = 0L;
    private static volatile long rebufferEventCount = 0L;
    private static volatile long rebufferTotalMs = 0L;
    private static volatile long rebufferStartMs = 0L;
    private static volatile long droppedFrames = 0L;
    private static volatile boolean fullyDrawnLogged = false;

    // --- AniList request counters (DEBUG-ONLY) ---
    private static volatile int anilistRequestCount = 0;
    private static volatile int anilistRequestCacheHit = 0;
    private PerfLogger() { }

    private static boolean enabled() {
        return BuildConfig.DEBUG;
    }

    /** Dipanggil di Application.onCreate — mulai jam cold start. */
    public static void onAppStart() {
        if (enabled()) appStartMs = SystemClock.uptimeMillis();
    }

    /** Dipanggil sekali saat konten utama siap (sejalan reportFullyDrawn). */
    public static void onFullyDrawn() {
        if (!enabled() || appStartMs == 0L || fullyDrawnLogged) return;
        fullyDrawnLogged = true;
        Log.i(TAG, "cold_start_fully_drawn_ms=" + (SystemClock.uptimeMillis() - appStartMs));
    }

    /** Smooting: dipanggil saat pengguna menyentuh episode sebelum resolver dijalankan. */
    public static void markEpisodeTap() {
        if (enabled()) {
            episodeTapMs = SystemClock.uptimeMillis();
            firstFrameLogged = false;
            Log.i(TAG, "episode_tap");
        }
    }

    /** Catat request AniList (DEBUG-ONLY). */
    public static void anilistRequest() {
        if (!enabled()) return;
        anilistRequestCount++;
    }

    /** Catat cache hit AniList (DEBUG-ONLY). */
    public static void anilistCacheHit() {
        if (!enabled()) return;
        anilistRequestCacheHit++;
    }

    /** Reset counter request (DEBUG). */
    public static void resetAnilistCount() {
        if (!enabled()) return;
        anilistRequestCount = 0;
        anilistRequestCacheHit = 0;
    }

    /** Dapatkan jumlah request AniList (DEBUG). */
    public static int getAnilistRequestCount() {
        return anilistRequestCount;
    }

    /** Dapatkan jumlah cache hit AniList (DEBUG). */
    public static int getAnilistCacheHitCount() {
        return anilistRequestCacheHit;
    }

    /** Dapatkan ringkasan AniList (DEBUG). */
    public static String anilistSummary() {
        if (!enabled()) return "";
        return "anilist_requests=" + anilistRequestCount + " cache_hits=" + anilistRequestCacheHit;
    }

    /** Listener untuk player: first frame, dropped frames, rebuffering, puncak memori terpantau via sampler. */
    public static AnalyticsListener exoPlayerListener() {
        if (!enabled()) return null;
        return new AnalyticsListener() {
            @Override
            public void onRenderedFirstFrame(EventTime eventTime, Object output, long renderTimeMs) {
                if (firstFrameLogged) return;
                firstFrameLogged = true;
                long now = SystemClock.uptimeMillis();
                Log.i(TAG, "first_frame_after_tap_ms=" + (episodeTapMs > 0 ? (now - episodeTapMs) : -1)
                        + " pe=setup_to_frame=" + (appStartMs > 0 ? (now - appStartMs) : -1));
            }

            @Override
            public void onDroppedVideoFrames(EventTime eventTime, int droppedFrameCount, long elapsedCpuTimeMs) {
                droppedFrames += droppedFrameCount;
                Log.i(TAG, "dropped_frames_cumulative=" + droppedFrames);
            }

            @Override
            public void onPlaybackStateChanged(EventTime eventTime, int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    if (rebufferStartMs == 0L) {
                        rebufferStartMs = SystemClock.uptimeMillis();
                        rebufferEventCount++;
                        Log.i(TAG, "rebuffer_start count=" + rebufferEventCount);
                    }
                } else if (rebufferStartMs != 0L && playbackState == Player.STATE_READY) {
                    rebufferTotalMs += SystemClock.uptimeMillis() - rebufferStartMs;
                    rebufferStartMs = 0L;
                    Log.i(TAG, "rebuffer_end total_ms=" + rebufferTotalMs);
                }
            }

            @Override
            public void onPlayerError(EventTime eventTime, PlaybackException error) {
                Log.i(TAG, "player_error code=" + error.errorCode);
            }

            @Override
            public void onPlaybackParametersChanged(EventTime eventTime, PlaybackParameters playbackParameters) {}

            @Override
            public void onVideoSizeChanged(EventTime eventTime, VideoSize videoSize) {}
        };
    }

    /** Botol dipanggil dari interceptor network — hanya total byte disimpan,
     * tanpa URL berparameter sensitif.
     */
    public static void addNetworkBytes(long requestBytes, long responseBytes) {
        if (!enabled()) return;
        totalNetworkBytes += requestBytes + responseBytes;
    }

    public static void logPlayerSummary(ExoPlayer player) {
        if (!enabled()) return;
        long runtimeMs = SystemClock.uptimeMillis() - appStartMs;
        Log.i(TAG, "summary dropped=" + droppedFrames
                + " rebuffer_events=" + rebufferEventCount
                + " rebuffer_ms=" + rebufferTotalMs
                + " network_bytes=" + totalNetworkBytes
                + " peak_memory_kb=" + (peakMemoryBytes / 1024)
                + " session_ms=" + runtimeMs
                + " " + anilistSummary());
    }

    /** Sampel memory saat ini; interval aman akses thread. */
    public static void sampleMemory(Runtime rt) {
        if (!enabled()) return;
        long used = rt.totalMemory() - rt.freeMemory();
        if (used > peakMemoryBytes) peakMemoryBytes = used;
    }
}
