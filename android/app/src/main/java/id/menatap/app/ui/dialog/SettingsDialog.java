package id.menatap.app.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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

import id.menatap.app.BuildConfig;
import id.menatap.app.R;
import id.menatap.app.data.subtitle.AiSubtitleService;
import id.menatap.app.util.AnimeThemeManager;
import id.menatap.app.util.AppSettings;
import id.menatap.app.util.AppUpdateManager;
import id.menatap.app.util.HapticHelper;
import id.menatap.app.util.LocaleHelper;

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

        // 1. Theme Selection
        setupThemeSection(dialogView, activity, listener);

        // 2. Language Selection (Trilingual: ID / EN / JA)
        setupLanguageSection(dialogView, activity, listener, dialog);

        // 3. Safe Mode & Haptic
        setupSystemSection(dialogView, activity, listener);

        // 4. AI Subtitle & Model Selector
        setupAiSubtitleSection(dialogView, activity);

        // 5. About & Update
        setupAboutSection(dialogView, activity);

        dialog.show();
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
            updateRadios.run();
            if (listener != null) listener.onThemeChanged();
        });

        cardSakura.setOnClickListener(v -> {
            HapticHelper.performSelection(v);
            AnimeThemeManager.setTheme(activity, AnimeThemeManager.THEME_SAKURA);
            updateRadios.run();
            if (listener != null) listener.onThemeChanged();
        });

        cardCyber.setOnClickListener(v -> {
            HapticHelper.performSelection(v);
            AnimeThemeManager.setTheme(activity, AnimeThemeManager.THEME_CYBER);
            updateRadios.run();
            if (listener != null) listener.onThemeChanged();
        });

        cardGhibli.setOnClickListener(v -> {
            HapticHelper.performSelection(v);
            AnimeThemeManager.setTheme(activity, AnimeThemeManager.THEME_GHIBLI);
            updateRadios.run();
            if (listener != null) listener.onThemeChanged();
        });
    }

    private static void setupLanguageSection(View root, Activity activity, OnSettingsChangeListener listener, AlertDialog dialog) {
        TextView btnLangId = root.findViewById(R.id.btn_lang_id);
        TextView btnLangEn = root.findViewById(R.id.btn_lang_en);
        TextView btnLangJa = root.findViewById(R.id.btn_lang_ja);

        if (btnLangId == null || btnLangEn == null || btnLangJa == null) return;

        Runnable updateChips = () -> {
            String active = LocaleHelper.getLanguage(activity);
            boolean isId = LocaleHelper.LANG_ID.equalsIgnoreCase(active);
            boolean isEn = LocaleHelper.LANG_EN.equalsIgnoreCase(active);
            boolean isJa = LocaleHelper.LANG_JA.equalsIgnoreCase(active);

            btnLangId.setBackgroundResource(isId ? R.drawable.bg_chip_selected : R.drawable.bg_chip_frost_inactive);
            btnLangId.setTextColor(ContextCompat.getColor(activity, isId ? R.color.accent_glacial : R.color.text_frost_bright));

            btnLangEn.setBackgroundResource(isEn ? R.drawable.bg_chip_selected : R.drawable.bg_chip_frost_inactive);
            btnLangEn.setTextColor(ContextCompat.getColor(activity, isEn ? R.color.accent_glacial : R.color.text_frost_bright));

            btnLangJa.setBackgroundResource(isJa ? R.drawable.bg_chip_selected : R.drawable.bg_chip_frost_inactive);
            btnLangJa.setTextColor(ContextCompat.getColor(activity, isJa ? R.color.accent_glacial : R.color.text_frost_bright));
        };
        updateChips.run();

        View.OnClickListener langClickListener = v -> {
            HapticHelper.performSelection(v);
            String selectedLang = LocaleHelper.LANG_ID;
            if (v.getId() == R.id.btn_lang_en) {
                selectedLang = LocaleHelper.LANG_EN;
            } else if (v.getId() == R.id.btn_lang_ja) {
                selectedLang = LocaleHelper.LANG_JA;
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
    }

    private static void setupSystemSection(View root, Activity activity, OnSettingsChangeListener listener) {
        SwitchMaterial switchSafeMode = root.findViewById(R.id.switch_safe_mode);
        SwitchMaterial switchHaptic = root.findViewById(R.id.switch_haptic);

        switchSafeMode.setChecked(AppSettings.isSafeMode(activity));
        switchSafeMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            HapticHelper.performClick(buttonView);
            AppSettings.setSafeMode(activity, isChecked);
            if (listener != null) listener.onSafeModeChanged(isChecked);
            Toast.makeText(activity, isChecked ? "Safe Mode Aktif (Ecchi/Erotica disaring)" : "Safe Mode Nonaktif", Toast.LENGTH_SHORT).show();
        });

        switchHaptic.setChecked(AppSettings.isHapticEnabled(activity));
        switchHaptic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSettings.setHapticEnabled(activity, isChecked);
            if (isChecked) {
                HapticHelper.performHeavy(activity);
                Toast.makeText(activity, "Umpan Balik Haptic Aktif", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Umpan Balik Haptic Nonaktif", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void setupAiSubtitleSection(View root, Activity activity) {
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
            tvBadge.setText("Aktif: " + p.displayName);
        }

        String savedModel = AiSubtitleService.getModel(activity);
        if (savedModel != null && !savedModel.isEmpty()) {
            etModel.setText(savedModel);
        } else {
            AiSubtitleService.Provider p = AiSubtitleService.getActiveProvider(activity);
            etModel.setText(AiSubtitleService.getDefaultModel(p));
        }

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
                    tvBadge.setText("Terdeteksi: " + p.displayName);
                } else {
                    tvBadge.setText("Gemini / Groq / OpenAI / Ollama");
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
            Toast.makeText(activity, "Prompt direset ke default fansub anime", Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            String inputKey = etKey.getText().toString().trim();
            String inputPrompt = etPrompt.getText().toString().trim();
            String inputModel = etModel.getText().toString().trim();

            if (inputKey.isEmpty()) {
                Toast.makeText(activity, "Masukkan Kunci API atau URL Ollama", Toast.LENGTH_SHORT).show();
                return;
            }

            AiSubtitleService.saveKey(activity, inputKey);
            AiSubtitleService.saveCustomPrompt(activity, inputPrompt);
            AiSubtitleService.saveModel(activity, inputModel);

            AiSubtitleService.Provider p = AiSubtitleService.Provider.detect(inputKey);
            Toast.makeText(activity, "Pengaturan AI berhasil disimpan (" + p.displayName + ")", Toast.LENGTH_SHORT).show();
        });

        btnClear.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            AiSubtitleService.clearKey(activity);
            AiSubtitleService.clearCustomPrompt(activity);
            AiSubtitleService.clearModel(activity);

            etKey.setText("");
            etModel.setText(AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.GEMINI));
            etPrompt.setText(AiSubtitleService.DEFAULT_CUSTOM_PROMPT);
            tvBadge.setText("Gemini / Groq / OpenAI / Ollama");
            populateModelChips(layoutChips, activity, AiSubtitleService.Provider.GEMINI, etModel);

            Toast.makeText(activity, "Kunci API & model telah dibersihkan", Toast.LENGTH_SHORT).show();
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
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_frost_active);
            chip.setTextColor(ContextCompat.getColor(context, R.color.frost_void));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_frost_inactive);
            chip.setTextColor(ContextCompat.getColor(context, R.color.text_frost_bright));
        }
    }

    private static void setupAboutSection(View root, Activity activity) {
        TextView tvVersion = root.findViewById(R.id.tv_settings_app_version);
        if (tvVersion != null) {
            tvVersion.setText("Menatap v" + BuildConfig.VERSION_NAME);
        }

        Button btnCheckUpdate = root.findViewById(R.id.btn_settings_check_update);
        if (btnCheckUpdate != null) {
            btnCheckUpdate.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                AppUpdateManager.getInstance().checkUpdate(activity, true);
            });
        }
    }
}
