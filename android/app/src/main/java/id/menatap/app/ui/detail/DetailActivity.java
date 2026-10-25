package id.menatap.app.ui.detail;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import id.menatap.app.R;
import id.menatap.app.data.db.MenatapDatabase;
import id.menatap.app.data.model.Anime;
import id.menatap.app.data.model.Episode;
import id.menatap.app.data.service.HiAnimeClient;
import id.menatap.app.data.service.OtakudesuScraper;
import id.menatap.app.ui.adapter.EpisodeAdapter;
import id.menatap.app.ui.player.PlayerActivity;
import id.menatap.app.util.HapticHelper;
import id.menatap.app.util.ImageLoader;

public class DetailActivity extends AppCompatActivity {

    private ImageView ivBackdrop;
    private ImageButton btnBack;
    private TextView tvTitle;
    private TextView tvMeta;
    private TextView tvSynopsis;
    private Button btnPlayPrimary;
    private Button btnBookmark;
    private RecyclerView rvEpisodes;

    private Anime anime;
    private EpisodeAdapter episodeAdapter;
    private final List<Episode> episodeList = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int lastWatchedEp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        anime = (Anime) getIntent().getSerializableExtra("anime");
        if (anime == null) {
            finish();
            return;
        }

        initViews();
        bindAnimeData();
        setupEpisodeRecycler();
        loadAccurateEpisodes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBookmarkUI();
        refreshWatchedStates();
    }

    private void initViews() {
        ivBackdrop = findViewById(R.id.iv_detail_backdrop);
        btnBack = findViewById(R.id.btn_detail_back);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvMeta = findViewById(R.id.tv_detail_meta);
        tvSynopsis = findViewById(R.id.tv_detail_synopsis);
        btnPlayPrimary = findViewById(R.id.btn_play_primary);
        btnBookmark = findViewById(R.id.btn_toggle_bookmark);
        rvEpisodes = findViewById(R.id.rv_detail_episodes);

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
    }

    private void bindAnimeData() {
        tvTitle.setText(anime.title);

        String backdropUrl = (anime.banner != null && !anime.banner.isEmpty()) ? anime.banner : anime.poster;
        ImageLoader.load(backdropUrl, ivBackdrop);

        StringBuilder meta = new StringBuilder();
        if (anime.season != null && !anime.season.isEmpty()) {
            meta.append(anime.season).append(" ");
            if (anime.year > 0) meta.append(anime.year).append(" · ");
        } else if (anime.year > 0) {
            meta.append(anime.year).append(" · ");
        }

        meta.append(anime.format != null ? anime.format : "TV");

        if (anime.airedEpisodesCount > 0) {
            meta.append(" · ").append(anime.airedEpisodesCount).append(" Episode Tayang");
        } else if (anime.episodesCount > 0) {
            meta.append(" · ").append(anime.episodesCount).append(" Episode");
        } else if (anime.epsCounter != null && !anime.epsCounter.isEmpty()) {
            meta.append(" · ").append(anime.epsCounter);
        }

        if (anime.score > 0) {
            meta.append(String.format(Locale.US, " · %.1f ★", anime.score));
        }

        tvMeta.setText(meta.toString());

        if (anime.synopsis != null && !anime.synopsis.isEmpty()) {
            String clean = Html.fromHtml(anime.synopsis, Html.FROM_HTML_MODE_LEGACY).toString().trim();
            tvSynopsis.setText(clean);
        } else {
            tvSynopsis.setText("Sinopsis belum tersedia untuk anime ini.");
        }

        updateBookmarkUI();
    }

    private void updateBookmarkUI() {
        boolean bookmarked = MenatapDatabase.getInstance(this).isBookmarked(anime.slug);
        if (bookmarked) {
            btnBookmark.setText("★ Tersimpan");
            btnBookmark.setTextColor(ContextCompat.getColor(this, R.color.accent_glacial));
            btnBookmark.setBackgroundColor(Color.parseColor("#17253D"));
        } else {
            btnBookmark.setText("+ Favorit");
            btnBookmark.setTextColor(ContextCompat.getColor(this, R.color.text_frost_bright));
            btnBookmark.setBackgroundColor(ContextCompat.getColor(this, R.color.frost_elevated));
        }
    }

    private void setupEpisodeRecycler() {
        episodeAdapter = new EpisodeAdapter(this);
        GridLayoutManager glm = new GridLayoutManager(this, 4);
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

        if (lastWatchedEp > 0) {
            btnPlayPrimary.setText("▶ Lanjut Episode " + lastWatchedEp);
        } else {
            btnPlayPrimary.setText("▶ Putar Episode 1");
        }
    }

    private void loadAccurateEpisodes() {
        // Calculate strict safe maximum aired episodes
        int safeAiredCount = 1;
        boolean isReleasing = "RELEASING".equalsIgnoreCase(anime.status) || "AIRING".equalsIgnoreCase(anime.status);

        if (isReleasing) {
            if (anime.nextAiringEpisode > 1) {
                safeAiredCount = anime.nextAiringEpisode - 1;
            } else if (anime.airedEpisodesCount > 0) {
                safeAiredCount = anime.airedEpisodesCount;
            } else {
                safeAiredCount = 1; // Only start with episode 1 until provider reports more
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

        // Fetch real episodes from HiAnime (Primary provider)
        String query = (anime.title != null && !anime.title.isEmpty()) ? anime.title : anime.romajiTitle;
        HiAnimeClient.searchAnime(query, new HiAnimeClient.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> results) {
                if (results != null && !results.isEmpty()) {
                    String matchedSlug = results.get(0).slug;
                    HiAnimeClient.getEpisodes(matchedSlug, new HiAnimeClient.Callback<List<Episode>>() {
                        @Override
                        public void onSuccess(List<Episode> hiEpisodes) {
                            if (hiEpisodes != null && !hiEpisodes.isEmpty()) {
                                mainHandler.post(() -> {
                                    episodeList.clear();
                                    episodeList.addAll(hiEpisodes);
                                    anime.airedEpisodesCount = hiEpisodes.size();
                                    refreshWatchedStates();
                                    episodeAdapter.setData(episodeList);
                                });
                                return;
                            }
                            // Fallback to Otakudesu if HiAnime has empty episodes
                            fetchOtakudesuFallback();
                        }

                        @Override
                        public void onError(Throwable t) {
                            fetchOtakudesuFallback();
                        }
                    });
                } else {
                    fetchOtakudesuFallback();
                }
            }

            @Override
            public void onError(Throwable t) {
                fetchOtakudesuFallback();
            }
        });
    }

    private void fetchOtakudesuFallback() {
        String cleanTitle = (anime.romajiTitle != null && !anime.romajiTitle.isEmpty()) ? anime.romajiTitle : anime.title;
        cleanTitle = cleanTitle.replaceAll("(?i)(season|part|s\\d+|2nd|3rd|4th)\\b", "").trim();

        OtakudesuScraper.searchAnime(cleanTitle, new OtakudesuScraper.Callback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> results) {
                if (results != null && !results.isEmpty()) {
                    String matchedSlug = results.get(0).slug;
                    OtakudesuScraper.getEpisodes(matchedSlug, new OtakudesuScraper.Callback<List<Episode>>() {
                        @Override
                        public void onSuccess(List<Episode> otakuEpisodes) {
                            if (otakuEpisodes != null && !otakuEpisodes.isEmpty()) {
                                mainHandler.post(() -> {
                                    episodeList.clear();
                                    episodeList.addAll(otakuEpisodes);
                                    anime.airedEpisodesCount = otakuEpisodes.size();
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
            public void onError(Throwable t) {}
        });
    }

    private void startPlaybackAtLastOrFirst() {
        if (episodeList.isEmpty()) {
            Toast.makeText(this, "Memuat daftar episode...", Toast.LENGTH_SHORT).show();
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

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("anime", anime);
        intent.putExtra("episodes", (Serializable) episodeList);
        intent.putExtra("selectedIndex", index);
        startActivity(intent);
    }
}
