package id.menatap.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class LocaleHelper {

    public static final String PREF_NAME = "tatap_player_prefs";
    public static final String KEY_LANGUAGE = "app_language";

    public static final String LANG_ID = "in"; // Bahasa Indonesia (Default)
    public static final String LANG_EN = "en"; // English
    public static final String LANG_JA = "ja"; // 日本語 (Japanese)

    public static Context onAttach(Context context) {
        String lang = getLanguage(context);
        return setLocale(context, lang);
    }

    public static String getLanguage(Context context) {
        if (context == null) return LANG_ID;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANG_ID);
    }

    public static Context setLocale(Context context, String language) {
        if (context == null) return null;
        persistLanguage(context, language);

        Locale locale;
        if (LANG_JA.equalsIgnoreCase(language)) {
            locale = Locale.JAPANESE;
        } else if (LANG_EN.equalsIgnoreCase(language)) {
            locale = Locale.ENGLISH;
        } else {
            locale = new Locale("in", "ID");
        }

        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);
            return context.createConfigurationContext(configuration);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }

    public static void persistLanguage(Context context, String language) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }
}
