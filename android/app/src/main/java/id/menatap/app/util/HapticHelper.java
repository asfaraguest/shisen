package id.menatap.app.util;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

public class HapticHelper {
    public static void performClick(View v) {
        if (v == null) return;
        try {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}
    }

    public static void performTick(View v) {
        if (v == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                v.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE);
            } else {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        } catch (Exception ignored) {}
    }

    public static void performSelection(View v) {
        performTick(v);
    }

    public static void performHeavy(Context context) {
        if (context == null) return;
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(35);
                }
            }
        } catch (Exception ignored) {}
    }
}
