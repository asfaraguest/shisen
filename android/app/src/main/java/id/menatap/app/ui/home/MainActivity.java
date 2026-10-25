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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import id.menatap.app.R;
import id.menatap.app.data.db.MenatapDatabase;
import id.menatap.app.data.model.Anime;
import id.menatap.app.data.service.AniListClient;
import id.menatap.app.data.service.OtakudesuScraper;
import id.menatap.app.ui.adapter.AnimeAdapter;
import id.menatap.app.util.HapticHelper;

public class MainActivity extends AppCompatActivity {

    private enum ChipMode {
        TRENDING, SEASONAL, POPULAR, MOVIES, OTAKU
    }

    private enum DockMode {
        HOME, BOOKMARKS, HISTORY
    }

    private ProgressBar pbLoading;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvGrid;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;

    // Filter Chips
    private TextView chipTrending;
    private TextView chipSeasonal;
    private TextView chipPopular;
    private TextView chipMovies;
    private TextView chipOtaku;

    // Bottom Dock Items
    private TextView dockHome;
    private TextView dockBookmarks;
    private TextView dockHistory;

    private AnimeAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;

    private ChipMode currentChip = ChipMode.TRENDING;
    private DockMode currentDock = DockMode.HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        setupListeners();
        loadCurrentFeed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentDock == DockMode.BOOKMARKS || currentDock == DockMode.HISTORY) {
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

        chipTrending = findViewById(R.id.chip_filter_trending);
        chipSeasonal = findViewById(R.id.chip_filter_seasonal);
        chipPopular = findViewById(R.id.chip_filter_popular);
        chipMovies = findViewById(R.id.chip_filter_movies);
        chipOtaku = findViewById(R.id.chip_filter_otaku);

        dockHome = findViewById(R.id.nav_dock_home);
        dockBookmarks = findViewById(R.id.nav_dock_bookmarks);
        dockHistory = findViewById(R.id.nav_dock_history);

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

        // Chip Clicks
        chipTrending.setOnClickListener(v -> selectChip(ChipMode.TRENDING));
        chipSeasonal.setOnClickListener(v -> selectChip(ChipMode.SEASONAL));
        chipPopular.setOnClickListener(v -> selectChip(ChipMode.POPULAR));
        chipMovies.setOnClickListener(v -> selectChip(ChipMode.MOVIES));
        chipOtaku.setOnClickListener(v -> selectChip(ChipMode.OTAKU));

        // Dock Clicks
        dockHome.setOnClickListener(v -> selectDock(DockMode.HOME));
        dockBookmarks.setOnClickListener(v -> selectDock(DockMode.BOOKMARKS));
        dockHistory.setOnClickListener(v -> selectDock(DockMode.HISTORY));

        // Search TextWatcher
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

    private void selectChip(ChipMode mode) {
        if (currentChip == mode && currentDock == DockMode.HOME) return;
        currentChip = mode;
        currentDock = DockMode.HOME;
        updateDockUI();
        updateChipsUI();
        loadCurrentFeed();
    }

    private void selectDock(DockMode mode) {
        if (currentDock == mode) return;
        currentDock = mode;
        updateDockUI();
        loadCurrentFeed();
    }

    private void updateChipsUI() {
        TextView[] chips = {chipTrending, chipSeasonal, chipPopular, chipMovies, chipOtaku};
        ChipMode[] modes = {ChipMode.TRENDING, ChipMode.SEASONAL, ChipMode.POPULAR, ChipMode.MOVIES, ChipMode.OTAKU};

        for (int i = 0; i < chips.length; i++) {
            boolean isActive = (modes[i] == currentChip && currentDock == DockMode.HOME);
            if (isActive) {
                chips[i].setBackgroundResource(R.drawable.bg_chip_frost_active);
                chips[i].setTextColor(Color.parseColor("#080C14"));
            } else {
                chips[i].setBackgroundResource(R.drawable.bg_chip_frost_inactive);
                chips[i].setTextColor(ContextCompat.getColor(this, R.color.text_frost_subtle));
            }
        }
    }

    private void updateDockUI() {
        int colorActive = ContextCompat.getColor(this, R.color.accent_glacial);
        int colorDim = ContextCompat.getColor(this, R.color.text_frost_subtle);

        dockHome.setTextColor(currentDock == DockMode.HOME ? colorActive : colorDim);
        dockBookmarks.setTextColor(currentDock == DockMode.BOOKMARKS ? colorActive : colorDim);
        dockHistory.setTextColor(currentDock == DockMode.HISTORY ? colorActive : colorDim);

        if (currentDock != DockMode.HOME) {
            // Dim chips visually when on bookmarks/history
            for (TextView c : new TextView[]{chipTrending, chipSeasonal, chipPopular, chipMovies, chipOtaku}) {
                c.setBackgroundResource(R.drawable.bg_chip_frost_inactive);
                c.setTextColor(ContextCompat.getColor(this, R.color.text_frost_dim));
            }
        } else {
            updateChipsUI();
        }
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

        if (currentDock == DockMode.BOOKMARKS) {
            new Thread(() -> {
                List<Anime> bookmarks = MenatapDatabase.getInstance(this).getBookmarkList();
                displayResults(bookmarks, "Belum Ada Favorit", "Simpan anime favoritmu untuk ditonton nanti.");
            }).start();
            return;
        }

        if (currentDock == DockMode.HISTORY) {
            new Thread(() -> {
                List<Anime> history = MenatapDatabase.getInstance(this).getHistoryList();
                displayResults(history, "Riwayat Kosong", "Anime yang kamu tonton akan muncul di sini.");
            }).start();
            return;
        }

        // Home Chips
        switch (currentChip) {
            case TRENDING:
                AniListClient.getTrending(1, 30, createAniListCallback("Gagal Memuat Trending"));
                break;
            case SEASONAL:
                AniListClient.getSeasonal(AniListClient.getCurrentSeason(), 0, 1, 30, createAniListCallback("Gagal Memuat Musim Ini"));
                break;
            case POPULAR:
                AniListClient.getPopular(1, 30, createAniListCallback("Gagal Memuat Terpopuler"));
                break;
            case MOVIES:
                AniListClient.getMovies(1, 30, createAniListCallback("Gagal Memuat Film Anime"));
                break;
            case OTAKU:
                OtakudesuScraper.getOngoing(new OtakudesuScraper.Callback<List<Anime>>() {
                    @Override
                    public void onSuccess(List<Anime> result) {
                        displayResults(result, "Tidak Ada Update", "Belum ada rilis episode baru hari ini.");
                    }

                    @Override
                    public void onError(Throwable t) {
                        mainHandler.post(() -> {
                            setLoading(false);
                            Toast.makeText(MainActivity.this, "Gagal memuat Sub Indo: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            displayResults(new ArrayList<>(), "Gagal Memuat", "Periksa koneksi internet kamu.");
                        });
                    }
                });
                break;
        }
    }

    private AniListClient.Callback<List<Anime>> createAniListCallback(String errorLabel) {
        return new AniListClient.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result) {
                displayResults(result, "Tidak Ada Data", "Coba segarkan halaman atau periksa filter.");
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
        if (currentChip == ChipMode.OTAKU) {
            OtakudesuScraper.searchAnime(keyword, new OtakudesuScraper.Callback<List<Anime>>() {
                @Override
                public void onSuccess(List<Anime> result) {
                    displayResults(result, "Anime Tidak Ditemukan", "Coba kata kunci lain di katalog Sub Indo.");
                }

                @Override
                public void onError(Throwable t) {
                    mainHandler.post(() -> {
                        setLoading(false);
                        displayResults(new ArrayList<>(), "Pencarian Gagal", t.getMessage());
                    });
                }
            });
        } else {
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
}
