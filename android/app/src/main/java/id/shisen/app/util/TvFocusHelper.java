package id.shisen.app.util;

import android.content.res.ColorStateList;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import id.shisen.app.R;

/**
 * Helper untuk visual focus feedback di Android TV (D-pad remote).
 * Memberikan animasi zoom halus (scale 1.05x), elevasi dinamis, dan border highlight.
 */
public final class TvFocusHelper {

    private static final DecelerateInterpolator INTERPOLATOR = new DecelerateInterpolator();

    private TvFocusHelper() {}

    /**
     * Terapkan animasi skala, elevasi, dan stroke highlight saat View menerima fokus D-Pad.
     */
    public static void attachFocusScale(View view, float focusedScale) {
        if (view == null) return;
        final int origStrokeWidth = (view instanceof MaterialButton) ? ((MaterialButton) view).getStrokeWidth() : 0;
        final ColorStateList origStrokeColor = (view instanceof MaterialButton) ? ((MaterialButton) view).getStrokeColor() : null;
        final int focusStrokePx = Math.max(4, (int) (2.5f * view.getResources().getDisplayMetrics().density));

        view.setOnFocusChangeListener((v, hasFocus) -> {
            float targetScale = hasFocus ? focusedScale : 1.0f;
            float targetZ = hasFocus ? 14f : 0f;

            v.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .translationZ(targetZ)
                    .setDuration(160)
                    .setInterpolator(INTERPOLATOR)
                    .start();

            if (v instanceof MaterialButton) {
                MaterialButton mb = (MaterialButton) v;
                if (hasFocus) {
                    mb.setStrokeColor(ColorStateList.valueOf(AnimeThemeManager.getPrimaryAccentColor(v.getContext())));
                    mb.setStrokeWidth(focusStrokePx);
                } else {
                    mb.setStrokeColor(origStrokeColor);
                    mb.setStrokeWidth(origStrokeWidth);
                }
            }
        });
    }

    /**
     * Terapkan animasi skala + luminous stroke highlight untuk MaterialCardView.
     */
    public static void attachCardFocus(MaterialCardView card, float focusedScale) {
        if (card == null) return;
        final int defaultStrokePx = Math.max(1, (int) (1.0f * card.getResources().getDisplayMetrics().density));
        final int focusStrokePx = Math.max(5, (int) (3.0f * card.getResources().getDisplayMetrics().density));
        final ColorStateList defaultBgColor = card.getCardBackgroundColor();

        card.setOnFocusChangeListener((v, hasFocus) -> {
            float targetScale = hasFocus ? focusedScale : 1.0f;
            float targetZ = hasFocus ? 16f : 0f;

            card.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .translationZ(targetZ)
                    .setDuration(160)
                    .setInterpolator(INTERPOLATOR)
                    .start();

            int defaultStrokeColor = AnimeThemeManager.getBorderColor(card.getContext());
            int focusedStrokeColor = AnimeThemeManager.getPrimaryAccentColor(card.getContext());
            int surfaceColor = AnimeThemeManager.getSurfaceColor(card.getContext());
            ColorStateList focusedBgColor = ColorStateList.valueOf(
                    AnimeThemeManager.withAlpha(focusedStrokeColor, 0x22) == 0 ? surfaceColor :
                            blendOverlay(surfaceColor, focusedStrokeColor)
            );

            card.setStrokeColor(hasFocus ? focusedStrokeColor : defaultStrokeColor);
            card.setStrokeWidth(hasFocus ? focusStrokePx : defaultStrokePx);
            card.setCardBackgroundColor(hasFocus ? focusedBgColor : (defaultBgColor != null ? ColorStateList.valueOf(surfaceColor) : defaultBgColor));
        });
    }

    private static int blendOverlay(int baseColor, int accentColor) {
        int r = (int) (((baseColor >> 16) & 0xFF) * 0.85f + ((accentColor >> 16) & 0xFF) * 0.15f);
        int g = (int) (((baseColor >> 8) & 0xFF) * 0.85f + ((accentColor >> 8) & 0xFF) * 0.15f);
        int b = (int) ((baseColor & 0xFF) * 0.85f + (accentColor & 0xFF) * 0.15f);
        return (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
