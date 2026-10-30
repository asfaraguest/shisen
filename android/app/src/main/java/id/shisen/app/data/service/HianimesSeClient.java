package id.shisen.app.data.service;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import id.shisen.app.data.model.Anime;
import id.shisen.app.data.model.Episode;
import id.shisen.app.data.model.StreamSource;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Client untuk sumber streaming HiAnimes.se.
 * Mendukung Direct MAL ID route (Megaplay & ZokoAnime) dengan intro/outro tagging,
 * serta REST API fallback melalui animehot.cc / anitv.cfd.
 */
public class HianimesSeClient {

    private static final String TAG = "HianimesSeClient";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable t);
    }

    /**
     * Menyelesaikan stream langsung menggunakan ID MyAnimeList dan nomor episode.
     * Jalur ultra-cepat: langsung membidik embed URL Megaplay atau ZokoAnime tanpa scraping HTML.
     */
    public static void resolveByMalId(int malId, int epNumber, Callback<StreamSource> callback) {
        HttpClientProvider.execute(() -> {
            try {
                StreamSource source = resolveByMalIdSync(malId, epNumber);
                callback.onSuccess(source);
            } catch (Throwable t) {
                callback.onError(t);
            }
        });
    }

    /**
     * Resolusi sinkron berdasarkan MAL ID.
     */
    public static StreamSource resolveByMalIdSync(int malId, int epNumber) throws Exception {
        if (malId <= 0 || epNumber <= 0) {
            throw new IllegalArgumentException("MAL ID dan nomor episode harus valid");
        }

        Exception megaplayErr = null;

        // 1. Coba Megaplay embed
        String megaplayUrl = "https://megaplay.buzz/stream/mal/" + malId + "/" + epNumber + "/sub?autostart=false&asi=0";
        try {
            StreamSource source = HiAnimeClient.resolveMegaplay(megaplayUrl, NetworkConfig.HIANIMES_SE_BASE + "/");
            if (source != null && source.url != null && !source.url.isEmpty()) {
                source.server = "HiAnimes (Megaplay)";
                source.provider = "hianimes_se";
                return source;
            }
        } catch (Exception e) {
            megaplayErr = e;
            Log.w(TAG, "Megaplay MAL embed gagal untuk malId=" + malId + " ep=" + epNumber + ": " + e.getMessage());
        }

        // 2. Fallback ke ZokoAnime embed
        String zokoUrl = "https://zokoanime.video/stream/mal/" + malId + "/" + epNumber + "/sub?autostart=false&asi=0";
        try {
            StreamSource source = ZokoAnimeParser.fetchAndParse(zokoUrl, NetworkConfig.HIANIMES_SE_BASE + "/");
            if (source != null && source.url != null && !source.url.isEmpty()) {
                source.server = "HiAnimes (ZokoAnime)";
                source.provider = "hianimes_se";
                return source;
            }
        } catch (Exception e) {
            Log.w(TAG, "ZokoAnime MAL embed gagal untuk malId=" + malId + " ep=" + epNumber + ": " + e.getMessage());
            throw megaplayErr != null ? megaplayErr : e;
        }

        throw new IOException("Stream tidak tersedia di HiAnimes.se untuk MAL ID: " + malId);
    }

    /**
     * Mencari anime dan menyelesaikan stream episode di HiAnimes.se.
     */
    public static void searchAndResolve(Anime anime, Episode episode, Callback<StreamSource> callback) {
        if (anime == null || episode == null) {
            callback.onError(new IllegalArgumentException("Anime atau episode tidak boleh kosong"));
            return;
        }

        // Coba jalur langsung MAL jika ID MAL tersedia
        if (anime.idMal > 0) {
            resolveByMalId(anime.idMal, episode.number, new Callback<StreamSource>() {
                @Override
                public void onSuccess(StreamSource result) {
                    callback.onSuccess(result);
                }

                @Override
                public void onError(Throwable t) {
                    Log.d(TAG, "Direct MAL route gagal, mencoba pencarian API: " + t.getMessage());
                    searchViaApi(anime, episode, callback);
                }
            });
        } else {
            searchViaApi(anime, episode, callback);
        }
    }

    private static void searchViaApi(Anime anime, Episode episode, Callback<StreamSource> callback) {
        HttpClientProvider.execute(() -> {
            try {
                String query = (anime.title != null && !anime.title.isEmpty() && !"null".equalsIgnoreCase(anime.title))
                        ? anime.title : anime.romajiTitle;
                if (query == null || query.isEmpty()) {
                    callback.onError(new IOException("Judul anime tidak valid untuk pencarian HiAnimes.se"));
                    return;
                }

                String[] endpoints = {
                        NetworkConfig.ANIMEHOT_API_BASE + "/search",
                        NetworkConfig.ANITV_API_BASE + "/search"
                };

                JSONArray results = null;
                for (String endpoint : endpoints) {
                    try {
                        results = performSearchRequest(endpoint, query);
                        if (results != null && results.length() > 0) {
                            break;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Search API gagal di " + endpoint + ": " + e.getMessage());
                    }
                }

                if (results == null || results.length() == 0) {
                    callback.onError(new IOException("Anime tidak ditemukan di katalog HiAnimes.se"));
                    return;
                }

                // Cari anime paling cocok
                JSONObject bestMatch = null;
                String queryLower = query.toLowerCase(Locale.US);

                for (int i = 0; i < results.length(); i++) {
                    JSONObject item = results.optJSONObject(i);
                    if (item == null) continue;
                    String title = item.optString("title", "").toLowerCase(Locale.US);
                    String english = item.optString("English", "").toLowerCase(Locale.US);
                    String altTitle = item.optString("alternateTitle", "").toLowerCase(Locale.US);

                    if (title.contains(queryLower) || english.contains(queryLower) || altTitle.contains(queryLower)
                            || queryLower.contains(title)) {
                        bestMatch = item;
                        break;
                    }
                }

                if (bestMatch == null) {
                    bestMatch = results.optJSONObject(0);
                }

                if (bestMatch == null) {
                    callback.onError(new IOException("Kandidat anime HiAnimes.se tidak valid"));
                    return;
                }

                // Periksa apakah item memiliki mal_id
                int itemMalId = bestMatch.optInt("mal_id", 0);
                if (itemMalId > 0) {
                    try {
                        StreamSource src = resolveByMalIdSync(itemMalId, episode.number);
                        callback.onSuccess(src);
                        return;
                    } catch (Exception e) {
                        Log.w(TAG, "Resolusi sinkron MAL ID gagal: " + e.getMessage());
                    }
                }

                callback.onError(new IOException("Gagal menyelesaikan stream episode " + episode.number + " dari HiAnimes.se"));
            } catch (Throwable t) {
                callback.onError(t);
            }
        });
    }

    private static JSONArray performSearchRequest(String url, String query) throws Exception {
        OkHttpClient client = HttpClientProvider.getClient();
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("title", query);

        RequestBody body = RequestBody.create(bodyJson.toString(), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", HttpClientProvider.USER_AGENT)
                .header("Referer", NetworkConfig.HIANIMES_SE_BASE + "/")
                .header("Origin", NetworkConfig.HIANIMES_SE_BASE)
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Search HTTP gagal: " + response.code());
            }
            String respStr = response.body().string();
            String trimmed = respStr.trim();
            if (trimmed.startsWith("[")) {
                return new JSONArray(trimmed);
            } else if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                if (obj.has("data")) {
                    JSONArray dataArr = obj.optJSONArray("data");
                    if (dataArr != null) return dataArr;
                }
            }
            return new JSONArray();
        }
    }
}
