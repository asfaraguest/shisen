package id.menatap.app.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import id.menatap.app.R;
import id.menatap.app.data.db.MenatapDatabase;
import id.menatap.app.data.model.Anime;
import id.menatap.app.data.service.AniListClient;
import id.menatap.app.ui.adapter.AnimeAdapter;
import id.menatap.app.util.HapticHelper;

public class MainActivity extends AppCompatActivity {

    public enum MainTab {
        MUSIM, AIRING, JADWAL, RIWAYAT, FAVORIT, KATALOG, GENRE
    }

    // Top Aurora Bar & Dynamic Fluid Search Views
    private FrameLayout layoutAuroraTop;
    private LinearLayout layoutTopNormal;
    private TextView tvAppLogo;
    private TextView tvCurrentMenuBadge;
    private ProgressBar pbLoading;
    private LinearLayout btnSearchTrigger;

    private LinearLayout layoutTopSearchFluid;
    private ImageButton btnCollapseSearch;
    private EditText etSearch;
    private ImageView btnClearSearch;

    // Contextual Action Bar (Riwayat / Favorit)
    private LinearLayout layoutContextualBar;
    private TextView tvContextualTitle;
    private TextView btnClearHistoryAll;

    // Sub-Filter Container (Jadwal, Katalog, Genre)
    private HorizontalScrollView scrollSubFilters;
    private LinearLayout layoutSubChips;

    // Main Feed Views
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvGrid;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;

    // Folded Floating Menu Components (Unfolds upwards from bottom-right FAB)
    private View scrimFoldedMenu;
    private LinearLayout layoutFoldedMenu;
    private FrameLayout fabMenuTrigger;
    private ImageView ivFabIcon;
    private boolean isFoldedMenuOpen = false;

    // 7 Menu Items in Folded Floating Menu
    private View menuItemMusim, menuItemAiring, menuItemJadwal, menuItemRiwayat, menuItemFavorit, menuItemKatalog, menuItemGenre;
    private TextView tvMenuMusim, tvMenuAiring, tvMenuJadwal, tvMenuRiwayat, tvMenuFavorit, tvMenuKatalog, tvMenuGenre;

    private AnimeAdapter gridAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;

    private MainTab currentTab = MainTab.MUSIM;
    private boolean isSearchExpanded = false;

    // Contextual Sub-Filter States
    private int selectedDayOfWeek = Calendar.MONDAY;
    private String selectedCatalogFormat = "ALL";
    private String selectedCatalogSort = "POPULARITY_DESC";
    private String selectedGenre = "Action";

    private final String[] GENRES = {
            "Action", "Adventure", "Comedy", "Drama", "Fantasy",
            "Romance", "Sci-Fi", "Slice of Life", "Sports", "Supernatural",
            "Mystery", "Psychological", "Horror", "Thriller"
    };

    private final String[] DAYS_NAMES = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};
    private final int[] DAYS_CALENDAR = {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Auto-select today in Jakarta / WIB timezone
        Calendar wibCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"));
        selectedDayOfWeek = wibCal.get(Calendar.DAY_OF_WEEK);

        initViews();
        setupRecyclerView();
        setupListeners();
        setupBackHandler();
        selectTab(MainTab.MUSIM);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentTab == MainTab.RIWAYAT || currentTab == MainTab.FAVORIT) {
            loadCurrentFeed();
        }
    }

    private void initViews() {
        layoutAuroraTop = findViewById(R.id.layout_aurora_top);
        layoutTopNormal = findViewById(R.id.layout_top_normal);
        tvAppLogo = findViewById(R.id.tv_app_logo);
        tvCurrentMenuBadge = findViewById(R.id.tv_current_menu_badge);
        pbLoading = findViewById(R.id.pb_header_loading);
        btnSearchTrigger = findViewById(R.id.btn_search_trigger);

        layoutTopSearchFluid = findViewById(R.id.layout_top_search_fluid);
        btnCollapseSearch = findViewById(R.id.btn_collapse_search);
        etSearch = findViewById(R.id.et_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);

        layoutContextualBar = findViewById(R.id.layout_contextual_bar);
        tvContextualTitle = findViewById(R.id.tv_contextual_title);
        btnClearHistoryAll = findViewById(R.id.btn_clear_history_all);

        scrollSubFilters = findViewById(R.id.scroll_sub_filters);
        layoutSubChips = findViewById(R.id.layout_sub_chips);

        swipeRefresh = findViewById(R.id.swipe_refresh);
        rvGrid = findViewById(R.id.rv_anime_grid);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
        tvEmptyDesc = findViewById(R.id.tv_empty_desc);

        scrimFoldedMenu = findViewById(R.id.scrim_folded_menu);
        layoutFoldedMenu = findViewById(R.id.layout_folded_menu);
        fabMenuTrigger = findViewById(R.id.fab_menu_trigger);
        ivFabIcon = findViewById(R.id.iv_fab_icon);

        menuItemMusim = findViewById(R.id.menu_item_musim);
        menuItemAiring = findViewById(R.id.menu_item_airing);
        menuItemJadwal = findViewById(R.id.menu_item_jadwal);
        menuItemRiwayat = findViewById(R.id.menu_item_riwayat);
        menuItemFavorit = findViewById(R.id.menu_item_favorit);
        menuItemKatalog = findViewById(R.id.menu_item_katalog);
        menuItemGenre = findViewById(R.id.menu_item_genre);

        tvMenuMusim = findViewById(R.id.tv_menu_musim);
        tvMenuAiring = findViewById(R.id.tv_menu_airing);
        tvMenuJadwal = findViewById(R.id.tv_menu_jadwal);
        tvMenuRiwayat = findViewById(R.id.tv_menu_riwayat);
        tvMenuFavorit = findViewById(R.id.tv_menu_favorit);
        tvMenuKatalog = findViewById(R.id.tv_menu_katalog);
        tvMenuGenre = findViewById(R.id.tv_menu_genre);

        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent_glacial));
        swipeRefresh.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.frost_surface));
    }

    private void setupRecyclerView() {
        gridAdapter = new AnimeAdapter(this);
        GridLayoutManager glm = new GridLayoutManager(this, 2);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (gridAdapter.getItemViewType(position) == AnimeAdapter.VIEW_TYPE_HERO) {
                    return 2; // Spans 2 full columns for Hero Spotlight
                }
                return 1;
            }
        });
        rvGrid.setLayoutManager(glm);
        rvGrid.setAdapter(gridAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadCurrentFeed);

        // 1. Folded Floating Menu Triggers (Bottom-Right FAB + Scrim)
        fabMenuTrigger.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            toggleFoldedMenu();
        });

        scrimFoldedMenu.setOnClickListener(v -> {
            if (isFoldedMenuOpen) {
                setFoldedMenuOpen(false);
            }
        });

        // 7 Folded Menu Items
        menuItemMusim.setOnClickListener(v -> selectMenuItem(MainTab.MUSIM));
        menuItemAiring.setOnClickListener(v -> selectMenuItem(MainTab.AIRING));
        menuItemJadwal.setOnClickListener(v -> selectMenuItem(MainTab.JADWAL));
        menuItemRiwayat.setOnClickListener(v -> selectMenuItem(MainTab.RIWAYAT));
        menuItemFavorit.setOnClickListener(v -> selectMenuItem(MainTab.FAVORIT));
        menuItemKatalog.setOnClickListener(v -> selectMenuItem(MainTab.KATALOG));
        menuItemGenre.setOnClickListener(v -> selectMenuItem(MainTab.GENRE));

        // 2. Dynamic Fluid Searchbar Transitions
        btnSearchTrigger.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            expandFluidSearch();
        });

        btnCollapseSearch.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            collapseFluidSearch();
        });

        // Search text watcher & debouncing
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim();
                btnClearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);

                if (searchDebounceRunnable != null) {
                    mainHandler.removeCallbacks(searchDebounceRunnable);
                }
                if (!q.isEmpty()) {
                    searchDebounceRunnable = () -> performSearch(q);
                    mainHandler.postDelayed(searchDebounceRunnable, 450);
                } else if (before > 0 && q.isEmpty()) {
                    loadCurrentFeed();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            etSearch.setText("");
            loadCurrentFeed();
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = etSearch.getText().toString().trim();
                if (!q.isEmpty()) {
                    hideKeyboard();
                    performSearch(q);
                }
                return true;
            }
            return false;
        });

        // 3. Contextual Clear All History Button
        btnClearHistoryAll.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            new AlertDialog.Builder(this)
                    .setTitle("Hapus Riwayat Tontonan")
                    .setMessage("Apakah kamu yakin ingin menghapus seluruh riwayat tontonan?")
                    .setPositiveButton("Hapus Semua", (dialog, which) -> {
                        new Thread(() -> {
                            MenatapDatabase.getInstance(this).clearAllHistory();
                            mainHandler.post(() -> {
                                Toast.makeText(this, "Riwayat tontonan berhasil dibersihkan", Toast.LENGTH_SHORT).show();
                                loadCurrentFeed();
                            });
                        }).start();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isFoldedMenuOpen) {
                    setFoldedMenuOpen(false);
                } else if (isSearchExpanded) {
                    collapseFluidSearch();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // --- FOLDED FLOATING MENU EXPANSION LOGIC ---

    private void toggleFoldedMenu() {
        setFoldedMenuOpen(!isFoldedMenuOpen);
    }

    private void setFoldedMenuOpen(boolean open) {
        if (isFoldedMenuOpen == open) return;
        isFoldedMenuOpen = open;
        HapticHelper.performTick();

        if (open) {
            scrimFoldedMenu.setVisibility(View.VISIBLE);
            scrimFoldedMenu.setAlpha(0f);
            scrimFoldedMenu.animate().alpha(1.0f).setDuration(200).start();

            layoutFoldedMenu.setVisibility(View.VISIBLE);
            layoutFoldedMenu.setAlpha(0f);
            layoutFoldedMenu.setTranslationY(80f);
            layoutFoldedMenu.animate()
                    .alpha(1.0f)
                    .translationY(0f)
                    .setDuration(240)
                    .setInterpolator(new OvershootInterpolator(0.85f))
                    .start();

            ivFabIcon.animate().rotation(45f).setDuration(200).start();
        } else {
            scrimFoldedMenu.animate()
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction(() -> scrimFoldedMenu.setVisibility(View.GONE))
                    .start();

            layoutFoldedMenu.animate()
                    .alpha(0f)
                    .translationY(50f)
                    .setDuration(180)
                    .withEndAction(() -> layoutFoldedMenu.setVisibility(View.GONE))
                    .start();

            ivFabIcon.animate().rotation(0f).setDuration(180).start();
        }
    }

    private void selectMenuItem(MainTab tab) {
        setFoldedMenuOpen(false);
        selectTab(tab);
    }

    private void selectTab(MainTab tab) {
        currentTab = tab;
        updateTabsUI();
        updateSubFilterUI();
        loadCurrentFeed();
    }

    private void updateTabsUI() {
        View[] items = {menuItemMusim, menuItemAiring, menuItemJadwal, menuItemRiwayat, menuItemFavorit, menuItemKatalog, menuItemGenre};
        TextView[] titles = {tvMenuMusim, tvMenuAiring, tvMenuJadwal, tvMenuRiwayat, tvMenuFavorit, tvMenuKatalog, tvMenuGenre};
        MainTab[] tabEnums = {MainTab.MUSIM, MainTab.AIRING, MainTab.JADWAL, MainTab.RIWAYAT, MainTab.FAVORIT, MainTab.KATALOG, MainTab.GENRE};
        String[] badges = {"BERANDA", "SEDANG TAYANG", "JADWAL", "RIWAYAT", "FAVORIT", "KATALOG", "GENRE"};

        int activeColor = ContextCompat.getColor(this, R.color.accent_glacial);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_frost_bright);

        for (int i = 0; i < items.length; i++) {
            boolean isActive = (tabEnums[i] == currentTab);
            if (isActive) {
                items[i].setBackgroundResource(R.drawable.bg_folded_menu_pill_active);
                titles[i].setTextColor(activeColor);
                titles[i].setTypeface(null, android.graphics.Typeface.BOLD);
                tvCurrentMenuBadge.setText(badges[i]);
            } else {
                items[i].setBackgroundResource(R.drawable.bg_folded_menu_pill);
                titles[i].setTextColor(inactiveColor);
                titles[i].setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    // --- DYNAMIC FLUID SEARCHBOX LOGIC ---

    private void expandFluidSearch() {
        if (isSearchExpanded) return;
        isSearchExpanded = true;
        if (isFoldedMenuOpen) setFoldedMenuOpen(false);

        layoutTopNormal.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> layoutTopNormal.setVisibility(View.GONE))
                .start();

        layoutTopSearchFluid.setVisibility(View.VISIBLE);
        layoutTopSearchFluid.setAlpha(0f);
        layoutTopSearchFluid.animate().alpha(1.0f).setDuration(200).start();

        etSearch.requestFocus();
        mainHandler.postDelayed(this::showKeyboard, 100);
    }

    private void collapseFluidSearch() {
        if (!isSearchExpanded) return;
        isSearchExpanded = false;
        hideKeyboard();

        String prevQuery = etSearch.getText().toString().trim();
        etSearch.setText("");

        layoutTopSearchFluid.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> layoutTopSearchFluid.setVisibility(View.GONE))
                .start();

        layoutTopNormal.setVisibility(View.VISIBLE);
        layoutTopNormal.setAlpha(0f);
        layoutTopNormal.animate().alpha(1.0f).setDuration(200).start();

        if (!prevQuery.isEmpty()) {
            loadCurrentFeed();
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    // --- CONTEXTUAL SUB-FILTER BAR (Tatap Parity) ---

    private void updateSubFilterUI() {
        layoutSubChips.removeAllViews();

        if (currentTab == MainTab.JADWAL) {
            layoutContextualBar.setVisibility(View.GONE);
            scrollSubFilters.setVisibility(View.VISIBLE);

            Calendar wibNow = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"));
            int todayWib = wibNow.get(Calendar.DAY_OF_WEEK);

            for (int i = 0; i < DAYS_NAMES.length; i++) {
                final int calDay = DAYS_CALENDAR[i];
                boolean isSelected = (calDay == selectedDayOfWeek);
                String label = DAYS_NAMES[i] + (calDay == todayWib ? " (Hari Ini)" : "");

                TextView chip = createSubFilterChip(label, isSelected);
                chip.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    selectedDayOfWeek = calDay;
                    updateSubFilterUI();
                    loadCurrentFeed();
                });
                layoutSubChips.addView(chip);
            }
        } else if (currentTab == MainTab.KATALOG) {
            layoutContextualBar.setVisibility(View.GONE);
            scrollSubFilters.setVisibility(View.VISIBLE);

            String[] formats = {"ALL", "TV", "MOVIE", "OVA", "SPECIAL"};
            String[] formatLabels = {"Semua Format", "Serial TV", "Film / Movie", "OVA", "Spesial"};
            for (int i = 0; i < formats.length; i++) {
                final String f = formats[i];
                boolean isSelected = f.equalsIgnoreCase(selectedCatalogFormat);

                TextView chip = createSubFilterChip(formatLabels[i], isSelected);
                chip.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    selectedCatalogFormat = f;
                    updateSubFilterUI();
                    loadCurrentFeed();
                });
                layoutSubChips.addView(chip);
            }

            // Separator line
            View div = new View(this);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                    (int) (1.5 * getResources().getDisplayMetrics().density),
                    (int) (18 * getResources().getDisplayMetrics().density)
            );
            dLp.setMargins((int) (4 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
            div.setLayoutParams(dLp);
            div.setBackgroundColor(Color.parseColor("#243859"));
            layoutSubChips.addView(div);

            String[] sorts = {"POPULARITY_DESC", "SCORE_DESC", "TRENDING_DESC", "FAVOURITES_DESC"};
            String[] sortLabels = {"Terpopuler", "Skor Tertinggi", "Sedang Tren", "Favorit"};
            for (int i = 0; i < sorts.length; i++) {
                final String s = sorts[i];
                boolean isSelected = s.equalsIgnoreCase(selectedCatalogSort);

                TextView chip = createSubFilterChip(sortLabels[i], isSelected);
                chip.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    selectedCatalogSort = s;
                    updateSubFilterUI();
                    loadCurrentFeed();
                });
                layoutSubChips.addView(chip);
            }
        } else if (currentTab == MainTab.GENRE) {
            layoutContextualBar.setVisibility(View.GONE);
            scrollSubFilters.setVisibility(View.VISIBLE);
            for (String genre : GENRES) {
                boolean isSelected = genre.equalsIgnoreCase(selectedGenre);

                TextView chip = createSubFilterChip(genre, isSelected);
                chip.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    selectedGenre = genre;
                    updateSubFilterUI();
                    loadCurrentFeed();
                });
                layoutSubChips.addView(chip);
            }
        } else {
            scrollSubFilters.setVisibility(View.GONE);
        }
    }

    private TextView createSubFilterChip(String text, boolean isSelected) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (int) (28 * getResources().getDisplayMetrics().density)
        );
        lp.setMarginEnd((int) (6 * getResources().getDisplayMetrics().density));
        chip.setLayoutParams(lp);
        chip.setText(text);
        chip.setTextSize(11f);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setPadding(
                (int) (12 * getResources().getDisplayMetrics().density), 0,
                (int) (12 * getResources().getDisplayMetrics().density), 0
        );

        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_frost_active);
            chip.setTextColor(Color.parseColor("#090D16"));
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_frost_inactive);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_frost_subtle));
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        return chip;
    }

    private void setLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (!loading) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void loadCurrentFeed() {
        String searchQuery = etSearch.getText().toString().trim();
        if (!searchQuery.isEmpty()) {
            performSearch(searchQuery);
            return;
        }

        setLoading(true);

        if (currentTab == MainTab.MUSIM) {
            layoutContextualBar.setVisibility(View.GONE);
            // BERANDA: 1 Featured Spotlight Hero + 2-Column Grid
            AniListClient.getTrending(1, 35, new AniListClient.Callback<List<Anime>>() {
                @Override
                public void onSuccess(List<Anime> result) {
                    mainHandler.post(() -> {
                        setLoading(false);
                        if (result != null && !result.isEmpty()) {
                            layoutEmpty.setVisibility(View.GONE);
                            rvGrid.setVisibility(View.VISIBLE);

                            Anime hero = result.get(0);
                            List<Anime> gridList = (result.size() > 1) ? result.subList(1, result.size()) : new ArrayList<>();
                            gridAdapter.setHeroAnime(hero);
                            gridAdapter.setData(gridList);
                            rvGrid.scrollToPosition(0);
                        } else {
                            gridAdapter.setHeroAnime(null);
                            gridAdapter.setData(new ArrayList<>());
                            layoutEmpty.setVisibility(View.VISIBLE);
                        }
                    });
                }

                @Override
                public void onError(Throwable t) {
                    mainHandler.post(() -> {
                        setLoading(false);
                        Toast.makeText(MainActivity.this, "Gagal memuat beranda: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } else {
            gridAdapter.setHeroAnime(null);

            switch (currentTab) {
                case AIRING:
                    layoutContextualBar.setVisibility(View.VISIBLE);
                    tvContextualTitle.setText("SEDANG TAYANG (AIRING LIVE)");
                    btnClearHistoryAll.setVisibility(View.GONE);
                    AniListClient.getAiring(1, 40, createCallback("Gagal Memuat Sedang Tayang"));
                    break;
                case JADWAL:
                    layoutContextualBar.setVisibility(View.GONE);
                    AniListClient.getSchedule(selectedDayOfWeek, createCallback("Gagal Memuat Jadwal"));
                    break;
                case RIWAYAT:
                    new Thread(() -> {
                        List<Anime> history = MenatapDatabase.getInstance(this).getHistoryList();
                        mainHandler.post(() -> {
                            layoutContextualBar.setVisibility(View.VISIBLE);
                            tvContextualTitle.setText("RIWAYAT TONTONAN (" + history.size() + ")");
                            btnClearHistoryAll.setVisibility(history.isEmpty() ? View.GONE : View.VISIBLE);
                        });
                        displayResults(history, "Riwayat Tontonan Kosong", "Anime yang kamu putar akan tersimpan otomatis di sini.");
                    }).start();
                    break;
                case FAVORIT:
                    new Thread(() -> {
                        List<Anime> bookmarks = MenatapDatabase.getInstance(this).getBookmarkList();
                        mainHandler.post(() -> {
                            layoutContextualBar.setVisibility(View.VISIBLE);
                            tvContextualTitle.setText("ANIME FAVORIT (" + bookmarks.size() + ")");
                            btnClearHistoryAll.setVisibility(View.GONE);
                        });
                        displayResults(bookmarks, "Belum Ada Favorit", "Tekan ikon bintang di halaman detail anime untuk menyimpannya.");
                    }).start();
                    break;
                case KATALOG:
                    layoutContextualBar.setVisibility(View.GONE);
                    AniListClient.getCatalog(selectedCatalogFormat, selectedCatalogSort, 1, 40, createCallback("Gagal Memuat Katalog"));
                    break;
                case GENRE:
                    layoutContextualBar.setVisibility(View.GONE);
                    AniListClient.getByGenre(selectedGenre, 1, 40, createCallback("Gagal Memuat Genre " + selectedGenre));
                    break;
                default:
                    break;
            }
        }
    }

    private void performSearch(String query) {
        setLoading(true);
        gridAdapter.setHeroAnime(null);
        tvCurrentMenuBadge.setText("PENCARIAN");
        layoutContextualBar.setVisibility(View.VISIBLE);
        tvContextualTitle.setText("HASIL PENCARIAN: \"" + query + "\"");
        btnClearHistoryAll.setVisibility(View.GONE);

        AniListClient.search(query, 1, 40, new AniListClient.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result) {
                displayResults(result, "Pencarian Tidak Ditemukan", "Coba gunakan kata kunci lain atau judul bahasa Inggris.");
            }

            @Override
            public void onError(Throwable t) {
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(MainActivity.this, "Pencarian gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private AniListClient.Callback<List<Anime>> createCallback(String errorPrefix) {
        return new AniListClient.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result) {
                displayResults(result, "Tidak Ada Data", "Konten belum tersedia untuk kategori ini.");
            }

            @Override
            public void onError(Throwable t) {
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(MainActivity.this, errorPrefix + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        };
    }

    private void displayResults(List<Anime> list, String emptyTitle, String emptyDesc) {
        mainHandler.post(() -> {
            setLoading(false);
            if (list == null || list.isEmpty()) {
                gridAdapter.setHeroAnime(null);
                gridAdapter.setData(new ArrayList<>());
                layoutEmpty.setVisibility(View.VISIBLE);
                tvEmptyTitle.setText(emptyTitle);
                tvEmptyDesc.setText(emptyDesc);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                rvGrid.setVisibility(View.VISIBLE);
                gridAdapter.setHeroAnime(null);
                gridAdapter.setData(list);
                rvGrid.scrollToPosition(0);
            }
        });
    }
}
