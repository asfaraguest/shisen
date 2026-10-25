package id.menatap.app.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

    private DrawerLayout drawerLayout;
    private ImageButton btnDrawerOpen;
    private FrameLayout fabMenuTrigger;
    private TextView tvCurrentMenuBadge;

    private ProgressBar pbLoading;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvGrid;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;

    // Sub-Filter Container
    private HorizontalScrollView scrollSubFilters;
    private LinearLayout layoutSubChips;

    // 7 Drawer Items
    private View drawerItemMusim, drawerItemAiring, drawerItemJadwal, drawerItemRiwayat, drawerItemFavorit, drawerItemKatalog, drawerItemGenre;
    private TextView tvDrawerMusim, tvDrawerAiring, tvDrawerJadwal, tvDrawerRiwayat, tvDrawerFavorit, tvDrawerKatalog, tvDrawerGenre;

    private AnimeAdapter gridAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;

    private MainTab currentTab = MainTab.MUSIM;

    // Sub-filter states
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
        drawerLayout = findViewById(R.id.drawer_layout);
        btnDrawerOpen = findViewById(R.id.btn_drawer_open);
        fabMenuTrigger = findViewById(R.id.fab_menu_trigger);
        tvCurrentMenuBadge = findViewById(R.id.tv_current_menu_badge);

        pbLoading = findViewById(R.id.pb_header_loading);
        etSearch = findViewById(R.id.et_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        rvGrid = findViewById(R.id.rv_anime_grid);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
        tvEmptyDesc = findViewById(R.id.tv_empty_desc);

        scrollSubFilters = findViewById(R.id.scroll_sub_filters);
        layoutSubChips = findViewById(R.id.layout_sub_chips);

        // Drawer items
        drawerItemMusim = findViewById(R.id.drawer_item_musim);
        drawerItemAiring = findViewById(R.id.drawer_item_airing);
        drawerItemJadwal = findViewById(R.id.drawer_item_jadwal);
        drawerItemRiwayat = findViewById(R.id.drawer_item_riwayat);
        drawerItemFavorit = findViewById(R.id.drawer_item_favorit);
        drawerItemKatalog = findViewById(R.id.drawer_item_katalog);
        drawerItemGenre = findViewById(R.id.drawer_item_genre);

        tvDrawerMusim = findViewById(R.id.tv_drawer_title_musim);
        tvDrawerAiring = findViewById(R.id.tv_drawer_title_airing);
        tvDrawerJadwal = findViewById(R.id.tv_drawer_title_jadwal);
        tvDrawerRiwayat = findViewById(R.id.tv_drawer_title_riwayat);
        tvDrawerFavorit = findViewById(R.id.tv_drawer_title_favorit);
        tvDrawerKatalog = findViewById(R.id.tv_drawer_title_katalog);
        tvDrawerGenre = findViewById(R.id.tv_drawer_title_genre);

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
                    return 2; // Spans full width across 2 columns
                }
                return 1; // Standard grid card occupies 1 column
            }
        });
        rvGrid.setLayoutManager(glm);
        rvGrid.setAdapter(gridAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadCurrentFeed);

        // Drawer toggle triggers (Top Hamburger & Bottom Right Floating Trigger)
        View.OnClickListener toggleDrawer = v -> {
            HapticHelper.performClick(v);
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        };

        btnDrawerOpen.setOnClickListener(toggleDrawer);
        fabMenuTrigger.setOnClickListener(toggleDrawer);

        // 7 Drawer Items
        drawerItemMusim.setOnClickListener(v -> selectDrawerTab(MainTab.MUSIM));
        drawerItemAiring.setOnClickListener(v -> selectDrawerTab(MainTab.AIRING));
        drawerItemJadwal.setOnClickListener(v -> selectDrawerTab(MainTab.JADWAL));
        drawerItemRiwayat.setOnClickListener(v -> selectDrawerTab(MainTab.RIWAYAT));
        drawerItemFavorit.setOnClickListener(v -> selectDrawerTab(MainTab.FAVORIT));
        drawerItemKatalog.setOnClickListener(v -> selectDrawerTab(MainTab.KATALOG));
        drawerItemGenre.setOnClickListener(v -> selectDrawerTab(MainTab.GENRE));

        // Search text watcher
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
                    mainHandler.postDelayed(searchDebounceRunnable, 500);
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
                    performSearch(q);
                }
                return true;
            }
            return false;
        });
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void selectDrawerTab(MainTab tab) {
        HapticHelper.performTick();
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        selectTab(tab);
    }

    private void selectTab(MainTab tab) {
        currentTab = tab;
        updateTabsUI();
        updateSubFilterUI();
        loadCurrentFeed();
    }

    private void updateTabsUI() {
        View[] items = {drawerItemMusim, drawerItemAiring, drawerItemJadwal, drawerItemRiwayat, drawerItemFavorit, drawerItemKatalog, drawerItemGenre};
        TextView[] titles = {tvDrawerMusim, tvDrawerAiring, tvDrawerJadwal, tvDrawerRiwayat, tvDrawerFavorit, tvDrawerKatalog, tvDrawerGenre};
        MainTab[] tabEnums = {MainTab.MUSIM, MainTab.AIRING, MainTab.JADWAL, MainTab.RIWAYAT, MainTab.FAVORIT, MainTab.KATALOG, MainTab.GENRE};
        String[] badges = {"BERANDA", "SEDANG TAYANG", "JADWAL", "RIWAYAT", "FAVORIT", "KATALOG", "GENRE"};

        int activeColor = ContextCompat.getColor(this, R.color.accent_glacial);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_frost_bright);

        for (int i = 0; i < items.length; i++) {
            boolean isActive = (tabEnums[i] == currentTab);
            if (isActive) {
                items[i].setBackgroundResource(R.drawable.bg_drawer_item_active);
                titles[i].setTextColor(activeColor);
                titles[i].setTypeface(null, android.graphics.Typeface.BOLD);
                tvCurrentMenuBadge.setText(badges[i]);
            } else {
                items[i].setBackgroundResource(R.drawable.bg_drawer_item_inactive);
                titles[i].setTextColor(inactiveColor);
                titles[i].setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void updateSubFilterUI() {
        layoutSubChips.removeAllViews();

        if (currentTab == MainTab.JADWAL) {
            scrollSubFilters.setVisibility(View.VISIBLE);
            int todayCalendar = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

            for (int i = 0; i < DAYS_NAMES.length; i++) {
                final int calDay = DAYS_CALENDAR[i];
                boolean isSelected = (calDay == selectedDayOfWeek);
                String label = DAYS_NAMES[i] + (calDay == todayCalendar ? " (Hari Ini)" : "");

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
            scrollSubFilters.setVisibility(View.VISIBLE);

            String[] formats = {"ALL", "TV", "MOVIE", "OVA", "SPECIAL"};
            String[] formatLabels = {"Semua", "Serial TV", "Film / Movie", "OVA", "Spesial"};
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

            // Separator
            View div = new View(this);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                    (int) (1 * getResources().getDisplayMetrics().density),
                    (int) (16 * getResources().getDisplayMetrics().density)
            );
            dLp.setMargins((int) (4 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
            div.setLayoutParams(dLp);
            div.setBackgroundColor(Color.parseColor("#1F2E4A"));
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
            // BERANDA: 1 Featured Spotlight Hero + Breathable 2-Column Grid
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
                    AniListClient.getAiring(1, 40, createCallback("Gagal Memuat Sedang Tayang"));
                    break;
                case JADWAL:
                    AniListClient.getSchedule(selectedDayOfWeek, createCallback("Gagal Memuat Jadwal"));
                    break;
                case RIWAYAT:
                    new Thread(() -> {
                        List<Anime> history = MenatapDatabase.getInstance(this).getHistoryList();
                        displayResults(history, "Riwayat Kosong", "Anime yang kamu tonton akan muncul di sini.");
                    }).start();
                    break;
                case FAVORIT:
                    new Thread(() -> {
                        List<Anime> bookmarks = MenatapDatabase.getInstance(this).getBookmarkList();
                        displayResults(bookmarks, "Belum Ada Favorit", "Simpan anime favoritmu untuk ditonton nanti.");
                    }).start();
                    break;
                case KATALOG:
                    AniListClient.getCatalog(selectedCatalogFormat, selectedCatalogSort, 1, 40, createCallback("Gagal Memuat Katalog"));
                    break;
                case GENRE:
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
