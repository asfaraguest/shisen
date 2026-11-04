package id.shisen.app.ui.player;

import android.view.KeyEvent;

/**
 * Kontrol tombol keyboard/remote untuk varian Mobile (no-op, mengandalkan gesture sentuh).
 */
public final class PlayerKeys {

    public static boolean isTv() {
        return false;
    }

    public static boolean handle(PlayerActivity host, KeyEvent event) {
        return false;
    }

    public static boolean handleBack(PlayerActivity host) {
        return false;
    }
}
