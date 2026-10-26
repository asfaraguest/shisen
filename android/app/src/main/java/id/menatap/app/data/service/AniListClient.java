package id.menatap.app.data.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public interface PagedCallback<T> {
        void onSuccess(T result, boolean hasNextPage, int total);
        void onError(Throwable t);
    }

    private static final String MEDIA_FIELDS =
            "id idMal title { romaji english native } coverImage { extraLarge large } " +
            "bannerImage averageScore status format episodes duration season seasonYear " +
            "description(asHtml: false) genres studios(isMain: true) { nodes { name } } " +
            "nextAiringEpisode { episode timeUntilAiring }";

    public static class SeasonPair {
        public final String season; // "WINTER", "SPRING", "SUMMER", "FALL"
        public final int year;
        public SeasonPair(String season, int year) {
            this.season = season;
            this.year = year;
        }
    }

    public static class RelationsResult {
        public final List<Anime> seasons = new ArrayList<>();
        public final List<Anime> movies = new ArrayList<>();
    }

    public static SeasonPair getCurrentSeasonPair() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH); // 0-based: 0=Jan..11=Dec
        int year = cal.get(Calendar.YEAR);
        String season;
        if (month >= 0 && month <= 2) season = "WINTER";
        else if (month >= 3 && month <= 5) season = "SPRING";
        else if (month >= 6 && month <= 8) season = "SUMMER";
        else season = "FALL";
        return new SeasonPair(season, year);
    }

    public static SeasonPair getPreviousSeasonPair(SeasonPair cur) {
        String[] order = {"WINTER", "SPRING", "SUMMER", "FALL"};
        int idx = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i].equalsIgnoreCase(cur.season)) {
                idx = i;
                break;
            }
        }
        if (idx == 0) {
            return new SeasonPair("FALL", cur.year - 1);
        } else {
            return new SeasonPair(order[idx - 1], cur.year);
        }
    }

    public static boolean isLongRunning(Anime anime) {
        if (anime == null) return false;
        // If an anime has a defined seasonal episode count (1-30 eps), it is a seasonal anime, not infinite
        if (anime.episodesCount > 0 && anime.episodesCount <= 30) {
            return false;
        }
        if (anime.episodesCount > 30 || anime.airedEpisodesCount > 30) {
            return true;
        }
        String t = (anime.title != null ? anime.title : "").toLowerCase(Locale.US);
        String r = (anime.romajiTitle != null ? anime.romajiTitle : "").toLowerCase(Locale.US);
        String s = (anime.slug != null ? anime.slug : "").toLowerCase(Locale.US);

        String[] known = {
            "one-piece", "one piece", "detective conan", "case closed", "pokemon",
            "crayon shin-chan", "shin-chan", "doraemon", "chibi maruko-chan", "chibi-maruko",
            "boruto", "sazae-san", "sazae", "yu-gi-oh"
        };
        for (String k : known) {
            if (t.contains(k) || r.contains(k) || s.contains(k)) {
                return true;
            }
        }
        if (anime.episodesCount == 0 && anime.year > 0 && anime.year < Calendar.getInstance().get(Calendar.YEAR) - 1) {
            return true;
        }
        return false;
    }

    /**
     * Beranda: Anime yang tayang pada musim saat ini (Current Season Airing).
     * Mengecualikan serial panjang berkelanjutan dan anime dari musim sebelumnya/mendatang agar Beranda murni anime musiman aktif.
     */
    public static void getCurrentSeasonAiring(int page, int perPage, PagedCallback<List<Anime>> callback) {
        SeasonPair cur = getCurrentSeasonPair();
        String query = String.format(Locale.US,
                "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "pageInfo { hasNextPage total currentPage } " +
                "media(season: %s, seasonYear: %d, sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }", cur.season, cur.year);

        executePagedQuery(query, page, perPage, new PagedCallback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result, boolean hasNextPage, int total) {
                List<Anime> seasonalOnly = new ArrayList<>();
                if (result != null) {
                    for (Anime a : result) {
                        if (a == null) continue;
                        // Strict season isolation: must strictly belong to current season & year
                        boolean isStrictCurrentSeason = cur.season.equalsIgnoreCase(a.season) && cur.year == a.year;
                        if (isStrictCurrentSeason && !isLongRunning(a)) {
                            seasonalOnly.add(a);
                        }
                    }
                }
                callback.onSuccess(seasonalOnly, hasNextPage, total);
            }

            @Override
            public void onError(Throwable t) {
                // Fallback ke seasonal umum jika kueri spesifik gagal
                getSeasonal(cur.season, cur.year, page, perPage, new Callback<List<Anime>>() {
                    @Override
                    public void onSuccess(List<Anime> fallbackResult) {
                        List<Anime> seasonalOnly = new ArrayList<>();
                        if (fallbackResult != null) {
                            for (Anime a : fallbackResult) {
                                if (a == null) continue;
                                boolean isStrictCurrentSeason = cur.season.equalsIgnoreCase(a.season) && cur.year == a.year;
                                if (isStrictCurrentSeason && !isLongRunning(a)) {
                                    seasonalOnly.add(a);
                                }
                            }
                        }
                        callback.onSuccess(seasonalOnly, false, seasonalOnly.size());
                    }

                    @Override
                    public void onError(Throwable fallbackError) {
                        callback.onError(fallbackError);
                    }
                });
            }
        });
    }

    public static void getCurrentSeasonAiring(int page, int perPage, Callback<List<Anime>> callback) {
        getCurrentSeasonAiring(page, perPage, new PagedCallback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result, boolean hasNextPage, int total) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    /**
     * Masih Tayang (Airing): List anime sedang tayang (releasing) kontinu yang dipisahkan
     * secara terstruktur menjadi 3 jalur yang saling terisolasi:
     * 1. Musim Berjalan (Current Season Ongoing)
     * 2. Lanjutan Musim Sebelumnya (Previous Season Carry-over)
     * 3. Serial Panjang Berkelanjutan (Continuous On-Going)
     */
    public static void getMasihTayangDualLane(int page, int perPage, String filterLane, PagedCallback<List<Anime>> callback) {
        SeasonPair cur = getCurrentSeasonPair();
        String query = "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "pageInfo { hasNextPage total currentPage } " +
                "media(status: RELEASING, sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }";

        executePagedQuery(query, page, perPage, new PagedCallback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> list, boolean hasNextPage, int total) {
                List<Anime> currentSeasonList = new ArrayList<>();
                List<Anime> prevSeasonList = new ArrayList<>();
                List<Anime> longRunningList = new ArrayList<>();
                Set<Integer> seenIds = new HashSet<>();

                if (list != null) {
                    for (Anime a : list) {
                        if (a == null || seenIds.contains(a.id)) continue;
                        seenIds.add(a.id);
                        if (isLongRunning(a)) {
                            a.isLongRunning = true;
                            longRunningList.add(a);
                        } else {
                            a.isLongRunning = false;
                            boolean isCurrentSeason = a.season != null && cur.season.equalsIgnoreCase(a.season) && a.year == cur.year;
                            if (isCurrentSeason) {
                                currentSeasonList.add(a);
                            } else {
                                // Started in previous seasons / years but still airing
                                prevSeasonList.add(a);
                            }
                        }
                    }
                }

                List<Anime> result = new ArrayList<>();
                if ("CUR".equalsIgnoreCase(filterLane) || "CURRENT".equalsIgnoreCase(filterLane)) {
                    result.addAll(currentSeasonList);
                } else if ("PREV".equalsIgnoreCase(filterLane) || "PREVIOUS".equalsIgnoreCase(filterLane)) {
                    result.addAll(prevSeasonList);
                } else if ("LONG".equalsIgnoreCase(filterLane)) {
                    result.addAll(longRunningList);
                } else {
                    // Tampilan 3-Lane Terstruktur dengan section header pembatas
                    if (!currentSeasonList.isEmpty()) {
                        result.add(Anime.createHeader("今期放送枠 • MUSIM BERJALAN (" + cur.season + " " + cur.year + ")", "放送中"));
                        result.addAll(currentSeasonList);
                    }
                    if (!prevSeasonList.isEmpty()) {
                        result.add(Anime.createHeader("前期継続枠 • LANJUTAN MUSIM SEBELUMNYA", "継続中"));
                        result.addAll(prevSeasonList);
                    }
                    if (!longRunningList.isEmpty()) {
                        result.add(Anime.createHeader("長期連載枠 • SERIAL PANJANG BERKELANJUTAN", "定番作品"));
                        result.addAll(longRunningList);
                    }
                }

                callback.onSuccess(result, hasNextPage, total);
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    public static void getMasihTayangDualLane(int page, int perPage, String filterLane, Callback<List<Anime>> callback) {
        getMasihTayangDualLane(page, perPage, filterLane, new PagedCallback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result, boolean hasNextPage, int total) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    public static List<Anime> parseMediaArray(JSONObject pageObj) {
        List<Anime> list = new ArrayList<>();
        if (pageObj == null) return list;
        JSONArray mediaArr = pageObj.optJSONArray("media");
        if (mediaArr == null) return list;
        for (int i = 0; i < mediaArr.length(); i++) {
            JSONObject m = mediaArr.optJSONObject(i);
            if (m != null) {
                Anime a = parseMediaNode(m);
                if (a != null) {
                    list.add(a);
                }
            }
        }
        return list;
    }

    public static void getTrending(int page, int perPage, Callback<List<Anime>> callback) {
        String query = "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "pageInfo { hasNextPage total currentPage } " +
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
                "pageInfo { hasNextPage total currentPage } " +
                "media(season: %s, seasonYear: %d, sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }", season.toUpperCase(), year);
        executePagedQuery(query, page, perPage, callback);
    }

    public static void getAiring(int page, int perPage, Callback<List<Anime>> callback) {
        String query = "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "pageInfo { hasNextPage total currentPage } " +
                "media(status: RELEASING, sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }";
        executePagedQuery(query, page, perPage, callback);
    }

    public static void getPopular(int page, int perPage, Callback<List<Anime>> callback) {
        String query = "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "pageInfo { hasNextPage total currentPage } " +
                "media(sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                MEDIA_FIELDS + " } } }";
        executePagedQuery(query, page, perPage, callback);
    }

    private static final Set<String> ANILIST_OFFICIAL_GENRES = new HashSet<>(Arrays.asList(
            "action", "adventure", "comedy", "drama", "ecchi", "fantasy", "horror",
            "mahou shoujo", "mecha", "music", "mystery", "psychological", "romance",
            "sci-fi", "slice of life", "sports", "supernatural", "thriller"
    ));

    public static void getCatalog(String format, String sort, int page, int perPage, Callback<List<Anime>> callback) {
        getCatalogCrossFiltered(sort, "", "", "", format, "", page, perPage, callback);
    }

    public static void getCatalogCrossFiltered(String sort, String year, String season, String month, String type, String status, int page, int perPage, PagedCallback<List<Anime>> callback) {
        String sortOrder = "POPULARITY_DESC";
        if ("score".equalsIgnoreCase(sort)) sortOrder = "SCORE_DESC";
        else if ("latest".equalsIgnoreCase(sort)) sortOrder = "START_DATE_DESC";
        else if ("trending".equalsIgnoreCase(sort)) sortOrder = "TRENDING_DESC";
        else if ("az".equalsIgnoreCase(sort)) sortOrder = "TITLE_ROMAJI";
        else if ("most_favorite".equalsIgnoreCase(sort)) sortOrder = "FAVOURITES_DESC";

        StringBuilder filters = new StringBuilder();
        if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type.trim())) {
            filters.append(", format: ").append(type.trim().toUpperCase(Locale.US));
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            if ("releasing".equalsIgnoreCase(status) || "airing".equalsIgnoreCase(status)) {
                filters.append(", status: RELEASING");
            } else if ("completed".equalsIgnoreCase(status)) {
                filters.append(", status: FINISHED");
            }
        }
        if (season != null && !season.trim().isEmpty() && !"ALL".equalsIgnoreCase(season.trim())) {
            filters.append(", season: ").append(season.trim().toUpperCase(Locale.US));
        }

        if (year != null && !year.trim().isEmpty()) {
            String y = year.trim();
            if (y.matches("\\d{4}")) {
                if (month != null && !month.trim().isEmpty()) {
                    try {
                        int m = Integer.parseInt(month.trim());
                        if (m >= 1 && m <= 12) {
                            int yInt = Integer.parseInt(y);
                            int start = yInt * 10000 + m * 100 + 1;
                            int end = yInt * 10000 + m * 100 + 31;
                            filters.append(", startDate_greater: ").append(start).append(", startDate_lesser: ").append(end);
                        } else {
                            filters.append(", seasonYear: ").append(y);
                        }
                    } catch (Exception e) {
                        filters.append(", seasonYear: ").append(y);
                    }
                } else {
                    filters.append(", seasonYear: ").append(y);
                }
            } else if ("2015-2019".equals(y)) {
                filters.append(", startDate_greater: 20150101, startDate_lesser: 20191231");
            } else if ("2010-2014".equals(y)) {
                filters.append(", startDate_greater: 20100101, startDate_lesser: 20141231");
            } else if ("2000-2009".equals(y)) {
                filters.append(", startDate_greater: 20000101, startDate_lesser: 20091231");
            } else if (y.startsWith("<") || y.contains("2000")) {
                filters.append(", startDate_lesser: 20000101");
            }
        }

        String query = String.format(Locale.US,
                "query ($page: Int, $perPage: Int) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "pageInfo { hasNextPage total currentPage } " +
                "media(sort: %s, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]%s) { " +
                MEDIA_FIELDS + " } } }", sortOrder, filters.toString());
        executePagedQuery(query, page, perPage, callback);
    }

    public static void getCatalogCrossFiltered(String sort, String year, String season, String month, String type, String status, int page, int perPage, Callback<List<Anime>> callback) {
        getCatalogCrossFiltered(sort, year, season, month, type, status, page, perPage, new PagedCallback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result, boolean hasNextPage, int total) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    public static void getByGenre(String genre, int page, int perPage, PagedCallback<List<Anime>> callback) {
        if (genre == null || genre.trim().isEmpty()) {
            getPopular(page, perPage, new Callback<List<Anime>>() {
                @Override
                public void onSuccess(List<Anime> result) {
                    callback.onSuccess(result, result != null && result.size() >= perPage, result != null ? result.size() : 0);
                }

                @Override
                public void onError(Throwable t) {
                    callback.onError(t);
                }
            });
            return;
        }
        String clean = genre.trim();
        boolean isOfficialGenre = ANILIST_OFFICIAL_GENRES.contains(clean.toLowerCase(Locale.US));

        String query;
        if (isOfficialGenre) {
            query = "query ($page: Int, $perPage: Int, $genre: String) { " +
                    "Page(page: $page, perPage: $perPage) { " +
                    "pageInfo { hasNextPage total currentPage } " +
                    "media(genre: $genre, sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                    MEDIA_FIELDS + " } } }";
        } else {
            query = "query ($page: Int, $perPage: Int, $tag: String) { " +
                    "Page(page: $page, perPage: $perPage) { " +
                    "pageInfo { hasNextPage total currentPage } " +
                    "media(tag: $tag, sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: [\"Hentai\"]) { " +
                    MEDIA_FIELDS + " } } }";
        }

        try {
            JSONObject variables = new JSONObject();
            variables.put("page", page);
            variables.put("perPage", perPage);
            if (isOfficialGenre) {
                variables.put("genre", clean);
            } else {
                variables.put("tag", clean);
            }

            JSONObject body = new JSONObject();
            body.put("query", query);
            body.put("variables", variables);

            executePostPaged(body.toString(), callback);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public static void getByGenre(String genre, int page, int perPage, Callback<List<Anime>> callback) {
        getByGenre(genre, page, perPage, new PagedCallback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result, boolean hasNextPage, int total) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
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

    public static void search(String keyword, int page, int perPage, PagedCallback<List<Anime>> callback) {
        String query = "query ($page: Int, $perPage: Int, $search: String) { " +
                "Page(page: $page, perPage: $perPage) { " +
                "pageInfo { hasNextPage total currentPage } " +
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

            executePostPaged(body.toString(), callback);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public static void search(String keyword, int page, int perPage, Callback<List<Anime>> callback) {
        search(keyword, page, perPage, new PagedCallback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result, boolean hasNextPage, int total) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    private static final String RELATIONS_QUERY =
            "query ($id: Int, $search: String) { " +
            "  Media(id: $id, search: $search, type: ANIME, isAdult: false) { " +
            "    id " +
            "    title { romaji english native } " +
            "    format " +
            "    relations { " +
            "      edges { " +
            "        relationType " +
            "        node { " +
            "          id idMal title { romaji english native } coverImage { extraLarge large medium } " +
            "          bannerImage averageScore status format episodes season seasonYear " +
            "          description(asHtml: false) genres studios(isMain: true) { nodes { name } } " +
            "        } " +
            "      } " +
            "    } " +
            "  } " +
            "}";

    public static void getRelations(int animeId, String searchTitle, Callback<RelationsResult> callback) {
        if (animeId <= 0 && (searchTitle == null || searchTitle.trim().isEmpty())) {
            callback.onSuccess(new RelationsResult());
            return;
        }

        try {
            JSONObject variables = new JSONObject();
            if (animeId > 0) {
                variables.put("id", animeId);
            } else {
                variables.put("search", searchTitle.trim());
            }

            JSONObject body = new JSONObject();
            body.put("query", RELATIONS_QUERY);
            body.put("variables", variables);

            executePostCustom(body.toString(), new Callback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject root) {
                    try {
                        RelationsResult result = parseRelationsResponse(root, animeId);
                        callback.onSuccess(result);

                        // Asynchronously enrich with 2nd-hop franchise traversal via GraphQL aliases
                        fetchSecondHopRelations(result, animeId, callback);
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
    }

    private static void fetchSecondHopRelations(RelationsResult result, int originalAnimeId, Callback<RelationsResult> callback) {
        if (result == null) return;
        List<Integer> candidateIds = new ArrayList<>();
        Set<Integer> existingIds = new HashSet<>();
        if (originalAnimeId > 0) existingIds.add(originalAnimeId);

        for (Anime a : result.seasons) {
            existingIds.add(a.id);
            if (("PREQUEL".equalsIgnoreCase(a.relationType) || "SEQUEL".equalsIgnoreCase(a.relationType) || "PARENT".equalsIgnoreCase(a.relationType))
                    && a.id > 0 && !candidateIds.contains(a.id)) {
                candidateIds.add(a.id);
                if (candidateIds.size() >= 3) break;
            }
        }
        for (Anime a : result.movies) {
            existingIds.add(a.id);
            if (("PREQUEL".equalsIgnoreCase(a.relationType) || "SEQUEL".equalsIgnoreCase(a.relationType))
                    && a.id > 0 && !candidateIds.contains(a.id)) {
                candidateIds.add(a.id);
                if (candidateIds.size() >= 3) break;
            }
        }

        if (candidateIds.isEmpty()) return;

        try {
            StringBuilder queryBuilder = new StringBuilder("query { ");
            for (int i = 0; i < candidateIds.size(); i++) {
                queryBuilder.append("m").append(i).append(": Media(id: ").append(candidateIds.get(i))
                        .append(", type: ANIME, isAdult: false) { ")
                        .append("relations { edges { relationType node { ")
                        .append("id idMal title { romaji english native } coverImage { extraLarge large medium } ")
                        .append("bannerImage averageScore status format episodes season seasonYear ")
                        .append("description(asHtml: false) genres studios(isMain: true) { nodes { name } } ")
                        .append("} } } } ");
            }
            queryBuilder.append("}");

            JSONObject body = new JSONObject();
            body.put("query", queryBuilder.toString());

            executePostCustom(body.toString(), new Callback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject hopRoot) {
                    try {
                        JSONObject data = hopRoot != null ? hopRoot.optJSONObject("data") : null;
                        if (data == null) return;

                        boolean added = false;
                        for (int i = 0; i < candidateIds.size(); i++) {
                            JSONObject mObj = data.optJSONObject("m" + i);
                            if (mObj == null) continue;
                            JSONObject relObj = mObj.optJSONObject("relations");
                            if (relObj == null) continue;
                            JSONArray edges = relObj.optJSONArray("edges");
                            if (edges == null) continue;

                            for (int j = 0; j < edges.length(); j++) {
                                JSONObject edge = edges.optJSONObject(j);
                                if (edge == null) continue;
                                String relType = optSafeString(edge, "relationType", "");
                                JSONObject node = edge.optJSONObject("node");
                                if (node == null) continue;

                                int nid = node.optInt("id", 0);
                                if (nid <= 0 || existingIds.contains(nid)) continue;

                                String fmt = optSafeString(node, "format", "TV");
                                if (!isAnimeFormat(fmt)) continue;

                                boolean isRelevant = "PREQUEL".equalsIgnoreCase(relType) ||
                                                     "SEQUEL".equalsIgnoreCase(relType) ||
                                                     "PARENT".equalsIgnoreCase(relType) ||
                                                     "ALTERNATIVE".equalsIgnoreCase(relType) ||
                                                     "MOVIE".equalsIgnoreCase(fmt);
                                if (!isRelevant) continue;

                                Anime subAnime = parseMediaNode(node);
                                if (subAnime == null) continue;

                                existingIds.add(nid);
                                subAnime.relationType = relType;

                                if ("MOVIE".equalsIgnoreCase(fmt)) {
                                    String moviePart = extractMoviePart(subAnime.title, subAnime.romajiTitle);
                                    if (moviePart != null) {
                                        subAnime.relationLabel = moviePart;
                                    } else if ("PREQUEL".equalsIgnoreCase(relType)) {
                                        subAnime.relationLabel = "Prekuel Film";
                                    } else if ("SEQUEL".equalsIgnoreCase(relType)) {
                                        subAnime.relationLabel = "Sekuel Film";
                                    } else {
                                        subAnime.relationLabel = "Movie";
                                    }
                                    result.movies.add(subAnime);
                                    added = true;
                                } else {
                                    int explicitSeason = extractExplicitSeasonNumber(subAnime.title, subAnime.romajiTitle);
                                    if (explicitSeason > 1) {
                                        subAnime.relationLabel = "Season " + explicitSeason;
                                    } else if (explicitSeason == 1) {
                                        subAnime.relationLabel = "Season 1";
                                    } else if ("PREQUEL".equalsIgnoreCase(relType)) {
                                        subAnime.relationLabel = "Prekuel";
                                    } else if ("SEQUEL".equalsIgnoreCase(relType)) {
                                        subAnime.relationLabel = "Sekuel";
                                    } else if ("PARENT".equalsIgnoreCase(relType)) {
                                        subAnime.relationLabel = "Seri Utama";
                                    } else if ("SIDE_STORY".equalsIgnoreCase(relType)) {
                                        subAnime.relationLabel = "Side Story";
                                    } else if ("SPIN_OFF".equalsIgnoreCase(relType)) {
                                        subAnime.relationLabel = "Spin-Off";
                                    } else if ("OVA".equalsIgnoreCase(fmt)) {
                                        subAnime.relationLabel = "OVA";
                                    } else if ("SPECIAL".equalsIgnoreCase(fmt)) {
                                        subAnime.relationLabel = "Spesial";
                                    } else {
                                        subAnime.relationLabel = "Terkait";
                                    }
                                    result.seasons.add(subAnime);
                                    added = true;
                                }
                            }
                        }

                        if (added) {
                            sortRelationsChronologically(result);
                            callback.onSuccess(result);
                        }
                    } catch (Throwable ignored) {}
                }

                @Override
                public void onError(Throwable t) {
                    // 1st hop was already delivered; ignore 2nd hop background error
                }
            });
        } catch (Throwable ignored) {}
    }

    private static boolean isAnimeFormat(String fmt) {
        if (fmt == null) return false;
        String f = fmt.toUpperCase(Locale.US);
        return f.equals("TV") || f.equals("TV_SHORT") || f.equals("MOVIE") ||
               f.equals("OVA") || f.equals("ONA") || f.equals("SPECIAL");
    }

    private static int extractExplicitSeasonNumber(String title, String romaji) {
        String[] titles = {title, romaji};
        Pattern p1 = Pattern.compile("(?i)\\b(\\d+)(?:nd|rd|th|st)?\\s*season\\b");
        Pattern p2 = Pattern.compile("(?i)\\bseason\\s*(\\d+)\\b");
        Pattern p3 = Pattern.compile("(?i)\\bpart\\s*(\\d+)\\b");
        Pattern p4 = Pattern.compile("(?i)\\bs(\\d+)\\b");

        for (String t : titles) {
            if (t == null || t.isEmpty()) continue;
            Matcher m = p1.matcher(t);
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
            }
            m = p2.matcher(t);
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
            }
            m = p3.matcher(t);
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
            }
            m = p4.matcher(t);
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private static String extractMoviePart(String title, String romaji) {
        String[] titles = {title, romaji};
        Pattern pPart = Pattern.compile("(?i)\\bpart\\s*(\\d+)\\b");
        Pattern pMovie = Pattern.compile("(?i)\\bmovie\\s*(\\d+)\\b");

        for (String t : titles) {
            if (t == null || t.isEmpty()) continue;
            Matcher m = pPart.matcher(t);
            if (m.find()) return "Part " + m.group(1);
            m = pMovie.matcher(t);
            if (m.find()) return "Movie " + m.group(1);
        }
        return null;
    }

    private static int relationRank(String rel) {
        if ("PREQUEL".equalsIgnoreCase(rel)) return 1;
        if ("PARENT".equalsIgnoreCase(rel)) return 2;
        if ("SEQUEL".equalsIgnoreCase(rel)) return 3;
        if ("ALTERNATIVE".equalsIgnoreCase(rel)) return 4;
        if ("SIDE_STORY".equalsIgnoreCase(rel)) return 5;
        if ("SPIN_OFF".equalsIgnoreCase(rel)) return 6;
        return 7;
    }

    public static RelationsResult parseRelationsResponse(JSONObject root, int currentId) {
        RelationsResult result = new RelationsResult();
        if (root == null) return result;
        JSONObject data = root.optJSONObject("data");
        if (data == null) return result;
        JSONObject media = data.optJSONObject("Media");
        if (media == null) return result;

        int targetId = media.optInt("id", currentId);
        Set<Integer> seenIds = new HashSet<>();
        if (targetId > 0) seenIds.add(targetId);
        if (currentId > 0) seenIds.add(currentId);

        JSONObject relationsObj = media.optJSONObject("relations");
        if (relationsObj == null) return result;
        JSONArray edges = relationsObj.optJSONArray("edges");
        if (edges == null) return result;

        List<Anime> rawSeasons = new ArrayList<>();
        List<Anime> rawMovies = new ArrayList<>();

        for (int i = 0; i < edges.length(); i++) {
            JSONObject edge = edges.optJSONObject(i);
            if (edge == null) continue;
            String relType = optSafeString(edge, "relationType", "");
            JSONObject node = edge.optJSONObject("node");
            if (node == null) continue;

            int nid = node.optInt("id", 0);
            if (nid <= 0 || seenIds.contains(nid)) continue;

            String fmt = optSafeString(node, "format", "TV");
            if (!isAnimeFormat(fmt)) continue;

            Anime anime = parseMediaNode(node);
            if (anime == null) continue; // Hentai or invalid

            seenIds.add(nid);
            anime.relationType = relType;

            if ("MOVIE".equalsIgnoreCase(fmt)) {
                String moviePart = extractMoviePart(anime.title, anime.romajiTitle);
                if (moviePart != null) {
                    anime.relationLabel = moviePart;
                } else if ("PREQUEL".equalsIgnoreCase(relType)) {
                    anime.relationLabel = "Prekuel Film";
                } else if ("SEQUEL".equalsIgnoreCase(relType)) {
                    anime.relationLabel = "Sekuel Film";
                } else {
                    anime.relationLabel = "Movie";
                }
                rawMovies.add(anime);
            } else {
                int explicitSeason = extractExplicitSeasonNumber(anime.title, anime.romajiTitle);
                if (explicitSeason > 1) {
                    anime.relationLabel = "Season " + explicitSeason;
                } else if ("PREQUEL".equalsIgnoreCase(relType)) {
                    anime.relationLabel = "Prekuel";
                } else if ("SEQUEL".equalsIgnoreCase(relType)) {
                    anime.relationLabel = "Sekuel";
                } else if ("PARENT".equalsIgnoreCase(relType)) {
                    anime.relationLabel = "Seri Utama";
                } else if ("SIDE_STORY".equalsIgnoreCase(relType)) {
                    anime.relationLabel = "Side Story";
                } else if ("SPIN_OFF".equalsIgnoreCase(relType)) {
                    anime.relationLabel = "Spin-Off";
                } else if ("OVA".equalsIgnoreCase(fmt)) {
                    anime.relationLabel = "OVA";
                } else if ("SPECIAL".equalsIgnoreCase(fmt)) {
                    anime.relationLabel = "Spesial";
                } else {
                    anime.relationLabel = "Terkait";
                }
                rawSeasons.add(anime);
            }

            // Check 2nd-hop relations under PREQUEL, SEQUEL, PARENT
            if ("PREQUEL".equalsIgnoreCase(relType) || "SEQUEL".equalsIgnoreCase(relType) || "PARENT".equalsIgnoreCase(relType)) {
                JSONObject subRels = node.optJSONObject("relations");
                if (subRels != null) {
                    JSONArray subEdges = subRels.optJSONArray("edges");
                    if (subEdges != null) {
                        for (int j = 0; j < subEdges.length(); j++) {
                            JSONObject subEdge = subEdges.optJSONObject(j);
                            if (subEdge == null) continue;
                            String subRelType = optSafeString(subEdge, "relationType", "");
                            JSONObject subNode = subEdge.optJSONObject("node");
                            if (subNode == null) continue;

                            int snid = subNode.optInt("id", 0);
                            if (snid <= 0 || seenIds.contains(snid)) continue;

                            String sfmt = optSafeString(subNode, "format", "TV");
                            if (!isAnimeFormat(sfmt)) continue;

                            boolean isRelevant = "PREQUEL".equalsIgnoreCase(subRelType) ||
                                                 "SEQUEL".equalsIgnoreCase(subRelType) ||
                                                 "PARENT".equalsIgnoreCase(subRelType) ||
                                                 "ALTERNATIVE".equalsIgnoreCase(subRelType) ||
                                                 "MOVIE".equalsIgnoreCase(sfmt);
                            if (!isRelevant) continue;

                            Anime subAnime = parseMediaNode(subNode);
                            if (subAnime == null) continue;

                            seenIds.add(snid);
                            subAnime.relationType = subRelType;

                            if ("MOVIE".equalsIgnoreCase(sfmt)) {
                                String moviePart = extractMoviePart(subAnime.title, subAnime.romajiTitle);
                                if (moviePart != null) {
                                    subAnime.relationLabel = moviePart;
                                } else if ("PREQUEL".equalsIgnoreCase(subRelType)) {
                                    subAnime.relationLabel = "Prekuel Film";
                                } else if ("SEQUEL".equalsIgnoreCase(subRelType)) {
                                    subAnime.relationLabel = "Sekuel Film";
                                } else {
                                    subAnime.relationLabel = "Movie";
                                }
                                rawMovies.add(subAnime);
                            } else {
                                int explicitSeason = extractExplicitSeasonNumber(subAnime.title, subAnime.romajiTitle);
                                if (explicitSeason > 1) {
                                    subAnime.relationLabel = "Season " + explicitSeason;
                                } else if (explicitSeason == 1) {
                                    subAnime.relationLabel = "Season 1";
                                } else if ("PREQUEL".equalsIgnoreCase(subRelType) && "PREQUEL".equalsIgnoreCase(relType)) {
                                    subAnime.relationLabel = "Season 1";
                                } else if ("PREQUEL".equalsIgnoreCase(subRelType)) {
                                    subAnime.relationLabel = "Prekuel";
                                } else if ("SEQUEL".equalsIgnoreCase(subRelType)) {
                                    subAnime.relationLabel = "Sekuel";
                                } else if ("PARENT".equalsIgnoreCase(subRelType)) {
                                    subAnime.relationLabel = "Seri Utama";
                                } else if ("SIDE_STORY".equalsIgnoreCase(subRelType)) {
                                    subAnime.relationLabel = "Side Story";
                                } else if ("SPIN_OFF".equalsIgnoreCase(subRelType)) {
                                    subAnime.relationLabel = "Spin-Off";
                                } else if ("OVA".equalsIgnoreCase(sfmt)) {
                                    subAnime.relationLabel = "OVA";
                                } else if ("SPECIAL".equalsIgnoreCase(sfmt)) {
                                    subAnime.relationLabel = "Spesial";
                                } else {
                                    subAnime.relationLabel = "Terkait";
                                }
                                rawSeasons.add(subAnime);
                            }
                        }
                    }
                }
            }
        }

        result.seasons.addAll(rawSeasons);
        result.movies.addAll(rawMovies);
        sortRelationsChronologically(result);
        return result;
    }

    public static void sortRelationsChronologically(RelationsResult result) {
        if (result == null) return;
        Collections.sort(result.seasons, new Comparator<Anime>() {
            @Override
            public int compare(Anime a, Anime b) {
                int yA = a.year > 0 ? a.year : 9999;
                int yB = b.year > 0 ? b.year : 9999;
                if (yA != yB) return Integer.compare(yA, yB);
                return Integer.compare(relationRank(a.relationType), relationRank(b.relationType));
            }
        });

        if (!result.seasons.isEmpty()) {
            Anime first = result.seasons.get(0);
            if ("Prekuel".equals(first.relationLabel) && first.year > 0) {
                first.relationLabel = "Season 1";
            }
        }

        Collections.sort(result.movies, new Comparator<Anime>() {
            @Override
            public int compare(Anime a, Anime b) {
                int yA = a.year > 0 ? a.year : 9999;
                int yB = b.year > 0 ? b.year : 9999;
                if (yA != yB) return Integer.compare(yA, yB);
                return Integer.compare(relationRank(a.relationType), relationRank(b.relationType));
            }
        });
    }

    private static void executePagedQuery(String query, int page, int perPage, PagedCallback<List<Anime>> callback) {
        try {
            JSONObject variables = new JSONObject();
            variables.put("page", page);
            variables.put("perPage", perPage);

            JSONObject body = new JSONObject();
            body.put("query", query);
            body.put("variables", variables);

            executePostPaged(body.toString(), callback);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    private static void executePagedQuery(String query, int page, int perPage, Callback<List<Anime>> callback) {
        executePagedQuery(query, page, perPage, new PagedCallback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result, boolean hasNextPage, int total) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
    }

    private static void executePostPaged(String jsonBody, PagedCallback<List<Anime>> callback) {
        executePostCustom(jsonBody, new Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject root) {
                try {
                    JSONObject data = root != null ? root.optJSONObject("data") : null;
                    JSONObject pageObj = data != null ? data.optJSONObject("Page") : null;
                    if (pageObj == null) {
                        callback.onSuccess(new ArrayList<>(), false, 0);
                        return;
                    }
                    JSONArray mediaArr = pageObj.optJSONArray("media");
                    JSONObject pageInfo = pageObj.optJSONObject("pageInfo");
                    boolean hasNextPage = pageInfo != null && pageInfo.optBoolean("hasNextPage", false);
                    int total = pageInfo != null ? pageInfo.optInt("total", 0) : 0;

                    List<Anime> list = new ArrayList<>();
                    if (mediaArr != null) {
                        for (int i = 0; i < mediaArr.length(); i++) {
                            JSONObject m = mediaArr.optJSONObject(i);
                            if (m != null) {
                                Anime anime = parseMediaNode(m);
                                if (anime != null) list.add(anime);
                            }
                        }
                    }
                    callback.onSuccess(list, hasNextPage, total);
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

    private static void executePost(String jsonBody, Callback<List<Anime>> callback) {
        executePostPaged(jsonBody, new PagedCallback<List<Anime>>() {
            @Override
            public void onSuccess(List<Anime> result, boolean hasNextPage, int total) {
                callback.onSuccess(result);
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
            a.nativeTitle = nat;
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
            } else if (a.nextAiringEpisode == 1) {
                a.airedEpisodesCount = 0;
            }
        } else if ("FINISHED".equalsIgnoreCase(a.status) && a.episodesCount > 0) {
            a.airedEpisodesCount = a.episodesCount;
        }

        boolean isOngoing = "RELEASING".equalsIgnoreCase(a.status) || "AIRING".equalsIgnoreCase(a.status);
        boolean isNotYetAired = "NOT_YET_RELEASED".equalsIgnoreCase(a.status) ||
                (isOngoing && a.nextAiringEpisode == 1 && a.airedEpisodesCount == 0);

        if (isNotYetAired) {
            a.epsCounter = "Belum Tayang";
        } else if (isOngoing) {
            if (a.airedEpisodesCount > 0) {
                if (a.episodesCount > 0) {
                    a.epsCounter = "Ep " + a.airedEpisodesCount + "/" + a.episodesCount;
                } else {
                    a.epsCounter = "Ep " + a.airedEpisodesCount;
                }
            } else {
                a.epsCounter = "Ongoing";
            }
        } else if ("FINISHED".equalsIgnoreCase(a.status)) {
            if (a.episodesCount > 0) {
                a.epsCounter = a.episodesCount + " Ep (Tamat)";
            } else if (a.airedEpisodesCount > 0) {
                a.epsCounter = a.airedEpisodesCount + " Ep (Tamat)";
            } else {
                a.epsCounter = "Tamat";
            }
        } else if (a.episodesCount > 0) {
            a.epsCounter = a.episodesCount + " Ep";
        } else {
            a.epsCounter = a.format != null ? a.format : "TV";
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
