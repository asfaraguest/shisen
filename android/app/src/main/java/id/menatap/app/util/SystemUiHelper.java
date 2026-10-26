package id.menatap.app.util;

import android.app.Activity;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * SystemUiHelper - Menyelaraskan pewarnaan System UI (Status Bar & Navigation Bar)
 * serta indikator sistem (Jam, Baterai, Wi-Fi, Sinyal) agar selaras dengan tema aplikasi Shisen.
 */
public class SystemUiHelper {

    public static void applySystemBarsTheme(Activity activity) {
        if (activity == null) return;
        applySystemBarsTheme(activity, AnimeThemeManager.getTheme(activity));
    }

    public static void applySystemBarsTheme(Activity activity, String themeKey) {
        if (activity == null || activity.isFinishing()) return;

        Window window = activity.getWindow();
        if (window == null) return;

        int barColor = AnimeThemeManager.getSystemBarColorForTheme(activity, themeKey);

        // Pastikan window menggambar latar belakang bilah sistem
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        // Setel warna status bar dan navigation bar sesuai tema aktif
        window.setStatusBarColor(barColor);
        window.setNavigationBarColor(barColor);

        // Selaraskan indikator sistem di atas (jam, baterai, wifi, sinyal):
        // Selalu PUTIH / LIGHT ICONS (appearanceLightStatusBars = false)
        // agar terbaca tajam dan tidak gelap di atas latar belakang aplikasi bernuansa gelap.
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
            insetsController.setAppearanceLightNavigationBars(false);
        }
    }
}
