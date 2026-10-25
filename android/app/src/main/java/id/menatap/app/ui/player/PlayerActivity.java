package id.menatap.app.ui.player;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import id.menatap.app.R;
import id.menatap.app.data.db.MenatapDatabase;
import id.menatap.app.data.model.Anime;
import id.menatap.app.data.model.Episode;
import id.menatap.app.data.model.StreamSource;
import id.menatap.app.data.model.SubtitleTrack;
import id.menatap.app.data.service.AniSkipClient;
import id.menatap.app.data.service.HttpClientProvider;
import id.menatap.app.data.service.StreamResolver;
import id.menatap.app.ui.adapter.EpisodeAdapter;
import id.menatap.app.util.HapticHelper;
import okhttp3.OkHttpClient;

public class PlayerActivity extends AppCompatActivity {

    private FrameLayout flPlayerWrapper;
    private PlayerView playerView;
    private Button btnSkipOpEd;
    private ImageButton btnScreenLock;
    private LinearLayout layoutBuffering;
    private ProgressBar pbBuffering;
    private TextView tvBufferingText;
    private TextView tvSubtitles;

    // HUD & Gestures
    private LinearLayout hudHoldSpeed;
    private LinearLayout hudOsd;
    private TextView tvOsdIcon;
    private ProgressBar pbOsdBar;
    private TextView tvOsdVal;
    private TextView tvRippleLeft;
    private TextView tvRippleRight;

    // Landscape Top Bar
    private LinearLayout layoutTopBar;
    private ImageButton btnPlayerBack;
    private TextView tvPlayerTitle;
    private TextView tvSubStatus;
    private Button btnAspect;
    private Button btnQuality;
    private Button btnSpeed;
    private Button btnSubSelector;

    // Portrait Mode Bottom Panel
    private LinearLayout layoutPortraitBottom;
    private ImageButton btnPortraitBack;
    private TextView tvPortraitAnimeTitle;
    private TextView tvPortraitCurrentEp;
    private Button btnPortraitSub;
    private Button btnPortraitQuality;
    private Button btnPortraitSpeed;
    private Button btnPortraitFullscreen;
    private TextView tvPortraitEpCount;
    private Button btnPortraitSort;
    private RecyclerView rvPortraitEpisodes;

    private ExoPlayer player;
    private Anime anime;
    private final List<Episode> episodeList = new ArrayList<>();
    private int currentIndex = 0;
    private EpisodeAdapter episodeAdapter;
    private boolean isSortAscending = true;

    private AniSkipClient.SkipResult currentSkipTimes;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private final Handler autoHideHandler = new Handler(Looper.getMainLooper());

    private float currentSpeed = 1.0f;
    private boolean isScreenLocked = false;
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;

    private StreamSource currentStreamSource = null;
    private AudioManager audioManager;

    // Continuous volume float accumulator (anti-lag on modern Android)
    private float currentVolumeFraction = 0.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_player);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

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
        setupExoNativeStyling();
        setupPortraitRecycler();
        setupGestureControls();
        updateOrientationLayout(getResources().getConfiguration().orientation);
        playEpisode(currentIndex);
    }

    private void initViews() {
        flPlayerWrapper = findViewById(R.id.fl_player_wrapper);
        playerView = findViewById(R.id.player_view);
        btnSkipOpEd = findViewById(R.id.btn_skip_op_ed);
        btnScreenLock = findViewById(R.id.btn_screen_lock);
        layoutBuffering = findViewById(R.id.layout_buffering);
        pbBuffering = findViewById(R.id.pb_video_buffering);
        tvBufferingText = findViewById(R.id.tv_buffering_text);
        tvSubtitles = findViewById(R.id.tv_subtitles);

        hudHoldSpeed = findViewById(R.id.hud_hold_speed);
        hudOsd = findViewById(R.id.hud_osd);
        tvOsdIcon = findViewById(R.id.tv_osd_icon);
        pbOsdBar = findViewById(R.id.pb_osd_bar);
        tvOsdVal = findViewById(R.id.tv_osd_val);
        tvRippleLeft = findViewById(R.id.tv_ripple_left);
        tvRippleRight = findViewById(R.id.tv_ripple_right);

        layoutTopBar = findViewById(R.id.layout_top_bar);
        btnPlayerBack = findViewById(R.id.btn_player_back);
        tvPlayerTitle = findViewById(R.id.tv_player_title);
        tvSubStatus = findViewById(R.id.tv_sub_status);
        btnAspect = findViewById(R.id.btn_aspect);
        btnQuality = findViewById(R.id.btn_quality);
        btnSpeed = findViewById(R.id.btn_speed);
        btnSubSelector = findViewById(R.id.btn_sub_selector);

        layoutPortraitBottom = findViewById(R.id.layout_portrait_bottom);
        btnPortraitBack = findViewById(R.id.btn_portrait_back);
        tvPortraitAnimeTitle = findViewById(R.id.tv_portrait_anime_title);
        tvPortraitCurrentEp = findViewById(R.id.tv_portrait_current_ep);
        btnPortraitSub = findViewById(R.id.btn_portrait_sub);
        btnPortraitQuality = findViewById(R.id.btn_portrait_quality);
        btnPortraitSpeed = findViewById(R.id.btn_portrait_speed);
        btnPortraitFullscreen = findViewById(R.id.btn_portrait_fullscreen);
        tvPortraitEpCount = findViewById(R.id.tv_portrait_ep_count);
        btnPortraitSort = findViewById(R.id.btn_portrait_sort);
        rvPortraitEpisodes = findViewById(R.id.rv_portrait_episodes);

        setupClickListeners();
    }

    private void setupClickListeners() {
        btnPortraitBack.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            finish();
        });

        btnPlayerBack.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            int orient = getResources().getConfiguration().orientation;
            if (orient == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                finish();
            }
        });

        btnPortraitFullscreen.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            toggleFullscreen();
        });

        btnAspect.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            cycleAspectRatio();
        });

        btnPortraitQuality.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            showQualityDialog();
        });

        btnQuality.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            showQualityDialog();
        });

        btnPortraitSpeed.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            showSpeedDialog();
        });

        btnSpeed.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            showSpeedDialog();
        });

        btnPortraitSub.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            showSubtitleDialog();
        });

        btnSubSelector.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            showSubtitleDialog();
        });

        btnPortraitSort.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            toggleEpisodeSort();
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
        OkHttpClient okClient = HttpClientProvider.getClient();
        OkHttpDataSource.Factory okHttpFactory = new OkHttpDataSource.Factory(okClient)
                .setUserAgent(HttpClientProvider.USER_AGENT);

        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(okHttpFactory);

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .build();

        playerView.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    if (layoutBuffering != null) layoutBuffering.setVisibility(View.VISIBLE);
                } else {
                    if (layoutBuffering != null) layoutBuffering.setVisibility(View.GONE);
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

            @Override
            public void onPlayerError(@NonNull androidx.media3.common.PlaybackException error) {
                if (layoutBuffering != null) layoutBuffering.setVisibility(View.GONE);
                String msg = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
                Toast.makeText(PlayerActivity.this, "Stream gagal diputar: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupExoNativeStyling() {
        playerView.post(() -> {
            View controlsBg = playerView.findViewById(androidx.media3.ui.R.id.exo_controls_background);
            if (controlsBg != null) {
                controlsBg.setBackgroundColor(Color.TRANSPARENT);
            }

            View exoBottomBar = playerView.findViewById(androidx.media3.ui.R.id.exo_bottom_bar);
            if (exoBottomBar != null) {
                exoBottomBar.setBackgroundResource(R.drawable.bg_player_bottom_scrim);
            }

            View posView = playerView.findViewById(androidx.media3.ui.R.id.exo_position);
            if (posView instanceof TextView) {
                ((TextView) posView).setTextColor(0xFFF1F5F9);
                ((TextView) posView).setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            }

            View durView = playerView.findViewById(androidx.media3.ui.R.id.exo_duration);
            if (durView instanceof TextView) {
                ((TextView) durView).setTextColor(0xFF94A3B8);
                ((TextView) durView).setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            }

            androidx.media3.ui.TimeBar timeBar = playerView.findViewById(androidx.media3.ui.R.id.exo_progress);
            if (timeBar instanceof androidx.media3.ui.DefaultTimeBar) {
                androidx.media3.ui.DefaultTimeBar dtb = (androidx.media3.ui.DefaultTimeBar) timeBar;
                dtb.setPlayedColor(0xFF38BDF8);
                dtb.setScrubberColor(0xFF38BDF8);
                dtb.setBufferedColor(0x6638BDF8);
                dtb.setUnplayedColor(0x33FFFFFF);
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
        tvPortraitEpCount.setText("DAFTAR EPISODE (" + episodeList.size() + ")");

        episodeAdapter.setListener((episode, position) -> playEpisode(position));
    }

    private void toggleEpisodeSort() {
        isSortAscending = !isSortAscending;
        btnPortraitSort.setText(isSortAscending ? "Urutan: 1 - N" : "Urutan: N - 1");
        Collections.reverse(episodeList);
        episodeAdapter.setData(episodeList);
        // Find new index of current episode
        if (player != null) {
            int currentEpNumber = currentIndex + 1;
            int newIdx = episodeAdapter.findPositionByNumber(currentEpNumber);
            if (newIdx != -1) currentIndex = newIdx;
        }
    }

    private void setupGestureControls() {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (player == null || isScreenLocked) return false;
                int width = flPlayerWrapper.getWidth();
                float x = e.getX();
                if (x < width * 0.4f) {
                    // Double tap left: Rewind 10s
                    long newPos = Math.max(0, player.getCurrentPosition() - 10000);
                    player.seekTo(newPos);
                    showRippleFeedback(tvRippleLeft);
                    return true;
                } else if (x > width * 0.6f) {
                    // Double tap right: Forward 10s
                    long dur = player.getDuration();
                    long target = player.getCurrentPosition() + 10000;
                    if (dur > 0) target = Math.min(dur, target);
                    player.seekTo(target);
                    showRippleFeedback(tvRippleRight);
                    return true;
                }
                return false;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                if (player == null || isScreenLocked) return;
                // Press & Hold 2X Speed HUD
                player.setPlaybackParameters(new PlaybackParameters(2.0f));
                hudHoldSpeed.setVisibility(View.VISIBLE);
                HapticHelper.performTick();
            }

            @Override
            public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                if (isScreenLocked || e1 == null || e2 == null) return false;
                int width = flPlayerWrapper.getWidth();
                float x = e1.getX();

                // Left half: Brightness swipe
                if (x < width * 0.45f) {
                    handleBrightnessSwipe(distanceY);
                    return true;
                }
                // Right half: Volume swipe
                else if (x > width * 0.55f) {
                    handleVolumeSwipe(distanceY);
                    return true;
                }
                return false;
            }
        });

        flPlayerWrapper.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if (hudHoldSpeed.getVisibility() == View.VISIBLE) {
                    hudHoldSpeed.setVisibility(View.GONE);
                    if (player != null) {
                        player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
                    }
                }
            }
            return false;
        });
    }

    private void handleBrightnessSwipe(float distanceY) {
        float delta = distanceY / flPlayerWrapper.getHeight();
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float current = lp.screenBrightness < 0 ? 0.5f : lp.screenBrightness;
        float newBrightness = Math.max(0.01f, Math.min(1.0f, current + delta * 1.5f));
        lp.screenBrightness = newBrightness;
        getWindow().setAttributes(lp);

        showOsd("BRI", (int) (newBrightness * 100));
    }

    private void handleVolumeSwipe(float distanceY) {
        if (audioManager == null) return;
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (maxVol <= 0) return;

        float deltaFraction = (distanceY / flPlayerWrapper.getHeight()) * 1.5f;
        currentVolumeFraction = Math.max(0f, Math.min(1.0f, currentVolumeFraction + deltaFraction));
        int targetVol = Math.round(currentVolumeFraction * maxVol);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0);

        showOsd("VOL", (int) (currentVolumeFraction * 100));
    }

    private void showOsd(String icon, int percent) {
        tvOsdIcon.setText(icon);
        pbOsdBar.setProgress(percent);
        tvOsdVal.setText(percent + "%");
        hudOsd.setVisibility(View.VISIBLE);

        autoHideHandler.removeCallbacksAndMessages(null);
        autoHideHandler.postDelayed(() -> hudOsd.setVisibility(View.GONE), 1200);
    }

    private void showRippleFeedback(View rippleView) {
        rippleView.setVisibility(View.VISIBLE);
        rippleView.setAlpha(1.0f);
        rippleView.animate().alpha(0.0f).setDuration(500).withEndAction(() -> rippleView.setVisibility(View.GONE)).start();
    }

    private void playEpisode(int index) {
        if (index < 0 || index >= episodeList.size()) return;
        currentIndex = index;
        Episode ep = episodeList.get(index);

        String safeTitle = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title : anime.romajiTitle;
        tvPortraitAnimeTitle.setText(safeTitle);
        tvPlayerTitle.setText(safeTitle + " · Ep " + ep.number);
        tvPortraitCurrentEp.setText("Episode " + ep.number + " · Menghubungkan...");
        episodeAdapter.setCurrentPlayingNumber(ep.number);
        rvPortraitEpisodes.smoothScrollToPosition(index);

        if (layoutBuffering != null) layoutBuffering.setVisibility(View.VISIBLE);
        btnSkipOpEd.setVisibility(View.GONE);
        currentSkipTimes = null;

        if (player != null) {
            player.stop();
        }

        // TIER 1 HiAnime -> TIER 2 AniChan -> TIER 3 Otakudesu
        StreamResolver.resolve(anime, ep, new StreamResolver.Callback() {
            @Override
            public void onSuccess(StreamSource source) {
                runOnUiThread(() -> {
                    currentStreamSource = source;
                    tvPortraitCurrentEp.setText("Episode " + ep.number + " · " + source.server);
                    tvSubStatus.setText("Server: " + source.server + (source.subtitles.isEmpty() ? " (Hardsub)" : " (Multi-Sub)"));
                    startExoPlayback(source, ep);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    if (layoutBuffering != null) layoutBuffering.setVisibility(View.GONE);
                    Toast.makeText(PlayerActivity.this, "Gagal memuat stream:\n" + t.getMessage(), Toast.LENGTH_LONG).show();
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
        if (!headers.containsKey("Referer") || headers.get("Referer") == null || headers.get("Referer").isEmpty()) {
            headers.put("Referer", "https://megaplay.buzz/");
        }
        if (!headers.containsKey("Origin") || headers.get("Origin") == null || headers.get("Origin").isEmpty()) {
            headers.put("Origin", "https://megaplay.buzz");
        }

        OkHttpClient okClient = HttpClientProvider.getClient();
        OkHttpDataSource.Factory okHttpFactory = new OkHttpDataSource.Factory(okClient)
                .setUserAgent(HttpClientProvider.USER_AGENT)
                .setDefaultRequestProperties(headers);

        DefaultMediaSourceFactory msFactory = new DefaultMediaSourceFactory(okHttpFactory);
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(Uri.parse(source.url));

        if (source.subtitles != null && !source.subtitles.isEmpty()) {
            List<MediaItem.SubtitleConfiguration> subConfigs = new ArrayList<>();
            for (SubtitleTrack track : source.subtitles) {
                MediaItem.SubtitleConfiguration subConfig = new MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url))
                        .setMimeType(androidx.media3.common.MimeTypes.TEXT_VTT)
                        .setLanguage(track.language)
                        .setLabel(track.label)
                        .setSelectionFlags(track.isDefault ? androidx.media3.common.C.SELECTION_FLAG_DEFAULT : 0)
                        .build();
                subConfigs.add(subConfig);
            }
            mediaItemBuilder.setSubtitleConfigurations(subConfigs);
        }

        MediaItem mediaItem = mediaItemBuilder.build();
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
            btnSkipOpEd.setText("Lewati Opening");
            btnSkipOpEd.setVisibility(View.VISIBLE);
        } else if (currentSkipTimes.outro != null && posSec >= currentSkipTimes.outro.startTime && posSec < currentSkipTimes.outro.endTime) {
            btnSkipOpEd.setText("Lewati Ending");
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
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void cycleAspectRatio() {
        if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
            btnAspect.setText("Zoom");
            Toast.makeText(this, "Mode: Zoom (Penuh Layar)", Toast.LENGTH_SHORT).show();
        } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL;
            btnAspect.setText("Fill");
            Toast.makeText(this, "Mode: Fill (Rentang Rasio)", Toast.LENGTH_SHORT).show();
        } else {
            currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
            btnAspect.setText("Fit");
            Toast.makeText(this, "Mode: Fit (Asli 16:9)", Toast.LENGTH_SHORT).show();
        }
        playerView.setResizeMode(currentResizeMode);
    }

    private void showQualityDialog() {
        CharSequence[] qualities = {"Auto (Paling Pas)", "1080p Full HD", "720p HD", "480p SD", "360p Hemat"};
        new AlertDialog.Builder(this)
                .setTitle("Kualitas Video")
                .setItems(qualities, (dialog, which) -> {
                    String q = qualities[which].toString();
                    btnQuality.setText(q.split(" ")[0]);
                    btnPortraitQuality.setText(q.split(" ")[0]);
                    Toast.makeText(this, "Kualitas: " + q, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showSpeedDialog() {
        CharSequence[] speeds = {"0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x"};
        float[] speedVals = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

        new AlertDialog.Builder(this)
                .setTitle("Kecepatan Pemutaran")
                .setItems(speeds, (dialog, which) -> {
                    currentSpeed = speedVals[which];
                    if (player != null) {
                        player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
                    }
                    String label = String.format(Locale.US, "%.1fx", currentSpeed);
                    btnPortraitSpeed.setText(label);
                    btnSpeed.setText(label);
                })
                .show();
    }

    private void showSubtitleDialog() {
        List<String> labels = new ArrayList<>();
        labels.add("Subtitle Indonesia (Otomatis)");
        if (currentStreamSource != null && currentStreamSource.subtitles != null) {
            for (SubtitleTrack st : currentStreamSource.subtitles) {
                labels.add(st.label);
            }
        }
        labels.add("Nonaktifkan Subtitle");

        CharSequence[] items = labels.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
                .setTitle("Pilihan Subtitle")
                .setItems(items, (dialog, which) -> {
                    String chosen = items[which].toString();
                    btnPortraitSub.setText(chosen.contains("Indo") ? "Sub Indo" : (chosen.contains("Non") ? "Sub Off" : chosen));
                    Toast.makeText(this, "Subtitle: " + chosen, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void toggleScreenLock() {
        isScreenLocked = !isScreenLocked;
        btnScreenLock.setImageResource(isScreenLocked ? R.drawable.ic_lock : R.drawable.ic_lock_open);
        playerView.setUseController(!isScreenLocked);
        Toast.makeText(this, isScreenLocked ? "Layar Terkunci" : "Layar Terbuka", Toast.LENGTH_SHORT).show();
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
            layoutTopBar.setVisibility(View.VISIBLE);
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
            layoutTopBar.setVisibility(View.GONE);
            btnScreenLock.setVisibility(View.GONE);
            btnPortraitFullscreen.setText("⛶ Full");

            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int playerHeight = (int) (screenWidth * 9f / 16f);
            if (playerHeight <= 0) playerHeight = (int) (220 * getResources().getDisplayMetrics().density);

            flPlayerWrapper.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    playerHeight
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
