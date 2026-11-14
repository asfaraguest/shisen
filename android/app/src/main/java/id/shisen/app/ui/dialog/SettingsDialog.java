package id.shisen.app.ui.dialog;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

import id.shisen.app.BuildConfig;
import id.shisen.app.R;
import id.shisen.app.data.cache.FeedCache;
import id.shisen.app.data.subtitle.AiSubtitleService;
import id.shisen.app.util.AnimeThemeManager;
import id.shisen.app.util.AppSettings;
import id.shisen.app.util.AppUpdateManager;
import id.shisen.app.util.HapticHelper;
import id.shisen.app.util.LocaleHelper;
import id.shisen.app.util.SystemUiHelper;

public class SettingsDialog {

    public interface OnSettingsChangeListener {
        void onThemeChanged();
        void onSafeModeChanged(boolean isSafeMode);
        default void onLanguageChanged() {}
    }

    public static void show(Activity activity, OnSettingsChangeListener listener) {
        show(activity, false, listener);
    }

    public static void show(Activity activity, boolean focusThemeSection, OnSettingsChangeListener listener) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_settings, null);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageButton btnClose = dialogView.findViewById(R.id.btn_close_settings);
        btnClose.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            dialog.dismiss();
        });

        if (id.shisen.app.ui.player.PlayerKeys.isTv()) {
            id.shisen.app.util.TvFocusHelper.attachFocusScale(btnClose, 1.1f);
        }

        // Khusus Shisen TV: sembunyikan Tema dan Safety, sediakan toggle untuk BYOK AI
        if (id.shisen.app.ui.player.PlayerKeys.isTv()) {
            View sectionTheme = dialogView.findViewById(R.id.container_section_theme);
            View sectionSafety = dialogView.findViewById(R.id.container_section_safety);
            View sectionAi = dialogView.findViewById(R.id.container_section_ai);
            View btnToggleByok = dialogView.findViewById(R.id.btn_toggle_byok_ai);
            TextView tvToggleIcon = dialogView.findViewById(R.id.tv_toggle_byok_icon);

            if (sectionTheme != null) sectionTheme.setVisibility(View.GONE);
            if (sectionSafety != null) sectionSafety.setVisibility(View.GONE);
            if (sectionAi != null) sectionAi.setVisibility(View.GONE);

            final boolean[] aiSetupDone = new boolean[]{false};

            if (btnToggleByok != null) {
                btnToggleByok.setVisibility(View.VISIBLE);
                id.shisen.app.util.TvFocusHelper.attachFocusScale(btnToggleByok, 1.04f);
                btnToggleByok.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    if (sectionAi != null) {
                        boolean isVisible = sectionAi.getVisibility() == View.VISIBLE;
                        if (!isVisible && !aiSetupDone[0]) {
                            aiSetupDone[0] = true;
                            setupAiSubtitleSection(dialogView, activity, dialog);
                        }
                        sectionAi.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                        if (tvToggleIcon != null) {
                            tvToggleIcon.setText(isVisible ? "▼" : "▲");
                        }
                        if (!isVisible) {
                            View firstAiInput = dialogView.findViewById(R.id.et_settings_ai_key);
                            if (firstAiInput != null) {
                                firstAiInput.post(firstAiInput::requestFocus);
                            }
                        }
                    }
                });
            }
        } else {
            // 1. Theme Selection
            setupThemeSection(dialogView, activity, listener);

            // 3. Safe Mode & Haptic
            setupSystemSection(dialogView, activity, listener);

            // 4. AI Subtitle & Model Selector
            setupAiSubtitleSection(dialogView, activity, dialog);
        }

        // 2. Language Selection (ID / EN / JA / ES)
        setupLanguageSection(dialogView, activity, listener, dialog);

        // 5. About & Update
        setupAboutSection(dialogView, activity);

        applyDialogTheme(dialogView, activity);

        SystemUiHelper.setupCustomDialogBounds(dialog, activity);
        dialog.show();

        if (id.shisen.app.ui.player.PlayerKeys.isTv()) {
            View btnLangId = dialogView.findViewById(R.id.btn_lang_id);
            if (btnLangId != null) {
                btnLangId.post(btnLangId::requestFocus);
            }
        }
    }

    private static void setupThemeSection(View root, Activity activity, OnSettingsChangeListener listener) {
        View cardShinkai = root.findViewById(R.id.card_theme_shinkai);
        View cardSakura = root.findViewById(R.id.card_theme_sakura);
        View cardCyber = root.findViewById(R.id.card_theme_cyber);
        View cardGhibli = root.findViewById(R.id.card_theme_ghibli);

        RadioButton rbShinkai = root.findViewById(R.id.rb_theme_shinkai);
        RadioButton rbSakura = root.findViewById(R.id.rb_theme_sakura);
        RadioButton rbCyber = root.findViewById(R.id.rb_theme_cyber);
        RadioButton rbGhibli = root.findViewById(R.id.rb_theme_ghibli);

        Runnable updateRadios = () -> {
            String current = AnimeThemeManager.getTheme(activity);
            rbShinkai.setChecked(AnimeThemeManager.THEME_SHINKAI.equals(current));
            rbSakura.setChecked(AnimeThemeManager.THEME_SAKURA.equals(current));
            rbCyber.setChecked(AnimeThemeManager.THEME_CYBER.equals(current));
            rbGhibli.setChecked(AnimeThemeManager.THEME_GHIBLI.equals(current));
        };
        updateRadios.run();

        cardShinkai.setOnClickListener(v -> {
            HapticHelper.performSelection(v);
            AnimeThemeManager.setTheme(activity, AnimeThemeManager.THEME_SHINKAI);
            SystemUiHelper.applySystemBarsTheme(activity, AnimeThemeManager.THEME_SHINKAI);
            updateRadios.run();
            applyDialogTheme(root, activity);
            if (listener != null) listener.onThemeChanged();
        });

        cardSakura.setOnClickListener(v -> {
            HapticHelper.performSelection(v);
            AnimeThemeManager.setTheme(activity, AnimeThemeManager.THEME_SAKURA);
            SystemUiHelper.applySystemBarsTheme(activity, AnimeThemeManager.THEME_SAKURA);
            updateRadios.run();
            applyDialogTheme(root, activity);
            if (listener != null) listener.onThemeChanged();
        });

        cardCyber.setOnClickListener(v -> {
            HapticHelper.performSelection(v);
            AnimeThemeManager.setTheme(activity, AnimeThemeManager.THEME_CYBER);
            SystemUiHelper.applySystemBarsTheme(activity, AnimeThemeManager.THEME_CYBER);
            updateRadios.run();
            applyDialogTheme(root, activity);
            if (listener != null) listener.onThemeChanged();
        });

        cardGhibli.setOnClickListener(v -> {
            HapticHelper.performSelection(v);
            AnimeThemeManager.setTheme(activity, AnimeThemeManager.THEME_GHIBLI);
            SystemUiHelper.applySystemBarsTheme(activity, AnimeThemeManager.THEME_GHIBLI);
            updateRadios.run();
            applyDialogTheme(root, activity);
            if (listener != null) listener.onThemeChanged();
        });
    }

    private static void updateLanguageChips(View root, Activity activity) {
        TextView btnLangId = root.findViewById(R.id.btn_lang_id);
        TextView btnLangEn = root.findViewById(R.id.btn_lang_en);
        TextView btnLangJa = root.findViewById(R.id.btn_lang_ja);
        TextView btnLangEs = root.findViewById(R.id.btn_lang_es);
        if (btnLangId == null || btnLangEn == null || btnLangJa == null || btnLangEs == null) return;

        String active = LocaleHelper.getLanguage(activity);
        boolean isId = LocaleHelper.LANG_ID.equalsIgnoreCase(active);
        boolean isEn = LocaleHelper.LANG_EN.equalsIgnoreCase(active);
        boolean isJa = LocaleHelper.LANG_JA.equalsIgnoreCase(active);
        boolean isEs = LocaleHelper.LANG_ES.equalsIgnoreCase(active);

        int selectedColor = Color.parseColor("#080C14");
        int normalColor = ContextCompat.getColor(activity, R.color.text_frost_bright);

        TextView[] chips = {btnLangId, btnLangEn, btnLangJa, btnLangEs};
        boolean[] states = {isId, isEn, isJa, isEs};
        for (int i = 0; i < chips.length; i++) {
            AnimeThemeManager.applyBackgroundPreservingPadding(
                    chips[i], AnimeThemeManager.createChipDrawable(activity, states[i]));
            chips[i].setTextColor(states[i] ? selectedColor : normalColor);
        }
    }

    private static void applyDialogTheme(View root, Activity activity) {
        int accent = AnimeThemeManager.getPrimaryAccentColor(activity);
        int border = AnimeThemeManager.getBorderColor(activity);
        android.content.res.ColorStateList accentTint = android.content.res.ColorStateList.valueOf(accent);
        android.content.res.ColorStateList borderTint = android.content.res.ColorStateList.valueOf(border);

        updateLanguageChips(root, activity);

        SwitchMaterial switchSafeMode = root.findViewById(R.id.switch_safe_mode);
        if (switchSafeMode != null) switchSafeMode.setThumbTintList(accentTint);

        SwitchMaterial switchHaptic = root.findViewById(R.id.switch_haptic);
        if (switchHaptic != null) switchHaptic.setThumbTintList(accentTint);

        TextView tvToggleIcon = root.findViewById(R.id.tv_toggle_byok_icon);
        if (tvToggleIcon != null) tvToggleIcon.setTextColor(accent);

        EditText etModel = root.findViewById(R.id.et_settings_ai_model);
        if (etModel != null) etModel.setTextColor(accent);

        TextView btnResetPrompt = root.findViewById(R.id.btn_settings_reset_prompt);
        if (btnResetPrompt != null) btnResetPrompt.setTextColor(accent);

        Button btnSave = root.findViewById(R.id.btn_settings_save_ai);
        if (btnSave != null) btnSave.setBackgroundTintList(accentTint);

        Button btnClear = root.findViewById(R.id.btn_settings_clear_ai);
        if (btnClear instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton) btnClear).setStrokeColor(borderTint);
        }
        Button btnCheckUpdate = root.findViewById(R.id.btn_settings_check_update);
        if (btnCheckUpdate instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton) btnCheckUpdate).setStrokeColor(borderTint);
        }
        Button btnClearCache = root.findViewById(R.id.btn_settings_clear_cache);
        if (btnClearCache instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton) btnClearCache).setStrokeColor(borderTint);
        }

        LinearLayout layoutChips = root.findViewById(R.id.layout_model_chips);
        if (layoutChips != null) {
            String currentModel = etModel != null ? etModel.getText().toString().trim() : "";
            for (int i = 0; i < layoutChips.getChildCount(); i++) {
                View child = layoutChips.getChildAt(i);
                if (child instanceof TextView) {
                    boolean sel = ((TextView) child).getText().toString().equalsIgnoreCase(currentModel);
                    applyChipStyle((TextView) child, activity, sel);
                }
            }
        }
    }

    private static void setupLanguageSection(View root, Activity activity, OnSettingsChangeListener listener, AlertDialog dialog) {
        TextView btnLangId = root.findViewById(R.id.btn_lang_id);
        TextView btnLangEn = root.findViewById(R.id.btn_lang_en);
        TextView btnLangJa = root.findViewById(R.id.btn_lang_ja);
        TextView btnLangEs = root.findViewById(R.id.btn_lang_es);

        if (btnLangId == null || btnLangEn == null || btnLangJa == null || btnLangEs == null) return;

        Runnable updateChips = () -> updateLanguageChips(root, activity);
        updateChips.run();

        View.OnClickListener langClickListener = v -> {
            HapticHelper.performSelection(v);
            String selectedLang = LocaleHelper.LANG_ID;
            if (v.getId() == R.id.btn_lang_en) {
                selectedLang = LocaleHelper.LANG_EN;
            } else if (v.getId() == R.id.btn_lang_ja) {
                selectedLang = LocaleHelper.LANG_JA;
            } else if (v.getId() == R.id.btn_lang_es) {
                selectedLang = LocaleHelper.LANG_ES;
            }

            if (!selectedLang.equalsIgnoreCase(LocaleHelper.getLanguage(activity))) {
                LocaleHelper.setLocale(activity, selectedLang);
                updateChips.run();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                if (listener != null) {
                    listener.onLanguageChanged();
                } else {
                    activity.recreate();
                }
            }
        };

        btnLangId.setOnClickListener(langClickListener);
        btnLangEn.setOnClickListener(langClickListener);
        btnLangJa.setOnClickListener(langClickListener);
        btnLangEs.setOnClickListener(langClickListener);

        if (id.shisen.app.ui.player.PlayerKeys.isTv()) {
            id.shisen.app.util.TvFocusHelper.attachFocusScale(btnLangId, 1.05f);
            id.shisen.app.util.TvFocusHelper.attachFocusScale(btnLangEn, 1.05f);
            id.shisen.app.util.TvFocusHelper.attachFocusScale(btnLangJa, 1.05f);
            id.shisen.app.util.TvFocusHelper.attachFocusScale(btnLangEs, 1.05f);
        }
    }

    private static void setupSystemSection(View root, Activity activity, OnSettingsChangeListener listener) {
        SwitchMaterial switchSafeMode = root.findViewById(R.id.switch_safe_mode);
        SwitchMaterial switchHaptic = root.findViewById(R.id.switch_haptic);

        switchSafeMode.setChecked(AppSettings.isSafeMode(activity));
        switchSafeMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            HapticHelper.performClick(buttonView);
            AppSettings.setSafeMode(activity, isChecked);
            if (listener != null) listener.onSafeModeChanged(isChecked);
            Toast.makeText(activity, activity.getString(isChecked
                    ? R.string.settings_toast_safe_mode_on
                    : R.string.settings_toast_safe_mode_off), Toast.LENGTH_SHORT).show();
        });

        switchHaptic.setChecked(AppSettings.isHapticEnabled(activity));
        switchHaptic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSettings.setHapticEnabled(activity, isChecked);
            if (isChecked) {
                HapticHelper.performHeavy(activity);
                Toast.makeText(activity, activity.getString(R.string.settings_toast_haptic_on), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, activity.getString(R.string.settings_toast_haptic_off), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void setupAiSubtitleSection(View root, Activity activity, AlertDialog dialog) {
        TextView tvBadge = root.findViewById(R.id.tv_settings_ai_badge);
        EditText etKey = root.findViewById(R.id.et_settings_ai_key);
        LinearLayout layoutChips = root.findViewById(R.id.layout_model_chips);
        EditText etModel = root.findViewById(R.id.et_settings_ai_model);
        TextView btnResetPrompt = root.findViewById(R.id.btn_settings_reset_prompt);
        EditText etPrompt = root.findViewById(R.id.et_settings_ai_prompt);
        Button btnClear = root.findViewById(R.id.btn_settings_clear_ai);
        Button btnSave = root.findViewById(R.id.btn_settings_save_ai);

        String currentKey = AiSubtitleService.getKey(activity);
        if (currentKey != null && !currentKey.isEmpty()) {
            etKey.setText(currentKey);
            AiSubtitleService.Provider p = AiSubtitleService.Provider.detect(currentKey);
            tvBadge.setText(activity.getString(R.string.settings_ai_badge_active, p.displayName));
        }

        String savedModel = AiSubtitleService.getModel(activity);
        AiSubtitleService.Provider activeProv = AiSubtitleService.getActiveProvider(activity);
        etModel.setText(AiSubtitleService.getEffectiveModel(activeProv, savedModel));

        etPrompt.setText(AiSubtitleService.getCustomPrompt(activity));

        populateModelChips(layoutChips, activity, AiSubtitleService.getActiveProvider(activity), etModel);

        etKey.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String input = s != null ? s.toString().trim() : "";
                AiSubtitleService.Provider p = AiSubtitleService.Provider.detect(input);
                if (!input.isEmpty()) {
                    tvBadge.setText(activity.getString(R.string.settings_ai_badge_detected, p.displayName));
                } else {
                    tvBadge.setText(activity.getString(R.string.settings_ai_badge_none));
                }
                populateModelChips(layoutChips, activity, p, etModel);
                if (etModel.getText().toString().trim().isEmpty()) {
                    etModel.setText(AiSubtitleService.getDefaultModel(p));
                }
            }
        });

        btnResetPrompt.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            etPrompt.setText(AiSubtitleService.DEFAULT_CUSTOM_PROMPT);
            AiSubtitleService.saveCustomPrompt(activity, AiSubtitleService.DEFAULT_CUSTOM_PROMPT);
            Toast.makeText(activity, activity.getString(R.string.settings_toast_prompt_reset), Toast.LENGTH_SHORT).show();
        });

        final Runnable autoSaveTask = () -> {
            String inputKey = etKey.getText().toString().trim();
            String inputPrompt = etPrompt.getText().toString().trim();
            String inputModel = etModel.getText().toString().trim();

            if (!inputKey.isEmpty()) {
                boolean saved = AiSubtitleService.saveKey(activity, inputKey);
                if (!saved && activity != null) {
                    android.widget.Toast.makeText(activity, R.string.secure_store_failed, android.widget.Toast.LENGTH_LONG).show();
                }
            }
            if (!inputModel.isEmpty()) {
                AiSubtitleService.saveModel(activity, inputModel);
            }
            if (!inputPrompt.isEmpty()) {
                AiSubtitleService.saveCustomPrompt(activity, inputPrompt);
            }
        };

        if (dialog != null) {
            dialog.setOnDismissListener(d -> autoSaveTask.run());
        }

        btnSave.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            autoSaveTask.run();

            String inputKey = etKey.getText().toString().trim();
            if (!inputKey.isEmpty()) {
                AiSubtitleService.Provider p = AiSubtitleService.Provider.detect(inputKey);
                Toast.makeText(activity, activity.getString(R.string.settings_toast_ai_saved, p.displayName), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Pengaturan AI subtitle tersimpan", Toast.LENGTH_SHORT).show();
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        });

        btnClear.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            AiSubtitleService.clearKey(activity);
            AiSubtitleService.clearCustomPrompt(activity);
            AiSubtitleService.clearModel(activity);

            etKey.setText("");
            etModel.setText(AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.GEMINI));
            etPrompt.setText(AiSubtitleService.DEFAULT_CUSTOM_PROMPT);
            tvBadge.setText(activity.getString(R.string.settings_ai_badge_none));
            populateModelChips(layoutChips, activity, AiSubtitleService.Provider.GEMINI, etModel);

            Toast.makeText(activity, activity.getString(R.string.settings_toast_ai_cleared), Toast.LENGTH_SHORT).show();
        });
    }

    public static void populateModelChips(LinearLayout layout, Context context, AiSubtitleService.Provider provider, EditText etModel) {
        if (layout == null || context == null) return;
        layout.removeAllViews();

        List<String> models = AiSubtitleService.getAvailableModels(provider);
        String currentModel = etModel != null ? etModel.getText().toString().trim() : "";
        if (currentModel.isEmpty()) {
            currentModel = AiSubtitleService.getDefaultModel(provider);
        }

        for (String m : models) {
            TextView chip = new TextView(context);
            chip.setText(m);
            chip.setTextSize(11f);
            chip.setPadding(24, 12, 24, 12);
            chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(lp);

            boolean isSelected = m.equalsIgnoreCase(currentModel);
            applyChipStyle(chip, context, isSelected);

            chip.setOnClickListener(v -> {
                HapticHelper.performSelection(v);
                if (etModel != null) {
                    etModel.setText(m);
                }
                AiSubtitleService.saveModel(context, m);
                for (int i = 0; i < layout.getChildCount(); i++) {
                    View child = layout.getChildAt(i);
                    if (child instanceof TextView) {
                        applyChipStyle((TextView) child, context, child == chip);
                    }
                }
            });

            layout.addView(chip);
        }
    }

    private static void applyChipStyle(TextView chip, Context context, boolean isSelected) {
        AnimeThemeManager.applyBackgroundPreservingPadding(
                chip, AnimeThemeManager.createChipDrawable(context, isSelected));
        if (isSelected) {
            chip.setTextColor(Color.parseColor("#080C14"));
        } else {
            chip.setTextColor(ContextCompat.getColor(context, R.color.text_frost_bright));
        }
    }

    private static void setupAboutSection(View root, Activity activity) {
        TextView tvVersion = root.findViewById(R.id.tv_settings_app_version);
        if (tvVersion != null) {
            tvVersion.setText("Shisen v" + BuildConfig.VERSION_NAME);
        }

        Button btnCheckUpdate = root.findViewById(R.id.btn_settings_check_update);
        if (btnCheckUpdate != null) {
            btnCheckUpdate.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                AppUpdateManager.getInstance().checkUpdate(activity, true);
            });
        }

        Button btnClearCache = root.findViewById(R.id.btn_settings_clear_cache);
        if (btnClearCache != null) {
            btnClearCache.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                FeedCache.getInstance(activity).clearAllCache();
                Toast.makeText(activity, R.string.settings_cache_cleared, Toast.LENGTH_SHORT).show();
            });
        }

        if (id.shisen.app.ui.player.PlayerKeys.isTv()) {
            if (btnCheckUpdate != null) id.shisen.app.util.TvFocusHelper.attachFocusScale(btnCheckUpdate, 1.04f);
            if (btnClearCache != null) id.shisen.app.util.TvFocusHelper.attachFocusScale(btnClearCache, 1.04f);
        }
    }
}
