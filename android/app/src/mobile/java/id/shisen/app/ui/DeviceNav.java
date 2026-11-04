package id.shisen.app.ui;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import id.shisen.app.R;
import id.shisen.app.ui.home.MainActivity;
import id.shisen.app.util.HapticHelper;

/**
 * Pengontrol navigasi khusus varian Mobile:
 * Mengatur Floating Action Button (FAB), folded pop-up menu, dan floating pagination bar.
 */
public final class DeviceNav {

    private final MainActivity host;

    private View scrimFoldedMenu;
    private LinearLayout layoutFoldedMenu;
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
        layoutFoldedMenu = root.findViewById(R.id.layout_folded_menu);
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
        if (fabMenuTrigger != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fabMenuTrigger.getLayoutParams();
            if (lp != null) {
                lp.bottomMargin = (int) (20 * density) + bottomInset;
                fabMenuTrigger.setLayoutParams(lp);
            }
        }

        if (layoutPagination != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutPagination.getLayoutParams();
            if (lp != null) {
                lp.bottomMargin = (int) (20 * density) + bottomInset;
                layoutPagination.setLayoutParams(lp);
            }
        }

        if (layoutFoldedMenu != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutFoldedMenu.getLayoutParams();
            if (lp != null) {
                lp.bottomMargin = (int) (84 * density) + bottomInset;
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
                scrimFoldedMenu.animate().alpha(1.0f).setDuration(200).start();
            } else {
                scrimFoldedMenu.animate().alpha(0f).setDuration(180).withEndAction(() -> {
                    scrimFoldedMenu.setVisibility(View.GONE);
                }).start();
            }
        }

        if (layoutFoldedMenu != null) {
            if (open) {
                layoutFoldedMenu.setVisibility(View.VISIBLE);
                layoutFoldedMenu.setAlpha(0f);
                layoutFoldedMenu.setTranslationY(80f);
                layoutFoldedMenu.animate()
                        .alpha(1.0f)
                        .translationY(0f)
                        .setDuration(240)
                        .setInterpolator(new OvershootInterpolator(0.85f))
                        .start();
            } else {
                layoutFoldedMenu.animate()
                        .alpha(0f)
                        .translationY(60f)
                        .setDuration(160)
                        .withEndAction(() -> layoutFoldedMenu.setVisibility(View.GONE))
                        .start();
            }
        }

        if (ivFabIcon != null) {
            ivFabIcon.animate()
                    .rotation(open ? 90f : 0f)
                    .setDuration(200)
                    .start();
            ivFabIcon.setImageResource(open ? R.drawable.ic_close : R.drawable.ic_menu);
        }
    }
}
