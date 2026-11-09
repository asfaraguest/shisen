package id.shisen.app.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import id.shisen.app.R;

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

    public static int calculateDialogWidth(int screenWidth, float density) {
        float safeDensity = Math.max(1.0f, density);
        int maxWidth = (int) (520 * safeDensity);
        int desired = (int) (screenWidth * 0.92f);
        return Math.min(desired, maxWidth);
    }

    public static void setupDialogBounds(android.app.Dialog dialog, android.content.Context context) {
        setupDialogBounds(dialog, context, false);
    }

    public static void setupCustomDialogBounds(android.app.Dialog dialog, android.content.Context context) {
        setupDialogBounds(dialog, context, true);
    }

    public static void setupDialogBounds(android.app.Dialog dialog, android.content.Context context, boolean isCustomViewWithBackground) {
        if (dialog == null || context == null) return;
        Window window = dialog.getWindow();
        if (window == null) return;

        if (isCustomViewWithBackground) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        } else {
            // Untuk AlertDialog atau dialog standar tanpa layout kustom berlatar:
            // Pasang bg_dialog_frost agar memiliki latar belakang solid frost_surface yang elegan dan tidak transparan!
            window.setBackgroundDrawableResource(R.drawable.bg_dialog_frost);
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        Runnable applyBounds = () -> {
            Window win = dialog.getWindow();
            if (win == null) return;
            android.util.DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int width = calculateDialogWidth(dm.widthPixels, dm.density);
            win.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

            if (!isCustomViewWithBackground) {
                ensureDialogContrast(dialog);
            }
        };

        if (dialog.isShowing()) {
            applyBounds.run();
        } else {
            dialog.setOnShowListener(d -> applyBounds.run());
        }
    }

    public static void ensureDialogContrast(android.app.Dialog dialog) {
        if (dialog == null) return;

        if (dialog instanceof androidx.appcompat.app.AlertDialog) {
            androidx.appcompat.app.AlertDialog ad = (androidx.appcompat.app.AlertDialog) dialog;
            android.widget.Button pos = ad.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            if (pos != null) pos.setTextColor(0xFF38BDF8);
            android.widget.Button neg = ad.getButton(android.content.DialogInterface.BUTTON_NEGATIVE);
            if (neg != null) neg.setTextColor(0xFFF1F5F9);
            android.widget.Button neu = ad.getButton(android.content.DialogInterface.BUTTON_NEUTRAL);
            if (neu != null) neu.setTextColor(0xFF94A3B8);
        } else if (dialog instanceof android.app.AlertDialog) {
            android.app.AlertDialog ad = (android.app.AlertDialog) dialog;
            android.widget.Button pos = ad.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            if (pos != null) pos.setTextColor(0xFF38BDF8);
            android.widget.Button neg = ad.getButton(android.content.DialogInterface.BUTTON_NEGATIVE);
            if (neg != null) neg.setTextColor(0xFFF1F5F9);
            android.widget.Button neu = ad.getButton(android.content.DialogInterface.BUTTON_NEUTRAL);
            if (neu != null) neu.setTextColor(0xFF94A3B8);
        }

        Window win = dialog.getWindow();
        if (win != null && win.getDecorView() != null) {
            applyContrastToHierarchy(win.getDecorView());
        }
    }

    private static void applyContrastToHierarchy(View view) {
        if (view == null) return;
        if (view instanceof android.widget.TextView) {
            android.widget.TextView tv = (android.widget.TextView) view;
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_focused},
                    new int[]{android.R.attr.state_selected},
                    new int[]{}
            };
            int[] colors = new int[]{
                    0xFFFFFFFF, // Putih terang menyala saat terfokus
                    0xFFFFFFFF, // Putih terang menyala saat terpilih
                    0xFFCBD5E1  // Abu-abu terang kontras saat default
            };
            tv.setTextColor(new android.content.res.ColorStateList(states, colors));

            if (tv instanceof android.widget.CheckedTextView) {
                android.widget.CheckedTextView ctv = (android.widget.CheckedTextView) tv;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ctv.setCheckMarkTintList(android.content.res.ColorStateList.valueOf(0xFF38BDF8));
                }
            }
        }
        if (view instanceof android.widget.ListView) {
            android.widget.ListView lv = (android.widget.ListView) view;
            try {
                lv.setSelector(R.drawable.sel_dialog_item);
            } catch (Throwable t) {
                lv.setSelector(new android.graphics.drawable.ColorDrawable(0xFF1E3A5F));
            }
            lv.setDrawSelectorOnTop(true);

            float density = lv.getResources().getDisplayMetrics().density;
            int padH = (int) (8 * density);
            int padV = (int) (4 * density);
            lv.setPadding(padH, padV, padH, padV);
            lv.setClipToPadding(false);
            lv.setDivider(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            lv.setDividerHeight((int) (4 * density));

            lv.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    applyContrastToHierarchy(child);
                }
                @Override
                public void onChildViewRemoved(View parent, View child) {}
            });
            for (int i = 0; i < lv.getChildCount(); i++) {
                applyContrastToHierarchy(lv.getChildAt(i));
            }
            if (id.shisen.app.ui.player.PlayerKeys.isTv()) {
                lv.post(() -> {
                    lv.requestFocus();
                    if (lv.getAdapter() != null && lv.getAdapter().getCount() > 0) {
                        int target = lv.getCheckedItemPosition();
                        if (target < 0 || target >= lv.getAdapter().getCount()) {
                            target = 0;
                        }
                        lv.setSelection(target);
                    }
                });
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyContrastToHierarchy(vg.getChildAt(i));
            }
        }
    }
}
