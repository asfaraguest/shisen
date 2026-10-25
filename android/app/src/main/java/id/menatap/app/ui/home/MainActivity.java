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
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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

    private ProgressBar pbLoading;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvGrid;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;

    // 7 Navigation Tabs
    private TextView tabMusim;
    private TextView tabAiring;
    private TextView tabJadwal;
    private TextView tabRiwayat;
    private TextView tabFavorit;
    private TextView tabKatalog;
    private TextView tabGenre;

    // Sub-Filter Container
    private HorizontalScrollView scrollSubFilters;
    private LinearLayout layoutSubChips;

    private AnimeAdapter adapter;
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
        pbLoading = findViewById(R.id.pb_header_loading);
        etSearch = findViewById(R.id.et_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        rvGrid = findViewById(R.id.rv_anime_grid);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
        tvEmptyDesc = findViewById(R.id.tv_empty_desc);

        tabMusim = findViewById(R.id.tab_nav_musim);
        tabAiring = findViewById(R.id.tab_nav_airing);
        tabJadwal = findViewById(R.id.tab_nav_jadwal);
        tabRiwayat = findViewById(R.id.tab_nav_riwayat);
        tabFavorit = findViewById(R.id.tab_nav_favorit);
        tabKatalog = findViewById(R.id.tab_nav_katalog);
        tabGenre = findViewById(R.id.tab_nav_genre);

        scrollSubFilters = findViewById(R.id.scroll_sub_filters);
        layoutSubChips = findViewById(R.id.layout_sub_chips);

        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent_glacial));
        swipeRefresh.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.frost_surface));
    }

    private void setupRecyclerView() {
        adapter = new AnimeAdapter(this);
        GridLayoutManager glm = new GridLayoutManager(this, 2);
        rvGrid.setLayoutManager(glm);
        rvGrid.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadCurrentFeed);

        // 7 Main Tabs
        tabMusim.setOnClickListener(v -> selectTab(MainTab.MUSIM));
        tabAiring.setOnClickListener(v -> selectTab(MainTab.AIRING));
        tabJadwal.setOnClickListener(v -> selectTab(MainTab.JADWAL));
        tabRiwayat.setOnClickListener(v -> selectTab(MainTab.RIWAYAT));
        tabFavorit.setOnClickListener(v -> selectTab(MainTab.FAVORIT));
        tabKatalog.setOnClickListener(v -> selectTab(MainTab.KATALOG));
        tabGenre.setOnClickListener(v -> selectTab(MainTab.GENRE));

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
                    mainHandler.postDelayed(searchDebounceRunnable, 600);
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

    private void selectTab(MainTab tab) {
        currentTab = tab;
        updateTabsUI();
        updateSubFilterUI();
        loadCurrentFeed();
    }

    private void updateTabsUI() {
        TextView[] tabs = {tabMusim, tabAiring, tabJadwal, tabRiwayat, tabFavorit, tabKatalog, tabGenre};
        MainTab[] tabEnums = {MainTab.MUSIM, MainTab.AIRING, MainTab.JADWAL, MainTab.RIWAYAT, MainTab.FAVORIT, MainTab.KATALOG, MainTab.GENRE};

        for (int i = 0; i < tabs.length; i++) {
            boolean isActive = (tabEnums[i] == currentTab);
            if (isActive) {
                tabs[i].setBackgroundResource(R.drawable.bg_chip_frost_active);
                tabs[i].setTextColor(Color.parseColor("#080C14"));
            } else {
                tabs[i].setBackgroundResource(R.drawable.bg_chip_frost_inactive);
                tabs[i].setTextColor(ContextCompat.getColor(this, R.color.text_frost_subtle));
            }
        }
    }

    private void updateSubFilterUI() {
        layoutSubChips.removeAllViews();

        if (currentTab == MainTab.JADWAL) {
            scrollSubFilters.setVisibility(View.VISIBLE);
            for (int i = 0; i < DAYS_NAMES.length; i++) {
                final int dayCal = DAYS_CALENDAR[i];
                final String dayName = DAYS_NAMES[i];
                boolean isSelected = (dayCal == selectedDayOfWeek);

                TextView chip = createSubFilterChip(dayName, isSelected);
                chip.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    selectedDayOfWeek = dayCal;
                    updateSubFilterUI();
                    loadCurrentFeed();
                });
                layoutSubChips.addView(chip);
            }
        } else if (currentTab == MainTab.KATALOG) {
            scrollSubFilters.setVisibility(View.VISIBLE);

            // Format filters
            String[] formats = {"Semua", "TV", "Movie", "OVA"};
            String[] formatVals = {"ALL", "TV", "MOVIE", "OVA"};

            for (int i = 0; i < formats.length; i++) {
                final String fVal = formatVals[i];
                boolean isSelected = fVal.equalsIgnoreCase(selectedCatalogFormat);

                TextView chip = createSubFilterChip(formats[i], isSelected);
                chip.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    selectedCatalogFormat = fVal;
                    updateSubFilterUI();
                    loadCurrentFeed();
                });
                layoutSubChips.addView(chip);
            }

            // Divider chip
            View div = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(2, (int) (18 * getResources().getDisplayMetrics().density));
            lp.setMargins((int) (8 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
            div.setLayoutParams(lp);
            div.setBackgroundColor(ContextCompat.getColor(this, R.color.frost_border));
            layoutSubChips.addView(div);

            // Sort filters
            String[] sorts = {"Populer", "Rating", "Trending"};
            String[] sortVals = {"POPULARITY_DESC", "SCORE_DESC", "TRENDING_DESC"};

            for (int i = 0; i < sorts.length; i++) {
                final String sVal = sortVals[i];
                boolean isSelected = sVal.equalsIgnoreCase(selectedCatalogSort);

                TextView chip = createSubFilterChip(sorts[i], isSelected);
                chip.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    selectedCatalogSort = sVal;
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
            chip.setTextColor(Color.parseColor("#080C14"));
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

    private void displayResults(List<Anime> list, String emptyTitle, String emptyDesc) {
        mainHandler.post(() -> {
            setLoading(false);
            if (list == null || list.isEmpty()) {
                adapter.setData(new ArrayList<>());
                layoutEmpty.setVisibility(View.VISIBLE);
                tvEmptyTitle.setText(emptyTitle);
                tvEmptyDesc.setText(emptyDesc);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                adapter.setData(list);
            }
        });
    }

    private void loadCurrentFeed() {
        setLoading(true);

        switch (currentTab) {
            case MUSIM:
                AniListClient.getSeasonal(AniListClient.getCurrentSeason(), 0, 1, 30, createCallback("Gagal Memuat Musim Ini"));
                break;
            case AIRING:
                AniListClient.getAiring(1, 30, createCallback("Gagal Memuat Sedang Tayang"));
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
                AniListClient.getCatalog(selectedCatalogFormat, selectedCatalogSort, 1, 30, createCallback("Gagal Memuat Katalog"));
                break;
            case GENRE:
                AniListClient.getByGenre(selectedGenre, 1, 30, createCallback("Gagal Memuat Genre " + selectedGenre));
                break;
        }
    }

    private AniListClient.Callback<List<Anime>> createCallback(String errorLabel) {
        return new AniListClient.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result) {
                displayResults(result, "Tidak Ada Anime", "Coba segarkan halaman atau periksa filter.");
            }

            @Override
            public void onError(Throwable t) {
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(MainActivity.this, errorLabel + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    displayResults(new ArrayList<>(), "Gagal Terhubung", "Periksa koneksi internet kamu.");
                });
            }
        };
    }

    private void performSearch(String keyword) {
        setLoading(true);
        AniListClient.search(keyword, 1, 30, new AniListClient.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result) {
                displayResults(result, "Anime Tidak Ditemukan", "Coba kata kunci yang lebih spesifik.");
            }

            @Override
            public void onError(Throwable t) {
                mainHandler.post(() -> {
                    setLoading(false);
                    displayResults(new ArrayList<>(), "Pencarian Gagal", t.getMessage());
                });
            }
        });
    }
}
