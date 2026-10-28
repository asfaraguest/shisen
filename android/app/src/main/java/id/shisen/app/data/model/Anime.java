package id.shisen.app.data.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Anime implements Serializable {
    public int id;
    public int idMal = 0;
    public int anilistId = 0;
    public String slug = "";
    public String title = "";
    public String romajiTitle = "";
    public String nativeTitle = "";
    public String poster = "";
    public String banner = "";
    public double score = 0.0;
    public String status = "";
    public String format = "TV";
    public int episodesCount = 0;
    public int airedEpisodesCount = 0;
    public int nextAiringEpisode = 0;
    public int nextAiringTime = 0;
    public long airingAt = 0L; // UNIX epoch timestamp dalam detik rilis episode berikutnya
    public String airingDay = "";
    public String epsCounter = "";
    public String season = "";
    public int year = 0;
    public String synopsis = "";
    public List<String> genres = new ArrayList<>();
    public String studio = "";
    public boolean isFavorite = false;
    public int lastPlayedEp = 0;
    public long lastPlayedProgressMs = 0;
    public String lastPlayedAt = "";

    // Dual-Lane Header & Long-Running support
    public boolean isHeader = false;
    public String headerType = "";
    public String headerTitle = "";
    public String headerSubtitle = "";
    public boolean isLongRunning = false;

    // Franchise Relations support (Seasons & Movies)
    public String relationType = "";
    public String relationLabel = "";

    public Anime() {}

    public static Anime createHeader(String title, String subtitle) {
        Anime a = new Anime();
        a.isHeader = true;
        a.headerTitle = title;
        a.headerSubtitle = subtitle;
        a.title = title;
        a.slug = "header_" + (title != null ? title.hashCode() : 0);
        return a;
    }

    public static Anime createLaneHeader(String type, String season, int year, String fallbackTitle, String fallbackTag) {
        Anime a = new Anime();
        a.isHeader = true;
        a.headerType = type != null ? type : "";
        a.season = season != null ? season : "";
        a.year = year;
        a.headerTitle = fallbackTitle != null ? fallbackTitle : "";
        a.headerSubtitle = fallbackTag != null ? fallbackTag : "";
        a.title = a.headerTitle;
        a.slug = "header_" + a.headerType + "_" + (a.headerTitle.hashCode());
        return a;
    }

    private static String safeStr(JSONObject json, String key, String fallback) {
        if (json == null || !json.has(key) || json.isNull(key)) return fallback;
        String s = json.optString(key, fallback);
        if (s == null || "null".equalsIgnoreCase(s.trim())) return fallback;
        return s.trim();
    }

    public static Anime fromJson(JSONObject json) {
        Anime a = new Anime();
        if (json == null) return a;
        a.id = json.optInt("id", 0);
        a.slug = safeStr(json, "slug", safeStr(json, "id", ""));
        a.title = safeStr(json, "title", "Anime");
        a.romajiTitle = safeStr(json, "romaji_title", a.title);
        a.poster = safeStr(json, "poster", "");
        a.banner = safeStr(json, "banner", "");
        a.score = json.optDouble("score", 0.0);
        a.status = safeStr(json, "status", "");
        a.format = safeStr(json, "type", safeStr(json, "format", "TV"));
        a.episodesCount = json.optInt("eps", json.optInt("episodes", 0));
        a.airedEpisodesCount = json.optInt("aired_eps", 0);
        a.nextAiringEpisode = json.optInt("next_ep", 0);
        a.nextAiringTime = json.optInt("time_until_airing", 0);
        a.airingAt = json.optLong("airing_at", 0L);
        a.airingDay = safeStr(json, "airing_day", "");
        a.epsCounter = safeStr(json, "eps_counter", "");
        a.season = safeStr(json, "season", "");
        a.year = json.optInt("year", 0);
        a.synopsis = safeStr(json, "synopsis", "");
        a.studio = safeStr(json, "studio", "");
        a.relationType = safeStr(json, "relation_type", "");
        a.relationLabel = safeStr(json, "relation_label", "");
        a.isHeader = json.optBoolean("is_header", false);
        a.headerType = safeStr(json, "header_type", "");
        a.headerTitle = safeStr(json, "header_title", "");
        a.headerSubtitle = safeStr(json, "header_subtitle", "");
        a.isLongRunning = json.optBoolean("is_long_running", false);
        
        JSONArray gArr = json.optJSONArray("genres");
        if (gArr != null) {
            for (int i = 0; i < gArr.length(); i++) {
                String g = gArr.optString(i, "");
                if (g != null && !g.isEmpty() && !"null".equalsIgnoreCase(g)) {
                    a.genres.add(g.trim());
                }
            }
        }
        return a;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("slug", slug);
            json.put("title", title);
            json.put("romaji_title", romajiTitle);
            json.put("poster", poster);
            json.put("banner", banner);
            json.put("score", score);
            json.put("status", status);
            json.put("type", format);
            json.put("eps", episodesCount);
            json.put("aired_eps", airedEpisodesCount);
            json.put("next_ep", nextAiringEpisode);
            json.put("time_until_airing", nextAiringTime);
            json.put("airing_at", airingAt);
            json.put("airing_day", airingDay);
            json.put("eps_counter", epsCounter);
            json.put("season", season);
            json.put("year", year);
            json.put("synopsis", synopsis);
            json.put("studio", studio);
            json.put("relation_type", relationType);
            json.put("relation_label", relationLabel);
            json.put("is_header", isHeader);
            json.put("header_type", headerType);
            json.put("header_title", headerTitle);
            json.put("header_subtitle", headerSubtitle);
            json.put("is_long_running", isLongRunning);
            JSONArray gArr = new JSONArray();
            for (String g : genres) {
                gArr.put(g);
            }
            json.put("genres", gArr);
        } catch (Exception ignored) {}
        return json;
    }

    /**
     * Membandingkan apakah dua objek Anime memiliki representasi visual yang sama persis
     * untuk mencegah rebind/flicker yang tidak perlu di RecyclerView Adapter.
     */
    public boolean isVisuallyEquivalent(Anime o) {
        if (o == null) return false;
        if (this == o) return true;
        return this.id == o.id &&
                Double.compare(this.score, o.score) == 0 &&
                this.airedEpisodesCount == o.airedEpisodesCount &&
                this.nextAiringEpisode == o.nextAiringEpisode &&
                this.isHeader == o.isHeader &&
                this.isLongRunning == o.isLongRunning &&
                java.util.Objects.equals(this.slug, o.slug) &&
                java.util.Objects.equals(this.title, o.title) &&
                java.util.Objects.equals(this.poster, o.poster) &&
                java.util.Objects.equals(this.status, o.status) &&
                java.util.Objects.equals(this.epsCounter, o.epsCounter) &&
                java.util.Objects.equals(this.airingDay, o.airingDay) &&
                java.util.Objects.equals(this.headerTitle, o.headerTitle) &&
                java.util.Objects.equals(this.headerSubtitle, o.headerSubtitle);
    }
}
