package id.shisen.app.ui.detail;

import id.shisen.app.data.model.Anime;

/**
 * DetailDataHolder holds an in-memory reference to the active Anime shown in DetailActivity.
 * Ensures that if the activity is recreated after staying idle or screen-off, the view
 * does not finish abruptly due to null intent extras.
 */
public final class DetailDataHolder {

    private static volatile Anime activeAnime;

    private DetailDataHolder() {}

    public static synchronized void setAnime(Anime anime) {
        activeAnime = anime;
    }

    public static synchronized Anime getAnime() {
        return activeAnime;
    }

    public static synchronized void clear() {
        activeAnime = null;
    }
}
