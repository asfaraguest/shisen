package id.menatap.app.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import id.menatap.app.R;
import id.menatap.app.data.db.MenatapDatabase;
import id.menatap.app.data.model.Anime;
import id.menatap.app.data.service.AniListClient;
import id.menatap.app.ui.adapter.AnimeAdapter;
import id.menatap.app.ui.adapter.HistoryHorizontalAdapter;
import id.menatap.app.ui.adapter.PosterHorizontalAdapter;
import id.menatap.app.ui.detail.DetailActivity;
import id.menatap.app.util.HapticHelper;
import id.menatap.app.util.ImageLoader;

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

    // Netflix Home Views
    private NestedScrollView scrollNetflixHome;
    private View viewHeroBillboard;
    private ImageView ivHeroBackdrop;
    private TextView tvHeroGenres;
    private TextView tvHeroTitle;
    private TextView tvHeroFormat;
    private TextView tvHeroScore;
    private TextView tvHeroEps;
    private Button btnHeroPlay;
    private Button btnHeroInfo;

    // Swimlanes
    private View swimlaneHistoryView;
    private TextView tvSwimlaneHistoryTitle;
    private RecyclerView rvSwimlaneHistory;
    private HistoryHorizontalAdapter historyAdapter;

    private View swimlaneTrendingView;
    private TextView tvSwimlaneTrendingTitle;
    private RecyclerView rvSwimlaneTrending;
    private PosterHorizontalAdapter trendingAdapter;

    private View swimlaneSeasonalView;
    private TextView tvSwimlaneSeasonalTitle;
    private RecyclerView rvSwimlaneSeasonal;
    private PosterHorizontalAdapter seasonalAdapter;

    private View swimlaneScheduleView;
    private TextView tvSwimlaneScheduleTitle;
    private RecyclerView rvSwimlaneSchedule;
    private PosterHorizontalAdapter scheduleAdapter;

    private View swimlanePopularView;
    private TextView tvSwimlanePopularTitle;
    private RecyclerView rvSwimlanePopular;
    private PosterHorizontalAdapter popularAdapter;

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

    private AnimeAdapter gridAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;

    private MainTab currentTab = MainTab.MUSIM;
    private Anime currentHeroAnime = null;

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
        setupNetflixCarousels();
        setupRecyclerView();
        setupListeners();
        selectTab(MainTab.MUSIM);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentTab == MainTab.MUSIM) {
            refreshHistorySwimlane();
        } else if (currentTab == MainTab.RIWAYAT || currentTab == MainTab.FAVORIT) {
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

        scrollNetflixHome = findViewById(R.id.scroll_netflix_home);
        viewHeroBillboard = findViewById(R.id.view_hero_billboard);
        ivHeroBackdrop = viewHeroBillboard.findViewById(R.id.iv_hero_backdrop);
        tvHeroGenres = viewHeroBillboard.findViewById(R.id.tv_hero_genres);
        tvHeroTitle = viewHeroBillboard.findViewById(R.id.tv_hero_title);
        tvHeroFormat = viewHeroBillboard.findViewById(R.id.tv_hero_format);
        tvHeroScore = viewHeroBillboard.findViewById(R.id.tv_hero_score);
        tvHeroEps = viewHeroBillboard.findViewById(R.id.tv_hero_eps);
        btnHeroPlay = viewHeroBillboard.findViewById(R.id.btn_hero_play);
        btnHeroInfo = viewHeroBillboard.findViewById(R.id.btn_hero_info);

        // Swimlane views
        swimlaneHistoryView = findViewById(R.id.swimlane_history);
        tvSwimlaneHistoryTitle = swimlaneHistoryView.findViewById(R.id.tv_swimlane_title);
        tvSwimlaneHistoryTitle.setText("Lanjutkan Menonton");
        swimlaneHistoryView.findViewById(R.id.btn_swimlane_more).setVisibility(View.GONE);
        rvSwimlaneHistory = swimlaneHistoryView.findViewById(R.id.rv_swimlane_items);

        swimlaneTrendingView = findViewById(R.id.swimlane_trending);
        tvSwimlaneTrendingTitle = swimlaneTrendingView.findViewById(R.id.tv_swimlane_title);
        tvSwimlaneTrendingTitle.setText("Sedang Tren Sekarang");
        swimlaneTrendingView.findViewById(R.id.btn_swimlane_more).setOnClickListener(v -> selectTab(MainTab.AIRING));
        rvSwimlaneTrending = swimlaneTrendingView.findViewById(R.id.rv_swimlane_items);

        swimlaneSeasonalView = findViewById(R.id.swimlane_seasonal);
        tvSwimlaneSeasonalTitle = swimlaneSeasonalView.findViewById(R.id.tv_swimlane_title);
        tvSwimlaneSeasonalTitle.setText("Anime Musim Ini");
        swimlaneSeasonalView.findViewById(R.id.btn_swimlane_more).setOnClickListener(v -> selectTab(MainTab.KATALOG));
        rvSwimlaneSeasonal = swimlaneSeasonalView.findViewById(R.id.rv_swimlane_items);

        swimlaneScheduleView = findViewById(R.id.swimlane_schedule);
        tvSwimlaneScheduleTitle = swimlaneScheduleView.findViewById(R.id.tv_swimlane_title);
        tvSwimlaneScheduleTitle.setText("Jadwal Tayang Hari Ini");
        swimlaneScheduleView.findViewById(R.id.btn_swimlane_more).setOnClickListener(v -> selectTab(MainTab.JADWAL));
        rvSwimlaneSchedule = swimlaneScheduleView.findViewById(R.id.rv_swimlane_items);

        swimlanePopularView = findViewById(R.id.swimlane_popular);
        tvSwimlanePopularTitle = swimlanePopularView.findViewById(R.id.tv_swimlane_title);
        tvSwimlanePopularTitle.setText("Populer Sepanjang Masa");
        swimlanePopularView.findViewById(R.id.btn_swimlane_more).setOnClickListener(v -> selectTab(MainTab.KATALOG));
        rvSwimlanePopular = swimlanePopularView.findViewById(R.id.rv_swimlane_items);

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

    private void setupNetflixCarousels() {
        // History horizontal
        historyAdapter = new HistoryHorizontalAdapter(this);
        rvSwimlaneHistory.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSwimlaneHistory.setAdapter(historyAdapter);

        // Trending horizontal
        trendingAdapter = new PosterHorizontalAdapter(this);
        rvSwimlaneTrending.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSwimlaneTrending.setAdapter(trendingAdapter);

        // Seasonal horizontal
        seasonalAdapter = new PosterHorizontalAdapter(this);
        rvSwimlaneSeasonal.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSwimlaneSeasonal.setAdapter(seasonalAdapter);

        // Schedule horizontal
        scheduleAdapter = new PosterHorizontalAdapter(this);
        rvSwimlaneSchedule.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSwimlaneSchedule.setAdapter(scheduleAdapter);

        // Popular horizontal
        popularAdapter = new PosterHorizontalAdapter(this);
        rvSwimlanePopular.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSwimlanePopular.setAdapter(popularAdapter);
    }

    private void setupRecyclerView() {
        gridAdapter = new AnimeAdapter(this);
        GridLayoutManager glm = new GridLayoutManager(this, 2);
        rvGrid.setLayoutManager(glm);
        rvGrid.setAdapter(gridAdapter);
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

        // Hero CTA listeners
        btnHeroPlay.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            if (currentHeroAnime != null) {
                Intent intent = new Intent(this, DetailActivity.class);
                intent.putExtra("anime", currentHeroAnime);
                startActivity(intent);
            }
        });

        btnHeroInfo.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            if (currentHeroAnime != null) {
                Intent intent = new Intent(this, DetailActivity.class);
                intent.putExtra("anime", currentHeroAnime);
                startActivity(intent);
            }
        });

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
                tabs[i].setTextColor(Color.parseColor("#090D16"));
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

            View div = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(2, (int) (18 * getResources().getDisplayMetrics().density));
            lp.setMargins((int) (8 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
            div.setLayoutParams(lp);
            div.setBackgroundColor(ContextCompat.getColor(this, R.color.frost_border));
            layoutSubChips.addView(div);

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

    private void refreshHistorySwimlane() {
        new Thread(() -> {
            List<Anime> history = MenatapDatabase.getInstance(this).getHistoryList();
            mainHandler.post(() -> {
                if (history != null && !history.isEmpty()) {
                    swimlaneHistoryView.setVisibility(View.VISIBLE);
                    historyAdapter.setData(history);
                } else {
                    swimlaneHistoryView.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void loadCurrentFeed() {
        String searchQuery = etSearch.getText().toString().trim();
        if (!searchQuery.isEmpty()) {
            performSearch(searchQuery);
            return;
        }

        if (currentTab == MainTab.MUSIM) {
            // MODE 1: NETFLIX-CLASS HOME EXPERIENCE
            scrollNetflixHome.setVisibility(View.VISIBLE);
            rvGrid.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            setLoading(true);

            refreshHistorySwimlane();

            // 1. Trending & Hero Spotlight
            AniListClient.getTrending(1, 15, new AniListClient.Callback<List<Anime>>() {
                @Override
                public void onSuccess(List<Anime> result) {
                    mainHandler.post(() -> {
                        if (result != null && !result.isEmpty()) {
                            bindHeroBillboard(result.get(0));
                            // Remaining list goes to trending swimlane
                            List<Anime> carouselItems = (result.size() > 1) ? result.subList(1, result.size()) : result;
                            trendingAdapter.setData(carouselItems);
                        }
                    });
                }

                @Override
                public void onError(Throwable t) {}
            });

            // 2. Seasonal Hits
            AniListClient.getSeasonal(AniListClient.getCurrentSeason(), 0, 1, 15, new AniListClient.Callback<List<Anime>>() {
                @Override
                public void onSuccess(List<Anime> result) {
                    mainHandler.post(() -> seasonalAdapter.setData(result));
                }

                @Override
                public void onError(Throwable t) {}
            });

            // 3. Today's Airing Schedule
            int todayCalendar = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
            AniListClient.getSchedule(todayCalendar, new AniListClient.Callback<List<Anime>>() {
                @Override
                public void onSuccess(List<Anime> result) {
                    mainHandler.post(() -> scheduleAdapter.setData(result));
                }

                @Override
                public void onError(Throwable t) {}
            });

            // 4. All-Time Popular
            AniListClient.getPopular(1, 15, new AniListClient.Callback<List<Anime>>() {
                @Override
                public void onSuccess(List<Anime> result) {
                    mainHandler.post(() -> {
                        setLoading(false);
                        popularAdapter.setData(result);
                    });
                }

                @Override
                public void onError(Throwable t) {
                    mainHandler.post(() -> setLoading(false));
                }
            });

        } else {
            // MODE 2: FULL CATALOG GRID
            scrollNetflixHome.setVisibility(View.GONE);
            rvGrid.setVisibility(View.VISIBLE);
            setLoading(true);

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

    private void bindHeroBillboard(Anime anime) {
        if (anime == null) return;
        currentHeroAnime = anime;

        String displayTitle = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title : anime.romajiTitle;
        tvHeroTitle.setText(displayTitle);

        if (anime.genres != null && !anime.genres.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int count = Math.min(3, anime.genres.size());
            for (int i = 0; i < count; i++) {
                sb.append(anime.genres.get(i).toUpperCase(Locale.US));
                if (i < count - 1) sb.append(" • ");
            }
            tvHeroGenres.setText(sb.toString());
            tvHeroGenres.setVisibility(View.VISIBLE);
        } else {
            tvHeroGenres.setVisibility(View.GONE);
        }

        tvHeroFormat.setText(anime.format != null ? anime.format : "TV");

        if (anime.score > 0) {
            tvHeroScore.setVisibility(View.VISIBLE);
            tvHeroScore.setText(String.format(Locale.US, "%.1f", anime.score));
        } else {
            tvHeroScore.setVisibility(View.GONE);
        }

        if (anime.epsCounter != null && !anime.epsCounter.isEmpty() && !"null".equalsIgnoreCase(anime.epsCounter)) {
            tvHeroEps.setText(anime.epsCounter);
            tvHeroEps.setVisibility(View.VISIBLE);
        } else {
            tvHeroEps.setVisibility(View.GONE);
        }

        String heroImg = (anime.banner != null && !anime.banner.isEmpty() && !"null".equalsIgnoreCase(anime.banner))
                ? anime.banner : anime.poster;
        ImageLoader.load(heroImg, ivHeroBackdrop);
    }

    private void displayResults(List<Anime> list, String emptyTitle, String emptyDesc) {
        mainHandler.post(() -> {
            setLoading(false);
            if (list == null || list.isEmpty()) {
                gridAdapter.setData(new ArrayList<>());
                layoutEmpty.setVisibility(View.VISIBLE);
                tvEmptyTitle.setText(emptyTitle);
                tvEmptyDesc.setText(emptyDesc);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                gridAdapter.setData(list);
            }
        });
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
        scrollNetflixHome.setVisibility(View.GONE);
        rvGrid.setVisibility(View.VISIBLE);
        scrollSubFilters.setVisibility(View.GONE);
        setLoading(true);

        AniListClient.search(keyword, 1, 40, new AniListClient.Callback<List<Anime>>() {
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
