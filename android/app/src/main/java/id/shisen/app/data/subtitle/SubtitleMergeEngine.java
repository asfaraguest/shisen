package id.shisen.app.data.subtitle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubtitleMergeEngine {

    public static class MergeResult {
        public List<SubtitleCue> mergedCues;
        public long offsetMs;
        public int snappedCount;
        public int gapFilledCount;

        public MergeResult(List<SubtitleCue> mergedCues, long offsetMs, int snappedCount, int gapFilledCount) {
            this.mergedCues = mergedCues;
            this.offsetMs = offsetMs;
            this.snappedCount = snappedCount;
            this.gapFilledCount = gapFilledCount;
        }
    }

    public static long estimateOptimalOffset(List<SubtitleCue> builtinCues, List<SubtitleCue> externalCues) {
        if (builtinCues == null || builtinCues.isEmpty() || externalCues == null || externalCues.isEmpty()) {
            return 0L;
        }

        Map<Long, Integer> buckets = new HashMap<>();
        int limitB = Math.min(builtinCues.size(), 300);
        int limitO = Math.min(externalCues.size(), 300);

        for (int i = 0; i < limitB; i++) {
            long bs = builtinCues.get(i).startMs;
            for (int j = 0; j < limitO; j++) {
                long os = externalCues.get(j).startMs;
                long diff = bs - os;
                if (Math.abs(diff) <= 15000) {
                    // Kuantisasi ke resolusi 200ms
                    long q = Math.round(diff / 200.0) * 200;
                    buckets.put(q, buckets.getOrDefault(q, 0) + 1);
                }
            }
        }

        if (buckets.isEmpty()) return 0L;

        long bestQ = 0L;
        int maxCount = 0;
        for (Map.Entry<Long, Integer> entry : buckets.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                bestQ = entry.getKey();
            }
        }

        int minRequired = Math.max(2, Math.min(limitB, limitO) / 10);
        if (maxCount < minRequired) return 0L;

        return bestQ;
    }

    public static MergeResult merge(List<SubtitleCue> builtinCues, List<SubtitleCue> externalCues, long offsetMs) {
        return merge(builtinCues, externalCues, offsetMs, null);
    }

    public static MergeResult merge(List<SubtitleCue> builtinCues, List<SubtitleCue> externalCues,
                                    long offsetMs, Map<Integer, String> translatedGaps) {
        if (externalCues == null || externalCues.isEmpty()) {
            return new MergeResult(builtinCues != null ? builtinCues : new ArrayList<>(), 0L, 0, 0);
        }
        if (builtinCues == null || builtinCues.isEmpty()) {
            List<SubtitleCue> shifted = new ArrayList<>();
            for (SubtitleCue c : externalCues) {
                shifted.add(new SubtitleCue(Math.max(0, c.startMs + offsetMs), Math.max(100, c.endMs + offsetMs), c.text));
            }
            return new MergeResult(shifted, offsetMs, 0, 0);
        }

        List<SubtitleCue> merged = new ArrayList<>();
        int snappedCount = 0;
        int gapFilledCount = 0;
        Set<Integer> matchedBuiltinIndices = new HashSet<>();

        for (SubtitleCue oc : externalCues) {
            long os = Math.max(0, oc.startMs + offsetMs);
            long oe = Math.max(os + 100, oc.endMs + offsetMs);
            String txt = oc.text;
            if (txt.isEmpty()) continue;

            // Cari cue bawaan terdekat
            int bestIdx = -1;
            long bestDist = 999999;
            for (int b = 0; b < builtinCues.size(); b++) {
                SubtitleCue bc = builtinCues.get(b);
                long overlap = Math.min(oe, bc.endMs) - Math.max(os, bc.startMs);
                if (overlap >= -250 || Math.abs(os - bc.startMs) <= 650) {
                    matchedBuiltinIndices.add(b);
                    long dist = Math.abs(os - bc.startMs) + Math.abs(oe - bc.endMs);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestIdx = b;
                    }
                }
            }

            long finalS = os;
            long finalE = oe;
            if (bestIdx >= 0) {
                SubtitleCue bc = builtinCues.get(bestIdx);
                if (Math.abs(os - bc.startMs) <= 850 && Math.abs(oe - bc.endMs) <= 1100) {
                    finalS = bc.startMs;
                    finalE = bc.endMs;
                    snappedCount++;
                }
            }

            merged.add(new SubtitleCue(finalS, finalE, txt));
        }

        // Isi celah dari subtitle bawaan yang tidak ada di rilis fansub eksternal (Signs, OP/ED)
        // Ref: translate.py#L340-L353
        for (int b = 0; b < builtinCues.size(); b++) {
            if (!matchedBuiltinIndices.contains(b)) {
                SubtitleCue bc = builtinCues.get(b);
                if (!bc.text.isEmpty()) {
                    // Gunakan terjemahan jika tersedia
                    String gapText = (translatedGaps != null) ? translatedGaps.get(b) : null;
                    if (gapText != null && !gapText.isEmpty()) {
                        merged.add(new SubtitleCue(bc.startMs, bc.endMs, gapText));
                        gapFilledCount++;
                    }
                    // Jika tidak ada terjemahan → SKIP, jangan inject English mentah
                }
            }
        }

        List<SubtitleCue> finalCues = SubtitleParser.mergeOverlappingCues(merged);
        return new MergeResult(finalCues, offsetMs, snappedCount, gapFilledCount);
    }

    /**
     * Cari indeks cue bawaan yang TIDAK cocok dengan external cues.
     * Ref: translate.py find_unmatched_builtin_cues()
     *
     * @return List of int[] dimana int[0] = index di builtinCues
     */
    public static List<int[]> findUnmatchedBuiltinIndices(
            List<SubtitleCue> builtinCues,
            List<SubtitleCue> externalCues,
            long offsetMs) {
        List<int[]> result = new ArrayList<>();
        if (builtinCues == null || externalCues == null) return result;

        for (int b = 0; b < builtinCues.size(); b++) {
            SubtitleCue bc = builtinCues.get(b);
            if (bc.text.isEmpty()) continue;

            boolean matched = false;
            for (SubtitleCue oc : externalCues) {
                long os = Math.max(0, oc.startMs + offsetMs);
                long oe = Math.max(os + 100, oc.endMs + offsetMs);
                long overlap = Math.min(oe, bc.endMs) - Math.max(os, bc.startMs);
                if (overlap >= -250 || Math.abs(os - bc.startMs) <= 650) {
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                result.add(new int[]{b});
            }
        }
        return result;
    }
}
