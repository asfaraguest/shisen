package id.shisen.app.ui.home;

import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import id.shisen.app.data.subtitle.AiSubtitleService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import id.shisen.app.R;
import id.shisen.app.data.cache.FeedCache;
import id.shisen.app.data.db.MenatapDatabase;
import id.shisen.app.ui.dialog.SettingsDialog;
import id.shisen.app.util.AppSettings;
import id.shisen.app.data.model.Anime;
import id.shisen.app.data.service.AniListClient;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import id.shisen.app.util.AnimeThemeManager;
import id.shisen.app.ui.adapter.AnimeAdapter;
import id.shisen.app.util.AppUpdateManager;
import id.shisen.app.util.HapticHelper;
import id.shisen.app.util.LocaleHelper;
import id.shisen.app.util.SystemUiHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    public enum MainTab {
        MUSIM, AIRING, JADWAL, RIWAYAT, FAVORIT, KATALOG, GENRE
    }

    // Root Layout & Theme Support
    private View layoutMainRoot;

    // Top Aurora Bar & Dynamic Fluid Search Views
    private FrameLayout layoutAuroraTop;
    private LinearLayout layoutTopNormal;
    private TextView tvAppLogo;
    private TextView tvAppLogoKanji;
    public TextView tvCurrentMenuBadge;
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

    private id.shisen.app.ui.DeviceNav deviceNav;

    // Menu Items in Folded Floating Menu / TV Left Rail
    public View menuItemMusim, menuItemAiring, menuItemJadwal, menuItemRiwayat, menuItemFavorit, menuItemKatalog, menuItemGenre, menuItemSettings;
    public TextView tvMenuMusim, tvMenuAiring, tvMenuJadwal, tvMenuRiwayat, tvMenuFavorit, tvMenuKatalog, tvMenuGenre;

    public MainTab getCurrentTab() {
        return currentTab;
    }

    private AnimeAdapter gridAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;

    // Floating Transparent Pagination Bar Components
    private LinearLayout layoutPagination;
    private TextView btnPrevPage;
    private TextView tvPageIndicator;
    private TextView btnNextPage;
    private int currentPage = 1;
    private boolean isPaginationFolded = false;
    private final Runnable expandPaginationRunnable = () -> setPaginationFolded(false);

    private MainTab currentTab = MainTab.MUSIM;
    private boolean isSearchExpanded = false;
    private boolean focusGridOnNextLoad = false;

    public boolean isSearchExpanded() {
        return isSearchExpanded;
    }

    public boolean focusFirstGridItem() {
        if (rvGrid == null || rvGrid.getVisibility() != View.VISIBLE || rvGrid.getChildCount() <= 0) {
            return false;
        }
        View first = rvGrid.getChildAt(0);
        if (first != null) {
            View heroBtn = first.findViewById(R.id.btn_hero_play);
            if (heroBtn != null && heroBtn.getVisibility() == View.VISIBLE && heroBtn.isFocusable()) {
                return heroBtn.requestFocus();
            }
            return first.requestFocus();
        }
        return false;
    }

    // Contextual Sub-Filter States
    private int selectedDayOfWeek = Calendar.MONDAY;
    private String selectedGenre = "Action";
    private String selectedAiringLane = "ALL";

    // Full Tatap Catalog Cross-Filter States
    private String filterCatalogSort = "popularity";
    private String filterCatalogYear = "";
    private String filterCatalogSeason = "";
    private String filterCatalogMonth = "";
    private String filterCatalogType = "";
    private String filterCatalogStatus = "";

    public static class GenreItem {
        public final String title;
        public final String slug;
        public GenreItem(String title, String slug) {
            this.title = title;
            this.slug = slug;
        }
    }

    // Complete 90+ Genre List (Full Tatap Parity, Zero-Tolerance Hentai)
    public static final List<GenreItem> TATAP_GENRES = Arrays.asList(
            new GenreItem("Action", "action"),
            new GenreItem("Action & Adventure", "action-adventure"),
            new GenreItem("Adult Cast", "adult-cast"),
            new GenreItem("Adventure", "adventure"),
            new GenreItem("Animation", "animation"),
            new GenreItem("Anthropomorphic", "anthropomorphic"),
            new GenreItem("Avant Garde", "avant-garde"),
            new GenreItem("Award Winning", "award-winning"),
            new GenreItem("Boys Love", "boys-love"),
            new GenreItem("Cars", "cars"),
            new GenreItem("CGDCT", "cgdct"),
            new GenreItem("Childcare", "childcare"),
            new GenreItem("Combat Sports", "combat-sports"),
            new GenreItem("Comedy", "comedy"),
            new GenreItem("Crossdressing", "crossdressing"),
            new GenreItem("Delinquents", "delinquents"),
            new GenreItem("Dementia", "dementia"),
            new GenreItem("Demons", "demons"),
            new GenreItem("Detective", "detective"),
            new GenreItem("Drama", "drama"),
            new GenreItem("Educational", "educational"),
            new GenreItem("Fantasy", "fantasy"),
            new GenreItem("Gag Humor", "gag-humor"),
            new GenreItem("Game", "game"),
            new GenreItem("Girls Love", "girls-love"),
            new GenreItem("Gore", "gore"),
            new GenreItem("Gourmet", "gourmet"),
            new GenreItem("Harem", "harem"),
            new GenreItem("High Stakes Game", "high-stakes-game"),
            new GenreItem("Historical", "historical"),
            new GenreItem("Horror", "horror"),
            new GenreItem("Idols (Female)", "idols-female"),
            new GenreItem("Idols (Male)", "idols-male"),
            new GenreItem("Isekai", "isekai"),
            new GenreItem("Iyashikei", "iyashikei"),
            new GenreItem("Josei", "josei"),
            new GenreItem("Kids", "kids"),
            new GenreItem("Love Polygon", "love-polygon"),
            new GenreItem("Love Status Quo", "love-status-quo"),
            new GenreItem("Magic", "magic"),
            new GenreItem("Mahou Shoujo", "mahou-shoujo"),
            new GenreItem("Martial Arts", "martial-arts"),
            new GenreItem("Mecha", "mecha"),
            new GenreItem("Medical", "medical"),
            new GenreItem("Military", "military"),
            new GenreItem("Music", "music"),
            new GenreItem("Mystery", "mystery"),
            new GenreItem("Mythology", "mythology"),
            new GenreItem("Organized Crime", "organized-crime"),
            new GenreItem("Otaku Culture", "otaku-culture"),
            new GenreItem("Parody", "parody"),
            new GenreItem("Performing Arts", "performing-arts"),
            new GenreItem("Pets", "pets"),
            new GenreItem("Police", "police"),
            new GenreItem("Psychological", "psychological"),
            new GenreItem("Racing", "racing"),
            new GenreItem("Reincarnation", "reincarnation"),
            new GenreItem("Reverse Harem", "reverse-harem"),
            new GenreItem("Romance", "romance"),
            new GenreItem("Samurai", "samurai"),
            new GenreItem("School", "school"),
            new GenreItem("Sci-Fi", "sci-fi"),
            new GenreItem("Sci-Fi & Fantasy", "sci-fi-fantasy"),
            new GenreItem("Seinen", "seinen"),
            new GenreItem("Shoujo", "shoujo"),
            new GenreItem("Shoujo Ai", "shoujo-ai"),
            new GenreItem("Shounen", "shounen"),
            new GenreItem("Shounen Ai", "shounen-ai"),
            new GenreItem("Showbiz", "showbiz"),
            new GenreItem("Slice of Life", "slice-of-life"),
            new GenreItem("Space", "space"),
            new GenreItem("Sports", "sports"),
            new GenreItem("Strategy Game", "strategy-game"),
            new GenreItem("Supernatural", "supernatural"),
            new GenreItem("Super Power", "super-power"),
            new GenreItem("Survival", "survival"),
            new GenreItem("Suspense", "suspense"),
            new GenreItem("Team Sports", "team-sports"),
            new GenreItem("Thriller", "thriller"),
            new GenreItem("Time Travel", "time-travel"),
            new GenreItem("Urban Fantasy", "urban-fantasy"),
            new GenreItem("Vampire", "vampire"),
            new GenreItem("Video Game", "video-game"),
            new GenreItem("Villainess", "villainess"),
            new GenreItem("Visual Arts", "visual-arts"),
            new GenreItem("Workplace", "workplace")
    );

    private String[] getDayNames() {
        return new String[]{
                getString(R.string.day_mon),
                getString(R.string.day_tue),
                getString(R.string.day_wed),
                getString(R.string.day_thu),
                getString(R.string.day_fri),
                getString(R.string.day_sat),
                getString(R.string.day_sun)
        };
    }

    private final int[] DAYS_CALENDAR = {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SystemUiHelper.applySystemBarsTheme(this);

        // Auto-select today in Jakarta / WIB timezone
        Calendar wibCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"));
        selectedDayOfWeek = wibCal.get(Calendar.DAY_OF_WEEK);

        initViews();
        deviceNav = new id.shisen.app.ui.DeviceNav(this);
        deviceNav.init(getWindow().getDecorView());
        setupWindowInsets();
        setupRecyclerView();
        setupListeners();
        setupBackHandler();

        // Bersihkan sisa APK pembaruan lama di subfolder updates/ cache
        AppUpdateManager.cleanupAtStart(this);
        // Bersihkan entri cache lama (> 14 hari) di latar belakang
        FeedCache.getInstance(this).cleanupOldCache();

        MainTab targetTab = MainTab.MUSIM;
        if (savedInstanceState != null) {
            String tabName = savedInstanceState.getString("saved_tab");
            if (tabName != null) {
                try {
                    targetTab = MainTab.valueOf(tabName);
                } catch (Exception ignored) {}
            }
            currentPage = savedInstanceState.getInt("saved_page", 1);
        }
        selectTab(targetTab);

        // Logger performa DEBUG-ONLY: reportFullyDrawn resmi + log cold start sekali
        getWindow().getDecorView().post(() -> {
            id.shisen.app.util.PerfLogger.onFullyDrawn();
            try { reportFullyDrawn(); } catch (Throwable ignored) {}
        });

        // Silent background update check on app launch
        mainHandler.postDelayed(() -> AppUpdateManager.getInstance().checkUpdate(this, false), 2500);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentTab != null) {
            outState.putString("saved_tab", currentTab.name());
        }
        outState.putInt("saved_page", currentPage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyCurrentTheme();
        if (currentTab == MainTab.RIWAYAT || currentTab == MainTab.FAVORIT) {
            loadCurrentFeed();
        }
    }

    private void applyCurrentTheme() {
        if (layoutMainRoot != null) {
            layoutMainRoot.setBackgroundResource(AnimeThemeManager.getCanvasBackgroundResId(this));
        }
        SystemUiHelper.applySystemBarsTheme(this);
    }

    private void initViews() {
        layoutMainRoot = findViewById(R.id.layout_main_root);
        layoutAuroraTop = findViewById(R.id.layout_aurora_top);
        layoutTopNormal = findViewById(R.id.layout_top_normal);
        tvAppLogo = findViewById(R.id.tv_app_logo);
        tvAppLogoKanji = findViewById(R.id.tv_app_logo_kanji);
        String kanji = getString(R.string.app_logo_kanji);
        if (tvAppLogoKanji != null) {
            if (TextUtils.isEmpty(kanji)) {
                tvAppLogoKanji.setVisibility(View.GONE);
            } else {
                tvAppLogoKanji.setText(kanji);
                tvAppLogoKanji.setVisibility(View.VISIBLE);
            }
        }
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

        menuItemMusim = findViewById(R.id.menu_item_musim);
        menuItemAiring = findViewById(R.id.menu_item_airing);
        menuItemJadwal = findViewById(R.id.menu_item_jadwal);
        menuItemRiwayat = findViewById(R.id.menu_item_riwayat);
        menuItemFavorit = findViewById(R.id.menu_item_favorit);
        menuItemKatalog = findViewById(R.id.menu_item_katalog);
        menuItemGenre = findViewById(R.id.menu_item_genre);
        menuItemSettings = findViewById(R.id.menu_item_settings);

        tvMenuMusim = findViewById(R.id.tv_menu_musim);
        tvMenuAiring = findViewById(R.id.tv_menu_airing);
        tvMenuJadwal = findViewById(R.id.tv_menu_jadwal);
        tvMenuRiwayat = findViewById(R.id.tv_menu_riwayat);
        tvMenuFavorit = findViewById(R.id.tv_menu_favorit);
        tvMenuKatalog = findViewById(R.id.tv_menu_katalog);
        tvMenuGenre = findViewById(R.id.tv_menu_genre);

        layoutPagination = findViewById(R.id.layout_pagination);
        btnPrevPage = findViewById(R.id.btn_prev_page);
        tvPageIndicator = findViewById(R.id.tv_page_indicator);
        btnNextPage = findViewById(R.id.btn_next_page);

        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent_glacial));
        swipeRefresh.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.frost_surface));
    }

    private void setupWindowInsets() {
        if (layoutMainRoot == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(layoutMainRoot, (v, windowInsets) -> {
            androidx.core.graphics.Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            androidx.core.graphics.Insets displayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
            androidx.core.graphics.Insets navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());

            int topInset = Math.max(statusBars.top, displayCutout.top);
            int bottomInset = navBars.bottom;
            int leftInset = Math.max(navBars.left, displayCutout.left);
            int rightInset = Math.max(navBars.right, displayCutout.right);
            float density = getResources().getDisplayMetrics().density;

            if (layoutAuroraTop != null) {
                int baseHeight = (int) (54 * density);
                ViewGroup.LayoutParams lp = layoutAuroraTop.getLayoutParams();
                if (lp != null) {
                    lp.height = baseHeight + topInset;
                    layoutAuroraTop.setLayoutParams(lp);
                }
                layoutAuroraTop.setPadding(leftInset, topInset, rightInset, 0);
            }

            if (deviceNav != null) {
                deviceNav.applyInsets(topInset, bottomInset, leftInset, rightInset, density);
            }

            if (rvGrid != null) {
                int basePaddingBottom = (int) (80 * density);
                int basePaddingH = (int) (2 * density);
                rvGrid.setPadding(leftInset + basePaddingH, rvGrid.getPaddingTop(), rightInset + basePaddingH, basePaddingBottom + bottomInset);
            }

            return windowInsets;
        });
    }

    private void setupRecyclerView() {
        gridAdapter = new AnimeAdapter(this);
        int gridSpan = calculateGridSpan();
        GridLayoutManager glm = new GridLayoutManager(this, gridSpan);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int type = gridAdapter.getItemViewType(position);
                if (type == AnimeAdapter.VIEW_TYPE_HERO || type == AnimeAdapter.VIEW_TYPE_HEADER) {
                    return glm.getSpanCount(); // Spans full columns for Hero Spotlight & Section Headers
                }
                return 1;
            }
        });
        rvGrid.setLayoutManager(glm);
        rvGrid.setAdapter(gridAdapter);

        // Auto-Fold pagination on scroll, auto-expand on idle
        rvGrid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (layoutPagination == null || layoutPagination.getVisibility() != View.VISIBLE) return;

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    mainHandler.removeCallbacks(expandPaginationRunnable);
                    setPaginationFolded(true);
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mainHandler.removeCallbacks(expandPaginationRunnable);
                    mainHandler.postDelayed(expandPaginationRunnable, 300);
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (layoutPagination == null || layoutPagination.getVisibility() != View.VISIBLE) return;

                if (Math.abs(dy) > 6 && !isPaginationFolded) {
                    mainHandler.removeCallbacks(expandPaginationRunnable);
                    setPaginationFolded(true);
                }
            }
        });
    }

    private int calculateGridSpan() {
        float density = getResources().getDisplayMetrics().density;
        int widthDp = (int) (getResources().getDisplayMetrics().widthPixels / density);
        if (deviceNav != null) {
            return deviceNav.getGridSpan(widthDp);
        }
        if (widthDp >= 920) return 5;
        if (widthDp >= 640) return 4;
        if (widthDp >= 480) return 3;
        return 2;
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (rvGrid != null && rvGrid.getLayoutManager() instanceof GridLayoutManager) {
            ((GridLayoutManager) rvGrid.getLayoutManager()).setSpanCount(calculateGridSpan());
            gridAdapter.notifyDataSetChanged();
        }
        if (layoutMainRoot != null) {
            layoutMainRoot.requestApplyInsets();
        }
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            loadCurrentFeed();
        });

        // Pagination buttons
        if (btnPrevPage != null) {
            btnPrevPage.setOnClickListener(v -> {
                if (currentPage > 1) {
                    HapticHelper.performClick(v);
                    currentPage--;
                    loadCurrentFeed();
                    rvGrid.scrollToPosition(0);
                }
            });
        }

        if (btnNextPage != null) {
            btnNextPage.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                currentPage++;
                loadCurrentFeed();
                rvGrid.scrollToPosition(0);
            });
        }

        if (layoutPagination != null) {
            layoutPagination.setOnClickListener(v -> {
                if (isPaginationFolded) {
                    HapticHelper.performClick(v);
                    mainHandler.removeCallbacks(expandPaginationRunnable);
                    setPaginationFolded(false);
                }
            });
        }

        // 1. Folded Floating Menu Triggers (Bottom-Right FAB + Scrim)
        // 7 Menu Items
        if (menuItemMusim != null) menuItemMusim.setOnClickListener(v -> selectMenuItem(MainTab.MUSIM));
        if (menuItemAiring != null) menuItemAiring.setOnClickListener(v -> selectMenuItem(MainTab.AIRING));
        if (menuItemJadwal != null) menuItemJadwal.setOnClickListener(v -> selectMenuItem(MainTab.JADWAL));
        if (menuItemRiwayat != null) menuItemRiwayat.setOnClickListener(v -> selectMenuItem(MainTab.RIWAYAT));
        if (menuItemFavorit != null) menuItemFavorit.setOnClickListener(v -> selectMenuItem(MainTab.FAVORIT));
        if (menuItemKatalog != null) menuItemKatalog.setOnClickListener(v -> selectMenuItem(MainTab.KATALOG));
        if (menuItemGenre != null) menuItemGenre.setOnClickListener(v -> selectMenuItem(MainTab.GENRE));

        if (menuItemSettings != null) {
            menuItemSettings.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                if (deviceNav != null) deviceNav.onTabChanged(currentTab);
                openSettingsDialog(false);
            });
        }

        if (tvAppLogo != null) {
            tvAppLogo.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                AppUpdateManager.getInstance().checkUpdate(this, true);
            });
        }

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
                if (!q.isEmpty() && q.length() >= 3) {
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
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String q = etSearch.getText().toString().trim();
                if (!q.isEmpty()) {
                    hideKeyboard();
                    focusGridOnNextLoad = true;
                    performSearch(q);
                }
                return true;
            }
            return false;
        });

        etSearch.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    hideKeyboard();
                    if (focusFirstGridItem()) {
                        return true;
                    }
                    if (rvGrid != null) {
                        return rvGrid.requestFocus();
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    int sel = etSearch.getSelectionStart();
                    if (sel <= 0 && btnCollapseSearch != null) {
                        btnCollapseSearch.requestFocus();
                        return true;
                    }
                } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    String q = etSearch.getText().toString().trim();
                    if (!q.isEmpty()) {
                        hideKeyboard();
                        focusGridOnNextLoad = true;
                        performSearch(q);
                        return true;
                    }
                }
            }
            return false;
        });

        if (btnCollapseSearch != null) {
            btnCollapseSearch.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        etSearch.requestFocus();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        hideKeyboard();
                        if (focusFirstGridItem()) {
                            return true;
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (deviceNav != null) {
                            View railSearch = findViewById(R.id.btn_search_trigger);
                            if (railSearch != null) {
                                railSearch.requestFocus();
                                return true;
                            }
                        }
                    }
                }
                return false;
            });
        }

        // 3. Contextual Action Button (Riwayat: Hapus Semua, Katalog: Reset Filter, Genre: Pilih Genre)
        btnClearHistoryAll.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            if (currentTab == MainTab.RIWAYAT) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_clear_history_title)
                        .setMessage(R.string.dialog_clear_history_msg)
                        .setPositiveButton(R.string.action_clear_all, (d, which) -> {
                            new Thread(() -> {
                                MenatapDatabase.getInstance(this).clearAllHistory();
                                mainHandler.post(() -> {
                                    Toast.makeText(this, R.string.toast_history_cleared, Toast.LENGTH_SHORT).show();
                                    loadCurrentFeed();
                                });
                            }).start();
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .create();
                id.shisen.app.util.SystemUiHelper.setupDialogBounds(dialog, this);
                dialog.show();
            } else if (currentTab == MainTab.KATALOG) {
                if (isCatalogFilterActive()) {
                    resetCatalogFilterState();
                    updateTabsUI();
                    updateSubFilterUI();
                    loadCurrentFeed();
                } else {
                    showCatalogFilterDialog();
                }
            } else if (currentTab == MainTab.GENRE) {
                showGenreSelectorDialog();
            }
        });

        if (tvContextualTitle != null) {
            tvContextualTitle.setOnClickListener(v -> {
                if (currentTab == MainTab.KATALOG) {
                    HapticHelper.performClick(v);
                    showCatalogFilterDialog();
                } else if (currentTab == MainTab.GENRE) {
                    HapticHelper.performClick(v);
                    showGenreSelectorDialog();
                }
            });
        }
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isSearchExpanded) {
                    collapseFluidSearch();
                    if (deviceNav != null) {
                        deviceNav.handleBackPressed();
                    }
                    return;
                }
                if (deviceNav != null && deviceNav.handleBackPressed()) {
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /* package */ void selectMenuItem(MainTab tab) {
        if (deviceNav != null) {
            deviceNav.onTabChanged(tab);
        }
        if (tab == MainTab.GENRE) {
            showGenreSelectorDialog();
            return;
        }
        if (tab == MainTab.KATALOG && currentTab == MainTab.KATALOG) {
            showCatalogFilterDialog();
            return;
        }
        selectTab(tab);
    }

    private void selectTab(MainTab tab) {
        currentTab = tab;
        currentPage = 1;
        updateTabsUI();
        updateSubFilterUI();
        loadCurrentFeed();
    }

    private void updateTabsUI() {
        if (deviceNav != null) {
            deviceNav.onTabChanged(currentTab);
        }
        if (tvCurrentMenuBadge != null) {
            String badge = "";
            switch (currentTab) {
                case MUSIM: badge = getString(R.string.badge_this_season); break;
                case AIRING: badge = getString(R.string.badge_airing); break;
                case JADWAL: badge = getString(R.string.badge_schedule); break;
                case RIWAYAT: badge = getString(R.string.badge_history); break;
                case FAVORIT: badge = getString(R.string.badge_favorites); break;
                case KATALOG: badge = getString(R.string.badge_catalog); break;
                case GENRE: badge = getString(R.string.badge_genre); break;
            }
            tvCurrentMenuBadge.setText(badge);
        }
    }

    // --- DYNAMIC FLUID SEARCHBOX LOGIC ---

    private void expandFluidSearch() {
        if (isSearchExpanded) return;
        isSearchExpanded = true;
        if (deviceNav != null) deviceNav.onTabChanged(currentTab);

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

    public void collapseFluidSearch() {
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

        if (currentTab == MainTab.AIRING) {
            layoutContextualBar.setVisibility(View.GONE);
            scrollSubFilters.setVisibility(View.VISIBLE);

            String[] lanes = {"ALL", "CUR", "PREV", "LONG"};
            String[] laneLabels = {
                    getString(R.string.lane_all),
                    getString(R.string.lane_cur),
                    getString(R.string.lane_prev),
                    getString(R.string.lane_long)
            };
            for (int i = 0; i < lanes.length; i++) {
                final String l = lanes[i];
                boolean isSelected = l.equalsIgnoreCase(selectedAiringLane);

                TextView chip = createSubFilterChip(laneLabels[i], isSelected);
                chip.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    selectedAiringLane = l;
                    currentPage = 1;
                    updateSubFilterUI();
                    loadCurrentFeed();
                });
                layoutSubChips.addView(chip);
            }
        } else if (currentTab == MainTab.JADWAL) {
            layoutContextualBar.setVisibility(View.GONE);
            scrollSubFilters.setVisibility(View.VISIBLE);

            Calendar wibNow = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"));
            int todayWib = wibNow.get(Calendar.DAY_OF_WEEK);
            String[] dayNames = getDayNames();

            for (int i = 0; i < dayNames.length; i++) {
                final int calDay = DAYS_CALENDAR[i];
                boolean isSelected = (calDay == selectedDayOfWeek);
                String label = dayNames[i] + (calDay == todayWib ? getString(R.string.day_today_suffix) : "");

                TextView chip = createSubFilterChip(label, isSelected);
                chip.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    selectedDayOfWeek = calDay;
                    currentPage = 1;
                    updateSubFilterUI();
                    loadCurrentFeed();
                });
                layoutSubChips.addView(chip);
            }
        } else if (currentTab == MainTab.KATALOG) {
            layoutContextualBar.setVisibility(View.VISIBLE);
            tvContextualTitle.setText(getCatalogFilterSummary());
            btnClearHistoryAll.setText(isCatalogFilterActive() ? getString(R.string.action_reset) : getString(R.string.action_filter));
            btnClearHistoryAll.setVisibility(View.VISIBLE);
            scrollSubFilters.setVisibility(View.VISIBLE);

            // 1. Primary Action Chip: Open Full Cross-Filter Dialog
            TextView btnFilterDialog = createSubFilterChip("⚙ " + getString(R.string.action_filter), isCatalogFilterActive());
            btnFilterDialog.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                showCatalogFilterDialog();
            });
            layoutSubChips.addView(btnFilterDialog);

            // Separator
            View div = new View(this);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                    (int) (1.5 * getResources().getDisplayMetrics().density),
                    (int) (18 * getResources().getDisplayMetrics().density)
            );
            dLp.setMargins((int) (4 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
            div.setLayoutParams(dLp);
            div.setBackgroundColor(Color.parseColor("#243859"));
            layoutSubChips.addView(div);

            // Quick Sort Chips
            String[] sorts = {"popularity", "score", "latest", "trending", "az", "most_favorite"};
            String[] sortLabels = {
                    getString(R.string.sort_popular),
                    getString(R.string.sort_score),
                    getString(R.string.sort_recent),
                    getString(R.string.sort_trending),
                    getString(R.string.sort_alpha),
                    getString(R.string.sort_favorites)
            };
            for (int i = 0; i < sorts.length; i++) {
                final String s = sorts[i];
                boolean isSelected = s.equalsIgnoreCase(filterCatalogSort);

                TextView chip = createSubFilterChip(sortLabels[i], isSelected);
                chip.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    filterCatalogSort = s;
                    currentPage = 1;
                    updateSubFilterUI();
                    loadCurrentFeed();
                });
                layoutSubChips.addView(chip);
            }
        } else if (currentTab == MainTab.GENRE) {
            layoutContextualBar.setVisibility(View.VISIBLE);
            tvContextualTitle.setText(getString(R.string.label_genre_title, selectedGenre.toUpperCase(Locale.US)));
            btnClearHistoryAll.setText(getString(R.string.action_select_genre));
            btnClearHistoryAll.setVisibility(View.VISIBLE);
            scrollSubFilters.setVisibility(View.VISIBLE);

            // 1. Primary Action Chip: Open Genre Selector Dialog
            TextView btnPickGenre = createSubFilterChip("🔍 " + getString(R.string.action_select_genre), true);
            btnPickGenre.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                showGenreSelectorDialog();
            });
            layoutSubChips.addView(btnPickGenre);

            // Separator
            View div = new View(this);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                    (int) (1.5 * getResources().getDisplayMetrics().density),
                    (int) (18 * getResources().getDisplayMetrics().density)
            );
            dLp.setMargins((int) (4 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
            div.setLayoutParams(dLp);
            div.setBackgroundColor(Color.parseColor("#243859"));
            layoutSubChips.addView(div);

            // Top Quick Genres for Instant Access
            String[] quickGenres = {
                    "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Isekai",
                    "Magic", "Mecha", "Romance", "Sci-Fi", "Slice of Life", "Sports",
                    "Supernatural", "Thriller", "Psychological", "Horror", "Mystery"
            };
            for (String genre : quickGenres) {
                boolean isSelected = genre.equalsIgnoreCase(selectedGenre);

                TextView chip = createSubFilterChip(genre, isSelected);
                chip.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    selectedGenre = genre;
                    currentPage = 1;
                    updateSubFilterUI();
                    loadCurrentFeed();
                });
                layoutSubChips.addView(chip);
            }
        } else {
            scrollSubFilters.setVisibility(View.GONE);
        }
    }

    private boolean isCatalogFilterActive() {
        return !"popularity".equals(filterCatalogSort) ||
                !filterCatalogYear.isEmpty() ||
                !filterCatalogSeason.isEmpty() ||
                !filterCatalogMonth.isEmpty() ||
                !filterCatalogType.isEmpty() ||
                !filterCatalogStatus.isEmpty();
    }

    private String getCatalogFilterSummary() {
        List<String> parts = new ArrayList<>();
        if ("score".equals(filterCatalogSort)) parts.add(getString(R.string.sort_score).toUpperCase(Locale.US));
        else if ("latest".equals(filterCatalogSort)) parts.add(getString(R.string.sort_recent).toUpperCase(Locale.US));
        else if ("trending".equals(filterCatalogSort)) parts.add(getString(R.string.sort_trending).toUpperCase(Locale.US));
        else if ("az".equals(filterCatalogSort)) parts.add(getString(R.string.sort_alpha).toUpperCase(Locale.US));
        else if ("most_favorite".equals(filterCatalogSort)) parts.add(getString(R.string.sort_favorites).toUpperCase(Locale.US));
        else parts.add(getString(R.string.sort_popular).toUpperCase(Locale.US));

        if (!filterCatalogYear.isEmpty()) parts.add(filterCatalogYear.toUpperCase(Locale.US));
        if (!filterCatalogSeason.isEmpty()) {
            if ("spring".equals(filterCatalogSeason)) parts.add(getString(R.string.season_spring).toUpperCase(Locale.US));
            else if ("summer".equals(filterCatalogSeason)) parts.add(getString(R.string.season_summer).toUpperCase(Locale.US));
            else if ("fall".equals(filterCatalogSeason)) parts.add(getString(R.string.season_fall).toUpperCase(Locale.US));
            else if ("winter".equals(filterCatalogSeason)) parts.add(getString(R.string.season_winter).toUpperCase(Locale.US));
            else parts.add(filterCatalogSeason.toUpperCase(Locale.US));
        }
        if (!filterCatalogMonth.isEmpty()) {
            int[] monthRes = {
                    0,
                    R.string.month_1, R.string.month_2, R.string.month_3, R.string.month_4,
                    R.string.month_5, R.string.month_6, R.string.month_7, R.string.month_8,
                    R.string.month_9, R.string.month_10, R.string.month_11, R.string.month_12
            };
            try {
                int mi = Integer.parseInt(filterCatalogMonth);
                if (mi >= 1 && mi <= 12) {
                    String raw = getString(monthRes[mi]);
                    int parenIdx = raw.indexOf(" (");
                    parts.add((parenIdx > 0 ? raw.substring(0, parenIdx) : raw).toUpperCase(Locale.US));
                }
            } catch (Exception ignored) {}
        }
        if (!filterCatalogType.isEmpty()) parts.add(filterCatalogType.toUpperCase(Locale.US));
        if (!filterCatalogStatus.isEmpty()) {
            parts.add("releasing".equals(filterCatalogStatus) ? getString(R.string.status_airing).toUpperCase(Locale.US) : getString(R.string.status_finished).toUpperCase(Locale.US));
        }
        return getString(R.string.badge_catalog).toUpperCase(Locale.US) + ": " + TextUtils.join(" · ", parts);
    }

    private void resetCatalogFilterState() {
        filterCatalogSort = "popularity";
        filterCatalogYear = "";
        filterCatalogSeason = "";
        filterCatalogMonth = "";
        filterCatalogType = "";
        filterCatalogStatus = "";
        currentPage = 1;
    }

    private void setupDialogWindowBounds(Dialog dialog) {
        SystemUiHelper.setupCustomDialogBounds(dialog, this);
    }

    private void showCatalogFilterDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_catalog_filter);
        setupDialogWindowBounds(dialog);

        final String[] tempSort = new String[]{filterCatalogSort};
        final String[] tempYear = new String[]{filterCatalogYear};
        final String[] tempSeason = new String[]{filterCatalogSeason};
        final String[] tempMonth = new String[]{filterCatalogMonth};
        final String[] tempType = new String[]{filterCatalogType};
        final String[] tempStatus = new String[]{filterCatalogStatus};

        interface ChipSelectAction {
            void onSelect(String val);
        }

        class ChipGroupHelper {
            void bind(int[] ids, String[] values, String current, ChipSelectAction action) {
                for (int i = 0; i < ids.length; i++) {
                    final int idx = i;
                    TextView chip = dialog.findViewById(ids[i]);
                    if (chip == null) continue;
                    chip.setFocusable(true);
                    chip.setClickable(true);
                    id.shisen.app.util.TvFocusHelper.attachFocusScale(chip, 1.05f);
                    boolean selected = values[i].equals(current);
                    chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_tab_inactive);
                    chip.setTextColor(selected ? 0xFF090D16 : 0xFF94A3B8);
                    if (chip.getCompoundDrawables()[0] != null) {
                        chip.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(selected ? 0xFF090D16 : 0xFF94A3B8));
                    }
                    chip.setOnClickListener(v -> {
                        HapticHelper.performClick(v);
                        for (int j = 0; j < ids.length; j++) {
                            TextView c = dialog.findViewById(ids[j]);
                            if (c != null) {
                                boolean nowSel = (j == idx);
                                c.setBackgroundResource(nowSel ? R.drawable.bg_chip_selected : R.drawable.bg_tab_inactive);
                                c.setTextColor(nowSel ? 0xFF090D16 : 0xFF94A3B8);
                                if (c.getCompoundDrawables()[0] != null) {
                                    c.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(nowSel ? 0xFF090D16 : 0xFF94A3B8));
                                }
                            }
                        }
                        action.onSelect(values[idx]);
                    });
                }
            }
        }
        ChipGroupHelper helper = new ChipGroupHelper();

        // 1. Sort
        int[] sortIds = new int[]{
                R.id.chip_sort_pop, R.id.chip_sort_score, R.id.chip_sort_latest,
                R.id.chip_sort_trending, R.id.chip_sort_az, R.id.chip_sort_fav
        };
        String[] sortVals = new String[]{"popularity", "score", "latest", "trending", "az", "most_favorite"};
        helper.bind(sortIds, sortVals, tempSort[0], val -> tempSort[0] = val);

        // 2. Year
        int[] yearIds = new int[]{
                R.id.chip_year_all, R.id.chip_year_2026, R.id.chip_year_2025, R.id.chip_year_2024,
                R.id.chip_year_2023, R.id.chip_year_2022, R.id.chip_year_2021, R.id.chip_year_2020,
                R.id.chip_year_2015_2019, R.id.chip_year_2010_2014, R.id.chip_year_2000_2009, R.id.chip_year_classic
        };
        String[] yearVals = new String[]{
                "", "2026", "2025", "2024", "2023", "2022", "2021", "2020",
                "2015-2019", "2010-2014", "2000-2009", "<2000"
        };
        helper.bind(yearIds, yearVals, tempYear[0], val -> tempYear[0] = val);

        // 3. Season
        int[] seasonIds = new int[]{
                R.id.chip_season_all, R.id.chip_season_spring, R.id.chip_season_summer,
                R.id.chip_season_fall, R.id.chip_season_winter
        };
        String[] seasonVals = new String[]{"", "spring", "summer", "fall", "winter"};
        helper.bind(seasonIds, seasonVals, tempSeason[0], val -> tempSeason[0] = val);

        // 4. Month
        int[] monthIds = new int[]{
                R.id.chip_month_all, R.id.chip_month_1, R.id.chip_month_2, R.id.chip_month_3,
                R.id.chip_month_4, R.id.chip_month_5, R.id.chip_month_6, R.id.chip_month_7,
                R.id.chip_month_8, R.id.chip_month_9, R.id.chip_month_10, R.id.chip_month_11, R.id.chip_month_12
        };
        String[] monthVals = new String[]{"", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
        helper.bind(monthIds, monthVals, tempMonth[0], val -> tempMonth[0] = val);

        // 5. Type
        int[] typeIds = new int[]{
                R.id.chip_type_all, R.id.chip_type_tv, R.id.chip_type_movie,
                R.id.chip_type_ova, R.id.chip_type_ona, R.id.chip_type_special
        };
        String[] typeVals = new String[]{"", "tv", "movie", "ova", "ona", "special"};
        helper.bind(typeIds, typeVals, tempType[0], val -> tempType[0] = val);

        // 6. Status
        int[] statusIds = new int[]{
                R.id.chip_status_all, R.id.chip_status_airing, R.id.chip_status_completed
        };
        String[] statusVals = new String[]{"", "releasing", "completed"};
        helper.bind(statusIds, statusVals, tempStatus[0], val -> tempStatus[0] = val);

        // Action Buttons
        View btnClose = dialog.findViewById(R.id.btn_close_catalog_dialog);
        View btnReset = dialog.findViewById(R.id.btn_reset_catalog_filter);
        View btnApply = dialog.findViewById(R.id.btn_apply_catalog_filter);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                dialog.dismiss();
            });
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                dialog.dismiss();
                resetCatalogFilterState();
                currentTab = MainTab.KATALOG;
                currentPage = 1;
                updateTabsUI();
                updateSubFilterUI();
                loadCurrentFeed();
            });
        }

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                dialog.dismiss();
                filterCatalogSort = tempSort[0];
                filterCatalogYear = tempYear[0];
                filterCatalogSeason = tempSeason[0];
                filterCatalogMonth = tempMonth[0];
                filterCatalogType = tempType[0];
                filterCatalogStatus = tempStatus[0];
                currentTab = MainTab.KATALOG;
                currentPage = 1;
                updateTabsUI();
                updateSubFilterUI();
                loadCurrentFeed();
            });
        }

        if (id.shisen.app.ui.player.PlayerKeys.isTv()) {
            if (btnClose != null) id.shisen.app.util.TvFocusHelper.attachFocusScale(btnClose, 1.05f);
            if (btnReset != null) id.shisen.app.util.TvFocusHelper.attachFocusScale(btnReset, 1.05f);
            if (btnApply != null) id.shisen.app.util.TvFocusHelper.attachFocusScale(btnApply, 1.05f);
            View firstChip = dialog.findViewById(R.id.chip_sort_pop);
            if (firstChip != null) {
                firstChip.post(firstChip::requestFocus);
            }
        }

        dialog.show();
    }

    private void showGenreSelectorDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_genre_selector);
        setupDialogWindowBounds(dialog);

        EditText etFilter = dialog.findViewById(R.id.et_filter_genre);
        RecyclerView rvGenres = dialog.findViewById(R.id.rv_genres);
        Button btnClose = dialog.findViewById(R.id.btn_close_genre_dialog);
        Button btnDismiss = dialog.findViewById(R.id.btn_dismiss_genre);
        Button btnReset = dialog.findViewById(R.id.btn_reset_genre_filter);

        rvGenres.setLayoutManager(new GridLayoutManager(this, 2));
        GenreChipAdapter genreAdapter = new GenreChipAdapter((slug, title) -> {
            dialog.dismiss();
            setGenreFilter(title);
        });
        rvGenres.setAdapter(genreAdapter);
        genreAdapter.setData(TATAP_GENRES);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                dialog.dismiss();
            });
        }
        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                dialog.dismiss();
            });
        }
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                dialog.dismiss();
                selectedGenre = "Action";
                currentTab = MainTab.GENRE;
                currentPage = 1;
                updateTabsUI();
                updateSubFilterUI();
                loadCurrentFeed();
            });
        }

        if (etFilter != null) {
            etFilter.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    genreAdapter.filter(s != null ? s.toString() : "");
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (id.shisen.app.ui.player.PlayerKeys.isTv()) {
            if (btnClose != null) id.shisen.app.util.TvFocusHelper.attachFocusScale(btnClose, 1.05f);
            if (btnDismiss != null) id.shisen.app.util.TvFocusHelper.attachFocusScale(btnDismiss, 1.05f);
            if (btnReset != null) id.shisen.app.util.TvFocusHelper.attachFocusScale(btnReset, 1.05f);
            rvGenres.post(rvGenres::requestFocus);
        }

        dialog.show();
    }

    private void setGenreFilter(String title) {
        selectedGenre = title;
        currentTab = MainTab.GENRE;
        currentPage = 1;
        updateTabsUI();
        updateSubFilterUI();
        loadCurrentFeed();
    }

    public static class GenreChipAdapter extends RecyclerView.Adapter<GenreChipAdapter.ViewHolder> {
        private final List<GenreItem> allGenres = new ArrayList<>();
        private final List<GenreItem> filteredList = new ArrayList<>();
        private final OnGenreClickListener listener;

        public interface OnGenreClickListener {
            void onGenreClick(String slug, String title);
        }

        public GenreChipAdapter(OnGenreClickListener listener) {
            this.listener = listener;
        }

        public void setData(List<GenreItem> data) {
            allGenres.clear();
            filteredList.clear();
            if (data != null) {
                allGenres.addAll(data);
                filteredList.addAll(data);
            }
            notifyDataSetChanged();
        }

        public void filter(String query) {
            filteredList.clear();
            if (query == null || query.trim().isEmpty()) {
                filteredList.addAll(allGenres);
            } else {
                String q = query.toLowerCase(Locale.US).trim();
                for (GenreItem item : allGenres) {
                    if (item.title.toLowerCase(Locale.US).contains(q) || item.slug.toLowerCase(Locale.US).contains(q)) {
                        filteredList.add(item);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_genre_chip, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GenreItem item = filteredList.get(position);
            holder.tvName.setText(item.title);
            holder.itemView.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                if (listener != null) listener.onGenreClick(item.slug, item.title);
            });
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_genre_name);
                id.shisen.app.util.TvFocusHelper.attachFocusScale(itemView, 1.05f);
            }
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
        chip.setFocusable(true);
        chip.setClickable(true);
        id.shisen.app.util.TvFocusHelper.attachFocusScale(chip, 1.06f);

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

    private void setPaginationFolded(boolean folded) {
        if (isPaginationFolded == folded || layoutPagination == null || layoutPagination.getVisibility() != View.VISIBLE) {
            return;
        }
        isPaginationFolded = folded;

        Transition transition = new AutoTransition()
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator(1.6f));
        TransitionManager.beginDelayedTransition(layoutPagination, transition);

        if (folded) {
            if (btnPrevPage != null) btnPrevPage.setVisibility(View.GONE);
            if (btnNextPage != null) btnNextPage.setVisibility(View.GONE);
            layoutPagination.animate()
                    .alpha(0.72f)
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(200)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            if (btnPrevPage != null) {
                btnPrevPage.setVisibility(View.VISIBLE);
                boolean canPrev = currentPage > 1;
                btnPrevPage.setEnabled(canPrev);
                btnPrevPage.setAlpha(canPrev ? 1.0f : 0.4f);
            }
            if (btnNextPage != null) {
                btnNextPage.setVisibility(View.VISIBLE);
            }
            layoutPagination.animate()
                    .alpha(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void updatePaginationUI(int currentCount, int pageSize) {
        updatePaginationUI(currentCount, pageSize, currentCount >= pageSize);
    }

    private void updatePaginationUI(int currentCount, int pageSize, boolean hasNextPage) {
        if (layoutPagination == null) return;
        mainHandler.removeCallbacks(expandPaginationRunnable);
        isPaginationFolded = false;
        layoutPagination.setVisibility(View.VISIBLE);
        layoutPagination.setAlpha(1.0f);
        if (tvPageIndicator != null) {
            tvPageIndicator.setText(getString(R.string.page_indicator_format, currentPage));
        }
        if (btnPrevPage != null) {
            btnPrevPage.setText(getString(R.string.page_prev));
            btnPrevPage.setVisibility(View.VISIBLE);
            boolean canPrev = currentPage > 1;
            btnPrevPage.setEnabled(canPrev);
            btnPrevPage.setAlpha(canPrev ? 1.0f : 0.4f);
        }
        if (btnNextPage != null) {
            btnNextPage.setText(getString(R.string.page_next));
            btnNextPage.setVisibility(View.VISIBLE);
            boolean canNext = hasNextPage;
            btnNextPage.setEnabled(canNext);
            btnNextPage.setAlpha(canNext ? 1.0f : 0.4f);
        }
    }

    private String getCurrentCacheKey() {
        String searchQuery = etSearch.getText().toString().trim();
        if (!searchQuery.isEmpty()) {
            return "feed_search_" + searchQuery.toLowerCase(Locale.US).trim() + "_p" + currentPage;
        }
        if (currentTab == MainTab.MUSIM) {
            return "feed_musim_p" + currentPage;
        } else if (currentTab == MainTab.AIRING) {
            return "feed_airing_" + selectedAiringLane + "_p" + currentPage;
        } else if (currentTab == MainTab.JADWAL) {
            return "feed_jadwal_" + selectedDayOfWeek;
        } else if (currentTab == MainTab.KATALOG) {
            return "feed_katalog_" + filterCatalogSort + "_" + filterCatalogYear + "_" + filterCatalogSeason +
                    "_" + filterCatalogMonth + "_" + filterCatalogType + "_" + filterCatalogStatus + "_p" + currentPage;
        } else if (currentTab == MainTab.GENRE) {
            return "feed_genre_" + selectedGenre + "_p" + currentPage;
        }
        return null;
    }

    private void renderFeedList(List<Anime> rawList, boolean hasNextPage, int pageSize) {
        final List<Anime> result = AppSettings.filterSafe(this, rawList);
        if (result != null && !result.isEmpty()) {
            layoutEmpty.setVisibility(View.GONE);
            rvGrid.setVisibility(View.VISIBLE);

            if (currentTab == MainTab.MUSIM && currentPage == 1) {
                Anime hero = result.get(0);
                List<Anime> gridList = (result.size() > 1) ? result.subList(1, result.size()) : new ArrayList<>();
                gridAdapter.updateFeed(hero, gridList);
            } else {
                gridAdapter.updateFeed(null, result);
            }

            if (currentTab != MainTab.JADWAL) {
                updatePaginationUI(result.size(), pageSize, hasNextPage);
            } else {
                if (layoutPagination != null) layoutPagination.setVisibility(View.GONE);
            }
        }
    }

    private void loadCurrentFeed() {
        String searchQuery = etSearch.getText().toString().trim();
        if (!searchQuery.isEmpty()) {
            performSearch(searchQuery, currentPage);
            return;
        }

        final String cacheKey = getCurrentCacheKey();
        FeedCache.CachePayload cached = null;
        if (cacheKey != null) {
            cached = FeedCache.getInstance(this).getImmediate(cacheKey);
            if (cached != null && !cached.items.isEmpty()) {
                // TAMPILKAN SEKETIKA DARI CACHE (0ms, TANPA LOADER)!
                renderFeedList(cached.items, cached.hasNextPage, currentTab == MainTab.MUSIM ? 35 : 40);

                // Cek apakah jadwal tayang anime terdekat sudah tiba / perlu revalidasi
                boolean isUserPullToRefresh = swipeRefresh.isRefreshing();
                boolean needsRevalidate = isUserPullToRefresh || FeedCache.isRevalidationNeeded(cached);
                if (!needsRevalidate) {
                    // Data masih valid dan belum jam rilis anime, hemat baterai & jaringan!
                    swipeRefresh.setRefreshing(false);
                    return;
                }
            }
        }

        final boolean hasStaleData = (cached != null && !cached.items.isEmpty());
        if (!hasStaleData) {
            setLoading(true);
        }

        if (currentTab == MainTab.MUSIM) {
            layoutContextualBar.setVisibility(View.GONE);
            // BERANDA: Anime yang tayang pada musim saat ini (Current Season Airing)
            AniListClient.getCurrentSeasonAiring(currentPage, 35, new AniListClient.PagedCallback<List<Anime>>() {
                @Override
                public void onSuccess(List<Anime> rawResult, boolean hasNextPage, int total) {
                    final List<Anime> result = AppSettings.filterSafe(MainActivity.this, rawResult);
                    if (cacheKey != null && result != null && !result.isEmpty()) {
                        FeedCache.getInstance(MainActivity.this).put(cacheKey, result, hasNextPage, total);
                    }
                    mainHandler.post(() -> {
                        setLoading(false);
                        if (result != null && !result.isEmpty()) {
                            layoutEmpty.setVisibility(View.GONE);
                            rvGrid.setVisibility(View.VISIBLE);

                            if (currentPage == 1) {
                                Anime hero = result.get(0);
                                List<Anime> gridList = (result.size() > 1) ? result.subList(1, result.size()) : new ArrayList<>();
                                gridAdapter.updateFeed(hero, gridList);
                            } else {
                                gridAdapter.updateFeed(null, result);
                            }
                            if (!hasStaleData) {
                                rvGrid.scrollToPosition(0);
                            }
                            updatePaginationUI(result.size(), 35, hasNextPage);
                        } else {
                            if (currentPage > 1) {
                                currentPage--;
                                Toast.makeText(MainActivity.this, R.string.toast_last_page, Toast.LENGTH_SHORT).show();
                                updatePaginationUI(0, 35, false);
                            } else if (!hasStaleData) {
                                gridAdapter.updateFeed(null, new ArrayList<>());
                                layoutEmpty.setVisibility(View.VISIBLE);
                                if (layoutPagination != null) layoutPagination.setVisibility(View.GONE);
                            }
                        }
                    });
                }

                @Override
                public void onError(Throwable t) {
                    mainHandler.post(() -> {
                        setLoading(false);
                        if (!hasStaleData) {
                            Toast.makeText(MainActivity.this, getString(R.string.error_load_season, t.getMessage()), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } else {
            switch (currentTab) {
                case AIRING:
                    layoutContextualBar.setVisibility(View.GONE);
                    // MASIH TAYANG: Dual-Lane (Anime musiman berjalan + serial panjang berkelanjutan)
                    AniListClient.getMasihTayangDualLane(currentPage, 35, selectedAiringLane, createPagedCallback(getString(R.string.error_load_airing), 35, cacheKey, hasStaleData));
                    break;
                case JADWAL:
                    layoutContextualBar.setVisibility(View.GONE);
                    if (layoutPagination != null) layoutPagination.setVisibility(View.GONE);
                    AniListClient.getSchedule(selectedDayOfWeek, createCallback(getString(R.string.error_load_schedule), 100, cacheKey, hasStaleData));
                    break;
                case RIWAYAT:
                    new Thread(() -> {
                        List<Anime> history = MenatapDatabase.getInstance(this).getHistoryList();
                        FeedCache.getInstance(this).hydrateAnimeList(history);
                        int total = history.size();
                        int pageSize = 40;
                        int fromIndex = (currentPage - 1) * pageSize;
                        List<Anime> pagedList = new ArrayList<>();
                        if (fromIndex < total) {
                            int toIndex = Math.min(fromIndex + pageSize, total);
                            pagedList = history.subList(fromIndex, toIndex);
                        }
                        final List<Anime> finalPagedList = pagedList;
                        mainHandler.post(() -> {
                            layoutContextualBar.setVisibility(View.VISIBLE);
                            tvContextualTitle.setText(getString(R.string.label_history_title, total));
                            btnClearHistoryAll.setVisibility(history.isEmpty() ? View.GONE : View.VISIBLE);
                        });
                        boolean hasNextPage = (fromIndex + pageSize) < total;
                        displayResults(finalPagedList, getString(R.string.label_empty_history_title), getString(R.string.label_empty_history_desc), pageSize, hasNextPage, false);
                        if (total <= pageSize) {
                            mainHandler.post(() -> {
                                if (layoutPagination != null) layoutPagination.setVisibility(View.GONE);
                            });
                        }
                    }).start();
                    break;
                case FAVORIT:
                    new Thread(() -> {
                        List<Anime> bookmarks = MenatapDatabase.getInstance(this).getBookmarkList();
                        FeedCache.getInstance(this).hydrateAnimeList(bookmarks);
                        int total = bookmarks.size();
                        int pageSize = 40;
                        int fromIndex = (currentPage - 1) * pageSize;
                        List<Anime> pagedList = new ArrayList<>();
                        if (fromIndex < total) {
                            int toIndex = Math.min(fromIndex + pageSize, total);
                            pagedList = bookmarks.subList(fromIndex, toIndex);
                        }
                        final List<Anime> finalPagedList = pagedList;
                        mainHandler.post(() -> {
                            layoutContextualBar.setVisibility(View.VISIBLE);
                            tvContextualTitle.setText(getString(R.string.label_favorites_title, total));
                            btnClearHistoryAll.setVisibility(View.GONE);
                        });
                        boolean hasNextPage = (fromIndex + pageSize) < total;
                        displayResults(finalPagedList, getString(R.string.label_empty_favorites_title), getString(R.string.label_empty_favorites_desc), pageSize, hasNextPage, false);
                        if (total <= pageSize) {
                            mainHandler.post(() -> {
                                if (layoutPagination != null) layoutPagination.setVisibility(View.GONE);
                            });
                        }
                    }).start();
                    break;
                case KATALOG:
                    layoutContextualBar.setVisibility(View.VISIBLE);
                    tvContextualTitle.setText(getCatalogFilterSummary());
                    btnClearHistoryAll.setText(isCatalogFilterActive() ? getString(R.string.action_reset) : getString(R.string.action_filter));
                    btnClearHistoryAll.setVisibility(View.VISIBLE);
                    AniListClient.getCatalogCrossFiltered(
                            filterCatalogSort, filterCatalogYear, filterCatalogSeason, filterCatalogMonth,
                            filterCatalogType, filterCatalogStatus, currentPage, 40,
                            createPagedCallback(getString(R.string.error_load_catalog), 40, cacheKey, hasStaleData)
                    );
                    break;
                case GENRE:
                    layoutContextualBar.setVisibility(View.VISIBLE);
                    tvContextualTitle.setText(getString(R.string.label_genre_title, selectedGenre.toUpperCase(Locale.US)));
                    btnClearHistoryAll.setText(R.string.action_select_genre);
                    btnClearHistoryAll.setVisibility(View.VISIBLE);
                    AniListClient.getByGenre(selectedGenre, currentPage, 40, createPagedCallback(getString(R.string.error_load_genre, selectedGenre), 40, cacheKey, hasStaleData));
                    break;
                default:
                    break;
            }
        }
    }

    private void performSearch(String query) {
        currentPage = 1;
        performSearch(query, currentPage);
    }

    private void performSearch(String query, int page) {
        final String cacheKey = "feed_search_" + query.toLowerCase(Locale.US).trim() + "_p" + page;
        FeedCache.CachePayload cached = FeedCache.getInstance(this).getImmediate(cacheKey);
        final boolean hasStaleData = (cached != null && !cached.items.isEmpty());

        if (hasStaleData) {
            renderFeedList(cached.items, cached.hasNextPage, 40);
        } else {
            setLoading(true);
        }

        tvCurrentMenuBadge.setText(getString(R.string.badge_search));
        layoutContextualBar.setVisibility(View.VISIBLE);
        String pageSuffix = page > 1 ? " (" + getString(R.string.page_indicator_format, page) + ")" : "";
        tvContextualTitle.setText(getString(R.string.label_search_results_title, query, pageSuffix));
        btnClearHistoryAll.setVisibility(View.GONE);

        if (!AppSettings.isSafeQuery(this, query)) {
            setLoading(false);
            if (query.toLowerCase().contains("hentai")) {
                displayResults(new ArrayList<>(), getString(R.string.empty_blocked_title), getString(R.string.empty_blocked_desc), 40, false, false);
            } else {
                displayResults(new ArrayList<>(), getString(R.string.empty_safemode_title), getString(R.string.empty_safemode_desc), 40, false, false);
            }
            return;
        }

        if (hasStaleData && !swipeRefresh.isRefreshing() && !FeedCache.isRevalidationNeeded(cached)) {
            swipeRefresh.setRefreshing(false);
            return;
        }

        AniListClient.search(query, page, 40, createPagedCallback(getString(R.string.error_search_failed), 40, cacheKey, hasStaleData));
    }

    private AniListClient.PagedCallback<List<Anime>> createPagedCallback(String errorPrefix, int pageSize, String cacheKey, boolean hasStaleData) {
        return new AniListClient.PagedCallback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result, boolean hasNextPage, int total) {
                if (cacheKey != null && result != null && !result.isEmpty()) {
                    FeedCache.getInstance(MainActivity.this).put(cacheKey, result, hasNextPage, total);
                }
                displayResults(result, getString(R.string.empty_no_data_title), getString(R.string.empty_no_data_desc), pageSize, hasNextPage, hasStaleData);
            }

            @Override
            public void onError(Throwable t) {
                mainHandler.post(() -> {
                    setLoading(false);
                    if (!hasStaleData) {
                        Toast.makeText(MainActivity.this, errorPrefix + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }

    private AniListClient.Callback<List<Anime>> createCallback(String errorPrefix, int pageSize, String cacheKey, boolean hasStaleData) {
        return new AniListClient.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result) {
                if (cacheKey != null && result != null && !result.isEmpty()) {
                    FeedCache.getInstance(MainActivity.this).put(cacheKey, result, result.size() >= pageSize, result.size());
                }
                displayResults(result, getString(R.string.empty_no_data_title), getString(R.string.empty_no_data_desc), pageSize, result != null && result.size() >= pageSize, hasStaleData);
            }

            @Override
            public void onError(Throwable t) {
                mainHandler.post(() -> {
                    setLoading(false);
                    if (!hasStaleData) {
                        Toast.makeText(MainActivity.this, errorPrefix + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }

    private void displayResults(List<Anime> rawList, String emptyTitle, String emptyDesc, int pageSize, boolean hasNextPage, boolean hasStaleData) {
        final List<Anime> list = AppSettings.filterSafe(this, rawList);
        mainHandler.post(() -> {
            setLoading(false);
            if (list == null || list.isEmpty()) {
                if (currentPage > 1) {
                    currentPage--;
                    Toast.makeText(MainActivity.this, R.string.toast_last_page, Toast.LENGTH_SHORT).show();
                    updatePaginationUI(0, pageSize, false);
                } else if (!hasStaleData) {
                    gridAdapter.updateFeed(null, new ArrayList<>());
                    layoutEmpty.setVisibility(View.VISIBLE);
                    tvEmptyTitle.setText(emptyTitle);
                    tvEmptyDesc.setText(emptyDesc);
                    if (layoutPagination != null) layoutPagination.setVisibility(View.GONE);
                }
            } else {
                layoutEmpty.setVisibility(View.GONE);
                rvGrid.setVisibility(View.VISIBLE);
                if (currentTab == MainTab.MUSIM && currentPage == 1) {
                    Anime hero = list.get(0);
                    List<Anime> gridList = (list.size() > 1) ? list.subList(1, list.size()) : new ArrayList<>();
                    gridAdapter.updateFeed(hero, gridList);
                } else {
                    gridAdapter.updateFeed(null, list);
                }
                if (!hasStaleData) {
                    rvGrid.scrollToPosition(0);
                }
                if (focusGridOnNextLoad) {
                    focusGridOnNextLoad = false;
                    rvGrid.postDelayed(this::focusFirstGridItem, 150);
                }
                if (currentTab != MainTab.JADWAL) {
                    updatePaginationUI(list.size(), pageSize, hasNextPage);
                } else {
                    if (layoutPagination != null) layoutPagination.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            View focus = getCurrentFocus();

            if (isSearchExpanded) {
                // Dari search box atau collapse button, D-Pad Down langsung beralih ke konten anime
                if (focus == etSearch || focus == btnCollapseSearch || focus == btnClearSearch) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        hideKeyboard();
                        if (focusFirstGridItem()) {
                            return true;
                        }
                        if (rvGrid != null) {
                            return rvGrid.requestFocus();
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        // Di atas search box tidak ada elemen lain; tahan fokus agar tidak crash/terlempar
                        return true;
                    }
                }
                // Dari item baris paling atas di anime grid, D-Pad Up kembali ke search box
                if (focus != null && rvGrid != null && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    View parentCard = focus;
                    while (parentCard != null && parentCard.getParent() != rvGrid) {
                        if (parentCard.getParent() instanceof View) {
                            parentCard = (View) parentCard.getParent();
                        } else {
                            parentCard = null;
                            break;
                        }
                    }
                    if (parentCard != null && parentCard.getParent() == rvGrid) {
                        int pos = rvGrid.getChildAdapterPosition(parentCard);
                        if (pos >= 0 && pos < calculateGridSpan()) {
                            if (etSearch != null && etSearch.requestFocus()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /* package */ void openSettingsDialog(boolean focusTheme) {
        SettingsDialog.show(this, focusTheme, new SettingsDialog.OnSettingsChangeListener() {
            @Override
            public void onThemeChanged() {
                applyCurrentTheme();
                if (gridAdapter != null) gridAdapter.notifyDataSetChanged();
            }

            @Override
            public void onSafeModeChanged(boolean isSafeMode) {
                loadCurrentFeed();
            }

            @Override
            public void onLanguageChanged() {
                recreate();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppUpdateManager.REQUEST_CODE_INSTALL_PERMISSION) {
            AppUpdateManager.getInstance().onInstallPermissionResult(this);
        }
    }
}
