package id.shisen.app.ui.player;

import android.view.KeyEvent;

/**
 * Kontrol tombol keyboard/remote untuk varian Mobile (no-op, mengandalkan gesture sentuh).
 */
public final class PlayerKeys {
    public static boolean handle(PlayerActivity host, KeyEvent event) {
        return false;
    }
}
