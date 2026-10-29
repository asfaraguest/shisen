package id.shisen.app.data.subtitle;

import id.shisen.app.data.service.HttpClientProvider;
import id.shisen.app.util.TitleMatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class OpenSubtitlesClient {

    public static class SubtitleSearchResult {
        public String fileId;
        public String filename;
        public String downloadUrl;
        public int downloads;

        public SubtitleSearchResult(String fileId, String filename, String downloadUrl, int downloads) {
            this.fileId = fileId;
            this.filename = filename;
            this.downloadUrl = downloadUrl;
            this.downloads = downloads;
        }
    }

    public interface SearchCallback {
        void onSuccess(List<SubtitleSearchResult> results);
        void onError(String error);
    }

    public interface DownloadCallback {
        void onSuccess(List<SubtitleCue> cues);
        void onError(String error);
    }

    private static final Pattern AD_PATTERN = Pattern.compile(
            "(?i)(opensubtitles|subdl|subtitles downloaded from|sync and correct|synchronize|advertisement|www\\.)"
    );

    public static void searchIndonesianAsync(String title, int episode, SearchCallback callback) {
        new Thread(() -> {
            try {
                int season = TitleMatcher.extractSeasonNumber(title);
                OkHttpClient client = HttpClientProvider.getClient();
                String cleanTitle = cleanAnimeTitle(title);
                String imdbId = resolveImdbId(client, cleanTitle);

                List<SubtitleSearchResult> results = new ArrayList<>();
                if (!imdbId.isEmpty()) {
                    // Coba via Stremio OpenSubtitles v3 API dengan Season dan Episode dinamis
                    String stremioUrl = "https://opensubtitles-v3.strem.io/subtitles/series/" + imdbId + ":" + season + ":" + episode + ".json";
                    Request req = new Request.Builder()
                            .url(stremioUrl)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0")
                            .build();

                    try (Response resp = client.newCall(req).execute()) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            JSONObject json = new JSONObject(resp.body().string());
                            JSONArray subs = json.optJSONArray("subtitles");
                            if (subs != null) {
                                for (int i = 0; i < subs.length(); i++) {
                                    JSONObject sub = subs.getJSONObject(i);
                                    String lang = sub.optString("lang", "").toLowerCase(Locale.US);
                                    if ("id".equals(lang) || "ind".equals(lang) || "indonesian".equals(lang)) {
                                        String fileId = sub.optString("id", String.valueOf(i));
                                        String dlUrl = sub.optString("url", "");
                                        if (!dlUrl.isEmpty()) {
                                            results.add(new SubtitleSearchResult(fileId, "OpenSubtitles #" + fileId, dlUrl, 100));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (callback != null) {
                    callback.onSuccess(results);
                }
            } catch (Exception e) {
                if (callback != null) callback.onError("Gagal mencari OpenSubtitles: " + e.getMessage());
            }
        }).start();
    }

    public static void downloadAndParseAsync(String downloadUrl, DownloadCallback callback) {
        new Thread(() -> {
            try {
                OkHttpClient client = HttpClientProvider.getClient();
                Request req = new Request.Builder()
                        .url(downloadUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0")
                        .header("Referer", "https://opensubtitles.org/")
                        .build();

                try (Response resp = client.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        if (callback != null) callback.onError("Gagal mengunduh subtitle OpenSubtitles: HTTP " + resp.code());
                        return;
                    }

                    String rawContent = resp.body().string();
                    List<SubtitleCue> cues = SubtitleParser.parse(rawContent);
                    if (cues.isEmpty()) {
                        if (callback != null) callback.onError("Subtitle OpenSubtitles kosong");
                        return;
                    }

                    // Hapus watermark/iklan dan terapkan leksikon universal
                    List<SubtitleCue> cleanCues = new ArrayList<>();
                    for (SubtitleCue c : cues) {
                        if (AD_PATTERN.matcher(c.text).find()) {
                            continue;
                        }
                        c.text = SmartLexicon.applyUniversalLexicon(c.text);
                        cleanCues.add(c);
                    }

                    if (callback != null) callback.onSuccess(cleanCues);
                }
            } catch (Exception e) {
                if (callback != null) callback.onError("Error mengunduh OpenSubtitles: " + e.getMessage());
            }
        }).start();
    }

    private static String resolveImdbId(OkHttpClient client, String cleanTitle) {
        try {
            String cinemetaUrl = "https://v3-cinemeta.strem.io/catalog/series/top/search=" + URLEncoder.encode(cleanTitle, "UTF-8") + ".json";
            Request req = new Request.Builder().url(cinemetaUrl).build();
            try (Response resp = client.newCall(req).execute()) {
                if (resp.isSuccessful() && resp.body() != null) {
                    JSONObject json = new JSONObject(resp.body().string());
                    JSONArray metas = json.optJSONArray("metas");
                    if (metas != null && metas.length() > 0) {
                        for (int i = 0; i < Math.min(metas.length(), 4); i++) {
                            JSONObject m = metas.getJSONObject(i);
                            String imdb = m.optString("imdb_id", m.optString("id", ""));
                            String name = m.optString("name", "");
                            if (imdb.startsWith("tt")) {
                                if (name.isEmpty() || TitleMatcher.isTitleMatch(cleanTitle, name)) {
                                    return imdb;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String cleanAnimeTitle(String title) {
        if (title == null) return "";
        String t = title.replaceAll("(?i)\\b(season\\s*\\d+|\\d+(?:nd|rd|th|st)\\s*season|part\\s*\\d+|cour\\s*\\d+|s\\d+)\\b", "");
        t = t.replaceAll("\\([^)]*\\)", " ");
        t = t.replaceAll("\\[[^\\]]*\\]", " ");
        return t.replaceAll("\\s+", " ").trim();
    }
}
