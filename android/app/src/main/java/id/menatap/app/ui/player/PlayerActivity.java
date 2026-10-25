package id.menatap.app.ui.player;

import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.TimeBar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import id.menatap.app.data.subtitle.AnimeToshoClient;
import id.menatap.app.data.subtitle.OpenSubtitlesClient;
import id.menatap.app.data.subtitle.SubtitleCue;
import id.menatap.app.data.subtitle.SubtitleManager;
import id.menatap.app.data.subtitle.SubtitleMergeEngine;
import id.menatap.app.data.subtitle.SubtitleParser;
import id.menatap.app.ui.adapter.EpisodeAdapter;
import id.menatap.app.util.HapticHelper;
import okhttp3.OkHttpClient;

public class PlayerActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_SUBTITLE = 4042;

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

    // ExoPlayer Built-in Controller Buttons (Tatap OG Parity)
    private View btnExoPlayPause;
    private View btnExoRew;
    private View btnExoFfwd;
    private View btnExoNext;
    private View btnExoPrev;
    private View btnExoSub;
    private View btnExoPip;
    private View btnExoSettings;
    private View btnExoFullscreen;

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
    private final Handler subtitleHandler = new Handler(Looper.getMainLooper());

    private float currentSpeed = 1.0f;
    private boolean isScreenLocked = false;
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private boolean isActivityRunning = false;

    private StreamSource currentStreamSource = null;
    private AudioManager audioManager;
    private float currentVolumeFraction = 0.5f;

    // Multi-source Subtitle Manager
    private SubtitleManager subtitleManager;

    private final Runnable subtitleSyncRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && subtitleManager != null) {
                if (subtitleManager.isSubEnabled()) {
                    String text = subtitleManager.getCurrentText(player.getCurrentPosition());
                    if (text != null && !text.isEmpty()) {
                        if (!text.equals(tvSubtitles.getText().toString())) {
                            tvSubtitles.setText(text);
                        }
                        if (tvSubtitles.getVisibility() != View.VISIBLE) {
                            tvSubtitles.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (tvSubtitles.getVisibility() != View.GONE) {
                            tvSubtitles.setVisibility(View.GONE);
                        }
                    }
                } else {
                    if (tvSubtitles.getVisibility() != View.GONE) {
                        tvSubtitles.setVisibility(View.GONE);
                    }
                }
            }
            if (isActivityRunning) {
                subtitleHandler.postDelayed(this, 80);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isActivityRunning = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_player);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        subtitleManager = new SubtitleManager(this);

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
        setupSubtitleManager();
        setupPlayer();
        setupPortraitRecycler();
        setupGestureControls();
        applySubtitleStyle();
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

    private void setupSubtitleManager() {
        subtitleManager.setListener(new SubtitleManager.SubtitleStateListener() {
            @Override
            public void onSubtitleLoaded(String label, int count) {
                runOnUiThread(() -> {
                    tvSubStatus.setText(label + " (" + count + " baris)");
                    btnPortraitSub.setText(label.contains("Indo") ? "Sub: ID" : (label.contains("Eng") ? "Sub: EN" : "Sub: ON"));
                    btnPortraitSub.setAlpha(1.0f);
                    updateExoSubButtonState(true);
                });
            }

            @Override
            public void onSubtitleLoading(String message) {
                runOnUiThread(() -> tvSubStatus.setText("Subtitle: " + message));
            }

            @Override
            public void onSubtitleError(String error) {
                runOnUiThread(() -> {
                    tvSubStatus.setText("Sub: " + error);
                    updateExoSubButtonState(false);
                });
            }
        });
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
            showSubtitleSelectorDialog();
        });

        btnSubSelector.setOnClickListener(v -> {
            HapticHelper.performClick(v);
            showSubtitleSelectorDialog();
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

        // Hide default ExoPlayer subtitle view since we use seamless custom overlay
        androidx.media3.ui.SubtitleView subView = playerView.getSubtitleView();
        if (subView != null) {
            subView.setVisibility(View.GONE);
            subView.setUserDefaultStyle();
        }

        bindExoControlButtons();

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

        // Controller visibility listener to sync landscape top bar
        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && !isScreenLocked) {
                layoutTopBar.setVisibility(visibility == View.VISIBLE ? View.VISIBLE : View.GONE);
                btnScreenLock.setVisibility(visibility == View.VISIBLE ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void bindExoControlButtons() {
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
                ((TextView) posView).setTextColor(0xFFF5F5F7);
                ((TextView) posView).setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            }

            View durView = playerView.findViewById(androidx.media3.ui.R.id.exo_duration);
            if (durView instanceof TextView) {
                ((TextView) durView).setTextColor(0xFFA1A1A6);
                ((TextView) durView).setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            }

            TimeBar timeBar = playerView.findViewById(androidx.media3.ui.R.id.exo_progress);
            if (timeBar instanceof DefaultTimeBar) {
                DefaultTimeBar dtb = (DefaultTimeBar) timeBar;
                dtb.setPlayedColor(0xFF38BDF8);
                dtb.setScrubberColor(0xFF38BDF8);
                dtb.setBufferedColor(0x6638BDF8);
                dtb.setUnplayedColor(0x33FFFFFF);
                dtb.setKeyTimeIncrement(10000);
                dtb.addListener(new TimeBar.OnScrubListener() {
                    @Override
                    public void onScrubStart(TimeBar timeBar, long position) {
                        HapticHelper.performClick(playerView);
                    }

                    @Override
                    public void onScrubMove(TimeBar timeBar, long position) {
                        HapticHelper.performTick(playerView);
                    }

                    @Override
                    public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                        HapticHelper.performClick(playerView);
                    }
                });
            }

            // 1. Play / Pause Center
            btnExoPlayPause = playerView.findViewById(androidx.media3.ui.R.id.exo_play_pause);
            if (btnExoPlayPause == null) {
                btnExoPlayPause = playerView.findViewById(androidx.media3.ui.R.id.exo_play);
            }
            if (btnExoPlayPause != null) {
                btnExoPlayPause.setBackgroundResource(R.drawable.bg_exo_play_center);
            }

            // 2. Rewind (-5s)
            btnExoRew = playerView.findViewById(androidx.media3.ui.R.id.exo_rew);
            if (btnExoRew == null) {
                btnExoRew = playerView.findViewById(androidx.media3.ui.R.id.exo_rew_with_amount);
            }
            if (btnExoRew != null) {
                btnExoRew.setBackgroundResource(R.drawable.bg_exo_icon_btn);
                btnExoRew.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    seekRelative(-5000);
                    showRippleFeedback(tvRippleLeft);
                });
            }

            // 3. Fast Forward (+5s)
            btnExoFfwd = playerView.findViewById(androidx.media3.ui.R.id.exo_ffwd);
            if (btnExoFfwd == null) {
                btnExoFfwd = playerView.findViewById(androidx.media3.ui.R.id.exo_ffwd_with_amount);
            }
            if (btnExoFfwd != null) {
                btnExoFfwd.setBackgroundResource(R.drawable.bg_exo_icon_btn);
                btnExoFfwd.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    seekRelative(5000);
                    showRippleFeedback(tvRippleRight);
                });
            }

            // 4. Next & Prev Episode
            btnExoNext = playerView.findViewById(androidx.media3.ui.R.id.exo_next);
            if (btnExoNext != null) {
                btnExoNext.setBackgroundResource(R.drawable.bg_exo_icon_btn);
                btnExoNext.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    playNextEpisode();
                });
            }

            btnExoPrev = playerView.findViewById(androidx.media3.ui.R.id.exo_prev);
            if (btnExoPrev != null) {
                btnExoPrev.setBackgroundResource(R.drawable.bg_exo_icon_btn);
                btnExoPrev.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    playPrevEpisode();
                });
            }

            // 5. Subtitle Button (CC)
            btnExoSub = playerView.findViewById(androidx.media3.ui.R.id.exo_subtitle);
            if (btnExoSub != null) {
                btnExoSub.setBackgroundResource(R.drawable.bg_exo_icon_btn);
                updateExoSubButtonState(subtitleManager.isSubEnabled());
                btnExoSub.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    showSubtitleSelectorDialog();
                });
            }

            // 6. Picture-in-Picture (exo_vr)
            btnExoPip = playerView.findViewById(androidx.media3.ui.R.id.exo_vr);
            if (btnExoPip != null) {
                btnExoPip.setBackgroundResource(R.drawable.bg_exo_icon_btn);
                if (btnExoPip instanceof ImageView) {
                    ((ImageView) btnExoPip).setImageResource(R.drawable.ic_pip);
                }
                btnExoPip.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
                btnExoPip.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    enterPipMode();
                });
            }

            // 7. Settings Button
            btnExoSettings = playerView.findViewById(androidx.media3.ui.R.id.exo_settings);
            if (btnExoSettings != null) {
                btnExoSettings.setVisibility(View.VISIBLE);
                btnExoSettings.setBackgroundResource(R.drawable.bg_exo_icon_btn);
                btnExoSettings.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    showPlayerUnifiedSettingsDialog();
                });
            }

            // 8. Fullscreen / Aspect Ratio
            btnExoFullscreen = playerView.findViewById(androidx.media3.ui.R.id.exo_fullscreen);
            if (btnExoFullscreen != null) {
                btnExoFullscreen.setVisibility(View.VISIBLE);
                btnExoFullscreen.setBackgroundResource(R.drawable.bg_exo_icon_btn);
                btnExoFullscreen.setOnClickListener(v -> {
                    HapticHelper.performClick(v);
                    int orient = getResources().getConfiguration().orientation;
                    if (orient == Configuration.ORIENTATION_LANDSCAPE) {
                        cycleAspectRatio();
                    } else {
                        toggleFullscreen();
                    }
                });
                btnExoFullscreen.setOnLongClickListener(v -> {
                    HapticHelper.performClick(v);
                    toggleFullscreen();
                    return true;
                });
            }
        });
    }

    private void updateExoSubButtonState(boolean enabled) {
        if (btnExoSub != null) {
            btnExoSub.setAlpha(enabled ? 1.0f : 0.55f);
            if (btnExoSub instanceof ImageView) {
                ((ImageView) btnExoSub).setColorFilter(enabled ? 0xFF38BDF8 : 0xFFFFFFFF);
            }
        }
    }

    private void seekRelative(long deltaMs) {
        if (player == null) return;
        long cur = player.getCurrentPosition();
        long dur = player.getDuration();
        long target = cur + deltaMs;
        if (target < 0) target = 0;
        if (dur > 0 && target > dur) target = dur;
        player.seekTo(target);
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
                enterPictureInPictureMode(builder.build());
            } catch (Exception e) {
                Toast.makeText(this, "Tidak dapat masuk mode PiP: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
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
                    seekRelative(-10000);
                    showRippleFeedback(tvRippleLeft);
                    return true;
                } else if (x > width * 0.6f) {
                    seekRelative(10000);
                    showRippleFeedback(tvRippleRight);
                    return true;
                }
                return false;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                if (player == null || isScreenLocked) return;
                player.setPlaybackParameters(new PlaybackParameters(2.0f));
                hudHoldSpeed.setVisibility(View.VISIBLE);
                HapticHelper.performTick();
            }

            @Override
            public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                if (isScreenLocked || e1 == null || e2 == null) return false;
                int width = flPlayerWrapper.getWidth();
                float x = e1.getX();

                if (x < width * 0.45f) {
                    handleBrightnessSwipe(distanceY);
                    return true;
                } else if (x > width * 0.55f) {
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

        // Reset subtitle cues for new episode
        subtitleManager.clearCues();

        // TIER 1 HiAnime -> TIER 2 AniChan -> TIER 3 Otakudesu
        StreamResolver.resolve(anime, ep, new StreamResolver.Callback() {
            @Override
            public void onSuccess(StreamSource source) {
                runOnUiThread(() -> {
                    currentStreamSource = source;
                    tvPortraitCurrentEp.setText("Episode " + ep.number + " · " + source.server);
                    tvSubStatus.setText("Server: " + source.server + (source.subtitles.isEmpty() ? " (Hardsub)" : " (Multi-Sub)"));
                    startExoPlayback(source, ep);

                    // Load subtitles into multi-source subtitle engine
                    subtitleManager.loadFromTracks(source.subtitles, safeTitle, ep.number, false);
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

    private void playPrevEpisode() {
        if (currentIndex - 1 >= 0) {
            Toast.makeText(this, "Memutar episode sebelumnya...", Toast.LENGTH_SHORT).show();
            playEpisode(currentIndex - 1);
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

    // --- SUBTITLE ENGINE MULTI-SOURCE & DIALOGS (Tatap OG Parity) ---

    private void showSubtitleSelectorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pengaturan Subtitle & Terjemahan");

        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        boolean isSubOn = subtitleManager.isSubEnabled();
        String curLang = subtitleManager.getActiveLanguage();
        String curEngine = subtitleManager.getActiveEngine();

        // 1. [ID] Smart Fansub & Merge (AnimeTosho ID + Google Translate GTX + Smart Lexicon)
        boolean isSmartIdSelected = isSubOn && "id".equals(curLang) && "smart_merge".equals(curEngine);
        options.add("[ID] Bahasa Indonesia (Smart Fansub & Lexicon)" + (isSmartIdSelected ? " [Aktif]" : ""));
        actions.add(() -> {
            subtitleManager.setSubEnabled(true);
            if (currentStreamSource != null) {
                String safeTitle = (anime.title != null && !anime.title.isEmpty()) ? anime.title : anime.romajiTitle;
                subtitleManager.loadFromTracks(currentStreamSource.subtitles, safeTitle, episodeList.get(currentIndex).number, false);
            }
        });

        // 2. [ID] Asli / Bawaan (jika tersedia di track asli)
        if (currentStreamSource != null && currentStreamSource.subtitles != null) {
            for (SubtitleTrack st : currentStreamSource.subtitles) {
                if (st.lang.toLowerCase().contains("id") || st.label.toLowerCase().contains("indonesia")) {
                    boolean isIdRawSelected = isSubOn && "id".equals(curLang) && "raw".equals(curEngine);
                    options.add("[ID] Bahasa Indonesia (Asli Bawaan)" + (isIdRawSelected ? " [Aktif]" : ""));
                    actions.add(() -> {
                        subtitleManager.setSubEnabled(true);
                        subtitleManager.loadRawTrack(st.url, st.label, "id");
                    });
                    break;
                }
            }
        }

        // 3. [EN] English (Original)
        boolean isEnSelected = isSubOn && "en".equals(curLang) && "raw".equals(curEngine);
        options.add("[EN] English (Original / Asli)" + (isEnSelected ? " [Aktif]" : ""));
        actions.add(() -> {
            subtitleManager.setSubEnabled(true);
            if (currentStreamSource != null) {
                String safeTitle = (anime.title != null && !anime.title.isEmpty()) ? anime.title : anime.romajiTitle;
                subtitleManager.loadFromTracks(currentStreamSource.subtitles, safeTitle, episodeList.get(currentIndex).number, false);
            }
        });

        // 4. Other tracks from stream
        if (currentStreamSource != null && currentStreamSource.subtitles != null) {
            for (SubtitleTrack st : currentStreamSource.subtitles) {
                String l = st.lang.toLowerCase();
                if (l.contains("en") || l.contains("id")) continue;
                options.add(st.label);
                actions.add(() -> {
                    subtitleManager.setSubEnabled(true);
                    subtitleManager.loadRawTrack(st.url, st.label, st.lang);
                });
            }
        }

        // 5. Refresh / Re-translate (Bypass Cache)
        if (isSubOn && "id".equals(curLang)) {
            options.add("[Refresh] Terjemahkan Ulang Subtitle (Hapus Cache)");
            actions.add(() -> {
                Toast.makeText(this, "Menerjemahkan ulang subtitle dari awal...", Toast.LENGTH_SHORT).show();
                if (currentStreamSource != null) {
                    String safeTitle = (anime.title != null && !anime.title.isEmpty()) ? anime.title : anime.romajiTitle;
                    subtitleManager.loadFromTracks(currentStreamSource.subtitles, safeTitle, episodeList.get(currentIndex).number, true);
                }
            });
        }

        // 6. Search AnimeTosho & OpenSubtitles
        options.add("[+] Cari Subtitle Bahasa Indonesia (AnimeTosho & OpenSubtitles)...");
        actions.add(this::showOpenSubtitlesSearchDialog);

        // 7. Local Subtitle File Picker
        options.add("[+] Pilih File Subtitle Lokal (.srt / .vtt / .ass)...");
        actions.add(this::openSubtitleFilePicker);

        // 8. Custom Subtitle URL
        options.add("[+] Masukkan URL Subtitle Kustom...");
        actions.add(this::showCustomSubtitleUrlDialog);

        // 9. Time Sync (Offset)
        long curOff = subtitleManager.getSubtitleOffsetMs();
        String offStr = curOff == 0L ? "0.0s (Normal)" : String.format(Locale.US, "%+.2fs", curOff / 1000.0);
        options.add("[Sync] Sinkronisasi Waktu Subtitle (" + offStr + ")...");
        actions.add(this::showSubtitleOffsetPicker);

        // 10. Styling / Appearance
        options.add("Pengaturan Tampilan (Posisi, Latar Belakang & Ukuran)...");
        actions.add(this::showSubtitleStyleDialog);

        // 11. Disable Subtitles
        options.add("Matikan Subtitle" + (!isSubOn ? " [Aktif]" : ""));
        actions.add(() -> {
            subtitleManager.setSubEnabled(false);
            btnPortraitSub.setText("Sub: OFF");
            btnPortraitSub.setAlpha(0.6f);
            tvSubStatus.setText("Subtitle: Nonaktif");
            updateExoSubButtonState(false);
        });

        builder.setItems(options.toArray(new String[0]), (dialog, which) -> {
            if (which >= 0 && which < actions.size()) {
                actions.get(which).run();
            }
        });
        builder.setNegativeButton("Tutup", null);
        builder.show();
    }

    private void showOpenSubtitlesSearchDialog() {
        Toast.makeText(this, "Mencari subtitle di AnimeTosho & OpenSubtitles...", Toast.LENGTH_SHORT).show();
        String safeTitle = (anime.title != null && !anime.title.isEmpty()) ? anime.title : anime.romajiTitle;
        int epNumber = episodeList.get(currentIndex).number;

        AnimeToshoClient.searchIndonesianAsync(safeTitle, epNumber, new AnimeToshoClient.SearchCallback() {
            @Override
            public void onSuccess(List<AnimeToshoClient.SubtitleSearchResult> atResults) {
                OpenSubtitlesClient.searchIndonesianAsync(safeTitle, epNumber, new OpenSubtitlesClient.SearchCallback() {
                    @Override
                    public void onSuccess(List<OpenSubtitlesClient.SubtitleSearchResult> osResults) {
                        runOnUiThread(() -> {
                            if (atResults.isEmpty() && osResults.isEmpty()) {
                                Toast.makeText(PlayerActivity.this, "Tidak ditemukan subtitle fansub untuk episode ini", Toast.LENGTH_LONG).show();
                                return;
                            }

                            List<String> labels = new ArrayList<>();
                            List<Runnable> itemActions = new ArrayList<>();

                            for (AnimeToshoClient.SubtitleSearchResult at : atResults) {
                                labels.add("[AnimeTosho] " + at.filename);
                                itemActions.add(() -> downloadAndApplyAnimeToshoSub(at.downloadUrl, at.filename));
                            }

                            for (OpenSubtitlesClient.SubtitleSearchResult os : osResults) {
                                labels.add("[OpenSubtitles] " + os.filename);
                                itemActions.add(() -> downloadAndApplyOpenSubtitlesSub(os.downloadUrl, os.filename));
                            }

                            new AlertDialog.Builder(PlayerActivity.this)
                                    .setTitle("Pilih Subtitle Bahasa Indonesia")
                                    .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                                        if (which >= 0 && which < itemActions.size()) {
                                            itemActions.get(which).run();
                                        }
                                    })
                                    .setNegativeButton("Batal", null)
                                    .show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> Toast.makeText(PlayerActivity.this, error, Toast.LENGTH_SHORT).show());
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerActivity.this, error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void downloadAndApplyAnimeToshoSub(String url, String filename) {
        Toast.makeText(this, "Mengunduh subtitle " + filename + "...", Toast.LENGTH_SHORT).show();
        AnimeToshoClient.downloadAndParseAsync(url, new AnimeToshoClient.DownloadCallback() {
            @Override
            public void onSuccess(List<SubtitleCue> cues) {
                runOnUiThread(() -> {
                    subtitleManager.setCustomCues(cues, "[AnimeTosho] " + filename);
                    Toast.makeText(PlayerActivity.this, "Subtitle berhasil dimuat (" + cues.size() + " baris)", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerActivity.this, error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void downloadAndApplyOpenSubtitlesSub(String url, String filename) {
        Toast.makeText(this, "Mengunduh subtitle " + filename + "...", Toast.LENGTH_SHORT).show();
        OpenSubtitlesClient.downloadAndParseAsync(url, new OpenSubtitlesClient.DownloadCallback() {
            @Override
            public void onSuccess(List<SubtitleCue> cues) {
                runOnUiThread(() -> {
                    subtitleManager.setCustomCues(cues, "[OpenSubtitles] " + filename);
                    Toast.makeText(PlayerActivity.this, "Subtitle berhasil dimuat (" + cues.size() + " baris)", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerActivity.this, error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void openSubtitleFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimeTypes = {"text/plain", "text/vtt", "application/x-subrip", "application/octet-stream"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityForResult(intent, REQUEST_CODE_PICK_SUBTITLE);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_CODE_PICK_SUBTITLE);
            } catch (Exception ex) {
                Toast.makeText(this, "Gagal membuka pengelola berkas: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_SUBTITLE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            loadCustomSubtitleFromUri(uri);
        }
    }

    private void loadCustomSubtitleFromUri(Uri uri) {
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) throw new Exception("Tidak dapat membaca berkas");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();

                List<SubtitleCue> cues = SubtitleParser.parse(sb.toString());
                if (cues.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "Berkas subtitle kosong atau format tidak didukung", Toast.LENGTH_LONG).show());
                    return;
                }

                runOnUiThread(() -> {
                    subtitleManager.setCustomCues(cues, "Subtitle Lokal");
                    Toast.makeText(this, "Subtitle lokal berhasil dimuat (" + cues.size() + " baris)", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Gagal memuat berkas subtitle: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void showCustomSubtitleUrlDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Masukkan URL Subtitle");
        final EditText input = new EditText(this);
        input.setHint("https://example.com/sub.vtt atau .srt");
        input.setSingleLine(true);
        input.setPadding(40, 30, 40, 30);
        b.setView(input);

        b.setPositiveButton("Muat", (dialog, which) -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                subtitleManager.loadRawTrack(url, "Kustom URL", "custom");
            }
        });
        b.setNegativeButton("Batal", null);
        b.show();
    }

    private void showSubtitleStyleDialog() {
        SharedPreferences sp = getSharedPreferences("tatap_player_prefs", Context.MODE_PRIVATE);
        String curPos = sp.getString("sub_pos", "low");
        String curBg = sp.getString("sub_bg", "subtle");
        String curSize = sp.getString("sub_size", "medium");

        String posLabel = "low".equals(curPos) ? "Bawah (22dp) [Pas]" : ("mid".equals(curPos) ? "Sedang (36dp)" : "Tinggi (54dp)");
        String bgLabel = "transparent".equals(curBg) ? "Transparan (Tanpa Box)" : ("subtle".equals(curBg) ? "Samar / Lembut (30%)" : ("medium".equals(curBg) ? "Sedang (55%)" : "Pekat (80%)"));
        String sizeLabel = "small".equals(curSize) ? "Kecil (14sp)" : ("large".equals(curSize) ? "Besar (21sp)" : ("xlarge".equals(curSize) ? "Sangat Besar (25sp)" : "Sedang (17sp) [Standar HP]"));

        long curOff = subtitleManager.getSubtitleOffsetMs();
        String offLabel = curOff == 0L ? "0.0s (Normal)" : String.format(Locale.US, "%+.2fs", curOff / 1000.0);

        String[] menu = new String[]{
                "Posisi Subtitle: " + posLabel,
                "Latar Belakang: " + bgLabel,
                "Ukuran Huruf: " + sizeLabel,
                "Sinkronisasi Waktu (Offset): " + offLabel,
                "Reset ke Default"
        };

        new AlertDialog.Builder(this)
                .setTitle("Pengaturan Tampilan Subtitle")
                .setItems(menu, (dialog, which) -> {
                    if (which == 0) {
                        showPositionPicker(sp);
                    } else if (which == 1) {
                        showBackgroundPicker(sp);
                    } else if (which == 2) {
                        showSizePicker(sp);
                    } else if (which == 3) {
                        showSubtitleOffsetPicker();
                    } else if (which == 4) {
                        subtitleManager.setSubtitleOffsetMs(0L);
                        sp.edit().putString("sub_pos", "low").putString("sub_bg", "subtle").putString("sub_size", "medium").apply();
                        applySubtitleStyle();
                        Toast.makeText(this, "Tampilan subtitle direset ke default", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Tutup", null)
                .show();
    }

    private void showPositionPicker(SharedPreferences sp) {
        String[] options = {"Bawah (22dp) [Direkomendasikan]", "Sedang (36dp)", "Tinggi (54dp)"};
        String cur = sp.getString("sub_pos", "low");
        int checked = "high".equals(cur) ? 2 : ("mid".equals(cur) ? 1 : 0);

        new AlertDialog.Builder(this)
                .setTitle("Pilih Posisi Subtitle")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    String val = which == 2 ? "high" : (which == 1 ? "mid" : "low");
                    sp.edit().putString("sub_pos", val).apply();
                    applySubtitleStyle();
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showBackgroundPicker(SharedPreferences sp) {
        String[] options = {"Transparan (Tanpa Box / Shadow)", "Samar (30% Alpha) [Rekomendasi]", "Sedang (55% Alpha)", "Pekat (80% Alpha)"};
        String cur = sp.getString("sub_bg", "subtle");
        int checked = "dark".equals(cur) ? 3 : ("medium".equals(cur) ? 2 : ("transparent".equals(cur) ? 0 : 1));

        new AlertDialog.Builder(this)
                .setTitle("Pilih Latar Belakang Subtitle")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    String val = which == 3 ? "dark" : (which == 2 ? "medium" : (which == 0 ? "transparent" : "subtle"));
                    sp.edit().putString("sub_bg", val).apply();
                    applySubtitleStyle();
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showSizePicker(SharedPreferences sp) {
        String[] options = {"Kecil (14sp)", "Sedang (17sp) [Standar HP]", "Besar (21sp)", "Sangat Besar (25sp)"};
        String cur = sp.getString("sub_size", "medium");
        int checked = "xlarge".equals(cur) ? 3 : ("large".equals(cur) ? 2 : ("small".equals(cur) ? 0 : 1));

        new AlertDialog.Builder(this)
                .setTitle("Pilih Ukuran Huruf Subtitle")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    String val = which == 3 ? "xlarge" : (which == 2 ? "large" : (which == 0 ? "small" : "medium"));
                    sp.edit().putString("sub_size", val).apply();
                    applySubtitleStyle();
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showSubtitleOffsetPicker() {
        final long[] offsets = {-2000L, -1000L, -500L, -250L, 0L, 250L, 500L, 1000L, 2000L};
        final String[] labels = {
                "-2.0s (Lebih Cepat 2 Detik)",
                "-1.0s (Lebih Cepat 1 Detik)",
                "-0.5s (Lebih Cepat 0.5 Detik)",
                "-0.25s (Lebih Cepat 0.25 Detik)",
                "0.0s (Normal / Pas)",
                "+0.25s (Lebih Lambat 0.25 Detik)",
                "+0.5s (Lebih Lambat 0.5 Detik)",
                "+1.0s (Lebih Lambat 1 Detik)",
                "+2.0s (Lebih Lambat 2 Detik)"
        };

        long curOff = subtitleManager.getSubtitleOffsetMs();
        int checked = 4;
        for (int i = 0; i < offsets.length; i++) {
            if (offsets[i] == curOff) {
                checked = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Sinkronisasi Waktu Subtitle (Offset)")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    if (which >= 0 && which < offsets.length) {
                        subtitleManager.setSubtitleOffsetMs(offsets[which]);
                        Toast.makeText(this, "Offset subtitle: " + labels[which], Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void applySubtitleStyle() {
        if (tvSubtitles == null) return;
        SharedPreferences sp = getSharedPreferences("tatap_player_prefs", Context.MODE_PRIVATE);
        String pos = sp.getString("sub_pos", "low");
        String bg = sp.getString("sub_bg", "subtle");
        String size = sp.getString("sub_size", "medium");

        // 1. Posisi
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tvSubtitles.getLayoutParams();
        int marginDp = "high".equals(pos) ? 54 : ("mid".equals(pos) ? 36 : 22);
        int marginPx = (int) (marginDp * getResources().getDisplayMetrics().density);
        lp.bottomMargin = marginPx;
        tvSubtitles.setLayoutParams(lp);

        // 2. Latar Belakang
        if ("transparent".equals(bg)) {
            tvSubtitles.setBackground(null);
            tvSubtitles.setShadowLayer(5.0f, 2.0f, 2.0f, 0xFF000000);
        } else if ("medium".equals(bg)) {
            tvSubtitles.setBackgroundResource(R.drawable.bg_subtitle_pill);
            if (tvSubtitles.getBackground() != null) {
                tvSubtitles.getBackground().setAlpha((int) (255 * 0.55f));
            }
            tvSubtitles.setShadowLayer(3.5f, 1.5f, 1.5f, 0xFF000000);
        } else if ("dark".equals(bg)) {
            tvSubtitles.setBackgroundResource(R.drawable.bg_subtitle_pill);
            if (tvSubtitles.getBackground() != null) {
                tvSubtitles.getBackground().setAlpha((int) (255 * 0.85f));
            }
            tvSubtitles.setShadowLayer(2.0f, 1.0f, 1.0f, 0xFF000000);
        } else { // subtle (30%)
            tvSubtitles.setBackgroundResource(R.drawable.bg_subtitle_pill);
            if (tvSubtitles.getBackground() != null) {
                tvSubtitles.getBackground().setAlpha((int) (255 * 0.30f));
            }
            tvSubtitles.setShadowLayer(4.0f, 1.5f, 1.5f, 0xFF000000);
        }

        // 3. Ukuran Huruf
        float spVal = "small".equals(size) ? 14f : ("large".equals(size) ? 21f : ("xlarge".equals(size) ? 25f : 17f));
        tvSubtitles.setTextSize(TypedValue.COMPLEX_UNIT_SP, spVal);
    }

    private void showPlayerUnifiedSettingsDialog() {
        String subLabel = subtitleManager.isSubEnabled() ? subtitleManager.getActiveLanguage().toUpperCase() : "Mati";
        String[] menu = {
                "Pilihan Subtitle & Terjemahan (" + subLabel + ")",
                "Pengaturan Tampilan Subtitle",
                "Sinkronisasi Waktu Subtitle (Offset)",
                "Kualitas Video (" + btnQuality.getText() + ")",
                "Kecepatan Pemutaran (" + btnSpeed.getText() + ")",
                "Skala Layar (" + btnAspect.getText() + ")",
                "Kunci Layar (" + (isScreenLocked ? "Terkunci" : "Terbuka") + ")",
                "Picture-in-Picture (PiP)"
        };

        new AlertDialog.Builder(this)
                .setTitle("Pengaturan Pemutar Video")
                .setItems(menu, (dialog, which) -> {
                    switch (which) {
                        case 0: showSubtitleSelectorDialog(); break;
                        case 1: showSubtitleStyleDialog(); break;
                        case 2: showSubtitleOffsetPicker(); break;
                        case 3: showQualityDialog(); break;
                        case 4: showSpeedDialog(); break;
                        case 5: cycleAspectRatio(); break;
                        case 6: toggleScreenLock(); break;
                        case 7: enterPipMode(); break;
                    }
                })
                .setNegativeButton("Tutup", null)
                .show();
    }

    private void toggleScreenLock() {
        isScreenLocked = !isScreenLocked;
        btnScreenLock.setImageResource(isScreenLocked ? R.drawable.ic_lock : R.drawable.ic_lock_open);
        playerView.setUseController(!isScreenLocked);
        layoutTopBar.setVisibility(isScreenLocked ? View.GONE : View.VISIBLE);
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
    protected void onResume() {
        super.onResume();
        isActivityRunning = true;
        subtitleHandler.removeCallbacks(subtitleSyncRunnable);
        subtitleHandler.post(subtitleSyncRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityRunning = false;
        subtitleHandler.removeCallbacks(subtitleSyncRunnable);
        if (player != null) {
            player.pause();
        }
        stopProgressTracker();
        saveCurrentPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityRunning = false;
        subtitleHandler.removeCallbacks(subtitleSyncRunnable);
        stopProgressTracker();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
