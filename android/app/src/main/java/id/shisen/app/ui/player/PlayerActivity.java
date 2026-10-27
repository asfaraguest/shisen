package id.shisen.app.ui.player;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Rational;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.MediaSession;
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

import id.shisen.app.R;
import id.shisen.app.data.db.MenatapDatabase;
import id.shisen.app.data.model.Anime;
import id.shisen.app.data.model.Episode;
import id.shisen.app.data.model.StreamSource;
import id.shisen.app.data.model.SubtitleTrack;
import id.shisen.app.data.service.AniSkipClient;
import id.shisen.app.data.service.HttpClientProvider;
import id.shisen.app.data.service.StreamResolver;
import id.shisen.app.data.subtitle.AiSubtitleService;
import id.shisen.app.data.subtitle.AnimeToshoClient;
import id.shisen.app.data.subtitle.OpenSubtitlesClient;
import id.shisen.app.data.subtitle.SubtitleCue;
import id.shisen.app.data.subtitle.SubtitleManager;
import id.shisen.app.data.subtitle.SubtitleMergeEngine;
import id.shisen.app.data.subtitle.SubtitleParser;
import id.shisen.app.ui.adapter.EpisodeAdapter;
import id.shisen.app.ui.dialog.SettingsDialog;
import id.shisen.app.util.HapticHelper;
import id.shisen.app.util.LocaleHelper;
import id.shisen.app.util.SystemUiHelper;
import okhttp3.OkHttpClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private static final int REQUEST_CODE_PICK_SUBTITLE = 4042;

    private FrameLayout flPlayerWrapper;
    private PlayerView playerView;
    private Button btnSkipOpEd;
    private ImageButton btnScreenLock;
    private View layoutSafetyUnlock;
    private final Runnable hideSafetyUnlockRunnable = this::hideSafetyUnlockPill;
    private LinearLayout layoutBuffering;
    private ProgressBar pbBuffering;
    private TextView tvBufferingText;
    private TextView tvSubtitles;

    // HUD & Gestures
    private LinearLayout hudHoldSpeed;
    private TextView tvHoldSpeedIcon;
    private TextView tvHoldSpeedText;
    private LinearLayout hudOsd;
    private TextView tvOsdIcon;
    private ProgressBar pbOsdBar;
    private TextView tvOsdVal;
    private TextView tvRippleLeft;
    private TextView tvRippleRight;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private int activeGestureZone = 0; // 0 = none, 1 = brightness, 2 = volume
    private boolean isHoldingSpeed = false;
    private boolean isHoldingRewind = false;
    private float currentBrightness = -1f;
    private int lastHapticStep = -1;
    private MediaSession mediaSession;
    private final Handler gestureHandler = new Handler(Looper.getMainLooper());
    private final Runnable holdRewindRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isHoldingRewind || player == null) return;
            long curPos = player.getCurrentPosition();
            long newPos = Math.max(0, curPos - 2000);
            player.seekTo(newPos);
            HapticHelper.performTick(playerView);
            if (isHoldingRewind) {
                gestureHandler.postDelayed(this, 250);
            }
        }
    };

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
    private Button btnPortraitPip;
    private Button btnPortraitFullscreen;
    private TextView tvPortraitEpCount;
    private Button btnPortraitSort;
    private RecyclerView rvPortraitEpisodes;

    // PiP Remote Action Constants
    private static final String ACTION_PIP_CONTROL = "id.shisen.app.PIP_CONTROL";
    private static final String EXTRA_PIP_ACTION = "pip_action";
    private static final int PIP_ACTION_REWIND = 1;
    private static final int PIP_ACTION_PLAY_PAUSE = 2;
    private static final int PIP_ACTION_FORWARD = 3;
    private BroadcastReceiver pipReceiver;
    private boolean pipReceiverRegistered = false;
    private BroadcastReceiver timeTickReceiver;
    private boolean timeTickReceiverRegistered = false;

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
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private long lastHistorySaveTime = 0;
    private long lastHistorySavePosition = -1;

    private boolean isSortAscending = true;

    private volatile AniSkipClient.SkipResult currentSkipTimes;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private final Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private final Handler subtitleHandler = new Handler(Looper.getMainLooper());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private float currentSpeed = 1.0f;
    private boolean isScreenLocked = false;
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private String preferredServer = "hianime";
    private boolean isActivityRunning = false;

    private StreamSource currentStreamSource = null;
    private AudioManager audioManager;
    private float currentVolumeFraction = 0.5f;

    // Multi-source Subtitle Manager
    private SubtitleManager subtitleManager;

    private final Runnable subtitleSyncRunnable = new Runnable() {
        @Override
        public void run() {
            try {
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
            } catch (Throwable ignored) {}
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
        SystemUiHelper.applySystemBarsTheme(this);
        setContentView(R.layout.activity_player);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        subtitleManager = new SubtitleManager(this);

        if (getIntent() != null) {
            anime = (Anime) getIntent().getSerializableExtra("anime");
            @SuppressWarnings("unchecked")
            List<Episode> eps = (List<Episode>) getIntent().getSerializableExtra("episodes");
            if (eps != null) {
                episodeList.addAll(eps);
            }
            currentIndex = getIntent().getIntExtra("selectedIndex", 0);
        }

        if (anime == null && savedInstanceState != null) {
            anime = (Anime) savedInstanceState.getSerializable("saved_anime");
            @SuppressWarnings("unchecked")
            List<Episode> savedEps = (List<Episode>) savedInstanceState.getSerializable("saved_episodes");
            if (savedEps != null && !savedEps.isEmpty()) {
                episodeList.addAll(savedEps);
            }
            currentIndex = savedInstanceState.getInt("saved_index", 0);
        }

        if ((anime == null || episodeList.isEmpty()) && PlaybackDataHolder.hasData()) {
            anime = PlaybackDataHolder.getAnime();
            episodeList.clear();
            episodeList.addAll(PlaybackDataHolder.getEpisodes());
            currentIndex = PlaybackDataHolder.getIndex();
        }

        if (anime == null || episodeList.isEmpty()) {
            Toast.makeText(this, R.string.player_toast_no_episodes, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PlaybackDataHolder.set(anime, episodeList, currentIndex);

        initViews();
        registerPipReceiver();
        setupSubtitleManager();
        setupPlayer();
        setupPortraitRecycler();
        setupGestureControls();
        applySubtitleStyle();
        updateOrientationLayout(getResources().getConfiguration().orientation);
        playEpisode(currentIndex);
    }

    private void initViews() {
        View layoutPlayerRoot = findViewById(R.id.layout_player_root);
        if (layoutPlayerRoot != null) {
            layoutPlayerRoot.setBackgroundResource(id.shisen.app.util.AnimeThemeManager.getCanvasBackgroundResId(this));
        }
        flPlayerWrapper = findViewById(R.id.fl_player_wrapper);
        playerView = findViewById(R.id.player_view);
        btnSkipOpEd = findViewById(R.id.btn_skip_op_ed);
        btnScreenLock = findViewById(R.id.btn_screen_lock);
        layoutSafetyUnlock = findViewById(R.id.layout_safety_unlock);
        layoutBuffering = findViewById(R.id.layout_buffering);
        pbBuffering = findViewById(R.id.pb_video_buffering);
        tvBufferingText = findViewById(R.id.tv_buffering_text);
        tvSubtitles = findViewById(R.id.tv_subtitles);

        hudHoldSpeed = findViewById(R.id.hud_hold_speed);
        tvHoldSpeedIcon = findViewById(R.id.tv_hold_speed_icon);
        tvHoldSpeedText = findViewById(R.id.tv_hold_speed_text);
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
        btnPortraitPip = findViewById(R.id.btn_portrait_pip);
        if (btnPortraitPip != null) {
            boolean showPip = isPipSupported();
            btnPortraitPip.setVisibility(showPip ? View.VISIBLE : View.GONE);
            btnPortraitPip.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                enterPipMode();
            });
        }
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

        if (layoutSafetyUnlock != null) {
            layoutSafetyUnlock.setOnClickListener(v -> unlockScreen(true));
        }

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
        id.shisen.app.util.PerfLogger.markEpisodeTap();
        OkHttpClient okClient = HttpClientProvider.getClient();
        OkHttpDataSource.Factory okHttpFactory = new OkHttpDataSource.Factory(okClient)
                .setUserAgent(HttpClientProvider.USER_AGENT);

        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(okHttpFactory);

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .build();

        // Fase 2.1: Jika pemutaran dilanjutkan dari notifikasi saat layar mati,
        // tahan wake lock jaringan agar audio tidak tersendat. Tidak ada efek saat fg.
        try {
            player.setWakeMode(C.WAKE_MODE_NETWORK);
        } catch (Throwable ignored) {}

        // Logger performa DEBUG-ONLY: first frame, dropped frames, rebuffering
        androidx.media3.exoplayer.analytics.AnalyticsListener perfListener = id.shisen.app.util.PerfLogger.exoPlayerListener();
        if (perfListener != null) {
            player.addAnalyticsListener(perfListener);
        }
        playerView.setPlayer(player);
        playerView.setShowNextButton(true);
        playerView.setShowPreviousButton(true);
        playerView.setShowSubtitleButton(true);
        playerView.setShowVrButton(isPipSupported());

        // Initialize Media3 MediaSession & background PlaybackService
        try {
            Intent sessionIntent = new Intent(this, PlayerActivity.class);
            if (anime != null) sessionIntent.putExtra("anime", anime);
            if (!episodeList.isEmpty()) sessionIntent.putExtra("episodes", (Serializable) episodeList);
            sessionIntent.putExtra("selectedIndex", currentIndex);

            PendingIntent pi = PendingIntent.getActivity(this, 0, sessionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            mediaSession = new MediaSession.Builder(this, player)
                    .setSessionActivity(pi)
                    // Whitelist controller: hanya app sendiri, controller notifikasi media,
                    // dan sistem tepercaya (lockscreen/headset). Pihak ketiga ditolak.
                    .setCallback(PlaybackService.createControllerCallback(this))
                    .build();
            PlaybackService.setActiveSession(mediaSession);

            Intent serviceIntent = new Intent(this, PlaybackService.class);
            try {
                startService(serviceIntent);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}

        // Hide default ExoPlayer subtitle view since we use seamless custom overlay
        androidx.media3.ui.SubtitleView subView = playerView.getSubtitleView();
        if (subView != null) {
            subView.setVisibility(View.GONE);
            subView.setUserDefaultStyle();
        }

        // Reparent tvSubtitles ke playerView.getOverlayFrameLayout() agar berada di panggung overlay resmi Media3 di atas SurfaceView
        FrameLayout overlayFrame = playerView.getOverlayFrameLayout();
        if (overlayFrame != null && tvSubtitles != null) {
            ViewGroup oldParent = (ViewGroup) tvSubtitles.getParent();
            if (oldParent != null && oldParent != overlayFrame) {
                oldParent.removeView(tvSubtitles);
                FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
                );
                int marginH = (int) dpToPx(24);
                flp.leftMargin = marginH;
                flp.rightMargin = marginH;
                int marginB = (int) dpToPx(22);
                flp.bottomMargin = marginB;
                overlayFrame.addView(tvSubtitles, flp);
            }
            tvSubtitles.setElevation(25f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tvSubtitles.setZ(25f);
            }
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
                Toast.makeText(PlayerActivity.this, getString(R.string.player_toast_stream_failed, msg), Toast.LENGTH_LONG).show();
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
                boolean showPip = isPipSupported();
                btnExoPip.setBackgroundResource(R.drawable.bg_exo_icon_btn);
                if (btnExoPip instanceof ImageView) {
                    ((ImageView) btnExoPip).setImageResource(R.drawable.ic_pip);
                }
                btnExoPip.setVisibility(showPip ? View.VISIBLE : View.GONE);
                btnExoPip.setEnabled(showPip);
                btnExoPip.setClickable(showPip);
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

    public boolean isPipSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    private void registerPipReceiver() {
        if (pipReceiver == null && isPipSupported()) {
            pipReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null || !ACTION_PIP_CONTROL.equals(intent.getAction()) || player == null) return;
                    int action = intent.getIntExtra(EXTRA_PIP_ACTION, 0);
                    if (action == PIP_ACTION_REWIND) {
                        seekRelative(-10000);
                    } else if (action == PIP_ACTION_PLAY_PAUSE) {
                        if (player.isPlaying()) {
                            player.pause();
                        } else {
                            player.play();
                        }
                        updatePipParams();
                    } else if (action == PIP_ACTION_FORWARD) {
                        seekRelative(10000);
                    }
                }
            };
            IntentFilter filter = new IntentFilter(ACTION_PIP_CONTROL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(pipReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(pipReceiver, filter);
            }
            pipReceiverRegistered = true;
        }
    }

    private PictureInPictureParams buildPipParams() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null;

        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();

        // 1. Native Video Aspect Ratio
        Rational rational = new Rational(16, 9);
        if (player != null && player.getVideoSize() != null && player.getVideoSize().width > 0 && player.getVideoSize().height > 0) {
            int w = player.getVideoSize().width;
            int h = player.getVideoSize().height;
            float ratio = (float) w / (float) h;
            if (ratio >= 0.418410f && ratio <= 2.39f) {
                rational = new Rational(w, h);
            }
        }
        builder.setAspectRatio(rational);

        // 2. Source Rect Hint
        if (playerView != null) {
            Rect sourceRect = new Rect();
            playerView.getGlobalVisibleRect(sourceRect);
            if (!sourceRect.isEmpty()) {
                builder.setSourceRectHint(sourceRect);
            }
        }

        // 3. Android 12+ (API 31) Auto-Enter and Seamless Resize
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(player != null && player.isPlaying());
            builder.setSeamlessResizeEnabled(true);
        }

        // 4. Remote Actions
        ArrayList<RemoteAction> actions = new ArrayList<>();

        // Action 1: Mundur 10 detik
        Intent rewindIntent = new Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_PIP_ACTION, PIP_ACTION_REWIND);
        rewindIntent.setPackage(getPackageName());
        PendingIntent rewindPendingIntent = PendingIntent.getBroadcast(this, PIP_ACTION_REWIND, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        Icon rewindIcon = Icon.createWithResource(this, R.drawable.ic_replay_10);
        actions.add(new RemoteAction(rewindIcon, "Mundur 10d", "Mundur 10 detik", rewindPendingIntent));

        // Action 2: Play / Pause
        boolean isPlaying = (player != null && player.isPlaying());
        Intent playPauseIntent = new Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_PIP_ACTION, PIP_ACTION_PLAY_PAUSE);
        playPauseIntent.setPackage(getPackageName());
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(this, PIP_ACTION_PLAY_PAUSE, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        Icon playPauseIcon = Icon.createWithResource(this, isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        actions.add(new RemoteAction(playPauseIcon, isPlaying ? "Jeda" : "Putar", isPlaying ? "Jeda video" : "Putar video", playPausePendingIntent));

        // Action 3: Maju 10 detik
        Intent forwardIntent = new Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_PIP_ACTION, PIP_ACTION_FORWARD);
        forwardIntent.setPackage(getPackageName());
        PendingIntent forwardPendingIntent = PendingIntent.getBroadcast(this, PIP_ACTION_FORWARD, forwardIntent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        Icon forwardIcon = Icon.createWithResource(this, R.drawable.ic_forward_10);
        actions.add(new RemoteAction(forwardIcon, "Maju 10d", "Maju 10 detik", forwardPendingIntent));

        builder.setActions(actions);

        return builder.build();
    }

    private void updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPipSupported()) {
            try {
                PictureInPictureParams params = buildPipParams();
                if (params != null) {
                    setPictureInPictureParams(params);
                }
            } catch (Throwable ignored) {}
        }
    }

    private void enterPipMode() {
        if (!isPipSupported() || player == null) return;

        // Safety Screen Lock MUTLAK TIDAK DIBERLAKUKAN di PiP!
        if (isScreenLocked) {
            unlockScreen(false);
        }
        if (layoutSafetyUnlock != null) {
            layoutSafetyUnlock.setVisibility(View.GONE);
        }

        if (playerView != null) {
            playerView.hideController();
        }
        if (layoutTopBar != null) layoutTopBar.setVisibility(View.GONE);
        if (layoutPortraitBottom != null) layoutPortraitBottom.setVisibility(View.GONE);
        if (btnScreenLock != null) btnScreenLock.setVisibility(View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PictureInPictureParams params = buildPipParams();
                if (params != null) {
                    enterPictureInPictureMode(params);
                }
            } catch (Throwable t) {
                Toast.makeText(this, getString(R.string.player_toast_pip_failed, t.getMessage()), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupPortraitRecycler() {
        episodeAdapter = new EpisodeAdapter(this);
        float density = Math.max(1.0f, getResources().getDisplayMetrics().density);
        int spanCount = (int) (getResources().getDisplayMetrics().widthPixels / density) >= 600 ? 5 : 4;
        GridLayoutManager glm = new GridLayoutManager(this, spanCount);
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

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void startHoldingSpeed() {
        if (player == null || isHoldingSpeed || isHoldingRewind) return;
        isHoldingSpeed = true;
        player.setPlaybackParameters(new PlaybackParameters(2.0f));
        if (tvHoldSpeedIcon != null) tvHoldSpeedIcon.setText("⚡");
        if (tvHoldSpeedText != null) tvHoldSpeedText.setText(R.string.player_fast_forward);
        hudHoldSpeed.setVisibility(View.VISIBLE);
        HapticHelper.performTick(playerView);
    }

    private void stopHoldingSpeed() {
        if (!isHoldingSpeed) return;
        isHoldingSpeed = false;
        if (player != null) {
            player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
        }
        hudHoldSpeed.setVisibility(View.GONE);
    }

    private void startHoldingRewind() {
        if (player == null || isHoldingRewind || isHoldingSpeed) return;
        isHoldingRewind = true;
        if (tvHoldSpeedIcon != null) tvHoldSpeedIcon.setText("⏪");
        if (tvHoldSpeedText != null) tvHoldSpeedText.setText("<< Putar Mundur");
        hudHoldSpeed.setVisibility(View.VISIBLE);
        HapticHelper.performTick(playerView);
        gestureHandler.post(holdRewindRunnable);
    }

    private void stopHoldingRewind() {
        if (!isHoldingRewind) return;
        isHoldingRewind = false;
        gestureHandler.removeCallbacks(holdRewindRunnable);
        hudHoldSpeed.setVisibility(View.GONE);
    }

    private void setupGestureControls() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isScreenLocked) return false;
                if (playerView != null) {
                    if (playerView.isControllerFullyVisible()) {
                        playerView.hideController();
                    } else {
                        playerView.showController();
                    }
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (player == null || isScreenLocked || playerView == null) return false;
                int width = playerView.getWidth();
                if (width <= 0) return false;
                float x = e.getX();

                if (x < width * 0.35f) {
                    seekRelative(-10000);
                    showRippleFeedback(tvRippleLeft);
                } else if (x > width * 0.65f) {
                    seekRelative(10000);
                    showRippleFeedback(tvRippleRight);
                } else {
                    if (player.isPlaying()) {
                        player.pause();
                    } else {
                        player.play();
                    }
                }
                HapticHelper.performTick(playerView);
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                if (player == null || isScreenLocked || activeGestureZone != 0 || playerView == null) return;
                int width = playerView.getWidth();
                if (width <= 0) return;
                float x = e.getX();

                if (x >= width * 0.5f) {
                    startHoldingSpeed();
                } else {
                    startHoldingRewind();
                }
            }

            @Override
            public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                if (isScreenLocked || isHoldingSpeed || isHoldingRewind || e1 == null || e2 == null || playerView == null) return false;

                float viewHeight = playerView.getHeight();
                float viewWidth = playerView.getWidth();
                if (viewHeight <= 0 || viewWidth <= 0) return false;

                // Exclusion zones: cegah swipe saat menyentuh status bar atau navigation bar
                boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                float topExclusion = isLandscape ? Math.max(dpToPx(50), viewHeight * 0.12f) : dpToPx(24);
                float bottomExclusion = viewHeight - (isLandscape ? Math.max(dpToPx(40), viewHeight * 0.10f) : dpToPx(24));
                if (e1.getY() < topExclusion || e1.getY() > bottomExclusion) {
                    return false;
                }

                // Disambiguasi awal dan penguncian zona (Edge zoning: kiri 35% kecerahan, kanan 35% volume, tengah 30% deadzone)
                if (activeGestureZone == 0) {
                    float totalDeltaY = Math.abs(e1.getY() - e2.getY());
                    float totalDeltaX = Math.abs(e1.getX() - e2.getX());
                    if (totalDeltaY < dpToPx(12) || totalDeltaY <= totalDeltaX * 1.2f) {
                        return false;
                    }
                    if (e1.getX() <= viewWidth * 0.35f) {
                        activeGestureZone = 1;
                    } else if (e1.getX() >= viewWidth * 0.65f) {
                        activeGestureZone = 2;
                    } else {
                        return false; // Deadzone di tengah layar
                    }
                }

                if (activeGestureZone == 1) {
                    handleBrightnessSwipe(distanceY / viewHeight);
                    return true;
                } else if (activeGestureZone == 2) {
                    handleVolumeSwipe(distanceY / viewHeight);
                    return true;
                }
                return false;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (isScreenLocked || playerView == null) return false;
                float factor = detector.getScaleFactor();
                if (factor > 1.08f && currentResizeMode != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                    currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
                    playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                    btnAspect.setText("ZOOM");
                    btnPortraitFullscreen.setText("ZOOM");
                    showOsd("⛶", 100);
                    HapticHelper.performClick(playerView);
                    return true;
                } else if (factor < 0.92f && currentResizeMode != AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                    currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
                    playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    btnAspect.setText("FIT");
                    btnPortraitFullscreen.setText("FIT");
                    showOsd("⛶", 0);
                    HapticHelper.performClick(playerView);
                    return true;
                }
                return false;
            }
        });

        playerView.setOnTouchListener((v, event) -> {
            if (isScreenLocked) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    showSafetyUnlockPill();
                }
                return true;
            }

            if (scaleGestureDetector != null && event.getPointerCount() > 1) {
                scaleGestureDetector.onTouchEvent(event);
                activeGestureZone = 0;
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                activeGestureZone = 0;
                if (audioManager != null) {
                    int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    currentVolumeFraction = maxVol > 0 ? ((float) curVol / (float) maxVol) : 0f;
                }
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                currentBrightness = lp.screenBrightness < 0 ? 0.5f : lp.screenBrightness;
                lastHapticStep = (int) (currentBrightness * 100) / 5;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                currentVolumeFraction = -1f;
                activeGestureZone = 0;
                if (isHoldingSpeed) {
                    stopHoldingSpeed();
                }
                if (isHoldingRewind) {
                    stopHoldingRewind();
                }
            }

            boolean handled = gestureDetector.onTouchEvent(event);
            return handled || activeGestureZone != 0 || isHoldingSpeed || isHoldingRewind;
        });
    }

    private void handleBrightnessSwipe(float deltaFraction) {
        if (currentBrightness < 0) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            currentBrightness = lp.screenBrightness < 0 ? 0.5f : lp.screenBrightness;
        }
        currentBrightness = Math.max(0.01f, Math.min(1.0f, currentBrightness + deltaFraction * 1.5f));
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = currentBrightness;
        getWindow().setAttributes(lp);

        int percent = Math.round(currentBrightness * 100);
        int step = percent / 5;
        if (step != lastHapticStep) {
            lastHapticStep = step;
            HapticHelper.performTick(playerView);
        }
        showOsd("☀", percent);
    }

    private void handleVolumeSwipe(float deltaFraction) {
        if (audioManager == null) return;
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (maxVol <= 0) return;

        if (currentVolumeFraction < 0) {
            int curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            currentVolumeFraction = (float) curVol / (float) maxVol;
        }

        currentVolumeFraction = Math.max(0f, Math.min(1.0f, currentVolumeFraction + deltaFraction * 1.5f));
        int targetVol = Math.round(currentVolumeFraction * maxVol);
        int curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (targetVol != curVol) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0);
            HapticHelper.performTick(playerView);
        }

        int percent = Math.round(currentVolumeFraction * 100);
        int step = percent / 5;
        if (step != lastHapticStep) {
            lastHapticStep = step;
            HapticHelper.performTick(playerView);
        }
        String icon = targetVol == 0 ? "🔇" : (percent < 50 ? "🔉" : "🔊");
        showOsd(icon, percent);
    }

    private void showOsd(String icon, int percent) {
        tvOsdIcon.setText(icon);
        pbOsdBar.setProgress(percent);
        tvOsdVal.setText(percent + "%");
        hudOsd.bringToFront();
        if (hudOsd.getVisibility() != View.VISIBLE) {
            hudOsd.setAlpha(0f);
            hudOsd.setVisibility(View.VISIBLE);
            hudOsd.animate().alpha(1f).setDuration(120).start();
        } else {
            hudOsd.animate().cancel();
            hudOsd.setAlpha(1f);
        }

        autoHideHandler.removeCallbacksAndMessages(null);
        autoHideHandler.postDelayed(() -> {
            if (hudOsd != null && hudOsd.getVisibility() == View.VISIBLE) {
                hudOsd.animate().alpha(0f).setDuration(180).withEndAction(() -> hudOsd.setVisibility(View.GONE)).start();
            }
        }, 1200);
    }

    private void showRippleFeedback(View rippleView) {
        rippleView.setVisibility(View.VISIBLE);
        rippleView.setAlpha(1.0f);
        rippleView.animate().alpha(0.0f).setDuration(500).withEndAction(() -> rippleView.setVisibility(View.GONE)).start();
    }

    private void playEpisode(int index) {
        playEpisodeWithServer(index, preferredServer);
    }

    private void playEpisodeWithServer(int index, String server) {
        if (index < 0 || index >= episodeList.size()) return;
        currentIndex = index;
        preferredServer = server != null ? server : "hianime";
        Episode ep = episodeList.get(index);

        String safeTitle = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                ? anime.title : anime.romajiTitle;
        tvPortraitAnimeTitle.setText(safeTitle);
        tvPlayerTitle.setText(safeTitle + " · Ep " + ep.number);
        tvPortraitCurrentEp.setText("Episode " + ep.number + " · Menghubungkan (" + preferredServer.toUpperCase() + ")...");
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

        StreamResolver.resolveWithServer(anime, ep, preferredServer, new StreamResolver.Callback() {
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
                    String raw = t.getMessage() != null ? t.getMessage() : "";
                    if (raw.contains("CLEARTEXT")) {
                        Toast.makeText(PlayerActivity.this, "Sumber ini hanya HTTP tanpa enkripsi — diblokir oleh kebijakan keamanan.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(PlayerActivity.this, "Gagal memuat stream:\n" + raw, Toast.LENGTH_LONG).show();
                    }
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

        MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder();
        if (ep != null) {
            metaBuilder.setTitle(ep.title != null && !ep.title.isEmpty() ? ep.title : "Episode " + ep.number);
        }
        if (anime != null) {
            metaBuilder.setArtist(anime.title);
            if (anime.poster != null && !anime.poster.isEmpty()) {
                metaBuilder.setArtworkUri(Uri.parse(anime.poster));
            }
        }
        mediaItemBuilder.setMediaMetadata(metaBuilder.build());

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
                try {
                    if (player != null && player.isPlaying()) {
                        checkSkipTimes();
                        saveCurrentPlayback(false);
                    }
                } catch (Throwable ignored) {}
                if (isActivityRunning) {
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

        boolean autoSkip = getSharedPreferences("menatap_player_prefs", Context.MODE_PRIVATE)
                .getBoolean("pref_auto_skip", false);

        if (currentSkipTimes.intro != null && posSec >= currentSkipTimes.intro.startTime && posSec < currentSkipTimes.intro.endTime) {
            if (autoSkip) {
                player.seekTo((long) (currentSkipTimes.intro.endTime * 1000));
                Toast.makeText(this, R.string.player_toast_auto_op, Toast.LENGTH_SHORT).show();
                btnSkipOpEd.setVisibility(View.GONE);
            } else {
                btnSkipOpEd.setText(getString(R.string.player_skip_op));
                btnSkipOpEd.setVisibility(View.VISIBLE);
            }
        } else if (currentSkipTimes.outro != null && posSec >= currentSkipTimes.outro.startTime && posSec < currentSkipTimes.outro.endTime) {
            if (autoSkip) {
                player.seekTo((long) (currentSkipTimes.outro.endTime * 1000));
                Toast.makeText(this, R.string.player_toast_auto_ed, Toast.LENGTH_SHORT).show();
                btnSkipOpEd.setVisibility(View.GONE);
            } else {
                btnSkipOpEd.setText(getString(R.string.player_skip_ed));
                btnSkipOpEd.setVisibility(View.VISIBLE);
            }
        } else {
            btnSkipOpEd.setVisibility(View.GONE);
        }
    }

    private void saveCurrentPlayback() {
        saveCurrentPlayback(false);
    }

    private void saveCurrentPlayback(boolean force) {
        try {
            if (player == null || anime == null || episodeList.isEmpty() || currentIndex >= episodeList.size()) return;
            Episode ep = episodeList.get(currentIndex);
            final long pos = player.getCurrentPosition();
            long durVal = player.getDuration();
            if (durVal <= 0) durVal = 1;
            final long dur = durVal;

            PlaybackDataHolder.setPositionMs(pos);

            long now = System.currentTimeMillis();
            // Debounce disk SQLite writes: write at most once every 10s unless forced (e.g. pause/stop/episode change)
            if (!force && (now - lastHistorySaveTime < 10000) && Math.abs(pos - lastHistorySavePosition) < 10000) {
                return;
            }
            lastHistorySaveTime = now;
            lastHistorySavePosition = pos;

            final String slug = anime.slug;
            final String title = anime.title;
            final String poster = anime.poster;
            final int epNum = ep.number;
            dbExecutor.execute(() -> {
                try {
                    MenatapDatabase.getInstance(getApplicationContext()).saveHistory(slug, title, poster, epNum, pos, dur);
                } catch (Throwable ignored) {}
            });

            if (!ep.isWatched) {
                ep.isWatched = true;
                if (episodeAdapter != null) {
                    episodeAdapter.notifyItemChanged(currentIndex);
                }
            }
        } catch (Throwable ignored) {}
    }

    private void playNextEpisode() {
        if (currentIndex + 1 < episodeList.size()) {
            Toast.makeText(this, R.string.player_toast_next_ep, Toast.LENGTH_SHORT).show();
            playEpisode(currentIndex + 1);
        }
    }

    private void playPrevEpisode() {
        if (currentIndex - 1 >= 0) {
            Toast.makeText(this, R.string.player_toast_prev_ep, Toast.LENGTH_SHORT).show();
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
        String[] qKeys = {"auto", "1080", "720", "480", "360"};

        int selectedIndex = 0;
        String cur = btnQuality.getText().toString();
        for (int i = 0; i < qKeys.length; i++) {
            if (cur.toLowerCase().contains(qKeys[i])) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.player_dialog_quality_title)
                .setSingleChoiceItems(qualities, selectedIndex, (dialog, which) -> {
                    String key = qKeys[which];
                    String label = qualities[which].toString().split(" ")[0];
                    btnQuality.setText(label);
                    btnPortraitQuality.setText(label);
                    applyHlsTrackSelection(key);
                    getSharedPreferences("menatap_player_prefs", Context.MODE_PRIVATE)
                            .edit().putString("pref_quality", key).apply();
                    Toast.makeText(this, getString(R.string.player_toast_quality_changed, label), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void applyHlsTrackSelection(String quality) {
        if (player == null) return;
        if (quality == null || quality.isEmpty()) quality = "auto";

        androidx.media3.common.TrackSelectionParameters.Builder builder =
                player.getTrackSelectionParameters().buildUpon()
                        .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_VIDEO)
                        .clearVideoSizeConstraints();

        if (!"auto".equalsIgnoreCase(quality)) {
            int targetH = 1080;
            if (quality.contains("1080")) {
                targetH = 1080;
            } else if (quality.contains("720")) {
                targetH = 720;
            } else if (quality.contains("480")) {
                targetH = 480;
            } else if (quality.contains("360")) {
                targetH = 360;
            }

            androidx.media3.common.Tracks currentTracks = player.getCurrentTracks();
            androidx.media3.common.TrackGroup targetGroup = null;
            int targetIndex = -1;
            int bestDiff = Integer.MAX_VALUE;

            for (androidx.media3.common.Tracks.Group group : currentTracks.getGroups()) {
                if (group.getType() == androidx.media3.common.C.TRACK_TYPE_VIDEO) {
                    androidx.media3.common.TrackGroup tg = group.getMediaTrackGroup();
                    for (int i = 0; i < tg.length; i++) {
                        androidx.media3.common.Format format = tg.getFormat(i);
                        int diff = Math.abs(format.height - targetH);
                        if (diff < bestDiff) {
                            bestDiff = diff;
                            targetGroup = tg;
                            targetIndex = i;
                        }
                    }
                }
            }

            if (targetGroup != null && targetIndex >= 0) {
                builder.addOverride(new androidx.media3.common.TrackSelectionOverride(targetGroup, targetIndex));
            } else {
                int maxH = Integer.MAX_VALUE;
                int maxW = Integer.MAX_VALUE;
                if (quality.contains("1080")) {
                    maxH = 1080; maxW = 1920;
                } else if (quality.contains("720")) {
                    maxH = 720; maxW = 1280;
                } else if (quality.contains("480")) {
                    maxH = 480; maxW = 854;
                } else if (quality.contains("360")) {
                    maxH = 360; maxW = 640;
                }
                builder.setMaxVideoSize(maxW, maxH);
            }
        }
        player.setTrackSelectionParameters(builder.build());
    }

    private void showServerSelectorDialog() {
        CharSequence[] servers = {
                "HiAnime (Utama · Multi-Audio & Subtitle HD)",
                "AniChan (Cepat · Direct HLS Stream)",
                "Otakudesu (Cadangan · Hardsub Indonesia)"
        };
        String[] serverKeys = {"hianime", "anichan", "otakudesu"};

        int selectedIdx = 0;
        for (int i = 0; i < serverKeys.length; i++) {
            if (serverKeys[i].equalsIgnoreCase(preferredServer)) {
                selectedIdx = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.player_dialog_server_title)
                .setSingleChoiceItems(servers, selectedIdx, (dialog, which) -> {
                    preferredServer = serverKeys[which];
                    dialog.dismiss();
                    Toast.makeText(this, getString(R.string.player_toast_server_switching, servers[which].toString().split(" ")[0]), Toast.LENGTH_SHORT).show();
                    if (!episodeList.isEmpty() && currentIndex < episodeList.size()) {
                        playEpisodeWithServer(currentIndex, preferredServer);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showSpeedDialog() {
        CharSequence[] speeds = {"0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x"};
        float[] speedVals = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

        new AlertDialog.Builder(this)
                .setTitle(R.string.player_dialog_speed_title)
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

    private int getCurrentEpisodeNumberSafe() {
        if (episodeList != null && currentIndex >= 0 && currentIndex < episodeList.size()) {
            Episode ep = episodeList.get(currentIndex);
            if (ep != null) return ep.number;
        }
        return 1;
    }

    private String getSafeAnimeTitle() {
        if (anime == null) return "";
        if (anime.title != null && !anime.title.trim().isEmpty()) {
            return anime.title.trim();
        }
        if (anime.romajiTitle != null && !anime.romajiTitle.trim().isEmpty()) {
            return anime.romajiTitle.trim();
        }
        return "";
    }

    private SubtitleTrack findEnglishTrack(List<SubtitleTrack> tracks) {
        if (tracks == null) return null;
        for (SubtitleTrack t : tracks) {
            String l = t.lang.toLowerCase();
            if (l.contains("en") || t.label.toLowerCase().contains("english")) {
                return t;
            }
        }
        return null;
    }

    private void showSubtitleSelectorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.player_dialog_sub_title);

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
            subtitleManager.setMode("id", "smart_merge"); // Set state SEBELUM load
            if (currentStreamSource != null) {
                subtitleManager.loadFromTracks(currentStreamSource.subtitles, getSafeAnimeTitle(), getCurrentEpisodeNumberSafe(), false);
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
                        subtitleManager.loadRawTrack(st.url, st.label, "id", st.referer);
                    });
                    break;
                }
            }
        }

        // 3. AI Subtitle BYOK & Custom Prompt Quick Configuration
        String aiKey = AiSubtitleService.getKey(this);
        String aiLabel = (aiKey != null && !aiKey.isEmpty())
                ? "✨ Pengaturan AI Subtitle & Prompt [" + AiSubtitleService.Provider.detect(aiKey).displayName + "]"
                : "✨ Pengaturan AI Subtitle (Gemini / Groq / OpenAI)...";
        options.add(aiLabel);
        actions.add(this::showAiSubtitleSettingsDialog);

        // 3. [EN] English (Original)
        boolean isEnSelected = isSubOn && "en".equals(curLang) && "raw".equals(curEngine);
        options.add("[EN] English (Original / Asli)" + (isEnSelected ? " [Aktif]" : ""));
        actions.add(() -> {
            subtitleManager.setSubEnabled(true);
            if (currentStreamSource != null) {
                SubtitleTrack enTrack = findEnglishTrack(currentStreamSource.subtitles);
                if (enTrack != null) {
                    subtitleManager.loadRawTrack(enTrack.url, "English (Original)", "en", enTrack.referer);
                } else if (!currentStreamSource.subtitles.isEmpty()) {
                    SubtitleTrack first = currentStreamSource.subtitles.get(0);
                    subtitleManager.loadRawTrack(first.url, first.label, first.lang, first.referer);
                }
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
                    subtitleManager.loadRawTrack(st.url, st.label, st.lang, st.referer);
                });
            }
        }

        // 5. Refresh / Re-translate (Bypass Cache)
        if (isSubOn && "id".equals(curLang)) {
            options.add("[Refresh] Terjemahkan Ulang Subtitle (Hapus Cache)");
            actions.add(() -> {
                Toast.makeText(this, R.string.player_toast_retranslating, Toast.LENGTH_SHORT).show();
                if (currentStreamSource != null) {
                    subtitleManager.loadFromTracks(currentStreamSource.subtitles, getSafeAnimeTitle(), getCurrentEpisodeNumberSafe(), true);
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
        Toast.makeText(this, R.string.player_toast_searching_subs, Toast.LENGTH_SHORT).show();
        String safeTitle = getSafeAnimeTitle();
        int epNumber = getCurrentEpisodeNumberSafe();

        AnimeToshoClient.searchIndonesianAsync(safeTitle, epNumber, new AnimeToshoClient.SearchCallback() {
            @Override
            public void onSuccess(List<AnimeToshoClient.SubtitleSearchResult> atResults) {
                OpenSubtitlesClient.searchIndonesianAsync(safeTitle, epNumber, new OpenSubtitlesClient.SearchCallback() {
                    @Override
                    public void onSuccess(List<OpenSubtitlesClient.SubtitleSearchResult> osResults) {
                        runOnUiThread(() -> {
                            if (atResults.isEmpty() && osResults.isEmpty()) {
                                Toast.makeText(PlayerActivity.this, R.string.player_toast_subs_not_found, Toast.LENGTH_LONG).show();
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
                                    .setTitle(R.string.player_dialog_fansub_title)
                                    .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                                        if (which >= 0 && which < itemActions.size()) {
                                            itemActions.get(which).run();
                                        }
                                    })
                                    .setNegativeButton(R.string.dialog_cancel, null)
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
        Toast.makeText(this, getString(R.string.player_toast_downloading_sub, filename), Toast.LENGTH_SHORT).show();
        AnimeToshoClient.downloadAndParseAsync(url, new AnimeToshoClient.DownloadCallback() {
            @Override
            public void onSuccess(List<SubtitleCue> cues) {
                runOnUiThread(() -> {
                    subtitleManager.setCustomCues(cues, "[AnimeTosho] " + filename);
                    Toast.makeText(PlayerActivity.this, getString(R.string.player_toast_sub_loaded, cues.size()), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(PlayerActivity.this, error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void downloadAndApplyOpenSubtitlesSub(String url, String filename) {
        Toast.makeText(this, getString(R.string.player_toast_downloading_sub, filename), Toast.LENGTH_SHORT).show();
        OpenSubtitlesClient.downloadAndParseAsync(url, new OpenSubtitlesClient.DownloadCallback() {
            @Override
            public void onSuccess(List<SubtitleCue> cues) {
                runOnUiThread(() -> {
                    subtitleManager.setCustomCues(cues, "[OpenSubtitles] " + filename);
                    Toast.makeText(PlayerActivity.this, getString(R.string.player_toast_sub_loaded, cues.size()), Toast.LENGTH_SHORT).show();
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
                    runOnUiThread(() -> Toast.makeText(this, R.string.player_toast_sub_empty, Toast.LENGTH_LONG).show());
                    return;
                }

                runOnUiThread(() -> {
                    subtitleManager.setCustomCues(cues, "Subtitle Lokal");
                    Toast.makeText(this, getString(R.string.player_toast_sub_loaded, cues.size()), Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Gagal memuat berkas subtitle: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void showCustomSubtitleUrlDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.player_dialog_custom_sub_title);
        final EditText input = new EditText(this);
        input.setHint("https://example.com/sub.vtt atau .srt");
        input.setSingleLine(true);
        input.setPadding(40, 30, 40, 30);
        b.setView(input);

        b.setPositiveButton(R.string.dialog_load, (dialog, which) -> {
            String url = input.getText().toString().trim();
            if (!url.isEmpty()) {
                subtitleManager.loadRawTrack(url, "Kustom URL", "custom");
            }
        });
        b.setNegativeButton(R.string.dialog_cancel, null);
        b.show();
    }

    private void showSubtitleStyleDialog() {
        SharedPreferences sp = getSharedPreferences("menatap_player_prefs", Context.MODE_PRIVATE);
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
                .setTitle(R.string.player_dialog_sub_style_title)
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
                        Toast.makeText(this, R.string.player_toast_sub_style_reset, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private void showPositionPicker(SharedPreferences sp) {
        String[] options = {"Bawah (22dp) [Direkomendasikan]", "Sedang (36dp)", "Tinggi (54dp)"};
        String cur = sp.getString("sub_pos", "low");
        int checked = "high".equals(cur) ? 2 : ("mid".equals(cur) ? 1 : 0);

        new AlertDialog.Builder(this)
                .setTitle(R.string.player_dialog_sub_pos_title)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    String val = which == 2 ? "high" : (which == 1 ? "mid" : "low");
                    sp.edit().putString("sub_pos", val).apply();
                    applySubtitleStyle();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showBackgroundPicker(SharedPreferences sp) {
        String[] options = {"Transparan (Tanpa Box / Shadow)", "Samar (30% Alpha) [Rekomendasi]", "Sedang (55% Alpha)", "Pekat (80% Alpha)"};
        String cur = sp.getString("sub_bg", "subtle");
        int checked = "dark".equals(cur) ? 3 : ("medium".equals(cur) ? 2 : ("transparent".equals(cur) ? 0 : 1));

        new AlertDialog.Builder(this)
                .setTitle(R.string.player_dialog_sub_bg_title)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    String val = which == 3 ? "dark" : (which == 2 ? "medium" : (which == 0 ? "transparent" : "subtle"));
                    sp.edit().putString("sub_bg", val).apply();
                    applySubtitleStyle();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showSizePicker(SharedPreferences sp) {
        String[] options = {"Kecil (14sp)", "Sedang (17sp) [Standar HP]", "Besar (21sp)", "Sangat Besar (25sp)"};
        String cur = sp.getString("sub_size", "medium");
        int checked = "xlarge".equals(cur) ? 3 : ("large".equals(cur) ? 2 : ("small".equals(cur) ? 0 : 1));

        new AlertDialog.Builder(this)
                .setTitle(R.string.player_dialog_sub_size_title)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    String val = which == 3 ? "xlarge" : (which == 2 ? "large" : (which == 0 ? "small" : "medium"));
                    sp.edit().putString("sub_size", val).apply();
                    applySubtitleStyle();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showAiSubtitleSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_subtitle_settings, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvBadge = dialogView.findViewById(R.id.tv_ai_provider_badge);
        EditText etKey = dialogView.findViewById(R.id.et_ai_api_key);
        LinearLayout layoutChips = dialogView.findViewById(R.id.layout_model_chips);
        EditText etModel = dialogView.findViewById(R.id.et_ai_model);
        EditText etCustomPrompt = dialogView.findViewById(R.id.et_ai_custom_prompt);
        TextView btnResetPrompt = dialogView.findViewById(R.id.btn_reset_ai_prompt);
        Button btnSave = dialogView.findViewById(R.id.btn_save_ai_key);
        Button btnClear = dialogView.findViewById(R.id.btn_clear_ai_key);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close_ai_dialog);

        String currentKey = AiSubtitleService.getKey(this);
        if (currentKey != null && !currentKey.isEmpty()) {
            etKey.setText(currentKey);
            AiSubtitleService.Provider p = AiSubtitleService.Provider.detect(currentKey);
            tvBadge.setText("Provider Aktif: " + p.displayName);
        }

        String savedModel = AiSubtitleService.getModel(this);
        if (savedModel != null && !savedModel.isEmpty()) {
            if (etModel != null) etModel.setText(savedModel);
        } else {
            AiSubtitleService.Provider p = AiSubtitleService.getActiveProvider(this);
            if (etModel != null) etModel.setText(AiSubtitleService.getDefaultModel(p));
        }

        SettingsDialog.populateModelChips(layoutChips, this, AiSubtitleService.getActiveProvider(this), etModel);

        if (etCustomPrompt != null) {
            etCustomPrompt.setText(AiSubtitleService.getCustomPrompt(this));
        }

        if (btnResetPrompt != null && etCustomPrompt != null) {
            btnResetPrompt.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                etCustomPrompt.setText(AiSubtitleService.DEFAULT_CUSTOM_PROMPT);
                Toast.makeText(this, "Prompt direset ke default fansub anime", Toast.LENGTH_SHORT).show();
            });
        }

        etKey.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String input = s != null ? s.toString().trim() : "";
                AiSubtitleService.Provider p = AiSubtitleService.Provider.detect(input);
                if (!input.isEmpty()) {
                    tvBadge.setText("Terdeteksi: " + p.displayName);
                } else {
                    tvBadge.setText("Provider: Gemini / Groq / OpenAI / Ollama");
                }
                SettingsDialog.populateModelChips(layoutChips, PlayerActivity.this, p, etModel);
                if (etModel != null && etModel.getText().toString().trim().isEmpty()) {
                    etModel.setText(AiSubtitleService.getDefaultModel(p));
                }
            }
        });

        btnSave.setOnClickListener(v -> {
            String newKey = etKey.getText().toString().trim();
            if (newKey.isEmpty()) {
                Toast.makeText(this, "Kunci API tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            AiSubtitleService.saveKey(this, newKey);
            if (etCustomPrompt != null) {
                String promptText = etCustomPrompt.getText().toString().trim();
                AiSubtitleService.saveCustomPrompt(this, promptText);
            }
            if (etModel != null) {
                AiSubtitleService.saveModel(this, etModel.getText().toString().trim());
            }
            AiSubtitleService.Provider p = AiSubtitleService.Provider.detect(newKey);
            Toast.makeText(this, "Pengaturan " + p.displayName + " & Model Selector berhasil disimpan", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnClear.setOnClickListener(v -> {
            AiSubtitleService.clearKey(this);
            AiSubtitleService.clearCustomPrompt(this);
            AiSubtitleService.clearModel(this);
            etKey.setText("");
            if (etModel != null) {
                etModel.setText(AiSubtitleService.getDefaultModel(AiSubtitleService.Provider.GEMINI));
            }
            SettingsDialog.populateModelChips(layoutChips, this, AiSubtitleService.Provider.GEMINI, etModel);
            if (etCustomPrompt != null) {
                etCustomPrompt.setText(AiSubtitleService.DEFAULT_CUSTOM_PROMPT);
            }
            tvBadge.setText("Provider: Gemini / Groq / OpenAI / Ollama");
            Toast.makeText(this, "Kunci API dan pengaturan telah dihapus", Toast.LENGTH_SHORT).show();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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
                .setTitle(R.string.player_dialog_sub_offset_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    if (which >= 0 && which < offsets.length) {
                        subtitleManager.setSubtitleOffsetMs(offsets[which]);
                        Toast.makeText(this, getString(R.string.player_toast_offset, labels[which]), Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void applySubtitleStyle() {
        if (tvSubtitles == null) return;
        SharedPreferences sp = getSharedPreferences("menatap_player_prefs", Context.MODE_PRIVATE);
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
        boolean autoSkip = getSharedPreferences("menatap_player_prefs", Context.MODE_PRIVATE)
                .getBoolean("pref_auto_skip", false);

        String[] menu = {
                "Pilihan Subtitle & Terjemahan (" + subLabel + ")",
                "Pengaturan Tampilan Subtitle",
                "Sinkronisasi Waktu Subtitle (Offset)",
                "Kualitas Video (" + btnQuality.getText() + ")",
                "Pilihan Server / Sumber Video (" + preferredServer.toUpperCase() + ")",
                "Auto-Skip Opening/Ending (" + (autoSkip ? "Aktif" : "Mati") + ")",
                "Kecepatan Pemutaran (" + btnSpeed.getText() + ")",
                "Skala Layar (" + btnAspect.getText() + ")",
                "Kunci Layar (" + (isScreenLocked ? "Terkunci" : "Terbuka") + ")",
                "Picture-in-Picture (PiP)"
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.player_dialog_main_title)
                .setItems(menu, (dialog, which) -> {
                    switch (which) {
                        case 0: showSubtitleSelectorDialog(); break;
                        case 1: showSubtitleStyleDialog(); break;
                        case 2: showSubtitleOffsetPicker(); break;
                        case 3: showQualityDialog(); break;
                        case 4: showServerSelectorDialog(); break;
                        case 5:
                            boolean newAuto = !autoSkip;
                            getSharedPreferences("menatap_player_prefs", Context.MODE_PRIVATE)
                                    .edit().putBoolean("pref_auto_skip", newAuto).apply();
                            Toast.makeText(this, getString(R.string.player_toast_auto_skip, (newAuto ? getString(R.string.label_enabled) : getString(R.string.label_disabled))), Toast.LENGTH_SHORT).show();
                            break;
                        case 6: showSpeedDialog(); break;
                        case 7: cycleAspectRatio(); break;
                        case 8: toggleScreenLock(); break;
                        case 9: enterPipMode(); break;
                    }
                })
                .setNegativeButton(R.string.dialog_close, null)
                .show();
    }

    private void showSafetyUnlockPill() {
        if (layoutSafetyUnlock == null) return;
        mainHandler.removeCallbacks(hideSafetyUnlockRunnable);
        layoutSafetyUnlock.setVisibility(View.VISIBLE);
        layoutSafetyUnlock.setAlpha(1.0f);
        mainHandler.postDelayed(hideSafetyUnlockRunnable, 3500);
    }

    private void hideSafetyUnlockPill() {
        if (layoutSafetyUnlock == null || layoutSafetyUnlock.getVisibility() != View.VISIBLE) return;
        layoutSafetyUnlock.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction(() -> {
                    if (layoutSafetyUnlock != null) layoutSafetyUnlock.setVisibility(View.GONE);
                })
                .start();
    }

    private void lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
            return;
        }
        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, R.string.player_toast_lock_fullscreen_only, Toast.LENGTH_SHORT).show();
            return;
        }
        isScreenLocked = true;
        if (btnScreenLock != null) {
            btnScreenLock.setImageResource(R.drawable.ic_lock);
            btnScreenLock.setVisibility(View.GONE);
        }
        if (playerView != null) {
            playerView.hideController();
            playerView.setUseController(false);
        }
        if (layoutTopBar != null) layoutTopBar.setVisibility(View.GONE);
        if (btnSkipOpEd != null) btnSkipOpEd.setVisibility(View.GONE);
        showSafetyUnlockPill();
        HapticHelper.performClick(layoutSafetyUnlock != null ? layoutSafetyUnlock : playerView);
        Toast.makeText(this, R.string.player_toast_screen_locked, Toast.LENGTH_SHORT).show();
    }

    private void unlockScreen(boolean showFeedback) {
        if (!isScreenLocked) return;
        isScreenLocked = false;
        mainHandler.removeCallbacks(hideSafetyUnlockRunnable);
        if (layoutSafetyUnlock != null) {
            layoutSafetyUnlock.setVisibility(View.GONE);
        }
        if (btnScreenLock != null) {
            btnScreenLock.setImageResource(R.drawable.ic_lock_open);
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            boolean isInPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode();
            btnScreenLock.setVisibility(isLandscape && !isInPip ? View.VISIBLE : View.GONE);
        }
        if (playerView != null) {
            playerView.setUseController(true);
            playerView.showController();
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (layoutTopBar != null) layoutTopBar.setVisibility(View.VISIBLE);
        }
        if (showFeedback) {
            HapticHelper.performClick(layoutSafetyUnlock != null ? layoutSafetyUnlock : playerView);
            Toast.makeText(this, R.string.player_toast_screen_unlocked, Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleScreenLock() {
        if (isScreenLocked) {
            unlockScreen(true);
        } else {
            lockScreen();
        }
    }

    @Override
    public void onBackPressed() {
        if (isScreenLocked) {
            showSafetyUnlockPill();
            Toast.makeText(this, R.string.player_toast_lock_hint, Toast.LENGTH_SHORT).show();
            return;
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            return;
        }
        super.onBackPressed();
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
            layoutTopBar.setVisibility(!isScreenLocked ? View.VISIBLE : View.GONE);
            boolean isInPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode();
            btnScreenLock.setVisibility(!isInPip && !isScreenLocked ? View.VISIBLE : View.GONE);
            btnPortraitFullscreen.setText(R.string.player_minimize);

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
            if (isScreenLocked) {
                unlockScreen(false); // Otomatis buka kunci bila diputar ke Portrait!
            }
            if (layoutSafetyUnlock != null) {
                layoutSafetyUnlock.setVisibility(View.GONE);
            }
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
            getWindow().setStatusBarColor(Color.BLACK);
            if (controller != null) {
                controller.show(WindowInsetsCompat.Type.systemBars());
                controller.setAppearanceLightStatusBars(false);
                controller.setAppearanceLightNavigationBars(false);
            }
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && isPipSupported() && player != null && player.isPlaying()) {
            enterPipMode();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            if (isScreenLocked) {
                unlockScreen(false); // MUTLAK BEBAS KUNCI DI PIP
            }
            if (layoutSafetyUnlock != null) {
                layoutSafetyUnlock.setVisibility(View.GONE);
            }
            if (playerView != null) {
                playerView.hideController();
                playerView.setUseController(false);
            }
            if (layoutTopBar != null) layoutTopBar.setVisibility(View.GONE);
            if (layoutPortraitBottom != null) layoutPortraitBottom.setVisibility(View.GONE);
            if (btnScreenLock != null) btnScreenLock.setVisibility(View.GONE);
            if (hudOsd != null) hudOsd.setVisibility(View.GONE);
            if (hudHoldSpeed != null) hudHoldSpeed.setVisibility(View.GONE);
            if (tvSubtitles != null) {
                tvSubtitles.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
            }
        } else {
            isScreenLocked = false;
            if (playerView != null) {
                playerView.setUseController(true);
            }
            applySubtitleStyle();

            int orientation = getResources().getConfiguration().orientation;
            updateOrientationLayout(orientation);
            updatePipParams();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        backgroundStopHandler.removeCallbacks(backgroundStopRunnable);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
            return;
        }
        isActivityRunning = true;
        subtitleHandler.removeCallbacks(subtitleSyncRunnable);
        subtitleHandler.post(subtitleSyncRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
            return;
        }
        isActivityRunning = false;
        subtitleHandler.removeCallbacks(subtitleSyncRunnable);
        if (player != null) {
            player.pause();
        }
        stopProgressTracker();
        saveCurrentPlayback(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.pause();
        }
        stopProgressTracker();
        saveCurrentPlayback(true);

        // Fase 2.1(b): Pause di background lebih dari 3 menit → hentikan service & release player
        // (menghemat memori/wake lock; sesi media dihentikan, data progres sudah disimpan).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode()) {
            backgroundStopHandler.postDelayed(backgroundStopRunnable, 3 * 60 * 1000L);
        }
    }

    private final Handler backgroundStopHandler = new Handler(Looper.getMainLooper());

    private final Runnable backgroundStopRunnable = new Runnable() {
        @Override
        public void run() {
            if (isActivityRunning) return; // user sudah kembali ke foreground
            if (player == null || mediaSession == null) return;
            if (player.isPlaying()) return; // tetap hidup jika ternyata masih memutar
            try {
                PlaybackService.setActiveSession(null);
                mediaSession.release();
            } catch (Throwable ignored) {}
            mediaSession = null;
            try {
                stopService(new Intent(PlayerActivity.this, PlaybackService.class));
            } catch (Throwable ignored) {}
            try {
                player.release();
            } catch (Throwable ignored) {}
            player = null;
        }
    };

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (anime != null) {
            outState.putSerializable("saved_anime", anime);
        }
        if (!episodeList.isEmpty()) {
            outState.putSerializable("saved_episodes", (Serializable) new ArrayList<>(episodeList));
        }
        outState.putInt("saved_index", currentIndex);
        if (player != null) {
            long pos = player.getCurrentPosition();
            outState.putLong("saved_position", pos);
            PlaybackDataHolder.setPositionMs(pos);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.hasExtra("anime")) {
            Anime newAnime = (Anime) intent.getSerializableExtra("anime");
            @SuppressWarnings("unchecked")
            List<Episode> newEps = (List<Episode>) intent.getSerializableExtra("episodes");
            int newIndex = intent.getIntExtra("selectedIndex", -1);
            if (newAnime != null && newEps != null && !newEps.isEmpty()) {
                anime = newAnime;
                episodeList.clear();
                episodeList.addAll(newEps);
                if (newIndex >= 0) {
                    PlaybackDataHolder.set(anime, episodeList, newIndex);
                    if (newIndex != currentIndex) {
                        playEpisode(newIndex);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundStopHandler.removeCallbacks(backgroundStopRunnable);
        if (pipReceiverRegistered && pipReceiver != null) {
            try {
                unregisterReceiver(pipReceiver);
            } catch (IllegalArgumentException ignored) {}
            pipReceiverRegistered = false;
            pipReceiver = null;
        }
        if (timeTickReceiverRegistered && timeTickReceiver != null) {
            try {
                unregisterReceiver(timeTickReceiver);
            } catch (IllegalArgumentException ignored) {}
            timeTickReceiverRegistered = false;
            timeTickReceiver = null;
        }
        isActivityRunning = false;
        subtitleHandler.removeCallbacks(subtitleSyncRunnable);
        stopProgressTracker();
        if (player != null) {
            saveCurrentPlayback(true);
        }
        if (mediaSession != null) {
            PlaybackService.setActiveSession(null);
            try {
                mediaSession.release();
            } catch (Throwable ignored) {}
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
