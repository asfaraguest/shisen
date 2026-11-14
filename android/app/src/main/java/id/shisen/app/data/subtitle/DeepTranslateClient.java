package id.shisen.app.data.subtitle;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import id.shisen.app.data.service.HttpClientProvider;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * DeepTranslateClient - Client penerjemah cerdas berbasis DeepTranslate.ai (Model GPT-4.1-nano).
 * Menyediakan subtitle AI fansub luwes secara zero-config (gratis tanpa key),
 * dengan preservasi format SRT, sanitasi iklan, dan fallback otomatis ke Google Translate (GTX).
 */
public class DeepTranslateClient {

    private static final String API_URL = "https://deeptranslate.ai/api/text-translator";
    private static final String MODEL_ID = "ai:3"; // GPT-4.1-nano
    private static final int BATCH_SIZE = 20;

    private static final String PROMPT_PREFIX =
            "[Petunjuk: Terjemahkan dialog English anime berikut ke bahasa Indonesia fansub santai dan luwes " +
            "(gunakan aku/kamu, gaya percakapan santai, jangan kaku ala mesin penerjemah). " +
            "Pertahankan nomor urut dan format timestamp SRT persis seperti aslinya.]\n\n";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient HTTP_CLIENT = HttpClientProvider.getClient().newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static final Pattern SRT_BLOCK_PATTERN = Pattern.compile(
            "(\\d+)\\s*\\r?\\n(\\d{2}:\\d{2}:\\d{2}[,\\.]\\d{3})\\s*-->\\s*(\\d{2}:\\d{2}:\\d{2}[,\\.]\\d{3})\\s*\\r?\\n([\\s\\S]*?)(?=(?:\\r?\\n\\s*\\d+\\s*\\r?\\n\\d{2}:\\d{2}:\\d{2}|$))",
            Pattern.MULTILINE
    );

    public interface Callback {
        void onProgress(int progress, int total);
        void onSuccess(List<SubtitleCue> translatedCues);
        void onError(String error);
    }

    public static void translateAsync(Context context, String sourceUrl, List<SubtitleCue> cues, boolean forceRefresh, Callback callback) {
        translateAsync(context, sourceUrl, cues, forceRefresh, null, callback);
    }

    public static void translateAsync(Context context, String sourceUrl, List<SubtitleCue> cues, boolean forceRefresh,
                                      java.util.function.BooleanSupplier isCancelled, Callback callback) {
        if (cues == null || cues.isEmpty()) {
            if (callback != null) callback.onError("Daftar subtitle kosong");
            return;
        }

        EXECUTOR.execute(() -> {
            if (isCancelled != null && isCancelled.getAsBoolean()) return;

            String cacheKey = generateCacheKey(sourceUrl, cues);
            File cacheFile = getCacheFile(context, cacheKey);

            if (!forceRefresh && cacheFile != null && cacheFile.exists() && cacheFile.length() > 0) {
                List<SubtitleCue> cached = loadFromDiskCache(cacheFile);
                if (cached != null && !cached.isEmpty() && !AiSubtitleService.containsDominantArabicScript(cached)) {
                    List<SubtitleCue> sanitized = finalizeCues(cached);
                    postSuccess(callback, sanitized);
                    return;
                } else if (cached != null && !cached.isEmpty()) {
                    cacheFile.delete();
                }
            }

            String errMsg = "DeepTranslate gagal menerjemahkan subtitle";
            try {
                List<SubtitleCue> translated = translateBatchesSync(cues, isCancelled, callback);
                if (isCancelled != null && isCancelled.getAsBoolean()) return;
                if (translated != null && !translated.isEmpty()) {
                    List<SubtitleCue> finalized = finalizeCues(translated);
                    if (cacheFile != null && !AiSubtitleService.containsDominantArabicScript(finalized)) {
                        saveToDiskCache(cacheFile, finalized);
                    }
                    postSuccess(callback, finalized);
                    return;
                }
            } catch (Exception e) {
                if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    errMsg = e.getMessage();
                }
            }

            if (isCancelled == null || !isCancelled.getAsBoolean()) {
                if (callback != null) callback.onError(errMsg);
            }
        });
    }

    private static List<SubtitleCue> translateBatchesSync(List<SubtitleCue> cues,
                                                          java.util.function.BooleanSupplier isCancelled,
                                                          Callback callback) throws Exception {
        int total = cues.size();
        List<SubtitleCue> result = new ArrayList<>(total);

        for (int i = 0; i < total; i += BATCH_SIZE) {
            if (isCancelled != null && isCancelled.getAsBoolean()) {
                return null;
            }
            int end = Math.min(i + BATCH_SIZE, total);
            List<SubtitleCue> batch = cues.subList(i, end);

            List<SubtitleCue> translatedBatch = translateSingleBatch(batch, i + 1);
            if (translatedBatch == null || translatedBatch.size() != batch.size()) {
                // Jika satu batch gagal atau jumlah cue tidak cocok, lempar Exception agar failover ke GTX
                throw new IOException("Gagal menerjemahkan batch cue DeepTranslate");
            }
            result.addAll(translatedBatch);

            if (callback != null && (isCancelled == null || !isCancelled.getAsBoolean())) {
                int progress = Math.min(end, total);
                callback.onProgress(progress, total);
            }

            // Jeda aman 200ms antar batch
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {}
        }

        return result;
    }

    private static List<SubtitleCue> translateSingleBatch(List<SubtitleCue> batch, int startIndex) throws Exception {
        String srtInput = buildSrtString(batch, startIndex);
        String textPayload = PROMPT_PREFIX + srtInput;

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("modelId", MODEL_ID);
        bodyJson.put("sourceLanguage", "en");
        bodyJson.put("targetLanguage", "id");
        bodyJson.put("text", textPayload);

        RequestBody body = RequestBody.create(bodyJson.toString(), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://deeptranslate.ai/translate")
                .header("Origin", "https://deeptranslate.ai")
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("HTTP error: " + response.code());
            }

            String respStr = response.body().string();
            JSONObject resJson = new JSONObject(respStr);
            String translationText = resJson.optString("translation", "");
            if (translationText.isEmpty()) {
                throw new IOException("Empty translation output");
            }

            return parseSrtTranslation(batch, translationText);
        }
    }

    public static String buildSrtString(List<SubtitleCue> cues, int startIndex) {
        StringBuilder sb = new StringBuilder();
        int idx = startIndex;
        for (SubtitleCue c : cues) {
            sb.append(idx).append("\n");
            sb.append(formatSrtTime(c.startMs)).append(" --> ").append(formatSrtTime(c.endMs)).append("\n");
            sb.append(c.text.trim()).append("\n\n");
            idx++;
        }
        return sb.toString();
    }

    public static List<SubtitleCue> parseSrtTranslation(List<SubtitleCue> originalBatch, String srtOutput) {
        if (srtOutput == null || srtOutput.isEmpty()) return null;

        // Bersihkan header prompt yang mungkin ikut tercetak di bagian awal
        String cleanText = srtOutput;
        int promptEnd = cleanText.indexOf("]\n");
        if (promptEnd != -1 && cleanText.startsWith("[")) {
            cleanText = cleanText.substring(promptEnd + 2).trim();
        }

        Matcher m = SRT_BLOCK_PATTERN.matcher(cleanText);
        List<String> translatedTexts = new ArrayList<>();
        while (m.find()) {
            String text = m.group(4);
            if (text != null) {
                translatedTexts.add(text.trim());
            }
        }

        if (translatedTexts.size() != originalBatch.size()) {
            return null;
        }

        List<SubtitleCue> result = new ArrayList<>(originalBatch.size());
        for (int i = 0; i < originalBatch.size(); i++) {
            SubtitleCue orig = originalBatch.get(i);
            String tr = translatedTexts.get(i);
            result.add(new SubtitleCue(orig.startMs, orig.endMs, tr));
        }
        return result;
    }

    private static List<SubtitleCue> finalizeCues(List<SubtitleCue> cues) {
        List<SubtitleCue> sanitized = SmartLexicon.sanitizeAdsAndWatermarks(cues);
        if (sanitized.isEmpty()) sanitized = cues;

        List<SubtitleCue> out = new ArrayList<>(sanitized.size());
        for (SubtitleCue c : sanitized) {
            String polished = SmartLexicon.applyUniversalLexicon(c.text);
            out.add(new SubtitleCue(c.startMs, c.endMs, polished));
        }
        return out;
    }

    private static String formatSrtTime(long ms) {
        long h = ms / 3600000;
        long m = (ms % 3600000) / 60000;
        long s = (ms % 60000) / 1000;
        long msec = ms % 1000;
        return String.format("%02d:%02d:%02d,%03d", h, m, s, msec);
    }

    private static void postSuccess(Callback callback, List<SubtitleCue> cues) {
        if (callback != null) {
            callback.onSuccess(cues);
        }
    }

    private static String generateCacheKey(String sourceUrl, List<SubtitleCue> cues) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (sourceUrl != null) md.update(sourceUrl.getBytes(StandardCharsets.UTF_8));
            md.update(String.valueOf(cues.size()).getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder("deeptranslate_");
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "deeptranslate_" + (sourceUrl != null ? sourceUrl.hashCode() : cues.size());
        }
    }

    private static File getCacheFile(Context context, String key) {
        if (context == null) return null;
        File dir = new File(context.getCacheDir(), "sub_deeptranslate");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, key + ".cache");
    }

    private static void saveToDiskCache(File file, List<SubtitleCue> cues) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (SubtitleCue c : cues) {
                String line = c.startMs + "|" + c.endMs + "|" + c.text.replace("\n", "\\n") + "\n";
                fos.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }

    private static List<SubtitleCue> loadFromDiskCache(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream((int) Math.min(file.length(), 65536))) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = fis.read(buf)) != -1) {
                bos.write(buf, 0, read);
            }
            String content = bos.toString("UTF-8");
            String[] lines = content.split("\n");
            List<SubtitleCue> cues = new ArrayList<>();
            for (String l : lines) {
                if (l.trim().isEmpty()) continue;
                String[] parts = l.split("\\|", 3);
                if (parts.length == 3) {
                    long start = Long.parseLong(parts[0]);
                    long end = Long.parseLong(parts[1]);
                    String text = parts[2].replace("\\n", "\n");
                    cues.add(new SubtitleCue(start, end, text));
                }
            }
            return cues;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
