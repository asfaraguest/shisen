package id.shisen.app;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.util.Log;

import id.shisen.app.data.db.MenatapDatabase;
import id.shisen.app.util.ImageLoader;
import id.shisen.app.util.LocaleHelper;

public class MenatapApplication extends Application {

    private static final String TAG = "MenatapApp";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setupGlobalExceptionHandler();

        // Initialize local database
        MenatapDatabase.getInstance(this);

        // Initialize disk and memory image cache
        ImageLoader.init(this);
    }

    private void setupGlobalExceptionHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception on thread: " + thread.getName(), throwable);

            // If crash occurred on a background thread, log and swallow to prevent killing UI activity
            if (Looper.myLooper() != Looper.getMainLooper()) {
                Log.w(TAG, "Background exception caught and prevented process termination: " + throwable.getMessage());
                return;
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
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
