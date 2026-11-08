package id.shisen.app.ui;

import android.animation.ValueAnimator;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import id.shisen.app.R;
import id.shisen.app.ui.home.MainActivity;
import id.shisen.app.util.TvFocusHelper;

/**
 * Pengontrol navigasi khusus varian Android TV:
 * Mengatur Left-Side Navigation Rail yang dapat otomatis expand (184dp) saat fokus
 * dan collapse (64dp ikon saja) saat fokus berpindah ke konten utama.
 */
public final class DeviceNav {

    private static final int RAIL_COLLAPSED_DP = 64;
    private static final int RAIL_EXPANDED_DP = 180;

    private final MainActivity host;
    private View layoutTvSideRail;
    private View[] railLabels;
    private View railDividerSettings;
    private boolean isRailExpanded = false;
    private ValueAnimator railWidthAnimator;

    public DeviceNav(MainActivity host) {
        this.host = host;
    }

    public void init(View root) {
        if (root == null || host == null) return;

        layoutTvSideRail = root.findViewById(R.id.layout_tv_side_rail);
        railDividerSettings = root.findViewById(R.id.rail_divider_settings);

        railLabels = new View[]{
                root.findViewById(R.id.tv_app_logo),
                root.findViewById(R.id.tv_app_logo_kanji),
                root.findViewById(R.id.tv_menu_search),
                host.tvMenuMusim,
                host.tvMenuAiring,
                host.tvMenuJadwal,
                host.tvMenuRiwayat,
                host.tvMenuFavorit,
                host.tvMenuKatalog,
                host.tvMenuGenre,
                root.findViewById(R.id.tv_menu_settings)
        };

        View[] railItems = {
                host.findViewById(R.id.btn_search_trigger),
                host.menuItemMusim,
                host.menuItemAiring,
                host.menuItemJadwal,
                host.menuItemRiwayat,
                host.menuItemFavorit,
                host.menuItemKatalog,
                host.menuItemGenre,
                host.menuItemSettings
        };

        View.OnKeyListener railKeyListener = (v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (focusMainContentArea()) {
                    return true;
                }
            }
            return false;
        };

        for (View item : railItems) {
            if (item != null) {
                item.setFocusable(true);
                item.setClickable(true);
                TvFocusHelper.attachFocusScale(item, 1.04f);
                item.setOnKeyListener(railKeyListener);
            }
        }

        // Pasang focus scale pada kontrol topbar TV (Pagination, Clear History, Search Collapse)
        int[] topFocusableIds = {
                R.id.btn_prev_page,
                R.id.btn_next_page,
                R.id.btn_clear_history_all,
                R.id.btn_collapse_search
        };
        for (int id : topFocusableIds) {
            View btn = root.findViewById(id);
            if (btn != null) {
                btn.setFocusable(true);
                TvFocusHelper.attachFocusScale(btn, 1.06f);
            }
        }

        // Pantau perubahan fokus global untuk otomatis expand/collapse sidebar
        root.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> {
            if (newFocus == null || layoutTvSideRail == null) return;
            boolean focusInRail = isViewDescendantOf(newFocus, layoutTvSideRail);
            setRailExpanded(focusInRail);
        });
    }

    private void setRailExpanded(boolean expanded) {
        if (layoutTvSideRail == null || isRailExpanded == expanded) return;
        isRailExpanded = expanded;

        float density = host.getResources().getDisplayMetrics().density;
        int startWidth = layoutTvSideRail.getWidth();
        if (startWidth <= 0) {
            startWidth = (int) ((expanded ? RAIL_COLLAPSED_DP : RAIL_EXPANDED_DP) * density);
        }
        int targetWidth = (int) ((expanded ? RAIL_EXPANDED_DP : RAIL_COLLAPSED_DP) * density);

        if (railWidthAnimator != null && railWidthAnimator.isRunning()) {
            railWidthAnimator.cancel();
        }

        if (expanded) {
            setLabelsVisible(true, 0f);
        }

        final int sWidth = startWidth;
        final int tWidth = targetWidth;

        railWidthAnimator = ValueAnimator.ofFloat(0f, 1f);
        railWidthAnimator.setDuration(160);
        railWidthAnimator.setInterpolator(new DecelerateInterpolator());
        railWidthAnimator.addUpdateListener(anim -> {
            float frac = anim.getAnimatedFraction();
            int curWidth = (int) (sWidth + (tWidth - sWidth) * frac);
            ViewGroup.LayoutParams lp = layoutTvSideRail.getLayoutParams();
            if (lp != null) {
                lp.width = curWidth;
                layoutTvSideRail.setLayoutParams(lp);
            }
            setLabelsAlpha(expanded ? frac : (1f - frac));
        });
        railWidthAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (!expanded) {
                    setLabelsVisible(false, 0f);
                } else {
                    setLabelsAlpha(1f);
                }
            }
        });
        railWidthAnimator.start();
    }

    private void setLabelsVisible(boolean visible, float initialAlpha) {
        if (railLabels != null) {
            for (View label : railLabels) {
                if (label != null) {
                    label.setVisibility(visible ? View.VISIBLE : View.GONE);
                    label.setAlpha(initialAlpha);
                }
            }
        }
        if (railDividerSettings != null) {
            railDividerSettings.setVisibility(visible ? View.VISIBLE : View.GONE);
            railDividerSettings.setAlpha(initialAlpha);
        }
    }

    private void setLabelsAlpha(float alpha) {
        if (railLabels != null) {
            for (View label : railLabels) {
                if (label != null && label.getVisibility() == View.VISIBLE) {
                    label.setAlpha(alpha);
                }
            }
        }
        if (railDividerSettings != null && railDividerSettings.getVisibility() == View.VISIBLE) {
            railDividerSettings.setAlpha(alpha);
        }
    }

    private boolean focusMainContentArea() {
        if (host == null) return false;

        // 0. Jika mode pencarian sedang aktif, prioritaskan search input
        if (host.isSearchExpanded()) {
            View et = host.findViewById(R.id.et_search);
            if (et != null && et.isFocusable() && et.requestFocus()) {
                return true;
            }
        }

        // 1. Prioritaskan item pertama di RecyclerView katalog anime (Hero button atau kartu anime)
        if (host.focusFirstGridItem()) {
            return true;
        }

        // 2. Fallback ke chip sub-filter jika sedang tampil
        View scrollSub = host.findViewById(R.id.scroll_sub_filters);
        ViewGroup layoutChips = host.findViewById(R.id.layout_sub_chips);
        if (scrollSub != null && scrollSub.getVisibility() == View.VISIBLE
                && layoutChips != null && layoutChips.getChildCount() > 0) {
            for (int i = 0; i < layoutChips.getChildCount(); i++) {
                View chip = layoutChips.getChildAt(i);
                if (chip != null && chip.isFocusable() && chip.requestFocus()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void applyInsets(int topInset, int bottomInset, float density) {
        // Pada Android TV / Landscape, insets system bars tidak menggeser layout rail
    }

    public void onTabChanged(MainActivity.MainTab tab) {
        updateTvRailHighlight(tab);
    }

    private void updateTvRailHighlight(MainActivity.MainTab currentTab) {
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
            items[i].setSelected(isActive);
            titles[i].setTextColor(isActive ? activeColor : inactiveColor);
            titles[i].setTypeface(null, isActive ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    public boolean handleBackPressed() {
        if (host == null) return false;

        // Jika pencarian sedang aktif, tutup pencarian dan kembalikan fokus ke sidebar
        if (host.isSearchExpanded()) {
            host.collapseFluidSearch();
            View activeMenuItem = getActiveRailItem();
            if (activeMenuItem != null) {
                activeMenuItem.requestFocus();
            }
            return true;
        }

        View currentFocus = host.getCurrentFocus();
        if (currentFocus != null && layoutTvSideRail != null) {
            // Jika fokus sedang di grid anime atau sub-filter, kembalikan fokus ke menu samping kiri (otomatis expand)
            boolean isInRail = isViewDescendantOf(currentFocus, layoutTvSideRail);
            if (!isInRail) {
                View activeMenuItem = getActiveRailItem();
                if (activeMenuItem != null) {
                    activeMenuItem.requestFocus();
                    return true;
                }
            }
        }
        return false;
    }

    private View getActiveRailItem() {
        if (host == null) return null;
        MainActivity.MainTab tab = host.getCurrentTab();
        if (tab == null) return host.menuItemMusim;

        switch (tab) {
            case AIRING: return host.menuItemAiring;
            case JADWAL: return host.menuItemJadwal;
            case RIWAYAT: return host.menuItemRiwayat;
            case FAVORIT: return host.menuItemFavorit;
            case KATALOG: return host.menuItemKatalog;
            case GENRE: return host.menuItemGenre;
            case MUSIM:
            default:
                return host.menuItemMusim;
        }
    }

    private boolean isViewDescendantOf(View child, View parent) {
        if (child == null || parent == null) return false;
        if (child == parent) return true;
        Object p = child.getParent();
        return (p instanceof View) && isViewDescendantOf((View) p, parent);
    }

    public int getGridSpan(int widthDp) {
        // Layar Android TV landscape 1080p/4K optimal pada 6 kolom; 720p pada 5 kolom
        if (widthDp >= 840) return 6;
        return 5;
    }
}
