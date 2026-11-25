package id.shisen.app.ui;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.view.animation.PathInterpolatorCompat;
import id.shisen.app.R;
import id.shisen.app.ui.home.MainActivity;
import id.shisen.app.util.HapticHelper;

/**
 * Pengontrol navigasi khusus varian Mobile:
 * Mengatur Floating Bottom Dock Bar, Floating Action Button (FAB),
 * folded pop-up menu, dan koordinasi animasi fluid yang bebas overlap.
 */
public final class DeviceNav {

    private static final Interpolator FLUID_ENTER = PathInterpolatorCompat.create(0.16f, 1.0f, 0.3f, 1.0f);
    private static final Interpolator FLUID_EXIT = PathInterpolatorCompat.create(0.4f, 0.0f, 0.2f, 1.0f);

    private final MainActivity host;

    private View scrimFoldedMenu;
    private ScrollView scrollFoldedMenu;
    private LinearLayout layoutFoldedMenu;
    private FrameLayout fabMenuTrigger;
    private ImageView ivFabIcon;
    private boolean isFoldedMenuOpen = false;

    private View layoutSideRail;
    private View railDividerSettings;
    private ScrollView scrollSidebarMenu;
    private int lastSidebarViewportHeight = -1;
    private LinearLayout layoutPagination;

    public DeviceNav(MainActivity host) {
        this.host = host;
    }

    public boolean hasSidebar() {
        return layoutSideRail != null;
    }

    public void init(View root) {
        if (root == null) return;

        layoutSideRail = root.findViewById(R.id.layout_tv_side_rail);
        railDividerSettings = root.findViewById(R.id.rail_divider_settings);
        scrollSidebarMenu = root.findViewById(R.id.scroll_sidebar_menu);
        lastSidebarViewportHeight = -1;
        if (layoutSideRail != null) {
            View tvLogo = root.findViewById(R.id.tv_app_logo);
            View tvKanji = root.findViewById(R.id.tv_app_logo_kanji);
            if (tvLogo != null) tvLogo.setVisibility(View.GONE);
            if (tvKanji != null) tvKanji.setVisibility(View.GONE);
        }
        if (scrollSidebarMenu != null) {
            scrollSidebarMenu.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                int h = bottom - top;
                if (h > 0 && h != lastSidebarViewportHeight && host != null) {
                    lastSidebarViewportHeight = h;
                    updateFoldedMenuHighlight(host.getCurrentTab());
                }
            });
        }
        scrimFoldedMenu = root.findViewById(R.id.scrim_folded_menu);
        scrollFoldedMenu = root.findViewById(R.id.scroll_folded_menu);
        layoutFoldedMenu = root.findViewById(R.id.layout_folded_menu);
        fabMenuTrigger = root.findViewById(R.id.fab_menu_trigger);
        ivFabIcon = root.findViewById(R.id.iv_fab_icon);
        layoutPagination = root.findViewById(R.id.layout_pagination);
        isFoldedMenuOpen = false;

        if (fabMenuTrigger != null) {
            fabMenuTrigger.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                toggleFoldedMenu();
            });
        }

        if (scrimFoldedMenu != null) {
            scrimFoldedMenu.setOnClickListener(v -> {
                if (isFoldedMenuOpen) {
                    setFoldedMenuOpen(false);
                }
            });
        }
    }

    public void applyInsets(int topInset, int bottomInset, float density) {
        applyInsets(topInset, bottomInset, 0, 0, density);
    }

    public void applyInsets(int topInset, int bottomInset, int leftInset, int rightInset, float density) {
        if (layoutSideRail != null) {
            int padH = Math.round(6 * density);
            int padTop = Math.round(10 * density) + topInset;
            int padBottom = Math.round(10 * density) + bottomInset;
            layoutSideRail.setPadding(padH + leftInset, padTop, padH, padBottom);
            ViewGroup.LayoutParams lp = layoutSideRail.getLayoutParams();
            if (lp != null) {
                int targetW = Math.round(60 * density) + leftInset;
                if (lp.width != targetW) {
                    lp.width = targetW;
                    layoutSideRail.setLayoutParams(lp);
                }
            }
            return;
        }

        int baseBottomMargin = (int) (18 * density) + bottomInset;
        int baseEndMargin = (int) (16 * density) + rightInset;

        if (fabMenuTrigger != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fabMenuTrigger.getLayoutParams();
            if (lp != null) {
                lp.bottomMargin = baseBottomMargin;
                lp.setMarginEnd(baseEndMargin);
                fabMenuTrigger.setLayoutParams(lp);
            }
        }
        if (layoutPagination != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutPagination.getLayoutParams();
            if (lp != null) {
                lp.bottomMargin = baseBottomMargin;
                layoutPagination.setLayoutParams(lp);
            }
        }

        int menuBottomMargin = baseBottomMargin + (int) (48 * density) + (int) (10 * density);

        boolean isLand = host != null && host.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;

        if (scrollFoldedMenu != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) scrollFoldedMenu.getLayoutParams();
            if (lp != null) {
                lp.bottomMargin = menuBottomMargin;
                lp.setMarginEnd(baseEndMargin);
                if (isLand) {
                    lp.height = (int) (210 * density);
                } else {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                scrollFoldedMenu.setLayoutParams(lp);
            }
        } else if (layoutFoldedMenu != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutFoldedMenu.getLayoutParams();
            if (lp != null) {
                lp.bottomMargin = menuBottomMargin;
                lp.setMarginEnd(baseEndMargin);
                layoutFoldedMenu.setLayoutParams(lp);
            }
        }
    }

    public void onTabChanged(MainActivity.MainTab tab) {
        if (layoutSideRail == null) {
            setFoldedMenuOpen(false);
        }
        updateFoldedMenuHighlight(tab);
    }

    public void applyTheme() {
        if (host == null) return;
        int accent = id.shisen.app.util.AnimeThemeManager.getPrimaryAccentColor(host);
        if (layoutSideRail != null) {
            id.shisen.app.util.AnimeThemeManager.applyBackgroundPreservingPadding(
                    layoutSideRail,
                    id.shisen.app.util.AnimeThemeManager.createSaoSideRailDrawable(host));
            View tvLogo = host.findViewById(R.id.tv_app_logo);
            View tvKanji = host.findViewById(R.id.tv_app_logo_kanji);
            if (tvLogo != null) tvLogo.setVisibility(View.GONE);
            if (tvKanji != null) tvKanji.setVisibility(View.GONE);
        }
        if (railDividerSettings != null) {
            railDividerSettings.setBackgroundColor(id.shisen.app.util.AnimeThemeManager.getSaoHairlineColor());
        }
        if (scrimFoldedMenu != null && id.shisen.app.util.AnimeThemeManager.isMobileSaoEnabled()) {
            scrimFoldedMenu.setBackgroundColor(0xC40E1014);
        }
        if (fabMenuTrigger != null) {
            id.shisen.app.util.AnimeThemeManager.applyBackgroundPreservingPadding(
                    fabMenuTrigger,
                    id.shisen.app.util.AnimeThemeManager.createFabTriggerDrawable(host));
        }
        if (ivFabIcon != null) {
            ivFabIcon.setImageTintList(android.content.res.ColorStateList.valueOf(accent));
        }
        if (host.menuItemSettings != null) {
            id.shisen.app.util.AnimeThemeManager.applyBackgroundPreservingPadding(
                    host.menuItemSettings,
                    id.shisen.app.util.AnimeThemeManager.createMenuPillDrawable(host, false));
            tintMenuIcon(host.menuItemSettings, accent, false);
        }
        updateFoldedMenuHighlight(host.getCurrentTab());
    }

    private void tintMenuIcon(View item, int accentColor, boolean isActive) {
        if (!(item instanceof ViewGroup) || host == null) return;
        ViewGroup vg = (ViewGroup) item;
        boolean sao = id.shisen.app.util.AnimeThemeManager.isMobileSaoEnabled();
        int iconTint = (sao && !isActive) ? 0xFFCBD5E1 : accentColor;
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            if (child instanceof ImageView) {
                ImageView iv = (ImageView) child;
                iv.setImageTintList(android.content.res.ColorStateList.valueOf(iconTint));
                if (sao) {
                    float d = host.getResources().getDisplayMetrics().density;
                    int badgeSize = Math.round(24f * d);
                    ViewGroup.LayoutParams lp = iv.getLayoutParams();
                    if (lp != null && lp.width != badgeSize) {
                        lp.width = badgeSize;
                        lp.height = badgeSize;
                        iv.setLayoutParams(lp);
                    }
                    int pad = Math.round(4f * d);
                    iv.setPadding(pad, pad, pad, pad);
                    int fill = isActive
                            ? id.shisen.app.util.AnimeThemeManager.withAlpha(accentColor, 0x24)
                            : id.shisen.app.util.AnimeThemeManager.getSaoElevatedColor();
                    int stroke = isActive
                            ? accentColor
                            : id.shisen.app.util.AnimeThemeManager.getSaoHairlineColor();
                    iv.setBackground(id.shisen.app.util.AnimeThemeManager.createSaoSquareBadgeDrawable(
                            host, fill, stroke, 1f));
                }
                break;
            }
        }
    }

    private void updateFoldedMenuHighlight(MainActivity.MainTab currentTab) {
        if (host == null) return;
        View[] items = {
                host.menuItemMusim, host.menuItemAiring, host.menuItemJadwal,
                host.menuItemRiwayat, host.menuItemFavorit, host.menuItemKatalog, host.menuItemGenre,
                host.menuItemSettings
        };
        TextView[] titles = {
                host.tvMenuMusim, host.tvMenuAiring, host.tvMenuJadwal,
                host.tvMenuRiwayat, host.tvMenuFavorit, host.tvMenuKatalog, host.tvMenuGenre,
                host.tvMenuSettings
        };
        MainActivity.MainTab[] tabEnums = {
                MainActivity.MainTab.MUSIM, MainActivity.MainTab.AIRING, MainActivity.MainTab.JADWAL,
                MainActivity.MainTab.RIWAYAT, MainActivity.MainTab.FAVORIT, MainActivity.MainTab.KATALOG, MainActivity.MainTab.GENRE,
                MainActivity.MainTab.PENGATURAN
        };

        int activeColor = id.shisen.app.util.AnimeThemeManager.getPrimaryAccentColor(host);
        int inactiveColor = ContextCompat.getColor(host, R.color.text_frost_bright);

        boolean collapsedSidebar = (layoutSideRail != null);
        float d = host.getResources().getDisplayMetrics().density;
        int compactH = Math.round(40f * d);
        int scrollH = scrollSidebarMenu != null ? scrollSidebarMenu.getHeight() : 0;
        int activeH = Math.max(Math.round(120f * d), scrollH);
        int marginBottom = Math.round(4f * d);
        int padH = Math.round(4f * d);
        int padV = Math.round(8f * d);
        View activeScrollItem = null;

        for (int i = 0; i < items.length; i++) {
            if (items[i] == null || titles[i] == null) continue;
            boolean isActive = (tabEnums[i] == currentTab);

            if (collapsedSidebar) {
                boolean isScrollItem = (items[i] != host.menuItemSettings);
                if (isScrollItem) {
                    int targetH = isActive ? activeH : compactH;
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) items[i].getLayoutParams();
                    if (lp != null && (lp.height != targetH || lp.bottomMargin != marginBottom)) {
                        lp.height = targetH;
                        lp.bottomMargin = marginBottom;
                        items[i].setLayoutParams(lp);
                    }
                    items[i].setPadding(padH, isActive ? padV : 0, padH, isActive ? padV : 0);
                    titles[i].setVisibility(isActive ? View.VISIBLE : View.GONE);
                    if (titles[i] instanceof VerticalTextView) {
                        ((VerticalTextView) titles[i]).setVerticalText(true);
                    }
                    if (isActive) {
                        titles[i].setMaxHeight(Math.max(Math.round(60f * d), activeH - Math.round(52f * d)));
                        activeScrollItem = items[i];
                    }
                } else {
                    titles[i].setVisibility(View.GONE);
                }
            }

            id.shisen.app.util.AnimeThemeManager.applyBackgroundPreservingPadding(
                    items[i],
                    id.shisen.app.util.AnimeThemeManager.createMenuPillDrawable(host, isActive));
            tintMenuIcon(items[i], activeColor, isActive);
            titles[i].setTextColor(isActive ? activeColor : inactiveColor);
            titles[i].setTypeface(null, isActive ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }

        if (collapsedSidebar && scrollSidebarMenu != null && activeScrollItem != null) {
            final View target = activeScrollItem;
            scrollSidebarMenu.post(() -> scrollSidebarMenu.smoothScrollTo(0, target.getTop()));
        }
    }

    public boolean handleBackPressed() {
        if (isFoldedMenuOpen) {
            setFoldedMenuOpen(false);
            return true;
        }
        return false;
    }

    public int getGridSpan(int widthDp) {
        int effectiveWidth = (layoutSideRail != null) ? Math.max(320, widthDp - 60) : widthDp;
        if (layoutSideRail != null) {
            if (effectiveWidth >= 720) return 5;
            if (effectiveWidth >= 500) return 4;
            return 3;
        }
        if (effectiveWidth >= 920) return 5;
        if (effectiveWidth >= 640) return 4;
        if (effectiveWidth >= 480) return 3;
        return 2;
    }

    public void toggleFoldedMenu() {
        setFoldedMenuOpen(!isFoldedMenuOpen);
    }

    public void setFoldedMenuOpen(boolean open) {
        if (isFoldedMenuOpen == open) return;
        isFoldedMenuOpen = open;
        HapticHelper.performTick();

        if (scrimFoldedMenu != null) {
            if (open) {
                scrimFoldedMenu.setVisibility(View.VISIBLE);
                scrimFoldedMenu.setAlpha(0f);
                scrimFoldedMenu.animate()
                        .alpha(1.0f)
                        .setDuration(240)
                        .setInterpolator(FLUID_ENTER)
                        .start();
            } else {
                scrimFoldedMenu.animate()
                        .alpha(0f)
                        .setDuration(190)
                        .setInterpolator(FLUID_EXIT)
                        .withEndAction(() -> scrimFoldedMenu.setVisibility(View.GONE))
                        .start();
            }
        }

        View targetMenu = scrollFoldedMenu != null ? scrollFoldedMenu : layoutFoldedMenu;
        if (targetMenu != null) {
            if (open) {
                targetMenu.setVisibility(View.VISIBLE);
                targetMenu.post(() -> {
                    targetMenu.setPivotX(targetMenu.getWidth());
                    targetMenu.setPivotY(targetMenu.getHeight());
                });
                targetMenu.setAlpha(0f);
                targetMenu.setScaleX(0.85f);
                targetMenu.setScaleY(0.85f);
                targetMenu.setTranslationY(40f);
                targetMenu.animate()
                        .alpha(1.0f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationY(0f)
                        .setDuration(280)
                        .setInterpolator(FLUID_ENTER)
                        .start();
            } else {
                targetMenu.animate()
                        .alpha(0f)
                        .scaleX(0.88f)
                        .scaleY(0.88f)
                        .translationY(30f)
                        .setDuration(190)
                        .setInterpolator(FLUID_EXIT)
                        .withEndAction(() -> targetMenu.setVisibility(View.GONE))
                        .start();
            }
        }

        // Koordinasi Pagination Bar: Meluncur turun saat menu buka, meluncur naik saat menu tutup
        if (layoutPagination != null && layoutPagination.getVisibility() == View.VISIBLE) {
            if (open) {
                layoutPagination.animate()
                        .alpha(0f)
                        .translationY(24f)
                        .setDuration(180)
                        .setInterpolator(FLUID_EXIT)
                        .start();
            } else {
                layoutPagination.animate()
                        .alpha(1.0f)
                        .translationY(0f)
                        .setDuration(260)
                        .setInterpolator(FLUID_ENTER)
                        .start();
            }
        }

        // FAB Icon: Rotasi 135° dengan micro-bounce scale yang elastis
        if (ivFabIcon != null) {
            if (open) {
                ivFabIcon.animate()
                        .rotation(135f)
                        .scaleX(1.08f)
                        .scaleY(1.08f)
                        .setDuration(280)
                        .setInterpolator(FLUID_ENTER)
                        .start();
                ivFabIcon.setImageResource(R.drawable.ic_close);
            } else {
                ivFabIcon.animate()
                        .rotation(0f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(220)
                        .setInterpolator(FLUID_EXIT)
                        .start();
                ivFabIcon.setImageResource(R.drawable.ic_menu);
            }
        }
    }
}
