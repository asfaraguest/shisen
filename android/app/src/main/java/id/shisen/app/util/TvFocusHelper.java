package id.shisen.app.util;

import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.core.content.ContextCompat;
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
     * Terapkan animasi skala dan elevasi saat View menerima atau kehilangan fokus D-Pad.
     */
    public static void attachFocusScale(View view, float focusedScale) {
        if (view == null) return;
        view.setOnFocusChangeListener((v, hasFocus) -> {
            float targetScale = hasFocus ? focusedScale : 1.0f;
            float targetZ = hasFocus ? 10f : 0f;

            v.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .translationZ(targetZ)
                    .setDuration(160)
                    .setInterpolator(INTERPOLATOR)
                    .start();
        });
    }

    /**
     * Terapkan animasi skala + stroke highlight untuk MaterialCardView.
     */
    public static void attachCardFocus(MaterialCardView card, float focusedScale) {
        if (card == null) return;
        final int defaultStrokeColor = ContextCompat.getColor(card.getContext(), R.color.frost_border);
        final int focusedStrokeColor = ContextCompat.getColor(card.getContext(), R.color.accent_glacial);

        card.setOnFocusChangeListener((v, hasFocus) -> {
            float targetScale = hasFocus ? focusedScale : 1.0f;
            float targetZ = hasFocus ? 12f : 0f;

            card.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .translationZ(targetZ)
                    .setDuration(160)
                    .setInterpolator(INTERPOLATOR)
                    .start();

            card.setStrokeColor(hasFocus ? focusedStrokeColor : defaultStrokeColor);
            card.setStrokeWidth(hasFocus ? 3 : 1);
        });
    }
}
