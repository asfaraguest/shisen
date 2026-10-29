package id.shisen.app.ui.player;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommands;

/**
 * PlaybackService mengelola MediaSession untuk notifikasi background media playback.
 * Berjalan sebagai MediaSessionService terisolasi tanpa memori leak atau background runaway timer.
 */
public class PlaybackService extends MediaSessionService {

    private static final String TAG = "PlaybackService";

    private static volatile MediaSession activeSession;
    private static volatile PlaybackService instance;
    private static volatile boolean keepScreenOn = false;
    private static volatile long pauseStartMs = 0L;
    private static final long PAUSE_BACKGROUND_TIMEOUT_MS = 3 * 60 * 1000L; // 3 menit

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Runnable pauseTimeoutRunnable = () -> {
        long now = SystemClock.uptimeMillis();
        if (pauseStartMs > 0 && (now - pauseStartMs) >= PAUSE_BACKGROUND_TIMEOUT_MS) {
            Log.d(TAG, "Pause >= 3 menit, menghentikan PlaybackService");
            if (instance != null) {
                instance.stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return activeSession;
    }

    /** Callback sesi yang membatasi controller yang boleh terhubung. */
    public static MediaSession.Callback createControllerCallback(Context context) {
        return new MediaSession.Callback() {
            @Override
            public MediaSession.ConnectionResult onConnect(MediaSession session,
                    MediaSession.ControllerInfo controllerInfo) {
                String ownPackage = context != null ? context.getPackageName() : null;
                String ctrlPackage = controllerInfo.getPackageName();
                boolean isOwnApp = ownPackage != null && ownPackage.equals(ctrlPackage);
                boolean isSystemUid = Process.SYSTEM_UID == controllerInfo.getUid();
                boolean isSystemUi = "com.android.systemui".equals(ctrlPackage);

                if (isOwnApp || isSystemUid || isSystemUi) {
                    return MediaSession.ConnectionResult.accept(
                            new SessionCommands.Builder().build(),
                            new Player.Commands.Builder().addAllCommands().build());
                }
                return MediaSession.ConnectionResult.reject();
            }
        };
    }

    /**
     * Setup ExoPlayer dengan wake mode NETWORK agar tidak tersendat saat audio background.
     * Panggil saat membuat player baru.
     */
    public static void setupPlayerWakeMode(ExoPlayer p) {
        if (p != null) {
            p.setWakeMode(C.WAKE_MODE_NETWORK);
            Log.d(TAG, "Wake mode set to WAKE_MODE_NETWORK");
        }
    }

    /**
     * Panggil saat memulai pemutaran: aktifkan keepScreenOn dan reset timer pause.
     */
    public static void onPlaybackStarted() {
        keepScreenOn = true;
        pauseStartMs = 0L;
        mainHandler.removeCallbacks(pauseTimeoutRunnable);
    }

    /**
     * Panggil saat pemutaran dihentikan (pause). Aktifkan timer pause terkelola.
     */
    public static void onPlaybackPaused() {
        keepScreenOn = false;
        pauseStartMs = SystemClock.uptimeMillis();
        mainHandler.removeCallbacks(pauseTimeoutRunnable);
        mainHandler.postDelayed(pauseTimeoutRunnable, PAUSE_BACKGROUND_TIMEOUT_MS);
    }

    /**
     * Panggil resume dari pause: batalkan timer jika masih dalam ambang.
     */
    public static void onPlaybackResumed() {
        pauseStartMs = 0L;
        mainHandler.removeCallbacks(pauseTimeoutRunnable);
    }

    public static void setActiveSession(MediaSession session) {
        activeSession = session;
    }

    public static MediaSession getActiveSession() {
        return activeSession;
    }

    /**
     * Setel keepScreenOn — hanya dipakai saat benar-benar memutar.
     */
    public static void setKeepScreenOn(boolean on) {
        keepScreenOn = on;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Hentikan service jika task dihapus dari recent apps dan sedang tidak memutar
        MediaSession session = activeSession;
        if (session == null || session.getPlayer() == null || !session.getPlayer().isPlaying()) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacks(pauseTimeoutRunnable);
        instance = null;
        if (activeSession != null) {
            try {
                activeSession.release();
            } catch (Throwable ignored) {}
            activeSession = null;
        }
        super.onDestroy();
    }
}
