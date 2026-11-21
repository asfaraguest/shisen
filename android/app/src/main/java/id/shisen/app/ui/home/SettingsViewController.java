package id.shisen.app.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import id.shisen.app.BuildConfig;
import id.shisen.app.R;
import id.shisen.app.data.cache.FeedCache;
import id.shisen.app.data.subtitle.AiSubtitleService;
import id.shisen.app.ui.dialog.SettingsDialog;
import id.shisen.app.ui.player.PlayerKeys;
import id.shisen.app.util.AnimeThemeManager;
import id.shisen.app.util.AppSettings;
import id.shisen.app.util.AppUpdateManager;
import id.shisen.app.util.HapticHelper;
import id.shisen.app.util.LocaleHelper;
import id.shisen.app.util.SystemUiHelper;
import id.shisen.app.util.TvFocusHelper;

/**
 * Controller untuk Dedicated Settings View di MainActivity.
 * Menghandle pemilihan tema via Option Picker, default subtitle language,
 * bahasa antarmuka, safe mode, haptic, AI subtitle BYOK, dan dukungan Trakteer.
 */
public class SettingsViewController {

    private final MainActivity host;
    private final View root;

    // Section 1: Tema Tampilan
    private View cardSettingTheme;
    private View swatchCurrentTheme;
    private TextView tvCurrentThemeName;

    // Section 2: Bahasa & Subtitle
    private TextView btnLangId;
    private TextView btnLangEn;
    private TextView btnLangJa;
    private TextView btnLangEs;
    private View cardSettingDefaultSub;
    private TextView tvCurrentDefaultSubName;

    // Section 3: Keamanan & Haptic
    private SwitchMaterial switchSafeMode;
    private View containerHaptic;
    private SwitchMaterial switchHaptic;

    // Section 4: AI Subtitle (BYOK)
    private TextView tvAiStatus;
    private LinearLayout layoutAiChips;
    private EditText etAiKey;
    private EditText etAiModel;
    private View btnTogglePromptBox;
    private TextView tvTogglePromptIcon;
    private View containerPromptFoldable;
    private EditText etAiPrompt;
    private TextView btnResetPrompt;
    private TextView btnAiClear;
    private TextView btnAiSave;

    // Section 5: Sistem & Pembaruan
    private TextView btnTrakteer;
    private TextView btnCheckUpdate;
    private TextView btnClearCache;
    private TextView tvSystemInfo;
    private TextView tvAppVersion;

    public SettingsViewController(MainActivity host, View settingsRoot) {
        this.host = host;
        this.root = settingsRoot;
        initViews();
    }

    private void initViews() {
        if (root == null || host == null) return;

        // Theme
        cardSettingTheme = root.findViewById(R.id.card_setting_theme);
        swatchCurrentTheme = root.findViewById(R.id.swatch_current_theme);
        tvCurrentThemeName = root.findViewById(R.id.tv_current_theme_name);

        // Language & Subtitle
        btnLangId = root.findViewById(R.id.btn_lang_id);
        btnLangEn = root.findViewById(R.id.btn_lang_en);
        btnLangJa = root.findViewById(R.id.btn_lang_ja);
        btnLangEs = root.findViewById(R.id.btn_lang_es);
        cardSettingDefaultSub = root.findViewById(R.id.card_setting_default_sub);
        tvCurrentDefaultSubName = root.findViewById(R.id.tv_current_default_sub_name);

        // Safety & Haptic
        switchSafeMode = root.findViewById(R.id.switch_safe_mode);
        containerHaptic = root.findViewById(R.id.container_setting_haptic);
        switchHaptic = root.findViewById(R.id.switch_haptic);

        // AI
        tvAiStatus = root.findViewById(R.id.tv_settings_ai_status);
        layoutAiChips = root.findViewById(R.id.layout_settings_ai_chips);
        etAiKey = root.findViewById(R.id.et_settings_ai_key);
        etAiModel = root.findViewById(R.id.et_settings_ai_model);
        btnTogglePromptBox = root.findViewById(R.id.btn_toggle_prompt_box);
        tvTogglePromptIcon = root.findViewById(R.id.tv_toggle_prompt_icon);
        containerPromptFoldable = root.findViewById(R.id.container_prompt_foldable);
        etAiPrompt = root.findViewById(R.id.et_settings_ai_prompt);
        btnResetPrompt = root.findViewById(R.id.btn_settings_ai_reset_prompt);
        btnAiClear = root.findViewById(R.id.btn_settings_ai_clear);
        btnAiSave = root.findViewById(R.id.btn_settings_ai_save);

        // Support & Update
        btnTrakteer = root.findViewById(R.id.btn_settings_trakteer);
        btnCheckUpdate = root.findViewById(R.id.btn_settings_check_update);
        btnClearCache = root.findViewById(R.id.btn_settings_clear_cache);
        tvSystemInfo = root.findViewById(R.id.tv_settings_system_info);
        tvAppVersion = root.findViewById(R.id.tv_settings_app_version);

        setupThemeOption();
        setupLanguageChips();
        setupDefaultSubtitleOption();
        setupSafetyAndHaptic();
        setupAiSubtitle();
        setupSupportAndUpdates();
        setupTvNavigation();

        refreshUI();
    }

    // ==========================================
    // 1. TEMA TAMPILAN (OPTION PICKER, NO RADIO)
    // ==========================================
    private void setupThemeOption() {
        if (cardSettingTheme != null) {
            cardSettingTheme.setOnClickListener(v -> {
                HapticHelper.performSelection(v);
                showThemePickerDialog();
            });
        }
    }

    private void showThemePickerDialog() {
        if (host == null || host.isFinishing() || host.isDestroyed()) return;

        final String[] themeKeys = {
                AnimeThemeManager.THEME_SHINKAI,
                AnimeThemeManager.THEME_AINCRAD,
                AnimeThemeManager.THEME_SAKURA,
                AnimeThemeManager.THEME_CYBER,
                AnimeThemeManager.THEME_GHIBLI
        };
        final int[] themeTitles = {
                R.string.settings_theme_shinkai_title,
                R.string.settings_theme_aincrad_title,
                R.string.settings_theme_sakura_title,
                R.string.settings_theme_cyber_title,
                R.string.settings_theme_ghibli_title
        };
        final int[] themeDescs = {
                R.string.settings_theme_shinkai_desc,
                R.string.settings_theme_aincrad_desc,
                R.string.settings_theme_sakura_desc,
                R.string.settings_theme_cyber_desc,
                R.string.settings_theme_ghibli_desc
        };
        final int[] themeColors = {0xFF38BDF8, 0xFFF59E0B, 0xFFFB7185, 0xFF00F0FF, 0xFF10B981};

        View dialogView = LayoutInflater.from(host).inflate(R.layout.dialog_setting_option_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(host)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_option_picker_title);
        if (tvTitle != null) {
            tvTitle.setText(R.string.settings_theme_picker_title);
        }

        LinearLayout container = dialogView.findViewById(R.id.layout_option_items_container);
        if (container != null) {
            container.removeAllViews();
            String current = AnimeThemeManager.getTheme(host);

            for (int i = 0; i < themeKeys.length; i++) {
                final String targetKey = themeKeys[i];
                View row = LayoutInflater.from(host).inflate(R.layout.item_setting_option, container, false);

                View swatch = row.findViewById(R.id.view_option_swatch);
                TextView title = row.findViewById(R.id.tv_option_title);
                TextView desc = row.findViewById(R.id.tv_option_desc);
                TextView check = row.findViewById(R.id.tv_option_check);

                if (swatch != null) swatch.setBackgroundColor(themeColors[i]);
                if (title != null) title.setText(themeTitles[i]);
                if (desc != null) desc.setText(themeDescs[i]);

                boolean isSelected = targetKey.equals(current);
                if (check != null) check.setVisibility(isSelected ? View.VISIBLE : View.GONE);

                if (PlayerKeys.isTv()) {
                    TvFocusHelper.attachFocusScale(row, 1.04f);
                }

                row.setOnClickListener(v -> {
                    HapticHelper.performSelection(v);
                    AnimeThemeManager.setTheme(host, targetKey);
                    SystemUiHelper.applySystemBarsTheme(host, targetKey);
                    host.applyCurrentTheme();
                    refreshUI();
                    dialog.dismiss();
                });

                container.addView(row);
            }
        }

        View btnClose = dialogView.findViewById(R.id.btn_close_option_picker);
        if (btnClose != null) {
            if (PlayerKeys.isTv()) TvFocusHelper.attachFocusScale(btnClose, 1.08f);
            btnClose.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                dialog.dismiss();
            });
        }

        SystemUiHelper.setupCustomDialogBounds(dialog, host);
        dialog.show();

        if (PlayerKeys.isTv() && container != null && container.getChildCount() > 0) {
            View toFocus = null;
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                View check = child.findViewById(R.id.tv_option_check);
                if (check != null && check.getVisibility() == View.VISIBLE) {
                    toFocus = child;
                    break;
                }
            }
            if (toFocus == null) toFocus = container.getChildAt(0);
            final View finalFocus = toFocus;
            if (finalFocus != null) finalFocus.post(finalFocus::requestFocus);
        }
    }

    // ==========================================
    // 2. BAHASA ANTARMUKA & DEFAULT SUBTITLE
    // ==========================================
    private void setupLanguageChips() {
        View.OnClickListener langListener = v -> {
            HapticHelper.performSelection(v);
            String selectedLang = LocaleHelper.LANG_ID;
            if (v.getId() == R.id.btn_lang_en) {
                selectedLang = LocaleHelper.LANG_EN;
            } else if (v.getId() == R.id.btn_lang_ja) {
                selectedLang = LocaleHelper.LANG_JA;
            } else if (v.getId() == R.id.btn_lang_es) {
                selectedLang = LocaleHelper.LANG_ES;
            }

            if (!selectedLang.equalsIgnoreCase(LocaleHelper.getLanguage(host))) {
                LocaleHelper.setLocale(host, selectedLang);
                host.recreate();
            }
        };

        if (btnLangId != null) btnLangId.setOnClickListener(langListener);
        if (btnLangEn != null) btnLangEn.setOnClickListener(langListener);
        if (btnLangJa != null) btnLangJa.setOnClickListener(langListener);
        if (btnLangEs != null) btnLangEs.setOnClickListener(langListener);
    }

    private void setupDefaultSubtitleOption() {
        if (cardSettingDefaultSub != null) {
            cardSettingDefaultSub.setOnClickListener(v -> {
                HapticHelper.performSelection(v);
                showDefaultSubtitlePickerDialog();
            });
        }
    }

    private void showDefaultSubtitlePickerDialog() {
        if (host == null || host.isFinishing() || host.isDestroyed()) return;

        final String[] subKeys = {
                AppSettings.SUB_LANG_ID,
                AppSettings.SUB_LANG_EN,
                AppSettings.SUB_LANG_OFF
        };
        final int[] subTitles = {
                R.string.settings_sub_lang_id,
                R.string.settings_sub_lang_en,
                R.string.settings_sub_lang_off
        };
        final String[] subDescs = {
                "Prioritas subtitle resmi atau otomatis terjemahan Smart Merge ID",
                "Memuat track teks bahasa Inggris resmi bawaan secara langsung",
                "Subtitle dalam kondisi nonaktif saat video pertama kali diputar"
        };
        final int[] subColors = {0xFF38BDF8, 0xFF10B981, 0xFFEF4444};

        View dialogView = LayoutInflater.from(host).inflate(R.layout.dialog_setting_option_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(host)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_option_picker_title);
        if (tvTitle != null) {
            tvTitle.setText(R.string.settings_default_sub_picker_title);
        }

        LinearLayout container = dialogView.findViewById(R.id.layout_option_items_container);
        if (container != null) {
            container.removeAllViews();
            String current = AppSettings.getDefaultSubtitleLang(host);

            for (int i = 0; i < subKeys.length; i++) {
                final String targetKey = subKeys[i];
                View row = LayoutInflater.from(host).inflate(R.layout.item_setting_option, container, false);

                View swatch = row.findViewById(R.id.view_option_swatch);
                TextView title = row.findViewById(R.id.tv_option_title);
                TextView desc = row.findViewById(R.id.tv_option_desc);
                TextView check = row.findViewById(R.id.tv_option_check);

                if (swatch != null) swatch.setBackgroundColor(subColors[i]);
                if (title != null) title.setText(subTitles[i]);
                if (desc != null) desc.setText(subDescs[i]);

                boolean isSelected = targetKey.equalsIgnoreCase(current);
                if (check != null) check.setVisibility(isSelected ? View.VISIBLE : View.GONE);

                if (PlayerKeys.isTv()) {
                    TvFocusHelper.attachFocusScale(row, 1.04f);
                }

                row.setOnClickListener(v -> {
                    HapticHelper.performSelection(v);
                    AppSettings.setDefaultSubtitleLang(host, targetKey);
                    refreshUI();
                    dialog.dismiss();
                });

                container.addView(row);
            }
        }

        View btnClose = dialogView.findViewById(R.id.btn_close_option_picker);
        if (btnClose != null) {
            if (PlayerKeys.isTv()) TvFocusHelper.attachFocusScale(btnClose, 1.08f);
            btnClose.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                dialog.dismiss();
            });
        }

        SystemUiHelper.setupCustomDialogBounds(dialog, host);
        dialog.show();

        if (PlayerKeys.isTv() && container != null && container.getChildCount() > 0) {
            View toFocus = null;
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                View check = child.findViewById(R.id.tv_option_check);
                if (check != null && check.getVisibility() == View.VISIBLE) {
                    toFocus = child;
                    break;
                }
            }
            if (toFocus == null) toFocus = container.getChildAt(0);
            final View finalFocus = toFocus;
            if (finalFocus != null) finalFocus.post(finalFocus::requestFocus);
        }
    }

    // ==========================================
    // 3. KEAMANAN & HAPTIC
    // ==========================================
    private void setupSafetyAndHaptic() {
        if (switchSafeMode != null) {
            switchSafeMode.setChecked(AppSettings.isSafeMode(host));
            switchSafeMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                HapticHelper.performClick(buttonView);
                AppSettings.setSafeMode(host, isChecked);
                host.onSafeModeChanged(isChecked);
                Toast.makeText(host, host.getString(isChecked
                        ? R.string.settings_toast_safe_mode_on
                        : R.string.settings_toast_safe_mode_off), Toast.LENGTH_SHORT).show();
            });
        }

        if (containerHaptic != null) {
            if (PlayerKeys.isTv()) {
                containerHaptic.setVisibility(View.GONE);
            } else {
                containerHaptic.setVisibility(View.VISIBLE);
                if (switchHaptic != null) {
                    switchHaptic.setChecked(AppSettings.isHapticEnabled(host));
                    switchHaptic.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        AppSettings.setHapticEnabled(host, isChecked);
                        if (isChecked) {
                            HapticHelper.performHeavy(host);
                            Toast.makeText(host, host.getString(R.string.settings_toast_haptic_on), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(host, host.getString(R.string.settings_toast_haptic_off), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
    }

    // ==========================================
    // 4. AI SUBTITLE (BYOK)
    // ==========================================
    private void setupAiSubtitle() {
        if (etAiKey == null || etAiModel == null || etAiPrompt == null) return;

        String currentKey = AiSubtitleService.getKey(host);
        if (currentKey != null && !currentKey.isEmpty()) {
            etAiKey.setText(currentKey);
            AiSubtitleService.Provider p = AiSubtitleService.Provider.detect(currentKey);
            if (tvAiStatus != null) {
                tvAiStatus.setText(host.getString(R.string.settings_ai_badge_active, p.displayName));
            }
        }

        String savedModel = AiSubtitleService.getModel(host);
        AiSubtitleService.Provider activeProv = AiSubtitleService.getActiveProvider(host);
        etAiModel.setText(AiSubtitleService.getEffectiveModel(activeProv, savedModel));
        etAiPrompt.setText(AiSubtitleService.getCustomPrompt(host));

        SettingsDialog.populateModelChips(layoutAiChips, host, activeProv, etAiModel);

        etAiKey.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String input = s != null ? s.toString().trim() : "";
                AiSubtitleService.Provider p = AiSubtitleService.Provider.detect(input);
                if (tvAiStatus != null) {
                    if (!input.isEmpty()) {
                        tvAiStatus.setText(host.getString(R.string.settings_ai_badge_detected, p.displayName));
                    } else {
                        tvAiStatus.setText(host.getString(R.string.settings_ai_badge_none));
                    }
                }
                SettingsDialog.populateModelChips(layoutAiChips, host, p, etAiModel);
                if (etAiModel.getText().toString().trim().isEmpty()) {
                    etAiModel.setText(AiSubtitleService.getDefaultModel(p));
                }
            }
        });

        if (btnTogglePromptBox != null) {
            btnTogglePromptBox.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                togglePromptBox();
            });
        }

        if (btnResetPrompt != null) {
            btnResetPrompt.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                etAiPrompt.setText(AiSubtitleService.DEFAULT_CUSTOM_PROMPT);
                AiSubtitleService.saveCustomPrompt(host, AiSubtitleService.DEFAULT_CUSTOM_PROMPT);
                Toast.makeText(host, host.getString(R.string.settings_toast_prompt_reset), Toast.LENGTH_SHORT).show();
            });
        }

        if (btnAiSave != null) {
            btnAiSave.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                saveAiConfiguration();
            });
        }

        if (btnAiClear != null) {
            btnAiClear.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                AiSubtitleService.clearKey(host);
                AiSubtitleService.saveModel(host, "");
                AiSubtitleService.saveCustomPrompt(host, AiSubtitleService.DEFAULT_CUSTOM_PROMPT);

                etAiKey.setText("");
                etAiModel.setText(AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.GEMINI));
                etAiPrompt.setText(AiSubtitleService.DEFAULT_CUSTOM_PROMPT);
                if (tvAiStatus != null) tvAiStatus.setText(host.getString(R.string.settings_ai_badge_none));
                SettingsDialog.populateModelChips(layoutAiChips, host, AiSubtitleService.Provider.GEMINI, etAiModel);

                Toast.makeText(host, host.getString(R.string.settings_toast_ai_cleared), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void togglePromptBox() {
        if (containerPromptFoldable == null) return;
        boolean expand = containerPromptFoldable.getVisibility() != View.VISIBLE;
        if (expand) {
            containerPromptFoldable.setVisibility(View.VISIBLE);
            containerPromptFoldable.setAlpha(0f);
            containerPromptFoldable.animate().alpha(1f).setDuration(200).start();
            if (tvTogglePromptIcon != null) tvTogglePromptIcon.setText("▲");
            if (etAiPrompt != null) etAiPrompt.requestFocus();
        } else {
            containerPromptFoldable.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                containerPromptFoldable.setVisibility(View.GONE);
            }).start();
            if (tvTogglePromptIcon != null) tvTogglePromptIcon.setText("▼");
        }
    }

    private void saveAiConfiguration() {
        if (etAiKey == null || etAiModel == null || etAiPrompt == null || host == null) return;
        String inputKey = etAiKey.getText().toString().trim();
        String inputPrompt = etAiPrompt.getText().toString().trim();
        String inputModel = etAiModel.getText().toString().trim();

        if (inputKey.isEmpty()) {
            Toast.makeText(host, host.getString(R.string.settings_toast_key_required), Toast.LENGTH_SHORT).show();
            return;
        }

        boolean saved = AiSubtitleService.saveKey(host, inputKey);
        if (!saved) {
            Toast.makeText(host, R.string.secure_store_failed, Toast.LENGTH_LONG).show();
        }
        if (!inputModel.isEmpty()) {
            AiSubtitleService.saveModel(host, inputModel);
        }
        if (!inputPrompt.isEmpty()) {
            AiSubtitleService.saveCustomPrompt(host, inputPrompt);
        }

        AiSubtitleService.Provider p = AiSubtitleService.Provider.detect(inputKey);
        if (tvAiStatus != null) {
            tvAiStatus.setText(host.getString(R.string.settings_ai_badge_active, p.displayName));
        }
        Toast.makeText(host, host.getString(R.string.settings_toast_ai_saved, p.displayName), Toast.LENGTH_SHORT).show();
    }

    // ==========================================
    // 5. SISTEM & PEMBARUAN
    // ==========================================
    private void setupSupportAndUpdates() {
        if (btnTrakteer != null) {
            btnTrakteer.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://trakteer.id/bagus_azizul"));
                    host.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(host, "Gagal membuka browser", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnCheckUpdate != null) {
            btnCheckUpdate.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                AppUpdateManager.getInstance().checkUpdate(host, true);
            });
        }

        if (btnClearCache != null) {
            btnClearCache.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                FeedCache.getInstance(host).clearAllCache();
                Toast.makeText(host, host.getString(R.string.settings_cache_cleared), Toast.LENGTH_SHORT).show();
            });
        }

        if (tvAppVersion != null) {
            String ver = BuildConfig.APP_VERSION_NAME;
            tvAppVersion.setText(host.getString(R.string.settings_app_version) + " v" + ver + " (Pure Java Native)");
        }
    }

    // ==========================================
    // 6. TV REMOTE D-PAD NAVIGATION
    // ==========================================
    private void setupTvNavigation() {
        if (!PlayerKeys.isTv()) return;

        View[] focusables = {
                btnTrakteer,
                cardSettingTheme,
                btnLangId, btnLangEn, btnLangJa, btnLangEs,
                cardSettingDefaultSub,
                switchSafeMode,
                etAiKey, etAiModel,
                btnTogglePromptBox, etAiPrompt, btnResetPrompt,
                btnAiClear, btnAiSave,
                btnCheckUpdate, btnClearCache
        };

        for (View item : focusables) {
            if (item != null) {
                item.setFocusable(true);
                TvFocusHelper.attachFocusScale(item, 1.04f);
            }
        }

        View.OnKeyListener backToRailListener = (v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (host != null && host.menuItemSettings != null) {
                    host.menuItemSettings.requestFocus();
                    return true;
                }
            }
            return false;
        };

        // Pasang listener kembali ke rail hanya pada view yang berada di sisi paling kiri
        View[] leftEdgeItems = {
                btnTrakteer,
                cardSettingTheme,
                btnLangId,
                cardSettingDefaultSub,
                switchSafeMode,
                etAiKey, etAiModel,
                btnTogglePromptBox, etAiPrompt, btnResetPrompt,
                btnCheckUpdate, btnClearCache
        };

        for (View item : leftEdgeItems) {
            if (item != null) {
                item.setOnKeyListener(backToRailListener);
            }
        }
    }

    // ==========================================
    // 7. DYNAMIC THEMING & UI REFRESH
    // ==========================================
    public void refreshUI() {
        if (host == null || root == null) return;

        // Current Theme Swatch & Label
        String currentTheme = AnimeThemeManager.getTheme(host);
        if (swatchCurrentTheme != null) {
            swatchCurrentTheme.setBackgroundColor(AnimeThemeManager.getPrimaryAccentColor(host));
        }
        if (tvCurrentThemeName != null) {
            int titleRes = R.string.settings_theme_shinkai_title;
            switch (currentTheme) {
                case AnimeThemeManager.THEME_AINCRAD: titleRes = R.string.settings_theme_aincrad_title; break;
                case AnimeThemeManager.THEME_SAKURA: titleRes = R.string.settings_theme_sakura_title; break;
                case AnimeThemeManager.THEME_CYBER: titleRes = R.string.settings_theme_cyber_title; break;
                case AnimeThemeManager.THEME_GHIBLI: titleRes = R.string.settings_theme_ghibli_title; break;
            }
            tvCurrentThemeName.setText(titleRes);
        }

        // Current Language Chips
        String activeLang = LocaleHelper.getLanguage(host);
        boolean isId = LocaleHelper.LANG_ID.equalsIgnoreCase(activeLang);
        boolean isEn = LocaleHelper.LANG_EN.equalsIgnoreCase(activeLang);
        boolean isJa = LocaleHelper.LANG_JA.equalsIgnoreCase(activeLang);
        boolean isEs = LocaleHelper.LANG_ES.equalsIgnoreCase(activeLang);

        TextView[] chips = {btnLangId, btnLangEn, btnLangJa, btnLangEs};
        boolean[] states = {isId, isEn, isJa, isEs};
        int selectedColor = Color.parseColor("#080C14");
        int normalColor = ContextCompat.getColor(host, R.color.text_frost_bright);

        for (int i = 0; i < chips.length; i++) {
            if (chips[i] != null) {
                AnimeThemeManager.applyBackgroundPreservingPadding(
                        chips[i], AnimeThemeManager.createChipDrawable(host, states[i]));
                chips[i].setTextColor(states[i] ? selectedColor : normalColor);
            }
        }

        // Current Default Subtitle
        String defaultSub = AppSettings.getDefaultSubtitleLang(host);
        if (tvCurrentDefaultSubName != null) {
            if (AppSettings.SUB_LANG_OFF.equalsIgnoreCase(defaultSub)) {
                tvCurrentDefaultSubName.setText(R.string.settings_sub_lang_off);
            } else if (AppSettings.SUB_LANG_EN.equalsIgnoreCase(defaultSub)) {
                tvCurrentDefaultSubName.setText(R.string.settings_sub_lang_en);
            } else {
                tvCurrentDefaultSubName.setText(R.string.settings_sub_lang_id);
            }
        }

        // Safe mode & Haptic switches
        if (switchSafeMode != null) switchSafeMode.setChecked(AppSettings.isSafeMode(host));
        if (switchHaptic != null) switchHaptic.setChecked(AppSettings.isHapticEnabled(host));

        applyTheme();
    }

    public void applyTheme() {
        if (host == null || root == null) return;

        int accent = AnimeThemeManager.getPrimaryAccentColor(host);
        int border = AnimeThemeManager.getBorderColor(host);
        android.content.res.ColorStateList accentTint = android.content.res.ColorStateList.valueOf(accent);

        if (switchSafeMode != null) switchSafeMode.setThumbTintList(accentTint);
        if (switchHaptic != null) switchHaptic.setThumbTintList(accentTint);
        if (etAiModel != null) etAiModel.setTextColor(accent);
        if (tvTogglePromptIcon != null) tvTogglePromptIcon.setTextColor(accent);
        if (btnResetPrompt != null) btnResetPrompt.setTextColor(accent);
        if (btnAiSave != null) btnAiSave.setTextColor(Color.parseColor("#080C14"));
    }

    public void focusFirstItem() {
        if (btnTrakteer != null) {
            btnTrakteer.requestFocus();
        } else if (cardSettingTheme != null) {
            cardSettingTheme.requestFocus();
        }
    }
}
