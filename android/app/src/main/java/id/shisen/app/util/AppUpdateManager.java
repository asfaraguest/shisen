package id.shisen.app.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

import id.shisen.app.R;
import id.shisen.app.data.service.HttpClientProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Manajer In-App Update Seamless untuk Shisen (100% Pure Native Java).
 * - Pemeriksaan rilis publik via GitHub Releases API.
 * - Mengunduh streaming APK resmi bertanda tangan dengan progress bar halus.
 * - Meluncurkan PackageInstaller resmi Android via FileProvider tanpa browser eksternal.
 */
public class AppUpdateManager {

    public static final int REQUEST_CODE_INSTALL_PERMISSION = 7129;
    private static final String[] UPDATE_URLS = {
            "https://api.github.com/repos/asfaraguest/shisen/releases/latest",
            "https://api.github.com/repos/asfaraguest/menatap/releases/latest"
    };

    private static volatile AppUpdateManager instance;

    private volatile boolean isChecking = false;
    private volatile boolean isDownloading = false;
    private volatile boolean isDownloadCancelled = false;
    private Thread activeDownloadThread = null;
    private File pendingApkFile = null;

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
        return id.shisen.app.BuildConfig.APP_VERSION_NAME;
    }

    public void checkUpdate(Activity activity, boolean isManual) {
        if (activity == null || activity.isFinishing()) return;

        if (isChecking) {
            if (isManual) {
                Toast.makeText(activity, "Pemeriksaan pembaruan sedang berjalan...", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        isChecking = true;
        if (isManual) {
            Toast.makeText(activity, "Memeriksa pembaruan rilis...", Toast.LENGTH_SHORT).show();
        }

        new Thread(() -> {
            try {
                OkHttpClient client = HttpClientProvider.getClient();
                JSONObject releaseObj = null;
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
                                releaseObj = new JSONObject(json);
                                break;
                            } else {
                                lastError = "HTTP " + resp.code();
                            }
                        }
                    } catch (Exception err) {
                        lastError = err.getMessage();
                    }
                }

                if (releaseObj == null) {
                    onCheckFinished(activity, isManual, false, "Gagal terhubung ke GitHub (" + lastError + ")", null, null, null, 0);
                    return;
                }

                String tagName = releaseObj.optString("tag_name", "");
                String bodyNotes = releaseObj.optString("body", "Pembaruan kestabilan dan fitur terbaru Shisen (視線).");
                String htmlUrl = releaseObj.optString("html_url", "https://github.com/asfaraguest/shisen/releases");

                String cleanRemote = tagName.replaceFirst("^v", "");
                String cleanInstalled = getInstalledVersion(activity);

                // Temukan asset APK resmi
                String downloadUrl = null;
                long apkSize = 0;
                JSONArray assets = releaseObj.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject a = assets.getJSONObject(i);
                        String name = a.optString("name", "");
                        if (name.endsWith(".apk")) {
                            downloadUrl = a.optString("browser_download_url", null);
                            apkSize = a.optLong("size", 0);
                            if (name.contains("shisen-release") || name.contains("menatap-release") || name.contains("miru-release")) {
                                break; // Preferred official signed asset
                            }
                        }
                    }
                }

                if (downloadUrl == null) {
                    downloadUrl = htmlUrl;
                }

                boolean hasUpdate = isNewerVersion(cleanRemote, cleanInstalled);
                onCheckFinished(activity, isManual, hasUpdate, null, cleanRemote, bodyNotes, downloadUrl, apkSize);
            } catch (Exception e) {
                onCheckFinished(activity, isManual, false, "Koneksi terganggu: " + e.getMessage(), null, null, null, 0);
            }
        }).start();
    }

    private void onCheckFinished(Activity activity, boolean isManual, boolean hasUpdate, String errorMsg,
                                 String latestVer, String notes, String downloadUrl, long sizeBytes) {
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
                showUpdateDialog(activity, latestVer, notes, downloadUrl, sizeBytes);
            } else if (isManual) {
                Toast.makeText(activity, "Shisen sudah dalam versi terbaru (v" + getInstalledVersion(activity) + ")", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUpdateDialog(Activity activity, String latestVer, String notes, String downloadUrl, long sizeBytes) {
        if (activity == null || activity.isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app_update);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

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
            btnNow.setOnClickListener(v -> {
                HapticHelper.performClick(v);
                dialog.dismiss();
                startStreamingDownload(activity, latestVer, downloadUrl);
            });
        }

        if (tvFallbackBrowser != null) {
            tvFallbackBrowser.setOnClickListener(v -> {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                    activity.startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(activity, "Tidak ada browser yang tersedia", Toast.LENGTH_SHORT).show();
                }
            });
        }

        dialog.show();
    }

    private void startStreamingDownload(Activity activity, String latestVer, String downloadUrl) {
        if (activity == null || activity.isFinishing()) return;

        if (isDownloading) {
            Toast.makeText(activity, "Pengunduhan sedang berlangsung...", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog downloadDialog = new Dialog(activity);
        downloadDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        downloadDialog.setContentView(R.layout.dialog_app_download);
        downloadDialog.setCancelable(false);

        if (downloadDialog.getWindow() != null) {
            downloadDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90f);
            downloadDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTarget = downloadDialog.findViewById(R.id.tv_download_target_version);
        ProgressBar pbHorizontal = downloadDialog.findViewById(R.id.pb_download_horizontal);
        TextView tvStatus = downloadDialog.findViewById(R.id.tv_download_status);
        TextView tvPercent = downloadDialog.findViewById(R.id.tv_download_percent);
        MaterialButton btnCancel = downloadDialog.findViewById(R.id.btn_cancel_download);

        if (tvTarget != null) {
            tvTarget.setText("Versi Target: v" + latestVer);
        }
        if (pbHorizontal != null) {
            pbHorizontal.setMax(100);
            pbHorizontal.setProgress(0);
        }
        if (tvStatus != null) {
            tvStatus.setText("Menghubungkan ke GitHub...");
        }

        isDownloading = true;
        isDownloadCancelled = false;

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                isDownloadCancelled = true;
                if (activeDownloadThread != null) {
                    activeDownloadThread.interrupt();
                }
                downloadDialog.dismiss();
                isDownloading = false;
                Toast.makeText(activity, "Pengunduhan dibatalkan", Toast.LENGTH_SHORT).show();
            });
        }

        downloadDialog.show();

        activeDownloadThread = new Thread(() -> {
            File outFile = null;
            try {
                OkHttpClient client = HttpClientProvider.getClient();
                Request req = new Request.Builder()
                        .url(downloadUrl)
                        .header("User-Agent", "Shisen-Android")
                        .build();

                try (Response resp = client.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        throw new Exception("HTTP " + resp.code());
                    }

                    long totalBytes = resp.body().contentLength();
                    File cacheDir = activity.getExternalCacheDir() != null ? activity.getExternalCacheDir() : activity.getCacheDir();
                    // Simpan di subfolder khusus updates/ (satu-satunya area yang dibagikan FileProvider)
                    outFile = UpdateApkStore.apkFile(cacheDir, latestVer);
                    if (outFile.exists()) {
                        outFile.delete();
                    }

                    try (InputStream in = resp.body().byteStream();
                         FileOutputStream fos = new FileOutputStream(outFile)) {

                        byte[] buffer = new byte[16384];
                        long downloadedBytes = 0;
                        int bytesRead;
                        long lastUpdateMs = 0;

                        while ((bytesRead = in.read(buffer)) != -1) {
                            if (isDownloadCancelled) {
                                if (outFile.exists()) outFile.delete();
                                return;
                            }

                            fos.write(buffer, 0, bytesRead);
                            downloadedBytes += bytesRead;

                            long now = System.currentTimeMillis();
                            if (now - lastUpdateMs > 120 || downloadedBytes == totalBytes) {
                                lastUpdateMs = now;
                                final long cur = downloadedBytes;
                                final long tot = totalBytes;
                                mainHandler.post(() -> {
                                    if (downloadDialog.isShowing()) {
                                        if (tot > 0) {
                                            int pct = (int) ((cur * 100) / tot);
                                            if (pbHorizontal != null) pbHorizontal.setProgress(pct);
                                            if (tvPercent != null) tvPercent.setText(pct + "%");
                                            double curMB = cur / (1024.0 * 1024.0);
                                            double totMB = tot / (1024.0 * 1024.0);
                                            if (tvStatus != null) {
                                                tvStatus.setText(String.format(Locale.US, "%.1f MB / %.1f MB", curMB, totMB));
                                            }
                                        } else {
                                            if (pbHorizontal != null) pbHorizontal.setIndeterminate(true);
                                            double curMB = cur / (1024.0 * 1024.0);
                                            if (tvStatus != null) {
                                                tvStatus.setText(String.format(Locale.US, "Mengunduh %.1f MB...", curMB));
                                            }
                                        }
                                    }
                                });
                            }
                        }
                        fos.flush();
                    }

                    final File finalApk = outFile;
                    mainHandler.post(() -> {
                        isDownloading = false;
                        if (downloadDialog.isShowing()) {
                            downloadDialog.dismiss();
                        }
                        installApk(activity, finalApk);
                    });
                }
            } catch (Exception e) {
                if (outFile != null && outFile.exists()) {
                    outFile.delete();
                }
                mainHandler.post(() -> {
                    isDownloading = false;
                    if (downloadDialog.isShowing()) {
                        downloadDialog.dismiss();
                    }
                    if (!isDownloadCancelled) {
                        Toast.makeText(activity, "Gagal mengunduh APK: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        activeDownloadThread.start();
    }

    public void installApk(Activity activity, File apkFile) {
        if (activity == null || activity.isFinishing() || apkFile == null || !apkFile.exists()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                this.pendingApkFile = apkFile;
                new AlertDialog.Builder(activity)
                        .setTitle("Izin Pemasangan Aplikasi")
                        .setMessage("Shisen memerlukan izin untuk memasang pembaruan langsung dari berkas APK resmi bertanda tangan. Silakan aktifkan 'Izinkan dari sumber ini' pada layar pengaturan.")
                        .setPositiveButton("Buka Pengaturan", (d, w) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                                activity.startActivityForResult(intent, REQUEST_CODE_INSTALL_PERMISSION);
                            } catch (Exception e) {
                                try {
                                    Intent fallback = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                                    activity.startActivity(fallback);
                                } catch (Exception ignored) {}
                            }
                        })
                        .setNegativeButton("Batal", null)
                        .show();
                return;
            }
        }

        try {
            Uri apkUri = FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".fileprovider",
                    apkFile
            );

            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(installIntent);

            // Setelah instalasi dimulai, bersihkan APK pembaruan lain (APK yang dipasang dihapus oleh
            // penginstal/system, atau saat aplikasi berikutnya start)
            try {
                File cacheDir = activity.getExternalCacheDir() != null ? activity.getExternalCacheDir() : activity.getCacheDir();
                UpdateApkStore.deleteOldApks(cacheDir, apkFile);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            Toast.makeText(activity, "Gagal meluncurkan penginstal paket: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    public void onInstallPermissionResult(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (activity.getPackageManager().canRequestPackageInstalls()) {
                if (pendingApkFile != null && pendingApkFile.exists()) {
                    File toInstall = pendingApkFile;
                    pendingApkFile = null;
                    installApk(activity, toInstall);
                }
            } else {
                Toast.makeText(activity, "Izin pemasangan aplikasi tidak diberikan", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static boolean isNewerVersion(String remote, String installed) {
        if (remote == null || remote.isEmpty() || installed == null || installed.isEmpty()) {
            return false;
        }

        try {
            // Bersihkan format (misal 0.1.0-beta.2 -> 0.1.0.2 atau bandingkan segment)
            String[] rParts = remote.replaceAll("[^0-9.]", ".").replaceAll("\\.+", ".").split("\\.");
            String[] iParts = installed.replaceAll("[^0-9.]", ".").replaceAll("\\.+", ".").split("\\.");

            int maxLen = Math.max(rParts.length, iParts.length);
            for (int i = 0; i < maxLen; i++) {
                int rVal = i < rParts.length && !rParts[i].isEmpty() ? Integer.parseInt(rParts[i]) : 0;
                int iVal = i < iParts.length && !iParts[i].isEmpty() ? Integer.parseInt(iParts[i]) : 0;
                if (rVal > iVal) return true;
                if (rVal < iVal) return false;
            }
        } catch (Exception ignored) {
            return !remote.equalsIgnoreCase(installed);
        }
        return false;
    }
}
