package id.menatap.app.data.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Anime implements Serializable {
    public int id;
    public int idMal = 0;
    public String slug = "";
    public String title = "";
    public String romajiTitle = "";
    public String poster = "";
    public String banner = "";
    public double score = 0.0;
    public String status = "";
    public String format = "TV";
    public int episodesCount = 0;
    public int airedEpisodesCount = 0;
    public int nextAiringEpisode = 0;
    public int nextAiringTime = 0;
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
    public String headerTitle = "";
    public String headerSubtitle = "";
    public boolean isLongRunning = false;

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
        a.epsCounter = safeStr(json, "eps_counter", "");
        a.season = safeStr(json, "season", "");
        a.year = json.optInt("year", 0);
        a.synopsis = safeStr(json, "synopsis", "");
        a.studio = safeStr(json, "studio", "");
        
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
            json.put("eps_counter", epsCounter);
            json.put("season", season);
            json.put("year", year);
            json.put("synopsis", synopsis);
            json.put("studio", studio);
            JSONArray gArr = new JSONArray();
            for (String g : genres) {
                gArr.put(g);
            }
            json.put("genres", gArr);
        } catch (Exception ignored) {}
        return json;
    }
}
