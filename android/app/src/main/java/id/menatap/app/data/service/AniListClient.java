package id.menatap.app.data.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
            "description(asHtml: false) genres studios(isMain: true) { nodes { name } } " +
            "nextAiringEpisode { episode timeUntilAiring }";

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

    public static void getAiring(int page, int perPage, Callback<List<Anime>> callback) {
        String query = "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "media(status: RELEASING, sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }";
        executePagedQuery(query, page, perPage, callback);
    }

    public static void getPopular(int page, int perPage, Callback<List<Anime>> callback) {
        String query = "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "media(sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }";
        executePagedQuery(query, page, perPage, callback);
    }

    public static void getCatalog(String format, String sort, int page, int perPage, Callback<List<Anime>> callback) {
        if (sort == null || sort.isEmpty()) sort = "POPULARITY_DESC";
        String formatFilter = (format != null && !format.isEmpty() && !"ALL".equalsIgnoreCase(format))
                ? String.format(Locale.US, ", format: %s", format.toUpperCase())
                : "";

        String query = String.format(Locale.US,
                "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "media(sort: %s, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]%s) { " +
                MEDIA_FIELDS + " } } }", sort, formatFilter);
        executePagedQuery(query, page, perPage, callback);
    }

    public static void getByGenre(String genre, int page, int perPage, Callback<List<Anime>> callback) {
        String query = "query ($page: Int, $perPage: Int, $genre: String) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "media(genre: $genre, sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }";

        try {
            JSONObject variables = new JSONObject();
            variables.put("page", page);
            variables.put("perPage", perPage);
            variables.put("genre", genre);

            JSONObject body = new JSONObject();
            body.put("query", query);
            body.put("variables", variables);

            executePost(body.toString(), callback);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public static void getSchedule(int targetDayOfWeek, Callback<List<Anime>> callback) {
        // targetDayOfWeek: Calendar.MONDAY .. Calendar.SUNDAY
        new Thread(() -> {
            try {
                TimeZone wibTz = TimeZone.getTimeZone("Asia/Jakarta");
                Calendar cal = Calendar.getInstance(wibTz);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                int currentDay = cal.get(Calendar.DAY_OF_WEEK);
                int dayDiff = targetDayOfWeek - currentDay;
                cal.add(Calendar.DAY_OF_YEAR, dayDiff);

                long startEpoch = cal.getTimeInMillis() / 1000;
                long endEpoch = startEpoch + 86400 - 1;

                String query = String.format(Locale.US,
                        "query { Page(page: 1, perPage: 50) { " +
                        "airingSchedules(airingAt_greater: %d, airingAt_lesser: %d) { " +
                        "episode airingAt media { %s } } } }",
                        startEpoch, endEpoch, MEDIA_FIELDS);

                JSONObject body = new JSONObject();
                body.put("query", query);

                executePostCustom(body.toString(), new Callback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject root) {
                        try {
                            JSONArray arr = root.getJSONObject("data").getJSONObject("Page").getJSONArray("airingSchedules");
                            List<Anime> list = new ArrayList<>();
                            List<Integer> seenIds = new ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject item = arr.getJSONObject(i);
                                JSONObject media = item.optJSONObject("media");
                                if (media != null) {
                                    Anime a = parseMediaNode(media);
                                    if (a != null && !seenIds.contains(a.id)) {
                                        seenIds.add(a.id);
                                        int ep = item.optInt("episode", 0);
                                        if (ep > 0) a.epsCounter = "Ep " + ep;
                                        long airingAt = item.optLong("airingAt", 0);
                                        if (airingAt > 0) {
                                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm 'WIB'", Locale.US);
                                            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
                                            a.airingDay = sdf.format(new java.util.Date(airingAt * 1000L));
                                        }
                                        list.add(a);
                                    }
                                }
                            }
                            callback.onSuccess(list);
                        } catch (Exception e) {
                            callback.onError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        callback.onError(t);
                    }
                });
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
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
        executePostCustom(jsonBody, new Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject root) {
                try {
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

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    private static void executePostCustom(String jsonBody, Callback<JSONObject> callback) {
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
                    callback.onSuccess(root);
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    public static String optSafeString(JSONObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key) || obj.isNull(key)) {
            return fallback;
        }
        String val = obj.optString(key, fallback);
        if (val == null || "null".equalsIgnoreCase(val.trim())) {
            return fallback;
        }
        return val.trim();
    }

    public static String optSafeString(JSONArray arr, int index, String fallback) {
        if (arr == null || arr.isNull(index)) {
            return fallback;
        }
        String val = arr.optString(index, fallback);
        if (val == null || "null".equalsIgnoreCase(val.trim())) {
            return fallback;
        }
        return val.trim();
    }

    public static Anime parseMediaNode(JSONObject m) {
        if (m == null || m.optBoolean("isAdult", false)) return null;
        Anime a = new Anime();
        a.id = m.optInt("id", 0);
        a.idMal = m.optInt("idMal", 0);
        
        JSONObject titleObj = m.optJSONObject("title");
        if (titleObj != null) {
            String eng = optSafeString(titleObj, "english", "");
            String rom = optSafeString(titleObj, "romaji", "");
            String nat = optSafeString(titleObj, "native", "");

            if (!eng.isEmpty()) {
                a.title = eng;
            } else if (!rom.isEmpty()) {
                a.title = rom;
            } else if (!nat.isEmpty()) {
                a.title = nat;
            } else {
                a.title = "Anime";
            }
            a.romajiTitle = !rom.isEmpty() ? rom : a.title;
        } else {
            a.title = "Anime";
            a.romajiTitle = "Anime";
        }
        
        a.slug = a.title.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (a.slug.isEmpty()) {
            a.slug = "anime-" + a.id;
        }

        JSONObject coverObj = m.optJSONObject("coverImage");
        if (coverObj != null) {
            a.poster = optSafeString(coverObj, "extraLarge", optSafeString(coverObj, "large", ""));
        }
        a.banner = optSafeString(m, "bannerImage", "");

        int rawScore = m.optInt("averageScore", 0);
        a.score = rawScore > 0 ? (rawScore / 10.0) : 0.0;

        a.status = optSafeString(m, "status", "");
        a.format = optSafeString(m, "format", "TV");
        a.episodesCount = m.optInt("episodes", 0);
        a.season = optSafeString(m, "season", "");
        a.year = m.optInt("seasonYear", 0);
        a.synopsis = optSafeString(m, "description", "");

        // Next Airing Episode handling
        JSONObject nextEpObj = m.optJSONObject("nextAiringEpisode");
        if (nextEpObj != null) {
            a.nextAiringEpisode = nextEpObj.optInt("episode", 0);
            a.nextAiringTime = nextEpObj.optInt("timeUntilAiring", 0);
            if (a.nextAiringEpisode > 1) {
                a.airedEpisodesCount = a.nextAiringEpisode - 1;
            }
        }

        boolean isOngoing = "RELEASING".equalsIgnoreCase(a.status) || "AIRING".equalsIgnoreCase(a.status);
        if (a.airedEpisodesCount > 0) {
            if (a.episodesCount > 0) {
                a.epsCounter = a.airedEpisodesCount + "/" + a.episodesCount + " Ep";
            } else {
                a.epsCounter = a.airedEpisodesCount + " Ep";
            }
        } else if (isOngoing) {
            a.epsCounter = a.episodesCount > 0 ? ("1/" + a.episodesCount + " Ep") : "Ongoing";
        } else if (a.episodesCount > 0) {
            a.epsCounter = a.episodesCount + " Ep";
        } else {
            a.epsCounter = a.format;
        }

        JSONArray gArr = m.optJSONArray("genres");
        if (gArr != null) {
            for (int i = 0; i < gArr.length(); i++) {
                String g = optSafeString(gArr, i, "");
                if ("hentai".equalsIgnoreCase(g)) return null;
                if (!g.isEmpty()) {
                    a.genres.add(g);
                }
            }
        }

        JSONObject studiosObj = m.optJSONObject("studios");
        if (studiosObj != null) {
            JSONArray nodes = studiosObj.optJSONArray("nodes");
            if (nodes != null && nodes.length() > 0) {
                JSONObject studioNode = nodes.optJSONObject(0);
                if (studioNode != null) {
                    a.studio = optSafeString(studioNode, "name", "");
                }
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
