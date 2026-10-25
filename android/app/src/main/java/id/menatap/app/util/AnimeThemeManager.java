package id.menatap.app.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import id.menatap.app.R;

public class AnimeThemeManager {

    private static final String PREF_NAME = "menatap_theme_prefs";
    private static final String KEY_CURRENT_THEME = "current_anime_theme";

    public static final String THEME_SHINKAI = "shinkai";
    public static final String THEME_SAKURA = "sakura";
    public static final String THEME_CYBER = "cyber";
    public static final String THEME_GHIBLI = "ghibli";

    public static String getTheme(Context context) {
        if (context == null) return THEME_SHINKAI;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_CURRENT_THEME, THEME_SHINKAI);
    }

    public static void setTheme(Context context, String themeKey) {
        if (context == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_CURRENT_THEME, themeKey).apply();
    }

    public static int getCanvasBackgroundResId(Context context) {
        return getCanvasBackgroundResIdForTheme(getTheme(context));
    }

    public static int getCanvasBackgroundResIdForTheme(String theme) {
        if (theme == null) theme = THEME_SHINKAI;
        switch (theme) {
            case THEME_SAKURA:
                return R.drawable.bg_anime_canvas_sakura;
            case THEME_CYBER:
                return R.drawable.bg_anime_canvas_cyber;
            case THEME_GHIBLI:
                return R.drawable.bg_anime_canvas_ghibli;
            case THEME_SHINKAI:
            default:
                return R.drawable.bg_anime_canvas_shinkai;
        }
    }

    public static int getPrimaryAccentColor(Context context) {
        String theme = getTheme(context);
        switch (theme) {
            case THEME_SAKURA:
                return ContextCompat.getColor(context, R.color.anime_sakura_pink);
            case THEME_CYBER:
                return ContextCompat.getColor(context, R.color.anime_cyan_neon);
            case THEME_GHIBLI:
                return ContextCompat.getColor(context, R.color.anime_mint_miku);
            case THEME_SHINKAI:
            default:
                return ContextCompat.getColor(context, R.color.anime_cyan_glacial);
        }
    }

    public static void showThemeSelectorDialog(Activity activity, Runnable onThemeChanged) {
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_anime_theme_selector, null);
        dialog.setContentView(view);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.92f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        RadioGroup rgThemes = view.findViewById(R.id.rg_anime_themes);
        RadioButton rbShinkai = view.findViewById(R.id.rb_theme_shinkai);
        RadioButton rbSakura = view.findViewById(R.id.rb_theme_sakura);
        RadioButton rbCyber = view.findViewById(R.id.rb_theme_cyber);
        RadioButton rbGhibli = view.findViewById(R.id.rb_theme_ghibli);
        View btnClose = view.findViewById(R.id.btn_close_theme_dialog);

        String current = getTheme(activity);
        if (THEME_SAKURA.equals(current)) {
            rbSakura.setChecked(true);
        } else if (THEME_CYBER.equals(current)) {
            rbCyber.setChecked(true);
        } else if (THEME_GHIBLI.equals(current)) {
            rbGhibli.setChecked(true);
        } else {
            rbShinkai.setChecked(true);
        }

        rgThemes.setOnCheckedChangeListener((group, checkedId) -> {
            String selected = THEME_SHINKAI;
            if (checkedId == R.id.rb_theme_sakura) {
                selected = THEME_SAKURA;
            } else if (checkedId == R.id.rb_theme_cyber) {
                selected = THEME_CYBER;
            } else if (checkedId == R.id.rb_theme_ghibli) {
                selected = THEME_GHIBLI;
            }

            setTheme(activity, selected);
            HapticHelper.performClick(group);

            if (onThemeChanged != null) {
                onThemeChanged.run();
            }
        });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                dialog.dismiss();
            });
        }

        dialog.show();
    }
}
