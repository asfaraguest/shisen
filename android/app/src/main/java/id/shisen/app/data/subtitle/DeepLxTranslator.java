package id.shisen.app.data.subtitle;

import android.content.Context;
import id.shisen.app.data.service.HttpClientProvider;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DeepLxTranslator - Penerjemah Subtitle DeepLX Cerdas dengan Mirror Failover,
 * Progressive Lazy Loading untuk Movie, Sanitasi Iklan, dan Auto-Failover ke GTX.
 */
public class DeepLxTranslator {

    public interface DeepLxCallback {
        void onProgress(int progress, int total);
        void onInitialChunkReady(List<SubtitleCue> initialCues);
        void onSuccess(List<SubtitleCue> fullCues);
        void onError(String error);
    }

    private static final int BATCH_SIZE = 25;
    private static final int MOVIE_INITIAL_BATCH_SIZE = 150;
    private static final Pattern P_TAG_RE = Pattern.compile("<p\\s+id=[\"']?\\s*(\\d+)\\s*[\"']?\\s*>(.*?)</p>", Pattern.DOTALL);
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    private static final String[] DEEPLX_MIRRORS = {
            "https://deeplx.owo.network/translate",
            "https://api.deeplx.org/translate",
            "https://deeplx.run/translate"
    };

    /**
     * Penerjemahan reguler untuk episode anime serial (~300-450 cue).
     */
    public static void translateAsync(Context context,
                                      String sourceIdentifier,
                                      List<SubtitleCue> cues,
                                      boolean forceRefresh,
                                      DeepLxCallback callback) {
        if (cues == null || cues.isEmpty()) {
            if (callback != null) callback.onError("Tidak ada cue subtitle untuk diterjemahkan");
            return;
        }

        new Thread(() -> {
            try {
                // 1. Cek Cache Lokal
                String cacheKey = sha1(sourceIdentifier + "_deeplx_id_v1");
                File cacheDir = new File(context.getCacheDir(), "sub_cache");
                if (!cacheDir.exists()) cacheDir.mkdirs();
                File cacheFile = new File(cacheDir, cacheKey + ".json");

                if (!forceRefresh && cacheFile.exists() && cacheFile.length() > 0) {
                    try {
                        String jsonStr = readFile(cacheFile);
                        List<SubtitleCue> cachedCues = deserializeCues(jsonStr);
                        if (!cachedCues.isEmpty()) {
                            if (callback != null) callback.onSuccess(cachedCues);
                            return;
                        }
                    } catch (Exception ignored) {}
                }

                OkHttpClient client = HttpClientProvider.getClient();
                List<SubtitleCue> translated = new ArrayList<>(cues.size());
                int totalCues = cues.size();

                for (int startIdx = 0; startIdx < totalCues; startIdx += BATCH_SIZE) {
                    int endIdx = Math.min(startIdx + BATCH_SIZE, totalCues);
                    List<SubtitleCue> batch = cues.subList(startIdx, endIdx);

                    List<String> translatedBatchTexts = translateBatchWithFallback(context, client, batch);
                    for (int j = 0; j < batch.size(); j++) {
                        SubtitleCue orig = batch.get(j);
                        String trText = (j < translatedBatchTexts.size() && !translatedBatchTexts.get(j).isEmpty())
                                ? translatedBatchTexts.get(j)
                                : orig.text;
                        translated.add(new SubtitleCue(orig.startMs, orig.endMs, trText));
                    }

                    if (callback != null) {
                        callback.onProgress(translated.size(), totalCues);
                    }
                }

                // Sanitasi iklan & haluskan leksikon bahasa lisan
                List<SubtitleCue> sanitizedCues = SmartLexicon.sanitizeCuesWithContext(translated, "gtx+", 3, 15.0);
                List<SubtitleCue> cleanCues = SmartLexicon.sanitizeAdsAndWatermarks(sanitizedCues);

                // Simpan ke Cache
                try {
                    String jsonOut = serializeCues(cleanCues);
                    writeFile(cacheFile, jsonOut);
                } catch (Exception ignored) {}

                if (callback != null) {
                    callback.onSuccess(cleanCues);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Gagal menerjemahkan subtitle DeepLX: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Lazy translation untuk film anime Movie panjang (>800 cue).
     * Menerjemahkan 150 cue awal dulu (~15 menit pertama) agar video langsung berputar,
     * kemudian menyelesaikan sisa cue secara bertahap di background thread dengan jeda aman.
     */
    public static void translateMovieLazyAsync(Context context,
                                               String sourceIdentifier,
                                               List<SubtitleCue> cues,
                                               boolean forceRefresh,
                                               DeepLxCallback callback) {
        if (cues == null || cues.isEmpty()) {
            if (callback != null) callback.onError("Tidak ada cue subtitle untuk diterjemahkan");
            return;
        }

        new Thread(() -> {
            try {
                // 1. Cek Cache Lokal Full Movie
                String cacheKey = sha1(sourceIdentifier + "_deeplx_id_v1");
                File cacheDir = new File(context.getCacheDir(), "sub_cache");
                if (!cacheDir.exists()) cacheDir.mkdirs();
                File cacheFile = new File(cacheDir, cacheKey + ".json");

                if (!forceRefresh && cacheFile.exists() && cacheFile.length() > 0) {
                    try {
                        String jsonStr = readFile(cacheFile);
                        List<SubtitleCue> cachedCues = deserializeCues(jsonStr);
                        if (!cachedCues.isEmpty()) {
                            if (callback != null) {
                                callback.onInitialChunkReady(cachedCues);
                                callback.onSuccess(cachedCues);
                            }
                            return;
                        }
                    } catch (Exception ignored) {}
                }

                OkHttpClient client = HttpClientProvider.getClient();
                int totalCues = cues.size();
                int initialCutoff = Math.min(MOVIE_INITIAL_BATCH_SIZE, totalCues);

                List<SubtitleCue> initialCuesList = new ArrayList<>(initialCutoff);
                List<SubtitleCue> fullCuesList = new ArrayList<>(totalCues);

                // Tahap 1: Terjemahkan blok awal (150 cue)
                for (int startIdx = 0; startIdx < initialCutoff; startIdx += BATCH_SIZE) {
                    int endIdx = Math.min(startIdx + BATCH_SIZE, initialCutoff);
                    List<SubtitleCue> batch = cues.subList(startIdx, endIdx);

                    List<String> translatedTexts = translateBatchWithFallback(context, client, batch);
                    for (int j = 0; j < batch.size(); j++) {
                        SubtitleCue orig = batch.get(j);
                        String trText = (j < translatedTexts.size() && !translatedTexts.get(j).isEmpty())
                                ? translatedTexts.get(j)
                                : orig.text;
                        SubtitleCue cue = new SubtitleCue(orig.startMs, orig.endMs, trText);
                        initialCuesList.add(cue);
                        fullCuesList.add(cue);
                    }
                }

                // Sanitasi blok awal dan serahkan ke player agar video langsung bisa ditonton!
                List<SubtitleCue> cleanInitial = SmartLexicon.sanitizeAdsAndWatermarks(
                        SmartLexicon.sanitizeCuesWithContext(initialCuesList, "gtx+", 3, 15.0)
                );
                if (callback != null) {
                    callback.onInitialChunkReady(cleanInitial);
                }

                // Tahap 2: Selesaikan sisa film di background thread dengan jeda mikro (1200ms) anti-429
                for (int startIdx = initialCutoff; startIdx < totalCues; startIdx += BATCH_SIZE) {
                    try {
                        Thread.sleep(1200); // Throttling jitter aman
                    } catch (InterruptedException ignored) {}

                    int endIdx = Math.min(startIdx + BATCH_SIZE, totalCues);
                    List<SubtitleCue> batch = cues.subList(startIdx, endIdx);

                    List<String> translatedTexts = translateBatchWithFallback(context, client, batch);
                    for (int j = 0; j < batch.size(); j++) {
                        SubtitleCue orig = batch.get(j);
                        String trText = (j < translatedTexts.size() && !translatedTexts.get(j).isEmpty())
                                ? translatedTexts.get(j)
                                : orig.text;
                        fullCuesList.add(new SubtitleCue(orig.startMs, orig.endMs, trText));
                    }

                    if (callback != null) {
                        callback.onProgress(fullCuesList.size(), totalCues);
                    }
                }

                // Sanitasi menyeluruh & simpan cache
                List<SubtitleCue> finalFullClean = SmartLexicon.sanitizeAdsAndWatermarks(
                        SmartLexicon.sanitizeCuesWithContext(fullCuesList, "gtx+", 3, 15.0)
                );
                try {
                    String jsonOut = serializeCues(finalFullClean);
                    writeFile(cacheFile, jsonOut);
                } catch (Exception ignored) {}

                if (callback != null) {
                    callback.onSuccess(finalFullClean);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Gagal menerjemahkan lazy movie: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Menerjemahkan 1 batch teks. Mencoba mirror DeepLX dahulu; jika gagal/429 langsung fallback ke GTX.
     */
    private static List<String> translateBatchWithFallback(Context context, OkHttpClient client, List<SubtitleCue> batch) {
        List<String> result = translateBatchDeepLx(client, batch);
        if (result != null && !result.isEmpty()) {
            return result;
        }

        // DeepLX gagal / terkena limit -> Cascade Failover otomatis ke GTX
        try {
            return GtxTranslator.translateBatchSync(context, batch);
        } catch (Exception e) {
            List<String> fallback = new ArrayList<>(batch.size());
            for (SubtitleCue c : batch) {
                fallback.add(c.text);
            }
            return fallback;
        }
    }

    private static List<String> translateBatchDeepLx(OkHttpClient client, List<SubtitleCue> batch) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            String text = SmartLexicon.preprocessEnglishForGtx(batch.get(i).text);
            String escaped = text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br>");
            joined.append("<p id=\"").append(i).append("\">").append(escaped).append("</p>");
        }

        for (String endpoint : DEEPLX_MIRRORS) {
            try {
                JSONObject jsonReq = new JSONObject();
                jsonReq.put("text", joined.toString());
                jsonReq.put("source_lang", "EN");
                jsonReq.put("target_lang", "ID");

                RequestBody body = RequestBody.create(jsonReq.toString(), JSON_MEDIA);
                Request request = new Request.Builder()
                        .url(endpoint)
                        .post(body)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String respStr = response.body().string();
                        JSONObject jsonResp = new JSONObject(respStr);
                        int code = jsonResp.optInt("code", 0);
                        String rawData = jsonResp.optString("data", "");

                        if (code == 200 && !rawData.isEmpty()) {
                            Matcher m = P_TAG_RE.matcher(rawData);
                            Map<Integer, String> map = new HashMap<>();
                            while (m.find()) {
                                try {
                                    int id = Integer.parseInt(m.group(1));
                                    String text = m.group(2)
                                            .replace("<br>", "\n")
                                            .replace("&gt;", ">")
                                            .replace("&lt;", "<")
                                            .replace("&amp;", "&");
                                    map.put(id, text.trim());
                                } catch (Exception ignored) {}
                            }

                            if (!map.isEmpty()) {
                                List<String> out = new ArrayList<>(batch.size());
                                for (int i = 0; i < batch.size(); i++) {
                                    out.add(map.getOrDefault(i, batch.get(i).text));
                                }
                                return out;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Coba mirror berikutnya
            }
        }
        return null;
    }

    private static String serializeCues(List<SubtitleCue> cues) {
        JSONArray arr = new JSONArray();
        for (SubtitleCue c : cues) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("s", c.startMs);
                obj.put("e", c.endMs);
                obj.put("t", c.text);
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        return arr.toString();
    }

    private static List<SubtitleCue> deserializeCues(String json) {
        List<SubtitleCue> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new SubtitleCue(obj.getLong("s"), obj.getLong("e"), obj.getString("t")));
            }
        } catch (Exception ignored) {}
        return list;
    }

    private static String sha1(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }

    private static String readFile(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int read = fis.read(data);
            return new String(data, 0, read, StandardCharsets.UTF_8);
        }
    }

    private static void writeFile(File file, String content) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
