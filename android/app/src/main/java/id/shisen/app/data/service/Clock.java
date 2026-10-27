package id.shisen.app.data.service;

/** Clock: interface for injectable time source (deterministic testing). */
public interface Clock {
    /** Millis since boot, suitable for elapsed-timers and cooldowns. */
    long millisNow();
}
