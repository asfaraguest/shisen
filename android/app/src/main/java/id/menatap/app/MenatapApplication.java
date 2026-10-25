package id.menatap.app;

import android.app.Application;

import id.menatap.app.data.db.MenatapDatabase;
import id.menatap.app.util.ImageLoader;

public class MenatapApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize local database
        MenatapDatabase.getInstance(this);
        // Initialize disk and memory image cache
        ImageLoader.init(this);
    }
}
