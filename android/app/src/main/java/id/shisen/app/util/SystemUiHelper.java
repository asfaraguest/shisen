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
        if (context instanceof id.shisen.app.ui.player.PlayerActivity) {
            ((id.shisen.app.ui.player.PlayerActivity) context).trackDialog(dialog);
        }
        Window window = dialog.getWindow();
        if (window == null) return;

        if (isCustomViewWithBackground) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        } else {
            // Gunakan latar bertema aktif agar semua AlertDialog/dialog daftar selaras tema
            window.setBackgroundDrawable(AnimeThemeManager.createDialogFrostDrawable(context));
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        Runnable applyBounds = () -> {
            Window win = dialog.getWindow();
            if (win == null) return;
            android.util.DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int width = calculateDialogWidth(dm.widthPixels, dm.density);
            win.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

            if (context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    && !id.shisen.app.ui.player.PlayerKeys.isTv()) {
                View decor = win.getDecorView();
                if (decor != null) {
                    decor.post(() -> {
                        int maxHeight = (int) (dm.heightPixels * 0.90f);
                        if (decor.getHeight() > maxHeight) {
                            win.setLayout(width, maxHeight);
                        }
                    });
                }
            }

            if (isCustomViewWithBackground) {
                View content = win.findViewById(android.R.id.content);
                AnimeThemeManager.applyCustomDialogTheme(content, context);
            } else {
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

        android.widget.Button pos = null;
        android.widget.Button neg = null;
        android.widget.Button neu = null;
        if (dialog instanceof androidx.appcompat.app.AlertDialog) {
            androidx.appcompat.app.AlertDialog ad = (androidx.appcompat.app.AlertDialog) dialog;
            pos = ad.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            neg = ad.getButton(android.content.DialogInterface.BUTTON_NEGATIVE);
            neu = ad.getButton(android.content.DialogInterface.BUTTON_NEUTRAL);
        } else if (dialog instanceof android.app.AlertDialog) {
            android.app.AlertDialog ad = (android.app.AlertDialog) dialog;
            pos = ad.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            neg = ad.getButton(android.content.DialogInterface.BUTTON_NEGATIVE);
            neu = ad.getButton(android.content.DialogInterface.BUTTON_NEUTRAL);
        }

        int accent = AnimeThemeManager.getPrimaryAccentColor(dialog.getContext());
        boolean isTv = id.shisen.app.ui.player.PlayerKeys.isTv();
        if (pos != null) {
            pos.setTextColor(accent);
            pos.setFocusable(true);
            if (isTv) TvFocusHelper.attachFocusScale(pos, 1.05f);
        }
        if (neg != null) {
            neg.setTextColor(0xFFF1F5F9);
            neg.setFocusable(true);
            if (isTv) TvFocusHelper.attachFocusScale(neg, 1.05f);
        }
        if (neu != null) {
            neu.setTextColor(0xFF94A3B8);
            neu.setFocusable(true);
            if (isTv) TvFocusHelper.attachFocusScale(neu, 1.05f);
        }

        Window win = dialog.getWindow();
        if (win != null && win.getDecorView() != null) {
            applyContrastToHierarchy(win.getDecorView(), accent);
        }
    }

    private static void applyContrastToHierarchy(View view, int accent) {
        if (view == null) return;
        if (view instanceof android.widget.TextView) {
            android.widget.TextView tv = (android.widget.TextView) view;
            tv.setBackgroundColor(Color.TRANSPARENT);

            CharSequence text = tv.getText();
            boolean hasActiveBadge = text != null && (
                    text.toString().contains("[Aktif]") ||
                    text.toString().contains("[Active]") ||
                    text.toString().contains("[有効]") ||
                    text.toString().contains("[Activo]")
            );

            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_focused},
                    new int[]{android.R.attr.state_selected},
                    new int[]{android.R.attr.state_checked},
                    new int[]{android.R.attr.state_activated},
                    new int[]{}
            };
            int[] colors = new int[]{
                    0xFFFFFFFF, // Putih terang menyala saat terfokus
                    0xFFFFFFFF, // Putih terang menyala saat terpilih
                    accent,     // Aksen tema aktif untuk opsi yang terpilih/tercentang
                    accent,     // Aksen tema aktif untuk opsi aktif
                    hasActiveBadge ? accent : 0xFFE2E8F0
            };
            tv.setTextColor(new android.content.res.ColorStateList(states, colors));

            if (tv instanceof android.widget.CheckedTextView) {
                android.widget.CheckedTextView ctv = (android.widget.CheckedTextView) tv;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ctv.setCheckMarkTintList(android.content.res.ColorStateList.valueOf(accent));
                }
            }
        }
        if (view instanceof android.widget.ListView) {
            android.widget.ListView lv = (android.widget.ListView) view;
            try {
                lv.setSelector(AnimeThemeManager.createDialogItemSelectorDrawable(lv.getContext()));
            } catch (Throwable t) {
                lv.setSelector(new android.graphics.drawable.ColorDrawable(AnimeThemeManager.getBorderColor(lv.getContext())));
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
                    applyContrastToHierarchy(child, accent);
                }
                @Override
                public void onChildViewRemoved(View parent, View child) {}
            });
            for (int i = 0; i < lv.getChildCount(); i++) {
                applyContrastToHierarchy(lv.getChildAt(i), accent);
            }
            if (id.shisen.app.ui.player.PlayerKeys.isTv()) {
                lv.post(() -> {
                    lv.requestFocus();
                    if (lv.getAdapter() != null && lv.getAdapter().getCount() > 0) {
                        int target = lv.getCheckedItemPosition();
                        if (target < 0) {
                            for (int i = 0; i < lv.getAdapter().getCount(); i++) {
                                Object item = lv.getAdapter().getItem(i);
                                if (item != null) {
                                    String str = item.toString();
                                    if (str.contains("[Aktif]") || str.contains("[Active]") ||
                                            str.contains("[有効]") || str.contains("[Activo]")) {
                                        target = i;
                                        break;
                                    }
                                }
                            }
                        }
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
                applyContrastToHierarchy(vg.getChildAt(i), accent);
            }
        }
    }
}
