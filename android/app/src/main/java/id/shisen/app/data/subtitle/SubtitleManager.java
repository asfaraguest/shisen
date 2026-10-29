package id.shisen.app.data.subtitle;

import android.content.Context;
import id.shisen.app.data.model.SubtitleTrack;
import id.shisen.app.data.service.HttpClientProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubtitleManager {

    public interface SubtitleStateListener {
        void onSubtitleLoaded(String label, int count);
        void onSubtitleLoading(String message);
        void onSubtitleError(String error);
    }

    private final Context context;
    private SubtitleStateListener listener;

    private volatile List<SubtitleCue> activeCues = new ArrayList<>();
    private volatile List<SubtitleCue> sourceEnglishCues = new ArrayList<>();

    private boolean subEnabled = true;
    private String activeLanguage = "id"; // "id" | "en" | ...
    private String activeEngine = "smart_merge"; // "smart_merge" | "raw" | "custom"
    private long subtitleOffsetMs = 0L;
    private String activeLabel = "Bahasa Indonesia";

    private int currentLoadId = 0;

    public SubtitleManager(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    public void setListener(SubtitleStateListener listener) {
        this.listener = listener;
    }

    public boolean isSubEnabled() {
        return subEnabled;
    }

    public void setSubEnabled(boolean enabled) {
        this.subEnabled = enabled;
        if (!enabled) {
            synchronized (this) {
                ++currentLoadId;
                clearCues();
            }
        }
    }

    public String getActiveLanguage() {
        return activeLanguage;
    }

    public String getActiveEngine() {
        return activeEngine;
    }

    public String getActiveLabel() {
        return activeLabel;
    }

    /**
     * Set mode subtitle secara eksplisit sebelum load.
     * Dipanggil oleh PlayerActivity saat user pilih opsi subtitle.
     */
    public void setMode(String language, String engine) {
        this.activeLanguage = language;
        this.activeEngine = engine;
    }

    public long getSubtitleOffsetMs() {
        return subtitleOffsetMs;
    }

    public void setSubtitleOffsetMs(long offsetMs) {
        this.subtitleOffsetMs = offsetMs;
    }

    public synchronized int getCurrentLoadId() {
        return currentLoadId;
    }

    public synchronized int getActiveCuesCount() {
        return activeCues != null ? activeCues.size() : 0;
    }

    public synchronized void clearCues() {
        activeCues = new ArrayList<>();
        sourceEnglishCues = new ArrayList<>();
        lastCueIndex = -1;
    }

    /**
     * Pointer cue terakhir yang terlihat (urutan cue terjamin sorted oleh SubtitleParser).
     * Hit aktif berikutnya memakai pointer; kalau tidak aktif kita binary startMs (O(log n)).
     */
    private int lastCueIndex = -1;

    public synchronized String getCurrentText(long currentPositionMs) {
        if (!subEnabled) return "";
        List<SubtitleCue> cues = activeCues;
        if (cues == null || cues.isEmpty()) return "";

        long effectivePos = currentPositionMs + subtitleOffsetMs;
        int size = cues.size();

        // Binary search: cue terakhir dengan startMs <= pos
        int lo = 0, hi = size - 1, idx = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            SubtitleCue c = cues.get(mid);
            if (c == null) { lo++; continue; }
            if (c.startMs <= effectivePos) { idx = mid; lo = mid + 1; }
            else { hi = mid - 1; }
        }
        lastCueIndex = idx;
        if (idx < 0) return "";

        List<String> activeTexts = new ArrayList<>();
        // Periksa mundur dari idx untuk mengumpulkan semua cue yang masih aktif pada effectivePos
        for (int i = idx; i >= 0; i--) {
            SubtitleCue c = cues.get(i);
            if (c == null) continue;
            if (c.isActive(effectivePos)) {
                if (c.text != null && !c.text.isEmpty() && !activeTexts.contains(c.text)) {
                    activeTexts.add(0, c.text); // Pertahankan urutan kronologis
                }
            }
            // Batasi pengecekan mundur jika startMs sudah lebih dari 2 menit yang lalu
            if (effectivePos - c.startMs > 120_000) {
                break;
            }
        }

        if (activeTexts.isEmpty()) return "";
        if (activeTexts.size() == 1) return activeTexts.get(0);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < activeTexts.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(activeTexts.get(i));
        }
        return sb.toString();
    }

    public void loadFromTracks(List<SubtitleTrack> tracks, String title, int episode) {
        loadFromTracks(tracks, title, episode, false);
    }

    public void loadFromTracks(List<SubtitleTrack> tracks, String title, int episode, boolean forceRefresh) {
        if (tracks == null || tracks.isEmpty()) {
            if (listener != null) listener.onSubtitleLoading("Tidak ada subtitle bawaan");
            return;
        }

        final SubtitleTrack sourceEn = SubtitleTrack.findEnglishTrack(tracks);
        if (sourceEn == null) {
            if (listener != null) listener.onSubtitleLoading("Tidak ada subtitle bawaan");
            return;
        }
        final int loadId = ++currentLoadId;
        synchronized (this) {
            activeLanguage = "id";
            activeEngine = "smart_merge";
            clearCues();
        }

        if (listener != null) listener.onSubtitleLoading("Menyiapkan subtitle...");

        new Thread(() -> {
            List<SubtitleCue> enCues = new ArrayList<>();
            try {
                enCues = fetchCuesFromUrl(sourceEn.url, sourceEn.referer);
                if (loadId != currentLoadId) return;

                if (!enCues.isEmpty()) {
                    synchronized (SubtitleManager.this) {
                        sourceEnglishCues = enCues;
                        // Tampilkan loading indicator, jangan English mentah
                        if (activeCues.isEmpty()) {
                            activeLabel = "Menerjemahkan ke Bahasa Indonesia...";
                            if (listener != null) listener.onSubtitleLoading(activeLabel);
                        }
                    }
                }
            } catch (Exception ignored) {
                // Jangan fatal jika download English source gagal! Lanjutkan pencarian fansub Indonesia!
            }

            final List<SubtitleCue> finalEnCues = enCues;

            // Coba cari rilis subtitle Indonesia dari AnimeTosho (Tier 1)
            AnimeToshoClient.searchIndonesianAsync(title, episode, new AnimeToshoClient.SearchCallback() {
                @Override
                public void onSuccess(List<AnimeToshoClient.SubtitleSearchResult> results) {
                    if (loadId != currentLoadId) return;
                    if (!results.isEmpty()) {
                        // Ditemukan rilis fansub Indonesia di AnimeTosho! Unduh dan merge
                        AnimeToshoClient.downloadAndParseAsync(results.get(0).downloadUrl, new AnimeToshoClient.DownloadCallback() {
                            @Override
                            public void onSuccess(List<SubtitleCue> atCues) {
                                if (loadId != currentLoadId) return;
                                List<SubtitleCue> cleanedCues;
                                if (!finalEnCues.isEmpty()) {
                                    long offset = SubtitleMergeEngine.estimateOptimalOffset(finalEnCues, atCues);

                                    // Terjemahkan gap cues (Signs/OP/ED) sebelum merge
                                    // Ref: main.py#L2834-L2841
                                    Map<Integer, String> translatedGaps = new HashMap<>();
                                    List<int[]> unmatched = SubtitleMergeEngine.findUnmatchedBuiltinIndices(finalEnCues, atCues, offset);
                                    if (!unmatched.isEmpty() && unmatched.size() <= 180) {
                                        List<SubtitleCue> gapCues = new ArrayList<>();
                                        List<Integer> gapIndices = new ArrayList<>();
                                        for (int[] pair : unmatched) {
                                            gapIndices.add(pair[0]);
                                            gapCues.add(finalEnCues.get(pair[0]));
                                        }
                                        List<String> trTexts = GtxTranslator.translateBatchSync(context, gapCues);
                                        for (int g = 0; g < gapIndices.size(); g++) {
                                            if (g < trTexts.size() && !trTexts.get(g).equals(gapCues.get(g).text)) {
                                                translatedGaps.put(gapIndices.get(g), trTexts.get(g));
                                            }
                                        }
                                    }

                                    SubtitleMergeEngine.MergeResult mr = SubtitleMergeEngine.merge(finalEnCues, atCues, offset, translatedGaps);
                                    cleanedCues = new ArrayList<>(mr.mergedCues.size());
                                    for (SubtitleCue c : mr.mergedCues) {
                                        cleanedCues.add(new SubtitleCue(c.startMs, c.endMs, SmartLexicon.applyUniversalLexicon(c.text)));
                                    }
                                } else {
                                    cleanedCues = new ArrayList<>(atCues.size());
                                    for (SubtitleCue c : atCues) {
                                        cleanedCues.add(new SubtitleCue(c.startMs, c.endMs, SmartLexicon.applyUniversalLexicon(c.text)));
                                    }
                                }

                                synchronized (SubtitleManager.this) {
                                    activeCues = cleanedCues;
                                    lastCueIndex = -1;
                                    activeLabel = "Bahasa Indonesia [Fansub & Merge]";
                                    activeLanguage = "id";
                                    activeEngine = "smart_merge";
                                    if (listener != null) listener.onSubtitleLoaded(activeLabel, activeCues.size());
                                }
                            }

                            @Override
                            public void onError(String error) {
                                // Fallback ke OpenSubtitles (Tier 2)
                                runOpenSubtitlesFallback(finalEnCues, title, episode, sourceEn.url, sourceEn.referer, loadId, forceRefresh);
                            }
                        });
                    } else {
                        // Tidak ada di AnimeTosho -> fallback ke OpenSubtitles (Tier 2)
                        runOpenSubtitlesFallback(finalEnCues, title, episode, sourceEn.url, sourceEn.referer, loadId, forceRefresh);
                    }
                }

                @Override
                public void onError(String error) {
                    // Fallback ke OpenSubtitles (Tier 2)
                    runOpenSubtitlesFallback(finalEnCues, title, episode, sourceEn.url, sourceEn.referer, loadId, forceRefresh);
                }
            });
        }).start();
    }

    private void runOpenSubtitlesFallback(List<SubtitleCue> enCues, String title, int episode, String sourceUrl, String sourceReferer, int loadId, boolean forceRefresh) {
        if (listener != null) listener.onSubtitleLoading("Mencari di OpenSubtitles...");
        OpenSubtitlesClient.searchIndonesianAsync(title, episode, new OpenSubtitlesClient.SearchCallback() {
            @Override
            public void onSuccess(List<OpenSubtitlesClient.SubtitleSearchResult> results) {
                if (loadId != currentLoadId) return;
                if (!results.isEmpty()) {
                    OpenSubtitlesClient.downloadAndParseAsync(results.get(0).downloadUrl, new OpenSubtitlesClient.DownloadCallback() {
                        @Override
                        public void onSuccess(List<SubtitleCue> osCues) {
                            if (loadId != currentLoadId) return;
                            List<SubtitleCue> cleanedCues;
                            if (!enCues.isEmpty()) {
                                long offset = SubtitleMergeEngine.estimateOptimalOffset(enCues, osCues);

                                // Terjemahkan gap cues (Signs/OP/ED) sebelum merge
                                Map<Integer, String> translatedGaps = new HashMap<>();
                                List<int[]> unmatched = SubtitleMergeEngine.findUnmatchedBuiltinIndices(enCues, osCues, offset);
                                if (!unmatched.isEmpty() && unmatched.size() <= 180) {
                                    List<SubtitleCue> gapCues = new ArrayList<>();
                                    List<Integer> gapIndices = new ArrayList<>();
                                    for (int[] pair : unmatched) {
                                        gapIndices.add(pair[0]);
                                        gapCues.add(enCues.get(pair[0]));
                                    }
                                    List<String> trTexts = GtxTranslator.translateBatchSync(context, gapCues);
                                    for (int g = 0; g < gapIndices.size(); g++) {
                                        if (g < trTexts.size() && !trTexts.get(g).equals(gapCues.get(g).text)) {
                                            translatedGaps.put(gapIndices.get(g), trTexts.get(g));
                                        }
                                    }
                                }

                                SubtitleMergeEngine.MergeResult mr = SubtitleMergeEngine.merge(enCues, osCues, offset, translatedGaps);

                                // Otomasi kata ganti OpenSubtitles berdasarkan konteks adegan
                                List<SubtitleCue> pronounAdjusted = SmartLexicon.automateOpenSubtitlesPronouns(mr.mergedCues, "id", 2, 10.0);
                                cleanedCues = new ArrayList<>(pronounAdjusted.size());
                                for (SubtitleCue c : pronounAdjusted) {
                                    cleanedCues.add(new SubtitleCue(c.startMs, c.endMs, SmartLexicon.applyUniversalLexicon(c.text)));
                                }
                            } else {
                                List<SubtitleCue> pronounAdjusted = SmartLexicon.automateOpenSubtitlesPronouns(osCues, "id", 2, 10.0);
                                cleanedCues = new ArrayList<>(pronounAdjusted.size());
                                for (SubtitleCue c : pronounAdjusted) {
                                    cleanedCues.add(new SubtitleCue(c.startMs, c.endMs, SmartLexicon.applyUniversalLexicon(c.text)));
                                }
                            }

                            synchronized (SubtitleManager.this) {
                                activeCues = cleanedCues;
                                lastCueIndex = -1;
                                activeLabel = "Bahasa Indonesia [OpenSubtitles & Merge]";
                                activeLanguage = "id";
                                activeEngine = "smart_merge";
                                if (listener != null) listener.onSubtitleLoaded(activeLabel, activeCues.size());
                            }
                        }

                        @Override
                        public void onError(String error) {
                            // Fallback ke Google Translate GTX (Tier 3)
                            runGtxFallback(enCues, sourceUrl, loadId, forceRefresh);
                        }
                    });
                } else {
                    // Fallback ke Google Translate GTX (Tier 3)
                    runGtxFallback(enCues, sourceUrl, loadId, forceRefresh);
                }
            }

            @Override
            public void onError(String error) {
                runGtxFallback(enCues, sourceUrl, loadId, forceRefresh);
            }
        });
    }

    private void runGtxFallback(List<SubtitleCue> enCues, String sourceUrl, int loadId, boolean forceRefresh) {
        if (enCues.isEmpty()) {
            if (listener != null) listener.onSubtitleError("Subtitle tidak tersedia untuk episode ini");
            return;
        }
        GtxTranslator.translateAsync(context, sourceUrl, enCues, forceRefresh, new GtxTranslator.TranslateCallback() {
            @Override
            public void onProgress(int progress, int total) {
                if (loadId != currentLoadId) return;
                int pct = total > 0 ? (progress * 100 / total) : 0;
                if (listener != null) listener.onSubtitleLoading("Menerjemahkan (" + pct + "%)...");
            }

            @Override
            public void onSuccess(List<SubtitleCue> translatedCues) {
                if (loadId != currentLoadId) return;
                synchronized (SubtitleManager.this) {
                    activeCues = translatedCues;
                    lastCueIndex = -1;
                    activeLabel = "Bahasa Indonesia [Smart Lexicon]";
                    activeLanguage = "id";
                    activeEngine = "smart_merge";
                    if (listener != null) listener.onSubtitleLoaded(activeLabel, activeCues.size());
                }
            }

            @Override
            public void onError(String error) {
                if (loadId != currentLoadId) return;
                if (listener != null) listener.onSubtitleError("Gagal menerjemahkan subtitle: " + error);
            }
        });
    }

    public void loadAiSubtitleFromTracks(List<SubtitleTrack> tracks, String title, int episode) {
        loadAiSubtitleFromTracks(tracks, title, episode, false);
    }

    public void loadAiSubtitleFromTracks(List<SubtitleTrack> tracks, String title, int episode, boolean forceRefresh) {
        if (tracks == null || tracks.isEmpty()) {
            if (listener != null) listener.onSubtitleLoading("Tidak ada subtitle bawaan untuk AI Fansub");
            return;
        }

        final SubtitleTrack sourceEn = SubtitleTrack.findEnglishTrack(tracks);
        if (sourceEn == null) {
            if (listener != null) listener.onSubtitleLoading("Tidak ada subtitle bawaan untuk AI Fansub");
            return;
        }
        final int loadId = ++currentLoadId;
        synchronized (this) {
            activeLanguage = "id";
            activeEngine = "ai";
            clearCues();
        }

        if (listener != null) listener.onSubtitleLoading("Menyiapkan sumber subtitle English...");

        new Thread(() -> {
            try {
                List<SubtitleCue> enCues = fetchCuesFromUrl(sourceEn.url, sourceEn.referer);
                if (loadId != currentLoadId) return;
                if (enCues == null || enCues.isEmpty()) {
                    if (listener != null) listener.onSubtitleError("Gagal mengambil teks subtitle sumber");
                    return;
                }
                loadAiSubtitle(enCues, sourceEn.url, forceRefresh, loadId);
            } catch (Exception e) {
                if (loadId == currentLoadId && listener != null) {
                    listener.onSubtitleError("Gagal mengunduh subtitle sumber: " + e.getMessage());
                }
            }
        }).start();
    }

    public void loadAiSubtitle(List<SubtitleCue> enCues, String sourceUrl, boolean forceRefresh) {
        final int loadId = ++currentLoadId;
        loadAiSubtitle(enCues, sourceUrl, forceRefresh, loadId);
    }

    public void loadAiSubtitle(List<SubtitleCue> enCues, String sourceUrl, boolean forceRefresh, int loadId) {
        String apiKey = AiSubtitleService.getKey(context);
        if (apiKey.isEmpty()) {
            if (listener != null) listener.onSubtitleError("Kunci API LLM belum diatur");
            return;
        }

        AiSubtitleService.Provider prov = AiSubtitleService.getActiveProvider(context);
        String model = AiSubtitleService.getEffectiveModel(context, prov);
        String customPrompt = AiSubtitleService.getCustomPrompt(context);

        AiSubtitleService aiService = new AiSubtitleService();
        aiService.translateCuesAsync(context, sourceUrl, enCues, customPrompt, model, apiKey, forceRefresh, new AiSubtitleService.TranslateCuesCallback() {
            @Override
            public void onProgress(int progress, int total) {
                if (loadId != currentLoadId) return;
                int pct = total > 0 ? (progress * 100 / total) : 0;
                if (listener != null) listener.onSubtitleLoading("Menerjemahkan AI (" + pct + "%)...");
            }

            @Override
            public void onSuccess(List<SubtitleCue> translatedCues) {
                if (loadId != currentLoadId) return;
                synchronized (SubtitleManager.this) {
                    activeCues = translatedCues;
                    lastCueIndex = -1;
                    activeLabel = "Bahasa Indonesia [AI Fansub · " + model + "]";
                    activeLanguage = "id";
                    activeEngine = "ai";
                    if (listener != null) listener.onSubtitleLoaded(activeLabel, activeCues.size());
                }
            }

            @Override
            public void onError(String error) {
                if (loadId != currentLoadId) return;
                if (listener != null) listener.onSubtitleError("AI Fansub error: " + error);
            }
        });
    }

    public void loadRawTrack(String url, String label, String lang) {
        loadRawTrack(url, label, lang, "");
    }

    public void loadRawTrack(String url, String label, String lang, String referer) {
        final int loadId = ++currentLoadId;
        synchronized (this) {
            activeLanguage = lang != null ? lang : "en";
            activeEngine = "raw";
            clearCues();
        }
        if (listener != null) listener.onSubtitleLoading("Memuat track: " + label);

        new Thread(() -> {
            try {
                List<SubtitleCue> cues = fetchCuesFromUrl(url, referer);
                if (loadId != currentLoadId) return;

                if (lang != null && (lang.toLowerCase().contains("id") || lang.toLowerCase().contains("ind"))) {
                    List<SubtitleCue> lexCues = new ArrayList<>(cues.size());
                    for (SubtitleCue c : cues) {
                        lexCues.add(new SubtitleCue(c.startMs, c.endMs, SmartLexicon.applyUniversalLexicon(c.text)));
                    }
                    cues = lexCues;
                }

                synchronized (SubtitleManager.this) {
                    activeCues = cues;
                    lastCueIndex = -1;
                    activeLabel = label;
                    activeLanguage = lang;
                    activeEngine = "raw";
                    if (listener != null) listener.onSubtitleLoaded(activeLabel, activeCues.size());
                }
            } catch (Exception e) {
                if (loadId == currentLoadId && listener != null) {
                    listener.onSubtitleError("Gagal memuat track: " + e.getMessage());
                }
            }
        }).start();
    }

    public void setCustomCues(List<SubtitleCue> cues, String label) {
        synchronized (this) {
            ++currentLoadId;
            activeLanguage = "custom";
            activeEngine = "custom";
            clearCues();

            List<SubtitleCue> target = cues != null ? cues : new ArrayList<>();
            if (label != null && label.toLowerCase().contains("indo")) {
                List<SubtitleCue> lexCues = new ArrayList<>(target.size());
                for (SubtitleCue c : target) {
                    lexCues.add(new SubtitleCue(c.startMs, c.endMs, SmartLexicon.applyUniversalLexicon(c.text)));
                }
                target = lexCues;
            }
            activeCues = target;
            lastCueIndex = -1;
            activeLabel = label;
            subEnabled = true;
            if (listener != null) listener.onSubtitleLoaded(activeLabel, activeCues.size());
        }
    }

    private List<SubtitleCue> fetchCuesFromUrl(String url) throws Exception {
        return fetchCuesFromUrl(url, "");
    }

    private List<SubtitleCue> fetchCuesFromUrl(String url, String preferredReferer) throws Exception {
        OkHttpClient client = HttpClientProvider.getClient();

        // Rantai referer anti-403 untuk berbagai proteksi hotlinking CDN (Megaplay, EclipseHarbor, EmeraldDrift, dsb.)
        List<String> refererCandidates = new ArrayList<>();
        if (preferredReferer != null && !preferredReferer.isEmpty()) {
            refererCandidates.add(preferredReferer);
        }
        // Coba gunakan host URL itu sendiri sebagai referer/origin
        try {
            okhttp3.HttpUrl u = okhttp3.HttpUrl.parse(url);
            if (u != null) {
                String scheme = u.scheme();
                String host = u.host();
                if (scheme != null && host != null) {
                    String base = scheme + "://" + host + "/";
                    if (!refererCandidates.contains(base)) {
                        refererCandidates.add(base);
                    }
                }
            }
        } catch (Exception ignored) {}

        refererCandidates.add("https://megaplay.buzz/");
        refererCandidates.add("https://hianime.to/");
        refererCandidates.add("https://hianime.at/");
        refererCandidates.add("https://hianime.sx/");
        refererCandidates.add("https://hianime.nz/");
        refererCandidates.add("https://zoro.to/");
        refererCandidates.add("https://aniwave.to/");
        refererCandidates.add("https://zokoanime.video/");
        refererCandidates.add("https://kaido.to/");
        refererCandidates.add("https://animixplay.to/");
        refererCandidates.add(""); // direct request tanpa referer

        Exception lastErr = null;
        for (String ref : refererCandidates) {
            try {
                Request.Builder reqB = new Request.Builder().url(url);
                reqB.header("User-Agent", HttpClientProvider.USER_AGENT);
                reqB.header("Accept", "text/vtt,text/plain,application/x-subrip,*/*;q=0.8");
                reqB.header("Accept-Language", "en-US,en;q=0.9,id;q=0.8");
                reqB.header("Connection", "keep-alive");
                reqB.header("Sec-Fetch-Dest", "empty");
                reqB.header("Sec-Fetch-Mode", "cors");
                reqB.header("Sec-Fetch-Site", "cross-site");

                if (!ref.isEmpty()) {
                    reqB.header("Referer", ref);
                    String origin = ref.endsWith("/") ? ref.substring(0, ref.length() - 1) : ref;
                    reqB.header("Origin", origin);
                } else {
                    reqB.header("Sec-Fetch-Site", "none");
                }

                try (Response resp = client.newCall(reqB.build()).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String content = resp.body().string();
                        List<SubtitleCue> cues = SubtitleParser.parse(content);
                        if (cues != null && !cues.isEmpty()) {
                            return cues;
                        }
                    } else if (resp.code() == 403 || resp.code() == 401) {
                        lastErr = new Exception("HTTP " + resp.code());
                        continue;
                    } else {
                        lastErr = new Exception("HTTP " + resp.code());
                    }
                }
            } catch (Exception e) {
                lastErr = e;
            }
        }

        throw (lastErr != null ? lastErr : new Exception("Gagal mengunduh subtitle"));
    }
}
