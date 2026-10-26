package id.shisen.app.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * SystemUiHelper - Menyelaraskan pewarnaan System UI (Status Bar & Navigation Bar)
 * secara Edge-to-Edge murni tanpa bilah hitam di atas, serta menjamin indikator sistem
 * (Jam, Baterai, Wi-Fi, Sinyal) selalu berkontras tinggi (putih/terang) di atas tema Shisen.
 */
public class SystemUiHelper {

    public static void applyEdgeToEdge(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        Window window = activity.getWindow();
        if (window == null) return;

        // Izinkan kanvas aplikasi menggambar di balik bilah status & bilah navigasi (Edge-to-Edge murni)
        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        // Bilah status sepenuhnya transparan agar header aurora / poster kanvas mengalir mulus ke pucuk layar
        window.setStatusBarColor(Color.TRANSPARENT);

        // Bilah navigasi transparan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }
        window.setNavigationBarColor(Color.TRANSPARENT);

        // Selaraskan indikator sistem di atas (jam, baterai, wifi, sinyal):
        // Selalu PUTIH / LIGHT ICONS (appearanceLightStatusBars = false)
        // agar terbaca tajam di atas header aurora dan kanvas gelap.
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
            insetsController.setAppearanceLightNavigationBars(false);
        }
    }

    public static void applySystemBarsTheme(Activity activity) {
        applyEdgeToEdge(activity);
    }

    public static void applySystemBarsTheme(Activity activity, String themeKey) {
        applyEdgeToEdge(activity);
    }
}
