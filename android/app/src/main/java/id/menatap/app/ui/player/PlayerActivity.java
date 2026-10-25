package id.menatap.app.ui.player;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import id.menatap.app.R;
import id.menatap.app.data.db.MenatapDatabase;
import id.menatap.app.data.model.Anime;
import id.menatap.app.data.model.Episode;
import id.menatap.app.data.model.StreamSource;
import id.menatap.app.data.service.AniSkipClient;
import id.menatap.app.data.service.HttpClientProvider;
import id.menatap.app.data.service.OtakudesuScraper;
import id.menatap.app.ui.adapter.EpisodeAdapter;
import id.menatap.app.util.HapticHelper;

public class PlayerActivity extends AppCompatActivity {

    private FrameLayout flPlayerWrapper;
    private PlayerView playerView;
    private Button btnSkipOpEd;
    private ImageButton btnScreenLock;
    private ProgressBar pbBuffering;

    private LinearLayout layoutPortraitBottom;
    private ImageButton btnPortraitBack;
    private TextView tvPortraitAnimeTitle;
    private TextView tvPortraitCurrentEp;
    private Button btnPortraitSub;
    private Button btnPortraitSpeed;
    private Button btnPortraitFullscreen;
    private RecyclerView rvPortraitEpisodes;

    private ExoPlayer player;
    private Anime anime;
    private List<Episode> episodeList = new ArrayList<>();
    private int currentIndex = 0;
    private EpisodeAdapter episodeAdapter;

    private AniSkipClient.SkipResult currentSkipTimes;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    private float currentSpeed = 1.0f;
    private boolean isScreenLocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        anime = (Anime) getIntent().getSerializableExtra("anime");
        @SuppressWarnings("unchecked")
        List<Episode> eps = (List<Episode>) getIntent().getSerializableExtra("episodes");
        if (eps != null) {
            episodeList.addAll(eps);
        }
        currentIndex = getIntent().getIntExtra("selectedIndex", 0);

        if (anime == null || episodeList.isEmpty()) {
            Toast.makeText(this, "Data episode tidak ditemukan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupPlayer();
        setupPortraitRecycler();
        updateOrientationLayout(getResources().getConfiguration().orientation);
        playEpisode(currentIndex);
    }

    private void initViews() {
        flPlayerWrapper = findViewById(R.id.fl_player_wrapper);
        playerView = findViewById(R.id.player_view);
        btnSkipOpEd = findViewById(R.id.btn_skip_op_ed);
        btnScreenLock = findViewById(R.id.btn_screen_lock);
        pbBuffering = findViewById(R.id.pb_video_buffering);

        layoutPortraitBottom = findViewById(R.id.layout_portrait_bottom);
        btnPortraitBack = findViewById(R.id.btn_portrait_back);
        tvPortraitAnimeTitle = findViewById(R.id.tv_portrait_anime_title);
        tvPortraitCurrentEp = findViewById(R.id.tv_portrait_current_ep);
        btnPortraitSub = findViewById(R.id.btn_portrait_sub);
        btnPortraitSpeed = findViewById(R.id.btn_portrait_speed);
        btnPortraitFullscreen = findViewById(R.id.btn_portrait_fullscreen);
        rvPortraitEpisodes = findViewById(R.id.rv_portrait_episodes);

        btnPortraitBack.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            finish();
        });

        btnPortraitFullscreen.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            toggleFullscreen();
        });

        btnPortraitSpeed.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            showSpeedDialog();
        });

        btnPortraitSub.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            showSubtitleDialog();
        });

        btnScreenLock.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            toggleScreenLock();
        });

        btnSkipOpEd.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            if (player == null || currentSkipTimes == null) return;
            long posSec = player.getCurrentPosition() / 1000;
            if (currentSkipTimes.intro != null && posSec >= currentSkipTimes.intro.startTime && posSec < currentSkipTimes.intro.endTime) {
                player.seekTo((long) (currentSkipTimes.intro.endTime * 1000));
            } else if (currentSkipTimes.outro != null && posSec >= currentSkipTimes.outro.startTime && posSec < currentSkipTimes.outro.endTime) {
                player.seekTo((long) (currentSkipTimes.outro.endTime * 1000));
            }
            btnSkipOpEd.setVisibility(View.GONE);
        });
    }

    private void setupPlayer() {
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(HttpClientProvider.USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000);

        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpDataSourceFactory);
        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .build();

        playerView.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    pbBuffering.setVisibility(View.VISIBLE);
                } else {
                    pbBuffering.setVisibility(View.GONE);
                }

                if (playbackState == Player.STATE_ENDED) {
                    playNextEpisode();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    startProgressTracker();
                } else {
                    stopProgressTracker();
                    saveCurrentPlayback();
                }
            }
        });
    }

    private void setupPortraitRecycler() {
        episodeAdapter = new EpisodeAdapter(this);
        GridLayoutManager glm = new GridLayoutManager(this, 4);
        rvPortraitEpisodes.setLayoutManager(glm);
        rvPortraitEpisodes.setAdapter(episodeAdapter);

        // Mark watched state
        for (Episode ep : episodeList) {
            ep.isWatched = MenatapDatabase.getInstance(this).isEpisodeWatched(anime.slug, ep.number);
        }
        episodeAdapter.setData(episodeList);

        episodeAdapter.setListener((episode, position) -> {
            playEpisode(position);
        });
    }

    private void playEpisode(int index) {
        if (index < 0 || index >= episodeList.size()) return;
        currentIndex = index;
        Episode ep = episodeList.get(index);

        tvPortraitAnimeTitle.setText(anime.title);
        tvPortraitCurrentEp.setText("Memutar Episode " + ep.number);
        episodeAdapter.setCurrentPlayingNumber(ep.number);
        rvPortraitEpisodes.smoothScrollToPosition(index);

        pbBuffering.setVisibility(View.VISIBLE);
        btnSkipOpEd.setVisibility(View.GONE);
        currentSkipTimes = null;

        if (player != null) {
            player.stop();
        }

        // Resolve stream
        String targetSlug = (ep.slug != null && !ep.slug.isEmpty()) ? ep.slug : (anime.slug + "-episode-" + ep.number);
        OtakudesuScraper.resolveStream(targetSlug, new OtakudesuScraper.Callback<StreamSource>() {
            @Override
            public void onSuccess(StreamSource source) {
                runOnUiThread(() -> {
                    startExoPlayback(source, ep);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    // Try fallback search resolution
                    OtakudesuScraper.searchAnime(anime.title, new OtakudesuScraper.Callback<List<Anime>>() {
                        @Override
                        public void onSuccess(List<Anime> results) {
                            if (results != null && !results.isEmpty()) {
                                String matchedSlug = results.get(0).slug;
                                String fallbackEpSlug = matchedSlug + "-episode-" + ep.number;
                                OtakudesuScraper.resolveStream(fallbackEpSlug, new OtakudesuScraper.Callback<StreamSource>() {
                                    @Override
                                    public void onSuccess(StreamSource source) {
                                        runOnUiThread(() -> startExoPlayback(source, ep));
                                    }

                                    @Override
                                    public void onError(Throwable t2) {
                                        runOnUiThread(() -> {
                                            pbBuffering.setVisibility(View.GONE);
                                            Toast.makeText(PlayerActivity.this, "Gagal memuat stream episode: " + t2.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(Throwable t2) {
                            runOnUiThread(() -> {
                                pbBuffering.setVisibility(View.GONE);
                                Toast.makeText(PlayerActivity.this, "Stream tidak tersedia", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                });
            }
        });
    }

    private void startExoPlayback(StreamSource source, Episode ep) {
        if (player == null) return;

        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", HttpClientProvider.USER_AGENT);
        if (source.headers != null) {
            headers.putAll(source.headers);
        }

        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(HttpClientProvider.USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(headers);

        DefaultDataSource.Factory dsFactory = new DefaultDataSource.Factory(this, httpFactory);
        DefaultMediaSourceFactory msFactory = new DefaultMediaSourceFactory(dsFactory);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(source.url));
        player.setMediaSource(msFactory.createMediaSource(mediaItem));
        player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
        player.prepare();
        player.play();

        // Fetch AniSkip
        if (anime.idMal > 0) {
            AniSkipClient.getSkipTimes(anime.idMal, ep.number, 1420, new AniSkipClient.Callback() {
                @Override
                public void onSuccess(AniSkipClient.SkipResult result) {
                    currentSkipTimes = result;
                }

                @Override
                public void onError(Throwable t) {}
            });
        }
    }

    private void startProgressTracker() {
        stopProgressTracker();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && player.isPlaying()) {
                    checkSkipTimes();
                    saveCurrentPlayback();
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
        progressHandler.postDelayed(progressRunnable, 1000);
    }

    private void stopProgressTracker() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    private void checkSkipTimes() {
        if (player == null || currentSkipTimes == null) return;
        long posSec = player.getCurrentPosition() / 1000;

        if (currentSkipTimes.intro != null && posSec >= currentSkipTimes.intro.startTime && posSec < currentSkipTimes.intro.endTime) {
            btnSkipOpEd.setText("Lewati Intro");
            btnSkipOpEd.setVisibility(View.VISIBLE);
        } else if (currentSkipTimes.outro != null && posSec >= currentSkipTimes.outro.startTime && posSec < currentSkipTimes.outro.endTime) {
            btnSkipOpEd.setText("Lewati Outro");
            btnSkipOpEd.setVisibility(View.VISIBLE);
        } else {
            btnSkipOpEd.setVisibility(View.GONE);
        }
    }

    private void saveCurrentPlayback() {
        if (player == null || episodeList.isEmpty() || currentIndex >= episodeList.size()) return;
        Episode ep = episodeList.get(currentIndex);
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        if (dur <= 0) dur = 1;

        MenatapDatabase.getInstance(this).saveHistory(anime.slug, anime.title, anime.poster, ep.number, pos, dur);
        ep.isWatched = true;
        episodeAdapter.notifyItemChanged(currentIndex);
    }

    private void playNextEpisode() {
        if (currentIndex + 1 < episodeList.size()) {
            Toast.makeText(this, "Memutar episode selanjutnya...", Toast.LENGTH_SHORT).show();
            playEpisode(currentIndex + 1);
        }
    }

    private void toggleFullscreen() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }

    private void toggleScreenLock() {
        isScreenLocked = !isScreenLocked;
        btnScreenLock.setImageResource(isScreenLocked ? R.drawable.ic_lock : R.drawable.ic_lock_open);
        playerView.setUseController(!isScreenLocked);
        Toast.makeText(this, isScreenLocked ? "Layar Terkunci" : "Layar Terbuka", Toast.LENGTH_SHORT).show();
    }

    private void showSpeedDialog() {
        CharSequence[] speeds = {"0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x"};
        float[] speedVals = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

        new AlertDialog.Builder(this)
                .setTitle("Kecepatan Pemutaran")
                .setItems(speeds, (dialog, which) -> {
                    currentSpeed = speedVals[which];
                    if (player != null) {
                        player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
                    }
                    btnPortraitSpeed.setText(String.format(Locale.US, "%.2fx", currentSpeed));
                })
                .show();
    }

    private void showSubtitleDialog() {
        CharSequence[] options = {"Subtitle Indonesia (Default)", "Nonaktifkan Subtitle"};
        new AlertDialog.Builder(this)
                .setTitle("Pilihan Subtitle")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        btnPortraitSub.setText("Sub Indo");
                        Toast.makeText(this, "Subtitle Indonesia aktif", Toast.LENGTH_SHORT).show();
                    } else {
                        btnPortraitSub.setText("Sub Off");
                        Toast.makeText(this, "Subtitle dinonaktifkan", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateOrientationLayout(newConfig.orientation);
    }

    private void updateOrientationLayout(int orientation) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutPortraitBottom.setVisibility(View.GONE);
            btnScreenLock.setVisibility(View.VISIBLE);
            btnPortraitFullscreen.setText("Kecilkan");

            flPlayerWrapper.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            if (controller != null) {
                controller.hide(WindowInsetsCompat.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            layoutPortraitBottom.setVisibility(View.VISIBLE);
            btnScreenLock.setVisibility(View.GONE);
            btnPortraitFullscreen.setText("Fullscreen");

            int heightPx = (int) (220 * getResources().getDisplayMetrics().density);
            flPlayerWrapper.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    heightPx
            ));

            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
            if (controller != null) {
                controller.show(WindowInsetsCompat.Type.systemBars());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
        stopProgressTracker();
        saveCurrentPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressTracker();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
