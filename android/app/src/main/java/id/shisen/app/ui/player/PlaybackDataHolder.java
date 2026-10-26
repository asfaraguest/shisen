package id.shisen.app.ui.player;

import java.util.ArrayList;
import java.util.List;

import id.shisen.app.data.model.Anime;
import id.shisen.app.data.model.Episode;

/**
 * PlaybackDataHolder holds an in-memory reference to the current anime and episodes
 * being played in PlayerActivity. This prevents unexpected finishes caused by
 * TransactionTooLargeException or OS clearing Intent extras when the device stays idle.
 */
public final class PlaybackDataHolder {

    private static volatile Anime activeAnime;
    private static final List<Episode> activeEpisodes = new ArrayList<>();
    private static volatile int activeIndex = 0;
    private static volatile long activePositionMs = 0;

    private PlaybackDataHolder() {}

    public static synchronized void set(Anime anime, List<Episode> episodes, int index) {
        activeAnime = anime;
        activeEpisodes.clear();
        if (episodes != null) {
            activeEpisodes.addAll(episodes);
        }
        activeIndex = Math.max(0, index);
        activePositionMs = 0;
    }

    public static synchronized Anime getAnime() {
        return activeAnime;
    }

    public static synchronized List<Episode> getEpisodes() {
        return new ArrayList<>(activeEpisodes);
    }

    public static synchronized int getIndex() {
        return activeIndex;
    }

    public static synchronized void setIndex(int index) {
        activeIndex = Math.max(0, index);
    }

    public static synchronized long getPositionMs() {
        return activePositionMs;
    }

    public static synchronized void setPositionMs(long posMs) {
        activePositionMs = posMs;
    }

    public static synchronized boolean hasData() {
        return activeAnime != null && !activeEpisodes.isEmpty();
    }

    public static synchronized void clear() {
        activeAnime = null;
        activeEpisodes.clear();
        activeIndex = 0;
        activePositionMs = 0;
    }
}
