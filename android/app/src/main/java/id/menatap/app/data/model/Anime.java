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

    public Anime() {}

    public static Anime fromJson(JSONObject json) {
        Anime a = new Anime();
        if (json == null) return a;
        a.id = json.optInt("id", 0);
        a.slug = json.optString("slug", json.optString("id", ""));
        a.title = json.optString("title", "");
        a.romajiTitle = json.optString("romaji_title", a.title);
        a.poster = json.optString("poster", "");
        a.banner = json.optString("banner", "");
        a.score = json.optDouble("score", 0.0);
        a.status = json.optString("status", "");
        a.format = json.optString("type", json.optString("format", "TV"));
        a.episodesCount = json.optInt("eps", json.optInt("episodes", 0));
        a.epsCounter = json.optString("eps_counter", "");
        a.season = json.optString("season", "");
        a.year = json.optInt("year", 0);
        a.synopsis = json.optString("synopsis", "");
        a.studio = json.optString("studio", "");
        
        JSONArray gArr = json.optJSONArray("genres");
        if (gArr != null) {
            for (int i = 0; i < gArr.length(); i++) {
                a.genres.add(gArr.optString(i));
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
