package id.menatap.app.data.subtitle;

import android.content.Context;
import id.menatap.app.data.model.SubtitleTrack;
import id.menatap.app.data.service.HttpClientProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;

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
        this.context = context.getApplicationContext();
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
            clearCues();
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

    public long getSubtitleOffsetMs() {
        return subtitleOffsetMs;
    }

    public void setSubtitleOffsetMs(long offsetMs) {
        this.subtitleOffsetMs = offsetMs;
    }

    public synchronized void clearCues() {
        activeCues = new ArrayList<>();
    }

    public synchronized String getCurrentText(long currentPositionMs) {
        if (!subEnabled) return "";
        List<SubtitleCue> cues = activeCues;
        if (cues == null || cues.isEmpty()) return "";

        long effectivePos = currentPositionMs + subtitleOffsetMs;
        int size = cues.size();
        for (int i = 0; i < size; i++) {
            if (i >= cues.size()) break;
            SubtitleCue cue = cues.get(i);
            if (cue != null && cue.isActive(effectivePos)) {
                return cue.text != null ? cue.text : "";
            }
        }
        return "";
    }

    public void loadFromTracks(List<SubtitleTrack> tracks, String title, int episode, boolean forceRefresh) {
        if (tracks == null || tracks.isEmpty()) {
            if (listener != null) listener.onSubtitleLoading("Tidak ada subtitle bawaan");
            return;
        }

        SubtitleTrack enTrack = null;
        SubtitleTrack idTrack = null;

        for (SubtitleTrack t : tracks) {
            String l = t.lang.toLowerCase();
            if (enTrack == null && (l.contains("en") || t.label.toLowerCase().contains("english"))) {
                enTrack = t;
            }
            if (idTrack == null && (l.contains("id") || l.contains("ind") || t.label.toLowerCase().contains("indonesia"))) {
                idTrack = t;
            }
        }

        if (idTrack != null && "raw".equals(activeEngine)) {
            loadRawTrack(idTrack.url, "Bahasa Indonesia (Asli)", "id");
            return;
        }

        if ("en".equals(activeLanguage)) {
            if (enTrack != null) {
                loadRawTrack(enTrack.url, "English (Original)", "en");
            } else {
                loadRawTrack(tracks.get(0).url, tracks.get(0).label, tracks.get(0).lang);
            }
            return;
        }

        // Mode Smart Fansub & Merge (Bahasa Indonesia):
        // 1. Ambil track English terlebih dahulu
        final SubtitleTrack sourceEn = (enTrack != null) ? enTrack : tracks.get(0);
        final int loadId = ++currentLoadId;

        if (listener != null) listener.onSubtitleLoading("Menyiapkan subtitle...");

        new Thread(() -> {
            try {
                // Unduh English source cues
                List<SubtitleCue> enCues = fetchCuesFromUrl(sourceEn.url);
                if (loadId != currentLoadId) return;

                if (!enCues.isEmpty()) {
                    synchronized (SubtitleManager.this) {
                        sourceEnglishCues = enCues;
                        // Tampilkan teks sumber terlebih dahulu agar instan (zero black screen)
                        if (activeCues.isEmpty()) {
                            activeCues = new ArrayList<>(enCues);
                            activeLabel = "English [Menerjemahkan ke ID...]";
                            if (listener != null) listener.onSubtitleLoaded(activeLabel, activeCues.size());
                        }
                    }
                }

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
                                    long offset = SubtitleMergeEngine.estimateOptimalOffset(enCues, atCues);
                                    SubtitleMergeEngine.MergeResult mr = SubtitleMergeEngine.merge(enCues, atCues, offset);

                                    // Terapkan Universal Lexicon (Koreksi istilah RPG, Game Satisfy Overgeared, dll.)
                                    List<SubtitleCue> cleanedCues = new ArrayList<>(mr.mergedCues.size());
                                    for (SubtitleCue c : mr.mergedCues) {
                                        cleanedCues.add(new SubtitleCue(c.startMs, c.endMs, SmartLexicon.applyUniversalLexicon(c.text)));
                                    }

                                    synchronized (SubtitleManager.this) {
                                        activeCues = cleanedCues;
                                        activeLabel = "Bahasa Indonesia [Fansub & Merge]";
                                        activeLanguage = "id";
                                        activeEngine = "smart_merge";
                                        if (listener != null) listener.onSubtitleLoaded(activeLabel, activeCues.size());
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    // Fallback ke OpenSubtitles (Tier 2)
                                    runOpenSubtitlesFallback(enCues, title, episode, sourceEn.url, loadId, forceRefresh);
                                }
                            });
                        } else {
                            // Tidak ada di AnimeTosho -> fallback ke OpenSubtitles (Tier 2)
                            runOpenSubtitlesFallback(enCues, title, episode, sourceEn.url, loadId, forceRefresh);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Fallback ke OpenSubtitles (Tier 2)
                        runOpenSubtitlesFallback(enCues, title, episode, sourceEn.url, loadId, forceRefresh);
                    }
                });

            } catch (Exception e) {
                if (loadId == currentLoadId && listener != null) {
                    listener.onSubtitleError("Gagal memuat subtitle: " + e.getMessage());
                }
            }
        }).start();
    }

    private void runOpenSubtitlesFallback(List<SubtitleCue> enCues, String title, int episode, String sourceUrl, int loadId, boolean forceRefresh) {
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
                            long offset = SubtitleMergeEngine.estimateOptimalOffset(enCues, osCues);
                            SubtitleMergeEngine.MergeResult mr = SubtitleMergeEngine.merge(enCues, osCues, offset);

                            // Otomasi kata ganti OpenSubtitles berdasarkan konteks adegan
                            List<SubtitleCue> pronounAdjusted = SmartLexicon.automateOpenSubtitlesPronouns(mr.mergedCues, "id", 2, 10.0);
                            List<SubtitleCue> cleanedCues = new ArrayList<>(pronounAdjusted.size());
                            for (SubtitleCue c : pronounAdjusted) {
                                cleanedCues.add(new SubtitleCue(c.startMs, c.endMs, SmartLexicon.applyUniversalLexicon(c.text)));
                            }

                            synchronized (SubtitleManager.this) {
                                activeCues = cleanedCues;
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
        if (enCues.isEmpty()) return;
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
                    activeLabel = "Bahasa Indonesia [Smart Lexicon]";
                    activeLanguage = "id";
                    activeEngine = "smart_merge";
                    if (listener != null) listener.onSubtitleLoaded(activeLabel, activeCues.size());
                }
            }

            @Override
            public void onError(String error) {
                if (loadId != currentLoadId) return;
                if (listener != null) listener.onSubtitleLoaded("English [Teks Asli]", activeCues.size());
            }
        });
    }

    public void loadRawTrack(String url, String label, String lang) {
        final int loadId = ++currentLoadId;
        if (listener != null) listener.onSubtitleLoading("Memuat track: " + label);

        new Thread(() -> {
            try {
                List<SubtitleCue> cues = fetchCuesFromUrl(url);
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
            List<SubtitleCue> target = cues != null ? cues : new ArrayList<>();
            if (label != null && label.toLowerCase().contains("indo")) {
                List<SubtitleCue> lexCues = new ArrayList<>(target.size());
                for (SubtitleCue c : target) {
                    lexCues.add(new SubtitleCue(c.startMs, c.endMs, SmartLexicon.applyUniversalLexicon(c.text)));
                }
                target = lexCues;
            }
            activeCues = target;
            activeLabel = label;
            activeLanguage = "custom";
            activeEngine = "custom";
            subEnabled = true;
            if (listener != null) listener.onSubtitleLoaded(activeLabel, activeCues.size());
        }
    }

    private List<SubtitleCue> fetchCuesFromUrl(String url) throws Exception {
        OkHttpClient client = HttpClientProvider.getClient();
        Request.Builder reqB = new Request.Builder().url(url);
        if (url.toLowerCase().contains("megaplay") || url.toLowerCase().contains("megacloud") || url.toLowerCase().contains("broforgotsave")) {
            reqB.header("Referer", "https://megaplay.buzz/");
        }
        try (Response resp = client.newCall(reqB.build()).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                throw new Exception("HTTP " + resp.code());
            }
            String content = resp.body().string();
            return SubtitleParser.parse(content);
        }
    }
}
