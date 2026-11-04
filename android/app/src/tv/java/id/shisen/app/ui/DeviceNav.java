package id.shisen.app.ui;

import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import id.shisen.app.R;
import id.shisen.app.ui.home.MainActivity;
import id.shisen.app.util.TvFocusHelper;

/**
 * Pengontrol navigasi khusus varian Android TV:
 * Mengatur Left-Side Navigation Rail, D-Pad remote traversal, dan 5-kolom lanskap.
 * Bebas total dari Floating Action Button (FAB).
 */
public final class DeviceNav {

    private final MainActivity host;
    private View layoutTvSideRail;

    public DeviceNav(MainActivity host) {
        this.host = host;
    }

    public void init(View root) {
        if (root == null || host == null) return;

        layoutTvSideRail = root.findViewById(R.id.layout_tv_side_rail);

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

        for (View item : railItems) {
            if (item != null) {
                item.setFocusable(true);
                item.setClickable(true);
                TvFocusHelper.attachFocusScale(item, 1.04f);
            }
        }
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

        View currentFocus = host.getCurrentFocus();
        if (currentFocus != null && layoutTvSideRail != null) {
            // Jika fokus sedang di grid anime atau sub-filter, kembalikan fokus ke menu samping kiri
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
        // Layar Android TV landscape 1080p/4K optimal pada 5 kolom
        return 5;
    }
}
