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

    public static int getSaoHoneycombFacetColor(int accentColor) {
        return blendArgb(0xFF13161C, accentColor, 0.06f);
    }

    public static int getSaoHoneycombStrokeColor(int accentColor) {
        return blendArgb(0xFF1A1E26, accentColor, 0.20f);
    }

    /** Kanvas Gray Night Honeycomb bergaya SAO Mobile beraksen warna tema aktif via BitmapShader tiling. */
    public static Drawable createSaoCanvasDrawable(Context context) {
        return new SaoHoneycombDrawable(density(context), getPrimaryAccentColor(context));
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
        private static int cachedAccent = 0;
        private static android.graphics.BitmapShader cachedShader;
        private final float d;
        private final int accentColor;
        private final android.graphics.Paint fillPaint = new android.graphics.Paint();

        SaoHoneycombDrawable(float density, int accentColor) {
            this.d = Math.max(1f, density);
            this.accentColor = accentColor;
        }

        private void ensureShader() {
            if (cachedShader != null && Math.abs(cachedDensity - d) < 0.01f && cachedAccent == accentColor) {
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
            facetPaint.setColor(withAlpha(getSaoHoneycombFacetColor(accentColor), 0x59));
            facetPaint.setMaskFilter(new android.graphics.BlurMaskFilter(
                    Math.max(2f, 2.2f * d), android.graphics.BlurMaskFilter.Blur.NORMAL));

            android.graphics.Paint strokePaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            strokePaint.setStyle(android.graphics.Paint.Style.STROKE);
            strokePaint.setStrokeWidth(Math.max(1.2f, 1.15f * d));
            // Opacity rendah (~32%) agar pola beraksen tema tetap terasa tanpa mencuri fokus dari katalog
            strokePaint.setColor(withAlpha(getSaoHoneycombStrokeColor(accentColor), 0x52));
            strokePaint.setMaskFilter(new android.graphics.BlurMaskFilter(
                    Math.max(1.8f, 1.8f * d), android.graphics.BlurMaskFilter.Blur.NORMAL));

            // Sorot halus faset tengah + salinan horizontal periodik agar halo blur tanpa garis tile
            float[] facetX = {hw - hexW, hw, hw + hexW};
            for (float fx : facetX) {
                c.drawPath(buildHexPath(fx, halfH, hw, segY), facetPaint);
            }

            float[][] centers = {
                    {hw, halfH},
                    {-hw, halfH}, {hexW + hw, halfH},
                    {hw, -halfH}, {hw, tileH + halfH},
                    {0f, 0f}, {hexW, 0f},
                    {0f, tileH}, {hexW, tileH}
            };
            for (float[] pt : centers) {
                c.drawPath(buildHexPath(pt[0], pt[1], hw, segY), strokePaint);
            }

            cachedShader = new android.graphics.BitmapShader(
                    bmp, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT);
            cachedDensity = d;
            cachedAccent = accentColor;
            fillPaint.setFilterBitmap(true);
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

    /** Header atas HUD ala Sword Art Online (Mobile) atau aurora gradient (TV). */
    public static Drawable createAuroraHeaderDrawable(Context context) {
        return createAuroraHeaderDrawable(context, 0);
    }

    public static Drawable createAuroraHeaderDrawable(Context context, int topInsetPx) {
        int accent = getPrimaryAccentColor(context);
        int secondary = getSecondaryAccentColor(context);
        if (isMobileSaoEnabled()) {
            return new SaoHeaderHudDrawable(density(context), accent, secondary, Math.max(0, topInsetPx));
        }
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{withAlpha(accent, 0x2A), withAlpha(secondary, 0x12), 0x00000000}
        );
    }

    /** Hitung posisi vertikal awal zona HUD SAO di bawah status bar/notch agar tidak bertabrakan. */
    public static int computeSaoHudTop(int topInsetPx, int totalHeightPx, float density) {
        float d = Math.max(1f, density);
        int minHudHeight = Math.round(40f * d);
        int maxTop = Math.max(0, totalHeightPx - minHudHeight);
        return Math.max(0, Math.min(topInsetPx, maxTop));
    }

    /** Warna ujung gradasi gauge SAO agar harmonis dengan tema aktif. */
    public static int getSaoHeaderGaugeEndColor(int accentColor, int secondaryColor) {
        return blendArgb(accentColor, secondaryColor, 0.45f);
    }

    /** Badge kategori aktif pada header SAO (rail vertikal kiri + sudut kanan-atas chamfered 45°). */
    public static Drawable createSaoHeaderBadgeDrawable(Context context) {
        if (!isMobileSaoEnabled()) {
            return createSearchCapsuleDrawable(context);
        }
        int accent = getPrimaryAccentColor(context);
        return createSaoChamferedDrawable(
                context,
                withAlpha(getSaoVoidColor(), 0xD9),
                withAlpha(getSaoHairlineColor(), 0xCC),
                accent,
                5f,
                1f,
                false
        );
    }

    /** Kapsul trigger & terminal pencarian pada header SAO (sudut chamfered 45°). */
    public static Drawable createSaoHeaderSearchDrawable(Context context, boolean isInputTerminal) {
        if (!isMobileSaoEnabled()) {
            return createSearchCapsuleDrawable(context);
        }
        int accent = getPrimaryAccentColor(context);
        int fill = isInputTerminal ? getSaoVoidColor() : getSaoSurfaceColor();
        int stroke = isInputTerminal ? withAlpha(accent, 0xCC) : withAlpha(getSaoHairlineColor(), 0xCC);
        return createSaoChamferedDrawable(context, fill, stroke, Color.TRANSPARENT, 6f, 1f, !isInputTerminal);
    }

    /** Bentuk poligon chamfered 45° khas UI Aincrad SAO untuk header emblem, badge, dan trigger search. */
    public static Drawable createSaoChamferedDrawable(
            Context context,
            int fillColor,
            int strokeColor,
            int leftAccentColor,
            float bevelDp,
            float strokeDp,
            boolean withRipple) {
        float d = density(context);
        SaoChamferedDrawable base = new SaoChamferedDrawable(
                d, fillColor, strokeColor, leftAccentColor, bevelDp, strokeDp);
        if (!withRipple) return base;
        int accent = getPrimaryAccentColor(context);
        return new RippleDrawable(ColorStateList.valueOf(withAlpha(accent, 0x33)), base, null);
    }

    private static final class SaoChamferedDrawable extends Drawable {
        private final float d;
        private final int fillColor;
        private final int strokeColor;
        private final int leftAccentColor;
        private final float bevelPx;
        private final float strokePx;
        private final android.graphics.Paint fillPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint strokePaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Path path = new android.graphics.Path();

        SaoChamferedDrawable(float density, int fillColor, int strokeColor, int leftAccentColor, float bevelDp, float strokeDp) {
            this.d = Math.max(1f, density);
            this.fillColor = fillColor;
            this.strokeColor = strokeColor;
            this.leftAccentColor = leftAccentColor;
            this.bevelPx = Math.max(0f, bevelDp) * this.d;
            this.strokePx = Math.max(0.8f, strokeDp * this.d);

            fillPaint.setStyle(android.graphics.Paint.Style.FILL);
            fillPaint.setColor(fillColor);

            strokePaint.setStyle(android.graphics.Paint.Style.STROKE);
            strokePaint.setStrokeWidth(strokePx);
            strokePaint.setColor(strokeColor);
        }

        @Override
        protected void onBoundsChange(android.graphics.Rect b) {
            super.onBoundsChange(b);
            path.reset();
            if (b.isEmpty()) return;
            float half = strokePx * 0.5f;
            float l = b.left + half;
            float t = b.top + half;
            float r = b.right - half;
            float bot = b.bottom - half;
            float c = Math.min(bevelPx, Math.min((r - l) * 0.4f, (bot - t) * 0.45f));

            if (leftAccentColor != Color.TRANSPARENT) {
                // Sisi kiri lurus untuk rail aksen vertikal, chamfer di sudut kanan atas & bawah
                path.moveTo(l, t);
                path.lineTo(r - c, t);
                path.lineTo(r, t + c);
                path.lineTo(r, bot);
                path.lineTo(l, bot);
                path.close();
            } else {
                // Chamfer diagonal kiri-atas & kanan-bawah
                path.moveTo(l + c, t);
                path.lineTo(r, t);
                path.lineTo(r, bot - c);
                path.lineTo(r - c, bot);
                path.lineTo(l, bot);
                path.lineTo(l, t + c);
                path.close();
            }
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect b = getBounds();
            if (b.isEmpty()) return;
            if (fillColor != Color.TRANSPARENT) {
                canvas.drawPath(path, fillPaint);
            }
            if (strokeColor != Color.TRANSPARENT) {
                canvas.drawPath(path, strokePaint);
            }
            if (leftAccentColor != Color.TRANSPARENT) {
                fillPaint.setColor(leftAccentColor);
                float railW = Math.max(2f, 2.2f * d);
                canvas.drawRect(b.left, b.top, b.left + railW, b.bottom, fillPaint);
                fillPaint.setColor(fillColor);
            }
        }

        @Override public void setAlpha(int alpha) { fillPaint.setAlpha(alpha); strokePaint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { fillPaint.setColorFilter(cf); strokePaint.setColorFilter(cf); }
        @Override @SuppressWarnings("deprecation") public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    /** Kanvas HUD header Sword Art Online: L-bracket, top hairline, dan slanted HP/Sync gauge strip. */
    private static final class SaoHeaderHudDrawable extends Drawable {
        private final float d;
        private final int accentColor;
        private final int secondaryColor;
        private final int topInsetPx;
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Path trackPath = new android.graphics.Path();
        private final android.graphics.Path fillPath = new android.graphics.Path();
        private android.graphics.Shader bgShader;
        private android.graphics.Shader gaugeShader;

        SaoHeaderHudDrawable(float density, int accentColor, int secondaryColor, int topInsetPx) {
            this.d = Math.max(1f, density);
            this.accentColor = accentColor;
            this.secondaryColor = secondaryColor;
            this.topInsetPx = Math.max(0, topInsetPx);
        }

        @Override
        protected void onBoundsChange(android.graphics.Rect b) {
            super.onBoundsChange(b);
            if (b.isEmpty()) return;
            int voidColor = getSaoVoidColor();
            int hudTop = computeSaoHudTop(topInsetPx, b.height(), d);
            float tHud = Math.min(0.85f, (float) hudTop / Math.max(1f, b.height()));

            bgShader = new android.graphics.LinearGradient(
                    0f, b.top, 0f, b.bottom,
                    new int[]{voidColor, withAlpha(voidColor, 0xEB), withAlpha(0xFF151922, 0xCC), withAlpha(voidColor, 0x00)},
                    new float[]{0f, tHud, Math.min(0.95f, tHud + (1f - tHud) * 0.72f), 1f},
                    android.graphics.Shader.TileMode.CLAMP);

            float padH = 10f * d;
            float gaugeH = 3.2f * d;
            float gaugeBottom = b.bottom - 1.5f * d;
            float gaugeTop = gaugeBottom - gaugeH;
            float gaugeLeft = b.left + padH;
            float gaugeTrackW = Math.min((b.width() - padH * 2f) * 0.44f, 180f * d);
            float slant = gaugeH * 1.15f;

            trackPath.reset();
            trackPath.moveTo(gaugeLeft, gaugeTop);
            trackPath.lineTo(gaugeLeft + gaugeTrackW, gaugeTop);
            trackPath.lineTo(gaugeLeft + gaugeTrackW - slant, gaugeBottom);
            trackPath.lineTo(gaugeLeft, gaugeBottom);
            trackPath.close();

            float gaugeFillW = gaugeTrackW * 0.72f;
            fillPath.reset();
            fillPath.moveTo(gaugeLeft, gaugeTop);
            fillPath.lineTo(gaugeLeft + gaugeFillW, gaugeTop);
            fillPath.lineTo(gaugeLeft + gaugeFillW - slant, gaugeBottom);
            fillPath.lineTo(gaugeLeft, gaugeBottom);
            fillPath.close();

            gaugeShader = new android.graphics.LinearGradient(
                    gaugeLeft, gaugeTop, gaugeLeft + gaugeFillW, gaugeTop,
                    accentColor, getSaoHeaderGaugeEndColor(accentColor, secondaryColor),
                    android.graphics.Shader.TileMode.CLAMP);
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect b = getBounds();
            if (b.isEmpty()) return;

            paint.setShader(bgShader);
            paint.setStyle(android.graphics.Paint.Style.FILL);
            canvas.drawRect(b, paint);
            paint.setShader(null);

            int hudTop = computeSaoHudTop(topInsetPx, b.height(), d);
            float padH = 10f * d;
            float topRuleY = hudTop + 1f * d;
            float hairlineThick = Math.max(1f, 0.9f * d);

            // Garis pembatas tipis di bawah status bar + aksen blok kiri
            paint.setColor(withAlpha(getSaoHairlineColor(), 0x73));
            canvas.drawRect(b.left + padH, topRuleY, b.right - padH, topRuleY + hairlineThick, paint);
            paint.setColor(withAlpha(accentColor, 0xCC));
            canvas.drawRect(b.left + padH, topRuleY, b.left + padH + 28f * d, topRuleY + Math.max(1.5f, 1.4f * d), paint);

            // Siku L-bracket kiri-atas & kanan-atas zona HUD
            float arm = 7f * d;
            float bw = Math.max(1.4f, 1.3f * d);
            float bracketTop = hudTop + 3f * d;
            float lX = b.left + 6f * d;
            float rX = b.right - 6f * d;
            paint.setColor(withAlpha(accentColor, 0xD9));
            canvas.drawRect(lX, bracketTop, lX + arm, bracketTop + bw, paint);
            canvas.drawRect(lX, bracketTop, lX + bw, bracketTop + arm, paint);

            paint.setColor(withAlpha(accentColor, 0x99));
            canvas.drawRect(rX - arm, bracketTop, rX, bracketTop + bw, paint);
            canvas.drawRect(rX - bw, bracketTop, rX, bracketTop + arm, paint);

            // SAO Slanted HP/Sync Gauge Track + Active Gradient Fill
            paint.setColor(withAlpha(getSaoHairlineColor(), 0x99));
            canvas.drawPath(trackPath, paint);

            paint.setShader(gaugeShader);
            canvas.drawPath(fillPath, paint);
            paint.setShader(null);

            // Sekat segmen HP gauge ala SAO
            float gaugeH = 3.2f * d;
            float gaugeBottom = b.bottom - 1.5f * d;
            float gaugeTop = gaugeBottom - gaugeH;
            float gaugeLeft = b.left + padH;
            float gaugeTrackW = Math.min((b.width() - padH * 2f) * 0.44f, 180f * d);
            paint.setColor(getSaoVoidColor());
            float notchW = Math.max(1.2f, 1.2f * d);
            canvas.drawRect(gaugeLeft + gaugeTrackW * 0.24f, gaugeTop, gaugeLeft + gaugeTrackW * 0.24f + notchW, gaugeBottom, paint);
            canvas.drawRect(gaugeLeft + gaugeTrackW * 0.48f, gaugeTop, gaugeLeft + gaugeTrackW * 0.48f + notchW, gaugeBottom, paint);

            // Garis telemetri kanan + diamond pip
            float ruleStart = gaugeLeft + gaugeTrackW + 6f * d;
            float ruleEnd = b.right - padH - 6f * d;
            float ruleY = gaugeBottom - gaugeH * 0.5f;
            if (ruleEnd > ruleStart) {
                paint.setColor(withAlpha(getSaoHairlineColor(), 0xB3));
                canvas.drawRect(ruleStart, ruleY, ruleEnd, ruleY + hairlineThick, paint);
            }
            float pipR = 2.2f * d;
            float pipCx = b.right - padH - 1.5f * d;
            paint.setColor(accentColor);
            canvas.save();
            canvas.rotate(45f, pipCx, ruleY + hairlineThick * 0.5f);
            canvas.drawRect(pipCx - pipR, ruleY - pipR, pipCx + pipR, ruleY + pipR, paint);
            canvas.restore();
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { paint.setColorFilter(cf); }
        @Override @SuppressWarnings("deprecation") public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
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

    /** Menerapkan aksen garis vertikal kiri & siku L-bracket ala System Menu SAO pada MaterialCardView (0dp). */
    public static void applySaoCardAccentFrame(
            com.google.android.material.card.MaterialCardView card,
            Context context,
            int surface,
            int border,
            int accent,
            boolean isHero) {
        if (card == null) return;
        float d = density(context);
        card.setRadius(0f);
        card.setCardElevation(0f);
        card.setCardBackgroundColor(surface);
        card.setStrokeWidth(Math.max(1, Math.round(d)));
        card.setStrokeColor(isHero ? withAlpha(accent, 0x59) : withAlpha(border, 0xB8));

        Drawable existing = card.getForeground();
        if (existing instanceof SaoAccentFrameForegroundDrawable) {
            ((SaoAccentFrameForegroundDrawable) existing).setAccentColor(accent);
            card.invalidate();
            return;
        }
        SaoAccentLinesDrawable accentLines = new SaoAccentLinesDrawable(d, accent, isHero);
        Drawable[] layers = existing != null ? new Drawable[]{accentLines, existing} : new Drawable[]{accentLines};
        card.setForeground(new SaoAccentFrameForegroundDrawable(layers, accentLines));
    }

    private static final class SaoAccentFrameForegroundDrawable extends LayerDrawable {
        private final SaoAccentLinesDrawable linesDrawable;

        SaoAccentFrameForegroundDrawable(Drawable[] layers, SaoAccentLinesDrawable linesDrawable) {
            super(layers);
            this.linesDrawable = linesDrawable;
        }

        void setAccentColor(int accent) {
            linesDrawable.setAccentColor(accent);
        }

        @Override
        public boolean getPadding(android.graphics.Rect padding) {
            if (padding != null) padding.set(0, 0, 0, 0);
            return false;
        }
    }

    private static final class SaoAccentLinesDrawable extends Drawable {
        private final float d;
        private final boolean isHero;
        private int accent;
        private final android.graphics.Paint paint = new android.graphics.Paint();

        SaoAccentLinesDrawable(float density, int accent, boolean isHero) {
            this.d = Math.max(1f, density);
            this.accent = accent;
            this.isHero = isHero;
        }

        void setAccentColor(int accent) {
            this.accent = accent;
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect b = getBounds();
            if (b.isEmpty()) return;

            float railW = Math.max(2f, (isHero ? 2.5f : 2f) * d);
            float armLen = (isHero ? 12f : 7f) * d;
            float bracketW = Math.max(1.5f, (isHero ? 1.8f : 1.4f) * d);

            // Garis aksen vertikal kiri khas System Menu SAO
            paint.setColor(withAlpha(accent, isHero ? 0xF2 : 0xD9));
            canvas.drawRect(b.left, b.top, b.left + railW, b.bottom, paint);

            // Siku corner bracket L kiri-atas & kanan-bawah
            paint.setColor(withAlpha(accent, isHero ? 0xFF : 0xE6));
            canvas.drawRect(b.left, b.top, b.left + armLen, b.top + bracketW, paint);
            canvas.drawRect(b.left, b.top, b.left + bracketW, b.top + armLen, paint);

            canvas.drawRect(b.right - armLen, b.bottom - bracketW, b.right, b.bottom, paint);
            canvas.drawRect(b.right - bracketW, b.bottom - armLen, b.right, b.bottom, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { paint.setColorFilter(cf); }
        @Override @SuppressWarnings("deprecation") public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
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

    public static int getSaoDetailHeroTintColor(int accentColor) {
        return blendArgb(getSaoVoidColor(), accentColor, 0.14f);
    }

    public static int getSaoDetailMidTintColor(int accentColor) {
        return blendArgb(getSaoVoidColor(), accentColor, 0.06f);
    }

    public static Drawable createDetailHeroScrimDrawable(Context context) {
        if (isMobileSaoEnabled()) {
            int heroTint = getSaoDetailHeroTintColor(getPrimaryAccentColor(context));
            return new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x00000000, withAlpha(heroTint, 0x55), withAlpha(heroTint, 0xCC), heroTint}
            );
        }
        int voidColor = getVoidColor(context);
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, withAlpha(voidColor, 0x55), withAlpha(voidColor, 0xCC), voidColor}
        );
    }

    /**
     * Gradasi vertikal panel Detail Portrait Mobile: menyambung solid di akhir wallpaper 440dp lalu
     * memudar transparan penuh ke paling bawah agar latar honeycomb blur terlihat paling jelas.
     */
    public static Drawable createDetailScrollGradientDrawable(Context context) {
        int accent = getPrimaryAccentColor(context);
        return new DetailScrollGradientDrawable(
                density(context),
                getSaoDetailHeroTintColor(accent),
                getSaoDetailMidTintColor(accent),
                getSaoVoidColor());
    }

    public static Drawable createDetailLandscapeScrimDrawable(Context context) {
        int accent = getPrimaryAccentColor(context);
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        withAlpha(getSaoDetailHeroTintColor(accent), 0xE6),
                        withAlpha(getSaoDetailMidTintColor(accent), 0xB8),
                        withAlpha(getSaoVoidColor(), 0x66)
                }
        );
    }

    private static final class DetailScrollGradientDrawable extends Drawable {
        private final float d;
        private final int heroTint;
        private final int midTint;
        private final int voidColor;
        private final android.graphics.Paint paint = new android.graphics.Paint();

        DetailScrollGradientDrawable(float density, int heroTint, int midTint, int voidColor) {
            this.d = Math.max(1f, density);
            this.heroTint = heroTint;
            this.midTint = midTint;
            this.voidColor = voidColor;
        }

        @Override
        protected void onBoundsChange(android.graphics.Rect bounds) {
            super.onBoundsChange(bounds);
            float h = Math.max(1f, bounds.height());
            float t0 = Math.min(0.75f, (440f * d) / h);
            float tMid = t0 + (1f - t0) * 0.42f;
            float tLow = t0 + (1f - t0) * 0.78f;
            paint.setShader(new android.graphics.LinearGradient(
                    0f, 0f, 0f, h,
                    new int[]{heroTint, heroTint, withAlpha(midTint, 0xB8), withAlpha(voidColor, 0x33), withAlpha(voidColor, 0x00)},
                    new float[]{0f, t0, tMid, tLow, 1f},
                    android.graphics.Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            canvas.drawRect(getBounds(), paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
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
        // Hilangkan border kotak di tombol play/pause center ExoPlayer agar tampil bersih di Mobile
        int shape = (sao && !isCenterPlay) ? GradientDrawable.RECTANGLE : GradientDrawable.OVAL;
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
        if (isCenterPlay && !sao) {
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
