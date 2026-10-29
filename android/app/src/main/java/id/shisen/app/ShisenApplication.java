package id.shisen.app;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.util.Log;

import id.shisen.app.data.db.MenatapDatabase;
import id.shisen.app.util.ImageLoader;
import id.shisen.app.util.LocaleHelper;

public class ShisenApplication extends Application {

    private static final String TAG = "ShisenApp";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        id.shisen.app.util.PerfLogger.onAppStart();

        setupGlobalExceptionHandler();

        // Initialize local database (mempertahankan menatap.db untuk zero data loss)
        MenatapDatabase.getInstance(this);

        // Initialize disk and memory image cache
        ImageLoader.init(this);

        // Initialize Feed & Episode Cache
        id.shisen.app.data.cache.FeedCache.getInstance(this);

        // Aktifkan cache disk HTTP 50MB untuk GET metadata
        id.shisen.app.data.service.HttpClientProvider.configureCache(
                new java.io.File(getCacheDir(), "http_cache"));
    }

    private void setupGlobalExceptionHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception on thread: " + (thread != null ? thread.getName() : "null"), throwable);
            logExceptionToFile(thread, throwable);

            // Cegah penghentian proses UI activity jika crash berasal dari background thread
            if (Looper.myLooper() != Looper.getMainLooper()) {
                Log.w(TAG, "Background exception dicegah menghentikan proses: " + (throwable != null ? throwable.getMessage() : "null"));
                return;
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }

    private void logExceptionToFile(Thread thread, Throwable throwable) {
        try {
            java.io.File logFile = new java.io.File(getCacheDir(), "crash_reports.log");
            if (logFile.exists() && logFile.length() > 512 * 1024) {
                logFile.delete();
            }
            try (java.io.FileWriter fw = new java.io.FileWriter(logFile, true);
                 java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
                pw.println("--- CRASH REPORT ---");
                pw.println("Time: " + new java.util.Date());
                pw.println("Thread: " + (thread != null ? thread.getName() : "null") + " (ID: " + (thread != null ? thread.getId() : -1) + ")");
                if (throwable != null) {
                    throwable.printStackTrace(pw);
                }
                pw.println();
                pw.flush();
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        ImageLoader.trimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.clearMemoryCache();
    }
}
