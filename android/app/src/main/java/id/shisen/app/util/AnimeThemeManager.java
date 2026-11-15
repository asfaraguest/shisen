package id.shisen.app.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import id.shisen.app.R;

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
        return getPrimaryAccentColorForTheme(getTheme(context));
    }

    public static int getPrimaryAccentColorForTheme(String theme) {
        if (theme == null) theme = THEME_SHINKAI;
        switch (theme) {
            case THEME_SAKURA:
                return 0xFFFB7185;
            case THEME_CYBER:
                return 0xFF00F0FF;
            case THEME_GHIBLI:
                return 0xFF10B981;
            case THEME_SHINKAI:
            default:
                return 0xFF38BDF8;
        }
    }

    public static int getSecondaryAccentColor(Context context) {
        return getSecondaryAccentColorForTheme(getTheme(context));
    }

    public static int getSecondaryAccentColorForTheme(String theme) {
        if (theme == null) theme = THEME_SHINKAI;
        switch (theme) {
            case THEME_SAKURA:
                return 0xFFFDA4AF;
            case THEME_CYBER:
                return 0xFFA855F7;
            case THEME_GHIBLI:
                return 0xFF34D399;
            case THEME_SHINKAI:
            default:
                return 0xFF818CF8;
        }
    }

    public static int getSurfaceColor(Context context) {
        return getSurfaceColorForTheme(getTheme(context));
    }

    public static int getSurfaceColorForTheme(String theme) {
        if (theme == null) theme = THEME_SHINKAI;
        switch (theme) {
            case THEME_SAKURA:
                return 0xFF1C1226;
            case THEME_CYBER:
                return 0xFF0E1424;
            case THEME_GHIBLI:
                return 0xFF0E211E;
            case THEME_SHINKAI:
            default:
                return 0xFF10162A;
        }
    }

    public static int getBorderColor(Context context) {
        return getBorderColorForTheme(getTheme(context));
    }

    public static int getBorderColorForTheme(String theme) {
        if (theme == null) theme = THEME_SHINKAI;
        switch (theme) {
            case THEME_SAKURA:
                return 0xFF381D47;
            case THEME_CYBER:
                return 0xFF1E2C4A;
            case THEME_GHIBLI:
                return 0xFF1B3D37;
            case THEME_SHINKAI:
            default:
                return 0xFF233252;
        }
    }

    public static int getVoidColor(Context context) {
        return getVoidColorForTheme(getTheme(context));
    }

    public static int getElevatedColor(Context context) {
        return blendArgb(getSurfaceColor(context), getBorderColor(context), 0.45f);
    }

    public static int getVoidColorForTheme(String theme) {
        if (theme == null) theme = THEME_SHINKAI;
        switch (theme) {
            case THEME_SAKURA:
                return 0xFF0F0815;
            case THEME_CYBER:
                return 0xFF060810;
            case THEME_GHIBLI:
                return 0xFF06100E;
            case THEME_SHINKAI:
            default:
                return 0xFF0A0D18;
        }
    }

    public static int withAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | ((clamped & 0xFF) << 24);
    }

    /** Blending ARGB murni Java agar aman dipanggil di JVM unit test tanpa mock Color. */
    public static int blendArgb(int color1, int color2, float ratio) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        float inv = 1f - clamped;
        int a = Math.round(((color1 >>> 24) & 0xFF) * inv + ((color2 >>> 24) & 0xFF) * clamped);
        int r = Math.round(((color1 >>> 16) & 0xFF) * inv + ((color2 >>> 16) & 0xFF) * clamped);
        int g = Math.round(((color1 >>> 8) & 0xFF) * inv + ((color2 >>> 8) & 0xFF) * clamped);
        int b = Math.round((color1 & 0xFF) * inv + (color2 & 0xFF) * clamped);
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int darkenColor(int color, float amount) {
        return blendArgb(color, 0xFF000000, amount);
    }

    public static int lightenColor(int color, float amount) {
        return blendArgb(color, 0xFFFFFFFF, amount);
    }

    /** Gaya 2.5D Anime aktif khusus Shisen Mobile; TV tetap memakai UI solid 10-foot original. */
    public static boolean isMobile25DEnabled() {
        return !id.shisen.app.ui.player.PlayerKeys.isTv();
    }

    private static float density(Context context) {
        return context != null ? Math.max(1f, context.getResources().getDisplayMetrics().density) : 2f;
    }

    /** Latar kanvas cel-gradient berlapis khusus Mobile agar panel 2.5D menonjol tajam. */
    public static Drawable create25DCanvasBackgroundDrawable(Context context) {
        int voidColor = getVoidColor(context);
        int surface = getSurfaceColor(context);
        int accent = getPrimaryAccentColor(context);
        int secondary = getSecondaryAccentColor(context);

        GradientDrawable base = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{blendArgb(voidColor, surface, 0.55f), voidColor, darkenColor(voidColor, 0.28f)}
        );
        GradientDrawable celSweep = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{withAlpha(accent, 0x26), 0x00000000, withAlpha(secondary, 0x1A)}
        );
        return new LayerDrawable(new Drawable[]{base, celSweep});
    }

    /** Membangun satu state layer 2.5D (lip bawah tebal + muka cel-gradient + kilau tepi atas). */
    private static LayerDrawable build25DLayer(float d, float radiusDp, int topFill, int bottomFill,
                                               int lipColor, int strokeColor, float strokeDp,
                                               float depthDp, float topShiftDp, float bottomShiftDp) {
        float r = radiusDp * d;
        int strokePx = Math.max(1, Math.round(strokeDp * d));
        int topInset = Math.max(0, Math.round(topShiftDp * d));
        int bottomInset = Math.max(0, Math.round(bottomShiftDp * d));

        GradientDrawable lip = new GradientDrawable();
        lip.setShape(GradientDrawable.RECTANGLE);
        lip.setCornerRadius(r);
        lip.setColor(lipColor);

        GradientDrawable face = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topFill, bottomFill}
        );
        face.setCornerRadius(r);
        face.setStroke(strokePx, strokeColor);

        GradientDrawable sheen = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x38FFFFFF, 0x00FFFFFF}
        );
        sheen.setCornerRadius(Math.max(1f, r - strokePx));

        LayerDrawable ld = new LayerDrawable(new Drawable[]{lip, face, sheen});
        ld.setLayerInset(0, 0, Math.max(1, Math.round(depthDp * 0.35f * d)), 0, 0);
        ld.setLayerInset(1, 0, topInset, 0, bottomInset);
        ld.setLayerInset(2, strokePx, topInset + strokePx, strokePx, bottomInset + Math.max(strokePx, Math.round(6f * d)));
        return ld;
    }

    /** Panel 2.5D timbul dengan bibir bawah tebal & outline crisp (fallback flat di TV). */
    public static Drawable create25DPanelDrawable(Context context, int fillColor, int lipColor,
                                                  int strokeColor, float strokeDp, float radiusDp, float depthDp) {
        if (!isMobile25DEnabled()) {
            return createPillDrawable(context, fillColor, strokeColor, strokeDp, radiusDp);
        }
        float d = density(context);
        return build25DLayer(d, radiusDp, lightenColor(fillColor, 0.09f), fillColor,
                lipColor, strokeColor, strokeDp, depthDp, 0f, depthDp);
    }

    /** Tombol/keycap 2.5D dengan efek tekan fisik (muka turun menimpa bibir bawah saat disentuh). */
    public static Drawable create25DButtonDrawable(Context context, boolean isPrimaryAccent,
                                                   boolean isActiveState, float radiusDp, float depthDp) {
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        int secondary = getSecondaryAccentColor(context);
        int surface = getSurfaceColor(context);
        int border = getBorderColor(context);
        int elevated = getElevatedColor(context);

        int topFill;
        int bottomFill;
        int lipColor;
        int strokeColor;

        if (isPrimaryAccent) {
            topFill = lightenColor(accent, 0.16f);
            bottomFill = blendArgb(accent, secondary, 0.22f);
            lipColor = darkenColor(accent, 0.48f);
            strokeColor = lightenColor(accent, 0.32f);
        } else if (isActiveState) {
            topFill = blendArgb(elevated, accent, 0.34f);
            bottomFill = blendArgb(surface, accent, 0.20f);
            lipColor = darkenColor(accent, 0.44f);
            strokeColor = accent;
        } else {
            topFill = lightenColor(elevated, 0.08f);
            bottomFill = surface;
            lipColor = darkenColor(border, 0.42f);
            strokeColor = blendArgb(border, accent, 0.28f);
        }

        StateListDrawable states = new StateListDrawable();
        float sinkTop = Math.max(1f, depthDp - 1f);

        LayerDrawable pressed = build25DLayer(
                d, radiusDp,
                isPrimaryAccent ? accent : blendArgb(topFill, accent, 0.18f),
                bottomFill,
                lipColor,
                isPrimaryAccent ? Color.WHITE : accent,
                1.6f, depthDp, sinkTop, 1f
        );
        LayerDrawable focused = build25DLayer(
                d, radiusDp, topFill, bottomFill, lipColor,
                isPrimaryAccent ? Color.WHITE : accent,
                2.0f, depthDp, 0f, depthDp
        );
        LayerDrawable normal = build25DLayer(
                d, radiusDp, topFill, bottomFill, lipColor, strokeColor,
                1.4f, depthDp, 0f, depthDp
        );

        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{}, normal);
        return wrapRipple(states, withAlpha(accent, 0x38), radiusDp * d);
    }

    /** Slot input cekung 2.5D (dinding dalam gelap + sorot aksen saat fokus). */
    public static Drawable create25DRecessedInputDrawable(Context context, float radiusDp) {
        float d = density(context);
        float r = radiusDp * d;
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);
        int border = getBorderColor(context);
        int voidColor = getVoidColor(context);
        int strokeNormal = Math.max(1, Math.round(1.2f * d));
        int strokeFocus = Math.max(2, Math.round(1.8f * d));

        StateListDrawable sld = new StateListDrawable();
        for (int s = 0; s < 2; s++) {
            boolean isFocus = (s == 0);
            GradientDrawable rim = new GradientDrawable();
            rim.setShape(GradientDrawable.RECTANGLE);
            rim.setCornerRadius(r);
            rim.setColor(darkenColor(voidColor, 0.45f));
            rim.setStroke(isFocus ? strokeFocus : strokeNormal, isFocus ? accent : blendArgb(border, accent, 0.22f));

            GradientDrawable cavity = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{darkenColor(voidColor, 0.15f), blendArgb(voidColor, surface, 0.65f)}
            );
            cavity.setCornerRadius(Math.max(1f, r - 1.5f * d));

            LayerDrawable ld = new LayerDrawable(new Drawable[]{rim, cavity});
            int pad = Math.round(1.5f * d);
            ld.setLayerInset(1, pad, Math.round(3.2f * d), pad, pad);
            if (isFocus) {
                sld.addState(new int[]{android.R.attr.state_focused}, ld);
            } else {
                sld.addState(new int[]{}, ld);
            }
        }
        return sld;
    }

    /** Badge/tag anime 2.5D dengan outline tegas dan lip 1.5dp. */
    public static Drawable create25DBadgeDrawable(Context context, int badgeAccent) {
        int voidColor = getVoidColor(context);
        int accent = badgeAccent != 0 ? badgeAccent : getPrimaryAccentColor(context);
        int border = getBorderColor(context);
        return create25DPanelDrawable(
                context,
                blendArgb(voidColor, getSurfaceColor(context), 0.72f),
                darkenColor(badgeAccent != 0 ? badgeAccent : border, 0.48f),
                badgeAccent != 0 ? lightenColor(badgeAccent, 0.2f) : blendArgb(border, accent, 0.45f),
                1.1f,
                6f,
                1.8f
        );
    }

    /** Latar bodi bawah kartu katalog 2.5D (pembatas tipis + gradien cel + bibir bawah 3.5dp). */
    public static Drawable create25DCardBodyDrawable(Context context) {
        float d = density(context);
        int surface = getSurfaceColor(context);
        int border = getBorderColor(context);
        int accent = getPrimaryAccentColor(context);

        GradientDrawable bottomLip = new GradientDrawable();
        bottomLip.setColor(darkenColor(border, 0.35f));

        GradientDrawable bodyFace = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{lightenColor(surface, 0.05f), surface}
        );

        GradientDrawable topLine = new GradientDrawable();
        topLine.setColor(withAlpha(blendArgb(border, accent, 0.35f), 0xB3));

        int lipPx = Math.max(3, Math.round(3.5f * d));
        int linePx = Math.max(1, Math.round(1f * d));
        LayerDrawable ld = new LayerDrawable(new Drawable[]{bottomLip, topLine, bodyFace});
        ld.setLayerInset(1, 0, 0, 0, lipPx);
        ld.setLayerInset(2, 0, linePx, 0, lipPx);
        return ld;
    }

    /** Keycap episode 2.5D dengan status aktif, watched, dan efek tekan fisik. */
    public static Drawable create25DEpisodeTileDrawable(Context context, boolean isCurrent, boolean isWatched) {
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);
        int border = getBorderColor(context);
        int mint = ContextCompat.getColor(context, R.color.accent_mint);

        int fill = isCurrent
                ? blendArgb(surface, accent, 0.28f)
                : (isWatched ? blendArgb(surface, mint, 0.14f) : lightenColor(surface, 0.04f));
        int lip = isCurrent
                ? darkenColor(accent, 0.42f)
                : (isWatched ? darkenColor(mint, 0.55f) : darkenColor(border, 0.38f));
        int stroke = isCurrent
                ? accent
                : (isWatched ? withAlpha(mint, 0x88) : blendArgb(border, accent, 0.25f));

        float depth = isCurrent ? 2.0f : 3.2f;
        float topRest = isCurrent ? 1.2f : 0f;
        float bottomRest = isCurrent ? 2.0f : 3.2f;

        StateListDrawable states = new StateListDrawable();
        states.addState(
                new int[]{android.R.attr.state_pressed},
                build25DLayer(d, 10f, blendArgb(fill, accent, 0.2f), fill, lip, accent, 1.6f, depth, 2.2f, 1.0f)
        );
        states.addState(
                new int[]{android.R.attr.state_focused},
                build25DLayer(d, 10f, lightenColor(fill, 0.1f), fill, lip, accent, 2.2f, depth, topRest, bottomRest)
        );
        states.addState(
                new int[]{},
                build25DLayer(d, 10f, lightenColor(fill, 0.07f), fill, lip, stroke, isCurrent ? 1.6f : 1.2f, depth, topRest, bottomRest)
        );
        return wrapRipple(states, withAlpha(accent, 0x33), 10f * d);
    }

    /** Menerapkan outline crisp & shadow tint bertema pada MaterialCardView katalog Mobile. */
    public static void styleMobile25DCard(com.google.android.material.card.MaterialCardView card, Context context, boolean highlightAccent) {
        if (card == null || context == null) return;
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);
        int border = getBorderColor(context);
        float d = density(context);

        card.setCardBackgroundColor(surface);
        if (!card.isFocused()) {
            card.setStrokeColor(highlightAccent ? blendArgb(accent, border, 0.25f) : blendArgb(border, accent, 0.34f));
            card.setStrokeWidth(Math.max(2, Math.round((highlightAccent ? 1.8f : 1.4f) * d)));
        }
        if (isMobile25DEnabled()) {
            card.setCardElevation((highlightAccent ? 7f : 4.5f) * d);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                int shadow = darkenColor(highlightAccent ? accent : getVoidColor(context), 0.35f);
                card.setOutlineAmbientShadowColor(shadow);
                card.setOutlineSpotShadowColor(shadow);
            }
        }
    }

    public static Drawable createAuroraHeaderDrawable(Context context) {
        int accent = getPrimaryAccentColor(context);
        int secondary = getSecondaryAccentColor(context);
        if (!isMobile25DEnabled()) {
            return new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{withAlpha(accent, 0x2A), withAlpha(secondary, 0x12), 0x00000000}
            );
        }
        float d = density(context);
        int surface = getSurfaceColor(context);
        int border = getBorderColor(context);
        GradientDrawable lip = new GradientDrawable();
        lip.setColor(blendArgb(border, accent, 0.42f));
        GradientDrawable face = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{blendArgb(surface, accent, 0.20f), blendArgb(surface, secondary, 0.10f)}
        );
        LayerDrawable ld = new LayerDrawable(new Drawable[]{lip, face});
        ld.setLayerInset(1, 0, 0, 0, Math.max(2, Math.round(2.5f * d)));
        return ld;
    }

    public static Drawable createSearchCapsuleDrawable(Context context) {
        if (isMobile25DEnabled()) {
            return create25DPanelDrawable(
                    context,
                    getElevatedColor(context),
                    darkenColor(getBorderColor(context), 0.35f),
                    blendArgb(getBorderColor(context), getPrimaryAccentColor(context), 0.32f),
                    1.2f,
                    18f,
                    2.5f
            );
        }
        float d = density(context);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(18f * d);
        gd.setColor(getSurfaceColor(context));
        gd.setStroke(Math.max(1, (int) (1f * d)), getBorderColor(context));
        return gd;
    }

    public static Drawable createChipDrawable(Context context, boolean isSelected) {
        if (isMobile25DEnabled()) {
            return create25DButtonDrawable(context, isSelected, false, 16f, isSelected ? 2.2f : 2.8f);
        }
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);
        int border = getBorderColor(context);

        StateListDrawable sld = new StateListDrawable();
        GradientDrawable focused = new GradientDrawable();
        focused.setShape(GradientDrawable.RECTANGLE);
        focused.setCornerRadius(16f * d);

        GradientDrawable normal = new GradientDrawable();
        normal.setShape(GradientDrawable.RECTANGLE);
        normal.setCornerRadius(16f * d);

        if (isSelected) {
            focused.setColor(accent);
            focused.setStroke(Math.max(2, (int) (2f * d)), Color.WHITE);
            normal.setColor(accent);
        } else {
            focused.setColor(withAlpha(accent, 0x26));
            focused.setStroke(Math.max(2, (int) (1.5f * d)), accent);
            normal.setColor(surface);
            normal.setStroke(Math.max(1, (int) (1f * d)), border);
        }

        sld.addState(new int[]{android.R.attr.state_focused}, focused);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    public static Drawable createMenuPillDrawable(Context context, boolean isActive) {
        if (isMobile25DEnabled()) {
            return create25DButtonDrawable(context, false, isActive, 18f, 3.2f);
        }
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);
        int border = getBorderColor(context);

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(22f * d);
        if (isActive) {
            gd.setColor(withAlpha(surface, 0xF5));
            gd.setStroke(Math.max(2, (int) (1.8f * d)), accent);
        } else {
            gd.setColor(withAlpha(surface, 0xEB));
            gd.setStroke(Math.max(1, (int) (1f * d)), border);
        }
        return wrapRipple(gd, withAlpha(accent, 0x33), 22f * d);
    }

    public static Drawable createFabTriggerDrawable(Context context) {
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);
        if (isMobile25DEnabled()) {
            return create25DButtonDrawable(context, false, true, 16f, 3.5f);
        }

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(withAlpha(surface, 0xE6));
        gd.setStroke(Math.max(2, (int) (1.5f * d)), withAlpha(accent, 0x88));

        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(Color.WHITE);
        return new RippleDrawable(ColorStateList.valueOf(withAlpha(accent, 0x4D)), gd, mask);
    }

    public static Drawable createPaginationBarDrawable(Context context) {
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);
        if (isMobile25DEnabled()) {
            return create25DPanelDrawable(
                    context,
                    blendArgb(surface, getElevatedColor(context), 0.5f),
                    darkenColor(accent, 0.48f),
                    blendArgb(getBorderColor(context), accent, 0.45f),
                    1.5f,
                    22f,
                    3.5f
            );
        }
        float d = density(context);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(24f * d);
        gd.setColor(withAlpha(surface, 0xE6));
        gd.setStroke(Math.max(2, (int) (1.5f * d)), withAlpha(accent, 0x80));
        return gd;
    }

    public static Drawable createPageIndicatorDrawable(Context context) {
        if (isMobile25DEnabled()) {
            return create25DRecessedInputDrawable(context, 16f);
        }
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(18f * d);
        gd.setColor(withAlpha(surface, 0xE6));
        gd.setStroke(Math.max(1, (int) (1.2f * d)), withAlpha(accent, 0x66));
        return gd;
    }

    public static Drawable createPageButtonDrawable(Context context) {
        if (isMobile25DEnabled()) {
            return create25DButtonDrawable(context, false, true, 16f, 2.5f);
        }
        float d = density(context);
        int accent = getPrimaryAccentColor(context);

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(20f * d);
        gd.setColor(withAlpha(accent, 0x1A));
        gd.setStroke(Math.max(1, (int) (1f * d)), withAlpha(accent, 0x40));
        return wrapRipple(gd, withAlpha(accent, 0x4D), 20f * d);
    }

    public static Drawable createDetailHeroScrimDrawable(Context context) {
        int voidColor = getVoidColor(context);
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, withAlpha(voidColor, 0x66), withAlpha(voidColor, 0xCC), voidColor}
        );
    }

    public static Drawable createFrostedGlassButtonDrawable(Context context, boolean isPrimaryAccent, boolean isActiveState) {
        if (isMobile25DEnabled()) {
            return create25DButtonDrawable(context, isPrimaryAccent, isActiveState, 18f, 4.0f);
        }
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);
        float radius = 24f * d;

        LayerDrawable normal = buildGlassLayer(
                radius,
                isPrimaryAccent
                        ? new int[]{withAlpha(accent, 0x52), withAlpha(accent, 0x26)}
                        : (isActiveState
                        ? new int[]{withAlpha(accent, 0x38), withAlpha(surface, 0x88)}
                        : new int[]{withAlpha(surface, 0x8C), withAlpha(0xFF0A0D18, 0x73)}),
                Math.max(2, (int) (1.2f * d)),
                isPrimaryAccent ? withAlpha(accent, 0xCC) : (isActiveState ? withAlpha(accent, 0xAA) : 0x4DFFFFFF)
        );
        LayerDrawable focused = buildGlassLayer(
                radius,
                isPrimaryAccent
                        ? new int[]{withAlpha(accent, 0x80), withAlpha(accent, 0x4D)}
                        : new int[]{withAlpha(accent, 0x4D), withAlpha(surface, 0xB3)},
                Math.max(4, (int) (2.5f * d)),
                isPrimaryAccent ? Color.WHITE : accent
        );

        return wrapFocusRipple(normal, focused, withAlpha(accent, 0x40), radius);
    }

    public static Drawable createFrostedIconButtonDrawable(Context context) {
        if (isMobile25DEnabled()) {
            return create25DButtonDrawable(context, false, false, 12f, 3.0f);
        }
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);
        float radius = 12f * d;

        LayerDrawable normal = buildGlassLayer(
                radius,
                new int[]{withAlpha(surface, 0x8C), withAlpha(surface, 0x8C)},
                Math.max(1, (int) (1f * d)),
                0x40FFFFFF
        );
        LayerDrawable focused = buildGlassLayer(
                radius,
                new int[]{withAlpha(accent, 0x40), withAlpha(accent, 0x40)},
                Math.max(4, (int) (2.2f * d)),
                accent
        );

        return wrapFocusRipple(normal, focused, withAlpha(accent, 0x40), radius);
    }

    private static LayerDrawable buildGlassLayer(float radius, int[] bgColors, int strokePx, int strokeColor) {
        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TL_BR, bgColors);
        base.setCornerRadius(radius);

        GradientDrawable sheen = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x28FFFFFF, 0x06FFFFFF}
        );
        sheen.setCornerRadius(radius);
        sheen.setStroke(strokePx, strokeColor);
        return new LayerDrawable(new Drawable[]{base, sheen});
    }

    private static Drawable wrapFocusRipple(Drawable normal, Drawable focused, int rippleColor, float cornerRadiusPx) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{}, normal);
        return wrapRipple(states, rippleColor, cornerRadiusPx);
    }

    private static Drawable wrapRipple(Drawable content, int rippleColor, float cornerRadiusPx) {
        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.RECTANGLE);
        mask.setCornerRadius(cornerRadiusPx);
        mask.setColor(Color.WHITE);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    public static int getSystemBarColor(Context context) {
        return getSystemBarColorForTheme(context, getTheme(context));
    }

    public static int getSystemBarColorForTheme(Context context, String theme) {
        if (context == null) return Color.parseColor("#0A0D18");
        if (theme == null) theme = THEME_SHINKAI;
        switch (theme) {
            case THEME_SAKURA:
                return ContextCompat.getColor(context, R.color.anime_void_sakura);
            case THEME_CYBER:
                return ContextCompat.getColor(context, R.color.anime_void_cyber);
            case THEME_GHIBLI:
                return ContextCompat.getColor(context, R.color.anime_void_ghibli);
            case THEME_SHINKAI:
            default:
                return ContextCompat.getColor(context, R.color.anime_void_shinkai);
        }
    }

    public static void showThemeSelectorDialog(Activity activity, Runnable onThemeChanged) {
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_anime_theme_selector, null);
        dialog.setContentView(view);
        SystemUiHelper.setupCustomDialogBounds(dialog, activity);

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

        Runnable styleSelector = () -> {
            applyCustomDialogTheme(view, activity);
            if (!isMobile25DEnabled()) return;
            String sel = getTheme(activity);
            RadioButton[] rbs = {rbShinkai, rbSakura, rbCyber, rbGhibli};
            String[] keys = {THEME_SHINKAI, THEME_SAKURA, THEME_CYBER, THEME_GHIBLI};
            for (int i = 0; i < rbs.length; i++) {
                if (rbs[i] == null) continue;
                boolean act = keys[i].equals(sel);
                int cAccent = getPrimaryAccentColorForTheme(keys[i]);
                int cSurface = getSurfaceColorForTheme(keys[i]);
                applyBackgroundPreservingPadding(
                        rbs[i],
                        create25DPanelDrawable(
                                activity,
                                act ? blendArgb(cSurface, cAccent, 0.22f) : cSurface,
                                darkenColor(cAccent, act ? 0.35f : 0.55f),
                                act ? cAccent : blendArgb(getBorderColorForTheme(keys[i]), cAccent, 0.3f),
                                act ? 1.8f : 1.2f,
                                12f,
                                act ? 2.2f : 3.5f
                        )
                );
            }
        };
        styleSelector.run();

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
            SystemUiHelper.applySystemBarsTheme(activity, selected);
            HapticHelper.performClick(group);
            styleSelector.run();

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

    public static void applyBackgroundPreservingPadding(android.view.View view, Drawable drawable) {
        if (view == null || drawable == null) return;
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        view.setBackground(drawable);
        view.setPadding(left, top, right, bottom);
    }

    public static Drawable createPillDrawable(Context context, int fillColor, int strokeColor, float strokeDp, float radiusDp) {
        float d = density(context);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        if (strokeDp > 0 && strokeColor != Color.TRANSPARENT) {
            gd.setStroke(Math.max(1, (int) (strokeDp * d)), strokeColor);
        }
        gd.setCornerRadius(radiusDp * d);
        return gd;
    }

    public static Drawable createDialogFrostDrawable(Context context) {
        if (isMobile25DEnabled()) {
            int voidColor = getVoidColor(context);
            int surface = getSurfaceColor(context);
            int border = getBorderColor(context);
            int accent = getPrimaryAccentColor(context);
            return create25DPanelDrawable(
                    context,
                    surface,
                    darkenColor(voidColor, 0.35f),
                    blendArgb(border, accent, 0.42f),
                    1.6f,
                    16f,
                    5.0f
            );
        }
        return createPillDrawable(context, getSurfaceColor(context), getBorderColor(context), 1f, 16f);
    }

    public static Drawable createBadgeFrostDrawable(Context context) {
        if (isMobile25DEnabled()) {
            return create25DBadgeDrawable(context, 0);
        }
        return createPillDrawable(context, withAlpha(getVoidColor(context), 0xCC), getBorderColor(context), 1f, 6f);
    }

    public static Drawable createDialogItemSelectorDrawable(Context context) {
        int accent = getPrimaryAccentColor(context);
        float d = density(context);
        float r = 10f * d;
        StateListDrawable sld = new StateListDrawable();

        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(withAlpha(accent, 0x4D));
        pressed.setStroke(Math.max(1, (int) d), accent);
        pressed.setCornerRadius(r);
        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);

        int[] focusAttrs = {
                android.R.attr.state_focused,
                android.R.attr.state_selected,
                android.R.attr.state_checked,
                android.R.attr.state_activated
        };
        for (int attr : focusAttrs) {
            GradientDrawable st = new GradientDrawable();
            st.setColor(withAlpha(accent, 0x26));
            st.setStroke(Math.max(1, (int) (1.2f * d)), accent);
            st.setCornerRadius(r);
            sld.addState(new int[]{attr}, st);
        }

        GradientDrawable normal = new GradientDrawable();
        normal.setColor(Color.TRANSPARENT);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    public static Drawable createRippleSeekDrawable(Context context, boolean isLeft) {
        int accent = getPrimaryAccentColor(context);
        float d = density(context);
        float r = 120f * d;
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(withAlpha(accent, 0x26));
        gd.setStroke(Math.max(1, (int) d), withAlpha(accent, 0x40));
        gd.setCornerRadii(isLeft
                ? new float[]{0f, 0f, r, r, r, r, 0f, 0f}
                : new float[]{r, r, 0f, 0f, 0f, 0f, r, r});
        return gd;
    }

    public static Drawable createExoButtonDrawable(Context context, boolean isCenterPlay) {
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);
        int voidColor = getVoidColor(context);
        float d = density(context);
        if (isMobile25DEnabled() && isCenterPlay) {
            return create25DButtonDrawable(context, false, true, 28f, 3.0f);
        }
        StateListDrawable sld = new StateListDrawable();

        GradientDrawable focused = new GradientDrawable();
        focused.setShape(GradientDrawable.OVAL);
        focused.setColor(blendArgb(surface, accent, 0.12f));
        focused.setStroke(Math.max(2, (int) (2f * d)), accent);
        sld.addState(new int[]{android.R.attr.state_focused}, focused);

        GradientDrawable pressed = new GradientDrawable();
        pressed.setShape(GradientDrawable.OVAL);
        pressed.setColor(withAlpha(accent, 0x33));
        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);

        GradientDrawable normal = new GradientDrawable();
        normal.setShape(GradientDrawable.OVAL);
        if (isCenterPlay) {
            normal.setColor(withAlpha(voidColor, 0xB3));
            normal.setStroke(Math.max(1, (int) d), withAlpha(accent, 0x40));
        } else {
            normal.setColor(Color.TRANSPARENT);
        }
        sld.addState(new int[]{}, normal);
        return sld;
    }

    public static Drawable createTvFocusDrawable(Context context, boolean isNavRail) {
        int accent = getPrimaryAccentColor(context);
        float d = density(context);
        float r = (isNavRail ? 12f : 10f) * d;
        StateListDrawable sld = new StateListDrawable();

        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(withAlpha(accent, 0x40));
        pressed.setCornerRadius(r);
        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);

        GradientDrawable focused = new GradientDrawable();
        focused.setColor(withAlpha(accent, 0x33));
        focused.setStroke(Math.max(2, (int) ((isNavRail ? 2.5f : 2f) * d)), accent);
        focused.setCornerRadius(r);
        sld.addState(new int[]{android.R.attr.state_focused}, focused);

        if (isNavRail) {
            GradientDrawable selected = new GradientDrawable();
            selected.setColor(withAlpha(accent, 0x24));
            selected.setStroke(Math.max(1, (int) (1.5f * d)), accent);
            selected.setCornerRadius(r);
            sld.addState(new int[]{android.R.attr.state_selected}, selected);
        }

        GradientDrawable normal = new GradientDrawable();
        normal.setColor(Color.TRANSPARENT);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    /** Memetakan warna statis Shinkai pada dialog XML kustom agar selaras dengan tema aktif. */
    public static void applyCustomDialogTheme(View root, Context context) {
        if (root == null || context == null) return;
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);
        int border = getBorderColor(context);
        int elevated = getElevatedColor(context);
        int voidColor = getVoidColor(context);

        View panel = root;
        if (panel instanceof android.widget.FrameLayout && ((ViewGroup) panel).getChildCount() > 0) {
            panel = ((ViewGroup) panel).getChildAt(0);
        }
        if (panel instanceof android.widget.ScrollView && ((ViewGroup) panel).getChildCount() > 0) {
            panel = ((ViewGroup) panel).getChildAt(0);
        }
        if (panel != null && panel.getBackground() != null) {
            applyBackgroundPreservingPadding(panel, createDialogFrostDrawable(context));
        }

        remapDialogChildren(panel, context, accent, surface, border, elevated, voidColor);
    }

    private static boolean isAnyThemeAccent(int c) {
        return c == 0xFF38BDF8 || c == 0xFFFB7185 || c == 0xFF00F0FF || c == 0xFF10B981;
    }

    private static boolean isAnyThemeSurface(int c) {
        return c == 0xFF10162A || c == 0xFF1C1226 || c == 0xFF0E1424 || c == 0xFF0E211E;
    }

    private static boolean isAnyThemeBorder(int c) {
        return c == 0xFF233252 || c == 0xFF381D47 || c == 0xFF1E2C4A || c == 0xFF1B3D37 || c == 0xFF243859;
    }

    private static boolean isAnyThemeElevated(int c) {
        return c == 0xFF161F38 || c == 0xFF16233B
                || c == blendArgb(0xFF10162A, 0xFF233252, 0.45f)
                || c == blendArgb(0xFF1C1226, 0xFF381D47, 0.45f)
                || c == blendArgb(0xFF0E1424, 0xFF1E2C4A, 0.45f)
                || c == blendArgb(0xFF0E211E, 0xFF1B3D37, 0.45f);
    }

    private static void remapDialogChildren(View view, Context context, int accent, int surface, int border, int elevated, int voidColor) {
        if (view == null) return;
        int id = view.getId();
        // Jangan ubah swatch pratinjau kartu tema di dialog pengaturan/pemilih tema
        if (id == R.id.rb_theme_shinkai || id == R.id.rb_theme_sakura
                || id == R.id.rb_theme_cyber || id == R.id.rb_theme_ghibli) {
            return;
        }

        if (view instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton mb = (com.google.android.material.button.MaterialButton) view;
            ColorStateList bgTint = mb.getBackgroundTintList();
            boolean isPrimary = bgTint != null && isAnyThemeAccent(bgTint.getDefaultColor());
            boolean hasButtonSurface = bgTint != null || mb.getStrokeWidth() > 0 || mb.getText().length() > 0;
            if (isMobile25DEnabled() && hasButtonSurface) {
                mb.setBackgroundTintList(null);
                mb.setStrokeWidth(0);
                mb.setStateListAnimator(null);
                applyBackgroundPreservingPadding(mb, create25DButtonDrawable(context, isPrimary, false, 10f, 3.5f));
                if (isPrimary) {
                    mb.setTextColor(0xFF080C14);
                }
            } else {
                if (bgTint != null) {
                    int c = bgTint.getDefaultColor();
                    if (isAnyThemeAccent(c)) mb.setBackgroundTintList(ColorStateList.valueOf(accent));
                    else if (isAnyThemeElevated(c)) mb.setBackgroundTintList(ColorStateList.valueOf(elevated));
                    else if (isAnyThemeSurface(c)) mb.setBackgroundTintList(ColorStateList.valueOf(surface));
                }
                ColorStateList sc = mb.getStrokeColor();
                if (sc != null) {
                    int s = sc.getDefaultColor();
                    if (isAnyThemeBorder(s)) mb.setStrokeColor(ColorStateList.valueOf(border));
                    else if (isAnyThemeAccent(s)) mb.setStrokeColor(ColorStateList.valueOf(accent));
                }
            }
            ColorStateList iconTint = mb.getIconTint();
            if (iconTint != null && isAnyThemeAccent(iconTint.getDefaultColor())) {
                mb.setIconTint(ColorStateList.valueOf(accent));
            }
        } else if (isMobile25DEnabled() && view instanceof android.widget.EditText) {
            applyBackgroundPreservingPadding(view, create25DRecessedInputDrawable(context, 10f));
        }

        if (view instanceof androidx.appcompat.widget.SwitchCompat) {
            androidx.appcompat.widget.SwitchCompat sw = (androidx.appcompat.widget.SwitchCompat) view;
            sw.setThumbTintList(ColorStateList.valueOf(accent));
            sw.setTrackTintList(ColorStateList.valueOf(elevated));
        } else if (view instanceof TextView) {
            TextView tv = (TextView) view;
            int cur = tv.getCurrentTextColor();
            if (isAnyThemeAccent(cur)) {
                tv.setTextColor(accent);
            } else if (cur == 0xFF0A0D18 || cur == 0xFF090D16 || cur == 0xFF0F0815 || cur == 0xFF060810 || cur == 0xFF06100E) {
                tv.setTextColor(0xFF080C14);
            }
            if (tv instanceof CompoundButton) {
                ((CompoundButton) tv).setButtonTintList(ColorStateList.valueOf(accent));
            }
        } else if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            ColorStateList tint = iv.getImageTintList();
            if (tint != null && isAnyThemeAccent(tint.getDefaultColor())) {
                iv.setImageTintList(ColorStateList.valueOf(accent));
            }
        } else if (view instanceof ProgressBar) {
            ProgressBar pb = (ProgressBar) view;
            pb.setIndeterminateTintList(ColorStateList.valueOf(accent));
            pb.setProgressTintList(ColorStateList.valueOf(accent));
            pb.setProgressBackgroundTintList(ColorStateList.valueOf(border));
        } else if (view.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
            int bgColor = ((android.graphics.drawable.ColorDrawable) view.getBackground()).getColor();
            if (isAnyThemeBorder(bgColor)) {
                view.setBackgroundColor(border);
            } else if (isAnyThemeElevated(bgColor)) {
                view.setBackgroundColor(elevated);
            } else if (isAnyThemeSurface(bgColor)) {
                view.setBackgroundColor(surface);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                remapDialogChildren(vg.getChildAt(i), context, accent, surface, border, elevated, voidColor);
            }
        }
    }
}
