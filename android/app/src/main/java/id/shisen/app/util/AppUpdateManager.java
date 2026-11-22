package id.shisen.app.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

import id.shisen.app.R;
import id.shisen.app.data.service.HttpClientProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Manajer Pembaruan Shisen (100% Pure Native Java).
 * - Pemeriksaan rilis publik via GitHub Releases API.
 * - Membuka halaman rilis resmi GitHub / tautan unduhan APK di browser sistem tanpa
 *   meminta izin REQUEST_INSTALL_PACKAGES agar aman dari pemblokiran Google Play Protect.
 */
public class AppUpdateManager {

    public static final String GITHUB_OWNER = "asfaraguest";
    public static final String GITHUB_REPO = "shisen";
    public static final String GITHUB_RELEASES_URL = "https://github.com/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases";
    public static final String GITHUB_API_RELEASES = "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases?per_page=10";
    public static final String GITHUB_API_LATEST_RELEASE = "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest";

    private static final String[] UPDATE_URLS = {
            GITHUB_API_RELEASES,
            GITHUB_API_LATEST_RELEASE
    };

    public static class UpdateCandidate {
        public final String tagName;
        public final String version;
        public final String notes;
        public final String downloadUrl;
        public final long sizeBytes;
        public final String htmlUrl;

        public UpdateCandidate(String tagName, String version, String notes, String downloadUrl, long sizeBytes, String htmlUrl) {
            this.tagName = tagName;
            this.version = version;
            this.notes = notes;
            this.downloadUrl = downloadUrl;
            this.sizeBytes = sizeBytes;
            this.htmlUrl = htmlUrl;
        }
    }

    private static volatile AppUpdateManager instance;

    private volatile boolean isChecking = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AppUpdateManager() {}

    public static AppUpdateManager getInstance() {
        if (instance == null) {
            synchronized (AppUpdateManager.class) {
                if (instance == null) {
                    instance = new AppUpdateManager();
                }
            }
        }
        return instance;
    }

    public static String getInstalledVersion(Context context) {
        if (context != null) {
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                if (pInfo != null && pInfo.versionName != null && !pInfo.versionName.trim().isEmpty()) {
                    return pInfo.versionName.trim().replaceFirst("^v", "");
                }
            } catch (Exception ignored) {}
        }
        return id.shisen.app.BuildConfig.VERSION_NAME;
    }

    public void checkUpdate(Activity activity, boolean isManual) {
        if (activity == null || activity.isFinishing()) return;

        if (isChecking) {
            if (isManual) {
                Toast.makeText(activity, R.string.update_toast_checking, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        isChecking = true;
        if (isManual) {
            Toast.makeText(activity, R.string.update_toast_checking, Toast.LENGTH_SHORT).show();
        }

        new Thread(() -> {
            try {
                OkHttpClient client = HttpClientProvider.getClient();
                String cleanInstalled = getInstalledVersion(activity);
                boolean isTvFlavor = "tv".equalsIgnoreCase(id.shisen.app.BuildConfig.FLAVOR);
                UpdateCandidate candidate = null;
                boolean connected = false;
                String lastError = null;

                for (String endpoint : UPDATE_URLS) {
                    try {
                        Request req = new Request.Builder()
                                .url(endpoint)
                                .header("Accept", "application/vnd.github.v3+json")
                                .header("User-Agent", "Shisen-Android")
                                .build();

                        try (Response resp = client.newCall(req).execute()) {
                            if (resp.isSuccessful() && resp.body() != null) {
                                String json = resp.body().string();
                                connected = true;
                                candidate = findNewerRelease(json, cleanInstalled, isTvFlavor);
                                break;
                            } else {
                                lastError = "HTTP " + resp.code();
                            }
                        }
                    } catch (Exception err) {
                        lastError = err.getMessage();
                    }
                }

                if (!connected) {
                    onCheckFinished(activity, isManual, false, "Gagal terhubung ke GitHub (" + lastError + ")", null, null, null, 0, null);
                    return;
                }

                if (candidate != null) {
                    onCheckFinished(activity, isManual, true, null, candidate.version, candidate.notes, candidate.downloadUrl, candidate.sizeBytes, candidate.htmlUrl);
                } else {
                    onCheckFinished(activity, isManual, false, null, cleanInstalled, null, null, 0, null);
                }
            } catch (Exception e) {
                onCheckFinished(activity, isManual, false, "Koneksi terganggu: " + e.getMessage(), null, null, null, 0, null);
            }
        }).start();
    }

    public static UpdateCandidate findNewerRelease(String json, String installedVersion, boolean isTvFlavor) {
        if (json == null || json.trim().isEmpty()) return null;
        String trimmed = json.trim();
        try {
            if (trimmed.startsWith("[")) {
                JSONArray releasesArray = new JSONArray(trimmed);
                for (int i = 0; i < releasesArray.length(); i++) {
                    JSONObject rel = releasesArray.optJSONObject(i);
                    if (rel == null) continue;
                    UpdateCandidate candidate = parseCandidate(rel, installedVersion, isTvFlavor);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            } else if (trimmed.startsWith("{")) {
                JSONObject rel = new JSONObject(trimmed);
                return parseCandidate(rel, installedVersion, isTvFlavor);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static UpdateCandidate parseCandidate(JSONObject releaseObj, String installedVersion, boolean isTvFlavor) {
        if (releaseObj == null) return null;
        if (releaseObj.optBoolean("draft", false)) return null;
        String tagName = releaseObj.optString("tag_name", "");
        if (tagName.isEmpty()) return null;

        String cleanRemote = tagName.replaceFirst("^v", "");
        String cleanInstalled = installedVersion != null ? installedVersion.replaceFirst("^v", "") : "";

        if (!isNewerVersion(cleanRemote, cleanInstalled)) {
            return null;
        }

        String bodyNotes = releaseObj.optString("body", "Pembaruan kestabilan dan fitur terbaru Shisen (視線).");
        String htmlUrl = releaseObj.optString("html_url", GITHUB_RELEASES_URL);

        // Cari file APK resmi dari assets
        String downloadUrl = null;
        long apkSize = 0;
        JSONArray assets = releaseObj.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject a = assets.optJSONObject(i);
                if (a == null) continue;
                String name = a.optString("name", "");
                if (name.endsWith(".apk")) {
                    if (isTvFlavor) {
                        if (name.contains("shisen-tv-release") || name.toLowerCase().contains("tv")) {
                            downloadUrl = a.optString("browser_download_url", null);
                            apkSize = a.optLong("size", 0);
                            break;
                        }
                    } else {
                        if (name.contains("shisen-release") && !name.toLowerCase().contains("tv")) {
                            downloadUrl = a.optString("browser_download_url", null);
                            apkSize = a.optLong("size", 0);
                            break;
                        }
                    }
                }
            }
        }

        // Fallback deterministik URL rilis GitHub resmi jika asset belum sempat terindeks di API JSON
        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            if (isTvFlavor) {
                downloadUrl = "https://github.com/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/download/" + tagName + "/shisen-tv-release-" + tagName + ".apk";
            } else {
                downloadUrl = "https://github.com/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/download/" + tagName + "/shisen-release-" + tagName + ".apk";
            }
        }

        return new UpdateCandidate(tagName, cleanRemote, bodyNotes, downloadUrl, apkSize, htmlUrl);
    }

    private void onCheckFinished(Activity activity, boolean isManual, boolean hasUpdate, String errorMsg,
                                 String latestVer, String notes, String downloadUrl, long sizeBytes, String htmlUrl) {
        isChecking = false;
        mainHandler.post(() -> {
            if (activity == null || activity.isFinishing()) return;

            if (errorMsg != null) {
                if (isManual) {
                    Toast.makeText(activity, errorMsg, Toast.LENGTH_SHORT).show();
                }
                return;
            }

            if (hasUpdate) {
                showUpdateDialog(activity, latestVer, notes, downloadUrl, sizeBytes, htmlUrl);
            } else if (isManual) {
                Toast.makeText(activity, activity.getString(R.string.update_toast_latest, getInstalledVersion(activity)), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUpdateDialog(Activity activity, String latestVer, String notes, String downloadUrl, long sizeBytes, String htmlUrl) {
        if (activity == null || activity.isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app_update);
        SystemUiHelper.setupCustomDialogBounds(dialog, activity);

        TextView tvTitle = dialog.findViewById(R.id.tv_update_title);
        TextView tvVersionBadge = dialog.findViewById(R.id.tv_update_version_badge);
        TextView tvNotes = dialog.findViewById(R.id.tv_update_notes);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_update_dialog);
        MaterialButton btnLater = dialog.findViewById(R.id.btn_update_later);
        MaterialButton btnNow = dialog.findViewById(R.id.btn_update_now);
        TextView tvFallbackBrowser = dialog.findViewById(R.id.tv_update_fallback_browser);

        String curVer = getInstalledVersion(activity);
        if (tvVersionBadge != null) {
            tvVersionBadge.setText("v" + curVer + " ➔ v" + latestVer);
        }

        if (tvNotes != null) {
            tvNotes.setText(notes != null && !notes.trim().isEmpty() ? notes.trim() : "Pembaruan performa dan peningkatan fitur.");
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnLater != null) {
            btnLater.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnNow != null) {
            btnNow.setVisibility(View.VISIBLE);
            btnNow.setEnabled(true);
            btnNow.setText(R.string.update_now);
            btnNow.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                dialog.dismiss();
                String target = (downloadUrl != null && !downloadUrl.trim().isEmpty())
                        ? downloadUrl
                        : (htmlUrl != null ? htmlUrl : GITHUB_RELEASES_URL);
                openReleaseInBrowser(activity, target);
            });
        }

        if (tvFallbackBrowser != null) {
            tvFallbackBrowser.setOnClickListener(v -> {
                String target = htmlUrl != null ? htmlUrl : (downloadUrl != null ? downloadUrl : GITHUB_RELEASES_URL);
                openReleaseInBrowser(activity, target);
            });
        }

        if (id.shisen.app.ui.player.PlayerKeys.isTv()) {
            if (btnClose != null) id.shisen.app.util.TvFocusHelper.attachFocusScale(btnClose, 1.1f);
            if (btnLater != null) id.shisen.app.util.TvFocusHelper.attachFocusScale(btnLater, 1.05f);
            if (btnNow != null) {
                id.shisen.app.util.TvFocusHelper.attachFocusScale(btnNow, 1.05f);
                btnNow.post(btnNow::requestFocus);
            }
            if (tvFallbackBrowser != null) id.shisen.app.util.TvFocusHelper.attachFocusScale(tvFallbackBrowser, 1.05f);
        }

        dialog.show();
    }

    private void openReleaseInBrowser(Activity activity, String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(activity, "Buka GitHub Releases: " + GITHUB_RELEASES_URL, Toast.LENGTH_LONG).show();
        }
    }

    /** Panggil saat aplikasi start: APK pembaruan apa pun yang tersisa dibuang. */
    public static void cleanupAtStart(Context context) {
        if (context == null) return;
        try {
            File cacheDir = context.getExternalCacheDir() != null ? context.getExternalCacheDir() : context.getCacheDir();
            new Thread(() -> UpdateApkStore.deleteOldApks(cacheDir, null)).start();
        } catch (Exception ignored) {}
    }

    static boolean isNewerVersion(String remote, String installed) {
        if (remote == null || remote.isEmpty() || installed == null || installed.isEmpty()) {
            return false;
        }

        try {
            String r = remote.trim();
            String ins = installed.trim();
            if (r.startsWith("v") || r.startsWith("V")) r = r.substring(1);
            if (ins.startsWith("v") || ins.startsWith("V")) ins = ins.substring(1);

            int[] rSem = parseSemVer(r);
            int[] iSem = parseSemVer(ins);

            // Bandingkan major, minor, patch
            for (int i = 0; i < 3; i++) {
                if (rSem[i] > iSem[i]) return true;
                if (rSem[i] < iSem[i]) return false;
            }

            // Jika major, minor, patch sama:
            // Versi stabil (tanpa -beta, beta = -1) SELALU lebih baru daripada versi -beta.N
            int rBeta = rSem[3] < 0 ? Integer.MAX_VALUE : rSem[3];
            int iBeta = iSem[3] < 0 ? Integer.MAX_VALUE : iSem[3];
            return rBeta > iBeta;
        } catch (Exception ignored) {
            return !remote.equalsIgnoreCase(installed);
        }
    }

    private static int[] parseSemVer(String v) {
        int[] res = new int[]{0, 0, 0, -1};
        String[] dashParts = v.split("-", 2);
        String mainPart = dashParts[0];
        String[] dotParts = mainPart.split("\\.");
        if (dotParts.length > 0 && !dotParts[0].isEmpty()) res[0] = Integer.parseInt(dotParts[0].replaceAll("[^0-9]", ""));
        if (dotParts.length > 1 && !dotParts[1].isEmpty()) res[1] = Integer.parseInt(dotParts[1].replaceAll("[^0-9]", ""));
        if (dotParts.length > 2 && !dotParts[2].isEmpty()) res[2] = Integer.parseInt(dotParts[2].replaceAll("[^0-9]", ""));

        if (dashParts.length > 1) {
            String pre = dashParts[1].replaceAll("[^0-9]", "");
            if (!pre.isEmpty()) {
                res[3] = Integer.parseInt(pre);
            } else {
                res[3] = 0;
            }
        }
        return res;
    }
}
