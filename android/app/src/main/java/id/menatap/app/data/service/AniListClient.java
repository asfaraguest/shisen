package id.menatap.app.data.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import id.menatap.app.data.model.Anime;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AniListClient {
    private static final String ANILIST_URL = "https://graphql.anilist.co";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable t);
    }

    private static final String MEDIA_FIELDS =
            "id idMal title { romaji english native } coverImage { extraLarge large } " +
            "bannerImage averageScore status format episodes duration season seasonYear " +
            "description(asHtml: false) genres studios(isMain: true) { nodes { name } }";

    public static void getTrending(int page, int perPage, Callback<List<Anime>> callback) {
        String query = "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "media(sort: TRENDING_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }";
        executePagedQuery(query, page, perPage, callback);
    }

    public static void getSeasonal(String season, int year, int page, int perPage, Callback<List<Anime>> callback) {
        if (season == null || season.isEmpty()) {
            season = getCurrentSeason();
        }
        if (year <= 0) {
            year = Calendar.getInstance().get(Calendar.YEAR);
        }
        String query = String.format(Locale.US,
                "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "media(season: %s, seasonYear: %d, sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }", season.toUpperCase(), year);
        executePagedQuery(query, page, perPage, callback);
    }

    public static void getPopular(int page, int perPage, Callback<List<Anime>> callback) {
        String query = "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "media(sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }";
        executePagedQuery(query, page, perPage, callback);
    }

    public static void getMovies(int page, int perPage, Callback<List<Anime>> callback) {
        String query = "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "media(format: MOVIE, sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }";
        executePagedQuery(query, page, perPage, callback);
    }

    public static void search(String keyword, int page, int perPage, Callback<List<Anime>> callback) {
        String query = "query ($page: Int, $perPage: Int, $search: String) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "media(search: $search, sort: SEARCH_MATCH, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }";

        try {
            JSONObject variables = new JSONObject();
            variables.put("page", page);
            variables.put("perPage", perPage);
            variables.put("search", keyword);

            JSONObject body = new JSONObject();
            body.put("query", query);
            body.put("variables", variables);

            executePost(body.toString(), callback);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    private static void executePagedQuery(String query, int page, int perPage, Callback<List<Anime>> callback) {
        try {
            JSONObject variables = new JSONObject();
            variables.put("page", page);
            variables.put("perPage", perPage);

            JSONObject body = new JSONObject();
            body.put("query", query);
            body.put("variables", variables);

            executePost(body.toString(), callback);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    private static void executePost(String jsonBody, Callback<List<Anime>> callback) {
        OkHttpClient client = HttpClientProvider.getClient();
        RequestBody reqBody = RequestBody.create(jsonBody, JSON_MEDIA);
        Request request = new Request.Builder()
                .url(ANILIST_URL)
                .post(reqBody)
                .header("User-Agent", HttpClientProvider.USER_AGENT)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new IOException("AniList HTTP error: " + response.code()));
                    return;
                }
                try {
                    String respStr = response.body().string();
                    JSONObject root = new JSONObject(respStr);
                    JSONArray mediaArr = root.getJSONObject("data").getJSONObject("Page").getJSONArray("media");
                    List<Anime> list = new ArrayList<>();
                    for (int i = 0; i < mediaArr.length(); i++) {
                        JSONObject m = mediaArr.getJSONObject(i);
                        Anime anime = parseMediaNode(m);
                        if (anime != null) list.add(anime);
                    }
                    callback.onSuccess(list);
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    public static Anime parseMediaNode(JSONObject m) {
        if (m == null) return null;
        Anime a = new Anime();
        a.id = m.optInt("id", 0);
        a.idMal = m.optInt("idMal", 0);
        
        JSONObject titleObj = m.optJSONObject("title");
        if (titleObj != null) {
            a.title = titleObj.optString("english", titleObj.optString("romaji", ""));
            if (a.title.isEmpty()) {
                a.title = titleObj.optString("romaji", titleObj.optString("native", "Anime"));
            }
            a.romajiTitle = titleObj.optString("romaji", a.title);
        } else {
            a.title = "Anime";
        }
        
        a.slug = a.title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");

        JSONObject coverObj = m.optJSONObject("coverImage");
        if (coverObj != null) {
            a.poster = coverObj.optString("extraLarge", coverObj.optString("large", ""));
        }
        a.banner = m.optString("bannerImage", "");

        int rawScore = m.optInt("averageScore", 0);
        a.score = rawScore > 0 ? (rawScore / 10.0) : 0.0;

        a.status = m.optString("status", "");
        a.format = m.optString("format", "TV");
        a.episodesCount = m.optInt("episodes", 0);
        a.epsCounter = a.episodesCount > 0 ? (a.episodesCount + " Ep") : (a.status.equalsIgnoreCase("RELEASING") ? "Ongoing" : "Anime");
        a.season = m.optString("season", "");
        a.year = m.optInt("seasonYear", 0);
        a.synopsis = m.optString("description", "");

        JSONArray gArr = m.optJSONArray("genres");
        if (gArr != null) {
            for (int i = 0; i < gArr.length(); i++) {
                a.genres.add(gArr.optString(i));
            }
        }

        JSONObject studiosObj = m.optJSONObject("studios");
        if (studiosObj != null) {
            JSONArray nodes = studiosObj.optJSONArray("nodes");
            if (nodes != null && nodes.length() > 0) {
                a.studio = nodes.optJSONObject(0).optString("name", "");
            }
        }

        return a;
    }

    public static String getCurrentSeason() {
        int month = Calendar.getInstance().get(Calendar.MONTH);
        if (month >= 0 && month <= 2) return "WINTER";
        if (month >= 3 && month <= 5) return "SPRING";
        if (month >= 6 && month <= 8) return "SUMMER";
        return "FALL";
    }
}
