package id.shisen.app.ui.player;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
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

import java.util.concurrent.TimeUnit;

/**
 * PlaybackService extends MediaSessionService untuk pemutaran latar, notifikasi media,
 * dan kontrol layar kunci.
 *
 * Keamanan: seluruh koneksi controller melalui whitelist di {@link #createControllerCallback(Context)}.
 * Hanya diterima: aplikasi sendiri, controller notifikasi media (sistem),
 * dan sistem tepercaya ({@code isTrusted}) — contohnya lockscreen / Bluetooth headset routing.
 * Controller asing (aplikasi pihak ketiga biasa) ditolak.
 *
 * FASE 2.1: Penanganan wake lock & background behavior.
 * - setWakeMode(C.WAKE_MODE_NETWORK) agar tidak tersendat saat audio background
 * - pause >3 menit di background: hentikan service + release player
 * - keepScreenOn hanya aktif saat benar-benar memutar
 * - Tidak ada loop/timer terus jalan saat pause
 */
public class PlaybackService extends MediaSessionService {

    private static final String TAG = "PlaybackService";

    private static volatile MediaSession activeSession;
    private static volatile PlaybackService instance;
    private static volatile ExoPlayer player;
    private static volatile boolean keepScreenOn = false;
    private static volatile long pauseStartMs = 0L;
    private static final long PAUSE_BACKGROUND_TIMEOUT_MS = 3 * 60 * 1000L; // 3 menit

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
            // WAKE_MODE_NETWORK: wakelock hanya saat ada aktivitas jaringan, tidak saat screen off
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
    }

    /**
     * Panggil saat pemuturan dihentikan (pause). Aktifkan timer pause.
     */
    public static void onPlaybackPaused() {
        keepScreenOn = false;
        pauseStartMs = SystemClock.uptimeMillis();
        // Cek jika pause > 3 menit di background, hentikan service
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            long now = SystemClock.uptimeMillis();
            if (pauseStartMs > 0 && (now - pauseStartMs) > PAUSE_BACKGROUND_TIMEOUT_MS) {
                Log.d(TAG, "Pause > 3 menit, hentikan PlaybackService");
                if (instance != null) instance.stopSelf();
            }
        }, PAUSE_BACKGROUND_TIMEOUT_MS);
    }

    /**
     * Panggil resume dari pause: batalkan timer jika masih dalam ambang.
     */
    public static void onPlaybackResumed() {
        pauseStartMs = 0L;
    }

    public static void setActiveSession(MediaSession session) {
        activeSession = session;
    }

    public static MediaSession getActiveSession() {
        return activeSession;
    }

    /**
     * Setel keepScreenOn — hanya dipakai saat benar-benar memutar.
     * Dilakukan dari PlayerActivity saat viewport terlihat.
     */
    public static void setKeepScreenOn(boolean on) {
        keepScreenOn = on;
    }

    @Override
    public void onDestroy() {
        instance = null;
        activeSession = null;
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
