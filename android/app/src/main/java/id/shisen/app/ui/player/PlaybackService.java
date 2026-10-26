package id.shisen.app.ui.player;

import android.content.Context;
import android.content.Intent;
import android.os.Process;

import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommands;

/**
 * PlaybackService extends MediaSessionService untuk pemutaran latar, notifikasi media,
 * dan kontrol layar kunci.
 *
 * Keamanan: seluruh koneksi controller melalui whitelist di {@link #createControllerCallback(Context)}.
 * Hanya diterima: aplikasi sendiri, controller notifikasi media (sistem),
 * dan sistem tepercaya ({@code isTrusted}) — contohnya lockscreen / Bluetooth headset routing.
 * Controller asing (aplikasi pihak ketiga biasa) ditolak.
 */
public class PlaybackService extends MediaSessionService {

    private static volatile MediaSession activeSession;

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return activeSession;
    }

    /**
     * Callback sesi yang membatasi controller yang boleh terhubung.
     * Dipakai saat MediaSession dibangun di PlayerActivity.
     *
     * Catatan versi: pada Media3 1.2.1, {@code ControllerInfo.isTrusted()} dan
     * {@code isMediaNotificationController()} belum berstatus public (publik mulai 1.3+),
     * sehingga padanannya: package aplikasi sendiri, UID sistem (SystemUI/lockscreen/AVRCP),
     * dan package com.android.systemui.
     */
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
                    // Terima dengan seluruh perintah player standar (play/pause/seek) yang
                    // dibutuhkan lockscreen/notifikasi/headset; app tidak memakai session command kustom.
                    return MediaSession.ConnectionResult.accept(
                            new SessionCommands.Builder().build(),
                            new Player.Commands.Builder().addAllCommands().build());
                }
                return MediaSession.ConnectionResult.reject();
            }
        };
    }

    public static void setActiveSession(MediaSession session) {
        activeSession = session;
    }

    public static MediaSession getActiveSession() {
        return activeSession;
    }

    @Override
    public void onDestroy() {
        activeSession = null;
        super.onDestroy();
    }
}
