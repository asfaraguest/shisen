package id.shisen.app.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import id.shisen.app.R;
import id.shisen.app.data.db.MenatapDatabase;
import id.shisen.app.data.model.Anime;
import id.shisen.app.data.model.Episode;
import id.shisen.app.data.service.AniChanClient;
import id.shisen.app.data.service.AniListClient;
import id.shisen.app.data.service.HiAnimeClient;
import id.shisen.app.data.service.OtakudesuScraper;
import id.shisen.app.ui.adapter.EpisodeAdapter;
import id.shisen.app.ui.adapter.RelatedAnimeAdapter;
import id.shisen.app.ui.player.PlaybackDataHolder;
import id.shisen.app.ui.player.PlayerActivity;
import id.shisen.app.util.AnimeThemeManager;
import id.shisen.app.util.HapticHelper;
import id.shisen.app.util.ImageLoader;
import id.shisen.app.util.LocaleHelper;
import id.shisen.app.util.SystemUiHelper;
import id.shisen.app.util.TitleMatcher;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private View layoutDetailRoot;
    private ImageView ivBackdrop;
    private ImageView ivPoster;
    private ImageButton btnBack;
    private TextView tvTitle;
    private TextView tvRomaji;
    private TextView tvMeta;
    private TextView tvGenres;
    private TextView tvSynopsis;
    private TextView tvSynopsisToggle;
    private Button btnPlayPrimary;
    private Button btnBookmark;
    private ProgressBar pbEpisodesLoading;
    private RecyclerView rvEpisodes;

    private View layoutRelatedSeasons;
    private TextView tvRelatedSeasonsCount;
    private RecyclerView rvRelatedSeasons;
    private RelatedAnimeAdapter seasonsAdapter;

    private View layoutRelatedMovies;
    private TextView tvRelatedMoviesCount;
    private RecyclerView rvRelatedMovies;
    private RelatedAnimeAdapter moviesAdapter;

    private Anime anime;
    private EpisodeAdapter episodeAdapter;
    private final List<Episode> episodeList = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int lastWatchedEp = 0;
    private boolean isSynopsisExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        SystemUiHelper.applySystemBarsTheme(this);

        if (getIntent() != null && getIntent().hasExtra("anime")) {
            anime = (Anime) getIntent().getSerializableExtra("anime");
        }
        if (anime == null && savedInstanceState != null) {
            anime = (Anime) savedInstanceState.getSerializable("saved_anime");
        }
        if (anime == null) {
            anime = DetailDataHolder.getAnime();
        }

        if (anime == null) {
            finish();
            return;
        }

        DetailDataHolder.setAnime(anime);

        initViews();
        setupWindowInsets();
        bindAnimeData();
        setupEpisodeRecycler();
        loadAccurateEpisodes();
        loadRelations();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (anime != null) {
            outState.putSerializable("saved_anime", anime);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SystemUiHelper.applySystemBarsTheme(this);
        if (layoutDetailRoot != null) {
            layoutDetailRoot.setBackgroundResource(AnimeThemeManager.getCanvasBackgroundResId(this));
        }
        updateBookmarkUI();
        refreshWatchedStates();
    }

    private void initViews() {
        layoutDetailRoot = findViewById(R.id.layout_detail_root);
        if (layoutDetailRoot != null) {
            layoutDetailRoot.setBackgroundResource(AnimeThemeManager.getCanvasBackgroundResId(this));
        }
        ivBackdrop = findViewById(R.id.iv_detail_backdrop);
        ivPoster = findViewById(R.id.iv_detail_poster);
        btnBack = findViewById(R.id.btn_detail_back);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvRomaji = findViewById(R.id.tv_detail_romaji);
        tvMeta = findViewById(R.id.tv_detail_meta);
        tvGenres = findViewById(R.id.tv_detail_genres);
        tvSynopsis = findViewById(R.id.tv_detail_synopsis);
        tvSynopsisToggle = findViewById(R.id.tv_synopsis_toggle);
        btnPlayPrimary = findViewById(R.id.btn_play_primary);
        btnBookmark = findViewById(R.id.btn_toggle_bookmark);
        pbEpisodesLoading = findViewById(R.id.pb_episodes_loading);
        rvEpisodes = findViewById(R.id.rv_detail_episodes);

        layoutRelatedSeasons = findViewById(R.id.layout_related_seasons);
        tvRelatedSeasonsCount = findViewById(R.id.tv_related_seasons_count);
        rvRelatedSeasons = findViewById(R.id.rv_detail_seasons);
        seasonsAdapter = new RelatedAnimeAdapter(this);
        rvRelatedSeasons.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRelatedSeasons.setAdapter(seasonsAdapter);

        layoutRelatedMovies = findViewById(R.id.layout_related_movies);
        tvRelatedMoviesCount = findViewById(R.id.tv_related_movies_count);
        rvRelatedMovies = findViewById(R.id.rv_detail_movies);
        moviesAdapter = new RelatedAnimeAdapter(this);
        rvRelatedMovies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRelatedMovies.setAdapter(moviesAdapter);

        btnBack.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            finish();
        });

        btnBookmark.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            MenatapDatabase.getInstance(this).toggleBookmark(anime);
            updateBookmarkUI();
        });

        btnPlayPrimary.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            startPlaybackAtLastOrFirst();
        });

        tvSynopsisToggle.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            isSynopsisExpanded = !isSynopsisExpanded;
            if (isSynopsisExpanded) {
                tvSynopsis.setMaxLines(Integer.MAX_VALUE);
                tvSynopsisToggle.setText(getString(R.string.detail_hide));
            } else {
                tvSynopsis.setMaxLines(3);
                tvSynopsisToggle.setText(getString(R.string.detail_read_more));
            }
        });
    }

    private void setupWindowInsets() {
        if (layoutDetailRoot == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(layoutDetailRoot, (v, windowInsets) -> {
            androidx.core.graphics.Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            androidx.core.graphics.Insets displayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
            androidx.core.graphics.Insets navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());

            int topInset = Math.max(statusBars.top, displayCutout.top);
            int bottomInset = navBars.bottom;
            float density = getResources().getDisplayMetrics().density;

            View backdropContainer = findViewById(R.id.fl_detail_backdrop_container);
            if (backdropContainer != null) {
                ViewGroup.LayoutParams lp = backdropContainer.getLayoutParams();
                if (lp != null) {
                    lp.height = (int) (230 * density) + topInset;
                    backdropContainer.setLayoutParams(lp);
                }
            }

            if (btnBack != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) btnBack.getLayoutParams();
                if (lp != null) {
                    lp.topMargin = (int) (16 * density) + topInset;
                    btnBack.setLayoutParams(lp);
                }
            }

            View scrollContent = findViewById(R.id.layout_detail_scroll_content);
            if (scrollContent != null) {
                scrollContent.setPadding(
                        scrollContent.getPaddingLeft(),
                        scrollContent.getPaddingTop(),
                        scrollContent.getPaddingRight(),
                        (int) (50 * density) + bottomInset
                );
            }

            return windowInsets;
        });
    }

    private void bindAnimeData() {
        // 1. Character Cover Poster: Prioritas mutlak poster resmi yang memfokuskan visual pada karakter anime terkait
        String characterCoverUrl = (anime.poster != null && !anime.poster.isEmpty() && !"null".equalsIgnoreCase(anime.poster))
                ? anime.poster : anime.banner;
        if (ivPoster != null) {
            ImageLoader.load(characterCoverUrl, ivPoster);
        }

        // 2. Ambient Backdrop: Gunakan banner lanskap bila ada, atau fallback ke poster karakter dengan efek ambient
        String backdropUrl = (anime.banner != null && !anime.banner.isEmpty() && !"null".equalsIgnoreCase(anime.banner))
                ? anime.banner : characterCoverUrl;
        ImageLoader.load(backdropUrl, ivBackdrop);

        // 3. Judul Utama
        String safeTitle = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title : anime.romajiTitle;
        tvTitle.setText(safeTitle);

        // 4. Judul Romaji & Judul Asli Jepang (Kanji/Katakana)
        StringBuilder subTitle = new StringBuilder();
        if (anime.romajiTitle != null && !anime.romajiTitle.isEmpty() && !"null".equalsIgnoreCase(anime.romajiTitle) && !anime.romajiTitle.equalsIgnoreCase(safeTitle)) {
            subTitle.append(anime.romajiTitle);
        }
        if (anime.nativeTitle != null && !anime.nativeTitle.isEmpty() && !"null".equalsIgnoreCase(anime.nativeTitle)) {
            if (subTitle.length() > 0) subTitle.append(" • ");
            subTitle.append(anime.nativeTitle);
        }

        if (subTitle.length() > 0) {
            tvRomaji.setText(subTitle.toString());
            tvRomaji.setVisibility(View.VISIBLE);
        } else {
            tvRomaji.setVisibility(View.GONE);
        }

        StringBuilder meta = new StringBuilder();
        if (anime.season != null && !anime.season.isEmpty() && !"null".equalsIgnoreCase(anime.season)) {
            meta.append(anime.season.substring(0, 1).toUpperCase(Locale.US)).append(anime.season.substring(1).toLowerCase(Locale.US)).append(" ");
            if (anime.year > 0) meta.append(anime.year).append(" · ");
        } else if (anime.year > 0) {
            meta.append(anime.year).append(" · ");
        }

        meta.append(anime.format != null ? anime.format : "TV");

        boolean isNotYet = "NOT_YET_RELEASED".equalsIgnoreCase(anime.status) ||
                (("RELEASING".equalsIgnoreCase(anime.status) || "AIRING".equalsIgnoreCase(anime.status)) && anime.nextAiringEpisode == 1 && anime.airedEpisodesCount == 0);

        if (anime.airedEpisodesCount > 0) {
            meta.append(" · ").append(getString(R.string.detail_aired_format, anime.airedEpisodesCount));
            if (anime.episodesCount > 0) {
                meta.append(" ").append(getString(R.string.detail_total_format, anime.episodesCount));
            }
        } else if (isNotYet) {
            meta.append(" · ").append(getString(R.string.detail_not_aired));
            if (anime.episodesCount > 0) {
                meta.append(" ").append(getString(R.string.detail_planned_format, anime.episodesCount));
            }
        } else if (anime.episodesCount > 0) {
            meta.append(" · ").append(anime.episodesCount).append(" ").append(getString(R.string.detail_episode_word));
        } else if (anime.epsCounter != null && !anime.epsCounter.isEmpty() && !"null".equalsIgnoreCase(anime.epsCounter)) {
            meta.append(" · ").append(anime.epsCounter);
        }

        if (anime.score > 0) {
            meta.append(String.format(Locale.US, " · %.1f ★", anime.score));
        }

        tvMeta.setText(meta.toString());

        if (anime.genres != null && !anime.genres.isEmpty()) {
            StringBuilder gb = new StringBuilder();
            int c = Math.min(4, anime.genres.size());
            for (int i = 0; i < c; i++) {
                gb.append(anime.genres.get(i));
                if (i < c - 1) gb.append(" • ");
            }
            tvGenres.setText(gb.toString());
            tvGenres.setVisibility(View.VISIBLE);
        } else {
            tvGenres.setVisibility(View.GONE);
        }

        if (anime.synopsis != null && !anime.synopsis.isEmpty() && !"null".equalsIgnoreCase(anime.synopsis)) {
            String clean = Html.fromHtml(anime.synopsis, Html.FROM_HTML_MODE_LEGACY).toString().trim();
            tvSynopsis.setText(clean);
        } else {
            tvSynopsis.setText(R.string.detail_synopsis_empty);
        }

        updateBookmarkUI();
    }

    private void updateBookmarkUI() {
        boolean bookmarked = MenatapDatabase.getInstance(this).isBookmarked(anime.slug);
        if (bookmarked) {
            btnBookmark.setText(getString(R.string.detail_saved));
            btnBookmark.setTextColor(ContextCompat.getColor(this, R.color.accent_glacial));
            btnBookmark.setBackgroundColor(Color.parseColor("#17253D"));
        } else {
            btnBookmark.setText(getString(R.string.detail_favorite));
            btnBookmark.setTextColor(ContextCompat.getColor(this, R.color.text_frost_bright));
            btnBookmark.setBackgroundColor(ContextCompat.getColor(this, R.color.frost_elevated));
        }
    }

    private void setupEpisodeRecycler() {
        episodeAdapter = new EpisodeAdapter(this);
        GridLayoutManager glm = new GridLayoutManager(this, getResources().getDisplayMetrics().widthPixels / (int)getResources().getDisplayMetrics().density >= 600 ? 4 : 3);
        rvEpisodes.setLayoutManager(glm);
        rvEpisodes.setAdapter(episodeAdapter);

        episodeAdapter.setListener((episode, position) -> launchPlayer(position));
    }

    private void refreshWatchedStates() {
        lastWatchedEp = MenatapDatabase.getInstance(this).getLastWatchedEpisode(anime.slug);
        for (Episode ep : episodeList) {
            ep.isWatched = MenatapDatabase.getInstance(this).isEpisodeWatched(anime.slug, ep.number);
        }
        episodeAdapter.notifyDataSetChanged();

        if (episodeList.isEmpty()) {
            boolean isNotYet = "NOT_YET_RELEASED".equalsIgnoreCase(anime.status) ||
                    (("RELEASING".equalsIgnoreCase(anime.status) || "AIRING".equalsIgnoreCase(anime.status)) && anime.nextAiringEpisode == 1 && anime.airedEpisodesCount == 0);
            if (isNotYet) {
                btnPlayPrimary.setText(getString(R.string.detail_not_aired));
            } else {
                btnPlayPrimary.setText(getString(R.string.detail_not_available));
            }
            btnPlayPrimary.setEnabled(false);
            btnPlayPrimary.setAlpha(0.5f);
        } else {
            btnPlayPrimary.setEnabled(true);
            btnPlayPrimary.setAlpha(1.0f);
            if (lastWatchedEp > 0) {
                btnPlayPrimary.setText(getString(R.string.detail_resume_format, lastWatchedEp));
            } else {
                btnPlayPrimary.setText(getString(R.string.detail_play_ep1));
            }
        }
    }

    private void loadAccurateEpisodes() {
        boolean isReleasing = "RELEASING".equalsIgnoreCase(anime.status) || "AIRING".equalsIgnoreCase(anime.status);
        boolean isNotYetReleased = "NOT_YET_RELEASED".equalsIgnoreCase(anime.status) ||
                (isReleasing && anime.nextAiringEpisode == 1 && anime.airedEpisodesCount == 0);

        if (isNotYetReleased) {
            episodeList.clear();
            episodeAdapter.setData(episodeList);
            if (pbEpisodesLoading != null) pbEpisodesLoading.setVisibility(View.GONE);
            refreshWatchedStates();
            return;
        }

        // Safe airing count (anti-overflow on on-going anime)
        int safeAiredCount = 1;
        if (isReleasing) {
            if (anime.nextAiringEpisode > 1) {
                safeAiredCount = anime.nextAiringEpisode - 1;
            } else if (anime.airedEpisodesCount > 0) {
                safeAiredCount = anime.airedEpisodesCount;
            } else {
                safeAiredCount = 1; // Default to 1 episode until provider confirms more
            }
        } else {
            if ("MOVIE".equalsIgnoreCase(anime.format)) {
                safeAiredCount = 1;
            } else if (anime.episodesCount > 0) {
                safeAiredCount = anime.episodesCount;
            } else if (anime.airedEpisodesCount > 0) {
                safeAiredCount = anime.airedEpisodesCount;
            } else {
                safeAiredCount = 12;
            }
        }

        episodeList.clear();
        for (int i = 1; i <= safeAiredCount; i++) {
            Episode ep = new Episode(i);
            ep.slug = anime.slug + "-episode-" + i;
            episodeList.add(ep);
        }
        refreshWatchedStates();
        episodeAdapter.setData(episodeList);

        if (pbEpisodesLoading != null) pbEpisodesLoading.setVisibility(View.VISIBLE);

        // TIER 1: HiAnime Search & Accurate Episode Fetch
        String primaryTitle = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title : anime.romajiTitle;
        final String targetQuery = primaryTitle != null ? primaryTitle : "Anime";
        final String altQuery = (anime.romajiTitle != null && !anime.romajiTitle.isEmpty() && !"null".equalsIgnoreCase(anime.romajiTitle))
                ? anime.romajiTitle : "";

        HiAnimeClient.searchAnime(targetQuery, new HiAnimeClient.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> results) {
                String matchedSlug = pickBestSlug(results, targetQuery, altQuery);
                if (matchedSlug != null) {
                    HiAnimeClient.getEpisodes(matchedSlug, new HiAnimeClient.Callback<List<Episode>>() {
                        @Override
                        public void onSuccess(List<Episode> hiEpisodes) {
                            List<Episode> filtered = filterAccurateEpisodes(hiEpisodes);
                            if (!filtered.isEmpty()) {
                                mainHandler.post(() -> {
                                    if (pbEpisodesLoading != null) pbEpisodesLoading.setVisibility(View.GONE);
                                    episodeList.clear();
                                    episodeList.addAll(filtered);
                                    anime.airedEpisodesCount = filtered.size();
                                    refreshWatchedStates();
                                    episodeAdapter.setData(episodeList);
                                });
                                return;
                            }
                            fetchAniChanFallback();
                        }

                        @Override
                        public void onError(Throwable t) {
                            fetchAniChanFallback();
                        }
                    });
                } else if (!altQuery.isEmpty() && !altQuery.equalsIgnoreCase(targetQuery)) {
                    HiAnimeClient.searchAnime(altQuery, new HiAnimeClient.Callback<List<Anime>>() {
                        @Override
                        public void onSuccess(List<Anime> res2) {
                            String matched2 = pickBestSlug(res2, targetQuery, altQuery);
                            if (matched2 != null) {
                                HiAnimeClient.getEpisodes(matched2, new HiAnimeClient.Callback<List<Episode>>() {
                                    @Override
                                    public void onSuccess(List<Episode> hiEps) {
                                        List<Episode> filtered = filterAccurateEpisodes(hiEps);
                                        if (!filtered.isEmpty()) {
                                            mainHandler.post(() -> {
                                                if (pbEpisodesLoading != null) pbEpisodesLoading.setVisibility(View.GONE);
                                                episodeList.clear();
                                                episodeList.addAll(filtered);
                                                anime.airedEpisodesCount = filtered.size();
                                                refreshWatchedStates();
                                                episodeAdapter.setData(episodeList);
                                            });
                                            return;
                                        }
                                        fetchAniChanFallback();
                                    }

                                    @Override
                                    public void onError(Throwable t) {
                                        fetchAniChanFallback();
                                    }
                                });
                            } else {
                                fetchAniChanFallback();
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            fetchAniChanFallback();
                        }
                    });
                } else {
                    fetchAniChanFallback();
                }
            }

            @Override
            public void onError(Throwable t) {
                fetchAniChanFallback();
            }
        });
    }

    private List<Episode> filterAccurateEpisodes(List<Episode> rawList) {
        if (rawList == null || rawList.isEmpty()) return new ArrayList<>();
        boolean isReleasing = "RELEASING".equalsIgnoreCase(anime.status) || "AIRING".equalsIgnoreCase(anime.status);
        int maxAired = Integer.MAX_VALUE;
        if (isReleasing) {
            if (anime.nextAiringEpisode > 1) {
                maxAired = anime.nextAiringEpisode - 1;
            } else if (anime.airedEpisodesCount > 0) {
                maxAired = anime.airedEpisodesCount;
            }
        }

        List<Episode> result = new ArrayList<>();
        for (Episode ep : rawList) {
            if (isReleasing && maxAired < Integer.MAX_VALUE && ep.number > maxAired) {
                // Skip episodes that have not aired yet
                continue;
            }
            result.add(ep);
        }
        return result;
    }

    private String pickBestSlug(List<Anime> list, String target, String alt) {
        if (list == null || list.isEmpty()) return null;
        for (Anime a : list) {
            if (TitleMatcher.isMatchAny(anime, a)) {
                return a.slug;
            }
            if (TitleMatcher.isTitleMatch(target, a.title) || (!alt.isEmpty() && TitleMatcher.isTitleMatch(alt, a.title))) {
                return a.slug;
            }
            if (a.romajiTitle != null && !a.romajiTitle.isEmpty()) {
                if (TitleMatcher.isTitleMatch(target, a.romajiTitle) || (!alt.isEmpty() && TitleMatcher.isTitleMatch(alt, a.romajiTitle))) {
                    return a.slug;
                }
            }
        }
        // Season-consistent match
        int sTarget = TitleMatcher.extractSeasonNumber(target);
        if (sTarget <= 1 && !alt.isEmpty()) {
            sTarget = TitleMatcher.extractSeasonNumber(alt);
        }
        for (Anime a : list) {
            int sCandidate = TitleMatcher.extractSeasonNumber(a.title);
            if (sTarget == sCandidate && (TitleMatcher.isTitleMatch(target, a.title) || (!alt.isEmpty() && TitleMatcher.isTitleMatch(alt, a.title)))) {
                return a.slug;
            }
        }
        return null;
    }

    // TIER 2 Fallback: AniChan
    private void fetchAniChanFallback() {
        if (anime.id <= 0) {
            fetchOtakudesuFallback();
            return;
        }

        AniChanClient.getEpisodes(anime.id, new AniChanClient.Callback<List<Episode>>() {
            @Override
            public void onSuccess(List<Episode> aniChanEps) {
                List<Episode> filtered = filterAccurateEpisodes(aniChanEps);
                if (!filtered.isEmpty()) {
                    mainHandler.post(() -> {
                        if (pbEpisodesLoading != null) pbEpisodesLoading.setVisibility(View.GONE);
                        episodeList.clear();
                        episodeList.addAll(filtered);
                        anime.airedEpisodesCount = filtered.size();
                        refreshWatchedStates();
                        episodeAdapter.setData(episodeList);
                    });
                    return;
                }
                fetchOtakudesuFallback();
            }

            @Override
            public void onError(Throwable t) {
                fetchOtakudesuFallback();
            }
        });
    }

    // TIER 3 Fallback: Otakudesu
    private void fetchOtakudesuFallback() {
        String cleanTitle = (anime.romajiTitle != null && !anime.romajiTitle.isEmpty() && !"null".equalsIgnoreCase(anime.romajiTitle))
                ? anime.romajiTitle : anime.title;
        cleanTitle = cleanTitle.replaceAll("(?i)(season|part|s\\d+|2nd|3rd|4th)\\b", "").trim();

        OtakudesuScraper.searchAnime(cleanTitle, new OtakudesuScraper.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> results) {
                mainHandler.post(() -> {
                    if (pbEpisodesLoading != null) pbEpisodesLoading.setVisibility(View.GONE);
                });
                if (results != null && !results.isEmpty()) {
                    String matchedSlug = results.get(0).slug;
                    OtakudesuScraper.getEpisodes(matchedSlug, new OtakudesuScraper.Callback<List<Episode>>() {
                        @Override
                        public void onSuccess(List<Episode> otakuEpisodes) {
                            List<Episode> filtered = filterAccurateEpisodes(otakuEpisodes);
                            if (!filtered.isEmpty()) {
                                mainHandler.post(() -> {
                                    episodeList.clear();
                                    episodeList.addAll(filtered);
                                    anime.airedEpisodesCount = filtered.size();
                                    refreshWatchedStates();
                                    episodeAdapter.setData(episodeList);
                                });
                            }
                        }

                        @Override
                        public void onError(Throwable t) {}
                    });
                }
            }

            @Override
            public void onError(Throwable t) {
                mainHandler.post(() -> {
                    if (pbEpisodesLoading != null) pbEpisodesLoading.setVisibility(View.GONE);
                });
            }
        });
    }

    private void loadRelations() {
        String queryTitle = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title : anime.romajiTitle;

        AniListClient.getRelations(anime.id, queryTitle, new AniListClient.Callback<AniListClient.RelationsResult>() {
            @Override
            public void onSuccess(AniListClient.RelationsResult result) {
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (result != null && result.seasons != null && !result.seasons.isEmpty()) {
                        seasonsAdapter.setData(result.seasons);
                        if (tvRelatedSeasonsCount != null) {
                            tvRelatedSeasonsCount.setText(getString(R.string.detail_series_count_format, result.seasons.size()));
                        }
                        if (layoutRelatedSeasons != null) {
                            layoutRelatedSeasons.setVisibility(View.VISIBLE);
                        }
                    } else if (layoutRelatedSeasons != null) {
                        layoutRelatedSeasons.setVisibility(View.GONE);
                    }

                    if (result != null && result.movies != null && !result.movies.isEmpty()) {
                        moviesAdapter.setData(result.movies);
                        if (tvRelatedMoviesCount != null) {
                            tvRelatedMoviesCount.setText(getString(R.string.detail_movies_count_format, result.movies.size()));
                        }
                        if (layoutRelatedMovies != null) {
                            layoutRelatedMovies.setVisibility(View.VISIBLE);
                        }
                    } else if (layoutRelatedMovies != null) {
                        layoutRelatedMovies.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                mainHandler.post(() -> {
                    if (layoutRelatedSeasons != null) layoutRelatedSeasons.setVisibility(View.GONE);
                    if (layoutRelatedMovies != null) layoutRelatedMovies.setVisibility(View.GONE);
                });
            }
        });
    }

    private void startPlaybackAtLastOrFirst() {
        if (episodeList.isEmpty()) {
            Toast.makeText(this, R.string.detail_loading_episodes, Toast.LENGTH_SHORT).show();
            return;
        }

        int targetIndex = 0;
        if (lastWatchedEp > 0) {
            int pos = episodeAdapter.findPositionByNumber(lastWatchedEp);
            if (pos != -1) targetIndex = pos;
        }
        launchPlayer(targetIndex);
    }

    private void launchPlayer(int index) {
        if (index < 0 || index >= episodeList.size()) return;

        PlaybackDataHolder.set(anime, episodeList, index);

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("anime", anime);
        intent.putExtra("episodes", (Serializable) episodeList);
        intent.putExtra("selectedIndex", index);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            DetailDataHolder.clear();
        }
    }
}
