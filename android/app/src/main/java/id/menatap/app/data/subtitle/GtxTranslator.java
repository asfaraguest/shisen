package id.menatap.app.data.subtitle;

import android.content.Context;
import android.text.Html;
import id.menatap.app.data.service.HttpClientProvider;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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

public class GtxTranslator {

    public interface TranslateCallback {
        void onProgress(int progress, int total);
        void onSuccess(List<SubtitleCue> translatedCues);
        void onError(String error);
    }

    private static final int BATCH_SIZE = 25;
    private static final Pattern P_TAG_RE = Pattern.compile("<p\\s+id=[\"']?\\s*(\\d+)\\s*[\"']?\\s*>(.*?)</p>", Pattern.DOTALL);

    private static final String[] ENDPOINTS = {
            "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl=en&tl=id",
            "https://clients5.google.com/translate_a/t?client=tw-ob&sl=en&tl=id",
            "https://translate.google.com/translate_a/t?client=dict-chrome-ex&sl=en&tl=id",
            "https://translate.googleapis.com/translate_a/single?client=dict-chrome-ex&sl=en&tl=id&dt=t"
    };

    public static void translateAsync(Context context,
                                      String sourceIdentifier,
                                      List<SubtitleCue> cues,
                                      boolean forceRefresh,
                                      TranslateCallback callback) {
        if (cues == null || cues.isEmpty()) {
            if (callback != null) callback.onError("Tidak ada cue subtitle untuk diterjemahkan");
            return;
        }

        new Thread(() -> {
            try {
                // 1. Cek Cache Lokal di Disk
                String cacheKey = sha1(sourceIdentifier + "_gtx_id");
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

                    List<String> translatedBatchTexts = translateBatch(client, batch);
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

                // Terapkan Sliding Window Scene Context Sanitizer (Kamus Leksikon Universal, Overgeared Satisfy, & Scene Context)
                List<SubtitleCue> sanitizedCues = SmartLexicon.sanitizeCuesWithContext(translated, "gtx+", 3, 15.0);

                // 2. Simpan Hasil ke Disk Cache
                try {
                    String jsonOut = serializeCues(sanitizedCues);
                    writeFile(cacheFile, jsonOut);
                } catch (Exception ignored) {}

                if (callback != null) {
                    callback.onSuccess(sanitizedCues);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Gagal menerjemahkan subtitle: " + e.getMessage());
                }
            }
        }).start();
    }

    private static List<String> translateBatch(OkHttpClient client, List<SubtitleCue> batch) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            String text = batch.get(i).text;
            String escaped = text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br>");
            joined.append("<p id=\"").append(i).append("\">").append(escaped).append("</p>");
        }

        for (String endpoint : ENDPOINTS) {
            try {
                FormBody formBody = new FormBody.Builder()
                        .add("q", joined.toString())
                        .build();

                Request request = new Request.Builder()
                        .url(endpoint)
                        .post(formBody)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String respBody = response.body().string();
                        String rawOut = extractJsonPayload(respBody);

                        Matcher m = P_TAG_RE.matcher(rawOut);
                        Map<Integer, String> map = new HashMap<>();
                        while (m.find()) {
                            try {
                                int id = Integer.parseInt(m.group(1));
                                String val = m.group(2).replace("<br>", "\n").replace("<br/>", "\n");
                                val = Html.fromHtml(val, Html.FROM_HTML_MODE_LEGACY).toString().trim();
                                map.put(id, val);
                            } catch (Exception ignored) {}
                        }

                        if (!map.isEmpty()) {
                            List<String> res = new ArrayList<>();
                            for (int i = 0; i < batch.size(); i++) {
                                String text = map.get(i);
                                res.add(text != null && !text.isEmpty() ? text : batch.get(i).text);
                            }
                            return res;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Fallback jika Google offline: kembalikan teks asli
        List<String> fallback = new ArrayList<>();
        for (SubtitleCue c : batch) {
            fallback.add(c.text);
        }
        return fallback;
    }

    private static String extractJsonPayload(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) return "";
        String trimmed = jsonStr.trim();
        if (trimmed.startsWith("[")) {
            try {
                JSONArray arr = new JSONArray(trimmed);
                StringBuilder sb = new StringBuilder();
                if (arr.length() > 0 && arr.get(0) instanceof JSONArray) {
                    JSONArray inner = arr.getJSONArray(0);
                    for (int i = 0; i < inner.length(); i++) {
                        Object obj = inner.get(i);
                        if (obj instanceof JSONArray) {
                            JSONArray item = (JSONArray) obj;
                            if (item.length() > 0) sb.append(item.optString(0, ""));
                        }
                    }
                } else {
                    for (int i = 0; i < arr.length(); i++) {
                        sb.append(arr.optString(i, ""));
                    }
                }
                return sb.toString();
            } catch (Exception ignored) {}
        }
        return jsonStr;
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

    private static String readFile(File f) throws Exception {
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] data = new byte[(int) f.length()];
            fis.read(data);
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private static void writeFile(File f, String content) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
