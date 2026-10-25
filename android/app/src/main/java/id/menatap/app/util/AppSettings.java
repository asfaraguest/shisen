package id.menatap.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import id.menatap.app.data.model.Anime;

public class AppSettings {

    private static final String PREFS_NAME = "menatap_app_settings";
    public static final String KEY_SAFE_MODE = "safe_mode_enabled";
    public static final String KEY_HAPTIC_ENABLED = "haptic_enabled";

    private static final Set<String> ADULT_TAGS = new HashSet<>(Arrays.asList(
            "ecchi", "erotica", "magical-sex-shift", "sexual-violence", "nudity"
    ));

    private static SharedPreferences getPrefs(Context context) {
        if (context == null) return null;
        Context appCtx = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        return appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isHapticEnabled(Context context) {
        SharedPreferences sp = getPrefs(context);
        return sp == null || sp.getBoolean(KEY_HAPTIC_ENABLED, true);
    }

    public static void setHapticEnabled(Context context, boolean enabled) {
        SharedPreferences sp = getPrefs(context);
        if (sp != null) {
            sp.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply();
        }
    }

    public static boolean isSafeMode(Context context) {
        SharedPreferences sp = getPrefs(context);
        return sp == null || sp.getBoolean(KEY_SAFE_MODE, true);
    }

    public static void setSafeMode(Context context, boolean enabled) {
        SharedPreferences sp = getPrefs(context);
        if (sp != null) {
            sp.edit().putBoolean(KEY_SAFE_MODE, enabled).apply();
        }
    }

    public static boolean isSafeQuery(Context context, String query) {
        return isSafeQuery(query, isSafeMode(context));
    }

    public static boolean isSafeQuery(String query, boolean safeMode) {
        if (query == null) return true;
        String q = query.trim().toLowerCase(Locale.US);
        // Hentai is always blocked regardless of Safe Mode
        if (q.contains("hentai")) return false;

        if (safeMode) {
            for (String tag : ADULT_TAGS) {
                if (q.contains(tag)) return false;
            }
        }
        return true;
    }

    public static boolean isSafeAnime(Context context, Anime anime) {
        return isSafeAnime(anime, isSafeMode(context));
    }

    public static boolean isSafeAnime(Anime anime, boolean safeMode) {
        if (anime == null) return false;

        // Blacklist Hentai 100% permanen
        if (anime.genres != null) {
            for (String g : anime.genres) {
                if (g != null && "hentai".equalsIgnoreCase(g.trim())) {
                    return false;
                }
            }
        }
        String t = (anime.title != null ? anime.title : "").toLowerCase(Locale.US);
        String s = (anime.slug != null ? anime.slug : "").toLowerCase(Locale.US);
        if (t.contains("hentai") || s.contains("hentai")) {
            return false;
        }

        // Jika Safe Mode nonaktif, izinkan konten ecchi / erotica
        if (!safeMode) {
            return true;
        }

        // Jika Safe Mode aktif, saring konten ecchi, erotica, dan adult tags
        if (anime.genres != null) {
            for (String g : anime.genres) {
                if (g != null && ADULT_TAGS.contains(g.trim().toLowerCase(Locale.US))) {
                    return false;
                }
            }
        }

        for (String tag : ADULT_TAGS) {
            if (t.contains(tag) || s.contains(tag)) {
                return false;
            }
        }

        return true;
    }

    public static List<Anime> filterSafe(Context context, List<Anime> list) {
        return filterSafe(list, isSafeMode(context));
    }

    public static List<Anime> filterSafe(List<Anime> list, boolean safeMode) {
        if (list == null) return new ArrayList<>();
        List<Anime> filtered = new ArrayList<>();
        for (Anime a : list) {
            if (isSafeAnime(a, safeMode)) {
                filtered.add(a);
            }
        }
        return filtered;
    }
}
