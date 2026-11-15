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
    public static final String THEME_AINCRAD = "aincrad";
    public static final String THEME_SAKURA = "sakura";
    public static final String THEME_CYBER = "cyber";
    public static final String THEME_GHIBLI = "ghibli";

    /** Aktifkan estetika SAO S1 (Aincrad Night-Clean) khusus Shisen Mobile; TV tetap original 10-foot UI. */
    public static boolean isMobileSaoEnabled() {
        return !id.shisen.app.ui.player.PlayerKeys.isTv();
    }

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
            case THEME_AINCRAD:
                return 0xFFF59E0B;
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
            case THEME_AINCRAD:
                return 0xFFFBBF24;
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
        if (isMobileSaoEnabled()) return getSaoSurfaceColor();
        if (theme == null) theme = THEME_SHINKAI;
        switch (theme) {
            case THEME_AINCRAD:
                return 0xFF191C22;
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
        if (isMobileSaoEnabled()) return getSaoHairlineColor();
        if (theme == null) theme = THEME_SHINKAI;
        switch (theme) {
            case THEME_AINCRAD:
                return 0xFF2E3440;
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
        if (isMobileSaoEnabled()) return getSaoElevatedColor();
        return blendArgb(getSurfaceColor(context), getBorderColor(context), 0.45f);
    }

    public static int getVoidColorForTheme(String theme) {
        if (isMobileSaoEnabled()) return getSaoVoidColor();
        if (theme == null) theme = THEME_SHINKAI;
        switch (theme) {
            case THEME_AINCRAD:
                return 0xFF111317;
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

    public static int getSaoVoidColor() {
        return 0xFF111317;
    }

    public static int getSaoSurfaceColor() {
        return 0xFF191C22;
    }

    public static int getSaoElevatedColor() {
        return 0xFF21252D;
    }

    public static int getSaoHairlineColor() {
        return 0xFF2E3440;
    }

    public static int withAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | ((clamped & 0xFF) << 24);
    }

    public static int blendArgb(int color1, int color2, float ratio) {
        float c = Math.max(0f, Math.min(1f, ratio));
        float inv = 1f - c;
        int a = Math.round(((color1 >>> 24) & 0xFF) * inv + ((color2 >>> 24) & 0xFF) * c);
        int r = Math.round(((color1 >>> 16) & 0xFF) * inv + ((color2 >>> 16) & 0xFF) * c);
        int g = Math.round(((color1 >>> 8) & 0xFF) * inv + ((color2 >>> 8) & 0xFF) * c);
        int b = Math.round((color1 & 0xFF) * inv + (color2 & 0xFF) * c);
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static float density(Context context) {
        return context != null ? Math.max(1f, context.getResources().getDisplayMetrics().density) : 2f;
    }

    /** Kanvas Gray Night Honeycomb bergaya SAO Mobile via BitmapShader tiling ringan. */
    public static Drawable createSaoCanvasDrawable(Context context) {
        return new SaoHoneycombDrawable(density(context));
    }

    /** Gradien scrim bawah poster agar menyatu ke latar kartu #191C22 tanpa garis navy. */
    public static Drawable createSaoPosterScrimDrawable() {
        int surf = getSaoSurfaceColor();
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, withAlpha(surf, 0x80), surf}
        );
    }

    private static final class SaoHoneycombDrawable extends Drawable {
        private static float cachedDensity = -1f;
        private static android.graphics.BitmapShader cachedShader;
        private final float d;
        private final android.graphics.Paint fillPaint = new android.graphics.Paint();

        SaoHoneycombDrawable(float density) {
            this.d = Math.max(1f, density);
        }

        private void ensureShader() {
            if (cachedShader != null && Math.abs(cachedDensity - d) < 0.01f) {
                fillPaint.setShader(cachedShader);
                return;
            }
            // Ukuran tile genap agar simpul heksagon berulang mulus tanpa celah
            int hexW = Math.max(24, Math.round(26f * d));
            if ((hexW & 1) != 0) hexW++;
            float ry = (float) (hexW / Math.sqrt(3.0));
            int tileH = Math.max(36, Math.round(3f * ry));
            if ((tileH & 1) != 0) tileH++;

            float hw = hexW / 2f;
            float segY = tileH / 3f;
            float halfH = tileH / 2f;

            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                    hexW, tileH, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(bmp);
            c.drawColor(getSaoVoidColor());

            android.graphics.Paint facetPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            facetPaint.setStyle(android.graphics.Paint.Style.FILL);
            facetPaint.setColor(0xFF13161C);

            android.graphics.Paint strokePaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            strokePaint.setStyle(android.graphics.Paint.Style.STROKE);
            strokePaint.setStrokeWidth(Math.max(1f, 0.9f * d));
            strokePaint.setColor(0xFF1A1E26);

            // Sorot halus satu faset tengah untuk kedalaman pola sarang lebah
            android.graphics.Path centerHex = buildHexPath(hw, halfH, hw, segY);
            c.drawPath(centerHex, facetPaint);

            float[][] centers = {
                    {hw, halfH},
                    {0f, 0f}, {hexW, 0f},
                    {0f, tileH}, {hexW, tileH}
            };
            for (float[] pt : centers) {
                c.drawPath(buildHexPath(pt[0], pt[1], hw, segY), strokePaint);
            }

            cachedShader = new android.graphics.BitmapShader(
                    bmp, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT);
            cachedDensity = d;
            fillPaint.setShader(cachedShader);
        }

        private static android.graphics.Path buildHexPath(float cx, float cy, float hw, float ry) {
            android.graphics.Path p = new android.graphics.Path();
            p.moveTo(cx, cy - ry);
            p.lineTo(cx + hw, cy - ry * 0.5f);
            p.lineTo(cx + hw, cy + ry * 0.5f);
            p.lineTo(cx, cy + ry);
            p.lineTo(cx - hw, cy + ry * 0.5f);
            p.lineTo(cx - hw, cy - ry * 0.5f);
            p.close();
            return p;
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            ensureShader();
            canvas.drawRect(getBounds(), fillPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            fillPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            fillPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
        }
    }

    /** Header atas menyatu mulus ke kanvas body tanpa borderline pemotong. */
    public static Drawable createAuroraHeaderDrawable(Context context) {
        if (isMobileSaoEnabled()) {
            return new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{getSaoVoidColor(), withAlpha(getSaoVoidColor(), 0xCC), 0x00111317}
            );
        }
        int accent = getPrimaryAccentColor(context);
        int secondary = getSecondaryAccentColor(context);
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{withAlpha(accent, 0x2A), withAlpha(secondary, 0x12), 0x00000000}
        );
    }

    public static Drawable createSearchCapsuleDrawable(Context context) {
        float d = density(context);
        if (isMobileSaoEnabled()) {
            return createPillDrawable(context, getSaoSurfaceColor(), withAlpha(getSaoHairlineColor(), 0xAA), 1f, 0f);
        }
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(18f * d);
        gd.setColor(getSurfaceColor(context));
        gd.setStroke(Math.max(1, (int) (1f * d)), getBorderColor(context));
        return gd;
    }

    public static Drawable createSaoRecessedInputDrawable(Context context) {
        float d = density(context);
        float r = 0f;
        int accent = getPrimaryAccentColor(context);
        int hairline = getSaoHairlineColor();
        int voidSteel = getSaoVoidColor();

        StateListDrawable sld = new StateListDrawable();
        GradientDrawable focused = new GradientDrawable();
        focused.setCornerRadius(r);
        focused.setColor(voidSteel);
        focused.setStroke(Math.max(2, Math.round(1.4f * d)), accent);
        sld.addState(new int[]{android.R.attr.state_focused}, focused);

        GradientDrawable normal = new GradientDrawable();
        normal.setCornerRadius(r);
        normal.setColor(voidSteel);
        normal.setStroke(Math.max(1, Math.round(1f * d)), hairline);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    public static Drawable createSaoCircleBadgeDrawable(Context context, int fillColor, int ringColor, float strokeDp) {
        return createSaoSquareBadgeDrawable(context, fillColor, ringColor, strokeDp);
    }

    public static Drawable createSaoSquareBadgeDrawable(Context context, int fillColor, int ringColor, float strokeDp) {
        float d = density(context);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(0f);
        gd.setColor(fillColor);
        if (strokeDp > 0f && ringColor != Color.TRANSPARENT) {
            gd.setStroke(Math.max(1, Math.round(strokeDp * d)), ringColor);
        }
        return gd;
    }

    public static Drawable createSaoEpisodeTileDrawable(Context context, boolean isCurrent, boolean isWatched) {
        float d = density(context);
        float r = 0f;
        int accent = getPrimaryAccentColor(context);
        int surface = getSaoSurfaceColor();
        int hairline = getSaoHairlineColor();
        int hpGreen = 0xFF22C55E;

        int fill = isCurrent ? blendArgb(surface, accent, 0.16f) : surface;
        int stroke = isCurrent ? accent : withAlpha(hairline, 0xAA);

        GradientDrawable base = new GradientDrawable();
        base.setCornerRadius(r);
        base.setColor(fill);
        base.setStroke(Math.max(1, Math.round((isCurrent ? 1.2f : 0.9f) * d)), stroke);

        if (isWatched && !isCurrent) {
            base.setStroke(Math.max(1, Math.round(1f * d)), withAlpha(hpGreen, 0x80));
        }
        return wrapRipple(base, withAlpha(accent, 0x30), r);
    }

    public static Drawable createChipDrawable(Context context, boolean isSelected) {
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        if (isMobileSaoEnabled()) {
            float r = 0f;
            GradientDrawable normal = new GradientDrawable();
            normal.setCornerRadius(r);
            if (isSelected) {
                normal.setColor(accent);
            } else {
                normal.setColor(getSaoElevatedColor());
                normal.setStroke(Math.max(1, Math.round(1f * d)), getSaoHairlineColor());
            }
            return wrapRipple(normal, withAlpha(accent, 0x33), r);
        }
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
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        if (isMobileSaoEnabled()) {
            float r = 0f;
            int surface = getSaoSurfaceColor();
            int elevated = getSaoElevatedColor();
            int hairline = getSaoHairlineColor();
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(r);
            if (isActive) {
                gd.setColor(blendArgb(elevated, accent, 0.14f));
                gd.setStroke(Math.max(1, Math.round(1.2f * d)), accent);
            } else {
                gd.setColor(surface);
                gd.setStroke(Math.max(1, Math.round(0.9f * d)), withAlpha(hairline, 0xB8));
            }
            return wrapRipple(gd, withAlpha(accent, 0x33), r);
        }
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
        if (isMobileSaoEnabled()) {
            Drawable rect = createPillDrawable(context, getSaoSurfaceColor(), accent, 1.5f, 0f);
            return wrapRipple(rect, withAlpha(accent, 0x40), 0f);
        }
        int surface = getSurfaceColor(context);

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
        float d = density(context);
        if (isMobileSaoEnabled()) {
            return createPillDrawable(context, getSaoSurfaceColor(), getSaoHairlineColor(), 1f, 0f);
        }
        int accent = getPrimaryAccentColor(context);
        int surface = getSurfaceColor(context);

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(24f * d);
        gd.setColor(withAlpha(surface, 0xE6));
        gd.setStroke(Math.max(2, (int) (1.5f * d)), withAlpha(accent, 0x80));
        return gd;
    }

    public static Drawable createPageIndicatorDrawable(Context context) {
        float d = density(context);
        if (isMobileSaoEnabled()) {
            return createPillDrawable(context, getSaoVoidColor(), withAlpha(getSaoHairlineColor(), 0x80), 1f, 0f);
        }
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
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        if (isMobileSaoEnabled()) {
            Drawable btn = createPillDrawable(context, getSaoElevatedColor(), getSaoHairlineColor(), 1f, 0f);
            return wrapRipple(btn, withAlpha(accent, 0x38), 0f);
        }

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(20f * d);
        gd.setColor(withAlpha(accent, 0x1A));
        gd.setStroke(Math.max(1, (int) (1f * d)), withAlpha(accent, 0x40));
        return wrapRipple(gd, withAlpha(accent, 0x4D), 20f * d);
    }

    public static Drawable createDetailHeroScrimDrawable(Context context) {
        int voidColor = isMobileSaoEnabled() ? getSaoVoidColor() : getVoidColor(context);
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, withAlpha(voidColor, 0x55), withAlpha(voidColor, 0xCC), voidColor}
        );
    }

    public static Drawable createFrostedGlassButtonDrawable(Context context, boolean isPrimaryAccent, boolean isActiveState) {
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        if (isMobileSaoEnabled()) {
            float r = 0f;
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(r);
            if (isPrimaryAccent) {
                gd.setColor(accent);
            } else if (isActiveState) {
                gd.setColor(blendArgb(getSaoSurfaceColor(), accent, 0.14f));
                gd.setStroke(Math.max(1, Math.round(1.2f * d)), accent);
            } else {
                gd.setColor(getSaoElevatedColor());
                gd.setStroke(Math.max(1, Math.round(1f * d)), getSaoHairlineColor());
            }
            return wrapRipple(gd, withAlpha(accent, 0x38), r);
        }
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

    public static Drawable createFrostedGlassButtonDrawable(Context context, boolean isPrimaryAccent) {
        return createFrostedGlassButtonDrawable(context, isPrimaryAccent, isPrimaryAccent);
    }

    public static Drawable createFrostedIconButtonDrawable(Context context) {
        float d = density(context);
        int accent = getPrimaryAccentColor(context);
        if (isMobileSaoEnabled()) {
            Drawable rect = createPillDrawable(
                    context, withAlpha(getSaoSurfaceColor(), 0xD9), getSaoHairlineColor(), 1.2f, 0f);
            return wrapRipple(rect, withAlpha(accent, 0x38), 0f);
        }
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
        if (isMobileSaoEnabled()) {
            return getSaoVoidColor();
        }
        if (context == null) return Color.parseColor("#0A0D18");
        if (theme == null) theme = THEME_SHINKAI;
        switch (theme) {
            case THEME_AINCRAD:
                return 0xFF111317;
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
        applyCustomDialogTheme(view, activity);

        RadioGroup rgThemes = view.findViewById(R.id.rg_anime_themes);
        RadioButton rbShinkai = view.findViewById(R.id.rb_theme_shinkai);
        RadioButton rbAincrad = view.findViewById(R.id.rb_theme_aincrad);
        RadioButton rbSakura = view.findViewById(R.id.rb_theme_sakura);
        RadioButton rbCyber = view.findViewById(R.id.rb_theme_cyber);
        RadioButton rbGhibli = view.findViewById(R.id.rb_theme_ghibli);
        View btnClose = view.findViewById(R.id.btn_close_theme_dialog);

        String current = getTheme(activity);
        if (THEME_AINCRAD.equals(current) && rbAincrad != null) {
            rbAincrad.setChecked(true);
        } else if (THEME_SAKURA.equals(current)) {
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
            if (checkedId == R.id.rb_theme_aincrad) {
                selected = THEME_AINCRAD;
            } else if (checkedId == R.id.rb_theme_sakura) {
                selected = THEME_SAKURA;
            } else if (checkedId == R.id.rb_theme_cyber) {
                selected = THEME_CYBER;
            } else if (checkedId == R.id.rb_theme_ghibli) {
                selected = THEME_GHIBLI;
            }

            setTheme(activity, selected);
            SystemUiHelper.applySystemBarsTheme(activity, selected);
            applyCustomDialogTheme(view, activity);
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
        if (isMobileSaoEnabled()) {
            return createPillDrawable(context, getSaoSurfaceColor(), withAlpha(getSaoHairlineColor(), 0xB3), 1f, 0f);
        }
        return createPillDrawable(context, getSurfaceColor(context), getBorderColor(context), 1f, 16f);
    }

    public static Drawable createBadgeFrostDrawable(Context context) {
        if (isMobileSaoEnabled()) {
            return createPillDrawable(context, withAlpha(getSaoVoidColor(), 0xCC), withAlpha(getSaoHairlineColor(), 0x99), 1f, 0f);
        }
        return createPillDrawable(context, withAlpha(getVoidColor(context), 0xCC), getBorderColor(context), 1f, 6f);
    }

    public static Drawable createDialogItemSelectorDrawable(Context context) {
        int accent = getPrimaryAccentColor(context);
        float d = density(context);
        float r = isMobileSaoEnabled() ? 0f : 10f * d;
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
        if (isMobileSaoEnabled()) {
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(0f);
            gd.setColor(withAlpha(accent, 0x26));
            gd.setStroke(Math.max(1, (int) d), withAlpha(accent, 0x40));
            return gd;
        }
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
        boolean sao = isMobileSaoEnabled();
        int shape = sao ? GradientDrawable.RECTANGLE : GradientDrawable.OVAL;
        StateListDrawable sld = new StateListDrawable();

        GradientDrawable focused = new GradientDrawable();
        focused.setShape(shape);
        focused.setColor(blendArgb(surface, accent, 0.12f));
        focused.setStroke(Math.max(2, (int) (2f * d)), accent);
        sld.addState(new int[]{android.R.attr.state_focused}, focused);

        GradientDrawable pressed = new GradientDrawable();
        pressed.setShape(shape);
        pressed.setColor(withAlpha(accent, 0x33));
        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);

        GradientDrawable normal = new GradientDrawable();
        normal.setShape(shape);
        if (isCenterPlay) {
            normal.setColor(withAlpha(sao ? getSaoSurfaceColor() : voidColor, sao ? 0xCC : 0xB3));
            normal.setStroke(Math.max(1, (int) d), sao ? accent : withAlpha(accent, 0x40));
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
        return c == 0xFF38BDF8 || c == 0xFFF59E0B || c == 0xFFFB7185 || c == 0xFF00F0FF || c == 0xFF10B981;
    }

    private static boolean isAnyThemeSurface(int c) {
        return c == 0xFF10162A || c == 0xFF191C22 || c == 0xFF1C1226 || c == 0xFF0E1424 || c == 0xFF0E211E;
    }

    private static boolean isAnyThemeBorder(int c) {
        return c == 0xFF233252 || c == 0xFF2E3440 || c == 0xFF381D47 || c == 0xFF1E2C4A || c == 0xFF1B3D37 || c == 0xFF243859;
    }

    private static boolean isAnyThemeElevated(int c) {
        return c == 0xFF161F38 || c == 0xFF16233B || c == 0xFF21252D
                || c == blendArgb(0xFF10162A, 0xFF233252, 0.45f)
                || c == blendArgb(0xFF1C1226, 0xFF381D47, 0.45f)
                || c == blendArgb(0xFF0E1424, 0xFF1E2C4A, 0.45f)
                || c == blendArgb(0xFF0E211E, 0xFF1B3D37, 0.45f);
    }

    private static void remapDialogChildren(View view, Context context, int accent, int surface, int border, int elevated, int voidColor) {
        if (view == null) return;
        int id = view.getId();
        // Jangan ubah swatch pratinjau kartu tema di dialog pengaturan/pemilih tema
        if (id == R.id.rb_theme_shinkai || id == R.id.rb_theme_aincrad
                || id == R.id.rb_theme_sakura || id == R.id.rb_theme_cyber
                || id == R.id.rb_theme_ghibli || id == R.id.swatch_theme_shinkai
                || id == R.id.swatch_theme_aincrad || id == R.id.swatch_theme_sakura
                || id == R.id.swatch_theme_cyber || id == R.id.swatch_theme_ghibli) {
            return;
        }

        boolean sao = isMobileSaoEnabled();

        if (view instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton mb = (com.google.android.material.button.MaterialButton) view;
            ColorStateList bgTint = mb.getBackgroundTintList();
            int prevStroke = mb.getStrokeWidth();
            boolean hasButtonSurface = bgTint != null || prevStroke > 0 || mb.getText().length() > 0;
            if (sao) {
                mb.setBackgroundTintList(null);
                mb.setStrokeWidth(0);
                mb.setCornerRadius(0);
                mb.setStateListAnimator(null);
                if (hasButtonSurface) {
                    boolean isPrimaryAccent = bgTint != null && isAnyThemeAccent(bgTint.getDefaultColor());
                    applyBackgroundPreservingPadding(mb, createFrostedGlassButtonDrawable(context, isPrimaryAccent));
                    if (isPrimaryAccent) {
                        mb.setTextColor(0xFF080C14);
                    }
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
        } else if (sao && view instanceof android.widget.EditText) {
            applyBackgroundPreservingPadding(view, createSaoRecessedInputDrawable(context));
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
                view.setBackgroundColor(sao ? withAlpha(getSaoHairlineColor(), 0x3D) : border);
            } else if (isAnyThemeElevated(bgColor)) {
                view.setBackgroundColor(sao && view instanceof TextView ? Color.TRANSPARENT : elevated);
            } else if (isAnyThemeSurface(bgColor)) {
                view.setBackgroundColor(sao ? Color.TRANSPARENT : surface);
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
