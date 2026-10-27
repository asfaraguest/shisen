package id.shisen.app.data.service;

import android.os.SystemClock;

/** RealClock: production implementation using system elapsed time. */
public final class RealClock implements Clock {
    public RealClock() { }

    @Override
    public long millisNow() {
        return SystemClock.elapsedRealtime();
    }
}
