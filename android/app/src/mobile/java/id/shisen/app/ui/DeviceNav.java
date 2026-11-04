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
    private View layoutFloatingDockBar;
    private FrameLayout fabMenuTrigger;
    private ImageView ivFabIcon;
    private boolean isFoldedMenuOpen = false;

    private LinearLayout layoutPagination;

    public DeviceNav(MainActivity host) {
        this.host = host;
    }

    public void init(View root) {
        if (root == null) return;

        scrimFoldedMenu = root.findViewById(R.id.scrim_folded_menu);
        scrollFoldedMenu = root.findViewById(R.id.scroll_folded_menu);
        layoutFoldedMenu = root.findViewById(R.id.layout_folded_menu);
        layoutFloatingDockBar = root.findViewById(R.id.layout_floating_dock_bar);
        fabMenuTrigger = root.findViewById(R.id.fab_menu_trigger);
        ivFabIcon = root.findViewById(R.id.iv_fab_icon);
        layoutPagination = root.findViewById(R.id.layout_pagination);

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
        int baseBottomMargin = (int) (18 * density) + bottomInset;

        if (layoutFloatingDockBar != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutFloatingDockBar.getLayoutParams();
            if (lp != null) {
                lp.bottomMargin = baseBottomMargin;
                layoutFloatingDockBar.setLayoutParams(lp);
            }
        } else {
            if (fabMenuTrigger != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fabMenuTrigger.getLayoutParams();
                if (lp != null) {
                    lp.bottomMargin = baseBottomMargin;
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
        }

        int menuBottomMargin = baseBottomMargin + (int) (48 * density) + (int) (10 * density);

        if (scrollFoldedMenu != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) scrollFoldedMenu.getLayoutParams();
            if (lp != null) {
                lp.bottomMargin = menuBottomMargin;
                scrollFoldedMenu.setLayoutParams(lp);
            }
        } else if (layoutFoldedMenu != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutFoldedMenu.getLayoutParams();
            if (lp != null) {
                lp.bottomMargin = menuBottomMargin;
                layoutFoldedMenu.setLayoutParams(lp);
            }
        }
    }

    public void onTabChanged(MainActivity.MainTab tab) {
        setFoldedMenuOpen(false);
        updateFoldedMenuHighlight(tab);
    }

    private void updateFoldedMenuHighlight(MainActivity.MainTab currentTab) {
        if (host == null) return;
        View[] items = {
                host.menuItemMusim, host.menuItemAiring, host.menuItemJadwal,
                host.menuItemRiwayat, host.menuItemFavorit, host.menuItemKatalog, host.menuItemGenre
        };
        TextView[] titles = {
                host.tvMenuMusim, host.tvMenuAiring, host.tvMenuJadwal,
                host.tvMenuRiwayat, host.tvMenuFavorit, host.tvMenuKatalog, host.tvMenuGenre
        };
        MainActivity.MainTab[] tabEnums = {
                MainActivity.MainTab.MUSIM, MainActivity.MainTab.AIRING, MainActivity.MainTab.JADWAL,
                MainActivity.MainTab.RIWAYAT, MainActivity.MainTab.FAVORIT, MainActivity.MainTab.KATALOG, MainActivity.MainTab.GENRE
        };

        int activeColor = ContextCompat.getColor(host, R.color.accent_glacial);
        int inactiveColor = ContextCompat.getColor(host, R.color.text_frost_bright);

        for (int i = 0; i < items.length; i++) {
            if (items[i] == null || titles[i] == null) continue;
            boolean isActive = (tabEnums[i] == currentTab);
            if (isActive) {
                items[i].setBackgroundResource(R.drawable.bg_folded_menu_pill_active);
                titles[i].setTextColor(activeColor);
                titles[i].setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                items[i].setBackgroundResource(R.drawable.bg_folded_menu_pill);
                titles[i].setTextColor(inactiveColor);
                titles[i].setTypeface(null, android.graphics.Typeface.NORMAL);
            }
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
        if (widthDp >= 900) return 4;
        if (widthDp >= 600) return 3;
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
