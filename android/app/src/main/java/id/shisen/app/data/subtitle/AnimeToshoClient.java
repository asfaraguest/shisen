package id.shisen.app.data.subtitle;

import id.shisen.app.data.service.HttpClientProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnimeToshoClient {

    public static class SubtitleSearchResult {
        public String id;
        public String filename;
        public String downloadUrl;
        public String source;
        public int downloads;

        public SubtitleSearchResult(String id, String filename, String downloadUrl, String source, int downloads) {
            this.id = id;
            this.filename = filename;
            this.downloadUrl = downloadUrl;
            this.source = source;
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

    private static final Pattern ATTACHMENT_RE = Pattern.compile(
            "<a[^>]+href=[\"'](/download/\\d+/subs/file/(\\d+))[\"'][^>]*>([^<]+)</a>",
            Pattern.CASE_INSENSITIVE
    );

    public static void searchIndonesianAsync(String title, int episode, SearchCallback callback) {
        new Thread(() -> {
            try {
                String cleanTitle = cleanAnimeTitle(title);
                String[] queries = {
                        cleanTitle + " " + String.format(java.util.Locale.US, "%02d", episode),
                        cleanTitle + " E" + String.format(java.util.Locale.US, "%02d", episode),
                        cleanTitle + " S01E" + String.format(java.util.Locale.US, "%02d", episode)
                };

                OkHttpClient client = HttpClientProvider.getClient();
                List<SubtitleSearchResult> results = new ArrayList<>();

                for (String q : queries) {
                    if (!results.isEmpty()) break;
                    String url = "https://feed.animetosho.net/json?q=" + URLEncoder.encode(q, "UTF-8");
                    Request req = new Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0")
                            .build();

                    try (Response resp = client.newCall(req).execute()) {
                        if (!resp.isSuccessful() || resp.body() == null) continue;
                        String body = resp.body().string();
                        JSONArray items = new JSONArray(body);

                        for (int i = 0; i < Math.min(items.length(), 10); i++) {
                            JSONObject it = items.getJSONObject(i);
                            String torrentTitle = it.optString("title", it.optString("torrent_name", ""));
                            String tId = it.optString("id", "");
                            if (tId.isEmpty()) continue;

                            // Verifikasi nomor episode di judul torrent
                            String tLow = torrentTitle.toLowerCase();
                            String epStr = String.format(java.util.Locale.US, "%02d", episode);
                            if (!tLow.contains(epStr) && !tLow.contains(" " + episode + " ") && !tLow.contains("e" + episode)) {
                                continue;
                            }

                            // Kunjungi halaman view untuk mengekstrak attachment
                            String viewUrl = "https://animetosho.net/view/" + tId;
                            Request viewReq = new Request.Builder().url(viewUrl).build();
                            try (Response viewResp = client.newCall(viewReq).execute()) {
                                if (!viewResp.isSuccessful() || viewResp.body() == null) continue;
                                String html = viewResp.body().string();

                                Matcher m = ATTACHMENT_RE.matcher(html);
                                while (m.find()) {
                                    String href = m.group(1);
                                    String fileId = m.group(2);
                                    String label = m.group(3).trim();

                                    if (isIndonesianLabel(label)) {
                                        String dlUrl = "https://animetosho.net" + href;
                                        results.add(new SubtitleSearchResult("at_" + fileId, label, dlUrl, "AnimeTosho", 0));
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
                if (callback != null) callback.onError("Gagal mencari subtitle AnimeTosho: " + e.getMessage());
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
                        .header("Referer", "https://animetosho.net/")
                        .build();

                try (Response resp = client.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) {
                        if (callback != null) callback.onError("Gagal mengunduh subtitle: HTTP " + resp.code());
                        return;
                    }

                    byte[] rawBytes = resp.body().bytes();
                    String rawContent;
                    if (rawBytes.length >= 6 && rawBytes[0] == (byte) 0xFD && rawBytes[1] == '7' && rawBytes[2] == 'z' && rawBytes[3] == 'X' && rawBytes[4] == 'Z' && rawBytes[5] == 0x00) {
                        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(rawBytes);
                             org.tukaani.xz.XZInputStream xzIn = new org.tukaani.xz.XZInputStream(bais);
                             java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                            byte[] buf = new byte[8192];
                            int read;
                            while ((read = xzIn.read(buf)) != -1) {
                                baos.write(buf, 0, read);
                            }
                            rawContent = baos.toString("UTF-8");
                        }
                    } else {
                        rawContent = new String(rawBytes, java.nio.charset.StandardCharsets.UTF_8);
                    }

                    List<SubtitleCue> cues = SubtitleParser.parse(rawContent);
                    if (cues.isEmpty()) {
                        if (callback != null) callback.onError("Subtitle kosong atau format tidak didukung");
                        return;
                    }

                    // Terapkan leksikon universal
                    for (SubtitleCue c : cues) {
                        c.text = SmartLexicon.applyUniversalLexicon(c.text);
                    }

                    if (callback != null) callback.onSuccess(cues);
                }
            } catch (Exception e) {
                if (callback != null) callback.onError("Error mengunduh subtitle: " + e.getMessage());
            }
        }).start();
    }

    private static boolean isIndonesianLabel(String label) {
        if (label == null) return false;
        String l = label.toLowerCase();
        return l.contains("[ind,") || l.contains("[ind ") || l.contains("(ind)") ||
                l.contains("indonesian") || l.contains("bahasa indonesia") ||
                l.contains("cr [ind") || l.contains("[may,") || l.contains("malay");
    }

    private static String cleanAnimeTitle(String title) {
        if (title == null) return "";
        String t = title.replaceAll("(?i)\\b(season\\s*\\d+|\\d+(?:nd|rd|th|st)\\s*season|part\\s*\\d+|cour\\s*\\d+|s\\d+)\\b", "");
        t = t.replaceAll("\\([^)]*\\)", " ");
        t = t.replaceAll("\\[[^\\]]*\\]", " ");
        return t.replaceAll("\\s+", " ").trim();
    }
}
