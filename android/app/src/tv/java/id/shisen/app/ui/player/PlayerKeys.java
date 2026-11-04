package id.shisen.app.ui.player;

import android.view.KeyEvent;

/**
 * Kontrol remote D-Pad & media keys untuk varian Android TV di PlayerActivity.
 */
public final class PlayerKeys {

    public static boolean handle(PlayerActivity host, KeyEvent event) {
        if (host == null || event == null) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

        int code = event.getKeyCode();
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                host.togglePlayPause();
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                host.handleTvSeek(-10000);
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                host.handleTvSeek(10000);
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                host.openSubtitleMenu();
                return true;

            default:
                return false;
        }
    }
}
