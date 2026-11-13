import Foundation

public final class SubtitleMergeEngine {
    public static let shared = SubtitleMergeEngine()
    
    private init() {}
    
    /// Merge ID cues (as base) with English gap cues (Signs, OP, ED) ONLY when translated
    public func merge(
        builtinCues: [Cue],          // Usually English from video stream (complete with signs)
        externalIdCues: [Cue],       // Indonesian subtitles (dialog base)
        optimalOffsetMs: Int64 = 0,
        translatedGaps: [Int: String] = [:] // Map of builtin index -> translated text
    ) -> [Cue] {
        var merged: [Cue] = []
        
        // 1. Add all external Indonesian base cues (with applied offset if needed)
        for cue in externalIdCues {
            let adjustedStart = max(0, cue.startMs + optimalOffsetMs)
            let adjustedEnd = max(adjustedStart + 100, cue.endMs + optimalOffsetMs)
            let sanitized = SmartLexicon.shared.sanitizeCue(cue.text)
            merged.append(Cue(startMs: adjustedStart, endMs: adjustedEnd, text: sanitized, originalText: cue.text))
        }
        
        // 2. Invariant: Gap-fill HANYA masuk jika ada terjemahan; jangan inject English mentah
        for (idx, translatedText) in translatedGaps {
            guard idx >= 0 && idx < builtinCues.count else { continue }
            let gapCue = builtinCues[idx]
            let cleanTranslated = SmartLexicon.shared.sanitizeCue(translatedText)
            
            // Validate that translation is not empty and not identical to raw English
            if !cleanTranslated.isEmpty && cleanTranslated.lowercased() != gapCue.text.lowercased() {
                merged.append(Cue(startMs: gapCue.startMs, endMs: gapCue.endMs, text: cleanTranslated, originalText: gapCue.text))
            }
        }
        
        // 3. Sort chronologically by start timestamp
        merged.sort { $0.startMs < $1.startMs }
        return merged
    }
    
    /// Cari index cue pada builtin (English) yang belum ada padanannya di ID (Signs, Song lyrics, On-screen text)
    public func findUnmatchedBuiltinIndices(builtin: [Cue], idCues: [Cue], offsetMs: Int64 = 0) -> [Int] {
        var unmatched: [Int] = []
        let toleranceMs: Int64 = 1500
        
        for (idx, bCue) in builtin.enumerated() {
            var matched = false
            for idCue in idCues {
                let adjIdStart = idCue.startMs + offsetMs
                let adjIdEnd = idCue.endMs + offsetMs
                
                // Cek overlap waktu
                if abs(bCue.startMs - adjIdStart) <= toleranceMs ||
                   (bCue.startMs >= adjIdStart && bCue.startMs <= adjIdEnd) ||
                   (adjIdStart >= bCue.startMs && adjIdStart <= bCue.endMs) {
                    matched = true
                    break
                }
            }
            if !matched {
                unmatched.append(idx)
            }
        }
        return unmatched
    }
    
    /// Estimasi perbedaan waktu (offset) antara track English bawaan dan track Indonesia eksternal
    public func estimateOptimalOffset(builtin: [Cue], idCues: [Cue]) -> Int64 {
        guard !builtin.isEmpty && !idCues.isEmpty else { return 0 }
        
        // Bandingkan cue dialog pertama
        let bFirst = builtin.first(where: { $0.text.count > 3 }) ?? builtin[0]
        let idFirst = idCues.first(where: { $0.text.count > 3 }) ?? idCues[0]
        
        let diff = bFirst.startMs - idFirst.startMs
        // Batasi rentang offset wajar (-10s s/d +10s)
        if abs(diff) <= 10000 {
            return diff
        }
        return 0
    }
}
