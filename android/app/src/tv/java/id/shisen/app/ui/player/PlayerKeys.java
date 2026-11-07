package id.shisen.app.ui.player;

import android.view.KeyEvent;

/**
 * Kontrol remote D-Pad & media keys khusus varian Android TV di PlayerActivity.
 * - Saat overlay kontrol tersembunyi: D-Pad Kiri/Kanan langsung seek -/+10d, Tengah Play/Pause (atau Skip Intro), Atas/Bawah membuka kontrol.
 * - Saat overlay kontrol tampil: D-Pad bebas menavigasi tombol kontrol ExoPlayer (Subtitle, Settings, Next/Prev, Back).
 * - Tombol Back selalu langsung keluar ke DetailActivity (tanpa mode portrait).
 */
public final class PlayerKeys {

    public static boolean isTv() {
        return true;
    }

    public static boolean handleBack(PlayerActivity host) {
        if (host == null) return false;
        if (host.isControllerVisible()) {
            host.hideTvController();
            return true;
        }
        host.finish();
        return true;
    }

    public static boolean handle(PlayerActivity host, KeyEvent event) {
        if (host == null || event == null) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

        int code = event.getKeyCode();

        // Tombol khusus media di remote TV selalu aktif kapan saja
        switch (code) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                host.togglePlayPause();
                return true;

            case KeyEvent.KEYCODE_MEDIA_REWIND:
                host.handleTvSeek(-10000);
                return true;

            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                host.handleTvSeek(10000);
                return true;

            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
                host.playNextEpisode();
                return true;

            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:
            case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
                host.playPrevEpisode();
                return true;

            case KeyEvent.KEYCODE_CAPTIONS:
                host.openSubtitleMenu();
                return true;
        }

        // Jika controller overlay sedang tampil, biarkan D-Pad menggerakkan fokus antar tombol UI player
        if (host.isControllerVisible()) {
            return false;
        }

        // Jika controller overlay sedang tersembunyi:
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (host.performSkipIfVisible()) {
                    return true;
                }
                host.togglePlayPause();
                host.showTvController(true);
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                host.handleTvSeek(-10000);
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                host.handleTvSeek(10000);
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                host.showTvController(true);
                return true;

            default:
                return false;
        }
    }
}
